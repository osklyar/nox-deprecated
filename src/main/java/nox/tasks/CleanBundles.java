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


public class CleanBundles extends DefaultTask {

	public static final String name = CleanBundles.class.getSimpleName().toLowerCase();

	private final Platform platform;

	public CleanBundles() {
		setGroup("nox.Platform");
		setDescription("Cleans generated bundles and the P2 repository.");

		platform = getProject().getExtensions().findByType(Platform.class);
	}

	@TaskAction
	public void action() {
		try {
			FileUtils.deleteDirectory(platform.getP2Dir());
			FileUtils.deleteDirectory(getProject().getBuildDir());

		} catch (IOException ex) {
			throw new GradleException("Failed to clean platform artifacts.", ex);
		}
	}
}
