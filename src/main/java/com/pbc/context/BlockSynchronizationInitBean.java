package com.pbc.context;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.pbc.job.BlockDataReceiverThread;
import com.pbc.job.BlockSynchronizationThread;
import com.pbc.job.TxnReceiverBlockSenderThread;

@Configuration
public class BlockSynchronizationInitBean {

	@Autowired
	private BlockSynchronizationThread blockSynchronizationThread;

	@Autowired
	private TxnReceiverBlockSenderThread txnReceiverBlockSender;

	@Autowired
	private BlockDataReceiverThread blockDataReceiver;

	@PostConstruct
	public void listenOnSynchronizationPort() {

		blockSynchronizationThread.setDaemon(true);
		blockSynchronizationThread.start();

		txnReceiverBlockSender.setDaemon(true);
		txnReceiverBlockSender.start();

		blockDataReceiver.setDaemon(true);
		blockDataReceiver.start();
	}
}
