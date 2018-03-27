/*
 * Copyright (c) 2018 author All Rights Reserved.
 */
package com.hyperledger.fabric.consortium;

import com.google.protobuf.ByteString;
import com.hyperledger.fabric.components.Chaincode;
import com.hyperledger.fabric.components.Orderers;
import com.hyperledger.fabric.components.Peers;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.commons.codec.CharEncoding.UTF_8;

/**
 * @author: jate  Date: 2018/3/19 Time: 11:17
 */
public class ConsortiumChaincodeManager {
    private static final Logger logger = LoggerFactory.getLogger(ConsortiumChaincodeManager.class);
    private ConsortiumConfig consortiumConfig;
    private Orderers orderers;
    private Peers peers;
    private Chaincode chaincode;

    private HFClient hfClient;
    private ConsortiumOrg consortiumOrg;
    private Channel channel;
    private ChaincodeID chaincodeID;

    public ConsortiumChaincodeManager(ConsortiumConfig consortiumConfig)
            throws CryptoException, InvalidArgumentException, IOException, NoSuchMethodException, RuntimeException, TransactionException, ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.consortiumConfig = consortiumConfig;
        this.orderers = consortiumConfig.getOrderers();
        this.peers = consortiumConfig.getPeers();
        chaincode = consortiumConfig.getChaincode();
        hfClient = HFClient.createNewInstance();
        logger.debug("Create instance of HFClient");
        hfClient.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        logger.debug("Set Crypto Suite of HFClient");
        consortiumOrg = getConsortiumOrg();
        channel = getChannel();
        chaincodeID = getChaincodeID();
        hfClient.setUserContext(consortiumOrg.getPeerAdmin());
    }
    private ChaincodeID getChaincodeID() {
        return ChaincodeID.newBuilder().setName(chaincode.getChaincodeName()).setVersion(chaincode.getChaincodeVersion()).setPath(chaincode.getChaincodePath()).build();
    }
    private Channel getChannel() throws InvalidArgumentException, TransactionException {
        hfClient.setUserContext(consortiumOrg.getPeerAdmin());
        return getChannel(consortiumOrg,hfClient);
    }
    private Channel getChannel(ConsortiumOrg consortiumOrg, HFClient hfClient) throws InvalidArgumentException, TransactionException {
        Channel channel = hfClient.newChannel(chaincode.getChaincodeName());
        logger.debug("Get Chain :{}",chaincode.getChaincodeName());
        //channel.setTransactionWaitTime(chaincode.getInvokeWatiTime());
        //channel.setDeployWaitTime(chaincode.getDeployWatiTime());
        for(int i=0; i<peers.get().size();i++){
            //the Org1 full path:   /root/fabric-samples/first-network/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls
            //                      /root/fabric-samples/first-network/crypto-config/peerOrganizations/org1.example.com/peers/peer1.org1.example.com/tls
            //the Org2 full path:   /root/fabric-samples/first-network/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls
            //                      /root/fabric-samples/first-network/crypto-config/peerOrganizations/org2.example.com/peers/peer1.org2.example.com/tls
            File peerCert = Paths.get(consortiumConfig.getCryptoConfigPath(),"/peerOrganizations",
                    peers.getOrgDomainName(),"peers",peers.get().get(i).getPeerDomainName(),"tls/server.crt").toFile();
            if(!peerCert.exists()){
                throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s",peers.get().get(i).getPeerDomainName(),peerCert.getAbsolutePath()));
            }
            Properties peerProperties = new Properties();
            peerProperties.setProperty("pemFile", peerCert.getAbsolutePath());
            peerProperties.setProperty("hostnameOverride", peers.getOrgDomainName());
            peerProperties.setProperty("sslProvider", "openSSL");
            peerProperties.setProperty("negotiationType", "TLS");
            peerProperties.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);
            channel.addPeer(hfClient.newPeer(peers.get().get(i).getPeerDomainName(),consortiumOrg.getPeerLocation(peers.get().get(i).getPeerDomainName()),peerProperties));
            if(peers.get().get(i).isAddEventHub()){
                channel.addEventHub(hfClient.newEventHub(peers.get().get(i).getPeerEventHubName(),
                        consortiumOrg.getEventHubLocation(peers.get().get(i).getPeerEventHubName()), peerProperties));
            }
        }
        for(int i=0; i<orderers.get().size();i++){
            //the full path: /root/fabric-samples/first-network/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tls
            File ordererCert = Paths.get(consortiumConfig.getCryptoConfigPath(), "/ordererOrganizations",
                    orderers.getOrdererDomainName(), "orderers", orderers.get().get(i).getOrdererName(),
                    "tls/server.crt").toFile();
            if (!ordererCert.exists()) {
                throw new RuntimeException(
                        String.format("Missing cert file for: %s. Could not find at location: %s", orderers.get().get(i).getOrdererName(), ordererCert.getAbsolutePath()));
            }
            Properties ordererProperties = new Properties();
            ordererProperties.setProperty("pemFile", ordererCert.getAbsolutePath());
            ordererProperties.setProperty("hostnameOverride", orderers.getOrdererDomainName());
            ordererProperties.setProperty("sslProvider", "openSSL");
            ordererProperties.setProperty("negotiationType", "TLS");
            ordererProperties.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);
            ordererProperties.setProperty("ordererWaitTimeMilliSecs", "300000");
            channel.addOrderer(
                    hfClient.newOrderer(orderers.get().get(i).getOrdererName(), consortiumOrg.getOrdererLocation(orderers.get().get(i).getOrdererName()), ordererProperties));
        }
        logger.debug("channel.isInitialized() = " + channel.isInitialized());
        if (!channel.isInitialized()) {
            channel.initialize();
        }
        if (consortiumConfig.isRegisterEvent()) {
            channel.registerBlockListener(new BlockListener() {
                @Override
                public void received(BlockEvent event) {
                    ByteString byteString = event.getBlock().getData().getData(0);
                    String result = byteString.toStringUtf8();
                    String r1[] = result.split("END CERTIFICATE");
                    String rr = r1[2];
                }
            });
        }
        return channel;
    }
    /**
     * get the consortium org instance.
     * @return
     * @throws IOException
     * @throws NoSuchMethodException
     */
    private ConsortiumOrg getConsortiumOrg() throws IOException, NoSuchMethodException {
        File storeFile = new File("/root/fabric-samples/first-network/firstnetwork.properties");
        ConsortiumStore consortiumStore = new ConsortiumStore(storeFile);
        ConsortiumOrg org = new ConsortiumOrg(peers,orderers,consortiumStore,consortiumConfig.getCryptoConfigPath());
        logger.debug("Get FabricOrg");
        return org;
    }
    /**
     * invoke method
     * @param fcn
     * @param args
     * @return
     * @throws UnsupportedEncodingException
     * @throws InvalidArgumentException
     * @throws ProposalException
     */
    public Map<String, String> invoke(String fcn, String[] args) throws UnsupportedEncodingException, InvalidArgumentException, ProposalException {
        Map<String, String> resultMap = new HashMap<>();
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn(fcn);
        transactionProposalRequest.setArgs(args);
        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8));
        transactionProposalRequest.setTransientMap(tm2);
        Collection<ProposalResponse> transactionPropResp = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        Collection<Set<ProposalResponse>> proposalConsistencySets = SDKUtils.getProposalConsistencySets(transactionPropResp);
        if (proposalConsistencySets.size() != 1) {
            logger.error("Expected only one set of consistent proposal responses but got {}",proposalConsistencySets.size());
        }
        if (failed.size() > 0) {
            ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
            logger.error("Not enough endorsers for inspect:" + failed.size() + " endorser error: " + firstTransactionProposalResponse.getMessage() + ". Was verified: "
                    + firstTransactionProposalResponse.isVerified());
            resultMap.put("code", "error");
            resultMap.put("data", firstTransactionProposalResponse.getMessage());
            return resultMap;
        } else {
            logger.info("Successfully received transaction proposal responses.");
            ProposalResponse resp = transactionPropResp.iterator().next();
            byte[] x = resp.getChaincodeActionResponsePayload();
            String resultAsString = null;
            if (x != null) {
                resultAsString = new String(x, "UTF-8");
            }
            logger.info("resultAsString = " + resultAsString);
            channel.sendTransaction(successful);
            resultMap.put("code", "success");
            resultMap.put("data", resultAsString);
            return resultMap;
        }
    }

    /**
     * query method
     * @param fcn
     * @param args
     * @return
     * @throws UnsupportedEncodingException
     * @throws InvalidArgumentException
     * @throws ProposalException
     */
    public Map<String, String> query(String fcn, String[] args) throws UnsupportedEncodingException, InvalidArgumentException, ProposalException {
        Map<String, String> resultMap = new HashMap<>();
        String payload = "";
        QueryByChaincodeRequest queryByChaincodeRequest = hfClient.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(args);
        queryByChaincodeRequest.setFcn(fcn);
        queryByChaincodeRequest.setChaincodeID(chaincodeID);
        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
        queryByChaincodeRequest.setTransientMap(tm2);
        Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
                logger.debug("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() + ". Messages: "
                        + proposalResponse.getMessage() + ". Was verified : " + proposalResponse.isVerified());
                resultMap.put("code", "error");
                resultMap.put("data", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus() + ". Messages: "
                        + proposalResponse.getMessage() + ". Was verified : " + proposalResponse.isVerified());
            } else {
                payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                logger.debug("Query payload from peer: " + proposalResponse.getPeer().getName());
                logger.debug("" + payload);
                resultMap.put("code", "success");
                resultMap.put("data", payload);
            }
        }
        return resultMap;
    }
}