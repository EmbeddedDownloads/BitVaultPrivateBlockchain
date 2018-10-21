package com.pbc.utility;

import java.io.InputStream;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class GetSystemIp {
	private static final Logger logger = Logger.getLogger(GetSystemIp.class);

	private static String systemLocalIp = "";

	public void initializeIpLocal() {
		try {
			int count = 0;
			final StringBuilder ipAddress = new StringBuilder();

			final Process process = Runtime.getRuntime()
					.exec("wget -qO- http://instance-data/latest/meta-data/public-ipv4");

			final InputStream inputStream2 = process.getInputStream();
			while ((count = inputStream2.read()) != -1) {
				ipAddress.append((char) count);
			}
			logger.info("Calculated Local Ip address: " + ipAddress.toString());
			systemLocalIp = ipAddress.toString();
			logger.info("System Local Ip address: " + systemLocalIp);
		} catch (final Exception e) {

		}
	}

	public static String getSystemLocalIp() {
		return systemLocalIp;
	}

}