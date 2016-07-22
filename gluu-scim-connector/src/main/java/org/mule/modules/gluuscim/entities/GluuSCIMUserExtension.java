package org.mule.modules.gluuscim.entities;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GluuSCIMUserExtension implements Serializable{

	private static final long serialVersionUID = 6267136865890403307L;
	
	@JsonProperty(value="printPlusDigital")
	private String[] printPlusDigital;

	public String[] getPrintPlusDigital() {
		return printPlusDigital;
	}

	public void setPrintPlusDigital(String[] printPlusDigital) {
		this.printPlusDigital = printPlusDigital;
	}
	

}
