package com.deepsearch.vector.cache;

import com.deepsearch.vector.model.Vector;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Redis向量缓存测试类
 */
@ExtendWith(MockitoExtension.class)
class RedisVectorCacheTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisVectorCache vectorCache;
    private Vector testVector;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Duration defaultTtl = Duration.ofHours(24);

        vectorCache = new RedisVectorCache(redisTemplate, objectMapper, defaultTtl);

        // 设置mock行为
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 创建测试向量
        testVector = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test-model");
    }

    @Test
    @DisplayName("缓存向量 - 带TTL")
    void testPut_WithTtl() throws Exception {
        // Arrange
        String key = "test-key";
        Duration ttl = Duration.ofMinutes(30);
        String expectedJson = objectMapper.writeValueAsString(testVector);

        // Act
        vectorCache.put(key, testVector, ttl);

        // Assert
        verify(valueOperations).set(eq("vector:test-key"), eq(expectedJson), eq(ttl));
    }

    @Test
    @DisplayName("缓存向量 - 使用默认TTL")
    void testPut_WithDefaultTtl() throws Exception {
        // Arrange
        String key = "test-key";
        String expectedJson = objectMapper.writeValueAsString(testVector);

        // Act
        vectorCache.put(key, testVector);

        // Assert
        verify(valueOperations).set(eq("vector:test-key"), eq(expectedJson), any(Duration.class));
    }

    @Test
    @DisplayName("缓存向量 - null键")
    void testPut_NullKey() {
        // Act
        vectorCache.put(null, testVector, Duration.ofMinutes(30));

        // Assert
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("缓存向量 - null向量")
    void testPut_NullVector() {
        // Act
        vectorCache.put("test-key", null, Duration.ofMinutes(30));

        // Assert
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("获取缓存向量 - 命中")
    void testGet_CacheHit() throws Exception {
        // Arrange
        String key = "test-key";
        String vectorJson = objectMapper.writeValueAsString(testVector);

        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(vectorJson);

        // Act
        Optional<Vector> result = vectorCache.get(key);

        // Assert
        assertTrue(result.isPresent());
        Vector retrieved = result.get();
        assertEquals(testVector.getDimension(), retrieved.getDimension());
        assertEquals(testVector.getModelName(), retrieved.getModelName());
        assertEquals(testVector.getData(), retrieved.getData());
    }

    @Test
    @DisplayName("获取缓存向量 - 未命中")
    void testGet_CacheMiss() {
        // Arrange
        String key = "test-key";

        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(null);

        // Act
        Optional<Vector> result = vectorCache.get(key);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("获取缓存向量 - null键")
    void testGet_NullKey() {
        // Act
        Optional<Vector> result = vectorCache.get(null);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("获取缓存向量 - JSON反序列化失败")
    void testGet_DeserializationError() {
        // Arrange
        String key = "test-key";
        String invalidJson = "invalid json";

        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(invalidJson);

        // Act
        Optional<Vector> result = vectorCache.get(key);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("检查键存在")
    void testExists_KeyExists() {
        // Arrange
        String key = "test-key";
        when(redisTemplate.hasKey("vector:test-key")).thenReturn(true);

        // Act
        boolean exists = vectorCache.exists(key);

        // Assert
        assertTrue(exists);
    }

    @Test
    @DisplayName("检查键不存在")
    void testExists_KeyNotExists() {
        // Arrange
        String key = "test-key";
        when(redisTemplate.hasKey("vector:test-key")).thenReturn(false);

        // Act
        boolean exists = vectorCache.exists(key);

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("检查键存在 - null键")
    void testExists_NullKey() {
        // Act
        boolean exists = vectorCache.exists(null);

        // Assert
        assertFalse(exists);
    }

    @Test
    @DisplayName("删除单个向量")
    void testDelete_SingleKey() {
        // Arrange
        String key = "test-key";
        when(redisTemplate.delete("vector:test-key")).thenReturn(true);

        // Act
        boolean deleted = vectorCache.delete(key);

        // Assert
        assertTrue(deleted);
    }

    @Test
    @DisplayName("删除单个向量 - 键不存在")
    void testDelete_KeyNotExists() {
        // Arrange
        String key = "test-key";
        when(redisTemplate.delete("vector:test-key")).thenReturn(false);

        // Act
        boolean deleted = vectorCache.delete(key);

        // Assert
        assertFalse(deleted);
    }

    @Test
    @DisplayName("批量删除向量")
    void testDelete_MultipleKeys() {
        // Arrange
        Set<String> keys = Set.of("key1", "key2", "key3");
        when(redisTemplate.delete(any(Set.class))).thenReturn(3L);

        // Act
        long deletedCount = vectorCache.delete(keys);

        // Assert
        assertEquals(3L, deletedCount);
    }

    @Test
    @DisplayName("批量删除向量 - 空集合")
    void testDelete_EmptySet() {
        // Act
        long deletedCount = vectorCache.delete(Set.of());

        // Assert
        assertEquals(0L, deletedCount);
    }

    @Test
    @DisplayName("批量删除向量 - null集合")
    void testDelete_NullSet() {
        // Act
        long deletedCount = vectorCache.delete((Set<String>) null);

        // Assert
        assertEquals(0L, deletedCount);
    }

    @Test
    @DisplayName("清空缓存")
    void testClear() {
        // Arrange
        Set<String> keys = Set.of("vector:key1", "vector:key2");
        when(redisTemplate.keys("vector:*")).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L);

        // Act
        vectorCache.clear();

        // Assert
        verify(redisTemplate).keys("vector:*");
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("清空缓存 - 无键存在")
    void testClear_NoKeys() {
        // Arrange
        when(redisTemplate.keys("vector:*")).thenReturn(Set.of());

        // Act
        vectorCache.clear();

        // Assert
        verify(redisTemplate).keys("vector:*");
        verify(redisTemplate, never()).delete(any(Set.class));
    }

    @Test
    @DisplayName("获取缓存统计")
    void testGetStats() {
        // Arrange
        when(redisTemplate.opsForValue().get("vector:stats:hits")).thenReturn("100");
        when(redisTemplate.opsForValue().get("vector:stats:misses")).thenReturn("20");
        when(redisTemplate.opsForValue().get("vector:stats:requests")).thenReturn("120");
        when(redisTemplate.keys("vector:*")).thenReturn(Set.of("vector:key1", "vector:key2"));

        // Act
        VectorCache.CacheStats stats = vectorCache.getStats();

        // Assert
        assertEquals(120, stats.totalRequests());
        assertEquals(100, stats.hitCount());
        assertEquals(20, stats.missCount());
        assertEquals(100.0 / 120.0, stats.hitRate(), 0.001);
        assertEquals(2, stats.estimatedSize());
    }

    @Test
    @DisplayName("获取缓存统计 - 无统计数据")
    void testGetStats_NoData() {
        // Arrange
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(null);
        when(redisTemplate.keys("vector:*")).thenReturn(Set.of());

        // Act
        VectorCache.CacheStats stats = vectorCache.getStats();

        // Assert
        assertEquals(0, stats.totalRequests());
        assertEquals(0, stats.hitCount());
        assertEquals(0, stats.missCount());
        assertEquals(0.0, stats.hitRate());
        assertEquals(0, stats.estimatedSize());
    }

    @Test
    @DisplayName("设置过期时间")
    void testExpire() {
        // Arrange
        String key = "test-key";
        Duration ttl = Duration.ofMinutes(30);
        when(redisTemplate.expire("vector:test-key", ttl)).thenReturn(true);

        // Act
        boolean result = vectorCache.expire(key, ttl);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("设置过期时间 - null键")
    void testExpire_NullKey() {
        // Act
        boolean result = vectorCache.expire(null, Duration.ofMinutes(30));

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("设置过期时间 - null TTL")
    void testExpire_NullTtl() {
        // Act
        boolean result = vectorCache.expire("test-key", null);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("获取剩余过期时间")
    void testGetTtl() {
        // Arrange
        String key = "test-key";
        when(redisTemplate.getExpire("vector:test-key", TimeUnit.SECONDS)).thenReturn(1800L);

        // Act
        Optional<Duration> ttl = vectorCache.getTtl(key);

        // Assert
        assertTrue(ttl.isPresent());
        assertEquals(Duration.ofSeconds(1800), ttl.get());
    }

    @Test
    @DisplayName("获取剩余过期时间 - 无过期时间")
    void testGetTtl_NoExpiration() {
        // Arrange
        String key = "test-key";
        when(redisTemplate.getExpire("vector:test-key", TimeUnit.SECONDS)).thenReturn(-1L);

        // Act
        Optional<Duration> ttl = vectorCache.getTtl(key);

        // Assert
        assertFalse(ttl.isPresent());
    }

    @Test
    @DisplayName("获取剩余过期时间 - null键")
    void testGetTtl_NullKey() {
        // Act
        Optional<Duration> ttl = vectorCache.getTtl(null);

        // Assert
        assertFalse(ttl.isPresent());
    }

    @Test
    @DisplayName("清除统计数据")
    void testClearStats() {
        // Act
        vectorCache.clearStats();

        // Assert
        verify(redisTemplate).delete("vector:stats:hits");
        verify(redisTemplate).delete("vector:stats:misses");
        verify(redisTemplate).delete("vector:stats:requests");
    }

    @Test
    @DisplayName("获取键数量")
    void testGetKeyCount() {
        // Arrange
        String pattern = "test*";
        Set<String> keys = Set.of("vector:test1", "vector:test2", "vector:test3");
        when(redisTemplate.keys("vector:test*")).thenReturn(keys);

        // Act
        long count = vectorCache.getKeyCount(pattern);

        // Assert
        assertEquals(3, count);
    }

    @Test
    @DisplayName("获取键数量 - 无匹配键")
    void testGetKeyCount_NoMatches() {
        // Arrange
        String pattern = "nonexistent*";
        when(redisTemplate.keys("vector:nonexistent*")).thenReturn(Set.of());

        // Act
        long count = vectorCache.getKeyCount(pattern);

        // Assert
        assertEquals(0, count);
    }

    @Test
    @DisplayName("JSON序列化异常处理")
    void testJsonSerializationError() {
        // Arrange
        ObjectMapper faultyMapper = mock(ObjectMapper.class);
        RedisVectorCache faultyCacheRedisVectorCache = new RedisVectorCache(redisTemplate, faultyMapper, Duration.ofHours(1));

        try {
            when(faultyMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization error"));
        } catch (Exception e) {
            // Expected for mock setup
        }

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            faultyCacheRedisVectorCache.put("test-key", testVector, Duration.ofMinutes(30));
        });
    }

    @Test
    @DisplayName("Redis连接异常处理")
    void testRedisConnectionError() {
        // Arrange
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection error"));

        // Act & Assert
        // 缓存操作应该优雅地处理Redis连接错误
        assertDoesNotThrow(() -> {
            // 这些操作可能会抛出异常，但不应该导致应用崩溃
            vectorCache.put("test-key", testVector);
        });
    }

    @Test
    @DisplayName("Lua脚本执行")
    void testLuaScriptExecution() {
        // Arrange
        String key = "test-key";
        String vectorJson = "{\"data\":[1.0,2.0,3.0],\"modelName\":\"test-model\"}";

        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any()))
            .thenReturn(vectorJson);

        // Act
        Optional<Vector> result = vectorCache.get(key);

        // Assert
        assertTrue(result.isPresent());
        verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any());
    }
}