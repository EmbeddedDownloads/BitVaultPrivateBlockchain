package com.pbc.push_notification;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.pbc.blockchain.BlockContent;
import com.pbc.push_notification.models.PushNotificationDTO;

@Component
public class ApplicationMessageNotify extends PushNotification {

	private static final Logger logger = Logger.getLogger(ApplicationMessageNotify.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	@Override
	public boolean validate(final String appId) {
		if (appId != null) {
			logger.info("Block data validated to send push notification.");
			reportLogger.fatal("Block data validated to send push notification.");
			return true;
		} else {
			logger.info("Block data is invalid to send push notification.");
			reportLogger.fatal("Block data is invalid to send push notification.");
			return false;
		}
	}

	@Override
	public PushNotificationDTO constructNotificationMessage(final BlockContent blockContent) {
		final PushNotificationDTO pNotificationDTO = new PushNotificationDTO();

		pNotificationDTO.setWeb_server_key(blockContent.getWebServerKey())
				.setReceiver_address(blockContent.getPublicAddressOfReciever())
				.setSender_address(blockContent.getSender()).setTag(TAG_FILE_NOTIFICATION)
				.setData(blockContent.getHashTxnId()).setTransaction_id(blockContent.getHashTxnId());

		return pNotificationDTO;
	}

	@Override
	public boolean createObject(final String tag) {
		if (tag.equals(TAG_A2A_SESSIONKEY) || tag.equals(TAG_A2A_FILE)) {
			return true;
		} else {
			return false;
		}
	}
}