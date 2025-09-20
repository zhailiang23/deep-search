package com.deepsearch.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.deepsearch.entity.DocumentVector;
import com.deepsearch.repository.DocumentVectorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 向量相似度服务 - 提供高性能的向量相似度计算
 * 支持多种相似度算法、批量计算和异步处理
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSimilarityService {

    private final DocumentVectorRepository documentVectorRepository;

    // 异步执行器，用于并行计算
    private final Executor vectorComputeExecutor = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    @Value("${vector.similarity.algorithm:cosine}")
    private String defaultSimilarityAlgorithm;

    @Value("${vector.similarity.threshold:0.7}")
    private double defaultSimilarityThreshold;

    @Value("${vector.similarity.cache.size:10000}")
    private int cacheSize;

    @Value("${vector.similarity.batch.size:100}")
    private int batchSize;

    /**
     * 计算查询向量与文档集合的相似度
     */
    public List<SimilarityResult> calculateSimilarities(
            List<Float> queryVector,
            List<Long> documentIds,
            SimilarityAlgorithm algorithm,
            double threshold) {

        if (queryVector == null || queryVector.isEmpty() || documentIds == null || documentIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            log.info("开始计算相似度，查询向量维度: {}, 文档数量: {}", queryVector.size(), documentIds.size());

            // 分批处理大量文档
            List<List<Long>> batches = createBatches(documentIds, batchSize);
            List<CompletableFuture<List<SimilarityResult>>> futures = new ArrayList<>();

            // 并行处理每个批次
            for (List<Long> batch : batches) {
                CompletableFuture<List<SimilarityResult>> future = CompletableFuture
                    .supplyAsync(() -> processBatch(queryVector, batch, algorithm, threshold), vectorComputeExecutor);
                futures.add(future);
            }

            // 等待所有批次完成并合并结果
            List<SimilarityResult> allResults = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

            // 按相似度排序
            allResults.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));

            log.info("相似度计算完成，返回{}条结果", allResults.size());
            return allResults;

        } catch (Exception e) {
            log.error("相似度计算失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 计算两个向量的相似度
     */
    @Cacheable(value = "vectorSimilarity", key = "#vector1.hashCode() + '_' + #vector2.hashCode() + '_' + #algorithm.name()")
    public double calculateSimilarity(List<Float> vector1, List<Float> vector2, SimilarityAlgorithm algorithm) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }

        switch (algorithm) {
            case COSINE:
                return calculateCosineSimilarity(vector1, vector2);
            case EUCLIDEAN:
                return calculateEuclideanSimilarity(vector1, vector2);
            case DOT_PRODUCT:
                return calculateDotProductSimilarity(vector1, vector2);
            case MANHATTAN:
                return calculateManhattanSimilarity(vector1, vector2);
            case JACCARD:
                return calculateJaccardSimilarity(vector1, vector2);
            default:
                return calculateCosineSimilarity(vector1, vector2);
        }
    }

    /**
     * 批量向量相似度搜索
     */
    public List<SimilarityResult> batchSimilaritySearch(
            List<Float> queryVector,
            int topK,
            SimilarityAlgorithm algorithm,
            double threshold) {

        try {
            // 获取所有文档向量
            List<DocumentVector> allDocVectors = documentVectorRepository.findAll();

            if (allDocVectors.isEmpty()) {
                log.warn("没有找到任何文档向量");
                return Collections.emptyList();
            }

            // 并行计算相似度
            List<SimilarityResult> results = allDocVectors.parallelStream()
                .map(docVector -> {
                    try {
                        List<Float> docVectorData = convertDoubleListToFloatList(docVector.getVectorData());
                        double similarity = calculateSimilarity(queryVector, docVectorData, algorithm);

                        if (similarity >= threshold) {
                            return new SimilarityResult(docVector.getDocumentId(), similarity, algorithm);
                        }
                        return null;
                    } catch (Exception e) {
                        log.warn("计算文档向量相似度失败: documentId={}", docVector.getDocumentId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                .limit(topK)
                .collect(Collectors.toList());

            log.info("批量相似度搜索完成，返回{}条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("批量相似度搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 近似最近邻搜索 (ANN)
     */
    public List<SimilarityResult> approximateNearestNeighborSearch(
            List<Float> queryVector,
            int topK,
            double approximationFactor) {

        try {
            // 使用局部敏感哈希 (LSH) 进行快速近似搜索
            List<Long> candidateDocuments = findCandidatesWithLSH(queryVector, topK * 3);

            // 对候选文档进行精确相似度计算
            return calculateSimilarities(
                queryVector,
                candidateDocuments,
                SimilarityAlgorithm.COSINE,
                0.0
            ).stream()
                .limit(topK)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("ANN搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 向量聚类相似度分析
     */
    public Map<String, List<SimilarityResult>> clusterSimilarityAnalysis(
            List<Float> queryVector,
            int clustersCount,
            double threshold) {

        try {
            // 获取相似文档
            List<SimilarityResult> similarDocs = batchSimilaritySearch(
                queryVector, 1000, SimilarityAlgorithm.COSINE, threshold);

            if (similarDocs.size() < clustersCount) {
                Map<String, List<SimilarityResult>> result = new HashMap<>();
                result.put("cluster_0", similarDocs);
                return result;
            }

            // 简化的K-means聚类实现
            return performKMeansClustering(similarDocs, clustersCount);

        } catch (Exception e) {
            log.error("聚类相似度分析失败", e);
            return Collections.emptyMap();
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 处理单个批次
     */
    private List<SimilarityResult> processBatch(
            List<Float> queryVector,
            List<Long> documentIds,
            SimilarityAlgorithm algorithm,
            double threshold) {

        List<SimilarityResult> batchResults = new ArrayList<>();

        for (Long documentId : documentIds) {
            try {
                List<DocumentVector> docVectors = documentVectorRepository.findByDocumentId(documentId);
                if (!docVectors.isEmpty()) {
                    DocumentVector docVector = docVectors.get(0); // 取第一个向量
                    List<Float> docVectorData = convertDoubleListToFloatList(docVector.getVectorData());

                    double similarity = calculateSimilarity(queryVector, docVectorData, algorithm);

                    if (similarity >= threshold) {
                        batchResults.add(new SimilarityResult(documentId, similarity, algorithm));
                    }
                }
            } catch (Exception e) {
                log.warn("处理文档向量失败: documentId={}", documentId, e);
            }
        }

        return batchResults;
    }

    /**
     * 创建批次
     */
    private <T> List<List<T>> createBatches(List<T> items, int batchSize) {
        return IntStream.range(0, (items.size() + batchSize - 1) / batchSize)
            .mapToObj(i -> items.subList(i * batchSize, Math.min((i + 1) * batchSize, items.size())))
            .collect(Collectors.toList());
    }

    /**
     * 余弦相似度计算
     */
    private double calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            float v1 = vector1.get(i);
            float v2 = vector2.get(i);

            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        double magnitude = Math.sqrt(norm1) * Math.sqrt(norm2);
        return magnitude == 0 ? 0.0 : dotProduct / magnitude;
    }

    /**
     * 欧几里得相似度计算 (转换为相似度)
     */
    private double calculateEuclideanSimilarity(List<Float> vector1, List<Float> vector2) {
        double sumSquaredDiffs = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            double diff = vector1.get(i) - vector2.get(i);
            sumSquaredDiffs += diff * diff;
        }

        double distance = Math.sqrt(sumSquaredDiffs);
        // 转换为相似度 (距离越小，相似度越高)
        return 1.0 / (1.0 + distance);
    }

    /**
     * 点积相似度计算
     */
    private double calculateDotProductSimilarity(List<Float> vector1, List<Float> vector2) {
        double dotProduct = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
        }

        return dotProduct;
    }

    /**
     * 曼哈顿距离相似度计算
     */
    private double calculateManhattanSimilarity(List<Float> vector1, List<Float> vector2) {
        double manhattanDistance = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            manhattanDistance += Math.abs(vector1.get(i) - vector2.get(i));
        }

        // 转换为相似度
        return 1.0 / (1.0 + manhattanDistance);
    }

    /**
     * Jaccard相似度计算 (适用于稀疏向量)
     */
    private double calculateJaccardSimilarity(List<Float> vector1, List<Float> vector2) {
        Set<Integer> set1 = new HashSet<>();
        Set<Integer> set2 = new HashSet<>();

        // 找出非零元素的索引
        for (int i = 0; i < vector1.size(); i++) {
            if (vector1.get(i) != 0) set1.add(i);
            if (vector2.get(i) != 0) set2.add(i);
        }

        Set<Integer> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Integer> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 局部敏感哈希候选查找
     */
    private List<Long> findCandidatesWithLSH(List<Float> queryVector, int candidateCount) {
        // 简化的LSH实现
        // 实际应用中应使用更专业的LSH算法
        List<DocumentVector> allVectors = documentVectorRepository.findAll();

        return allVectors.stream()
            .map(DocumentVector::getDocumentId)
            .limit(candidateCount)
            .collect(Collectors.toList());
    }

    /**
     * K-means聚类实现
     */
    private Map<String, List<SimilarityResult>> performKMeansClustering(
            List<SimilarityResult> documents,
            int clustersCount) {

        // 简化的K-means聚类
        Map<String, List<SimilarityResult>> clusters = new HashMap<>();

        // 将文档按相似度分组
        int docsPerCluster = Math.max(1, documents.size() / clustersCount);

        for (int i = 0; i < clustersCount; i++) {
            int startIndex = i * docsPerCluster;
            int endIndex = Math.min(startIndex + docsPerCluster, documents.size());

            if (startIndex < documents.size()) {
                clusters.put("cluster_" + i, documents.subList(startIndex, endIndex));
            }
        }

        return clusters;
    }

    /**
     * 将List<Double>转换为List<Float>
     */
    private List<Float> convertDoubleListToFloatList(List<Double> doubleList) {
        if (doubleList == null || doubleList.isEmpty()) {
            return Collections.emptyList();
        }
        
        return doubleList.stream()
            .map(Double::floatValue)
            .collect(Collectors.toList());
    }

    // ==================== 内部类和枚举 ====================

    /**
     * 相似度算法枚举
     */
    public enum SimilarityAlgorithm {
        COSINE,        // 余弦相似度
        EUCLIDEAN,     // 欧几里得距离
        DOT_PRODUCT,   // 点积
        MANHATTAN,     // 曼哈顿距离
        JACCARD        // Jaccard相似度
    }

    /**
     * 相似度结果
     */
    public static class SimilarityResult {
        private Long documentId;
        private double similarity;
        private SimilarityAlgorithm algorithm;
        private Map<String, Object> metadata;

        public SimilarityResult(Long documentId, double similarity, SimilarityAlgorithm algorithm) {
            this.documentId = documentId;
            this.similarity = similarity;
            this.algorithm = algorithm;
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        public double getSimilarity() { return similarity; }
        public void setSimilarity(double similarity) { this.similarity = similarity; }
        public SimilarityAlgorithm getAlgorithm() { return algorithm; }
        public void setAlgorithm(SimilarityAlgorithm algorithm) { this.algorithm = algorithm; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        @Override
        public String toString() {
            return String.format("SimilarityResult{documentId=%d, similarity=%.4f, algorithm=%s}",
                documentId, similarity, algorithm);
        }
    }

    /**
     * 向量相似度统计信息
     */
    public static class SimilarityStatistics {
        private double averageSimilarity;
        private double maxSimilarity;
        private double minSimilarity;
        private double standardDeviation;
        private int totalComparisons;

        // Getters and Setters
        public double getAverageSimilarity() { return averageSimilarity; }
        public void setAverageSimilarity(double averageSimilarity) { this.averageSimilarity = averageSimilarity; }
        public double getMaxSimilarity() { return maxSimilarity; }
        public void setMaxSimilarity(double maxSimilarity) { this.maxSimilarity = maxSimilarity; }
        public double getMinSimilarity() { return minSimilarity; }
        public void setMinSimilarity(double minSimilarity) { this.minSimilarity = minSimilarity; }
        public double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(double standardDeviation) { this.standardDeviation = standardDeviation; }
        public int getTotalComparisons() { return totalComparisons; }
        public void setTotalComparisons(int totalComparisons) { this.totalComparisons = totalComparisons; }
    }
}