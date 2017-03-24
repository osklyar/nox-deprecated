/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import nox.ext.Platform;
import nox.internal.dep.Bundle;
import nox.internal.dep.BundleUniverse;
import nox.internal.dep.Dependency;
import nox.internal.dep.DependencyResolver;
import nox.internal.dep.Duplicates;
import nox.internal.dep.MetadataExporter;
import nox.internal.dep.impl.DefaultBundleUniverse;
import nox.internal.dep.impl.DefaultDependencyResolver;
import nox.internal.dep.impl.IvyMetadataExporter;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


public class Ivynize extends DefaultTask {

	public static final String name = Ivynize.class.getSimpleName().toLowerCase();

	private final Platform platform;

	private final BundleUniverse universe;

	@InputDirectory
	public File getPluginsDir() {
		return new File(platform.getTargetPlatformDir(), Platform.PLUGINS_DIR);
	}

	@OutputDirectory
	public File getIvyDir() {
		return new File(platform.getTargetPlatformDir(), Platform.IVY_DIR);
	}

	public Ivynize() {
		setGroup("nox.Platform");
		setDescription("Resolves OSGi target platform bundle dependencies and wraps the former into an Ivy repo " +
			"(the repo is auto-added with nox.Java).");

		platform = getProject().getExtensions().findByType(Platform.class);
		universe = new DefaultBundleUniverse(Duplicates.Overwrite);

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

			Collection<Bundle> bundles = loadBundles();
			for (Bundle bundle : bundles) {
				universe.with(bundle);
			}
			DependencyResolver resolver = new DefaultDependencyResolver(universe);
			for (Bundle bundle : bundles) {
				Collection<Dependency> deps = resolver.resolveFor(bundle);
				MetadataExporter exporter = new IvyMetadataExporter(bundle, Platform.GROUP_NAME, deps);
				exporter.exportTo(getIvyDir());
			}
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private Collection<Bundle> loadBundles() throws IOException {
		File[] files = getPluginsDir().listFiles(file -> !file.getName().contains(".source_"));
		Preconditions.checkNotNull(files, "No permissions to list target plugins directory");
		List<Bundle> bundles = Lists.newArrayList();
		for (File file : files) {
			Manifest manifest;
			if (file.isDirectory()) {
				try (FileInputStream is = FileUtils.openInputStream(new File(file, "META-INF/MANIFEST.MF"))) {
					manifest = new Manifest(is);
				}
			} else {
				manifest = new JarFile(file).getManifest();
			}
			Bundle bundle = Bundle.parse(manifest);
			bundles.add(bundle);
		}
		return bundles;
	}
}
