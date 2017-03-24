/**
 * Created by skol on 17.03.17.
 */
package nox.internal.bundle;

import org.gradle.api.file.FileCollection;
import org.gradle.api.java.archives.Manifest;
import org.gradle.internal.file.PathToFileResolver;

import java.io.File;

public interface OSGiManifest extends Manifest, ModuleDef {

	static OSGiManifest instance(PathToFileResolver fileResolver) {
		return new OSGiManifestImpl(fileResolver);
	}

	OSGiManifest withClassesJarOrDir(File classesJarOrDir);

	OSGiManifest withClasspath(FileCollection classpath);
}
