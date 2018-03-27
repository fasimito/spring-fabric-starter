/*
 * Copyright (c) 2018 author All Rights Reserved.
 * this is the consortium store object.
 */
package com.hyperledger.fabric.consortium;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.util.encoders.Hex;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.PrivateKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author: jate  Date: 2018/3/19 Time: 11:16
 */
public class ConsortiumStore {
    private static final Logger logger = LoggerFactory.getLogger(ConsortiumStore.class);
    private String file;
    private final Map<String,ConsortiumUser> members = new HashMap<String, ConsortiumUser>();
    public ConsortiumStore(File file){
        this.file = file.getAbsolutePath();
    }

    /**
     * set the name relative value.
     * @param name
     * @param value
     */
    public void setValue(String name, String value){
        Properties properties = loadProperties();
        try (OutputStream os = new FileOutputStream(file)){
            properties.setProperty(name,value);
            properties.store(os,"");
        }catch (IOException ex){
            logger.error("exception happened when process setValue,ex={}",ex);
        }
    }

    /**
     * get the name related value.
     * @param name
     * @return
     */
    public String getValue(String name){
        Properties properties = loadProperties();
        return properties.getProperty(name);
    }

    /**
     * load the configuration file
     * @return
     */
    private Properties loadProperties() {
        logger.info("loading the properties file");
        Properties properties = new Properties();
        try(InputStream inputStream = new FileInputStream(file)){
            properties.load(inputStream);
        }catch (FileNotFoundException ex){
            logger.error("could not find the file:{},cause the exception:{}",file,ex);
        }catch (IOException ex){
            logger.error("Could not load keyvalue store from file:{},cause the exception:{}",file,ex);
        }
        return properties;
    }

    /**
     * get the {@link ConsortiumUser} object by {@code name} and {@code org}
     * @param name
     * @param org
     * @return consortium user
     */
    public ConsortiumUser getMember(String name, String org){
        ConsortiumUser consortiumUser = members.get(ConsortiumUser.toKeyValStoreName(name,org));
        if(null != consortiumUser){
            return consortiumUser;
        }
        consortiumUser = new ConsortiumUser(name,org,this);
        return consortiumUser;
    }

    /**
     * get the specify {@link ConsortiumUser} by the {@code name} and the {@code org} and {@code mspId}
     * @param name
     * @param org
     * @param mspId
     * @param privateKeyFile
     * @param certificateFile
     * @return consortium user
     * @throws IOException
     * @throws NoSuchMethodException
     */
    public ConsortiumUser getMember(String name, String org, String mspId, File privateKeyFile, File certificateFile)
             throws IOException,NoSuchMethodException{
        try{
            ConsortiumUser consortiumUser = members.get(ConsortiumUser.toKeyValStoreName(name,org));
            if(null != consortiumUser){
                logger.info("get the consortium user in the cache, consortiumUser = {}",consortiumUser);
                return consortiumUser;
            }
            consortiumUser = new ConsortiumUser(name,org,this);
            consortiumUser.setMspId(mspId);
            String certificate = new String(IOUtils.toByteArray(new FileInputStream(certificateFile)),"UTF-8");
            PrivateKey privateKey = getPrivateKeyFromBytes(IOUtils.toByteArray(new FileInputStream(privateKeyFile)));
            consortiumUser.setEnrollment(new StoreEnrollement(privateKey,certificate));
            return consortiumUser;
        }catch (IOException ex){
            logger.error("IO exception happened while get the consortium user:{}",ex);
            throw ex;
        }catch (NoSuchMethodException ex){
            logger.error("can't find method exception:{}",ex);
            throw ex;
        }
    }

    /**
     * get the private key {@link PrivateKey} through the byte array {@code data}
     * @param data byte array
     * @return private key
     * @throws IOException
     * @throws NoSuchMethodException
     */
    private PrivateKey getPrivateKeyFromBytes(byte[] data) throws IOException,NoSuchMethodException{
        Reader pemReader = new StringReader(new String(data));
        PrivateKeyInfo pemPair = null;
        PEMParser pemParser = new PEMParser(pemReader);
        pemPair = (PrivateKeyInfo) pemParser.readObject();
        PrivateKey privateKey = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);
        return privateKey;
    }

    static {
        try{
            Security.addProvider(new BouncyCastleProvider());
        }catch (Exception ex){
            logger.error("get security provider exception:{}",ex);
        }
    }

    /**
     * inner class of enrollment.
     */
    static final class StoreEnrollement implements Enrollment,Serializable{

        private static final long serialVersionUID = 6965341351899577992L;

        //private key
        private final PrivateKey privateKey;
        //authorize certificate
        private final String certificate;
        StoreEnrollement(PrivateKey privateKey,String certificate){
            this.privateKey = privateKey;
            this.certificate = certificate;
        }
        /**
         * Get the user's private key
         * @return private key.
         */
        @Override
        public PrivateKey getKey() {
            return this.privateKey;
        }

        /**
         * Get the user's signed certificate.
         * @return a certificate.
         */
        @Override
        public String getCert() {
            return this.certificate;
        }
    }


    public void saveChannel(Channel channel) throws IOException, InvalidArgumentException {

        setValue("channel." + channel.getName(), Hex.toHexString(channel.serializeChannel()));

    }

    Channel getChannel(HFClient client, String name) throws IOException, ClassNotFoundException, InvalidArgumentException {
        Channel ret = null;
        String channelHex = getValue("channel." + name);
        if (channelHex != null) {
            ret = client.deSerializeChannel(Hex.decode(channelHex));
        }
        return ret;
    }

    public void storeClientPEMTLSKey(ConsortiumOrg consortiumOrg, String key) {

        setValue("clientPEMTLSKey." + consortiumOrg.getName(), key);

    }

    public String getClientPEMTLSKey(ConsortiumOrg consortiumOrg) {

        return getValue("clientPEMTLSKey." + consortiumOrg.getName());

    }

    public void storeClientPEMTLCertificate(ConsortiumOrg consortiumOrg, String certificate) {

        setValue("clientPEMTLSCertificate." + consortiumOrg.getName(), certificate);

    }

    public String getClientPEMTLSCertificate(ConsortiumOrg consortiumOrg) {

        return getValue("clientPEMTLSCertificate." + consortiumOrg.getName());

    }

}