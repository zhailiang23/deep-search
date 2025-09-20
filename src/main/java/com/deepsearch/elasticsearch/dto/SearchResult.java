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

    // 新增字段 - 增强搜索结果信息

    @Schema(description = "搜索性能统计")
    private SearchPerformanceStats performanceStats;

    @Schema(description = "搜索权重信息")
    private SearchWeightInfo weightInfo;

    @Schema(description = "结果分类统计")
    private Map<String, Integer> categoryDistribution;

    @Schema(description = "相关度分数分布")
    private ScoreDistribution scoreDistribution;

    @Schema(description = "搜索建议详情")
    private List<SearchSuggestion> detailedSuggestions;

    @Schema(description = "是否已去重", example = "false")
    private boolean deduplicated = false;

    @Schema(description = "去重前结果数量", example = "200")
    private Integer originalResultCount;

    @Schema(description = "搜索质量指标")
    private SearchQualityMetrics qualityMetrics;

    @Schema(description = "相关搜索推荐")
    private List<String> relatedQueries;

    @Schema(description = "搜索过滤器应用情况")
    private Map<String, Object> appliedFilters;

    @Schema(description = "错误信息或警告")
    private List<String> warnings;

    @Schema(description = "扩展查询列表")
    private List<String> expandedQueries;

    @Schema(description = "查询类型", example = "PRODUCT_QUERY")
    private String queryType;

    @Schema(description = "元数据信息")
    private Map<String, Object> metadata;

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

        // 初始化性能统计
        this.performanceStats = new SearchPerformanceStats(responseTimeMs, totalResults);
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
     * 设置元数据信息
     */
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 添加元数据信息
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * 获取总结果数（别名方法，兼容性）
     */
    public int getTotalHits() {
        return totalResults;
    }

    /**
     * 设置总结果数（别名方法，兼容性）
     */
    public void setTotalHits(int totalHits) {
        this.totalResults = totalHits;
    }

    /**
     * 获取响应时间（别名方法，兼容性）
     */
    public long getResponseTime() {
        return responseTimeMs;
    }

    /**
     * 设置搜索建议
     */
    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    /**
     * 添加详细搜索建议
     */
    public void addDetailedSuggestion(String text, String type, float confidence) {
        if (this.detailedSuggestions == null) {
            this.detailedSuggestions = new java.util.ArrayList<>();
        }
        this.detailedSuggestions.add(new SearchSuggestion(text, type, confidence));
    }

    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new java.util.ArrayList<>();
        }
        this.warnings.add(warning);
    }

    /**
     * 设置权重信息
     */
    public void setWeightInfo(float keywordWeight, float vectorWeight, float freshnessWeight, 
                             float qualityWeight, float popularityWeight) {
        this.weightInfo = new SearchWeightInfo(keywordWeight, vectorWeight, 
                                              freshnessWeight, qualityWeight, popularityWeight);
    }

    /**
     * 设置分类分布
     */
    public void setCategoryDistribution(Map<String, Integer> distribution) {
        this.categoryDistribution = distribution;
    }

    /**
     * 设置分数分布
     */
    public void setScoreDistribution(float minScore, float maxScore, float avgScore) {
        this.scoreDistribution = new ScoreDistribution(minScore, maxScore, avgScore);
    }

    /**
     * 设置质量指标
     */
    public void setQualityMetrics(float precision, float recall, float coverage) {
        this.qualityMetrics = new SearchQualityMetrics(precision, recall, coverage);
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
     * 获取结果摘要信息
     */
    public String getResultSummary() {
        return String.format("找到 %d 条结果，用时 %d 毫秒", totalResults, responseTimeMs);
    }

    /**
     * 搜索性能统计内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索性能统计")
    public static class SearchPerformanceStats {
        @Schema(description = "响应时间（毫秒）", example = "245")
        private long responseTime;

        @Schema(description = "结果数量", example = "156")
        private int resultCount;

        @Schema(description = "每毫秒处理的结果数", example = "0.64")
        private double throughput;

        @Schema(description = "性能等级", example = "EXCELLENT", allowableValues = {"EXCELLENT", "GOOD", "AVERAGE", "POOR"})
        private String performanceLevel;

        public SearchPerformanceStats(long responseTime, int resultCount) {
            this.responseTime = responseTime;
            this.resultCount = resultCount;
            this.throughput = responseTime > 0 ? (double) resultCount / responseTime : 0.0;
            this.performanceLevel = calculatePerformanceLevel(responseTime);
        }

        private String calculatePerformanceLevel(long responseTime) {
            if (responseTime < 100) return "EXCELLENT";
            if (responseTime < 200) return "GOOD";
            if (responseTime < 500) return "AVERAGE";
            return "POOR";
        }
    }

    /**
     * 搜索权重信息内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索权重信息")
    public static class SearchWeightInfo {
        @Schema(description = "关键词权重", example = "1.0")
        private float keywordWeight;

        @Schema(description = "向量权重", example = "2.0")
        private float vectorWeight;

        @Schema(description = "新鲜度权重", example = "0.1")
        private float freshnessWeight;

        @Schema(description = "质量权重", example = "0.1")
        private float qualityWeight;

        @Schema(description = "热度权重", example = "0.1")
        private float popularityWeight;

        @Schema(description = "权重总和", example = "3.3")
        private float totalWeight;

        public SearchWeightInfo(float keywordWeight, float vectorWeight, float freshnessWeight,
                               float qualityWeight, float popularityWeight) {
            this.keywordWeight = keywordWeight;
            this.vectorWeight = vectorWeight;
            this.freshnessWeight = freshnessWeight;
            this.qualityWeight = qualityWeight;
            this.popularityWeight = popularityWeight;
            this.totalWeight = keywordWeight + vectorWeight + freshnessWeight + qualityWeight + popularityWeight;
        }
    }

    /**
     * 分数分布内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "相关度分数分布")
    public static class ScoreDistribution {
        @Schema(description = "最小分数", example = "0.1")
        private float minScore;

        @Schema(description = "最大分数", example = "0.95")
        private float maxScore;

        @Schema(description = "平均分数", example = "0.67")
        private float avgScore;

        @Schema(description = "分数范围", example = "0.85")
        private float scoreRange;

        public ScoreDistribution(float minScore, float maxScore, float avgScore) {
            this.minScore = minScore;
            this.maxScore = maxScore;
            this.avgScore = avgScore;
            this.scoreRange = maxScore - minScore;
        }
    }

    /**
     * 搜索建议内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索建议详情")
    public static class SearchSuggestion {
        @Schema(description = "建议文本", example = "银行理财产品")
        private String text;

        @Schema(description = "建议类型", example = "CORRECTION", allowableValues = {"CORRECTION", "EXPANSION", "RELATED"})
        private String type;

        @Schema(description = "置信度", example = "0.85")
        private float confidence;

        @Schema(description = "预期结果数量", example = "42")
        private Integer expectedResults;

        // 添加三参数构造函数
        public SearchSuggestion(String text, String type, float confidence) {
            this.text = text;
            this.type = type;
            this.confidence = confidence;
        }
    }

    /**
     * 搜索质量指标内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "搜索质量指标")
    public static class SearchQualityMetrics {
        @Schema(description = "精确率", example = "0.85")
        private float precision;

        @Schema(description = "召回率", example = "0.75")
        private float recall;

        @Schema(description = "覆盖率", example = "0.90")
        private float coverage;

        @Schema(description = "F1分数", example = "0.80")
        private float f1Score;

        public SearchQualityMetrics(float precision, float recall, float coverage) {
            this.precision = precision;
            this.recall = recall;
            this.coverage = coverage;
            this.f1Score = precision + recall > 0 ? 2 * (precision * recall) / (precision + recall) : 0;
        }
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