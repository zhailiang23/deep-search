package com.deepsearch.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.deepsearch.entity.Document;

/**
 * 文档数据访问层
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 根据用户ID查找文档
     */
    List<Document> findByUserId(Long userId);

    /**
     * 根据用户ID分页查找文档
     */
    Page<Document> findByUserId(Long userId, Pageable pageable);

    /**
     * 根据状态查找文档
     */
    List<Document> findByStatus(Document.Status status);

    /**
     * 根据用户ID和状态查找文档
     */
    List<Document> findByUserIdAndStatus(Long userId, Document.Status status);

    /**
     * 根据文件类型查找文档
     */
    List<Document> findByFileType(String fileType);

    /**
     * 根据标题模糊查找文档
     */
    List<Document> findByTitleContainingIgnoreCase(String title);

    /**
     * 根据内容模糊查找文档
     */
    List<Document> findByContentContainingIgnoreCase(String content);

    /**
     * 根据用户ID和标题模糊查找文档
     */
    List<Document> findByUserIdAndTitleContainingIgnoreCase(Long userId, String title);

    /**
     * 统计用户的文档数量
     */
    Long countByUserId(Long userId);

    /**
     * 统计指定状态的文档数量
     */
    Long countByStatus(Document.Status status);

    /**
     * 查找指定时间范围内创建的文档
     */
    @Query("SELECT d FROM Document d WHERE d.createdAt BETWEEN :startDate AND :endDate")
    List<Document> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * 查找用户最近创建的文档
     */
    @Query("SELECT d FROM Document d WHERE d.userId = :userId ORDER BY d.createdAt DESC")
    List<Document> findRecentDocumentsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 根据文件大小范围查找文档
     */
    @Query("SELECT d FROM Document d WHERE d.fileSize BETWEEN :minSize AND :maxSize")
    List<Document> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * 检查用户是否拥有指定标题的文档
     */
    Boolean existsByUserIdAndTitle(Long userId, String title);
}