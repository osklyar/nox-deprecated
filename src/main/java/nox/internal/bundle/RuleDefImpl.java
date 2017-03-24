/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.bundle;

import org.gradle.util.ConfigureUtil;

import groovy.lang.Closure;


class RuleDefImpl extends AbstractDefImpl implements RuleDef {

	RuleDefImpl(String depString, Closure... closures) {
		this(closures);
		String[] parts = depString.split(":");
		switch (parts.length) {
			case 3:
				version = parts[2];
			case 2:
				artifactId = parts[1];
			case 1:
				groupId = parts[0];
				break;
			default:
		}
	}

	RuleDefImpl(String groupId, String artifactId, String version, Closure... closures) {
		this(closures);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	RuleDefImpl(String groupId, String artifactId, Closure... closures) {
		this(closures);
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	RuleDefImpl(Closure... closures) {
		if (closures != null) {
			for (Closure closure: closures) {
				ConfigureUtil.configure(closure, this);
			}
		}
	}
}
