package com.pbc.job;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pbc.blockchain.Block;
import com.pbc.blockchain.ParseableBlockDTO;
import com.pbc.models.BlockStatusEnum;
import com.pbc.models.GetStatusRequest;
import com.pbc.repository.model.BlockStatus;
import com.pbc.service.BlockService;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.IOFileUtil;

/**
 * A thread responsible to receive Map of Tag,TxnId and status for block
 * comparison and also to receive block data which was ask from another node.
 *
 */
@Component
public class BlockDataReceiverThread extends Thread {

	private static final Logger logger = Logger.getLogger(BlockDataReceiverThread.class);

	@Autowired
	private BlockService blockService;

	@Autowired
	private IOFileUtil ioFileUtil;

	/**
	 * Contains Host as Key and value is Map of GetStatusRequest(tag,
	 * transaction key) and corresponding status.
	 */
	static Map<String, Map<GetStatusRequest, String>> mapHostStatus = new ConcurrentHashMap<>();
	Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();
	String status = "";

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(ConfigConstants.PORT_NO_BLOCK_RECEIVE);
			while (true) {
				try {
					final Socket socket;
					try {
						socket = serverSocket.accept();
						socket.setSoTimeout(1000 * 60);
					} catch (final Exception e) {
						continue;
					}
					final Runnable listener = () -> {
						try (DataInputStream dataIn = new DataInputStream(socket.getInputStream());) {
							// ObjectInputStream ois = new
							// ObjectInputStream(socket.getInputStream());
							// final SyncBlockDataModel
							// receivedBlockAndFileModel = (SyncBlockDataModel)
							// ois.readObject();
							String jsonString = "";
							try {
								jsonString = "" + dataIn.readUTF();
							} catch (final Exception e) {
								logger.info("Need to worry:" + e);
							}
							logger.info("Streamed message: " + jsonString);
							final SyncBlockDataModel receivedBlockAndFileModel = gson.fromJson(jsonString,
									SyncBlockDataModel.class);

							if (null == receivedBlockAndFileModel) {
							} else if (receivedBlockAndFileModel.getMapWithStatus() != null) {
								logger.info("Message Received: " + receivedBlockAndFileModel.toString()
										+ " from the Host: " + socket.getInetAddress().getHostAddress());
								final Map<GetStatusRequest, String> mapWithStatus = new ConcurrentHashMap<>();
								mapWithStatus.putAll(receivedBlockAndFileModel.getMapWithStatus());
								mapHostStatus.put(socket.getInetAddress().getHostAddress(), mapWithStatus);
							} else if (receivedBlockAndFileModel.getBlock() != null) {
								// final byte[] file =
								// receivedBlockAndFileModel.getFile();
								final String tag = receivedBlockAndFileModel.getBlock().getBlockContent().getTag();
								final String transactionId = receivedBlockAndFileModel.getBlock().getBlockContent()
										.getHashTxnId();
								// logger.info("Block data received " +
								// receivedBlockAndFileModel.getBlock().toString());
								logger.info("Downloading the File for transaction id " + transactionId);
								// Downloading the file.................
								final String filePath = ioFileUtil.writObjectLocally(dataIn, tag + transactionId);
								// Clear byte array
								// Arrays.fill(file, (byte) 0);
								logger.info("File downloading Completed for transaction id " + transactionId);

								BlockStatus blockStatus = blockService.getBlockStatus(tag, transactionId);
								if (null == blockStatus) {
									blockStatus = getBlockStatusObject(receivedBlockAndFileModel.getBlock());
									blockService.insert(blockStatus);
								}
								status = blockStatus.getStatus();
								logger.info("DB status for transaction id " + transactionId + " is " + status);
								if (status.equals(BlockStatusEnum.SAVED.name())
										|| status.equals(BlockStatusEnum.DELETED.name())) {
									// Do Nothing
									logger.info("Not saving the block because current status is " + status);
								} else {

									createAndSaveBlock(receivedBlockAndFileModel.getBlock(), filePath);
									logger.info("Creating Block in BlockController for txnId : " + transactionId);
								}
							}
						} catch (final Exception e) {
							logger.error("Error occured in saving block ", e);
						} finally {
							if (null != socket) {
								try {
									socket.close();
								} catch (final Exception e) {
								}
							}
						}
					};
					new Thread(listener).start();
				} catch (final Exception e) {
					logger.error("Error occured in saving block ", e);
				}
			}
		} catch (

		final Exception e) {
			logger.error("Problem Occurred due to ", e);
		}
	}

	private BlockStatus getBlockStatusObject(final Block block) {
		return new BlockStatus().setTag(block.getBlockContent().getTag())
				.setTransactionId(block.getBlockContent().getHashTxnId())
				.setReceiverAddress(block.getBlockContent().getPublicAddressOfReciever())
				.setStatus(BlockStatusEnum.INPROCESS.name());
	}

	private void createAndSaveBlock(final Block block, final String filePath) {
		final ParseableBlockDTO parseableDTO = new ParseableBlockDTO();
		parseableDTO.setAppId(block.getBlockContent().getAppId()).setCrc(block.getBlockContent().getCrc())
				.setDataHash(block.getBlockContent().getDataHash()).setFilePath(filePath)
				.setPbcId(block.getBlockContent().getPbcId())
				.setReceiver(block.getBlockContent().getPublicAddressOfReciever())
				.setSender(block.getBlockContent().getSender()).setSessionKey(block.getBlockContent().getSessionKey())
				.setTag(block.getBlockContent().getTag()).setTimeStamp(block.getBlockContent().getTimestamp())
				.setTransactionId(block.getBlockContent().getHashTxnId())
				.setWebServerKey(block.getBlockContent().getWebServerKey());
		blockService.createAndSaveBlock(parseableDTO);
	}

	public Map<String, Map<GetStatusRequest, String>> getMapHostStatus() {
		return mapHostStatus;
	}

	public void setMapHostStatus(final Map<String, Map<GetStatusRequest, String>> mapHostStatus) {
		BlockDataReceiverThread.mapHostStatus = mapHostStatus;
	}

	public void clearMapHostStatus() {
		mapHostStatus.clear();
	}
}
