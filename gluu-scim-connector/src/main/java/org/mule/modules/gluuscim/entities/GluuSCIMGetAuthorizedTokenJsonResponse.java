package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMGetAuthorizedTokenJsonResponse implements Serializable {
	
	private static final long serialVersionUID = 4628736684868457677L;
	
	@JsonProperty(value="rpt")
	private String authorizedToken;

	public String getAuthorizedToken() {
		return authorizedToken;
	}

	public void setAuthorizedToken(String authorizedToken) {
		this.authorizedToken = authorizedToken;
	}

}
