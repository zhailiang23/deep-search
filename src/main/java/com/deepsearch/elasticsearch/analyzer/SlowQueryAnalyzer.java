package com.deepsearch.elasticsearch.analyzer;

import com.deepsearch.elasticsearch.monitor.SearchPerformanceMonitor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SlowQueryAnalyzer {

    @Autowired
    private SearchPerformanceMonitor performanceMonitor;

    private static final long SLOW_QUERY_THRESHOLD = 1000; // 1秒
    private static final long VERY_SLOW_QUERY_THRESHOLD = 3000; // 3秒
    private static final int ANALYSIS_REPORT_SIZE = 10;

    /**
     * 定期分析慢查询（每30分钟执行一次）
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30分钟
    public void analyzeSlowQueries() {
        log.info("开始执行慢查询分析...");

        try {
            SlowQueryReport report = generateSlowQueryReport();

            if (report.hasSlowQueries()) {
                logSlowQueryReport(report);

                // 如果有严重慢查询，发出警告
                if (report.hasVerySlowQueries()) {
                    log.warn("检测到严重慢查询，建议立即优化！");
                }
            } else {
                log.info("慢查询分析完成，无慢查询检测到");
            }

        } catch (Exception e) {
            log.error("慢查询分析执行失败", e);
        }
    }

    /**
     * 生成慢查询报告
     */
    public SlowQueryReport generateSlowQueryReport() {
        List<SearchPerformanceMonitor.SearchMetrics> slowQueries =
            performanceMonitor.getSlowQueries(100);

        // 分析慢查询模式
        Map<String, List<SearchPerformanceMonitor.SearchMetrics>> queriesByType =
            slowQueries.stream()
                .collect(Collectors.groupingBy(SearchPerformanceMonitor.SearchMetrics::getSearchType));

        // 分析查询文本模式
        Map<String, Long> queryPatterns = analyzeSimilarQueries(slowQueries);

        // 分析响应时间分布
        Map<String, Long> responseTimeDistribution = analyzeResponseTimeDistribution(slowQueries);

        // 找出最慢的查询
        List<SearchPerformanceMonitor.SearchMetrics> slowestQueries =
            slowQueries.stream()
                .sorted((a, b) -> Long.compare(b.getResponseTimeMs(), a.getResponseTimeMs()))
                .limit(ANALYSIS_REPORT_SIZE)
                .collect(Collectors.toList());

        return new SlowQueryReport(
            LocalDateTime.now(),
            slowQueries.size(),
            countVerySlowQueries(slowQueries),
            queriesByType,
            queryPatterns,
            responseTimeDistribution,
            slowestQueries,
            generateOptimizationSuggestions(slowQueries)
        );
    }

    /**
     * 分析相似查询模式
     */
    private Map<String, Long> analyzeSimilarQueries(List<SearchPerformanceMonitor.SearchMetrics> slowQueries) {
        Map<String, Long> patterns = new HashMap<>();

        for (SearchPerformanceMonitor.SearchMetrics metrics : slowQueries) {
            String pattern = extractQueryPattern(metrics.getQuery());
            patterns.merge(pattern, 1L, Long::sum);
        }

        // 返回出现频率最高的模式
        return patterns.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(ANALYSIS_REPORT_SIZE)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * 提取查询模式（简化查询文本，识别相似模式）
     */
    private String extractQueryPattern(String query) {
        if (query == null || query.isEmpty()) {
            return "空查询";
        }

        // 移除数字，保留查询结构
        String pattern = query.replaceAll("\\d+", "N")
                             .replaceAll("\\s+", " ")
                             .trim()
                             .toLowerCase();

        // 如果查询很长，截取前50个字符
        if (pattern.length() > 50) {
            pattern = pattern.substring(0, 47) + "...";
        }

        return pattern;
    }

    /**
     * 分析响应时间分布
     */
    private Map<String, Long> analyzeResponseTimeDistribution(List<SearchPerformanceMonitor.SearchMetrics> slowQueries) {
        Map<String, Long> distribution = new LinkedHashMap<>();

        long count1to2s = slowQueries.stream()
            .filter(m -> m.getResponseTimeMs() >= 1000 && m.getResponseTimeMs() < 2000)
            .count();

        long count2to5s = slowQueries.stream()
            .filter(m -> m.getResponseTimeMs() >= 2000 && m.getResponseTimeMs() < 5000)
            .count();

        long count5to10s = slowQueries.stream()
            .filter(m -> m.getResponseTimeMs() >= 5000 && m.getResponseTimeMs() < 10000)
            .count();

        long countOver10s = slowQueries.stream()
            .filter(m -> m.getResponseTimeMs() >= 10000)
            .count();

        distribution.put("1-2秒", count1to2s);
        distribution.put("2-5秒", count2to5s);
        distribution.put("5-10秒", count5to10s);
        distribution.put("10秒以上", countOver10s);

        return distribution;
    }

    /**
     * 统计严重慢查询数量
     */
    private long countVerySlowQueries(List<SearchPerformanceMonitor.SearchMetrics> slowQueries) {
        return slowQueries.stream()
            .filter(m -> m.getResponseTimeMs() > VERY_SLOW_QUERY_THRESHOLD)
            .count();
    }

    /**
     * 生成优化建议
     */
    private List<String> generateOptimizationSuggestions(List<SearchPerformanceMonitor.SearchMetrics> slowQueries) {
        List<String> suggestions = new ArrayList<>();

        if (slowQueries.isEmpty()) {
            return suggestions;
        }

        // 分析查询类型分布
        Map<String, Long> typeCount = slowQueries.stream()
            .collect(Collectors.groupingBy(
                SearchPerformanceMonitor.SearchMetrics::getSearchType,
                Collectors.counting()
            ));

        if (typeCount.getOrDefault("vector", 0L) > slowQueries.size() * 0.6) {
            suggestions.add("向量搜索慢查询较多，建议：1) 调整向量维度大小；2) 优化向量索引配置；3) 增加候选文档数量限制");
        }

        if (typeCount.getOrDefault("hybrid", 0L) > slowQueries.size() * 0.4) {
            suggestions.add("混合搜索性能较差，建议：1) 调整关键词和向量权重；2) 优化查询结合策略；3) 使用缓存机制");
        }

        // 检查查询长度
        long longQueries = slowQueries.stream()
            .filter(m -> m.getQuery() != null && m.getQuery().length() > 100)
            .count();

        if (longQueries > slowQueries.size() * 0.3) {
            suggestions.add("长查询文本较多，建议：1) 限制查询文本长度；2) 提取关键词；3) 使用查询预处理");
        }

        // 检查结果数量
        OptionalDouble avgResults = slowQueries.stream()
            .mapToInt(SearchPerformanceMonitor.SearchMetrics::getResultCount)
            .average();

        if (avgResults.isPresent() && avgResults.getAsDouble() > 500) {
            suggestions.add("搜索结果数量较大，建议：1) 设置合理的分页大小；2) 添加更精确的过滤条件；3) 使用搜索后过滤");
        }

        // 通用优化建议
        if (slowQueries.size() > 10) {
            suggestions.add("总体性能建议：1) 检查Elasticsearch集群资源；2) 优化索引分片配置；3) 启用查询缓存；4) 考虑添加搜索预热");
        }

        return suggestions;
    }

    /**
     * 记录慢查询报告到日志
     */
    private void logSlowQueryReport(SlowQueryReport report) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n========== 慢查询分析报告 ==========\n");
        logMessage.append("分析时间: ").append(report.getAnalysisTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        logMessage.append("慢查询总数: ").append(report.getTotalSlowQueries()).append("\n");
        logMessage.append("严重慢查询数: ").append(report.getVerySlowQueries()).append("\n\n");

        // 查询类型分布
        logMessage.append("查询类型分布:\n");
        report.getQueriesByType().forEach((type, queries) ->
            logMessage.append("  ").append(type).append(": ").append(queries.size()).append(" 个\n")
        );

        // 响应时间分布
        logMessage.append("\n响应时间分布:\n");
        report.getResponseTimeDistribution().forEach((range, count) ->
            logMessage.append("  ").append(range).append(": ").append(count).append(" 个\n")
        );

        // 最慢查询前5个
        logMessage.append("\n最慢查询 TOP 5:\n");
        report.getSlowestQueries().stream()
            .limit(5)
            .forEach(metrics -> logMessage.append("  ")
                .append(metrics.getSearchType()).append(" - ")
                .append(metrics.getResponseTimeMs()).append("ms - ")
                .append(metrics.getQuery().length() > 50 ?
                    metrics.getQuery().substring(0, 47) + "..." : metrics.getQuery())
                .append("\n")
            );

        // 优化建议
        if (!report.getOptimizationSuggestions().isEmpty()) {
            logMessage.append("\n优化建议:\n");
            report.getOptimizationSuggestions().forEach(suggestion ->
                logMessage.append("  • ").append(suggestion).append("\n")
            );
        }

        logMessage.append("=====================================\n");

        log.warn(logMessage.toString());
    }

    /**
     * 慢查询报告数据类
     */
    public static class SlowQueryReport {
        private final LocalDateTime analysisTime;
        private final long totalSlowQueries;
        private final long verySlowQueries;
        private final Map<String, List<SearchPerformanceMonitor.SearchMetrics>> queriesByType;
        private final Map<String, Long> queryPatterns;
        private final Map<String, Long> responseTimeDistribution;
        private final List<SearchPerformanceMonitor.SearchMetrics> slowestQueries;
        private final List<String> optimizationSuggestions;

        public SlowQueryReport(LocalDateTime analysisTime, long totalSlowQueries, long verySlowQueries,
                             Map<String, List<SearchPerformanceMonitor.SearchMetrics>> queriesByType,
                             Map<String, Long> queryPatterns, Map<String, Long> responseTimeDistribution,
                             List<SearchPerformanceMonitor.SearchMetrics> slowestQueries,
                             List<String> optimizationSuggestions) {
            this.analysisTime = analysisTime;
            this.totalSlowQueries = totalSlowQueries;
            this.verySlowQueries = verySlowQueries;
            this.queriesByType = queriesByType;
            this.queryPatterns = queryPatterns;
            this.responseTimeDistribution = responseTimeDistribution;
            this.slowestQueries = slowestQueries;
            this.optimizationSuggestions = optimizationSuggestions;
        }

        public boolean hasSlowQueries() {
            return totalSlowQueries > 0;
        }

        public boolean hasVerySlowQueries() {
            return verySlowQueries > 0;
        }

        // Getters
        public LocalDateTime getAnalysisTime() { return analysisTime; }
        public long getTotalSlowQueries() { return totalSlowQueries; }
        public long getVerySlowQueries() { return verySlowQueries; }
        public Map<String, List<SearchPerformanceMonitor.SearchMetrics>> getQueriesByType() { return queriesByType; }
        public Map<String, Long> getQueryPatterns() { return queryPatterns; }
        public Map<String, Long> getResponseTimeDistribution() { return responseTimeDistribution; }
        public List<SearchPerformanceMonitor.SearchMetrics> getSlowestQueries() { return slowestQueries; }
        public List<String> getOptimizationSuggestions() { return optimizationSuggestions; }
    }
}