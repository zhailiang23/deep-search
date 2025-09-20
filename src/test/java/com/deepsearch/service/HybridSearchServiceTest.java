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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 混合搜索服务测试
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchServiceTest {

    @Mock
    private ElasticsearchSearchService elasticsearchService;

    @Mock
    private SearchRelevanceService relevanceService;

    @InjectMocks
    private HybridSearchService hybridSearchService;

    private SearchRequest searchRequest;
    private List<DocumentIndex> mockKeywordResults;
    private List<DocumentIndex> mockSemanticResults;
    private List<DocumentIndex> mockMergedResults;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        searchRequest = new SearchRequest("银行产品", 0, 10);
        searchRequest.setKeywordWeight(1.0f);
        searchRequest.setVectorWeight(2.0f);

        // 模拟关键词搜索结果
        mockKeywordResults = Arrays.asList(
            createMockDocument("1", "银行储蓄产品介绍", "详细介绍银行的各种储蓄产品"),
            createMockDocument("2", "银行投资理财", "专业的投资理财服务")
        );

        // 模拟语义搜索结果
        mockSemanticResults = Arrays.asList(
            createMockDocument("3", "金融服务指南", "全方位的金融服务"),
            createMockDocument("1", "银行储蓄产品介绍", "详细介绍银行的各种储蓄产品") // 重复文档
        );

        // 模拟合并后的结果
        mockMergedResults = Arrays.asList(
            createMockDocument("1", "银行储蓄产品介绍", "详细介绍银行的各种储蓄产品"),
            createMockDocument("3", "金融服务指南", "全方位的金融服务"),
            createMockDocument("2", "银行投资理财", "专业的投资理财服务")
        );
    }

    @Test
    void testHybridSearch_Success() throws IOException {
        // 准备mock
        when(elasticsearchService.keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockKeywordResults);
        when(elasticsearchService.vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);
        when(relevanceService.mergeAndRank(any(), any(), any()))
            .thenReturn(mockMergedResults);

        // 执行测试
        SearchResult result = hybridSearchService.hybridSearch(searchRequest);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getQuery()).isEqualTo("银行产品");
        assertThat(result.getSearchType()).isEqualTo("hybrid");
        assertThat(result.getTotalResults()).isEqualTo(3);
        assertThat(result.getDocuments()).hasSize(3);
        assertThat(result.getResponseTimeMs()).isGreaterThan(0);
        assertThat(result.getPerformanceStats()).isNotNull();

        // 验证服务调用
        verify(elasticsearchService).keywordSearch(eq("银行产品"), isNull(), isNull(), eq(0), eq(30));
        verify(elasticsearchService).vectorSearch(eq("银行产品"), isNull(), isNull(), eq(0), eq(30));
        verify(relevanceService).mergeAndRank(eq(mockKeywordResults), eq(mockSemanticResults), any());
    }

    @Test
    void testHybridSearch_WithPagination() throws IOException {
        // 准备大量结果进行分页测试
        List<DocumentIndex> largeResults = Arrays.asList(
            createMockDocument("1", "文档1", "内容1"),
            createMockDocument("2", "文档2", "内容2"),
            createMockDocument("3", "文档3", "内容3"),
            createMockDocument("4", "文档4", "内容4"),
            createMockDocument("5", "文档5", "内容5")
        );

        searchRequest.setFrom(2);  // 从第3个结果开始
        searchRequest.setSize(2);  // 每页2个结果

        when(elasticsearchService.keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockKeywordResults);
        when(elasticsearchService.vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);
        when(relevanceService.mergeAndRank(any(), any(), any()))
            .thenReturn(largeResults);

        // 执行测试
        SearchResult result = hybridSearchService.hybridSearch(searchRequest);

        // 验证分页结果
        assertThat(result.getTotalResults()).isEqualTo(5);
        assertThat(result.getDocuments()).hasSize(2);  // 分页后只有2个
        assertThat(result.getPageNumber()).isEqualTo(1); // from=2, size=2 -> page=1
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.isHasMore()).isTrue();
    }

    @Test
    void testHybridSearch_KeywordSearchFails() throws IOException {
        // 模拟关键词搜索失败
        when(elasticsearchService.keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenThrow(new IOException("Elasticsearch连接失败"));
        when(elasticsearchService.vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);
        when(relevanceService.mergeAndRank(any(), any(), any()))
            .thenReturn(mockSemanticResults);

        // 执行测试
        SearchResult result = hybridSearchService.hybridSearch(searchRequest);

        // 验证：即使关键词搜索失败，语义搜索仍应正常工作
        assertThat(result).isNotNull();
        assertThat(result.getSearchType()).isEqualTo("hybrid");

        // 验证关键词搜索被调用但失败，语义搜索正常
        verify(elasticsearchService).keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt());
        verify(elasticsearchService).vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt());
        verify(relevanceService).mergeAndRank(eq(Collections.emptyList()), eq(mockSemanticResults), any());
    }

    @Test
    void testHybridSearch_BothSearchesFail() throws IOException {
        // 模拟两种搜索都失败
        when(elasticsearchService.keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenThrow(new IOException("Elasticsearch连接失败"));
        when(elasticsearchService.vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenThrow(new IOException("向量搜索失败"));

        // 模拟降级搜索也失败
        when(elasticsearchService.keywordSearch(eq("银行产品"), anyString(), anyList(), eq(0), eq(10)))
            .thenThrow(new IOException("降级搜索失败"));

        // 执行测试
        SearchResult result = hybridSearchService.hybridSearch(searchRequest);

        // 验证：返回失败结果
        assertThat(result).isNotNull();
        assertThat(result.getSearchType()).isEqualTo("failed");
        assertThat(result.getTotalResults()).isEqualTo(0);
        assertThat(result.getDocuments()).isEmpty();
    }

    @Test
    void testAdaptiveWeights() {
        // 测试智能权重调整
        HybridSearchService.SearchWeights originalWeights =
            new HybridSearchService.SearchWeights(1.0f, 2.0f);

        // 测试包含特定术语的查询
        HybridSearchService.SearchWeights adaptedWeights1 =
            hybridSearchService.adaptiveWeights("银行产品", originalWeights);
        assertThat(adaptedWeights1.getKeywordWeight()).isGreaterThan(originalWeights.getKeywordWeight());

        // 测试概念性查询
        HybridSearchService.SearchWeights adaptedWeights2 =
            hybridSearchService.adaptiveWeights("如何办理银行业务流程", originalWeights);
        assertThat(adaptedWeights2.getVectorWeight()).isGreaterThan(originalWeights.getVectorWeight());

        // 测试普通查询
        HybridSearchService.SearchWeights adaptedWeights3 =
            hybridSearchService.adaptiveWeights("测试查询", originalWeights);
        assertThat(adaptedWeights3.getKeywordWeight()).isEqualTo(originalWeights.getKeywordWeight());
        assertThat(adaptedWeights3.getVectorWeight()).isEqualTo(originalWeights.getVectorWeight());
    }

    @Test
    void testSearchWeights() {
        // 测试权重配置类
        HybridSearchService.SearchWeights weights =
            new HybridSearchService.SearchWeights(1.5f, 2.5f);

        assertThat(weights.getKeywordWeight()).isEqualTo(1.5f);
        assertThat(weights.getVectorWeight()).isEqualTo(2.5f);
        assertThat(weights.getTotalWeight()).isEqualTo(4.0f);
        assertThat(weights.getNormalizedKeywordWeight()).isEqualTo(0.375f);
        assertThat(weights.getNormalizedVectorWeight()).isEqualTo(0.625f);
    }

    @Test
    void testDefaultWeights() throws IOException {
        // 测试默认权重
        SearchRequest requestWithoutWeights = new SearchRequest("测试查询", 0, 10);

        when(elasticsearchService.keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockKeywordResults);
        when(elasticsearchService.vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(mockSemanticResults);
        when(relevanceService.mergeAndRank(any(), any(), any()))
            .thenReturn(mockMergedResults);

        SearchResult result = hybridSearchService.hybridSearch(requestWithoutWeights);

        // 验证默认权重被应用
        assertThat(result).isNotNull();
        verify(relevanceService).mergeAndRank(any(), any(), argThat(weights ->
            weights.getKeywordWeight() == 1.0f && weights.getVectorWeight() == 2.0f));
    }

    @Test
    void testEmptyResults() throws IOException {
        // 测试空结果
        when(elasticsearchService.keywordSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(elasticsearchService.vectorSearch(anyString(), anyString(), anyList(), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(relevanceService.mergeAndRank(any(), any(), any()))
            .thenReturn(Collections.emptyList());

        SearchResult result = hybridSearchService.hybridSearch(searchRequest);

        assertThat(result).isNotNull();
        assertThat(result.getTotalResults()).isEqualTo(0);
        assertThat(result.getDocuments()).isEmpty();
        assertThat(result.isEmpty()).isTrue();
    }

    /**
     * 创建模拟文档
     */
    private DocumentIndex createMockDocument(String id, String title, String content) {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(id);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSummary(content.substring(0, Math.min(50, content.length())));
        doc.setCategory("测试分类");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }
}