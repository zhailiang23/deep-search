package com.deepsearch.service;

import com.deepsearch.entity.SearchLog;
import com.deepsearch.entity.UserSearchPreference;
import com.deepsearch.repository.SearchLogRepository;
import com.deepsearch.repository.UserSearchPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为分析服务
 * 负责分析用户搜索行为，建立用户画像，支持个性化推荐
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorService {

    private final SearchLogRepository searchLogRepository;
    private final UserSearchPreferenceRepository preferenceRepository;

    /**
     * 获取用户画像
     */
    public UserProfile getUserProfile(Long userId) {
        log.debug("获取用户画像: userId={}", userId);

        UserSearchPreference preference = preferenceRepository.findByUserId(userId)
            .orElse(UserSearchPreference.createDefaultPreferences(userId));

        List<SearchLog> recentSearches = getRecentSearches(userId, 100);

        return buildUserProfile(preference, recentSearches);
    }

    /**
     * 记录搜索行为并更新用户偏好
     */
    @Transactional
    public void recordSearchBehavior(Long userId, String query, SearchLog.SearchType searchType,
                                   int resultCount, int responseTime) {
        log.debug("记录搜索行为: userId={}, query={}, type={}", userId, query, searchType);

        // 1. 获取或创建用户偏好
        UserSearchPreference preference = preferenceRepository.findByUserId(userId)
            .orElse(UserSearchPreference.createDefaultPreferences(userId));

        // 2. 更新搜索统计
        updateSearchStatistics(preference, query, searchType);

        // 3. 更新搜索历史
        updateSearchHistory(preference, query, searchType, resultCount);

        // 4. 调整搜索偏好权重
        adjustSearchPreferences(preference, searchType, resultCount);

        // 5. 分析时间模式
        analyzeTemporalPatterns(preference);

        // 6. 保存更新后的偏好
        preference.setLastSearchTime(LocalDateTime.now());
        preferenceRepository.save(preference);

        log.debug("用户偏好已更新: {}", preference);
    }

    /**
     * 记录点击行为
     */
    @Transactional
    public void recordClickBehavior(Long userId, String documentId, int position, String category) {
        log.debug("记录点击行为: userId={}, docId={}, position={}", userId, documentId, position);

        UserSearchPreference preference = preferenceRepository.findByUserId(userId)
            .orElse(UserSearchPreference.createDefaultPreferences(userId));

        // 更新点击统计
        preference.setTotalClicks(preference.getTotalClicks() + 1);

        // 更新点击模式
        updateClickPatterns(preference, position, category);

        // 更新分类偏好
        updateCategoryPreferences(preference, category);

        preferenceRepository.save(preference);
    }

    /**
     * 分析用户搜索意图
     */
    public SearchIntent analyzeSearchIntent(Long userId, String query) {
        UserProfile profile = getUserProfile(userId);

        SearchIntent intent = new SearchIntent();
        intent.setQuery(query);
        intent.setUserId(userId);

        // 基于历史行为分析意图
        if (profile.hasPreferenceForCategory("产品")) {
            intent.setPrimaryIntent("PRODUCT_INQUIRY");
        } else if (profile.hasPreferenceForCategory("服务")) {
            intent.setPrimaryIntent("SERVICE_INQUIRY");
        } else if (query.contains("如何") || query.contains("怎么")) {
            intent.setPrimaryIntent("HOW_TO_INQUIRY");
        } else {
            intent.setPrimaryIntent("GENERAL_INQUIRY");
        }

        // 预测用户偏好的搜索类型
        intent.setPreferredSearchType(predictPreferredSearchType(profile, query));

        // 设置个性化权重
        intent.setPersonalizationWeight(profile.getPersonalizationStrength());

        return intent;
    }

    /**
     * 获取个性化权重配置
     */
    public PersonalizationWeights getPersonalizationWeights(Long userId) {
        UserSearchPreference preference = preferenceRepository.findByUserId(userId)
            .orElse(UserSearchPreference.createDefaultPreferences(userId));

        PersonalizationWeights weights = new PersonalizationWeights();
        weights.setKeywordWeight(preference.getKeywordPreferenceStrength());
        weights.setSemanticWeight(preference.getSemanticPreferenceStrength());
        weights.setFreshnessWeight(preference.getFreshnessSensitivity());
        weights.setPopularityWeight(preference.getPopularitySensitivity());
        weights.setPersonalizationStrength(preference.getPersonalizationStrength());

        return weights;
    }

    /**
     * 批量分析用户行为模式
     */
    public void analyzeUserBehaviorPatterns() {
        log.info("开始批量分析用户行为模式");

        List<UserSearchPreference> activeUsers = preferenceRepository
            .findActiveUsers(LocalDateTime.now().minusDays(30));

        for (UserSearchPreference preference : activeUsers) {
            try {
                analyzeAndUpdateUserPattern(preference);
            } catch (Exception e) {
                log.error("分析用户{}行为模式失败", preference.getUserId(), e);
            }
        }

        log.info("完成用户行为模式分析，处理用户数: {}", activeUsers.size());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取用户最近搜索记录
     */
    private List<SearchLog> getRecentSearches(Long userId, int limit) {
        return searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 构建用户画像
     */
    private UserProfile buildUserProfile(UserSearchPreference preference, List<SearchLog> recentSearches) {
        UserProfile profile = new UserProfile();
        profile.setUserId(preference.getUserId());
        profile.setPreferences(preference.getPreferredCategoriesMap());
        profile.setRecentSearches(extractRecentQueries(recentSearches));
        profile.setPersonalizationStrength(preference.getPersonalizationStrength());
        profile.setPreferenceType(preference.getPreferenceType());
        profile.setClickThroughRate(preference.getClickThroughRate());
        profile.setTotalSearches(preference.getTotalSearches());
        profile.setScenePreferences(preference.getScenePreferencesMap());

        return profile;
    }

    /**
     * 提取最近查询文本
     */
    private List<String> extractRecentQueries(List<SearchLog> searchLogs) {
        return searchLogs.stream()
            .map(SearchLog::getQueryText)
            .limit(20)
            .collect(Collectors.toList());
    }

    /**
     * 更新搜索统计信息
     */
    private void updateSearchStatistics(UserSearchPreference preference, String query, SearchLog.SearchType searchType) {
        preference.setTotalSearches(preference.getTotalSearches() + 1);
        preference.setLastSearchTime(LocalDateTime.now());
    }

    /**
     * 更新搜索历史
     */
    private void updateSearchHistory(UserSearchPreference preference, String query,
                                   SearchLog.SearchType searchType, int resultCount) {
        List<Map<String, Object>> history = preference.getSearchHistoryList();

        Map<String, Object> searchRecord = new HashMap<>();
        searchRecord.put("query", query);
        searchRecord.put("type", searchType.toString());
        searchRecord.put("resultCount", resultCount);
        searchRecord.put("timestamp", LocalDateTime.now().toString());

        history.add(0, searchRecord); // 添加到开头

        // 保持历史记录数量在合理范围内
        if (history.size() > 50) {
            history = history.subList(0, 50);
        }

        preference.setSearchHistoryList(history);
    }

    /**
     * 调整搜索偏好权重
     */
    private void adjustSearchPreferences(UserSearchPreference preference, SearchLog.SearchType searchType, int resultCount) {
        float learningRate = 0.1f; // 学习率

        switch (searchType) {
            case KEYWORD:
                if (resultCount > 0) {
                    // 关键词搜索有效果，提高偏好
                    float newWeight = preference.getKeywordPreferenceStrength() + learningRate * 0.1f;
                    preference.setKeywordPreferenceStrength(Math.min(1.0f, newWeight));
                }
                break;
            case SEMANTIC:
                if (resultCount > 0) {
                    // 语义搜索有效果，提高偏好
                    float newWeight = preference.getSemanticPreferenceStrength() + learningRate * 0.1f;
                    preference.setSemanticPreferenceStrength(Math.min(1.0f, newWeight));
                }
                break;
            case HYBRID:
                // 混合搜索成功，平衡两种权重
                if (resultCount > 0) {
                    float keywordAdj = learningRate * 0.05f;
                    float semanticAdj = learningRate * 0.05f;
                    preference.setKeywordPreferenceStrength(
                        Math.min(1.0f, preference.getKeywordPreferenceStrength() + keywordAdj));
                    preference.setSemanticPreferenceStrength(
                        Math.min(1.0f, preference.getSemanticPreferenceStrength() + semanticAdj));
                }
                break;
        }
    }

    /**
     * 分析时间模式
     */
    private void analyzeTemporalPatterns(UserSearchPreference preference) {
        Map<String, Object> patterns = preference.getTemporalPatternsMap();

        LocalDateTime now = LocalDateTime.now();
        String hourKey = "hour_" + now.getHour();
        String dayKey = "day_" + now.getDayOfWeek().toString();

        // 更新小时级别统计
        patterns.put(hourKey, (Integer) patterns.getOrDefault(hourKey, 0) + 1);

        // 更新星期级别统计
        patterns.put(dayKey, (Integer) patterns.getOrDefault(dayKey, 0) + 1);

        preference.setTemporalPatternsMap(patterns);
    }

    /**
     * 更新点击模式
     */
    private void updateClickPatterns(UserSearchPreference preference, int position, String category) {
        Map<String, Object> patterns = preference.getClickPatternsMap();

        // 更新位置偏好
        String positionKey = "position_" + (position <= 3 ? "top" : position <= 10 ? "middle" : "bottom");
        patterns.put(positionKey, (Integer) patterns.getOrDefault(positionKey, 0) + 1);

        // 更新分类点击
        if (category != null) {
            String categoryKey = "category_" + category;
            patterns.put(categoryKey, (Integer) patterns.getOrDefault(categoryKey, 0) + 1);
        }

        preference.setClickPatternsMap(patterns);
    }

    /**
     * 更新分类偏好
     */
    private void updateCategoryPreferences(UserSearchPreference preference, String category) {
        if (category == null) return;

        Map<String, Double> categories = preference.getPreferredCategoriesMap();
        double currentWeight = categories.getOrDefault(category, 0.0);
        double newWeight = Math.min(1.0, currentWeight + 0.1); // 增加权重

        categories.put(category, newWeight);
        preference.setPreferredCategoriesMap(categories);
    }

    /**
     * 预测用户偏好的搜索类型
     */
    private String predictPreferredSearchType(UserProfile profile, String query) {
        if (profile.getPreferenceType().equals("SEMANTIC_ORIENTED")) {
            return "semantic";
        } else if (profile.getPreferenceType().equals("KEYWORD_ORIENTED")) {
            return "keyword";
        } else {
            // 根据查询特征判断
            if (query.length() > 10 && !query.matches(".*\\d.*")) {
                return "semantic";
            } else {
                return "hybrid";
            }
        }
    }

    /**
     * 分析并更新单个用户模式
     */
    private void analyzeAndUpdateUserPattern(UserSearchPreference preference) {
        Long userId = preference.getUserId();
        List<SearchLog> recentLogs = getRecentSearches(userId, 100);

        if (recentLogs.isEmpty()) {
            return;
        }

        // 分析搜索类型偏好
        analyzeSearchTypePreference(preference, recentLogs);

        // 分析个性化效果
        analyzePersonalizationEffectiveness(preference, recentLogs);

        // 更新个性化强度
        updatePersonalizationStrength(preference);

        preferenceRepository.save(preference);
    }

    /**
     * 分析搜索类型偏好
     */
    private void analyzeSearchTypePreference(UserSearchPreference preference, List<SearchLog> logs) {
        Map<SearchLog.SearchType, Long> typeCounts = logs.stream()
            .collect(Collectors.groupingBy(SearchLog::getSearchType, Collectors.counting()));

        long totalSearches = logs.size();
        if (totalSearches > 0) {
            float semanticRatio = typeCounts.getOrDefault(SearchLog.SearchType.SEMANTIC, 0L).floatValue() / totalSearches;
            float keywordRatio = typeCounts.getOrDefault(SearchLog.SearchType.KEYWORD, 0L).floatValue() / totalSearches;

            // 平滑更新偏好权重
            float alpha = 0.3f; // 平滑因子
            preference.setSemanticPreferenceStrength(
                alpha * semanticRatio + (1 - alpha) * preference.getSemanticPreferenceStrength()
            );
            preference.setKeywordPreferenceStrength(
                alpha * keywordRatio + (1 - alpha) * preference.getKeywordPreferenceStrength()
            );
        }
    }

    /**
     * 分析个性化效果
     */
    private void analyzePersonalizationEffectiveness(UserSearchPreference preference, List<SearchLog> logs) {
        // 计算平均结果数量
        double avgResults = logs.stream()
            .mapToInt(log -> log.getResultCount() != null ? log.getResultCount() : 0)
            .average()
            .orElse(0.0);

        // 基于搜索效果调整个性化敏感度
        if (avgResults > 10) {
            // 搜索结果较多，可能个性化不够
            preference.setPersonalizationStrength(
                Math.min(1.0f, preference.getPersonalizationStrength() + 0.05f)
            );
        } else if (avgResults < 3) {
            // 搜索结果较少，可能个性化过度
            preference.setPersonalizationStrength(
                Math.max(0.1f, preference.getPersonalizationStrength() - 0.05f)
            );
        }
    }

    /**
     * 更新个性化强度
     */
    private void updatePersonalizationStrength(UserSearchPreference preference) {
        // 基于用户活跃度调整个性化强度
        if (preference.isNewUser()) {
            preference.setPersonalizationStrength(0.3f); // 新用户低个性化
        } else if (preference.getTotalSearches() > 100) {
            preference.setPersonalizationStrength(
                Math.min(0.9f, preference.getPersonalizationStrength() + 0.1f)
            ); // 活跃用户高个性化
        }
    }

    // ==================== 内部数据类 ====================

    /**
     * 用户画像
     */
    public static class UserProfile {
        private Long userId;
        private Map<String, Double> preferences = new HashMap<>();
        private List<String> recentSearches = new ArrayList<>();
        private Float personalizationStrength;
        private String preferenceType;
        private Double clickThroughRate;
        private Long totalSearches;
        private Map<String, Double> scenePreferences = new HashMap<>();

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public Map<String, Double> getPreferences() { return preferences; }
        public void setPreferences(Map<String, Double> preferences) { this.preferences = preferences; }

        public List<String> getRecentSearches() { return recentSearches; }
        public void setRecentSearches(List<String> recentSearches) { this.recentSearches = recentSearches; }

        public Float getPersonalizationStrength() { return personalizationStrength; }
        public void setPersonalizationStrength(Float personalizationStrength) { this.personalizationStrength = personalizationStrength; }

        public String getPreferenceType() { return preferenceType; }
        public void setPreferenceType(String preferenceType) { this.preferenceType = preferenceType; }

        public Double getClickThroughRate() { return clickThroughRate; }
        public void setClickThroughRate(Double clickThroughRate) { this.clickThroughRate = clickThroughRate; }

        public Long getTotalSearches() { return totalSearches; }
        public void setTotalSearches(Long totalSearches) { this.totalSearches = totalSearches; }

        public Map<String, Double> getScenePreferences() { return scenePreferences; }
        public void setScenePreferences(Map<String, Double> scenePreferences) { this.scenePreferences = scenePreferences; }

        public boolean hasPreferenceForCategory(String category) {
            return preferences.getOrDefault(category, 0.0) > 0.5;
        }
    }

    /**
     * 搜索意图
     */
    public static class SearchIntent {
        private String query;
        private Long userId;
        private String primaryIntent;
        private String preferredSearchType;
        private Float personalizationWeight;

        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getPrimaryIntent() { return primaryIntent; }
        public void setPrimaryIntent(String primaryIntent) { this.primaryIntent = primaryIntent; }

        public String getPreferredSearchType() { return preferredSearchType; }
        public void setPreferredSearchType(String preferredSearchType) { this.preferredSearchType = preferredSearchType; }

        public Float getPersonalizationWeight() { return personalizationWeight; }
        public void setPersonalizationWeight(Float personalizationWeight) { this.personalizationWeight = personalizationWeight; }
    }

    /**
     * 个性化权重配置
     */
    public static class PersonalizationWeights {
        private Float keywordWeight;
        private Float semanticWeight;
        private Float freshnessWeight;
        private Float popularityWeight;
        private Float personalizationStrength;

        // Getters and Setters
        public Float getKeywordWeight() { return keywordWeight; }
        public void setKeywordWeight(Float keywordWeight) { this.keywordWeight = keywordWeight; }

        public Float getSemanticWeight() { return semanticWeight; }
        public void setSemanticWeight(Float semanticWeight) { this.semanticWeight = semanticWeight; }

        public Float getFreshnessWeight() { return freshnessWeight; }
        public void setFreshnessWeight(Float freshnessWeight) { this.freshnessWeight = freshnessWeight; }

        public Float getPopularityWeight() { return popularityWeight; }
        public void setPopularityWeight(Float popularityWeight) { this.popularityWeight = popularityWeight; }

        public Float getPersonalizationStrength() { return personalizationStrength; }
        public void setPersonalizationStrength(Float personalizationStrength) { this.personalizationStrength = personalizationStrength; }
    }
}