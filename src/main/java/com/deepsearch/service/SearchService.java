package com.deepsearch.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import com.deepsearch.entity.Document;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.repository.DocumentRepository;
import com.deepsearch.repository.SearchLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 搜索业务服务 - 整合Elasticsearch搜索引擎与业务逻辑
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final DocumentRepository documentRepository;
    private final SearchLogRepository searchLogRepository;
    private final ElasticsearchSearchService elasticsearchSearchService;
    private final SearchSecurityService searchSecurityService;
    private final SearchResultConverter searchResultConverter;

    /**
     * 执行搜索 - 统一搜索入口
     */
    public SearchResult search(SearchRequest searchRequest) {
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentResponseDto> results;

            // 根据查询文本判断搜索类型，如果没有指定则默认为混合搜索
            String searchType = determineSearchType(searchRequest);
            
            // 使用Elasticsearch引擎执行搜索
            switch (searchType.toLowerCase()) {
                case "keyword":
                    results = performKeywordSearch(searchRequest);
                    break;
                case "semantic":
                case "vector":
                    results = performSemanticSearch(searchRequest);
                    break;
                case "hybrid":
                default:
                    results = performHybridSearch(searchRequest);
                    break;
            }

            int totalResults = results.size();

            // 处理分页
            int from = searchRequest.getFrom();
            int size = searchRequest.getSize();
            int startIndex = from;
            int endIndex = Math.min(startIndex + size, totalResults);

            List<DocumentResponseDto> pagedResults = results.subList(startIndex, endIndex);

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            // 记录搜索日志
            recordSearchLog(searchRequest, totalResults, (int) responseTime, searchType);

            // 构建SearchResult响应
            SearchResult searchResult = new SearchResult(
                searchRequest.getQuery(),
                searchResultConverter.convertToDocumentIndex(pagedResults),
                totalResults,
                from / size,
                size,
                responseTime,
                searchType
            );

            return searchResult;

        } catch (Exception e) {
            log.error("搜索执行失败: {}", searchRequest.getQuery(), e);
            throw new BadRequestException("搜索执行失败: " + e.getMessage());
        }
    }

    /**
     * 判断搜索类型
     */
    private String determineSearchType(SearchRequest searchRequest) {
        // 如果设置了向量权重，说明是混合搜索
        if (searchRequest.getVectorWeight() != null && searchRequest.getVectorWeight() > 0) {
            return "hybrid";
        }
        // 如果设置了关键词权重但没有向量权重，说明是关键词搜索
        if (searchRequest.getKeywordWeight() != null && searchRequest.getKeywordWeight() > 0 &&
            (searchRequest.getVectorWeight() == null || searchRequest.getVectorWeight() == 0)) {
            return "keyword";
        }
        // 默认使用混合搜索
        return "hybrid";
    }

    /**
     * 关键词搜索 - 委托给Elasticsearch
     */
    private List<DocumentResponseDto> performKeywordSearch(SearchRequest searchRequest) {
        try {
            String spaceId = searchSecurityService.getCurrentUserSpaceId();
            List<String> channels = searchSecurityService.getCurrentUserChannels();
            
            List<DocumentIndex> elasticsearchResults = elasticsearchSearchService.keywordSearch(
                searchRequest.getQueryText(),
                spaceId,
                channels,
                0,
                1000  // 获取更多结果用于业务层处理
            );

            return searchResultConverter.convertElasticsearchResults(elasticsearchResults);
        } catch (IOException e) {
            log.warn("Elasticsearch关键词搜索失败，降级到数据库搜索: {}", e.getMessage());
            return performFallbackKeywordSearch(searchRequest);
        }
    }

    /**
     * 语义搜索 - 委托给Elasticsearch向量搜索
     */
    private List<DocumentResponseDto> performSemanticSearch(SearchRequest searchRequest) {
        try {
            String spaceId = searchSecurityService.getCurrentUserSpaceId();
            List<String> channels = searchSecurityService.getCurrentUserChannels();
            
            List<DocumentIndex> elasticsearchResults = elasticsearchSearchService.vectorSearch(
                searchRequest.getQueryText(),
                spaceId,
                channels,
                0,
                1000
            );

            return searchResultConverter.convertElasticsearchResults(elasticsearchResults);
        } catch (IOException e) {
            log.warn("Elasticsearch语义搜索失败，降级到关键词搜索: {}", e.getMessage());
            return performFallbackKeywordSearch(searchRequest);
        }
    }

    /**
     * 混合搜索 - 委托给Elasticsearch混合搜索
     */
    private List<DocumentResponseDto> performHybridSearch(SearchRequest searchRequest) {
        try {
            String spaceId = searchSecurityService.getCurrentUserSpaceId();
            List<String> channels = searchSecurityService.getCurrentUserChannels();
            
            List<DocumentIndex> elasticsearchResults = elasticsearchSearchService.hybridSearch(
                searchRequest.getQueryText(),
                spaceId,
                channels,
                0,
                1000,
                1.0f,  // 关键词权重
                2.0f   // 向量权重
            );

            return searchResultConverter.convertElasticsearchResults(elasticsearchResults);
        } catch (IOException e) {
            log.warn("Elasticsearch混合搜索失败，降级到关键词搜索: {}", e.getMessage());
            return performFallbackKeywordSearch(searchRequest);
        }
    }

    /**
     * 降级搜索 - 当Elasticsearch不可用时使用数据库搜索
     */
    private List<DocumentResponseDto> performFallbackKeywordSearch(SearchRequest searchRequest) {
        String queryText = searchRequest.getQueryText().toLowerCase();

        // 搜索标题和内容匹配的文档
        List<Document> titleMatches = documentRepository.findByTitleContainingIgnoreCase(queryText);

        // 合并结果并去重
        return titleMatches.stream()
                .filter(doc -> doc.getStatus() == Document.Status.INDEXED)
                .map(DocumentResponseDto::new)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 记录搜索日志
     */
    private void recordSearchLog(SearchRequest searchRequest, int resultCount, int responseTime, String searchType) {
        try {
            Long userId = searchSecurityService.getCurrentUserIdOrNull();

            SearchLog searchLog = new SearchLog();
            searchLog.setQueryText(searchRequest.getQuery());
            searchLog.setUserId(userId);
            
            // 将字符串转换为SearchLog.SearchType枚举
            SearchLog.SearchType logSearchType;
            switch (searchType.toLowerCase()) {
                case "keyword":
                    logSearchType = SearchLog.SearchType.KEYWORD;
                    break;
                case "semantic":
                case "vector":
                    logSearchType = SearchLog.SearchType.SEMANTIC;
                    break;
                case "hybrid":
                default:
                    logSearchType = SearchLog.SearchType.HYBRID;
                    break;
            }
            searchLog.setSearchType(logSearchType);
            searchLog.setResultCount(resultCount);
            searchLog.setResponseTimeMs(responseTime);

            searchLogRepository.save(searchLog);
            log.info("搜索日志记录成功: {} - {} 条结果", searchRequest.getQuery(), resultCount);
        } catch (Exception e) {
            log.warn("搜索日志记录失败", e);
            // 不抛出异常，避免影响搜索功能
        }
    }

    /**
     * 获取用户搜索历史
     */
    @Transactional(readOnly = true)
    public List<SearchLog> getUserSearchHistory(Long userId, int limit) {
        // 使用统一的权限验证服务
        searchSecurityService.validateUserAccess(userId, "查看搜索历史");

        Pageable pageable = PageRequest.of(0, limit);
        return searchLogRepository.findRecentSearchLogsByUserId(userId, pageable);
    }

    /**
     * 获取当前用户搜索历史
     */
    @Transactional(readOnly = true)
    public List<SearchLog> getCurrentUserSearchHistory(int limit) {
        Long currentUserId = searchSecurityService.getCurrentUserId();
        return getUserSearchHistory(currentUserId, limit);
    }

    /**
     * 获取搜索统计信息
     */
    @Transactional(readOnly = true)
    public SearchStatistics getSearchStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        searchSecurityService.validateAdminAccess("查看搜索统计");

        long totalSearches = searchLogRepository.countByCreatedAtBetween(startDate, endDate);

        long keywordSearches = searchLogRepository.findByCreatedAtBetween(startDate, endDate)
                .stream()
                .filter(log -> log.getSearchType() == SearchLog.SearchType.KEYWORD)
                .count();

        long semanticSearches = searchLogRepository.findByCreatedAtBetween(startDate, endDate)
                .stream()
                .filter(log -> log.getSearchType() == SearchLog.SearchType.SEMANTIC)
                .count();

        long hybridSearches = searchLogRepository.findByCreatedAtBetween(startDate, endDate)
                .stream()
                .filter(log -> log.getSearchType() == SearchLog.SearchType.HYBRID)
                .count();

        Double averageResponseTime = searchLogRepository.calculateAverageResponseTime();

        return new SearchStatistics(
                totalSearches,
                keywordSearches,
                semanticSearches,
                hybridSearches,
                averageResponseTime != null ? averageResponseTime : 0.0
        );
    }

    /**
     * 获取热门搜索词
     */
    @Transactional(readOnly = true)
    public List<PopularSearchTerm> getPopularSearchTerms(int limit) {
        searchSecurityService.validateAdminAccess("查看热门搜索词统计");

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = searchLogRepository.findPopularSearchTerms(pageable);

        return results.stream()
                .map(row -> new PopularSearchTerm((String) row[0], ((Number) row[1]).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 删除搜索历史
     */
    public void deleteUserSearchHistory(Long userId) {
        // 使用统一的权限验证服务
        searchSecurityService.validateUserAccess(userId, "删除搜索历史");

        List<SearchLog> userSearchLogs = searchLogRepository.findByUserId(userId);
        searchLogRepository.deleteAll(userSearchLogs);
        log.info("用户搜索历史删除成功: userId={}", userId);
    }

    /**
     * 搜索统计信息类
     */
    public static class SearchStatistics {
        private final long totalSearches;
        private final long keywordSearches;
        private final long semanticSearches;
        private final long hybridSearches;
        private final double averageResponseTime;

        public SearchStatistics(long totalSearches, long keywordSearches,
                              long semanticSearches, long hybridSearches,
                              double averageResponseTime) {
            this.totalSearches = totalSearches;
            this.keywordSearches = keywordSearches;
            this.semanticSearches = semanticSearches;
            this.hybridSearches = hybridSearches;
            this.averageResponseTime = averageResponseTime;
        }

        // Getters
        public long getTotalSearches() { return totalSearches; }
        public long getKeywordSearches() { return keywordSearches; }
        public long getSemanticSearches() { return semanticSearches; }
        public long getHybridSearches() { return hybridSearches; }
        public double getAverageResponseTime() { return averageResponseTime; }
    }

    /**
     * 热门搜索词类
     */
    public static class PopularSearchTerm {
        private final String term;
        private final long count;

        public PopularSearchTerm(String term, long count) {
            this.term = term;
            this.count = count;
        }

        // Getters
        public String getTerm() { return term; }
        public long getCount() { return count; }
    }
}