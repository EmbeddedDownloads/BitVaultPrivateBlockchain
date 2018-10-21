package com.pbc.context;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.pbc.threads.DeleteBlockServerSocketThread;

@Configuration
public class DeleteNotificationInitBean {

	@Autowired
	private DeleteBlockServerSocketThread deleteBlockThread;

	@PostConstruct
	public void listenOnDeletePort() {
		deleteBlockThread.setDaemon(true);
		deleteBlockThread.start();
	}
}
