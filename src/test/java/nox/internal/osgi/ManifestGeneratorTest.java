/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.osgi;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.google.common.base.Preconditions;

import org.gradle.api.internal.plugins.osgi.ContainedVersionAnalyzer;
import org.gradle.api.internal.plugins.osgi.DefaultAnalyzerFactory;
import org.gradle.internal.impldep.aQute.bnd.osgi.Analyzer;
import org.gradle.internal.impldep.aQute.bnd.osgi.Jar;
import org.gradle.internal.impldep.org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;


public class ManifestGeneratorTest {

	@Test
	public void generate_forJar_success1() throws Exception {
		URL resource = ManifestGeneratorTest.class.getClassLoader().getResource("spring-context-4.3.5.RELEASE.jar");
		Preconditions.checkNotNull(resource);
		File jarFile = new File(resource.toURI());
/*
		DefaultOsgiManifest manifest = new DefaultOsgiManifest(new BaseDirFileResolver());
		manifest.setClassesDir(jarFile);
		DefaultManifest foo = manifest.getEffectiveManifest();
		Assert.assertNotNull(foo);
*/
	}

	@Test
	public void generate_forJar_success() throws Exception {
		// DefaultManifest effectiveManifest = new DefaultManifest(null);

		URL resource = ManifestGeneratorTest.class.getClassLoader().getResource("spring-context-4.3.5.RELEASE.jar");
		Preconditions.checkNotNull(resource);
		File jarFile = new File(resource.toURI());

		Manifest manifest = generate(jarFile, null);
		Assert.assertNotNull(manifest);
	}

	private Manifest generate(File jarFile, Document pom) throws Exception {

		Analyzer analyzer = getAnalyzer(jarFile);

		return analyzer.calcManifest();
	}

	private Analyzer getAnalyzer(File jarFile) throws Exception {
		// https://github.com/gradle/gradle/blob/master/subprojects/osgi/src/main/java/org/gradle/api/internal/plugins/osgi/DefaultOsgiManifest.java
		ContainedVersionAnalyzer analyzer = new DefaultAnalyzerFactory().create();
		analyzer.setJar(new Jar(jarFile));

		Manifest manifest = new JarFile(jarFile).getManifest();

		for (Map.Entry<String, Attributes> attribute : manifest.getEntries().entrySet()) {
			String key = attribute.getKey();
			if (!"Manifest-Version".equals(key)) {
				analyzer.setProperty(key, attribute.getValue().toString());
			}
		}

		analyzer.setPedantic(true);

		// analyzer.setClasspath(getClasspath().getFiles().toArray(new File[0]));
		return analyzer;
	}

}
