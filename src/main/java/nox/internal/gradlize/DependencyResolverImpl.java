/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nox.internal.entity.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;


class DependencyResolverImpl implements DependencyResolver {

	private static final Logger logger = LoggerFactory.getLogger(DependencyResolverImpl.class);

	private final BundleUniverse universe;

	DependencyResolverImpl(BundleUniverse universe) {
		this.universe = universe;
	}

	@Override
	public Collection<Dependency> resolveFor(Bundle bundle) {
		Map<String, SortedSet<Version>> depRange = Maps.newHashMap();
		for (Requirement bundleReq: bundle.requiredBundles) {
			if (!bundleReq.optional && !Objects.equal(bundle.name, bundleReq.name)) {
				SortedSet<Version> allBundleVersions = universe.bundleVersions(bundleReq.name);
				SortedSet<Version> okBundleVersions = allBundleVersions.subSet(bundleReq.from, bundleReq.to);
				if (!okBundleVersions.isEmpty()) {
					depRange.put(bundleReq.name, Sets.newTreeSet(okBundleVersions));
				} else if (!allBundleVersions.isEmpty()) {
					logger.info("Required bundle dependency {} version mismatch for {}", bundleReq.name, bundle);
					depRange.put(bundleReq.name, allBundleVersions);
				} else {
					logger.warn("Required bundle dependency {} not available for {}", bundleReq.name, bundle);
				}
			}
		}

		for (Requirement pkgReq: bundle.importedPackages) {
			Entry<String, SortedSet<Version>> dep = resolveForPackage(bundle, pkgReq);
			if (dep != null) {
				depRange.put(dep.getKey(), dep.getValue()); // possibly overwrite with more stringent
			}
		}

		List<Dependency> res = Lists.newArrayList();
		for (Entry<String, SortedSet<Version>> entry: depRange.entrySet()) {
			res.add(new Dependency(entry.getKey(), entry.getValue().last())); // above code guarantees at least 1 value
		}
		if (!res.isEmpty()) {
			logger.debug("Dependency set for {}: {}", bundle, res);
		}
		return res;
	}

	// - will prefer current matching over new
	// - may restrict the version range of current
	// - if no exact matching found may return mismatching version range for implementing bundle (will keep current)
	private Entry<String, SortedSet<Version>> resolveForPackage(Bundle bundle, Requirement pkgReq) {
		if (pkgReq.optional || pkgReq.name.startsWith("javax.")) {
			return null;
		}
		SortedMap<Version, Bundle> allPkgVersions = universe.packageVersionsWithExportingBundles(pkgReq.name);
		allPkgVersions = Maps.filterValues(allPkgVersions, implBundle -> !Objects.equal(implBundle.name, bundle.name));
		if (allPkgVersions.isEmpty()) {
			logger.warn("No bundle exports package {} required by {}", pkgReq.name, bundle);
			return null;
		}

		SortedMap<Version, Bundle> pkgVersions = allPkgVersions.subMap(pkgReq.from, pkgReq.to);
		boolean imperfectMatch = false;
		if (pkgVersions.isEmpty()) {
			imperfectMatch = true;
			pkgVersions = allPkgVersions;
		}

		// prefer bundle providing latest package version
		String bundleName = pkgVersions.get(pkgVersions.lastKey()).name;
		// but return all bundle versions of the same bundle
		SortedSet<Version> bundleVersions = Sets.newTreeSet();
		for (Bundle expBundle : pkgVersions.values()) {
			if (Objects.equal(bundleName, expBundle.name)) {
				bundleVersions.add(expBundle.version);
			}
		}
		Entry<String, SortedSet<Version>> res = new SimpleEntry<>(bundleName, bundleVersions);
		if (imperfectMatch) {
			logger.warn("Package requirement {} version mismatch {} for bundle {}", pkgReq, res, bundle);
		}
		return res;
	}

}
