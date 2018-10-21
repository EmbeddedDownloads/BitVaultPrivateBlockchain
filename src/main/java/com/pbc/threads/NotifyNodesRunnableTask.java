package com.pbc.threads;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.pbc.models.CompleteRequest;
import com.pbc.notification.NotificationSender;
import com.pbc.utility.JSONObjectEnum;

/**
 * This task will notify other nodes using the server sockets. This task shares
 * a common property with all other tasks in project with modified setters. This
 * modified setters help to remove verbose lines to set the different instance
 * properties.
 *
 */
@Scope("prototype")
@Component
public class NotifyNodesRunnableTask implements Runnable {

	private String crc;
	private String tag;
	private boolean isDelete;
	private String transactionId;

	private static final Logger logger = Logger.getLogger(NotifyNodesRunnableTask.class);

	public NotifyNodesRunnableTask setCrc(final String crc) {
		this.crc = crc;
		return this;
	}

	public NotifyNodesRunnableTask setTag(final String tag) {
		this.tag = tag;
		return this;
	}

	public NotifyNodesRunnableTask setDelete(final boolean isDelete) {
		this.isDelete = isDelete;
		return this;
	}

	public NotifyNodesRunnableTask setTransactionId(final String transactionId) {
		this.transactionId = transactionId;
		return this;
	}

	@Override
	public void run() {
		logger.info("inside NotifyNodesRunnableTask::" + tag + transactionId);
		final CompleteRequest completeRequest = new CompleteRequest(crc, transactionId, tag);
		final NotificationSender notificationSender = new NotificationSender();
		notificationSender.setCompleteRequest(completeRequest);
		notificationSender.notifyAllHosts(isDelete, JSONObjectEnum.CRC, false);
	}
}
