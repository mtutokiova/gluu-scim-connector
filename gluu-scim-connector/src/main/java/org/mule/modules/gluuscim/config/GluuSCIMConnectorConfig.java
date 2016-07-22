package org.mule.modules.gluuscim.config;

import org.mule.api.annotations.components.Configuration;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.param.Default;

@Configuration(friendlyName = "SCIM Configuration")
public class GluuSCIMConnectorConfig {

	@Configurable
	@FriendlyName("Gluu Hostname")
    @Default(value="idp.d.aws.economist.com")
    private String host;
	
	@Configurable
	@FriendlyName("Mule Runtime Hostname")
    @Default(value="https://dev-economistapi.cloudhub.io")
    private String redirectUri;
	
	@Configurable()
    private String aatRefreshToken;
    
//	client_id
	@Configurable
    private String username;
	
//	client_scret
	@Configurable
    private String password;
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}

	public String getAatRefreshToken() {
		return aatRefreshToken;
	}

	public void setAatRefreshToken(String aatRefreshToken) {
		this.aatRefreshToken = aatRefreshToken;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}