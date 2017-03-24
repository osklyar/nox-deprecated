/**
 * Created by skol on 24.03.17.
 */
package nox.internal.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nox.internal.system.Arch;
import nox.internal.system.OS;
import nox.internal.system.Win;

class AssemblerImpl implements Assembler {

	private static final Logger logger = LoggerFactory.getLogger(AssemblerImpl.class);

	@Override
	public void assemble(File eclipseExec, Target target, File outputDir) throws IOException {
		List<String> locations = Lists.newArrayList();
		List<String> bundles = Lists.newArrayList();
		for (Location location : target.locations.location) {
			locations.add(location.repository.location);
			bundles.addAll(Lists.transform(location.unit, unit -> String.format("%s/%s", unit.id, unit.version)));
		}

		// FIXME: replace with a command that takes the xml file as information gets lost here!

		String[] execWithEclipseSdk = new String[]{
			eclipseExec.getAbsolutePath(),
			"-application", "org.eclipse.equinox.p2.director",
			"-repository", StringUtils.join(locations, ","),
			"-installIU", StringUtils.join(bundles, ","),
			"-tag", "target-platform",
			"-destination", outputDir.getAbsolutePath(),
			"-profile", "SDKProfile",
			"-bundlepool", outputDir.getAbsolutePath(),
			"-p2.os", OS.current().toString(),
			"-p2.ws", Win.current().toString(),
			"-p2.arch", Arch.current().toString(),
			"-roaming",
			"-nosplash",
			"-consoleLog",
			"-vmargs", "-Declipse.p2.mirror=false"
		};

		Process p = withProcessLogs(Runtime.getRuntime().exec(execWithEclipseSdk));

		try {
			int exitCode = p.waitFor();
			if (exitCode != 0) {
				throw new GradleException(String.format("Non zero exit code: %d", exitCode));
			}
		} catch (InterruptedException ex) {
			throw new GradleException("Interrupted");
		}
	}

	@Override
	public void publishBundles(File eclipseExec, File bundleDir, File outputDir) throws IOException {

		String[] execWithEclipseSdk = new String[]{
			eclipseExec.getAbsolutePath(),
			"-application", "org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher",
			"-metadataRepository", "file://" + outputDir.getAbsolutePath(),
			"-artifactRepository", "file://" + outputDir.getAbsolutePath(),
			"-source", bundleDir.getAbsolutePath(),
			"-configs", "ANY",
			"-publishArtifacts",
			"-nosplash",
			"-consoleLog",
		};

		Process p = withProcessLogs(Runtime.getRuntime().exec(execWithEclipseSdk));

		try {
			int exitCode = p.waitFor();
			if (exitCode != 0) {
				throw new GradleException(String.format("Non zero exit code: %d", exitCode));
			}
		} catch (InterruptedException ex) {
			throw new GradleException("Interrupted");
		}

	}

	private Process withProcessLogs(Process p) {
		new Thread(() -> {
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			try {
				String line;
				while ((line = input.readLine()) != null) {
					logger.info(line);
				}
			} catch (IOException e) {
				logger.error("Error reading eclipse SDK input: {}", e);
			}
		}).start();
		return p;
	}

}
