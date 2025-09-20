package com.deepsearch.vector.util;

import com.deepsearch.vector.model.Vector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 向量工具类测试
 */
class VectorUtilsTest {

    @Test
    @DisplayName("欧几里得距离计算")
    void testEuclideanDistance() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        Vector v2 = new Vector(Arrays.asList(4.0, 5.0, 6.0), "test");

        // Act
        double distance = VectorUtils.euclideanDistance(v1, v2);

        // Assert
        // 距离应该是 sqrt((4-1)^2 + (5-2)^2 + (6-3)^2) = sqrt(9+9+9) = sqrt(27)
        assertEquals(Math.sqrt(27), distance, 0.0001);
    }

    @Test
    @DisplayName("欧几里得距离 - 不兼容向量")
    void testEuclideanDistance_IncompatibleVectors() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0), "test");
        Vector v2 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.euclideanDistance(v1, v2);
        });
    }

    @Test
    @DisplayName("曼哈顿距离计算")
    void testManhattanDistance() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        Vector v2 = new Vector(Arrays.asList(4.0, 5.0, 6.0), "test");

        // Act
        double distance = VectorUtils.manhattanDistance(v1, v2);

        // Assert
        // 距离应该是 |4-1| + |5-2| + |6-3| = 3 + 3 + 3 = 9
        assertEquals(9.0, distance, 0.0001);
    }

    @Test
    @DisplayName("向量归一化")
    void testNormalize() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(3.0, 4.0), "test");

        // Act
        Vector normalized = VectorUtils.normalize(vector);

        // Assert
        assertEquals(1.0, normalized.getMagnitude(), 0.0001);
        assertEquals(0.6, normalized.getData().get(0), 0.0001); // 3/5
        assertEquals(0.8, normalized.getData().get(1), 0.0001); // 4/5
    }

    @Test
    @DisplayName("向量归一化 - 零向量异常")
    void testNormalize_ZeroVector() {
        // Arrange
        Vector zeroVector = new Vector(Arrays.asList(0.0, 0.0, 0.0), "test");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.normalize(zeroVector);
        });
    }

    @Test
    @DisplayName("向量加法")
    void testAdd() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        Vector v2 = new Vector(Arrays.asList(4.0, 5.0, 6.0), "test");

        // Act
        Vector result = VectorUtils.add(v1, v2);

        // Assert
        List<Double> expected = Arrays.asList(5.0, 7.0, 9.0);
        assertEquals(expected, result.getData());
        assertEquals("test", result.getModelName());
    }

    @Test
    @DisplayName("向量减法")
    void testSubtract() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(4.0, 5.0, 6.0), "test");
        Vector v2 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");

        // Act
        Vector result = VectorUtils.subtract(v1, v2);

        // Assert
        List<Double> expected = Arrays.asList(3.0, 3.0, 3.0);
        assertEquals(expected, result.getData());
    }

    @Test
    @DisplayName("标量乘法")
    void testMultiply() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        double scalar = 2.5;

        // Act
        Vector result = VectorUtils.multiply(vector, scalar);

        // Assert
        List<Double> expected = Arrays.asList(2.5, 5.0, 7.5);
        assertEquals(expected, result.getData());
    }

    @Test
    @DisplayName("点积计算")
    void testDotProduct() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        Vector v2 = new Vector(Arrays.asList(4.0, 5.0, 6.0), "test");

        // Act
        double dotProduct = VectorUtils.dotProduct(v1, v2);

        // Assert
        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0, dotProduct, 0.0001);
    }

    @Test
    @DisplayName("向量压缩")
    void testCompress() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), "test");
        int targetDimension = 3;

        // Act
        Vector compressed = VectorUtils.compress(vector, targetDimension);

        // Assert
        assertEquals(targetDimension, compressed.getDimension());
        assertTrue(compressed.getModelName().contains("compressed"));
        assertEquals(3, compressed.getData().size());
    }

    @Test
    @DisplayName("向量压缩 - 目标维度更大")
    void testCompress_LargerTargetDimension() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        int targetDimension = 5;

        // Act
        Vector result = VectorUtils.compress(vector, targetDimension);

        // Assert
        // 目标维度更大时，应该返回原向量
        assertEquals(vector.getDimension(), result.getDimension());
        assertEquals(vector.getData(), result.getData());
    }

    @Test
    @DisplayName("向量压缩 - 无效目标维度")
    void testCompress_InvalidTargetDimension() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.compress(vector, 0);
        });
    }

    @Test
    @DisplayName("向量量化")
    void testQuantize() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.123456789, 2.987654321, 3.555555555), "test");
        int precision = 3;

        // Act
        Vector quantized = VectorUtils.quantize(vector, precision);

        // Assert
        assertEquals(1.123, quantized.getData().get(0), 0.0001);
        assertEquals(2.988, quantized.getData().get(1), 0.0001);
        assertEquals(3.556, quantized.getData().get(2), 0.0001);
    }

    @Test
    @DisplayName("向量量化 - 无效精度")
    void testQuantize_InvalidPrecision() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.quantize(vector, 0);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.quantize(vector, 16);
        });
    }

    @Test
    @DisplayName("计算质心向量")
    void testCalculateCentroid() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 2.0, 3.0), "test"),
            new Vector(Arrays.asList(4.0, 5.0, 6.0), "test"),
            new Vector(Arrays.asList(7.0, 8.0, 9.0), "test")
        );

        // Act
        Vector centroid = VectorUtils.calculateCentroid(vectors);

        // Assert
        List<Double> expected = Arrays.asList(4.0, 5.0, 6.0); // 平均值
        assertEquals(expected, centroid.getData());
        assertTrue(centroid.getModelName().contains("centroid"));
    }

    @Test
    @DisplayName("计算质心 - 空列表")
    void testCalculateCentroid_EmptyList() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.calculateCentroid(List.of());
        });
    }

    @Test
    @DisplayName("计算质心 - 不兼容向量")
    void testCalculateCentroid_IncompatibleVectors() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 2.0), "test"),
            new Vector(Arrays.asList(3.0, 4.0, 5.0), "test") // 不同维度
        );

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            VectorUtils.calculateCentroid(vectors);
        });
    }

    @Test
    @DisplayName("计算与质心的平均距离")
    void testCalculateAverageDistanceFromCentroid() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(0.0, 0.0), "test"),
            new Vector(Arrays.asList(1.0, 0.0), "test"),
            new Vector(Arrays.asList(0.0, 1.0), "test")
        );

        // Act
        double avgDistance = VectorUtils.calculateAverageDistanceFromCentroid(vectors);

        // Assert
        assertTrue(avgDistance > 0.0);
        assertTrue(avgDistance < 2.0); // 合理的距离范围
    }

    @Test
    @DisplayName("K-最近邻搜索")
    void testFindKNearestNeighbors() {
        // Arrange
        Vector queryVector = new Vector(Arrays.asList(1.0, 1.0), "test");
        List<Vector> candidates = Arrays.asList(
            new Vector(Arrays.asList(1.0, 0.0), "test"), // 相似度高
            new Vector(Arrays.asList(0.0, 1.0), "test"), // 相似度高
            new Vector(Arrays.asList(-1.0, -1.0), "test"), // 相似度低
            new Vector(Arrays.asList(2.0, 2.0), "test") // 相似度最高
        );
        int k = 2;

        // Act
        List<VectorUtils.VectorSimilarity> result = VectorUtils.findKNearestNeighbors(queryVector, candidates, k);

        // Assert
        assertEquals(k, result.size());
        // 结果应该按相似度降序排列
        assertTrue(result.get(0).similarity() >= result.get(1).similarity());
    }

    @Test
    @DisplayName("批量相似度计算")
    void testCalculateBatchSimilarity() {
        // Arrange
        Vector queryVector = new Vector(Arrays.asList(1.0, 0.0), "test");
        List<Vector> candidates = Arrays.asList(
            new Vector(Arrays.asList(1.0, 0.0), "test"), // 相似度=1.0
            new Vector(Arrays.asList(0.0, 1.0), "test"), // 相似度=0.0
            new Vector(Arrays.asList(-1.0, 0.0), "test") // 相似度=-1.0
        );

        // Act
        Map<Vector, Double> similarities = VectorUtils.calculateBatchSimilarity(queryVector, candidates);

        // Assert
        assertEquals(3, similarities.size());
        assertEquals(1.0, similarities.get(candidates.get(0)), 0.0001);
        assertEquals(0.0, similarities.get(candidates.get(1)), 0.0001);
        assertEquals(-1.0, similarities.get(candidates.get(2)), 0.0001);
    }

    @Test
    @DisplayName("简单聚类")
    void testSimpleCluster() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 1.0), "test"),
            new Vector(Arrays.asList(1.1, 0.9), "test"), // 接近第一个
            new Vector(Arrays.asList(10.0, 10.0), "test"),
            new Vector(Arrays.asList(10.1, 9.9), "test") // 接近第三个
        );
        int clusterCount = 2;

        // Act
        List<VectorUtils.VectorCluster> clusters = VectorUtils.simpleCluster(vectors, clusterCount);

        // Assert
        assertTrue(clusters.size() <= clusterCount);
        assertTrue(clusters.size() > 0);

        // 每个簇都应该有质心
        for (VectorUtils.VectorCluster cluster : clusters) {
            assertNotNull(cluster.centroid());
            assertFalse(cluster.vectors().isEmpty());
        }
    }

    @Test
    @DisplayName("简单聚类 - 簇数大于向量数")
    void testSimpleCluster_MoreClustersThanVectors() {
        // Arrange
        List<Vector> vectors = Arrays.asList(
            new Vector(Arrays.asList(1.0, 1.0), "test"),
            new Vector(Arrays.asList(2.0, 2.0), "test")
        );
        int clusterCount = 5;

        // Act
        List<VectorUtils.VectorCluster> clusters = VectorUtils.simpleCluster(vectors, clusterCount);

        // Assert
        assertEquals(2, clusters.size()); // 应该等于向量数
    }

    @Test
    @DisplayName("存储大小估算")
    void testEstimateStorageSize() {
        // Arrange
        Vector vector = new Vector(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0), "test-model");

        // Act
        long storageSize = VectorUtils.estimateStorageSize(vector);

        // Assert
        assertTrue(storageSize > 0);
        // 5个double(40字节) + 模型名称 + 对象开销，应该大于40字节
        assertTrue(storageSize > 40);
    }

    @Test
    @DisplayName("存储大小估算 - null向量")
    void testEstimateStorageSize_NullVector() {
        // Act
        long storageSize = VectorUtils.estimateStorageSize(null);

        // Assert
        assertEquals(0, storageSize);
    }

    @Test
    @DisplayName("零向量的特殊情况")
    void testZeroVectorSpecialCases() {
        // Arrange
        Vector zeroVector = new Vector(Arrays.asList(0.0, 0.0, 0.0), "test");
        Vector normalVector = new Vector(Arrays.asList(1.0, 1.0, 1.0), "test");

        // Act & Assert
        assertEquals(0.0, zeroVector.getMagnitude(), 0.0001);
        assertEquals(0.0, VectorUtils.euclideanDistance(zeroVector, zeroVector), 0.0001);
        assertEquals(0.0, VectorUtils.manhattanDistance(zeroVector, zeroVector), 0.0001);
        assertEquals(0.0, VectorUtils.dotProduct(zeroVector, normalVector), 0.0001);
    }

    @Test
    @DisplayName("向量运算的可交换性")
    void testVectorOperationCommutativity() {
        // Arrange
        Vector v1 = new Vector(Arrays.asList(1.0, 2.0, 3.0), "test");
        Vector v2 = new Vector(Arrays.asList(4.0, 5.0, 6.0), "test");

        // Act & Assert
        // 加法可交换
        assertEquals(VectorUtils.add(v1, v2).getData(), VectorUtils.add(v2, v1).getData());

        // 点积可交换
        assertEquals(VectorUtils.dotProduct(v1, v2), VectorUtils.dotProduct(v2, v1), 0.0001);

        // 欧几里得距离可交换
        assertEquals(VectorUtils.euclideanDistance(v1, v2), VectorUtils.euclideanDistance(v2, v1), 0.0001);

        // 曼哈顿距离可交换
        assertEquals(VectorUtils.manhattanDistance(v1, v2), VectorUtils.manhattanDistance(v2, v1), 0.0001);
    }
}