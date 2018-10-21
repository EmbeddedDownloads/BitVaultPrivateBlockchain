package com.pbc.context;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

@Configuration
public class StartPushNotificationBean {

	// @Autowired
	// ReceiveTimeStampNotification receiveTimeStampNotification;
	// @Autowired
	// PushNotificationTxnIdReceiver notificationTxnIdReceiver;

	@PostConstruct
	public void startPushNotificationThread() {
		// // receiveTimeStampNotification.setDaemon(true);
		// receiveTimeStampNotification.start();
		//
		// // notificationTxnIdReceiver.setDaemon(true);
		// notificationTxnIdReceiver.start();
	}
}
