package com.pbc.models;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CustomErrorResponse<T> extends CustomResponse<T> {

	private static final long serialVersionUID = 1170900728123614455L;

	private T errors;

	public CustomErrorResponse() {
		// error response.
		super(ERROR);
	}

	public CustomErrorResponse(final T errors) {
		super(ERROR);
		this.errors = errors;
	}

	public T getResultSet() {
		return errors;
	}

	public void setResultSet(final T errors) {
		this.errors = errors;
	}
}
