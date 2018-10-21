package com.pbc.models;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class GetMessageRequest {

	private String tag;
	private String transactionId;
	private String receiverAddress;

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

	public String getReceiverAddress() {
		return receiverAddress;
	}

	public void setReceiverAddress(final String receiverAddress) {
		this.receiverAddress = receiverAddress;
	}

}
