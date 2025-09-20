package com.deepsearch.service;

import com.deepsearch.entity.Synonym;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 查询扩展服务
 * 实现智能查询扩展算法，包括同义词扩展、词汇变体生成、上下文相关扩展等
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QueryExpansionService {

    private final SynonymService synonymService;

    @Value("${deepsearch.query.expansion.maxTerms:10}")
    private int maxExpansionTerms;

    @Value("${deepsearch.query.expansion.minTermLength:2}")
    private int minTermLength;

    @Value("${deepsearch.query.expansion.confidenceThreshold:0.7}")
    private float confidenceThreshold;

    @Value("${deepsearch.query.expansion.enablePhoneticMatching:true}")
    private boolean enablePhoneticMatching;

    // 中文数字映射
    private static final Map<String, String> CHINESE_NUMBERS = Map.of(
        "一", "1", "二", "2", "三", "3", "四", "4", "五", "5",
        "六", "6", "七", "7", "八", "8", "九", "9", "十", "10"
    );

    // 银行业务缩写映射
    private static final Map<String, Set<String>> BANKING_ABBREVIATIONS = Map.of(
        "房贷", Set.of("住房贷款", "按揭贷款", "房屋贷款", "个人住房贷款"),
        "车贷", Set.of("汽车贷款", "车辆贷款", "个人汽车贷款"),
        "网银", Set.of("网上银行", "在线银行", "网络银行"),
        "手机银行", Set.of("移动银行", "手机端", "APP银行"),
        "信用卡", Set.of("贷记卡", "透支卡"),
        "储蓄卡", Set.of("借记卡", "存款卡"),
        "ATM", Set.of("自动取款机", "取款机", "现金机"),
        "POS", Set.of("刷卡机", "收银机", "终端机")
    );

    // 查询类型枚举
    public enum QueryType {
        PRODUCT_QUERY,    // 产品查询
        SERVICE_QUERY,    // 服务查询
        PROCEDURE_QUERY,  // 流程查询
        GENERAL_QUERY     // 一般查询
    }

    /**
     * 智能查询扩展 - 主入口方法
     *
     * @param originalQuery 原始查询
     * @param contextHints 上下文提示（如用户历史、当前页面等）
     * @return 扩展后的查询结果
     */
    public QueryExpansionResult expandQuery(String originalQuery, Map<String, Object> contextHints) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return new QueryExpansionResult(originalQuery, Collections.emptySet(), QueryType.GENERAL_QUERY);
        }

        long startTime = System.currentTimeMillis();
        String normalizedQuery = normalizeQuery(originalQuery);

        log.debug("开始查询扩展: '{}'", originalQuery);

        // 1. 查询类型识别
        QueryType queryType = identifyQueryType(normalizedQuery, contextHints);

        // 2. 多层次扩展
        Set<String> expandedTerms = new LinkedHashSet<>();
        expandedTerms.add(normalizedQuery); // 保留原始查询

        // 2.1 同义词扩展
        Set<String> synonymExpansion = performSynonymExpansion(normalizedQuery);
        expandedTerms.addAll(synonymExpansion);

        // 2.2 词汇变体扩展
        Set<String> variantExpansion = performVariantExpansion(normalizedQuery);
        expandedTerms.addAll(variantExpansion);

        // 2.3 上下文相关扩展
        Set<String> contextualExpansion = performContextualExpansion(normalizedQuery, queryType, contextHints);
        expandedTerms.addAll(contextualExpansion);

        // 2.4 领域特定扩展
        Set<String> domainExpansion = performDomainSpecificExpansion(normalizedQuery, queryType);
        expandedTerms.addAll(domainExpansion);

        // 2.5 语义相关扩展
        Set<String> semanticExpansion = performSemanticExpansion(normalizedQuery, queryType);
        expandedTerms.addAll(semanticExpansion);

        // 3. 结果过滤和排序
        Set<String> filteredTerms = filterAndRankExpansions(expandedTerms, originalQuery);

        long duration = System.currentTimeMillis() - startTime;
        log.debug("查询扩展完成: '{}' -> {} 个扩展词 (耗时: {}ms)",
                 originalQuery, filteredTerms.size(), duration);

        return new QueryExpansionResult(originalQuery, filteredTerms, queryType);
    }

    /**
     * 识别查询类型
     */
    private QueryType identifyQueryType(String query, Map<String, Object> contextHints) {
        String lowerQuery = query.toLowerCase();

        // 产品相关关键词
        if (containsAny(lowerQuery, Arrays.asList("房贷", "车贷", "信用卡", "储蓄", "理财", "基金", "保险", "贷款"))) {
            return QueryType.PRODUCT_QUERY;
        }

        // 服务相关关键词
        if (containsAny(lowerQuery, Arrays.asList("转账", "汇款", "查询", "开户", "销户", "挂失", "解冻", "激活"))) {
            return QueryType.SERVICE_QUERY;
        }

        // 流程相关关键词
        if (containsAny(lowerQuery, Arrays.asList("如何", "怎么", "流程", "步骤", "手续", "材料", "条件"))) {
            return QueryType.PROCEDURE_QUERY;
        }

        return QueryType.GENERAL_QUERY;
    }

    /**
     * 同义词扩展
     */
    private Set<String> performSynonymExpansion(String query) {
        Set<String> expansions = new HashSet<>();

        // 使用同义词服务进行扩展
        Set<String> synonyms = synonymService.expandQuery(query);
        expansions.addAll(synonyms);

        // 分词后逐词扩展
        String[] tokens = tokenize(query);
        for (String token : tokens) {
            if (token.length() >= minTermLength) {
                List<String> tokenSynonyms = synonymService.getSynonyms(token);
                expansions.addAll(tokenSynonyms);

                Set<String> bidirectionalSynonyms = synonymService.getBidirectionalSynonyms(token);
                expansions.addAll(bidirectionalSynonyms);
            }
        }

        return expansions;
    }

    /**
     * 词汇变体扩展
     */
    private Set<String> performVariantExpansion(String query) {
        Set<String> expansions = new HashSet<>();

        // 数字转换（中文数字 <-> 阿拉伯数字）
        expansions.addAll(expandNumbers(query));

        // 缩写扩展
        expansions.addAll(expandAbbreviations(query));

        // 拼音扩展（如果启用）
        if (enablePhoneticMatching) {
            expansions.addAll(expandPhonetic(query));
        }

        // 词形变化（如：动词时态、名词单复数等）
        expansions.addAll(expandMorphological(query));

        return expansions;
    }

    /**
     * 上下文相关扩展
     */
    private Set<String> performContextualExpansion(String query, QueryType queryType, Map<String, Object> contextHints) {
        Set<String> expansions = new HashSet<>();

        if (contextHints == null) {
            return expansions;
        }

        // 基于用户历史搜索的扩展
        if (contextHints.containsKey("searchHistory")) {
            @SuppressWarnings("unchecked")
            List<String> searchHistory = (List<String>) contextHints.get("searchHistory");
            expansions.addAll(expandFromHistory(query, searchHistory));
        }

        // 基于当前页面上下文的扩展
        if (contextHints.containsKey("currentPage")) {
            String currentPage = (String) contextHints.get("currentPage");
            expansions.addAll(expandFromPageContext(query, currentPage));
        }

        // 基于用户角色的扩展
        if (contextHints.containsKey("userRole")) {
            String userRole = (String) contextHints.get("userRole");
            expansions.addAll(expandByUserRole(query, userRole));
        }

        return expansions;
    }

    /**
     * 领域特定扩展
     */
    private Set<String> performDomainSpecificExpansion(String query, QueryType queryType) {
        Set<String> expansions = new HashSet<>();

        switch (queryType) {
            case PRODUCT_QUERY:
                expansions.addAll(expandBankingProducts(query));
                break;
            case SERVICE_QUERY:
                expansions.addAll(expandBankingServices(query));
                break;
            case PROCEDURE_QUERY:
                expansions.addAll(expandBankingProcedures(query));
                break;
            default:
                expansions.addAll(expandGeneralBankingTerms(query));
        }

        return expansions;
    }

    /**
     * 语义相关扩展
     */
    private Set<String> performSemanticExpansion(String query, QueryType queryType) {
        Set<String> expansions = new HashSet<>();

        // 语义场扩展（相关概念）
        expansions.addAll(expandSemanticField(query));

        // 上下位词扩展
        expansions.addAll(expandHyponymsHypernyms(query));

        // 关联词扩展
        expansions.addAll(expandAssociatedTerms(query, queryType));

        return expansions;
    }

    /**
     * 数字扩展
     */
    private Set<String> expandNumbers(String query) {
        Set<String> expansions = new HashSet<>();

        // 中文数字转阿拉伯数字
        for (Map.Entry<String, String> entry : CHINESE_NUMBERS.entrySet()) {
            if (query.contains(entry.getKey())) {
                expansions.add(query.replace(entry.getKey(), entry.getValue()));
            }
        }

        // 阿拉伯数字转中文数字
        for (Map.Entry<String, String> entry : CHINESE_NUMBERS.entrySet()) {
            if (query.contains(entry.getValue())) {
                expansions.add(query.replace(entry.getValue(), entry.getKey()));
            }
        }

        return expansions;
    }

    /**
     * 缩写扩展
     */
    private Set<String> expandAbbreviations(String query) {
        Set<String> expansions = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : BANKING_ABBREVIATIONS.entrySet()) {
            String abbreviation = entry.getKey();
            Set<String> fullForms = entry.getValue();

            if (query.contains(abbreviation)) {
                for (String fullForm : fullForms) {
                    expansions.add(query.replace(abbreviation, fullForm));
                }
            }

            // 反向扩展
            for (String fullForm : fullForms) {
                if (query.contains(fullForm)) {
                    expansions.add(query.replace(fullForm, abbreviation));
                }
            }
        }

        return expansions;
    }

    /**
     * 银行产品扩展
     */
    private Set<String> expandBankingProducts(String query) {
        Set<String> expansions = new HashSet<>();

        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("贷款")) {
            expansions.addAll(Arrays.asList("个人贷款", "企业贷款", "消费贷", "经营贷", "抵押贷", "信用贷"));
        }

        if (lowerQuery.contains("理财")) {
            expansions.addAll(Arrays.asList("理财产品", "投资理财", "财富管理", "资产配置", "定期理财", "活期理财"));
        }

        if (lowerQuery.contains("保险")) {
            expansions.addAll(Arrays.asList("人寿保险", "意外保险", "健康保险", "财产保险", "车险", "家财险"));
        }

        return expansions;
    }

    /**
     * 银行服务扩展
     */
    private Set<String> expandBankingServices(String query) {
        Set<String> expansions = new HashSet<>();

        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("转账")) {
            expansions.addAll(Arrays.asList("汇款", "转钱", "付款", "跨行转账", "同行转账", "实时转账"));
        }

        if (lowerQuery.contains("查询")) {
            expansions.addAll(Arrays.asList("余额查询", "明细查询", "交易查询", "账单查询", "积分查询"));
        }

        return expansions;
    }

    /**
     * 语义场扩展
     */
    private Set<String> expandSemanticField(String query) {
        Set<String> expansions = new HashSet<>();

        // 根据查询内容确定语义场，然后添加相关词汇
        // 这里简化处理，实际可以使用更复杂的语义网络

        if (query.contains("投资")) {
            expansions.addAll(Arrays.asList("收益", "风险", "回报", "收益率", "投资组合", "资产配置"));
        }

        if (query.contains("安全")) {
            expansions.addAll(Arrays.asList("保障", "风控", "防护", "安全性", "可靠", "稳定"));
        }

        return expansions;
    }

    /**
     * 结果过滤和排序
     */
    private Set<String> filterAndRankExpansions(Set<String> expansions, String originalQuery) {
        return expansions.stream()
            .filter(term -> term != null && !term.trim().isEmpty())
            .filter(term -> term.length() >= minTermLength)
            .filter(term -> !term.equals(originalQuery)) // 避免重复
            .distinct()
            .limit(maxExpansionTerms)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // 辅助方法

    private String normalizeQuery(String query) {
        return query.trim().toLowerCase();
    }

    private String[] tokenize(String text) {
        // 简单分词，实际可以集成jieba等中文分词工具
        return text.split("[\\s\\p{Punct}]+");
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    // 以下是简化的扩展方法，实际实现可以更复杂

    private Set<String> expandPhonetic(String query) {
        // 拼音扩展的简化实现
        return new HashSet<>();
    }

    private Set<String> expandMorphological(String query) {
        // 词形变化扩展的简化实现
        return new HashSet<>();
    }

    private Set<String> expandFromHistory(String query, List<String> searchHistory) {
        // 基于搜索历史的扩展
        return new HashSet<>();
    }

    private Set<String> expandFromPageContext(String query, String currentPage) {
        // 基于页面上下文的扩展
        return new HashSet<>();
    }

    private Set<String> expandByUserRole(String query, String userRole) {
        // 基于用户角色的扩展
        return new HashSet<>();
    }

    private Set<String> expandBankingProcedures(String query) {
        // 银行流程扩展
        return new HashSet<>();
    }

    private Set<String> expandGeneralBankingTerms(String query) {
        // 一般银行术语扩展
        return new HashSet<>();
    }

    private Set<String> expandHyponymsHypernyms(String query) {
        // 上下位词扩展
        return new HashSet<>();
    }

    private Set<String> expandAssociatedTerms(String query, QueryType queryType) {
        // 关联词扩展
        return new HashSet<>();
    }

    /**
     * 查询扩展结果类
     */
    public static class QueryExpansionResult {
        private final String originalQuery;
        private final Set<String> expandedTerms;
        private final QueryType queryType;

        public QueryExpansionResult(String originalQuery, Set<String> expandedTerms, QueryType queryType) {
            this.originalQuery = originalQuery;
            this.expandedTerms = expandedTerms;
            this.queryType = queryType;
        }

        public String getOriginalQuery() {
            return originalQuery;
        }

        public Set<String> getExpandedTerms() {
            return expandedTerms;
        }

        public QueryType getQueryType() {
            return queryType;
        }

        public Set<String> getAllTerms() {
            Set<String> allTerms = new LinkedHashSet<>();
            allTerms.add(originalQuery);
            allTerms.addAll(expandedTerms);
            return allTerms;
        }

        @Override
        public String toString() {
            return String.format("QueryExpansionResult{original='%s', expanded=%d terms, type=%s}",
                originalQuery, expandedTerms.size(), queryType);
        }
    }
}