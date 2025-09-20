package com.deepsearch.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 配置多级缓存：本地Caffeine缓存 + 分布式Redis缓存
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * 配置Caffeine本地缓存管理器（主要用于热数据）
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 配置默认的Caffeine规格
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.debug("Caffeine缓存移除: key={}, cause={}", key, cause);
                }));

        // 设置缓存名称
        cacheManager.setCacheNames(
                "autoCompleteSuggestions",
                "popularQueries",
                "userPreferences",
                "searchSuggestions",
                "trieStats"
        );

        return cacheManager;
    }

    /**
     * 配置Redis分布式缓存管理器（用于跨实例共享）
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {

        // 配置序列化方式
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)) // 默认TTL 2小时
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存null值

        // 针对不同缓存名称配置不同的TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // 自动补全建议 - 30分钟
        cacheConfigurations.put("redis:autoComplete",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // 热门查询 - 1小时
        cacheConfigurations.put("redis:popularQueries",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // 用户偏好 - 2小时
        cacheConfigurations.put("redis:userPreferences",
                defaultConfig.entryTtl(Duration.ofHours(2)));

        // 搜索建议 - 1小时
        cacheConfigurations.put("redis:searchSuggestions",
                defaultConfig.entryTtl(Duration.ofHours(1)));

        // Trie树数据 - 24小时
        cacheConfigurations.put("redis:trieData",
                defaultConfig.entryTtl(Duration.ofHours(24)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Caffeine缓存管理器自定义配置
     */
    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer() {
        return cacheManager -> {
            // 为特定缓存配置不同的Caffeine规格

            // 自动补全建议缓存 - 高频访问，大容量
            cacheManager.registerCustomCache("autoCompleteSuggestions",
                    Caffeine.newBuilder()
                            .maximumSize(50000)
                            .expireAfterWrite(30, TimeUnit.MINUTES)
                            .recordStats()
                            .build());

            // 热门查询缓存 - 中等容量，较长过期时间
            cacheManager.registerCustomCache("popularQueries",
                    Caffeine.newBuilder()
                            .maximumSize(1000)
                            .expireAfterWrite(1, TimeUnit.HOURS)
                            .recordStats()
                            .build());

            // 用户偏好缓存 - 用户级数据
            cacheManager.registerCustomCache("userPreferences",
                    Caffeine.newBuilder()
                            .maximumSize(10000)
                            .expireAfterAccess(2, TimeUnit.HOURS)
                            .recordStats()
                            .build());

            // 搜索建议缓存 - 中等频率访问
            cacheManager.registerCustomCache("searchSuggestions",
                    Caffeine.newBuilder()
                            .maximumSize(10000)
                            .expireAfterWrite(1, TimeUnit.HOURS)
                            .recordStats()
                            .build());

            log.info("Caffeine缓存管理器自定义配置完成");
        };
    }

    /**
     * 缓存统计信息Bean（用于监控）
     */
    @Bean
    public CacheStatsService cacheStatsService(CacheManager caffeineCacheManager,
                                              CacheManager redisCacheManager) {
        return new CacheStatsService(caffeineCacheManager, redisCacheManager);
    }

    /**
     * 缓存统计服务
     */
    public static class CacheStatsService {
        private final CacheManager caffeineCacheManager;
        private final CacheManager redisCacheManager;

        public CacheStatsService(CacheManager caffeineCacheManager, CacheManager redisCacheManager) {
            this.caffeineCacheManager = caffeineCacheManager;
            this.redisCacheManager = redisCacheManager;
        }

        /**
         * 获取Caffeine缓存统计
         */
        public Map<String, Object> getCaffeineStats() {
            Map<String, Object> stats = new HashMap<>();

            caffeineCacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = caffeineCacheManager.getCache(cacheName);
                if (cache instanceof org.springframework.cache.caffeine.CaffeineCache) {
                    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                            ((org.springframework.cache.caffeine.CaffeineCache) cache).getNativeCache();

                    Map<String, Object> cacheStats = new HashMap<>();
                    cacheStats.put("hitCount", nativeCache.stats().hitCount());
                    cacheStats.put("missCount", nativeCache.stats().missCount());
                    cacheStats.put("hitRate", nativeCache.stats().hitRate());
                    cacheStats.put("evictionCount", nativeCache.stats().evictionCount());
                    cacheStats.put("estimatedSize", nativeCache.estimatedSize());

                    stats.put(cacheName, cacheStats);
                }
            });

            return stats;
        }

        /**
         * 获取Redis缓存统计
         */
        public Map<String, Object> getRedisStats() {
            Map<String, Object> stats = new HashMap<>();

            redisCacheManager.getCacheNames().forEach(cacheName -> {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", cacheName);
                cacheInfo.put("type", "redis");
                stats.put(cacheName, cacheInfo);
            });

            return stats;
        }

        /**
         * 清除所有缓存
         */
        public void clearAllCaches() {
            // 清除Caffeine缓存
            caffeineCacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = caffeineCacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });

            // 清除Redis缓存
            redisCacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = redisCacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });

            log.info("所有缓存已清除");
        }

        /**
         * 清除指定缓存
         */
        public void clearCache(String cacheName) {
            // 清除Caffeine缓存
            org.springframework.cache.Cache caffeineCache = caffeineCacheManager.getCache(cacheName);
            if (caffeineCache != null) {
                caffeineCache.clear();
            }

            // 清除Redis缓存
            org.springframework.cache.Cache redisCache = redisCacheManager.getCache("redis:" + cacheName);
            if (redisCache != null) {
                redisCache.clear();
            }

            log.info("缓存 {} 已清除", cacheName);
        }
    }
}