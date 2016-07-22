package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMGetTicketJsonResponse implements Serializable {

	private static final long serialVersionUID = -9131078372859387306L;

	@JsonProperty(value="ticket")
	private String scimTicket;

	public String getScimTicket() {
		return scimTicket;
	}

	public void setScimTicket(String scimTicket) {
		this.scimTicket = scimTicket;
	}
	
}
