package com.pbc.repository.model;

import java.io.Serializable;
import java.util.Date;

public class BlockStatus implements Serializable {

	private static final long serialVersionUID = -2459431092416280876L;

	private String transactionId;
	private String tag;
	private String receiverAddress;
	private String status;
	private Date createdAt;
	private Date updatedAt;

	public BlockStatus() {
		// Default
	}

	public BlockStatus(final String tag, final String transactionId, final String status) {
		this.transactionId = transactionId;
		this.status = status;
		this.tag = tag;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public BlockStatus setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	public String getTag() {
		return tag;
	}

	public BlockStatus setTag(final String tag) {
		this.tag = tag;
		return this;
	}

	public String getReceiverAddress() {
		return receiverAddress;
	}

	public BlockStatus setReceiverAddress(final String receiverAddress) {
		this.receiverAddress = receiverAddress;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public BlockStatus setStatus(final String status) {
		this.status = status;
		return this;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public BlockStatus setCreatedAt(final Date createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public Date getUpdatedAt() {
		return updatedAt;
	}

	public BlockStatus setUpdatedAt(final Date updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}
}
