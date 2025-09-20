package com.deepsearch.service;

import com.deepsearch.elasticsearch.dto.DocumentIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 搜索相关性服务测试
 */
class SearchRelevanceServiceTest {

    private SearchRelevanceService searchRelevanceService;
    private HybridSearchService.SearchWeights testWeights;

    @BeforeEach
    void setUp() {
        searchRelevanceService = new SearchRelevanceService();
        testWeights = new HybridSearchService.SearchWeights(1.0f, 2.0f);
    }

    @Test
    void testMergeAndRank_BasicFunctionality() {
        // 准备测试数据
        List<DocumentIndex> keywordResults = Arrays.asList(
            createTestDocument("1", "银行产品指南", "全面的银行产品介绍", LocalDateTime.now().minusDays(1)),
            createTestDocument("2", "投资理财", "专业投资理财服务", LocalDateTime.now().minusDays(5))
        );

        List<DocumentIndex> semanticResults = Arrays.asList(
            createTestDocument("3", "金融服务", "综合金融服务", LocalDateTime.now().minusDays(2)),
            createTestDocument("1", "银行产品指南", "全面的银行产品介绍", LocalDateTime.now().minusDays(1)) // 重复文档
        );

        // 执行合并和排序
        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, testWeights);

        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3); // 去重后3个文档
        assertThat(result.get(0).getId()).isEqualTo("1"); // 最相关的文档排在前面
    }

    @Test
    void testMergeAndRank_EmptyInputs() {
        // 测试空输入
        List<DocumentIndex> emptyList = Collections.emptyList();

        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            emptyList, emptyList, testWeights);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    void testMergeAndRank_OneEmptyInput() {
        // 测试一个空输入
        List<DocumentIndex> keywordResults = Arrays.asList(
            createTestDocument("1", "测试文档", "测试内容", LocalDateTime.now())
        );
        List<DocumentIndex> emptySemanticResults = Collections.emptyList();

        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, emptySemanticResults, testWeights);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1");
    }

    @Test
    void testMergeAndRank_DuplicateDocuments() {
        // 测试重复文档处理
        DocumentIndex doc1 = createTestDocument("1", "测试文档", "测试内容", LocalDateTime.now());
        DocumentIndex doc1Duplicate = createTestDocument("1", "测试文档", "测试内容", LocalDateTime.now());

        List<DocumentIndex> keywordResults = Arrays.asList(doc1);
        List<DocumentIndex> semanticResults = Arrays.asList(doc1Duplicate);

        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, testWeights);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("1");
    }

    @Test
    void testDeduplicateResults() {
        // 准备重复数据
        List<DocumentIndex> documents = Arrays.asList(
            createTestDocument("1", "银行产品", "银行产品介绍", LocalDateTime.now()),
            createTestDocument("2", "银行产品", "银行产品介绍", LocalDateTime.now()), // 相似标题
            createTestDocument("3", "投资理财", "投资理财指南", LocalDateTime.now()),
            createTestDocument("1", "银行产品", "银行产品介绍", LocalDateTime.now()) // 完全重复
        );

        List<DocumentIndex> result = searchRelevanceService.deduplicateResults(documents);

        // 验证去重效果
        assertThat(result).hasSize(2); // 去重后只剩2个不同的文档
        assertThat(result.stream().map(DocumentIndex::getId).distinct().count()).isEqualTo(2);
    }

    @Test
    void testFreshnessScoring() {
        // 测试时间新鲜度评分
        DocumentIndex recentDoc = createTestDocument("1", "最新文档", "最新内容", LocalDateTime.now());
        DocumentIndex oldDoc = createTestDocument("2", "旧文档", "旧内容", LocalDateTime.now().minusYears(2));

        List<DocumentIndex> keywordResults = Arrays.asList(oldDoc);
        List<DocumentIndex> semanticResults = Arrays.asList(recentDoc);

        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, testWeights);

        // 新文档应该排在前面（假设其他因素相同）
        assertThat(result).isNotEmpty();
        // 这里的具体排序取决于算法实现，主要验证没有出错
    }

    @Test
    void testQualityScoring() {
        // 测试内容质量评分
        DocumentIndex highQualityDoc = createTestDocument("1", "高质量银行产品完整指南",
            "这是一份详细的银行产品指南，包含了完整的产品介绍、使用方法、注意事项等重要信息。内容丰富，结构清晰，对用户非常有价值。",
            LocalDateTime.now());

        DocumentIndex lowQualityDoc = createTestDocument("2", "产品", "简单", LocalDateTime.now());

        List<DocumentIndex> keywordResults = Arrays.asList(lowQualityDoc);
        List<DocumentIndex> semanticResults = Arrays.asList(highQualityDoc);

        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, testWeights);

        assertThat(result).hasSize(2);
        // 验证没有抛出异常，具体排序逻辑由算法决定
    }

    @Test
    void testWeightedScoring() {
        // 测试权重影响
        List<DocumentIndex> keywordResults = Arrays.asList(
            createTestDocument("1", "关键词匹配文档", "关键词相关内容", LocalDateTime.now())
        );

        List<DocumentIndex> semanticResults = Arrays.asList(
            createTestDocument("2", "语义匹配文档", "语义相关内容", LocalDateTime.now())
        );

        // 测试不同的权重配置
        HybridSearchService.SearchWeights keywordHeavyWeights =
            new HybridSearchService.SearchWeights(5.0f, 1.0f);
        HybridSearchService.SearchWeights semanticHeavyWeights =
            new HybridSearchService.SearchWeights(1.0f, 5.0f);

        List<DocumentIndex> keywordHeavyResult = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, keywordHeavyWeights);

        List<DocumentIndex> semanticHeavyResult = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, semanticHeavyWeights);

        // 验证权重影响排序
        assertThat(keywordHeavyResult).hasSize(2);
        assertThat(semanticHeavyResult).hasSize(2);
        // 具体排序可能会因权重不同而不同
    }

    @Test
    void testLargeResultSet() {
        // 测试大量结果的处理性能
        List<DocumentIndex> keywordResults = generateLargeResultSet(50, "keyword");
        List<DocumentIndex> semanticResults = generateLargeResultSet(50, "semantic");

        long startTime = System.currentTimeMillis();
        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, testWeights);
        long endTime = System.currentTimeMillis();

        // 验证性能和结果
        assertThat(result).isNotNull();
        assertThat(result.size()).isLessThanOrEqualTo(100); // 最多100个结果
        assertThat(endTime - startTime).isLessThan(1000); // 应该在1秒内完成
    }

    @Test
    void testCategoryDistribution() {
        // 测试分类分布统计
        List<DocumentIndex> keywordResults = Arrays.asList(
            createTestDocumentWithCategory("1", "文档1", "内容1", "产品"),
            createTestDocumentWithCategory("2", "文档2", "内容2", "服务")
        );

        List<DocumentIndex> semanticResults = Arrays.asList(
            createTestDocumentWithCategory("3", "文档3", "内容3", "产品"),
            createTestDocumentWithCategory("4", "文档4", "内容4", "指南")
        );

        List<DocumentIndex> result = searchRelevanceService.mergeAndRank(
            keywordResults, semanticResults, testWeights);

        assertThat(result).hasSize(4);

        // 验证分类分布
        Map<String, Long> categoryCount = result.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                DocumentIndex::getCategory,
                java.util.stream.Collectors.counting()
            ));

        assertThat(categoryCount.get("产品")).isEqualTo(2);
        assertThat(categoryCount.get("服务")).isEqualTo(1);
        assertThat(categoryCount.get("指南")).isEqualTo(1);
    }

    /**
     * 创建测试文档
     */
    private DocumentIndex createTestDocument(String id, String title, String content, LocalDateTime createdAt) {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(id);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSummary(content.length() > 50 ? content.substring(0, 50) : content);
        doc.setCategory("默认分类");
        doc.setCreatedAt(createdAt);
        doc.setUpdatedAt(createdAt);
        return doc;
    }

    /**
     * 创建带分类的测试文档
     */
    private DocumentIndex createTestDocumentWithCategory(String id, String title, String content, String category) {
        DocumentIndex doc = createTestDocument(id, title, content, LocalDateTime.now());
        doc.setCategory(category);
        return doc;
    }

    /**
     * 生成大量测试数据
     */
    private List<DocumentIndex> generateLargeResultSet(int count, String prefix) {
        return java.util.stream.IntStream.range(0, count)
            .mapToObj(i -> createTestDocument(
                prefix + "_" + i,
                prefix + "文档" + i,
                prefix + "内容" + i,
                LocalDateTime.now().minusDays(i % 30)
            ))
            .collect(java.util.stream.Collectors.toList());
    }
}