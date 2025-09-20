package com.deepsearch.repository;

import com.deepsearch.entity.SearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索记录数据访问层
 */
@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 根据用户ID查找搜索记录
     */
    List<SearchLog> findByUserId(Long userId);

    /**
     * 根据用户ID分页查找搜索记录
     */
    Page<SearchLog> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据搜索类型查找记录
     */
    List<SearchLog> findBySearchType(SearchLog.SearchType searchType);

    /**
     * 根据用户ID和搜索类型查找记录
     */
    List<SearchLog> findByUserIdAndSearchType(Long userId, SearchLog.SearchType searchType);

    /**
     * 根据查询文本模糊查找
     */
    List<SearchLog> findByQueryTextContainingIgnoreCase(String queryText);

    /**
     * 统计用户的搜索次数
     */
    Long countByUserId(Long userId);

    /**
     * 统计指定搜索类型的次数
     */
    Long countBySearchType(SearchLog.SearchType searchType);

    /**
     * 查找指定时间范围内的搜索记录
     */
    @Query("SELECT s FROM SearchLog s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<SearchLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 查找用户最近的搜索记录
     */
    @Query("SELECT s FROM SearchLog s WHERE s.userId = :userId ORDER BY s.createdAt DESC")
    List<SearchLog> findRecentSearchLogsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 统计指定时间范围内的搜索次数
     */
    @Query("SELECT COUNT(s) FROM SearchLog s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * 查找热门搜索词（按查询次数分组）
     */
    @Query("SELECT s.queryText, COUNT(s) as searchCount FROM SearchLog s " +
           "GROUP BY s.queryText ORDER BY searchCount DESC")
    List<Object[]> findPopularSearchTerms(Pageable pageable);

    /**
     * 查找用户在指定时间范围内的搜索记录
     */
    @Query("SELECT s FROM SearchLog s WHERE s.userId = :userId " +
           "AND s.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY s.createdAt DESC")
    List<SearchLog> findByUserIdAndCreatedAtBetween(@Param("userId") Long userId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 计算平均响应时间
     */
    @Query("SELECT AVG(s.responseTimeMs) FROM SearchLog s WHERE s.responseTimeMs IS NOT NULL")
    Double calculateAverageResponseTime();

    /**
     * 根据响应时间范围查找记录
     */
    @Query("SELECT s FROM SearchLog s WHERE s.responseTimeMs BETWEEN :minTime AND :maxTime")
    List<SearchLog> findByResponseTimeBetween(@Param("minTime") Integer minTime,
                                             @Param("maxTime") Integer maxTime);

    /**
     * 根据查询文本包含特定内容查找记录（支持分页）
     */
    @Query("SELECT s FROM SearchLog s WHERE LOWER(s.queryText) LIKE LOWER(CONCAT('%', :queryText, '%')) ORDER BY s.createdAt DESC")
    List<SearchLog> findByQueryTextContaining(@Param("queryText") String queryText, Pageable pageable);

    /**
     * 获取最近的搜索记录（用于Trie树构建）
     */
    @Query("SELECT s FROM SearchLog s ORDER BY s.createdAt DESC")
    List<SearchLog> findRecentSearchLogs(Pageable pageable);

    /**
     * 获取指定数量的最近搜索记录
     */
    @Query(value = "SELECT * FROM search_logs ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<SearchLog> findRecentSearchLogs(@Param("limit") int limit);

    /**
     * 查找包含指定查询文本的热门搜索词
     */
    @Query("SELECT s.queryText, COUNT(s) as searchCount FROM SearchLog s " +
           "WHERE LOWER(s.queryText) LIKE LOWER(CONCAT('%', :queryText, '%')) " +
           "GROUP BY s.queryText ORDER BY searchCount DESC")
    List<Object[]> findPopularSearchTermsContaining(@Param("queryText") String queryText, Pageable pageable);

    /**
     * 获取指定时间范围内的热门搜索词
     */
    @Query("SELECT s.queryText, COUNT(s) as searchCount FROM SearchLog s " +
           "WHERE s.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY s.queryText ORDER BY searchCount DESC")
    List<Object[]> findPopularSearchTermsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate,
                                                    Pageable pageable);

    /**
     * 获取用户最常搜索的词汇
     */
    @Query("SELECT s.queryText, COUNT(s) as searchCount FROM SearchLog s " +
           "WHERE s.userId = :userId " +
           "GROUP BY s.queryText ORDER BY searchCount DESC")
    List<Object[]> findUserPopularSearchTerms(@Param("userId") Long userId, Pageable pageable);

    /**
     * 查找与指定查询相关的搜索记录（基于共现）
     */
    @Query("SELECT DISTINCT s2.queryText FROM SearchLog s1, SearchLog s2 " +
           "WHERE s1.userId = s2.userId " +
           "AND s1.queryText = :queryText " +
           "AND s2.queryText != :queryText " +
           "AND ABS(TIMESTAMPDIFF(MINUTE, s1.createdAt, s2.createdAt)) <= 30 " +
           "ORDER BY s2.createdAt DESC")
    List<String> findRelatedQueries(@Param("queryText") String queryText, Pageable pageable);
}