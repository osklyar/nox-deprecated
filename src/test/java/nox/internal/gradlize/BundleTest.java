/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.gradlize;

import com.google.common.collect.Sets;
import nox.internal.entity.Version;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class BundleTest {

	private static Manifest guavaManifest;
	private static Manifest helloworldManifest;

	@BeforeClass
	public static void init() throws Exception {
		ClassLoader cl = BundleTest.class.getClassLoader();
		guavaManifest = new Manifest(cl.getResourceAsStream("com.google.guava-MANIFEST.MF"));
		helloworldManifest = new Manifest(cl.getResourceAsStream("helloworld-MANIFEST.MF"));
	}

	@Test
	public void parse_forNameAndVersion_success() throws Exception {
		Bundle bundle = Bundle.parse(helloworldManifest);

		assertEquals("com.profidata.osgipoc.helloworld", bundle.name);
		assertEquals(new Version(0, 1, 0), bundle.version);
	}

	@Test
	public void parse_forExportedPackages_success() throws Exception {
		Bundle bundle = Bundle.parse(guavaManifest);

		assertEquals(16, bundle.exportedPackages.size());
		assertTrue(bundle.exportedPackages.contains(new ExportedPackage("com.google.common.math", new Version(21, 0,0))));
	}

	@Test
	public void parse_forRequirements_success() throws Exception {
		Bundle bundle = Bundle.parse(helloworldManifest);

		assertEquals(3, bundle.requiredBundles.size());
		assertEquals("org.eclipse.ui", bundle.requiredBundles.get(0).name);
		assertEquals(new Version(3, 1, 2), bundle.requiredBundles.get(0).from);
		assertEquals(new Version(4, 0, 0), bundle.requiredBundles.get(0).to);
		assertEquals("org.eclipse.core.runtime", bundle.requiredBundles.get(1).name);
		assertEquals(Version.MIN, bundle.requiredBundles.get(1).from);
		assertEquals(Version.MAX, bundle.requiredBundles.get(1).to);

		assertEquals(10, bundle.importedPackages.size());
		assertEquals("com.profidata.osgipoc.shared", bundle.importedPackages.get(0).name);
		assertEquals(new Version(0, 1, 0), bundle.importedPackages.get(0).from);
		assertEquals(new Version(1, 0, 0), bundle.importedPackages.get(0).to);
		assertEquals("org.osgi.framework", bundle.importedPackages.get(9).name);
		assertEquals(new Version(1, 8, 0), bundle.importedPackages.get(9).from);
		assertEquals(new Version(2, 0, 0), bundle.importedPackages.get(9).to);
	}

	@Test
	public void parseMfLine_singleEntityWithQuotes_success() throws Exception {
		String line = guavaManifest.getMainAttributes().getValue("Require-Capability");
		Map<String, Map<String, String>> data = Bundle.parseMfLine(line);
		assertEquals(1, data.size());
		assertEquals(1, data.get("osgi.ee").size());
		assertEquals("(&(osgi.ee=JavaSE)(version=1.8))", data.get("osgi.ee").get("filter"));
	}

	@Test
	public void parseMfLine_multipleEntities_success() throws Exception {
		String line = guavaManifest.getMainAttributes().getValue("Import-Package");
		Map<String, Map<String, String>> data = Bundle.parseMfLine(line);
		assertEquals(Sets.newHashSet("javax.annotation", "javax.crypto", "javax.crypto.spec", "sun.misc"), data.keySet());
		for (Map<String, String> values: data.values()) {
			assertEquals(1, values.size());
			assertEquals("optional", values.get("resolution"));
		}
	}

	@Test
	public void parseMfLine_withCommasInQuotes_success() throws Exception {
		String line = guavaManifest.getMainAttributes().getValue("Export-Package");
		Map<String, Map<String, String>> data = Bundle.parseMfLine(line);
		assertEquals("21.0.0", data.get("com.google.common.graph").get("version"));
		assertEquals("com.google.common.collect,javax.annotation", data.get("com.google.common.graph").get("uses"));
		assertEquals("21.0.0", data.get("com.google.common.xml").get("version"));
		assertEquals("com.google.common.escape", data.get("com.google.common.xml").get("uses"));
	}
}
