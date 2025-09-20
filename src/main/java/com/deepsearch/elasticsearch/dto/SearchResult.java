package com.deepsearch.elasticsearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索结果响应")
public class SearchResult {

    @Schema(description = "查询文本", example = "银行产品")
    private String query;

    @Schema(description = "搜索结果文档列表")
    private List<DocumentIndex> documents;

    @Schema(description = "结果总数", example = "156")
    private int totalResults;

    @Schema(description = "当前页码", example = "0")
    private int pageNumber;

    @Schema(description = "每页大小", example = "10")
    private int pageSize;

    @Schema(description = "响应时间（毫秒）", example = "245")
    private long responseTimeMs;

    @Schema(description = "搜索类型", example = "hybrid", allowableValues = {"keyword", "vector", "hybrid"})
    private String searchType;

    @Schema(description = "搜索时间戳")
    private LocalDateTime timestamp;

    @Schema(description = "是否有更多结果", example = "true")
    private boolean hasMore;

    @Schema(description = "总页数", example = "16")
    private int totalPages;

    @Schema(description = "高亮片段")
    private Map<String, List<String>> highlights;

    @Schema(description = "聚合结果")
    private Map<String, Object> aggregations;

    @Schema(description = "搜索建议")
    private List<String> suggestions;

    // 主要构造方法
    public SearchResult(String query, List<DocumentIndex> documents, int totalResults,
                       int pageNumber, int pageSize, long responseTimeMs, String searchType) {
        this.query = query;
        this.documents = documents;
        this.totalResults = totalResults;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.responseTimeMs = responseTimeMs;
        this.searchType = searchType;
        this.timestamp = LocalDateTime.now();

        // 计算分页信息
        this.totalPages = (int) Math.ceil((double) totalResults / pageSize);
        this.hasMore = (pageNumber + 1) * pageSize < totalResults;
    }

    // 便捷构造方法
    public SearchResult(String query, List<DocumentIndex> documents, long responseTimeMs, String searchType) {
        this(query, documents, documents.size(), 0, documents.size(), responseTimeMs, searchType);
    }

    /**
     * 添加高亮信息
     */
    public void addHighlight(String field, List<String> fragments) {
        if (this.highlights == null) {
            this.highlights = new java.util.HashMap<>();
        }
        this.highlights.put(field, fragments);
    }

    /**
     * 添加聚合结果
     */
    public void addAggregation(String name, Object value) {
        if (this.aggregations == null) {
            this.aggregations = new java.util.HashMap<>();
        }
        this.aggregations.put(name, value);
    }

    /**
     * 设置搜索建议
     */
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * 获取当前页的第一个结果的索引
     */
    public int getStartIndex() {
        return pageNumber * pageSize;
    }

    /**
     * 获取当前页的最后一个结果的索引
     */
    public int getEndIndex() {
        return Math.min(getStartIndex() + pageSize, totalResults);
    }

    /**
     * 检查是否为空结果
     */
    public boolean isEmpty() {
        return documents == null || documents.isEmpty();
    }

    /**
     * 获取搜索效果统计信息
     */
    @Schema(description = "搜索性能统计")
    public static class SearchStats {
        private final long responseTime;
        private final int totalResults;
        private final String searchType;
        private final LocalDateTime timestamp;

        public SearchStats(long responseTime, int totalResults, String searchType) {
            this.responseTime = responseTime;
            this.totalResults = totalResults;
            this.searchType = searchType;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public long getResponseTime() { return responseTime; }
        public int getTotalResults() { return totalResults; }
        public String getSearchType() { return searchType; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}