package com.pbc.threads;

import org.apache.log4j.Logger;

import com.pbc.blockchain.ParseableBlockDTO;
import com.pbc.service.BlockService;

/**
 * Simple runnable task to add the block in java. This task shares a common
 * property with all other tasks in project with modified setters. This modified
 * setters help to remove verbose lines to set the different instance
 * properties.
 *
 */

public class SaveBlockRunnableTask implements Runnable {

	private final BlockService blockService;

	private static final Logger logger = Logger.getLogger(SaveBlockRunnableTask.class);

	private ParseableBlockDTO parseableBlockDTO;

	public SaveBlockRunnableTask setParseableBlockDTO(final ParseableBlockDTO parseableBlockDTO) {
		this.parseableBlockDTO = parseableBlockDTO;
		return this;
	}

	public SaveBlockRunnableTask(final BlockService blockSerive) {
		this.blockService = blockSerive;
	}

	@Override
	public void run() {
		try {
			logger.info("Block creation start inside saveblockrunnabletask: " + parseableBlockDTO.getTag()
					+ parseableBlockDTO.getTransactionId());
			blockService.createAndSaveBlock(parseableBlockDTO);
			logger.info("Block created and saved for combinedKey: " + parseableBlockDTO.getTag()
					+ parseableBlockDTO.getTransactionId());
		} catch (final Exception e) {
			logger.error("Error while saving data for combined key: " + parseableBlockDTO.getTag()
					+ parseableBlockDTO.getTransactionId(), e);
		}
	}
}
