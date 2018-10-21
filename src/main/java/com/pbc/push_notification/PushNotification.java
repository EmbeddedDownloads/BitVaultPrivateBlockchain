package com.pbc.push_notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pbc.blockchain.BlockContent;
import com.pbc.push_notification.models.PushNotificationDTO;

@Component
public abstract class PushNotification {

	@Autowired
	NotificationClient notificationClient;

	final String TAG_SECURE_MESSAGE = "secure_message";
	final String TAG_A2A_SESSIONKEY = "A2A_Sessionkey";
	final String TAG_A2A_FILE = "A2A_File";
	final String TAG_FILE_NOTIFICATION = "A2A_FileNotification";
	final String TAG_B2A_FILENOTIFICATION = "B2A_FileNotification";

	public abstract boolean validate(String appId);

	public abstract PushNotificationDTO constructNotificationMessage(BlockContent blockContent);

	public abstract boolean createObject(String tag);

	public void send(final PushNotificationDTO message) {
		notificationClient.push(message);
	}
}
