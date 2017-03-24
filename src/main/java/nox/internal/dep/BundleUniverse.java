/**
 * Created by skol on 28.02.17.
 */
package nox.internal.dep;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;


public interface BundleUniverse {

	Set<String> bundleNames();

	SortedSet<Version> bundleVersions(String bundleName);

	Set<String> packageNames();

	SortedMap<Version, Bundle> packageVersionsWithExportingBundles(String pkgName);

	BundleUniverse with(Bundle bundle);
}
