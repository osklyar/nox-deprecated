/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import nox.tasks.Bundle;
import nox.tasks.Create;
import nox.tasks.GetSdk;
import nox.tasks.Ivynize;
import nox.tasks.Reset;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logging;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.tasks.TaskContainer;
import org.slf4j.Logger;


public class Platform implements Plugin<Project> {

	private static final Logger logger = Logging.getLogger(Platform.class);

	@Override
	public void apply(final Project project) {
		ExtensionContainer extensions = project.getExtensions();

		logger.info("Registering Platform ext for {}", project.getName());
		extensions.create(nox.ext.Platform.name, nox.ext.Platform.class);

		TaskContainer tasks = project.getTasks();
		tasks.create(Reset.name, Reset.class);
		tasks.create(GetSdk.name, GetSdk.class);
		Bundle bundle = tasks.create(Bundle.name, Bundle.class);
		Ivynize ivynize = tasks.create(Ivynize.name, Ivynize.class);
		ivynize.dependsOn(bundle);
		Create create = tasks.create(Create.name, Create.class);
		create.dependsOn(ivynize);
	}
}
