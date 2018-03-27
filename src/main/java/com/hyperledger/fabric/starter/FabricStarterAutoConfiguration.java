/*
 * Copyright (c) 2018 author All Rights Reserved.
 */
package com.hyperledger.fabric.starter;

import com.hyperledger.fabric.properties.FabricServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author: jate  Date: 2018/3/18 Time: 8:51
 */
@Configuration
@EnableConfigurationProperties(FabricServerProperties.class)
@ConditionalOnProperty(prefix = "hyperledger.fabric.server",value = "enabled",matchIfMissing = true)
public class FabricStarterAutoConfiguration {
    @Autowired
    private Environment environment;
}