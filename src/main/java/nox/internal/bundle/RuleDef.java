/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.bundle;

import java.util.LinkedHashMap;
import java.util.List;

import groovy.lang.Closure;


public interface RuleDef {

	static RuleDef instance(String depString, Closure... closures) {
		return new RuleDefImpl(depString, closures);
	}

	static RuleDef instance(String groupId, String artifactId, String version, Closure... closures) {
		return new RuleDefImpl(groupId, artifactId, version, closures);
	}

	static RuleDef instance(String groupId, String artifactId, Closure... closures) {
		return new RuleDefImpl(groupId, artifactId, closures);
	}

	static RuleDef instance(Closure... closures) {
		return new RuleDefImpl(closures);
	}

	void groupId(String groupId);

	String getGroupId();

	void artifactId(String artifactId);

	String getArtifactId();

	void version(String version);

	String getVersion();

	void withQualifier(boolean flag);

	boolean getWithQualifier();

	void symbolicName(String symbolicName);

	String getSymbolicName();

	void instruction(String instruction, String value);

	LinkedHashMap<String, String> getInstructions();

	void exports(String... pkgNames);

	List<String> getExports();

	void privates(String... pkgNames);

	List<String> getPrivates();

	void optionals(String... pkgNames);

	List<String> getOptionals();
}
