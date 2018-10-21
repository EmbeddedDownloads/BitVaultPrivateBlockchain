package com.pbc.push_notification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.repository.impl.BlockStatusDaoImpl;
import com.pbc.repository.model.BlockStatus;
import com.pbc.utility.ConfigConstants;

@Component
public class ReceiveTimeStampNotification extends Thread {

	private static final Logger logger = Logger.getLogger(ReceiveTimeStampNotification.class);

	@Autowired
	NotificationClient notificationClient;
	@Autowired
	BlockStatusDaoImpl daoImpl;

	boolean flag = false;
	List<Date> list;

	private static Map<String, List<Date>> mapForTimestamp = new HashMap<>();

	private final ObjectMapper mapper = new ObjectMapper();

	public static Map<String, List<Date>> getMapForTimestamp() {
		return mapForTimestamp;
	}

	@Override
	public void run() {

		ServerSocket serverSocket = null;
		try {
			final StringBuilder builder = new StringBuilder();
			serverSocket = new ServerSocket(ConfigConstants.PORT_FOR_PUSH_SEND);
			while (true) {
				final Socket socket;
				try {
					socket = serverSocket.accept();
					socket.setSoTimeout(1000 * 10);
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
						logger.info("Host to send data: " + socket.getInetAddress().getHostAddress());
						logger.info("Getting the data and data is: " + builder.toString());
						synchronized (this) {
							final BlockStatus blockStatus = mapper.readValue(builder.toString(), BlockStatus.class);
							logger.info("BlockStatus got from host: " + socket.getInetAddress().getHostAddress()
									+ "is: " + blockStatus);
							logger.info("Timestamp corresponding to TxnId: " + blockStatus.getTag()
									+ blockStatus.getTransactionId() + "  is: " + blockStatus.getUpdatedAt());
							if (blockStatus.getUpdatedAt() == null) {
								final Date today = new Date();
								LocalDateTime localDateTime = today.toInstant().atZone(ZoneId.systemDefault())
										.toLocalDateTime();
								localDateTime = localDateTime.plusYears(50).plusMonths(1).plusDays(1);
								final Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

								blockStatus.setUpdatedAt(date);
								logger.info("Default timeStamp added : " + date.getTime() + " for Txnid: "
										+ blockStatus.getTag() + blockStatus.getTransactionId());

							}

							list = mapForTimestamp.get(blockStatus.getTransactionId());
							logger.info("Size of the list is: " + list + "  Saved for TxnId is : "
									+ blockStatus.getTransactionId());
							if (list == null) {
								andPutInMap(blockStatus.getUpdatedAt(), blockStatus.getTransactionId());
								logger.info("Added when list was null TxnId : " + blockStatus.getTransactionId()
										+ " with timeStamp : " + blockStatus.getUpdatedAt().getTime());
							} else {
								if (list.size() < ConfigConstants.TOTAL_NODES) {
									list.add(blockStatus.getUpdatedAt());
									logger.info("Added TxnId : " + blockStatus.getTransactionId() + " with timeStamp : "
											+ blockStatus.getUpdatedAt().getTime());
								}
								if (list.size() == ConfigConstants.TOTAL_NODES) {
									logger.info("List size Total_Node = " + ConfigConstants.TOTAL_NODES);
									final BlockStatus statusifSaved = daoImpl.getStatusifSaved(blockStatus.getTag(),
											blockStatus.getTransactionId());
									if (statusifSaved != null) {
										final long systemTxnId = statusifSaved.getUpdatedAt().getTime();
										// final long systemTxnId = Long
										// .parseLong(statusifSaved.getUpdatedAt().toString());
										for (final Date timeStamp : list) {
											if (systemTxnId > timeStamp.getTime()) {
												flag = true;
												System.out.println("FLAG value: " + flag);
												break;
											}
										}
									}
									if (flag == false) {
										logger.info(
												"All step done to send data to NotificationServer for transactionId: 	"
														+ blockStatus.getTransactionId());
										logger.info("Size of the list before sending Notification.........."
												+ mapForTimestamp.get(blockStatus.getTransactionId()).size());

										notificationClient.pushAfterValidating();
										logger.info(
												"Removing data from map for TxnId = " + blockStatus.getTransactionId());
										mapForTimestamp.remove(blockStatus.getTransactionId());
									}
									flag = false;
								}
							}
						}
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

	public void andPutInMap(final Date date, final String transactionId) {
		list = new ArrayList<>();
		list.add(date);
		mapForTimestamp.put(transactionId, list);

	}

}
