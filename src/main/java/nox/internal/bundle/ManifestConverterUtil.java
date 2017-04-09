/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Objects;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.ModuleVersionIdentifier;

import nox.internal.entity.Version;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;


class ManifestConverterUtil {

	private static final DateTimeFormatter formatter =
		new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendLiteral("v")
			.appendValue(YEAR, 4)
			.appendValue(MONTH_OF_YEAR, 2)
			.appendValue(DAY_OF_MONTH, 2)
			.appendLiteral("-")
			.appendValue(ChronoField.HOUR_OF_DAY, 2)
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2).toFormatter();

	boolean isRelevant(RuleDef ruleDef, ModuleVersionIdentifier moduleId) {
		if (StringUtils.isBlank(ruleDef.getGroupId())) {
			return true;
		}
		if (!Objects.equal(ruleDef.getGroupId(), moduleId.getGroup())) {
			return false;
		}
		if (StringUtils.isBlank(ruleDef.getArtifactId())) {
			return true;
		}
		if (!Objects.equal(ruleDef.getArtifactId(), moduleId.getName())) {
			return false;
		}
		if (StringUtils.isBlank(ruleDef.getVersion())) {
			return true;
		}
		Pattern pattern = Pattern.compile(ruleDef.getVersion().replaceAll("\\.", "\\.").replaceAll("\\+", ".*"));
		return pattern.matcher(moduleId.getVersion()).matches();
	}

	String bundleSymbolicName(ModuleVersionIdentifier moduleId, List<RuleDef> ruleDefs) {
		String groupId = moduleId.getGroup();
		String artifactId = moduleId.getName();

		String symbolicName;
		if (!artifactId.startsWith(groupId)) {
			int i = groupId.lastIndexOf('.');
			String lastSection = groupId.substring(++i);
			if (artifactId.equals(lastSection)) {
				symbolicName = groupId;
			} else {
				if (artifactId.startsWith(lastSection)) {
					artifactId = artifactId.substring(lastSection.length());
					if (!Character.isLetterOrDigit(artifactId.charAt(0))) {
						artifactId = artifactId.substring(1);
					}
				}
				symbolicName = String.format("%s.%s", groupId, artifactId);
			}
		} else {
			symbolicName = artifactId;
		}

		for (RuleDef ruleDef : ruleDefs) {
			if (isRelevant(ruleDef, moduleId)) {
				if (StringUtils.isNotBlank(ruleDef.getInstructions().get("Bundle-SymbolicName"))) {
					symbolicName = ruleDef.getInstructions().get("Bundle-SymbolicName");
				}
				if (StringUtils.isNotBlank(ruleDef.getSymbolicName())) {
					symbolicName = ruleDef.getSymbolicName();
				}
			}
		}
		return symbolicName;
	}

	Version bundleVersion(ModuleVersionIdentifier moduleId, List<RuleDef> ruleDefs) {
		boolean withSuffix = true;
		try {
			String version = moduleId.getVersion();
			for (RuleDef ruleDef : ruleDefs) {
				// ignore version set on the bundle, it is resolved properly but was normally specified as wildcards
				if (!(ruleDef instanceof BundleDef) && isRelevant(ruleDef, moduleId)) {
					if (StringUtils.isNotBlank(ruleDef.getInstructions().get("Bundle-Version"))) {
						version = ruleDef.getInstructions().get("Bundle-Version");
					}
					if (StringUtils.isNotBlank(ruleDef.getVersion())) {
						version = ruleDef.getVersion();
					}
					if (!ruleDef.getWithQualifier()) {
						withSuffix = false;
					}
				}
			}

			Version v = new Version(version, false);
			if (!withSuffix) {
				return v;
			}
			return new Version(v.major, v.minor, v.build, LocalDateTime.now().format(formatter));
		} catch (IllegalArgumentException ex) {
			throw new GradleException(String.format("Incorrect version for %s", moduleId), ex);
		}
	}
}
