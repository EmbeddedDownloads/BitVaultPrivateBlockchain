package com.pbc.job;

import static com.pbc.utility.ConfigConstants.MIN_NODE_VALIDITY;

import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.blockchain.BlockContent;
import com.pbc.models.GetStatusRequest;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.GetSystemIp;
import com.pbc.utility.StringConstants;

@Component
public class SyncProcessThread extends Thread {

	private static Logger logger = Logger.getLogger(SyncProcessThread.class);

	@Autowired
	private BlockSynchronizationReceiver synchronizationReceiver;

	// Wait to 1 minute so that sync data received properly before start sync
	// process.
	private final Long waitForInterval = 1000 * 60 * 1L;

	private Map<String, PbcSyncModel> receivedSyncModelMap = null;

	private final Set<BlockContent> commonBlockSet = new TreeSet<>();

	private static List<String> listOfHosts = new CopyOnWriteArrayList<>();
	final Map<String, List<GetStatusRequest>> mapWithStatus = new HashMap<>();

	static {
		listOfHosts = ConfigConstants.NODES;
		listOfHosts = listOfHosts.stream().filter(t -> !t.isEmpty()).collect(Collectors.toList());
	}

	/**
	 * Map of tag + transactionId and list of host which contains this key. Map
	 * Maintained for the blocks which need to get from other nodes.
	 */
	private final Map<GetStatusRequest, List<String>> receiveBlockForTxnIds = new HashMap<>();

	/**
	 * List of tag + transaction id which is to be deleted from this node.
	 */
	private final List<GetStatusRequest> deleteBlockForTxnIds = new ArrayList<>();

	private final List<String> localHashList = new ArrayList<>();

	@Override
	public void run() {
		try {

			// Start operation after given time minute
			Thread.sleep(waitForInterval);

			// Setting SynchronizationReceiver to not accept any incoming data
			// which sync is in process.
			synchronizationReceiver.doAccept(false);

			receivedSyncModelMap = synchronizationReceiver.getReceivedSyncModelMap();

			if (receivedSyncModelMap.size() > 0 && null != receivedSyncModelMap.get(GetSystemIp.getSystemLocalIp())) {
				localHashList.addAll(receivedSyncModelMap.get(GetSystemIp.getSystemLocalIp()).getHashList());
			}
			// Build common set.
			buildCommonBlockSet();
			if (null != receivedSyncModelMap && receivedSyncModelMap.size() >= MIN_NODE_VALIDITY) {
				// Start block synchronization mechanism.
				doSync();
				// Send TxnId with Status to other nodes.
				sendTxnListToOtherNode();
			} else {
				logger.info(
						"Preventing block syncing because no. of communicated host is " + receivedSyncModelMap.size());
			}
			// Clear the collections for fresh data
			cleanSynchronizationBuffers();
			// Clear common set for new incoming data.
			synchronizationReceiver.clearReceivedSyncModelMap();
			// Setting SynchronizationReceiver to accept incoming data now.
			synchronizationReceiver.doAccept(true);
			// Terminating this thread.
			interrupt();
		} catch (final InterruptedException e) {
			// Empty catch.
			logger.warn("Thread was interrupted " + e.getMessage());
		} catch (final Exception e) {
			logger.error("Problem while starting Sync Process Thread ", e);
		}
	}

	private void cleanSynchronizationBuffers() {
		commonBlockSet.clear();
		receiveBlockForTxnIds.clear();
		deleteBlockForTxnIds.clear();
		localHashList.clear();
		if (null != receivedSyncModelMap) {
			receivedSyncModelMap.clear();
		}
	}

	/**
	 * Build a common set from the sets of all nodes.
	 */
	private void buildCommonBlockSet() {
		commonBlockSet.addAll(receivedSyncModelMap.values().stream().map(set -> set.getOrderedBlockSet())
				.flatMap(Set::stream).collect(Collectors.toSet()));
	}

	/**
	 * Implemented all the cases which is responsible for block chain
	 * synchronization.
	 */
	private void doSync() {
		final StringBuilder combineKey = new StringBuilder();
		final List<GetStatusRequest> listOfTxn = new ArrayList<>();
		commonBlockSet.forEach(blockContent -> {
			try {
				combineKey.append(blockContent.getTag()).append(blockContent.getHashTxnId());
				final GetStatusRequest tagTxn = new GetStatusRequest();
				tagTxn.setTag(blockContent.getTag());
				tagTxn.setTransactionId(blockContent.getHashTxnId());
				listOfTxn.add(tagTxn);
			} catch (final Exception e) {
				logger.error("Error while validating block to sync for transaction id " + blockContent.getHashTxnId(),
						e);
			}
		});
		mapWithStatus.put(StringConstants.TXT_STATUS, listOfTxn);
	}

	private void sendTxnListToOtherNode() {
		final ObjectMapper mapper = new ObjectMapper();
		listOfHosts.forEach((host) -> {
			try (final Socket socket = new Socket();) {
				// logger.info("Sending MapOfTagTxnId: " + mapWithStatus + " To
				// host: " + host);
				socket.setTcpNoDelay(true);
				socket.connect(new InetSocketAddress(host, ConfigConstants.PORT_NO_TAGTXD_SEND), 1000 * 10);
				final String valueAsString = mapper.writeValueAsString(mapWithStatus);
				final PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
				printWriter.write(valueAsString);
				printWriter.flush();
				printWriter.close();
			} catch (final Exception e) {
				logger.error("Problem while Sending listOftagTaxnId: " + mapWithStatus + " to host: " + host);
			}
		});
	}

	public void sendTxnListToOtherNode(final Map<String, List<GetStatusRequest>> mapToSend, final String clientip) {
		final ObjectMapper mapper = new ObjectMapper();
		try (final Socket socket = new Socket();) {
			// logger.info("Sending MapToSend: " + mapToSend + " To host: " +
			// clientip);
			socket.setTcpNoDelay(true);
			socket.connect(new InetSocketAddress(clientip, ConfigConstants.PORT_NO_TAGTXD_SEND), 1000 * 10);
			final String valueAsString = mapper.writeValueAsString(mapToSend);
			// logger.info("Sending value: " + valueAsString + " to Host: " +
			// clientip);
			final PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
			printWriter.write(valueAsString);
			printWriter.flush();
			printWriter.close();
		} catch (final Exception e) {
			logger.error("Problem while Sending listOftagTaxnId: " + mapWithStatus + " to host: " + clientip);
		}
	}
}