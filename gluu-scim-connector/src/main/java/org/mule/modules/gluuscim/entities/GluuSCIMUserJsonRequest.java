package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@JsonInclude(Include.NON_NULL)
public class GluuSCIMUserJsonRequest implements Serializable{

	private static final long serialVersionUID = -3245741686775688146L;

	@JsonProperty(value="schemas")
	private String[] schemas;
	
	@JsonProperty(value="id")
	private String id;
	
	@JsonProperty(value="userName")
	private String userName;
	
	@JsonProperty(value="name")
	private GluuSCIMUserNameJsonObject name;
	
	@JsonProperty(value="displayName")
	private String displayName;
	
	@JsonProperty(value="password")
	private String password;
	
	@JsonProperty(value="urn:ietf:params:scim:schemas:extension:gluu:2.0:User")
	private ObjectNode userExtension;

	public String[] getSchemas() {
		return schemas;
	}

	public void setSchemas(String[] schemas) {
		this.schemas = schemas;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ObjectNode getUserExtension() {
		return userExtension;
	}

	public void setUserExtension(ObjectNode userExtension) {
		this.userExtension = userExtension;
	}

	public GluuSCIMUserNameJsonObject getName() {
		return name;
	}

	public void setName(GluuSCIMUserNameJsonObject name) {
		this.name = name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
