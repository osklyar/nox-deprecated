/*
 * Copyright 2010 the original gradle/osgi author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nox.internal.osgi;

import aQute.bnd.osgi.Analyzer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.ConventionMapping;
import org.gradle.api.internal.IConventionAware;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.internal.DefaultManifest;
import org.gradle.api.specs.Spec;
import org.gradle.internal.UncheckedException;
import org.gradle.util.CollectionUtils;
import org.gradle.util.WrapUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public class DefaultOsgiManifest extends DefaultManifest implements OsgiManifest {

	/**
	 * Create-Version must match this pattern
	 */
	private static final Pattern OSGI_VERSION_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[0-9A-Za-z_-]+)?)?)?");

	private static final Pattern ONLY_NUMBERS = Pattern.compile("[0-9]+");

	private static final Pattern QUALIFIER = Pattern.compile("[0-9A-Za-z_\\-]*");

	// Because these properties can be convention mapped we need special handling in here.
	// If you add another one of these “modelled” properties, you need to update:
	// - maybeAppendModelledInstruction()
	// - maybePrependModelledInstruction()
	// - maybeSetModelledInstruction()
	// - getModelledInstructions()
	// - instructionValue()
	private String symbolicName;
	private String name;
	private String version;
	private String description;
	private String license;
	private String vendor;
	private String docURL;

	private File classesDir;
	private FileCollection classpath;

	private Map<String, List<String>> unmodelledInstructions = Maps.newHashMap();

	public DefaultOsgiManifest(String groupId, String artifactId, String version, FileResolver fileResolver) {
		super(fileResolver);
		ConventionMapping mapping = ((IConventionAware) this).getConventionMapping();
		mapping.map("version", new Callable<Object>() {
			public Object call() throws Exception {
				return evalBundleVersion(version);
			}
		});
		mapping.map("name", new Callable<Object>() {
			public Object call() throws Exception {
				return artifactId;
			}
		});
		mapping.map("symbolicName", new Callable<Object>() {
			public Object call() throws Exception {
				return evalBundleSymbolicName(groupId, artifactId);
			}
		});
	}

	@Override
	public DefaultManifest getEffectiveManifest() {
		DefaultManifest manifest = new DefaultManifest(null);
		try {
			Manifest osgiManifest = evalOSGiManifest();

			java.util.jar.Attributes attributes = osgiManifest.getMainAttributes();
			for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
				manifest.attributes(WrapUtil.toMap(entry.getKey().toString(), (String) entry.getValue()));
			}

			manifest.attributes(getAttributes());
			for (Map.Entry<String, Attributes> ent : getSections().entrySet()) {
				manifest.attributes(ent.getValue(), ent.getKey());
			}
			if (classesDir != null) {
				long mod = classesDir.lastModified();
				if (mod > 0) {
					manifest.getAttributes().put(Analyzer.BND_LASTMODIFIED, mod);
				}
			}
		} catch (Exception e) {
			throw UncheckedException.throwAsUncheckedException(e);
		}
		return getEffectiveManifestInternal(manifest);
	}

	private Manifest evalOSGiManifest() throws Exception {
		Analyzer analyzer = new Analyzer();
		for (Map.Entry<String, Object> attribute : getAttributes().entrySet()) {
			String key = attribute.getKey();
			if (!"Manifest-Version".equals(key)) {
				analyzer.setProperty(key, attribute.getValue().toString());
			}
		}

		Set<String> instructionNames = getInstructions().keySet();
		if (!instructionNames.contains(Analyzer.IMPORT_PACKAGE)) {
			analyzer.setProperty(Analyzer.IMPORT_PACKAGE,
				"*, !org.apache.ant.*, !org.junit.*, !org.jmock.*, !org.easymock.*, !org.mockito.*");
		}
		if (!instructionNames.contains(Analyzer.BUNDLE_VERSION)) {
			analyzer.setProperty(Analyzer.BUNDLE_VERSION, getVersion());
		}
		if (!instructionNames.contains(Analyzer.BUNDLE_NAME)) {
			analyzer.setProperty(Analyzer.BUNDLE_NAME, getName());
		}
		if (!instructionNames.contains(Analyzer.BUNDLE_SYMBOLICNAME)) {
			analyzer.setProperty(Analyzer.BUNDLE_SYMBOLICNAME, getSymbolicName());
		}
		if (!instructionNames.contains(Analyzer.EXPORT_PACKAGE)) {
			analyzer.setProperty(Analyzer.EXPORT_PACKAGE, "*;-noimport:=false;version=" + getVersion());
		}
		for (String instructionName : instructionNames) {
			String list = createPropertyStringFromList(instructionValue(instructionName));
			if (list != null && list.length() > 0) {
				analyzer.setProperty(instructionName, list);
			}
		}

		analyzer.setJar(getClassesDir());
		analyzer.setClasspath(getClasspath().getFiles().toArray(new File[0]));
		return analyzer.calcManifest();
	}

	@Override
	public List<String> instructionValue(String instructionName) {
		switch (instructionName) {
			case Analyzer.BUNDLE_SYMBOLICNAME:
				return createListFromPropertyString(getSymbolicName());
			case Analyzer.BUNDLE_NAME:
				return createListFromPropertyString(getName());
			case Analyzer.BUNDLE_VERSION:
				return createListFromPropertyString(getVersion());
			case Analyzer.BUNDLE_DESCRIPTION:
				return createListFromPropertyString(getDescription());
			case Analyzer.BUNDLE_LICENSE:
				return createListFromPropertyString(getLicense());
			case Analyzer.BUNDLE_VENDOR:
				return createListFromPropertyString(getVendor());
			case Analyzer.BUNDLE_DOCURL:
				return createListFromPropertyString(getDocURL());
			default:
				return unmodelledInstructions.get(instructionName);
		}
	}

	@Override
	public OsgiManifest instruction(String name, String... values) {
		if (!maybeAppendModelledInstruction(name, values)) {
			unmodelledInstructions.computeIfAbsent(name, k -> Lists.newArrayList());
			unmodelledInstructions.get(name).addAll(Arrays.asList(values));
		}
		return this;
	}

	private String appendValues(String existingValues, String... toPrepend) {
		List<String> parts = createListFromPropertyString(existingValues);
		if (parts == null) {
			return createPropertyStringFromArray(toPrepend);
		} else {
			parts.addAll(Arrays.asList(toPrepend));
			return createPropertyStringFromList(parts);
		}
	}

	private boolean maybeAppendModelledInstruction(String name, String... values) {
		switch (name) {
			case Analyzer.BUNDLE_SYMBOLICNAME:
				setSymbolicName(appendValues(getSymbolicName(), values));
				return true;
			case Analyzer.BUNDLE_NAME:
				setName(appendValues(getName(), values));
				return true;
			case Analyzer.BUNDLE_VERSION:
				setVersion(appendValues(getVersion(), values));
				return true;
			case Analyzer.BUNDLE_DESCRIPTION:
				setDescription(appendValues(getDescription(), values));
				return true;
			case Analyzer.BUNDLE_LICENSE:
				setLicense(appendValues(getLicense(), values));
				return true;
			case Analyzer.BUNDLE_VENDOR:
				setVendor(appendValues(getVendor(), values));
				return true;
			case Analyzer.BUNDLE_DOCURL:
				setDocURL(appendValues(getDocURL(), values));
				return true;
			default:
				return false;
		}
	}

	@Override
	public OsgiManifest instructionFirst(String name, String... values) {
		if (!maybePrependModelledInstruction(name, values)) {
			unmodelledInstructions.computeIfAbsent(name, k -> Lists.newArrayList());
			unmodelledInstructions.get(name).addAll(0, Arrays.asList(values));
		}
		return this;
	}

	private String prependValues(String existingValues, String... toPrepend) {
		List<String> parts = createListFromPropertyString(existingValues);
		if (parts == null) {
			return createPropertyStringFromArray(toPrepend);
		} else {
			parts.addAll(0, Arrays.asList(toPrepend));
			return createPropertyStringFromList(parts);
		}
	}

	private boolean maybePrependModelledInstruction(String name, String... values) {
		switch (name) {
			case Analyzer.BUNDLE_SYMBOLICNAME:
				setSymbolicName(prependValues(getSymbolicName(), values));
				return true;
			case Analyzer.BUNDLE_NAME:
				setName(prependValues(getName(), values));
				return true;
			case Analyzer.BUNDLE_VERSION:
				setVersion(prependValues(getVersion(), values));
				return true;
			case Analyzer.BUNDLE_DESCRIPTION:
				setDescription(prependValues(getDescription(), values));
				return true;
			case Analyzer.BUNDLE_LICENSE:
				setLicense(prependValues(getLicense(), values));
				return true;
			case Analyzer.BUNDLE_VENDOR:
				setVendor(prependValues(getVendor(), values));
				return true;
			case Analyzer.BUNDLE_DOCURL:
				setDocURL(prependValues(getDocURL(), values));
				return true;
			default:
				return false;
		}
	}

	@Override
	public OsgiManifest instructionReplace(String name, String... values) {
		if (!maybeSetModelledInstruction(name, values)) {
			if (values.length == 0 || (values.length == 1 && values[0] == null)) {
				unmodelledInstructions.remove(name);
			} else {
				unmodelledInstructions.computeIfAbsent(name, k -> Lists.newArrayList());
				List<String> instructionsForName = unmodelledInstructions.get(name);
				instructionsForName.clear();
				Collections.addAll(instructionsForName, values);
			}
		}
		return this;
	}

	private boolean maybeSetModelledInstruction(String name, String... values) {
		switch (name) {
			case Analyzer.BUNDLE_SYMBOLICNAME:
				setSymbolicName(createPropertyStringFromArray(values));
				return true;
			case Analyzer.BUNDLE_NAME:
				setName(createPropertyStringFromArray(values));
				return true;
			case Analyzer.BUNDLE_VERSION:
				setVersion(createPropertyStringFromArray(values));
				return true;
			case Analyzer.BUNDLE_DESCRIPTION:
				setDescription(createPropertyStringFromArray(values));
				return true;
			case Analyzer.BUNDLE_LICENSE:
				setLicense(createPropertyStringFromArray(values));
				return true;
			case Analyzer.BUNDLE_VENDOR:
				setVendor(createPropertyStringFromArray(values));
				return true;
			case Analyzer.BUNDLE_DOCURL:
				setDocURL(createPropertyStringFromArray(values));
				return true;
			default:
				return false;
		}
	}

	@Override
	public Map<String, List<String>> getInstructions() {
		Map<String, List<String>> instructions = new HashMap<String, List<String>>();
		instructions.putAll(unmodelledInstructions);
		instructions.putAll(getModelledInstructions());
		return instructions;
	}

	private String createPropertyStringFromArray(String... valueList) {
		return createPropertyStringFromList(Arrays.asList(valueList));
	}

	private String createPropertyStringFromList(List<String> valueList) {
		return valueList == null || valueList.isEmpty() ? null : CollectionUtils.join(",", valueList);
	}

	private List<String> createListFromPropertyString(String propertyString) {
		return propertyString == null || propertyString.length() == 0 ? null : new LinkedList<String>(Arrays.asList(propertyString.split(",")));
	}

	private Map<String, List<String>> getModelledInstructions() {
		Map<String, List<String>> modelledInstructions = new HashMap<String, List<String>>();
		modelledInstructions.put(Analyzer.BUNDLE_SYMBOLICNAME, createListFromPropertyString(symbolicName));
		modelledInstructions.put(Analyzer.BUNDLE_NAME, createListFromPropertyString(name));
		modelledInstructions.put(Analyzer.BUNDLE_VERSION, createListFromPropertyString(version));
		modelledInstructions.put(Analyzer.BUNDLE_DESCRIPTION, createListFromPropertyString(description));
		modelledInstructions.put(Analyzer.BUNDLE_LICENSE, createListFromPropertyString(description));
		modelledInstructions.put(Analyzer.BUNDLE_VENDOR, createListFromPropertyString(vendor));
		modelledInstructions.put(Analyzer.BUNDLE_DOCURL, createListFromPropertyString(docURL));

		return CollectionUtils.filter(modelledInstructions, new Spec<Map.Entry<String, List<String>>>() {
			public boolean isSatisfiedBy(Map.Entry<String, List<String>> element) {
				return element.getValue() != null;
			}
		});
	}

	/**
	 * Get the symbolic name as group + "." + archivesBaseName, with the following exceptions
	 * <ul>
	 * <li>
	 * if group has only one section (no dots) and archivesBaseName is not null then the first package
	 * name with classes is returned. eg. commons-logging:commons-logging -> org.apache.commons.logging
	 * </li>
	 * <li>
	 * if archivesBaseName is equal to last section of group then group is returned.
	 * eg. org.gradle:gradle -> org.gradle
	 * </li>
	 * <li>
	 * if archivesBaseName starts with last section of group that portion is removed.
	 * eg. org.gradle:gradle-core -> org.gradle.core
	 * </li>
	 * <li>
	 * if archivesBaseName starts with the full group, the archivesBaseName is return,
	 * e.g. org.gradle:org.gradle.core -> org.gradle.core
	 * </li>
	 * </ul>
	 *
	 * @param groupId    The group
	 * @param artifactId The base name of the artifact
	 * @return Returns the SymbolicName that should be used for the bundle.
	 */
	private String evalBundleSymbolicName(String groupId, String artifactId) {

		if (artifactId.startsWith(groupId)) {
			return artifactId;
		}
		int i = groupId.lastIndexOf('.');
		String lastSection = groupId.substring(++i);
		if (artifactId.equals(lastSection)) {
			return groupId;
		}
		if (artifactId.startsWith(lastSection)) {
			artifactId = artifactId.substring(lastSection.length());
			if (!Character.isLetterOrDigit(artifactId.charAt(0))) {
				artifactId = artifactId.substring(1);
			}
		}
		return String.format("%s.%s", groupId, artifactId);
	}

	private String evalBundleVersion(String version) {
		if (OSGI_VERSION_PATTERN.matcher(version).matches()) {
			return version;
		}

		int group = 0;
		boolean groupToken = true;
		String[] groups = new String[4];
		groups[0] = "0";
		groups[1] = "0";
		groups[2] = "0";
		groups[3] = "";
		StringTokenizer st = new StringTokenizer(version, ",./;'?:\\|=+-_*&^%$#@!~", true);
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (groupToken) {
				if (group < 3) {
					if (ONLY_NUMBERS.matcher(token).matches()) {
						groups[group++] = token;
						groupToken = false;
					} else {
						// if not a number, i.e. 2.ABD
						groups[3] = token + fillQualifier(st);
					}
				} else {
					// Last group; what ever is left take that replace all characters that are not alphanum or '_' or '-'
					groups[3] = token + fillQualifier(st);
				}
			} else {
				// If a delimiter; if dot, swap to groupToken, otherwise the rest belongs in qualifier.
				if (".".equals(token)) {
					groupToken = true;
				} else {
					groups[3] = fillQualifier(st);
				}
			}
		}
		String ver = groups[0] + "." + groups[1] + "." + groups[2];
		String result;
		if (groups[3].length() > 0) {
			result = ver + "." + groups[3];
		} else {
			result = ver;
		}
		if (!OSGI_VERSION_PATTERN.matcher(result).matches()) {
			throw new GradleException("OSGi plugin unable to convert version to a compliant version");
		}
		return result;
	}

	private String fillQualifier(StringTokenizer st) {
		StringBuilder buf = new StringBuilder();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (QUALIFIER.matcher(token).matches()) {
				buf.append(token);
			} else {
				buf.append("_");
			}
		}
		return buf.toString();
	}

	@Override
	public String getSymbolicName() {
		return symbolicName;
	}

	@Override
	public void setSymbolicName(String symbolicName) {
		this.symbolicName = symbolicName;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getLicense() {
		return license;
	}

	@Override
	public void setLicense(String license) {
		this.license = license;
	}

	@Override
	public String getVendor() {
		return vendor;
	}

	@Override
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	@Override
	public String getDocURL() {
		return docURL;
	}

	@Override
	public void setDocURL(String docURL) {
		this.docURL = docURL;
	}

	@Override
	public File getClassesDir() {
		return classesDir;
	}

	@Override
	public void setClassesDir(File classesDir) {
		this.classesDir = classesDir;
	}

	@Override
	public FileCollection getClasspath() {
		return classpath;
	}

	@Override
	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}
}
