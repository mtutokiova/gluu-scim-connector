package org.mule.modules.gluuscim.exception;

public class GluuSCIMServerErrorException extends Exception{

	private static final long serialVersionUID = 1L;

	public GluuSCIMServerErrorException() {
	}

	public GluuSCIMServerErrorException(String message) {
		super(message);
	}

	public GluuSCIMServerErrorException(Throwable cause) {
		super(cause);
	}

	public GluuSCIMServerErrorException(String message, Throwable cause) {
		super(message, cause);
	}
}
