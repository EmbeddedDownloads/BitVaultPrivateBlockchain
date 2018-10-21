package com.pbc.models;

/**
 * This object will be associated with String in confirmation map. The main
 * purpose of this class is to track that if the already confirmation is already
 * done from this node. Also it helps to detect the need to save the block. If
 * the block is already saved then we don't need to save it again.
 *
 *
 */

public class ConfirmationHelper {

	private String hostName;
	private boolean valid;
	private boolean alreadyProcessed;

	public ConfirmationHelper() {
		// Default
	}

	public ConfirmationHelper(final String hostName, final boolean valid) {
		this.hostName = hostName;
		this.valid = valid;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(final String hostName) {
		this.hostName = hostName;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(final boolean valid) {
		this.valid = valid;
	}

	public synchronized boolean isAlreadyProcessed() {
		return alreadyProcessed;
	}

	public synchronized void setAlreadyProcessed(final boolean alreadyProcessed) {
		this.alreadyProcessed = alreadyProcessed;
	}
}
