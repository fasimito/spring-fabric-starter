/*
 * Copyright (c) 2018 www.yiji.com. fx.yiji.com All Rights Reserved.
 */
package com.hyperledger.fabric.controller;

import com.hyperledger.fabric.consortium.ConsortiumChaincodeManager;
import com.hyperledger.fabric.consortium.ConsortiumManager;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * @author: jate  Date: 2018/3/26 Time: 21:27
 */
@RestController
public class BlockTestController {

    private static final Logger logger = LoggerFactory.getLogger(BlockTestController.class);

    private ConsortiumManager consortiumManager;

    @RequestMapping(value = "/query")
    public String query(){
        String result = "query failed";
        logger.info("this is a query test of the fabric Blockchain");
        try {
            consortiumManager = new ConsortiumManager();
            ConsortiumChaincodeManager ccManager = consortiumManager.getManager();
            String[] parameters = {"a"};
            Map<String,String> queryMap = ccManager.query("query",parameters);
            logger.info("the query results is :{}",queryMap);
            result = "query success";
        } catch (CryptoException e) {
            logger.error("CryptoException happened: {}",e);
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            logger.error("InvalidArgumentException happened: {}",e);
            e.printStackTrace();
        } catch (TransactionException e) {
            logger.error("TransactionException happened: {}",e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("IOException happened: {}",e);
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            logger.error("NoSuchMethodException happened: {}",e);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            logger.error("ClassNotFoundException happened: {}",e);
            e.printStackTrace();
        } catch (InstantiationException e) {
            logger.error("InstantiationException happened: {}",e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            logger.error("IllegalAccessException happened: {}",e);
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            logger.error("InvocationTargetException happened: {}",e);
            e.printStackTrace();
        } catch (ProposalException e) {
            logger.error("ProposalException happened: {}",e);
            e.printStackTrace();
        }
        return result;
    }
}