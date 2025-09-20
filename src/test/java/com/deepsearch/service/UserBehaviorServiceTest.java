package com.deepsearch.service;

import com.deepsearch.entity.SearchLog;
import com.deepsearch.entity.UserSearchPreference;
import com.deepsearch.repository.SearchLogRepository;
import com.deepsearch.repository.UserSearchPreferenceRepository;
import com.deepsearch.service.UserBehaviorService.PersonalizationWeights;
import com.deepsearch.service.UserBehaviorService.SearchIntent;
import com.deepsearch.service.UserBehaviorService.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserBehaviorService测试类
 */
@ExtendWith(MockitoExtension.class)
class UserBehaviorServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @Mock
    private UserSearchPreferenceRepository preferenceRepository;

    @InjectMocks
    private UserBehaviorService userBehaviorService;

    private UserSearchPreference testPreference;
    private List<SearchLog> testSearchLogs;

    @BeforeEach
    void setUp() {
        // 设置测试用户偏好
        testPreference = new UserSearchPreference();
        testPreference.setId(1L);
        testPreference.setUserId(100L);
        testPreference.setTotalSearches(50L);
        testPreference.setTotalClicks(30L);
        testPreference.setKeywordPreferenceStrength(0.6f);
        testPreference.setSemanticPreferenceStrength(0.7f);
        testPreference.setPersonalizationStrength(0.8f);
        testPreference.setLastSearchTime(LocalDateTime.now());

        // 设置偏好分类
        Map<String, Double> categories = Map.of(
            "产品", 0.8,
            "服务", 0.6,
            "活动", 0.3
        );
        testPreference.setPreferredCategoriesMap(categories);

        // 设置测试搜索日志
        testSearchLogs = Arrays.asList(
            createSearchLog(1L, "银行产品查询", SearchLog.SearchType.HYBRID, 15),
            createSearchLog(2L, "手机银行使用", SearchLog.SearchType.SEMANTIC, 8),
            createSearchLog(3L, "理财产品", SearchLog.SearchType.KEYWORD, 12)
        );
    }

    @Test
    void testGetUserProfile() {
        // Given
        Long userId = 100L;
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(testSearchLogs);

        // When
        UserProfile profile = userBehaviorService.getUserProfile(userId);

        // Then
        assertNotNull(profile);
        assertEquals(userId, profile.getUserId());
        assertEquals("SEMANTIC_ORIENTED", profile.getPreferenceType());
        assertEquals(Double.valueOf(0.8), profile.getPersonalizationStrength());
        assertEquals(3, profile.getRecentSearches().size());
        assertTrue(profile.hasPreferenceForCategory("产品"));
        assertFalse(profile.hasPreferenceForCategory("其他"));

        verify(preferenceRepository).findByUserId(userId);
        verify(searchLogRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void testGetUserProfileNewUser() {
        // Given
        Long userId = 200L;
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(Arrays.asList());

        // When
        UserProfile profile = userBehaviorService.getUserProfile(userId);

        // Then
        assertNotNull(profile);
        assertEquals(userId, profile.getUserId());
        assertEquals("NEW_USER", profile.getPreferenceType());
        assertEquals(0.5f, profile.getPersonalizationStrength());
        assertTrue(profile.getRecentSearches().isEmpty());

        verify(preferenceRepository).findByUserId(userId);
    }

    @Test
    void testRecordSearchBehavior() {
        // Given
        Long userId = 100L;
        String query = "新的搜索查询";
        SearchLog.SearchType searchType = SearchLog.SearchType.HYBRID;
        int resultCount = 10;
        int responseTime = 150;

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(preferenceRepository.save(any(UserSearchPreference.class))).thenReturn(testPreference);

        // When
        userBehaviorService.recordSearchBehavior(userId, query, searchType, resultCount, responseTime);

        // Then
        verify(preferenceRepository).findByUserId(userId);
        verify(preferenceRepository).save(argThat(preference -> {
            assertEquals(51L, preference.getTotalSearches()); // 增加了1次搜索
            assertNotNull(preference.getLastSearchTime());
            return true;
        }));
    }

    @Test
    void testRecordClickBehavior() {
        // Given
        Long userId = 100L;
        String documentId = "doc123";
        int position = 3;
        String category = "产品";

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(preferenceRepository.save(any(UserSearchPreference.class))).thenReturn(testPreference);

        // When
        userBehaviorService.recordClickBehavior(userId, documentId, position, category);

        // Then
        verify(preferenceRepository).findByUserId(userId);
        verify(preferenceRepository).save(argThat(preference -> {
            assertEquals(31L, preference.getTotalClicks()); // 增加了1次点击
            return true;
        }));
    }

    @Test
    void testAnalyzeSearchIntent() {
        // Given
        Long userId = 100L;
        String query = "如何使用手机银行转账";
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(testSearchLogs);

        // When
        SearchIntent intent = userBehaviorService.analyzeSearchIntent(userId, query);

        // Then
        assertNotNull(intent);
        assertEquals(query, intent.getQuery());
        assertEquals(userId, intent.getUserId());
        assertEquals("HOW_TO_INQUIRY", intent.getPrimaryIntent());
        assertNotNull(intent.getPreferredSearchType());
        assertEquals(0.8f, intent.getPersonalizationWeight());
    }

    @Test
    void testAnalyzeSearchIntentProductInquiry() {
        // Given
        Long userId = 100L;
        String query = "理财产品推荐";

        // 设置用户对产品有强烈偏好
        Map<String, Double> categories = Map.of("产品", 0.9);
        testPreference.setPreferredCategoriesMap(categories);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
        when(searchLogRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(testSearchLogs);

        // When
        SearchIntent intent = userBehaviorService.analyzeSearchIntent(userId, query);

        // Then
        assertEquals("PRODUCT_INQUIRY", intent.getPrimaryIntent());
    }

    @Test
    void testGetPersonalizationWeights() {
        // Given
        Long userId = 100L;
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));

        // When
        PersonalizationWeights weights = userBehaviorService.getPersonalizationWeights(userId);

        // Then
        assertNotNull(weights);
        assertEquals(0.6f, weights.getKeywordWeight());
        assertEquals(0.7f, weights.getSemanticWeight());
        assertEquals(0.8f, weights.getPersonalizationStrength());
    }

    @Test
    void testGetPersonalizationWeightsNewUser() {
        // Given
        Long userId = 200L;
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        PersonalizationWeights weights = userBehaviorService.getPersonalizationWeights(userId);

        // Then
        assertNotNull(weights);
        assertEquals(0.5f, weights.getKeywordWeight());
        assertEquals(0.5f, weights.getSemanticWeight());
        assertEquals(0.5f, weights.getPersonalizationStrength()); // 新用户默认值
    }

    @Test
    void testAnalyzeUserBehaviorPatterns() {
        // Given
        List<UserSearchPreference> activeUsers = Arrays.asList(testPreference);
        when(preferenceRepository.findActiveUsers(any(LocalDateTime.class))).thenReturn(activeUsers);
        when(searchLogRepository.findByUserIdOrderByCreatedAtDesc(anyLong())).thenReturn(testSearchLogs);
        when(preferenceRepository.save(any(UserSearchPreference.class))).thenReturn(testPreference);

        // When
        userBehaviorService.analyzeUserBehaviorPatterns();

        // Then
        verify(preferenceRepository).findActiveUsers(any(LocalDateTime.class));
        verify(preferenceRepository, atLeastOnce()).save(any(UserSearchPreference.class));
    }

    @Test
    void testClickThroughRateCalculation() {
        // Given
        testPreference.setTotalSearches(100L);
        testPreference.setTotalClicks(60L);

        // When
        Double ctr = testPreference.getClickThroughRate();

        // Then
        assertEquals(0.6, ctr, 0.001);
    }

    @Test
    void testNewUserIdentification() {
        // Given
        testPreference.setTotalSearches(5L);

        // When
        boolean isNewUser = testPreference.isNewUser();

        // Then
        assertTrue(isNewUser);
    }

    @Test
    void testActiveUserIdentification() {
        // Given
        testPreference.setLastSearchTime(LocalDateTime.now().minusDays(3));

        // When
        boolean isActive = testPreference.isActiveUser();

        // Then
        assertTrue(isActive);
    }

    @Test
    void testInactiveUserIdentification() {
        // Given
        testPreference.setLastSearchTime(LocalDateTime.now().minusDays(10));

        // When
        boolean isActive = testPreference.isActiveUser();

        // Then
        assertFalse(isActive);
    }

    @Test
    void testPreferenceTypeClassification() {
        // Test semantic oriented
        testPreference.setSemanticPreferenceStrength(0.8f);
        testPreference.setKeywordPreferenceStrength(0.3f);
        assertEquals("SEMANTIC_ORIENTED", testPreference.getPreferenceType());

        // Test keyword oriented
        testPreference.setSemanticPreferenceStrength(0.2f);
        testPreference.setKeywordPreferenceStrength(0.8f);
        assertEquals("KEYWORD_ORIENTED", testPreference.getPreferenceType());

        // Test balanced
        testPreference.setSemanticPreferenceStrength(0.5f);
        testPreference.setKeywordPreferenceStrength(0.5f);
        assertEquals("BALANCED", testPreference.getPreferenceType());
    }

    // 辅助方法
    private SearchLog createSearchLog(Long id, String query, SearchLog.SearchType type, int resultCount) {
        SearchLog log = new SearchLog();
        log.setId(id);
        log.setQueryText(query);
        log.setSearchType(type);
        log.setResultCount(resultCount);
        log.setUserId(100L);
        log.setCreatedAt(LocalDateTime.now().minusHours(id));
        return log;
    }
}