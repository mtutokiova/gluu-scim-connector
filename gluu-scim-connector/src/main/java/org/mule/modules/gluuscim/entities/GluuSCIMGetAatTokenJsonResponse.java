package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMGetAatTokenJsonResponse implements Serializable{

	private static final long serialVersionUID = -13442454564850759L;
	
	@JsonProperty(value="access_token")
	private String aatToken;
	
	@JsonProperty(value="expires_in")
	private long expirationTimeInSeconds;
	
	@JsonProperty(value="refresh_token")
	private String aatRefreshToken;
	
	@JsonProperty(value="token_type")
	private String aatTokenType;
	
	@JsonProperty(value="id_token")
	private String aatIdToken;
	
	public String getAatToken() {
		return aatToken;
	}

	public void setAatToken(String aatToken) {
		this.aatToken = aatToken;
	}

	public long getExpirationTimeInSeconds() {
		return expirationTimeInSeconds;
	}

	public void setExpirationTimeInSeconds(long expirationTimeInSeconds) {
		this.expirationTimeInSeconds = expirationTimeInSeconds;
	}

	public String getAatRefreshToken() {
		return aatRefreshToken;
	}

	public void setAatRefreshToken(String aatRefreshToken) {
		this.aatRefreshToken = aatRefreshToken;
	}

	public String getAatTokenType() {
		return aatTokenType;
	}

	public void setAatTokenType(String aatTokenType) {
		this.aatTokenType = aatTokenType;
	}

	public String getAatIdToken() {
		return aatIdToken;
	}

	public void setAatIdToken(String aatIdToken) {
		this.aatIdToken = aatIdToken;
	}
	
}
