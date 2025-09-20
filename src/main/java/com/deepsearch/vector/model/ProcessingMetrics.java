package com.deepsearch.vector.model;

/**
 * 向量处理指标
 */
public class ProcessingMetrics {
    private final long totalRequests;
    private final long successfulRequests;
    private final long failedRequests;
    private final double averageProcessingTime;
    private final int totalCostCents;
    private final int currentQueueSize;
    private final double systemLoadAverage;

    public ProcessingMetrics(long totalRequests, long successfulRequests, long failedRequests,
                           double averageProcessingTime, int totalCostCents,
                           int currentQueueSize, double systemLoadAverage) {
        this.totalRequests = totalRequests;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.averageProcessingTime = averageProcessingTime;
        this.totalCostCents = totalCostCents;
        this.currentQueueSize = currentQueueSize;
        this.systemLoadAverage = systemLoadAverage;
    }

    // Getters
    public long getTotalRequests() { return totalRequests; }
    public long getSuccessfulRequests() { return successfulRequests; }
    public long getFailedRequests() { return failedRequests; }
    public double getAverageProcessingTime() { return averageProcessingTime; }
    public int getTotalCostCents() { return totalCostCents; }
    public int getCurrentQueueSize() { return currentQueueSize; }
    public double getSystemLoadAverage() { return systemLoadAverage; }

    public double getSuccessRate() {
        return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
    }

    public double getErrorRate() {
        return totalRequests > 0 ? (double) failedRequests / totalRequests : 0.0;
    }
}