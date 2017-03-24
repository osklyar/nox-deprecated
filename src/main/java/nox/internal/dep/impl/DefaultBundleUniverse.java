/**
 * Created by skol on 28.02.17.
 */
package nox.internal.dep.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nox.internal.dep.Bundle;
import nox.internal.dep.BundleUniverse;
import nox.internal.dep.Duplicates;
import nox.internal.dep.ExportedPackage;
import nox.internal.dep.Version;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;


public class DefaultBundleUniverse implements BundleUniverse {

	private final Map<String, SortedSet<Version>> bundleVersions = Maps.newHashMap();

	private final Map<String, SortedMap<Version, Bundle>> pkgImplBundles = Maps.newHashMap();

	private final Duplicates duplicates;

	public DefaultBundleUniverse(Duplicates duplicates) {
		this.duplicates = duplicates;
	}

	@Override
	public Set<String> bundleNames() {
		return bundleVersions.keySet();
	}

	@Override
	public SortedSet<Version> bundleVersions(String bundleName) {
		SortedSet<Version> res = bundleVersions.get(bundleName);
		if (res == null) {
			return Sets.newTreeSet();
		}
		return Sets.newTreeSet(res); // copy
	}

	@Override
	public Set<String> packageNames() {
		return pkgImplBundles.keySet();
	}

	@Override
	public SortedMap<Version, Bundle> packageVersionsWithExportingBundles(String pkgName) {
		SortedMap<Version, Bundle> res = pkgImplBundles.get(pkgName);
		if (res == null) {
			return Maps.newTreeMap();
		}
		return Maps.newTreeMap(res); // copy
	}

	@Override
	public BundleUniverse with(Bundle bundle) {
		if (!bundleVersions.containsKey(bundle.name)) {
			bundleVersions.put(bundle.name, Sets.newTreeSet());
		}
		SortedSet<Version> versions = bundleVersions.get(bundle.name);
		Preconditions.checkState(duplicates.permitted() || !versions.contains(bundle.version),
			"Create %s already exists", bundle);
		versions.add(bundle.version);

		for (ExportedPackage expPkg : bundle.exportedPackages) {
			if (!pkgImplBundles.containsKey(expPkg.name)) {
				pkgImplBundles.put(expPkg.name, Maps.newTreeMap());
			}
			SortedMap<Version, Bundle> pkgVersions = pkgImplBundles.get(expPkg.name);
			// overwrites already present bundle for the same package version
			pkgVersions.put(expPkg.version, bundle);
		}
		return this;
	}
}
