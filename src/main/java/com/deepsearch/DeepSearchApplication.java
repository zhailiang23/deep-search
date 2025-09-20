package com.deepsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Deep Search Application 主启动类
 * 智能搜索平台的核心应用程序入口
 */
@SpringBootApplication
@EnableTransactionManagement
public class DeepSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepSearchApplication.class, args);
    }
}