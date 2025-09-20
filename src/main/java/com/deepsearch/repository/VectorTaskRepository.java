package com.deepsearch.repository;

import com.deepsearch.entity.VectorProcessingTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 向量处理任务数据访问层
 */
@Repository
public interface VectorTaskRepository extends JpaRepository<VectorProcessingTask, Long> {

    /**
     * 根据文档ID查找任务
     */
    List<VectorProcessingTask> findByDocumentId(Long documentId);

    /**
     * 根据文档ID和任务类型查找任务
     */
    List<VectorProcessingTask> findByDocumentIdAndTaskType(Long documentId, VectorProcessingTask.TaskType taskType);

    /**
     * 根据状态查找任务
     */
    List<VectorProcessingTask> findByStatus(VectorProcessingTask.Status status);

    /**
     * 根据状态分页查找任务
     */
    Page<VectorProcessingTask> findByStatus(VectorProcessingTask.Status status, Pageable pageable);

    /**
     * 根据任务类型查找任务
     */
    List<VectorProcessingTask> findByTaskType(VectorProcessingTask.TaskType taskType);

    /**
     * 根据处理模式查找任务
     */
    List<VectorProcessingTask> findByProcessingMode(VectorProcessingTask.ProcessingMode processingMode);

    /**
     * 根据模型名称查找任务
     */
    List<VectorProcessingTask> findByModelName(String modelName);

    /**
     * 查找等待处理的任务（按优先级和创建时间排序）
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.status = 'PENDING' " +
           "AND (vpt.scheduledAt IS NULL OR vpt.scheduledAt <= :now) " +
           "ORDER BY vpt.priority ASC, vpt.createdAt ASC")
    List<VectorProcessingTask> findPendingTasks(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 查找高优先级等待任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.status = 'PENDING' " +
           "AND vpt.priority <= :maxPriority " +
           "AND (vpt.scheduledAt IS NULL OR vpt.scheduledAt <= :now) " +
           "ORDER BY vpt.priority ASC, vpt.createdAt ASC")
    List<VectorProcessingTask> findHighPriorityPendingTasks(@Param("maxPriority") Integer maxPriority,
                                                            @Param("now") LocalDateTime now,
                                                            Pageable pageable);

    /**
     * 查找正在处理的任务
     */
    List<VectorProcessingTask> findByStatusOrderByStartedAtAsc(VectorProcessingTask.Status status);

    /**
     * 查找失败的任务
     */
    List<VectorProcessingTask> findByStatusOrderByCompletedAtDesc(VectorProcessingTask.Status status);

    /**
     * 查找可重试的失败任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.status = 'FAILED' " +
           "AND vpt.retryCount < vpt.maxRetries " +
           "ORDER BY vpt.retryCount ASC, vpt.completedAt ASC")
    List<VectorProcessingTask> findRetriableFailedTasks();

    /**
     * 查找超时的处理任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.status = 'PROCESSING' " +
           "AND vpt.startedAt < :timeoutThreshold")
    List<VectorProcessingTask> findTimeoutTasks(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 根据创建时间范围查找任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.createdAt BETWEEN :startDate AND :endDate")
    List<VectorProcessingTask> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * 根据完成时间范围查找任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.completedAt BETWEEN :startDate AND :endDate")
    List<VectorProcessingTask> findByCompletedAtBetween(@Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * 统计文档的任务数量
     */
    Long countByDocumentId(Long documentId);

    /**
     * 统计指定状态的任务数量
     */
    Long countByStatus(VectorProcessingTask.Status status);

    /**
     * 统计指定任务类型的数量
     */
    Long countByTaskType(VectorProcessingTask.TaskType taskType);

    /**
     * 统计指定处理模式的任务数量
     */
    Long countByProcessingMode(VectorProcessingTask.ProcessingMode processingMode);

    /**
     * 检查文档是否有指定类型的任务
     */
    Boolean existsByDocumentIdAndTaskType(Long documentId, VectorProcessingTask.TaskType taskType);

    /**
     * 检查文档是否有指定状态的任务
     */
    Boolean existsByDocumentIdAndStatus(Long documentId, VectorProcessingTask.Status status);

    /**
     * 查找文档最近的任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.documentId = :documentId " +
           "ORDER BY vpt.createdAt DESC")
    List<VectorProcessingTask> findRecentTasksByDocumentId(@Param("documentId") Long documentId, Pageable pageable);

    /**
     * 查找文档最新的指定类型任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.documentId = :documentId " +
           "AND vpt.taskType = :taskType ORDER BY vpt.createdAt DESC")
    Optional<VectorProcessingTask> findLatestTaskByDocumentIdAndType(@Param("documentId") Long documentId,
                                                                    @Param("taskType") VectorProcessingTask.TaskType taskType);

    /**
     * 批量更新任务状态
     */
    @Modifying
    @Query("UPDATE VectorProcessingTask vpt SET vpt.status = :newStatus WHERE vpt.id IN :taskIds")
    int updateTaskStatus(@Param("taskIds") List<Long> taskIds, @Param("newStatus") VectorProcessingTask.Status newStatus);

    /**
     * 批量取消任务
     */
    @Modifying
    @Query("UPDATE VectorProcessingTask vpt SET vpt.status = 'CANCELLED', vpt.completedAt = :now " +
           "WHERE vpt.id IN :taskIds AND vpt.status IN ('PENDING', 'PROCESSING')")
    int cancelTasks(@Param("taskIds") List<Long> taskIds, @Param("now") LocalDateTime now);

    /**
     * 删除文档的所有任务
     */
    @Modifying
    @Query("DELETE FROM VectorProcessingTask vpt WHERE vpt.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 删除已完成的旧任务
     */
    @Modifying
    @Query("DELETE FROM VectorProcessingTask vpt WHERE vpt.status IN ('COMPLETED', 'CANCELLED') " +
           "AND vpt.completedAt < :cutoffDate")
    int deleteOldCompletedTasks(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 获取任务状态统计
     */
    @Query("SELECT vpt.status, COUNT(vpt) FROM VectorProcessingTask vpt GROUP BY vpt.status")
    List<Object[]> getTaskStatusStatistics();

    /**
     * 获取任务类型统计
     */
    @Query("SELECT vpt.taskType, COUNT(vpt) FROM VectorProcessingTask vpt GROUP BY vpt.taskType")
    List<Object[]> getTaskTypeStatistics();

    /**
     * 获取处理模式统计
     */
    @Query("SELECT vpt.processingMode, COUNT(vpt), AVG(vpt.priority) " +
           "FROM VectorProcessingTask vpt GROUP BY vpt.processingMode")
    List<Object[]> getProcessingModeStatistics();

    /**
     * 获取任务执行时间统计
     */
    @Query("SELECT vpt.status, AVG(TIMESTAMPDIFF(SECOND, vpt.startedAt, vpt.completedAt)) " +
           "FROM VectorProcessingTask vpt WHERE vpt.startedAt IS NOT NULL AND vpt.completedAt IS NOT NULL " +
           "GROUP BY vpt.status")
    List<Object[]> getTaskExecutionTimeStatistics();

    /**
     * 查找重试次数较多的任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.retryCount >= :minRetryCount " +
           "ORDER BY vpt.retryCount DESC")
    List<VectorProcessingTask> findHighRetryCountTasks(@Param("minRetryCount") Integer minRetryCount);

    /**
     * 查找长时间运行的任务
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE vpt.status = 'PROCESSING' " +
           "AND TIMESTAMPDIFF(MINUTE, vpt.startedAt, :now) > :thresholdMinutes")
    List<VectorProcessingTask> findLongRunningTasks(@Param("thresholdMinutes") Integer thresholdMinutes,
                                                   @Param("now") LocalDateTime now);

    /**
     * 根据错误代码查找失败任务
     */
    List<VectorProcessingTask> findByStatusAndErrorCode(VectorProcessingTask.Status status, String errorCode);

    /**
     * 获取错误统计
     */
    @Query("SELECT vpt.errorCode, COUNT(vpt) FROM VectorProcessingTask vpt " +
           "WHERE vpt.status = 'FAILED' AND vpt.errorCode IS NOT NULL " +
           "GROUP BY vpt.errorCode ORDER BY COUNT(vpt) DESC")
    List<Object[]> getErrorStatistics();

    /**
     * 查找队列深度（等待任务数）
     */
    @Query("SELECT COUNT(vpt) FROM VectorProcessingTask vpt WHERE vpt.status = 'PENDING'")
    Long getQueueDepth();

    /**
     * 查找正在处理的任务数
     */
    @Query("SELECT COUNT(vpt) FROM VectorProcessingTask vpt WHERE vpt.status = 'PROCESSING'")
    Long getProcessingTaskCount();

    /**
     * 获取按日期分组的任务统计
     */
    @Query("SELECT DATE(vpt.createdAt), vpt.status, COUNT(vpt) " +
           "FROM VectorProcessingTask vpt WHERE vpt.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(vpt.createdAt), vpt.status ORDER BY DATE(vpt.createdAt)")
    List<Object[]> getTaskStatisticsByDateRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * 查找孤儿任务（对应的文档不存在）
     */
    @Query("SELECT vpt FROM VectorProcessingTask vpt WHERE NOT EXISTS " +
           "(SELECT 1 FROM Document d WHERE d.id = vpt.documentId)")
    List<VectorProcessingTask> findOrphanTasks();

    /**
     * 清理孤儿任务
     */
    @Modifying
    @Query("DELETE FROM VectorProcessingTask vpt WHERE NOT EXISTS " +
           "(SELECT 1 FROM Document d WHERE d.id = vpt.documentId)")
    int deleteOrphanTasks();
}