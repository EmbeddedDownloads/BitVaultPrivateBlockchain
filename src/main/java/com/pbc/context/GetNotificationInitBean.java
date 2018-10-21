package com.pbc.context;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.pbc.threads.SaveBlockServerSocketThread;

@Configuration
public class GetNotificationInitBean {

	@Autowired
	private SaveBlockServerSocketThread saveSocketThread;

	// start thread to broadcast some message on a port always open.
	@PostConstruct
	public void getNotification() {
		saveSocketThread.setDaemon(true);
		saveSocketThread.start();
	}
}