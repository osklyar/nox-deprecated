/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.ext;

import java.io.File;

import org.gradle.api.Project;


public interface Platform {

	static Platform instance(Project project) {
		return new PlatformImpl(project);
	}

	String name = Platform.class.getSimpleName().toLowerCase();

	String PLUGINS_SUBDIR = "plugins";

	String IVY_SUBDIR = "ivy-metadata";

	String GROUP_NAME = "bundle";

	File getRoot();

	void setRoot(File value);

	File getTargetPlatformDir();

	void setTargetPlatformDir(File value);

	File getSdkDir();

	void setSdkDir(File value);

	File getSdkExec();

	void setP2Dir(File p2Dir);

	File getP2Dir();
}
