package com.deepsearch.dto;

import com.deepsearch.entity.SearchLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 搜索请求DTO
 */
@Data
public class SearchRequestDto {

    @NotBlank(message = "搜索查询不能为空")
    @Size(max = 1000, message = "搜索查询长度不能超过1000个字符")
    private String queryText;

    @NotNull(message = "搜索类型不能为空")
    private SearchLog.SearchType searchType;

    private Integer pageNumber = 0;
    private Integer pageSize = 10;
}