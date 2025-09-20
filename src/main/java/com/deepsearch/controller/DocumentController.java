package com.deepsearch.controller;

import com.deepsearch.dto.ApiResponse;
import com.deepsearch.dto.DocumentCreateDto;
import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.entity.Document;
import com.deepsearch.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档管理控制器
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文档管理", description = "文档创建、查询、更新、删除API")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     */
    @Operation(summary = "创建文档", description = "创建新的文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> createDocument(
            @Valid @RequestBody DocumentCreateDto createDto) {
        log.info("创建文档请求: {}", createDto.getTitle());
        DocumentResponseDto documentResponse = documentService.createDocument(createDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("文档创建成功", documentResponse));
    }

    /**
     * 根据ID获取文档
     */
    @Operation(summary = "获取文档详情", description = "根据文档ID获取文档详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文档不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权访问")
    })
    @GetMapping("/{documentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> getDocumentById(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        DocumentResponseDto documentResponse = documentService.getDocumentById(documentId);
        return ResponseEntity.ok(ApiResponse.success(documentResponse));
    }

    /**
     * 更新文档
     */
    @Operation(summary = "更新文档", description = "更新指定文档的信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文档不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权修改")
    })
    @PutMapping("/{documentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> updateDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Valid @RequestBody DocumentCreateDto updateDto) {
        DocumentResponseDto documentResponse = documentService.updateDocument(documentId, updateDto);
        return ResponseEntity.ok(ApiResponse.success("文档更新成功", documentResponse));
    }

    /**
     * 删除文档
     */
    @Operation(summary = "删除文档", description = "删除指定文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文档不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权删除")
    })
    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteDocument(
            @Parameter(description = "文档ID") @PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(ApiResponse.success("文档删除成功"));
    }

    /**
     * 获取当前用户的文档列表
     */
    @Operation(summary = "获取当前用户文档", description = "分页获取当前用户的文档列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<DocumentResponseDto>>> getCurrentUserDocuments(
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        Page<DocumentResponseDto> documents = documentService.getCurrentUserDocuments(pageable);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * 根据用户ID获取文档列表（管理员）
     */
    @Operation(summary = "获取指定用户文档", description = "管理员分页获取指定用户的文档列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "用户不存在")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<DocumentResponseDto>>> getUserDocuments(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "DESC") Sort.Direction sortDir) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortBy));
        Page<DocumentResponseDto> documents = documentService.getUserDocuments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * 根据状态获取文档列表（管理员）
     */
    @Operation(summary = "根据状态获取文档", description = "管理员根据文档状态获取文档列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getDocumentsByStatus(
            @Parameter(description = "文档状态") @PathVariable Document.Status status) {
        List<DocumentResponseDto> documents = documentService.getDocumentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * 搜索文档（按标题）
     */
    @Operation(summary = "搜索文档", description = "根据标题关键词搜索文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "搜索成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "用户未认证")
    })
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> searchDocuments(
            @Parameter(description = "搜索关键词") @RequestParam String title) {
        List<DocumentResponseDto> documents = documentService.searchDocumentsByTitle(title);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * 更新文档状态（管理员）
     */
    @Operation(summary = "更新文档状态", description = "管理员更新文档的处理状态")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "文档不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/{documentId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponseDto>> updateDocumentStatus(
            @Parameter(description = "文档ID") @PathVariable Long documentId,
            @Parameter(description = "新状态") @RequestParam Document.Status status) {
        DocumentResponseDto documentResponse = documentService.updateDocumentStatus(documentId, status);
        return ResponseEntity.ok(ApiResponse.success("文档状态更新成功", documentResponse));
    }

    /**
     * 获取用户文档统计
     */
    @Operation(summary = "获取用户文档统计", description = "获取指定用户的文档数量统计")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/stats/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @documentService.getCurrentUserId() == #userId")
    public ResponseEntity<ApiResponse<Long>> getUserDocumentCount(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        Long count = documentService.getUserDocumentCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 获取状态统计（管理员）
     */
    @Operation(summary = "获取文档状态统计", description = "管理员获取指定状态的文档数量")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/stats/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getDocumentCountByStatus(
            @Parameter(description = "文档状态") @PathVariable Document.Status status) {
        Long count = documentService.getDocumentCountByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * 获取时间范围内的文档（管理员）
     */
    @Operation(summary = "获取时间范围内文档", description = "管理员获取指定时间范围内创建的文档")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DocumentResponseDto>>> getDocumentsByDateRange(
            @Parameter(description = "开始时间")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "结束时间")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<DocumentResponseDto> documents = documentService.getDocumentsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }
}