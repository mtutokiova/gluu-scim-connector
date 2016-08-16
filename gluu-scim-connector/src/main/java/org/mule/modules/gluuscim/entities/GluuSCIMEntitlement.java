package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

public class GluuSCIMEntitlement implements Serializable{
	
	private static final long serialVersionUID = -1125208405055001907L;

	private String productName;
	private String productCode;
	private String startDate;
	private String endDate;
	
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getProductCode() {
		return productCode;
	}
	public void setProductCode(String productCode) {
		this.productCode = productCode;
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
	
}
