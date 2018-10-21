package com.pbc.threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.pbc.exception.BlockProcessingException;
import com.pbc.service.BlockService;
import com.pbc.utility.IOFileUtil;
import com.pbc.utility.StringConstants;

/**
 * This task will convert the file into byte array and write it down to some
 * local folder or in bucket. This task shares a common property with all other
 * tasks in project with modified setters. This modified setters help to remove
 * verbose lines to set the different instance properties.
 *
 */
@Scope("prototype")
@Component
public class ConversionAndSaveCallableTask implements Callable<Map<String, String>> {

	private static final Logger logger = Logger.getLogger(ConversionAndSaveCallableTask.class);

	@Autowired
	private BlockService blockService;

	@Autowired
	private IOFileUtil ioFileUtil;

	private String tag;
	private InputStream file;
	private String transactionId;

	public ConversionAndSaveCallableTask setTag(final String tag) {
		this.tag = tag;
		return this;
	}

	public ConversionAndSaveCallableTask setFile(final InputStream file) {
		this.file = file;
		return this;
	}

	public ConversionAndSaveCallableTask setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	@Override
	public Map<String, String> call() throws Exception {
		// This code block might potentially throw an IOException.
		logger.info("Going inside SaveCallBackTask for key " + tag + transactionId);
		// final byte[] encryptedData = getBytesFromInputStream(file);

		String filePath = null;
		try {
			logger.info("Saving object on disk for combined key: " + tag + transactionId);
			filePath = ioFileUtil.writObjectLocally(file, tag + transactionId);

			// IOException potential code ends here.

		} catch (final BlockProcessingException e) {
			// This message is not saved in bucket.
			// Block status is now not available.
			logger.error("Unable to save file locally ", e);
		}

		final String dataHash = blockService.calculateHash(new FileInputStream(new File(filePath)), "SHA-256");
		logger.info("Calculated data hash ::" + dataHash + " for key " + tag + transactionId);
		// Arrays.fill(encryptedData, (byte) 0);
		final Map<String, String> map = new HashMap<>();
		map.put(StringConstants.HASH_KEY_STRING, dataHash);
		map.put(StringConstants.SAVED_FILE_PATH, filePath);
		return map;
	}

	public byte[] getBytesFromInputStream(final InputStream is) throws IOException {
		final byte[] bytes = IOUtils.toByteArray(is);
		return bytes;
	}
}
