package com.pbc.service;

import static com.pbc.utility.ConfigConstants.REPORT_LOG_FILE;
import static com.pbc.utility.ConfigConstants.REPORT_LOG_FILE_PATH;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.blockchain.Block;
import com.pbc.blockchain.BlockContent;
import com.pbc.blockchain.BlockHeader;
import com.pbc.blockchain.BlockResponseDTO;
import com.pbc.blockchain.ParseableBlockDTO;
import com.pbc.blockchain.creation.Persistor;
import com.pbc.blockchain.creation.SingleFileJsonPersistor;
import com.pbc.exception.BlockProcessingException;
import com.pbc.exception.DataException;
import com.pbc.exception.ServiceException;
import com.pbc.models.BlockStatusEnum;
import com.pbc.models.CompleteRequest;
import com.pbc.notification.NotificationReceiver;
import com.pbc.push_notification.service.PNService;
import com.pbc.repository.BlockStatusDao;
import com.pbc.repository.TemporaryUrlDownloadDao;
import com.pbc.repository.model.BlockStatus;
import com.pbc.repository.model.TemporaryUrlDownload;
import com.pbc.utility.StringConstants;

@Service("blockService")
public class BlockService {

	final String TAG_SECURE_MESSAGE = "secure_message";
	final String TAG_A2A_SESSIONKEY = "A2A_Sessionkey";
	final String TAG_A2A_FILE = "A2A_File";

	private static ObjectMapper om = new ObjectMapper();
	private static String firstBlockHash = null;
	private static final Logger logger = Logger.getLogger(BlockService.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	@Autowired
	private BlockStatusDao blockStatusDao;

	@Autowired
	private Persistor<Block> jsonPersistor;

	@Autowired
	private NotificationReceiver notificationReceiver;

	@Autowired
	private PNService pnService;

	@Autowired
	private TemporaryUrlDownloadDao temporaryUrlDownloadDao;

	@Autowired
	ObjectFactory<TransactionMessageService> transactionFactory;

	public void createAndSaveBlock(final ParseableBlockDTO parseableBlockDTO) {
		final Block block = createActualBlock(parseableBlockDTO);
		saveBlock(block);
	}

	/**
	 * Service method to save a block into our Private Block chain. Is also
	 * insert the block status entry into DB.
	 *
	 * @param block
	 *            Block
	 */
	private void saveBlock(final Block block) {
		Persistor.globalLock.writeLock().lock();
		try {
			final BlockStatus blockStatus = getBlockStatus(block.getBlockContent().getTag(),
					block.getBlockContent().getHashTxnId());
			if (null != blockStatus && !blockStatus.getStatus().equals(BlockStatusEnum.SAVED.name())
					&& !blockStatus.getStatus().equals(BlockStatusEnum.DELETED.name())) {

				jsonPersistor.addBlock(block);

				blockStatusDao.updateStatus(block.getBlockContent().getTag(), block.getBlockContent().getHashTxnId(),
						BlockStatusEnum.SAVED.name(), block.getBlockContent().getPublicAddressOfReciever());

				reportLogger.fatal("Block successfully saved into Private BlockChain for transaction id : "
						+ block.getBlockContent().getHashTxnId());

				pnService.sendNotification(getBlockList(block));
			} else {
				logger.info("Block not saved because its already processed for transaction id "
						+ block.getBlockContent().getHashTxnId());
				reportLogger
						.fatal("Block already processed for transaction id" + block.getBlockContent().getHashTxnId());
			}
		} catch (final DataException | BlockProcessingException de) {
			blockStatusDao.updateStatus(block.getBlockContent().getTag(), block.getBlockContent().getHashTxnId(),
					BlockStatusEnum.ERROR_OCCURED.name(), null);
			logger.error("Error while adding block with combined key: " + block.getBlockContent().getTag()
					+ block.getBlockContent().getHashTxnId(), de);
			reportLogger.fatal("Unable to save block for transaction id : " + block.getBlockContent().getHashTxnId());
			throw new ServiceException(de);
		} catch (final Exception e) {
			blockStatusDao.updateStatus(block.getBlockContent().getTag(), block.getBlockContent().getHashTxnId(),
					BlockStatusEnum.ERROR_OCCURED.name(), null);
			logger.error("Error occured while adding block so updating the block status for combined key: "
					+ block.getBlockContent().getTag() + block.getBlockContent().getHashTxnId(), e);
			reportLogger.fatal("Unable to save block for transaction id : " + block.getBlockContent().getHashTxnId());
			throw new ServiceException("Error occured while adding block.", e);
		} finally {
			Persistor.globalLock.writeLock().unlock();
		}
	}

	/**
	 * Fetch list of blocks matching to its transaction Id.
	 *
	 * @param block
	 * @return
	 */
	private List<Block> getBlockList(final Block block) {
		final List<Block> blockList = new ArrayList<>();
		blockList.add(block);
		return blockList;
	}

	/**
	 * A service, which enables you to remove a block from our Private Block
	 * chain. It requires tag and transaction id of the block which is to be
	 * removed. This method update block status as
	 * {@link BlockStatusEnum#DELETED}
	 *
	 * @param tag
	 *            tag of Block
	 * @param transactionId
	 *            transaction id of Block
	 */
	public void removeBlock(final String tag, final String transactionId) {
		final String combineKey = tag + transactionId;
		try {
			jsonPersistor.removeBlockWithHash(combineKey);
			updateDeleteStatus(tag, transactionId, BlockStatusEnum.DELETED.name());
			logger.info("Block removed with status deleted for combined key: " + combineKey);
			reportLogger.fatal("Block removed from the Blockchain for transaction id " + transactionId);
		} catch (final DataException | BlockProcessingException de) {
			reportLogger.fatal("Unable to delete block for transaction id : " + transactionId);
			logger.error("Error while removing data from block chain for combined key: " + combineKey, de);
			throw new ServiceException(de);
		} catch (final Exception e) {
			logger.error("Error while removing data from block chain for combined key: " + combineKey, e);
			reportLogger.fatal("Unable to delete block for transaction id : " + transactionId);
			throw new ServiceException("Error while removing data from block chain.", e);
		}
	}

	public Block getBlock(final String hash) {
		Persistor.globalLock.readLock().lock();
		try {
			return jsonPersistor.getBlock(hash);
		} catch (final BlockProcessingException bpe) {
			logger.error("Block Procesing Error while fetching block with combined key: " + hash, bpe);
			throw new ServiceException(bpe);
		} catch (final Exception e) {
			logger.error("Error while fetching block with combined key: " + hash, e);
			throw new ServiceException("Unable to get block now.", e);
		} finally {
			Persistor.globalLock.readLock().unlock();
		}
	}

	/**
	 * This method construct actual block(Block object containing (
	 * {@link BlockHeader}, {@link BlockContent}, blockHash etc.) from
	 * {@link ParseableBlockDTO} reference.
	 *
	 * @param parseableBlockDTO
	 *            ParsableBlockDTO
	 * @return Block
	 */
	public Block createActualBlock(final ParseableBlockDTO parseableBlockDTO) {
		try {
			final BlockContent blockContent = new BlockContent();
			blockContent.setCrc(parseableBlockDTO.getCrc());
			blockContent.setTag(parseableBlockDTO.getTag());
			blockContent.setHashTxnId(parseableBlockDTO.getTransactionId());
			blockContent.setPublicAddressOfReciever(parseableBlockDTO.getReceiver());
			blockContent.setDataHash(parseableBlockDTO.getDataHash());
			blockContent.setFilePath(parseableBlockDTO.getFilePath());
			blockContent.setPbcId(parseableBlockDTO.getPbcId());
			blockContent.setAppId(parseableBlockDTO.getAppId());
			blockContent.setSessionKey(parseableBlockDTO.getSessionKey());
			blockContent.setTimestamp(parseableBlockDTO.getTimeStamp());
			blockContent.setSender(parseableBlockDTO.getSender());
			blockContent.setWebServerKey(parseableBlockDTO.getWebServerKey());
			final BlockHeader blockHeader = new BlockHeader(System.currentTimeMillis(), firstBlockHash);
			final Block block = new Block(blockHeader, blockContent);
			block.setBlockHash(calculateHash(om.writeValueAsString(blockContent).getBytes(), "SHA-256"));
			block.setCreatedOn(System.currentTimeMillis());
			block.setUpdatedOn(System.currentTimeMillis());
			return block;
		} catch (final JsonProcessingException jpe) {
			logger.error("Could not contruct actual block for combined key: " + parseableBlockDTO.getTag()
					+ parseableBlockDTO.getTransactionId(), jpe);
			throw new ServiceException(jpe);
		} catch (final Exception e) {
			throw new ServiceException("Problem in contructing actual block for combined key: "
					+ parseableBlockDTO.getTag() + parseableBlockDTO.getTransactionId(), e);
		}
	}

	/**
	 * Used to calculate hash of provided bytes.
	 *
	 * @param fileBytes
	 *            Byte[] of file
	 * @param algoName
	 *            Algorithm like {@code SHA-256 or MD5}
	 * @return hash string
	 */
	public String calculateHash(final byte fileBytes[], final String algoName) {
		try {
			final MessageDigest digest = MessageDigest.getInstance(algoName);
			final byte[] bytesHash = digest.digest(fileBytes);
			return convertByteArrayToHexString(bytesHash);
		} catch (final NoSuchAlgorithmException nsae) {
			logger.error("Provided algorithm not allowed.", nsae);
			throw new ServiceException("Provided algorithm not allowed.", nsae);
		} catch (final Exception e) {
			logger.error("Error while calculating hash.", e);
			throw new ServiceException("Error while calculating hash.", e);
		}
	}

	public String calculateHash(final InputStream content, final String algoName) throws Exception {
		final byte[] buffer = new byte[8192];
		final MessageDigest md = MessageDigest.getInstance(algoName);
		final DigestInputStream dis = new DigestInputStream(content, md);
		try {
			while (dis.read(buffer) != -1) {
				;
			}
		} finally {
			dis.close();
		}
		return convertByteArrayToHexString(md.digest()); // return
															// DatatypeConverter.printHexBinary(md.digest());
															// }
	}

	private static String convertByteArrayToHexString(final byte[] arrayBytes) {
		final StringBuffer stringBuffer = new StringBuffer();
		for (final byte arrayByte : arrayBytes) {
			stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16).substring(1));
		}
		return stringBuffer.toString();
	}

	/**
	 * This create a complete request from {@link ParseableBlockDTO}.
	 *
	 * @param parseableBlockDTO
	 * @return {@link CompleteRequest}
	 */
	public CompleteRequest createCompleteRequest(final ParseableBlockDTO parseableBlockDTO) {
		final CompleteRequest completeRequest = new CompleteRequest();
		completeRequest.setCrc(parseableBlockDTO.getCrc());
		completeRequest.setTransactionId(parseableBlockDTO.getTransactionId());
		completeRequest.setTag(parseableBlockDTO.getTag());
		return completeRequest;
	}

	/**
	 * This method parse {@link Block} object to {@link BlockResponseDTO} which
	 * is used for sending API response.
	 *
	 * @param block
	 *            Block
	 * @return {@link BlockResponseDTO}
	 */
	public BlockResponseDTO getBlockResponseDTO(final Block block) {
		try {
			final BlockResponseDTO blockResponse = new BlockResponseDTO();
			final BlockContent blockContent = block.getBlockContent();
			blockResponse.setCrc(blockContent.getCrc()).setReceiver(blockContent.getPublicAddressOfReciever())
					.setTransactionId(blockContent.getHashTxnId()).setTag(blockContent.getTag())
					.setFileId(UUID.randomUUID().toString()).setPbcId(blockContent.getPbcId())
					.setAppId(blockContent.getAppId()).setTimestamp(blockContent.getTimestamp())
					.setSessionKey(blockContent.getSessionKey()).setSender(blockContent.getSender())
					.setWebServerKey(blockContent.getWebServerKey());
			return blockResponse;
		} catch (final Exception e) {
			logger.error("Problem in pasring block object to BlockResponseDTO.", e);
			throw new ServiceException("Problem in pasring Block object to BlockResponseDTO.", e);
		}
	}

	/**
	 * In Spring MVC rest service, we need a FileSystemResource object of file
	 * for downloading file from server. This method will be used to get a
	 * FileSystemResource, which will be send as a API response.
	 *
	 * @param fileId
	 *            String fileId
	 * @return FileSystemResource
	 */
	public FileSystemResource getDownloadFile(final String fileId) throws DataException {
		final TemporaryUrlDownload filePathAndStatus = temporaryUrlDownloadDao.getFilePath(fileId);
		String messageForDisplay = null;
		if (filePathAndStatus != null && Boolean.FALSE.equals(filePathAndStatus.getStatus())) {
			final String filePath = filePathAndStatus.getFilePath();
			logger.info("File location retrieved from database is: " + filePath + " for given file id: " + fileId);
			temporaryUrlDownloadDao.updateStatus(fileId, true);
			final File bufferedFile = new File(filePath);
			try {
				final FileInputStream inputStream = new FileInputStream(bufferedFile);
				if (filePathAndStatus.getDataHash().equals(calculateHash(inputStream, "SHA-256"))) {
					logger.info("Data hash is matched for requested file for fileId " + fileId);
				} else {
					logger.info("Data hash is different for requested file for fileId " + fileId);
				}
			} catch (final Exception e) {
				logger.error("Problem while calculating hash ", e);
			}
			return new FileSystemResource(bufferedFile);
		}
		if (filePathAndStatus == null) {
			logger.info(messageForDisplay = "File not found for the given temp url for fileId: " + fileId);
		} else if (Boolean.TRUE.equals(filePathAndStatus.getStatus())) {
			logger.info(messageForDisplay = "Url is present but expired for given fileId: " + fileId);
		}
		return new FileSystemResource(getErrorMsg(messageForDisplay));
	}

	/**
	 * Construct HTML error response message file. This message will send as
	 * response if request file not found onto the server.
	 *
	 * @param filePath
	 * @return String
	 */
	public String getErrorMsg(final String errorMessage) {
		final StringBuilder builder = new StringBuilder();
		builder.append("<!DOCTYPE html>");
		builder.append("<html>");
		builder.append("<head>");
		builder.append("</head>");
		builder.append("<body>");
		builder.append("<h3>");
		builder.append(errorMessage);
		builder.append("<h3>");
		builder.append("</body>");
		builder.append("</html>");
		return builder.toString();
	}

	/**
	 * To change the status of a Block.
	 *
	 * @param tag
	 * @param transactionId
	 */
	public void changeStatus(final String tag, final String transactionId) {
		try {
			final BlockStatus blockStatus = getBlockStatus(tag, transactionId);
			if (blockStatus != null && blockStatus.getStatus().equals(BlockStatusEnum.INPROCESS.name())) {
				blockStatusDao.updateStatus(tag, transactionId, BlockStatusEnum.SAVE_FAILED.name(), null);
			}
		} catch (final DataException de) {
			logger.error("Problem occured while updating block status for combined key: " + tag + transactionId, de);
			throw new ServiceException(de);
		} catch (final Exception e) {
			logger.error("Problem occured while updating block status for combined key: " + tag + transactionId, e);
			throw new ServiceException("Problem occured while updating Block status.", e);
		}
	}

	public boolean taskToNotify(final BlockStatus blockStatus, final ParseableBlockDTO parseableBlockDTO) {
		final String transactionId = parseableBlockDTO.getTransactionId();
		final String tag = parseableBlockDTO.getTag();

		if (insert(blockStatus)) {
			transactionFactory.getObject().putCRCValue(
					parseableBlockDTO.getTag() + parseableBlockDTO.getTransactionId(), parseableBlockDTO.getCrc());
			notificationReceiver.putBlockDTOInCache(tag + transactionId, parseableBlockDTO);
			logger.info("File uploaded successfully for data with combinedKey: " + tag + transactionId);
			transactionFactory.getObject().createTaskToNotify(parseableBlockDTO.getCrc(), tag, transactionId, false);
			return true;
		}

		final BlockStatus status = getBlockStatus(tag, transactionId);

		if (null != status && status.getStatus().equals(BlockStatusEnum.BLOCK_TO_BE_CREATED.name())) {
			createAndSaveBlock(parseableBlockDTO);
			transactionFactory.getObject().createTaskToNotify(parseableBlockDTO.getCrc(), tag, transactionId, false);
			return true;
		}
		return false;
	}

	/**
	 * To get the current status of a Block.
	 *
	 * @param tag
	 *            Tag of block
	 * @param transactionId
	 *            transaction id of block
	 * @return {@link BlockStatus}
	 */
	public BlockStatus getBlockStatus(final String tag, final String transactionId) {
		try {
			return blockStatusDao.getStatus(tag, transactionId);
		} catch (final DataException de) {
			logger.error("Data access exception while getting block status for combined key: " + tag + transactionId,
					de);
			throw new ServiceException(de);
		} catch (final Exception e) {
			logger.error("Unable to get block status for combined key: " + tag + transactionId, e);
			throw new ServiceException("Unable to get block status for combined key: " + tag + transactionId, e);
		}
	}

	/**
	 * This method insert an entry into {@code url_download} table for the
	 * block, if getMessage or getBlocks API requested. This is to restrict file
	 * download validation. Status of this inserted entry is changed to false
	 * when getFile API is requested, means user or client can't use his file id
	 * to get/download file once he used it.
	 *
	 * @param blockResponseDTO
	 * @param block
	 */
	public void createDownloadUrl(final BlockResponseDTO blockResponseDTO, final Block block) {
		try {
			String dataHash = "";
			if (block != null && block.getBlockContent() != null) {
				dataHash = block.getBlockContent().getDataHash();
			}
			final TemporaryUrlDownload urlDownload = new TemporaryUrlDownload(blockResponseDTO.getFileId(),
					block.getBlockContent().getFilePath(), false, dataHash);
			temporaryUrlDownloadDao.insert(urlDownload);
		} catch (final DataException de) {
			logger.error("Problem into inserting data for combined key: " + blockResponseDTO.getTag()
					+ blockResponseDTO.getTransactionId(), de);
			throw new ServiceException(de);
		} catch (final Exception e) {
			logger.error("Problem into inserting data for combined key: " + blockResponseDTO.getTag()
					+ blockResponseDTO.getTransactionId(), e);
			throw new ServiceException("Problem into inserting data.", e);
		}
	}

	/**
	 * This method returns the list of all blocks for the requested receiver
	 * addresses. This method internally call
	 * {@link BlockStatusDao#getBlockList(List)} to get the list of blocks for
	 * the receiver addresses and after that it calls (
	 * {@link TemporaryUrlDownloadDao#insert(TemporaryUrlDownload)}) to insert
	 * download entry.
	 *
	 * @param addresses
	 *            List<String>
	 * @return List<BlockResponseDTO>
	 */
	public List<BlockResponseDTO> getBlockList(final List<String> addresses) {
		try {
			final List<BlockStatus> blockStatusList = blockStatusDao.getBlockList(addresses);

			final List<BlockResponseDTO> listOfBlocks = new CopyOnWriteArrayList<>();
			final List<TemporaryUrlDownload> tDownloads = new CopyOnWriteArrayList<>();
			final StringBuilder combineKey = new StringBuilder();

			if (blockStatusList != null && !blockStatusList.isEmpty()) {
				logger.info("Getting Blocks of list size : " + blockStatusList.size());
				blockStatusList.forEach(blockStatus -> {
					combineKey.append(blockStatus.getTag()).append(blockStatus.getTransactionId());
					logger.info("Combine Key : " + combineKey);
					final Block block = jsonPersistor.getBlock(combineKey.toString());

					if (null != block && block.getBlockContent().getTag().equals(TAG_SECURE_MESSAGE)) {
						getListOfBlock(block, tDownloads, listOfBlocks);
					} else if (null != block && block.getBlockContent().getTag().equals(TAG_A2A_SESSIONKEY)) {

						// if (map.get(block.getBlockContent().getHashTxnId())
						// == null) {
						// map.put(block.getBlockContent().getHashTxnId(),
						// block);
						// } else {
						// getListOfBlock(map.get(block.getBlockContent().getHashTxnId()),
						// tDownloads, listOfBlocks);
						// }
						final BlockStatus status = blockStatusDao.getStatus(TAG_A2A_FILE,
								block.getBlockContent().getHashTxnId());
						if (status != null) {
							getListOfBlock(block, tDownloads, listOfBlocks);
						}
					}
					combineKey.setLength(0);
				});
				temporaryUrlDownloadDao.bulkUrlInsert(tDownloads);
				tDownloads.clear();
			}
			return listOfBlocks;
		} catch (final DataException | BlockProcessingException de) {
			logger.error("Error occured while getting block list: " + Arrays.toString(addresses.toArray()), de);
			throw new ServiceException(de);
		} catch (final Exception e) {
			logger.error("Error occured while getting block list: " + Arrays.toString(addresses.toArray()), e);
			throw new ServiceException("Error occured while getting block list.", e);
		}
	}

	private void getListOfBlock(final Block block, final List<TemporaryUrlDownload> tDownloads,
			final List<BlockResponseDTO> listOfBlocks) {
		final BlockResponseDTO blockResponseDTO = getBlockResponseDTO(block);
		final TemporaryUrlDownload urlDownload = new TemporaryUrlDownload(blockResponseDTO.getFileId(),
				block.getBlockContent().getFilePath(), false, block.getBlockContent().getDataHash());
		tDownloads.add(urlDownload);
		listOfBlocks.add(blockResponseDTO);
	}

	/**
	 * Insert a block status entry into block_status table.
	 *
	 * @param blockStatus
	 */
	public synchronized boolean insert(final BlockStatus blockStatus) {
		boolean firstTimeStatus = false;
		try {
			if (getBlockStatus(blockStatus.getTag(), blockStatus.getTransactionId()) == null) {
				logger.info("Block status is null so going to insert" + blockStatus.getTag()
						+ blockStatus.getTransactionId());
				blockStatusDao.insert(blockStatus);
				firstTimeStatus = true;
			}
		} catch (final DataException de) {
			logger.error("Can't insert block status with combined key: " + blockStatus.getTag()
					+ blockStatus.getTransactionId(), de);
		} catch (final Exception e) {
			logger.error("Can't insert block status now with combined key: " + blockStatus.getTag()
					+ blockStatus.getTransactionId(), e);
		}
		return firstTimeStatus;
	}

	/**
	 * It insert a block status into block_status table if it not present else
	 * update.
	 *
	 * @param blockStatus
	 *            BlockStatus
	 */
	public void insertOrUpdate(final BlockStatus blockStatus) {
		try {
			if (getBlockStatus(blockStatus.getTag(), blockStatus.getTransactionId()) != null) {
				blockStatusDao.insert(blockStatus);
				return;
			}
			blockStatusDao.updateStatus(blockStatus.getTag(), blockStatus.getTransactionId(), blockStatus.getStatus(),
					null);
		} catch (final DataException de) {
			logger.error("Problem inserting/updating block status with combined key: " + blockStatus.getTag()
					+ blockStatus.getTransactionId(), de);
			throw new ServiceException(de);
		} catch (final Exception e) {
			logger.error("Problem inserting/updating block status with combined key: " + blockStatus.getTag()
					+ blockStatus.getTransactionId(), e);
			throw new ServiceException("Problem inserting/updating block status.", e);
		}
	}

	public synchronized boolean updateDeleteStatus(final String tag, final String transactionId,
			final String statusValue) {

		if (statusValue.equals(BlockStatusEnum.DELETED.name())) {
			blockStatusDao.updateStatus(tag, transactionId, statusValue, null);
			return true;
		}

		final BlockStatus blockStatus = getBlockStatus(tag, transactionId);
		if (blockStatus == null || blockStatus.getStatus().equals(BlockStatusEnum.DELETED.name())
				|| blockStatus.getStatus().equals(statusValue)) {
			return false;
		}
		blockStatusDao.updateStatus(tag, transactionId, statusValue, null);
		return true;
	}

	public boolean checkAndNotify(final String crc, final String tag, final String transactionId) {
		if (updateDeleteStatus(tag, transactionId, BlockStatusEnum.BLOCK_DELETE_IN_PROCESS.name())) {
			transactionFactory.getObject().putCRCValue(tag + transactionId + StringConstants.DELETE_TAG, crc);
			transactionFactory.getObject().createTaskToNotify(crc, tag, transactionId, true);
			return true;
		} else {
			return false;
		}
	}

	public boolean checkIfExsist(final String tag, final String transactionId) {
		final BlockStatus blockStatus = blockStatusDao.getStatus(tag, transactionId);
		if (blockStatus == null) {
			return true;
		} else if (blockStatus.getStatus().equals(BlockStatusEnum.BLOCK_TO_BE_CREATED.name())) {
			return true;
		} else {
			return false;
		}
	}

	public Map<String, String> readLog(final long pointerLocation) {
		String data = null;
		try (RandomAccessFile raf = new RandomAccessFile(new File(REPORT_LOG_FILE_PATH + REPORT_LOG_FILE), "rw")) {
			raf.seek(pointerLocation);
			final long length = raf.length();
			final int readDataLength = (int) ((int) length - pointerLocation);
			final byte[] blockBytes = new byte[readDataLength];
			raf.read(blockBytes, 0, readDataLength);
			data = new String(blockBytes, "UTF-8");

			final Map<String, String> mapData = new HashMap<>();
			mapData.put("currentPointer", String.valueOf(length));
			mapData.put("logData", data);
			return mapData;
		} catch (final Exception e) {
			logger.error("Problem reading log data ", e);
			throw new ServiceException(e.getMessage(), e);
		}
	}

	public long totalBlocks() {
		return blockStatusDao.getTotalBlockCount();
	}

	public long availableBlocks() {
		return blockStatusDao.getAvailableBlockCount();
	}

	public long getDeletedBlockCount() {
		return blockStatusDao.getDeletedBlockCount();
	}

	public TreeSet<BlockContent> getOrderedSet() {
		if (jsonPersistor instanceof SingleFileJsonPersistor<?>) {
			return ((SingleFileJsonPersistor<Block>) jsonPersistor).getOrderedSet();
		} else {
			return null;
		}
	}

	public List<BlockStatus> getBlockToBeCreatedList() {
		return blockStatusDao.getBlockToBeCreatedList();
	}

	public List<BlockStatus> getBlockStatusListByPage(final int pageNo) {
		return blockStatusDao.getBlockStatusListByPage(pageNo);
	}

}