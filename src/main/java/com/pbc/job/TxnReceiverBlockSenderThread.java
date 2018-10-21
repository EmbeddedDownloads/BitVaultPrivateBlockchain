package com.pbc.job;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pbc.blockchain.Block;
import com.pbc.models.GetStatusRequest;
import com.pbc.repository.model.BlockStatus;
import com.pbc.service.BlockService;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.StringConstants;

@Component
public class TxnReceiverBlockSenderThread extends Thread {

	private static Logger logger = Logger.getLogger(TxnReceiverBlockSenderThread.class);

	@Autowired
	private BlockService blockService;

	private final ObjectMapper mapper = new ObjectMapper();

	private final Map<GetStatusRequest, String> mapForStatus = new HashMap<>();
	Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create();

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(ConfigConstants.PORT_NO_TAGTXD_SEND);
			while (true) {
				final Socket socket;
				try {
					socket = serverSocket.accept();
					socket.setSoTimeout(1000 * 60);
				} catch (final Exception e) {
					continue;
				}
				final Runnable runnable = () -> {
					try (final InputStream inputStream = socket.getInputStream();
							final BufferedReader bufferedReader = new BufferedReader(
									new InputStreamReader(inputStream));) {
						final StringBuilder builder = new StringBuilder();
						String line = null;
						while ((line = bufferedReader.readLine()) != null) {
							builder.append(line);
						}
						final Map<String, List<GetStatusRequest>> mapWithStatus = mapper.readValue(builder.toString(),
								new TypeReference<Map<String, List<GetStatusRequest>>>() {
								});
						// logger.info("Builder data :: " + builder.toString());
						builder.setLength(0);
						final String host = socket.getInetAddress().getHostAddress().toString();
						// logger.info(
						// "ListOfTagTxnId Received: " +
						// gson.toJson(mapWithStatus) + " from the Host: " +
						// host);
						mapWithStatus.forEach((statusOfList, getStatusRequest) -> {
							if (statusOfList.equals(StringConstants.TXT_STATUS)) {
								logger.info("Map was for getting Status, So now procceding.");
								getStatusRequest.forEach((getStatus) -> {
									final BlockStatus blockStatus = blockService.getBlockStatus(getStatus.getTag(),
											getStatus.getTransactionId());
									if (null != blockStatus) {
										mapForStatus.put(getStatus, blockStatus.getStatus());
									} else {
										mapForStatus.put(getStatus, "NULL");
									}
								});
								// Returning the Block And File to node.
								sendBlockDataToHost(mapForStatus, host);
							} else if (statusOfList.equals(StringConstants.TXT_BLOCK)) {
								logger.info("Map was for getting the file, So procceding now.");
								getStatusRequest.forEach((getStatus) -> {
									logger.info("Getting block from blockchain for " + getStatus.toString());
									final Block block = blockService
											.getBlock(getStatus.getTag() + getStatus.getTransactionId());
									logger.info("SEnding block: " + block.toString() + " to host: " + host);
									// Returning the Block And File to node.
									sendBlockDataToHost(block, host);
								});
							}
						});
						mapWithStatus.clear();
					} catch (final Exception e) {
						e.printStackTrace();
					} finally {
						if (socket != null) {
							try {
								socket.close();
							} catch (final Exception e) {
							}
						}
					}
				};
				new Thread(runnable).start();
			}
		} catch (final IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private void sendBlockDataToHost(final Block block, final String clientIp) {
		try (final Socket socket = new Socket();) {
			// logger.info("Send block data to host " + clientIp + " Block data
			// " + block.toString());
			socket.setTcpNoDelay(true);
			logger.info("Sending data to: " + clientIp + " : " + ConfigConstants.PORT_NO_BLOCK_RECEIVE);
			socket.connect(new InetSocketAddress(clientIp, ConfigConstants.PORT_NO_BLOCK_RECEIVE), 1000 * 10);
			final DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
			// final File file = new
			// File(block.getBlockContent().getFilePath());
			// final byte[] readAllBytes = Files.readAllBytes(file.toPath());
			//
			// final SyncBlockDataModel model = new SyncBlockDataModel();
			// model.setBlock(block);
			// model.setFile(readAllBytes);
			// final ObjectOutputStream oos = new
			// ObjectOutputStream(socket.getOutputStream());
			// oos.writeObject(model);
			// Arrays.fill(readAllBytes, (byte) 0);
			// oos.close();
			// logger.info("Block data sent to host " + clientIp);
			// } catch (final Exception e) {
			// logger.error("Error occurred in sending block data for
			// transaction id "
			// + block.getBlockContent().getHashTxnId() + "\nTo host " +
			// clientIp, e);
			// }
			final SyncBlockDataModel model = new SyncBlockDataModel();
			model.setBlock(block);
			final File file = new File(block.getBlockContent().getFilePath());

			final DataInputStream dataIn = new DataInputStream(new FileInputStream(file));

			final String json = gson.toJson(model);
			dataOut.writeUTF(json);
			dataOut.flush();
			final byte[] buffer = new byte[8 * 1024];
			int temp;
			while ((temp = dataIn.read(buffer)) > 0) {
				dataOut.write(buffer, 0, temp);
			}
			dataOut.flush();
			dataIn.close();
			dataOut.close();
		} catch (final Exception e) {
			logger.error("Problem in sending data to: " + clientIp);
		} finally {

		}
	}

	private void sendBlockDataToHost(final Map<GetStatusRequest, String> mapWithTxnStatus, final String clientIp) {
		try (final Socket socket = new Socket();) {
			// logger.info("Sending data: " + mapWithTxnStatus + " To: " +
			// clientIp);
			socket.setTcpNoDelay(true);
			socket.connect(new InetSocketAddress(clientIp, ConfigConstants.PORT_NO_BLOCK_RECEIVE), 1000 * 10);
			// final SyncBlockDataModel dataModel = new SyncBlockDataModel();
			// dataModel.setMapWithStatus(mapWithTxnStatus);
			// final ObjectOutputStream oos = new
			// ObjectOutputStream(socket.getOutputStream());
			// oos.writeObject(dataModel);
			// oos.close();
			final SyncBlockDataModel dataModel = new SyncBlockDataModel();
			dataModel.setMapWithStatus(mapWithTxnStatus);
			final String json = gson.toJson(dataModel);
			final DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
			dataOut.writeUTF(json);
			dataOut.flush();
			dataOut.close();
			// logger.info("Map Sent : " + mapWithTxnStatus + " To :" +
			// clientIp);
		} catch (final Exception e) {
			logger.error("Error occurred in sending the data: " + mapWithTxnStatus + " To: " + clientIp);
		}
	}
}