package com.pbc.models;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CustomSuccessResponse<T> extends CustomResponse<T> {

	private static final long serialVersionUID = -4136633734184716632L;

	private T resultSet;

	public CustomSuccessResponse() {
		// This is a success response.
		super(SUCCESS);
	}

	public T getResultSet() {
		return resultSet;
	}

	public void setResultSet(final T resultSet) {
		this.resultSet = resultSet;
	}

}
