package com.deepsearch.service;

import com.deepsearch.dto.Suggestion;
import com.deepsearch.dto.SuggestionType;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.entity.User;
import com.deepsearch.repository.SearchLogRepository;
import com.deepsearch.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 自动补全服务
 * 提供智能搜索建议，包括前缀匹配、热门搜索、个性化推荐等
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoCompleteService {

    private final TrieService trieService;
    private final SearchLogRepository searchLogRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 本地缓存，存储用户最近的建议
    private final Cache<String, List<Suggestion>> suggestionCache =
            Caffeine.newBuilder()
                    .maximumSize(50000)
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .recordStats()
                    .build();

    // 热门查询缓存
    private final Cache<String, List<Suggestion>> popularQueryCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .recordStats()
                    .build();

    // 用户个性化建议缓存
    private final Cache<Long, List<String>> userPreferencesCache =
            Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(2, TimeUnit.HOURS)
                    .recordStats()
                    .build();

    /**
     * 初始化服务
     */
    @PostConstruct
    public void initialize() {
        log.info("AutoCompleteService 初始化完成");
        // 可以在这里预加载热门查询等数据
        refreshPopularQueries();
    }

    /**
     * 获取搜索建议
     *
     * @param query               查询前缀
     * @param userId              用户ID（可为null）
     * @param limit               返回建议数量限制
     * @param includePersonalized 是否包含个性化建议
     * @return 建议列表
     */
    public List<Suggestion> getSuggestions(String query, Long userId, int limit, boolean includePersonalized) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = buildCacheKey(normalizedQuery, userId, includePersonalized);

        return suggestionCache.get(cacheKey, key -> {
            List<Suggestion> allSuggestions = new ArrayList<>();

            // 1. Trie树前缀匹配
            List<Suggestion> trieMatches = getTrieMatches(normalizedQuery, limit * 2);
            allSuggestions.addAll(trieMatches);

            // 2. 热门搜索建议
            List<Suggestion> popularSuggestions = getPopularSuggestions(normalizedQuery, limit);
            allSuggestions.addAll(popularSuggestions);

            // 3. 个性化建议（如果用户已登录且启用）
            if (includePersonalized && userId != null) {
                List<Suggestion> personalizedSuggestions = getPersonalizedSuggestions(normalizedQuery, userId, limit);
                allSuggestions.addAll(personalizedSuggestions);
            }

            // 4. 语义相关建议
            List<Suggestion> semanticSuggestions = getSemanticSuggestions(normalizedQuery, limit / 2);
            allSuggestions.addAll(semanticSuggestions);

            // 5. 合并、去重、排序
            return mergeSuggestions(allSuggestions, limit);
        });
    }

    /**
     * 获取热门查询
     */
    public List<Suggestion> getPopularQueries(int limit, int days) {
        String cacheKey = String.format("popular_%d_%d", limit, days);

        return popularQueryCache.get(cacheKey, key -> {
            LocalDateTime startDate = LocalDateTime.now().minusDays(days);
            Pageable pageable = PageRequest.of(0, limit);

            List<Object[]> results = searchLogRepository.findPopularSearchTerms(pageable);

            return results.stream()
                    .map(row -> Suggestion.of(
                            (String) row[0],
                            SuggestionType.POPULAR,
                            calculatePopularityScore(((Number) row[1]).longValue())
                    ))
                    .collect(Collectors.toList());
        });
    }

    /**
     * 记录用户选择的建议（用于改进推荐算法）
     */
    public void recordSelection(String originalQuery, String selectedSuggestion,
                                String suggestionType, Long userId) {
        try {
            // 异步记录到Redis，用于后续分析
            String key = String.format("suggestion_selection:%s:%s", originalQuery, selectedSuggestion);
            Map<String, Object> selectionData = new HashMap<>();
            selectionData.put("originalQuery", originalQuery);
            selectionData.put("selectedSuggestion", selectedSuggestion);
            selectionData.put("suggestionType", suggestionType);
            selectionData.put("userId", userId);
            selectionData.put("timestamp", System.currentTimeMillis());

            redisTemplate.opsForHash().putAll(key, selectionData);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);

            // 如果是个性化建议，更新用户偏好缓存
            if (userId != null) {
                updateUserPreferences(userId, selectedSuggestion);
            }

            log.debug("建议选择记录成功: {} -> {}", originalQuery, selectedSuggestion);

        } catch (Exception e) {
            log.warn("建议选择记录失败", e);
        }
    }

    /**
     * 获取Trie树匹配结果
     */
    private List<Suggestion> getTrieMatches(String query, int limit) {
        try {
            List<String> matches = trieService.getPrefixMatches(query, limit);
            return matches.stream()
                    .map(match -> Suggestion.of(match, SuggestionType.PREFIX_MATCH, 0.6f))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Trie树匹配失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取热门搜索建议
     */
    private List<Suggestion> getPopularSuggestions(String query, int limit) {
        try {
            // 查找包含查询词的热门搜索
            List<Suggestion> allPopular = getPopularQueries(limit * 3, 7);

            return allPopular.stream()
                    .filter(suggestion -> suggestion.getText().toLowerCase().contains(query))
                    .limit(limit)
                    .map(suggestion -> new Suggestion(
                            suggestion.getText(),
                            SuggestionType.POPULAR,
                            suggestion.getScore() * 0.8f, // 稍微降低权重
                            "popular",
                            null
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("热门搜索建议获取失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取个性化建议
     */
    private List<Suggestion> getPersonalizedSuggestions(String query, Long userId, int limit) {
        try {
            List<String> userPreferences = getUserPreferences(userId);

            return userPreferences.stream()
                    .filter(pref -> pref.toLowerCase().contains(query))
                    .limit(limit)
                    .map(pref -> Suggestion.of(pref, SuggestionType.PERSONALIZED, 1.0f))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("个性化建议获取失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取语义相关建议
     */
    private List<Suggestion> getSemanticSuggestions(String query, int limit) {
        try {
            // 这里可以集成词向量模型或者使用同义词词典
            // 暂时实现简单的关键词扩展
            List<String> semanticTerms = expandQuery(query);

            return semanticTerms.stream()
                    .limit(limit)
                    .map(term -> Suggestion.of(term, SuggestionType.SEMANTIC, 0.7f))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("语义建议获取失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 合并和排序建议
     */
    private List<Suggestion> mergeSuggestions(List<Suggestion> allSuggestions, int limit) {
        Map<String, Suggestion> suggestionMap = new LinkedHashMap<>();

        // 按优先级合并建议，避免重复
        for (Suggestion suggestion : allSuggestions) {
            String text = suggestion.getText();
            if (!suggestionMap.containsKey(text) ||
                    suggestion.getScore() > suggestionMap.get(text).getScore()) {
                suggestionMap.put(text, suggestion);
            }
        }

        // 按评分排序并限制数量
        return suggestionMap.values().stream()
                .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户偏好
     */
    private List<String> getUserPreferences(Long userId) {
        return userPreferencesCache.get(userId, id -> {
            // 获取用户最近的搜索历史
            Pageable pageable = PageRequest.of(0, 50);
            List<SearchLog> recentSearches = searchLogRepository.findRecentSearchLogsByUserId(id, pageable);

            return recentSearches.stream()
                    .map(SearchLog::getQueryText)
                    .distinct()
                    .collect(Collectors.toList());
        });
    }

    /**
     * 更新用户偏好
     */
    private void updateUserPreferences(Long userId, String selectedSuggestion) {
        List<String> preferences = getUserPreferences(userId);

        // 将选择的建议添加到偏好列表前面
        List<String> updatedPreferences = new ArrayList<>();
        updatedPreferences.add(selectedSuggestion);
        updatedPreferences.addAll(preferences.stream()
                .filter(p -> !p.equals(selectedSuggestion))
                .limit(49) // 保持列表大小
                .collect(Collectors.toList()));

        userPreferencesCache.put(userId, updatedPreferences);
    }

    /**
     * 查询扩展
     */
    private List<String> expandQuery(String query) {
        // 简单的同义词和相关词扩展
        List<String> expansions = new ArrayList<>();

        // 这里可以集成更复杂的NLP模型
        // 暂时使用简单的规则
        if (query.contains("银行")) {
            expansions.addAll(Arrays.asList("金融", "存款", "贷款", "理财"));
        }
        if (query.contains("产品")) {
            expansions.addAll(Arrays.asList("服务", "功能", "特色"));
        }

        return expansions;
    }

    /**
     * 计算热门度评分
     */
    private Float calculatePopularityScore(long count) {
        // 简单的对数归一化
        return (float) Math.min(1.0, Math.log10(count + 1) / 4.0);
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(String query, Long userId, boolean includePersonalized) {
        return String.format("%s_%s_%s", query,
                userId != null ? userId : "anonymous",
                includePersonalized);
    }

    /**
     * 刷新热门查询缓存
     */
    private void refreshPopularQueries() {
        try {
            popularQueryCache.invalidateAll();
            log.info("热门查询缓存已刷新");
        } catch (Exception e) {
            log.warn("热门查询缓存刷新失败", e);
        }
    }

    /**
     * 获取缓存统计信息（用于监控）
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("suggestionCacheStats", suggestionCache.stats());
        stats.put("popularQueryCacheStats", popularQueryCache.stats());
        stats.put("userPreferencesCacheStats", userPreferencesCache.stats());
        return stats;
    }
}