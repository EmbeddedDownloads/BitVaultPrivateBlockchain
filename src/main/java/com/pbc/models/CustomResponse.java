package com.pbc.models;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CustomResponse<T> implements Serializable {

	private static final long serialVersionUID = 2366225947274619785L;

	// Response string constants
	public static final String SUCCESS = "success";
	public static final String ERROR = "error";

	private String status;

	private String message;

	public CustomResponse() {
		// default and to be used internally.
	}

	public CustomResponse(final String status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public CustomResponse<T> setMessage(final String message) {
		this.message = message;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public CustomResponse<T> setStatus(final String status) {
		this.status = status;
		return this;
	}

	@Override
	public String toString() {
		return "CustomResponse [status=" + status + ", message=" + message + "]";
	}
}