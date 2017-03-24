/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.bundle;

public interface ModuleDef extends RuleDef {

	void replaceOSGiManifest(boolean flag);

	boolean getReplaceOSGiManifest();
}
