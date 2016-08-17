package org.mule.modules.gluuscim.entities;

import java.io.Serializable;
import java.util.List;

public class GluuSCIMUser implements Serializable {

	private static final long serialVersionUID = -3738849420258843959L;

	private String firstName;
	private String lastName;
	
	// mandatory field
	private String displayName; 
	private String email;
	private String password;
	private String gluuId;
	private List<GluuSCIMEntitlement> entitlements;
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getGluuId() {
		return gluuId;
	}
	public void setGluuId(String gluuId) {
		this.gluuId = gluuId;
	}
	public List<GluuSCIMEntitlement> getEntitlements() {
		return entitlements;
	}
	public void setEntitlements(List<GluuSCIMEntitlement> entitlements) {
		this.entitlements = entitlements;
	}
	
	public boolean hasEntitlements(){
		return this.getEntitlements() != null && !this.getEntitlements().isEmpty();
	}
	
}
