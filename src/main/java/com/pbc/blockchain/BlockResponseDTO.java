package com.pbc.blockchain;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BlockResponseDTO {

	private String crc;
	private String tag;
	private String receiver;
	private String sender;
	private String transactionId;
	private String fileId;
	private String pbcId;
	private String appId;
	private String sessionKey;
	private long timestamp;
	private String webServerKey;

	public String getWebServerKey() {
		return webServerKey;
	}

	public void setWebServerKey(final String webServerKey) {
		this.webServerKey = webServerKey;
	}

	public String getCrc() {
		return crc;
	}

	public BlockResponseDTO setCrc(final String crc) {
		this.crc = crc;
		return this;
	}

	public String getTag() {
		return tag;
	}

	public BlockResponseDTO setTag(final String tag) {
		this.tag = tag;
		return this;
	}

	public String getReceiver() {
		return receiver;
	}

	public BlockResponseDTO setReceiver(final String receiver) {
		this.receiver = receiver;
		return this;
	}

	public String getSender() {
		return sender;
	}

	public BlockResponseDTO setSender(final String sender) {
		this.sender = sender;
		return this;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public BlockResponseDTO setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	public String getFileId() {
		return fileId;
	}

	public BlockResponseDTO setFileId(final String fileId) {
		this.fileId = fileId;
		return this;
	}

	public String getPbcId() {
		return pbcId;
	}

	public BlockResponseDTO setPbcId(final String pbcId) {
		this.pbcId = pbcId;
		return this;
	}

	public String getAppId() {
		return appId;
	}

	public BlockResponseDTO setAppId(final String appId) {
		this.appId = appId;
		return this;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public BlockResponseDTO setSessionKey(final String sessionKey) {
		this.sessionKey = sessionKey;
		return this;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public BlockResponseDTO setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
		return this;
	}

}
