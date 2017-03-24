/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.platform;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


public class Unit {

	protected Unit() {}

	public Unit(String id, String version) {
		this.id = id;
		this.version = version;
	}

	@JacksonXmlProperty(isAttribute=true)
  public String id;
	@JacksonXmlProperty(isAttribute=true)
  public String version;
}
