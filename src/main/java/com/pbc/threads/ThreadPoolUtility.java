package com.pbc.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class has sole purpose of creating thread pool that will be used for
 * saving blocks. We surely don't intend to create a new thread to save block
 * for every request. A fixed thread pool is suitable for scenarios.
 *
 */
public class ThreadPoolUtility {

	private static ExecutorService executorService;

	private ThreadPoolUtility() {
		// Instantiation of this class is not permitted.
		throw new UnsupportedOperationException();
	}

	static {
		// This approach is platform dependent but intended to be in that way.
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	public static ExecutorService getThreadPool() {
		return executorService;
	}
}
