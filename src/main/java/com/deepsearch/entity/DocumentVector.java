package com.deepsearch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档向量实体类
 */
@Entity
@Table(name = "document_vectors", indexes = {
    @Index(name = "idx_document_model", columnList = "document_id, model_name"),
    @Index(name = "idx_model_dimension", columnList = "model_name, vector_dimension"),
    @Index(name = "idx_processing_mode", columnList = "processing_mode"),
    @Index(name = "idx_quality_score", columnList = "quality_score"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private Document document;

    /**
     * 向量数据，存储为JSON格式
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vector_data", nullable = false, columnDefinition = "JSON")
    private List<Double> vectorData;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "vector_dimension", nullable = false)
    private Integer vectorDimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_mode", nullable = false)
    private ProcessingMode processingMode;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "quality_score", precision = 5, scale = 3)
    private BigDecimal qualityScore;

    @Column(name = "chunk_index")
    private Integer chunkIndex = 0;

    @Column(name = "chunk_text", columnDefinition = "TEXT")
    private String chunkText;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 向量处理模式枚举
     */
    public enum ProcessingMode {
        OFFLINE, ONLINE
    }

    /**
     * 构造函数 - 创建新的文档向量
     */
    public DocumentVector(Long documentId, List<Double> vectorData, String modelName,
                         String modelVersion, ProcessingMode processingMode,
                         Integer processingTimeMs, String chunkText, Integer chunkIndex) {
        this.documentId = documentId;
        this.vectorData = vectorData;
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.vectorDimension = vectorData != null ? vectorData.size() : 0;
        this.processingMode = processingMode;
        this.processingTimeMs = processingTimeMs;
        this.chunkText = chunkText;
        this.chunkIndex = chunkIndex;
    }

    /**
     * 获取向量的L2范数
     */
    public double getVectorNorm() {
        if (vectorData == null || vectorData.isEmpty()) {
            return 0.0;
        }
        return Math.sqrt(vectorData.stream()
            .mapToDouble(Double::doubleValue)
            .map(x -> x * x)
            .sum());
    }

    /**
     * 计算与另一个向量的余弦相似度
     */
    public double calculateCosineSimilarity(DocumentVector other) {
        if (other == null || other.vectorData == null || this.vectorData == null ||
            this.vectorData.size() != other.vectorData.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorData.size(); i++) {
            double a = vectorData.get(i);
            double b = other.vectorData.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 验证向量数据的有效性
     */
    public boolean isVectorValid() {
        return vectorData != null &&
               !vectorData.isEmpty() &&
               vectorData.stream().allMatch(v -> v != null && !Double.isNaN(v) && !Double.isInfinite(v)) &&
               vectorDimension != null &&
               vectorDimension.equals(vectorData.size());
    }
}