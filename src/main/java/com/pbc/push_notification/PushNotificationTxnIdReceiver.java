package com.pbc.push_notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.models.GetStatusRequest;
import com.pbc.repository.impl.BlockStatusDaoImpl;
import com.pbc.repository.model.BlockStatus;
import com.pbc.utility.ConfigConstants;

@Component
public class PushNotificationTxnIdReceiver extends Thread {

	private static Logger logger = Logger.getLogger(PushNotificationTxnIdReceiver.class);

	@Autowired
	BlockStatusDaoImpl daoimpl;

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void run() {

		ServerSocket serverSocket = null;
		try {
			final StringBuilder builder = new StringBuilder();
			serverSocket = new ServerSocket(ConfigConstants.PORT_FOR_PUSHNOTIFY_RECEIVE);
			while (true) {
				final Socket socket;
				try {
					socket = serverSocket.accept();
					socket.setSoTimeout(1000 * 5);
				} catch (final Exception e) {
					continue;
				}
				final Runnable runnable = () -> {
					try (final InputStream inputStream = socket.getInputStream();
							final BufferedReader bufferedReader = new BufferedReader(
									new InputStreamReader(inputStream));) {

						String line = null;
						while ((line = bufferedReader.readLine()) != null) {
							builder.append(line);
						}

						final GetStatusRequest combineKey = mapper.readValue(builder.toString(),
								GetStatusRequest.class);
						logger.info("CombineKey received to send BlockStatus " + combineKey.getTag()
								+ combineKey.getTransactionId() + " from host "
								+ socket.getInetAddress().getHostAddress());
						// Getting BlockStatus For this combineKey
						// final Block block =
						// blockService.getBlock(combineKey.getTag() +
						// combineKey.getTransactionId());
						BlockStatus status = daoimpl.getStatusifSaved(combineKey.getTag(),
								combineKey.getTransactionId());
						logger.info("Retrived BlockStatus from database:" + status + "  for the host : "
								+ socket.getInetAddress().getHostName());
						if (status == null) {
							status = new BlockStatus();
							status.setTransactionId(combineKey.getTransactionId());
							status.setTag(combineKey.getTag());
						}
						// logger.info("Sending the data back to host: " +
						// socket.getInetAddress().getHostName());
						// logger.info("Data to be Sended for txnId " +
						// status.getTag() + status.getTransactionId());
						// Returning the Block And File to particular Node.
						sendBlockDataToHost(status, socket.getInetAddress().getHostAddress());

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
				builder.setLength(0);
			}
		} catch (final IOException e) {
			System.out.println(e.getMessage());
		}
	}

	private void sendBlockDataToHost(final BlockStatus blockStatus, final String clientIp) {
		try (final Socket socket = new Socket();) {
			logger.info("Send TimeStamp to host " + clientIp);
			logger.info("Sending TimeStamp data " + blockStatus.getTag() + blockStatus.getTransactionId());

			socket.setTcpNoDelay(true);
			socket.connect(new InetSocketAddress(clientIp, ConfigConstants.PORT_FOR_PUSH_SEND), 1000 * 10);

			final String valueAsString = mapper.writeValueAsString(blockStatus);
			final PrintWriter printWriter = new PrintWriter(socket.getOutputStream());
			printWriter.write(valueAsString);
			printWriter.flush();
			printWriter.close();
			logger.info("BlockStatus sent to host " + clientIp + " value returned with : " + valueAsString);
		} catch (final Exception e) {
			logger.error("Error occurred in sending TimeStamp for transaction id " + blockStatus.getTransactionId()
					+ "\nTo host " + clientIp, e);
		}
	}
}
