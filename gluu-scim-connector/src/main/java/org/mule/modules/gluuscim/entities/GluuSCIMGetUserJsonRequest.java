package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMGetUserJsonRequest implements Serializable {
	
	private static final long serialVersionUID = -3491878401908363449L;

	@JsonProperty(value="attribute")
	String attribute;
	
	@JsonProperty(value="value")
	String value;

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
