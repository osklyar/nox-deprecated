/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.google.common.collect.Maps;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;

import nox.internal.bundle.OSGiManifest;


public class OSGi implements Plugin<Project> {

	/**
	 * Add osgi-unpackManifest=false to gradle.properties to prevent copying
	 */
	private static final String UNPACK_OSGI_MANIFEST = "osgi-unpackManifest";

	/**
	 * Add osgi-requireBundles=true to gradle.properties to require bundles
	 */
	private static final String REQUIRE_BUNDLES = "osgi-requireBundles";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaBasePlugin.class);

		ProjectInternal internalProject = (ProjectInternal) project;
		FileResolver fileResolver = internalProject.getFileResolver();
		String archivesBaseName = project.getConvention()
			.getPlugin(BasePluginConvention.class)
			.getArchivesBaseName();
		File classesDir = project.getConvention()
			.getPlugin(JavaPluginConvention.class)
			.getSourceSets()
			.getByName("main")
			.getOutput()
			.getClassesDir();
		Configuration classpath = project.getConfigurations().getByName("runtime");

		OSGiManifest manifest = OSGiManifest.instance(fileResolver)
			.withClassesJarOrDir(classesDir)
			.withClasspath(classpath);
		manifest.groupId(project.getGroup().toString());
		manifest.artifactId(archivesBaseName);
		manifest.version(project.getVersion().toString());

		Jar jarTask = (Jar) project.getTasks().getByName("jar");
		jarTask.setManifest(manifest);

		registerUnpackAction(project);

		project.afterEvaluate(p -> registerJarManifestAction(p, manifest));
	}

	private void registerJarManifestAction(Project project, OSGiManifest manifest) {
		ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties();
		if (ext.has(REQUIRE_BUNDLES) && Boolean.valueOf(
			String.valueOf(ext.get(REQUIRE_BUNDLES))).booleanValue()) {
			for (ResolvedArtifact artifact : project.getConfigurations()
				.getByName("runtime")
				.getResolvedConfiguration()
				.getResolvedArtifacts()) {
				ModuleVersionIdentifier resolvedId = artifact.getModuleVersion().getId();
				manifest.withBundleDependency(
					new DefaultModuleVersionIdentifier(resolvedId.getName(), resolvedId.getName(), resolvedId.getVersion()));
			}
		}
	}

	private void registerUnpackAction(Project project) {
		ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties();
		if (!ext.has(UNPACK_OSGI_MANIFEST) || Boolean.valueOf(
			String.valueOf(ext.get(UNPACK_OSGI_MANIFEST))).booleanValue()) {
			File projectDir = project.getProjectDir().getAbsoluteFile();
			Jar jarTask = (Jar) project.getTasks().getByName("jar");
			Task buildTask = project.getTasks().getByName("build");
			buildTask.doLast(task -> unpackOSGiManifest(projectDir, jarTask));
		}
	}

	private void unpackOSGiManifest(File projectDir, Jar jarTask) {
		// ignore failure here, will throw below
		new File(projectDir, "META-INF").mkdirs();
		URI jarUri = URI.create("jar:" + jarTask.getArchivePath().toURI());
		try (FileSystem jarfs = FileSystems.newFileSystem(
      jarUri, Maps.newHashMap())) {
      Path source = jarfs.getPath("META-INF", "MANIFEST.MF");
      Path target = Paths.get(projectDir.getAbsolutePath(), "META-INF",
        "MANIFEST.MF");
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new GradleException("Failed to copy MANIFEST.MF out of the jar");
    }
	}
}
