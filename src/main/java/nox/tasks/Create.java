/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import nox.ext.Platform;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.File;


public class Create extends DefaultTask {

	public static final String name = Create.class.getSimpleName().toLowerCase();

	private final Platform platform;

	private File p2Root;

	public void setP2Root(File p2Root) {
		this.p2Root = p2Root;
	}

	@InputDirectory
	public File getP2Root() {
		return p2Root;
	}

	@OutputDirectory
	public File getTargetPlatformDir() {
		return platform.getTargetPlatformDir();
	}

	public Create() {
		setGroup("nox.Platform");
		setDescription("Creates Equinox target platform from local bundles and remote repositories.");

		platform = getProject().getExtensions().findByType(Platform.class);
		p2Root = new File(getProject().getBuildDir(), Bundle.P2);
	}

	@TaskAction
	public void action(IncrementalTaskInputs inputs) {
		if (!inputs.isIncremental()) {
			action();
			return;
		}
		inputs.outOfDate(details -> action());
	}

	private void action() {

	}
}
