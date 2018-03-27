/*
 * Copyright (c) 2018 www.yiji.com. fx.yiji.com All Rights Reserved.
 * the property file is used for config the basic attributes of hyperledger fabir networks.
 * this is match the docker-compose files.
 */
package com.hyperledger.fabric.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author: jate  Date: 2018/3/18 Time: 22:54
 */
@ConfigurationProperties(prefix = "hyperledger.fabric.server")
public class FabricServerProperties {
    /**
     * @See <p>
     *   the properties are used for config the peers node servers.
     * </p>
     */
    private String ca;
    private String host;
    private String invokeWaitTime;
    private String deployWaitTime;
    private String proposalWaitTime;
    private String defaultConfig;

    public String getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(String defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public String getCa() {
        return ca;
    }

    public void setCa(String ca) {
        this.ca = ca;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getInvokeWaitTime() {
        return invokeWaitTime;
    }

    public void setInvokeWaitTime(String invokeWaitTime) {
        this.invokeWaitTime = invokeWaitTime;
    }

    public String getDeployWaitTime() {
        return deployWaitTime;
    }

    public void setDeployWaitTime(String deployWaitTime) {
        this.deployWaitTime = deployWaitTime;
    }

    public String getProposalWaitTime() {
        return proposalWaitTime;
    }

    public void setProposalWaitTime(String proposalWaitTime) {
        this.proposalWaitTime = proposalWaitTime;
    }
}