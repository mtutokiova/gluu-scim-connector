package org.mule.modules.gluuscim.exception;

public class GluuSCIMConnectorException extends Exception{

	private static final long serialVersionUID = 1L;

	public GluuSCIMConnectorException() {
	}

	public GluuSCIMConnectorException(String message) {
		super(message);
	}

	public GluuSCIMConnectorException(Throwable cause) {
		super(cause);
	}

	public GluuSCIMConnectorException(String message, Throwable cause) {
		super(message, cause);
	}
}
