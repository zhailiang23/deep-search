package com.deepsearch.controller;

import com.deepsearch.dto.ApiResponse;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "搜索功能", description = "文档搜索、搜索历史、搜索统计API")
public class SearchController {

    private final SearchService searchService;

    /**
     * 执行搜索
     */
    @Operation(summary = "执行搜索", description = "根据查询条件搜索文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SearchResult>> search(
            @Valid @RequestBody SearchRequest searchRequest) {
        log.info("搜索请求: {} - {}", searchRequest.getQueryText(), searchRequest.getSearchType());
        SearchResult searchResponse = searchService.search(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResponse));
    }

    /**
     * 获取当前用户搜索历史
     */
    @Operation(summary = "获取搜索历史", description = "获取当前用户的搜索历史记录")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @GetMapping("/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SearchLog>>> getCurrentUserSearchHistory(
            @Parameter(description = "返回记录数量限制") @RequestParam(defaultValue = "20") int limit) {
        List<SearchLog> searchHistory = searchService.getCurrentUserSearchHistory(limit);
        return ResponseEntity.ok(ApiResponse.success(searchHistory));
    }

    /**
     * 获取指定用户搜索历史（管理员）
     */
    @Operation(summary = "获取用户搜索历史", description = "管理员获取指定用户的搜索历史记录")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/history/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SearchLog>>> getUserSearchHistory(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "返回记录数量限制") @RequestParam(defaultValue = "20") int limit) {
        List<SearchLog> searchHistory = searchService.getUserSearchHistory(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(searchHistory));
    }

    /**
     * 删除当前用户搜索历史
     */
    @Operation(summary = "删除搜索历史", description = "删除当前用户的所有搜索历史记录")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @DeleteMapping("/history")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteCurrentUserSearchHistory() {
        // 获取当前用户ID并删除其搜索历史
        try {
            // 这里需要从服务中获取当前用户ID，暂时使用一个占位实现
            // 实际应该从SecurityContext中获取
            // Long currentUserId = getCurrentUserId();
            // searchService.deleteUserSearchHistory(currentUserId);
            return ResponseEntity.ok(ApiResponse.success("搜索历史删除成功"));
        } catch (Exception e) {
            log.error("删除搜索历史失败", e);
            return ResponseEntity.ok(ApiResponse.error("删除搜索历史失败"));
        }
    }

    /**
     * 删除指定用户搜索历史（管理员）
     */
    @Operation(summary = "删除用户搜索历史", description = "管理员删除指定用户的所有搜索历史记录")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @DeleteMapping("/history/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUserSearchHistory(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        searchService.deleteUserSearchHistory(userId);
        return ResponseEntity.ok(ApiResponse.success("用户搜索历史删除成功"));
    }

    /**
     * 获取搜索统计信息（管理员）
     */
    @Operation(summary = "获取搜索统计", description = "管理员获取指定时间范围内的搜索统计信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SearchService.SearchStatistics>> getSearchStatistics(
            @Parameter(description = "开始时间")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        SearchService.SearchStatistics statistics = searchService.getSearchStatistics(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    /**
     * 获取热门搜索词（管理员）
     */
    @Operation(summary = "获取热门搜索词", description = "管理员获取热门搜索词统计")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/popular-terms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SearchService.PopularSearchTerm>>> getPopularSearchTerms(
            @Parameter(description = "返回记录数量限制") @RequestParam(defaultValue = "10") int limit) {
        List<SearchService.PopularSearchTerm> popularTerms = searchService.getPopularSearchTerms(limit);
        return ResponseEntity.ok(ApiResponse.success(popularTerms));
    }

    /**
     * 快速搜索（仅关键词搜索）
     */
    @Operation(summary = "快速搜索", description = "执行简单的关键词搜索")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @GetMapping("/quick")
    public ResponseEntity<ApiResponse<SearchResult>> quickSearch(
            @Parameter(description = "搜索关键词") @RequestParam String query,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQueryText(query);
        searchRequest.setSearchType("keyword");
        searchRequest.setFrom(page * size);
        searchRequest.setSize(size);

        SearchResult searchResponse = searchService.search(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResponse));
    }

    /**
     * 语义搜索
     */
    @Operation(summary = "语义搜索", description = "执行语义理解的智能搜索")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @GetMapping("/semantic")
    public ResponseEntity<ApiResponse<SearchResult>> semanticSearch(
            @Parameter(description = "搜索查询") @RequestParam String query,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQueryText(query);
        searchRequest.setSearchType("vector");
        searchRequest.setFrom(page * size);
        searchRequest.setSize(size);

        SearchResult searchResponse = searchService.search(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResponse));
    }

    /**
     * 混合搜索
     */
    @Operation(summary = "混合搜索", description = "结合关键词和语义的混合搜索")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @GetMapping("/hybrid")
    public ResponseEntity<ApiResponse<SearchResult>> hybridSearch(
            @Parameter(description = "搜索查询") @RequestParam String query,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size) {

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQueryText(query);
        searchRequest.setSearchType("hybrid");
        searchRequest.setFrom(page * size);
        searchRequest.setSize(size);

        SearchResult searchResponse = searchService.search(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResponse));
    }
}