/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import groovy.lang.Closure;
import nox.internal.osgi.DefaultOsgiManifest;
import nox.internal.osgi.OsgiManifest;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.BasePluginConvention;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.internal.Actions;
import org.gradle.internal.reflect.Instantiator;

import static org.gradle.util.ConfigureUtil.configure;


public class OSGi implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaBasePlugin.class);

		final OsgiPluginConvention osgiConvention = new OsgiPluginConvention((ProjectInternal) project);
		project.getConvention().getPlugins().put("osgi", osgiConvention);

		project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
			JavaPluginConvention javaConvention = project.getConvention().getPlugin(JavaPluginConvention.class);

			OsgiManifest osgiManifest = createDefaultOsgiManifest((ProjectInternal) project);
			osgiManifest.setClassesDir(javaConvention.getSourceSets().getByName("main").getOutput().getClassesDir());
			osgiManifest.setClasspath(project.getConfigurations().getByName("runtime"));
			Jar jarTask = (Jar) project.getTasks().getByName("jar");
			jarTask.setManifest(osgiManifest);
		});
	}

	private static OsgiManifest createDefaultOsgiManifest(ProjectInternal project) {
		final String groupId = project.getGroup().toString();
		final String artifactId = project.getConvention().getPlugin(BasePluginConvention.class).getArchivesBaseName();
		final String version = project.getVersion().toString();

		return project.getServices().get(Instantiator.class)
			.newInstance(DefaultOsgiManifest.class, groupId, artifactId, version, project.getFileResolver());
	}

	/**
	 * Is mixed into the project when applying the {@link OSGi} plugin.
	 */
	public static class OsgiPluginConvention {
		private final ProjectInternal project;

		public OsgiPluginConvention(ProjectInternal project) {
			this.project = project;
		}

		/**
		 * Creates a new instance of {@link OsgiManifest}. The returned object is preconfigured with:
		 * <pre>
		 * version: project.version
		 * name: project.archivesBaseName
		 * symbolicName: project.group + "." + project.archivesBaseName (see below for exceptions to this rule)
		 * </pre>
		 * <p>
		 * The symbolic name is usually the group + "." + archivesBaseName, with the following exceptions
		 * <ul>
		 * <li>if group has only one section (no dots) and archivesBaseName is not null then the
		 * first package name with classes is returned. eg. commons-logging:commons-logging ->
		 * org.apache.commons.logging</li>
		 * <li>if archivesBaseName is equal to last section of group then group is returned. eg.
		 * org.gradle:gradle -> org.gradle</li>
		 * <li>if archivesBaseName starts with last section of group that portion is removed. eg.
		 * org.gradle:gradle-core -> org.gradle.core</li>
		 * </ul>
		 */
		public OsgiManifest osgiManifest() {
			return osgiManifest(Actions.doNothing());
		}

		/**
		 * Creates and configures a new instance of an  {@link OsgiManifest} . The closure configures
		 * the new manifest instance before it is returned.
		 */
		public OsgiManifest osgiManifest(Closure closure) {
			return configure(closure, createDefaultOsgiManifest(project));
		}

		/**
		 * Creates and configures a new instance of an  {@link OsgiManifest}. The action configures
		 * the new manifest instance before it is returned.
		 *
		 * @since 3.5
		 */
		public OsgiManifest osgiManifest(Action<? super OsgiManifest> action) {
			OsgiManifest manifest = createDefaultOsgiManifest(project);
			action.execute(manifest);
			return manifest;
		}
	}
}
