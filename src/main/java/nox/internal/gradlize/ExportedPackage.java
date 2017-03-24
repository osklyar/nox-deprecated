/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import nox.internal.entity.Version;
import nox.internal.entity.Versioned;

public class ExportedPackage extends Versioned {
	public ExportedPackage(String name, Version version) {
		super(name, version);
	}

	public ExportedPackage(String name) {
		super(name);
	}
}
