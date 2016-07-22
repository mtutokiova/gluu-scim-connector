package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMUserObjectName implements Serializable {

	private static final long serialVersionUID = -4516775913592052601L;
	
	@JsonProperty(value="familyName")
	private String familyName;
	
	@JsonProperty(value="givenName")
	private String givenName;

	public String getFamilyName() {
		return familyName;
	}

	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	
}
