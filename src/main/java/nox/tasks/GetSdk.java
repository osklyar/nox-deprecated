/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import nox.ext.Platform;
import nox.internal.system.Arch;
import nox.internal.system.OS;
import nox.internal.system.Win;
import org.apache.commons.io.FileUtils;
import org.gradle.api.AntBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;


public class GetSdk extends DefaultTask {

	public static final String name = GetSdk.class.getSimpleName().toLowerCase();

	private static final Logger logger = Logging.getLogger(GetSdk.class);

	private static final String baseUrl = "http://www.eclipse.org/downloads/download.php?file=/eclipse/downloads/drops4";

	private static final Map<String, String> prefixes = Maps.newTreeMap();

	static {
		prefixes.put("4.6", "R-4.6-201606061100");
		prefixes.put("4.6.2", "R-4.6.2-201611241400");
		prefixes.put("4.7M5", "S-4.7M5-201701261030");
	}

	@Optional
	@Input
	public String version = "4.6.2";

	private final Platform platform;

	@OutputDirectory
	public File getSdkDir() {
		return platform.getSdkDir();
	}

	public GetSdk() {
		setGroup("nox.Platform");
		setDescription("Downloads the Eclipse SDK as defined in the 'platform' extension.");
		platform = getProject().getExtensions().findByType(Platform.class);
	}

	@TaskAction
	public void action(IncrementalTaskInputs inputs) {
		if (inputs.isIncremental()) {
			return;
		}

		Preconditions.checkArgument(prefixes.containsKey(version), "Supported versions are: %s",
			prefixes.keySet());

		if (platform.getSdkDir().exists()) {
			try {
				FileUtils.deleteDirectory(platform.getSdkDir());
			} catch (IOException ex) {
				logger.warn("Failed to delete existing SDK directory, skipping the task", ex);
				return;
			}
		}

		File sdkArchive = new File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString() + (
			OS.is(OS.win32) ? ".zip" : ".tar.gz"));
		try {
			AntBuilder ant = getProject().getAnt();
			Map<String, Object> params;

			params = Maps.newHashMap();
			params.put("src", downloadUrl());
			params.put("dest", sdkArchive);
			params.put("verbose", logger.isDebugEnabled());
			logger.info("Downloading SDK {}", params);
			ant.invokeMethod("get", params);

			params = Maps.newHashMap();
			params.put("src", sdkArchive);
			params.put("dest", platform.getSdkDir().getParentFile());
			params.put("overwrite", true);
			if (!OS.is(OS.win32)) {
				params.put("compression", "gzip");
			}
			logger.info("Expanding SDK archive {}", params);
			ant.invokeMethod(OS.is(OS.win32) ? "unzip" : "untar", params);

			File dest = new File(platform.getSdkDir().getParentFile(), OS.is(OS.macosx) ? "Eclipse.app" : "eclipse");
			if (!Objects.equal(platform.getSdkDir(), dest)) {
				Preconditions.checkState(dest.renameTo(platform.getSdkDir()),
					"Failed to move SDK into the target directory");
			}

			logger.info("Setting executable flag to SDK eclipse {}", platform.getSdkExec());
			Preconditions.checkState(platform.getSdkExec().setExecutable(true),
				"Failed to set eclipse as executable");
		} finally {
			sdkArchive.delete();
		}
	}

	private String downloadUrl() {
		String archSuffix = Arch.is(Arch.x86_64) ? "-x86_64" : "";
		if (OS.is(OS.win32)) {
			return String.format("%s/%s/eclipse-SDK-%s-win32%s.zip&r=1", baseUrl, prefixes.get(version),
				version, archSuffix);
		}
		return String.format("%s/%s/eclipse-SDK-%s-%s-%s%s.tar.gz&r=1", baseUrl, prefixes.get(version),
			version, OS.current(), Win.current(), archSuffix);
	}
}
