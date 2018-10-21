package com.pbc.repository;

import java.util.List;

import com.pbc.repository.model.BlockStatus;

public interface BlockStatusDao {

	void insert(final BlockStatus blockStatus);

	void updateStatus(final String tag, final String transactionId, final String status, final String receiver);

	BlockStatus getStatus(final String tag, final String transactionId);

	List<BlockStatus> getStatus(final String[] tagList, final String[] trxnList);

	List<BlockStatus> getBlockList(List<String> list);

	long getTotalBlockCount();

	long getAvailableBlockCount();

	long getDeletedBlockCount();

	List<BlockStatus> getBlockToBeCreatedList();

	BlockStatus getStatusifSaved(String tag, String transactionId);

	List<BlockStatus> getBlockStatusListByPage(final int pageNo);

}