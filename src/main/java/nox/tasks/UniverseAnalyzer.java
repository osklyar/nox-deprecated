/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;

import nox.internal.gradlize.Bundle;
import nox.internal.gradlize.BundleUniverse;
import nox.internal.gradlize.Dependency;
import nox.internal.gradlize.DependencyResolver;


class UniverseAnalyzer {

	interface Action {
		default void action(Bundle bundle, Collection<Dependency> deps) throws IOException {
		}
	}

	private final File pluginsDir;

	private final Action action;

	UniverseAnalyzer(File pluginsDir) {
		this.pluginsDir = pluginsDir;
		this.action = new Action() {};
	}

	UniverseAnalyzer(File pluginsDir, Action action) {
		this.pluginsDir = pluginsDir;
		this.action = action;
	}

	void analyze(BundleUniverse universe) {
		try {

			Collection<Bundle> bundles = loadBundles(pluginsDir);
			for (Bundle bundle : bundles) {
				universe.with(bundle);
			}
			DependencyResolver resolver = DependencyResolver.instance(universe);
			for (Bundle bundle : bundles) {
				Collection<Dependency> deps = resolver.resolveFor(bundle);
				action.action(bundle, deps);
			}
		} catch (IOException ex) {
			throw new GradleException("Failed to analyze dependency tree", ex);
		}
	}

	private Collection<Bundle> loadBundles(File pluginsDir) throws IOException {
		File[] files = pluginsDir.listFiles(file -> !file.getName().contains(".source_"));
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
