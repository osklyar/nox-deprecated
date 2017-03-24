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


public class Reset extends DefaultTask {

	public static final String name = Reset.class.getSimpleName().toLowerCase();

	private final Platform platform;

	public Reset() {
		setGroup("nox.Platform");
		setDescription("Resets the target platform deleting the target directories.");
		platform = getProject().getExtensions().findByType(Platform.class);
	}

	@TaskAction
	public void action() {
		try {
			FileUtils.deleteDirectory(platform.getTargetPlatformDir());
		} catch (IOException ex) {
			throw new GradleException("Failed to delete the generated target platform", ex);
		}
	}
}
