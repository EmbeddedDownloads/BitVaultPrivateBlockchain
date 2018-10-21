package com.pbc.push_notification.models;

import java.io.Serializable;

public class PushNotificationDTO implements Serializable {

	private static final long serialVersionUID = -4943712935106239290L;

	private String web_server_key;
	private String sender_address;
	private String receiver_address;
	private String tag;
	private String data;
	private String transaction_id;

	public String getTransaction_id() {
		return transaction_id;
	}

	public PushNotificationDTO setTransaction_id(final String transaction_id) {
		this.transaction_id = transaction_id;
		return this;
	}

	public String getSender_address() {
		return sender_address;
	}

	public PushNotificationDTO setSender_address(final String sender_address) {
		this.sender_address = sender_address;
		return this;
	}

	public String getReceiver_address() {
		return receiver_address;
	}

	public PushNotificationDTO setReceiver_address(final String receiver_address) {
		this.receiver_address = receiver_address;
		return this;
	}

	public String getWeb_server_key() {
		return web_server_key;
	}

	public PushNotificationDTO setWeb_server_key(final String web_server_key) {
		this.web_server_key = web_server_key;
		return this;
	}

	public String getTag() {
		return tag;
	}

	public PushNotificationDTO setTag(final String tag) {
		this.tag = tag;
		return this;
	}

	public String getData() {
		return data;
	}

	public PushNotificationDTO setData(final String data) {
		this.data = data;
		return this;
	}

	@Override
	public String toString() {
		return "PushNotificationDTO [web_server_key=" + web_server_key + ", sender_address=" + sender_address
				+ ", receiver_address=" + receiver_address + ", tag=" + tag + ", data=" + data + ", transaction_id="
				+ transaction_id + "]";
	}

}