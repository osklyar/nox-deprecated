/**
 * Created by skol on 02.03.17.
 */
package nox.internal.entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class VersionTest {

	@Test
	public void version_maxGtMin_success() {
		assertEquals(Version.MIN.compareTo(Version.MAX), -1);
	}

	@Test
	public void version_min_correct() {
		assertEquals(Version.min(Version.MAX, Version.DEFAULT), Version.DEFAULT);
		assertEquals(Version.min(Version.DEFAULT), Version.DEFAULT);
	}

	@Test
	public void version_max_correct() {
		assertEquals(Version.max(Version.DEFAULT, Version.MIN), Version.DEFAULT);
		assertEquals(Version.max(Version.DEFAULT, Version.MIN), Version.DEFAULT);
	}


}
