package com.deepsearch.vector.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 文本预处理器
 * 提供文本清理、分块和优化功能，提高向量化质量
 */
@Component
@Slf4j
public class TextPreprocessor {

    // 常用停用词
    private static final List<String> STOP_WORDS = Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你",
        "会", "着", "没有", "看", "好", "自己", "这", "那", "他", "她", "它", "我们", "你们", "他们", "她们", "它们",
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are", "was", "were", "be", "been"
    );

    // 标点符号和特殊字符
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s]+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    @Value("${vector.preprocessing.max-chunk-size:512}")
    private int maxChunkSize;

    @Value("${vector.preprocessing.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${vector.preprocessing.min-chunk-size:50}")
    private int minChunkSize;

    @Value("${vector.preprocessing.remove-stop-words:false}")
    private boolean removeStopWords;

    /**
     * 预处理文本：清理、标准化和分块
     *
     * @param text 原始文本
     * @return 预处理后的文本块列表
     */
    public List<String> preprocess(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        log.debug("开始预处理文本，长度: {}", text.length());

        // 1. 基本清理
        String cleaned = cleanText(text);
        if (cleaned.isEmpty()) {
            return List.of();
        }

        // 2. 分块处理
        List<String> chunks = chunkText(cleaned);

        // 3. 过滤短块
        List<String> validChunks = chunks.stream()
            .filter(chunk -> chunk.length() >= minChunkSize)
            .toList();

        log.debug("预处理完成，原始长度: {}, 清理后长度: {}, 分块数: {}, 有效分块数: {}",
            text.length(), cleaned.length(), chunks.size(), validChunks.size());

        return validChunks;
    }

    /**
     * 清理文本内容
     */
    public String cleanText(String text) {
        if (text == null) {
            return "";
        }

        String result = text;

        // 1. 移除HTML标签
        result = HTML_TAG_PATTERN.matcher(result).replaceAll(" ");

        // 2. 移除URL
        result = URL_PATTERN.matcher(result).replaceAll(" ");

        // 3. 移除邮箱地址
        result = EMAIL_PATTERN.matcher(result).replaceAll(" ");

        // 4. 标准化空白字符
        result = WHITESPACE_PATTERN.matcher(result).replaceAll(" ");

        // 5. 移除多余的标点符号
        result = result.replaceAll("[\\p{Punct}]{2,}", " ");

        // 6. 移除停用词（可选）
        if (removeStopWords) {
            result = removeStopWords(result);
        }

        // 7. 去除首尾空白并转换为小写
        result = result.trim().toLowerCase();

        return result;
    }

    /**
     * 分块处理长文本
     */
    public List<String> chunkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        String[] sentences = splitIntoSentences(text);

        StringBuilder currentChunk = new StringBuilder();
        int currentLength = 0;

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }

            // 如果添加这个句子会超过最大长度，先保存当前块
            if (currentLength + trimmedSentence.length() > maxChunkSize && currentLength > 0) {
                String chunk = currentChunk.toString().trim();
                if (chunk.length() >= minChunkSize) {
                    chunks.add(chunk);
                }

                // 开始新的块，保留重叠部分
                currentChunk = new StringBuilder();
                if (chunkOverlap > 0 && chunk.length() >= chunkOverlap) {
                    String overlap = getLastWords(chunk, chunkOverlap);
                    currentChunk.append(overlap).append(" ");
                    currentLength = overlap.length() + 1;
                } else {
                    currentLength = 0;
                }
            }

            // 添加当前句子
            if (currentChunk.length() > 0) {
                currentChunk.append(" ");
                currentLength++;
            }
            currentChunk.append(trimmedSentence);
            currentLength += trimmedSentence.length();
        }

        // 添加最后一个块
        String lastChunk = currentChunk.toString().trim();
        if (lastChunk.length() >= minChunkSize) {
            chunks.add(lastChunk);
        }

        return chunks;
    }

    /**
     * 将文本分割成句子
     */
    private String[] splitIntoSentences(String text) {
        // 简单的句子分割，基于常见的句子结束标点
        return text.split("[.!?。！？]+");
    }

    /**
     * 获取文本的最后几个词，用于重叠
     */
    private String getLastWords(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        int currentLength = 0;

        // 从后往前添加词，直到接近maxLength
        for (int i = words.length - 1; i >= 0; i--) {
            String word = words[i];
            if (currentLength + word.length() + 1 > maxLength) {
                break;
            }

            if (result.length() > 0) {
                result.insert(0, " ");
                currentLength++;
            }
            result.insert(0, word);
            currentLength += word.length();
        }

        return result.toString();
    }

    /**
     * 移除停用词
     */
    private String removeStopWords(String text) {
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            String cleanWord = word.toLowerCase().trim();
            if (!STOP_WORDS.contains(cleanWord) && cleanWord.length() > 1) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word);
            }
        }

        return result.toString();
    }

    /**
     * 计算文本的复杂度得分（用于质量评估）
     *
     * @param text 文本内容
     * @return 复杂度得分（0-1之间，越高越复杂）
     */
    public double calculateComplexity(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        // 1. 词汇多样性
        String[] words = text.toLowerCase().split("\\s+");
        long uniqueWords = Arrays.stream(words).distinct().count();
        double lexicalDiversity = words.length > 0 ? (double) uniqueWords / words.length : 0.0;

        // 2. 平均句子长度
        String[] sentences = splitIntoSentences(text);
        double avgSentenceLength = sentences.length > 0 ? (double) words.length / sentences.length : 0.0;

        // 3. 标点符号密度
        long punctuationCount = text.chars().filter(ch -> Character.getType(ch) == Character.OTHER_PUNCTUATION).count();
        double punctuationDensity = text.length() > 0 ? (double) punctuationCount / text.length() : 0.0;

        // 综合得分（权重可调整）
        double complexity = (lexicalDiversity * 0.4) +
                           (Math.min(avgSentenceLength / 20.0, 1.0) * 0.4) +
                           (punctuationDensity * 0.2);

        return Math.min(complexity, 1.0);
    }

    /**
     * 估算文本的处理时间（毫秒）
     */
    public long estimateProcessingTime(String text) {
        if (text == null) {
            return 0;
        }

        // 基于文本长度和复杂度的简单估算
        int length = text.length();
        double complexity = calculateComplexity(text);

        // 基础处理时间（每1000字符约1毫秒）+ 复杂度影响
        long baseTime = length / 1000;
        long complexityTime = (long) (complexity * 10);

        return Math.max(baseTime + complexityTime, 1);
    }
}