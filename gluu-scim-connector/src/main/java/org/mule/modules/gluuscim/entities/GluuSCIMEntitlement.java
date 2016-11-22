package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

public class GluuSCIMEntitlement implements Serializable{
	
	private static final long serialVersionUID = -1125208405055001907L;

	private String productType;
	private String startDate;
	private String endDate;
	private String canonicalId;
	
	public String getProductType() {
		return productType;
	}
	public void setProductType(String productType) {
		this.productType = productType;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getCanonicalId() {
		return canonicalId;
	}
	public void setCanonicalId(String canonicalId) {
		this.canonicalId = canonicalId;
	}
	
}
