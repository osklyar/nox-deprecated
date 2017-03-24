/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import java.io.File;
import java.io.IOException;
import java.util.Collection;


public interface MetadataExporter {

	static MetadataExporter instance(Bundle bundle, String org, Collection<Dependency> dependencies) {
		return new MetadataExporterIvyImpl(bundle, org, dependencies);
	}

	void exportTo(File targetDir) throws IOException;
}
