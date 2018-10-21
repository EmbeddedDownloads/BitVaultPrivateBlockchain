package com.pbc.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlockSynchronizationThread extends Thread {

	@Autowired
	private BlockSynchronizationReceiver receiver;

	@Override
	public void run() {
		receiver.listenSynchronizationPort();
	}
}
