package com.pbc.push_notification;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.models.GetStatusRequest;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.GetSystemIp;

@Service
public class PushNotificationSender {

	@Autowired
	ReceiveTimeStampNotification stampNotification;

	public String timeStampFuture = "3471273000000";

	private static final Logger logger = Logger.getLogger(PushNotificationSender.class);

	private static List<String> listOfHosts = new CopyOnWriteArrayList<>();

	final GetStatusRequest request = new GetStatusRequest();

	private final ObjectMapper mapper = new ObjectMapper();

	static {
		listOfHosts = ConfigConstants.NODES;
		listOfHosts = listOfHosts.stream().filter(t -> !t.isEmpty()).collect(Collectors.toList());
	}

	public void addHostInList(final String newNode) {
		if (!listOfHosts.contains(newNode) && !newNode.equals(GetSystemIp.getSystemLocalIp())) {
			listOfHosts.add(newNode);
		}
	}

	public void setInitialPushData(final String tag, final String txnId) {
		request.setTag(tag);
		request.setTransactionId(txnId);

	}

	/**
	 * Send data to all host listed in {@link #listOfHosts}. Don't forget to call
	 * {@link #setInitialSyncData(String)} to set data before calling this method.
	 */
	public void sendPbcSyncNotification() {
		logger.info(
				"Sending TagTxnIDForTimeStamp start for Notification:" + request.getTag() + request.getTransactionId());
		listOfHosts.parallelStream().map(host -> {
			try (final Socket socket = new Socket();) {
				socket.setTcpNoDelay(true);
				socket.connect(new InetSocketAddress(host, ConfigConstants.PORT_FOR_PUSHNOTIFY_RECEIVE), 1000 * 10);
				final OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
				final BufferedWriter bw = new BufferedWriter(osw);
				final String writeValueAsString = mapper.writeValueAsString(request);
				bw.write(writeValueAsString);
				bw.flush();
				bw.close();
				osw.close();
				logger.info("PBC PushNotification data send for host : " + host);
				logger.info("Send PushNotification data : " + writeValueAsString);
				return Boolean.TRUE;
			} catch (final Exception e) {
				logger.error("Error is :" + e.getMessage());
				e.printStackTrace();
				final Map<String, List<Date>> mapForTimestamp = ReceiveTimeStampNotification.getMapForTimestamp();
				logger.error("Due to error setting a default timestamp value.....");
				final List<Date> list = mapForTimestamp.get(request.getTransactionId());
				final Date today = new Date();
				LocalDateTime localDateTime = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				localDateTime = localDateTime.plusYears(50).plusMonths(1).plusDays(1);
				final Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());

				if (mapForTimestamp.get(request.getTransactionId()) == null) {
					stampNotification.andPutInMap(date, request.getTransactionId());
					logger.error("Added a default timestamp : " + date.getTime());
				} else {
					// list = mapForTimestamp.get(request.getTransactionId());
					list.add(date);
					logger.error("Added a default timestamp : " + date.getTime());
				}
				// list = mapForTimestamp.get(request.getTransactionId());
				// list.add(date);
				logger.error("Added a default timestamp : " + date.getTime());
				logger.error(host + " is unable to reach ", e);
				return Boolean.FALSE;
			}
		}).count();
	}
}
