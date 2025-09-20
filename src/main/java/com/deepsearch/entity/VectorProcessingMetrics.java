package com.deepsearch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 向量处理指标实体类
 */
@Entity
@Table(name = "vector_processing_metrics",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_metrics", columnNames = {"date_hour", "processing_mode", "model_name"})
    },
    indexes = {
        @Index(name = "idx_date_mode", columnList = "date_hour, processing_mode"),
        @Index(name = "idx_model_performance", columnList = "model_name, avg_processing_time_ms"),
        @Index(name = "idx_cost_analysis", columnList = "total_cost_cents, date_hour")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VectorProcessingMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_hour", nullable = false)
    private LocalDateTime dateHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode", nullable = false)
    private ProcessingMode processingMode;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "total_requests")
    private Integer totalRequests = 0;

    @Column(name = "successful_requests")
    private Integer successfulRequests = 0;

    @Column(name = "failed_requests")
    private Integer failedRequests = 0;

    @Column(name = "avg_processing_time_ms")
    private Integer avgProcessingTimeMs;

    @Column(name = "min_processing_time_ms")
    private Integer minProcessingTimeMs;

    @Column(name = "max_processing_time_ms")
    private Integer maxProcessingTimeMs;

    @Column(name = "total_cost_cents")
    private Integer totalCostCents = 0;

    @Column(name = "avg_quality_score", precision = 5, scale = 3)
    private BigDecimal avgQualityScore;

    @Column(name = "total_tokens_used")
    private Integer totalTokensUsed = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 处理模式枚举
     */
    public enum ProcessingMode {
        OFFLINE, ONLINE
    }

    /**
     * 构造函数 - 创建新的指标记录
     */
    public VectorProcessingMetrics(LocalDateTime dateHour, ProcessingMode processingMode, String modelName) {
        this.dateHour = dateHour;
        this.processingMode = processingMode;
        this.modelName = modelName;
        this.totalRequests = 0;
        this.successfulRequests = 0;
        this.failedRequests = 0;
        this.totalCostCents = 0;
        this.totalTokensUsed = 0;
    }

    /**
     * 记录成功的处理请求
     */
    public void recordSuccessfulRequest(int processingTimeMs, BigDecimal qualityScore, int costCents, int tokensUsed) {
        this.totalRequests++;
        this.successfulRequests++;
        this.totalCostCents += costCents;
        this.totalTokensUsed += tokensUsed;

        // 更新处理时间统计
        updateProcessingTimeStats(processingTimeMs);

        // 更新质量评分
        updateQualityScore(qualityScore);
    }

    /**
     * 记录失败的处理请求
     */
    public void recordFailedRequest(int processingTimeMs) {
        this.totalRequests++;
        this.failedRequests++;

        // 更新处理时间统计
        updateProcessingTimeStats(processingTimeMs);
    }

    /**
     * 更新处理时间统计
     */
    private void updateProcessingTimeStats(int processingTimeMs) {
        if (this.minProcessingTimeMs == null || processingTimeMs < this.minProcessingTimeMs) {
            this.minProcessingTimeMs = processingTimeMs;
        }

        if (this.maxProcessingTimeMs == null || processingTimeMs > this.maxProcessingTimeMs) {
            this.maxProcessingTimeMs = processingTimeMs;
        }

        // 计算平均处理时间
        if (this.avgProcessingTimeMs == null) {
            this.avgProcessingTimeMs = processingTimeMs;
        } else {
            // 使用增量平均算法
            int totalTime = this.avgProcessingTimeMs * (this.totalRequests - 1) + processingTimeMs;
            this.avgProcessingTimeMs = totalTime / this.totalRequests;
        }
    }

    /**
     * 更新质量评分
     */
    private void updateQualityScore(BigDecimal qualityScore) {
        if (qualityScore != null) {
            if (this.avgQualityScore == null) {
                this.avgQualityScore = qualityScore;
            } else {
                // 计算平均质量评分
                BigDecimal totalScore = this.avgQualityScore
                    .multiply(BigDecimal.valueOf(this.successfulRequests - 1))
                    .add(qualityScore);
                this.avgQualityScore = totalScore.divide(BigDecimal.valueOf(this.successfulRequests), 3, BigDecimal.ROUND_HALF_UP);
            }
        }
    }

    /**
     * 计算成功率百分比
     */
    public double getSuccessRate() {
        if (totalRequests == null || totalRequests == 0) {
            return 0.0;
        }
        return (double) successfulRequests / totalRequests * 100.0;
    }

    /**
     * 计算失败率百分比
     */
    public double getFailureRate() {
        if (totalRequests == null || totalRequests == 0) {
            return 0.0;
        }
        return (double) failedRequests / totalRequests * 100.0;
    }

    /**
     * 获取平均成本（分）
     */
    public double getAvgCostCents() {
        if (successfulRequests == null || successfulRequests == 0) {
            return 0.0;
        }
        return (double) totalCostCents / successfulRequests;
    }

    /**
     * 获取平均token使用量
     */
    public double getAvgTokensUsed() {
        if (successfulRequests == null || successfulRequests == 0) {
            return 0.0;
        }
        return (double) totalTokensUsed / successfulRequests;
    }

    /**
     * 获取总成本（美元）
     */
    public double getTotalCostDollars() {
        return totalCostCents / 100.0;
    }

    /**
     * 获取平均成本（美元）
     */
    public double getAvgCostDollars() {
        return getAvgCostCents() / 100.0;
    }

    /**
     * 检查是否是有效的指标记录
     */
    public boolean isValidMetrics() {
        return dateHour != null &&
               processingMode != null &&
               modelName != null &&
               !modelName.trim().isEmpty() &&
               totalRequests != null &&
               totalRequests >= 0 &&
               successfulRequests != null &&
               successfulRequests >= 0 &&
               failedRequests != null &&
               failedRequests >= 0 &&
               totalRequests.equals(successfulRequests + failedRequests);
    }

    /**
     * 合并另一个指标记录（用于聚合）
     */
    public void mergeWith(VectorProcessingMetrics other) {
        if (other == null || !this.canMergeWith(other)) {
            return;
        }

        // 合并请求统计
        int oldTotalRequests = this.totalRequests;
        this.totalRequests += other.totalRequests;
        this.successfulRequests += other.successfulRequests;
        this.failedRequests += other.failedRequests;

        // 合并成本和token
        this.totalCostCents += other.totalCostCents;
        this.totalTokensUsed += other.totalTokensUsed;

        // 合并处理时间统计
        if (other.minProcessingTimeMs != null) {
            if (this.minProcessingTimeMs == null || other.minProcessingTimeMs < this.minProcessingTimeMs) {
                this.minProcessingTimeMs = other.minProcessingTimeMs;
            }
        }

        if (other.maxProcessingTimeMs != null) {
            if (this.maxProcessingTimeMs == null || other.maxProcessingTimeMs > this.maxProcessingTimeMs) {
                this.maxProcessingTimeMs = other.maxProcessingTimeMs;
            }
        }

        // 重新计算平均处理时间
        if (this.avgProcessingTimeMs != null && other.avgProcessingTimeMs != null) {
            int totalTime = this.avgProcessingTimeMs * oldTotalRequests +
                           other.avgProcessingTimeMs * other.totalRequests;
            this.avgProcessingTimeMs = totalTime / this.totalRequests;
        } else if (other.avgProcessingTimeMs != null) {
            this.avgProcessingTimeMs = other.avgProcessingTimeMs;
        }

        // 重新计算平均质量评分
        if (this.avgQualityScore != null && other.avgQualityScore != null) {
            BigDecimal totalScore = this.avgQualityScore.multiply(BigDecimal.valueOf(oldTotalRequests))
                .add(other.avgQualityScore.multiply(BigDecimal.valueOf(other.totalRequests)));
            this.avgQualityScore = totalScore.divide(BigDecimal.valueOf(this.totalRequests), 3, BigDecimal.ROUND_HALF_UP);
        } else if (other.avgQualityScore != null) {
            this.avgQualityScore = other.avgQualityScore;
        }
    }

    /**
     * 检查是否可以与另一个指标记录合并
     */
    public boolean canMergeWith(VectorProcessingMetrics other) {
        return other != null &&
               this.dateHour.equals(other.dateHour) &&
               this.processingMode == other.processingMode &&
               this.modelName.equals(other.modelName);
    }
}