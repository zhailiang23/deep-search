package com.deepsearch.service;

import com.deepsearch.entity.SearchLog;
import com.deepsearch.repository.SearchLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * TrieService单元测试
 */
@ExtendWith(MockitoExtension.class)
class TrieServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private TrieService trieService;

    @BeforeEach
    void setUp() {
        // 模拟搜索日志数据
        List<SearchLog> mockLogs = Arrays.asList(
                createSearchLog("银行产品", 5),
                createSearchLog("银行服务", 3),
                createSearchLog("银行卡", 8),
                createSearchLog("理财产品", 4),
                createSearchLog("贷款服务", 6)
        );

        when(searchLogRepository.findRecentSearchLogs(anyInt())).thenReturn(mockLogs);

        // 初始化Trie服务
        trieService.initialize();
    }

    @Test
    void testAddSingleWord() {
        // 测试添加单个词条
        trieService.addWord("信用卡", 10);

        assertTrue(trieService.contains("信用卡"));
        assertEquals(10, trieService.getFrequency("信用卡"));
    }

    @Test
    void testAddMultipleWords() {
        // 测试批量添加词条
        Map<String, Integer> words = new HashMap<>();
        words.put("存款", 5);
        words.put("转账", 3);
        words.put("查询", 7);

        trieService.addWords(words);

        assertTrue(trieService.contains("存款"));
        assertTrue(trieService.contains("转账"));
        assertTrue(trieService.contains("查询"));
        assertEquals(5, trieService.getFrequency("存款"));
        assertEquals(3, trieService.getFrequency("转账"));
        assertEquals(7, trieService.getFrequency("查询"));
    }

    @Test
    void testPrefixMatches() {
        // 测试前缀匹配
        List<String> matches = trieService.getPrefixMatches("银行", 10);

        assertNotNull(matches);
        assertFalse(matches.isEmpty());

        // 验证所有匹配项都以"银行"开头
        for (String match : matches) {
            assertTrue(match.startsWith("银行"), "匹配项应以'银行'开头: " + match);
        }

        // 验证结果按频率排序（频率高的在前）
        if (matches.size() >= 2) {
            int freq1 = trieService.getFrequency(matches.get(0));
            int freq2 = trieService.getFrequency(matches.get(1));
            assertTrue(freq1 >= freq2, "结果应按频率降序排列");
        }
    }

    @Test
    void testPrefixMatchesWithLimit() {
        // 测试限制返回数量的前缀匹配
        List<String> matches = trieService.getPrefixMatches("银行", 2);

        assertNotNull(matches);
        assertTrue(matches.size() <= 2, "返回结果数量不应超过限制");
    }

    @Test
    void testEmptyPrefixMatches() {
        // 测试空前缀查询
        List<String> matches = trieService.getPrefixMatches("", 10);
        assertTrue(matches.isEmpty(), "空前缀应返回空列表");

        matches = trieService.getPrefixMatches(null, 10);
        assertTrue(matches.isEmpty(), "null前缀应返回空列表");
    }

    @Test
    void testNonExistentPrefixMatches() {
        // 测试不存在的前缀查询
        List<String> matches = trieService.getPrefixMatches("不存在的前缀", 10);
        assertTrue(matches.isEmpty(), "不存在的前缀应返回空列表");
    }

    @Test
    void testContains() {
        // 测试词条存在性检查
        assertTrue(trieService.contains("银行产品"));
        assertTrue(trieService.contains("理财产品"));
        assertFalse(trieService.contains("不存在的词"));
        assertFalse(trieService.contains(""));
        assertFalse(trieService.contains(null));
    }

    @Test
    void testIncrementFrequency() {
        // 测试增加词条频率
        String word = "银行产品";
        int originalFreq = trieService.getFrequency(word);

        trieService.incrementFrequency(word);

        assertEquals(originalFreq + 1, trieService.getFrequency(word));
    }

    @Test
    void testIncrementFrequencyNewWord() {
        // 测试对新词条增加频率
        String newWord = "新产品";
        assertFalse(trieService.contains(newWord));

        trieService.incrementFrequency(newWord);

        assertTrue(trieService.contains(newWord));
        assertEquals(1, trieService.getFrequency(newWord));
    }

    @Test
    void testGetTopTerms() {
        // 测试获取最热门词条
        List<String> topTerms = trieService.getTopTerms(3);

        assertNotNull(topTerms);
        assertTrue(topTerms.size() <= 3, "返回数量不应超过限制");

        // 验证按频率降序排列
        for (int i = 0; i < topTerms.size() - 1; i++) {
            int freq1 = trieService.getFrequency(topTerms.get(i));
            int freq2 = trieService.getFrequency(topTerms.get(i + 1));
            assertTrue(freq1 >= freq2, "热门词条应按频率降序排列");
        }
    }

    @Test
    void testGetStats() {
        // 测试获取统计信息
        Map<String, Object> stats = trieService.getStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("totalTerms"));
        assertTrue(stats.containsKey("totalFrequency"));

        int totalTerms = (Integer) stats.get("totalTerms");
        assertTrue(totalTerms > 0, "总词条数应大于0");

        if (totalTerms > 0) {
            assertTrue(stats.containsKey("maxFrequency"));
            assertTrue(stats.containsKey("minFrequency"));
            assertTrue(stats.containsKey("averageFrequency"));

            int maxFreq = (Integer) stats.get("maxFrequency");
            int minFreq = (Integer) stats.get("minFrequency");
            double avgFreq = (Double) stats.get("averageFrequency");

            assertTrue(maxFreq >= minFreq, "最大频率应大于等于最小频率");
            assertTrue(avgFreq >= minFreq && avgFreq <= maxFreq, "平均频率应在最小和最大频率之间");
        }
    }

    @Test
    void testClear() {
        // 测试清空Trie树
        assertTrue(trieService.getStats().get("totalTerms").equals(5), "初始应有5个词条");

        trieService.clear();

        Map<String, Object> stats = trieService.getStats();
        assertEquals(0, stats.get("totalTerms"), "清空后应无词条");
    }

    @Test
    void testCaseInsensitivity() {
        // 测试大小写不敏感
        trieService.addWord("Test", 1);

        assertTrue(trieService.contains("test"));
        assertTrue(trieService.contains("TEST"));
        assertTrue(trieService.contains("Test"));

        List<String> matches = trieService.getPrefixMatches("te", 10);
        assertTrue(matches.stream().anyMatch(word -> word.equals("test")));
    }

    @Test
    void testWhitespaceHandling() {
        // 测试空白字符处理
        trieService.addWord("  银行业务  ", 1);

        assertTrue(trieService.contains("银行业务"));
        assertFalse(trieService.contains("  银行业务  "));

        // 测试空字符串和null处理
        trieService.addWord("", 1);
        trieService.addWord(null, 1);
        trieService.addWord("   ", 1);

        assertFalse(trieService.contains(""));
        assertFalse(trieService.contains(null));
        assertFalse(trieService.contains("   "));
    }

    @Test
    void testThreadSafety() throws InterruptedException {
        // 测试线程安全性
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    String word = "thread" + threadIndex + "word" + j;
                    trieService.addWord(word, j + 1);
                    trieService.incrementFrequency(word);
                    trieService.getPrefixMatches("thread", 10);
                }
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

        // 验证数据一致性
        Map<String, Object> stats = trieService.getStats();
        int totalTerms = (Integer) stats.get("totalTerms");
        assertTrue(totalTerms >= 5, "应包含初始词条和线程添加的词条"); // 至少包含初始的5个词条
    }

    /**
     * 创建测试用的SearchLog
     */
    private SearchLog createSearchLog(String queryText, int frequency) {
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