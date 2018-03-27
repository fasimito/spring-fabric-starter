/*
 * Copyright (c) 2018 Jate.Ma All Rights Reserved.
 * the orderers configuration a located in the file configtx.yaml also, like the Peers
 * <pre class="code">
 *Orderer: &OrdererDefaults
    # Orderer Type: The orderer implementation to start
    # Available types are "solo" and "kafka"
    OrdererType: solo
    Addresses:
        - orderer.example.com:7050
    # Batch Timeout: The amount of time to wait before creating a batch
    BatchTimeout: 2s
    # Batch Size: Controls the number of messages batched into a block
    BatchSize:
        # Max Message Count: The maximum number of messages to permit in a batch
        MaxMessageCount: 10
        # Absolute Max Bytes: The absolute maximum number of bytes allowed for
        # the serialized messages in a batch.
        AbsoluteMaxBytes: 99 MB
        # Preferred Max Bytes: The preferred maximum number of bytes allowed for
        # the serialized messages in a batch. A message larger than the preferred
        # max bytes will result in a batch larger than preferred max bytes.
        PreferredMaxBytes: 512 KB
    Kafka:
        # Brokers: A list of Kafka brokers to which the orderer connects
        # NOTE: Use IP:port notation
        Brokers:
            - 127.0.0.1:9092
    # Organizations is the list of orgs which are defined as participants on
    # the orderer side of the network
    Organizations:
 * </pre>
 */
package com.hyperledger.fabric.components;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: jate  Date: 2018/3/19 Time: 10:07
 * 2 attributes
 */
public class Orderers {
    /**
     * the orderer domain name {@code ordererDomainName}
     */
    private String ordererDomainName;
    /**
     * the orderer cluster members {@code orderers}
     */
    private List<Orderer> orderers = new ArrayList<Orderer>();

    public String getOrdererDomainName() {
        return ordererDomainName;
    }

    public void setOrdererDomainName(String ordererDomainName) {
        this.ordererDomainName = ordererDomainName;
    }

    public List<Orderer> getOrderers() {
        return orderers;
    }

    /**
     * get the Orderer list by simple get method, rewrite the getOrderers method
     * @return all the orders
     */
    public List<Orderer> get(){
        return orderers;
    }

    /**
     * add a new order by {@code name} and {@code location}
     * @param name
     * @param location
     */
    public void addOrderer(String name, String location){
        orderers.add(new Orderer(name,location));
    }

    public List<Orderer> setOrderers(Orderer orderer) {
        this.orderers.add(orderer);
        return this.orderers;
    }

    /**
     * the orderer details
     * 2 attributes
     */
    public class Orderer{
        /**
         * the domain name{@code ordererName} of the orderer server.
         */
        private String ordererName;
        /**
         * the orderer access location {@code ordererLocation}
         */
        private String ordererLocation;

        public Orderer(String ordererName, String ordererLocation){
            this.ordererName = ordererName;
            this.ordererLocation = ordererLocation;
        }

        public String getOrdererName() {
            return ordererName;
        }

        public void setOrdererName(String ordererName) {
            this.ordererName = ordererName;
        }

        public String getOrdererLocation() {
            return ordererLocation;
        }

        public void setOrdererLocation(String ordererLocation) {
            this.ordererLocation = ordererLocation;
        }
    }
}