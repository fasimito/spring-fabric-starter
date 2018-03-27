/*
 * Copyright (c) 2018 author All Rights Reserved.
 */
package com.hyperledger.fabric.consortium;

import com.hyperledger.fabric.components.Chaincode;
import com.hyperledger.fabric.components.Orderers;
import com.hyperledger.fabric.components.Peers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author: jate  Date: 2018/3/19 Time: 11:17
 */
public class ConsortiumConfig {
    private static final Logger logger = LoggerFactory.getLogger(ConsortiumConfig.class);
    /**
     * peer nodes object {@link Peers }
     */
    private Peers peers;
    /**
     * order nodes object {@link Orderers}
     */
    private Orderers orderers;
    /**
     * chaincode object {@link Chaincode}
     */
    private Chaincode chaincode;
    /**
     * channel-artifacts path (very important)
     * <pre class="code">
     *      under this directory there should generate the genesis block (the orderer block) under the next commands:
     *      #export FABRIC_CFG_PATH=$PWD
     *      #../bin/configtxgen -profile TwoOrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block
     *      it also create the Channel Configuration Transaction under these commands:
     *      #export CHANNEL_NAME=mychannel
     *      #../bin/configtxgen -profile TwoOrgsChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID $CHANNEL_NAME
     *      create the anchor peer of org1
     *      #../bin/configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -channelID $CHANNEL_NAME -asOrg Org1MSP
     *      create the anchor peer of org2
     *      #../bin/configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -channelID $CHANNEL_NAME -asOrg Org2MSP
     * </pre>
     * by default the path is located in: /xxx/WEB-INF/classes/fabric/channel-artifacts/
     */
    private String channelArtifactsPath;
    /**
     * crypto-config path (very important)
     * <pre class="code">
     *     ../bin/cryptogen generate --config=./crypto-config.yaml
     * </pre>
     * after execute above command, the directory crypto-config would be generated.
     * there are many MSP located in the folder.
     * by default the path is located in: /xxx/WEB-INF/classes/fabric/crypto-config/
     */
    private String cryptoConfigPath;
    private boolean registerEvent = false;

    public ConsortiumConfig(){
        channelArtifactsPath = "/root/fabric-samples/first-networ/channel-artifacts/";
        cryptoConfigPath = "/root/fabric-samples/first-networ/crypto-config/";
    }
    public Peers getPeers() {
        return peers;
    }

    public void setPeers(Peers peers) {
        this.peers = peers;
    }

    public Orderers getOrderers() {
        return orderers;
    }

    public void setOrderers(Orderers orderers) {
        this.orderers = orderers;
    }

    public Chaincode getChaincode() {
        return chaincode;
    }

    public void setChaincode(Chaincode chaincode) {
        this.chaincode = chaincode;
    }

    public String getChannelArtifactsPath() {
        return channelArtifactsPath;
    }

    public void setChannelArtifactsPath(String channelArtifactsPath) {
        this.channelArtifactsPath = channelArtifactsPath;
    }

    public String getCryptoConfigPath() {
        return cryptoConfigPath;
    }

    public void setCryptoConfigPath(String cryptoConfigPath) {
        this.cryptoConfigPath = cryptoConfigPath;
    }

    public boolean isRegisterEvent() {
        return registerEvent;
    }

}