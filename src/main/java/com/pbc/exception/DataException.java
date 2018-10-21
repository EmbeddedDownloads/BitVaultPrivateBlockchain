package com.pbc.exception;

public class DataException extends RuntimeException {

	private static final long serialVersionUID = -1972168144934080477L;

	public DataException() {
		// Default
	}

	public DataException(final String message) {
		super(message);
	}

	public DataException(final Throwable cause) {
		super(cause);
	}

	public DataException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DataException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
