package com.pbc.models;

import java.io.Serializable;
import java.util.List;

import com.pbc.repository.model.BlockStatus;

public class StatisticsModel implements Serializable {

	private static final long serialVersionUID = 520336949963669339L;

	private long totalBlocks;
	private long savedCount;
	private long deletedCount;
	private List<BlockStatus> blocks;

	public long getTotalBlocks() {
		return totalBlocks;
	}

	public StatisticsModel setTotalBlocks(final long totalBlocks) {
		this.totalBlocks = totalBlocks;
		return this;
	}

	public long getSavedCount() {
		return savedCount;
	}

	public StatisticsModel setSavedCount(final long savedCount) {
		this.savedCount = savedCount;
		return this;
	}

	public long getDeletedCount() {
		return deletedCount;
	}

	public StatisticsModel setDeletedCount(final long deletedCount) {
		this.deletedCount = deletedCount;
		return this;
	}

	public List<BlockStatus> getBlocks() {
		return blocks;
	}

	public StatisticsModel setBlocks(final List<BlockStatus> blocks) {
		this.blocks = blocks;
		return this;
	}

}
