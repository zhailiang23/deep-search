package com.deepsearch.repository;

import com.deepsearch.entity.UserSearchPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户搜索偏好数据访问层
 */
@Repository
public interface UserSearchPreferenceRepository extends JpaRepository<UserSearchPreference, Long> {

    /**
     * 根据用户ID查找搜索偏好
     */
    Optional<UserSearchPreference> findByUserId(Long userId);

    /**
     * 检查用户是否存在搜索偏好记录
     */
    boolean existsByUserId(Long userId);

    /**
     * 查找活跃用户（最近指定天数内有搜索活动）
     */
    @Query("SELECT p FROM UserSearchPreference p WHERE p.lastSearchTime >= :since")
    List<UserSearchPreference> findActiveUsers(@Param("since") LocalDateTime since);

    /**
     * 查找新用户（搜索次数少于指定次数）
     */
    @Query("SELECT p FROM UserSearchPreference p WHERE p.totalSearches < :maxSearches")
    List<UserSearchPreference> findNewUsers(@Param("maxSearches") Long maxSearches);

    /**
     * 查找高活跃度用户（搜索次数大于指定次数）
     */
    @Query("SELECT p FROM UserSearchPreference p WHERE p.totalSearches >= :minSearches ORDER BY p.totalSearches DESC")
    List<UserSearchPreference> findHighActivityUsers(@Param("minSearches") Long minSearches);

    /**
     * 查找启用个性化的用户
     */
    @Query("SELECT p FROM UserSearchPreference p WHERE p.personalizationEnabled = true")
    List<UserSearchPreference> findUsersWithPersonalizationEnabled();

    /**
     * 统计不同偏好类型的用户数量
     */
    @Query("SELECT " +
           "SUM(CASE WHEN p.semanticPreferenceStrength > 0.7 THEN 1 ELSE 0 END) as semanticOriented, " +
           "SUM(CASE WHEN p.keywordPreferenceStrength > 0.7 THEN 1 ELSE 0 END) as keywordOriented, " +
           "SUM(CASE WHEN p.semanticPreferenceStrength <= 0.7 AND p.keywordPreferenceStrength <= 0.7 THEN 1 ELSE 0 END) as balanced " +
           "FROM UserSearchPreference p")
    Object[] countUsersByPreferenceType();

    /**
     * 计算平均个性化强度
     */
    @Query("SELECT AVG(p.personalizationStrength) FROM UserSearchPreference p WHERE p.personalizationEnabled = true")
    Float calculateAveragePersonalizationStrength();

    /**
     * 查找需要更新模型的用户（最近有活动但模型很久没更新）
     */
    @Query("SELECT p FROM UserSearchPreference p WHERE " +
           "p.lastSearchTime >= :recentActivity AND " +
           "p.updatedAt <= :modelUpdateThreshold")
    List<UserSearchPreference> findUsersNeedingModelUpdate(
        @Param("recentActivity") LocalDateTime recentActivity,
        @Param("modelUpdateThreshold") LocalDateTime modelUpdateThreshold
    );

    /**
     * 批量删除非活跃用户的偏好数据
     */
    @Query("DELETE FROM UserSearchPreference p WHERE " +
           "p.lastSearchTime < :inactiveThreshold OR " +
           "(p.lastSearchTime IS NULL AND p.createdAt < :inactiveThreshold)")
    void deleteInactiveUserPreferences(@Param("inactiveThreshold") LocalDateTime inactiveThreshold);
}