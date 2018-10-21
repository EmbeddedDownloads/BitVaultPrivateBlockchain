package com.pbc.blockchain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class BlockHeader implements Serializable {

	private static final long serialVersionUID = -7507604889588918185L;
	private long timeStamp;
	private String prevHash;

	public BlockHeader() {
		// To be used by Block Header.
	}

	public BlockHeader(final long timeStamp, final String prevHash) {
		this.timeStamp = timeStamp;
		this.prevHash = prevHash;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(final long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getPrevHash() {
		return prevHash;
	}

	public void setPrevHash(final String prevHash) {
		this.prevHash = prevHash;
	}

	@Override
	public String toString() {
		return "BlockHeader [timeStamp=" + timeStamp + ", prevHash=" + prevHash + "]";
	}

}