/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import nox.ext.Platform;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;


public class CleanPlatform extends DefaultTask {

	public static final String name = CleanPlatform.class.getSimpleName().toLowerCase();

	private final Platform platform;

	public CleanPlatform() {
		setGroup("nox.Platform");
		setDescription("Cleans the target platform [implies cleanbundles, cleanivy.");

		platform = getProject().getExtensions().findByType(Platform.class);
	}

	@TaskAction
	public void action() {
		try {
			FileUtils.deleteDirectory(platform.getTargetPlatformDir());

		} catch (IOException ex) {
			throw new GradleException("Failed to clean platform artifacts.", ex);
		}
	}
}
