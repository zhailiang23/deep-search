package com.deepsearch.vector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 向量处理指标
 * 用于监控和统计向量处理性能
 *
 * @author DeepSearch Vector Team
 */
@JsonSerialize
@JsonDeserialize
public class ProcessingMetrics {

    @JsonProperty("totalRequests")
    private final AtomicInteger totalRequests;

    @JsonProperty("successfulRequests")
    private final AtomicInteger successfulRequests;

    @JsonProperty("failedRequests")
    private final AtomicInteger failedRequests;

    @JsonProperty("totalProcessingTimeMs")
    private final AtomicLong totalProcessingTimeMs;

    @JsonProperty("totalCostCents")
    private final AtomicInteger totalCostCents;

    @JsonProperty("startTime")
    private final Instant startTime;

    @JsonProperty("lastUpdateTime")
    private volatile Instant lastUpdateTime;

    @JsonProperty("currentMode")
    private volatile ProcessingMode currentMode;

    @JsonProperty("queueSize")
    private final AtomicInteger queueSize;

    @JsonCreator
    public ProcessingMetrics(
            @JsonProperty("totalRequests") Integer totalRequests,
            @JsonProperty("successfulRequests") Integer successfulRequests,
            @JsonProperty("failedRequests") Integer failedRequests,
            @JsonProperty("totalProcessingTimeMs") Long totalProcessingTimeMs,
            @JsonProperty("totalCostCents") Integer totalCostCents,
            @JsonProperty("startTime") Instant startTime,
            @JsonProperty("lastUpdateTime") Instant lastUpdateTime,
            @JsonProperty("currentMode") ProcessingMode currentMode,
            @JsonProperty("queueSize") Integer queueSize) {

        this.totalRequests = new AtomicInteger(totalRequests != null ? totalRequests : 0);
        this.successfulRequests = new AtomicInteger(successfulRequests != null ? successfulRequests : 0);
        this.failedRequests = new AtomicInteger(failedRequests != null ? failedRequests : 0);
        this.totalProcessingTimeMs = new AtomicLong(totalProcessingTimeMs != null ? totalProcessingTimeMs : 0L);
        this.totalCostCents = new AtomicInteger(totalCostCents != null ? totalCostCents : 0);
        this.startTime = startTime != null ? startTime : Instant.now();
        this.lastUpdateTime = lastUpdateTime != null ? lastUpdateTime : this.startTime;
        this.currentMode = currentMode != null ? currentMode : ProcessingMode.AUTO_SWITCH;
        this.queueSize = new AtomicInteger(queueSize != null ? queueSize : 0);
    }

    /**
     * 创建新的指标实例
     */
    public static ProcessingMetrics create() {
        return new ProcessingMetrics(0, 0, 0, 0L, 0, Instant.now(), Instant.now(), ProcessingMode.AUTO_SWITCH, 0);
    }

    /**
     * 记录成功请求
     */
    public void recordSuccess(long processingTimeMs, int costCents) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        totalProcessingTimeMs.addAndGet(processingTimeMs);
        totalCostCents.addAndGet(costCents);
        lastUpdateTime = Instant.now();
    }

    /**
     * 记录失败请求
     */
    public void recordFailure(long processingTimeMs) {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();
        totalProcessingTimeMs.addAndGet(processingTimeMs);
        lastUpdateTime = Instant.now();
    }

    /**
     * 更新队列大小
     */
    public void updateQueueSize(int size) {
        queueSize.set(size);
        lastUpdateTime = Instant.now();
    }

    /**
     * 更新当前处理模式
     */
    public void updateCurrentMode(ProcessingMode mode) {
        this.currentMode = mode;
        lastUpdateTime = Instant.now();
    }

    public int getTotalRequests() {
        return totalRequests.get();
    }

    public int getSuccessfulRequests() {
        return successfulRequests.get();
    }

    public int getFailedRequests() {
        return failedRequests.get();
    }

    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs.get();
    }

    public int getTotalCostCents() {
        return totalCostCents.get();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getLastUpdateTime() {
        return lastUpdateTime;
    }

    public ProcessingMode getCurrentMode() {
        return currentMode;
    }

    public int getQueueSize() {
        return queueSize.get();
    }

    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        int total = totalRequests.get();
        return total > 0 ? (double) successfulRequests.get() / total : 0.0;
    }

    /**
     * 计算失败率
     */
    public double getFailureRate() {
        return 1.0 - getSuccessRate();
    }

    /**
     * 计算平均处理时间
     */
    public double getAverageProcessingTimeMs() {
        int successful = successfulRequests.get();
        return successful > 0 ? (double) totalProcessingTimeMs.get() / successful : 0.0;
    }

    /**
     * 计算平均成本
     */
    public double getAverageCostCents() {
        int successful = successfulRequests.get();
        return successful > 0 ? (double) totalCostCents.get() / successful : 0.0;
    }

    /**
     * 计算请求速率（每分钟）
     */
    public double getRequestsPerMinute() {
        Duration duration = Duration.between(startTime, lastUpdateTime);
        long minutes = duration.toMinutes();
        return minutes > 0 ? (double) totalRequests.get() / minutes : 0.0;
    }

    /**
     * 计算运行时间
     */
    public Duration getUptime() {
        return Duration.between(startTime, lastUpdateTime);
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalProcessingTimeMs.set(0);
        totalCostCents.set(0);
        queueSize.set(0);
        lastUpdateTime = Instant.now();
    }

    /**
     * 获取指标摘要
     */
    public MetricsSummary getSummary() {
        return new MetricsSummary(
                getTotalRequests(),
                getSuccessfulRequests(),
                getFailedRequests(),
                getSuccessRate(),
                getAverageProcessingTimeMs(),
                getAverageCostCents(),
                getRequestsPerMinute(),
                getCurrentMode(),
                getQueueSize(),
                getUptime()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingMetrics that = (ProcessingMetrics) o;
        return Objects.equals(totalRequests.get(), that.totalRequests.get()) &&
                Objects.equals(successfulRequests.get(), that.successfulRequests.get()) &&
                Objects.equals(failedRequests.get(), that.failedRequests.get()) &&
                Objects.equals(totalProcessingTimeMs.get(), that.totalProcessingTimeMs.get()) &&
                Objects.equals(totalCostCents.get(), that.totalCostCents.get()) &&
                Objects.equals(startTime, that.startTime) &&
                currentMode == that.currentMode &&
                Objects.equals(queueSize.get(), that.queueSize.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                totalRequests.get(),
                successfulRequests.get(),
                failedRequests.get(),
                totalProcessingTimeMs.get(),
                totalCostCents.get(),
                startTime,
                currentMode,
                queueSize.get()
        );
    }

    @Override
    public String toString() {
        return "ProcessingMetrics{" +
                "totalRequests=" + totalRequests.get() +
                ", successfulRequests=" + successfulRequests.get() +
                ", failedRequests=" + failedRequests.get() +
                ", avgProcessingTimeMs=" + String.format("%.2f", getAverageProcessingTimeMs()) +
                ", successRate=" + String.format("%.2f%%", getSuccessRate() * 100) +
                ", currentMode=" + currentMode +
                ", queueSize=" + queueSize.get() +
                ", uptime=" + getUptime() +
                '}';
    }

    /**
     * 指标摘要内部类
     */
    public static class MetricsSummary {
        private final int totalRequests;
        private final int successfulRequests;
        private final int failedRequests;
        private final double successRate;
        private final double averageProcessingTimeMs;
        private final double averageCostCents;
        private final double requestsPerMinute;
        private final ProcessingMode currentMode;
        private final int queueSize;
        private final Duration uptime;

        public MetricsSummary(int totalRequests, int successfulRequests, int failedRequests,
                              double successRate, double averageProcessingTimeMs, double averageCostCents,
                              double requestsPerMinute, ProcessingMode currentMode, int queueSize, Duration uptime) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.successRate = successRate;
            this.averageProcessingTimeMs = averageProcessingTimeMs;
            this.averageCostCents = averageCostCents;
            this.requestsPerMinute = requestsPerMinute;
            this.currentMode = currentMode;
            this.queueSize = queueSize;
            this.uptime = uptime;
        }

        // Getters
        public int getTotalRequests() { return totalRequests; }
        public int getSuccessfulRequests() { return successfulRequests; }
        public int getFailedRequests() { return failedRequests; }
        public double getSuccessRate() { return successRate; }
        public double getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
        public double getAverageCostCents() { return averageCostCents; }
        public double getRequestsPerMinute() { return requestsPerMinute; }
        public ProcessingMode getCurrentMode() { return currentMode; }
        public int getQueueSize() { return queueSize; }
        public Duration getUptime() { return uptime; }
    }
}