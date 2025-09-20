package com.deepsearch.repository;

import com.deepsearch.entity.VectorProcessingMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 向量处理指标数据访问层
 */
@Repository
public interface VectorMetricsRepository extends JpaRepository<VectorProcessingMetrics, Long> {

    /**
     * 根据时间、处理模式和模型查找指标记录
     */
    Optional<VectorProcessingMetrics> findByDateHourAndProcessingModeAndModelName(
        LocalDateTime dateHour,
        VectorProcessingMetrics.ProcessingMode processingMode,
        String modelName
    );

    /**
     * 根据处理模式查找指标
     */
    List<VectorProcessingMetrics> findByProcessingMode(VectorProcessingMetrics.ProcessingMode processingMode);

    /**
     * 根据模型名称查找指标
     */
    List<VectorProcessingMetrics> findByModelName(String modelName);

    /**
     * 根据时间范围查找指标
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "ORDER BY vpm.dateHour ASC")
    List<VectorProcessingMetrics> findByDateHourBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 根据时间范围和处理模式查找指标
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "AND vpm.processingMode = :processingMode ORDER BY vpm.dateHour ASC")
    List<VectorProcessingMetrics> findByDateHourBetweenAndProcessingMode(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("processingMode") VectorProcessingMetrics.ProcessingMode processingMode
    );

    /**
     * 根据时间范围和模型查找指标
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "AND vpm.modelName = :modelName ORDER BY vpm.dateHour ASC")
    List<VectorProcessingMetrics> findByDateHourBetweenAndModelName(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("modelName") String modelName
    );

    /**
     * 查找最近的指标记录
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm ORDER BY vpm.dateHour DESC")
    List<VectorProcessingMetrics> findRecentMetrics(org.springframework.data.domain.Pageable pageable);

    /**
     * 查找指定模型的最近指标
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.modelName = :modelName " +
           "ORDER BY vpm.dateHour DESC")
    List<VectorProcessingMetrics> findRecentMetricsByModel(@Param("modelName") String modelName,
                                                          org.springframework.data.domain.Pageable pageable);

    /**
     * 统计总请求数
     */
    @Query("SELECT SUM(vpm.totalRequests) FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate")
    Long getTotalRequestsByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * 统计成功请求数
     */
    @Query("SELECT SUM(vpm.successfulRequests) FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate")
    Long getSuccessfulRequestsByDateRange(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 统计总成本
     */
    @Query("SELECT SUM(vpm.totalCostCents) FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate")
    Long getTotalCostByDateRange(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * 统计模型的总成本
     */
    @Query("SELECT SUM(vpm.totalCostCents) FROM VectorProcessingMetrics vpm WHERE vpm.modelName = :modelName " +
           "AND vpm.dateHour BETWEEN :startDate AND :endDate")
    Long getTotalCostByModelAndDateRange(@Param("modelName") String modelName,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * 获取平均处理时间
     */
    @Query("SELECT AVG(vpm.avgProcessingTimeMs) FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate")
    Double getAverageProcessingTimeByDateRange(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 获取模型的平均处理时间
     */
    @Query("SELECT AVG(vpm.avgProcessingTimeMs) FROM VectorProcessingMetrics vpm WHERE vpm.modelName = :modelName " +
           "AND vpm.dateHour BETWEEN :startDate AND :endDate")
    Double getAverageProcessingTimeByModelAndDateRange(@Param("modelName") String modelName,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 获取平均质量评分
     */
    @Query("SELECT AVG(vpm.avgQualityScore) FROM VectorProcessingMetrics vpm WHERE vpm.avgQualityScore IS NOT NULL " +
           "AND vpm.dateHour BETWEEN :startDate AND :endDate")
    BigDecimal getAverageQualityScoreByDateRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    /**
     * 获取模型使用统计
     */
    @Query("SELECT vpm.modelName, SUM(vpm.totalRequests), SUM(vpm.successfulRequests), " +
           "SUM(vpm.totalCostCents), AVG(vpm.avgProcessingTimeMs), AVG(vpm.avgQualityScore) " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY vpm.modelName ORDER BY SUM(vpm.totalRequests) DESC")
    List<Object[]> getModelUsageStatistics(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * 获取处理模式统计
     */
    @Query("SELECT vpm.processingMode, SUM(vpm.totalRequests), SUM(vpm.successfulRequests), " +
           "SUM(vpm.totalCostCents), AVG(vpm.avgProcessingTimeMs) " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY vpm.processingMode")
    List<Object[]> getProcessingModeStatistics(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 获取每日统计
     */
    @Query("SELECT DATE(vpm.dateHour), SUM(vpm.totalRequests), SUM(vpm.successfulRequests), " +
           "SUM(vpm.failedRequests), SUM(vpm.totalCostCents), AVG(vpm.avgProcessingTimeMs) " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(vpm.dateHour) ORDER BY DATE(vpm.dateHour)")
    List<Object[]> getDailyStatistics(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 获取每小时统计
     */
    @Query("SELECT vpm.dateHour, SUM(vpm.totalRequests), SUM(vpm.successfulRequests), " +
           "SUM(vpm.totalCostCents), AVG(vpm.avgProcessingTimeMs) " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY vpm.dateHour ORDER BY vpm.dateHour")
    List<Object[]> getHourlyStatistics(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 查找成本超过阈值的记录
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.totalCostCents > :thresholdCents " +
           "ORDER BY vpm.totalCostCents DESC")
    List<VectorProcessingMetrics> findHighCostMetrics(@Param("thresholdCents") Integer thresholdCents);

    /**
     * 查找处理时间较长的记录
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.avgProcessingTimeMs > :thresholdMs " +
           "ORDER BY vpm.avgProcessingTimeMs DESC")
    List<VectorProcessingMetrics> findSlowProcessingMetrics(@Param("thresholdMs") Integer thresholdMs);

    /**
     * 查找低质量评分的记录
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.avgQualityScore < :threshold " +
           "ORDER BY vpm.avgQualityScore ASC")
    List<VectorProcessingMetrics> findLowQualityMetrics(@Param("threshold") BigDecimal threshold);

    /**
     * 查找成功率低的记录
     */
    @Query("SELECT vpm FROM VectorProcessingMetrics vpm WHERE vpm.totalRequests > 0 " +
           "AND (CAST(vpm.successfulRequests AS DOUBLE) / vpm.totalRequests) < :threshold " +
           "ORDER BY (CAST(vpm.successfulRequests AS DOUBLE) / vpm.totalRequests) ASC")
    List<VectorProcessingMetrics> findLowSuccessRateMetrics(@Param("threshold") Double threshold);

    /**
     * 删除旧的指标记录
     */
    @Modifying
    @Query("DELETE FROM VectorProcessingMetrics vpm WHERE vpm.dateHour < :cutoffDate")
    int deleteOldMetrics(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 按日期聚合指标（用于降低数据粒度）
     */
    @Query("SELECT DATE(vpm.dateHour), vpm.processingMode, vpm.modelName, " +
           "SUM(vpm.totalRequests), SUM(vpm.successfulRequests), SUM(vpm.failedRequests), " +
           "AVG(vpm.avgProcessingTimeMs), MIN(vpm.minProcessingTimeMs), MAX(vpm.maxProcessingTimeMs), " +
           "SUM(vpm.totalCostCents), AVG(vpm.avgQualityScore), SUM(vpm.totalTokensUsed) " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(vpm.dateHour), vpm.processingMode, vpm.modelName " +
           "ORDER BY DATE(vpm.dateHour), vpm.processingMode, vpm.modelName")
    List<Object[]> aggregateMetricsByDate(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 获取成本效率分析（按模型）
     */
    @Query("SELECT vpm.modelName, " +
           "SUM(vpm.totalCostCents), SUM(vpm.successfulRequests), " +
           "(SUM(vpm.totalCostCents) / NULLIF(SUM(vpm.successfulRequests), 0)) AS cost_per_request, " +
           "AVG(vpm.avgQualityScore) " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY vpm.modelName " +
           "ORDER BY (SUM(vpm.totalCostCents) / NULLIF(SUM(vpm.successfulRequests), 0)) ASC")
    List<Object[]> getCostEfficiencyAnalysis(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    /**
     * 获取性能趋势分析
     */
    @Query("SELECT vpm.dateHour, vpm.modelName, " +
           "vpm.avgProcessingTimeMs, vpm.avgQualityScore, " +
           "(CAST(vpm.successfulRequests AS DOUBLE) / NULLIF(vpm.totalRequests, 0)) AS success_rate " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "ORDER BY vpm.dateHour, vpm.modelName")
    List<Object[]> getPerformanceTrend(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    /**
     * 获取峰值负载时间
     */
    @Query("SELECT vpm.dateHour, SUM(vpm.totalRequests) as total_load " +
           "FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate " +
           "GROUP BY vpm.dateHour " +
           "ORDER BY total_load DESC")
    List<Object[]> getPeakLoadTimes(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   org.springframework.data.domain.Pageable pageable);

    /**
     * 检查是否存在指定时间的指标记录
     */
    Boolean existsByDateHourAndProcessingModeAndModelName(
        LocalDateTime dateHour,
        VectorProcessingMetrics.ProcessingMode processingMode,
        String modelName
    );

    /**
     * 获取指标记录的数量
     */
    @Query("SELECT COUNT(vpm) FROM VectorProcessingMetrics vpm WHERE vpm.dateHour BETWEEN :startDate AND :endDate")
    Long countByDateHourBetween(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * 更新或插入指标记录（用于原子操作）
     */
    @Modifying
    @Query("UPDATE VectorProcessingMetrics vpm SET " +
           "vpm.totalRequests = vpm.totalRequests + :additionalRequests, " +
           "vpm.successfulRequests = vpm.successfulRequests + :additionalSuccessful, " +
           "vpm.failedRequests = vpm.failedRequests + :additionalFailed, " +
           "vpm.totalCostCents = vpm.totalCostCents + :additionalCost, " +
           "vpm.totalTokensUsed = vpm.totalTokensUsed + :additionalTokens " +
           "WHERE vpm.dateHour = :dateHour AND vpm.processingMode = :processingMode AND vpm.modelName = :modelName")
    int incrementMetrics(@Param("dateHour") LocalDateTime dateHour,
                        @Param("processingMode") VectorProcessingMetrics.ProcessingMode processingMode,
                        @Param("modelName") String modelName,
                        @Param("additionalRequests") Integer additionalRequests,
                        @Param("additionalSuccessful") Integer additionalSuccessful,
                        @Param("additionalFailed") Integer additionalFailed,
                        @Param("additionalCost") Integer additionalCost,
                        @Param("additionalTokens") Integer additionalTokens);
}