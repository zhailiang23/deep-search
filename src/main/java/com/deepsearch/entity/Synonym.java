package com.deepsearch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 同义词实体类
 * 用于存储银行业务领域的同义词映射关系
 */
@Entity
@Table(name = "synonyms",
       indexes = {
           @Index(name = "idx_term", columnList = "term"),
           @Index(name = "idx_synonym", columnList = "synonym"),
           @Index(name = "idx_source_confidence", columnList = "source, confidence")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Synonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 原始词项
     */
    @Column(name = "term", nullable = false, length = 100)
    private String term;

    /**
     * 同义词
     */
    @Column(name = "synonym", nullable = false, length = 100)
    private String synonym;

    /**
     * 同义词置信度，范围0.0-1.0
     */
    @Column(name = "confidence", nullable = false)
    private Float confidence = 1.0f;

    /**
     * 同义词来源类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private SynonymSource source = SynonymSource.MANUAL;

    /**
     * 词项类别，用于银行业务分类
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 是否启用
     */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * 使用频次统计
     */
    @Column(name = "usage_count", nullable = false)
    private Long usageCount = 0L;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 更新者ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 备注信息
     */
    @Column(name = "remarks", length = 500)
    private String remarks;

    /**
     * 同义词来源枚举
     */
    public enum SynonymSource {
        /**
         * 手动添加
         */
        MANUAL,

        /**
         * 自动发现
         */
        AUTO,

        /**
         * 机器学习生成
         */
        ML,

        /**
         * 外部词典导入
         */
        EXTERNAL
    }

    /**
     * 构造方法 - 用于创建基本同义词映射
     */
    public Synonym(String term, String synonym, Float confidence, SynonymSource source) {
        this.term = term;
        this.synonym = synonym;
        this.confidence = confidence;
        this.source = source;
        this.enabled = true;
        this.usageCount = 0L;
    }

    /**
     * 构造方法 - 用于创建带分类的同义词映射
     */
    public Synonym(String term, String synonym, Float confidence, SynonymSource source, String category) {
        this(term, synonym, confidence, source);
        this.category = category;
    }

    /**
     * 增加使用次数
     */
    public void incrementUsageCount() {
        this.usageCount++;
    }

    /**
     * 是否为高置信度同义词
     */
    public boolean isHighConfidence() {
        return this.confidence != null && this.confidence >= 0.8f;
    }

    /**
     * 是否为银行产品类同义词
     */
    public boolean isBankProductSynonym() {
        return "BANK_PRODUCT".equals(this.category);
    }

    /**
     * 是否为银行服务类同义词
     */
    public boolean isBankServiceSynonym() {
        return "BANK_SERVICE".equals(this.category);
    }
}