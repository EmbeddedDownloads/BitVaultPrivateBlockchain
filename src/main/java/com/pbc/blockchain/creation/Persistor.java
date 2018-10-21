package com.pbc.blockchain.creation;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * It provides the actual prototype for persisting blocks. A single file json
 * persistor will persist into single file. But this is the base type will be
 * available to every implementation.
 *
 * @see SingleFileJsonPersistor
 *
 * @param <T>
 */
public interface Persistor<T> {

	String SEPARATOR = "\n\n+++++++++++++++++++++++++++++++++++++++++++\n\n";
	long SEPARATOR_SIZE = SEPARATOR.getBytes().length;
	String SINGLE_SPACE = " ";
	String TRIMMED_LINE = SEPARATOR.trim();
	ReadWriteLock globalLock = new ReentrantReadWriteLock();

	ObjectMapper om = new ObjectMapper();

	void addBlocks(List<T> blocks);

	void addBlock(T block);

	T getBlock(String hash);

	void removeBlockWithHash(String hash);

	void initializeNodeStartup();

}