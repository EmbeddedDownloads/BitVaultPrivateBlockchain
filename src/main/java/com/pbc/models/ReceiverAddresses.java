package com.pbc.models;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ReceiverAddresses {

	private List<String> receiverAddress;

	public List<String> getReceiverAddress() {
		return receiverAddress;
	}

	public void setReceiverAddress(final List<String> receiverAddress) {
		this.receiverAddress = receiverAddress;
	}
}
