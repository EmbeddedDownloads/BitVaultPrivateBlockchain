package com.pbc.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pbc.models.BlockStatusEnum;
import com.pbc.models.GetStatusRequest;
import com.pbc.service.BlockService;
import com.pbc.threads.DeleteBlockRunnableTask;
import com.pbc.threads.ThreadPoolUtility;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.GetSystemIp;
import com.pbc.utility.IOFileUtil;
import com.pbc.utility.StringConstants;

@Component
public class ProcessAllMapReceived implements Runnable {

	private static final Logger logger = Logger.getLogger(ProcessAllMapReceived.class);

	@Autowired
	private IOFileUtil ioFileUtil;

	@Autowired
	private BlockService blockService;

	@Autowired
	private SyncProcessThread syncProcessThread;

	@Autowired
	private BlockDataReceiverThread blockDataReceiverThread;

	private final Long waitForInterval = 1000 * 60 * 2L;

	private final Map<GetStatusRequest, List<String>> mapTxnIdAndStatus = new ConcurrentHashMap<>();
	private final Map<GetStatusRequest, List<String>> mapTxnHost = new ConcurrentHashMap<>();
	private final List<GetStatusRequest> listOfTxnIdToGet = new CopyOnWriteArrayList<>();

	@Override
	public void run() {
		try {
			Thread.sleep(waitForInterval);
			logger.info("Inside ProcessAllMapReceived class.Now checking the Condition...");
			final Map<String, Map<GetStatusRequest, String>> mapHostStatus = blockDataReceiverThread.getMapHostStatus();
			if (mapHostStatus != null) {
				logger.info("MapHostStatus size before processing: " + mapHostStatus.size());
				final Map<GetStatusRequest, String> mapOfLocalHost = mapHostStatus.get(GetSystemIp.getSystemLocalIp());
				mapHostStatus.remove(GetSystemIp.getSystemLocalIp());
				if (null != mapOfLocalHost) {
					logger.info("LocalMap: number of TxnId are: " + mapOfLocalHost.size());
					mapOfLocalHost.forEach((key, value) -> {
						final List<String> listOfStatus = new ArrayList<>();
						final List<String> listOfHost = new ArrayList<>();
						listOfStatus.add(value);
						mapHostStatus.forEach((key1, value1) -> {
							if (value1.get(key) != null) {
								listOfStatus.add(value1.get(key));
								if (value1.get(key).equals(BlockStatusEnum.SAVED.name())) {
									listOfHost.add(key1);
								}
							}
						});
						// Map contain TxnId and correspond List of Host that
						// have SAVED status for that TxnId.
						mapTxnHost.put(key, listOfHost);
						logger.info("MapTxnHost for Txnid: " + mapTxnHost);
						// Map contain TxnId and correspond list contain list of
						// status Of that TxnId.
						mapTxnIdAndStatus.put(key, listOfStatus);
						logger.info("MapTxnIdAndStatus for TxnId: " + mapTxnIdAndStatus);
					});
				} else {
					logger.info("Local Map was null");
				}
				// Now iterating the map to see for which TxnId we have to get
				// the block And file.
				iterateMapToGetBlock();
			}
			sendTxnIdToOtherNodes();
		} catch (final InterruptedException e) {
			logger.error("Something wrong", e);
		} finally {
			blockDataReceiverThread.clearMapHostStatus();
			clearBuffer();
		}
	}

	public void sendTxnIdToOtherNodes() {
		final Map<String, List<GetStatusRequest>> mapToSendToHost = new HashMap<>();
		final List<GetStatusRequest> listOfTxnId = new ArrayList<>();
		listOfTxnIdToGet.forEach((txnId) -> {
			final List<String> listOfHost = mapTxnHost.get(txnId);
			final int n = ThreadLocalRandom.current().nextInt(0, ConfigConstants.MIN_NODE_VALIDITY);
			final String hostToSendTxnId = listOfHost.get(n);
			listOfTxnId.add(txnId);
			mapToSendToHost.put(StringConstants.TXT_BLOCK, listOfTxnId);
			logger.info("Sending TxnId: " + txnId.getTag() + txnId.getTransactionId() + " To Host: " + hostToSendTxnId);
			syncProcessThread.sendTxnListToOtherNode(mapToSendToHost, hostToSendTxnId);
			listOfTxnId.clear();
		});
	}

	private void clearBuffer() {
		mapTxnIdAndStatus.clear();
		mapTxnHost.clear();
		listOfTxnIdToGet.clear();
	}

	private void iterateMapToGetBlock() {
		mapTxnIdAndStatus.forEach((key, value) -> {
			final String combineKey = key.getTag() + key.getTransactionId();
			final long savedCount = value.stream().filter(status -> {
				return status.equals(BlockStatusEnum.SAVED.name());
			}).count();
			final long deletedCount = value.stream().filter((status) -> {
				return status.equals(BlockStatusEnum.DELETED.name());
			}).count();

			logger.info("Count for SAVED : " + savedCount + " Count For DELETED : " + deletedCount + " For key : "
					+ combineKey);
			final String statusValueAtLocal = value.get(0);

			if (statusValueAtLocal.equals(BlockStatusEnum.DELETED.name())) {
				// Do Nothing
				logger.info("The local status is DELETED for the key " + combineKey);
			} else if (statusValueAtLocal.equals(BlockStatusEnum.SAVED.name())) {
				if (savedCount >= ConfigConstants.MIN_NODE_VALIDITY) {
					// Do Nothing
					logger.info("The local status is SAVED for the key " + combineKey
							+ " and SAVED count from another nodes is " + savedCount);
				} else if (deletedCount >= ConfigConstants.MIN_NODE_VALIDITY) {
					// Make status DELETED of this TxnId and delete this block
					// locally.
					logger.info("The Local status is SAVED and DELETED count from other nodes is " + deletedCount
							+ " so delete this block from this node for key " + combineKey);
					deleteBlockFromLocal(key.getTag(), key.getTransactionId());
				}
			} else {
				if (savedCount >= ConfigConstants.MIN_NODE_VALIDITY) {
					logger.info("The status is neither DELETED nor SAVED for key : " + key.getTransactionId()
							+ " so now getting the block from other node.");
					// GetBlock FROM other Nodes.
					listOfTxnIdToGet.add(key);

				} else if (deletedCount >= ConfigConstants.MIN_NODE_VALIDITY) {
					// Make status DELETED and delete file this block locally
					logger.info("The status is neither DELETED nor SAVED for key : " + key.getTransactionId()
							+ " so now deleting the block from this node.");
					deleteBlockFromLocal(key.getTag(), key.getTransactionId());
				}
			}
		});
	}

	private void deleteBlockFromLocal(final String tag, final String transactionId) {
		final ExecutorService executorService = ThreadPoolUtility.getThreadPool();
		executorService.submit(
				new DeleteBlockRunnableTask(blockService, ioFileUtil).setTag(tag).setTransactionId(transactionId));
	}
}
