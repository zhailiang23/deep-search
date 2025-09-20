package com.deepsearch.dto;

/**
 * 搜索建议类型枚举
 */
public enum SuggestionType {

    /**
     * 前缀匹配建议（基于Trie树）
     */
    PREFIX_MATCH,

    /**
     * 热门搜索建议
     */
    POPULAR,

    /**
     * 个性化建议（基于用户历史）
     */
    PERSONALIZED,

    /**
     * 语义相关建议
     */
    SEMANTIC,

    /**
     * 拼写纠错建议
     */
    SPELLING_CORRECTION,

    /**
     * 搜索范围扩展建议
     */
    EXPANSION,

    /**
     * 搜索范围缩小建议
     */
    NARROWING,

    /**
     * 相关查询建议
     */
    RELATED_QUERY
}