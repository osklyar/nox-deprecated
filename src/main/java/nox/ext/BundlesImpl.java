/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.ext;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import groovy.lang.Closure;
import nox.internal.bundle.BundleDef;
import nox.internal.bundle.RuleDef;


class BundlesImpl implements Bundles {

	private final List<BundleDef> bundleDefs = Lists.newArrayList();

	private final List<RuleDef> ruleDefs = Lists.newArrayList();
	private boolean withSources = true;

	@Override
	public BundleDef bundle(String depString, Closure... closures) {
		BundleDef def = BundleDef.instance(depString, closures);
		bundleDefs.add(def);
		return def;
	}

	@Override
	public BundleDef bundle(String groupId, String artifactId, String version, Closure... closures) {
		BundleDef def = BundleDef.instance(groupId, artifactId, version, closures);
		bundleDefs.add(def);
		return def;
	}

	@Override
	public BundleDef bundle(Closure... closures) {
		BundleDef def = BundleDef.instance(closures);
		bundleDefs.add(def);
		return def;
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
	public void withSources(boolean withSources) {
		this.withSources = withSources;
	}

	@Override
	public boolean getWithSources() {
		return withSources;
	}

	@Override
	public List<BundleDef> getBundleDefs() {
		return Collections.unmodifiableList(bundleDefs);
	}

	public List<RuleDef> getRuleDefs() {
		return Collections.unmodifiableList(ruleDefs);
	}

	@Override
	public String toString() {
		return String.format("Bundles{%s}", Lists.transform(bundleDefs, def -> def.getGroupId() + ":" + def.getArtifactId()));
	}
}
