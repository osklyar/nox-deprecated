/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.osgi.Analyzer;
import nox.ext.Bundles;
import nox.ext.Platform;
import nox.internal.bundle.ArtifactResolver;
import nox.internal.bundle.BundleDef;
import nox.internal.bundle.Bundlizer;
import nox.internal.bundle.ManifestConverter;
import nox.internal.bundle.ResolvedArtifactExt;
import nox.internal.bundle.RuleDef;
import nox.internal.gradlize.BundleUniverse;
import nox.internal.gradlize.Duplicates;
import nox.internal.platform.Assembler;


public class Bundle extends DefaultTask {

	public static final String name = Bundle.class.getSimpleName().toLowerCase();

	private static final Logger logger = LoggerFactory.getLogger(Bundle.class);

	private final Platform platform;

	private final Bundles bundles;

	private final List<File> bundleJars = Lists.newArrayList();

	Assembler assembler = Assembler.instance();

	@InputFile
	public File getBundleConfigFile() {
		return new File(getProject().getBuildDir(), Bundles.bundlesConfigFile);
	}

	@OutputDirectory
	public File getP2Dir() {
		return platform.getP2Dir();
	}

	@OutputFiles
	public List<File> bundleJars() {
		return bundleJars;
	}

	public Bundle() {
		setGroup("nox.Platform");
		setDescription(
			"Generates OSGi p2 repository with bundles converted from external and internal Jars.");

		ExtensionContainer extensions = getProject().getExtensions();
		platform = extensions.findByType(Platform.class);
		bundles = extensions.findByType(Bundles.class);
	}

	@TaskAction
	public void action(IncrementalTaskInputs inputs) {
		if (!inputs.isIncremental()) {
			action();
		} else {
			inputs.outOfDate(na -> action());
		}
	}

	private void action() {
		File pluginsDir = new File(getProject().getBuildDir(), Platform.PLUGINS_SUBDIR);
		try {
			FileUtils.deleteDirectory(getP2Dir());
			FileUtils.deleteDirectory(pluginsDir);
		} catch (IOException ex) {
			throw new GradleException("Failed to clean up earlier p2 repository.", ex);
		}
		pluginsDir.mkdirs();
		if (!getP2Dir().mkdirs()) {
			throw new GradleException(String.format("Failed to create plugins directory %s", pluginsDir));
		}

		ArtifactResolver resolver = ArtifactResolver
			.withDependencyHelper(	getProject().getDependencies())
			.withConfigurationContainer(getProject().getConfigurations())
			.withSources(bundles.getWithSources())
			.instance();
		Bundlizer bundlizer = Bundlizer.instance(pluginsDir);

		for (BundleDef bundleDef : bundles.getBundleDefs()) {
			List<RuleDef> ruleDefs = Lists.newArrayList();
			ruleDefs.addAll(bundles.getRuleDefs());
			ruleDefs.addAll(bundleDef.getRuleDefs());
			ruleDefs = Collections.unmodifiableList(ruleDefs);

			Collection<ResolvedArtifactExt> artifacts = resolver.resolve(bundleDef);
			for (ResolvedArtifactExt artifact : artifacts) {
				ModuleVersionIdentifier moduleId = artifact.artifact.getModuleVersion().getId();
				Preconditions.checkNotNull(moduleId, "Module Id must be available");
				try {
					File jar = artifact.artifact.getFile();
					Preconditions.checkNotNull(jar, "Artifact file not found for %s", moduleId);
					Manifest originalManifest = new JarFile(jar).getManifest();

					ManifestConverter converter = ManifestConverter
						.withModuleId(artifact.artifact.getModuleVersion().getId())
						.withManifest(originalManifest)
						.withClassesJarOrDir(jar)
						.withRequiredModules(artifact.requiredModules)
						.withModuleDef(bundleDef)
						.withRuleDefs(ruleDefs)
						.instance();

					Manifest targetManifest = converter.convertToOSGiManifest();

					File bundleJar = bundlizer.bundleJar(artifact.artifact.getFile(), targetManifest);
					bundleJars.add(bundleJar);
					logger.info(":bundle {} -> {} => {}", bundleDef, moduleId, bundleJar);

					if (artifact.sourceJar != null) {
						// FIXME: is this a hack or is this actually better than add source support to converter?
						Manifest sourceManifest = new Manifest();
						Attributes attrs = sourceManifest.getMainAttributes();
						attrs.putAll(targetManifest.getMainAttributes());

						String bundleSymbolicName = attrs.getValue(Analyzer.BUNDLE_SYMBOLICNAME);
						String bundleVersion = attrs.getValue(Analyzer.BUNDLE_VERSION);
						attrs.putValue(Analyzer.BUNDLE_SYMBOLICNAME, bundleSymbolicName + ".source");
						attrs.putValue("Eclipse-SourceBundle", String.format("%s;version=\"%s\"", bundleSymbolicName, bundleVersion));
						attrs.remove(new Name(Analyzer.REQUIRE_BUNDLE));
						attrs.remove(new Name(Analyzer.REQUIRE_CAPABILITY));
						attrs.remove(new Name(Analyzer.IMPORT_PACKAGE));
						attrs.remove(new Name(Analyzer.EXPORT_PACKAGE));

						bundleJar = bundlizer.bundleJar(artifact.sourceJar, sourceManifest);
						bundleJars.add(bundleJar);
						logger.info(":bundle {} -> {} source => {}", bundleDef, moduleId, bundleJar);
					}

				} catch (IOException ex) {
					logger.error("Failed to bundlize {} as a dependency of {}: {}", moduleId, bundleDef, ex);
				}
			}
		}

		try {
			assembler.publishBundles(platform.getSdkExec(), getProject().getBuildDir(), getP2Dir());

			// print dependencies that cannot be resolved
			new UniverseAnalyzer(new File(getP2Dir(), Platform.PLUGINS_SUBDIR)).analyze(BundleUniverse.instance(Duplicates.Forbid));

		} catch (IOException ex) {
			throw new GradleException("Failed to assemble target platform", ex);
		}
	}
}
