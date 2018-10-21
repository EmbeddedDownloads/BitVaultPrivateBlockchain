package com.pbc.utility;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class ConfigConstants {

	public static final String FOLDER_PATH = PropertiesReader.getProperty("folderPath");
	public static final int PORT_NO_BLOCK = Integer.parseInt(PropertiesReader.getProperty("portNoBlock"));
	public static final int PORT_NO_DELETE = Integer.parseInt(PropertiesReader.getProperty("portNoDelete"));
	public static final int PORT_NO_SYNCHRONIZATION = Integer
			.parseInt(PropertiesReader.getProperty("portNoSynchronization"));
	public static final int PORT_NO_BLOCK_RECEIVE = Integer
			.parseInt(PropertiesReader.getProperty("portNoBlockReceive"));
	public static final int PORT_FOR_PUSHNOTIFY_RECEIVE = Integer
			.parseInt(PropertiesReader.getProperty("portNoTagTxnPushNotifyReceive"));
	public static final int PORT_FOR_PUSH_SEND = Integer
			.parseInt(PropertiesReader.getProperty("portNoTagTxnPushNotifySend"));
	public static final int PORT_FOR_BLOCK_FILE = Integer
			.parseInt(PropertiesReader.getProperty("portNoFileBlockReceive"));

	public static final int PORT_NO_TAGTXD_SEND = Integer.parseInt(PropertiesReader.getProperty("portNoTagTxnSend"));
	public static final int TOTAL_NODES = Integer.parseInt(PropertiesReader.getProperty("totalNodes"));
	public static final int MIN_NODE_VALIDITY = Integer.parseInt(PropertiesReader.getProperty("minNodeValidity"));
	public static final List<String> NODES = Arrays.asList(PropertiesReader.getProperty("nodes").split(","));
	public static final List<String> SYNCHRONIZATION_NODES = Arrays
			.asList(PropertiesReader.getProperty("synchronizationNodes").split(","));
	public static final String BLOCKCHAIN_CONTROLLER = PropertiesReader.getProperty("blockchain_controller");
	public static final String REPORT_LOG_FILE_PATH = PropertiesReader.getProperty("reportLogFilePath");
	public static final String REPORT_LOG_FILE = PropertiesReader.getProperty("reportLogFile");

	private ConfigConstants() {
		// Preventing from creating object
		throw new UnsupportedOperationException();
	}

	public static class PropertiesReader {
		private final static Logger LOG = Logger.getLogger(PropertiesReader.class);

		private static Properties properties;

		public static Properties getProperties() {
			if (null == properties) {
				LOG.info("Reading properties file.");
				properties = new Properties();
				try {
					properties.load(PropertiesReader.class.getResourceAsStream("/config.properties"));
				} catch (final Exception e) {
				}
			}
			return properties;
		}

		public static String getProperty(final String key) {
			return getProperties().getProperty(key).trim();
		}
	}

}
