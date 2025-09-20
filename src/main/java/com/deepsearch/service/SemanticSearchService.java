package com.deepsearch.service;

import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import com.deepsearch.entity.Document;
import com.deepsearch.entity.User;
import com.deepsearch.repository.DocumentRepository;
import com.deepsearch.vector.service.VectorProcessingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义搜索服务 - 提供高级语义搜索功能
 * 包括查询向量化、同义词扩展、场景关联和语义重排序
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchService {

    private final VectorProcessingEngine vectorProcessingEngine;
    private final VectorSimilarityService vectorSimilarityService;
    private final ElasticsearchSearchService elasticsearchSearchService;
    private final DocumentRepository documentRepository;

    @Value("${semantic.search.similarity.threshold:0.7}")
    private double similarityThreshold;

    @Value("${semantic.search.max.expanded.terms:10}")
    private int maxExpandedTerms;

    @Value("${semantic.search.vector.boost:2.0}")
    private float vectorBoost;

    @Value("${semantic.search.top.k:100}")
    private int defaultTopK;

    /**
     * 执行语义搜索
     */
    public List<DocumentResponseDto> semanticSearch(String query, SearchContext context) {
        try {
            log.info("开始语义搜索: {}", query);

            // 1. 查询向量化
            List<Float> queryVector = vectorizeQuery(query);
            if (queryVector == null || queryVector.isEmpty()) {
                log.warn("查询向量化失败，返回空结果");
                return Collections.emptyList();
            }

            // 2. 同义词扩展
            List<String> expandedQueries = expandQueryWithSynonyms(query);

            // 3. 场景关联查询
            SceneContext sceneContext = inferSearchScene(query, context);

            // 4. 执行向量相似度搜索 - 使用VectorSimilarityService
            List<VectorSearchResult> vectorResults = performVectorSimilaritySearch(
                queryVector, sceneContext, defaultTopK);

            // 5. 语义重排序
            List<DocumentResponseDto> rankedResults = semanticRanking(
                vectorResults, expandedQueries, sceneContext);

            log.info("语义搜索完成，返回{}条结果", rankedResults.size());
            return rankedResults;

        } catch (Exception e) {
            log.error("语义搜索执行失败: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 混合语义搜索 - 结合关键词搜索和向量搜索
     */
    public List<DocumentResponseDto> hybridSemanticSearch(String query, SearchContext context, 
                                                         float keywordWeight, float vectorWeight) {
        try {
            log.info("开始混合语义搜索: {}, 关键词权重: {}, 向量权重: {}", query, keywordWeight, vectorWeight);

            // 1. 并行执行关键词搜索和语义搜索
            List<DocumentResponseDto> keywordResults = performKeywordSearch(query, context);
            List<DocumentResponseDto> semanticResults = semanticSearch(query, context);

            // 2. 合并并重新排序结果
            List<DocumentResponseDto> hybridResults = mergeAndRankResults(
                keywordResults, semanticResults, keywordWeight, vectorWeight);

            log.info("混合语义搜索完成，返回{}条结果", hybridResults.size());
            return hybridResults;

        } catch (Exception e) {
            log.error("混合语义搜索失败: {}", query, e);
            return semanticSearch(query, context); // 降级到纯语义搜索
        }
    }

    /**
     * 查询向量化处理
     */
    @Cacheable(value = "queryVectors", key = "#query", unless = "#result == null || #result.isEmpty()")
    public List<Float> vectorizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 文本预处理
            String processedQuery = preprocessQuery(query);

            // 生成向量嵌入
            List<Float> vector = vectorProcessingEngine.generateEmbedding(processedQuery);

            if (vector == null || vector.isEmpty()) {
                log.warn("向量生成失败: {}", query);
                return Collections.emptyList();
            }

            // 向量标准化
            return normalizeVector(vector);

        } catch (Exception e) {
            log.error("查询向量化失败: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 同义词扩展查询
     */
    @Cacheable(value = "expandedQueries", key = "#query")
    public List<String> expandQueryWithSynonyms(String query) {
        Set<String> expandedTerms = new HashSet<>();
        expandedTerms.add(query); // 原查询

        try {
            // 分词处理
            List<String> tokens = tokenizeQuery(query);

            for (String token : tokens) {
                // 获取同义词
                List<String> synonyms = getSynonyms(token);
                expandedTerms.addAll(synonyms);

                // 如果扩展词太多，限制数量
                if (expandedTerms.size() >= maxExpandedTerms) {
                    break;
                }
            }

            log.debug("查询扩展: {} -> {}", query, expandedTerms);
            return new ArrayList<>(expandedTerms);

        } catch (Exception e) {
            log.error("同义词扩展失败: {}", query, e);
            return Collections.singletonList(query);
        }
    }

    /**
     * 场景关联推理
     */
    public SceneContext inferSearchScene(String query, SearchContext context) {
        SceneContext sceneContext = new SceneContext();

        try {
            // 基于查询内容推理场景
            SearchScene scene = classifySearchScene(query);
            sceneContext.setScene(scene);

            // 基于用户上下文推理
            if (context != null) {
                sceneContext.setUserPreferences(context.getUserPreferences());
                sceneContext.setTimeContext(context.getTimeContext());
                sceneContext.setLocationContext(context.getLocationContext());
            }

            // 构建过滤条件
            Map<String, Object> filters = buildSceneFilters(sceneContext);
            sceneContext.setFilters(filters);

            log.debug("场景推理完成: {} -> {}", query, scene);
            return sceneContext;

        } catch (Exception e) {
            log.error("场景推理失败: {}", query, e);
            return sceneContext; // 返回默认场景
        }
    }

    /**
     * 执行向量相似度搜索 - 使用VectorSimilarityService
     */
    private List<VectorSearchResult> performVectorSimilaritySearch(
            List<Float> queryVector, SceneContext sceneContext, int topK) {

        try {
            // 使用VectorSimilarityService进行高效的相似度计算
            List<VectorSimilarityService.SimilarityResult> similarityResults = 
                vectorSimilarityService.batchSimilaritySearch(
                    queryVector, 
                    topK, 
                    VectorSimilarityService.SimilarityAlgorithm.COSINE, 
                    similarityThreshold
                );

            // 转换为VectorSearchResult
            List<VectorSearchResult> results = new ArrayList<>();
            for (VectorSimilarityService.SimilarityResult simResult : similarityResults) {
                try {
                    Optional<Document> documentOpt = documentRepository.findById(simResult.getDocumentId());
                    
                    if (documentOpt.isPresent()) {
                        Document document = documentOpt.get();
                        
                        if (document.getStatus() == Document.Status.INDEXED) {
                            VectorSearchResult result = new VectorSearchResult();
                            result.setDocument(new DocumentResponseDto(document));
                            result.setSimilarityScore(simResult.getSimilarity());
                            result.setVectorScore(simResult.getSimilarity());
                            
                            // 添加算法元数据
                            result.getMetadata().put("algorithm", simResult.getAlgorithm().name());
                            result.getMetadata().put("originalScore", simResult.getSimilarity());
                            
                            results.add(result);
                        }
                    }
                } catch (Exception e) {
                    log.warn("处理相似度结果失败: documentId={}", simResult.getDocumentId(), e);
                }
            }

            log.debug("向量相似度搜索完成，找到{}条结果", results.size());
            return results;

        } catch (Exception e) {
            log.error("向量相似度搜索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行关键词搜索
     */
    private List<DocumentResponseDto> performKeywordSearch(String query, SearchContext context) {
        try {
            String spaceId = context != null ? context.getSpaceId() : null;
            List<String> channels = context != null ? context.getChannels() : null;
            
            List<DocumentIndex> elasticsearchResults = elasticsearchSearchService.keywordSearch(
                query, spaceId, channels, 0, defaultTopK);

            return convertElasticsearchResults(elasticsearchResults);
            
        } catch (IOException e) {
            log.warn("关键词搜索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 合并并重新排序结果
     */
    private List<DocumentResponseDto> mergeAndRankResults(
            List<DocumentResponseDto> keywordResults,
            List<DocumentResponseDto> semanticResults, 
            float keywordWeight,
            float vectorWeight) {
        
        Map<Long, HybridResult> resultMap = new HashMap<>();
        
        // 处理关键词搜索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            DocumentResponseDto doc = keywordResults.get(i);
            double keywordScore = 1.0 - (double) i / keywordResults.size(); // 基于排名的分数
            
            HybridResult hybridResult = resultMap.computeIfAbsent(doc.getId(), 
                k -> new HybridResult(doc));
            hybridResult.setKeywordScore(keywordScore);
        }
        
        // 处理语义搜索结果
        for (int i = 0; i < semanticResults.size(); i++) {
            DocumentResponseDto doc = semanticResults.get(i);
            double semanticScore = 1.0 - (double) i / semanticResults.size(); // 基于排名的分数
            
            HybridResult hybridResult = resultMap.computeIfAbsent(doc.getId(), 
                k -> new HybridResult(doc));
            hybridResult.setSemanticScore(semanticScore);
        }
        
        // 计算混合分数并排序
        return resultMap.values().stream()
            .peek(result -> {
                double hybridScore = result.getKeywordScore() * keywordWeight + 
                                   result.getSemanticScore() * vectorWeight;
                result.setHybridScore(hybridScore);
            })
            .sorted((a, b) -> Double.compare(b.getHybridScore(), a.getHybridScore()))
            .map(HybridResult::getDocument)
            .collect(Collectors.toList());
    }

    /**
     * 语义重排序
     */
    private List<DocumentResponseDto> semanticRanking(
            List<VectorSearchResult> vectorResults,
            List<String> expandedQueries,
            SceneContext sceneContext) {

        try {
            // 计算综合语义分数
            for (VectorSearchResult result : vectorResults) {
                double semanticScore = calculateSemanticRelevanceScore(
                    result, expandedQueries, sceneContext);
                result.setSemanticScore(semanticScore);

                // 计算最终排序分数
                double finalScore = combineScoringFactors(result, sceneContext);
                result.setFinalScore(finalScore);
            }

            // 按最终分数重新排序
            vectorResults.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));

            // 提取排序后的文档
            return vectorResults.stream()
                .map(VectorSearchResult::getDocument)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("语义重排序失败", e);
            // 降级处理：返回原始顺序
            return vectorResults.stream()
                .map(VectorSearchResult::getDocument)
                .collect(Collectors.toList());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 转换Elasticsearch结果为业务DTO
     */
    private List<DocumentResponseDto> convertElasticsearchResults(List<DocumentIndex> elasticsearchResults) {
        List<DocumentResponseDto> results = new ArrayList<>();
        
        for (DocumentIndex docIndex : elasticsearchResults) {
            try {
                Long documentId = Long.parseLong(docIndex.getId());
                Document document = documentRepository.findById(documentId).orElse(null);
                
                if (document != null && document.getStatus() == Document.Status.INDEXED) {
                    results.add(new DocumentResponseDto(document));
                }
            } catch (NumberFormatException e) {
                log.warn("无效的文档ID格式: {}", docIndex.getId());
            }
        }
        
        return results;
    }

    /**
     * 查询预处理
     */
    private String preprocessQuery(String query) {
        if (query == null) return "";

        return query.trim()
            .toLowerCase()
            .replaceAll("[\\p{Punct}&&[^\\u4e00-\\u9fff]]+", " ") // 移除标点但保留中文
            .replaceAll("\\s+", " "); // 标准化空格
    }

    /**
     * 向量标准化
     */
    private List<Float> normalizeVector(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return vector;
        }

        // 计算向量的模长
        double magnitude = vector.stream()
            .mapToDouble(Float::doubleValue)
            .map(x -> x * x)
            .sum();
        magnitude = Math.sqrt(magnitude);

        if (magnitude == 0) {
            return vector;
        }

        // 标准化
        return vector.stream()
            .map(x -> (float) (x / magnitude))
            .collect(Collectors.toList());
    }

    /**
     * 查询分词
     */
    private List<String> tokenizeQuery(String query) {
        // 简单分词实现，实际应用中应使用专业分词器
        return Arrays.stream(query.split("\\s+"))
            .filter(token -> !token.isEmpty() && token.length() > 1)
            .collect(Collectors.toList());
    }

    /**
     * 获取同义词 - 简化实现
     */
    private List<String> getSynonyms(String term) {
        // 这里应该从同义词词典或API获取
        // 简化实现：返回一些常见的银行业务同义词
        Map<String, List<String>> synonymMap = Map.of(
            "银行卡", Arrays.asList("储蓄卡", "借记卡"),
            "信用卡", Arrays.asList("贷记卡", "透支卡"),
            "贷款", Arrays.asList("放款", "借款", "融资"),
            "存款", Arrays.asList("储蓄", "定期", "活期"),
            "理财", Arrays.asList("投资", "财富管理", "资产配置")
        );

        return synonymMap.getOrDefault(term, Collections.emptyList());
    }

    /**
     * 搜索场景分类
     */
    private SearchScene classifySearchScene(String query) {
        // 简化的场景分类逻辑
        if (query.contains("贷款") || query.contains("借款")) {
            return SearchScene.LOAN;
        } else if (query.contains("理财") || query.contains("投资")) {
            return SearchScene.WEALTH_MANAGEMENT;
        } else if (query.contains("银行卡") || query.contains("信用卡")) {
            return SearchScene.CARD_SERVICE;
        } else if (query.contains("存款") || query.contains("储蓄")) {
            return SearchScene.DEPOSIT;
        } else {
            return SearchScene.GENERAL;
        }
    }

    /**
     * 构建场景过滤条件
     */
    private Map<String, Object> buildSceneFilters(SceneContext sceneContext) {
        Map<String, Object> filters = new HashMap<>();

        if (sceneContext.getScene() != null) {
            filters.put("scene", sceneContext.getScene().name());
        }

        return filters;
    }

    /**
     * 计算语义相关性分数
     */
    private double calculateSemanticRelevanceScore(
            VectorSearchResult result,
            List<String> expandedQueries,
            SceneContext sceneContext) {

        double baseScore = result.getSimilarityScore();

        // 同义词匹配加权
        double synonymBoost = calculateSynonymMatchScore(result.getDocument(), expandedQueries);

        // 场景相关性加权
        double sceneBoost = calculateSceneRelevanceScore(result.getDocument(), sceneContext);

        return baseScore * 0.6 + synonymBoost * 0.2 + sceneBoost * 0.2;
    }

    /**
     * 计算同义词匹配分数
     */
    private double calculateSynonymMatchScore(DocumentResponseDto document, List<String> expandedQueries) {
        String content = (document.getTitle() + " " + document.getContent()).toLowerCase();

        long matchCount = expandedQueries.stream()
            .mapToLong(query -> content.contains(query.toLowerCase()) ? 1 : 0)
            .sum();

        return (double) matchCount / expandedQueries.size();
    }

    /**
     * 计算场景相关性分数
     */
    private double calculateSceneRelevanceScore(DocumentResponseDto document, SceneContext sceneContext) {
        // 简化实现：基于文档类别与场景的匹配度
        if (sceneContext.getScene() == null) {
            return 0.5; // 默认中等相关性
        }

        String category = document.getCategory();
        if (category == null) {
            return 0.5;
        }

        // 根据场景类型匹配文档类别
        switch (sceneContext.getScene()) {
            case LOAN:
                return category.contains("贷款") ? 1.0 : 0.3;
            case WEALTH_MANAGEMENT:
                return category.contains("理财") ? 1.0 : 0.3;
            case CARD_SERVICE:
                return category.contains("银行卡") ? 1.0 : 0.3;
            case DEPOSIT:
                return category.contains("存款") ? 1.0 : 0.3;
            default:
                return 0.5;
        }
    }

    /**
     * 综合评分因子计算
     */
    private double combineScoringFactors(VectorSearchResult result, SceneContext sceneContext) {
        double vectorScore = result.getVectorScore() * 0.4;
        double semanticScore = result.getSemanticScore() * 0.4;
        double freshnessScore = calculateFreshnessScore(result.getDocument()) * 0.1;
        double popularityScore = calculatePopularityScore(result.getDocument()) * 0.1;

        return vectorScore + semanticScore + freshnessScore + popularityScore;
    }

    /**
     * 计算时效性分数
     */
    private double calculateFreshnessScore(DocumentResponseDto document) {
        // 简化实现：基于创建时间的时效性计算
        if (document.getCreatedAt() == null) {
            return 0.5;
        }

        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(
            document.getCreatedAt().toInstant(),
            java.time.Instant.now()
        );

        // 30天内的文档时效性较高
        return Math.max(0.1, 1.0 - (daysSinceCreation / 30.0));
    }

    /**
     * 计算流行度分数
     */
    private double calculatePopularityScore(DocumentResponseDto document) {
        // 简化实现：这里应该基于文档的浏览量、点赞数等
        return 0.5; // 默认中等流行度
    }

    // ==================== 内部类定义 ====================

    /**
     * 搜索上下文
     */
    public static class SearchContext {
        private Map<String, Object> userPreferences;
        private TimeContext timeContext;
        private LocationContext locationContext;
        private String spaceId;
        private List<String> channels;

        // Getters and Setters
        public Map<String, Object> getUserPreferences() { return userPreferences; }
        public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
        public TimeContext getTimeContext() { return timeContext; }
        public void setTimeContext(TimeContext timeContext) { this.timeContext = timeContext; }
        public LocationContext getLocationContext() { return locationContext; }
        public void setLocationContext(LocationContext locationContext) { this.locationContext = locationContext; }
        public String getSpaceId() { return spaceId; }
        public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
        public List<String> getChannels() { return channels; }
        public void setChannels(List<String> channels) { this.channels = channels; }
    }

    /**
     * 场景上下文
     */
    public static class SceneContext {
        private SearchScene scene;
        private Map<String, Object> userPreferences;
        private TimeContext timeContext;
        private LocationContext locationContext;
        private Map<String, Object> filters;

        // Getters and Setters
        public SearchScene getScene() { return scene; }
        public void setScene(SearchScene scene) { this.scene = scene; }
        public Map<String, Object> getUserPreferences() { return userPreferences; }
        public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
        public TimeContext getTimeContext() { return timeContext; }
        public void setTimeContext(TimeContext timeContext) { this.timeContext = timeContext; }
        public LocationContext getLocationContext() { return locationContext; }
        public void setLocationContext(LocationContext locationContext) { this.locationContext = locationContext; }
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }

        public String getSpaceId() {
            return filters != null ? (String) filters.get("spaceId") : null;
        }

        @SuppressWarnings("unchecked")
        public List<String> getChannels() {
            return filters != null ? (List<String>) filters.get("channels") : null;
        }
    }

    /**
     * 搜索场景枚举
     */
    public enum SearchScene {
        GENERAL,           // 通用搜索
        LOAN,              // 贷款相关
        WEALTH_MANAGEMENT, // 理财相关
        CARD_SERVICE,      // 银行卡服务
        DEPOSIT            // 存款相关
    }

    /**
     * 时间上下文
     */
    public static class TimeContext {
        private java.time.LocalDateTime currentTime;
        private String timeOfDay; // morning, afternoon, evening, night

        // Getters and Setters
        public java.time.LocalDateTime getCurrentTime() { return currentTime; }
        public void setCurrentTime(java.time.LocalDateTime currentTime) { this.currentTime = currentTime; }
        public String getTimeOfDay() { return timeOfDay; }
        public void setTimeOfDay(String timeOfDay) { this.timeOfDay = timeOfDay; }
    }

    /**
     * 位置上下文
     */
    public static class LocationContext {
        private String region;
        private String city;

        // Getters and Setters
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    /**
     * 向量搜索结果
     */
    public static class VectorSearchResult {
        private DocumentResponseDto document;
        private double similarityScore;
        private double vectorScore;
        private double semanticScore;
        private double finalScore;
        private Map<String, Object> metadata;

        public VectorSearchResult() {
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public DocumentResponseDto getDocument() { return document; }
        public void setDocument(DocumentResponseDto document) { this.document = document; }
        public double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
        public double getVectorScore() { return vectorScore; }
        public void setVectorScore(double vectorScore) { this.vectorScore = vectorScore; }
        public double getSemanticScore() { return semanticScore; }
        public void setSemanticScore(double semanticScore) { this.semanticScore = semanticScore; }
        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 混合搜索结果
     */
    private static class HybridResult {
        private DocumentResponseDto document;
        private double keywordScore = 0.0;
        private double semanticScore = 0.0;
        private double hybridScore = 0.0;

        public HybridResult(DocumentResponseDto document) {
            this.document = document;
        }

        // Getters and Setters
        public DocumentResponseDto getDocument() { return document; }
        public double getKeywordScore() { return keywordScore; }
        public void setKeywordScore(double keywordScore) { this.keywordScore = keywordScore; }
        public double getSemanticScore() { return semanticScore; }
        public void setSemanticScore(double semanticScore) { this.semanticScore = semanticScore; }
        public double getHybridScore() { return hybridScore; }
        public void setHybridScore(double hybridScore) { this.hybridScore = hybridScore; }
    }
}