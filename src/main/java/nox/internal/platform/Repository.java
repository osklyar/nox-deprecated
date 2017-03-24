/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.internal.platform;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


public class Repository {

	protected Repository() {}

	public Repository(String location) {
		this.location = location;
	}

	@JacksonXmlProperty(isAttribute=true)
	public String location;
}
