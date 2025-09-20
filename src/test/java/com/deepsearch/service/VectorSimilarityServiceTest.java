package com.deepsearch.service;

import com.deepsearch.entity.DocumentVector;
import com.deepsearch.repository.DocumentVectorRepository;
import com.deepsearch.service.VectorSimilarityService.SimilarityAlgorithm;
import com.deepsearch.service.VectorSimilarityService.SimilarityResult;
import com.deepsearch.vector.service.VectorProcessingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VectorSimilarityService 单元测试
 * 测试向量相似度服务的各项功能
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class VectorSimilarityServiceTest {

    @Mock
    private VectorProcessingEngine vectorProcessingEngine;

    @Mock
    private DocumentVectorRepository documentVectorRepository;

    @InjectMocks
    private VectorSimilarityService vectorSimilarityService;

    private List<Float> testVector1;
    private List<Float> testVector2;
    private List<Float> testVector3;
    private DocumentVector testDocumentVector1;
    private DocumentVector testDocumentVector2;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(vectorSimilarityService, "defaultSimilarityAlgorithm", "cosine");
        ReflectionTestUtils.setField(vectorSimilarityService, "defaultSimilarityThreshold", 0.7);
        ReflectionTestUtils.setField(vectorSimilarityService, "cacheSize", 10000);
        ReflectionTestUtils.setField(vectorSimilarityService, "batchSize", 100);

        // 创建测试向量
        testVector1 = Arrays.asList(1.0f, 0.0f, 0.0f); // 单位向量
        testVector2 = Arrays.asList(0.6f, 0.8f, 0.0f); // 标准化向量
        testVector3 = Arrays.asList(0.0f, 1.0f, 0.0f); // 垂直向量

        // 创建测试文档向量
        testDocumentVector1 = createTestDocumentVector(1L, Arrays.asList(0.6, 0.8, 0.0));
        testDocumentVector2 = createTestDocumentVector(2L, Arrays.asList(0.0, 1.0, 0.0));
    }

    @Test
    void testCalculateSimilarity_CosineSimilarity_Perfect() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 0.0f, 0.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.COSINE);

        // Then
        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testCalculateSimilarity_CosineSimilarity_Orthogonal() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(0.0f, 1.0f, 0.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.COSINE);

        // Then
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCalculateSimilarity_CosineSimilarity_Partial() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(0.6f, 0.8f, 0.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.COSINE);

        // Then
        assertEquals(0.6, similarity, 0.001);
    }

    @Test
    void testCalculateSimilarity_EuclideanSimilarity() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 0.0f, 0.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.EUCLIDEAN);

        // Then
        assertTrue(similarity > 0.5); // 距离为0，相似度应该很高
    }

    @Test
    void testCalculateSimilarity_DotProductSimilarity() {
        // Given
        List<Float> vector1 = Arrays.asList(2.0f, 3.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 2.0f, 0.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.DOT_PRODUCT);

        // Then
        assertEquals(8.0, similarity, 0.001); // 2*1 + 3*2 + 0*0 = 8
    }

    @Test
    void testCalculateSimilarity_ManhattanSimilarity() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 2.0f, 3.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 2.0f, 3.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.MANHATTAN);

        // Then
        assertEquals(1.0, similarity, 0.001); // 距离为0，相似度为1
    }

    @Test
    void testCalculateSimilarity_JaccardSimilarity() {
        // Given - 稀疏向量
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f, 1.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 0.0f, 0.0f, 1.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.JACCARD);

        // Then
        // 交集: {0} (第一个元素都非零)
        // 并集: {0, 2, 3} (第0,2,3个元素有非零值)
        // Jaccard = 1/3 ≈ 0.333
        assertEquals(0.333, similarity, 0.01);
    }

    @Test
    void testCalculateSimilarity_DifferentSizes() {
        // Given
        List<Float> vector1 = Arrays.asList(1.0f, 0.0f);
        List<Float> vector2 = Arrays.asList(1.0f, 0.0f, 0.0f);

        // When
        double similarity = vectorSimilarityService.calculateSimilarity(
            vector1, vector2, SimilarityAlgorithm.COSINE);

        // Then
        assertEquals(0.0, similarity);
    }

    @Test
    void testCalculateSimilarity_NullVectors() {
        // When & Then
        assertEquals(0.0, vectorSimilarityService.calculateSimilarity(
            null, testVector1, SimilarityAlgorithm.COSINE));
        assertEquals(0.0, vectorSimilarityService.calculateSimilarity(
            testVector1, null, SimilarityAlgorithm.COSINE));
        assertEquals(0.0, vectorSimilarityService.calculateSimilarity(
            null, null, SimilarityAlgorithm.COSINE));
    }

    @Test
    void testBatchSimilaritySearch_Success() {
        // Given
        List<Float> queryVector = testVector1;
        int topK = 5;
        double threshold = 0.5;

        List<DocumentVector> allDocVectors = Arrays.asList(
            testDocumentVector1, testDocumentVector2
        );

        when(documentVectorRepository.findAll()).thenReturn(allDocVectors);

        // When
        List<SimilarityResult> results = vectorSimilarityService.batchSimilaritySearch(
            queryVector, topK, SimilarityAlgorithm.COSINE, threshold);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size()); // 只有testDocumentVector1的相似度(0.6)超过阈值0.5

        SimilarityResult result = results.get(0);
        assertEquals(1L, result.getDocumentId());
        assertEquals(0.6, result.getSimilarity(), 0.001);
        assertEquals(SimilarityAlgorithm.COSINE, result.getAlgorithm());

        verify(documentVectorRepository).findAll();
    }

    @Test
    void testBatchSimilaritySearch_EmptyRepository() {
        // Given
        List<Float> queryVector = testVector1;
        when(documentVectorRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<SimilarityResult> results = vectorSimilarityService.batchSimilaritySearch(
            queryVector, 10, SimilarityAlgorithm.COSINE, 0.5);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testBatchSimilaritySearch_TopKLimiting() {
        // Given
        List<Float> queryVector = testVector1;
        int topK = 1; // 限制只返回1个结果

        List<DocumentVector> allDocVectors = Arrays.asList(
            createTestDocumentVector(1L, Arrays.asList(0.9, 0.436, 0.0)), // 高相似度
            createTestDocumentVector(2L, Arrays.asList(0.8, 0.6, 0.0)),   // 中相似度
            createTestDocumentVector(3L, Arrays.asList(0.7, 0.714, 0.0))  // 低相似度
        );

        when(documentVectorRepository.findAll()).thenReturn(allDocVectors);

        // When
        List<SimilarityResult> results = vectorSimilarityService.batchSimilaritySearch(
            queryVector, topK, SimilarityAlgorithm.COSINE, 0.5);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size()); // 应该只返回1个结果
        assertEquals(1L, results.get(0).getDocumentId()); // 应该是相似度最高的
    }

    @Test
    void testCalculateSimilarities_BatchProcessing() {
        // Given
        List<Float> queryVector = testVector1;
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);

        when(documentVectorRepository.findByDocumentId(1L))
            .thenReturn(Arrays.asList(testDocumentVector1));
        when(documentVectorRepository.findByDocumentId(2L))
            .thenReturn(Arrays.asList(testDocumentVector2));
        when(documentVectorRepository.findByDocumentId(3L))
            .thenReturn(Collections.emptyList()); // 模拟不存在的文档

        // When
        List<SimilarityResult> results = vectorSimilarityService.calculateSimilarities(
            queryVector, documentIds, SimilarityAlgorithm.COSINE, 0.5);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size()); // 只有一个超过阈值0.5

        verify(documentVectorRepository).findByDocumentId(1L);
        verify(documentVectorRepository).findByDocumentId(2L);
        verify(documentVectorRepository).findByDocumentId(3L);
    }

    @Test
    void testCalculateSimilarities_EmptyInput() {
        // When & Then
        assertTrue(vectorSimilarityService.calculateSimilarities(
            null, Arrays.asList(1L), SimilarityAlgorithm.COSINE, 0.5).isEmpty());

        assertTrue(vectorSimilarityService.calculateSimilarities(
            Collections.emptyList(), Arrays.asList(1L), SimilarityAlgorithm.COSINE, 0.5).isEmpty());

        assertTrue(vectorSimilarityService.calculateSimilarities(
            testVector1, null, SimilarityAlgorithm.COSINE, 0.5).isEmpty());

        assertTrue(vectorSimilarityService.calculateSimilarities(
            testVector1, Collections.emptyList(), SimilarityAlgorithm.COSINE, 0.5).isEmpty());
    }

    @Test
    void testApproximateNearestNeighborSearch() {
        // Given
        List<Float> queryVector = testVector1;
        int topK = 5;
        double approximationFactor = 1.1;

        // Mock LSH候选查找
        List<DocumentVector> candidateVectors = Arrays.asList(
            testDocumentVector1, testDocumentVector2
        );
        when(documentVectorRepository.findAll()).thenReturn(candidateVectors);

        when(documentVectorRepository.findByDocumentId(1L))
            .thenReturn(Arrays.asList(testDocumentVector1));
        when(documentVectorRepository.findByDocumentId(2L))
            .thenReturn(Arrays.asList(testDocumentVector2));

        // When
        List<SimilarityResult> results = vectorSimilarityService.approximateNearestNeighborSearch(
            queryVector, topK, approximationFactor);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= topK);
    }

    @Test
    void testClusterSimilarityAnalysis() {
        // Given
        List<Float> queryVector = testVector1;
        int clustersCount = 2;
        double threshold = 0.1; // 低阈值以包含更多文档

        List<DocumentVector> allDocVectors = Arrays.asList(
            createTestDocumentVector(1L, Arrays.asList(0.9, 0.436, 0.0)),
            createTestDocumentVector(2L, Arrays.asList(0.8, 0.6, 0.0)),
            createTestDocumentVector(3L, Arrays.asList(0.7, 0.714, 0.0)),
            createTestDocumentVector(4L, Arrays.asList(0.6, 0.8, 0.0))
        );

        when(documentVectorRepository.findAll()).thenReturn(allDocVectors);

        // When
        Map<String, List<SimilarityResult>> clusters = vectorSimilarityService.clusterSimilarityAnalysis(
            queryVector, clustersCount, threshold);

        // Then
        assertNotNull(clusters);
        assertFalse(clusters.isEmpty());
        assertTrue(clusters.size() <= clustersCount);
    }

    @Test
    void testClusterSimilarityAnalysis_InsufficientDocuments() {
        // Given
        List<Float> queryVector = testVector1;
        int clustersCount = 5; // 更多的聚类数量
        double threshold = 0.1;

        List<DocumentVector> allDocVectors = Arrays.asList(
            testDocumentVector1, testDocumentVector2 // 只有2个文档
        );

        when(documentVectorRepository.findAll()).thenReturn(allDocVectors);

        // When
        Map<String, List<SimilarityResult>> clusters = vectorSimilarityService.clusterSimilarityAnalysis(
            queryVector, clustersCount, threshold);

        // Then
        assertNotNull(clusters);
        assertEquals(1, clusters.size()); // 应该只有一个聚类
        assertTrue(clusters.containsKey("cluster_0"));
    }

    @Test
    void testVectorDeserialization_ValidData() {
        // Given
        DocumentVector docVector = createTestDocumentVector(1L, Arrays.asList(1.0, 2.0, 3.0));

        // When - 通过batchSimilaritySearch间接测试反序列化
        when(documentVectorRepository.findAll()).thenReturn(Arrays.asList(docVector));

        List<SimilarityResult> results = vectorSimilarityService.batchSimilaritySearch(
            Arrays.asList(1.0f, 2.0f, 3.0f), 10, SimilarityAlgorithm.DOT_PRODUCT, 0.0);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(14.0, results.get(0).getSimilarity(), 0.001); // 1*1 + 2*2 + 3*3 = 14
    }

    @Test
    void testVectorDeserialization_InvalidData() {
        // Given - 测试无效数据时的处理，但这次用null代替
        DocumentVector docVector = createTestDocumentVector(1L, null);

        // When
        when(documentVectorRepository.findAll()).thenReturn(Arrays.asList(docVector));

        List<SimilarityResult> results = vectorSimilarityService.batchSimilaritySearch(
            testVector1, 10, SimilarityAlgorithm.COSINE, 0.0);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty()); // 无效数据应该被过滤掉
    }

    @Test
    void testSimilarityResult_ToString() {
        // Given
        SimilarityResult result = new SimilarityResult(1L, 0.95, SimilarityAlgorithm.COSINE);

        // When
        String toString = result.toString();

        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("documentId=1"));
        assertTrue(toString.contains("similarity=0.9500"));
        assertTrue(toString.contains("algorithm=COSINE"));
    }

    @Test
    void testSimilarityResult_Metadata() {
        // Given
        SimilarityResult result = new SimilarityResult(1L, 0.95, SimilarityAlgorithm.COSINE);

        // When
        result.getMetadata().put("test_key", "test_value");

        // Then
        assertNotNull(result.getMetadata());
        assertEquals("test_value", result.getMetadata().get("test_key"));
    }

    // ==================== 辅助方法 ====================

    private DocumentVector createTestDocumentVector(Long documentId, List<Double> vectorData) {
        DocumentVector docVector = new DocumentVector();
        docVector.setId(documentId);
        docVector.setDocumentId(documentId);
        docVector.setVectorData(vectorData);
        docVector.setCreatedAt(LocalDateTime.now());
        return docVector;
    }
}