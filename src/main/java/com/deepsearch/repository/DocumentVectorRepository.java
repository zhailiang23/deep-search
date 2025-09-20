package com.deepsearch.repository;

import com.deepsearch.entity.DocumentVector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * 文档向量数据访问层
 */
@Repository
public interface DocumentVectorRepository extends JpaRepository<DocumentVector, Long> {

    /**
     * 根据文档ID查找向量
     */
    List<DocumentVector> findByDocumentId(Long documentId);

    /**
     * 根据文档ID和模型名称查找向量
     */
    List<DocumentVector> findByDocumentIdAndModelName(Long documentId, String modelName);

    /**
     * 根据文档ID、模型名称和分块索引查找向量
     */
    Optional<DocumentVector> findByDocumentIdAndModelNameAndChunkIndex(Long documentId, String modelName, Integer chunkIndex);

    /**
     * 根据模型名称查找向量
     */
    List<DocumentVector> findByModelName(String modelName);

    /**
     * 根据模型名称分页查找向量
     */
    Page<DocumentVector> findByModelName(String modelName, Pageable pageable);

    /**
     * 根据处理模式查找向量
     */
    List<DocumentVector> findByProcessingMode(DocumentVector.ProcessingMode processingMode);

    /**
     * 根据向量维度查找向量
     */
    List<DocumentVector> findByVectorDimension(Integer vectorDimension);

    /**
     * 根据质量评分范围查找向量
     */
    List<DocumentVector> findByQualityScoreBetween(BigDecimal minScore, BigDecimal maxScore);

    /**
     * 根据质量评分查找高质量向量
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.qualityScore >= :minScore ORDER BY dv.qualityScore DESC")
    List<DocumentVector> findHighQualityVectors(@Param("minScore") BigDecimal minScore);

    /**
     * 根据处理时间范围查找向量
     */
    List<DocumentVector> findByProcessingTimeMsBetween(Integer minTime, Integer maxTime);

    /**
     * 根据创建时间范围查找向量
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.createdAt BETWEEN :startDate AND :endDate")
    List<DocumentVector> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * 统计文档的向量数量
     */
    Long countByDocumentId(Long documentId);

    /**
     * 统计指定模型的向量数量
     */
    Long countByModelName(String modelName);

    /**
     * 统计指定处理模式的向量数量
     */
    Long countByProcessingMode(DocumentVector.ProcessingMode processingMode);

    /**
     * 检查文档是否已有指定模型的向量
     */
    Boolean existsByDocumentIdAndModelName(Long documentId, String modelName);

    /**
     * 检查文档的特定分块是否已有向量
     */
    Boolean existsByDocumentIdAndModelNameAndChunkIndex(Long documentId, String modelName, Integer chunkIndex);

    /**
     * 删除文档的所有向量
     */
    @Modifying
    @Query("DELETE FROM DocumentVector dv WHERE dv.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 删除指定模型的所有向量
     */
    @Modifying
    @Query("DELETE FROM DocumentVector dv WHERE dv.modelName = :modelName")
    void deleteByModelName(@Param("modelName") String modelName);

    /**
     * 删除文档的指定模型向量
     */
    @Modifying
    @Query("DELETE FROM DocumentVector dv WHERE dv.documentId = :documentId AND dv.modelName = :modelName")
    void deleteByDocumentIdAndModelName(@Param("documentId") Long documentId, @Param("modelName") String modelName);

    /**
     * 查找最近创建的向量
     */
    @Query("SELECT dv FROM DocumentVector dv ORDER BY dv.createdAt DESC")
    List<DocumentVector> findRecentVectors(Pageable pageable);

    /**
     * 查找文档的最新向量（按模型分组）
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.documentId = :documentId " +
           "AND dv.createdAt = (SELECT MAX(dv2.createdAt) FROM DocumentVector dv2 " +
           "WHERE dv2.documentId = :documentId AND dv2.modelName = dv.modelName)")
    List<DocumentVector> findLatestVectorsByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查找处理时间较长的向量
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.processingTimeMs > :threshold ORDER BY dv.processingTimeMs DESC")
    List<DocumentVector> findSlowProcessingVectors(@Param("threshold") Integer threshold);

    /**
     * 查找低质量向量
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.qualityScore < :threshold ORDER BY dv.qualityScore ASC")
    List<DocumentVector> findLowQualityVectors(@Param("threshold") BigDecimal threshold);

    /**
     * 获取模型的平均处理时间
     */
    @Query("SELECT AVG(dv.processingTimeMs) FROM DocumentVector dv WHERE dv.modelName = :modelName")
    Double getAverageProcessingTimeByModel(@Param("modelName") String modelName);

    /**
     * 获取模型的平均质量评分
     */
    @Query("SELECT AVG(dv.qualityScore) FROM DocumentVector dv WHERE dv.modelName = :modelName AND dv.qualityScore IS NOT NULL")
    BigDecimal getAverageQualityScoreByModel(@Param("modelName") String modelName);

    /**
     * 获取处理模式的统计信息
     */
    @Query("SELECT dv.processingMode, COUNT(dv), AVG(dv.processingTimeMs), AVG(dv.qualityScore) " +
           "FROM DocumentVector dv GROUP BY dv.processingMode")
    List<Object[]> getProcessingModeStatistics();

    /**
     * 获取模型使用统计
     */
    @Query("SELECT dv.modelName, COUNT(dv), AVG(dv.processingTimeMs), AVG(dv.qualityScore) " +
           "FROM DocumentVector dv GROUP BY dv.modelName ORDER BY COUNT(dv) DESC")
    List<Object[]> getModelUsageStatistics();

    /**
     * 查找需要重新处理的向量（质量评分低于阈值）
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.qualityScore < :threshold OR dv.qualityScore IS NULL")
    List<DocumentVector> findVectorsNeedingReprocessing(@Param("threshold") BigDecimal threshold);

    /**
     * 根据文档ID列表批量查找向量
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE dv.documentId IN :documentIds")
    List<DocumentVector> findByDocumentIdIn(@Param("documentIds") List<Long> documentIds);

    /**
     * 查找指定时间段内的向量统计
     */
    @Query("SELECT DATE(dv.createdAt), COUNT(dv), AVG(dv.processingTimeMs) " +
           "FROM DocumentVector dv WHERE dv.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(dv.createdAt) ORDER BY DATE(dv.createdAt)")
    List<Object[]> getVectorStatisticsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    /**
     * 查找孤儿向量（对应的文档不存在）
     */
    @Query("SELECT dv FROM DocumentVector dv WHERE NOT EXISTS " +
           "(SELECT 1 FROM Document d WHERE d.id = dv.documentId)")
    List<DocumentVector> findOrphanVectors();

    /**
     * 清理孤儿向量
     */
    @Modifying
    @Query("DELETE FROM DocumentVector dv WHERE NOT EXISTS " +
           "(SELECT 1 FROM Document d WHERE d.id = dv.documentId)")
    int deleteOrphanVectors();

    /**
     * 批量更新向量的质量评分
     */
    @Modifying
    @Query("UPDATE DocumentVector dv SET dv.qualityScore = :qualityScore WHERE dv.id IN :vectorIds")
    int updateQualityScores(@Param("vectorIds") List<Long> vectorIds, @Param("qualityScore") BigDecimal qualityScore);

    /**
     * 查找相似维度的向量（用于向量搜索优化）
     */
    @Query("SELECT DISTINCT dv.vectorDimension FROM DocumentVector dv WHERE dv.modelName = :modelName")
    List<Integer> findDistinctVectorDimensionsByModel(@Param("modelName") String modelName);
}