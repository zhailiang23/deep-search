package com.deepsearch.vector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 向量数据模型
 * 表示文档或查询的向量化结果
 */
@Data
public class Vector implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 向量数据
     */
    private final List<Double> data;

    /**
     * 向量维度
     */
    private final int dimension;

    /**
     * 生成向量的模型名称
     */
    private final String modelName;

    /**
     * 向量模长（用于归一化）
     */
    private final double magnitude;

    @JsonCreator
    public Vector(@JsonProperty("data") List<Double> data,
                  @JsonProperty("modelName") String modelName) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("向量数据不能为空");
        }
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException("模型名称不能为空");
        }

        this.data = Collections.unmodifiableList(new ArrayList<>(data)); // 不可变列表
        this.dimension = data.size();
        this.modelName = modelName;
        this.magnitude = calculateMagnitude(data);
    }

    /**
     * 计算向量模长
     */
    private double calculateMagnitude(List<Double> data) {
        double sum = 0.0;
        for (Double value : data) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    /**
     * 检查两个向量是否兼容（相同维度和模型）
     */
    public boolean isCompatibleWith(Vector other) {
        if (other == null) {
            return false;
        }
        return this.dimension == other.dimension &&
               this.modelName.equals(other.modelName);
    }

    /**
     * 计算与另一个向量的余弦相似度
     */
    public double cosineSimilarity(Vector other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("向量不兼容: 维度或模型不匹配");
        }

        double dotProduct = 0.0;
        for (int i = 0; i < dimension; i++) {
            dotProduct += this.data.get(i) * other.data.get(i);
        }

        return dotProduct / (this.magnitude * other.magnitude);
    }

    /**
     * 获取向量的哈希码，用于缓存键
     */
    public String getCacheKey(String content) {
        return String.format("%s_%s_%d",
            modelName,
            Integer.toHexString(content.hashCode()),
            dimension);
    }
}