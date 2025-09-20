package com.deepsearch.service;

import com.deepsearch.entity.SearchLog;
import com.deepsearch.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Trie树索引服务
 * 提供高效的前缀匹配功能，支持自动补全
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrieService {

    private final SearchLogRepository searchLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Trie树根节点
    private TrieNode root;

    // 读写锁，保证线程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 热门度权重缓存
    private final Map<String, Integer> termFrequency = new ConcurrentHashMap<>();

    /**
     * Trie树节点
     */
    static class TrieNode {
        Map<Character, TrieNode> children;
        boolean isEndOfWord;
        String word;
        int frequency;

        TrieNode() {
            children = new HashMap<>();
            isEndOfWord = false;
            frequency = 0;
        }
    }

    /**
     * 初始化Trie树
     */
    @PostConstruct
    public void initialize() {
        lock.writeLock().lock();
        try {
            root = new TrieNode();
            buildTrieFromSearchLogs();
            log.info("TrieService 初始化完成，加载了 {} 个词条", termFrequency.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 添加词条到Trie树
     *
     * @param word      词条
     * @param frequency 使用频率
     */
    public void addWord(String word, int frequency) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        String normalizedWord = word.trim().toLowerCase();
        lock.writeLock().lock();
        try {
            TrieNode current = root;

            for (char ch : normalizedWord.toCharArray()) {
                current.children.putIfAbsent(ch, new TrieNode());
                current = current.children.get(ch);
            }

            current.isEndOfWord = true;
            current.word = normalizedWord;
            current.frequency = frequency;

            // 更新频率缓存
            termFrequency.put(normalizedWord, frequency);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量添加词条
     */
    public void addWords(Map<String, Integer> words) {
        lock.writeLock().lock();
        try {
            for (Map.Entry<String, Integer> entry : words.entrySet()) {
                addWordInternal(entry.getKey(), entry.getValue());
            }
            log.info("批量添加了 {} 个词条", words.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取前缀匹配的建议
     *
     * @param prefix 前缀
     * @param limit  返回数量限制
     * @return 匹配的词条列表，按频率排序
     */
    public List<String> getPrefixMatches(String prefix, int limit) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedPrefix = prefix.trim().toLowerCase();
        lock.readLock().lock();
        try {
            TrieNode prefixNode = findPrefix(normalizedPrefix);
            if (prefixNode == null) {
                return Collections.emptyList();
            }

            List<TrieNode> matches = new ArrayList<>();
            collectAllWords(prefixNode, matches);

            return matches.stream()
                    .filter(node -> node.isEndOfWord)
                    .sorted((a, b) -> Integer.compare(b.frequency, a.frequency))
                    .map(node -> node.word)
                    .limit(limit)
                    .collect(Collectors.toList());

        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 检查词条是否存在
     */
    public boolean contains(String word) {
        if (word == null || word.trim().isEmpty()) {
            return false;
        }

        String normalizedWord = word.trim().toLowerCase();
        lock.readLock().lock();
        try {
            TrieNode current = root;

            for (char ch : normalizedWord.toCharArray()) {
                if (!current.children.containsKey(ch)) {
                    return false;
                }
                current = current.children.get(ch);
            }

            return current.isEndOfWord;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 获取词条的使用频率
     */
    public int getFrequency(String word) {
        if (word == null || word.trim().isEmpty()) {
            return 0;
        }

        String normalizedWord = word.trim().toLowerCase();
        return termFrequency.getOrDefault(normalizedWord, 0);
    }

    /**
     * 增加词条使用频率
     */
    public void incrementFrequency(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        String normalizedWord = word.trim().toLowerCase();
        lock.writeLock().lock();
        try {
            int currentFreq = termFrequency.getOrDefault(normalizedWord, 0);
            int newFreq = currentFreq + 1;

            // 更新Trie树中的频率
            TrieNode wordNode = findWord(normalizedWord);
            if (wordNode != null) {
                wordNode.frequency = newFreq;
            } else {
                // 如果词条不存在，添加它
                addWordInternal(normalizedWord, newFreq);
            }

            termFrequency.put(normalizedWord, newFreq);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取最热门的词条
     */
    public List<String> getTopTerms(int limit) {
        lock.readLock().lock();
        try {
            return termFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 清空Trie树
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            root = new TrieNode();
            termFrequency.clear();
            log.info("Trie树已清空");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取Trie树统计信息
     */
    public Map<String, Object> getStats() {
        lock.readLock().lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTerms", termFrequency.size());
            stats.put("totalFrequency", termFrequency.values().stream().mapToInt(Integer::intValue).sum());

            if (!termFrequency.isEmpty()) {
                int maxFreq = termFrequency.values().stream().mapToInt(Integer::intValue).max().orElse(0);
                int minFreq = termFrequency.values().stream().mapToInt(Integer::intValue).min().orElse(0);
                double avgFreq = termFrequency.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);

                stats.put("maxFrequency", maxFreq);
                stats.put("minFrequency", minFreq);
                stats.put("averageFrequency", avgFreq);
            }

            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 定期重建Trie树（每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    @Async
    public void rebuildTrie() {
        log.info("开始重建Trie树...");
        long startTime = System.currentTimeMillis();

        try {
            buildTrieFromSearchLogs();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Trie树重建完成，耗时: {}ms，词条数: {}", duration, termFrequency.size());

        } catch (Exception e) {
            log.error("Trie树重建失败", e);
        }
    }

    /**
     * 从搜索日志构建Trie树
     */
    private void buildTrieFromSearchLogs() {
        try {
            // 获取最近的搜索日志
            List<SearchLog> recentLogs = searchLogRepository.findRecentSearchLogs(10000);

            // 统计词频
            Map<String, Integer> wordFreq = new HashMap<>();
            for (SearchLog log : recentLogs) {
                String query = log.getQueryText();
                if (query != null && !query.trim().isEmpty()) {
                    String normalizedQuery = query.trim().toLowerCase();
                    wordFreq.put(normalizedQuery, wordFreq.getOrDefault(normalizedQuery, 0) + 1);
                }
            }

            // 重建Trie树
            lock.writeLock().lock();
            try {
                root = new TrieNode();
                termFrequency.clear();

                for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
                    addWordInternal(entry.getKey(), entry.getValue());
                }

            } finally {
                lock.writeLock().unlock();
            }

            log.info("从搜索日志构建Trie树完成，处理了 {} 条日志，生成 {} 个词条",
                    recentLogs.size(), wordFreq.size());

        } catch (Exception e) {
            log.error("从搜索日志构建Trie树失败", e);
        }
    }

    /**
     * 内部添加词条方法（不加锁）
     */
    private void addWordInternal(String word, int frequency) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        String normalizedWord = word.trim().toLowerCase();
        TrieNode current = root;

        for (char ch : normalizedWord.toCharArray()) {
            current.children.putIfAbsent(ch, new TrieNode());
            current = current.children.get(ch);
        }

        current.isEndOfWord = true;
        current.word = normalizedWord;
        current.frequency = frequency;
        termFrequency.put(normalizedWord, frequency);
    }

    /**
     * 查找前缀节点
     */
    private TrieNode findPrefix(String prefix) {
        TrieNode current = root;

        for (char ch : prefix.toCharArray()) {
            if (!current.children.containsKey(ch)) {
                return null;
            }
            current = current.children.get(ch);
        }

        return current;
    }

    /**
     * 查找完整词条节点
     */
    private TrieNode findWord(String word) {
        TrieNode prefixNode = findPrefix(word);
        return (prefixNode != null && prefixNode.isEndOfWord) ? prefixNode : null;
    }

    /**
     * 递归收集所有词条
     */
    private void collectAllWords(TrieNode node, List<TrieNode> result) {
        if (node == null) {
            return;
        }

        if (node.isEndOfWord) {
            result.add(node);
        }

        for (TrieNode child : node.children.values()) {
            collectAllWords(child, result);
        }
    }

    /**
     * 持久化Trie树到Redis（可选功能）
     */
    @Scheduled(fixedRate = 1800000) // 30分钟
    @Async
    public void persistToRedis() {
        try {
            String key = "trie:frequency_map";
            redisTemplate.opsForHash().putAll(key,
                termFrequency.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toString()
                    ))
            );
            redisTemplate.expire(key, 24, TimeUnit.HOURS);

            log.debug("Trie树词频数据已持久化到Redis");

        } catch (Exception e) {
            log.warn("Trie树数据持久化失败", e);
        }
    }

    /**
     * 从Redis加载Trie树数据（可选功能）
     */
    public void loadFromRedis() {
        try {
            String key = "trie:frequency_map";
            Map<Object, Object> data = redisTemplate.opsForHash().entries(key);

            if (!data.isEmpty()) {
                Map<String, Integer> wordFreq = data.entrySet().stream()
                        .collect(Collectors.toMap(
                                e -> e.getKey().toString(),
                                e -> Integer.parseInt(e.getValue().toString())
                        ));

                addWords(wordFreq);
                log.info("从Redis加载了 {} 个词条", wordFreq.size());
            }

        } catch (Exception e) {
            log.warn("从Redis加载Trie树数据失败", e);
        }
    }
}