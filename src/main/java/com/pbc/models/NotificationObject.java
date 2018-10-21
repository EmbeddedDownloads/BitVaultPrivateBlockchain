package com.pbc.models;

import javax.xml.bind.annotation.XmlRootElement;

import com.pbc.utility.JSONObjectEnum;

@XmlRootElement
public class NotificationObject {

	private String crc;
	private String tag;
	private boolean valid;
	private boolean isDelete;
	private String transactionId;
	private JSONObjectEnum notificationType;

	public NotificationObject() {
		// This constructor is needed for conversion from
		// string to json object.
	}

	public NotificationObject(final String transactionId, final String crc) {
		this.notificationType = JSONObjectEnum.CRC;
		this.transactionId = transactionId;
		this.crc = crc;
	}

	public NotificationObject(final String transactionId, final boolean valid) {
		this.notificationType = JSONObjectEnum.VALIDITY;
		this.transactionId = transactionId;
		this.valid = valid;
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

	public boolean isValid() {
		return valid;
	}

	public void setValid(final boolean valid) {
		this.valid = valid;
	}

	public boolean isDelete() {
		return isDelete;
	}

	public void setDelete(final boolean isDelete) {
		this.isDelete = isDelete;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public void setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
	}

	public JSONObjectEnum getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(final JSONObjectEnum notificationType) {
		this.notificationType = notificationType;
	}
}