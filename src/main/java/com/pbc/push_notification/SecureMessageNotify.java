package com.pbc.push_notification;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.pbc.blockchain.BlockContent;
import com.pbc.push_notification.models.PushNotificationDTO;

@Component
public class SecureMessageNotify extends PushNotification {

	private static final Logger logger = Logger.getLogger(SecureMessageNotify.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	public SecureMessageNotify() {
		// Default constructor
	}

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
		logger.info("Block to convert for PushNotification: " + blockContent.toString());
		pNotificationDTO.setWeb_server_key(blockContent.getWebServerKey())
				.setReceiver_address(blockContent.getPublicAddressOfReciever())
				.setSender_address(blockContent.getSender()).setTag(blockContent.getTag())
				.setData(blockContent.getHashTxnId()).setTransaction_id(blockContent.getHashTxnId());
		logger.info("parsebleBlockdTO returned to send pushNotification: " + pNotificationDTO.toString());
		return pNotificationDTO;
	}

	@Override
	public boolean createObject(final String tag) {
		return TAG_SECURE_MESSAGE.equals(tag) || TAG_B2A_FILENOTIFICATION.equals(tag);
	}
}