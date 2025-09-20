package com.deepsearch.vector.cache;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.deepsearch.vector.model.Vector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis向量缓存实现
 * 使用Redis作为向量缓存存储，支持TTL和性能监控
 */
@Component
@Slf4j
public class RedisVectorCache implements VectorCache {

    private static final String VECTOR_KEY_PREFIX = "vector:";
    private static final String STATS_KEY_PREFIX = "vector:stats:";
    private static final String HITS_KEY = STATS_KEY_PREFIX + "hits";
    private static final String MISSES_KEY = STATS_KEY_PREFIX + "misses";
    private static final String REQUESTS_KEY = STATS_KEY_PREFIX + "requests";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;

    // 本地计数器，用于减少Redis操作
    private final AtomicLong localHits = new AtomicLong(0);
    private final AtomicLong localMisses = new AtomicLong(0);
    private final AtomicLong localRequests = new AtomicLong(0);

    // Lua脚本：原子性的获取并增加统计
    private static final String GET_AND_COUNT_SCRIPT = """
            local key = KEYS[1]
            local hitsKey = KEYS[2]
            local missesKey = KEYS[3]
            local requestsKey = KEYS[4]

            local value = redis.call('GET', key)
            redis.call('INCR', requestsKey)

            if value then
                redis.call('INCR', hitsKey)
                return value
            else
                redis.call('INCR', missesKey)
                return nil
            end
            """;

    private final DefaultRedisScript<String> getAndCountScript;

    public RedisVectorCache(RedisTemplate<String, String> redisTemplate,
                           ObjectMapper objectMapper,
                           @Value("${vector.cache.default-ttl:24h}") Duration defaultTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultTtl = defaultTtl;
        this.getAndCountScript = new DefaultRedisScript<>(GET_AND_COUNT_SCRIPT, String.class);
    }

    @Override
    public void put(String key, Vector vector, Duration ttl) {
        if (key == null || vector == null) {
            log.warn("尝试缓存空的键或向量");
            return;
        }

        try {
            String fullKey = VECTOR_KEY_PREFIX + key;
            String vectorJson = objectMapper.writeValueAsString(vector);

            if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                redisTemplate.opsForValue().set(fullKey, vectorJson, ttl);
            } else {
                redisTemplate.opsForValue().set(fullKey, vectorJson);
            }

            log.debug("向量已缓存: key={}, dimension={}, ttl={}", key, vector.getDimension(), ttl);
        } catch (JsonProcessingException e) {
            log.error("向量序列化失败: key={}", key, e);
            throw new RuntimeException("向量缓存失败", e);
        }
    }

    @Override
    public void put(String key, Vector vector) {
        put(key, vector, defaultTtl);
    }

    @Override
    public Optional<Vector> get(String key) {
        if (key == null) {
            return Optional.empty();
        }

        try {
            String fullKey = VECTOR_KEY_PREFIX + key;

            // 使用Lua脚本原子性地获取值并更新统计
            String vectorJson = redisTemplate.execute(getAndCountScript,
                Collections.singletonList(fullKey),
                fullKey, HITS_KEY, MISSES_KEY, REQUESTS_KEY);

            // 更新本地计数器
            localRequests.incrementAndGet();

            if (vectorJson != null) {
                localHits.incrementAndGet();
                Vector vector = objectMapper.readValue(vectorJson, Vector.class);
                log.debug("向量缓存命中: key={}, dimension={}", key, vector.getDimension());
                return Optional.of(vector);
            } else {
                localMisses.incrementAndGet();
                log.debug("向量缓存未命中: key={}", key);
                return Optional.empty();
            }
        } catch (JsonProcessingException e) {
            log.error("向量反序列化失败: key={}", key, e);
            localMisses.incrementAndGet();
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }
        String fullKey = VECTOR_KEY_PREFIX + key;
        Boolean exists = redisTemplate.hasKey(fullKey);
        return exists != null && exists;
    }

    @Override
    public boolean delete(String key) {
        if (key == null) {
            return false;
        }
        String fullKey = VECTOR_KEY_PREFIX + key;
        Boolean deleted = redisTemplate.delete(fullKey);
        log.debug("删除向量缓存: key={}, success={}", key, deleted);
        return deleted != null && deleted;
    }

    @Override
    public long delete(Set<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        Set<String> fullKeys = keys.stream()
            .map(key -> VECTOR_KEY_PREFIX + key)
            .collect(Collectors.toSet());

        Long deletedCount = redisTemplate.delete(fullKeys);
        log.debug("批量删除向量缓存: count={}, success={}", keys.size(), deletedCount);
        return deletedCount != null ? deletedCount : 0;
    }

    @Override
    public void clear() {
        try {
            Set<String> keys = redisTemplate.keys(VECTOR_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清空向量缓存: 删除了{}个向量", keys.size());
            }
        } catch (Exception e) {
            log.error("清空向量缓存失败", e);
        }
    }

    @Override
    public CacheStats getStats() {
        try {
            // 从Redis获取统计数据
            String hitsStr = redisTemplate.opsForValue().get(HITS_KEY);
            String missesStr = redisTemplate.opsForValue().get(MISSES_KEY);
            String requestsStr = redisTemplate.opsForValue().get(REQUESTS_KEY);

            long hits = (hitsStr != null) ? Long.parseLong(hitsStr) : 0;
            long misses = (missesStr != null) ? Long.parseLong(missesStr) : 0;
            long requests = (requestsStr != null) ? Long.parseLong(requestsStr) : 0;

            // 加上本地计数器
            hits += localHits.get();
            misses += localMisses.get();
            requests += localRequests.get();

            double hitRate = requests > 0 ? (double) hits / requests : 0.0;

            // 估算缓存大小
            Set<String> keys = redisTemplate.keys(VECTOR_KEY_PREFIX + "*");
            long estimatedSize = keys != null ? keys.size() : 0;

            return new CacheStats(requests, hits, misses, hitRate, 0, estimatedSize);
        } catch (Exception e) {
            log.error("获取缓存统计失败", e);
            return new CacheStats(0, 0, 0, 0.0, 0, 0);
        }
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        if (key == null || ttl == null) {
            return false;
        }
        String fullKey = VECTOR_KEY_PREFIX + key;
        Boolean result = redisTemplate.expire(fullKey, ttl);
        return result != null && result;
    }

    @Override
    public Optional<Duration> getTtl(String key) {
        if (key == null) {
            return Optional.empty();
        }

        String fullKey = VECTOR_KEY_PREFIX + key;
        Long ttlSeconds = redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);

        if (ttlSeconds == null || ttlSeconds < 0) {
            return Optional.empty();
        }

        return Optional.of(Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 清除统计数据
     */
    public void clearStats() {
        redisTemplate.delete(HITS_KEY);
        redisTemplate.delete(MISSES_KEY);
        redisTemplate.delete(REQUESTS_KEY);
        localHits.set(0);
        localMisses.set(0);
        localRequests.set(0);
        log.info("缓存统计数据已清除");
    }

    /**
     * 获取缓存键的模式匹配数量
     */
    public long getKeyCount(String pattern) {
        Set<String> keys = redisTemplate.keys(VECTOR_KEY_PREFIX + pattern);
        return keys != null ? keys.size() : 0;
    }
}