/**
 * Created by skol on 17.03.17.
 */
package nox.internal.bundle;

import java.io.File;
import java.util.Collection;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Manifest;
import org.gradle.internal.file.PathToFileResolver;


public interface OSGiManifest extends Manifest, RuleDef {

	static OSGiManifest instance(PathToFileResolver fileResolver) {
		return new OSGiManifestImpl(fileResolver);
	}

	OSGiManifest withClassesJarOrDir(File classesJarOrDir);

	OSGiManifest withClasspath(FileCollection classpath);

	OSGiManifest withBundleDependency(ModuleVersionIdentifier moduleId);
}
