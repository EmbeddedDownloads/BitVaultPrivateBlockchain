package com.pbc.blockchain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BlockContent implements Comparable<BlockContent>, Serializable {

	private static final long serialVersionUID = 5117950191240409824L;
	private String crc;
	private String tag;
	private String pbcId;
	private String hashTxnId;
	private String dataHash;
	private String filePath;
	private String publicAddressOfReciever;
	private String sender;
	private String appId;
	private String sessionKey;
	private long timestamp;
	private String webServerKey;

	public BlockContent() {
		// This is to avoid accidently remove this constructor.
		// Constructor is used implicitly in faster XML library.
	}

	public String getCrc() {
		return crc;
	}

	public void setCrc(final String crc) {
		this.crc = crc;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(final String tag) {
		this.tag = tag;
	}

	public String getPbcId() {
		return pbcId;
	}

	public void setPbcId(final String pbcId) {
		this.pbcId = pbcId;
	}

	public String getHashTxnId() {
		return hashTxnId;
	}

	public void setHashTxnId(final String hashTxnId) {
		this.hashTxnId = hashTxnId;
	}

	public String getDataHash() {
		return dataHash;
	}

	public void setDataHash(final String dataHash) {
		this.dataHash = dataHash;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(final String filePath) {
		this.filePath = filePath;
	}

	public String getPublicAddressOfReciever() {
		return publicAddressOfReciever;
	}

	public void setPublicAddressOfReciever(final String publicAddressOfReciever) {
		this.publicAddressOfReciever = publicAddressOfReciever;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(final String sender) {
		this.sender = sender;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(final String appId) {
		this.appId = appId;
	}

	public String getSessionKey() {
		return sessionKey;
	}

	public void setSessionKey(final String sessionKey) {
		this.sessionKey = sessionKey;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	public String getWebServerKey() {
		return webServerKey;
	}

	public void setWebServerKey(final String webServerKey) {
		this.webServerKey = webServerKey;
	}

	@Override
	public int compareTo(final BlockContent other) {
		if (this.timestamp > other.getTimestamp()) {
			return 1;
		} else if (this.timestamp < other.getTimestamp()) {
			return -1;
		} else {
			return (this.getTag() + this.getHashTxnId()).compareTo(other.getTag() + other.getHashTxnId());
		}
	}

	@Override
	public String toString() {
		return "BlockContent [crc=" + crc + ", tag=" + tag + ", pbcId=" + pbcId + ", hashTxnId=" + hashTxnId
				+ ", dataHash=" + dataHash + ", filePath=" + filePath + ", publicAddressOfReciever="
				+ publicAddressOfReciever + ", sender=" + sender + ", appId=" + appId + ", sessionKey=" + sessionKey
				+ ", timestamp=" + timestamp + ", webServerKey=" + webServerKey + "]";
	}

}
