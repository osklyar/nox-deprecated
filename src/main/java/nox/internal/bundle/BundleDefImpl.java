/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.bundle;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import groovy.lang.Closure;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;


class BundleDefImpl extends AbstractDefImpl implements BundleDef {

	private final List<RuleDef> ruleDefs = Lists.newArrayList();

	private File jarFile = null;
	private File sourceJarFile = null;
	private boolean replaceOSGiManifest = false;

	BundleDefImpl(String depString, Closure... closures) {
		configure(closures);
		String[] parts = depString.split(":");
		Preconditions.checkArgument(parts.length == 3, "Incorrect dependency string, expected groupId:artifactId:version");
		groupId = parts[0];
		artifactId = parts[1];
		version = parts[2];
	}

	BundleDefImpl(String groupId, String artifactId, String version, Closure... closures) {
		configure(closures);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	BundleDefImpl(Closure... closures) {
		configure(closures);
		Preconditions.checkNotNull(groupId, "groupId is required for a bundle definition");
		Preconditions.checkNotNull(artifactId, "artifactId is required for a bundle definition");
		Preconditions.checkNotNull(version, "version is required for a bundle definition");
	}

	@Override
	public RuleDef rule(String depString, Closure... closures) {
		RuleDef def = RuleDef.instance(depString, closures);
		ruleDefs.add(def);
		return def;
	}

	@Override
	public RuleDef rule(String groupId, String artifactId, String version, Closure... closures) {
		RuleDef def = RuleDef.instance(groupId, artifactId, version, closures);
		ruleDefs.add(def);
		return def;
	}

	@Override
	public RuleDef rule(String groupId, String artifactId, Closure... closures) {
		RuleDef def = RuleDef.instance(groupId, artifactId, closures);
		ruleDefs.add(def);
		return def;
	}

	@Override
	public RuleDef rule(Closure... closures) {
		RuleDef def = RuleDef.instance(closures);
		ruleDefs.add(def);
		return def;
	}

	@Override
	public void jarFile(File jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	public File getJarFile() {
		return jarFile;
	}

	@Override
	public void sourceJarFile(File jarFile) {
		this.sourceJarFile = jarFile;
	}

	@Override
	public File getSourceJarFile() {
		return sourceJarFile;
	}

	@Override
	public String toDependencyString() {
		return String.format("%s:%s:%s", groupId, artifactId, version);
	}

	public List<RuleDef> getRuleDefs() {
		return Collections.unmodifiableList(ruleDefs);
	}

	private void configure(Closure[] closures) {
		if (closures != null) {
			for (Closure closure: closures) {
				ConfigureUtil.configure(closure, this);
			}
		}
	}

	@Override
	public void replaceOSGiManifest(boolean flag) {
		this.replaceOSGiManifest = flag;
	}

	@Override
	public boolean getReplaceOSGiManifest() {
		return replaceOSGiManifest;
	}
}
