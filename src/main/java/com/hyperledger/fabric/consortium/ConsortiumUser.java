/*
 * Copyright (c) 2018 author All Rights Reserved.
 */
package com.hyperledger.fabric.consortium;

import io.netty.util.internal.StringUtil;
import org.bouncycastle.util.encoders.Hex;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Set;

/**
 * @author: Xupeng.Ma  Date: 2018/3/25 Time: 13:15
 * actually, if you want to access the affiliation of the block chain network. you must take the
 * consortium user information and pass the identification.
 * or you could not use the block chain network services.
 */
public class ConsortiumUser implements User,Serializable{

    private static final Logger logger = LoggerFactory.getLogger(ConsortiumUser.class);

    private static final long serialVersionUID = 5695080465408336815L;

    private String name;
    private Set<String> roles;
    private String account;
    private String affiliation;
    private Enrollment enrollment;
    private String mspId;

    private String organization;
    private String enrollmentSecret;
    private String keyValStoreName;
    private transient ConsortiumStore consortiumStore;
    public ConsortiumUser(String name, String org,ConsortiumStore consortiumStore){
        this.name = name;
        this.organization = org;
        this.consortiumStore = consortiumStore;
        this.keyValStoreName = toKeyValStoreName(name,org);
        String memberStr = consortiumStore.getValue(keyValStoreName);
        if(null != memberStr){
            saveState();
        }else{
            restoreState();
        }
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
     * set the rule information and store the user state into the {@link ConsortiumStore} object.
     * @param roles
     */
    public void setRoles(Set<String> roles) {
        this.roles = roles;
        saveState();
    }

    /**
     * set the account information, and finally save it into the {@link ConsortiumStore} object.
     * @param account the account
     */
    public void setAccount(String account) {
        this.account = account;
        saveState();
    }
    /**
     * Save the state of this user to the key value store.
     * store the user state information into the {@link ConsortiumStore} object.
     */
    private void saveState() {
        logger.info("save state information into the ConsortiumStore object");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try{
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(this);
            os.flush();
            consortiumStore.setValue(keyValStoreName,Hex.toHexString(bos.toByteArray()));
            os.close();
        }catch (Exception ex){
            logger.error("Exception happened when save state info:{}",ex);
        }
    }

    /**
     * set the subordinate alliance and store the user state into the {@link ConsortiumStore} instance.
     * @param affiliation the subordinate alliance
     */
    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
        saveState();
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getEnrollmentSecret() {
        return enrollmentSecret;
    }

    /**
     * set the enrollment keys information and update the user state into {@link ConsortiumStore} object.
     * @param enrollmentSecret
     */
    public void setEnrollmentSecret(String enrollmentSecret) {
        this.enrollmentSecret = enrollmentSecret;
        saveState();
    }
    /**
     * check if the name had been registered
     * @return {@code true} if registered; otherwise {@code false}.
     */
    public boolean isRegistered() {
        return !StringUtil.isNullOrEmpty(enrollmentSecret);
    }
    /**
     * set the member service provider Id and store the user state into the {@link ConsortiumStore} object.
     * @param mspId
     */
    public void setMspId(String mspId) {
        this.mspId = mspId;
        saveState();
    }

    /**
     * set the member enrollment contents and update the user state into the {@link ConsortiumStore} object
     * @param enrollment
     */
    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
        saveState();
    }

    /**
     * Determine if this name has been enrolled.
     * @return @return {@code true} if enrolled; otherwise {@code false}.
     */
    public boolean isEnrolled() {
        return this.enrollment != null;
    }

    public String getKeyValStoreName() {
        return keyValStoreName;
    }

    public void setKeyValStoreName(String keyValStoreName) {
        this.keyValStoreName = keyValStoreName;
    }

    public ConsortiumStore getConsortiumStore() {
        return consortiumStore;
    }

    public void setConsortiumStore(ConsortiumStore consortiumStore) {
        this.consortiumStore = consortiumStore;
    }

    /**
     * Get the name that identifies the user.
     * @return the user name.
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Get the roles to which the user belongs.
     *
     * @return role names.
     */
    @Override
    public Set<String> getRoles() {
        return this.roles;
    }

    /**
     * Get the user's account
     *
     * @return the account name
     */
    @Override
    public String getAccount() {
        return this.account;
    }

    /**
     * Get the user's affiliation.
     *
     * @return the affiliation.
     */
    @Override
    public String getAffiliation() {
        return this.affiliation;
    }

    /**
     * Get the user's enrollment certificate information.
     *
     * @return the enrollment information.
     */
    @Override
    public Enrollment getEnrollment() {
        return this.enrollment;
    }

    /**
     * Get the Membership Service Provider Identifier provided by the user's organization.
     *
     * @return MSP Id.
     */
    @Override
    public String getMspId() {
        return this.mspId;
    }

    /**
     * find the user from the key-value cache;
     * if it had been found, restore the object, or, do nothing
     * @return
     */
    private ConsortiumUser restoreState(){
        String memberStr = consortiumStore.getValue(keyValStoreName);
        if(null != memberStr && !"".equals(memberStr)){ //the user had been found in the key-value cache.
            byte[] serialized = Hex.decode(memberStr);
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            try{
                ObjectInputStream ois = new ObjectInputStream(bis);
                ConsortiumUser user = (ConsortiumUser) ois.readObject();
                if(user != null){
                    this.name = user.getName();
                    this.roles = user.getRoles();
                    this.account = user.getAccount();
                    this.affiliation = user.getAffiliation();
                    this.organization = user.getOrganization();
                    this.enrollmentSecret = user.getEnrollmentSecret();
                    this.enrollment = user.getEnrollment();
                    this.mspId = user.getMspId();
                    return this;
                }
            }catch (Exception ex){
                logger.error("exception happened:{}",ex);
                throw new RuntimeException(String.format("Could not restore state of member %s", this.name),ex);
            }
        }
        return null;
    }

    public static String toKeyValStoreName(String name, String org){
        logger.info("toKeyValStoreName = " + "user." + name + org);
        return "user." + name + org;
    }
}