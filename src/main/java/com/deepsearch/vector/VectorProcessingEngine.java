package com.deepsearch.vector;

import com.deepsearch.vector.model.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 向量处理引擎核心接口
 * 支持离线批量处理和在线实时处理两种模式
 *
 * @author DeepSearch Vector Team
 */
public interface VectorProcessingEngine {

    /**
     * 处理单个文档，生成向量表示
     *
     * @param context 处理上下文，包含文档内容和处理要求
     * @return 异步返回向量结果
     */
    CompletableFuture<Vector> processDocument(ProcessingContext context);

    /**
     * 批量处理文档，生成向量表示
     *
     * @param contexts 处理上下文列表
     * @return 异步返回向量结果列表
     */
    CompletableFuture<List<Vector>> processDocuments(List<ProcessingContext> contexts);

    /**
     * 处理查询文本，生成向量表示
     * 通常使用在线实时模式，优先考虑响应速度
     *
     * @param query 查询文本
     * @param mode 处理模式
     * @return 异步返回向量结果
     */
    CompletableFuture<Vector> processQuery(String query, ProcessingMode mode);

    /**
     * 处理查询文本（使用默认在线模式）
     *
     * @param query 查询文本
     * @return 异步返回向量结果
     */
    default CompletableFuture<Vector> processQuery(String query) {
        return processQuery(query, ProcessingMode.ONLINE_REALTIME);
    }

    /**
     * 切换处理模式
     *
     * @param mode 目标处理模式
     */
    void switchMode(ProcessingMode mode);

    /**
     * 获取当前处理模式
     *
     * @return 当前处理模式
     */
    ProcessingMode getCurrentMode();

    /**
     * 获取处理指标和统计信息
     *
     * @return 处理指标
     */
    ProcessingMetrics getMetrics();

    /**
     * 获取指标摘要
     *
     * @return 指标摘要
     */
    default ProcessingMetrics.MetricsSummary getMetricsSummary() {
        return getMetrics().getSummary();
    }

    /**
     * 检查引擎健康状态
     *
     * @return 健康状态信息
     */
    CompletableFuture<HealthStatus> checkHealth();

    /**
     * 预热引擎（加载模型、初始化连接等）
     *
     * @return 异步返回预热结果
     */
    CompletableFuture<Void> warmup();

    /**
     * 停止引擎，清理资源
     *
     * @return 异步返回停止结果
     */
    CompletableFuture<Void> shutdown();

    /**
     * 重置处理指标
     */
    void resetMetrics();

    /**
     * 获取支持的模型列表
     *
     * @return 支持的模型名称列表
     */
    List<String> getSupportedModels();

    /**
     * 获取当前队列大小
     *
     * @return 待处理任务数量
     */
    int getQueueSize();

    /**
     * 检查是否支持指定模型
     *
     * @param modelName 模型名称
     * @return 是否支持
     */
    boolean supportsModel(String modelName);

    /**
     * 获取模型的默认维度
     *
     * @param modelName 模型名称
     * @return 向量维度，如果模型不支持则返回-1
     */
    int getModelDimension(String modelName);

    /**
     * 健康状态枚举
     */
    enum HealthStatus {
        HEALTHY("健康"),
        DEGRADED("降级"),
        UNHEALTHY("不健康"),
        UNKNOWN("未知");

        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public boolean isHealthy() {
            return this == HEALTHY;
        }

        public boolean isDegraded() {
            return this == DEGRADED;
        }

        public boolean isUnhealthy() {
            return this == UNHEALTHY;
        }
    }

    /**
     * 引擎状态信息
     */
    class EngineStatus {
        private final HealthStatus healthStatus;
        private final ProcessingMode currentMode;
        private final int queueSize;
        private final List<String> activeModels;
        private final String lastError;

        public EngineStatus(HealthStatus healthStatus, ProcessingMode currentMode,
                           int queueSize, List<String> activeModels, String lastError) {
            this.healthStatus = healthStatus;
            this.currentMode = currentMode;
            this.queueSize = queueSize;
            this.activeModels = activeModels;
            this.lastError = lastError;
        }

        public HealthStatus getHealthStatus() { return healthStatus; }
        public ProcessingMode getCurrentMode() { return currentMode; }
        public int getQueueSize() { return queueSize; }
        public List<String> getActiveModels() { return activeModels; }
        public String getLastError() { return lastError; }

        @Override
        public String toString() {
            return "EngineStatus{" +
                    "healthStatus=" + healthStatus +
                    ", currentMode=" + currentMode +
                    ", queueSize=" + queueSize +
                    ", activeModels=" + activeModels.size() +
                    ", hasError=" + (lastError != null) +
                    '}';
        }
    }
}