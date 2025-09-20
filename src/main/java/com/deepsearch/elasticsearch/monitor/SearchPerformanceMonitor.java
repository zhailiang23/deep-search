package com.deepsearch.elasticsearch.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SearchPerformanceMonitor {

    private static final int MAX_METRICS_HISTORY = 1000;
    private static final long SLOW_QUERY_THRESHOLD_MS = 1000; // 1秒

    // 性能指标收集
    private final LongAdder totalSearches = new LongAdder();
    private final LongAdder totalResponseTime = new LongAdder();
    private final LongAdder slowQueries = new LongAdder();
    private final LongAdder failedQueries = new LongAdder();

    // 分类型统计
    private final Map<String, LongAdder> searchTypeCounters = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> searchTypeResponseTimes = new ConcurrentHashMap<>();

    // 性能记录历史
    private final List<SearchMetrics> metricsHistory = new ArrayList<>();
    private final Object historyLock = new Object();

    /**
     * 记录搜索性能指标
     */
    public void recordSearch(String searchType, String query, long responseTimeMs,
                           int resultCount, boolean success) {

        // 更新总体统计
        totalSearches.increment();
        totalResponseTime.add(responseTimeMs);

        if (!success) {
            failedQueries.increment();
        }

        if (responseTimeMs > SLOW_QUERY_THRESHOLD_MS) {
            slowQueries.increment();
            log.warn("慢查询检测: type={}, query='{}', time={}ms",
                    searchType, query, responseTimeMs);
        }

        // 更新分类型统计
        searchTypeCounters.computeIfAbsent(searchType, k -> new LongAdder()).increment();
        searchTypeResponseTimes.computeIfAbsent(searchType, k -> new LongAdder()).add(responseTimeMs);

        // 记录详细指标历史
        SearchMetrics metrics = new SearchMetrics(
                LocalDateTime.now(),
                searchType,
                query,
                responseTimeMs,
                resultCount,
                success
        );

        synchronized (historyLock) {
            metricsHistory.add(metrics);
            // 保持历史记录在限制范围内
            if (metricsHistory.size() > MAX_METRICS_HISTORY) {
                metricsHistory.remove(0);
            }
        }

        log.debug("搜索性能记录: type={}, time={}ms, results={}, success={}",
                searchType, responseTimeMs, resultCount, success);
    }

    /**
     * 获取总体性能统计
     */
    public PerformanceStats getOverallStats() {
        long totalCount = totalSearches.sum();
        long totalTime = totalResponseTime.sum();
        double avgResponseTime = totalCount > 0 ? (double) totalTime / totalCount : 0.0;

        return new PerformanceStats(
                totalCount,
                avgResponseTime,
                slowQueries.sum(),
                failedQueries.sum(),
                calculateSuccessRate(),
                getCurrentQPS()
        );
    }

    /**
     * 获取分类型性能统计
     */
    public Map<String, TypePerformanceStats> getTypeStats() {
        Map<String, TypePerformanceStats> stats = new ConcurrentHashMap<>();

        for (String type : searchTypeCounters.keySet()) {
            long count = searchTypeCounters.get(type).sum();
            long totalTime = searchTypeResponseTimes.get(type).sum();
            double avgResponseTime = count > 0 ? (double) totalTime / count : 0.0;

            stats.put(type, new TypePerformanceStats(type, count, avgResponseTime));
        }

        return stats;
    }

    /**
     * 获取慢查询列表
     */
    public List<SearchMetrics> getSlowQueries(int limit) {
        synchronized (historyLock) {
            return metricsHistory.stream()
                    .filter(m -> m.getResponseTimeMs() > SLOW_QUERY_THRESHOLD_MS)
                    .sorted((a, b) -> Long.compare(b.getResponseTimeMs(), a.getResponseTimeMs()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取最近的性能趋势
     */
    public List<SearchMetrics> getRecentMetrics(int minutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(minutes);

        synchronized (historyLock) {
            return metricsHistory.stream()
                    .filter(m -> m.getTimestamp().isAfter(cutoff))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 计算成功率
     */
    private double calculateSuccessRate() {
        long total = totalSearches.sum();
        long failed = failedQueries.sum();
        return total > 0 ? (double) (total - failed) / total * 100 : 100.0;
    }

    /**
     * 计算当前QPS（基于最近1分钟）
     */
    private double getCurrentQPS() {
        List<SearchMetrics> recentMetrics = getRecentMetrics(1);
        return recentMetrics.size() / 60.0; // 每秒查询数
    }

    /**
     * 重置所有统计数据
     */
    public void reset() {
        totalSearches.reset();
        totalResponseTime.reset();
        slowQueries.reset();
        failedQueries.reset();

        searchTypeCounters.clear();
        searchTypeResponseTimes.clear();

        synchronized (historyLock) {
            metricsHistory.clear();
        }

        log.info("搜索性能监控数据已重置");
    }

    /**
     * 检查系统健康状态
     */
    public HealthStatus getHealthStatus() {
        PerformanceStats stats = getOverallStats();

        // 健康状态评估规则
        if (stats.getSuccessRate() < 95.0) {
            return new HealthStatus("CRITICAL", "成功率过低: " + stats.getSuccessRate() + "%");
        }

        if (stats.getAverageResponseTime() > 2000) {
            return new HealthStatus("WARNING", "平均响应时间过高: " + stats.getAverageResponseTime() + "ms");
        }

        if (stats.getSlowQueryCount() > stats.getTotalSearches() * 0.1) {
            return new HealthStatus("WARNING", "慢查询比例过高: " +
                    (stats.getSlowQueryCount() * 100.0 / stats.getTotalSearches()) + "%");
        }

        return new HealthStatus("HEALTHY", "搜索服务运行正常");
    }

    // 内部数据类
    public static class SearchMetrics {
        private final LocalDateTime timestamp;
        private final String searchType;
        private final String query;
        private final long responseTimeMs;
        private final int resultCount;
        private final boolean success;

        public SearchMetrics(LocalDateTime timestamp, String searchType, String query,
                           long responseTimeMs, int resultCount, boolean success) {
            this.timestamp = timestamp;
            this.searchType = searchType;
            this.query = query;
            this.responseTimeMs = responseTimeMs;
            this.resultCount = resultCount;
            this.success = success;
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSearchType() { return searchType; }
        public String getQuery() { return query; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public int getResultCount() { return resultCount; }
        public boolean isSuccess() { return success; }
    }

    public static class PerformanceStats {
        private final long totalSearches;
        private final double averageResponseTime;
        private final long slowQueryCount;
        private final long failedQueryCount;
        private final double successRate;
        private final double currentQPS;

        public PerformanceStats(long totalSearches, double averageResponseTime,
                              long slowQueryCount, long failedQueryCount,
                              double successRate, double currentQPS) {
            this.totalSearches = totalSearches;
            this.averageResponseTime = averageResponseTime;
            this.slowQueryCount = slowQueryCount;
            this.failedQueryCount = failedQueryCount;
            this.successRate = successRate;
            this.currentQPS = currentQPS;
        }

        // Getters
        public long getTotalSearches() { return totalSearches; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public long getSlowQueryCount() { return slowQueryCount; }
        public long getFailedQueryCount() { return failedQueryCount; }
        public double getSuccessRate() { return successRate; }
        public double getCurrentQPS() { return currentQPS; }
    }

    public static class TypePerformanceStats {
        private final String searchType;
        private final long searchCount;
        private final double averageResponseTime;

        public TypePerformanceStats(String searchType, long searchCount, double averageResponseTime) {
            this.searchType = searchType;
            this.searchCount = searchCount;
            this.averageResponseTime = averageResponseTime;
        }

        // Getters
        public String getSearchType() { return searchType; }
        public long getSearchCount() { return searchCount; }
        public double getAverageResponseTime() { return averageResponseTime; }
    }

    public static class HealthStatus {
        private final String status;
        private final String message;
        private final LocalDateTime timestamp;

        public HealthStatus(String status, String message) {
            this.status = status;
            this.message = message;
            this.timestamp = LocalDateTime.now();
        }

        // Getters
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}