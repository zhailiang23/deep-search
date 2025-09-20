package com.deepsearch.vector;

import com.deepsearch.vector.model.ProcessingContext;
import com.deepsearch.vector.model.Vector;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 向量服务抽象接口
 * 定义向量生成服务的通用行为
 *
 * @author DeepSearch Vector Team
 */
public interface VectorService {

    /**
     * 生成文本的向量表示
     *
     * @param text 输入文本
     * @param modelName 模型名称
     * @return 异步返回向量结果
     * @throws VectorProcessingException 向量处理异常
     */
    CompletableFuture<Vector> generateEmbedding(String text, String modelName);

    /**
     * 根据处理上下文生成向量表示
     *
     * @param context 处理上下文
     * @return 异步返回向量结果
     * @throws VectorProcessingException 向量处理异常
     */
    CompletableFuture<Vector> generateEmbedding(ProcessingContext context);

    /**
     * 批量生成向量表示
     *
     * @param texts 文本列表
     * @param modelName 模型名称
     * @return 异步返回向量结果列表
     * @throws VectorProcessingException 向量处理异常
     */
    CompletableFuture<List<Vector>> generateEmbeddings(List<String> texts, String modelName);

    /**
     * 批量根据处理上下文生成向量表示
     *
     * @param contexts 处理上下文列表
     * @return 异步返回向量结果列表
     * @throws VectorProcessingException 向量处理异常
     */
    CompletableFuture<List<Vector>> generateEmbeddings(List<ProcessingContext> contexts);

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    String getServiceName();

    /**
     * 获取服务类型（如：openai, local-bert等）
     *
     * @return 服务类型
     */
    String getServiceType();

    /**
     * 获取支持的模型列表
     *
     * @return 支持的模型名称列表
     */
    List<String> getSupportedModels();

    /**
     * 获取默认模型名称
     *
     * @return 默认模型名称
     */
    String getDefaultModel();

    /**
     * 检查是否支持指定模型
     *
     * @param modelName 模型名称
     * @return 是否支持
     */
    boolean supportsModel(String modelName);

    /**
     * 获取指定模型的向量维度
     *
     * @param modelName 模型名称
     * @return 向量维度
     * @throws UnsupportedOperationException 如果模型不支持
     */
    int getModelDimension(String modelName);

    /**
     * 获取指定模型的最大输入长度
     *
     * @param modelName 模型名称
     * @return 最大输入长度（字符数）
     * @throws UnsupportedOperationException 如果模型不支持
     */
    int getMaxInputLength(String modelName);

    /**
     * 检查服务是否可用
     *
     * @return 服务可用性检查结果
     */
    CompletableFuture<ServiceHealth> checkHealth();

    /**
     * 预热服务（加载模型、建立连接等）
     *
     * @return 异步返回预热结果
     */
    CompletableFuture<Void> warmup();

    /**
     * 估算处理成本（以分为单位）
     *
     * @param text 输入文本
     * @param modelName 模型名称
     * @return 预估成本（分）
     */
    int estimateCost(String text, String modelName);

    /**
     * 估算处理时间（毫秒）
     *
     * @param text 输入文本
     * @param modelName 模型名称
     * @return 预估处理时间（毫秒）
     */
    long estimateProcessingTime(String text, String modelName);

    /**
     * 关闭服务，释放资源
     *
     * @return 异步返回关闭结果
     */
    CompletableFuture<Void> shutdown();

    /**
     * 服务健康状态
     */
    enum ServiceHealth {
        HEALTHY("健康", "服务正常运行"),
        DEGRADED("降级", "服务运行但性能下降"),
        UNHEALTHY("不健康", "服务不可用"),
        UNKNOWN("未知", "无法确定服务状态");

        private final String status;
        private final String description;

        ServiceHealth(String status, String description) {
            this.status = status;
            this.description = description;
        }

        public String getStatus() { return status; }
        public String getDescription() { return description; }

        public boolean isHealthy() { return this == HEALTHY; }
        public boolean isDegraded() { return this == DEGRADED; }
        public boolean isUnhealthy() { return this == UNHEALTHY; }
        public boolean isAvailable() { return this == HEALTHY || this == DEGRADED; }
    }

    /**
     * 向量处理异常
     */
    class VectorProcessingException extends RuntimeException {
        private final String errorCode;
        private final boolean retryable;

        public VectorProcessingException(String message) {
            this(message, null, "UNKNOWN", false);
        }

        public VectorProcessingException(String message, Throwable cause) {
            this(message, cause, "UNKNOWN", false);
        }

        public VectorProcessingException(String message, String errorCode, boolean retryable) {
            this(message, null, errorCode, retryable);
        }

        public VectorProcessingException(String message, Throwable cause, String errorCode, boolean retryable) {
            super(message, cause);
            this.errorCode = errorCode;
            this.retryable = retryable;
        }

        public String getErrorCode() { return errorCode; }
        public boolean isRetryable() { return retryable; }

        // 常见错误代码
        public static final String ERROR_RATE_LIMIT = "RATE_LIMIT";
        public static final String ERROR_TIMEOUT = "TIMEOUT";
        public static final String ERROR_INVALID_INPUT = "INVALID_INPUT";
        public static final String ERROR_MODEL_UNAVAILABLE = "MODEL_UNAVAILABLE";
        public static final String ERROR_NETWORK = "NETWORK_ERROR";
        public static final String ERROR_AUTHENTICATION = "AUTHENTICATION_ERROR";
        public static final String ERROR_QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
        public static final String ERROR_INTERNAL = "INTERNAL_ERROR";

        // 创建常见异常的静态方法
        public static VectorProcessingException rateLimitExceeded(String message) {
            return new VectorProcessingException(message, ERROR_RATE_LIMIT, true);
        }

        public static VectorProcessingException timeout(String message) {
            return new VectorProcessingException(message, ERROR_TIMEOUT, true);
        }

        public static VectorProcessingException invalidInput(String message) {
            return new VectorProcessingException(message, ERROR_INVALID_INPUT, false);
        }

        public static VectorProcessingException modelUnavailable(String message) {
            return new VectorProcessingException(message, ERROR_MODEL_UNAVAILABLE, true);
        }

        public static VectorProcessingException networkError(String message, Throwable cause) {
            return new VectorProcessingException(message, cause, ERROR_NETWORK, true);
        }

        public static VectorProcessingException authenticationError(String message) {
            return new VectorProcessingException(message, ERROR_AUTHENTICATION, false);
        }

        public static VectorProcessingException quotaExceeded(String message) {
            return new VectorProcessingException(message, ERROR_QUOTA_EXCEEDED, false);
        }

        public static VectorProcessingException internalError(String message, Throwable cause) {
            return new VectorProcessingException(message, cause, ERROR_INTERNAL, false);
        }
    }

    /**
     * 服务性能指标
     */
    class ServiceMetrics {
        private final long totalRequests;
        private final long successfulRequests;
        private final long failedRequests;
        private final double averageLatencyMs;
        private final int totalCostCents;
        private final long lastRequestTime;

        public ServiceMetrics(long totalRequests, long successfulRequests, long failedRequests,
                             double averageLatencyMs, int totalCostCents, long lastRequestTime) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.failedRequests = failedRequests;
            this.averageLatencyMs = averageLatencyMs;
            this.totalCostCents = totalCostCents;
            this.lastRequestTime = lastRequestTime;
        }

        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public double getAverageLatencyMs() { return averageLatencyMs; }
        public int getTotalCostCents() { return totalCostCents; }
        public long getLastRequestTime() { return lastRequestTime; }

        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulRequests / totalRequests : 0.0;
        }

        public double getFailureRate() {
            return 1.0 - getSuccessRate();
        }
    }
}