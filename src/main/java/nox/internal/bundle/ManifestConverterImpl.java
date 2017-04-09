/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ModuleVersionIdentifier;

import aQute.bnd.osgi.Analyzer;
import nox.internal.entity.Version;
import nox.internal.entity.Version.Component;


class ManifestConverterImpl implements ManifestConverter, ManifestConverter.Configurator {

	private final ModuleVersionIdentifier moduleId;

	private Manifest manifest;

	private File classesJarOrDir = null;

	private Set<ModuleVersionIdentifier> requiredModules = Sets.newHashSet();

	private List<RuleDef> ruleDefs = Lists.newArrayList();

	// TODO interface and open to package for testing
	private ManifestConverterUtil manifestUtil = new ManifestConverterUtil();

	private ModuleDef moduleDef = null;

	private Set<File> classpath = Sets.newHashSet();

	ManifestConverterImpl(ModuleVersionIdentifier moduleId) throws IOException {
		Preconditions.checkNotNull(moduleId, "Module Id must be available");
		this.moduleId = moduleId;
	}

	@Override
	public Configurator withManifest(Manifest manifest) {
		this.manifest = manifest;
		return this;
	}

	@Override
	public Configurator withClassesJarOrDir(File classesJarOrDir) {
		this.classesJarOrDir = classesJarOrDir;
		return this;
	}

	@Override
	public Configurator withClasspath(Collection<File> classpath) {
		this.classpath.addAll(classpath);
		return this;
	}

	@Override
	public Configurator withRequiredModules(Collection<ModuleVersionIdentifier> moduleIds) {
		this.requiredModules.addAll(moduleIds);
		return this;
	}

	@Override
	public Configurator withModuleDef(ModuleDef moduleDef) {
		this.moduleDef = moduleDef;
		return this;
	}

	@Override
	public Configurator withRuleDefs(List<RuleDef> ruleDefs) {
		this.ruleDefs.addAll(ruleDefs);
		return this;
	}

	@Override
	public ManifestConverter instance() {
		// put moduleDef as the last rule if provided
		if (moduleDef != null) {
			ruleDefs.add(moduleDef);
		}
		return this;
	}

	@Override
	public Manifest convertToOSGiManifest() throws IOException {
		String bundleSymbolicName = manifestUtil.bundleSymbolicName(moduleId, ruleDefs);
		Version bundleVersion = manifestUtil.bundleVersion(moduleId, ruleDefs);
		Set<String> requiredBundles = requiredBundles();


		boolean alreadyOSGi = manifest != null && StringUtils.isNotBlank(manifest.getMainAttributes().getValue(Analyzer.BUNDLE_SYMBOLICNAME));
		boolean replaceOSGimanifest = moduleDef != null && moduleDef.getReplaceOSGiManifest();

		// for existing OSGi manifests, update name, version and required bundles only
		if (alreadyOSGi && !replaceOSGimanifest) {
			Attributes attrs = manifest.getMainAttributes();
			attrs.putValue(Analyzer.BUNDLE_SYMBOLICNAME, bundleSymbolicName);
			attrs.putValue(Analyzer.BUNDLE_VERSION, bundleVersion.toString(Component.Build));
			if (!requiredBundles.isEmpty()) {
				attrs.putValue(Analyzer.REQUIRE_BUNDLE, StringUtils.join(requiredBundles, ","));
			}
			return manifest;
		}

		Analyzer analyzer = new Analyzer();

		// copy existing manifest
		if (manifest != null && !alreadyOSGi) {
			for (Map.Entry<Object, Object> attribute : manifest.getMainAttributes().entrySet()) {
				set(analyzer, attribute.getKey().toString(), attribute.getValue().toString());
			}
		}

		set(analyzer, Analyzer.REQUIRE_BUNDLE, StringUtils.join(requiredBundles, ","));

		for (RuleDef ruleDef : ruleDefs) {
			if (manifestUtil.isRelevant(ruleDef, moduleId)) {
				for (String instruction: ruleDef.getInstructions().keySet()) {
					if (Analyzer.BUNDLE_SYMBOLICNAME.equalsIgnoreCase(instruction)) {
						throw new GradleException("Use the 'symbolicName' directive to override the bundle symbolic name");
					}
					if (Analyzer.BUNDLE_VERSION.equalsIgnoreCase(instruction)) {
						throw new GradleException("Use the 'version' directive to override the bundle version");
					}
					set(analyzer, instruction, ruleDef.getInstructions().get(instruction));
				}
				for (String importPackage : ruleDef.getOptionals()) {
					set(analyzer, Analyzer.IMPORT_PACKAGE, importPackage + ";resolution:=optional");
				}
				for (String exportPackage : ruleDef.getExports()) {
					set(analyzer, Analyzer.EXPORT_PACKAGE, exportPackage);
				}
				for (String privatePackage : ruleDef.getPrivates()) {
					set(analyzer, Analyzer.EXPORT_PACKAGE, "!" + privatePackage);
				}
			}
		}
		set(analyzer, Analyzer.IMPORT_PACKAGE, "*");
		set(analyzer, Analyzer.EXPORT_PACKAGE, "*;-noimport:=true;version=" + bundleVersion.toString(Component.Build));

		analyzer.setBundleSymbolicName(bundleSymbolicName);
		analyzer.setBundleVersion(bundleVersion.toString(Component.Build));

		analyzer.setJar(classesJarOrDir);
		if (!classpath.isEmpty()) {
			analyzer.setClasspath(classpath);
		}

		try {
			return analyzer.calcManifest();
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private void set(Analyzer analyzer, String key, String value) {
		if (StringUtils.isNotBlank(value)) {
			String current = analyzer.getProperties().getProperty(key, "");
			analyzer.getProperties()
				.setProperty(key, (StringUtils.isBlank(current) ? "" : current + ",") + value);
		}
	}

	private Set<String> requiredBundles() {
		Set<String> res = Sets.newHashSet();
		for (ModuleVersionIdentifier moduleId : requiredModules) {
			String symbolicName = manifestUtil.bundleSymbolicName(moduleId, ruleDefs);
			Version version = manifestUtil.bundleVersion(moduleId, ruleDefs);
			res.add(String.format("%s;bundle-version=\"[%s,%s)\"", symbolicName,
				version.toString(Component.Minor),
				version.nextMajor().toString(Component.Major)));
		}
		return res;
	}
}
