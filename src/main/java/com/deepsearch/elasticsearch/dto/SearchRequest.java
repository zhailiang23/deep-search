package com.deepsearch.elasticsearch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.List;

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

    // 兼容方法 - 为了保持与现有代码的兼容性
    public String getQueryText() {
        return this.query;
    }

    public void setQueryText(String queryText) {
        this.query = queryText;
    }
}