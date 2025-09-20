package com.deepsearch.vector.cache;

import com.deepsearch.vector.model.Vector;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * 向量缓存接口
 * 提供向量的缓存和检索功能，避免重复计算
 */
public interface VectorCache {

    /**
     * 存储向量到缓存
     *
     * @param key 缓存键
     * @param vector 向量对象
     * @param ttl 过期时间
     */
    void put(String key, Vector vector, Duration ttl);

    /**
     * 使用默认TTL存储向量到缓存
     *
     * @param key 缓存键
     * @param vector 向量对象
     */
    void put(String key, Vector vector);

    /**
     * 从缓存获取向量
     *
     * @param key 缓存键
     * @return 向量对象，如果不存在则返回空
     */
    Optional<Vector> get(String key);

    /**
     * 检查缓存中是否存在指定键
     *
     * @param key 缓存键
     * @return 如果存在则返回true
     */
    boolean exists(String key);

    /**
     * 从缓存删除向量
     *
     * @param key 缓存键
     * @return 如果删除成功则返回true
     */
    boolean delete(String key);

    /**
     * 批量删除向量
     *
     * @param keys 缓存键集合
     * @return 删除的数量
     */
    long delete(Set<String> keys);

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计
     */
    CacheStats getStats();

    /**
     * 设置向量的过期时间
     *
     * @param key 缓存键
     * @param ttl 过期时间
     * @return 如果设置成功则返回true
     */
    boolean expire(String key, Duration ttl);

    /**
     * 获取向量的剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余时间，如果键不存在或没有过期时间则返回空
     */
    Optional<Duration> getTtl(String key);

    /**
     * 缓存统计信息
     */
    class CacheStats {
        private final long totalRequests;
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long evictionCount;
        private final long estimatedSize;

        public CacheStats(long totalRequests, long hitCount, long missCount,
                         double hitRate, long evictionCount, long estimatedSize) {
            this.totalRequests = totalRequests;
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.evictionCount = evictionCount;
            this.estimatedSize = estimatedSize;
        }

        public long getTotalRequests() { return totalRequests; }
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public long getEvictionCount() { return evictionCount; }
        public long getEstimatedSize() { return estimatedSize; }
    }
}