/**
 * Created by skol on 17.03.17.
 */
package nox.internal.bundle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.java.archives.internal.DefaultManifest;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.util.WrapUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;


class OSGiManifestImpl extends DefaultManifest implements OSGiManifest {

	private String groupId = null;
	protected String artifactId = null;
	protected String version = null;
	private boolean withQualifier = false;
	private String symbolicName;
	private final LinkedHashMap<String, String> instructions = Maps.newLinkedHashMap();
	private final List<String> exports = Lists.newArrayList();
	private final List<String> privates = Lists.newArrayList();
	private final List<String> optionals = Lists.newArrayList();
	private File classesJarOrDir = null;
	private FileCollection classpath = null;
	private boolean replaceOSGiManifest = false;

	OSGiManifestImpl(PathToFileResolver fileResolver) {
		super(fileResolver);
	}

	@Override
	public DefaultManifest getEffectiveManifest() {
		ModuleVersionIdentifier moduleId = new DefaultModuleVersionIdentifier(groupId, artifactId, version);
		try {
			ManifestConverter converter = ManifestConverter.withModuleId(moduleId)
			.withModuleDef(this)
			.withClassesJarOrDir(classesJarOrDir)
			.withClasspath(classpath.getFiles())
			.instance();
			DefaultManifest baseManifest = new DefaultManifest(null);
			baseManifest.attributes(getAttributes());

			Manifest manifest = converter.convertToOSGiManifest();
			java.util.jar.Attributes attributes = manifest.getMainAttributes();
			for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
				baseManifest.attributes(WrapUtil.toMap(entry.getKey().toString(), (String) entry.getValue()));
			}

			return getEffectiveManifestInternal(baseManifest);
		} catch (IOException ex) {
			throw new GradleException("OSGi manifest generation failed", ex);
		}
	}

	@Override
	public void groupId(String groupId) {
		this.groupId = groupId;
	}

	@Override
	public String getGroupId() {
		return groupId;
	}

	@Override
	public void artifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public void version(String version) {
		this.version = version;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void withQualifier(boolean flag) {
		withQualifier = flag;
	}

	@Override
	public boolean getWithQualifier() {
		return withQualifier;
	}

	@Override
	public void symbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public void instruction(String instruction, String value) {
		instructions.put(instruction, value);
	}

	@Override
	public LinkedHashMap<String, String> getInstructions() {
		return instructions;
	}

	@Override
	public void exports(String... pkgNames) {
		exports.addAll(Lists.newArrayList(pkgNames));
	}

	@Override
	public List<String> getExports() {
		return Collections.unmodifiableList(exports);
	}

	@Override
	public void privates(String... pkgNames) {
		privates.addAll(Lists.newArrayList(pkgNames));
	}

	@Override
	public List<String> getPrivates() {
		return Collections.unmodifiableList(privates);
	}

	@Override
	public void optionals(String... pkgNames) {
		optionals.addAll(Lists.newArrayList(pkgNames));
	}

	@Override
	public List<String> getOptionals() {
		return Collections.unmodifiableList(optionals);
	}

	@Override
	public OSGiManifest withClassesJarOrDir(File classesJarOrDir) {
		this.classesJarOrDir = classesJarOrDir;
		return this;
	}

	@Override
	public OSGiManifest withClasspath(FileCollection classpath) {
		this.classpath = classpath;
		return this;
	}

	@Override
	public String toString() {
		String res = groupId;
		if (StringUtils.isNotBlank(artifactId)) {
			res += ":" + artifactId;
		}
		if (StringUtils.isNotBlank(version)) {
			res += ":" + version;
		}
		return res;
	}

	@Override
	public void replaceOSGiManifest(boolean flag) {
		this.replaceOSGiManifest = flag;
	}

	@Override
	public boolean getReplaceOSGiManifest() {
		return replaceOSGiManifest;
	}
}
