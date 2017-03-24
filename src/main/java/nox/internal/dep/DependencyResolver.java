/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.dep;

import java.util.Collection;


public interface DependencyResolver {
	Collection<Dependency> resolveFor(Bundle bundle);
}
