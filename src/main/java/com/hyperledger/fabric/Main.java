/*
 * Copyright (c) 2018 www.yiji.com. fx.yiji.com All Rights Reserved.
 */
package com.hyperledger.fabric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author: jate  Date: 2018/3/19 Time: 9:32
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.hyperledger.fabric")
public class Main {
    public static void main(String[] args){
        SpringApplication.run(Main.class,args);
    }
}