package com.pbc.context;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.pbc.blockchain.Block;
import com.pbc.blockchain.creation.Persistor;
import com.pbc.utility.GetSystemIp;

@Configuration
public class StartupContextInitBean {

	@Autowired
	private GetSystemIp systemIp;
	@Autowired
	private Persistor<Block> jsonPersistor;

	@PostConstruct
	public void initializeNodeBlockChain() {
		jsonPersistor.initializeNodeStartup();
		systemIp.initializeIpLocal();
	}
}
