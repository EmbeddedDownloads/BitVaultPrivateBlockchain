package com.pbc.push_notification.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pbc.blockchain.Block;
import com.pbc.blockchain.BlockContent;
import com.pbc.blockchain.creation.Persistor;
import com.pbc.push_notification.PushNotification;
import com.pbc.push_notification.models.PushNotificationDTO;

@Service
public class PNService {

	private static final Logger logger = Logger.getLogger(PNService.class);
	@Autowired
	private Persistor<Block> jsonPersistor;

	@Autowired
	private List<PushNotification> pushNotification;

	// @Autowired
	// private SingleFileJsonPersistor<Block> jsonPersistor;

	final String TAG_SECURE_MESSAGE = "secure_message";
	final String TAG_A2A_SESSIONKEY = "A2A_Sessionkey";
	final String TAG_A2A_FILE = "A2A_File";
	final String TAG_B2A_FILENOTIFICATION = "B2A_FileNotification";

	final Map<String, Integer> map = new HashMap<>();

	public void sendNotification(final List<Block> blocks) {
		logger.info("Validating block for send notification");
		BlockContent blockContent = null;

		for (final Block block : blocks) {
			logger.info("Block received to notify : " + block.toString());
			try {
				blockContent = block.getBlockContent();
				if (null == blockContent) {
					logger.info("block content was null");
					return;
				}
				logger.info("BlockContent retrived from Block is: " + blockContent);
				if (blockContent.getTag().equals(TAG_SECURE_MESSAGE)
						|| blockContent.getTag().equals(TAG_B2A_FILENOTIFICATION)) {
					processNotify(blockContent);
				} else {
					if (map.get(blockContent.getHashTxnId()) == null) {
						// Nothing present in this map for this transaction id,
						// now putting count 1.
						map.put(blockContent.getHashTxnId(), 1);
					} else {
						final Block blockForSessionKey = jsonPersistor
								.getBlock(TAG_A2A_SESSIONKEY + blockContent.getHashTxnId());

						processNotify(blockForSessionKey.getBlockContent());
						map.remove(blockContent.getHashTxnId());
					}
				}
			} catch (final Exception e) {
				logger.error("Problem in sending Push Notification " + e.getMessage());
			}
		}
	}

	public void processNotify(final BlockContent blockContent) {
		final PushNotification notification = objectForPushNotification(blockContent);
		if (notification != null) {
			logger.info("Validating notification for transaction id " + blockContent.getHashTxnId());
			if (notification.validate(blockContent.getAppId())) {
				logger.info(
						"Processing data to send push notification for transaction id " + blockContent.getHashTxnId());
				final PushNotificationDTO dto = notification.constructNotificationMessage(blockContent);
				notification.send(dto);
			}
		}
	}

	public PushNotification objectForPushNotification(final BlockContent blockContent) {
		for (final PushNotification notification : pushNotification) {
			if (notification.createObject(blockContent.getTag())) {
				return notification;
			}
		}
		logger.warn("tag not match for transaction id " + blockContent.getHashTxnId());
		return null;
	}
}
