package com.pbc.threads;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.pbc.notification.NotificationReceiver;
import com.pbc.utility.ConfigConstants;

/**
 * This thread will listen notification at given server socket for delete
 * operation. This is part of configuration so will be initialized with
 * application context.
 *
 */
@Configuration
public class DeleteBlockServerSocketThread extends Thread {

	@Autowired
	private NotificationReceiver notificationReceiver;

	@Override
	public void run() {
		notificationReceiver.listenNotification(ConfigConstants.PORT_NO_DELETE);
	}
}
