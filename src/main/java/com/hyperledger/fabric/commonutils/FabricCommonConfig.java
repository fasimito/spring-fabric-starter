/*
 *  Copyright 2018, author All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.hyperledger.fabric.commonutils;

import com.hyperledger.fabric.consortium.ConsortiumOrg;
import com.hyperledger.fabric.properties.FabricServerProperties;
import org.hyperledger.fabric.sdk.helper.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Config allows for a global config of the toolkit. Central location for all
 * toolkit configuration defaults. Has a local config file that can override any
 * property defaults. Config file can be relocated via a system property
 * "org.hyperledger.fabric.sdk.configuration". Any property can be overridden
 * with environment variable and then overridden
 * with a java system property. Property hierarchy goes System property
 * overrides environment variable which overrides config file for default values specified here.
 */

/**
 * Fabric Configuration
 */
public class FabricCommonConfig {
    private static final Logger logger = LoggerFactory.getLogger(FabricCommonConfig.class);
    @Autowired
    private Environment environment;
    @Autowired
    private FabricServerProperties serverProperties;
    private String defaultConfig;
    private String accesshost;

    private String invokewaittime =  "org.hyperledger.fabric.consortium.InvokeWaitTime";
    private String deploywaittime =  "org.hyperledger.fabric.consortium.DeployWaitTime";
    private String proposalwaittime = "org.hyperledger.fabric.consortium.ProposalWaitTime";

    private String integrationtestsOrg = "org.hyperledger.fabric.consortium.integration.org.";
    private Pattern orgPat = Pattern.compile("^" + Pattern.quote(integrationtestsOrg) + "([^\\.]+)\\.mspid$");

    private String integrationteststls =  "org.hyperledger.fabric.consortium.integration.tls";
    // location switching between fabric cryptogen and configtxgen artifacts for v1.0 and v1.1 in src/test/fixture/sdkintegration/e2e-2Orgs
    public String FAB_CONFIG_GEN_VERS =
            Objects.equals(System.getenv("ORG_HYPERLEDGER_FABRIC_SDKTEST_VERSION"), "1.0.0") ? "v1.0" : "v1.1";

    private FabricCommonConfig config;
    private Properties sdkProperties = new Properties();
    private boolean runningTLS;
    private boolean runningFabricCATLS;

    public boolean isRunningFabricTLS() {
        return runningFabricTLS;
    }

    private boolean runningFabricTLS;
    private HashMap<String, ConsortiumOrg> sampleOrgs = new HashMap<>();

    public FabricCommonConfig() {
        File loadFile;
        FileInputStream configProps;
        accesshost = serverProperties.getHost();
        defaultConfig = serverProperties.getDefaultConfig();
        try {
            loadFile = new File(defaultConfig).getAbsoluteFile();
            configProps = new FileInputStream(loadFile);
            sdkProperties.load(configProps);
        } catch (IOException e) { // if not there no worries just use defaults
            logger.error("IOException happened:{} on file:{}",e, defaultConfig);
        } finally {
            // Default values
            defaultProperty(invokewaittime, environment.getProperty("hyperledger.fabric.server.invokeWaitTime"));
            defaultProperty(deploywaittime, "120000");
            defaultProperty(proposalwaittime, "120000");
            //////
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.mspid", "Org1MSP");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.domname", "org1.example.com");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.ca_location", "http://" + accesshost + ":7054");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.caName", "ca0");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.peer_locations", "peer0.org1.example.com@grpc://" + accesshost + ":7051, peer1.org1.example.com@grpc://" + accesshost + ":7056");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.orderer_locations", "orderer.example.com@grpc://" + accesshost + ":7050");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg1.eventhub_locations", "peer0.org1.example.com@grpc://" + accesshost + ":7053,peer1.org1.example.com@grpc://" + accesshost + ":7058");

            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg2.mspid", "Org2MSP");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg2.domname", "org2.example.com");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg2.ca_location", "http://" + accesshost + ":8054");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg2.peer_locations", "peer0.org2.example.com@grpc://" + accesshost + ":8051,peer1.org2.example.com@grpc://" + accesshost + ":8056");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg2.orderer_locations", "orderer.example.com@grpc://" + accesshost + ":7050");
            defaultProperty( "org.hyperledger.fabric.consortium.integration.org.peerOrg2.eventhub_locations", "peer0.org2.example.com@grpc://" + accesshost + ":8053, peer1.org2.example.com@grpc://" + accesshost + ":8058");

            defaultProperty(integrationteststls, null);
            runningTLS = null != sdkProperties.getProperty(integrationteststls, null);
            runningFabricCATLS = runningTLS;
            runningFabricTLS = runningTLS;

            for (Map.Entry<Object, Object> x : sdkProperties.entrySet()) {
                final String key = x.getKey() + "";
                final String val = x.getValue() + "";
                if (key.startsWith(integrationtestsOrg)) {
                    Matcher match = orgPat.matcher(key);
                    if (match.matches() && match.groupCount() == 1) {
                        String orgName = match.group(1).trim();
                        sampleOrgs.put(orgName, new ConsortiumOrg(orgName, val.trim()));
                    }
                }
            }

            for (Map.Entry<String, ConsortiumOrg> org : sampleOrgs.entrySet()) {
                final ConsortiumOrg sampleOrg = org.getValue();
                final String orgName = org.getKey();
                String peerNames = sdkProperties.getProperty(integrationtestsOrg + orgName + ".peer_locations");
                String[] ps = peerNames.split("[ \t]*,[ \t]*");
                for (String peer : ps) {
                    String[] nl = peer.split("[ \t]*@[ \t]*");
                    sampleOrg.addPeerLocation(nl[0], grpcTLSify(nl[1]));
                }
                final String domainName = sdkProperties.getProperty(integrationtestsOrg + orgName + ".domname");
                sampleOrg.setDomainName(domainName);
                String ordererNames = sdkProperties.getProperty(integrationtestsOrg + orgName + ".orderer_locations");
                ps = ordererNames.split("[ \t]*,[ \t]*");
                for (String peer : ps) {
                    String[] nl = peer.split("[ \t]*@[ \t]*");
                    sampleOrg.addOrdererLocation(nl[0], grpcTLSify(nl[1]));
                }
                String eventHubNames = sdkProperties.getProperty(integrationtestsOrg + orgName + ".eventhub_locations");
                ps = eventHubNames.split("[ \t]*,[ \t]*");
                for (String peer : ps) {
                    String[] nl = peer.split("[ \t]*@[ \t]*");
                    sampleOrg.addEventHubLocation(nl[0], grpcTLSify(nl[1]));
                }
                sampleOrg.setCaLocation(httpTLSify(sdkProperties.getProperty((integrationtestsOrg + org.getKey() + ".ca_location"))));
                sampleOrg.setCaName(sdkProperties.getProperty((integrationtestsOrg + org.getKey() + ".caName")));
                if (runningFabricCATLS) {
                    String cert = "src/ca/ca.DNAME-cert.pem";
                    File cf = new File(cert);
                    if (!cf.exists() || !cf.isFile()) {
                        throw new RuntimeException("TEST is missing cert file " + cf.getAbsolutePath());
                    }
                    Properties properties = new Properties();
                    properties.setProperty("pemFile", cf.getAbsolutePath());
                    properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!
                    sampleOrg.setCaProperties(properties);
                }
            }
        }
    }
    private String grpcTLSify(String location) {
        location = location.trim();
        Exception e = Utils.checkGrpcUrl(location);
        if (e != null) {
            throw new RuntimeException(String.format("Bad TEST parameters for grpc url %s", location), e);
        }
        return runningFabricTLS ?
                location.replaceFirst("^grpc://", "grpcs://") : location;
    }
    private String httpTLSify(String location) {
        location = location.trim();
        return runningFabricCATLS ?
                location.replaceFirst("^http://", "https://") : location;
    }
    /**
     * getConfig return back singleton for SDK configuration.
     * @return Global configuration
     */
    public FabricCommonConfig getConfig() {
        if (null == config) {
            config = new FabricCommonConfig();
        }
        return config;
    }
    /**
     * getProperty return back property for the given value.
     * @param property
     * @return String value for the property
     */
    private String getProperty(String property) {
        String ret = sdkProperties.getProperty(property);
        if (null == ret) {
            logger.warn(String.format("No configuration value found for '%s'", property));
        }
        return ret;
    }
    private void defaultProperty(String key, String value) {
        String ret = System.getProperty(key);
        if (ret != null) {
            sdkProperties.put(key, ret);
        } else {
            String envKey = key.toUpperCase().replaceAll("\\.", "_");
            ret = System.getenv(envKey);
            if (null != ret) {
                sdkProperties.put(key, ret);
            } else {
                if (null == sdkProperties.getProperty(key) && value != null) {
                    sdkProperties.put(key, value);
                }
            }
        }
    }
    public int getTransactionWaitTime() {
        return Integer.parseInt(getProperty(invokewaittime));
    }
    public long getProposalWaitTime() {
        return Integer.parseInt(getProperty(proposalwaittime));
    }
    public Collection<ConsortiumOrg> getIntegrationTestsSampleOrgs() {
        return Collections.unmodifiableCollection(sampleOrgs.values());
    }
    public ConsortiumOrg getIntegrationTestsSampleOrg(String name) {
        return sampleOrgs.get(name);
    }
    public Properties getPeerProperties(String name) {
        return getEndPointProperties("peer", name);
    }
    public Properties getOrdererProperties(String name) {
        return getEndPointProperties("orderer", name);
    }
    private Properties getEndPointProperties(final String type, final String name) {
        final String domainName = getDomainName(name);
        File cert = Paths.get(getTestChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type), domainName, type + "s",
                name, "tls/server.crt").toFile();
        if (!cert.exists()) {
            throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
                    cert.getAbsolutePath()));
        }
        Properties ret = new Properties();
        ret.setProperty("pemFile", cert.getAbsolutePath());
        ret.setProperty("hostnameOverride", name);
        ret.setProperty("sslProvider", "openSSL");
        ret.setProperty("negotiationType", "TLS");
        return ret;
    }
    public Properties getEventHubProperties(String name) {
        return getEndPointProperties("peer", name); //uses same as named peer
    }
    public String getTestChannelPath() {
        return "src/test/fixture/sdkintegration/e2e-2Orgs/v1.0";
    }
    public boolean isRunningAgainstFabric10() {
        return "IntegrationSuiteV1.java".equals(System.getProperty("org.hyperledger.fabric.sdktest.ITSuite"));
    }
    private String getDomainName(final String name) {
        int dot = name.indexOf(".");
        if (-1 == dot) {
            return null;
        } else {
            return name.substring(dot + 1);
        }
    }
}
