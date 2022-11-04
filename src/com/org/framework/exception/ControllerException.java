package com.org.framework.exception;

public class ControllerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ControllerException() {
	}

	public ControllerException(String msg, Throwable t) {
		super(msg, t);
	}
}
