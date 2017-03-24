/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import java.util.Collection;


public interface DependencyResolver {

	static DependencyResolver instance(BundleUniverse universe) {
		return new DependencyResolverImpl(universe);
	}

	Collection<Dependency> resolveFor(Bundle bundle);
}
