package com.pbc.job;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.pbc.blockchain.Block;
import com.pbc.models.GetStatusRequest;

@JsonInclude(Include.NON_NULL)
public class SyncBlockDataModel implements Serializable {

	private static final long serialVersionUID = -7215538099280486186L;

	private Block block;
	private Map<GetStatusRequest, String> mapWithStatus;

	public Map<GetStatusRequest, String> getMapWithStatus() {
		return mapWithStatus;
	}

	public void setMapWithStatus(final Map<GetStatusRequest, String> mapWithStatus) {
		this.mapWithStatus = mapWithStatus;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(final Block block) {
		this.block = block;
	}

	@Override
	public String toString() {
		return "SyncBlockDataModel [block=" + block + ",  mapWithStatus=" + mapWithStatus + "]";
	}
}