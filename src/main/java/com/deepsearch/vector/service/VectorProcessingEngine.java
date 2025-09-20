package com.deepsearch.vector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 向量处理引擎 - 负责文本向量化和相似度计算
 */
@Service
public class VectorProcessingEngine {

    private static final Logger logger = LoggerFactory.getLogger(VectorProcessingEngine.class);

    @Value("${vector.processing.model:text-embedding-ada-002}")
    private String embeddingModel;

    @Value("${vector.processing.api.url:https://api.openai.com/v1/embeddings}")
    private String apiUrl;

    @Value("${vector.processing.api.key:}")
    private String apiKey;

    @Value("${vector.processing.enabled:false}")
    private boolean vectorProcessingEnabled;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public VectorProcessingEngine() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 生成文本嵌入向量
     */
    public List<Float> generateEmbedding(String text) {
        if (!vectorProcessingEnabled || apiKey.isEmpty()) {
            logger.warn("向量处理未启用或API密钥未配置，返回空向量");
            return generateDefaultVector();
        }

        try {
            return callEmbeddingAPI(text);
        } catch (Exception e) {
            logger.error("生成向量嵌入失败: {}", e.getMessage(), e);
            return generateDefaultVector();
        }
    }

    /**
     * 批量生成文本嵌入向量
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        List<List<Float>> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        return embeddings;
    }

    /**
     * 计算向量相似度（余弦相似度）
     */
    public double calculateSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        double magnitude = Math.sqrt(norm1) * Math.sqrt(norm2);
        return magnitude == 0 ? 0.0 : dotProduct / magnitude;
    }

    private List<Float> callEmbeddingAPI(String text) throws IOException {
        Map<String, Object> requestBody = Map.of(
            "model", embeddingModel,
            "input", text
        );

        RequestBody body = RequestBody.create(
            objectMapper.writeValueAsString(requestBody),
            MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode embeddingArray = jsonNode.get("data").get(0).get("embedding");

            List<Float> embedding = new ArrayList<>();
            for (JsonNode value : embeddingArray) {
                embedding.add(value.floatValue());
            }

            return embedding;
        }
    }

    /**
     * 生成默认向量（当API不可用时）
     */
    private List<Float> generateDefaultVector() {
        List<Float> defaultVector = new ArrayList<>();
        // 生成1536维的零向量（OpenAI text-embedding-ada-002的维度）
        for (int i = 0; i < 1536; i++) {
            defaultVector.add(0.0f);
        }
        return defaultVector;
    }

    /**
     * 检查向量处理是否可用
     */
    public boolean isVectorProcessingAvailable() {
        return vectorProcessingEnabled && !apiKey.isEmpty();
    }
}