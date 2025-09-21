package com.deepsearch.vector.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 向量元数据
 * 包含向量处理相关的附加信息
 *
 * @author DeepSearch Vector Team
 */
@JsonSerialize
@JsonDeserialize
public class VectorMetadata {

    @JsonProperty("sourceText")
    private final String sourceText;

    @JsonProperty("textLength")
    private final int textLength;

    @JsonProperty("chunkIndex")
    private final Integer chunkIndex;

    @JsonProperty("totalChunks")
    private final Integer totalChunks;

    @JsonProperty("documentId")
    private final String documentId;

    @JsonProperty("costCents")
    private final Integer costCents;

    @JsonProperty("customProperties")
    private final Map<String, Object> customProperties;

    @JsonCreator
    public VectorMetadata(
            @JsonProperty("sourceText") String sourceText,
            @JsonProperty("textLength") int textLength,
            @JsonProperty("chunkIndex") Integer chunkIndex,
            @JsonProperty("totalChunks") Integer totalChunks,
            @JsonProperty("documentId") String documentId,
            @JsonProperty("costCents") Integer costCents,
            @JsonProperty("customProperties") Map<String, Object> customProperties) {

        this.sourceText = sourceText;
        this.textLength = textLength;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.documentId = documentId;
        this.costCents = costCents;
        this.customProperties = customProperties != null ? new HashMap<>(customProperties) : new HashMap<>();
    }

    /**
     * 创建基础元数据的便捷构造方法
     */
    public static VectorMetadata create(String sourceText) {
        return new VectorMetadata(
                sourceText,
                sourceText != null ? sourceText.length() : 0,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * 创建带有分块信息的元数据
     */
    public static VectorMetadata createChunk(String sourceText, int chunkIndex, int totalChunks, String documentId) {
        return new VectorMetadata(
                sourceText,
                sourceText != null ? sourceText.length() : 0,
                chunkIndex,
                totalChunks,
                documentId,
                null,
                null
        );
    }

    /**
     * 创建带有成本信息的元数据
     */
    public static VectorMetadata createWithCost(String sourceText, int costCents) {
        return new VectorMetadata(
                sourceText,
                sourceText != null ? sourceText.length() : 0,
                null,
                null,
                null,
                costCents,
                null
        );
    }

    public String getSourceText() {
        return sourceText;
    }

    public int getTextLength() {
        return textLength;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public String getDocumentId() {
        return documentId;
    }

    public Integer getCostCents() {
        return costCents;
    }

    public Map<String, Object> getCustomProperties() {
        return new HashMap<>(customProperties);
    }

    /**
     * 获取自定义属性值
     */
    public Object getCustomProperty(String key) {
        return customProperties.get(key);
    }

    /**
     * 获取自定义属性值（泛型版本）
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomProperty(String key, Class<T> type) {
        Object value = customProperties.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 检查是否为分块数据
     */
    public boolean isChunk() {
        return chunkIndex != null && totalChunks != null;
    }

    /**
     * 检查是否为最后一个分块
     */
    public boolean isLastChunk() {
        return isChunk() && chunkIndex.equals(totalChunks - 1);
    }

    /**
     * 创建包含额外自定义属性的新实例
     */
    public VectorMetadata withCustomProperty(String key, Object value) {
        Map<String, Object> newCustomProperties = new HashMap<>(this.customProperties);
        newCustomProperties.put(key, value);

        return new VectorMetadata(
                sourceText,
                textLength,
                chunkIndex,
                totalChunks,
                documentId,
                costCents,
                newCustomProperties
        );
    }

    /**
     * 创建包含成本信息的新实例
     */
    public VectorMetadata withCost(int costCents) {
        return new VectorMetadata(
                sourceText,
                textLength,
                chunkIndex,
                totalChunks,
                documentId,
                costCents,
                customProperties
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorMetadata that = (VectorMetadata) o;
        return textLength == that.textLength &&
                Objects.equals(sourceText, that.sourceText) &&
                Objects.equals(chunkIndex, that.chunkIndex) &&
                Objects.equals(totalChunks, that.totalChunks) &&
                Objects.equals(documentId, that.documentId) &&
                Objects.equals(costCents, that.costCents) &&
                Objects.equals(customProperties, that.customProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceText, textLength, chunkIndex, totalChunks, documentId, costCents, customProperties);
    }

    @Override
    public String toString() {
        return "VectorMetadata{" +
                "textLength=" + textLength +
                ", chunkIndex=" + chunkIndex +
                ", totalChunks=" + totalChunks +
                ", documentId='" + documentId + '\'' +
                ", costCents=" + costCents +
                ", customProperties=" + customProperties.size() +
                '}';
    }
}