package com.deepsearch.service;

import com.deepsearch.entity.Synonym;
import com.deepsearch.repository.SynonymRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 同义词服务
 * 提供同义词管理、查询扩展和缓存功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SynonymService {

    private final SynonymRepository synonymRepository;

    @Value("${deepsearch.synonym.cache.maxSize:10000}")
    private int maxCacheSize;

    @Value("${deepsearch.synonym.cache.expireMinutes:60}")
    private int cacheExpireMinutes;

    @Value("${deepsearch.synonym.confidence.threshold:0.7}")
    private float confidenceThreshold;

    @Value("${deepsearch.synonym.max.expansion:5}")
    private int maxExpansionTerms;

    // 同义词缓存
    private final Cache<String, List<String>> synonymCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .recordStats()
        .build();

    // 双向同义词缓存（词项-同义词关系）
    private final Cache<String, Set<String>> bidirectionalCache = Caffeine.newBuilder()
        .maximumSize(5000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .recordStats()
        .build();

    /**
     * 获取词项的所有同义词
     *
     * @param term 原始词项
     * @return 同义词列表，按置信度降序排列
     */
    public List<String> getSynonyms(String term) {
        if (term == null || term.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedTerm = normalizeTerm(term);
        return synonymCache.get(normalizedTerm, key -> {
            log.debug("缓存未命中，从数据库查询同义词: {}", key);

            List<Synonym> synonyms = synonymRepository.findByTermAndEnabled(key);
            List<String> synonymList = synonyms.stream()
                .filter(s -> s.getConfidence() >= confidenceThreshold)
                .map(Synonym::getSynonym)
                .distinct()
                .limit(maxExpansionTerms)
                .collect(Collectors.toList());

            // 记录使用统计
            if (!synonyms.isEmpty()) {
                List<Long> synonymIds = synonyms.stream()
                    .map(Synonym::getId)
                    .collect(Collectors.toList());
                updateUsageStatistics(synonymIds);
            }

            return synonymList;
        });
    }

    /**
     * 双向查找同义词关系
     * 查找与给定词项相关的所有同义词（包括正向和反向关系）
     *
     * @param word 查询词项
     * @return 所有相关的同义词集合
     */
    public Set<String> getBidirectionalSynonyms(String word) {
        if (word == null || word.trim().isEmpty()) {
            return Collections.emptySet();
        }

        String normalizedWord = normalizeTerm(word);
        return bidirectionalCache.get(normalizedWord, key -> {
            log.debug("双向查询同义词: {}", key);

            List<Synonym> synonyms = synonymRepository.findByWordBidirectional(key);
            Set<String> allSynonyms = new HashSet<>();

            for (Synonym synonym : synonyms) {
                if (synonym.getConfidence() >= confidenceThreshold) {
                    allSynonyms.add(synonym.getTerm());
                    allSynonyms.add(synonym.getSynonym());
                }
            }

            // 移除原始查询词
            allSynonyms.remove(key);

            // 记录使用统计
            if (!synonyms.isEmpty()) {
                List<Long> synonymIds = synonyms.stream()
                    .map(Synonym::getId)
                    .collect(Collectors.toList());
                updateUsageStatistics(synonymIds);
            }

            return allSynonyms;
        });
    }

    /**
     * 查询扩展 - 为搜索查询生成扩展词项
     *
     * @param query 原始查询
     * @return 扩展后的查询词项集合
     */
    public Set<String> expandQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> expandedTerms = new HashSet<>();
        expandedTerms.add(query); // 包含原始查询

        // 分词处理（简单的空格分词，实际可以集成更复杂的NLP分词器）
        String[] tokens = query.split("\\s+");

        for (String token : tokens) {
            if (token.length() > 1) { // 过滤掉单字符词
                // 获取每个词的同义词
                List<String> synonyms = getSynonyms(token);
                expandedTerms.addAll(synonyms);

                // 获取双向同义词
                Set<String> bidirectionalSynonyms = getBidirectionalSynonyms(token);
                expandedTerms.addAll(bidirectionalSynonyms);
            }
        }

        // 银行业务特定扩展
        expandBankingTerms(query, expandedTerms);

        log.debug("查询扩展: '{}' -> {}", query, expandedTerms);
        return expandedTerms;
    }

    /**
     * 银行业务特定的查询扩展
     */
    private void expandBankingTerms(String query, Set<String> expandedTerms) {
        String lowerQuery = query.toLowerCase();

        // 银行产品同义词扩展
        if (lowerQuery.contains("房贷") || lowerQuery.contains("住房贷款")) {
            expandedTerms.addAll(Arrays.asList("房贷", "住房贷款", "按揭", "房屋按揭", "住房按揭"));
        }

        if (lowerQuery.contains("信用卡") || lowerQuery.contains("贷记卡")) {
            expandedTerms.addAll(Arrays.asList("信用卡", "贷记卡", "刷卡", "透支卡"));
        }

        if (lowerQuery.contains("储蓄") || lowerQuery.contains("存款")) {
            expandedTerms.addAll(Arrays.asList("储蓄", "存款", "定期", "活期", "理财"));
        }

        if (lowerQuery.contains("转账") || lowerQuery.contains("汇款")) {
            expandedTerms.addAll(Arrays.asList("转账", "汇款", "转钱", "付款", "支付"));
        }

        // 渠道相关扩展
        if (lowerQuery.contains("手机银行") || lowerQuery.contains("移动银行")) {
            expandedTerms.addAll(Arrays.asList("手机银行", "移动银行", "APP", "移动应用"));
        }

        if (lowerQuery.contains("网银") || lowerQuery.contains("网上银行")) {
            expandedTerms.addAll(Arrays.asList("网银", "网上银行", "在线银行", "网络银行"));
        }
    }

    /**
     * 根据分类获取同义词
     *
     * @param category 分类名称
     * @return 该分类下的所有同义词
     */
    public List<Synonym> getSynonymsByCategory(String category) {
        return synonymRepository.findByCategoryAndEnabled(category);
    }

    /**
     * 获取银行产品相关同义词
     */
    public List<Synonym> getBankProductSynonyms() {
        return synonymRepository.findBankProductSynonyms();
    }

    /**
     * 获取银行服务相关同义词
     */
    public List<Synonym> getBankServiceSynonyms() {
        return synonymRepository.findBankServiceSynonyms();
    }

    /**
     * 添加新的同义词
     *
     * @param term 原始词项
     * @param synonym 同义词
     * @param confidence 置信度
     * @param source 来源
     * @param category 分类
     * @param createdBy 创建者ID
     * @return 创建的同义词实体
     */
    @Transactional
    public Synonym addSynonym(String term, String synonym, Float confidence,
                             Synonym.SynonymSource source, String category, Long createdBy) {

        // 检查是否已存在
        Optional<Synonym> existing = synonymRepository.findByTermAndSynonym(term, synonym);
        if (existing.isPresent()) {
            log.warn("同义词关系已存在: {} -> {}", term, synonym);
            return existing.get();
        }

        Synonym newSynonym = new Synonym(term, synonym, confidence, source, category);
        newSynonym.setCreatedBy(createdBy);

        Synonym saved = synonymRepository.save(newSynonym);

        // 清除相关缓存
        clearCache(term);
        clearCache(synonym);

        log.info("添加新同义词: {} -> {} (置信度: {}, 来源: {})", term, synonym, confidence, source);
        return saved;
    }

    /**
     * 批量添加同义词
     */
    @Transactional
    public List<Synonym> batchAddSynonyms(List<Synonym> synonyms) {
        List<Synonym> savedSynonyms = synonymRepository.saveAll(synonyms);

        // 清除所有相关缓存
        synonyms.forEach(s -> {
            clearCache(s.getTerm());
            clearCache(s.getSynonym());
        });

        log.info("批量添加同义词: {} 条", savedSynonyms.size());
        return savedSynonyms;
    }

    /**
     * 更新同义词置信度
     */
    @Transactional
    public void updateConfidence(Long synonymId, Float confidence) {
        synonymRepository.updateConfidence(synonymId, confidence);

        // 由于不知道具体的词项，清除全部缓存
        clearAllCache();

        log.info("更新同义词置信度: ID={}, 新置信度={}", synonymId, confidence);
    }

    /**
     * 启用/禁用同义词
     */
    @Transactional
    public void updateEnabled(Long synonymId, Boolean enabled) {
        synonymRepository.updateEnabled(synonymId, enabled);

        // 清除全部缓存
        clearAllCache();

        log.info("更新同义词状态: ID={}, 启用={}", synonymId, enabled);
    }

    /**
     * 获取需要审核的低置信度同义词
     */
    public List<Synonym> getLowConfidenceSynonyms() {
        return synonymRepository.findLowConfidenceSynonyms(confidenceThreshold);
    }

    /**
     * 获取热门同义词
     */
    public List<Synonym> getPopularSynonyms(int limit) {
        return synonymRepository.findPopularSynonyms()
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 同义词统计信息
     */
    public Map<String, Object> getSynonymStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCount", synonymRepository.countEnabledSynonyms());
        stats.put("bySource", synonymRepository.countBySource());
        stats.put("byCategory", synonymRepository.countByCategory());
        stats.put("cacheStats", getCacheStatistics());

        return stats;
    }

    /**
     * 词项标准化
     */
    private String normalizeTerm(String term) {
        if (term == null) return "";

        return term.trim()
            .toLowerCase()
            .replaceAll("\\s+", " "); // 规范化空格
    }

    /**
     * 异步更新使用统计
     */
    private void updateUsageStatistics(List<Long> synonymIds) {
        if (synonymIds != null && !synonymIds.isEmpty()) {
            try {
                synonymRepository.batchIncrementUsageCount(synonymIds);
            } catch (Exception e) {
                log.warn("更新同义词使用统计失败", e);
            }
        }
    }

    /**
     * 清除指定词项的缓存
     */
    private void clearCache(String term) {
        if (term != null) {
            String normalizedTerm = normalizeTerm(term);
            synonymCache.invalidate(normalizedTerm);
            bidirectionalCache.invalidate(normalizedTerm);
        }
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        synonymCache.invalidateAll();
        bidirectionalCache.invalidateAll();
        log.info("已清除所有同义词缓存");
    }

    /**
     * 获取缓存统计信息
     */
    private Map<String, Object> getCacheStatistics() {
        Map<String, Object> cacheStats = new HashMap<>();

        cacheStats.put("synonymCache", Map.of(
            "size", synonymCache.estimatedSize(),
            "hitRate", synonymCache.stats().hitRate(),
            "missRate", synonymCache.stats().missRate(),
            "evictionCount", synonymCache.stats().evictionCount()
        ));

        cacheStats.put("bidirectionalCache", Map.of(
            "size", bidirectionalCache.estimatedSize(),
            "hitRate", bidirectionalCache.stats().hitRate(),
            "missRate", bidirectionalCache.stats().missRate(),
            "evictionCount", bidirectionalCache.stats().evictionCount()
        ));

        return cacheStats;
    }

    /**
     * 预热缓存 - 加载常用同义词
     */
    public void warmUpCache() {
        log.info("开始预热同义词缓存");

        List<Synonym> popularSynonyms = getPopularSynonyms(1000);
        for (Synonym synonym : popularSynonyms) {
            getSynonyms(synonym.getTerm());
            getBidirectionalSynonyms(synonym.getTerm());
        }

        log.info("同义词缓存预热完成，加载了 {} 个常用同义词", popularSynonyms.size());
    }
}