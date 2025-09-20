package com.deepsearch.service;

import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import com.deepsearch.entity.Document;
import com.deepsearch.repository.DocumentRepository;
import com.deepsearch.service.SemanticSearchService.SearchContext;
import com.deepsearch.service.SemanticSearchService.SearchScene;
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
 * SemanticSearchService 单元测试
 * 测试语义搜索服务的各项功能
 */
@ExtendWith(MockitoExtension.class)
class SemanticSearchServiceTest {

    @Mock
    private VectorProcessingEngine vectorProcessingEngine;

    @Mock
    private VectorSimilarityService vectorSimilarityService;

    @Mock
    private ElasticsearchSearchService elasticsearchSearchService;

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private SemanticSearchService semanticSearchService;

    private Document testDocument;
    private DocumentResponseDto testDocumentDto;
    private List<Float> testVector;

    @BeforeEach
    void setUp() {
        // 设置配置属性
        ReflectionTestUtils.setField(semanticSearchService, "similarityThreshold", 0.7);
        ReflectionTestUtils.setField(semanticSearchService, "maxExpandedTerms", 10);
        ReflectionTestUtils.setField(semanticSearchService, "vectorBoost", 2.0f);
        ReflectionTestUtils.setField(semanticSearchService, "defaultTopK", 100);

        // 创建测试数据
        testDocument = createTestDocument();
        testDocumentDto = new DocumentResponseDto(testDocument);
        testVector = createTestVector();
    }

    @Test
    void testSemanticSearch_Success() {
        // Given
        String query = "银行卡申请";
        SearchContext context = new SearchContext();

        List<SimilarityResult> similarityResults = Arrays.asList(
            new SimilarityResult(1L, 0.95, SimilarityAlgorithm.COSINE),
            new SimilarityResult(2L, 0.85, SimilarityAlgorithm.COSINE)
        );

        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(testVector);
        when(vectorSimilarityService.batchSimilaritySearch(
            any(), anyInt(), any(SimilarityAlgorithm.class), anyDouble()))
            .thenReturn(similarityResults);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(createTestDocument(2L, "信用卡服务")));

        // When
        List<DocumentResponseDto> results = semanticSearchService.semanticSearch(query, context);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("测试文档", results.get(0).getTitle());

        verify(vectorProcessingEngine).generateEmbedding(anyString());
        verify(vectorSimilarityService).batchSimilaritySearch(any(), anyInt(), any(), anyDouble());
        verify(documentRepository, times(2)).findById(anyLong());
    }

    @Test
    void testSemanticSearch_EmptyVector() {
        // Given
        String query = "银行卡申请";
        SearchContext context = new SearchContext();

        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(Collections.emptyList());

        // When
        List<DocumentResponseDto> results = semanticSearchService.semanticSearch(query, context);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(vectorProcessingEngine).generateEmbedding(anyString());
        verifyNoInteractions(vectorSimilarityService);
    }

    @Test
    void testVectorizeQuery_Success() {
        // Given
        String query = "银行卡申请流程";
        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(testVector);

        // When
        List<Float> result = semanticSearchService.vectorizeQuery(query);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(testVector.size(), result.size());

        verify(vectorProcessingEngine).generateEmbedding(anyString());
    }

    @Test
    void testVectorizeQuery_NullQuery() {
        // When
        List<Float> result = semanticSearchService.vectorizeQuery(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(vectorProcessingEngine);
    }

    @Test
    void testVectorizeQuery_EmptyQuery() {
        // When
        List<Float> result = semanticSearchService.vectorizeQuery("   ");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(vectorProcessingEngine);
    }

    @Test
    void testExpandQueryWithSynonyms_Success() {
        // Given
        String query = "银行卡";

        // When
        List<String> result = semanticSearchService.expandQueryWithSynonyms(query);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("银行卡")); // 原查询
        assertTrue(result.contains("储蓄卡")); // 同义词
        assertTrue(result.contains("借记卡")); // 同义词
    }

    @Test
    void testExpandQueryWithSynonyms_NoSynonyms() {
        // Given
        String query = "未知术语";

        // When
        List<String> result = semanticSearchService.expandQueryWithSynonyms(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("未知术语"));
    }

    @Test
    void testInferSearchScene_LoanScene() {
        // Given
        String query = "我想申请贷款";
        SearchContext context = new SearchContext();

        // When
        SemanticSearchService.SceneContext result = semanticSearchService.inferSearchScene(query, context);

        // Then
        assertNotNull(result);
        assertEquals(SearchScene.LOAN, result.getScene());
        assertNotNull(result.getFilters());
    }

    @Test
    void testInferSearchScene_WealthManagementScene() {
        // Given
        String query = "理财产品推荐";
        SearchContext context = new SearchContext();

        // When
        SemanticSearchService.SceneContext result = semanticSearchService.inferSearchScene(query, context);

        // Then
        assertNotNull(result);
        assertEquals(SearchScene.WEALTH_MANAGEMENT, result.getScene());
    }

    @Test
    void testInferSearchScene_CardServiceScene() {
        // Given
        String query = "信用卡申请条件";
        SearchContext context = new SearchContext();

        // When
        SemanticSearchService.SceneContext result = semanticSearchService.inferSearchScene(query, context);

        // Then
        assertNotNull(result);
        assertEquals(SearchScene.CARD_SERVICE, result.getScene());
    }

    @Test
    void testInferSearchScene_DepositScene() {
        // Given
        String query = "定期存款利率";
        SearchContext context = new SearchContext();

        // When
        SemanticSearchService.SceneContext result = semanticSearchService.inferSearchScene(query, context);

        // Then
        assertNotNull(result);
        assertEquals(SearchScene.DEPOSIT, result.getScene());
    }

    @Test
    void testInferSearchScene_GeneralScene() {
        // Given
        String query = "客服电话";
        SearchContext context = new SearchContext();

        // When
        SemanticSearchService.SceneContext result = semanticSearchService.inferSearchScene(query, context);

        // Then
        assertNotNull(result);
        assertEquals(SearchScene.GENERAL, result.getScene());
    }

    @Test
    void testHybridSemanticSearch_Success() throws Exception {
        // Given
        String query = "银行卡申请";
        SearchContext context = new SearchContext();
        float keywordWeight = 0.6f;
        float vectorWeight = 0.4f;

        // Mock 关键词搜索结果
        List<DocumentIndex> keywordSearchResults = Arrays.asList(
            createDocumentIndex("1", "银行卡申请指南"),
            createDocumentIndex("2", "信用卡办理流程")
        );

        // Mock 语义搜索结果
        List<SimilarityResult> similarityResults = Arrays.asList(
            new SimilarityResult(1L, 0.95, SimilarityAlgorithm.COSINE),
            new SimilarityResult(3L, 0.85, SimilarityAlgorithm.COSINE)
        );

        when(elasticsearchSearchService.keywordSearch(anyString(), isNull(), isNull(), anyInt(), anyInt()))
            .thenReturn(keywordSearchResults);
        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(testVector);
        when(vectorSimilarityService.batchSimilaritySearch(any(), anyInt(), any(), anyDouble()))
            .thenReturn(similarityResults);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(createTestDocument(2L, "信用卡服务")));
        when(documentRepository.findById(3L)).thenReturn(Optional.of(createTestDocument(3L, "银行产品")));

        // When
        List<DocumentResponseDto> results = semanticSearchService.hybridSemanticSearch(
            query, context, keywordWeight, vectorWeight);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());

        verify(elasticsearchSearchService).keywordSearch(anyString(), isNull(), isNull(), anyInt(), anyInt());
        verify(vectorProcessingEngine, times(2)).generateEmbedding(anyString()); // 一次在语义搜索，一次在hybridSemanticSearch调用semanticSearch
        verify(vectorSimilarityService, times(2)).batchSimilaritySearch(any(), anyInt(), any(), anyDouble());
    }

    @Test
    void testHybridSemanticSearch_FallbackToSemanticSearch() throws Exception {
        // Given
        String query = "银行卡申请";
        SearchContext context = new SearchContext();
        float keywordWeight = 0.6f;
        float vectorWeight = 0.4f;

        // Mock Elasticsearch 抛出异常
        when(elasticsearchSearchService.keywordSearch(anyString(), isNull(), isNull(), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("Elasticsearch error"));

        // Mock 语义搜索正常工作
        List<SimilarityResult> similarityResults = Arrays.asList(
            new SimilarityResult(1L, 0.95, SimilarityAlgorithm.COSINE)
        );
        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(testVector);
        when(vectorSimilarityService.batchSimilaritySearch(any(), anyInt(), any(), anyDouble()))
            .thenReturn(similarityResults);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        // When
        List<DocumentResponseDto> results = semanticSearchService.hybridSemanticSearch(
            query, context, keywordWeight, vectorWeight);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size()); // 应该降级到语义搜索
    }

    @Test
    void testVectorProcessing_VectorNormalization() {
        // Given
        String query = "测试查询";
        List<Float> unnormalizedVector = Arrays.asList(3.0f, 4.0f, 0.0f); // 模长为5
        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(unnormalizedVector);

        // When
        List<Float> result = semanticSearchService.vectorizeQuery(query);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // 验证向量是否已标准化（模长应该接近1）
        double magnitude = Math.sqrt(
            result.stream().mapToDouble(f -> f * f).sum()
        );
        assertEquals(1.0, magnitude, 0.001);
    }

    @Test
    void testSearchWithContext_SpaceAndChannelFiltering() throws Exception {
        // Given
        String query = "银行卡申请";
        SearchContext context = new SearchContext();
        context.setSpaceId("test-space");
        context.setChannels(Arrays.asList("mobile", "web"));

        List<DocumentIndex> elasticsearchResults = Arrays.asList(
            createDocumentIndex("1", "银行卡申请指南")
        );

        when(elasticsearchSearchService.keywordSearch(
            eq(query), eq("test-space"), eq(Arrays.asList("mobile", "web")), anyInt(), anyInt()))
            .thenReturn(elasticsearchResults);
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        // When - 测试关键词搜索部分
        // 直接调用内部方法进行测试不太合适，我们通过hybridSemanticSearch来间接测试
        List<SimilarityResult> similarityResults = Arrays.asList(
            new SimilarityResult(1L, 0.95, SimilarityAlgorithm.COSINE)
        );
        when(vectorProcessingEngine.generateEmbedding(anyString())).thenReturn(testVector);
        when(vectorSimilarityService.batchSimilaritySearch(any(), anyInt(), any(), anyDouble()))
            .thenReturn(similarityResults);

        List<DocumentResponseDto> results = semanticSearchService.hybridSemanticSearch(
            query, context, 1.0f, 0.0f); // 只使用关键词权重

        // Then
        verify(elasticsearchSearchService).keywordSearch(
            eq(query), eq("test-space"), eq(Arrays.asList("mobile", "web")), anyInt(), anyInt());
    }

    // ==================== 辅助方法 ====================

    private Document createTestDocument() {
        return createTestDocument(1L, "测试文档");
    }

    private Document createTestDocument(Long id, String title) {
        Document document = new Document();
        document.setId(id);
        document.setTitle(title);
        document.setContent("这是一个关于银行服务的测试文档");
        document.setStatus(Document.Status.INDEXED);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    private List<Float> createTestVector() {
        // 创建一个标准化的测试向量
        return Arrays.asList(0.6f, 0.8f, 0.0f); // 模长为1的向量
    }

    private DocumentIndex createDocumentIndex(String id, String title) {
        DocumentIndex docIndex = new DocumentIndex();
        docIndex.setId(id);
        docIndex.setTitle(title);
        docIndex.setContent("测试内容");
        docIndex.setCategory("测试分类");
        docIndex.setCreatedAt(LocalDateTime.now());
        return docIndex;
    }
}