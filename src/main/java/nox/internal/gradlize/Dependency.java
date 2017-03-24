/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import nox.internal.entity.Version;
import nox.internal.entity.Versioned;

public class Dependency extends Versioned {

	public final boolean optional;

	public Dependency(String name) {
		this(name, Version.DEFAULT, false);
	}

	public Dependency(String name, Version version) {
		this(name, version, false);
	}

  public Dependency(String name, Version version, boolean optional) {
    super(name, version);
    this.optional = optional;
  }
}
