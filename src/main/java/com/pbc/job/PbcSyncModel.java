package com.pbc.job;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.pbc.blockchain.BlockContent;
import com.pbc.models.ConfirmationHelper;

class PbcSyncModel {

	private Set<BlockContent> orderedBlockSet;
	private Map<String, List<ConfirmationHelper>> confirmationMap;
	private List<String> hashList;

	/**
	 * Strictly prohibited to use default construct for creating object of this
	 * class. Please use parameterized constructor instead. This is defined only
	 * to fulfill the purpose of ObjectMapper.
	 */
	public PbcSyncModel() {
		// Default constructor. For the purpose to resolve ObjectMapper issue.
	}

	public PbcSyncModel(final Set<BlockContent> orderedBlockSet, final Map<String, List<ConfirmationHelper>> map,
			final List<String> hashList) {
		this.orderedBlockSet = orderedBlockSet;
		this.confirmationMap = map;
		this.hashList = hashList;
	}

	public Set<BlockContent> getOrderedBlockSet() {
		return this.orderedBlockSet;
	}

	public Map<String, List<ConfirmationHelper>> getConfirmationMap() {
		return this.confirmationMap;
	}

	public List<String> getHashList() {
		return hashList;
	}

	@Override
	public String toString() {
		return "PbcSyncModel [orderedBlockSet=" + orderedBlockSet + ", confirmationMap=" + confirmationMap
				+ ", hashList=" + hashList + "]";
	}

}