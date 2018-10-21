package com.pbc.blockchain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ParseableBlockDTO implements Serializable {

	private static final long serialVersionUID = 9876543567897L;

	private String crc;
	private String tag;
	private String receiver;
	private String sender;
	private String pbcId;
	private String appId;
	private String transactionId;
	private long timeStamp;
	private String sessionKey;
	private String dataHash;
	private String filePath;
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

	public ParseableBlockDTO setCrc(final String crc) {
		this.crc = crc;
		return this;
	}

	public String getTag() {
		return tag;
	}

	public ParseableBlockDTO setTag(final String tag) {
		this.tag = tag;
		return this;

	}

	public String getReceiver() {
		return receiver;
	}

	public ParseableBlockDTO setReceiver(final String receiver) {
		this.receiver = receiver;
		return this;

	}

	public String getSender() {
		return sender;
	}

	public ParseableBlockDTO setSender(final String sender) {
		this.sender = sender;
		return this;
	}

	public String getPbcId() {
		return pbcId;
	}

	public ParseableBlockDTO setPbcId(final String pbcId) {
		this.pbcId = pbcId;
		return this;

	}

	public String getAppId() {
		return appId;
	}

	public ParseableBlockDTO setAppId(final String appId) {
		this.appId = appId;
		return this;

	}

	public String getTransactionId() {
		return transactionId;
	}

	public ParseableBlockDTO setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
		return this;

	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public ParseableBlockDTO setTimeStamp(final long timeStamp) {
		this.timeStamp = timeStamp;
		return this;

	}

	public String getSessionKey() {
		return sessionKey;
	}

	public ParseableBlockDTO setSessionKey(final String sessionKey) {
		this.sessionKey = sessionKey;
		return this;

	}

	public String getDataHash() {
		return dataHash;
	}

	public ParseableBlockDTO setDataHash(final String dataHash) {
		this.dataHash = dataHash;
		return this;
	}

	public String getFilePath() {
		return filePath;
	}

	public ParseableBlockDTO setFilePath(final String filePath) {
		this.filePath = filePath;
		return this;
	}

	@Override
	public String toString() {
		return "ParseableBlockDTO [crc=" + crc + ", tag=" + tag + ", receiver=" + receiver + ", sender=" + sender
				+ ", pbcId=" + pbcId + ", appId=" + appId + ", transactionId=" + transactionId + ", timeStamp="
				+ timeStamp + ", sessionKey=" + sessionKey + ", dataHash=" + dataHash + ", filePath=" + filePath
				+ ", webServerKey=" + webServerKey + "]";
	}

}
