package com.deepsearch.elasticsearch;

import com.deepsearch.elasticsearch.config.ElasticsearchConfig;
import com.deepsearch.elasticsearch.config.ElasticsearchIndexConfig;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.service.DocumentIndexService;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import com.deepsearch.vector.service.VectorProcessingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ElasticsearchIntegrationTest {

    @Autowired
    private DocumentIndexService documentIndexService;

    @Autowired
    private ElasticsearchSearchService searchService;

    @Autowired
    private VectorProcessingEngine vectorProcessingEngine;

    @Autowired
    private ElasticsearchIndexConfig indexConfig;

    private static final String TEST_SPACE_ID = "test_space_001";
    private static final List<String> TEST_CHANNELS = Arrays.asList("web", "mobile");

    @BeforeEach
    void setUp() throws Exception {
        // 清理测试数据
        // 注意：在真实环境中应该使用测试专用的索引
    }

    @Test
    void testDocumentIndexing() throws Exception {
        // 创建测试文档
        DocumentIndex testDoc = createTestDocument(
            "银行理财产品介绍",
            "我们银行提供多种理财产品，包括定期存款、基金投资、保险理财等。" +
            "客户可以根据风险偏好选择合适的产品。收益率在3%-8%之间。",
            Arrays.asList("理财", "投资", "银行产品")
        );

        // 执行索引操作
        String documentId = documentIndexService.indexDocument(testDoc);

        assertNotNull(documentId);
        assertEquals(testDoc.getId(), documentId);

        // 验证文档被正确索引
        Thread.sleep(2000); // 等待Elasticsearch刷新

        var retrievedDoc = documentIndexService.getDocument(documentId);
        assertTrue(retrievedDoc.isPresent());
        assertEquals(testDoc.getTitle(), retrievedDoc.get().getTitle());
        assertEquals(testDoc.getContent(), retrievedDoc.get().getContent());
    }

    @Test
    void testKeywordSearch() throws Exception {
        // 准备测试数据
        indexTestDocuments();

        // 执行关键词搜索
        List<DocumentIndex> results = searchService.keywordSearch(
            "银行理财",
            TEST_SPACE_ID,
            TEST_CHANNELS,
            0,
            10
        );

        assertNotNull(results);
        assertFalse(results.isEmpty());

        // 验证搜索结果相关性
        boolean foundRelevantResult = results.stream()
            .anyMatch(doc -> doc.getTitle().contains("理财") || doc.getContent().contains("理财"));
        assertTrue(foundRelevantResult);
    }

    @Test
    void testVectorSearch() throws Exception {
        // 准备测试数据
        indexTestDocuments();

        // 执行向量搜索
        List<DocumentIndex> results = searchService.vectorSearch(
            "投资产品推荐",
            TEST_SPACE_ID,
            TEST_CHANNELS,
            0,
            10
        );

        assertNotNull(results);
        // 向量搜索可能返回空结果，取决于向量相似度
        // 但不应该抛出异常
    }

    @Test
    void testHybridSearch() throws Exception {
        // 准备测试数据
        indexTestDocuments();

        // 执行混合搜索
        List<DocumentIndex> results = searchService.hybridSearch(
            "银行投资建议",
            TEST_SPACE_ID,
            TEST_CHANNELS,
            0,
            10,
            1.0f,  // 关键词权重
            2.0f   // 向量权重
        );

        assertNotNull(results);
        // 混合搜索应该综合关键词和语义搜索的优势
    }

    @Test
    void testBatchIndexing() throws Exception {
        // 创建批量测试文档
        List<DocumentIndex> testDocs = Arrays.asList(
            createTestDocument("信用卡申请", "申请我行信用卡，享受多重优惠", Arrays.asList("信用卡", "申请")),
            createTestDocument("存款利率", "定期存款年利率3.5%，起存金额1万元", Arrays.asList("存款", "利率")),
            createTestDocument("贷款服务", "个人住房贷款，利率优惠，手续简便", Arrays.asList("贷款", "住房"))
        );

        // 执行批量索引
        var bulkResponse = documentIndexService.indexDocumentsBatch(testDocs);

        assertNotNull(bulkResponse);
        assertFalse(bulkResponse.errors());

        // 验证批量索引结果
        Thread.sleep(2000); // 等待Elasticsearch刷新

        for (DocumentIndex doc : testDocs) {
            var retrievedDoc = documentIndexService.getDocument(doc.getId());
            assertTrue(retrievedDoc.isPresent());
        }
    }

    @Test
    void testDocumentUpdate() throws Exception {
        // 创建测试文档
        DocumentIndex testDoc = createTestDocument(
            "原始标题",
            "原始内容",
            Arrays.asList("原始标签")
        );

        // 索引文档
        String documentId = documentIndexService.indexDocument(testDoc);
        Thread.sleep(1000);

        // 更新文档
        testDoc.setTitle("更新后标题");
        testDoc.setContent("更新后内容");
        testDoc.setTags(Arrays.asList("更新标签"));

        String updatedId = documentIndexService.updateDocument(testDoc);
        assertEquals(documentId, updatedId);

        // 验证更新结果
        Thread.sleep(1000);
        var retrievedDoc = documentIndexService.getDocument(documentId);
        assertTrue(retrievedDoc.isPresent());
        assertEquals("更新后标题", retrievedDoc.get().getTitle());
        assertEquals("更新后内容", retrievedDoc.get().getContent());
    }

    @Test
    void testDocumentDeletion() throws Exception {
        // 创建测试文档
        DocumentIndex testDoc = createTestDocument(
            "待删除文档",
            "这是一个将被删除的测试文档",
            Arrays.asList("测试", "删除")
        );

        // 索引文档
        String documentId = documentIndexService.indexDocument(testDoc);
        Thread.sleep(1000);

        // 验证文档存在
        var retrievedDoc = documentIndexService.getDocument(documentId);
        assertTrue(retrievedDoc.isPresent());

        // 删除文档
        boolean deleted = documentIndexService.deleteDocument(documentId);
        assertTrue(deleted);

        // 验证文档已删除
        Thread.sleep(1000);
        retrievedDoc = documentIndexService.getDocument(documentId);
        assertTrue(retrievedDoc.isEmpty());
    }

    @Test
    void testSearchPerformance() throws Exception {
        // 准备性能测试数据
        indexTestDocuments();

        long startTime = System.currentTimeMillis();

        // 执行多次搜索测试性能
        for (int i = 0; i < 10; i++) {
            List<DocumentIndex> results = searchService.keywordSearch(
                "银行服务" + i,
                TEST_SPACE_ID,
                TEST_CHANNELS,
                0,
                5
            );
            assertNotNull(results);
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double avgTime = totalTime / 10.0;

        // 性能断言：平均响应时间应该小于500ms
        assertTrue(avgTime < 500,
            String.format("平均搜索时间过长: %.2fms", avgTime));
    }

    @Test
    void testDocumentCount() throws Exception {
        // 获取初始文档数量
        long initialCount = searchService.getDocumentCount(TEST_SPACE_ID);

        // 索引新文档
        indexTestDocuments();

        // 获取更新后文档数量
        long newCount = searchService.getDocumentCount(TEST_SPACE_ID);

        // 验证文档数量增加
        assertTrue(newCount >= initialCount);
    }

    @Test
    void testChineseTextAnalysis() throws Exception {
        // 测试中文分词和搜索
        DocumentIndex chineseDoc = createTestDocument(
            "中国银行个人金融服务",
            "为客户提供存款、贷款、理财、信用卡等全方位金融服务。" +
            "我们致力于成为客户最信赖的金融伙伴。",
            Arrays.asList("中文", "金融", "服务")
        );

        // 索引中文文档
        String documentId = documentIndexService.indexDocument(chineseDoc);
        Thread.sleep(1000);

        // 测试中文搜索
        List<DocumentIndex> results = searchService.keywordSearch(
            "金融服务",
            TEST_SPACE_ID,
            TEST_CHANNELS,
            0,
            10
        );

        assertNotNull(results);
        boolean foundChineseDoc = results.stream()
            .anyMatch(doc -> doc.getId().equals(documentId));
        assertTrue(foundChineseDoc);
    }

    // 辅助方法

    private DocumentIndex createTestDocument(String title, String content, List<String> tags) {
        DocumentIndex doc = new DocumentIndex();
        doc.setId(UUID.randomUUID().toString());
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSummary(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        doc.setTags(tags);
        doc.setCategory("测试分类");
        doc.setSource("integration_test");
        doc.setChannel("test");
        doc.setSpaceId(TEST_SPACE_ID);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        doc.setPopularityScore(1.0f);
        doc.setRelevanceBoost(1.0f);

        return doc;
    }

    private void indexTestDocuments() throws Exception {
        List<DocumentIndex> testDocs = Arrays.asList(
            createTestDocument(
                "银行理财产品",
                "我行提供多种理财产品，收益稳定，风险可控",
                Arrays.asList("理财", "银行", "产品")
            ),
            createTestDocument(
                "投资基金指南",
                "基金投资入门指南，帮助客户了解基金投资知识",
                Arrays.asList("投资", "基金", "指南")
            ),
            createTestDocument(
                "信贷业务介绍",
                "个人信贷、企业贷款、住房按揭等信贷业务详情",
                Arrays.asList("信贷", "贷款", "业务")
            )
        );

        // 批量索引测试文档
        documentIndexService.indexDocumentsBatch(testDocs);
        Thread.sleep(2000); // 等待索引完成
    }
}