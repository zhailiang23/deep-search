package com.deepsearch.vector.config;

import com.deepsearch.vector.model.ProcessingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 向量处理配置属性
 *
 * @author DeepSearch Vector Team
 */
@ConfigurationProperties(prefix = "vector-processing")
@Validated
public class VectorProperties {

    /**
     * 默认处理模式
     */
    @NotNull
    private ProcessingMode defaultMode = ProcessingMode.AUTO_SWITCH;

    /**
     * OpenAI配置
     */
    @Valid
    private OpenAI openai = new OpenAI();

    /**
     * 本地模型配置
     */
    @Valid
    private LocalModels localModels = new LocalModels();

    /**
     * 缓存配置
     */
    @Valid
    private Cache cache = new Cache();

    /**
     * 模式切换配置
     */
    @Valid
    private ModeSwitching modeSwitching = new ModeSwitching();

    /**
     * 任务队列配置
     */
    @Valid
    private TaskQueue taskQueue = new TaskQueue();

    /**
     * 处理配置
     */
    @Valid
    private Processing processing = new Processing();

    /**
     * 指标配置
     */
    @Valid
    private Metrics metrics = new Metrics();

    // Getters and Setters
    public ProcessingMode getDefaultMode() { return defaultMode; }
    public void setDefaultMode(ProcessingMode defaultMode) { this.defaultMode = defaultMode; }

    public OpenAI getOpenai() { return openai; }
    public void setOpenai(OpenAI openai) { this.openai = openai; }

    public LocalModels getLocalModels() { return localModels; }
    public void setLocalModels(LocalModels localModels) { this.localModels = localModels; }

    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    public ModeSwitching getModeSwitching() { return modeSwitching; }
    public void setModeSwitching(ModeSwitching modeSwitching) { this.modeSwitching = modeSwitching; }

    public TaskQueue getTaskQueue() { return taskQueue; }
    public void setTaskQueue(TaskQueue taskQueue) { this.taskQueue = taskQueue; }

    public Processing getProcessing() { return processing; }
    public void setProcessing(Processing processing) { this.processing = processing; }

    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics metrics) { this.metrics = metrics; }

    /**
     * OpenAI配置
     */
    public static class OpenAI {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private Models models = new Models();
        @Min(1) @Max(10000)
        private int rateLimit = 1000;
        @Min(1) @Max(300)
        private int timeoutSeconds = 30;
        @Min(0) @Max(10)
        private int maxRetries = 3;

        // Getters and Setters
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public Models getModels() { return models; }
        public void setModels(Models models) { this.models = models; }

        public int getRateLimit() { return rateLimit; }
        public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

        public static class Models {
            @NotEmpty
            private String small = "text-embedding-3-small";
            @NotEmpty
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
    public static class LocalModels {
        private Bert bert = new Bert();

        public Bert getBert() { return bert; }
        public void setBert(Bert bert) { this.bert = bert; }

        public static class Bert {
            private String modelPath = "./models/sentence-transformer";
            @Min(128) @Max(8192)
            private int maxSequenceLength = 512;
            @Min(1) @Max(128)
            private int batchSize = 32;
            @NotEmpty
            private String device = "cpu";

            public String getModelPath() { return modelPath; }
            public void setModelPath(String modelPath) { this.modelPath = modelPath; }

            public int getMaxSequenceLength() { return maxSequenceLength; }
            public void setMaxSequenceLength(int maxSequenceLength) { this.maxSequenceLength = maxSequenceLength; }

            public int getBatchSize() { return batchSize; }
            public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

            public String getDevice() { return device; }
            public void setDevice(String device) { this.device = device; }
        }
    }

    /**
     * 缓存配置
     */
    public static class Cache {
        private boolean enabled = true;
        @Min(1) @Max(168)
        private int ttlHours = 24;
        @Min(100) @Max(1000000)
        private int maxSize = 10000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getTtlHours() { return ttlHours; }
        public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
    }

    /**
     * 模式切换配置
     */
    public static class ModeSwitching {
        private boolean autoSwitchEnabled = true;
        @Min(0) @Max(100000)
        private int costThresholdCents = 1000;
        @Min(100) @Max(60000)
        private int latencyThresholdMs = 3000;
        @Min(1) @Max(10000)
        private int queueSizeThreshold = 100;
        @Min(1) @Max(100)
        private int loadThresholdPercent = 80;

        public boolean isAutoSwitchEnabled() { return autoSwitchEnabled; }
        public void setAutoSwitchEnabled(boolean autoSwitchEnabled) { this.autoSwitchEnabled = autoSwitchEnabled; }

        public int getCostThresholdCents() { return costThresholdCents; }
        public void setCostThresholdCents(int costThresholdCents) { this.costThresholdCents = costThresholdCents; }

        public int getLatencyThresholdMs() { return latencyThresholdMs; }
        public void setLatencyThresholdMs(int latencyThresholdMs) { this.latencyThresholdMs = latencyThresholdMs; }

        public int getQueueSizeThreshold() { return queueSizeThreshold; }
        public void setQueueSizeThreshold(int queueSizeThreshold) { this.queueSizeThreshold = queueSizeThreshold; }

        public int getLoadThresholdPercent() { return loadThresholdPercent; }
        public void setLoadThresholdPercent(int loadThresholdPercent) { this.loadThresholdPercent = loadThresholdPercent; }
    }

    /**
     * 任务队列配置
     */
    public static class TaskQueue {
        @Min(1) @Max(1000)
        private int batchSize = 50;
        @Min(1) @Max(100)
        private int maxConcurrentTasks = 10;
        @Min(1) @Max(3600)
        private int retryDelaySeconds = 60;
        @Min(100) @Max(100000)
        private int maxQueueSize = 1000;

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

        public int getMaxConcurrentTasks() { return maxConcurrentTasks; }
        public void setMaxConcurrentTasks(int maxConcurrentTasks) { this.maxConcurrentTasks = maxConcurrentTasks; }

        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public void setRetryDelaySeconds(int retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

        public int getMaxQueueSize() { return maxQueueSize; }
        public void setMaxQueueSize(int maxQueueSize) { this.maxQueueSize = maxQueueSize; }
    }

    /**
     * 处理配置
     */
    public static class Processing {
        @Min(128) @Max(8192)
        private int defaultDimensions = 1536;
        @Min(512) @Max(32768)
        private int chunkSize = 8192;
        @Min(0) @Max(1024)
        private int overlapSize = 200;

        public int getDefaultDimensions() { return defaultDimensions; }
        public void setDefaultDimensions(int defaultDimensions) { this.defaultDimensions = defaultDimensions; }

        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

        public int getOverlapSize() { return overlapSize; }
        public void setOverlapSize(int overlapSize) { this.overlapSize = overlapSize; }
    }

    /**
     * 指标配置
     */
    public static class Metrics {
        private boolean enabled = true;
        @Min(10) @Max(3600)
        private int collectionIntervalSeconds = 60;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getCollectionIntervalSeconds() { return collectionIntervalSeconds; }
        public void setCollectionIntervalSeconds(int collectionIntervalSeconds) { this.collectionIntervalSeconds = collectionIntervalSeconds; }
    }
}