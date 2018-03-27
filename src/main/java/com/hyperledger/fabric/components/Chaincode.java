/*
 * Copyright (c) 2018 Jate.Ma All Rights Reserved.
 * Chaincode applications encode logic that is invoked by specific types of transactions on the channel.
 * Chaincode that defines parameters for a change of asset ownership, for example,
 * ensures that all transactions that transfer ownership are subject to the same rules and requirements.
 * System chaincode is distinguished as chaincode that defines operating parameters for the entire channel.
 * Lifecycle and configuration system chaincode defines the rules for the channel;
 * endorsement and validation system chaincode defines the requirements for endorsing and validating transactions.
 * <pre class="code">
 * func (t *SimpleAsset) Init(stub shim.ChaincodeStubInterface) peer.Response
 * func (t *SimpleAsset) Invoke(stub shim.ChaincodeStubInterface) peer.Response
 * </pre>
 * the above two method must be involved
 * the chaincode install/instate/invoke/query methods as bellow:
 * ******************************
 * export CHANNEL_NAME=mychannel
 * peer chaincode install -n mycc -v 1.0 -p github.com/chaincode/chaincode_example02/go/
 * peer chaincode instantiate -o orderer.example.com:7050 --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C $CHANNEL_NAME -n mycc -v 1.0 -c '{"Args":["init","c", "300", "d","500"]}' -P "OR ('Org1MSP.member','Org2MSP.member')"
 * peer chaincode query -C $CHANNEL_NAME -n mycc -c '{"Args":["query","a"]}'
 * peer chaincode invoke -o orderer.example.com:7050 --tls --cafile /opt/gopath/src/github.com/hyperledger/fabric/peer/crypto/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C $CHANNEL_NAME -n mycc -c '{"Args":["invoke","a","b","10"]}'
 * ******************************
 * 6 attributes
 */
package com.hyperledger.fabric.components;

/**
 * @author: jate  Date: 2018/3/19 Time: 10:27
 */
public class Chaincode {
    /**
     * all the chaincode must run in a channel, and the channel name {@code channelName} is the unique identify element.
     * -C $CHANNEL_NAME
     */
    private String channelName;
    /**
     * all the chaincode have a unique name marked in the channel.
     * -n mycc
     */
    private String chaincodeName;
    /**
     * all the chaincode have a install path in the docker container.
     * -p github.com/chaincode/chaincode_example02/go/
     */
    private String chaincodePath;
    /**
     * all the chaincode have a version
     * -v 1.0
     */
    private String chaincodeVersion;
    /**
     * the chaincode process wait time.
     */
    private int invokeWatiTime = 100000;
    /**
     * the chaincode invoke wait time.
     */
    private int deployWatiTime = 120000;

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    public void setChaincodeName(String chaincodeName) {
        this.chaincodeName = chaincodeName;
    }

    public String getChaincodePath() {
        return chaincodePath;
    }

    public void setChaincodePath(String chaincodePath) {
        this.chaincodePath = chaincodePath;
    }

    public String getChaincodeVersion() {
        return chaincodeVersion;
    }

    public void setChaincodeVersion(String chaincodeVersion) {
        this.chaincodeVersion = chaincodeVersion;
    }

    public int getInvokeWatiTime() {
        return invokeWatiTime;
    }

    public void setInvokeWatiTime(int invokeWatiTime) {
        this.invokeWatiTime = invokeWatiTime;
    }

    public int getDeployWatiTime() {
        return deployWatiTime;
    }

    public void setDeployWatiTime(int deployWatiTime) {
        this.deployWatiTime = deployWatiTime;
    }
}