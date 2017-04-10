/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nox.internal.system.OS;


class PlatformImpl implements Platform {

	private static final String BUILD_P2 = "p2";

	private static final Logger logger = LoggerFactory.getLogger(PlatformImpl.class);

	private static volatile File root = null;

	private static volatile File targetPlatformDir = null;

	private static volatile File sdkDir = null;

	private static volatile Map<String, String> bundleMapping = Maps.newConcurrentMap();

	private final Project project;

	private File p2Dir = null;

	PlatformImpl(Project project) {
		this.project = project;
	}

	// do we really want to synchronize getters/setters to these statics? no

	@Override
	public File getRoot() {
		return root;
	}

	@Override
	public void setRoot(File value) {
		if (root != null && !Objects.equal(root, value)) {
			logger.warn("Platform 'root' is already set to a different value");
		}
		root = value;
	}

	@Override
	public File getTargetPlatformDir() {
		if (targetPlatformDir != null) {
			return targetPlatformDir;
		}
		File root = getRoot();
		Preconditions.checkNotNull(root, "Platform root undefined");
		return new File(root, "platform");
	}

	@Override
	public void setTargetPlatformDir(File value) {
		if (targetPlatformDir != null && !Objects.equal(targetPlatformDir, value)) {
			logger.warn("Platform 'targetPlatformDir' is already set to a different value");
		}
		targetPlatformDir = value;
	}

	@Override
	public File getSdkDir() {
		if (sdkDir != null) {
			return sdkDir;
		}
		File root = getRoot();
		Preconditions.checkNotNull(root, "Platform root undefined");
		if (OS.is(OS.macosx)) {
			return new File(root, "Eclipse.app");
		}
		return new File(root, "eclipse");
	}

	@Override
	public void setSdkDir(File value) {
		if (sdkDir != null && !Objects.equal(sdkDir, value)) {
			logger.warn("Plaform 'sdkDir' is already set to a different value");
		}
		sdkDir = value;
	}

	@Override
	public File getSdkExec() {
		switch (OS.current()) {
			case win32:
				return new File(getSdkDir(), "eclipse.exe");
			case macosx:
				return new File(getSdkDir(), "Contents/MacOS/eclipse");
		}
		return new File(getSdkDir(), "eclipse");
	}

	@Override
	public void setP2Dir(File p2Dir) {
		this.p2Dir = p2Dir;
	}

	@Override
	public File getP2Dir() {
		if (p2Dir != null) {
			return p2Dir;
		}
		return new File(project.getBuildDir(), BUILD_P2);
	}

	@Override
	public void setBundleMappingFile(File mappingFile) {
		try (FileInputStream is = new FileInputStream(mappingFile)) {
			Properties props = new Properties();
			props.load(is);
			bundleMapping.clear();
			Enumeration<?> it = props.propertyNames();
			while (it.hasMoreElements()) {
				String fromName = String.valueOf(it.nextElement());
				String toName = props.getProperty(fromName);
				bundleMapping.put(fromName, toName);
			}
		} catch (IOException ex) {
			throw new GradleException("Failed to load bundle mappings", ex);
		}
	}

	@Override
	public Map<String, String> bundleMapping() {
		return Collections.unmodifiableMap(bundleMapping);
	}

	@Override
	public int execEclipseApp(String application, String... args) throws IOException {
		List<String> cmd = Lists.newArrayList(
			getSdkExec().getAbsolutePath(),
			"-application",
			application);
		cmd.addAll(Arrays.asList(args));
		cmd.add("-roaming");
		cmd.add("-nosplash");
		cmd.add("-consoleLog");

		try {
			return withProcessLogs(cmd).waitFor();
		} catch (InterruptedException ex) {
			throw new GradleException("Interrupted");
		}
	}

	private Process withProcessLogs(List<String> cmd) throws IOException {
		Process p = Runtime.getRuntime().exec(cmd.toArray(new String[] {}));
		new Thread(() -> {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				for (String line = input.readLine(); line != null; line = input.readLine()) {
					logger.info(line);
				}
			} catch (IOException e) {
				logger.error("Error reading eclipse SDK output: {}", e);
			}
		}).start();
		new Thread(() -> {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
				for (String line = input.readLine(); line != null; line = input.readLine()) {
					logger.warn(line);
				}
			} catch (IOException e) {
				logger.error("Error reading eclipse SDK error output: {}", e);
			}
		}).start();
		return p;
	}

}
