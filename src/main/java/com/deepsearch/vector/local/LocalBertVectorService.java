package com.deepsearch.vector.local;

import com.deepsearch.vector.VectorService;
import com.deepsearch.vector.config.VectorProperties;
import com.deepsearch.vector.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 本地BERT向量服务实现
 * 集成本地sentence-transformers BERT模型
 *
 * @author DeepSearch Vector Team
 */
@Service
public class LocalBertVectorService implements VectorService {

    private static final Logger logger = LoggerFactory.getLogger(LocalBertVectorService.class);

    private final VectorProperties vectorProperties;
    private final Map<String, ModelInfo> availableModels = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private volatile ServiceHealth currentHealth = ServiceHealth.UNKNOWN;
    private volatile boolean modelLoaded = false;

    // 模拟的模型接口（实际使用时需要集成真实的sentence-transformers或其他本地模型）
    private BertModelInterface bertModel;

    @Autowired
    public LocalBertVectorService(VectorProperties vectorProperties) {
        this.vectorProperties = vectorProperties;
    }

    @PostConstruct
    public void initialize() {
        try {
            logger.info("初始化本地BERT向量服务");

            // 检查模型路径
            String modelPath = vectorProperties.getLocalModels().getBert().getModelPath();
            File modelDir = new File(modelPath);

            if (!modelDir.exists()) {
                logger.warn("本地BERT模型路径不存在: {}", modelPath);
                currentHealth = ServiceHealth.UNHEALTHY;
                return;
            }

            // 初始化可用模型
            initializeAvailableModels();

            // 模拟模型加载（实际实现中需要加载真实模型）
            this.bertModel = new MockBertModelInterface();

            currentHealth = ServiceHealth.HEALTHY;
            modelLoaded = true;
            logger.info("本地BERT向量服务初始化成功");

        } catch (Exception e) {
            logger.error("本地BERT向量服务初始化失败", e);
            currentHealth = ServiceHealth.UNHEALTHY;
        }
    }

    private void initializeAvailableModels() {
        // 注册常用的sentence-transformers模型
        availableModels.put("all-MiniLM-L6-v2", new ModelInfo("all-MiniLM-L6-v2", 384, 256));
        availableModels.put("all-mpnet-base-v2", new ModelInfo("all-mpnet-base-v2", 768, 384));
        availableModels.put("paraphrase-multilingual-MiniLM-L12-v2", new ModelInfo("paraphrase-multilingual-MiniLM-L12-v2", 384, 128));
        availableModels.put("distilbert-base-multilingual-cased", new ModelInfo("distilbert-base-multilingual-cased", 768, 512));

        // 默认模型
        String defaultModel = "all-MiniLM-L6-v2";
        if (!availableModels.containsKey(defaultModel)) {
            availableModels.put(defaultModel, new ModelInfo(defaultModel, 384, 256));
        }
    }

    @Override
    public CompletableFuture<Vector> generateEmbedding(String text, String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            validateInput(text, modelName);

            if (!modelLoaded) {
                throw VectorProcessingException.modelUnavailable("本地BERT模型未加载");
            }

            Instant startTime = Instant.now();
            long requestId = requestCounter.incrementAndGet();

            try {
                logger.debug("开始本地向量化请求 {} - 模型: {}, 文本长度: {}", requestId, modelName, text.length());

                // 文本预处理
                String processedText = preprocessText(text);

                // 调用模型生成向量
                float[] vectorData = bertModel.encode(processedText, modelName);

                long processingTime = Duration.between(startTime, Instant.now()).toMillis();

                VectorMetadata metadata = VectorMetadata.create(text);

                Vector vector = new Vector(
                        vectorData,
                        vectorData.length,
                        modelName,
                        "1.0",
                        ProcessingMode.OFFLINE_BATCH, // 本地模型默认批量模式
                        processingTime,
                        Instant.now(),
                        metadata
                );

                logger.debug("本地向量化请求 {} 完成 - 处理时间: {}ms, 维度: {}",
                        requestId, processingTime, vectorData.length);

                return vector;

            } catch (Exception e) {
                long processingTime = Duration.between(startTime, Instant.now()).toMillis();
                logger.error("本地向量化请求 {} 失败 - 处理时间: {}ms", requestId, processingTime, e);
                throw VectorProcessingException.internalError("本地BERT模型处理失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Vector> generateEmbedding(ProcessingContext context) {
        String modelName = context.hasPreferredModel() ?
                context.getPreferredModel() : getDefaultModel();

        return generateEmbedding(context.getSourceText(), modelName)
                .thenApply(vector -> {
                    // 创建包含上下文信息的新元数据
                    VectorMetadata contextMetadata = VectorMetadata.createChunk(
                            context.getSourceText(),
                            context.getProperty("chunkIndex", Integer.class),
                            context.getProperty("totalChunks", Integer.class),
                            context.getDocumentId()
                    );

                    return new Vector(
                            vector.getData(),
                            vector.getDimension(),
                            vector.getModelName(),
                            vector.getModelVersion(),
                            context.getRequestedMode(),
                            vector.getProcessingTimeMs(),
                            vector.getCreatedAt(),
                            contextMetadata
                    );
                });
    }

    @Override
    public CompletableFuture<List<Vector>> generateEmbeddings(List<String> texts, String modelName) {
        if (texts == null || texts.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            validateInput(texts.get(0), modelName); // 验证第一个文本

            if (!modelLoaded) {
                throw VectorProcessingException.modelUnavailable("本地BERT模型未加载");
            }

            Instant startTime = Instant.now();
            long requestId = requestCounter.incrementAndGet();

            try {
                logger.debug("开始本地批量向量化请求 {} - 模型: {}, 批量大小: {}", requestId, modelName, texts.size());

                // 批量处理配置
                int batchSize = vectorProperties.getLocalModels().getBert().getBatchSize();
                List<Vector> allVectors = new ArrayList<>();

                // 分批处理
                for (int i = 0; i < texts.size(); i += batchSize) {
                    int endIndex = Math.min(i + batchSize, texts.size());
                    List<String> batch = texts.subList(i, endIndex);

                    // 预处理文本批次
                    List<String> processedTexts = batch.stream()
                            .map(this::preprocessText)
                            .collect(Collectors.toList());

                    // 批量编码
                    float[][] batchVectors = bertModel.encodeBatch(processedTexts, modelName);

                    // 创建Vector对象
                    for (int j = 0; j < batch.size(); j++) {
                        VectorMetadata metadata = VectorMetadata.create(batch.get(j));
                        Vector vector = new Vector(
                                batchVectors[j],
                                batchVectors[j].length,
                                modelName,
                                "1.0",
                                ProcessingMode.OFFLINE_BATCH,
                                0L, // 批量处理时间单独计算
                                Instant.now(),
                                metadata
                        );
                        allVectors.add(vector);
                    }
                }

                long totalProcessingTime = Duration.between(startTime, Instant.now()).toMillis();
                long avgProcessingTime = totalProcessingTime / texts.size();

                // 更新每个向量的处理时间
                allVectors = allVectors.stream()
                        .map(vector -> new Vector(
                                vector.getData(),
                                vector.getDimension(),
                                vector.getModelName(),
                                vector.getModelVersion(),
                                vector.getProcessingMode(),
                                avgProcessingTime,
                                vector.getCreatedAt(),
                                vector.getMetadata()
                        ))
                        .collect(Collectors.toList());

                logger.debug("本地批量向量化请求 {} 完成 - 总处理时间: {}ms, 平均时间: {}ms",
                        requestId, totalProcessingTime, avgProcessingTime);

                return allVectors;

            } catch (Exception e) {
                long processingTime = Duration.between(startTime, Instant.now()).toMillis();
                logger.error("本地批量向量化请求 {} 失败 - 处理时间: {}ms", requestId, processingTime, e);
                throw VectorProcessingException.internalError("本地BERT批量处理失败: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<Vector>> generateEmbeddings(List<ProcessingContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // 按模型分组处理
        Map<String, List<ProcessingContext>> contextsByModel = contexts.stream()
                .collect(Collectors.groupingBy(context ->
                        context.hasPreferredModel() ? context.getPreferredModel() : getDefaultModel()));

        List<CompletableFuture<List<Vector>>> modelFutures = contextsByModel.entrySet().stream()
                .map(entry -> {
                    String modelName = entry.getKey();
                    List<String> texts = entry.getValue().stream()
                            .map(ProcessingContext::getSourceText)
                            .collect(Collectors.toList());

                    return generateEmbeddings(texts, modelName)
                            .thenApply(vectors -> {
                                // 重新关联向量和上下文
                                for (int i = 0; i < vectors.size(); i++) {
                                    Vector vector = vectors.get(i);
                                    ProcessingContext context = entry.getValue().get(i);

                                    VectorMetadata contextMetadata = VectorMetadata.createChunk(
                                            context.getSourceText(),
                                            context.getProperty("chunkIndex", Integer.class),
                                            context.getProperty("totalChunks", Integer.class),
                                            context.getDocumentId()
                                    );

                                    vectors.set(i, new Vector(
                                            vector.getData(),
                                            vector.getDimension(),
                                            vector.getModelName(),
                                            vector.getModelVersion(),
                                            context.getRequestedMode(),
                                            vector.getProcessingTimeMs(),
                                            vector.getCreatedAt(),
                                            contextMetadata
                                    ));
                                }
                                return vectors;
                            });
                })
                .collect(Collectors.toList());

        return CompletableFuture.allOf(modelFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> modelFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));
    }

    @Override
    public String getServiceName() {
        return "Local BERT Vector Service";
    }

    @Override
    public String getServiceType() {
        return "local-bert";
    }

    @Override
    public List<String> getSupportedModels() {
        return new ArrayList<>(availableModels.keySet());
    }

    @Override
    public String getDefaultModel() {
        return "all-MiniLM-L6-v2";
    }

    @Override
    public boolean supportsModel(String modelName) {
        return availableModels.containsKey(modelName);
    }

    @Override
    public int getModelDimension(String modelName) {
        ModelInfo info = availableModels.get(modelName);
        return info != null ? info.dimension : -1;
    }

    @Override
    public int getMaxInputLength(String modelName) {
        ModelInfo info = availableModels.get(modelName);
        return info != null ? info.maxTokens * 4 : vectorProperties.getLocalModels().getBert().getMaxSequenceLength() * 4;
    }

    @Override
    public CompletableFuture<ServiceHealth> checkHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!modelLoaded || bertModel == null) {
                    currentHealth = ServiceHealth.UNHEALTHY;
                    return currentHealth;
                }

                // 执行一个简单的测试
                String testText = "Health check";
                float[] result = bertModel.encode(testText, getDefaultModel());

                if (result != null && result.length > 0) {
                    currentHealth = ServiceHealth.HEALTHY;
                } else {
                    currentHealth = ServiceHealth.DEGRADED;
                }

            } catch (Exception e) {
                logger.warn("本地BERT健康检查失败", e);
                currentHealth = ServiceHealth.UNHEALTHY;
            }

            return currentHealth;
        });
    }

    @Override
    public CompletableFuture<Void> warmup() {
        return CompletableFuture.runAsync(() -> {
            logger.info("本地BERT向量服务预热开始");
            try {
                if (bertModel != null) {
                    // 执行预热请求
                    String warmupText = "Warmup request";
                    bertModel.encode(warmupText, getDefaultModel());
                    logger.info("本地BERT向量服务预热完成");
                }
            } catch (Exception e) {
                logger.warn("本地BERT向量服务预热失败", e);
            }
        });
    }

    @Override
    public int estimateCost(String text, String modelName) {
        // 本地模型无外部成本，返回0
        return 0;
    }

    @Override
    public long estimateProcessingTime(String text, String modelName) {
        // 基于文本长度和模型复杂度估算处理时间
        int textLength = text.length();
        ModelInfo info = availableModels.get(modelName);
        int baseTime = info != null && info.dimension > 500 ? 200 : 100; // 复杂模型更慢
        return baseTime + (textLength / 100) * 10; // 每100字符增加10ms
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("本地BERT向量服务关闭");
            modelLoaded = false;
            if (bertModel != null) {
                bertModel.close();
                bertModel = null;
            }
            currentHealth = ServiceHealth.UNHEALTHY;
        });
    }

    @PreDestroy
    public void destroy() {
        shutdown().join();
    }

    private void validateInput(String text, String modelName) {
        if (text == null || text.trim().isEmpty()) {
            throw VectorProcessingException.invalidInput("输入文本不能为空");
        }

        if (!supportsModel(modelName)) {
            throw VectorProcessingException.invalidInput("不支持的模型: " + modelName);
        }

        if (text.length() > getMaxInputLength(modelName)) {
            throw VectorProcessingException.invalidInput(
                    String.format("输入文本过长，最大长度: %d, 当前长度: %d",
                            getMaxInputLength(modelName), text.length()));
        }
    }

    private String preprocessText(String text) {
        if (text == null) return "";

        // 基本文本预处理
        return text.trim()
                .replaceAll("\\s+", " ") // 多个空白字符替换为单个空格
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", ""); // 移除控制字符
    }

    /**
     * 模型信息内部类
     */
    private static class ModelInfo {
        final String name;
        final int dimension;
        final int maxTokens;

        ModelInfo(String name, int dimension, int maxTokens) {
            this.name = name;
            this.dimension = dimension;
            this.maxTokens = maxTokens;
        }
    }

    /**
     * BERT模型接口（模拟实现）
     * 实际使用时需要替换为真实的模型集成
     */
    private interface BertModelInterface {
        float[] encode(String text, String modelName);
        float[][] encodeBatch(List<String> texts, String modelName);
        void close();
    }

    /**
     * 模拟的BERT模型实现
     * 实际部署时需要替换为真实的sentence-transformers或其他本地模型
     */
    private class MockBertModelInterface implements BertModelInterface {

        @Override
        public float[] encode(String text, String modelName) {
            // 模拟向量生成
            ModelInfo info = availableModels.get(modelName);
            int dimension = info != null ? info.dimension : 384;

            float[] vector = new float[dimension];
            Random random = new Random(text.hashCode()); // 使用文本hash作为种子保证一致性

            // 生成随机向量并归一化
            double norm = 0.0;
            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) (random.nextGaussian());
                norm += vector[i] * vector[i];
            }

            norm = Math.sqrt(norm);
            for (int i = 0; i < dimension; i++) {
                vector[i] = (float) (vector[i] / norm);
            }

            // 模拟处理延迟
            try {
                Thread.sleep(50 + random.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return vector;
        }

        @Override
        public float[][] encodeBatch(List<String> texts, String modelName) {
            float[][] vectors = new float[texts.size()][];
            for (int i = 0; i < texts.size(); i++) {
                vectors[i] = encode(texts.get(i), modelName);
            }
            return vectors;
        }

        @Override
        public void close() {
            // 模拟资源清理
            logger.debug("模拟BERT模型资源清理");
        }
    }
}