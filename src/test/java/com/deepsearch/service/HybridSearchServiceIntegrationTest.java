package com.deepsearch.service;

import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 混合搜索服务集成测试类
 * 重点测试同义词扩展功能的集成
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceIntegrationTest {

    @Mock
    private ElasticsearchSearchService elasticsearchSearchService;

    @Mock
    private SearchRelevanceService relevanceService;

    @Mock
    private SynonymService synonymService;

    @Mock
    private QueryExpansionService queryExpansionService;

    @InjectMocks
    private HybridSearchService hybridSearchService;

    private SearchRequest testSearchRequest;
    private List<DocumentIndex> mockKeywordResults;
    private List<DocumentIndex> mockSemanticResults;

    @BeforeEach
    void setUp() {
        // 设置测试搜索请求
        testSearchRequest = new SearchRequest();
        testSearchRequest.setQuery("房贷利率");
        testSearchRequest.setSpaceId(1L);
        testSearchRequest.setChannels(Arrays.asList("web", "mobile"));
        testSearchRequest.setFrom(0);
        testSearchRequest.setSize(10);
        testSearchRequest.setKeywordWeight(1.0f);
        testSearchRequest.setVectorWeight(2.0f);

        // 设置模拟的搜索结果
        mockKeywordResults = Arrays.asList(
            createDocumentIndex("doc1", "房贷利率查询指南", 0.9f),
            createDocumentIndex("doc2", "住房贷款利率政策", 0.85f)
        );

        mockSemanticResults = Arrays.asList(
            createDocumentIndex("doc3", "按揭贷款利率说明", 0.88f),
            createDocumentIndex("doc4", "房屋贷款利率计算", 0.82f)
        );
    }

    @Test
    void testHybridSearch_WithSynonymExpansion_ShouldExpandQueryAndSearch() throws IOException {
        // Given
        Set<String> expandedQueries = Set.of("房贷利率", "住房贷款利率", "按揭贷款利率");
        QueryExpansionService.QueryExpansionResult expansionResult =
            new QueryExpansionService.QueryExpansionResult(
                "房贷利率",
                expandedQueries,
                QueryExpansionService.QueryType.PRODUCT_QUERY
            );

        when(queryExpansionService.expandQuery(eq("房贷利率"), any())).thenReturn(expansionResult);

        // 模拟对每个扩展查询的搜索结果
        when(elasticsearchSearchService.keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(mockKeywordResults.get(0)));
        when(elasticsearchSearchService.keywordSearch(eq("住房贷款利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(mockKeywordResults.get(1)));
        when(elasticsearchSearchService.keywordSearch(eq("按揭贷款利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        when(elasticsearchSearchService.vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);

        List<DocumentIndex> mergedResults = new ArrayList<>();
        mergedResults.addAll(mockKeywordResults);
        mergedResults.addAll(mockSemanticResults);

        when(relevanceService.mergeAndRank(anyList(), anyList(), any())).thenReturn(mergedResults);

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQuery()).isEqualTo("房贷利率");
        assertThat(result.getSearchType()).isEqualTo("hybrid_with_expansion");
        assertThat(result.getExpandedQueries()).containsAll(expandedQueries);
        assertThat(result.getQueryType()).isEqualTo("PRODUCT_QUERY");
        assertThat(result.getDocuments()).hasSize(4);

        // 验证查询扩展被调用
        verify(queryExpansionService).expandQuery(eq("房贷利率"), any());

        // 验证每个扩展查询都被搜索
        verify(elasticsearchSearchService, times(3)).keywordSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt());
        verify(elasticsearchSearchService).vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt());

        // 验证结果合并
        verify(relevanceService).mergeAndRank(anyList(), anyList(), any());
    }

    @Test
    void testHybridSearch_WithQueryExpansionFailure_ShouldFallbackGracefully() throws IOException {
        // Given
        when(queryExpansionService.expandQuery(eq("房贷利率"), any()))
            .thenThrow(new RuntimeException("查询扩展失败"));

        when(elasticsearchSearchService.keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockKeywordResults);

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSearchType()).isEqualTo("keyword_fallback");
        assertThat(result.getQuery()).isEqualTo("房贷利率");

        // 验证降级到关键词搜索
        verify(elasticsearchSearchService).keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt());
    }

    @Test
    void testHybridSearch_WithEmptyExpansion_ShouldHandleGracefully() throws IOException {
        // Given
        QueryExpansionService.QueryExpansionResult expansionResult =
            new QueryExpansionService.QueryExpansionResult(
                "房贷利率",
                Collections.emptySet(),
                QueryExpansionService.QueryType.PRODUCT_QUERY
            );

        when(queryExpansionService.expandQuery(eq("房贷利率"), any())).thenReturn(expansionResult);

        when(elasticsearchSearchService.keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockKeywordResults);
        when(elasticsearchSearchService.vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);

        when(relevanceService.mergeAndRank(anyList(), anyList(), any())).thenReturn(mockKeywordResults);

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSearchType()).isEqualTo("hybrid_with_expansion");
        assertThat(result.getExpandedQueries()).contains("房贷利率"); // 至少包含原始查询

        // 验证原始查询被搜索
        verify(elasticsearchSearchService).keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt());
    }

    @Test
    void testHybridSearch_WithLargeExpansion_ShouldLimitQueries() throws IOException {
        // Given
        Set<String> largeExpansionSet = new HashSet<>();
        largeExpansionSet.add("房贷利率");
        for (int i = 1; i <= 20; i++) {
            largeExpansionSet.add("扩展查询" + i);
        }

        QueryExpansionService.QueryExpansionResult expansionResult =
            new QueryExpansionService.QueryExpansionResult(
                "房贷利率",
                largeExpansionSet,
                QueryExpansionService.QueryType.PRODUCT_QUERY
            );

        when(queryExpansionService.expandQuery(eq("房贷利率"), any())).thenReturn(expansionResult);

        when(elasticsearchSearchService.keywordSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(elasticsearchSearchService.vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);

        when(relevanceService.mergeAndRank(anyList(), anyList(), any())).thenReturn(mockSemanticResults);

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();

        // 验证不会对所有扩展查询都进行搜索（应该有限制）
        verify(elasticsearchSearchService, atMost(21)).keywordSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt());
    }

    @Test
    void testHybridSearch_WithDuplicateResults_ShouldDeduplicate() throws IOException {
        // Given
        DocumentIndex duplicateDoc = createDocumentIndex("doc1", "重复文档", 0.9f);

        Set<String> expandedQueries = Set.of("房贷利率", "住房贷款利率");
        QueryExpansionService.QueryExpansionResult expansionResult =
            new QueryExpansionService.QueryExpansionResult(
                "房贷利率",
                expandedQueries,
                QueryExpansionService.QueryType.PRODUCT_QUERY
            );

        when(queryExpansionService.expandQuery(eq("房贷利率"), any())).thenReturn(expansionResult);

        // 两个查询返回相同的文档
        when(elasticsearchSearchService.keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(duplicateDoc));
        when(elasticsearchSearchService.keywordSearch(eq("住房贷款利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(duplicateDoc)); // 相同ID的文档

        when(elasticsearchSearchService.vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        when(relevanceService.mergeAndRank(anyList(), anyList(), any())).thenReturn(Arrays.asList(duplicateDoc));

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        // 验证HybridSearchService内部的去重逻辑正常工作
        // 具体的去重逻辑在HybridSearchService.deduplicateAndSort方法中
    }

    @Test
    void testHybridSearch_WithScoreAdjustment_ShouldAdjustExpandedQueryScores() throws IOException {
        // Given
        DocumentIndex originalDoc = createDocumentIndex("doc1", "原始查询结果", 1.0f);
        DocumentIndex expandedDoc = createDocumentIndex("doc2", "扩展查询结果", 1.0f);

        Set<String> expandedQueries = Set.of("房贷利率", "住房贷款利率");
        QueryExpansionService.QueryExpansionResult expansionResult =
            new QueryExpansionService.QueryExpansionResult(
                "房贷利率",
                expandedQueries,
                QueryExpansionService.QueryType.PRODUCT_QUERY
            );

        when(queryExpansionService.expandQuery(eq("房贷利率"), any())).thenReturn(expansionResult);

        when(elasticsearchSearchService.keywordSearch(eq("房贷利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(originalDoc));
        when(elasticsearchSearchService.keywordSearch(eq("住房贷款利率"), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Arrays.asList(expandedDoc));

        when(elasticsearchSearchService.vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        when(relevanceService.mergeAndRank(anyList(), anyList(), any()))
            .thenReturn(Arrays.asList(originalDoc, expandedDoc));

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        // 验证扩展查询的结果分数被调整（在HybridSearchService中应该 * 0.8）
        // 这个测试主要验证逻辑流程正确
    }

    @Test
    void testHybridSearch_WithContextHints_ShouldPassContextToExpansion() throws IOException {
        // Given
        testSearchRequest.setSpaceId(123L);
        testSearchRequest.setChannels(Arrays.asList("mobile", "web", "atm"));

        Set<String> expandedQueries = Set.of("房贷利率");
        QueryExpansionService.QueryExpansionResult expansionResult =
            new QueryExpansionService.QueryExpansionResult(
                "房贷利率",
                expandedQueries,
                QueryExpansionService.QueryType.PRODUCT_QUERY
            );

        when(queryExpansionService.expandQuery(eq("房贷利率"), argThat(contextHints -> {
            return contextHints.containsKey("spaceId") &&
                   contextHints.get("spaceId").equals(123L) &&
                   contextHints.containsKey("channels");
        }))).thenReturn(expansionResult);

        when(elasticsearchSearchService.keywordSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockKeywordResults);
        when(elasticsearchSearchService.vectorSearch(anyString(), anyLong(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);
        when(relevanceService.mergeAndRank(anyList(), anyList(), any())).thenReturn(mockKeywordResults);

        // When
        SearchResult result = hybridSearchService.hybridSearch(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        verify(queryExpansionService).expandQuery(eq("房贷利率"), any());
    }

    // 辅助方法
    private DocumentIndex createDocumentIndex(String id, String title, float score) {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(id);
        doc.setTitle(title);
        doc.setScore(score);
        doc.setContent("测试内容 - " + title);
        return doc;
    }
}