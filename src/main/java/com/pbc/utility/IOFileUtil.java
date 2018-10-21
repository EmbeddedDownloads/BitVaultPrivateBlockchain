package com.pbc.utility;

import static com.pbc.utility.ConfigConstants.FOLDER_PATH;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pbc.exception.BlockProcessingException;
import com.pbc.exception.PBCException;
import com.pbc.service.BlockService;

@Component
public class IOFileUtil {

	private static final Logger logger = Logger.getLogger(IOFileUtil.class);

	@Autowired
	private BlockService blockService;

	/**
	 * Method can be used throughout the application for object persistence.
	 * second parameter which is filename should be unique. For custom request
	 * it should be transactionId.
	 *
	 * @param serializable
	 *            object.
	 * @param fileName
	 *            TransactionId for uniqueness.
	 * @throws PBCException
	 * @return
	 */
	public <T extends Serializable> String writObjectLocally(final byte[] dataBytes, final String fileName) {
		FileOutputStream fos;
		final String completeFilePath = getCompletePath(fileName, getCorrectFolderPath());
		try {
			fos = new FileOutputStream(completeFilePath);
			fos.write(dataBytes);
			fos.close();
		} catch (final IOException e) {
			logger.error("Object could not be saved on local files because of some I/O error", e);
			throw new BlockProcessingException("Object could not be saved on local files", e);
		}
		return completeFilePath;
	}

	public String writObjectLocally(final InputStream stream, final String fileName) {
		final String completeFilePath = getCompletePath(fileName, getCorrectFolderPath());
		try {
			final File targetFile = new File(completeFilePath);
			final OutputStream outStream = new FileOutputStream(targetFile);
			final byte[] buffer = new byte[8 * 1024];
			int bytesRead;
			while ((bytesRead = stream.read(buffer)) != -1) {
				outStream.write(buffer, 0, bytesRead);
			}
			IOUtils.closeQuietly(outStream);
		} catch (final Exception e) {
			logger.error("Object could not be saved on local files because of some I/O error", e);
			throw new BlockProcessingException("Object could not be saved on local files", e);
		}
		return completeFilePath;
	}

	/**
	 * This method returns correct folder path used for saving objects. This
	 * method solves common errors involving extra(or absence) of "/" in folder
	 * path. Also it tries to make platform independent path using
	 * File.separator.
	 *
	 * @return
	 */
	public String getCorrectFolderPath() {
		if (FOLDER_PATH.endsWith(File.separator)) {
			return FOLDER_PATH;
		}
		return FOLDER_PATH + File.separator;
	}

	public String getCompletePath(final String fileName, final String folderPath) {
		return folderPath + blockService.calculateHash(fileName.getBytes(), "MD5")
				+ StringConstants.TEXT_FILE_EXTENSION;
	}
}
