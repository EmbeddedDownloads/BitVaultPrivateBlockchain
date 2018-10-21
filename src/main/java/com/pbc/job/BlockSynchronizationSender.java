package com.pbc.job;

import static com.pbc.utility.ConfigConstants.PORT_NO_SYNCHRONIZATION;
import static com.pbc.utility.ConfigConstants.SYNCHRONIZATION_NODES;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.pbc.utility.GetSystemIp;

@Component
public class BlockSynchronizationSender {

	private static final Logger logger = Logger.getLogger(BlockSynchronizationSender.class);

	private static List<String> listOfHosts = new CopyOnWriteArrayList<>();

	private String initialSyncData;

	static {
		listOfHosts = SYNCHRONIZATION_NODES;
		listOfHosts = listOfHosts.stream().filter(t -> !t.isEmpty()).collect(Collectors.toList());
	}

	public void addHostInList(final String newNode) {
		if (!listOfHosts.contains(newNode) && !newNode.equals(GetSystemIp.getSystemLocalIp())) {
			listOfHosts.add(newNode);
		}
	}

	public void setInitialSyncData(final String initialSyncData) {
		this.initialSyncData = initialSyncData;
	}

	/**
	 * Send data to all host listed in {@link #listOfHosts}. Don't forget to call
	 * {@link #setInitialSyncData(String)} to set data before calling this method.
	 */
	public void sendPbcSyncNotification() {
		logger.info("Sending data start for block synchronization.");
		listOfHosts.parallelStream().map(host -> {
			try (final Socket socket = new Socket();) {
				socket.setTcpNoDelay(true);
				socket.connect(new InetSocketAddress(host, PORT_NO_SYNCHRONIZATION), 500);
				final OutputStreamWriter osw = new OutputStreamWriter(socket.getOutputStream());
				final BufferedWriter bw = new BufferedWriter(osw);
				bw.write(initialSyncData);
				bw.flush();
				bw.close();
				osw.close();
				logger.info("PBC Sync data send for host : " + host);
				// logger.info("Send sync data : " + initialSyncData);
				return Boolean.TRUE;
			} catch (final Exception e) {
				logger.error(host + " is unable to reach ", e);
				return Boolean.FALSE;
			}
		}).count();
	}
}
