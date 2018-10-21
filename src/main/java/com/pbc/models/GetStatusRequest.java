package com.pbc.models;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

public class GetStatusRequest implements Serializable {

	private static final long serialVersionUID = -4755510234278043971L;

	@NotNull
	private String tag;

	@NotNull
	private String transactionId;

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

	@Override
	public boolean equals(final Object obj) {
		boolean flag = false;
		final GetStatusRequest request = (GetStatusRequest) obj;
		if ((getTag() + getTransactionId()).equals(request.getTag() + request.getTransactionId())) {
			flag = true;
		}
		return flag;
	}

	@Override
	public int hashCode() {
		return getTag().hashCode() + getTransactionId().hashCode();
	}

	@Override
	public String toString() {
		return "GetStatusRequest [tag=" + tag + ", transactionId=" + transactionId + "]";
	}
}
