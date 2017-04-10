/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import com.google.common.base.Preconditions;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ClientModule;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule;
import org.gradle.api.internal.artifacts.repositories.layout.DefaultIvyPatternRepositoryLayout;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPlugin;

import groovy.lang.Closure;
import nox.ext.Platform;
import nox.internal.system.Arch;
import nox.internal.system.OS;
import nox.internal.system.Win;


public class Java implements Plugin<Project> {

	private static final String METHOD_NAME = "bundle";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaPlugin.class);

		ExtensionContainer extensions = project.getExtensions();
		RepositoryHandler repositories = project.getRepositories();

		extensions.add(Platform.name, Platform.instance(project));

		ExtraPropertiesExtension extProps = extensions.getExtraProperties();
		extProps.set(METHOD_NAME, new PluginDep(project));
		extProps.set("p2os", OS.current().toString());
		extProps.set("p2ws", Win.current().toString());
		extProps.set("p2arch", Arch.current().toString());

		Platform platform = project.getExtensions().findByType(Platform.class);
		repositories.ivy(repo -> {
			repo.setName("plugins");
			repo.setUrl(platform.getTargetPlatformDir());
			repo.layout("pattern", layout -> {
				DefaultIvyPatternRepositoryLayout ivyLayout = (DefaultIvyPatternRepositoryLayout) layout;
				ivyLayout.artifact(String.format("%s/[module](.[classifier])_[revision].[ext]", Platform.PLUGINS_SUBDIR));
				ivyLayout.artifact(String.format("%s/[module](.[classifier])_[revision]", Platform.PLUGINS_SUBDIR));
				ivyLayout.ivy(String.format("%s/[module](.[classifier])_[revision].[ext]", Platform.IVY_SUBDIR));
			});
		});
	}

	private static class PluginDep extends Closure<ClientModule> {

		public PluginDep(Project project) {
			super(project);
		}

		@Override
		public ClientModule call(Object... args) {
			Preconditions.checkArgument(args.length == 2, "Expected module name and version");
			String name = String.valueOf(args[0]);
			String version = String.valueOf(args[1]);
			return new DefaultClientModule(Platform.GROUP_NAME, name, version);
		}
	}
}
