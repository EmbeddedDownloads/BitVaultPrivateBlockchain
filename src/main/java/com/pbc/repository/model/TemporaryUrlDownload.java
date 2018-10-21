package com.pbc.repository.model;

public class TemporaryUrlDownload {

	private String uuid;
	private String filePath;
	private boolean status;
	private String dataHash;

	public TemporaryUrlDownload(final String uuid, final String filePath, final boolean status, final String dataHash) {
		this.uuid = uuid;
		this.filePath = filePath;
		this.status = status;
		this.dataHash = dataHash;
	}

	public String getUuid() {
		return uuid;
	}

	public TemporaryUrlDownload setUuid(final String uuid) {
		this.uuid = uuid;
		return this;
	}

	public String getFilePath() {
		return filePath;
	}

	public TemporaryUrlDownload setFilePath(final String filePath) {
		this.filePath = filePath;
		return this;
	}

	public boolean getStatus() {
		return status;
	}

	public TemporaryUrlDownload setStatus(final boolean status) {
		this.status = status;
		return this;
	}

	public String getDataHash() {
		return dataHash;
	}

	public TemporaryUrlDownload setDataHash(final String dataHash) {
		this.dataHash = dataHash;
		return this;
	}
}
