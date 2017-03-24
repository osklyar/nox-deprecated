/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import com.google.common.collect.Lists;
import groovy.lang.Closure;
import nox.ext.Platform;
import nox.internal.platform.Assembler;
import nox.internal.platform.Location;
import nox.internal.platform.Repository;
import nox.internal.platform.Target;
import nox.internal.platform.Unit;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class Create extends DefaultTask {

	public static final String name = Create.class.getSimpleName().toLowerCase();

	private final Platform platform;

	private final List<Location> locations = Lists.newArrayList();

	@InputDirectory
	public File getP2Dir() {
		return platform.getP2Dir();
	}

	@OutputDirectory
	public File getTargetPlatformDir() {
		return platform.getTargetPlatformDir();
	}

	public String pdeVersion = "3.8";

	public String vmArgs = "-XX:MaxPermSize=128M";

	/* open for testing */
	Assembler assembler = Assembler.instance();


	public Create() {
		setGroup("nox.Platform");
		setDescription("Creates target platform from local and remote bundles [implies bundle].");

		platform = getProject().getExtensions().findByType(Platform.class);
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
		Target target = new Target("foo")
			.withPdeVersion(pdeVersion)
			.withLocations(locations);
		File[] files = new File(getP2Dir(), Platform.PLUGINS_SUBDIR).listFiles();
		if (files != null) {
			Location location = new Location(new Repository("file://" + getP2Dir()));
			target.withLocation(location);
			for (File file : files) {
				if (file.getName().endsWith("sources.jar")) {
					continue;
				}
				String[] parts = file.getName().replace(".jar", "").split("_");
				if (parts.length == 2) {
					location.unit.add(new Unit(parts[0], parts[1]));
				}
			}
		}
		try {
			FileUtils.deleteDirectory(getTargetPlatformDir());
			getTargetPlatformDir().mkdirs();

			assembler.assemble(platform.getSdkExec(), target, getTargetPlatformDir());
		} catch (IOException ex) {
			throw new GradleException("Failed to assemble target platform", ex);
		}
	}

	public static class LocationUnit {
		List<Unit> units = Lists.newArrayList();

		public void unit(String id, String version) {
			units.add(new Unit(id, version));
		}
		public void unit(String jarFile) {
			String[] parts = jarFile.replace(".jar", "").split("_");
			if (parts.length == 2) {
				units.add(new Unit(parts[0], parts[1]));
			} else {
				throw new GradleException(String.format("Incorrectly formatted dependency: %s", jarFile));
			}
		}
	}

	public void location(String path, Closure closure) {
		LocationUnit locationUnit = new LocationUnit();
		ConfigureUtil.configure(closure, locationUnit);

		Location location = new Location(new Repository(path));
		location.unit.addAll(locationUnit.units);
		locations.add(location);
	}

}
