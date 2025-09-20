package com.deepsearch.repository;

import com.deepsearch.entity.Synonym;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 同义词数据访问接口
 * 提供同义词数据的CRUD操作和查询功能
 */
@Repository
public interface SynonymRepository extends JpaRepository<Synonym, Long> {

    /**
     * 根据原始词项查找所有同义词（启用状态）
     */
    @Query("SELECT s FROM Synonym s WHERE s.term = :term AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findByTermAndEnabled(@Param("term") String term);

    /**
     * 根据同义词查找所有原始词项（启用状态）
     */
    @Query("SELECT s FROM Synonym s WHERE s.synonym = :synonym AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findBySynonymAndEnabled(@Param("synonym") String synonym);

    /**
     * 双向查找：查找词项的所有同义词关系（包括原始词项和同义词）
     */
    @Query("SELECT s FROM Synonym s WHERE (s.term = :word OR s.synonym = :word) AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findByWordBidirectional(@Param("word") String word);

    /**
     * 根据词项和同义词查找精确匹配
     */
    Optional<Synonym> findByTermAndSynonym(String term, String synonym);

    /**
     * 根据分类查找同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.category = :category AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findByCategoryAndEnabled(@Param("category") String category);

    /**
     * 根据置信度范围查找同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.confidence >= :minConfidence AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findByConfidenceGreaterThanEqualAndEnabled(@Param("minConfidence") Float minConfidence);

    /**
     * 根据来源类型查找同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.source = :source AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findBySourceAndEnabled(@Param("source") Synonym.SynonymSource source);

    /**
     * 查找高频使用的同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.usageCount >= :minUsage AND s.enabled = true ORDER BY s.usageCount DESC, s.confidence DESC")
    List<Synonym> findByHighUsage(@Param("minUsage") Long minUsage);

    /**
     * 模糊搜索词项或同义词
     */
    @Query("SELECT s FROM Synonym s WHERE (s.term LIKE %:keyword% OR s.synonym LIKE %:keyword%) AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findByKeywordLike(@Param("keyword") String keyword);

    /**
     * 查找需要审核的低置信度同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.confidence < :threshold AND s.source != 'MANUAL' AND s.enabled = true ORDER BY s.confidence ASC")
    List<Synonym> findLowConfidenceSynonyms(@Param("threshold") Float threshold);

    /**
     * 根据银行产品类别查找同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.category IN ('BANK_PRODUCT', 'LOAN', 'DEPOSIT', 'CARD') AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findBankProductSynonyms();

    /**
     * 根据银行服务类别查找同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.category IN ('BANK_SERVICE', 'ONLINE_BANKING', 'MOBILE_BANKING', 'COUNTER_SERVICE') AND s.enabled = true ORDER BY s.confidence DESC")
    List<Synonym> findBankServiceSynonyms();

    /**
     * 获取热门同义词（按使用频次排序）
     */
    @Query("SELECT s FROM Synonym s WHERE s.enabled = true ORDER BY s.usageCount DESC")
    List<Synonym> findPopularSynonyms();

    /**
     * 增加同义词使用次数
     */
    @Modifying
    @Query("UPDATE Synonym s SET s.usageCount = s.usageCount + 1 WHERE s.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    /**
     * 批量增加同义词使用次数
     */
    @Modifying
    @Query("UPDATE Synonym s SET s.usageCount = s.usageCount + 1 WHERE s.id IN :ids")
    void batchIncrementUsageCount(@Param("ids") List<Long> ids);

    /**
     * 启用/禁用同义词
     */
    @Modifying
    @Query("UPDATE Synonym s SET s.enabled = :enabled WHERE s.id = :id")
    void updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    /**
     * 批量启用/禁用同义词
     */
    @Modifying
    @Query("UPDATE Synonym s SET s.enabled = :enabled WHERE s.id IN :ids")
    void batchUpdateEnabled(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);

    /**
     * 更新同义词置信度
     */
    @Modifying
    @Query("UPDATE Synonym s SET s.confidence = :confidence WHERE s.id = :id")
    void updateConfidence(@Param("id") Long id, @Param("confidence") Float confidence);

    /**
     * 根据创建者查找同义词
     */
    @Query("SELECT s FROM Synonym s WHERE s.createdBy = :createdBy AND s.enabled = true ORDER BY s.createdAt DESC")
    List<Synonym> findByCreatedBy(@Param("createdBy") Long createdBy);

    /**
     * 统计同义词总数
     */
    @Query("SELECT COUNT(s) FROM Synonym s WHERE s.enabled = true")
    Long countEnabledSynonyms();

    /**
     * 根据来源统计同义词数量
     */
    @Query("SELECT s.source, COUNT(s) FROM Synonym s WHERE s.enabled = true GROUP BY s.source")
    List<Object[]> countBySource();

    /**
     * 根据分类统计同义词数量
     */
    @Query("SELECT s.category, COUNT(s) FROM Synonym s WHERE s.enabled = true GROUP BY s.category")
    List<Object[]> countByCategory();

    /**
     * 删除低置信度的自动生成同义词
     */
    @Modifying
    @Query("DELETE FROM Synonym s WHERE s.confidence < :threshold AND s.source IN ('AUTO', 'ML')")
    void deleteLowequalConfidenceAutoSynonyms(@Param("threshold") Float threshold);

    /**
     * 查找重复的同义词对
     */
    @Query("SELECT s1 FROM Synonym s1, Synonym s2 WHERE s1.id != s2.id AND s1.term = s2.term AND s1.synonym = s2.synonym")
    List<Synonym> findDuplicateSynonyms();

    /**
     * 查找循环同义词（A->B, B->A）
     */
    @Query("SELECT s1 FROM Synonym s1, Synonym s2 WHERE s1.id != s2.id AND s1.term = s2.synonym AND s1.synonym = s2.term")
    List<Synonym> findCircularSynonyms();
}