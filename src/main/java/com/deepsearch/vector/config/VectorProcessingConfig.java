package com.deepsearch.vector.config;

import com.deepsearch.vector.enums.ProcessingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 向量处理配置
 */
@Configuration
@ConfigurationProperties(prefix = "vector-processing")
public class VectorProcessingConfig {

    private ProcessingMode defaultMode = ProcessingMode.AUTO_SWITCH;
    private OpenAIConfig openai = new OpenAIConfig();
    private LocalModelsConfig localModels = new LocalModelsConfig();
    private CacheConfig cache = new CacheConfig();
    private ModeSwitchingConfig modeSwitching = new ModeSwitchingConfig();
    private TaskQueueConfig taskQueue = new TaskQueueConfig();

    // Getters and Setters
    public ProcessingMode getDefaultMode() { return defaultMode; }
    public void setDefaultMode(ProcessingMode defaultMode) { this.defaultMode = defaultMode; }

    public OpenAIConfig getOpenai() { return openai; }
    public void setOpenai(OpenAIConfig openai) { this.openai = openai; }

    public LocalModelsConfig getLocalModels() { return localModels; }
    public void setLocalModels(LocalModelsConfig localModels) { this.localModels = localModels; }

    public CacheConfig getCache() { return cache; }
    public void setCache(CacheConfig cache) { this.cache = cache; }

    public ModeSwitchingConfig getModeSwitching() { return modeSwitching; }
    public void setModeSwitching(ModeSwitchingConfig modeSwitching) { this.modeSwitching = modeSwitching; }

    public TaskQueueConfig getTaskQueue() { return taskQueue; }
    public void setTaskQueue(TaskQueueConfig taskQueue) { this.taskQueue = taskQueue; }

    /**
     * OpenAI配置
     */
    public static class OpenAIConfig {
        private String apiKey;
        private ModelsConfig models = new ModelsConfig();
        private Integer rateLimit = 1000; // requests per minute

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public ModelsConfig getModels() { return models; }
        public void setModels(ModelsConfig models) { this.models = models; }

        public Integer getRateLimit() { return rateLimit; }
        public void setRateLimit(Integer rateLimit) { this.rateLimit = rateLimit; }

        public static class ModelsConfig {
            private String small = "text-embedding-3-small";
            private String large = "text-embedding-3-large";

            public String getSmall() { return small; }
            public void setSmall(String small) { this.small = small; }

            public String getLarge() { return large; }
            public void setLarge(String large) { this.large = large; }
        }
    }

    /**
     * 本地模型配置
     */
    public static class LocalModelsConfig {
        private BertConfig bert = new BertConfig();

        public BertConfig getBert() { return bert; }
        public void setBert(BertConfig bert) { this.bert = bert; }

        public static class BertConfig {
            private String modelPath = "./models/sentence-transformer";
            private Integer maxSequenceLength = 512;

            public String getModelPath() { return modelPath; }
            public void setModelPath(String modelPath) { this.modelPath = modelPath; }

            public Integer getMaxSequenceLength() { return maxSequenceLength; }
            public void setMaxSequenceLength(Integer maxSequenceLength) { this.maxSequenceLength = maxSequenceLength; }
        }
    }

    /**
     * 缓存配置
     */
    public static class CacheConfig {
        private boolean enabled = true;
        private Duration ttl = Duration.ofHours(24);
        private Integer maxSize = 10000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Duration getTtl() { return ttl; }
        public void setTtl(Duration ttl) { this.ttl = ttl; }

        public Integer getMaxSize() { return maxSize; }
        public void setMaxSize(Integer maxSize) { this.maxSize = maxSize; }
    }

    /**
     * 模式切换配置
     */
    public static class ModeSwitchingConfig {
        private boolean autoSwitchEnabled = true;
        private Integer costThresholdCents = 1000; // 每小时成本阈值
        private Long latencyThresholdMs = 3000L; // 延迟阈值
        private Integer queueSizeThreshold = 100; // 队列长度阈值
        private Double loadThreshold = 0.8; // 系统负载阈值

        public boolean isAutoSwitchEnabled() { return autoSwitchEnabled; }
        public void setAutoSwitchEnabled(boolean autoSwitchEnabled) { this.autoSwitchEnabled = autoSwitchEnabled; }

        public Integer getCostThresholdCents() { return costThresholdCents; }
        public void setCostThresholdCents(Integer costThresholdCents) { this.costThresholdCents = costThresholdCents; }

        public Long getLatencyThresholdMs() { return latencyThresholdMs; }
        public void setLatencyThresholdMs(Long latencyThresholdMs) { this.latencyThresholdMs = latencyThresholdMs; }

        public Integer getQueueSizeThreshold() { return queueSizeThreshold; }
        public void setQueueSizeThreshold(Integer queueSizeThreshold) { this.queueSizeThreshold = queueSizeThreshold; }

        public Double getLoadThreshold() { return loadThreshold; }
        public void setLoadThreshold(Double loadThreshold) { this.loadThreshold = loadThreshold; }
    }

    /**
     * 任务队列配置
     */
    public static class TaskQueueConfig {
        private Integer batchSize = 50;
        private Integer maxConcurrentTasks = 10;
        private Duration retryDelaySeconds = Duration.ofSeconds(60);
        private Integer maxRetries = 3;

        public Integer getBatchSize() { return batchSize; }
        public void setBatchSize(Integer batchSize) { this.batchSize = batchSize; }

        public Integer getMaxConcurrentTasks() { return maxConcurrentTasks; }
        public void setMaxConcurrentTasks(Integer maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }

        public Duration getRetryDelaySeconds() { return retryDelaySeconds; }
        public void setRetryDelaySeconds(Duration retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

        public Integer getMaxRetries() { return maxRetries; }
        public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }
    }
}