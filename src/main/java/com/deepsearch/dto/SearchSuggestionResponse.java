package com.deepsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索建议响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionResponse {

    /**
     * 原始查询文本
     */
    private String originalQuery;

    /**
     * 查询扩展建议
     */
    private List<String> expansionSuggestions;

    /**
     * 查询缩小建议
     */
    private List<String> narrowingSuggestions;

    /**
     * 相关查询建议
     */
    private List<String> relatedQueries;

    /**
     * 拼写纠错建议
     */
    private List<String> spellingSuggestions;

    /**
     * 总建议数量
     */
    private Integer totalSuggestions;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;
}