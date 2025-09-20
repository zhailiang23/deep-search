package com.deepsearch.vector.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向量模型测试类
 */
class VectorTest {

    @Test
    @DisplayName("创建向量 - 正常情况")
    void testCreateVector_Success() {
        // Arrange
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0, 4.0);
        String modelName = "test-model";

        // Act
        Vector vector = new Vector(data, modelName);

        // Assert
        assertEquals(4, vector.getDimension());
        assertEquals(modelName, vector.getModelName());
        assertEquals(data, vector.getData());
        assertEquals(Math.sqrt(30.0), vector.getMagnitude(), 0.0001);
    }

    @Test
    @DisplayName("创建向量 - 空数据异常")
    void testCreateVector_EmptyData() {
        // Arrange
        List<Double> emptyData = List.of();
        String modelName = "test-model";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new Vector(emptyData, modelName);
        });
    }

    @Test
    @DisplayName("创建向量 - 空模型名异常")
    void testCreateVector_EmptyModelName() {
        // Arrange
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0);
        String emptyModelName = "";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new Vector(data, emptyModelName);
        });
    }

    @Test
    @DisplayName("创建向量 - null数据异常")
    void testCreateVector_NullData() {
        // Arrange
        List<Double> nullData = null;
        String modelName = "test-model";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            new Vector(nullData, modelName);
        });
    }

    @Test
    @DisplayName("向量兼容性检查 - 兼容向量")
    void testIsCompatibleWith_Compatible() {
        // Arrange
        List<Double> data1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> data2 = Arrays.asList(4.0, 5.0, 6.0);
        String modelName = "same-model";

        Vector vector1 = new Vector(data1, modelName);
        Vector vector2 = new Vector(data2, modelName);

        // Act & Assert
        assertTrue(vector1.isCompatibleWith(vector2));
        assertTrue(vector2.isCompatibleWith(vector1));
    }

    @Test
    @DisplayName("向量兼容性检查 - 不同维度")
    void testIsCompatibleWith_DifferentDimensions() {
        // Arrange
        List<Double> data1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> data2 = Arrays.asList(4.0, 5.0);
        String modelName = "same-model";

        Vector vector1 = new Vector(data1, modelName);
        Vector vector2 = new Vector(data2, modelName);

        // Act & Assert
        assertFalse(vector1.isCompatibleWith(vector2));
    }

    @Test
    @DisplayName("向量兼容性检查 - 不同模型")
    void testIsCompatibleWith_DifferentModels() {
        // Arrange
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0);
        Vector vector1 = new Vector(data, "model-1");
        Vector vector2 = new Vector(data, "model-2");

        // Act & Assert
        assertFalse(vector1.isCompatibleWith(vector2));
    }

    @Test
    @DisplayName("向量兼容性检查 - null向量")
    void testIsCompatibleWith_NullVector() {
        // Arrange
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0);
        Vector vector = new Vector(data, "test-model");

        // Act & Assert
        assertFalse(vector.isCompatibleWith(null));
    }

    @Test
    @DisplayName("余弦相似度计算 - 正常情况")
    void testCosineSimilarity_Normal() {
        // Arrange
        List<Double> data1 = Arrays.asList(1.0, 0.0, 0.0);
        List<Double> data2 = Arrays.asList(0.0, 1.0, 0.0);
        String modelName = "test-model";

        Vector vector1 = new Vector(data1, modelName);
        Vector vector2 = new Vector(data2, modelName);

        // Act
        double similarity = vector1.cosineSimilarity(vector2);

        // Assert
        assertEquals(0.0, similarity, 0.0001); // 垂直向量相似度为0
    }

    @Test
    @DisplayName("余弦相似度计算 - 相同向量")
    void testCosineSimilarity_IdenticalVectors() {
        // Arrange
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0);
        String modelName = "test-model";

        Vector vector1 = new Vector(data, modelName);
        Vector vector2 = new Vector(data, modelName);

        // Act
        double similarity = vector1.cosineSimilarity(vector2);

        // Assert
        assertEquals(1.0, similarity, 0.0001); // 相同向量相似度为1
    }

    @Test
    @DisplayName("余弦相似度计算 - 相反向量")
    void testCosineSimilarity_OppositeVectors() {
        // Arrange
        List<Double> data1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> data2 = Arrays.asList(-1.0, -2.0, -3.0);
        String modelName = "test-model";

        Vector vector1 = new Vector(data1, modelName);
        Vector vector2 = new Vector(data2, modelName);

        // Act
        double similarity = vector1.cosineSimilarity(vector2);

        // Assert
        assertEquals(-1.0, similarity, 0.0001); // 相反向量相似度为-1
    }

    @Test
    @DisplayName("余弦相似度计算 - 不兼容向量异常")
    void testCosineSimilarity_IncompatibleVectors() {
        // Arrange
        List<Double> data1 = Arrays.asList(1.0, 2.0, 3.0);
        List<Double> data2 = Arrays.asList(4.0, 5.0);
        String modelName = "test-model";

        Vector vector1 = new Vector(data1, modelName);
        Vector vector2 = new Vector(data2, modelName);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            vector1.cosineSimilarity(vector2);
        });
    }

    @Test
    @DisplayName("生成缓存键")
    void testGetCacheKey() {
        // Arrange
        List<Double> data = Arrays.asList(1.0, 2.0, 3.0);
        String modelName = "test-model";
        Vector vector = new Vector(data, modelName);
        String content = "test content";

        // Act
        String cacheKey = vector.getCacheKey(content);

        // Assert
        assertNotNull(cacheKey);
        assertTrue(cacheKey.contains(modelName));
        assertTrue(cacheKey.contains("3")); // 维度
        assertEquals(2, cacheKey.split("_").length - 1); // 应该有2个下划线分隔符
    }

    @Test
    @DisplayName("向量数据不可变性")
    void testVectorDataImmutability() {
        // Arrange
        List<Double> originalData = Arrays.asList(1.0, 2.0, 3.0);
        String modelName = "test-model";
        Vector vector = new Vector(originalData, modelName);

        // Act
        List<Double> retrievedData = vector.getData();

        // Assert
        // 验证返回的列表是不可变的
        assertThrows(UnsupportedOperationException.class, () -> {
            retrievedData.add(4.0);
        });

        // 验证修改原始列表不影响向量
        originalData.set(0, 999.0);
        assertEquals(1.0, vector.getData().get(0), 0.0001);
    }

    @Test
    @DisplayName("向量模长计算准确性")
    void testMagnitudeCalculation() {
        // Arrange & Act
        Vector vector1 = new Vector(Arrays.asList(3.0, 4.0), "test");
        Vector vector2 = new Vector(Arrays.asList(1.0, 1.0, 1.0, 1.0), "test");
        Vector vector3 = new Vector(Arrays.asList(0.0, 0.0, 0.0), "test");

        // Assert
        assertEquals(5.0, vector1.getMagnitude(), 0.0001); // sqrt(9+16) = 5
        assertEquals(2.0, vector2.getMagnitude(), 0.0001); // sqrt(4) = 2
        assertEquals(0.0, vector3.getMagnitude(), 0.0001); // 零向量
    }

    @Test
    @DisplayName("复杂相似度计算")
    void testComplexSimilarityCalculation() {
        // Arrange
        // 两个单位向量，夹角45度
        double sqrt2 = Math.sqrt(2);
        List<Double> data1 = Arrays.asList(1.0, 0.0);
        List<Double> data2 = Arrays.asList(1/sqrt2, 1/sqrt2);

        Vector vector1 = new Vector(data1, "test");
        Vector vector2 = new Vector(data2, "test");

        // Act
        double similarity = vector1.cosineSimilarity(vector2);

        // Assert
        // cos(45°) = 1/√2 ≈ 0.7071
        assertEquals(1/sqrt2, similarity, 0.0001);
    }
}