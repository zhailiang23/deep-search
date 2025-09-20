package com.deepsearch.elasticsearch.controller;

import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.elasticsearch.dto.SearchRequest;
import com.deepsearch.elasticsearch.dto.SearchResult;
import com.deepsearch.elasticsearch.service.ElasticsearchSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/elasticsearch")
@Tag(name = "Elasticsearch API", description = "直接访问Elasticsearch搜索引擎的API")
@Slf4j
public class ElasticsearchController {

    @Autowired
    private ElasticsearchSearchService elasticsearchSearchService;

    @PostMapping("/search/keyword")
    @Operation(summary = "关键词搜索", description = "使用Elasticsearch进行关键词搜索，支持中文分词和模糊匹配")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "搜索服务错误")
    })
    public ResponseEntity<SearchResult> keywordSearch(@Valid @RequestBody SearchRequest request) {
        try {
            long startTime = System.currentTimeMillis();

            List<DocumentIndex> documents = elasticsearchSearchService.keywordSearch(
                request.getQuery(),
                request.getSpaceId(),
                request.getChannels(),
                request.getFrom(),
                request.getSize()
            );

            long responseTime = System.currentTimeMillis() - startTime;

            SearchResult result = new SearchResult(
                request.getQuery(),
                documents,
                documents.size(),
                request.getFrom() / request.getSize(),
                request.getSize(),
                responseTime,
                "keyword"
            );

            log.info("关键词搜索完成: query='{}', results={}, time={}ms",
                request.getQuery(), documents.size(), responseTime);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("关键词搜索失败: {}", request.getQuery(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/search/vector")
    @Operation(summary = "向量语义搜索", description = "使用向量相似性进行语义搜索，基于文本语义理解")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "搜索服务错误")
    })
    public ResponseEntity<SearchResult> vectorSearch(@Valid @RequestBody SearchRequest request) {
        try {
            long startTime = System.currentTimeMillis();

            List<DocumentIndex> documents = elasticsearchSearchService.vectorSearch(
                request.getQuery(),
                request.getSpaceId(),
                request.getChannels(),
                request.getFrom(),
                request.getSize()
            );

            long responseTime = System.currentTimeMillis() - startTime;

            SearchResult result = new SearchResult(
                request.getQuery(),
                documents,
                documents.size(),
                request.getFrom() / request.getSize(),
                request.getSize(),
                responseTime,
                "vector"
            );

            log.info("向量搜索完成: query='{}', results={}, time={}ms",
                request.getQuery(), documents.size(), responseTime);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("向量搜索失败: {}", request.getQuery(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/search/hybrid")
    @Operation(summary = "混合搜索", description = "结合关键词搜索和向量搜索，提供最佳搜索效果")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "搜索服务错误")
    })
    public ResponseEntity<SearchResult> hybridSearch(@Valid @RequestBody SearchRequest request) {
        try {
            long startTime = System.currentTimeMillis();

            // 使用默认权重：关键词权重1.0，向量权重2.0
            float keywordWeight = request.getKeywordWeight() != null ? request.getKeywordWeight() : 1.0f;
            float vectorWeight = request.getVectorWeight() != null ? request.getVectorWeight() : 2.0f;

            List<DocumentIndex> documents = elasticsearchSearchService.hybridSearch(
                request.getQuery(),
                request.getSpaceId(),
                request.getChannels(),
                request.getFrom(),
                request.getSize(),
                keywordWeight,
                vectorWeight
            );

            long responseTime = System.currentTimeMillis() - startTime;

            SearchResult result = new SearchResult(
                request.getQuery(),
                documents,
                documents.size(),
                request.getFrom() / request.getSize(),
                request.getSize(),
                responseTime,
                "hybrid"
            );

            log.info("混合搜索完成: query='{}', results={}, time={}ms, keyword_weight={}, vector_weight={}",
                request.getQuery(), documents.size(), responseTime, keywordWeight, vectorWeight);

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("混合搜索失败: {}", request.getQuery(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/count")
    @Operation(summary = "文档计数", description = "获取指定空间的文档总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "计数成功"),
        @ApiResponse(responseCode = "500", description = "计数服务错误")
    })
    public ResponseEntity<Long> getDocumentCount(
        @Parameter(description = "空间ID，为空则统计所有文档")
        @RequestParam(required = false) String spaceId) {
        try {
            long count = elasticsearchSearchService.getDocumentCount(spaceId);

            log.info("文档计数完成: spaceId='{}', count={}", spaceId, count);

            return ResponseEntity.ok(count);
        } catch (IOException e) {
            log.error("文档计数失败: spaceId='{}'", spaceId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/suggest")
    @Operation(summary = "搜索建议", description = "基于输入提供搜索建议词")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "建议获取成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "建议服务错误")
    })
    public ResponseEntity<List<String>> getSuggestions(
        @Parameter(description = "部分查询文本")
        @RequestParam String query,
        @Parameter(description = "建议数量限制")
        @RequestParam(defaultValue = "5") int limit) {
        try {
            // TODO: 实现基于Elasticsearch的搜索建议功能
            // 目前返回空列表，后续可以基于搜索历史或索引内容提供建议

            log.info("搜索建议请求: query='{}', limit={}", query, limit);

            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            log.error("搜索建议失败: query='{}'", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}