package com.deepsearch.service;

import com.deepsearch.dto.SearchSuggestionResponse;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.repository.SearchLogRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索建议服务
 * 提供智能搜索建议，包括查询扩展、查询缩小、相关查询、拼写纠错等
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSuggestionService {

    private final SearchLogRepository searchLogRepository;
    private final TrieService trieService;

    // 建议缓存
    private final Cache<String, SearchSuggestionResponse> suggestionCache =
            Caffeine.newBuilder()
                    .maximumSize(10000)
                    .expireAfterWrite(1, TimeUnit.HOURS)
                    .recordStats()
                    .build();

    // 同义词词典（简化版，实际项目中应从数据库加载）
    private final Map<String, List<String>> synonymMap = initializeSynonymMap();

    // 相关词词典
    private final Map<String, List<String>> relatedTermsMap = initializeRelatedTermsMap();

    // 常见拼写错误映射
    private final Map<String, String> commonMisspellings = initializeCommonMisspellings();

    /**
     * 生成搜索建议
     *
     * @param query              当前查询
     * @param currentResultCount 当前搜索结果数量
     * @param limit              每种建议类型的数量限制
     * @return 搜索建议响应
     */
    public SearchSuggestionResponse generateSuggestions(String query, int currentResultCount, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return new SearchSuggestionResponse();
        }

        String normalizedQuery = query.trim().toLowerCase();
        String cacheKey = String.format("%s_%d_%d", normalizedQuery, currentResultCount, limit);

        return suggestionCache.get(cacheKey, key -> {
            long startTime = System.currentTimeMillis();

            SearchSuggestionResponse response = new SearchSuggestionResponse();
            response.setOriginalQuery(query);

            try {
                // 分析查询意图
                QueryIntent intent = analyzeQueryIntent(normalizedQuery);

                // 1. 查询扩展建议（结果太少时）
                if (currentResultCount < 5) {
                    List<String> expansionSuggestions = generateExpansionSuggestions(normalizedQuery, intent, limit);
                    response.setExpansionSuggestions(expansionSuggestions);
                }

                // 2. 查询缩小建议（结果太多时）
                if (currentResultCount > 1000) {
                    List<String> narrowingSuggestions = generateNarrowingSuggestions(normalizedQuery, intent, limit);
                    response.setNarrowingSuggestions(narrowingSuggestions);
                }

                // 3. 相关查询建议
                List<String> relatedQueries = generateRelatedQueries(normalizedQuery, limit);
                response.setRelatedQueries(relatedQueries);

                // 4. 拼写纠错建议（无结果时）
                if (currentResultCount == 0) {
                    List<String> spellingSuggestions = generateSpellingSuggestions(normalizedQuery, limit);
                    response.setSpellingSuggestions(spellingSuggestions);
                }

                // 计算总建议数
                int totalSuggestions = 0;
                totalSuggestions += response.getExpansionSuggestions() != null ? response.getExpansionSuggestions().size() : 0;
                totalSuggestions += response.getNarrowingSuggestions() != null ? response.getNarrowingSuggestions().size() : 0;
                totalSuggestions += response.getRelatedQueries() != null ? response.getRelatedQueries().size() : 0;
                totalSuggestions += response.getSpellingSuggestions() != null ? response.getSpellingSuggestions().size() : 0;

                response.setTotalSuggestions(totalSuggestions);
                response.setResponseTimeMs(System.currentTimeMillis() - startTime);

                log.debug("生成搜索建议: query={}, currentResults={}, totalSuggestions={}",
                        query, currentResultCount, totalSuggestions);

                return response;

            } catch (Exception e) {
                log.error("生成搜索建议失败: query={}", query, e);
                response.setTotalSuggestions(0);
                response.setResponseTimeMs(System.currentTimeMillis() - startTime);
                return response;
            }
        });
    }

    /**
     * 分析查询意图
     */
    private QueryIntent analyzeQueryIntent(String query) {
        QueryIntent intent = new QueryIntent();

        // 简单的意图分析逻辑
        if (query.contains("银行") || query.contains("金融")) {
            intent.setDomain("banking");
        } else if (query.contains("产品") || query.contains("服务")) {
            intent.setDomain("product");
        } else if (query.contains("活动") || query.contains("优惠")) {
            intent.setDomain("activity");
        } else {
            intent.setDomain("general");
        }

        // 分析查询类型
        if (query.length() <= 3) {
            intent.setType("broad");
        } else if (query.split("\\s+").length > 3) {
            intent.setType("specific");
        } else {
            intent.setType("normal");
        }

        return intent;
    }

    /**
     * 生成查询扩展建议
     */
    private List<String> generateExpansionSuggestions(String query, QueryIntent intent, int limit) {
        List<String> suggestions = new ArrayList<>();

        try {
            // 1. 同义词扩展
            List<String> synonyms = getSynonyms(query);
            suggestions.addAll(synonyms);

            // 2. 相关词扩展
            List<String> relatedTerms = getRelatedTerms(query);
            suggestions.addAll(relatedTerms);

            // 3. 基于领域的扩展
            if ("banking".equals(intent.getDomain())) {
                suggestions.addAll(Arrays.asList(
                        query + " 手续费",
                        query + " 利率",
                        query + " 条件",
                        query + " 流程"
                ));
            } else if ("product".equals(intent.getDomain())) {
                suggestions.addAll(Arrays.asList(
                        query + " 特色",
                        query + " 优势",
                        query + " 说明",
                        query + " 介绍"
                ));
            }

            // 4. 基于历史数据的扩展
            List<String> historyBased = getHistoryBasedExpansions(query, limit);
            suggestions.addAll(historyBased);

            // 去重并限制数量
            return suggestions.stream()
                    .distinct()
                    .filter(s -> !s.equals(query))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("生成扩展建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成查询缩小建议
     */
    private List<String> generateNarrowingSuggestions(String query, QueryIntent intent, int limit) {
        List<String> suggestions = new ArrayList<>();

        try {
            // 1. 添加限定词
            suggestions.addAll(Arrays.asList(
                    "个人" + query,
                    "企业" + query,
                    "网上" + query,
                    "手机" + query
            ));

            // 2. 添加具体类型
            if ("banking".equals(intent.getDomain())) {
                suggestions.addAll(Arrays.asList(
                        query + " 储蓄",
                        query + " 理财",
                        query + " 贷款",
                        query + " 信用卡"
                ));
            }

            // 3. 添加时间限定
            suggestions.addAll(Arrays.asList(
                    query + " 2024",
                    query + " 最新",
                    query + " 当前"
            ));

            // 4. 基于用户行为的缩小建议
            List<String> behaviorBased = getBehaviorBasedNarrowing(query, limit);
            suggestions.addAll(behaviorBased);

            return suggestions.stream()
                    .distinct()
                    .filter(s -> !s.equals(query))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("生成缩小建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成相关查询建议
     */
    private List<String> generateRelatedQueries(String query, int limit) {
        List<String> suggestions = new ArrayList<>();

        try {
            // 1. 基于搜索历史的相关查询
            List<String> historyRelated = getRelatedQueriesFromHistory(query, limit);
            suggestions.addAll(historyRelated);

            // 2. 基于词典的相关查询
            List<String> dictionaryRelated = getRelatedTerms(query);
            suggestions.addAll(dictionaryRelated);

            // 3. 基于共现的相关查询
            List<String> coOccurrenceRelated = getCoOccurrenceQueries(query, limit);
            suggestions.addAll(coOccurrenceRelated);

            return suggestions.stream()
                    .distinct()
                    .filter(s -> !s.equals(query))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("生成相关查询失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成拼写纠错建议
     */
    private List<String> generateSpellingSuggestions(String query, int limit) {
        List<String> suggestions = new ArrayList<>();

        try {
            // 1. 检查常见拼写错误
            String corrected = commonMisspellings.get(query);
            if (corrected != null) {
                suggestions.add(corrected);
            }

            // 2. 编辑距离算法纠错
            List<String> editDistanceCorrections = getEditDistanceCorrections(query, limit);
            suggestions.addAll(editDistanceCorrections);

            // 3. 音近字纠错（简化版）
            List<String> phoneticCorrections = getPhoneticCorrections(query, limit);
            suggestions.addAll(phoneticCorrections);

            return suggestions.stream()
                    .distinct()
                    .filter(s -> !s.equals(query))
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("生成拼写纠错建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取同义词
     */
    private List<String> getSynonyms(String word) {
        return synonymMap.getOrDefault(word, Collections.emptyList());
    }

    /**
     * 获取相关词
     */
    private List<String> getRelatedTerms(String word) {
        return relatedTermsMap.getOrDefault(word, Collections.emptyList());
    }

    /**
     * 基于历史数据的扩展
     */
    private List<String> getHistoryBasedExpansions(String query, int limit) {
        try {
            // 查找包含当前查询的历史搜索
            Pageable pageable = PageRequest.of(0, limit * 2);
            List<SearchLog> logs = searchLogRepository.findByQueryTextContaining(query, pageable);

            return logs.stream()
                    .map(SearchLog::getQueryText)
                    .filter(q -> q.length() > query.length())
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("获取历史扩展建议失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 基于用户行为的缩小建议
     */
    private List<String> getBehaviorBasedNarrowing(String query, int limit) {
        // 简化实现，实际应基于用户行为数据
        return Arrays.asList(
                query + " 详细信息",
                query + " 操作指南",
                query + " 常见问题"
        );
    }

    /**
     * 从历史数据获取相关查询
     */
    private List<String> getRelatedQueriesFromHistory(String query, int limit) {
        try {
            // 查找与当前查询相关的历史搜索
            Pageable pageable = PageRequest.of(0, limit * 3);
            List<SearchLog> recentLogs = searchLogRepository.findRecentSearchLogs(1000);

            return recentLogs.stream()
                    .map(SearchLog::getQueryText)
                    .filter(q -> isRelated(query, q))
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("获取历史相关查询失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取共现查询
     */
    private List<String> getCoOccurrenceQueries(String query, int limit) {
        // 简化实现，实际应基于查询共现分析
        return Collections.emptyList();
    }

    /**
     * 编辑距离纠错
     */
    private List<String> getEditDistanceCorrections(String query, int limit) {
        List<String> corrections = new ArrayList<>();

        try {
            // 从Trie树中获取相似的词条
            List<String> candidates = trieService.getTopTerms(1000);

            for (String candidate : candidates) {
                if (editDistance(query, candidate) <= 2 && Math.abs(query.length() - candidate.length()) <= 2) {
                    corrections.add(candidate);
                    if (corrections.size() >= limit) {
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.warn("编辑距离纠错失败", e);
        }

        return corrections;
    }

    /**
     * 音近字纠错
     */
    private List<String> getPhoneticCorrections(String query, int limit) {
        // 简化实现，实际应使用更复杂的语音算法
        return Collections.emptyList();
    }

    /**
     * 计算编辑距离
     */
    private int editDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[m][n];
    }

    /**
     * 判断两个查询是否相关
     */
    private boolean isRelated(String query1, String query2) {
        if (query1.equals(query2)) {
            return false;
        }

        // 简单的相关性判断
        String[] words1 = query1.split("\\s+");
        String[] words2 = query2.split("\\s+");

        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) || getSynonyms(word1).contains(word2)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 初始化同义词词典
     */
    private Map<String, List<String>> initializeSynonymMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("银行", Arrays.asList("金融机构", "银行业", "金融"));
        map.put("产品", Arrays.asList("服务", "业务", "功能"));
        map.put("贷款", Arrays.asList("借款", "融资", "信贷"));
        map.put("存款", Arrays.asList("储蓄", "理财", "投资"));
        map.put("手机", Arrays.asList("移动", "手机端", "移动端"));
        map.put("网上", Arrays.asList("在线", "网络", "线上"));
        return map;
    }

    /**
     * 初始化相关词词典
     */
    private Map<String, List<String>> initializeRelatedTermsMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("银行", Arrays.asList("信用卡", "贷款", "存款", "理财", "转账"));
        map.put("理财", Arrays.asList("投资", "基金", "保险", "定期存款"));
        map.put("贷款", Arrays.asList("房贷", "车贷", "消费贷", "信用贷"));
        map.put("信用卡", Arrays.asList("申请", "额度", "还款", "积分"));
        return map;
    }

    /**
     * 初始化常见拼写错误
     */
    private Map<String, String> initializeCommonMisspellings() {
        Map<String, String> map = new HashMap<>();
        map.put("yinhang", "银行");
        map.put("daikuan", "贷款");
        map.put("cunkuan", "存款");
        map.put("licai", "理财");
        return map;
    }

    /**
     * 查询意图类
     */
    private static class QueryIntent {
        private String domain;
        private String type;

        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("suggestionCacheStats", suggestionCache.stats());
        return stats;
    }
}