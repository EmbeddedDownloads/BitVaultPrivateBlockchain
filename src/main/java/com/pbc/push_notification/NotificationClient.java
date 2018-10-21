package com.pbc.push_notification;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;

import com.pbc.push_notification.models.PushNotificationDTO;

@Controller
public class NotificationClient {

	private static final Logger logger = Logger.getLogger(NotificationClient.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	private final RestTemplate restTemplate = new RestTemplate();

	// Rest services
	private static final String NOTIFICATION_URL = "http://34.209.234.181/v1/send-notification";
	PushNotificationDTO data;

	public void push(final PushNotificationDTO data) {
		// this.data = data;
		// sender.setInitialPushData(data.getTag(), data.getData());
		// sender.sendPbcSyncNotification();
		try {
			logger.info("Push notification send to device for transaction id : " + data.toString());
			reportLogger.fatal("Push notification send to device for transaction id : " + data.getTransaction_id());

			final String response = restTemplate.postForObject(NOTIFICATION_URL, data, String.class);
			logger.info("Get response after send push notificaiton " + response);
		} catch (final Exception e) {
			logger.error("Exception in send push notification ", e);
		}

	}

	public void pushAfterValidating() {

	}
}
