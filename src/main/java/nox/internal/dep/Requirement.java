/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.dep;

public class Requirement {

  public String name;

  public Version from = Version.MIN; // inclusive

	public Version to = Version.MAX;   // exclusive

	public boolean optional = false;

	public Requirement(String name) {
		this.name = name;
	}

	public Requirement(String name, Version version) {
		this(name, version, version.nextMajor(), false);
	}

  public Requirement(String name, Version from, Version to, boolean optional) {
    this.name = name;
    this.from = from;
    this.to = to;
    this.optional = optional;
  }

  @Override
  public String toString() {
		return String.format("%s:[%s,%s)", name, from, to);
	}
}
