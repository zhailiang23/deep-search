package com.deepsearch.service;

import com.deepsearch.dto.DocumentResponseDto;
import com.deepsearch.elasticsearch.dto.DocumentIndex;
import com.deepsearch.entity.Document;
import com.deepsearch.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 搜索结果转换器测试
 */
@ExtendWith(MockitoExtension.class)
class SearchResultConverterTest {

    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private SearchResultConverter searchResultConverter;

    private Document testDocument;
    private DocumentIndex testDocumentIndex;
    private DocumentResponseDto testDocumentResponseDto;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();

        // 创建测试文档
        testDocument = new Document();
        testDocument.setId(1L);
        testDocument.setTitle("测试文档标题");
        testDocument.setContent("测试文档内容，包含详细的描述信息");
        testDocument.setStatus(Document.Status.INDEXED);
        testDocument.setUserId(1L);
        testDocument.setCreatedAt(testTime);
        testDocument.setUpdatedAt(testTime);

        // 创建测试DocumentIndex
        testDocumentIndex = new DocumentIndex();
        testDocumentIndex.setId("1");
        testDocumentIndex.setTitle("测试文档标题");
        testDocumentIndex.setContent("测试文档内容，包含详细的描述信息");
        testDocumentIndex.setSummary("测试文档摘要");
        testDocumentIndex.setCategory("测试分类");
        testDocumentIndex.setCreatedAt(testTime);
        testDocumentIndex.setUpdatedAt(testTime);

        // 创建测试DocumentResponseDto
        testDocumentResponseDto = new DocumentResponseDto(testDocument);
    }

    @Test
    void testConvertElasticsearchResults_Success() {
        // 准备测试数据
        List<DocumentIndex> elasticsearchResults = Arrays.asList(
            testDocumentIndex,
            createDocumentIndex("2", "第二个文档", "第二个文档内容")
        );

        Document secondDocument = createDocument(2L, "第二个文档", "第二个文档内容", Document.Status.INDEXED);

        // 准备mock
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.findById(2L)).thenReturn(Optional.of(secondDocument));

        // 执行测试
        List<DocumentResponseDto> result = searchResultConverter.convertElasticsearchResults(elasticsearchResults);

        // 验证结果
        assertThat(result).hasSize(2);

        DocumentResponseDto firstDto = result.get(0);
        assertThat(firstDto.getId()).isEqualTo(1L);
        assertThat(firstDto.getTitle()).isEqualTo("测试文档标题");
        assertThat(firstDto.getContent()).isEqualTo("测试文档内容，包含详细的描述信息");

        DocumentResponseDto secondDto = result.get(1);
        assertThat(secondDto.getId()).isEqualTo(2L);
        assertThat(secondDto.getTitle()).isEqualTo("第二个文档");

        // 验证数据库调用
        verify(documentRepository).findById(1L);
        verify(documentRepository).findById(2L);
    }

    @Test
    void testConvertElasticsearchResults_DocumentNotFound() {
        // 准备测试数据 - 文档不存在
        List<DocumentIndex> elasticsearchResults = Collections.singletonList(testDocumentIndex);

        // 准备mock - 文档不存在
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        // 执行测试
        List<DocumentResponseDto> result = searchResultConverter.convertElasticsearchResults(elasticsearchResults);

        // 验证结果 - 应该返回空列表
        assertThat(result).isEmpty();
        verify(documentRepository).findById(1L);
    }

    @Test
    void testConvertElasticsearchResults_DocumentNotIndexed() {
        // 准备测试数据 - 文档状态不是INDEXED
        Document nonIndexedDocument = createDocument(1L, "未索引文档", "未索引内容", Document.Status.PENDING);
        List<DocumentIndex> elasticsearchResults = Collections.singletonList(testDocumentIndex);

        // 准备mock
        when(documentRepository.findById(1L)).thenReturn(Optional.of(nonIndexedDocument));

        // 执行测试
        List<DocumentResponseDto> result = searchResultConverter.convertElasticsearchResults(elasticsearchResults);

        // 验证结果 - 应该过滤掉未索引的文档
        assertThat(result).isEmpty();
        verify(documentRepository).findById(1L);
    }

    @Test
    void testConvertElasticsearchResults_InvalidDocumentId() {
        // 准备测试数据 - 无效的文档ID
        DocumentIndex invalidIndex = new DocumentIndex();
        invalidIndex.setId("invalid_id");
        invalidIndex.setTitle("无效ID文档");

        List<DocumentIndex> elasticsearchResults = Collections.singletonList(invalidIndex);

        // 执行测试
        List<DocumentResponseDto> result = searchResultConverter.convertElasticsearchResults(elasticsearchResults);

        // 验证结果 - 应该处理NumberFormatException并返回空列表
        assertThat(result).isEmpty();

        // 验证没有数据库查询（因为ID格式无效）
        verify(documentRepository, never()).findById(any());
    }

    @Test
    void testConvertElasticsearchResults_EmptyInput() {
        // 执行测试 - 空输入
        List<DocumentResponseDto> result = searchResultConverter.convertElasticsearchResults(Collections.emptyList());

        // 验证结果
        assertThat(result).isEmpty();
        verify(documentRepository, never()).findById(any());
    }

    @Test
    void testConvertToDocumentIndex_Success() {
        // 准备测试数据
        DocumentResponseDto secondDto = new DocumentResponseDto(
            createDocument(2L, "第二个文档", "第二个文档内容", Document.Status.INDEXED)
        );
        List<DocumentResponseDto> documents = Arrays.asList(testDocumentResponseDto, secondDto);

        // 执行测试
        List<DocumentIndex> result = searchResultConverter.convertToDocumentIndex(documents);

        // 验证结果
        assertThat(result).hasSize(2);

        DocumentIndex firstIndex = result.get(0);
        assertThat(firstIndex.getId()).isEqualTo("1");
        assertThat(firstIndex.getTitle()).isEqualTo("测试文档标题");
        assertThat(firstIndex.getContent()).isEqualTo("测试文档内容，包含详细的描述信息");
        assertThat(firstIndex.getSummary()).isEqualTo("测试文档摘要");
        assertThat(firstIndex.getCategory()).isEqualTo("测试分类");
        assertThat(firstIndex.getSource()).isEqualTo("database");
        assertThat(firstIndex.getCreatedAt()).isEqualTo(testTime);
        assertThat(firstIndex.getUpdatedAt()).isEqualTo(testTime);

        DocumentIndex secondIndex = result.get(1);
        assertThat(secondIndex.getId()).isEqualTo("2");
        assertThat(secondIndex.getTitle()).isEqualTo("第二个文档");
        assertThat(secondIndex.getSource()).isEqualTo("database");
    }

    @Test
    void testConvertToDocumentIndex_EmptyInput() {
        // 执行测试 - 空输入
        List<DocumentIndex> result = searchResultConverter.convertToDocumentIndex(Collections.emptyList());

        // 验证结果
        assertThat(result).isEmpty();
    }

    @Test
    void testConvertToDocumentIndex_NullFields() {
        // 创建包含null字段的DTO
        Document documentWithNulls = new Document();
        documentWithNulls.setId(1L);
        documentWithNulls.setTitle("标题");
        documentWithNulls.setContent(null);  // null content
        documentWithNulls.setStatus(Document.Status.INDEXED);
        documentWithNulls.setUserId(1L);

        DocumentResponseDto dtoWithNulls = new DocumentResponseDto(documentWithNulls);
        List<DocumentResponseDto> documents = Collections.singletonList(dtoWithNulls);

        // 执行测试
        List<DocumentIndex> result = searchResultConverter.convertToDocumentIndex(documents);

        // 验证结果 - 应该正确处理null值
        assertThat(result).hasSize(1);
        DocumentIndex index = result.get(0);
        assertThat(index.getId()).isEqualTo("1");
        assertThat(index.getTitle()).isEqualTo("标题");
        assertThat(index.getContent()).isNull();
        assertThat(index.getSummary()).isEqualTo(""); // DocumentResponseDto会处理null content
        assertThat(index.getCategory()).isEqualTo("default"); // DocumentResponseDto设置默认分类
        assertThat(index.getSource()).isEqualTo("database");
    }

    /**
     * 创建测试用的Document
     */
    private Document createDocument(Long id, String title, String content, Document.Status status) {
        Document document = new Document();
        document.setId(id);
        document.setTitle(title);
        document.setContent(content);
        document.setStatus(status);
        document.setUserId(1L);
        document.setCreatedAt(testTime);
        document.setUpdatedAt(testTime);
        return document;
    }

    /**
     * 创建测试用的DocumentIndex
     */
    private DocumentIndex createDocumentIndex(String id, String title, String content) {
        DocumentIndex index = new DocumentIndex();
        index.setId(id);
        index.setTitle(title);
        index.setContent(content);
        index.setSummary(content.substring(0, Math.min(10, content.length())));
        index.setCategory("测试分类");
        index.setCreatedAt(testTime);
        index.setUpdatedAt(testTime);
        return index;
    }
}