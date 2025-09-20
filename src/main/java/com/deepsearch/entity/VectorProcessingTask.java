package com.deepsearch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 向量处理任务实体类
 */
@Entity
@Table(name = "vector_processing_tasks", indexes = {
    @Index(name = "idx_status_priority", columnList = "status, priority, created_at"),
    @Index(name = "idx_document_task", columnList = "document_id, task_type"),
    @Index(name = "idx_processing_mode", columnList = "processing_mode"),
    @Index(name = "idx_scheduled_at", columnList = "scheduled_at"),
    @Index(name = "idx_retry_count", columnList = "retry_count")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorProcessingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private TaskType taskType;

    @Column(name = "priority")
    private Integer priority = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode")
    private ProcessingMode processingMode = ProcessingMode.AUTO;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        /**
         * 初始向量化处理
         */
        INITIAL,
        /**
         * 重新处理
         */
        REPROCESS,
        /**
         * 质量检查
         */
        QUALITY_CHECK,
        /**
         * 更新向量
         */
        UPDATE
    }

    /**
     * 任务状态枚举
     */
    public enum Status {
        /**
         * 等待处理
         */
        PENDING,
        /**
         * 处理中
         */
        PROCESSING,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 失败
         */
        FAILED,
        /**
         * 已取消
         */
        CANCELLED
    }

    /**
     * 处理模式枚举
     */
    public enum ProcessingMode {
        /**
         * 离线批量处理
         */
        OFFLINE,
        /**
         * 在线实时处理
         */
        ONLINE,
        /**
         * 自动选择模式
         */
        AUTO
    }

    /**
     * 构造函数 - 创建新任务
     */
    public VectorProcessingTask(Long documentId, TaskType taskType, ProcessingMode processingMode,
                               String modelName, Integer priority) {
        this.documentId = documentId;
        this.taskType = taskType;
        this.processingMode = processingMode;
        this.modelName = modelName;
        this.priority = priority;
        this.status = Status.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
    }

    /**
     * 构造函数 - 创建简单任务
     */
    public VectorProcessingTask(Long documentId, TaskType taskType) {
        this(documentId, taskType, ProcessingMode.AUTO, null, 5);
    }

    /**
     * 开始处理任务
     */
    public void startProcessing() {
        this.status = Status.PROCESSING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * 完成任务
     */
    public void completeTask() {
        this.status = Status.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 任务失败
     */
    public void failTask(String errorMessage, String errorCode) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 取消任务
     */
    public void cancelTask() {
        this.status = Status.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return this.retryCount < this.maxRetries &&
               (this.status == Status.FAILED || this.status == Status.PENDING);
    }

    /**
     * 重置任务状态准备重试
     */
    public void resetForRetry() {
        if (canRetry()) {
            this.status = Status.PENDING;
            this.startedAt = null;
            this.completedAt = null;
            this.errorMessage = null;
            this.errorCode = null;
            incrementRetryCount();
        }
    }

    /**
     * 获取任务执行时长（毫秒）
     */
    public Long getExecutionTimeMs() {
        if (startedAt == null) {
            return null;
        }
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMillis();
    }

    /**
     * 检查任务是否已完成（包括成功和失败）
     */
    public boolean isFinished() {
        return status == Status.COMPLETED || status == Status.FAILED || status == Status.CANCELLED;
    }

    /**
     * 检查任务是否正在处理中
     */
    public boolean isProcessing() {
        return status == Status.PROCESSING;
    }

    /**
     * 设置计划执行时间
     */
    public void scheduleAt(LocalDateTime scheduledTime) {
        this.scheduledAt = scheduledTime;
    }

    /**
     * 检查任务是否到期可执行
     */
    public boolean isReadyToExecute() {
        return status == Status.PENDING &&
               (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now()) || scheduledAt.isEqual(LocalDateTime.now()));
    }
}