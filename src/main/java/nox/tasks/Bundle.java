/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import nox.ext.Platform;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.File;


public class Bundle extends DefaultTask {

	public static final String name = Bundle.class.getSimpleName().toLowerCase();

	public static final String P2 = "p2";

	private final Platform platform;

	private File p2Root;

	public void setP2Root(File p2Root) {
		this.p2Root = p2Root;
	}

	@OutputDirectory
	public File getP2Root() {
		return p2Root;
	}

	public Bundle() {
		setGroup("nox.Platform");
		setDescription("Generates OSGi p2 repository with bundles converted from external and internal Jars.");

		platform = getProject().getExtensions().findByType(Platform.class);
		p2Root = new File(getProject().getBuildDir(), P2);
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
