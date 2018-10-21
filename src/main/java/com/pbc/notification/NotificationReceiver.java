package com.pbc.notification;

import static com.pbc.utility.ConfigConstants.MIN_NODE_VALIDITY;
import static com.pbc.utility.ConfigConstants.PORT_NO_BLOCK;
import static com.pbc.utility.ConfigConstants.PORT_NO_DELETE;
import static com.pbc.utility.ConfigConstants.TOTAL_NODES;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.blockchain.ParseableBlockDTO;
import com.pbc.models.BlockStatusEnum;
import com.pbc.models.ConfirmationHelper;
import com.pbc.models.NotificationObject;
import com.pbc.repository.model.BlockStatus;
import com.pbc.service.BlockService;
import com.pbc.service.TransactionMessageService;
import com.pbc.threads.DeleteBlockRunnableTask;
import com.pbc.threads.SaveBlockRunnableTask;
import com.pbc.threads.ThreadPoolUtility;
import com.pbc.utility.GetSystemIp;
import com.pbc.utility.IOFileUtil;
import com.pbc.utility.JSONObjectEnum;
import com.pbc.utility.StringConstants;

@Service("getNotificationService")
public class NotificationReceiver {

	private TransactionMessageService transactionMessageService;

	@Autowired
	private IOFileUtil ioFileUtil;

	@Autowired
	private BlockService blockService;

	private static final Logger logger = Logger.getLogger(NotificationReceiver.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	/**
	 * This block DTO cache will hold DTO cache objects. Whenever the CRC
	 * verification and confirmation is done it will be used to receive the block
	 * info to be saved in Block chain.
	 */
	private static final Map<String, ParseableBlockDTO> blockDTOCache = new ConcurrentHashMap<>();

	public void putBlockDTOInCache(final String combineKey, final ParseableBlockDTO parseableBlockDTO) {
		blockDTOCache.put(combineKey, parseableBlockDTO);
	}

	/**
	 * This variable can be used to stop thread execution in run method. This
	 * approach is recommended in java specification as they deprecated
	 * <code>Thread.stop()</code>
	 */
	public static volatile boolean keepRunning = true;

	public TransactionMessageService getTransactionMessageService() {
		return transactionMessageService;
	}

	@Autowired
	public void setTransactionMessageService(final TransactionMessageService transactionMessageService) {
		this.transactionMessageService = transactionMessageService;
	}

	/**
	 * Start a thread to listen a socket continuously for a provided port number.
	 *
	 * @param portNumber
	 */
	public void listenNotification(final int portNumber) {
		ServerSocket serverSocket = null;
		try {
			if (available(portNumber)) {
				serverSocket = new ServerSocket(portNumber);
				serverSocket.setReuseAddress(true);
			} else {
				logger.error("Server socket was not closed properly. Shut down server properly before moving ahead.");
				return;
			}

			while (keepRunning) {
				final Socket receivedSocket;
				final String currentHost;
				try {
					receivedSocket = serverSocket.accept();
					logger.info("Socket Received with IP just after so timeout::"
							+ (receivedSocket.getInetAddress().getHostAddress()));
					receivedSocket.setSoTimeout(1000 * 60);

				} catch (final Exception e) {
					// Do nothing
					continue;
				}
				logger.info(
						"Socket Received with IP::" + (currentHost = receivedSocket.getInetAddress().getHostAddress()));
				final Runnable listenerThread = () -> {
					try (InputStream inputStream = receivedSocket.getInputStream();
							BufferedReader receivedBuffer = new BufferedReader(new InputStreamReader(inputStream));) {
						final StringBuilder sb = new StringBuilder();
						String input = null;
						while ((input = receivedBuffer.readLine()) != null && keepRunning) {
							sb.append(input);
						}
						if (PORT_NO_BLOCK == portNumber) {
							postProcessingNotification(sb, new String(currentHost));
						} else if (PORT_NO_DELETE == portNumber) {
							postProcessingDeleteNotification(sb, new String(currentHost));
						}
						sb.setLength(0);
					} catch (final Exception e) {
						logger.error("Problem while receiving : " + e.getMessage());
					} finally {
						if (null != receivedSocket) {
							try {
								receivedSocket.close();
							} catch (final Exception e) {
							}
						}
					}
				};
				new Thread(listenerThread).start();
			}
		} catch (final IOException ioe) {
			logger.error("Server Socket encountered this exception : " + ioe.getMessage());
		}
	}

	private void postProcessingNotification(final StringBuilder sb, final String currentHost) {
		final NotificationObject notificationObject = getNotificationObject(sb);
		if (null != notificationObject) {
			final String combinedKey = notificationObject.getTag() + notificationObject.getTransactionId();
			List<String> crcList = TransactionMessageService.getCrcMap().get(combinedKey);
			final List<ConfirmationHelper> confirmationList = getTransactionMessageService().getConfirmationMap()
					.get(combinedKey);
			if (notificationObject.getNotificationType().equals(JSONObjectEnum.VALIDITY)) {
				reportLogger.fatal("To SAVE block  VALIDITY received from node :: " + currentHost);
				if (!currentHost.equals(GetSystemIp.getSystemLocalIp())) {
					getTransactionMessageService().putConfirmationValue(combinedKey, notificationObject.isValid());
				}
				ParseableBlockDTO blockDTO = blockDTOCache.get(combinedKey);
				int helperListSize = 0;
				if (confirmationList != null) {
					helperListSize = confirmationList.size();
				}
				final boolean isToBeCreated = helperListSize >= MIN_NODE_VALIDITY
						&& isBlockProcessable(confirmationList);
				logger.info("Value of is to be created: " + isToBeCreated + " min node validity value "
						+ MIN_NODE_VALIDITY);
				if (isToBeCreated) {
					confirmationList.get(0).setAlreadyProcessed(true);
					if (blockDTO == null) {
						logger.info("Block is about to be created." + combinedKey);
						final BlockStatus blockStatus = new BlockStatus(notificationObject.getTag(),
								notificationObject.getTransactionId(), BlockStatusEnum.BLOCK_TO_BE_CREATED.name());
						if (!blockService.insert(blockStatus)) {
							logger.info("To be created not inserted going to while loop" + combinedKey);
							while ((blockDTO = blockDTOCache.get(combinedKey)) == null) {
								// For atomicity. Similar to CAS operations.
							}
							logger.info("while loop ends::" + combinedKey);
							saveBlock(blockDTO, combinedKey);
						}
					} else {
						logger.info("Block is about to be saved.");
						// save the block and also got API request.
						saveBlock(blockDTO, combinedKey);
					}
				}
				clearMaps(notificationObject.getTag(), notificationObject.getTransactionId(), crcList,
						confirmationList);
			} else {
				reportLogger.fatal("To SAVE block CRC received from node :: " + currentHost);
				// Put CRC values to crcMap if it is not from LocalHost.
				if (!currentHost.equals(GetSystemIp.getSystemLocalIp())) {
					logger.info("crcList before putting value into crcMap :: " + crcList);
					getTransactionMessageService().putCRCValue(combinedKey, notificationObject.getCrc());
					crcList = TransactionMessageService.getCrcMap().get(combinedKey);
					logger.info("crcList after putting value into crcMap :: " + crcList);
					logger.info(
							"Message received was a crc broadcast. Putting it to map of transaction service for combined Key: "
									+ combinedKey);
				} else {
					logger.info(
							"Message received was a crc broadcast, but not putting it to map because of localhost for combined Key: "
									+ combinedKey);
				}
				if (!clearMaps(notificationObject.getTag(), notificationObject.getTransactionId(), crcList,
						confirmationList)) {
					logger.info(
							"clear map is false  " + notificationObject.getTag() + notificationObject.getTransactionId()
									+ " ::crcList=" + crcList + " ::confirmationList:: " + confirmationList);
					getTransactionMessageService().verifyAndBroadCast(false, notificationObject);
				}
			}
		} else {
			logger.info("Unable to read received notification message.");
		}
	}

	public void saveBlock(final ParseableBlockDTO blockDTO, final String combinedKey) {
		final ExecutorService executorService = ThreadPoolUtility.getThreadPool();
		executorService.submit(new SaveBlockRunnableTask(blockService).setParseableBlockDTO(blockDTO));
		logger.info("Block created and saved with combined Key: " + combinedKey);

	}

	public boolean clearMaps(final String tag, final String transactionId, final List<String> crcList,
			final List<ConfirmationHelper> confirmationList) {
		final String combinedKey = tag + transactionId;
		if (crcList != null && confirmationList != null && crcList.size() >= TOTAL_NODES
				&& (blockService.getBlockStatus(tag, transactionId).getStatus().equals(BlockStatusEnum.DELETED.name())
						|| blockService.getBlockStatus(tag, transactionId).getStatus()
								.equals(BlockStatusEnum.SAVED.name()))
				&& confirmationList.size() >= TOTAL_NODES
				&& (confirmationList.get(0).isAlreadyProcessed() || !isBlockProcessable(confirmationList))) {
			confirmationList.get(0).setAlreadyProcessed(true);
			logger.info("combinedKey:: " + combinedKey + " inside clear Maps with values crcs: " + crcList
					+ " ::with confirmations: " + confirmationList);
			blockDTOCache.remove(combinedKey);
			getTransactionMessageService().removeDataWithKey(combinedKey);
			logger.info("Removing data from crc map and confirmation map for combined key: " + combinedKey);
			return true;
		}
		return false;
	}

	private void postProcessingDeleteNotification(final StringBuilder sb, final String currentHost) {
		final NotificationObject notificationObject = getNotificationObject(sb);
		if (null != notificationObject) {
			final String deleteComfirmationKey = notificationObject.getTag() + notificationObject.getTransactionId()
					+ StringConstants.DELETE_TAG;
			final List<ConfirmationHelper> confirmationList = getTransactionMessageService().getConfirmationMap()
					.get(deleteComfirmationKey);
			List<String> crcList = TransactionMessageService.getCrcMap().get(deleteComfirmationKey);
			if (notificationObject.getNotificationType().equals(JSONObjectEnum.VALIDITY)) {
				reportLogger.fatal("To DELETE block VALIDITY received from node :: " + currentHost);
				if (!currentHost.equals(GetSystemIp.getSystemLocalIp())) {
					getTransactionMessageService().putConfirmationValue(deleteComfirmationKey,
							notificationObject.isDelete());
				}

				int helperListSize = 0;
				if (confirmationList != null) {
					helperListSize = confirmationList.size();
				}
				final boolean isToBeDeleted = helperListSize >= MIN_NODE_VALIDITY
						&& isBlockProcessable(confirmationList);
				logger.info("Value of isToBeDeleleted " + isToBeDeleted);
				if (isToBeDeleted) {
					// Save the block as we have got confirmation from three
					// nodes.
					confirmationList.get(0).setAlreadyProcessed(true);
					final ExecutorService executorService = ThreadPoolUtility.getThreadPool();
					executorService.submit(
							new DeleteBlockRunnableTask(blockService, ioFileUtil).setTag(notificationObject.getTag())
									.setTransactionId(notificationObject.getTransactionId()));
					logger.info(
							"Block removed from block chain. Now removing data from crc map and confirmation map for combined key: "
									+ deleteComfirmationKey);
					reportLogger.fatal("Block successfully removed from the Private BlockChain for transaction id : "
							+ notificationObject.getTransactionId());
				}
				clearMaps(notificationObject.getTag(), notificationObject.getTransactionId(), crcList,
						confirmationList);

			} else {
				reportLogger.fatal("To DELETE block CRC received from node :: " + currentHost);
				// Put CRC values to crcMap if it is not from LocalHost.
				if (!currentHost.equals(GetSystemIp.getSystemLocalIp())) {
					logger.info("crcList before putting value into crcMap :: " + crcList);
					getTransactionMessageService().putCRCValue(deleteComfirmationKey, notificationObject.getCrc());
					crcList = TransactionMessageService.getCrcMap().get(deleteComfirmationKey);
					logger.info(
							"Message received was a crc broadcast Putting it to map of transaction service for combined Key: "
									+ deleteComfirmationKey);
				} else {
					logger.info(
							"Message received was a crc broadcast, but not putting it to map because of localhost for combined Key: "
									+ deleteComfirmationKey);
				}
				if (!clearMaps(notificationObject.getTag(), notificationObject.getTransactionId(), crcList,
						confirmationList)) {
					getTransactionMessageService().verifyAndBroadCast(true, notificationObject);
				}
			}
		} else {
			logger.info("Unable to read recieved notification message for delete block.");
		}
	}

	private NotificationObject getNotificationObject(final StringBuilder sb) {
		final ObjectMapper objMapper = new ObjectMapper();
		NotificationObject notificationObject = null;
		try {
			if (null != sb && !sb.toString().isEmpty()) {
				logger.info("Message received from socket is : " + sb.toString());
				notificationObject = objMapper.readValue(sb.toString(), NotificationObject.class);
			}
		} catch (final Exception e) {
			logger.error("A json parsing error occured while recieving notification ", e);
		}
		return notificationObject;
	}

	private boolean isBlockProcessable(final List<ConfirmationHelper> confirmationHelperList) {
		if (confirmationHelperList.get(0).isAlreadyProcessed()) {
			return false;
		}
		int trueCount = 0;
		for (final ConfirmationHelper currentConfirmation : confirmationHelperList) {
			if (Boolean.TRUE.equals(currentConfirmation.isValid())) {
				trueCount++;
			}
		}
		return trueCount >= MIN_NODE_VALIDITY;
	}

	/**
	 * Check availability of some port before connection. This method will be needed
	 * only if server socket is not set to be reusable using
	 * <code>setReuseAddress(true)</code>>.
	 *
	 * @param serverPortNumber
	 * @return
	 */
	private boolean available(final int serverPortNumber) {
		try (Socket ignored = new Socket(GetSystemIp.getSystemLocalIp(), serverPortNumber)) {
			return false;
		} catch (final IOException ignored) {
			return true;
		}
	}
}