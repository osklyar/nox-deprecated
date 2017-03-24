/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.dep;

import java.io.File;
import java.io.IOException;


public interface MetadataExporter {
	void exportTo(File targetDir) throws IOException;
}
