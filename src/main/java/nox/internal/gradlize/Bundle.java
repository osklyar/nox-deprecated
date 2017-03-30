/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;

import nox.internal.entity.Version;
import nox.internal.entity.Versioned;


public class Bundle extends Versioned {

	private static final Pattern MF_KV_PATTERN = Pattern.compile("^(.+?):?=(.+)$");

	private static final Pattern MF_VERS_PATTERN = Pattern.compile("^\\[(.+?),(.+?)(\\)|])$");

	public final Set<ExportedPackage> exportedPackages;

	public final List<Requirement> importedPackages;

	public final List<Requirement> requiredBundles;


	private Bundle(String name, Version version, Set<ExportedPackage> exportedPackages, List<Requirement> importedPackages, List<Requirement> requiredBundles) {
		super(name, version);
		this.exportedPackages = Collections.unmodifiableSet(exportedPackages);
		this.importedPackages = Collections.unmodifiableList(importedPackages);
		this.requiredBundles = Collections.unmodifiableList(requiredBundles);
	}

	public static Bundle parse(Manifest manifest) {
		Attributes attrs = manifest.getMainAttributes();

		String name = attrs.getValue("Bundle-SymbolicName");
		Preconditions.checkNotNull(name, "Missing Create-SymbolicName in manifest %s", manifest);
		name = name.split(";")[0].split(",")[0].trim();

		Version version = new Version(attrs.getValue("Bundle-Version"), true);

		Set<ExportedPackage> expPacks = parseExportedPackages(attrs.getValue("Export-Package"), version);
		List<Requirement> impPacks = parseRequirements(attrs.getValue("Import-Package"));
		List<Requirement> reqBndls = parseRequirements(attrs.getValue("Require-Bundle"));

		return new Bundle(name, version, expPacks, impPacks, reqBndls);
	}

	private static Set<ExportedPackage> parseExportedPackages(String exportString, Version bundleVersion) {
		if (StringUtils.isBlank(exportString)) {
			return Collections.emptySet();
		}
		Set<ExportedPackage> res = Sets.newHashSet();
		for (Map.Entry<String, Map<String, String>> entry: parseMfLine(exportString).entrySet()) {
			String version = entry.getValue().get("version");
			if (version != null) {
				res.add(new ExportedPackage(entry.getKey(), new Version(version, false)));
			} else {
				res.add(new ExportedPackage(entry.getKey(), bundleVersion));
			}
		}
		return res;
	}

	private static List<Requirement> parseRequirements(String requireString) {
		if (StringUtils.isBlank(requireString)) {
			return Collections.emptyList();
		}
		List<Requirement> res = Lists.newArrayList();
		for (Map.Entry<String, Map<String, String>> entry: parseMfLine(requireString).entrySet()) {
			String name = entry.getKey();
			boolean optional = "optional".equalsIgnoreCase(entry.getValue().get("resolution"));
			Version from = Version.MIN;
			Version to = Version.MAX;
			String versionString = entry.getValue().get("version");
			if (StringUtils.isBlank(versionString)) {
				versionString = entry.getValue().get("bundle-version");
			}
			if (StringUtils.isNotBlank(versionString)) {
				Matcher matcher = MF_VERS_PATTERN.matcher(versionString);
				if (matcher.find()) {
					from = new Version(matcher.group(1), false);
					to = new Version(matcher.group(2), false);
					if (Objects.equal(to, from)) {
						to = from.nextMajor();
					}
				} else {
					from = new Version(versionString, false);
					to = from.nextMajor();
				}
			}
			res.add(new Requirement(name, from, to, optional));
		}
		return res;
	}

	static Map<String, Map<String, String>> parseMfLine(String line) {
		Map<String, Map<String, String>> res = Maps.newLinkedHashMap();
		boolean inQuotation = false;
		for (int start = 0, end = 0; end < line.length(); end++) {
			char c = line.charAt(end);
			if (c == '"') {
				inQuotation = !inQuotation;
			}
			if (!inQuotation && end > start + 1 && (c == ',' || end == line.length() - 1)) {
				List<String> parts = Lists.newArrayList(line.substring(start, c == ',' ? end : end + 1).split(";"));
				start = end + 1;
				Map<String, String> elements = Maps.newHashMap();
				res.put(parts.remove(0).trim(), elements);
				for (String part: parts) {
					Matcher matcher = MF_KV_PATTERN.matcher(part);
					if (matcher.find()) {
						String key = matcher.group(1).trim();
						String value = matcher.group(2).trim();
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						}
						elements.put(key, value);
					}
				}
			}
		}
		return res;
	}

}
