package com.pbc.restcontroller;

import static com.pbc.utility.CustomMessageConstants.BLOCK_ALREADY_DELETED;
import static com.pbc.utility.CustomMessageConstants.BLOCK_DELETED_SUCCESSFULLY;
import static com.pbc.utility.CustomMessageConstants.DELETED_ALRDY_RECEIVED;
import static com.pbc.utility.CustomMessageConstants.ERR_IN_CREATE_BLOCK;
import static com.pbc.utility.CustomMessageConstants.FILE_UPLOADED_SUCCESSFULLY;
import static com.pbc.utility.CustomMessageConstants.MSG_BLOCK_RETURN_SUCCESSFULLY;
import static com.pbc.utility.CustomMessageConstants.MSG_NOT_VALID;
import static com.pbc.utility.CustomMessageConstants.NO_MESSAGE_FOR_TXNID;
import static com.pbc.utility.CustomMessageConstants.RECEIVER_ADDR_VALIDATION;
import static com.pbc.utility.CustomMessageConstants.STR_IS;
import static com.pbc.utility.CustomMessageConstants.STR_STATUS_MESSAGE;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.pbc.blockchain.Block;
import com.pbc.blockchain.BlockResponseDTO;
import com.pbc.blockchain.ParseableBlockDTO;
import com.pbc.models.AcknowledgeRequest;
import com.pbc.models.BlockStatusEnum;
import com.pbc.models.CustomErrorResponse;
import com.pbc.models.CustomResponse;
import com.pbc.models.CustomSuccessResponse;
import com.pbc.models.GetMessageRequest;
import com.pbc.models.GetStatusRequest;
import com.pbc.models.ReceiverAddresses;
import com.pbc.models.StatisticsModel;
import com.pbc.repository.model.BlockStatus;
import com.pbc.service.BlockService;
import com.pbc.service.TransactionMessageService;
import com.pbc.utility.CustomMessageConstants;
import com.pbc.utility.StringConstants;

@Controller
@RequestMapping("/")
public class BlockController {
	// URL constants
	private static final String SEND_MESSAGE_URL = "/sendMessage";
	private static final String BLOCK_STATUS_URL = "/block/messageStatus";
	private static final String GET_MESSAGE_URL = "/block/getMessage";
	private static final String ACKNOWLEDGE_DELETE_DATA = "/block/acknowledge/";
	private static final String GET_FILE = "/block/getFile";
	private static final String GET_BLOCKS = "/block/getBlocks";
	private static final String GET_LOG = "/getLog";
	private static final String GET_STATISTICS = "/getStatistics";

	private static final Logger logger = Logger.getLogger(BlockController.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	@Autowired
	private BlockService blockService;

	@Autowired
	ObjectFactory<TransactionMessageService> transactionFactory;

	@ResponseBody
	@RequestMapping(value = SEND_MESSAGE_URL, method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public CustomResponse<String> fileUpload(@NotNull @RequestParam("file") final MultipartFile file,
			@NotNull @RequestParam("transactionId") final String transactionId,
			@NotNull @RequestParam("crc") final String crc,
			@NotNull @RequestParam("receiverAddress") final String receiver,
			@NotNull @RequestParam("senderAddress") final String sender, @NotNull @RequestParam("tag") final String tag,
			@NotNull @RequestParam("pbcId") final String pbcId, @NotNull @RequestParam("appId") final String appId,
			@NotNull @RequestParam("timestamp") final String timestamp,
			@NotNull @RequestParam("sessionKey") final String sessionKey,
			@NotNull @RequestParam("webServerKey") final String webServerKey) {
		CustomResponse<String> customResponse = null;

		logger.info("Request received for transaction id : " + transactionId);
		reportLogger.fatal("Request received for transaction id : " + transactionId);

		final BlockStatus blockStatus = new BlockStatus();
		blockStatus.setTransactionId(transactionId).setTag(tag).setReceiverAddress(receiver);
		try {
			final ParseableBlockDTO parseableBlockDTO = new ParseableBlockDTO();
			parseableBlockDTO.setTransactionId(transactionId).setCrc(crc).setReceiver(receiver).setSender(sender)
					.setTag(tag).setPbcId(pbcId).setAppId(appId).setTimeStamp(Long.parseLong(timestamp))
					.setSessionKey(sessionKey).setWebServerKey(webServerKey);
			logger.info("Complete Request data:: " + parseableBlockDTO.toString() + " for " + tag + transactionId);

			final boolean isValidMessage = transactionFactory.getObject().parseRequestAndValidate(parseableBlockDTO,
					file.getInputStream());
			if (!isValidMessage) {
				logger.warn(
						"CRC sent does not match with calculated crc it's not gonna process it further for combinedKey: "
								+ tag + transactionId);
				reportLogger.fatal("CRC is not valid for transaction id : " + transactionId);
				customResponse = new CustomErrorResponse<>();
				customResponse.setMessage(MSG_NOT_VALID);
				return customResponse;
			}
			reportLogger.fatal("CRC validated successfully for transaction id : " + transactionId);
			blockStatus.setStatus(BlockStatusEnum.INPROCESS.name());
			if (blockService.taskToNotify(blockStatus, parseableBlockDTO)) {
				customResponse = new CustomSuccessResponse<>();
				customResponse.setMessage(FILE_UPLOADED_SUCCESSFULLY);
				return customResponse;
			} else {
				logger.warn("Transaction id already exist : " + transactionId);
				reportLogger.fatal("Request already exists for transaction id : " + transactionId);
				customResponse = new CustomErrorResponse<>();
				customResponse.setMessage(CustomMessageConstants.TXN_ALREADY_EXIST + " :: " + transactionId);
				return customResponse;
			}
		} catch (final Exception ex) {
			logger.error("An error occured while creating block for combined key: " + tag + transactionId, ex);
			blockStatus.setStatus(BlockStatusEnum.ERROR_OCCURED.name());
			blockService.insertOrUpdate(blockStatus);
			customResponse = new CustomErrorResponse<>();
			customResponse.setMessage(ERR_IN_CREATE_BLOCK);
			return customResponse;
		}
	}

	/**
	 * Method returns details from transaction received for given transaction
	 * id.
	 *
	 * @param completeRequest
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = BLOCK_STATUS_URL, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public CustomResponse<String> getBlockStatus(@RequestBody @NotNull @Validated final GetStatusRequest request) {
		logger.info(
				"Block status get request received for combined key: " + request.getTag() + request.getTransactionId());
		CustomResponse<String> response = null;
		final BlockStatus blockStatus = blockService.getBlockStatus(request.getTag(), request.getTransactionId());
		if (null == blockStatus) {
			response = new CustomErrorResponse<>();
			response.setMessage(CustomMessageConstants.NO_MESSAGE);
			return response;
		}

		final String status = blockStatus.getStatus();
		response = new CustomSuccessResponse<>();

		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(STR_STATUS_MESSAGE);
		stringBuilder.append(StringConstants.SPACE);
		stringBuilder.append(request.getTransactionId());
		stringBuilder.append(StringConstants.SPACE);
		stringBuilder.append(STR_IS);
		stringBuilder.append(StringConstants.SPACE);
		stringBuilder.append(status);

		response.setMessage(stringBuilder.toString());
		((CustomSuccessResponse<String>) response).setResultSet(status);
		logger.info(stringBuilder.toString() + " GetMessage by txnid " + blockStatus.getTag()
				+ blockStatus.getTransactionId());
		return response;
	}

	/**
	 * Method returns whole block received for given transaction id.
	 *
	 * @param completeRequest
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = GET_MESSAGE_URL, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Object recieveBlockData(@RequestBody @NotNull @Validated final GetMessageRequest getMessageRequest) {
		CustomResponse<BlockResponseDTO> response = null;
		final String combineKey = getMessageRequest.getTag() + getMessageRequest.getTransactionId();
		logger.info("Get message request for combined Key: " + combineKey);
		reportLogger.fatal("Get message requested for transaction id : " + getMessageRequest.getTransactionId());
		final Block block = blockService.getBlock(combineKey);
		if (block == null) {
			reportLogger.fatal(NO_MESSAGE_FOR_TXNID + getMessageRequest.getTransactionId());
			response = new CustomErrorResponse<>();
			response.setMessage(NO_MESSAGE_FOR_TXNID);
			return response;
		}
		final BlockResponseDTO blockResponseDTO = blockService.getBlockResponseDTO(block);
		blockService.createDownloadUrl(blockResponseDTO, block);
		response = new CustomSuccessResponse<>();
		response.setMessage(MSG_BLOCK_RETURN_SUCCESSFULLY);
		((CustomSuccessResponse<BlockResponseDTO>) response).setResultSet(blockResponseDTO);
		logger.info(MSG_BLOCK_RETURN_SUCCESSFULLY + " for transaction id " + combineKey + " Block data "
				+ block.toString());
		reportLogger
				.fatal(MSG_BLOCK_RETURN_SUCCESSFULLY + " for transaction id " + getMessageRequest.getTransactionId());
		return response;
	}

	@ResponseBody
	@RequestMapping(value = ACKNOWLEDGE_DELETE_DATA, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public CustomResponse<String> acknowledgeData(
			@RequestBody @NotNull @Validated final AcknowledgeRequest acknowledgeRequest) {
		final String combinedKey = acknowledgeRequest.getTag() + acknowledgeRequest.getTransactionId();
		logger.info("Acknowledge request for data with combined Key: " + combinedKey + " and crc: "
				+ acknowledgeRequest.getCrc());
		reportLogger
				.fatal("Data successfully received by receiver. Validating acknowledge to delete block from blockchain for transaction id "
						+ acknowledgeRequest.getTransactionId());
		CustomResponse<String> customResponse = null;
		final BlockStatus blockStatus = blockService.getBlockStatus(acknowledgeRequest.getTag(),
				acknowledgeRequest.getTransactionId());
		if (null == blockStatus) {
			reportLogger.fatal("No block found for transaction id : " + acknowledgeRequest.getTransactionId());
			logger.warn("Block was not found for given combined key: " + combinedKey);
			customResponse = new CustomErrorResponse<>();
			customResponse.setMessage(NO_MESSAGE_FOR_TXNID);
			return customResponse;
		}
		if (blockStatus.getStatus().equals(BlockStatusEnum.DELETED.name())) {
			logger.info("As block was already deleted so returning success response for combine key " + combinedKey);
			reportLogger
					.fatal("Block is already deleted for transaction id : " + acknowledgeRequest.getTransactionId());
			customResponse = new CustomSuccessResponse<>();
			customResponse.setMessage(BLOCK_ALREADY_DELETED);
			return customResponse;
		}
		final Block block = blockService.getBlock(combinedKey);
		final boolean isValidCRC = transactionFactory.getObject().verifyCRCAndDelete(block, acknowledgeRequest);
		if (!isValidCRC) {
			logger.warn(
					"CRC sent does not match with calculated crc it's not gonna process it further for combined key: "
							+ combinedKey);
			reportLogger.fatal("CRC validation failed for transaction id : " + acknowledgeRequest.getTransactionId());
			customResponse = new CustomErrorResponse<>();
			customResponse.setMessage(MSG_NOT_VALID);
			return customResponse;
		}
		reportLogger.fatal("For acknowledge reqest CRC validated successfully for transaction id : "
				+ acknowledgeRequest.getTransactionId());
		logger.info("For acknowledge reqest CRC validated successfully for combined key: " + combinedKey);
		final boolean flag = blockService.checkAndNotify(acknowledgeRequest.getCrc(), acknowledgeRequest.getTag(),
				acknowledgeRequest.getTransactionId());
		if (!flag) {
			reportLogger.fatal(DELETED_ALRDY_RECEIVED + " for transaction id " + acknowledgeRequest.getTransactionId());
			logger.info(DELETED_ALRDY_RECEIVED + " for combine key " + combinedKey);
			customResponse = new CustomErrorResponse<>();
			customResponse.setMessage(DELETED_ALRDY_RECEIVED);
			return customResponse;
		}
		customResponse = new CustomSuccessResponse<>();
		logger.info(BLOCK_DELETED_SUCCESSFULLY + " for combine key " + combinedKey);
		customResponse.setMessage(BLOCK_DELETED_SUCCESSFULLY);
		return customResponse;
	}

	@ResponseBody
	@RequestMapping(value = GET_FILE, method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public FileSystemResource getFilePath(@RequestParam("fileId") final String fileId) {
		logger.info("File download was requested for fileId: " + fileId);
		return blockService.getDownloadFile(fileId);
	}

	@ResponseBody
	@RequestMapping(value = GET_BLOCKS, method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Object getBlockForReceiver(@RequestBody @NotNull final ReceiverAddresses receiverAddresses) {
		logger.info("Get all blocks request for receiver address: " + receiverAddresses);
		final List<String> addresses = receiverAddresses.getReceiverAddress().stream()
				.filter(address -> !address.isEmpty()).collect(Collectors.toList());
		if (null == addresses || addresses.isEmpty()) {
			logger.info("List or receiver addresses were empty or null.");
			final CustomResponse<String> customResponse = new CustomErrorResponse<>();
			customResponse.setMessage(RECEIVER_ADDR_VALIDATION);
			return customResponse;
		}
		final CustomSuccessResponse<Object> response = new CustomSuccessResponse<>();
		response.setMessage(MSG_BLOCK_RETURN_SUCCESSFULLY);
		response.setResultSet(blockService.getBlockList(addresses));
		return response;
	}

	@ResponseBody
	@RequestMapping(value = GET_LOG, method = RequestMethod.GET, consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Object getLog(@RequestParam("pointerLocation") final String pointerLocation) {
		return blockService.readLog(Long.parseLong(pointerLocation));
	}

	@ResponseBody
	@RequestMapping(value = GET_STATISTICS, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Object getAllBlockDetail(@RequestParam("pageNo") final int pageNo) {
		CustomResponse<StatisticsModel> customResponse = null;
		if (pageNo <= 0) {
			customResponse = new CustomErrorResponse<>();
			customResponse.setMessage("Page No value must be greater than or equal to 1");
			return customResponse;
		}
		customResponse = new CustomSuccessResponse<>();
		customResponse.setMessage("Block status result for page no - " + pageNo);
		final StatisticsModel model = new StatisticsModel().setTotalBlocks(blockService.totalBlocks())
				.setSavedCount(blockService.availableBlocks()).setDeletedCount(blockService.getDeletedBlockCount())
				.setBlocks(blockService.getBlockStatusListByPage(pageNo));

		((CustomSuccessResponse<StatisticsModel>) customResponse).setResultSet(model);
		return customResponse;
	}
}
