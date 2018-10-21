package com.pbc.blockchain;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Block implements Serializable {

	private static final long serialVersionUID = 1452584753723526895L;
	private String blockHash;
	private BlockHeader header;
	private BlockContent blockContent;

	private long createdOn;
	private long updatedOn;

	public Block() {
		// should be used only by ObjectMapper.
	}

	public Block(final BlockHeader header, final BlockContent blockContent) {
		this.header = header;
		this.blockContent = blockContent;
	}

	public BlockHeader getHeader() {
		return header;
	}

	public void setHeader(final BlockHeader header) {
		this.header = header;
	}

	public String getBlockHash() {
		return blockHash;
	}

	public void setBlockHash(final String blockHash) {
		this.blockHash = blockHash;
	}

	public BlockContent getBlockContent() {
		return blockContent;
	}

	public void setBlockContent(final BlockContent blockContent) {
		this.blockContent = blockContent;
	}

	public long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(final long createdOn) {
		this.createdOn = createdOn;
	}

	public long getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(final long updatedOn) {
		this.updatedOn = updatedOn;
	}

	@Override
	public String toString() {
		return "Block [blockHash=" + blockHash + ", header=" + header.toString() + ", blockContent="
				+ blockContent.toString() + ", createdOn=" + createdOn + ", updatedOn=" + updatedOn + "]";
	}

}
