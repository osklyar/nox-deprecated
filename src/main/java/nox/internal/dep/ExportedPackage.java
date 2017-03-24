/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.dep;

public class ExportedPackage extends Versioned {
	public ExportedPackage(String name, Version version) {
		super(name, version);
	}

	public ExportedPackage(String name) {
		super(name);
	}
}
