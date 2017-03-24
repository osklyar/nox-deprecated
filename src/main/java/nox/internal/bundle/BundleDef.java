/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.bundle;

import groovy.lang.Closure;

import java.io.File;
import java.util.List;


public interface BundleDef extends ModuleDef {

	static BundleDef instance(String depString, Closure... closures) {
		return new BundleDefImpl(depString, closures);
	}

	static BundleDef instance(String groupId, String artifactId, String version, Closure... closures) {
		return new BundleDefImpl(groupId, artifactId, version, closures);
	}

	static BundleDef instance(Closure... closures) {
		return new BundleDefImpl(closures);
	}

	RuleDef rule(String depString, Closure... closures);
	RuleDef rule(String groupId, String artifactId, String version, Closure... closures);
	RuleDef rule(String groupId, String artifactId, Closure... closures);
	RuleDef rule(Closure... closures);

	void jarFile(File jarFile);
	File getJarFile();

	void sourceJarFile(File jarFile);
	File getSourceJarFile();

	String toDependencyString();

	List<RuleDef> getRuleDefs();
}
