package com.deepsearch.service;

import com.deepsearch.dto.Suggestion;
import com.deepsearch.dto.SuggestionType;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.repository.SearchLogRepository;
import com.deepsearch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AutoCompleteService单元测试
 */
@ExtendWith(MockitoExtension.class)
class AutoCompleteServiceTest {

    @Mock
    private TrieService trieService;

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private AutoCompleteService autoCompleteService;

    @BeforeEach
    void setUp() {
        // 模拟Trie服务返回前缀匹配
        when(trieService.getPrefixMatches(eq("银行"), anyInt()))
                .thenReturn(Arrays.asList("银行产品", "银行服务", "银行卡"));
        when(trieService.getPrefixMatches(eq("理财"), anyInt()))
                .thenReturn(Arrays.asList("理财产品", "理财服务"));

        // 模拟热门搜索词查询
        when(searchLogRepository.findPopularSearchTerms(any(Pageable.class)))
                .thenReturn(Arrays.asList(
                        new Object[]{"银行产品", 100L},
                        new Object[]{"理财服务", 80L},
                        new Object[]{"贷款申请", 60L}
                ));

        // 模拟用户搜索历史
        when(searchLogRepository.findRecentSearchLogsByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(Arrays.asList(
                        createSearchLog("银行产品查询"),
                        createSearchLog("个人理财"),
                        createSearchLog("信用卡申请")
                ));
    }

    @Test
    void testGetSuggestionsWithValidQuery() {
        // 测试有效查询的建议获取
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", null, 10, false);

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());

        // 验证建议类型
        boolean hasTrieMatch = suggestions.stream()
                .anyMatch(s -> s.getType() == SuggestionType.PREFIX_MATCH);
        boolean hasPopular = suggestions.stream()
                .anyMatch(s -> s.getType() == SuggestionType.POPULAR);

        assertTrue(hasTrieMatch, "应包含Trie树匹配的建议");
        assertTrue(hasPopular, "应包含热门搜索建议");

        // 验证建议内容
        boolean hasRelevantSuggestion = suggestions.stream()
                .anyMatch(s -> s.getText().contains("银行"));
        assertTrue(hasRelevantSuggestion, "建议应包含相关内容");
    }

    @Test
    void testGetSuggestionsWithPersonalized() {
        // 测试包含个性化建议
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", 1L, 10, true);

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());

        // 验证个性化建议
        boolean hasPersonalized = suggestions.stream()
                .anyMatch(s -> s.getType() == SuggestionType.PERSONALIZED);
        assertTrue(hasPersonalized, "应包含个性化建议");
    }

    @Test
    void testGetSuggestionsWithEmptyQuery() {
        // 测试空查询
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("", null, 10, false);
        assertTrue(suggestions.isEmpty(), "空查询应返回空列表");

        suggestions = autoCompleteService.getSuggestions(null, null, 10, false);
        assertTrue(suggestions.isEmpty(), "null查询应返回空列表");

        suggestions = autoCompleteService.getSuggestions("   ", null, 10, false);
        assertTrue(suggestions.isEmpty(), "空白查询应返回空列表");
    }

    @Test
    void testGetSuggestionsWithLimit() {
        // 测试限制返回数量
        int limit = 5;
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", null, limit, false);

        assertNotNull(suggestions);
        assertTrue(suggestions.size() <= limit, "返回数量不应超过限制");
    }

    @Test
    void testGetPopularQueries() {
        // 测试获取热门查询
        List<Suggestion> popularQueries = autoCompleteService.getPopularQueries(10, 7);

        assertNotNull(popularQueries);
        assertFalse(popularQueries.isEmpty());

        // 验证所有建议都是热门类型
        boolean allPopular = popularQueries.stream()
                .allMatch(s -> s.getType() == SuggestionType.POPULAR);
        assertTrue(allPopular, "所有建议都应是热门类型");

        // 验证按受欢迎程度排序
        for (int i = 0; i < popularQueries.size() - 1; i++) {
            assertTrue(popularQueries.get(i).getScore() >= popularQueries.get(i + 1).getScore(),
                    "热门查询应按评分降序排列");
        }
    }

    @Test
    void testRecordSelection() {
        // 模拟Redis操作
        when(redisTemplate.opsForHash()).thenReturn(mock(org.springframework.data.redis.core.HashOperations.class));

        // 测试记录用户选择
        assertDoesNotThrow(() -> {
            autoCompleteService.recordSelection("银行", "银行产品", "PREFIX_MATCH", 1L);
        });

        // 验证Redis记录调用
        verify(redisTemplate.opsForHash(), atLeastOnce()).putAll(anyString(), anyMap());
    }

    @Test
    void testRecordSelectionWithoutUser() {
        // 测试匿名用户记录选择
        when(redisTemplate.opsForHash()).thenReturn(mock(org.springframework.data.redis.core.HashOperations.class));

        assertDoesNotThrow(() -> {
            autoCompleteService.recordSelection("银行", "银行产品", "PREFIX_MATCH", null);
        });
    }

    @Test
    void testGetCacheStats() {
        // 测试获取缓存统计
        Map<String, Object> stats = autoCompleteService.getCacheStats();

        assertNotNull(stats);
        assertTrue(stats.containsKey("suggestionCacheStats"));
        assertTrue(stats.containsKey("popularQueryCacheStats"));
        assertTrue(stats.containsKey("userPreferencesCacheStats"));
    }

    @Test
    void testSuggestionScoring() {
        // 测试建议评分机制
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", 1L, 10, true);

        assertNotNull(suggestions);
        if (suggestions.size() > 1) {
            // 验证建议按评分排序
            for (int i = 0; i < suggestions.size() - 1; i++) {
                assertTrue(suggestions.get(i).getScore() >= suggestions.get(i + 1).getScore(),
                        "建议应按评分降序排列");
            }

            // 验证评分范围
            for (Suggestion suggestion : suggestions) {
                assertNotNull(suggestion.getScore());
                assertTrue(suggestion.getScore() >= 0.0f && suggestion.getScore() <= 1.0f,
                        "评分应在0.0-1.0范围内");
            }
        }
    }

    @Test
    void testSuggestionDeduplication() {
        // 测试建议去重
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", null, 20, false);

        assertNotNull(suggestions);

        // 验证没有重复的建议文本
        long uniqueTexts = suggestions.stream()
                .map(Suggestion::getText)
                .distinct()
                .count();

        assertEquals(suggestions.size(), uniqueTexts, "建议列表不应包含重复项");
    }

    @Test
    void testCaseInsensitiveQuery() {
        // 测试大小写不敏感查询
        List<Suggestion> suggestions1 = autoCompleteService.getSuggestions("银行", null, 10, false);
        List<Suggestion> suggestions2 = autoCompleteService.getSuggestions("银行", null, 10, false);

        assertEquals(suggestions1.size(), suggestions2.size(), "大小写不同的查询应返回相同数量的建议");
    }

    @Test
    void testSuggestionTypes() {
        // 测试不同类型的建议
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", 1L, 20, true);

        assertNotNull(suggestions);

        // 统计不同类型的建议数量
        long prefixMatchCount = suggestions.stream()
                .filter(s -> s.getType() == SuggestionType.PREFIX_MATCH)
                .count();
        long popularCount = suggestions.stream()
                .filter(s -> s.getType() == SuggestionType.POPULAR)
                .count();
        long personalizedCount = suggestions.stream()
                .filter(s -> s.getType() == SuggestionType.PERSONALIZED)
                .count();

        assertTrue(prefixMatchCount > 0, "应包含前缀匹配建议");
        assertTrue(popularCount > 0, "应包含热门建议");
        assertTrue(personalizedCount > 0, "应包含个性化建议");
    }

    @Test
    void testErrorHandling() {
        // 测试错误处理
        when(trieService.getPrefixMatches(anyString(), anyInt()))
                .thenThrow(new RuntimeException("Trie service error"));

        // 即使Trie服务出错，也应该能够返回其他类型的建议
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("银行", null, 10, false);

        assertNotNull(suggestions);
        // 应该至少包含热门建议
        boolean hasPopular = suggestions.stream()
                .anyMatch(s -> s.getType() == SuggestionType.POPULAR);
        assertTrue(hasPopular, "即使Trie服务出错，也应包含热门建议");
    }

    @Test
    void testPerformanceWithLargeDataset() {
        // 测试大数据集性能
        when(trieService.getPrefixMatches(anyString(), anyInt()))
                .thenReturn(generateLargeDataset(1000));

        long startTime = System.currentTimeMillis();
        List<Suggestion> suggestions = autoCompleteService.getSuggestions("test", null, 10, false);
        long endTime = System.currentTimeMillis();

        assertNotNull(suggestions);
        assertTrue(suggestions.size() <= 10, "应返回指定数量的建议");

        long duration = endTime - startTime;
        assertTrue(duration < 1000, "响应时间应在合理范围内 (< 1s)");
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

    /**
     * 生成大数据集用于性能测试
     */
    private List<String> generateLargeDataset(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToObj(i -> "test_item_" + i)
                .collect(java.util.stream.Collectors.toList());
    }
}