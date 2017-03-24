/**
 * Created by skol on 07.03.17.
 */
package nox.internal.bundle;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier;
import org.junit.Test;

import static org.gradle.internal.impldep.org.junit.Assert.assertFalse;
import static org.gradle.internal.impldep.org.junit.Assert.assertTrue;

public class ManifestConverterUtillTest {

	@Test
	public void isRelevant_true() {
		ModuleVersionIdentifier moduleId = new DefaultModuleVersionIdentifier("google", "guava", "15.0");
		ManifestConverterUtil underTest = new ManifestConverterUtil();

		assertTrue(underTest.isRelevant(new RuleDefImpl("google:guava:15.0"), moduleId));
		assertTrue(underTest.isRelevant(new RuleDefImpl("google:guava:15.+"), moduleId));
		assertTrue(underTest.isRelevant(new RuleDefImpl("google:guava:15+"), moduleId));
		assertTrue(underTest.isRelevant(new RuleDefImpl("google:guava:+"), moduleId));
		assertTrue(underTest.isRelevant(new RuleDefImpl("google:guava"), moduleId));
		assertTrue(underTest.isRelevant(new RuleDefImpl("google"), moduleId));
	}

	@Test
	public void isRelevant_false() {
		ModuleVersionIdentifier moduleId = new DefaultModuleVersionIdentifier("google", "guava", "15.0");
		ManifestConverterUtil underTest = new ManifestConverterUtil();

		assertFalse(underTest.isRelevant(new RuleDefImpl("google:guava:15.1"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("google:guava:14.0"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("google:guava:14.+"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("google:guava:14+"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("google:guavax"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("googlex"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("googlex:guava"), moduleId));
		assertFalse(underTest.isRelevant(new RuleDefImpl("googlex:guava:15.0"), moduleId));
	}
}
