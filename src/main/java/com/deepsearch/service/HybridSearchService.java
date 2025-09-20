package com.deepsearch.service;

import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 混合搜索服务 - 实现关键词搜索与语义搜索的并行执行与结果合并
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridSearchService {

    private final ElasticsearchSearchService elasticsearchService;
    private final SearchRelevanceService relevanceService;

    // 用于并行搜索的线程池
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(4);

    // 默认权重配置
    private static final float DEFAULT_KEYWORD_WEIGHT = 1.0f;
    private static final float DEFAULT_VECTOR_WEIGHT = 2.0f;

    /**
     * 执行混合搜索
     *
     * @param searchRequest 搜索请求参数
     * @return 搜索结果
     */
    public SearchResult hybridSearch(SearchRequest searchRequest) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始执行混合搜索: query={}, weights={}/{}",
                searchRequest.getQuery(),
                searchRequest.getKeywordWeight(),
                searchRequest.getVectorWeight());

            // 1. 并行执行关键词和语义搜索
            CompletableFuture<List<DocumentIndex>> keywordResults =
                CompletableFuture.supplyAsync(() -> performKeywordSearch(searchRequest), searchExecutor);

            CompletableFuture<List<DocumentIndex>> semanticResults =
                CompletableFuture.supplyAsync(() -> performSemanticSearch(searchRequest), searchExecutor);

            // 2. 等待两个搜索完成并获取结果
            List<DocumentIndex> keywordDocs = keywordResults.join();
            List<DocumentIndex> semanticDocs = semanticResults.join();

            log.info("并行搜索完成: 关键词结果={}, 语义结果={}", keywordDocs.size(), semanticDocs.size());

            // 3. 合并和重排序结果
            SearchWeights weights = buildSearchWeights(searchRequest);
            List<DocumentIndex> mergedResults = relevanceService.mergeAndRank(
                keywordDocs, semanticDocs, weights);

            // 4. 应用分页
            List<DocumentIndex> pagedResults = applyPagination(mergedResults, searchRequest);

            long responseTime = System.currentTimeMillis() - startTime;

            // 5. 构建SearchResult响应
            SearchResult result = new SearchResult(
                searchRequest.getQuery(),
                pagedResults,
                mergedResults.size(),
                searchRequest.getFrom() / searchRequest.getSize(),
                searchRequest.getSize(),
                responseTime,
                "hybrid"
            );

            log.info("混合搜索完成: 总结果={}, 响应时间={}ms", mergedResults.size(), responseTime);
            return result;

        } catch (Exception e) {
            log.error("混合搜索执行失败: query={}", searchRequest.getQuery(), e);

            // 降级到单一搜索策略
            return performFallbackSearch(searchRequest, startTime);
        }
    }

    /**
     * 执行关键词搜索
     */
    private List<DocumentIndex> performKeywordSearch(SearchRequest searchRequest) {
        try {
            log.debug("执行关键词搜索: {}", searchRequest.getQuery());

            return elasticsearchService.keywordSearch(
                searchRequest.getQuery(),
                searchRequest.getSpaceId(),
                searchRequest.getChannels(),
                0, // 获取更多结果用于合并
                Math.max(100, searchRequest.getSize() * 3)
            );
        } catch (IOException e) {
            log.warn("关键词搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 执行语义搜索
     */
    private List<DocumentIndex> performSemanticSearch(SearchRequest searchRequest) {
        try {
            log.debug("执行语义搜索: {}", searchRequest.getQuery());

            return elasticsearchService.vectorSearch(
                searchRequest.getQuery(),
                searchRequest.getSpaceId(),
                searchRequest.getChannels(),
                0, // 获取更多结果用于合并
                Math.max(100, searchRequest.getSize() * 3)
            );
        } catch (IOException e) {
            log.warn("语义搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建搜索权重配置
     */
    private SearchWeights buildSearchWeights(SearchRequest searchRequest) {
        Float keywordWeight = searchRequest.getKeywordWeight();
        Float vectorWeight = searchRequest.getVectorWeight();

        // 使用默认权重如果未设置
        if (keywordWeight == null) {
            keywordWeight = DEFAULT_KEYWORD_WEIGHT;
        }
        if (vectorWeight == null) {
            vectorWeight = DEFAULT_VECTOR_WEIGHT;
        }

        return new SearchWeights(keywordWeight, vectorWeight);
    }

    /**
     * 应用分页到搜索结果
     */
    private List<DocumentIndex> applyPagination(List<DocumentIndex> results, SearchRequest searchRequest) {
        int from = searchRequest.getFrom();
        int size = searchRequest.getSize();
        int totalResults = results.size();

        if (from >= totalResults) {
            return Collections.emptyList();
        }

        int endIndex = Math.min(from + size, totalResults);
        return results.subList(from, endIndex);
    }

    /**
     * 降级搜索策略 - 当混合搜索失败时使用
     */
    private SearchResult performFallbackSearch(SearchRequest searchRequest, long startTime) {
        log.warn("执行降级搜索策略: {}", searchRequest.getQuery());

        try {
            // 优先尝试关键词搜索
            List<DocumentIndex> fallbackResults = elasticsearchService.keywordSearch(
                searchRequest.getQuery(),
                searchRequest.getSpaceId(),
                searchRequest.getChannels(),
                searchRequest.getFrom(),
                searchRequest.getSize()
            );

            long responseTime = System.currentTimeMillis() - startTime;

            return new SearchResult(
                searchRequest.getQuery(),
                fallbackResults,
                fallbackResults.size(),
                searchRequest.getFrom() / searchRequest.getSize(),
                searchRequest.getSize(),
                responseTime,
                "keyword_fallback"
            );

        } catch (IOException e) {
            log.error("降级搜索也失败了", e);

            long responseTime = System.currentTimeMillis() - startTime;
            return new SearchResult(
                searchRequest.getQuery(),
                Collections.emptyList(),
                0,
                0,
                searchRequest.getSize(),
                responseTime,
                "failed"
            );
        }
    }

    /**
     * 智能权重自适应调整
     * 根据查询类型和历史表现动态调整权重
     */
    public SearchWeights adaptiveWeights(String query, SearchWeights currentWeights) {
        // 简单的自适应逻辑 - 可以后续扩展为机器学习模型
        float keywordWeight = currentWeights.getKeywordWeight();
        float vectorWeight = currentWeights.getVectorWeight();

        // 如果查询包含特定关键词，提高关键词权重
        if (containsSpecificTerms(query)) {
            keywordWeight *= 1.2f;
        }

        // 如果查询比较抽象或概念性，提高语义权重
        if (isConceptualQuery(query)) {
            vectorWeight *= 1.2f;
        }

        return new SearchWeights(keywordWeight, vectorWeight);
    }

    /**
     * 检查查询是否包含特定术语
     */
    private boolean containsSpecificTerms(String query) {
        String[] specificTerms = {"产品", "服务", "账户", "卡", "贷款", "理财"};
        String lowerQuery = query.toLowerCase();

        for (String term : specificTerms) {
            if (lowerQuery.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为概念性查询
     */
    private boolean isConceptualQuery(String query) {
        // 简单的启发式规则判断概念性查询
        return query.length() > 10 && !query.matches(".*\\d.*") &&
               (query.contains("如何") || query.contains("什么") || query.contains("为什么"));
    }

    /**
     * 搜索权重配置类
     */
    public static class SearchWeights {
        private final float keywordWeight;
        private final float vectorWeight;

        public SearchWeights(float keywordWeight, float vectorWeight) {
            this.keywordWeight = keywordWeight;
            this.vectorWeight = vectorWeight;
        }

        public float getKeywordWeight() {
            return keywordWeight;
        }

        public float getVectorWeight() {
            return vectorWeight;
        }

        public float getTotalWeight() {
            return keywordWeight + vectorWeight;
        }

        public float getNormalizedKeywordWeight() {
            return keywordWeight / getTotalWeight();
        }

        public float getNormalizedVectorWeight() {
            return vectorWeight / getTotalWeight();
        }

        @Override
        public String toString() {
            return String.format("SearchWeights{keyword=%.2f, vector=%.2f}", keywordWeight, vectorWeight);
        }
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        if (searchExecutor != null && !searchExecutor.isShutdown()) {
            searchExecutor.shutdown();
            log.info("混合搜索服务线程池已关闭");
        }
    }
}