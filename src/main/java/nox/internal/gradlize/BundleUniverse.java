/**
 * Created by skol on 28.02.17.
 */
package nox.internal.gradlize;

import nox.internal.entity.Version;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;


public interface BundleUniverse {

	static BundleUniverse instance(Duplicates duplicates) {
		return new BundleUniverseImpl(duplicates);
	}

	Set<String> bundleNames();

	SortedSet<Version> bundleVersions(String bundleName);

	Set<String> packageNames();

	SortedMap<Version, Bundle> packageVersionsWithExportingBundles(String pkgName);

	BundleUniverse with(Bundle bundle);
}
