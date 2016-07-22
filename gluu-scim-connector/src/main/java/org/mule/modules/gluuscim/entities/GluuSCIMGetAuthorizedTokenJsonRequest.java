package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMGetAuthorizedTokenJsonRequest implements Serializable{

	private static final long serialVersionUID = -5326093904001330449L;

	@JsonProperty(value="ticket")
	private String scimTicket;
	
	@JsonProperty(value="rpt")
	private String scimToken;
	
	@JsonProperty(value="Host")
	private String host;

	public String getScimTicket() {
		return scimTicket;
	}

	public void setScimTicket(String scimTicket) {
		this.scimTicket = scimTicket;
	}

	public String getScimToken() {
		return scimToken;
	}

	public void setScimToken(String scimToken) {
		this.scimToken = scimToken;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
	
}
