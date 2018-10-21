package com.pbc.job;

import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.blockchain.Block;
import com.pbc.blockchain.creation.Persistor;
import com.pbc.blockchain.creation.SingleFileJsonPersistor;
import com.pbc.service.TransactionMessageService;

@Configuration
@EnableAsync
@EnableScheduling
public class BlockChainSynchronizationJob {

	private static final Logger logger = Logger.getLogger(BlockChainSynchronizationJob.class);

	@Autowired
	private Persistor<Block> persistor;

	@Autowired
	private TransactionMessageService transactionMessageService;

	@Autowired
	private BlockSynchronizationSender synchronizationSender;

	@Autowired
	private SyncProcessThread processThread;

	@Autowired
	private ProcessAllMapReceived allMapReceived;

	private final ObjectMapper om = new ObjectMapper();

	@Bean
	public Executor getExecutor() {
		final SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(2);
		return executor;
	}

	@Async
	@Scheduled(cron = "0 0/15 0-23 * * *")
	public void startSynchronizationProcess() {
		logger.info("Scheduler start for block synchronization");

		SingleFileJsonPersistor<Block> singleFileJsonPersistor = null;
		if (persistor instanceof SingleFileJsonPersistor<?>) {
			singleFileJsonPersistor = (SingleFileJsonPersistor<Block>) persistor;
		}
		if (null != singleFileJsonPersistor) {
			getExecutor().execute(processThread);
			final Thread thread = new Thread(allMapReceived);
			thread.start();
			final PbcSyncModel pbcSyncModel = new PbcSyncModel(singleFileJsonPersistor.getOrderedSet(),
					transactionMessageService.getConfirmationMap(), singleFileJsonPersistor.getHashList());
			try {
				synchronizationSender.setInitialSyncData(om.writeValueAsString(pbcSyncModel));
				synchronizationSender.sendPbcSyncNotification();
			} catch (final JsonProcessingException jpe) {
				logger.error("Can not write value as string : ", jpe);
			} catch (final Exception e) {
				logger.error("Problem constructing initial synchronization data ", e);
			}
		}
	}
}
