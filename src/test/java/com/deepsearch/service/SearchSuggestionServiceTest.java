package com.deepsearch.service;

import com.deepsearch.dto.SearchSuggestionResponse;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.repository.SearchLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SearchSuggestionService单元测试
 */
@ExtendWith(MockitoExtension.class)
class SearchSuggestionServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private TrieService trieService;

    @InjectMocks
    private SearchSuggestionService searchSuggestionService;

    @BeforeEach
    void setUp() {
        // 模拟搜索历史数据
        when(searchLogRepository.findByQueryTextContaining(anyString(), any(Pageable.class)))
                .thenReturn(Arrays.asList(
                        createSearchLog("银行产品详细信息"),
                        createSearchLog("银行产品介绍"),
                        createSearchLog("银行产品特色")
                ));

        when(searchLogRepository.findRecentSearchLogs(anyInt()))
                .thenReturn(Arrays.asList(
                        createSearchLog("银行产品"),
                        createSearchLog("理财服务"),
                        createSearchLog("贷款申请"),
                        createSearchLog("信用卡办理")
                ));

        // 模拟Trie服务
        when(trieService.getTopTerms(anyInt()))
                .thenReturn(Arrays.asList("银行产品", "理财服务", "贷款申请", "信用卡"));
    }

    @Test
    void testGenerateSuggestionsWithFewResults() {
        // 测试结果太少时的建议生成
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 3, 5);

        assertNotNull(response);
        assertEquals("银行", response.getOriginalQuery());
        assertNotNull(response.getExpansionSuggestions());
        assertFalse(response.getExpansionSuggestions().isEmpty());
        assertNull(response.getNarrowingSuggestions()); // 结果少时不应有缩小建议
    }

    @Test
    void testGenerateSuggestionsWithManyResults() {
        // 测试结果太多时的建议生成
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("产品", 1500, 5);

        assertNotNull(response);
        assertEquals("产品", response.getOriginalQuery());
        assertNotNull(response.getNarrowingSuggestions());
        assertFalse(response.getNarrowingSuggestions().isEmpty());
        assertNull(response.getExpansionSuggestions()); // 结果多时不应有扩展建议
    }

    @Test
    void testGenerateSuggestionsWithNoResults() {
        // 测试无结果时的建议生成
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("不存在的查询", 0, 5);

        assertNotNull(response);
        assertEquals("不存在的查询", response.getOriginalQuery());
        assertNotNull(response.getSpellingSuggestions());
        // 可能为空，因为拼写纠错依赖于现有数据
    }

    @Test
    void testGenerateSuggestionsWithNormalResults() {
        // 测试正常结果数量时的建议生成
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 50, 5);

        assertNotNull(response);
        assertEquals("银行", response.getOriginalQuery());
        assertNotNull(response.getRelatedQueries());
        assertFalse(response.getRelatedQueries().isEmpty());

        // 正常结果数量时不应有扩展或缩小建议
        assertNull(response.getExpansionSuggestions());
        assertNull(response.getNarrowingSuggestions());
        assertNull(response.getSpellingSuggestions());
    }

    @Test
    void testGenerateSuggestionsWithEmptyQuery() {
        // 测试空查询
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("", 10, 5);

        assertNotNull(response);
        assertNull(response.getOriginalQuery());
        assertEquals(0, response.getTotalSuggestions());
    }

    @Test
    void testGenerateSuggestionsWithNullQuery() {
        // 测试null查询
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions(null, 10, 5);

        assertNotNull(response);
        assertNull(response.getOriginalQuery());
        assertEquals(0, response.getTotalSuggestions());
    }

    @Test
    void testExpansionSuggestionsForBankingDomain() {
        // 测试银行领域的扩展建议
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 2, 10);

        assertNotNull(response.getExpansionSuggestions());
        List<String> expansions = response.getExpansionSuggestions();

        // 验证包含银行相关的扩展词
        boolean hasBankingTerms = expansions.stream()
                .anyMatch(s -> s.contains("手续费") || s.contains("利率") || s.contains("条件") || s.contains("流程"));
        assertTrue(hasBankingTerms, "银行领域应包含相关扩展建议");
    }

    @Test
    void testExpansionSuggestionsForProductDomain() {
        // 测试产品领域的扩展建议
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("产品", 3, 10);

        assertNotNull(response.getExpansionSuggestions());
        List<String> expansions = response.getExpansionSuggestions();

        // 验证包含产品相关的扩展词
        boolean hasProductTerms = expansions.stream()
                .anyMatch(s -> s.contains("特色") || s.contains("优势") || s.contains("说明") || s.contains("介绍"));
        assertTrue(hasProductTerms, "产品领域应包含相关扩展建议");
    }

    @Test
    void testNarrowingSuggestions() {
        // 测试缩小建议
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 2000, 10);

        assertNotNull(response.getNarrowingSuggestions());
        List<String> narrowing = response.getNarrowingSuggestions();

        // 验证包含限定词
        boolean hasQualifiers = narrowing.stream()
                .anyMatch(s -> s.contains("个人") || s.contains("企业") || s.contains("网上") || s.contains("手机"));
        assertTrue(hasQualifiers, "缩小建议应包含限定词");
    }

    @Test
    void testRelatedQueries() {
        // 测试相关查询建议
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 100, 5);

        assertNotNull(response.getRelatedQueries());
        List<String> related = response.getRelatedQueries();

        // 验证相关查询不包含原查询
        boolean containsOriginal = related.contains("银行");
        assertFalse(containsOriginal, "相关查询不应包含原查询");

        // 验证所有相关查询都不为空
        boolean allNonEmpty = related.stream().allMatch(q -> q != null && !q.trim().isEmpty());
        assertTrue(allNonEmpty, "所有相关查询都应非空");
    }

    @Test
    void testSpellingSuggestions() {
        // 测试拼写纠错建议
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("yinhang", 0, 5);

        assertNotNull(response.getSpellingSuggestions());
        // 由于依赖预定义的拼写错误映射，这里主要测试功能不抛异常
    }

    @Test
    void testSuggestionLimit() {
        // 测试建议数量限制
        int limit = 3;
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 2, limit);

        assertNotNull(response.getExpansionSuggestions());
        assertTrue(response.getExpansionSuggestions().size() <= limit,
                "扩展建议数量不应超过限制");
    }

    @Test
    void testResponseTiming() {
        // 测试响应时间记录
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 50, 5);

        assertNotNull(response.getResponseTimeMs());
        assertTrue(response.getResponseTimeMs() >= 0, "响应时间应大于等于0");
    }

    @Test
    void testTotalSuggestionsCount() {
        // 测试总建议数统计
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 2, 5);

        int expectedTotal = 0;
        if (response.getExpansionSuggestions() != null) {
            expectedTotal += response.getExpansionSuggestions().size();
        }
        if (response.getNarrowingSuggestions() != null) {
            expectedTotal += response.getNarrowingSuggestions().size();
        }
        if (response.getRelatedQueries() != null) {
            expectedTotal += response.getRelatedQueries().size();
        }
        if (response.getSpellingSuggestions() != null) {
            expectedTotal += response.getSpellingSuggestions().size();
        }

        assertEquals(expectedTotal, response.getTotalSuggestions(),
                "总建议数应等于各类建议的总和");
    }

    @Test
    void testCacheStats() {
        // 测试缓存统计
        Map<String, Object> stats = searchSuggestionService.getCacheStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("suggestionCacheStats"));
    }

    @Test
    void testErrorHandling() {
        // 测试错误处理
        when(searchLogRepository.findByQueryTextContaining(anyString(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // 即使数据库出错，也应该能够返回基本建议
        SearchSuggestionResponse response = searchSuggestionService.generateSuggestions("银行", 2, 5);

        assertNotNull(response);
        assertEquals("银行", response.getOriginalQuery());
        assertNotNull(response.getTotalSuggestions());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        // 测试并发访问
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        SearchSuggestionResponse[] responses = new SearchSuggestionResponse[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                responses[index] = searchSuggestionService.generateSuggestions("银行" + index, 50, 5);
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有响应都成功
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(responses[i], "响应 " + i + " 不应为null");
            assertEquals("银行" + i, responses[i].getOriginalQuery());
        }
    }

    @Test
    void testDifferentResultCountThresholds() {
        // 测试不同结果数量阈值的处理

        // 测试边界值
        SearchSuggestionResponse response1 = searchSuggestionService.generateSuggestions("test", 5, 5);
        assertNull(response1.getExpansionSuggestions()); // 正好5个结果，不需要扩展

        SearchSuggestionResponse response2 = searchSuggestionService.generateSuggestions("test", 4, 5);
        assertNotNull(response2.getExpansionSuggestions()); // 少于5个结果，需要扩展

        SearchSuggestionResponse response3 = searchSuggestionService.generateSuggestions("test", 1000, 5);
        assertNull(response3.getNarrowingSuggestions()); // 正好1000个结果，不需要缩小

        SearchSuggestionResponse response4 = searchSuggestionService.generateSuggestions("test", 1001, 5);
        assertNotNull(response4.getNarrowingSuggestions()); // 超过1000个结果，需要缩小
    }

    /**
     * 创建测试用的SearchLog
     */
    private SearchLog createSearchLog(String queryText) {
        SearchLog log = new SearchLog();
        log.setQueryText(queryText);
        log.setUserId(1L);
        log.setSearchType(SearchLog.SearchType.KEYWORD);
        log.setResultCount(10);
        log.setResponseTimeMs(100);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}