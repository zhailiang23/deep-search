package com.deepsearch.vector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * 向量数据模型
 * 包含向量数据及相关元数据
 *
 * @author DeepSearch Vector Team
 */
@JsonSerialize
@JsonDeserialize
public class Vector {

    @JsonProperty("data")
    private final float[] data;

    @JsonProperty("dimension")
    private final int dimension;

    @JsonProperty("modelName")
    private final String modelName;

    @JsonProperty("modelVersion")
    private final String modelVersion;

    @JsonProperty("processingMode")
    private final ProcessingMode processingMode;

    @JsonProperty("processingTimeMs")
    private final long processingTimeMs;

    @JsonProperty("createdAt")
    private final Instant createdAt;

    @JsonProperty("metadata")
    private final VectorMetadata metadata;

    @JsonCreator
    public Vector(
            @JsonProperty("data") float[] data,
            @JsonProperty("dimension") int dimension,
            @JsonProperty("modelName") String modelName,
            @JsonProperty("modelVersion") String modelVersion,
            @JsonProperty("processingMode") ProcessingMode processingMode,
            @JsonProperty("processingTimeMs") long processingTimeMs,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("metadata") VectorMetadata metadata) {

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("向量数据不能为空");
        }
        if (data.length != dimension) {
            throw new IllegalArgumentException("向量维度不匹配，期望: " + dimension + ", 实际: " + data.length);
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }
        if (processingMode == null) {
            throw new IllegalArgumentException("处理模式不能为空");
        }

        this.data = Arrays.copyOf(data, data.length);
        this.dimension = dimension;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.processingMode = processingMode;
        this.processingTimeMs = processingTimeMs;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.metadata = metadata;
    }

    /**
     * 创建向量实例的便捷构造方法
     */
    public static Vector create(float[] data, String modelName, ProcessingMode processingMode) {
        return new Vector(
                data,
                data.length,
                modelName,
                null,
                processingMode,
                0L,
                Instant.now(),
                null
        );
    }

    /**
     * 创建带有处理时间的向量实例
     */
    public static Vector create(float[] data, String modelName, ProcessingMode processingMode, long processingTimeMs) {
        return new Vector(
                data,
                data.length,
                modelName,
                null,
                processingMode,
                processingTimeMs,
                Instant.now(),
                null
        );
    }

    public float[] getData() {
        return Arrays.copyOf(data, data.length);
    }

    public int getDimension() {
        return dimension;
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public ProcessingMode getProcessingMode() {
        return processingMode;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public VectorMetadata getMetadata() {
        return metadata;
    }

    /**
     * 计算向量长度（欧几里得范数）
     */
    public double magnitude() {
        double sum = 0.0;
        for (float value : data) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    /**
     * 向量归一化
     */
    public Vector normalize() {
        double magnitude = magnitude();
        if (magnitude == 0.0) {
            return this;
        }

        float[] normalizedData = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            normalizedData[i] = (float) (data[i] / magnitude);
        }

        return new Vector(
                normalizedData,
                dimension,
                modelName,
                modelVersion,
                processingMode,
                processingTimeMs,
                createdAt,
                metadata
        );
    }

    /**
     * 计算与另一个向量的余弦相似度
     */
    public double cosineSimilarity(Vector other) {
        if (other == null || this.dimension != other.dimension) {
            throw new IllegalArgumentException("向量维度不匹配或目标向量为空");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < dimension; i++) {
            dotProduct += this.data[i] * other.data[i];
            normA += this.data[i] * this.data[i];
            normB += other.data[i] * other.data[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector vector = (Vector) o;
        return dimension == vector.dimension &&
                processingTimeMs == vector.processingTimeMs &&
                Arrays.equals(data, vector.data) &&
                Objects.equals(modelName, vector.modelName) &&
                Objects.equals(modelVersion, vector.modelVersion) &&
                processingMode == vector.processingMode &&
                Objects.equals(createdAt, vector.createdAt) &&
                Objects.equals(metadata, vector.metadata);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(dimension, modelName, modelVersion, processingMode, processingTimeMs, createdAt, metadata);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "Vector{" +
                "dimension=" + dimension +
                ", modelName='" + modelName + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                ", processingMode=" + processingMode +
                ", processingTimeMs=" + processingTimeMs +
                ", createdAt=" + createdAt +
                ", magnitude=" + String.format("%.4f", magnitude()) +
                '}';
    }
}