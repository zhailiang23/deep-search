package com.deepsearch.repository;

import com.deepsearch.entity.SearchLog;
import com.deepsearch.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SearchLogRepository 单元测试
 */
@DataJpaTest
@ActiveProfiles("test")
class SearchLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SearchLogRepository searchLogRepository;

    private User testUser;
    private SearchLog testSearchLog;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("$2a$10$hashed_password");
        testUser.setRole(User.Role.USER);
        testUser = entityManager.persistAndFlush(testUser);

        // 创建测试搜索记录
        testSearchLog = new SearchLog();
        testSearchLog.setQueryText("test search query");
        testSearchLog.setUserId(testUser.getId());
        testSearchLog.setSearchType(SearchLog.SearchType.KEYWORD);
        testSearchLog.setResultCount(10);
        testSearchLog.setResponseTimeMs(150);
    }

    @Test
    void testSaveSearchLog() {
        // When
        SearchLog savedSearchLog = searchLogRepository.save(testSearchLog);

        // Then
        assertThat(savedSearchLog).isNotNull();
        assertThat(savedSearchLog.getId()).isNotNull();
        assertThat(savedSearchLog.getQueryText()).isEqualTo("test search query");
        assertThat(savedSearchLog.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedSearchLog.getSearchType()).isEqualTo(SearchLog.SearchType.KEYWORD);
        assertThat(savedSearchLog.getResultCount()).isEqualTo(10);
        assertThat(savedSearchLog.getResponseTimeMs()).isEqualTo(150);
        assertThat(savedSearchLog.getCreatedAt()).isNotNull();
    }

    @Test
    void testFindByUserId() {
        // Given
        SearchLog searchLog2 = new SearchLog();
        searchLog2.setQueryText("second search query");
        searchLog2.setUserId(testUser.getId());
        searchLog2.setSearchType(SearchLog.SearchType.SEMANTIC);
        searchLog2.setResultCount(5);
        searchLog2.setResponseTimeMs(200);

        entityManager.persistAndFlush(testSearchLog);
        entityManager.persistAndFlush(searchLog2);

        // When
        List<SearchLog> userSearchLogs = searchLogRepository.findByUserId(testUser.getId());

        // Then
        assertThat(userSearchLogs).hasSize(2);
        assertThat(userSearchLogs).extracting(SearchLog::getQueryText)
                .containsExactlyInAnyOrder("test search query", "second search query");
    }

    @Test
    void testFindByUserIdPaginated() {
        // Given
        for (int i = 0; i < 5; i++) {
            SearchLog log = new SearchLog();
            log.setQueryText("query " + i);
            log.setUserId(testUser.getId());
            log.setSearchType(SearchLog.SearchType.KEYWORD);
            log.setResultCount(i);
            log.setResponseTimeMs(100 + i);
            entityManager.persistAndFlush(log);
        }

        Pageable pageable = PageRequest.of(0, 3);

        // When
        Page<SearchLog> userSearchLogs = searchLogRepository.findByUserId(testUser.getId(), pageable);

        // Then
        assertThat(userSearchLogs.getContent()).hasSize(3);
        assertThat(userSearchLogs.getTotalElements()).isEqualTo(5);
        assertThat(userSearchLogs.getTotalPages()).isEqualTo(2);
    }

    @Test
    void testFindBySearchType() {
        // Given
        SearchLog keywordLog = new SearchLog();
        keywordLog.setQueryText("keyword search");
        keywordLog.setUserId(testUser.getId());
        keywordLog.setSearchType(SearchLog.SearchType.KEYWORD);
        keywordLog.setResultCount(8);

        SearchLog semanticLog = new SearchLog();
        semanticLog.setQueryText("semantic search");
        semanticLog.setUserId(testUser.getId());
        semanticLog.setSearchType(SearchLog.SearchType.SEMANTIC);
        semanticLog.setResultCount(6);

        entityManager.persistAndFlush(keywordLog);
        entityManager.persistAndFlush(semanticLog);

        // When
        List<SearchLog> keywordLogs = searchLogRepository.findBySearchType(SearchLog.SearchType.KEYWORD);
        List<SearchLog> semanticLogs = searchLogRepository.findBySearchType(SearchLog.SearchType.SEMANTIC);

        // Then
        assertThat(keywordLogs).hasSize(1);
        assertThat(keywordLogs.get(0).getQueryText()).isEqualTo("keyword search");
        assertThat(semanticLogs).hasSize(1);
        assertThat(semanticLogs.get(0).getQueryText()).isEqualTo("semantic search");
    }

    @Test
    void testFindByUserIdAndSearchType() {
        // Given
        SearchLog keywordLog = new SearchLog();
        keywordLog.setQueryText("user keyword search");
        keywordLog.setUserId(testUser.getId());
        keywordLog.setSearchType(SearchLog.SearchType.KEYWORD);

        entityManager.persistAndFlush(testSearchLog); // KEYWORD type
        entityManager.persistAndFlush(keywordLog); // Also KEYWORD type

        // When
        List<SearchLog> userKeywordLogs = searchLogRepository.findByUserIdAndSearchType(
                testUser.getId(), SearchLog.SearchType.KEYWORD);

        // Then
        assertThat(userKeywordLogs).hasSize(2);
    }

    @Test
    void testFindByQueryTextContainingIgnoreCase() {
        // Given
        SearchLog log1 = new SearchLog();
        log1.setQueryText("Important search query");
        log1.setUserId(testUser.getId());
        log1.setSearchType(SearchLog.SearchType.KEYWORD);

        SearchLog log2 = new SearchLog();
        log2.setQueryText("database topic");
        log2.setUserId(testUser.getId());
        log2.setSearchType(SearchLog.SearchType.SEMANTIC);

        entityManager.persistAndFlush(log1);
        entityManager.persistAndFlush(log2);

        // When
        List<SearchLog> searchLogs = searchLogRepository.findByQueryTextContainingIgnoreCase("search");

        // Then
        assertThat(searchLogs).hasSize(1);
        assertThat(searchLogs.get(0).getQueryText()).isEqualTo("Important search query");
    }

    @Test
    void testCountByUserId() {
        // Given
        SearchLog log2 = new SearchLog();
        log2.setQueryText("second query");
        log2.setUserId(testUser.getId());
        log2.setSearchType(SearchLog.SearchType.SEMANTIC);

        entityManager.persistAndFlush(testSearchLog);
        entityManager.persistAndFlush(log2);

        // When
        Long count = searchLogRepository.countByUserId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void testCountBySearchType() {
        // Given
        SearchLog keywordLog = new SearchLog();
        keywordLog.setQueryText("keyword query");
        keywordLog.setUserId(testUser.getId());
        keywordLog.setSearchType(SearchLog.SearchType.KEYWORD);

        SearchLog semanticLog = new SearchLog();
        semanticLog.setQueryText("semantic query");
        semanticLog.setUserId(testUser.getId());
        semanticLog.setSearchType(SearchLog.SearchType.SEMANTIC);

        entityManager.persistAndFlush(keywordLog);
        entityManager.persistAndFlush(semanticLog);

        // When
        Long keywordCount = searchLogRepository.countBySearchType(SearchLog.SearchType.KEYWORD);
        Long semanticCount = searchLogRepository.countBySearchType(SearchLog.SearchType.SEMANTIC);

        // Then
        assertThat(keywordCount).isEqualTo(1L);
        assertThat(semanticCount).isEqualTo(1L);
    }

    @Test
    void testFindByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        entityManager.persistAndFlush(testSearchLog);

        // When
        List<SearchLog> foundLogs = searchLogRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(foundLogs).hasSize(1);
        assertThat(foundLogs.get(0).getQueryText()).isEqualTo("test search query");
    }

    @Test
    void testFindRecentSearchLogsByUserId() {
        // Given
        for (int i = 0; i < 3; i++) {
            SearchLog log = new SearchLog();
            log.setQueryText("recent query " + i);
            log.setUserId(testUser.getId());
            log.setSearchType(SearchLog.SearchType.KEYWORD);
            log.setResultCount(i);
            entityManager.persistAndFlush(log);
        }

        Pageable pageable = PageRequest.of(0, 2);

        // When
        List<SearchLog> recentLogs = searchLogRepository.findRecentSearchLogsByUserId(testUser.getId(), pageable);

        // Then
        assertThat(recentLogs).hasSize(2);
        // 验证结果是按创建时间降序排列的
        LocalDateTime firstCreatedAt = recentLogs.get(0).getCreatedAt();
        LocalDateTime secondCreatedAt = recentLogs.get(1).getCreatedAt();
        assertThat(firstCreatedAt).isAfterOrEqualTo(secondCreatedAt);
    }

    @Test
    void testCountByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        SearchLog log1 = new SearchLog();
        log1.setQueryText("query1");
        log1.setUserId(testUser.getId());
        log1.setSearchType(SearchLog.SearchType.KEYWORD);

        SearchLog log2 = new SearchLog();
        log2.setQueryText("query2");
        log2.setUserId(testUser.getId());
        log2.setSearchType(SearchLog.SearchType.SEMANTIC);

        entityManager.persistAndFlush(log1);
        entityManager.persistAndFlush(log2);

        // When
        Long count = searchLogRepository.countByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void testFindPopularSearchTerms() {
        // Given
        String[] queries = {"java programming", "spring boot", "java programming", "database", "spring boot", "java programming"};

        for (String query : queries) {
            SearchLog log = new SearchLog();
            log.setQueryText(query);
            log.setUserId(testUser.getId());
            log.setSearchType(SearchLog.SearchType.KEYWORD);
            log.setResultCount(5);
            entityManager.persistAndFlush(log);
        }

        Pageable pageable = PageRequest.of(0, 3);

        // When
        List<Object[]> popularTerms = searchLogRepository.findPopularSearchTerms(pageable);

        // Then
        assertThat(popularTerms).hasSize(3);
        // "java programming" 应该是最受欢迎的（出现3次）
        Object[] firstResult = popularTerms.get(0);
        assertThat(firstResult[0]).isEqualTo("java programming");
        assertThat(firstResult[1]).isEqualTo(3L);
    }

    @Test
    void testFindByUserIdAndCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        SearchLog log1 = new SearchLog();
        log1.setQueryText("user query 1");
        log1.setUserId(testUser.getId());
        log1.setSearchType(SearchLog.SearchType.KEYWORD);

        entityManager.persistAndFlush(testSearchLog);
        entityManager.persistAndFlush(log1);

        // When
        List<SearchLog> userLogs = searchLogRepository.findByUserIdAndCreatedAtBetween(
                testUser.getId(), startDate, endDate);

        // Then
        assertThat(userLogs).hasSize(2);
        // 验证结果是按创建时间降序排列的
        LocalDateTime firstCreatedAt = userLogs.get(0).getCreatedAt();
        LocalDateTime secondCreatedAt = userLogs.get(1).getCreatedAt();
        assertThat(firstCreatedAt).isAfterOrEqualTo(secondCreatedAt);
    }

    @Test
    void testCalculateAverageResponseTime() {
        // Given
        SearchLog log1 = new SearchLog();
        log1.setQueryText("query1");
        log1.setUserId(testUser.getId());
        log1.setSearchType(SearchLog.SearchType.KEYWORD);
        log1.setResponseTimeMs(100);

        SearchLog log2 = new SearchLog();
        log2.setQueryText("query2");
        log2.setUserId(testUser.getId());
        log2.setSearchType(SearchLog.SearchType.SEMANTIC);
        log2.setResponseTimeMs(200);

        entityManager.persistAndFlush(log1);
        entityManager.persistAndFlush(log2);

        // When
        Double averageResponseTime = searchLogRepository.calculateAverageResponseTime();

        // Then
        assertThat(averageResponseTime).isEqualTo(150.0);
    }

    @Test
    void testFindByResponseTimeBetween() {
        // Given
        SearchLog fastLog = new SearchLog();
        fastLog.setQueryText("fast query");
        fastLog.setUserId(testUser.getId());
        fastLog.setSearchType(SearchLog.SearchType.KEYWORD);
        fastLog.setResponseTimeMs(50);

        SearchLog slowLog = new SearchLog();
        slowLog.setQueryText("slow query");
        slowLog.setUserId(testUser.getId());
        slowLog.setSearchType(SearchLog.SearchType.SEMANTIC);
        slowLog.setResponseTimeMs(300);

        entityManager.persistAndFlush(testSearchLog); // 150ms
        entityManager.persistAndFlush(fastLog);
        entityManager.persistAndFlush(slowLog);

        // When
        List<SearchLog> mediumSpeedLogs = searchLogRepository.findByResponseTimeBetween(100, 200);

        // Then
        assertThat(mediumSpeedLogs).hasSize(1);
        assertThat(mediumSpeedLogs.get(0).getQueryText()).isEqualTo("test search query");
    }

    @Test
    void testDeleteSearchLog() {
        // Given
        SearchLog persistedLog = entityManager.persistAndFlush(testSearchLog);
        Long logId = persistedLog.getId();

        // When
        searchLogRepository.deleteById(logId);
        entityManager.flush();

        // Then
        Optional<SearchLog> deletedLog = searchLogRepository.findById(logId);
        assertThat(deletedLog).isEmpty();
    }

    @Test
    void testSearchLogWithoutUser() {
        // Given - 创建没有用户的搜索记录（匿名搜索）
        SearchLog anonymousLog = new SearchLog();
        anonymousLog.setQueryText("anonymous search");
        anonymousLog.setUserId(null);
        anonymousLog.setSearchType(SearchLog.SearchType.KEYWORD);
        anonymousLog.setResultCount(3);
        anonymousLog.setResponseTimeMs(120);

        // When
        SearchLog savedLog = searchLogRepository.save(anonymousLog);

        // Then
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getUserId()).isNull();
        assertThat(savedLog.getQueryText()).isEqualTo("anonymous search");
    }
}