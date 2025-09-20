package com.deepsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索建议项DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Suggestion {

    /**
     * 建议文本
     */
    private String text;

    /**
     * 建议类型
     */
    private SuggestionType type;

    /**
     * 相关度评分（0.0-1.0）
     */
    private Float score;

    /**
     * 建议来源（可选）
     */
    private String source;

    /**
     * 额外元数据（可选）
     */
    private Object metadata;

    /**
     * 创建一个简单的建议项
     */
    public static Suggestion of(String text, SuggestionType type, Float score) {
        return new Suggestion(text, type, score, null, null);
    }
}