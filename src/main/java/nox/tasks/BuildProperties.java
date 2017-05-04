/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.tasks;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BuildProperties extends DefaultTask {

	public static final String name = BuildProperties.class.getSimpleName().toLowerCase();

	private final List<String> sources = Lists.newArrayList();
	private final List<String> output = Lists.newArrayList();
	private final Set<String> binincludes = Sets.newLinkedHashSet();
	private final Map<String, String> instructions = Maps.newLinkedHashMap();

	public void source(String... sources) {
		this.sources.addAll(Arrays.asList(sources));
	}

	public List<String> binincludes() {
		return Collections.unmodifiableList(Lists.newArrayList(binincludes));
	}

	public void binincludes(String... binincludes) {
		this.binincludes.addAll(Arrays.asList(binincludes));
	}

	public void output(String... output) {
		this.output.addAll(Arrays.asList(output));
	}

	public void instruction(String key, String value) {
		this.instructions.put(key, value);
	}

	public BuildProperties() {
		binincludes.add("META-INF/");
	}

	@TaskAction
	public void action() {
		List<String> lines = Lists.newArrayList();
		List<String> javaSources = getSources(sources, ss -> ss.getByName("main").getJava());
		if (!javaSources.isEmpty()) {
			lines.add("source.. = " + StringUtils.join(javaSources, ","));
		}
		Collection<String> resources = Sets.newLinkedHashSet(binincludes);
		resources.addAll(getSources(Lists.newArrayList(), ss -> ss.getByName("main").getResources()));
		resources = Collections2.filter(resources, path -> {
			if ("META-INF/".equals(path)) {
				return true;
			}
			return new File(getProject().getProjectDir(), path).exists();
		});
		if (!resources.isEmpty()) {
			lines.add("bin.includes = " + StringUtils.join(resources, ","));
		}
		if (!javaSources.isEmpty() || !resources.isEmpty()) {
			lines.add("output.. = " + (output.isEmpty() ? "bin/" : StringUtils.join(output, ",")));
		}
		for (Map.Entry<String, String> entry: instructions.entrySet()) {
			lines.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}

		try {
			Files.write(Paths.get(getProject().getProjectDir().getAbsolutePath(), "build.properties"), lines, Charset.forName("UTF-8"));
		} catch (IOException ex) {
			throw new GradleException("Failed to create build.properties for an OSGi bundle", ex);
		}
	}

	interface SourceExtractor {
		SourceDirectorySet extract(SourceSetContainer sourceSets);
	}

	private List<String> getSources(Collection<String> preconfigured,  SourceExtractor sourceExtractor) {
		List<String> src = Lists.newArrayList(preconfigured);
		if (src.isEmpty()) {
			File projectDir = getProject().getProjectDir();
			SourceSetContainer sourceSets = getProject().getConvention()
				.getPlugin(JavaPluginConvention.class)
				.getSourceSets();
			for (File sourceEntry: sourceExtractor.extract(sourceSets).getSrcDirs()) {
				if (sourceEntry.exists()) {
					String element = sourceEntry.getAbsolutePath().replace(projectDir.getAbsolutePath() + "/", "");
					if (!element.endsWith("/")) {
						element += "/";
					}
					src.add(element);
				}
			}
		}
		return src;
	}

	public void clean() {
		new File(getProject().getProjectDir(), "build.properties").delete();
	}
}
