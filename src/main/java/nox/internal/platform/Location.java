/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.platform;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.google.common.collect.Lists;


public class Location {

	protected Location() {}

	public Location(Repository repository) {
		this.repository = repository;
	}

	@JacksonXmlProperty(isAttribute=true)
  public boolean includeAllPlatforms = false;

	@JacksonXmlProperty(isAttribute=true)
  public boolean includeConfigurePhase = false;

	@JacksonXmlProperty(isAttribute=true)
  public String includeMode = "planner";

	@JacksonXmlProperty(isAttribute=true)
  public boolean includeSource = true;

	@JacksonXmlProperty(isAttribute=true)
  public String type = "InstallableUnit";

  public List<Unit> unit = Lists.newArrayList();

  public Repository repository;
}
