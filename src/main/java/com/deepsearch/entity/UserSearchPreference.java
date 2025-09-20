package com.deepsearch.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户搜索偏好实体类
 * 用于存储用户的搜索行为模式、偏好分类、点击习惯等信息
 * 支持个性化搜索和智能推荐
 */
@Entity
@Table(name = "user_search_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserSearchPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 偏好分类权重 (JSON格式)
     * 例如: {"产品": 0.8, "服务": 0.6, "活动": 0.3}
     */
    @Column(name = "preferred_categories", columnDefinition = "JSON")
    private String preferredCategories;

    /**
     * 搜索历史记录 (JSON格式)
     * 存储最近的搜索查询、时间戳、点击情况等
     */
    @Column(name = "search_history", columnDefinition = "JSON")
    private String searchHistory;

    /**
     * 点击模式分析 (JSON格式)
     * 分析用户点击习惯，如偏好的内容类型、位置偏好等
     */
    @Column(name = "click_patterns", columnDefinition = "JSON")
    private String clickPatterns;

    /**
     * 银行业务场景偏好 (JSON格式)
     * 记录用户在不同银行场景下的偏好
     * 例如: {"mobile_banking": 0.9, "internet_banking": 0.7, "counter": 0.2}
     */
    @Column(name = "scene_preferences", columnDefinition = "JSON")
    private String scenePreferences;

    /**
     * 搜索时间偏好模式 (JSON格式)
     * 分析用户搜索的时间规律
     */
    @Column(name = "temporal_patterns", columnDefinition = "JSON")
    private String temporalPatterns;

    /**
     * 关键词偏好强度
     * 用户对关键词搜索vs语义搜索的偏好程度 (0.0-1.0)
     */
    @Column(name = "keyword_preference_strength")
    private Float keywordPreferenceStrength = 0.5f;

    /**
     * 语义搜索偏好强度
     * 用户对语义搜索的偏好程度 (0.0-1.0)
     */
    @Column(name = "semantic_preference_strength")
    private Float semanticPreferenceStrength = 0.5f;

    /**
     * 新鲜度敏感度
     * 用户对内容新鲜度的敏感程度 (0.0-1.0)
     */
    @Column(name = "freshness_sensitivity")
    private Float freshnessSensitivity = 0.3f;

    /**
     * 热度敏感度
     * 用户对内容热度的敏感程度 (0.0-1.0)
     */
    @Column(name = "popularity_sensitivity")
    private Float popularitySensitivity = 0.2f;

    /**
     * 个性化强度
     * 个性化推荐的强度 (0.0-1.0)
     */
    @Column(name = "personalization_strength")
    private Float personalizationStrength = 0.7f;

    /**
     * 总搜索次数
     */
    @Column(name = "total_searches")
    private Long totalSearches = 0L;

    /**
     * 总点击次数
     */
    @Column(name = "total_clicks")
    private Long totalClicks = 0L;

    /**
     * 平均搜索会话时长（秒）
     */
    @Column(name = "avg_session_duration")
    private Float avgSessionDuration;

    /**
     * 最后搜索时间
     */
    @Column(name = "last_search_time")
    private LocalDateTime lastSearchTime;

    /**
     * 是否启用个性化
     */
    @Column(name = "personalization_enabled")
    private Boolean personalizationEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 用于JSON序列化的工具
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 便捷方法：获取偏好分类Map
    @SuppressWarnings("unchecked")
    public Map<String, Double> getPreferredCategoriesMap() {
        if (preferredCategories == null || preferredCategories.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(preferredCategories, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    // 便捷方法：设置偏好分类Map
    public void setPreferredCategoriesMap(Map<String, Double> categories) {
        try {
            this.preferredCategories = objectMapper.writeValueAsString(categories);
        } catch (JsonProcessingException e) {
            this.preferredCategories = "{}";
        }
    }

    // 便捷方法：获取搜索历史List
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSearchHistoryList() {
        if (searchHistory == null || searchHistory.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(searchHistory, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    // 便捷方法：设置搜索历史List
    public void setSearchHistoryList(List<Map<String, Object>> history) {
        try {
            this.searchHistory = objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException e) {
            this.searchHistory = "[]";
        }
    }

    // 便捷方法：获取点击模式Map
    @SuppressWarnings("unchecked")
    public Map<String, Object> getClickPatternsMap() {
        if (clickPatterns == null || clickPatterns.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(clickPatterns, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    // 便捷方法：设置点击模式Map
    public void setClickPatternsMap(Map<String, Object> patterns) {
        try {
            this.clickPatterns = objectMapper.writeValueAsString(patterns);
        } catch (JsonProcessingException e) {
            this.clickPatterns = "{}";
        }
    }

    // 便捷方法：获取场景偏好Map
    @SuppressWarnings("unchecked")
    public Map<String, Double> getScenePreferencesMap() {
        if (scenePreferences == null || scenePreferences.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(scenePreferences, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    // 便捷方法：设置场景偏好Map
    public void setScenePreferencesMap(Map<String, Double> preferences) {
        try {
            this.scenePreferences = objectMapper.writeValueAsString(preferences);
        } catch (JsonProcessingException e) {
            this.scenePreferences = "{}";
        }
    }

    // 便捷方法：获取时间模式Map
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTemporalPatternsMap() {
        if (temporalPatterns == null || temporalPatterns.isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(temporalPatterns, Map.class);
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    // 便捷方法：设置时间模式Map
    public void setTemporalPatternsMap(Map<String, Object> patterns) {
        try {
            this.temporalPatterns = objectMapper.writeValueAsString(patterns);
        } catch (JsonProcessingException e) {
            this.temporalPatterns = "{}";
        }
    }

    // 计算点击率
    public Double getClickThroughRate() {
        if (totalSearches == null || totalSearches == 0) {
            return 0.0;
        }
        return (totalClicks != null ? totalClicks.doubleValue() : 0.0) / totalSearches.doubleValue();
    }

    // 检查是否为新用户（搜索次数少于10次）
    public boolean isNewUser() {
        return totalSearches == null || totalSearches < 10;
    }

    // 检查是否为活跃用户（最近7天内有搜索）
    public boolean isActiveUser() {
        if (lastSearchTime == null) {
            return false;
        }
        return lastSearchTime.isAfter(LocalDateTime.now().minusDays(7));
    }

    // 获取用户搜索偏好类型
    public String getPreferenceType() {
        if (isNewUser()) {
            return "NEW_USER";
        }

        double semanticRatio = semanticPreferenceStrength != null ? semanticPreferenceStrength : 0.5;
        double keywordRatio = keywordPreferenceStrength != null ? keywordPreferenceStrength : 0.5;

        if (semanticRatio > 0.7) {
            return "SEMANTIC_ORIENTED";
        } else if (keywordRatio > 0.7) {
            return "KEYWORD_ORIENTED";
        } else {
            return "BALANCED";
        }
    }

    // 工厂方法：创建默认偏好设置
    public static UserSearchPreference createDefaultPreferences(Long userId) {
        UserSearchPreference preference = new UserSearchPreference();
        preference.setUserId(userId);
        preference.setPreferredCategories("{}");
        preference.setSearchHistory("[]");
        preference.setClickPatterns("{}");
        preference.setScenePreferences("{}");
        preference.setTemporalPatterns("{}");
        preference.setKeywordPreferenceStrength(0.5f);
        preference.setSemanticPreferenceStrength(0.5f);
        preference.setFreshnessSensitivity(0.3f);
        preference.setPopularitySensitivity(0.2f);
        preference.setPersonalizationStrength(0.5f); // 新用户从中等个性化开始
        preference.setTotalSearches(0L);
        preference.setTotalClicks(0L);
        preference.setPersonalizationEnabled(true);
        return preference;
    }

    @Override
    public String toString() {
        return String.format("UserSearchPreference{userId=%d, searches=%d, clicks=%d, preference=%s, personalization=%.2f}",
            userId, totalSearches, totalClicks, getPreferenceType(), personalizationStrength);
    }
}