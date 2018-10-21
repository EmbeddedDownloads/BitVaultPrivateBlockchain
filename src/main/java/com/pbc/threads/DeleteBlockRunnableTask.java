package com.pbc.threads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

import com.pbc.service.BlockService;
import com.pbc.utility.IOFileUtil;
import com.pbc.utility.StringConstants;

/**
 * This task will delete the block from block chain and local folder. This task
 * shares a common property with all other tasks in project with modified
 * setters. This modified setters help to remove verbose lines to set the
 * different instance properties.
 *
 *
 */

public class DeleteBlockRunnableTask implements Runnable {

	private static final Logger logger = Logger.getLogger(DeleteBlockRunnableTask.class);

	private final BlockService blockService;

	private final IOFileUtil ioFileUtil;

	private String tag;
	private String transactionId;

	public DeleteBlockRunnableTask(final BlockService blockService, final IOFileUtil ioFileUtil) {
		this.blockService = blockService;
		this.ioFileUtil = ioFileUtil;
	}

	public DeleteBlockRunnableTask setTag(final String tag) {
		this.tag = tag;
		return this;
	}

	public DeleteBlockRunnableTask setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	@Override
	public void run() {
		logger.info("DeleteBlockrunnableTask started" + DeleteBlockRunnableTask.class + "for txId:::" + tag
				+ transactionId);
		blockService.removeBlock(tag, transactionId);
		deleteFileLocally();
	}

	private void deleteFileLocally() {
		try {
			Files.deleteIfExists(Paths.get(ioFileUtil.getCorrectFolderPath()
					+ blockService.calculateHash((tag + transactionId).getBytes(), "MD5")
					+ StringConstants.TEXT_FILE_EXTENSION));
			logger.info("File deleted successfully for combined key: " + tag + transactionId);
		} catch (final IOException ioe) {
			logger.error("File could not be deleted because of following error.", ioe);
		}
	}
}
