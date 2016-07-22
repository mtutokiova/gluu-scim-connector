package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMGetTokenJsonResponse implements Serializable {
	
	private static final long serialVersionUID = -5389956075047832562L;

	@JsonProperty(value="rpt")
	private String scimToken;

	public String getScimToken() {
		return scimToken;
	}

	public void setScimToken(String scimToken) {
		this.scimToken = scimToken;
	}
	
}
