package com.deepsearch.vector.monitoring;

import com.deepsearch.vector.enums.ProcessingMode;
import com.deepsearch.vector.model.ProcessingMetrics;
import com.deepsearch.vector.queue.VectorTaskQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 向量处理性能监控和指标收集
 */
@Service
public class VectorMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(VectorMetricsCollector.class);

    @Autowired
    private VectorTaskQueueService queueService;

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    // 实时指标
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final DoubleAdder totalProcessingTime = new DoubleAdder();
    private final AtomicInteger totalCostCents = new AtomicInteger(0);

    // 按模式分类的指标
    private final Map<ProcessingMode, ModeMetrics> modeMetrics = new ConcurrentHashMap<>();

    // 按小时统计的历史指标
    private final Map<String, HourlyMetrics> hourlyMetrics = new ConcurrentHashMap<>();

    // 滑动窗口统计（最近5分钟）
    private final Queue<TimestampedMetric> recentMetrics = new LinkedList<>();
    private final Object recentMetricsLock = new Object();

    @PostConstruct
    public void init() {
        // 初始化各模式的指标
        for (ProcessingMode mode : ProcessingMode.values()) {
            modeMetrics.put(mode, new ModeMetrics());
        }
        logger.info("向量处理指标收集器已启动");
    }

    /**
     * 记录任务完成
     */
    public void recordTaskCompletion(long processingTimeMs, ProcessingMode mode) {
        totalRequests.incrementAndGet();
        successfulRequests.incrementAndGet();
        totalProcessingTime.add(processingTimeMs);

        // 按模式记录
        ModeMetrics metrics = modeMetrics.get(mode);
        if (metrics != null) {
            metrics.recordSuccess(processingTimeMs);
        }

        // 记录到滑动窗口
        recordRecentMetric(true, processingTimeMs, mode);

        // 估算成本（这里使用简化的成本模型）
        int estimatedCost = estimateProcessingCost(mode, processingTimeMs);
        totalCostCents.addAndGet(estimatedCost);

        logger.debug("任务完成记录: 模式={}, 处理时间={}ms, 估算成本={}分", mode, processingTimeMs, estimatedCost);
    }

    /**
     * 记录任务失败
     */
    public void recordTaskFailure(ProcessingMode mode) {
        totalRequests.incrementAndGet();
        failedRequests.incrementAndGet();

        // 按模式记录
        ModeMetrics metrics = modeMetrics.get(mode);
        if (metrics != null) {
            metrics.recordFailure();
        }

        // 记录到滑动窗口
        recordRecentMetric(false, 0, mode);

        logger.debug("任务失败记录: 模式={}", mode);
    }

    /**
     * 获取当前指标
     */
    public ProcessingMetrics getCurrentMetrics() {
        VectorTaskQueueService.QueueStatus queueStatus = queueService.getQueueStatus();

        double avgProcessingTime = calculateAverageProcessingTime();
        double systemLoad = osBean.getSystemLoadAverage();

        return new ProcessingMetrics(
            totalRequests.get(),
            successfulRequests.get(),
            failedRequests.get(),
            avgProcessingTime,
            totalCostCents.get(),
            queueStatus.getPendingTasks(),
            systemLoad
        );
    }

    /**
     * 获取按模式分类的指标
     */
    public Map<ProcessingMode, ProcessingMetrics> getMetricsByMode() {
        Map<ProcessingMode, ProcessingMetrics> result = new HashMap<>();

        for (Map.Entry<ProcessingMode, ModeMetrics> entry : modeMetrics.entrySet()) {
            ModeMetrics metrics = entry.getValue();
            ProcessingMode mode = entry.getKey();

            result.put(mode, new ProcessingMetrics(
                metrics.totalRequests.get(),
                metrics.successfulRequests.get(),
                metrics.failedRequests.get(),
                metrics.getAverageProcessingTime(),
                metrics.totalCostCents.get(),
                0, // 队列大小对单个模式没有意义
                osBean.getSystemLoadAverage()
            ));
        }

        return result;
    }

    /**
     * 获取最近5分钟的指标
     */
    public ProcessingMetrics getRecentMetrics() {
        synchronized (recentMetricsLock) {
            cleanupOldMetrics();

            if (recentMetrics.isEmpty()) {
                return getCurrentMetrics();
            }

            long totalRecentRequests = recentMetrics.size();
            long successfulRecentRequests = recentMetrics.stream()
                .mapToLong(m -> m.success ? 1 : 0)
                .sum();
            long failedRecentRequests = totalRecentRequests - successfulRecentRequests;

            double avgRecentProcessingTime = recentMetrics.stream()
                .filter(m -> m.success)
                .mapToDouble(m -> m.processingTimeMs)
                .average()
                .orElse(0.0);

            VectorTaskQueueService.QueueStatus queueStatus = queueService.getQueueStatus();

            return new ProcessingMetrics(
                totalRecentRequests,
                successfulRecentRequests,
                failedRecentRequests,
                avgRecentProcessingTime,
                0, // 最近成本计算较复杂，暂时设为0
                queueStatus.getPendingTasks(),
                osBean.getSystemLoadAverage()
            );
        }
    }

    /**
     * 获取历史指标摘要
     */
    public Map<String, HourlyMetrics> getHourlyMetricsSummary() {
        return new HashMap<>(hourlyMetrics);
    }

    /**
     * 重置所有指标
     */
    public void resetMetrics() {
        totalRequests.set(0);
        successfulRequests.set(0);
        failedRequests.set(0);
        totalProcessingTime.reset();
        totalCostCents.set(0);

        modeMetrics.values().forEach(ModeMetrics::reset);

        synchronized (recentMetricsLock) {
            recentMetrics.clear();
        }

        logger.info("所有指标已重置");
    }

    /**
     * 计算平均处理时间
     */
    private double calculateAverageProcessingTime() {
        long successful = successfulRequests.get();
        return successful > 0 ? totalProcessingTime.sum() / successful : 0.0;
    }

    /**
     * 估算处理成本
     */
    private int estimateProcessingCost(ProcessingMode mode, long processingTimeMs) {
        // 简化的成本模型
        return switch (mode) {
            case ONLINE_REALTIME -> (int) (processingTimeMs * 0.1); // 在线模式成本更高
            case OFFLINE_BATCH -> (int) (processingTimeMs * 0.05); // 离线模式成本较低
            case AUTO_SWITCH -> (int) (processingTimeMs * 0.075); // 中等成本
        };
    }

    /**
     * 记录到滑动窗口
     */
    private void recordRecentMetric(boolean success, long processingTimeMs, ProcessingMode mode) {
        synchronized (recentMetricsLock) {
            recentMetrics.offer(new TimestampedMetric(success, processingTimeMs, mode, System.currentTimeMillis()));
            cleanupOldMetrics();
        }
    }

    /**
     * 清理过期的滑动窗口数据（保留最近5分钟）
     */
    private void cleanupOldMetrics() {
        long fiveMinutesAgo = System.currentTimeMillis() - 5 * 60 * 1000;
        while (!recentMetrics.isEmpty() && recentMetrics.peek().timestamp < fiveMinutesAgo) {
            recentMetrics.poll();
        }
    }

    /**
     * 定时收集和持久化指标
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void collectAndPersistMetrics() {
        try {
            String hourKey = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

            HourlyMetrics hourly = hourlyMetrics.computeIfAbsent(hourKey, k -> new HourlyMetrics());

            // 更新小时指标
            ProcessingMetrics current = getCurrentMetrics();
            hourly.update(current);

            // 清理旧的小时数据（保留24小时）
            cleanupOldHourlyMetrics();

            logger.debug("指标收集完成: 总请求={}, 成功率={:.2f}%, 平均处理时间={:.2f}ms",
                        current.getTotalRequests(),
                        current.getSuccessRate() * 100,
                        current.getAverageProcessingTime());

        } catch (Exception e) {
            logger.error("指标收集异常", e);
        }
    }

    /**
     * 清理旧的小时指标
     */
    private void cleanupOldHourlyMetrics() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        String cutoffKey = cutoff.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

        hourlyMetrics.entrySet().removeIf(entry -> entry.getKey().compareTo(cutoffKey) < 0);
    }

    /**
     * 模式指标类
     */
    private static class ModeMetrics {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong successfulRequests = new AtomicLong(0);
        private final AtomicLong failedRequests = new AtomicLong(0);
        private final DoubleAdder totalProcessingTime = new DoubleAdder();
        private final AtomicInteger totalCostCents = new AtomicInteger(0);

        void recordSuccess(long processingTimeMs) {
            totalRequests.incrementAndGet();
            successfulRequests.incrementAndGet();
            totalProcessingTime.add(processingTimeMs);
        }

        void recordFailure() {
            totalRequests.incrementAndGet();
            failedRequests.incrementAndGet();
        }

        double getAverageProcessingTime() {
            long successful = successfulRequests.get();
            return successful > 0 ? totalProcessingTime.sum() / successful : 0.0;
        }

        void reset() {
            totalRequests.set(0);
            successfulRequests.set(0);
            failedRequests.set(0);
            totalProcessingTime.reset();
            totalCostCents.set(0);
        }
    }

    /**
     * 小时指标类
     */
    public static class HourlyMetrics {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double averageProcessingTime;
        private int totalCostCents;
        private double averageSystemLoad;
        private int updateCount;

        void update(ProcessingMetrics metrics) {
            this.totalRequests = metrics.getTotalRequests();
            this.successfulRequests = metrics.getSuccessfulRequests();
            this.failedRequests = metrics.getFailedRequests();

            // 计算移动平均
            this.averageProcessingTime = (this.averageProcessingTime * updateCount + metrics.getAverageProcessingTime()) / (updateCount + 1);
            this.averageSystemLoad = (this.averageSystemLoad * updateCount + metrics.getSystemLoadAverage()) / (updateCount + 1);

            this.totalCostCents = metrics.getTotalCostCents();
            this.updateCount++;
        }

        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public double getAverageProcessingTime() { return averageProcessingTime; }
        public int getTotalCostCents() { return totalCostCents; }
        public double getAverageSystemLoad() { return averageSystemLoad; }
        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }
    }

    /**
     * 时间戳指标类
     */
    private static class TimestampedMetric {
        final boolean success;
        final long processingTimeMs;
        final ProcessingMode mode;
        final long timestamp;

        TimestampedMetric(boolean success, long processingTimeMs, ProcessingMode mode, long timestamp) {
            this.success = success;
            this.processingTimeMs = processingTimeMs;
            this.mode = mode;
            this.timestamp = timestamp;
        }
    }
}