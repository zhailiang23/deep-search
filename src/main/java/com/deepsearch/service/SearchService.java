package com.deepsearch.service;

import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.dto.SearchRequestDto;
import com.deepsearch.dto.SearchResponseDto;
import com.deepsearch.entity.Document;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.entity.User;
import com.deepsearch.exception.BadRequestException;
import com.deepsearch.exception.ResourceNotFoundException;
import com.deepsearch.repository.DocumentRepository;
import com.deepsearch.repository.SearchLogRepository;
import com.deepsearch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索业务服务
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final DocumentRepository documentRepository;
    private final SearchLogRepository searchLogRepository;
    private final UserRepository userRepository;

    /**
     * 执行搜索
     */
    public SearchResponseDto search(SearchRequestDto searchRequest) {
        long startTime = System.currentTimeMillis();

        try {
            List<DocumentResponseDto> results;
            int totalResults;

            // 根据搜索类型执行不同的搜索策略
            switch (searchRequest.getSearchType()) {
                case KEYWORD:
                    results = performKeywordSearch(searchRequest);
                    break;
                case SEMANTIC:
                    results = performSemanticSearch(searchRequest);
                    break;
                case HYBRID:
                    results = performHybridSearch(searchRequest);
                    break;
                default:
                    throw new BadRequestException("不支持的搜索类型: " + searchRequest.getSearchType());
            }

            totalResults = results.size();

            // 分页处理
            int pageNumber = searchRequest.getPageNumber();
            int pageSize = searchRequest.getPageSize();
            int startIndex = pageNumber * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalResults);

            List<DocumentResponseDto> pagedResults = results.subList(startIndex, endIndex);

            long endTime = System.currentTimeMillis();
            long responseTime = endTime - startTime;

            // 记录搜索日志
            recordSearchLog(searchRequest, totalResults, (int) responseTime);

            return new SearchResponseDto(
                    searchRequest.getQueryText(),
                    pagedResults,
                    totalResults,
                    pageNumber,
                    pageSize,
                    responseTime
            );

        } catch (Exception e) {
            log.error("搜索执行失败: {}", searchRequest.getQueryText(), e);
            throw new BadRequestException("搜索执行失败: " + e.getMessage());
        }
    }

    /**
     * 关键词搜索
     */
    private List<DocumentResponseDto> performKeywordSearch(SearchRequestDto searchRequest) {
        String queryText = searchRequest.getQueryText().toLowerCase();

        // 搜索标题和内容匹配的文档
        List<Document> titleMatches = documentRepository.findByTitleContainingIgnoreCase(queryText);
        List<Document> contentMatches = documentRepository.findByContentContainingIgnoreCase(queryText);

        // 合并结果并去重
        return titleMatches.stream()
                .filter(doc -> doc.getStatus() == Document.Status.INDEXED)
                .map(DocumentResponseDto::new)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 语义搜索（暂时使用关键词搜索实现，后续可以集成向量搜索）
     */
    private List<DocumentResponseDto> performSemanticSearch(SearchRequestDto searchRequest) {
        // TODO: 后续集成向量搜索引擎
        // 目前先使用关键词搜索作为替代
        log.info("语义搜索暂时使用关键词搜索实现: {}", searchRequest.getQueryText());
        return performKeywordSearch(searchRequest);
    }

    /**
     * 混合搜索
     */
    private List<DocumentResponseDto> performHybridSearch(SearchRequestDto searchRequest) {
        // 结合关键词搜索和语义搜索的结果
        List<DocumentResponseDto> keywordResults = performKeywordSearch(searchRequest);
        List<DocumentResponseDto> semanticResults = performSemanticSearch(searchRequest);

        // 合并并去重结果
        return keywordResults.stream()
                .collect(Collectors.toList());
    }

    /**
     * 记录搜索日志
     */
    private void recordSearchLog(SearchRequestDto searchRequest, int resultCount, int responseTime) {
        try {
            Long userId = getCurrentUserIdOrNull();

            SearchLog searchLog = new SearchLog();
            searchLog.setQueryText(searchRequest.getQueryText());
            searchLog.setUserId(userId);
            searchLog.setSearchType(searchRequest.getSearchType());
            searchLog.setResultCount(resultCount);
            searchLog.setResponseTimeMs(responseTime);

            searchLogRepository.save(searchLog);
            log.info("搜索日志记录成功: {} - {} 条结果", searchRequest.getQueryText(), resultCount);
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
        // 检查权限：只有用户本人或管理员可以查看搜索历史
        Long currentUserId = getCurrentUserIdOrNull();
        if (!isAdmin() && (currentUserId == null || !currentUserId.equals(userId))) {
            throw new BadRequestException("无权访问其他用户的搜索历史");
        }

        Pageable pageable = PageRequest.of(0, limit);
        return searchLogRepository.findRecentSearchLogsByUserId(userId, pageable);
    }

    /**
     * 获取当前用户搜索历史
     */
    @Transactional(readOnly = true)
    public List<SearchLog> getCurrentUserSearchHistory(int limit) {
        Long currentUserId = getCurrentUserId();
        return getUserSearchHistory(currentUserId, limit);
    }

    /**
     * 获取搜索统计信息
     */
    @Transactional(readOnly = true)
    public SearchStatistics getSearchStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        if (!isAdmin()) {
            throw new BadRequestException("无权访问搜索统计信息");
        }

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
        if (!isAdmin()) {
            throw new BadRequestException("无权访问热门搜索词统计");
        }

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
        // 检查权限：只有用户本人或管理员可以删除搜索历史
        Long currentUserId = getCurrentUserIdOrNull();
        if (!isAdmin() && (currentUserId == null || !currentUserId.equals(userId))) {
            throw new BadRequestException("无权删除其他用户的搜索历史");
        }

        List<SearchLog> userSearchLogs = searchLogRepository.findByUserId(userId);
        searchLogRepository.deleteAll(userSearchLogs);
        log.info("用户搜索历史删除成功: userId={}", userId);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("用户未认证");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("用户", "用户名", username));

        return user.getId();
    }

    /**
     * 获取当前用户ID（允许为null）
     */
    private Long getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null; // 允许匿名搜索
        }
    }

    /**
     * 检查当前用户是否为管理员
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
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