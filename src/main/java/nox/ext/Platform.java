/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.ext;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import nox.internal.system.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class Platform {

	public static final String name = Platform.class.getSimpleName().toLowerCase();

	public static final String PLUGINS_DIR = "plugins";

	public static final String IVY_DIR = "ivy-metadata";

	public static final String GROUP_NAME = "bundles";

	private static final Logger logger = LoggerFactory.getLogger(Platform.class);


	private static volatile File root = null;

	private static volatile File targetPlatformDir = null;

	private static volatile File sdkDir = null;

	// do we really want to synchronize getters/setters to these statics? no

	public File getRoot() {
		return root;
	}

	public void setRoot(File value) {
		if (root != null && !Objects.equal(root, value)) {
			logger.warn("Platform 'root' is already set to a different value");
		}
		root = value;
	}

	public File getTargetPlatformDir() {
		if (targetPlatformDir != null) {
			return targetPlatformDir;
		}
		File root = getRoot();
		Preconditions.checkNotNull(root, "Platform root undefined");
		return new File(root, "platform");
	}

	public void setTargetPlatformDir(File value) {
		if (targetPlatformDir != null && !Objects.equal(targetPlatformDir, value)) {
			logger.warn("Platform 'targetPlatformDir' is already set to a different value");
		}
		targetPlatformDir = value;
	}

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

	public void setSdkDir(File value) {
		if (sdkDir != null && !Objects.equal(sdkDir, value)) {
			logger.warn("Plaform 'sdkDir' is already set to a different value");
		}
		sdkDir = value;
	}

	public File getSdkExec() {
		switch (OS.current()) {
			case win32:
				return new File(getSdkDir(), "eclipse.exe");
			case macosx:
				return new File(getSdkDir(), "Contents/MacOS/eclipse");
		}
		return new File(getSdkDir(), "eclipse");
	}
}
