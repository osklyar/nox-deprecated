/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.platform;

import org.junit.Assert;
import org.junit.Test;


public class TargetTest {

	@Test
	public void test() throws Exception {
		Target e46 = new Target("e46");
		Location location1 = new Location(new Repository("repo1"));
		location1.unit.add(new Unit("com.google.guava", "13"));
		location1.unit.add(new Unit("org.apache.commons", "15"));
		Location location2 = new Location(new Repository("repo2"));
		location2.unit.add(new Unit("org.apache.commons", "14"));
		e46.locations.location.add(location1);
		e46.locations.location.add(location2);

		String res = e46.toXMLString();

		Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><!-- GENERATED: DO NOT EDIT --><?pde version=\"3.8\"?><target name=\"e46\" sequenceNumber=\"10\"><locations><location includeAllPlatforms=\"false\" includeConfigurePhase=\"false\" includeMode=\"planner\" includeSource=\"true\" type=\"InstallableUnit\"><unit id=\"com.google.guava\" version=\"13\"/><unit id=\"org.apache.commons\" version=\"15\"/><repository location=\"repo1\"/></location><location includeAllPlatforms=\"false\" includeConfigurePhase=\"false\" includeMode=\"planner\" includeSource=\"true\" type=\"InstallableUnit\"><unit id=\"org.apache.commons\" version=\"14\"/><repository location=\"repo2\"/></location></locations><targetJRE path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8\"/><launcherArgs><vmArgs>-XX:MaxPermSize=128M</vmArgs></launcherArgs></target>", res);
	}


}
