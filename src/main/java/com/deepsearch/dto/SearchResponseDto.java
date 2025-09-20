package com.deepsearch.dto;

import lombok.Data;

import java.util.List;

/**
 * 搜索响应DTO
 */
@Data
public class SearchResponseDto {

    private String queryText;
    private List<DocumentResponseDto> results;
    private Integer totalResults;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
    private Long responseTimeMs;

    public SearchResponseDto(String queryText, List<DocumentResponseDto> results,
                           Integer totalResults, Integer pageNumber, Integer pageSize,
                           Long responseTimeMs) {
        this.queryText = queryText;
        this.results = results;
        this.totalResults = totalResults;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = (int) Math.ceil((double) totalResults / pageSize);
        this.responseTimeMs = responseTimeMs;
    }
}