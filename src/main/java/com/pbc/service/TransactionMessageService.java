package com.pbc.service;

import static com.pbc.utility.ConfigConstants.MIN_NODE_VALIDITY;
import static com.pbc.utility.ConfigConstants.TOTAL_NODES;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.pbc.blockchain.Block;
import com.pbc.blockchain.BlockContent;
import com.pbc.blockchain.ParseableBlockDTO;
import com.pbc.exception.ServiceException;
import com.pbc.models.AcknowledgeRequest;
import com.pbc.models.CompleteRequest;
import com.pbc.models.ConfirmationHelper;
import com.pbc.models.NotificationObject;
import com.pbc.notification.NotificationSender;
import com.pbc.threads.ConversionAndSaveCallableTask;
import com.pbc.threads.NotifyNodesRunnableTask;
import com.pbc.threads.ThreadPoolUtility;
import com.pbc.utility.GetSystemIp;
import com.pbc.utility.JSONObjectEnum;
import com.pbc.utility.StringConstants;

@Service
@Scope("prototype")
public class TransactionMessageService {

	private static final Logger logger = Logger.getLogger(TransactionMessageService.class);

	@Autowired
	private NotifyNodesRunnableTask notifyNodesRunnableTask;

	@Autowired
	private ConversionAndSaveCallableTask conversionAndSaveCallableTask;

	@Autowired
	private NotificationSender notificationSender;

	private static Map<String, List<ConfirmationHelper>> confirmationMap = new ConcurrentHashMap<>();
	private static Map<String, List<String>> crcMap = new ConcurrentHashMap<>();
	private final static String CRC_SEPARATOR = "|$$|";

	public static Map<String, List<String>> getCrcMap() {
		return crcMap;
	}

	public static void setCrcMap(final Map<String, List<String>> crcMap) {
		TransactionMessageService.crcMap = crcMap;
	}

	/**
	 * This method saves data into bucket or locally somewhere and then validate
	 * the crc. This save operation is necessary because we can not keep data in
	 * memory for a long time;
	 *
	 * @param parseableBlockDTO
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public boolean parseRequestAndValidate(final ParseableBlockDTO parseableBlockDTO, final InputStream file) {
		try {
			final ExecutorService executorService = ThreadPoolUtility.getThreadPool();
			final Future<Map<String, String>> futureStrings = executorService
					.submit(conversionAndSaveCallableTask.setFile(file).setTag(parseableBlockDTO.getTag())
							.setTransactionId(parseableBlockDTO.getTransactionId()));
			try {
				final Map<String, String> hashAndFilePath = futureStrings.get();
				parseableBlockDTO.setDataHash(hashAndFilePath.get(StringConstants.HASH_KEY_STRING));
				parseableBlockDTO.setFilePath(hashAndFilePath.get(StringConstants.SAVED_FILE_PATH));
			} catch (InterruptedException | ExecutionException e) {
				logger.error("Executor pool was interruped or there was some execution exception.", e);
			}
			// Start CRC validation.
			final String calculatedCrc = getCRC(createCombineString(parseableBlockDTO).getBytes());
			logger.info("Incoming crc: " + parseableBlockDTO.getCrc() + " : and Calculated CRC: " + calculatedCrc);
			if (parseableBlockDTO.getCrc().equals(calculatedCrc)) {
				logger.info("CRC is valid for given combined key: " + parseableBlockDTO.getTag()
						+ parseableBlockDTO.getTransactionId());
				return true;
			}
			logger.warn("CRC is not valid for given combined key: " + parseableBlockDTO.getTag()
					+ parseableBlockDTO.getTransactionId());
			return false;
		} catch (final Exception e) {
			logger.error("Problem in validating request ", e);
			throw new ServiceException("Problem in validating request ", e);
		}
	}

	private String createCombineString(final ParseableBlockDTO parseableBlockDTO) {
		logger.info("Parsable dto at the time of crc calculation" + parseableBlockDTO.toString());
		final StringBuilder sb = new StringBuilder();
		sb.append(parseableBlockDTO.getTag()).append(CRC_SEPARATOR).append(parseableBlockDTO.getTransactionId())
				.append(CRC_SEPARATOR).append(parseableBlockDTO.getReceiver()).append(CRC_SEPARATOR)
				.append(parseableBlockDTO.getDataHash()).append(CRC_SEPARATOR).append(parseableBlockDTO.getSessionKey())
				.append(CRC_SEPARATOR).append(parseableBlockDTO.getPbcId()).append(CRC_SEPARATOR)
				.append(parseableBlockDTO.getAppId()).append(CRC_SEPARATOR).append(parseableBlockDTO.getTimeStamp())
				.append(CRC_SEPARATOR).append(parseableBlockDTO.getSender()).append(CRC_SEPARATOR)
				.append(parseableBlockDTO.getWebServerKey());
		logger.info("Combining strings for crc generation:: " + sb.toString());
		return sb.toString();
	}

	/* Generate CRC */
	public String getCRC(final byte[] textBytes) {
		final Checksum checksum = new CRC32();
		checksum.update(textBytes, 0, textBytes.length);
		final long crcOfMessageTxnID = checksum.getValue();
		Arrays.fill(textBytes, (byte) 0);
		return Long.toHexString(crcOfMessageTxnID);
	}

	public void putConfirmationValue(final String tagAndTransactionId, final Boolean val) {
		List<ConfirmationHelper> current = confirmationMap.get(tagAndTransactionId);
		if (current == null) {
			current = new ArrayList<>();
		}
		current.add(new ConfirmationHelper(null, val));
		confirmationMap.put(tagAndTransactionId, current);
	}

	public void putCRCValue(final String tagAndTransactionId, final String crc) {
		List<String> current = getCrcMap().get(tagAndTransactionId);
		if (current == null) {
			current = new ArrayList<>();
		}
		current.add(crc);
		getCrcMap().put(tagAndTransactionId, current);
	}

	public Map<String, List<ConfirmationHelper>> getConfirmationMap() {
		return Collections.unmodifiableMap(confirmationMap);
	}

	public static int getConfirmationMapSize() {
		return confirmationMap.size();
	}

	public void verifyAndBroadCast(final boolean isDelete, final NotificationObject notificationObject) {
		String combinedKey = notificationObject.getTag() + notificationObject.getTransactionId();
		if (isDelete) {
			combinedKey = combinedKey + StringConstants.DELETE_TAG;
		}
		final List<String> crcValues = getCrcMap().get(combinedKey);
		logger.info("Crc map size:: " + crcValues + " at the time of validity confirmation for tag:: " + combinedKey);
		if (crcValues != null && crcValues.size() >= MIN_NODE_VALIDITY && crcValues.size() <= TOTAL_NODES) {
			final CompleteRequest completeRequest = new CompleteRequest();
			completeRequest.setTransactionId(notificationObject.getTransactionId());
			completeRequest.setCrc(notificationObject.getCrc());
			completeRequest.setTag(notificationObject.getTag());
			final boolean isVerifyCRC = verifyCRCs(crcValues);
			final boolean allCRCsVerified = isNotNotifiedAlready(combinedKey) && isVerifyCRC;
			logger.info("allCRCsVerified:: " + allCRCsVerified + " isVerifyCRC:: " + isVerifyCRC
					+ notificationObject.getTag() + notificationObject.getTransactionId());
			if (allCRCsVerified) {
				notificationSender.setCompleteRequest(completeRequest);
				putConfirmationValue(combinedKey, allCRCsVerified);
				logger.info("size of confirmation map and crc before writing notification::"
						+ getConfirmationMap().get(combinedKey).size() + " :: " + getCrcMap().get(combinedKey).size()
						+ " for key " + combinedKey);
				notificationSender.notifyAllHosts(isDelete, JSONObjectEnum.VALIDITY, allCRCsVerified);
				confirmationMap.get(combinedKey).get(0).setHostName(GetSystemIp.getSystemLocalIp());
				logger.info("All crcs verified. Broadcasting confirmation to other nodes for key " + combinedKey);
			} else if (crcValues.size() >= TOTAL_NODES && !isVerifyCRC) {
				logger.info("verifying crc false and crc values are equals total nodes "
						+ getConfirmationMap().get(combinedKey).size() + " for key " + combinedKey);
				notificationSender.notifyAllHosts(isDelete, JSONObjectEnum.VALIDITY, allCRCsVerified);
				putConfirmationValue(combinedKey, allCRCsVerified);
			}
		} else {
			logger.info("Waiting for more crcs from other nodes for key " + combinedKey);
		}
	}

	private boolean isNotNotifiedAlready(final String combinedKey) {
		final List<ConfirmationHelper> listOfConfirmation = confirmationMap.get(combinedKey);
		if (listOfConfirmation == null || listOfConfirmation.get(0).getHostName() == null) {
			return true;
		}
		return !listOfConfirmation.get(0).getHostName().equals(GetSystemIp.getSystemLocalIp());
	}

	private boolean verifyCRCs(final List<String> crcValues) {
		final Map<String, Integer> mapForCount = new HashMap<>();
		for (final String str : crcValues) {
			Integer count = mapForCount.get(str);
			if (count == null) {
				mapForCount.put(str, (count = 1));
			} else {
				mapForCount.put(str, (count = count + 1));
			}
			if (count.intValue() == MIN_NODE_VALIDITY) {
				return true;
			}
		}
		return false;
	}

	public boolean getConfirmationMajority(final String hashOfTransactionId) {
		final List<ConfirmationHelper> confirmationHelperList = confirmationMap.get(hashOfTransactionId);
		int trueCount = 0, falseCount = 0;
		for (final ConfirmationHelper currentConfirmation : confirmationHelperList) {
			if (Boolean.TRUE.equals(currentConfirmation.isValid())) {
				trueCount++;
			} else {
				falseCount++;
			}
		}
		return trueCount > falseCount;
	}

	public void removeDataWithKey(final String combinedKey) {
		logger.info("Removing data from confirmation map and crc map for given key: " + combinedKey);
		confirmationMap.remove(combinedKey);
		getCrcMap().remove(combinedKey);
	}

	/**
	 * It verify the CRC.
	 *
	 * @param block
	 * @param ackRequest
	 * @return boolean
	 */
	public boolean verifyCRCAndDelete(final Block block, final AcknowledgeRequest ackRequest) {
		try {
			if (null == block) {
				throw new ServiceException("Block is not received at this node yet.");
			}
			final BlockContent blockContent = block.getBlockContent();
			final ParseableBlockDTO parseableBlockDTO = new ParseableBlockDTO();
			parseableBlockDTO.setCrc(blockContent.getCrc()).setTransactionId(blockContent.getHashTxnId())
					.setTag(blockContent.getTag()).setDataHash(blockContent.getDataHash())
					.setFilePath(blockContent.getFilePath()).setPbcId(blockContent.getPbcId())
					.setAppId(blockContent.getAppId()).setSessionKey(blockContent.getSessionKey())
					.setTimeStamp(blockContent.getTimestamp()).setReceiver(blockContent.getPublicAddressOfReciever())
					.setSender(blockContent.getSender()).setWebServerKey(blockContent.getWebServerKey());

			final String combinedString = createCombineString(parseableBlockDTO);
			logger.info("Combined string for generating CRC for deletion:: " + combinedString + " for key "
					+ ackRequest.getTag() + ackRequest.getTransactionId());
			final String calculatedCRC = getCRC(combinedString.getBytes());
			logger.info("For Deletion calculated CRC:: " + calculatedCRC + " incoming crc:: " + ackRequest.getCrc()
					+ " for key " + ackRequest.getTag() + ackRequest.getTransactionId());
			if (calculatedCRC.equals(ackRequest.getCrc())) {
				logger.info("Putting own block delete crc into crcpMap " + getCrcMap()
						.get(ackRequest.getTag() + ackRequest.getTransactionId() + StringConstants.DELETE_TAG));
				return true;
			}
			// This condition is not clear with what to be done.
			return false;
		} catch (final Exception e) {
			logger.error("Unable to verify CRC.", e);
			throw new ServiceException("Unable to verify CRC.", e);
		}
	}

	/**
	 * Execute a task to notify all the nodes.
	 *
	 * @param crc
	 * @param tag
	 * @param transactionId
	 */
	public void createTaskToNotify(final String crc, final String tag, final String transactionId,
			final boolean isDelete) {
		try {
			logger.info("Inside create task to notify:: " + tag + transactionId);
			final ExecutorService executorService = ThreadPoolUtility.getThreadPool();
			executorService.execute(notifyNodesRunnableTask.setDelete(isDelete).setCrc(crc).setTag(tag)
					.setTransactionId(transactionId));
		} catch (final RejectedExecutionException ree) {
			logger.error("Task can't be accepted to execute ", ree);
			throw new ServiceException("Task can't be accepted to execute ", ree);
		} catch (final NullPointerException npe) {
			logger.error("Unable to start executor service ", npe);
			throw new ServiceException("Unable to start executor service ", npe);
		} catch (final Exception e) {
			logger.error("Unable to start executor service ", e);
			throw new ServiceException("Unable to start executor service ", e);
		}
	}
}