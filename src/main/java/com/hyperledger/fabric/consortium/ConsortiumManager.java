/*
 * Copyright (c) 2018 www.yiji.com. fx.yiji.com All Rights Reserved.
 */
package com.hyperledger.fabric.consortium;
import com.hyperledger.fabric.components.Chaincode;
import com.hyperledger.fabric.components.Orderers;
import com.hyperledger.fabric.components.Peers;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author: jate  Date: 2018/3/19 Time: 11:17
 */
public class ConsortiumManager {
    @Autowired
    private Environment env;
    private static final Logger log = LoggerFactory.getLogger(ConsortiumManager.class);
    private ConsortiumChaincodeManager manager;
    public ConsortiumManager()
            throws CryptoException, InvalidArgumentException, TransactionException, IOException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
        manager = new ConsortiumChaincodeManager(getConfig());
    }
    public ConsortiumChaincodeManager getManager() {
        return manager;
    }
    private ConsortiumConfig getConfig() {
        ConsortiumConfig config = new ConsortiumConfig();
        config.setOrderers(getOrderers());
        config.setPeers(getPeers());
        config.setChaincode(getChaincode("mychannel", "mycc", "github.com/chaincode/chaincode_example02/go", "1.0"));
        return config;
    }
    private Orderers getOrderers() {
        Orderers orderer = new Orderers();
        orderer.setOrdererDomainName("example.com");
        orderer.addOrderer("orderer.example.com", "grpc://127.0.0.1:7050");
        return orderer;
    }
    private Peers getPeers() {
        Peers peers = new Peers();
        peers.setOrgName("Org1");
        peers.setOrgMSPID("Org1MSP");
        peers.setOrgDomainName("org1.example.com");
        peers.addPeer("peer0.org1.example.com", "peer0.org1.example.com", "grpc://127.0.0.1:7051", "grpc://127.0.0.1:7053", "http://127.0.0.1:7054");
        return peers;
    }
    private Chaincode getChaincode(String channelName, String chaincodeName, String chaincodePath, String chaincodeVersion) {
        Chaincode chaincode = new Chaincode();
        chaincode.setChannelName(channelName);
        chaincode.setChaincodeName(chaincodeName);
        chaincode.setChaincodePath(chaincodePath);
        chaincode.setChaincodeVersion(chaincodeVersion);
        chaincode.setInvokeWatiTime(100000);
        chaincode.setDeployWatiTime(120000);
        return chaincode;
    }

}