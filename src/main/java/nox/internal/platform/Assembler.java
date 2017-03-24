/**
 * Created by skol on 24.03.17.
 */
package nox.internal.platform;

import java.io.File;
import java.io.IOException;

public interface Assembler {

	static Assembler instance() {
		return new AssemblerImpl();
	}

	void assemble(File eclipseExec, Target target, File outputDir) throws IOException;

	void publishBundles(File sdkExec, File bundleDir, File outputDir) throws IOException;
}
