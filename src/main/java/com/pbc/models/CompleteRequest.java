package com.pbc.models;

import java.io.Serializable;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;

import com.pbc.utility.ValidationConstraints;

@XmlRootElement
public class CompleteRequest implements Serializable {

	private static final long serialVersionUID = 4639342509625901594L;

	@NotNull(message = ValidationConstraints.EMPTY_CRC_ERR)
	private String crc;

	@NotNull(message = ValidationConstraints.EMPTY_TRANSACTION_ID_ERR)
	private String transactionId;

	private String tag;

	public CompleteRequest() {
		// Default
	}

	public CompleteRequest(final String crc, final String transactionId, final String tag) {
		this.crc = crc;
		this.transactionId = transactionId;
		this.tag = tag;
	}

	public String getCrc() {
		return crc;
	}

	public void setCrc(final String crc) {
		this.crc = crc;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(final String tag) {
		this.tag = tag;
	}
}
