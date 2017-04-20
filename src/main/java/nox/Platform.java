/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;

import nox.ext.Bundles;
import nox.tasks.Bundle;
import nox.tasks.CleanBundles;
import nox.tasks.CleanIvy;
import nox.tasks.CleanPlatform;
import nox.tasks.Create;
import nox.tasks.GetSdk;
import nox.tasks.Ivynize;


public class Platform implements Plugin<Project> {

	@Override
	public void apply(final Project project) {
		ExtensionContainer extensions = project.getExtensions();

		nox.ext.Platform platform = nox.ext.Platform.instance(project);
		extensions.add(nox.ext.Platform.name, platform);
		extensions.add(Bundles.name, Bundles.instance());

		TaskContainer tasks = project.getTasks();

		GetSdk getSdk = tasks.create(GetSdk.name, GetSdk.class);

		Bundle bundle = tasks.create(Bundle.name, Bundle.class);
		Create create = tasks.create(Create.name, Create.class);
		Ivynize ivynize = tasks.create(Ivynize.name, Ivynize.class);

		create.dependsOn(bundle);
		ivynize.dependsOn(create);

		CleanBundles cleanBundles = tasks.create(CleanBundles.name, CleanBundles.class);
		CleanIvy cleanIvy = tasks.create(CleanIvy.name, CleanIvy.class);
		CleanPlatform cleanPlatform = tasks.create(CleanPlatform.name, CleanPlatform.class);

		cleanIvy.dependsOn(cleanBundles);
		cleanPlatform.dependsOn(cleanIvy);

		bundle.mustRunAfter(cleanBundles, cleanIvy, cleanPlatform, getSdk);

		project.afterEvaluate(p -> {


			Bundles bundles = p.getExtensions().findByType(Bundles.class);
			try {
				String bundlesJson = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writeValueAsString(bundles);
				FileUtils.write(new File(platform.getPlatformBuildDir(), Bundles.bundlesConfigFile), bundlesJson, Charset.forName("UTF-8"));
			} catch (IOException ex) {
				throw new GradleException("Cannot store " + Bundles.bundlesConfigFile + " in the build dir", ex);
			}

			Create createTask = (Create) p.getTasksByName("create", false).iterator().next();
			try {
				String createJson = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true).writeValueAsString(createTask.getLocations());
				FileUtils.write(new File(platform.getPlatformBuildDir(), Create.createConfigFile), createJson, Charset.forName("UTF-8"));
			} catch (IOException ex) {
				throw new GradleException("Cannot store " + Create.createConfigFile + " in the build dir", ex);
			}
		});
	}
}
