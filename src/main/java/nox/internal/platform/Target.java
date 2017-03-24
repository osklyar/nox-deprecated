/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.platform;

import java.io.IOException;
import java.util.List;
import javax.xml.bind.annotation.XmlValue;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Lists;

@JacksonXmlRootElement(localName = "target")
public class Target {

	public static final String prefixFormat = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
		+ "<!-- GENERATED: DO NOT EDIT --><?pde version=\"%s\"?>";

	public static class Locations {
		public List<Location> location = Lists.newArrayList();
	}

	public static class TargetJRE {
		@JacksonXmlProperty(isAttribute=true)
		public String path = "org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8";
	}

	public static class LauncherArgs {
		@XmlValue
		public String vmArgs = "-XX:MaxPermSize=128M";
	}

	@JacksonXmlProperty(isAttribute=true)
	public String name;

	@JacksonXmlProperty(isAttribute=true)
	public String sequenceNumber = "10";

	public Locations locations = new Locations();

	public TargetJRE targetJRE = new TargetJRE();

	public LauncherArgs launcherArgs = new LauncherArgs();

	private String pdeVersion = "3.8";

	protected Target() {}

	public Target(String name) {
		this.name = name;
	}

	public Target withPdeVersion(String pdeVersion) {
		this.pdeVersion = pdeVersion;
		return this;
	}

	public Target withLocations(List<Location> locations) {
		this.locations.location.addAll(locations);
		return this;
	}

	public Target withLocation(Location location) {
		this.locations.location.add(location);
		return this;
	}

	public String toXMLString() throws IOException {
		JacksonXmlModule module = new JacksonXmlModule();
		module.setDefaultUseWrapper(false);
		XmlMapper mapper = new XmlMapper(module);
		return String.format(prefixFormat, pdeVersion) + mapper.writeValueAsString(this);
	}
}
