/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import nox.ext.Platform;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;


public class CleanIvy extends DefaultTask {

	public static final String name = CleanIvy.class.getSimpleName().toLowerCase();

	private final Platform platform;

	public CleanIvy() {
		setGroup("nox.Platform");
		setDescription("Cleans the Ivy metadata [implies cleanbundles].");

		platform = getProject().getExtensions().findByType(Platform.class);
	}

	@TaskAction
	public void action() {
		try {
			FileUtils.deleteDirectory(new File(platform.getTargetPlatformDir(), Platform.IVY_SUBDIR));

		} catch (IOException ex) {
			throw new GradleException("Failed to clean platform artifacts.", ex);
		}
	}
}
