package com.deepsearch.service;

import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.entity.Document;
import com.deepsearch.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索结果转换器 - 统一处理搜索结果的转换逻辑
 * 消除SearchService和HybridSearchService中的重复转换代码
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchResultConverter {

    private final DocumentRepository documentRepository;

    /**
     * 转换Elasticsearch结果为业务DTO
     */
    public List<DocumentResponseDto> convertElasticsearchResults(List<DocumentIndex> elasticsearchResults) {
        List<DocumentResponseDto> results = new ArrayList<>();

        for (DocumentIndex docIndex : elasticsearchResults) {
            try {
                Long documentId = Long.parseLong(docIndex.getId());
                Document document = documentRepository.findById(documentId).orElse(null);

                if (document != null && document.getStatus() == Document.Status.INDEXED) {
                    results.add(new DocumentResponseDto(document));
                }
            } catch (NumberFormatException e) {
                log.warn("无效的文档ID格式: {}", docIndex.getId());
            }
        }

        return results;
    }

    /**
     * 将DocumentResponseDto转换为DocumentIndex（用于SearchResult）
     */
    public List<DocumentIndex> convertToDocumentIndex(List<DocumentResponseDto> documents) {
        return documents.stream().map(doc -> {
            DocumentIndex docIndex = new DocumentIndex();
            docIndex.setId(doc.getId().toString());
            docIndex.setTitle(doc.getTitle());
            docIndex.setContent(doc.getContent());
            docIndex.setSummary(doc.getSummary());
            docIndex.setCategory(doc.getCategory());
            docIndex.setSource("database"); // 标记数据来源
            docIndex.setCreatedAt(doc.getCreatedAt());
            docIndex.setUpdatedAt(doc.getUpdatedAt());
            return docIndex;
        }).collect(Collectors.toList());
    }
}