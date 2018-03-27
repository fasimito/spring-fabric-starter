/*
 * Copyright (c) 2018 www.yiji.com. fx.yiji.com All Rights Reserved.
 */
package com.hyperledger.fabric.consortium;

import com.hyperledger.fabric.components.Orderers;
import com.hyperledger.fabric.components.Peers;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Organization Representation
 * Keeps track which resources are defined for the Organization it represents.
 * @author: jate  Date: 2018/3/19 Time: 11:16
 */
public class ConsortiumOrg {
    private static final Logger logger = LoggerFactory.getLogger(ConsortiumOrg.class);
    private String name;
    private String mspid;
    private HFCAClient caClient;

    Map<String,User> userMap = new HashMap<String, User>();
    Map<String,String> peerLocations = new HashMap<String, String>();
    Map<String,String> orderLocations = new HashMap<String, String>();
    Map<String,String> eventHubLocations = new HashMap<String, String>();

    Set<Peer> peers = new HashSet<Peer>();

    private ConsortiumUser admin;//the consortium administrator
    private String caLocation;//the local CA
    private String caName;
    private Properties caProperties = null;
    private ConsortiumUser peerAdmin;//the signal node peer administrator

    private String domainName;

    public String getCaName() {
        return caName;
    }

    public void setCaName(String caName) {
        this.caName = caName;
    }

    public ConsortiumOrg(String name, String mspid){
        this.name = name;
        this.mspid = mspid;
    }
    public ConsortiumOrg(Peers peers, Orderers orderers,ConsortiumStore consortiumStore,
                         String cryptoConfigPath)throws IOException,NoSuchMethodException {
        this.name = peers.getOrgName();
        this.mspid = peers.getOrgMSPID();
        for(int i=0; i<peers.get().size();i++){
            addPeerLocation(peers.get().get(i).getPeerDomainName(),peers.get().get(i).getPeerLocation());
            addEventHubLocation(peers.get().get(i).getPeerEventHubName(), peers.get().get(i).getPeerEventHubLocation());
            setCaLocation(peers.get().get(i).getCaLocation());
        }
        for(int i=0; i<orderers.get().size();i++){
            addOrdererLocation(orderers.get().get(i).getOrdererName(),orderers.get().get(i).getOrdererLocation());
        }
        setDomainName(peers.getOrgDomainName());
        setAdmin(consortiumStore.getMember("admin",peers.getOrgName()));
        ///root/fabric-samples/first-network/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore
        // 0d8185f2493958e63be68a723cf1a74278dac17003ef9cf4772fc21e5cf53594_sk
        File skFile = Paths.get(cryptoConfigPath,"peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore").toFile();
        ///root/fabric-samples/first-network/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts
        File certificateFile = Paths.get(cryptoConfigPath,"peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/signcerts").toFile();
        setPeerAdmin(consortiumStore.getMember(peers.getOrgName() + "Admin", peers.getOrgName(), peers.getOrgMSPID(), findFileSk(skFile), certificateFile));
    }

    private File findFileSk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if(null == matches){
            throw new RuntimeException(String.format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(String.format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }
        return matches[0];
    }

    public void addOrdererLocation(String ordererName, String ordererLocation) {
        orderLocations.put(ordererName,ordererLocation);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMspid() {
        return mspid;
    }

    public void setMspid(String mspid) {
        this.mspid = mspid;
    }

    public HFCAClient getCaClient() {
        return caClient;
    }

    public void setCaClient(HFCAClient caClient) {
        this.caClient = caClient;
    }

    public Map<String, User> getUserMap() {
        return userMap;
    }

    public void setUserMap(Map<String, User> userMap) {
        this.userMap = userMap;
    }

    public void addUser(ConsortiumUser consortiumUser) {
        userMap.put(consortiumUser.getName(), consortiumUser);
    }

    public Map<String, String> getPeerLocations() {
        return peerLocations;
    }

    public void setPeerLocations(Map<String, String> peerLocations) {
        this.peerLocations = peerLocations;
    }

    public String getPeerLocation(String name) {
        return peerLocations.get(name);
    }

    public Map<String, String> getOrderLocations() {
        return orderLocations;
    }

    public void setOrderLocations(Map<String, String> orderLocations) {
        this.orderLocations = orderLocations;
    }

    public String getOrdererLocation(String name) {
        return orderLocations.get(name);
    }

    public Map<String, String> getEventHubLocations() {
        return eventHubLocations;
    }

    public void setEventHubLocations(Map<String, String> eventHubLocations) {
        this.eventHubLocations = eventHubLocations;
    }

    public String getEventHubLocation(String name) {
        return eventHubLocations.get(name);
    }

    public Set<Peer> getPeers() {
        return peers;
    }

    public void setPeers(Set<Peer> peers) {
        this.peers = peers;
    }

    public ConsortiumUser getAdmin() {
        return admin;
    }

    public void setAdmin(ConsortiumUser admin) {
        this.admin = admin;
    }

    public String getCaLocation() {
        return caLocation;
    }

    public void setCaLocation(String caLocation) {
        this.caLocation = caLocation;
    }

    public Properties getCaProperties() {
        return caProperties;
    }

    public void setCaProperties(Properties caProperties) {
        this.caProperties = caProperties;
    }

    public ConsortiumUser getPeerAdmin() {
        return peerAdmin;
    }

    public void setPeerAdmin(ConsortiumUser peerAdmin) {
        this.peerAdmin = peerAdmin;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void addEventHubLocation(String peerEventHubName, String peerEventHubLocation) {
        eventHubLocations.put(peerEventHubName,peerEventHubLocation);
    }

    public void addPeerLocation(String peerDomainName, String peerLocation) {
        peerLocations.put(peerDomainName,peerLocation);
    }

    public Set<String> getPeerNames() {

        return Collections.unmodifiableSet(peerLocations.keySet());
    }


    public Set<String> getOrdererNames() {

        return Collections.unmodifiableSet(orderLocations.keySet());
    }

    public Set<String> getEventHubNames() {

        return Collections.unmodifiableSet(eventHubLocations.keySet());
    }
}