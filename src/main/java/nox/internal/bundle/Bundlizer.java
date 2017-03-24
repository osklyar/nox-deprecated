/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import java.io.File;
import java.io.IOException;
import java.util.jar.Manifest;


public interface Bundlizer {

	static Bundlizer instance(File targetDir) {
		return new BundlizerImpl(targetDir);
	}

	File bundleJar(File originalJar, Manifest manifest) throws IOException;

	File bundleJar(File originalJar, Manifest manifest, String classifier) throws IOException;
}
