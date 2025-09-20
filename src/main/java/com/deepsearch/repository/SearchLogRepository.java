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
}