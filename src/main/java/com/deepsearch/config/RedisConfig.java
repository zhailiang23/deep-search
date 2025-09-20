package com.deepsearch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类
 * 配置Redis连接、序列化和缓存策略
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Value("${vector.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${vector.cache.default-ttl:24h}")
    private Duration defaultTtl;

    /**
     * Redis连接工厂配置
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("初始化Redis连接工厂，缓存启用状态: {}", cacheEnabled);
        return new LettuceConnectionFactory();
    }

    /**
     * RedisTemplate配置
     * 配置键值序列化策略，支持向量对象的存储
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用String序列化器处理键
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 使用String序列化器处理值（JSON字符串）
        template.setValueSerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();

        log.info("Redis模板配置完成，默认TTL: {}", defaultTtl);
        return template;
    }

    /**
     * JSON序列化的ObjectMapper配置
     * 专门用于向量对象的序列化
     */
    @Bean("vectorObjectMapper")
    public ObjectMapper vectorObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册Java 8时间模块
        mapper.registerModule(new JavaTimeModule());

        // 配置序列化选项
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 配置反序列化选项
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        log.debug("向量专用ObjectMapper配置完成");
        return mapper;
    }

    /**
     * 通用Redis模板（支持对象序列化）
     * 用于存储复杂对象
     */
    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 键序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // 值序列化 - 使用Jackson JSON序列化
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(vectorObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();

        log.debug("对象Redis模板配置完成");
        return template;
    }

    /**
     * 缓存配置信息Bean
     * 提供缓存相关的配置信息
     */
    @Bean
    public CacheConfigInfo cacheConfigInfo() {
        return new CacheConfigInfo(cacheEnabled, defaultTtl);
    }

    /**
     * 缓存配置信息记录
     */
    public record CacheConfigInfo(boolean enabled, Duration defaultTtl) {}

    /**
     * 开发环境Redis配置
     * 使用内存数据库进行测试
     */
    @Configuration
    @Profile("dev")
    static class DevRedisConfig {

        @Bean
        public RedisTemplate<String, String> devRedisTemplate() {
            log.info("开发环境：使用内存Redis配置");
            // 在开发环境中，可以配置内存Redis或使用embedded Redis
            return new RedisTemplate<>();
        }
    }

    /**
     * 生产环境Redis配置
     * 包含连接池优化和监控配置
     */
    @Configuration
    @Profile("prod")
    static class ProdRedisConfig {

        @Bean
        public RedisTemplate<String, String> prodRedisTemplate(RedisConnectionFactory connectionFactory) {
            log.info("生产环境：使用优化Redis配置");

            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(connectionFactory);

            // 生产环境的特殊配置
            template.setEnableTransactionSupport(true);

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(stringSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(stringSerializer);

            template.afterPropertiesSet();
            return template;
        }
    }
}