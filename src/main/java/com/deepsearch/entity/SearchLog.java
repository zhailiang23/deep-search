package com.deepsearch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 搜索记录实体类
 */
@Entity
@Table(name = "search_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", nullable = false, length = 1000)
    private String queryText;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false)
    private SearchType searchType;

    @Column(name = "result_count")
    private Integer resultCount = 0;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SearchType {
        KEYWORD, SEMANTIC, HYBRID
    }
}