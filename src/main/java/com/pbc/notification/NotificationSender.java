package com.pbc.notification;

import static com.pbc.utility.ConfigConstants.PORT_NO_BLOCK;
import static com.pbc.utility.ConfigConstants.PORT_NO_DELETE;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbc.models.CompleteRequest;
import com.pbc.models.NotificationObject;
import com.pbc.utility.ConfigConstants;
import com.pbc.utility.GetSystemIp;
import com.pbc.utility.JSONObjectEnum;

@Service(value = "notificationSender")
@Scope("prototype")
public class NotificationSender {

	private static final Logger logger = Logger.getLogger(NotificationSender.class);
	private static final Logger reportLogger = Logger.getLogger("reportsLogger");

	// remember only getter is provided for this field.
	// Although that also does not prevent this from being accidently changing.
	private static List<String> listOfHosts = new CopyOnWriteArrayList<>();
	private CompleteRequest completeRequest;

	static {
		listOfHosts = ConfigConstants.NODES;
		listOfHosts = listOfHosts.stream().filter(t -> !t.isEmpty()).collect(Collectors.toList());
	}

	public void addHostInList(final String newNode) {
		if (!listOfHosts.contains(newNode) && !newNode.equals(GetSystemIp.getSystemLocalIp())) {
			listOfHosts.add(newNode);
		}
	}

	public CompleteRequest getCompleteRequest() {
		return completeRequest;
	}

	public void setCompleteRequest(final CompleteRequest completeRequest) {
		this.completeRequest = completeRequest;
	}

	public List<String> getListOfHosts() {
		return listOfHosts;
	}

	public void notifyAllHosts(final boolean isDelete, final JSONObjectEnum jsonEnum, final boolean validity) {

		logger.info("inside notify all host::" + completeRequest.getTransactionId() + completeRequest.getCrc()
				+ "JSON ENUM" + jsonEnum);
		listOfHosts.parallelStream().map(new Function<String, Boolean>() {

			@Override
			public Boolean apply(final String host) {
				OutputStreamWriter osw = null;
				BufferedWriter bw = null;
				try (final Socket socket = new Socket();) {
					logger.info("Broadcasting start for host :: " + host + completeRequest.getTransactionId()
							+ completeRequest.getCrc());
					final int portNumber = isDelete ? PORT_NO_DELETE : PORT_NO_BLOCK;
					socket.setTcpNoDelay(true);
					socket.connect(new InetSocketAddress(host, portNumber), 4000);

					final ObjectMapper objectMapper = new ObjectMapper();
					final NotificationObject notificationObject = getJsonObjectToNotify(jsonEnum, validity);
					notificationObject.setDelete(isDelete);
					final String stringNotificationObject = objectMapper.writeValueAsString(notificationObject);
					osw = new OutputStreamWriter(socket.getOutputStream());
					bw = new BufferedWriter(osw);
					bw.write(stringNotificationObject);
					bw.write("\n");
					bw.flush();

					reportLogger.fatal("Broadcasting " + notificationObject.getNotificationType() + " to host : " + host
							+ " for transaction id : " + notificationObject.getTransactionId());
					logger.info("Wrote this notification object to host:  " + host + " message sent: "
							+ stringNotificationObject + " and combined key: " + notificationObject.getTag()
							+ notificationObject.getTransactionId());
					return Boolean.TRUE;
				} catch (final Exception e) {
					logger.error("An error occured while sending data to the host - " + host);
					logger.error("@See Full Stack Trace : ", e);
					return Boolean.FALSE;
				} finally {
					try {
						if (bw != null) {
							bw.close();
						}
						if (osw != null) {
							osw.close();
						}
					} catch (final IOException e) {
						// Empty Catch
					}

				}
			}

			private NotificationObject getJsonObjectToNotify(final JSONObjectEnum jsonEnum, final boolean validity) {
				NotificationObject jsonObject = null;
				if (jsonEnum.equals(JSONObjectEnum.CRC)) {
					jsonObject = new NotificationObject(completeRequest.getTransactionId(), completeRequest.getCrc());
					jsonObject.setTag(completeRequest.getTag());
				} else {
					jsonObject = new NotificationObject(completeRequest.getTransactionId(), validity);
					jsonObject.setTag(completeRequest.getTag());
				}
				return jsonObject;
			}
		}).count();
	}
}