package com.pbc.utility;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.pbc.utility.ConfigConstants.PropertiesReader;

public class CustomMessageConstants {

	public static final String NO_MESSAGE_FOR_TXNID = MessagePropertiesReader.getProperty("noMsgForTxId");
	public static final String MSG_BLOCK_RETURN_SUCCESSFULLY = MessagePropertiesReader
			.getProperty("msgBlockReturnSuccessfullly");
	public static final String MSG_ALREADY_RECEIVED = MessagePropertiesReader.getProperty("msgAlreadyReceived");
	public static final String FILE_COULD_NOT_SAVED = MessagePropertiesReader.getProperty("fileCouldNotSaved");
	public static final String FILE_UPLOADED_SUCCESSFULLY = MessagePropertiesReader
			.getProperty("fileUploadedSuccessfully");
	public static final String BLOCK_DELETED_SUCCESSFULLY = MessagePropertiesReader
			.getProperty("blockDeletedSuccessfully");
	public static final String BLOCK_ALREADY_DELETED = MessagePropertiesReader.getProperty("blockAlreadyDeleted");
	public static final String MSG_NOT_VALID = MessagePropertiesReader.getProperty("msgNotValid");
	public static final String TXN_ALREADY_EXIST = MessagePropertiesReader.getProperty("txnIdAlreadyExsist");
	public static final String MSG_VALID = MessagePropertiesReader.getProperty("msgValid");
	public static final String ERR_IN_CREATE_BLOCK = MessagePropertiesReader.getProperty("errInCreateBlock");
	public static final String TXN_EXIST_ALREADY = MessagePropertiesReader.getProperty("txnexistalready");
	public static final String RECEIVER_ADDR_VALIDATION = MessagePropertiesReader.getProperty("receiverAddrValidation");
	public static final String NO_MESSAGE = MessagePropertiesReader.getProperty("noMessage");
	public static final String STR_STATUS_MESSAGE = MessagePropertiesReader.getProperty("strStatusMessage");
	public static final String STR_IS = MessagePropertiesReader.getProperty("strIs");
	public static final String DELETED_ALRDY_RECEIVED = MessagePropertiesReader.getProperty("deleteAlreadyReceived");

	public static class MessagePropertiesReader {
		private final static Logger LOG = Logger.getLogger(PropertiesReader.class);

		private static Properties properties;

		public static Properties getProperties() {
			if (null == properties) {
				LOG.info("Reading properties file.");
				properties = new Properties();
				try {
					properties.load(PropertiesReader.class.getResourceAsStream("/message_config.properties"));
				} catch (final Exception e) {
					LOG.error("Property File not loaded " + e);
				}
			}
			return properties;
		}

		public static String getProperty(final String key) {
			return getProperties().getProperty(key).trim();
		}
	}
}
