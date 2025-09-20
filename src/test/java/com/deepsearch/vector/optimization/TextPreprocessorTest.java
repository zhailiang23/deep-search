package com.deepsearch.vector.optimization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本预处理器测试类
 */
class TextPreprocessorTest {

    private TextPreprocessor textPreprocessor;

    @BeforeEach
    void setUp() {
        textPreprocessor = new TextPreprocessor();
        // 设置测试用的配置值
        ReflectionTestUtils.setField(textPreprocessor, "maxChunkSize", 100);
        ReflectionTestUtils.setField(textPreprocessor, "chunkOverlap", 10);
        ReflectionTestUtils.setField(textPreprocessor, "minChunkSize", 20);
        ReflectionTestUtils.setField(textPreprocessor, "removeStopWords", false);
    }

    @Test
    @DisplayName("文本清理 - 基本功能")
    void testCleanText_Basic() {
        // Arrange
        String dirtyText = "  Hello   World!  \n\n  This is a test.  ";

        // Act
        String cleaned = textPreprocessor.cleanText(dirtyText);

        // Assert
        assertEquals("hello world! this is a test.", cleaned);
    }

    @Test
    @DisplayName("文本清理 - HTML标签移除")
    void testCleanText_RemoveHtmlTags() {
        // Arrange
        String htmlText = "<p>Hello <strong>World</strong>!</p><div>Test content</div>";

        // Act
        String cleaned = textPreprocessor.cleanText(htmlText);

        // Assert
        assertFalse(cleaned.contains("<"));
        assertFalse(cleaned.contains(">"));
        assertTrue(cleaned.contains("hello"));
        assertTrue(cleaned.contains("world"));
    }

    @Test
    @DisplayName("文本清理 - URL移除")
    void testCleanText_RemoveUrls() {
        // Arrange
        String textWithUrls = "Visit https://www.example.com or http://test.org for more info.";

        // Act
        String cleaned = textPreprocessor.cleanText(textWithUrls);

        // Assert
        assertFalse(cleaned.contains("https://"));
        assertFalse(cleaned.contains("http://"));
        assertTrue(cleaned.contains("visit"));
        assertTrue(cleaned.contains("more info"));
    }

    @Test
    @DisplayName("文本清理 - 邮箱地址移除")
    void testCleanText_RemoveEmails() {
        // Arrange
        String textWithEmails = "Contact us at support@example.com or admin@test.org";

        // Act
        String cleaned = textPreprocessor.cleanText(textWithEmails);

        // Assert
        assertFalse(cleaned.contains("@"));
        assertTrue(cleaned.contains("contact"));
        assertTrue(cleaned.contains("us at"));
    }

    @Test
    @DisplayName("文本清理 - null输入")
    void testCleanText_NullInput() {
        // Act
        String result = textPreprocessor.cleanText(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("文本清理 - 空字符串输入")
    void testCleanText_EmptyInput() {
        // Act
        String result = textPreprocessor.cleanText("");

        // Assert
        assertEquals("", result);
    }

    @Test
    @DisplayName("文本分块 - 正常分块")
    void testChunkText_Normal() {
        // Arrange
        String longText = "This is the first sentence. This is the second sentence. " +
                         "This is the third sentence. This is the fourth sentence. " +
                         "This is the fifth sentence.";

        // Act
        List<String> chunks = textPreprocessor.chunkText(longText);

        // Assert
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 1);

        // 检查每个块的长度
        for (String chunk : chunks) {
            assertTrue(chunk.length() >= 20); // minChunkSize
        }
    }

    @Test
    @DisplayName("文本分块 - 短文本不分块")
    void testChunkText_ShortText() {
        // Arrange
        String shortText = "Short text that should not be chunked.";

        // Act
        List<String> chunks = textPreprocessor.chunkText(shortText);

        // Assert
        assertEquals(1, chunks.size());
        assertEquals(shortText.trim(), chunks.get(0));
    }

    @Test
    @DisplayName("文本分块 - 空输入")
    void testChunkText_EmptyInput() {
        // Act
        List<String> chunks = textPreprocessor.chunkText("");

        // Assert
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("文本分块 - null输入")
    void testChunkText_NullInput() {
        // Act
        List<String> chunks = textPreprocessor.chunkText(null);

        // Assert
        assertTrue(chunks.isEmpty());
    }

    @Test
    @DisplayName("文本预处理 - 完整流程")
    void testPreprocess_FullWorkflow() {
        // Arrange
        String complexText = "<p>This is a <strong>complex</strong> text with HTML tags.</p>" +
                           "Visit https://example.com for more information. " +
                           "Contact us at test@example.com if you have questions. " +
                           "This text should be cleaned and chunked properly.";

        // Act
        List<String> result = textPreprocessor.preprocess(complexText);

        // Assert
        assertFalse(result.isEmpty());

        // 检查HTML标签、URL和邮箱都被移除
        String combined = String.join(" ", result);
        assertFalse(combined.contains("<"));
        assertFalse(combined.contains("https://"));
        assertFalse(combined.contains("@"));

        // 检查内容保留
        assertTrue(combined.contains("complex"));
        assertTrue(combined.contains("information"));
    }

    @Test
    @DisplayName("复杂度计算 - 正常文本")
    void testCalculateComplexity_Normal() {
        // Arrange
        String normalText = "This is a normal text with some variety. " +
                          "It contains different words and sentences. " +
                          "The complexity should be moderate.";

        // Act
        double complexity = textPreprocessor.calculateComplexity(normalText);

        // Assert
        assertTrue(complexity >= 0.0);
        assertTrue(complexity <= 1.0);
        assertTrue(complexity > 0.1); // 应该有一定复杂度
    }

    @Test
    @DisplayName("复杂度计算 - 简单重复文本")
    void testCalculateComplexity_SimpleRepetitive() {
        // Arrange
        String simpleText = "test test test test test test";

        // Act
        double complexity = textPreprocessor.calculateComplexity(simpleText);

        // Assert
        assertTrue(complexity >= 0.0);
        assertTrue(complexity <= 1.0);
        assertTrue(complexity < 0.5); // 重复文本复杂度较低
    }

    @Test
    @DisplayName("复杂度计算 - 空文本")
    void testCalculateComplexity_EmptyText() {
        // Act
        double complexity = textPreprocessor.calculateComplexity("");

        // Assert
        assertEquals(0.0, complexity, 0.0001);
    }

    @Test
    @DisplayName("复杂度计算 - null文本")
    void testCalculateComplexity_NullText() {
        // Act
        double complexity = textPreprocessor.calculateComplexity(null);

        // Assert
        assertEquals(0.0, complexity, 0.0001);
    }

    @Test
    @DisplayName("处理时间估算 - 正常文本")
    void testEstimateProcessingTime_Normal() {
        // Arrange
        String text = "This is a test text for processing time estimation.";

        // Act
        long estimatedTime = textPreprocessor.estimateProcessingTime(text);

        // Assert
        assertTrue(estimatedTime >= 1); // 至少1毫秒
        assertTrue(estimatedTime < 1000); // 小文本应该很快
    }

    @Test
    @DisplayName("处理时间估算 - 长文本")
    void testEstimateProcessingTime_LongText() {
        // Arrange
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is sentence number ").append(i).append(". ");
        }

        // Act
        long estimatedTime = textPreprocessor.estimateProcessingTime(longText.toString());

        // Assert
        assertTrue(estimatedTime > 1); // 长文本需要更多时间
    }

    @Test
    @DisplayName("处理时间估算 - null文本")
    void testEstimateProcessingTime_NullText() {
        // Act
        long estimatedTime = textPreprocessor.estimateProcessingTime(null);

        // Assert
        assertEquals(0, estimatedTime);
    }

    @Test
    @DisplayName("文本分块重叠功能")
    void testChunkOverlap() {
        // Arrange
        // 设置更大的块大小以便测试重叠
        ReflectionTestUtils.setField(textPreprocessor, "maxChunkSize", 50);
        ReflectionTestUtils.setField(textPreprocessor, "chunkOverlap", 20);

        String text = "First sentence here. Second sentence here. Third sentence here. " +
                     "Fourth sentence here. Fifth sentence here. Sixth sentence here.";

        // Act
        List<String> chunks = textPreprocessor.chunkText(text);

        // Assert
        if (chunks.size() > 1) {
            // 检查相邻块之间有重叠内容
            String firstChunk = chunks.get(0);
            String secondChunk = chunks.get(1);

            // 应该有一些共同的词汇
            String[] firstWords = firstChunk.split("\\s+");
            String[] secondWords = secondChunk.split("\\s+");

            boolean hasOverlap = false;
            for (String word1 : firstWords) {
                for (String word2 : secondWords) {
                    if (word1.equals(word2)) {
                        hasOverlap = true;
                        break;
                    }
                }
                if (hasOverlap) break;
            }

            assertTrue(hasOverlap, "分块之间应该有重叠内容");
        }
    }

    @Test
    @DisplayName("最小块大小过滤")
    void testMinChunkSizeFiltering() {
        // Arrange
        ReflectionTestUtils.setField(textPreprocessor, "minChunkSize", 50);
        String shortText = "Short text.";

        // Act
        List<String> result = textPreprocessor.preprocess(shortText);

        // Assert
        assertTrue(result.isEmpty(), "短于最小长度的文本块应该被过滤掉");
    }

    @Test
    @DisplayName("特殊字符处理")
    void testSpecialCharacters() {
        // Arrange
        String textWithSpecialChars = "Hello!!! World??? This... is... a... test!!!";

        // Act
        String cleaned = textPreprocessor.cleanText(textWithSpecialChars);

        // Assert
        // 多个连续标点符号应该被处理
        assertFalse(cleaned.contains("!!!"));
        assertFalse(cleaned.contains("???"));
        assertFalse(cleaned.contains("..."));
        assertTrue(cleaned.contains("hello"));
        assertTrue(cleaned.contains("world"));
    }
}