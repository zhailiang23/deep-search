package com.deepsearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 数据库配置类
 * 配置JPA仓库和事务管理
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.deepsearch.repository")
@EnableTransactionManagement
public class DatabaseConfig {

    // Spring Boot 自动配置数据源，这里主要开启JPA仓库扫描
}