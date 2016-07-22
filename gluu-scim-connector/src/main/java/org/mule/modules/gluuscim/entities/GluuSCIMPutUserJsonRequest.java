package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMPutUserJsonRequest implements Serializable{

	private static final long serialVersionUID = -3245741686775688146L;

	@JsonProperty(value="schemas")
	private String[] schemas;
	
	@JsonProperty(value="urn:ietf:params:scim:schemas:extension:gluu:2.0:User")
	private GluuSCIMUserExtension userExtension;
	
	@JsonProperty(value="name")
	private GluuSCIMUserObjectName name;

	public String[] getSchemas() {
		return schemas;
	}

	public void setSchemas(String[] schemas) {
		this.schemas = schemas;
	}

	public GluuSCIMUserExtension getUserExtension() {
		return userExtension;
	}

	public void setUserExtension(GluuSCIMUserExtension userExtension) {
		this.userExtension = userExtension;
	}

	public GluuSCIMUserObjectName getName() {
		return name;
	}

	public void setName(GluuSCIMUserObjectName name) {
		this.name = name;
	}
	
}
