package com.deepsearch.controller;

import com.deepsearch.dto.ApiResponse;
import com.deepsearch.dto.Suggestion;
import com.deepsearch.dto.SearchSuggestionResponse;
import com.deepsearch.service.AutoCompleteService;
import com.deepsearch.service.SearchSuggestionService;
import com.deepsearch.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 自动补全控制器
 */
@RestController
@RequestMapping("/api/autocomplete")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "自动补全功能", description = "搜索自动补全、智能建议API")
public class AutoCompleteController {

    private final AutoCompleteService autoCompleteService;
    private final SearchSuggestionService searchSuggestionService;

    /**
     * 获取自动补全建议
     */
    @Operation(summary = "获取自动补全建议", description = "根据输入前缀返回智能补全建议")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Suggestion>>> getSuggestions(
            @Parameter(description = "搜索前缀") @RequestParam String query,
            @Parameter(description = "返回建议数量限制") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "包含个性化建议") @RequestParam(defaultValue = "true") boolean includePersonalized,
            Authentication authentication,
            HttpServletRequest request) {

        long startTime = System.currentTimeMillis();

        try {
            // 获取当前用户信息（如果已登录）
            Long userId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                userId = userPrincipal.getId();
            }

            // 获取自动补全建议
            List<Suggestion> suggestions = autoCompleteService.getSuggestions(
                    query, userId, limit, includePersonalized);

            long responseTime = System.currentTimeMillis() - startTime;
            log.info("自动补全请求完成: query={}, suggestions={}, responseTime={}ms",
                    query, suggestions.size(), responseTime);

            return ResponseEntity.ok(ApiResponse.success(suggestions));

        } catch (Exception e) {
            log.error("自动补全请求失败: query={}", query, e);
            return ResponseEntity.ok(ApiResponse.error("自动补全请求失败: " + e.getMessage()));
        }
    }

    /**
     * 获取搜索建议（用于改进搜索结果）
     */
    @Operation(summary = "获取搜索建议", description = "为当前搜索查询提供改进建议")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping("/search-suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionResponse>> getSearchSuggestions(
            @Parameter(description = "当前查询文本") @RequestParam String query,
            @Parameter(description = "当前搜索结果数量") @RequestParam(defaultValue = "0") int currentResultCount,
            @Parameter(description = "建议数量限制") @RequestParam(defaultValue = "5") int limit) {

        long startTime = System.currentTimeMillis();

        try {
            SearchSuggestionResponse response = searchSuggestionService.generateSuggestions(
                    query, currentResultCount, limit);

            response.setResponseTimeMs(System.currentTimeMillis() - startTime);

            log.info("搜索建议请求完成: query={}, suggestions={}, responseTime={}ms",
                    query, response.getTotalSuggestions(), response.getResponseTimeMs());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("搜索建议请求失败: query={}", query, e);
            return ResponseEntity.ok(ApiResponse.error("搜索建议请求失败: " + e.getMessage()));
        }
    }

    /**
     * 获取热门搜索词
     */
    @Operation(summary = "获取热门搜索词", description = "返回当前热门的搜索词列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<Suggestion>>> getPopularQueries(
            @Parameter(description = "返回数量限制") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "时间范围（天）") @RequestParam(defaultValue = "7") int days) {

        try {
            List<Suggestion> popularQueries = autoCompleteService.getPopularQueries(limit, days);

            log.info("热门搜索词请求完成: count={}", popularQueries.size());

            return ResponseEntity.ok(ApiResponse.success(popularQueries));

        } catch (Exception e) {
            log.error("热门搜索词请求失败", e);
            return ResponseEntity.ok(ApiResponse.error("热门搜索词请求失败: " + e.getMessage()));
        }
    }

    /**
     * 记录搜索选择（用于改进个性化推荐）
     */
    @Operation(summary = "记录搜索选择", description = "记录用户选择的建议项，用于改进推荐算法")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "记录成功")
    })
    @PostMapping("/record-selection")
    public ResponseEntity<ApiResponse<String>> recordSelection(
            @Parameter(description = "原始查询") @RequestParam String originalQuery,
            @Parameter(description = "选择的建议") @RequestParam String selectedSuggestion,
            @Parameter(description = "建议类型") @RequestParam String suggestionType,
            Authentication authentication) {

        try {
            // 获取当前用户信息（如果已登录）
            Long userId = null;
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                userId = userPrincipal.getId();
            }

            autoCompleteService.recordSelection(originalQuery, selectedSuggestion,
                    suggestionType, userId);

            log.info("搜索选择记录成功: originalQuery={}, selectedSuggestion={}, userId={}",
                    originalQuery, selectedSuggestion, userId);

            return ResponseEntity.ok(ApiResponse.success("搜索选择记录成功"));

        } catch (Exception e) {
            log.error("搜索选择记录失败: originalQuery={}, selectedSuggestion={}",
                    originalQuery, selectedSuggestion, e);
            return ResponseEntity.ok(ApiResponse.error("搜索选择记录失败: " + e.getMessage()));
        }
    }
}