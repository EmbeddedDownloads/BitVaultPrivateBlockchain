package com.pbc.job;

import static com.pbc.utility.ConfigConstants.PORT_NO_SYNCHRONIZATION;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.GetSystemIp;

@Component
public class BlockSynchronizationReceiver {

	private static final Logger logger = Logger.getLogger(BlockSynchronizationReceiver.class);

	final ObjectMapper om = new ObjectMapper();

	/**
	 * For blocking this thread to not accept any data when
	 * {@link SyncProcessThread} start working. Don't forget to set true once
	 * {@link SyncProcessThread} working done.
	 */
	public boolean accept = true;

	/**
	 * A map which contains a mapping of host and its PbcSyncModel data.
	 */
	private final Map<String, PbcSyncModel> receivedSyncModelMap = new HashMap<>();

	public Map<String, PbcSyncModel> getReceivedSyncModelMap() {
		return this.receivedSyncModelMap;
	}

	public void doAccept(final boolean accept) {
		this.accept = accept;
	}

	/**
	 * Clear {@link #receivedSyncModelMap} once {@link SyncProcessThread} finished;
	 */
	public void clearReceivedSyncModelMap() {
		receivedSyncModelMap.clear();
	}

	/**
	 * Start listening the port for synchronization.
	 */
	public void listenSynchronizationPort() {
		ServerSocket serverSocket = null;
		try {
			if (available()) {
				serverSocket = new ServerSocket(PORT_NO_SYNCHRONIZATION);
				serverSocket.setReuseAddress(true);
				serverSocket.setSoTimeout(1000 * 60);
			} else {
				logger.error("PBC Sync server socket not closed properly.");
				return;
			}
			while (true) {
				if (accept) {
					final Socket receivedSocket;
					try {
						receivedSocket = serverSocket.accept();
						receivedSocket.setSoTimeout(1000 * 60);
					} catch (final Exception e) {
						continue;
					}
					final Runnable runnable = () -> {
						try (InputStream inputStream = receivedSocket.getInputStream();) {
							final StringBuilder sb = new StringBuilder();
							logger.info("Receiving synchronization data from host :: "
									+ receivedSocket.getInetAddress().getHostAddress());
							parseSyncNotificationData(receivedSocket.getInetAddress().getHostAddress(), inputStream);
							sb.setLength(0);
						} catch (final Exception e) {
							logger.error("Data got some exception " + e.getMessage());
						} finally {
							if (receivedSocket != null) {
								try {
									receivedSocket.close();
								} catch (final Exception e) {
								}
							}
						}
					};
					new Thread(runnable).start();
				}
			}
		} catch (final IOException ioe) {
			logger.error("Server Socket encountered this exception : ", ioe);
		}
	}

	/**
	 * Check availability of some port before connection. This method will be needed
	 * only if server socket is not set to be reusable using
	 * <code>setReuseAddress(true)</code>>.
	 *
	 * @param serverPortNumber
	 * @return
	 */
	private boolean available() {
		try (Socket ignored = new Socket(GetSystemIp.getSystemLocalIp(), ConfigConstants.PORT_NO_SYNCHRONIZATION)) {
			return false;
		} catch (final IOException ignoredException) {
			return true;
		}
	}

	/**
	 * Parse received data and map.
	 *
	 * @param host
	 * @param notificationSyncData
	 */
	private void parseSyncNotificationData(final String host, final InputStream inputStream) {
		logger.info("Received synchronization data");
		try {
			final PbcSyncModel receivedPbcSyncModel = om.readValue(inputStream, PbcSyncModel.class);
			receivedSyncModelMap.put(host, receivedPbcSyncModel);
		} catch (final JsonParseException e) {
			logger.error("Preblem in parsing received sync data ", e);
		} catch (final JsonMappingException e) {
			logger.error("Preblem in mapping received sync data ", e);
		} catch (final IOException e) {
			logger.error("IO problem in parsing received sync data ", e);
		}
	}
}
