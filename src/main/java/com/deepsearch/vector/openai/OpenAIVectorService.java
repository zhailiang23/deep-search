package com.deepsearch.vector.openai;

import com.deepsearch.vector.VectorService;
import com.deepsearch.vector.config.VectorProperties;
import com.deepsearch.vector.model.*;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OpenAI向量服务实现
 * 集成OpenAI text-embedding-3 API
 *
 * @author DeepSearch Vector Team
 */
@Service
public class OpenAIVectorService implements VectorService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIVectorService.class);

    private final VectorProperties vectorProperties;
    private OpenAiService openAiService;
    private final Map<String, Integer> modelDimensions = new ConcurrentHashMap<>();
    private final Map<String, Integer> modelMaxTokens = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong(0);
    private volatile ServiceHealth currentHealth = ServiceHealth.UNKNOWN;

    @Autowired
    public OpenAIVectorService(VectorProperties vectorProperties) {
        this.vectorProperties = vectorProperties;
    }

    @PostConstruct
    public void initialize() {
        try {
            String apiKey = vectorProperties.getOpenai().getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                logger.warn("OpenAI API密钥未配置，服务将不可用");
                currentHealth = ServiceHealth.UNHEALTHY;
                return;
            }

            Duration timeout = Duration.ofSeconds(vectorProperties.getOpenai().getTimeoutSeconds());
            this.openAiService = new OpenAiService(apiKey, timeout);

            // 初始化模型配置
            initializeModelConfigurations();

            currentHealth = ServiceHealth.HEALTHY;
            logger.info("OpenAI向量服务初始化成功");

        } catch (Exception e) {
            logger.error("OpenAI向量服务初始化失败", e);
            currentHealth = ServiceHealth.UNHEALTHY;
        }
    }

    private void initializeModelConfigurations() {
        // text-embedding-3-small: 1536维度, 8191 tokens
        modelDimensions.put(vectorProperties.getOpenai().getModels().getSmall(), 1536);
        modelMaxTokens.put(vectorProperties.getOpenai().getModels().getSmall(), 8191);

        // text-embedding-3-large: 3072维度, 8191 tokens
        modelDimensions.put(vectorProperties.getOpenai().getModels().getLarge(), 3072);
        modelMaxTokens.put(vectorProperties.getOpenai().getModels().getLarge(), 8191);
    }

    @Override
    public CompletableFuture<Vector> generateEmbedding(String text, String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            validateInput(text, modelName);

            Instant startTime = Instant.now();
            long requestId = requestCounter.incrementAndGet();

            try {
                logger.debug("开始向量化请求 {} - 模型: {}, 文本长度: {}", requestId, modelName, text.length());

                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(modelName)
                        .input(Collections.singletonList(text))
                        .build();

                EmbeddingResult result = openAiService.createEmbeddings(request);

                if (result.getData() == null || result.getData().isEmpty()) {
                    throw VectorProcessingException.internalError("OpenAI返回空结果", null);
                }

                List<Double> embedding = result.getData().get(0).getEmbedding();
                float[] vectorData = embedding.stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList())
                        .stream()
                        .mapToDouble(Float::doubleValue)
                        .collect(
                                () -> new float[embedding.size()],
                                (array, value) -> array[Arrays.asList(array).indexOf(0f)] = (float) value,
                                (array1, array2) -> array1
                        );

                // 正确转换
                vectorData = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vectorData[i] = embedding.get(i).floatValue();
                }

                long processingTime = Duration.between(startTime, Instant.now()).toMillis();
                int costCents = estimateCost(text, modelName);

                VectorMetadata metadata = VectorMetadata.createWithCost(text, costCents);

                Vector vector = new Vector(
                        vectorData,
                        vectorData.length,
                        modelName,
                        "1.0", // OpenAI模型版本
                        ProcessingMode.ONLINE_REALTIME,
                        processingTime,
                        Instant.now(),
                        metadata
                );

                logger.debug("向量化请求 {} 完成 - 处理时间: {}ms, 成本: {}分, 维度: {}",
                        requestId, processingTime, costCents, vectorData.length);

                return vector;

            } catch (Exception e) {
                long processingTime = Duration.between(startTime, Instant.now()).toMillis();
                logger.error("向量化请求 {} 失败 - 处理时间: {}ms", requestId, processingTime, e);

                if (e.getMessage().contains("rate_limit")) {
                    throw VectorProcessingException.rateLimitExceeded("OpenAI API频率限制: " + e.getMessage());
                } else if (e.getMessage().contains("timeout")) {
                    throw VectorProcessingException.timeout("OpenAI API超时: " + e.getMessage());
                } else if (e.getMessage().contains("invalid_request")) {
                    throw VectorProcessingException.invalidInput("无效输入: " + e.getMessage());
                } else if (e.getMessage().contains("unauthorized")) {
                    throw VectorProcessingException.authenticationError("认证失败: " + e.getMessage());
                } else {
                    throw VectorProcessingException.internalError("OpenAI API调用失败: " + e.getMessage(), e);
                }
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

                    if (vector.getMetadata() != null && vector.getMetadata().getCostCents() != null) {
                        contextMetadata = contextMetadata.withCost(vector.getMetadata().getCostCents());
                    }

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

        // OpenAI API支持批量请求，但需要分批处理避免超过限制
        int batchSize = 100; // OpenAI建议的批量大小
        List<CompletableFuture<List<Vector>>> batchFutures = new ArrayList<>();

        for (int i = 0; i < texts.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, endIndex);
            batchFutures.add(generateEmbeddingsBatch(batch, modelName));
        }

        return CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> batchFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(List::stream)
                        .collect(Collectors.toList()));
    }

    private CompletableFuture<List<Vector>> generateEmbeddingsBatch(List<String> texts, String modelName) {
        return CompletableFuture.supplyAsync(() -> {
            validateInput(texts.get(0), modelName); // 验证第一个文本

            Instant startTime = Instant.now();
            long requestId = requestCounter.incrementAndGet();

            try {
                logger.debug("开始批量向量化请求 {} - 模型: {}, 批量大小: {}", requestId, modelName, texts.size());

                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(modelName)
                        .input(texts)
                        .build();

                EmbeddingResult result = openAiService.createEmbeddings(request);

                if (result.getData() == null || result.getData().size() != texts.size()) {
                    throw VectorProcessingException.internalError("OpenAI返回的结果数量不匹配", null);
                }

                long processingTime = Duration.between(startTime, Instant.now()).toMillis();

                return IntStream.range(0, texts.size())
                        .mapToObj(i -> {
                            List<Double> embedding = result.getData().get(i).getEmbedding();
                            float[] vectorData = new float[embedding.size()];
                            for (int j = 0; j < embedding.size(); j++) {
                                vectorData[j] = embedding.get(j).floatValue();
                            }

                            int costCents = estimateCost(texts.get(i), modelName);
                            VectorMetadata metadata = VectorMetadata.createWithCost(texts.get(i), costCents);

                            return new Vector(
                                    vectorData,
                                    vectorData.length,
                                    modelName,
                                    "1.0",
                                    ProcessingMode.OFFLINE_BATCH,
                                    processingTime / texts.size(), // 平均处理时间
                                    Instant.now(),
                                    metadata
                            );
                        })
                        .collect(Collectors.toList());

            } catch (Exception e) {
                long processingTime = Duration.between(startTime, Instant.now()).toMillis();
                logger.error("批量向量化请求 {} 失败 - 处理时间: {}ms", requestId, processingTime, e);
                throw handleException(e);
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

                                    // 更新向量元数据以包含上下文信息
                                    VectorMetadata contextMetadata = VectorMetadata.createChunk(
                                            context.getSourceText(),
                                            context.getProperty("chunkIndex", Integer.class),
                                            context.getProperty("totalChunks", Integer.class),
                                            context.getDocumentId()
                                    );

                                    if (vector.getMetadata() != null && vector.getMetadata().getCostCents() != null) {
                                        contextMetadata = contextMetadata.withCost(vector.getMetadata().getCostCents());
                                    }

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
        return "OpenAI Vector Service";
    }

    @Override
    public String getServiceType() {
        return "openai";
    }

    @Override
    public List<String> getSupportedModels() {
        return Arrays.asList(
                vectorProperties.getOpenai().getModels().getSmall(),
                vectorProperties.getOpenai().getModels().getLarge()
        );
    }

    @Override
    public String getDefaultModel() {
        return vectorProperties.getOpenai().getModels().getSmall();
    }

    @Override
    public boolean supportsModel(String modelName) {
        return getSupportedModels().contains(modelName);
    }

    @Override
    public int getModelDimension(String modelName) {
        return modelDimensions.getOrDefault(modelName, -1);
    }

    @Override
    public int getMaxInputLength(String modelName) {
        // OpenAI的embedding模型以token计算，这里返回字符近似值
        int maxTokens = modelMaxTokens.getOrDefault(modelName, 8191);
        return maxTokens * 4; // 粗略估算：1 token ≈ 4 字符
    }

    @Override
    public CompletableFuture<ServiceHealth> checkHealth() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (openAiService == null) {
                    currentHealth = ServiceHealth.UNHEALTHY;
                    return currentHealth;
                }

                // 执行一个简单的测试请求
                String testText = "Health check";
                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(getDefaultModel())
                        .input(Collections.singletonList(testText))
                        .build();

                EmbeddingResult result = openAiService.createEmbeddings(request);

                if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                    currentHealth = ServiceHealth.HEALTHY;
                } else {
                    currentHealth = ServiceHealth.DEGRADED;
                }

            } catch (Exception e) {
                logger.warn("OpenAI健康检查失败", e);
                currentHealth = ServiceHealth.UNHEALTHY;
            }

            return currentHealth;
        });
    }

    @Override
    public CompletableFuture<Void> warmup() {
        return CompletableFuture.runAsync(() -> {
            logger.info("OpenAI向量服务预热开始");
            try {
                // 执行一个预热请求
                String warmupText = "Warmup request";
                generateEmbedding(warmupText, getDefaultModel()).get();
                logger.info("OpenAI向量服务预热完成");
            } catch (Exception e) {
                logger.warn("OpenAI向量服务预热失败", e);
            }
        });
    }

    @Override
    public int estimateCost(String text, String modelName) {
        // OpenAI text-embedding-3-small: $0.00002 per 1K tokens
        // OpenAI text-embedding-3-large: $0.00013 per 1K tokens
        int tokens = estimateTokens(text);
        double costPerThousandTokens;

        if (modelName.equals(vectorProperties.getOpenai().getModels().getSmall())) {
            costPerThousandTokens = 0.02; // $0.00002 -> 0.002分
        } else if (modelName.equals(vectorProperties.getOpenai().getModels().getLarge())) {
            costPerThousandTokens = 1.3; // $0.00013 -> 0.013分
        } else {
            costPerThousandTokens = 0.02; // 默认使用small模型成本
        }

        return (int) Math.ceil((tokens / 1000.0) * costPerThousandTokens * 100); // 转换为分
    }

    @Override
    public long estimateProcessingTime(String text, String modelName) {
        // 基于文本长度估算处理时间
        int tokens = estimateTokens(text);
        long baseTime = 500; // 基础500ms
        long additionalTime = (tokens / 100) * 50; // 每100个token增加50ms
        return baseTime + additionalTime;
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            logger.info("OpenAI向量服务关闭");
            if (openAiService != null) {
                // OpenAI Java客户端没有显式关闭方法，清理引用即可
                openAiService = null;
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

    private int estimateTokens(String text) {
        // 简单估算：英文平均1词=1.3tokens，中文1字=2tokens
        if (text == null) return 0;

        int chineseChars = (int) text.chars().filter(c -> c >= 0x4E00 && c <= 0x9FFF).count();
        int otherChars = text.length() - chineseChars;
        int words = otherChars > 0 ? otherChars / 5 : 0; // 假设平均单词长度为5

        return (int) (chineseChars * 2 + words * 1.3);
    }

    private VectorProcessingException handleException(Exception e) {
        if (e instanceof VectorProcessingException) {
            return (VectorProcessingException) e;
        }

        String message = e.getMessage();
        if (message != null) {
            if (message.contains("rate_limit")) {
                return VectorProcessingException.rateLimitExceeded("OpenAI API频率限制: " + message);
            } else if (message.contains("timeout")) {
                return VectorProcessingException.timeout("OpenAI API超时: " + message);
            } else if (message.contains("invalid_request")) {
                return VectorProcessingException.invalidInput("无效输入: " + message);
            } else if (message.contains("unauthorized")) {
                return VectorProcessingException.authenticationError("认证失败: " + message);
            }
        }

        return VectorProcessingException.internalError("OpenAI API调用失败: " + message, e);
    }
}