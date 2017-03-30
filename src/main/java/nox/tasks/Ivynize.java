/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import nox.ext.Platform;
import nox.internal.gradlize.Bundle;
import nox.internal.gradlize.BundleUniverse;
import nox.internal.gradlize.Dependency;
import nox.internal.gradlize.Duplicates;
import nox.internal.gradlize.MetadataExporter;


public class Ivynize extends DefaultTask {

	public static final String name = Ivynize.class.getSimpleName().toLowerCase();

	private final Platform platform;

	private final BundleUniverse universe;

	@InputDirectory
	public File getPluginsDir() {
		return new File(platform.getTargetPlatformDir(), Platform.PLUGINS_SUBDIR);
	}

	@OutputDirectory
	public File getIvyDir() {
		return new File(platform.getTargetPlatformDir(), Platform.IVY_SUBDIR);
	}

	public Ivynize() {
		setGroup("nox.Platform");
		setDescription("Resolves target platform bundle dependencies into an Ivy repo, " +
			"the repo is auto-added with nox.Java [implies bundle, create].");

		platform = getProject().getExtensions().findByType(Platform.class);
		universe = BundleUniverse.instance(Duplicates.Overwrite);

		getProject().afterEvaluate(project -> getPluginsDir().mkdirs());
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
		try {
			FileUtils.deleteDirectory(getIvyDir());
			new UniverseAnalyzer(getPluginsDir(), new UniverseAnalyzer.Action() {
				@Override
				public void action(Bundle bundle, Collection<Dependency> deps) throws IOException {
					MetadataExporter exporter = MetadataExporter.instance(bundle, Platform.GROUP_NAME, deps);
					exporter.exportTo(getIvyDir());
				}
			}).analyze(universe);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
}
