/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.ext;

import java.util.List;

import groovy.lang.Closure;
import nox.internal.bundle.BundleDef;
import nox.internal.bundle.RuleDef;


public interface Bundles {

	String name = Bundles.class.getSimpleName().toLowerCase();

	String bundlesConfigFile = "bundles-config.json";

	static Bundles instance() {
		return new BundlesImpl();
	}

	BundleDef bundle(String depString, Closure... closures);
	BundleDef bundle(String groupId, String artifactId, String version, Closure... closures);
	BundleDef bundle(Closure... closures);

	RuleDef rule(String depString, Closure... closures);
	RuleDef rule(String groupId, String artifactId, String version, Closure... closures);
	RuleDef rule(String groupId, String artifactId, Closure... closures);
	RuleDef rule(Closure... closures);

	void withSources(boolean withSources);
	boolean getWithSources();

	List<BundleDef> getBundleDefs();
	List<RuleDef> getRuleDefs();
}
