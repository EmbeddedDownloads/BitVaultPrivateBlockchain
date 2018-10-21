package com.pbc.exception;

public class BlockProcessingException extends RuntimeException {

	private static final long serialVersionUID = 6299098100299296277L;

	public BlockProcessingException() {
	}

	public BlockProcessingException(final String message) {
		super(message);
	}

	public BlockProcessingException(final Throwable cause) {
		super(cause);
	}

	public BlockProcessingException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public BlockProcessingException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
