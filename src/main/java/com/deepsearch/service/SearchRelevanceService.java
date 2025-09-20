package com.deepsearch.service;

import com.deepsearch.elasticsearch.dto.DocumentIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索相关性服务 - 实现多维度评分模型和结果合并算法
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchRelevanceService {

    // 评分权重配置
    private static final float KEYWORD_SCORE_WEIGHT = 0.4f;
    private static final float SEMANTIC_SCORE_WEIGHT = 0.3f;
    private static final float FRESHNESS_WEIGHT = 0.1f;
    private static final float POPULARITY_WEIGHT = 0.1f;
    private static final float QUALITY_WEIGHT = 0.1f;

    // 时间衰减参数
    private static final long FRESHNESS_DECAY_DAYS = 365;

    /**
     * 合并并重排序搜索结果
     *
     * @param keywordResults 关键词搜索结果
     * @param semanticResults 语义搜索结果
     * @param weights 搜索权重配置
     * @return 合并和排序后的结果
     */
    public List<DocumentIndex> mergeAndRank(List<DocumentIndex> keywordResults,
                                          List<DocumentIndex> semanticResults,
                                          HybridSearchService.SearchWeights weights) {

        log.debug("开始合并搜索结果: 关键词={}, 语义={}", keywordResults.size(), semanticResults.size());

        // 1. 创建文档ID到结果的映射
        Map<String, DocumentScore> documentScores = new HashMap<>();

        // 2. 处理关键词搜索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            DocumentIndex doc = keywordResults.get(i);
            String docId = doc.getId();

            float keywordScore = calculatePositionScore(i, keywordResults.size());
            DocumentScore docScore = documentScores.computeIfAbsent(docId,
                k -> new DocumentScore(doc));
            docScore.setKeywordScore(keywordScore);
        }

        // 3. 处理语义搜索结果
        for (int i = 0; i < semanticResults.size(); i++) {
            DocumentIndex doc = semanticResults.get(i);
            String docId = doc.getId();

            float semanticScore = calculatePositionScore(i, semanticResults.size());
            DocumentScore docScore = documentScores.computeIfAbsent(docId,
                k -> new DocumentScore(doc));
            docScore.setSemanticScore(semanticScore);
        }

        // 4. 计算综合相关性分数
        for (DocumentScore docScore : documentScores.values()) {
            float relevanceScore = calculateRelevanceScore(docScore, weights);
            docScore.setFinalScore(relevanceScore);
        }

        // 5. 按照最终分数排序并返回文档列表
        List<DocumentIndex> rankedResults = documentScores.values().stream()
            .sorted((a, b) -> Float.compare(b.getFinalScore(), a.getFinalScore()))
            .map(DocumentScore::getDocument)
            .collect(Collectors.toList());

        log.debug("搜索结果合并完成: 总结果={}", rankedResults.size());
        return rankedResults;
    }

    /**
     * 计算基于位置的分数 (归一化的倒序分数)
     */
    private float calculatePositionScore(int position, int totalResults) {
        if (totalResults == 0) return 0.0f;

        // 使用对数衰减，确保首位结果权重更高
        return (float) (Math.log(totalResults - position + 1) / Math.log(totalResults + 1));
    }

    /**
     * 计算多维度相关性分数
     */
    private float calculateRelevanceScore(DocumentScore docScore, HybridSearchService.SearchWeights weights) {
        DocumentIndex doc = docScore.getDocument();

        // 1. 基础搜索分数（关键词 + 语义）
        float keywordContribution = docScore.getKeywordScore() * weights.getNormalizedKeywordWeight();
        float semanticContribution = docScore.getSemanticScore() * weights.getNormalizedVectorWeight();
        float baseScore = keywordContribution + semanticContribution;

        // 2. 时间新鲜度分数
        float freshnessScore = calculateFreshnessScore(doc.getCreatedAt());

        // 3. 内容质量分数
        float qualityScore = calculateQualityScore(doc);

        // 4. 热度分数 (基于内容长度和标题质量的简单估算)
        float popularityScore = calculatePopularityScore(doc);

        // 5. 综合加权分数
        float finalScore = baseScore * (KEYWORD_SCORE_WEIGHT + SEMANTIC_SCORE_WEIGHT) +
                          freshnessScore * FRESHNESS_WEIGHT +
                          qualityScore * QUALITY_WEIGHT +
                          popularityScore * POPULARITY_WEIGHT;

        log.trace("文档评分详情: id={}, base={:.3f}, freshness={:.3f}, quality={:.3f}, popularity={:.3f}, final={:.3f}",
            doc.getId(), baseScore, freshnessScore, qualityScore, popularityScore, finalScore);

        return finalScore;
    }

    /**
     * 计算时间新鲜度分数
     */
    private float calculateFreshnessScore(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 0.5f; // 默认中等分数
        }

        long daysSinceCreation = ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());

        // 使用指数衰减函数
        double decayFactor = Math.exp(-daysSinceCreation / (double) FRESHNESS_DECAY_DAYS);
        return (float) Math.max(0.1, Math.min(1.0, decayFactor));
    }

    /**
     * 计算内容质量分数
     */
    private float calculateQualityScore(DocumentIndex doc) {
        float score = 0.0f;
        int factors = 0;

        // 标题质量评估
        if (doc.getTitle() != null && !doc.getTitle().trim().isEmpty()) {
            String title = doc.getTitle().trim();
            if (title.length() >= 10 && title.length() <= 100) {
                score += 0.3f;
            } else if (title.length() >= 5) {
                score += 0.2f;
            } else {
                score += 0.1f;
            }
            factors++;
        }

        // 内容长度评估
        if (doc.getContent() != null && !doc.getContent().trim().isEmpty()) {
            int contentLength = doc.getContent().trim().length();
            if (contentLength >= 500) {
                score += 0.3f;
            } else if (contentLength >= 100) {
                score += 0.2f;
            } else {
                score += 0.1f;
            }
            factors++;
        }

        // 摘要质量评估
        if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
            score += 0.2f;
            factors++;
        }

        // 分类信息评估
        if (doc.getCategory() != null && !doc.getCategory().trim().isEmpty()) {
            score += 0.2f;
            factors++;
        }

        return factors > 0 ? score / factors : 0.5f;
    }

    /**
     * 计算热度分数
     */
    private float calculatePopularityScore(DocumentIndex doc) {
        float score = 0.5f; // 基础分数

        // 基于文档结构完整性的简单热度评估
        int completenessFactors = 0;

        if (doc.getTitle() != null && !doc.getTitle().trim().isEmpty()) {
            completenessFactors++;
        }
        if (doc.getContent() != null && doc.getContent().trim().length() > 200) {
            completenessFactors++;
        }
        if (doc.getSummary() != null && !doc.getSummary().trim().isEmpty()) {
            completenessFactors++;
        }
        if (doc.getCategory() != null && !doc.getCategory().trim().isEmpty()) {
            completenessFactors++;
        }

        // 完整性越高，热度分数越高
        score += (completenessFactors / 4.0f) * 0.5f;

        return Math.min(1.0f, score);
    }

    /**
     * 去重并合并相似文档
     * 基于内容相似性去除重复文档
     */
    public List<DocumentIndex> deduplicateResults(List<DocumentIndex> results) {
        Map<String, DocumentIndex> uniqueDocuments = new LinkedHashMap<>();
        Set<String> seenTitles = new HashSet<>();

        for (DocumentIndex doc : results) {
            String docId = doc.getId();
            String title = doc.getTitle();

            // 基于ID去重
            if (uniqueDocuments.containsKey(docId)) {
                continue;
            }

            // 基于标题相似性去重
            if (title != null && isNearDuplicateTitle(title, seenTitles)) {
                log.debug("发现近似重复标题，跳过文档: {}", title);
                continue;
            }

            uniqueDocuments.put(docId, doc);
            if (title != null) {
                seenTitles.add(title.toLowerCase().trim());
            }
        }

        log.debug("去重完成: 原始={}, 去重后={}", results.size(), uniqueDocuments.size());
        return new ArrayList<>(uniqueDocuments.values());
    }

    /**
     * 检查标题是否为近似重复
     */
    private boolean isNearDuplicateTitle(String title, Set<String> seenTitles) {
        String normalizedTitle = title.toLowerCase().trim();

        for (String seenTitle : seenTitles) {
            if (calculateStringSimilarity(normalizedTitle, seenTitle) > 0.8) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算字符串相似度 (简单的Jaccard相似度)
     */
    private double calculateStringSimilarity(String str1, String str2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(str1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(str2.split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * 文档评分内部类
     */
    private static class DocumentScore {
        private final DocumentIndex document;
        private float keywordScore = 0.0f;
        private float semanticScore = 0.0f;
        private float finalScore = 0.0f;

        public DocumentScore(DocumentIndex document) {
            this.document = document;
        }

        public DocumentIndex getDocument() {
            return document;
        }

        public float getKeywordScore() {
            return keywordScore;
        }

        public void setKeywordScore(float keywordScore) {
            this.keywordScore = keywordScore;
        }

        public float getSemanticScore() {
            return semanticScore;
        }

        public void setSemanticScore(float semanticScore) {
            this.semanticScore = semanticScore;
        }

        public float getFinalScore() {
            return finalScore;
        }

        public void setFinalScore(float finalScore) {
            this.finalScore = finalScore;
        }
    }
}