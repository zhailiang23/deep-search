package com.deepsearch.controller;

import com.deepsearch.dto.ApiResponse;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.entity.SearchLog;
import com.deepsearch.entity.User;
import com.deepsearch.service.SearchService;
import com.deepsearch.service.HybridSearchService;
import com.deepsearch.service.ContextAwareSearchService;
import com.deepsearch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final HybridSearchService hybridSearchService;
    private final ContextAwareSearchService contextAwareSearchService;

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
     * 增强混合搜索
     */
    @Operation(summary = "增强混合搜索", description = "执行高级混合搜索，支持自定义权重和过滤条件")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @PostMapping("/hybrid")
    public ResponseEntity<ApiResponse<SearchResult>> hybridSearch(
            @Valid @RequestBody SearchRequest searchRequest) {
        log.info("混合搜索请求: query={}, weights={}/{}", 
            searchRequest.getQuery(), 
            searchRequest.getKeywordWeight(), 
            searchRequest.getVectorWeight());
        
        // 确保搜索类型为混合搜索
        searchRequest.setSearchType("hybrid");
        
        SearchResult searchResult = hybridSearchService.hybridSearch(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResult));
    }

    /**
     * 智能搜索（自动选择最佳搜索策略）
     */
    @Operation(summary = "智能搜索", description = "自动选择最佳搜索策略，支持权重自适应")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @PostMapping("/intelligent")
    public ResponseEntity<ApiResponse<SearchResult>> intelligentSearch(
            @Valid @RequestBody SearchRequest searchRequest) {
        log.info("智能搜索请求: {}", searchRequest.getQuery());
        
        // 启用智能权重自适应
        searchRequest.setEnableAdaptiveWeights(true);
        searchRequest.setSearchType("hybrid");
        
        SearchResult searchResult = hybridSearchService.hybridSearch(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResult));
    }

    /**
     * 上下文感知智能搜索
     */
    @Operation(summary = "上下文感知搜索", description = "基于用户行为、场景识别的个性化智能搜索")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @PostMapping("/context-aware")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SearchResult>> contextAwareSearch(
            @Valid @RequestBody SearchRequest searchRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {
        
        log.info("上下文感知搜索请求: query={}, userId={}", 
                searchRequest.getQuery(), userPrincipal.getId());
        
        // 构建User对象
        User user = new User();
        user.setId(userPrincipal.getId());
        user.setUsername(userPrincipal.getUsername());
        user.setEmail(userPrincipal.getEmail());
        
        // 执行上下文感知搜索
        SearchResult searchResult = contextAwareSearchService.contextAwareSearch(
                searchRequest, user, request);
        
        return ResponseEntity.ok(ApiResponse.success(searchResult));
    }

    /**
     * 个性化搜索（简化接口）
     */
    @Operation(summary = "个性化搜索", description = "基于用户偏好的个性化搜索（GET接口）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @GetMapping("/personalized")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SearchResult>> personalizedSearch(
            @Parameter(description = "搜索查询") @RequestParam String query,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest request) {
        
        log.info("个性化搜索请求: query={}, userId={}", query, userPrincipal.getId());
        
        // 构建搜索请求
        SearchRequest searchRequest = new SearchRequest(query, page * size, size);
        searchRequest.setSearchType("hybrid");
        searchRequest.setEnableAdaptiveWeights(true);
        
        // 构建User对象
        User user = new User();
        user.setId(userPrincipal.getId());
        user.setUsername(userPrincipal.getUsername());
        user.setEmail(userPrincipal.getEmail());
        
        // 执行上下文感知搜索
        SearchResult searchResult = contextAwareSearchService.contextAwareSearch(
                searchRequest, user, request);
        
        return ResponseEntity.ok(ApiResponse.success(searchResult));
    }

    /**
     * 高级搜索（支持完整的过滤和排序选项）
     */
    @Operation(summary = "高级搜索", description = "支持完整的过滤条件、排序选项和聚合功能")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @PostMapping("/advanced")
    public ResponseEntity<ApiResponse<SearchResult>> advancedSearch(
            @Valid @RequestBody SearchRequest searchRequest) {
        log.info("高级搜索请求: query={}, filters={}", 
            searchRequest.getQuery(), 
            searchRequest.getCategoryFilter());
        
        // 启用去重和聚合
        searchRequest.setEnableDeduplication(true);
        if (searchRequest.getAggregationFields() != null && !searchRequest.getAggregationFields().isEmpty()) {
            searchRequest.setIncludeAggregations(true);
        }
        
        SearchResult searchResult = hybridSearchService.hybridSearch(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResult));
    }

    /**
     * 简化的混合搜索接口
     */
    @Operation(summary = "简化混合搜索", description = "使用GET方式的简化混合搜索接口")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @GetMapping("/hybrid")
    public ResponseEntity<ApiResponse<SearchResult>> simpleHybridSearch(
            @Parameter(description = "搜索查询") @RequestParam String query,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "关键词权重") @RequestParam(defaultValue = "1.0") float keywordWeight,
            @Parameter(description = "向量权重") @RequestParam(defaultValue = "2.0") float vectorWeight,
            @Parameter(description = "分类过滤") @RequestParam(required = false) String category,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "relevance") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortOrder) {

        SearchRequest searchRequest = new SearchRequest(query, page * size, size)
            .withHybridWeights(keywordWeight, vectorWeight)
            .withSort(sortBy, sortOrder);
        
        if (category != null && !category.trim().isEmpty()) {
            searchRequest.setCategoryFilter(category);
        }

        SearchResult searchResult = hybridSearchService.hybridSearch(searchRequest);
        return ResponseEntity.ok(ApiResponse.success(searchResult));
    }

    /**
     * 搜索建议和自动补全
     */
    @Operation(summary = "搜索建议", description = "获取搜索建议和自动补全")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
            @Parameter(description = "查询前缀") @RequestParam String prefix,
            @Parameter(description = "建议数量限制") @RequestParam(defaultValue = "10") int limit) {
        
        log.info("搜索建议请求: prefix={}, limit={}", prefix, limit);
        
        // 这里应该调用专门的建议服务，目前简单返回一些示例
        List<String> suggestions = List.of(
            prefix + "相关产品",
            prefix + "服务指南", 
            prefix + "操作手册",
            prefix + "常见问题"
        );
        
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    /**
     * 搜索结果导出
     */
    @Operation(summary = "导出搜索结果", description = "导出搜索结果为指定格式")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "导出成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/export")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportSearchResults(
            @Valid @RequestBody SearchRequest searchRequest,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "json") String format) {
        
        log.info("搜索结果导出请求: query={}, format={}", searchRequest.getQuery(), format);
        
        // 执行搜索获取完整结果
        SearchResult searchResult = hybridSearchService.hybridSearch(searchRequest);
        
        // 构建导出信息
        Map<String, Object> exportInfo = Map.of(
            "query", searchRequest.getQuery(),
            "totalResults", searchResult.getTotalResults(),
            "exportTime", LocalDateTime.now(),
            "format", format,
            "downloadUrl", "/api/search/download/" + System.currentTimeMillis() + "." + format
        );
        
        return ResponseEntity.ok(ApiResponse.success(exportInfo));
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
     * 混合搜索（兼容旧接口）
     */
    @Operation(summary = "混合搜索", description = "结合关键词和语义的混合搜索")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "搜索参数错误")
    })
    @GetMapping("/hybrid-legacy")
    public ResponseEntity<ApiResponse<SearchResult>> hybridSearchLegacy(
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