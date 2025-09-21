package com.deepsearch.vector.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 向量处理上下文
 * 包含处理请求的相关信息和约束条件
 *
 * @author DeepSearch Vector Team
 */
public class ProcessingContext {

    private final String requestId;
    private final String sourceText;
    private final String documentId;
    private final ProcessingMode requestedMode;
    private final boolean urgent;
    private final int maxRetries;
    private final long timeoutMs;
    private final Integer maxCostCents;
    private final String preferredModel;
    private final Map<String, Object> properties;
    private final Instant createdAt;

    private ProcessingContext(Builder builder) {
        this.requestId = builder.requestId;
        this.sourceText = builder.sourceText;
        this.documentId = builder.documentId;
        this.requestedMode = builder.requestedMode;
        this.urgent = builder.urgent;
        this.maxRetries = builder.maxRetries;
        this.timeoutMs = builder.timeoutMs;
        this.maxCostCents = builder.maxCostCents;
        this.preferredModel = builder.preferredModel;
        this.properties = new HashMap<>(builder.properties);
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
    }

    /**
     * 创建处理上下文构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建简单的处理上下文
     */
    public static ProcessingContext create(String sourceText, ProcessingMode mode) {
        return builder()
                .sourceText(sourceText)
                .requestedMode(mode)
                .build();
    }

    /**
     * 创建紧急处理上下文
     */
    public static ProcessingContext createUrgent(String sourceText) {
        return builder()
                .sourceText(sourceText)
                .requestedMode(ProcessingMode.ONLINE_REALTIME)
                .urgent(true)
                .timeoutMs(2000L) // 2秒超时
                .build();
    }

    /**
     * 创建批量处理上下文
     */
    public static ProcessingContext createBatch(String sourceText, String documentId) {
        return builder()
                .sourceText(sourceText)
                .documentId(documentId)
                .requestedMode(ProcessingMode.OFFLINE_BATCH)
                .urgent(false)
                .maxRetries(3)
                .timeoutMs(30000L) // 30秒超时
                .build();
    }

    public String getRequestId() {
        return requestId;
    }

    public String getSourceText() {
        return sourceText;
    }

    public String getDocumentId() {
        return documentId;
    }

    public ProcessingMode getRequestedMode() {
        return requestedMode;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public Integer getMaxCostCents() {
        return maxCostCents;
    }

    public String getPreferredModel() {
        return preferredModel;
    }

    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> type) {
        Object value = properties.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * 检查是否有成本限制
     */
    public boolean hasCostLimit() {
        return maxCostCents != null && maxCostCents > 0;
    }

    /**
     * 检查是否有首选模型
     */
    public boolean hasPreferredModel() {
        return preferredModel != null && !preferredModel.trim().isEmpty();
    }

    /**
     * 检查是否已超时
     */
    public boolean isTimedOut() {
        long elapsed = Instant.now().toEpochMilli() - createdAt.toEpochMilli();
        return elapsed > timeoutMs;
    }

    /**
     * 获取剩余超时时间（毫秒）
     */
    public long getRemainingTimeoutMs() {
        long elapsed = Instant.now().toEpochMilli() - createdAt.toEpochMilli();
        return Math.max(0, timeoutMs - elapsed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessingContext that = (ProcessingContext) o;
        return urgent == that.urgent &&
                maxRetries == that.maxRetries &&
                timeoutMs == that.timeoutMs &&
                Objects.equals(requestId, that.requestId) &&
                Objects.equals(sourceText, that.sourceText) &&
                Objects.equals(documentId, that.documentId) &&
                requestedMode == that.requestedMode &&
                Objects.equals(maxCostCents, that.maxCostCents) &&
                Objects.equals(preferredModel, that.preferredModel) &&
                Objects.equals(properties, that.properties) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, sourceText, documentId, requestedMode, urgent, maxRetries, timeoutMs, maxCostCents, preferredModel, properties, createdAt);
    }

    @Override
    public String toString() {
        return "ProcessingContext{" +
                "requestId='" + requestId + '\'' +
                ", documentId='" + documentId + '\'' +
                ", requestedMode=" + requestedMode +
                ", urgent=" + urgent +
                ", maxRetries=" + maxRetries +
                ", timeoutMs=" + timeoutMs +
                ", maxCostCents=" + maxCostCents +
                ", preferredModel='" + preferredModel + '\'' +
                ", textLength=" + (sourceText != null ? sourceText.length() : 0) +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * 处理上下文构建器
     */
    public static class Builder {
        private String requestId;
        private String sourceText;
        private String documentId;
        private ProcessingMode requestedMode = ProcessingMode.AUTO_SWITCH;
        private boolean urgent = false;
        private int maxRetries = 3;
        private long timeoutMs = 30000L; // 默认30秒
        private Integer maxCostCents;
        private String preferredModel;
        private Map<String, Object> properties = new HashMap<>();
        private Instant createdAt;

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder sourceText(String sourceText) {
            this.sourceText = sourceText;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder requestedMode(ProcessingMode requestedMode) {
            this.requestedMode = requestedMode;
            return this;
        }

        public Builder urgent(boolean urgent) {
            this.urgent = urgent;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder maxCostCents(Integer maxCostCents) {
            this.maxCostCents = maxCostCents;
            return this;
        }

        public Builder preferredModel(String preferredModel) {
            this.preferredModel = preferredModel;
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties.clear();
            this.properties.putAll(properties);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ProcessingContext build() {
            if (sourceText == null || sourceText.trim().isEmpty()) {
                throw new IllegalArgumentException("源文本不能为空");
            }
            if (requestId == null || requestId.trim().isEmpty()) {
                this.requestId = "req_" + System.currentTimeMillis() + "_" + Math.abs(sourceText.hashCode());
            }

            return new ProcessingContext(this);
        }
    }
}