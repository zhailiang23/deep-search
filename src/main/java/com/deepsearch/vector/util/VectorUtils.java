package com.deepsearch.vector.util;

import com.deepsearch.vector.model.Vector;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 向量工具类
 * 提供向量操作、相似度计算、压缩和性能优化功能
 */
@Slf4j
public class VectorUtils {

    private static final double EPSILON = 1e-10;

    /**
     * 计算两个向量的欧几里得距离
     */
    public static double euclideanDistance(Vector v1, Vector v2) {
        if (!v1.isCompatibleWith(v2)) {
            throw new IllegalArgumentException("向量不兼容: 维度或模型不匹配");
        }

        List<Double> data1 = v1.getData();
        List<Double> data2 = v2.getData();

        double sum = 0.0;
        for (int i = 0; i < data1.size(); i++) {
            double diff = data1.get(i) - data2.get(i);
            sum += diff * diff;
        }

        return Math.sqrt(sum);
    }

    /**
     * 计算曼哈顿距离
     */
    public static double manhattanDistance(Vector v1, Vector v2) {
        if (!v1.isCompatibleWith(v2)) {
            throw new IllegalArgumentException("向量不兼容: 维度或模型不匹配");
        }

        List<Double> data1 = v1.getData();
        List<Double> data2 = v2.getData();

        double sum = 0.0;
        for (int i = 0; i < data1.size(); i++) {
            sum += Math.abs(data1.get(i) - data2.get(i));
        }

        return sum;
    }

    /**
     * 向量归一化（单位向量）
     */
    public static Vector normalize(Vector vector) {
        double magnitude = vector.getMagnitude();
        if (magnitude < EPSILON) {
            throw new IllegalArgumentException("无法归一化零向量");
        }

        List<Double> normalizedData = vector.getData().stream()
            .map(value -> value / magnitude)
            .collect(Collectors.toList());

        return new Vector(normalizedData, vector.getModelName());
    }

    /**
     * 向量加法
     */
    public static Vector add(Vector v1, Vector v2) {
        if (!v1.isCompatibleWith(v2)) {
            throw new IllegalArgumentException("向量不兼容: 维度或模型不匹配");
        }

        List<Double> data1 = v1.getData();
        List<Double> data2 = v2.getData();

        List<Double> result = IntStream.range(0, data1.size())
            .mapToObj(i -> data1.get(i) + data2.get(i))
            .collect(Collectors.toList());

        return new Vector(result, v1.getModelName());
    }

    /**
     * 向量减法
     */
    public static Vector subtract(Vector v1, Vector v2) {
        if (!v1.isCompatibleWith(v2)) {
            throw new IllegalArgumentException("向量不兼容: 维度或模型不匹配");
        }

        List<Double> data1 = v1.getData();
        List<Double> data2 = v2.getData();

        List<Double> result = IntStream.range(0, data1.size())
            .mapToObj(i -> data1.get(i) - data2.get(i))
            .collect(Collectors.toList());

        return new Vector(result, v1.getModelName());
    }

    /**
     * 标量乘法
     */
    public static Vector multiply(Vector vector, double scalar) {
        List<Double> result = vector.getData().stream()
            .map(value -> value * scalar)
            .collect(Collectors.toList());

        return new Vector(result, vector.getModelName());
    }

    /**
     * 计算向量的点积
     */
    public static double dotProduct(Vector v1, Vector v2) {
        if (!v1.isCompatibleWith(v2)) {
            throw new IllegalArgumentException("向量不兼容: 维度或模型不匹配");
        }

        List<Double> data1 = v1.getData();
        List<Double> data2 = v2.getData();

        return IntStream.range(0, data1.size())
            .mapToDouble(i -> data1.get(i) * data2.get(i))
            .sum();
    }

    /**
     * 向量压缩 - 降维到指定维度
     */
    public static Vector compress(Vector vector, int targetDimension) {
        if (targetDimension <= 0) {
            throw new IllegalArgumentException("目标维度必须大于0");
        }

        List<Double> originalData = vector.getData();
        int originalDimension = originalData.size();

        if (targetDimension >= originalDimension) {
            return vector; // 不需要压缩
        }

        // 简单的降维策略：等间隔采样
        List<Double> compressedData = new ArrayList<>(targetDimension);
        double step = (double) originalDimension / targetDimension;

        for (int i = 0; i < targetDimension; i++) {
            int index = (int) Math.round(i * step);
            if (index >= originalDimension) {
                index = originalDimension - 1;
            }
            compressedData.add(originalData.get(index));
        }

        log.debug("向量压缩: {}维 -> {}维", originalDimension, targetDimension);
        return new Vector(compressedData, vector.getModelName() + "_compressed");
    }

    /**
     * 向量量化 - 减少精度以节省存储空间
     */
    public static Vector quantize(Vector vector, int precision) {
        if (precision <= 0 || precision > 15) {
            throw new IllegalArgumentException("精度必须在1-15之间");
        }

        double factor = Math.pow(10, precision);

        List<Double> quantizedData = vector.getData().stream()
            .map(value -> Math.round(value * factor) / factor)
            .collect(Collectors.toList());

        return new Vector(quantizedData, vector.getModelName());
    }

    /**
     * 计算向量集合的中心向量（质心）
     */
    public static Vector calculateCentroid(List<Vector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            throw new IllegalArgumentException("向量列表不能为空");
        }

        Vector first = vectors.get(0);
        int dimension = first.getDimension();
        String modelName = first.getModelName();

        // 检查所有向量兼容性
        for (Vector vector : vectors) {
            if (!first.isCompatibleWith(vector)) {
                throw new IllegalArgumentException("向量集合包含不兼容的向量");
            }
        }

        // 计算平均值
        List<Double> centroidData = new ArrayList<>(dimension);
        for (int d = 0; d < dimension; d++) {
            final int dimension_index = d;
            double average = vectors.stream()
                .mapToDouble(v -> v.getData().get(dimension_index))
                .average()
                .orElse(0.0);
            centroidData.add(average);
        }

        log.debug("计算质心向量: 基于{}个{}维向量", vectors.size(), dimension);
        return new Vector(centroidData, modelName + "_centroid");
    }

    /**
     * 计算向量与质心的平均距离
     */
    public static double calculateAverageDistanceFromCentroid(List<Vector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return 0.0;
        }

        Vector centroid = calculateCentroid(vectors);

        return vectors.stream()
            .mapToDouble(v -> euclideanDistance(v, centroid))
            .average()
            .orElse(0.0);
    }

    /**
     * K-最近邻搜索
     */
    public static List<VectorSimilarity> findKNearestNeighbors(Vector queryVector,
                                                             List<Vector> candidates,
                                                             int k) {
        if (queryVector == null || candidates == null || k <= 0) {
            return List.of();
        }

        // 过滤兼容的向量并计算相似度
        List<VectorSimilarity> similarities = candidates.stream()
            .filter(v -> queryVector.isCompatibleWith(v))
            .map(v -> new VectorSimilarity(v, queryVector.cosineSimilarity(v)))
            .sorted((a, b) -> Double.compare(b.similarity(), a.similarity())) // 降序排列
            .limit(k)
            .collect(Collectors.toList());

        log.debug("K-最近邻搜索: 查询向量维度={}, 候选向量数={}, K={}, 结果数={}",
            queryVector.getDimension(), candidates.size(), k, similarities.size());

        return similarities;
    }

    /**
     * 批量计算相似度
     */
    public static Map<Vector, Double> calculateBatchSimilarity(Vector queryVector,
                                                              List<Vector> candidates) {
        if (queryVector == null || candidates == null) {
            return Map.of();
        }

        return candidates.parallelStream()
            .filter(v -> queryVector.isCompatibleWith(v))
            .collect(Collectors.toMap(
                v -> v,
                v -> queryVector.cosineSimilarity(v),
                (existing, replacement) -> existing, // 保留现有值
                LinkedHashMap::new // 保持顺序
            ));
    }

    /**
     * 向量聚类分析（简单K-means思想）
     */
    public static List<VectorCluster> simpleCluster(List<Vector> vectors, int clusterCount) {
        if (vectors == null || vectors.isEmpty() || clusterCount <= 0) {
            return List.of();
        }

        if (clusterCount >= vectors.size()) {
            // 每个向量一个簇
            return IntStream.range(0, vectors.size())
                .mapToObj(i -> new VectorCluster(i, List.of(vectors.get(i)), vectors.get(i)))
                .collect(Collectors.toList());
        }

        // 随机选择初始质心
        Random random = new Random();
        List<Vector> centroids = random.ints(0, vectors.size())
            .distinct()
            .limit(clusterCount)
            .mapToObj(vectors::get)
            .collect(Collectors.toList());

        // 简单的聚类分配（单次迭代）
        Map<Integer, List<Vector>> clusters = new HashMap<>();
        for (int i = 0; i < clusterCount; i++) {
            clusters.put(i, new ArrayList<>());
        }

        for (Vector vector : vectors) {
            int bestCluster = 0;
            double bestSimilarity = -1.0;

            for (int i = 0; i < centroids.size(); i++) {
                Vector centroid = centroids.get(i);
                if (vector.isCompatibleWith(centroid)) {
                    double similarity = vector.cosineSimilarity(centroid);
                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity;
                        bestCluster = i;
                    }
                }
            }

            clusters.get(bestCluster).add(vector);
        }

        // 生成聚类结果
        List<VectorCluster> result = new ArrayList<>();
        for (int i = 0; i < clusterCount; i++) {
            List<Vector> clusterVectors = clusters.get(i);
            if (!clusterVectors.isEmpty()) {
                Vector newCentroid = calculateCentroid(clusterVectors);
                result.add(new VectorCluster(i, clusterVectors, newCentroid));
            }
        }

        log.debug("简单聚类完成: {}个向量分为{}簇", vectors.size(), result.size());
        return result;
    }

    /**
     * 计算向量数据的存储大小估算（字节）
     */
    public static long estimateStorageSize(Vector vector) {
        if (vector == null) {
            return 0;
        }

        // 每个double 8字节 + 对象开销
        long dataSize = vector.getDimension() * 8L;
        long metadataSize = vector.getModelName().length() * 2L; // Unicode字符
        long objectOverhead = 64L; // 估算的对象开销

        return dataSize + metadataSize + objectOverhead;
    }

    /**
     * 向量相似度结果
     */
    public record VectorSimilarity(Vector vector, double similarity) {}

    /**
     * 向量聚类结果
     */
    public record VectorCluster(int clusterId, List<Vector> vectors, Vector centroid) {}
}