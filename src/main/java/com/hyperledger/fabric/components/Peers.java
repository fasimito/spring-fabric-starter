/*
 * Copyright (c) 2018 Jate.Ma All Rights Reserved.
 * the peer nodes information come from the configtx.yaml file.
 * <pre class="code">
 *     Organizations:
    # SampleOrg defines an MSP using the sampleconfig.  It should never be used
    # in production but may be used as a template for other definitions
    - &OrdererOrg
        # DefaultOrg defines the organization which is used in the sampleconfig
        # of the fabric.git development environment
        Name: OrdererOrg
        # ID to load the MSP definition as
        ID: OrdererMSP
        # MSPDir is the filesystem path which contains the MSP configuration
        MSPDir: crypto-config/ordererOrganizations/example.com/msp
    - &Org1
        # DefaultOrg defines the organization which is used in the sampleconfig
        # of the fabric.git development environment
        Name: Org1MSP
        # ID to load the MSP definition as
        ID: Org1MSP
        MSPDir: crypto-config/peerOrganizations/org1.example.com/msp
        AnchorPeers:
            # AnchorPeers defines the location of peers which can be used
            # for cross org gossip communication.  Note, this value is only
            # encoded in the genesis block in the Application section context
            - Host: peer0.org1.example.com
              Port: 7051
 * </pre>
 */
package com.hyperledger.fabric.components;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: jate  Date: 2018/3/19 Time: 9:30
 * 4 attributes
 */
public class Peers {
    /**
     * all the Peers must belongs to one org {@code orgName}
     */
    private String orgName;
    /**
     *the org{@code orgName},must offer a unique identity as {@code orgMSPID}
     * this is the memeber service provider identifier={@code MSPID}.
     * it is very important for the auth process.
     */
    private String orgMSPID;
    /**
     * the current org {@code orgName}'s root domain name.
     */
    private String orgDomainName;
    /**
     * all the peers {@code peers} in current {@code orgName}
     */
    private List<Peer> peers = new ArrayList<Peer>();

    public Peers(){
        peers = new ArrayList<Peer>();
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOrgMSPID() {
        return orgMSPID;
    }

    public void setOrgMSPID(String orgMSPID) {
        this.orgMSPID = orgMSPID;
    }

    public String getOrgDomainName() {
        return orgDomainName;
    }

    public void setOrgDomainName(String orgDomainName) {
        this.orgDomainName = orgDomainName;
    }

    public List<Peer> getPeers() {
        return peers;
    }

    public void setPeers(List<Peer> peers) {
        this.peers = peers;
    }

    public void addPeer(Peer peer){
        peers.add(peer);
    }

    public void addPeer(String peerName, String peerEventHubName, String peerLocation, String peerEventHubLocation, String caLocation){
        peers.add(new Peer(peerName, peerEventHubName, peerLocation, peerEventHubLocation, caLocation));
    }

    public List<Peer> get(){
        return peers;
    }

    /**
     * peer details
     * 6 attributes
     */
    public class Peer{
        /**
         * the specify peer node's domain name like: peer0.org1.example.com
         */
        private String peerDomainName;
        /**
         * the specify peer node's event domain name,like: peer0.org1.example.com
         */
        private String peerEventHubName;
        /**
         * the specify peer node's access url, like: grpc://192.168.116.131:7051
         */
        private String peerLocation;
        /**
         * the specify peer node's event listener's access url, like: grpc://192.168.116.131:7053
         */
        private String peerEventHubLocation;
        /**
         * the specify peer node's ca access url, like: http://192.168.116.131:7054
         */
        private String caLocation;
        /**
         * make a alternative choose if you want to (or not) add a event process on the specified peer node
         * by default it is false.
         */
        private boolean addEventHub = false;

        public Peer(String peerName, String peerEventHubName, String peerLocation, String peerEventHubLocation, String caLocation){
            this.peerDomainName = peerName;
            this.peerEventHubName = peerEventHubName;
            this.peerLocation = peerLocation;
            this.peerEventHubLocation = peerEventHubLocation;
            this.caLocation = caLocation;
        }

        public String getPeerDomainName() {
            return peerDomainName;
        }

        public void setPeerDomainName(String peerDomainName) {
            this.peerDomainName = peerDomainName;
        }

        public String getPeerEventHubName() {
            return peerEventHubName;
        }

        public void setPeerEventHubName(String peerEventHubName) {
            this.peerEventHubName = peerEventHubName;
        }

        public String getPeerLocation() {
            return peerLocation;
        }

        public void setPeerLocation(String peerLocation) {
            this.peerLocation = peerLocation;
        }

        public String getPeerEventHubLocation() {
            return peerEventHubLocation;
        }

        public void setPeerEventHubLocation(String peerEventHubLocation) {
            this.peerEventHubLocation = peerEventHubLocation;
        }

        public String getCaLocation() {
            return caLocation;
        }

        public void setCaLocation(String caLocation) {
            this.caLocation = caLocation;
        }

        public boolean isAddEventHub() {
            return addEventHub;
        }

        public void setAddEventHub(boolean addEventHub) {
            this.addEventHub = addEventHub;
        }
    }
}