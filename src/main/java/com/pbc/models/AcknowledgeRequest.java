package com.pbc.models;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AcknowledgeRequest {

	@NotNull
	private String crc;

	@NotNull
	private String tag;

	@NotNull
	private String transactionId;

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

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
	}
}
