package com.deepsearch.elasticsearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索请求参数")
public class SearchRequest {

    @NotBlank(message = "查询文本不能为空")
    @Schema(description = "查询文本", example = "银行产品")
    private String query;

    @Schema(description = "空间ID，用于权限控制", example = "space_001")
    private String spaceId;

    @Schema(description = "渠道列表，用于过滤特定渠道的内容", example = "[\"mobile\", \"web\"]")
    private List<String> channels;

    @Min(value = 0, message = "from必须大于等于0")
    @Schema(description = "分页起始位置", example = "0", defaultValue = "0")
    private int from = 0;

    @Min(value = 1, message = "size必须大于0")
    @Max(value = 100, message = "size不能超过100")
    @Schema(description = "分页大小", example = "10", defaultValue = "10")
    private int size = 10;

    @Min(value = 0, message = "关键词权重必须大于等于0")
    @Max(value = 10, message = "关键词权重不能超过10")
    @Schema(description = "关键词搜索权重（仅混合搜索使用）", example = "1.0", defaultValue = "1.0")
    private Float keywordWeight;

    @Min(value = 0, message = "向量权重必须大于等于0")
    @Max(value = 10, message = "向量权重不能超过10")
    @Schema(description = "向量搜索权重（仅混合搜索使用）", example = "2.0", defaultValue = "2.0")
    private Float vectorWeight;

    @Schema(description = "是否启用高亮", example = "true", defaultValue = "false")
    private boolean highlight = false;

    @Schema(description = "高亮前缀标签", example = "<mark>", defaultValue = "<em>")
    private String highlightPreTag = "<em>";

    @Schema(description = "高亮后缀标签", example = "</mark>", defaultValue = "</em>")
    private String highlightPostTag = "</em>";

    @Schema(description = "最小相似度阈值（仅向量搜索使用）", example = "0.7", defaultValue = "0.6")
    private Float minScore = 0.6f;

    @Schema(description = "搜索超时时间（秒）", example = "30", defaultValue = "30")
    private Integer timeoutSeconds = 30;

    @Schema(description = "搜索类型", example = "keyword", allowableValues = {"keyword", "vector", "hybrid"})
    private String searchType = "keyword";

    // 新增混合搜索相关字段

    @Schema(description = "新鲜度权重（影响按时间排序的重要性）", example = "0.1", defaultValue = "0.1")
    private Float freshnessWeight = 0.1f;

    @Schema(description = "质量权重（影响内容质量评分的重要性）", example = "0.1", defaultValue = "0.1")
    private Float qualityWeight = 0.1f;

    @Schema(description = "热度权重（影响文档热度评分的重要性）", example = "0.1", defaultValue = "0.1")
    private Float popularityWeight = 0.1f;

    @Schema(description = "是否启用智能权重自适应", example = "true", defaultValue = "false")
    private boolean enableAdaptiveWeights = false;

    @Schema(description = "排序字段", example = "relevance", allowableValues = {"relevance", "created_at", "updated_at", "title"})
    private String sortBy = "relevance";

    @Schema(description = "排序方向", example = "desc", allowableValues = {"asc", "desc"})
    private String sortOrder = "desc";

    @Schema(description = "是否启用去重", example = "true", defaultValue = "false")
    private boolean enableDeduplication = false;

    @Schema(description = "分类过滤器（多个分类用逗号分隔）", example = "product,service")
    private String categoryFilter;

    @Schema(description = "内容类型过滤器", example = "[\"article\", \"guide\"]")
    private List<String> contentTypeFilter;

    @Schema(description = "创建时间范围过滤 - 开始时间（ISO格式）", example = "2024-01-01T00:00:00")
    private String createdAfter;

    @Schema(description = "创建时间范围过滤 - 结束时间（ISO格式）", example = "2024-12-31T23:59:59")
    private String createdBefore;

    @Schema(description = "标签过滤器", example = "[\"重要\", \"推荐\"]")
    private List<String> tagFilter;

    @Schema(description = "自定义过滤器（键值对形式）")
    private Map<String, Object> customFilters;

    @Schema(description = "是否包含聚合信息", example = "false", defaultValue = "false")
    private boolean includeAggregations = false;

    @Schema(description = "聚合字段列表", example = "[\"category\", \"content_type\"]")
    private List<String> aggregationFields;

    @Schema(description = "搜索建议数量限制", example = "5", defaultValue = "0")
    private int maxSuggestions = 0;

    @Schema(description = "是否启用搜索日志记录", example = "true", defaultValue = "true")
    private boolean enableLogging = true;

    @Schema(description = "用户上下文信息（用于个性化搜索）")
    private Map<String, Object> userContext;

    // 便捷构造方法
    public SearchRequest(String query) {
        this.query = query;
    }

    public SearchRequest(String query, int from, int size) {
        this.query = query;
        this.from = from;
        this.size = size;
    }

    public SearchRequest(String query, String spaceId, List<String> channels, int from, int size) {
        this.query = query;
        this.spaceId = spaceId;
        this.channels = channels;
        this.from = from;
        this.size = size;
    }

    // 便捷方法用于设置混合搜索权重
    public SearchRequest withHybridWeights(float keywordWeight, float vectorWeight) {
        this.keywordWeight = keywordWeight;
        this.vectorWeight = vectorWeight;
        this.searchType = "hybrid";
        return this;
    }

    // 便捷方法用于设置相关性权重
    public SearchRequest withRelevanceWeights(float freshnessWeight, float qualityWeight, float popularityWeight) {
        this.freshnessWeight = freshnessWeight;
        this.qualityWeight = qualityWeight;
        this.popularityWeight = popularityWeight;
        return this;
    }

    // 便捷方法用于设置过滤器
    public SearchRequest withFilters(String categoryFilter, List<String> contentTypeFilter) {
        this.categoryFilter = categoryFilter;
        this.contentTypeFilter = contentTypeFilter;
        return this;
    }

    // 便捷方法用于设置时间范围
    public SearchRequest withTimeRange(String createdAfter, String createdBefore) {
        this.createdAfter = createdAfter;
        this.createdBefore = createdBefore;
        return this;
    }

    // 便捷方法用于设置排序
    public SearchRequest withSort(String sortBy, String sortOrder) {
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        return this;
    }

    // 兼容方法 - 为了保持与现有代码的兼容性
    public String getQueryText() {
        return this.query;
    }

    public void setQueryText(String queryText) {
        this.query = queryText;
    }

    // 验证方法
    public boolean isValid() {
        return query != null && !query.trim().isEmpty() &&
               from >= 0 && size > 0 && size <= 100;
    }

    // 获取权重总和用于归一化
    public float getTotalWeight() {
        float total = 0.0f;
        if (keywordWeight != null) total += keywordWeight;
        if (vectorWeight != null) total += vectorWeight;
        if (freshnessWeight != null) total += freshnessWeight;
        if (qualityWeight != null) total += qualityWeight;
        if (popularityWeight != null) total += popularityWeight;
        return total > 0 ? total : 1.0f;
    }

    // 检查是否为混合搜索
    public boolean isHybridSearch() {
        return "hybrid".equalsIgnoreCase(searchType) ||
               (keywordWeight != null && keywordWeight > 0 && vectorWeight != null && vectorWeight > 0);
    }

    // 检查是否需要聚合
    public boolean needsAggregation() {
        return includeAggregations && aggregationFields != null && !aggregationFields.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("SearchRequest{query='%s', type='%s', from=%d, size=%d, weights=[%s/%s]}",
            query, searchType, from, size, keywordWeight, vectorWeight);
    }
}