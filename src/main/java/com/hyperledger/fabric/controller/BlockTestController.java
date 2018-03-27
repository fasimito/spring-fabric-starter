/*
 * Copyright (c) 2018 www.yiji.com. fx.yiji.com All Rights Reserved.
 */
package com.hyperledger.fabric.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: jate  Date: 2018/3/26 Time: 21:27
 */
@RestController
public class BlockTestController {

    @RequestMapping(value = "/query")
    public String query(){
        return "welcome to the query page";
    }
}