package com.pbc.blockchain.creation;

import static com.pbc.utility.ConfigConstants.FOLDER_PATH;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.pbc.blockchain.Block;
import com.pbc.blockchain.BlockContent;
import com.pbc.blockchain.BlockHeader;
import com.pbc.exception.BlockProcessingException;
import com.pbc.utility.ConfigConstants;

@Component
public class SingleFileJsonPersistor<T> implements Persistor<T> {

	private static final Logger logger = Logger.getLogger(SingleFileJsonPersistor.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	private static long lastByteLocation = 0l;

	// location pointers for pointers to block address.
	private final Map<String, Long> pointerMap = new ConcurrentHashMap<>();

	// size of each block associated with the blocks.
	private final Map<String, Integer> sizeMap = new ConcurrentHashMap<>();

	// TreeSet to maintain block order sorted on behalf of time stamp
	private final TreeSet<BlockContent> orderedBlockSet = new TreeSet<>();

	/**
	 * List to track which block was actually inserted after which one. In
	 * absence of this list it will be hard(Obviously there are ways but not
	 * faster than this one) to find out which is the actual list containing
	 * actual data.
	 */
	private final List<String> hashList = new CopyOnWriteArrayList<>();

	private final String BLOCKCHAIN_CONTROLLER = ConfigConstants.BLOCKCHAIN_CONTROLLER;

	public TreeSet<BlockContent> getOrderedSet() {
		return orderedBlockSet;
	}

	public List<String> getHashList() {
		return hashList;
	}

	@Override
	public void addBlocks(final List<T> blocks) {
		for (final T block : blocks) {
			addBlock(block);
		}
	}

	@Override
	public void addBlock(final T block) {
		final BlockContent blockContent = ((Block) block).getBlockContent();
		try {
			final StringBuilder sb = new StringBuilder();
			long nextBlockLocation = 0;
			long writeFrom = 0;
			int currentBlockLength = 0, higherBlockLength = 0, prevLenOfHigherBlock = 0, replacement = 0,
					insertHashAt = 0;

			final BlockHeader blockHeader = ((Block) block).getHeader();
			orderedBlockSet.add(blockContent);
			// Fetching block content object which time stamp is just less than
			// current block content
			final BlockContent lowerBlockContent = orderedBlockSet.lower(blockContent);
			if (null != lowerBlockContent) {
				final String lowerBlockKey = lowerBlockContent.getTag() + lowerBlockContent.getHashTxnId();
				final long lowerBlockPointerPosition = pointerMap.get(lowerBlockKey);
				final Block lowerBlock = (Block) getBlock(lowerBlockKey);
				final String lowerBlockString = om.writeValueAsString(lowerBlock);
				// Calculating next location for block
				writeFrom = nextBlockLocation = lowerBlockPointerPosition + lowerBlockString.getBytes().length;
				blockHeader.setPrevHash(lowerBlock.getBlockHash());
				insertHashAt = hashList.indexOf(lowerBlockKey) + 1;
			} else {
				blockHeader.setPrevHash(null);
			}
			final String txnIdPlusTag = ((Block) block).getBlockContent().getTag()
					+ ((Block) block).getBlockContent().getHashTxnId();

			final String blockString = om.writeValueAsString(block);
			currentBlockLength = blockString.getBytes().length;
			// Fetching block content object which time stamp is just greater
			// than current block content
			String higherBlockString = "";
			final BlockContent higherBlockContent = orderedBlockSet.higher(blockContent);
			if (null != higherBlockContent) {
				final String higherBlockKey = higherBlockContent.getTag() + higherBlockContent.getHashTxnId();
				final Block higherBlock = (Block) getBlock(higherBlockKey);
				final BlockHeader higherBlockHeader = higherBlock.getHeader();
				higherBlockHeader.setPrevHash(((Block) block).getBlockHash());
				higherBlockString = om.writeValueAsString(higherBlock);
				higherBlockLength = higherBlockString.getBytes().length;

				prevLenOfHigherBlock = sizeMap.replace(higherBlockKey, higherBlockLength);

				final long higherBlockPosition = pointerMap.get(higherBlockKey);

				nextBlockLocation = higherBlockPosition + prevLenOfHigherBlock;

				replacement = prevLenOfHigherBlock - higherBlockLength;
				pointerMap.replace(higherBlockKey, higherBlockPosition + currentBlockLength + SEPARATOR_SIZE);
				for (final BlockContent content : orderedBlockSet.tailSet(higherBlockContent, false)) {
					final String key = content.getTag() + content.getHashTxnId();
					pointerMap.replace(key, pointerMap.get(key) + SEPARATOR_SIZE + currentBlockLength - replacement);
				}
			}
			sb.append(SEPARATOR).append(blockString);
			if (!higherBlockString.isEmpty()) {
				sb.append(SEPARATOR).append(higherBlockString);
			}
			addBlockAt(writeFrom, nextBlockLocation, sb.toString());
			pointerMap.put(txnIdPlusTag, writeFrom + SEPARATOR_SIZE);
			sizeMap.put(txnIdPlusTag, currentBlockLength);
			hashList.add(insertHashAt, txnIdPlusTag);
			logger.info("Added block for this combination key : " + txnIdPlusTag);
			reportLogger.fatal("Current block chain size is :: " + orderedBlockSet.size());
			sb.setLength(0);
		} catch (final IOException ioe) {
			orderedBlockSet.remove(blockContent);
			logger.error("Problem into writing block data ", ioe);
			throw new BlockProcessingException("Problem into writing block data ", ioe);
		} catch (final Exception e) {
			orderedBlockSet.remove(blockContent);
			logger.error("An Exception occured while adding block in data ", e);
			throw new BlockProcessingException("An Exception occured while adding block in data ", e);
		}
	}

	/**
	 * Use to add a block data at a specific pointer location
	 *
	 * @param pointerLocation
	 * @param blockString
	 * @throws IOException
	 */
	public void addBlockAt(final long pointerLocation, final long copyFrom, final String blockString)
			throws IOException {
		final RandomAccessFile raf = new RandomAccessFile(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER), "rw");
		final RandomAccessFile rafTemp = new RandomAccessFile(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER + "~"),
				"rw");
		final long fileSize = raf.length();
		final FileChannel sourceChannel = raf.getChannel();
		final FileChannel targetChannel = rafTemp.getChannel();
		sourceChannel.transferTo(copyFrom, (fileSize - copyFrom), targetChannel);
		sourceChannel.truncate(pointerLocation);
		raf.seek(pointerLocation);
		raf.write(blockString.getBytes());
		final long newOffset = raf.getFilePointer();
		targetChannel.position(0L);
		sourceChannel.transferFrom(targetChannel, newOffset, (fileSize - copyFrom));
		sourceChannel.close();
		targetChannel.close();
		raf.close();
		rafTemp.close();
	}

	/**
	 * Get block for provided block hash.
	 *
	 * @param hash
	 * @return T
	 */
	@Override
	public T getBlock(final String hash) {
		globalLock.readLock().lock();
		try {
			final Long locationOfBlock = pointerMap.get(hash);

			if (locationOfBlock == null) {
				// block is already removed or it does not exist.
				return null;
			}
			logger.info("Block data successfully retrieved for this combination key : " + hash);
			final T block = getBlock(pointerMap.get(hash), sizeMap.get(hash));
			return block;
		} catch (final Exception e) {
			logger.error("Unable to get block ", e);
			throw new BlockProcessingException("Unable to get block ", e);
		} finally {
			globalLock.readLock().unlock();
		}
	}

	/**
	 * Get block from the provided position to addition of Block size.
	 *
	 * @param pointerLocation
	 * @param blockSize
	 * @return T
	 */
	@SuppressWarnings("unchecked")
	private T getBlock(final long pointerLocation, final long blockSize) {
		String blockData = null;
		try (RandomAccessFile raf = new RandomAccessFile(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER), "rw")) {
			raf.seek(pointerLocation);
			final byte[] blockBytes = new byte[(int) blockSize];
			raf.read(blockBytes, 0, (int) blockSize);
			blockData = new String(blockBytes, "UTF-8");
			logger.info("Block data  ::  " + blockData);
			return (T) om.readValue(blockData, Block.class);
		} catch (final FileNotFoundException fne) {
			logger.error("File path provided is not valid path or the access is not provided.", fne);
			throw new BlockProcessingException("File path provided is not valid path or the access is not provided.",
					fne);
		} catch (final IOException ioe) {
			logger.error("I/O Error while trying to get block", ioe);
			throw new BlockProcessingException("I/O Error while trying to get block", ioe);
		} catch (final Exception e) {
			logger.error("Block data was :: " + blockData);
			logger.error("Error occured during fetching the block data", e);
			throw new BlockProcessingException("Error occured during fetching the block data", e);
		}
	}

	/**
	 * Method is intentionally private and should not be called from any other
	 * method which does not handle thread safety on it's own.
	 *
	 * @param block
	 */
	private void updateBlock(final String blockValue, final long pointerLocation) {
		try (RandomAccessFile raf = new RandomAccessFile(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER), "rw")) {
			raf.seek(pointerLocation);
			raf.write(blockValue.getBytes());
			logger.info("Block updated after removing");
		} catch (final FileNotFoundException fne) {
			logger.error("File path provided is not valid path or the access is not provided.", fne);
		} catch (final IOException ioe) {
			logger.error("I/O Error while performing operation ", ioe);
		}
	}

	/**
	 * Remove block data from the Block chain for provided block hash.
	 *
	 * @param hash
	 */
	@Override
	public void removeBlockWithHash(final String hash) {
		try {
			globalLock.writeLock().lock();
			Long locationOfHash = pointerMap.get(hash);
			if (locationOfHash == null) {
				logger.warn("Deletion Time Pointer location is null for combineKey " + hash);
				return; // already removed or block does not exist.
			}
			/**
			 * This code snippet is being used to break links of blocks
			 *
			 ************ If Block is used in case of first Block in block chain.
			 *
			 ************ Second block(else if) one is being used in case of last block in
			 * block chain.
			 *
			 ************ Third block (else) is used for breaking links as this block is in
			 * middle.
			 */
			final int indexOfHash = hashList.indexOf(hash);
			final int sizeOfCurrentBlock = sizeMap.get(hash);
			final Block block = (Block) getBlock(locationOfHash, sizeOfCurrentBlock);
			logger.info("block retreiving at the time of deletion::" + block.toString());
			final String prevBlockHash = block.getHeader().getPrevHash();
			long updationPointer = 0;
			final long nextBlockLocation = locationOfHash + sizeOfCurrentBlock + SEPARATOR_SIZE;
			Block nextBlock = null;
			boolean isLastBlock = false;
			if (prevBlockHash == null) {
				if (hash.equals(hashList.get(hashList.size() - 1))) {
					logger.info("This block is last block as well as first block. " + hash);
					// This block is last block as well as first block.
					updationPointer = locationOfHash + sizeOfCurrentBlock;
					isLastBlock = true;
				} else {
					// It means this block is ideally first block of the
					// Block chain.
					logger.info("It means this block is ideally first block " + hash);
					nextBlock = (Block) getBlock(nextBlockLocation, sizeMap.get(hashList.get(indexOfHash + 1)));
					nextBlock.getHeader().setPrevHash(null);
				}
			} else if (hash.equals(hashList.get(hashList.size() - 1))) {
				// It means this block is last block to be deleted.
				// In this case all we have to do is just change the last block
				// hash value.
				logger.info("This block is last block to be deleted " + hash);
				updationPointer = locationOfHash + sizeOfCurrentBlock;
				isLastBlock = true;
			} else {
				nextBlock = (Block) getBlock(nextBlockLocation, sizeMap.get(hashList.get(indexOfHash + 1)));
				nextBlock.getHeader().setPrevHash(prevBlockHash);
			}
			if (nextBlock != null) {
				final int sizeOfNextBlock = sizeMap
						.get(nextBlock.getBlockContent().getTag() + nextBlock.getBlockContent().getHashTxnId());
				final String updatedNextBlockString = om.writeValueAsString(nextBlock);
				final int updateSizeOfNextBlock = updatedNextBlockString.getBytes().length;
				updationPointer = nextBlockLocation - (updateSizeOfNextBlock - sizeOfNextBlock);
				sizeMap.replace(nextBlock.getBlockContent().getTag() + nextBlock.getBlockContent().getHashTxnId(),
						updateSizeOfNextBlock);
				updateBlock(updatedNextBlockString, updationPointer);
			}
			/**
			 * Now we can safely remove this block from block chain.
			 */
			logger.info("Removing Block for combineKey " + hash);
			long totalReplacement = updationPointer - locationOfHash;
			if (isLastBlock) {
				locationOfHash -= SEPARATOR_SIZE;
				totalReplacement += SEPARATOR_SIZE;
			}
			removeBlockAt(locationOfHash, totalReplacement);
			// Change the pointer map.
			changeNextBlockReferences(indexOfHash, locationOfHash, totalReplacement);
			// remove references
			removeAllReferences(hash, block);
			logger.info("Block data removed for combination key: " + hash);
		} catch (final Exception e) {
			logger.error("Error while removing data from block chain for combined key: " + hash, e);
			throw new BlockProcessingException("Error while removing data from block chain for combined key: " + hash,
					e);
		} finally {
			globalLock.writeLock().unlock();
		}
	}

	private void changeNextBlockReferences(final int indexOfHash, final long locationOfHash,
			final long totalReplacement) {
		final List<String> nextHashSubList = hashList.subList(indexOfHash + 1, hashList.size());
		boolean firstNextBlock = true;
		for (final String nextHash : nextHashSubList) {
			if (firstNextBlock) {
				firstNextBlock = false;
				pointerMap.replace(nextHash, locationOfHash);
				continue;
			}
			pointerMap.replace(nextHash, pointerMap.get(nextHash) - totalReplacement);
		}
	}

	/**
	 * Clear all entries for the deleted block.
	 *
	 * @param hash
	 * @param block
	 */
	private void removeAllReferences(final String hash, final Block block) {
		hashList.remove(hash);
		pointerMap.remove(hash);
		sizeMap.remove(hash);
		orderedBlockSet.remove(block.getBlockContent());
	}

	/**
	 * Remove block data from provided location to block size.
	 *
	 * @param pointerLocation
	 * @param blockSize
	 * @throws IOException
	 */
	private void removeBlockAt(final long pointerLocation, final long blockSize) throws IOException {
		final RandomAccessFile raf = new RandomAccessFile(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER), "rw");
		final RandomAccessFile rtemp = new RandomAccessFile(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER + "~"), "rw");
		final long fileSize = raf.length();
		final FileChannel sourceChannel = raf.getChannel();
		final FileChannel targetChannel = rtemp.getChannel();
		final long afterObjectLocation = pointerLocation + blockSize;
		sourceChannel.transferTo(afterObjectLocation, (fileSize - afterObjectLocation), targetChannel);
		sourceChannel.truncate(pointerLocation);
		raf.seek(pointerLocation);
		targetChannel.position(0L);
		sourceChannel.transferFrom(targetChannel, pointerLocation, (fileSize - afterObjectLocation));
		sourceChannel.close();
		targetChannel.close();
		raf.close();
		rtemp.close();
	}

	/**
	 * Runs when node/server start. Initialize all the map and set with block
	 * chain data.
	 */
	@Override
	public void initializeNodeStartup() {
		if (!Files.exists(Paths.get(FOLDER_PATH + BLOCKCHAIN_CONTROLLER), LinkOption.NOFOLLOW_LINKS)) {
			logger.warn("The specified temp controller file does not exist at given path location.");
			logger.warn("Make sure you are starting server first time or you intend to this way.");
			return;
		}
		globalLock.writeLock().lock();
		try (BufferedReader br = new BufferedReader(new FileReader(new File(FOLDER_PATH + BLOCKCHAIN_CONTROLLER)))) {
			logger.info("Initializing node startup with different indexes.");
			final StringBuilder sb = new StringBuilder();
			String str = null;
			while ((str = br.readLine()) != null && (!str.equals(SEPARATOR.trim()) || str.trim().isEmpty())) {
				// skip first separator anyway.
			}
			while ((str = br.readLine()) != null) {
				if (str.equals(SEPARATOR.trim())) {
					initializeSkeleton(sb.toString());
					sb.setLength(0);
					continue;
				}
				sb.append(str);
			}
			initializeSkeleton(sb.toString());
		} catch (final IOException e) {
			logger.error(
					"Some error occured while initializing the server node and previous block chain. @See Full Stack Trace",
					e);
		} finally {
			globalLock.writeLock().unlock();
		}
	}

	/**
	 * Initializing the map and set.
	 *
	 * @param blockString
	 */
	private void initializeSkeleton(final String blockString) {
		if (blockString.trim().isEmpty()) {
			return;
		}
		try {
			int sizeOfCurrentBlock = 0;
			final Block block = om.readValue(blockString, Block.class);
			final BlockContent blockContent = block.getBlockContent();
			final String combinedKey = blockContent.getTag() + blockContent.getHashTxnId();
			pointerMap.put(combinedKey, (lastByteLocation += SEPARATOR_SIZE));
			lastByteLocation += (sizeOfCurrentBlock = blockString.trim().getBytes().length);
			sizeMap.put(combinedKey, sizeOfCurrentBlock);
			hashList.add(combinedKey);
			orderedBlockSet.add(blockContent);
		} catch (final Exception e) {
			logger.error("Error while parsing block or adding to block chain map.", e);
		}
	}
}