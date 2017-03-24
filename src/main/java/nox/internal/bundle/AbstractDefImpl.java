/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.bundle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;


public abstract class AbstractDefImpl implements RuleDef {
	protected String groupId = null;
	protected String artifactId = null;
	protected String version = null;
	protected boolean withQualifier = true;
	protected String symbolicName;
	protected final LinkedHashMap<String, String> instructions = Maps.newLinkedHashMap();
	protected final List<String> exports = Lists.newArrayList();
	protected final List<String> privates = Lists.newArrayList();
	protected final List<String> optionals = Lists.newArrayList();

	@Override
	public void groupId(String groupId) {
		this.groupId = groupId;
	}

	@Override
	public String getGroupId() {
		return groupId;
	}

	@Override
	public void artifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	@Override
	public String getArtifactId() {
		return artifactId;
	}

	@Override
	public void version(String version) {
		this.version = version;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void withQualifier(boolean flag) {
		withQualifier = flag;
	}

	@Override
	public boolean getWithQualifier() {
		return withQualifier;
	}

	@Override
	public void symbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public void instruction(String instruction, String value) {
		instructions.put(instruction, value);
	}

	@Override
	public LinkedHashMap<String, String> getInstructions() {
		return instructions;
	}

	@Override
	public void exports(String... pkgNames) {
		exports.addAll(Lists.newArrayList(pkgNames));
	}

	@Override
	public List<String> getExports() {
		return Collections.unmodifiableList(exports);
	}

	@Override
	public void privates(String... pkgNames) {
		privates.addAll(Lists.newArrayList(pkgNames));
	}

	@Override
	public List<String> getPrivates() {
		return Collections.unmodifiableList(privates);
	}

	@Override
	public void optionals(String... pkgNames) {
		optionals.addAll(Lists.newArrayList(pkgNames));
	}

	@Override
	public List<String> getOptionals() {
		return Collections.unmodifiableList(optionals);
	}

	@Override
	public String toString() {
		String res = groupId;
		if (StringUtils.isNotBlank(artifactId)) {
			res += ":" + artifactId;
		}
		if (StringUtils.isNotBlank(version)) {
			res += ":" + version;
		}
		return res;
 	}
}
