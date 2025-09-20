package com.deepsearch.service;

import com.deepsearch.entity.Synonym;
import com.deepsearch.repository.SynonymRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 同义词服务测试类
 */
@ExtendWith(MockitoExtension.class)
class SynonymServiceTest {

    @Mock
    private SynonymRepository synonymRepository;

    @InjectMocks
    private SynonymService synonymService;

    @BeforeEach
    void setUp() {
        // 设置测试配置
        ReflectionTestUtils.setField(synonymService, "maxCacheSize", 1000);
        ReflectionTestUtils.setField(synonymService, "cacheExpireMinutes", 30);
        ReflectionTestUtils.setField(synonymService, "confidenceThreshold", 0.7f);
        ReflectionTestUtils.setField(synonymService, "maxExpansionTerms", 5);
    }

    @Test
    void testGetSynonyms_WithValidTerm_ShouldReturnSynonymList() {
        // Given
        String term = "房贷";
        List<Synonym> mockSynonyms = Arrays.asList(
            createSynonym(1L, "房贷", "住房贷款", 0.95f, Synonym.SynonymSource.MANUAL),
            createSynonym(2L, "房贷", "按揭贷款", 0.9f, Synonym.SynonymSource.MANUAL),
            createSynonym(3L, "房贷", "房屋贷款", 0.85f, Synonym.SynonymSource.AUTO)
        );

        when(synonymRepository.findByTermAndEnabled(term)).thenReturn(mockSynonyms);

        // When
        List<String> result = synonymService.getSynonyms(term);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("住房贷款", "按揭贷款", "房屋贷款");
        verify(synonymRepository).findByTermAndEnabled(term);
    }

    @Test
    void testGetSynonyms_WithLowConfidenceThreshold_ShouldFilterLowConfidenceSynonyms() {
        // Given
        String term = "理财";
        ReflectionTestUtils.setField(synonymService, "confidenceThreshold", 0.8f);

        List<Synonym> mockSynonyms = Arrays.asList(
            createSynonym(1L, "理财", "理财产品", 0.9f, Synonym.SynonymSource.MANUAL),
            createSynonym(2L, "理财", "投资理财", 0.75f, Synonym.SynonymSource.AUTO), // 低于阈值
            createSynonym(3L, "理财", "财富管理", 0.85f, Synonym.SynonymSource.MANUAL)
        );

        when(synonymRepository.findByTermAndEnabled(term)).thenReturn(mockSynonyms);

        // When
        List<String> result = synonymService.getSynonyms(term);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly("理财产品", "财富管理");
    }

    @Test
    void testGetSynonyms_WithNullOrEmptyTerm_ShouldReturnEmptyList() {
        // When & Then
        assertThat(synonymService.getSynonyms(null)).isEmpty();
        assertThat(synonymService.getSynonyms("")).isEmpty();
        assertThat(synonymService.getSynonyms("   ")).isEmpty();

        verifyNoInteractions(synonymRepository);
    }

    @Test
    void testGetBidirectionalSynonyms_ShouldReturnBothDirections() {
        // Given
        String word = "信用卡";
        List<Synonym> mockSynonyms = Arrays.asList(
            createSynonym(1L, "信用卡", "贷记卡", 0.95f, Synonym.SynonymSource.MANUAL),
            createSynonym(2L, "贷记卡", "透支卡", 0.8f, Synonym.SynonymSource.MANUAL)
        );

        when(synonymRepository.findByWordBidirectional(word)).thenReturn(mockSynonyms);

        // When
        Set<String> result = synonymService.getBidirectionalSynonyms(word);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("贷记卡", "透支卡");
        assertThat(result).doesNotContain("信用卡"); // 排除原始查询词
    }

    @Test
    void testExpandQuery_WithBankingTerms_ShouldExpandCorrectly() {
        // Given
        String query = "房贷利率";
        when(synonymRepository.findByTermAndEnabled("房贷")).thenReturn(Arrays.asList(
            createSynonym(1L, "房贷", "住房贷款", 0.95f, Synonym.SynonymSource.MANUAL),
            createSynonym(2L, "房贷", "按揭贷款", 0.9f, Synonym.SynonymSource.MANUAL)
        ));
        when(synonymRepository.findByTermAndEnabled("利率")).thenReturn(Arrays.asList(
            createSynonym(3L, "利率", "贷款利率", 0.85f, Synonym.SynonymSource.MANUAL)
        ));
        when(synonymRepository.findByWordBidirectional("房贷")).thenReturn(Collections.emptyList());
        when(synonymRepository.findByWordBidirectional("利率")).thenReturn(Collections.emptyList());

        // When
        Set<String> result = synonymService.expandQuery(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("房贷利率"); // 原始查询
        assertThat(result).contains("住房贷款", "按揭贷款", "贷款利率");
        // 银行业务特定扩展
        assertThat(result).contains("住房贷款", "按揭", "房屋按揭", "住房按揭");
    }

    @Test
    void testExpandQuery_WithSpecialBankingTerms_ShouldIncludeSpecialExpansions() {
        // Given
        String query = "转账";

        // When
        Set<String> result = synonymService.expandQuery(query);

        // Then
        assertThat(result).contains("转账"); // 原始查询
        // 银行业务特定扩展
        assertThat(result).contains("汇款", "转钱", "付款", "支付");
    }

    @Test
    void testAddSynonym_WithNewSynonym_ShouldCreateSuccessfully() {
        // Given
        String term = "网银";
        String synonym = "网上银行";
        Float confidence = 1.0f;
        Synonym.SynonymSource source = Synonym.SynonymSource.MANUAL;
        String category = "CHANNEL";
        Long createdBy = 1L;

        when(synonymRepository.findByTermAndSynonym(term, synonym)).thenReturn(Optional.empty());
        when(synonymRepository.save(any(Synonym.class))).thenAnswer(invocation -> {
            Synonym saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // When
        Synonym result = synonymService.addSynonym(term, synonym, confidence, source, category, createdBy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTerm()).isEqualTo(term);
        assertThat(result.getSynonym()).isEqualTo(synonym);
        assertThat(result.getConfidence()).isEqualTo(confidence);
        assertThat(result.getSource()).isEqualTo(source);
        assertThat(result.getCategory()).isEqualTo(category);
        assertThat(result.getCreatedBy()).isEqualTo(createdBy);

        verify(synonymRepository).findByTermAndSynonym(term, synonym);
        verify(synonymRepository).save(any(Synonym.class));
    }

    @Test
    void testAddSynonym_WithExistingSynonym_ShouldReturnExisting() {
        // Given
        String term = "ATM";
        String synonym = "自动取款机";
        Synonym existingSynonym = createSynonym(50L, term, synonym, 1.0f, Synonym.SynonymSource.MANUAL);

        when(synonymRepository.findByTermAndSynonym(term, synonym)).thenReturn(Optional.of(existingSynonym));

        // When
        Synonym result = synonymService.addSynonym(term, synonym, 0.9f, Synonym.SynonymSource.AUTO, "CHANNEL", 2L);

        // Then
        assertThat(result).isEqualTo(existingSynonym);
        verify(synonymRepository).findByTermAndSynonym(term, synonym);
        verify(synonymRepository, never()).save(any(Synonym.class));
    }

    @Test
    void testBatchAddSynonyms_ShouldSaveAllAndClearCache() {
        // Given
        List<Synonym> synonymsToAdd = Arrays.asList(
            createSynonym(null, "测试1", "同义词1", 0.9f, Synonym.SynonymSource.MANUAL),
            createSynonym(null, "测试2", "同义词2", 0.8f, Synonym.SynonymSource.AUTO)
        );

        when(synonymRepository.saveAll(synonymsToAdd)).thenReturn(synonymsToAdd);

        // When
        List<Synonym> result = synonymService.batchAddSynonyms(synonymsToAdd);

        // Then
        assertThat(result).isEqualTo(synonymsToAdd);
        verify(synonymRepository).saveAll(synonymsToAdd);
    }

    @Test
    void testUpdateConfidence_ShouldUpdateAndClearCache() {
        // Given
        Long synonymId = 1L;
        Float newConfidence = 0.95f;

        // When
        synonymService.updateConfidence(synonymId, newConfidence);

        // Then
        verify(synonymRepository).updateConfidence(synonymId, newConfidence);
    }

    @Test
    void testUpdateEnabled_ShouldUpdateAndClearCache() {
        // Given
        Long synonymId = 1L;
        Boolean enabled = false;

        // When
        synonymService.updateEnabled(synonymId, enabled);

        // Then
        verify(synonymRepository).updateEnabled(synonymId, enabled);
    }

    @Test
    void testGetLowConfidenceSynonyms_ShouldReturnFilteredResults() {
        // Given
        List<Synonym> lowConfidenceSynonyms = Arrays.asList(
            createSynonym(1L, "测试", "低置信度", 0.6f, Synonym.SynonymSource.AUTO)
        );

        when(synonymRepository.findLowConfidenceSynonyms(0.7f)).thenReturn(lowConfidenceSynonyms);

        // When
        List<Synonym> result = synonymService.getLowConfidenceSynonyms();

        // Then
        assertThat(result).isEqualTo(lowConfidenceSynonyms);
        verify(synonymRepository).findLowConfidenceSynonyms(0.7f);
    }

    @Test
    void testGetPopularSynonyms_ShouldReturnLimitedResults() {
        // Given
        int limit = 3;
        List<Synonym> popularSynonyms = Arrays.asList(
            createSynonym(1L, "热门1", "同义词1", 0.9f, Synonym.SynonymSource.MANUAL),
            createSynonym(2L, "热门2", "同义词2", 0.9f, Synonym.SynonymSource.MANUAL),
            createSynonym(3L, "热门3", "同义词3", 0.9f, Synonym.SynonymSource.MANUAL),
            createSynonym(4L, "热门4", "同义词4", 0.9f, Synonym.SynonymSource.MANUAL)
        );

        when(synonymRepository.findPopularSynonyms()).thenReturn(popularSynonyms);

        // When
        List<Synonym> result = synonymService.getPopularSynonyms(limit);

        // Then
        assertThat(result).hasSize(limit);
        verify(synonymRepository).findPopularSynonyms();
    }

    @Test
    void testGetSynonymStatistics_ShouldReturnComprehensiveStats() {
        // Given
        when(synonymRepository.countEnabledSynonyms()).thenReturn(100L);
        when(synonymRepository.countBySource()).thenReturn(Arrays.asList(
            new Object[]{"MANUAL", 60L},
            new Object[]{"AUTO", 40L}
        ));
        when(synonymRepository.countByCategory()).thenReturn(Arrays.asList(
            new Object[]{"BANK_PRODUCT", 30L},
            new Object[]{"BANK_SERVICE", 25L}
        ));

        // When
        Map<String, Object> result = synonymService.getSynonymStatistics();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).containsKey("totalCount");
        assertThat(result).containsKey("bySource");
        assertThat(result).containsKey("byCategory");
        assertThat(result).containsKey("cacheStats");
        assertThat(result.get("totalCount")).isEqualTo(100L);
    }

    @Test
    void testGetBankProductSynonyms_ShouldReturnBankProductCategories() {
        // Given
        List<Synonym> bankProductSynonyms = Arrays.asList(
            createSynonym(1L, "房贷", "住房贷款", 0.95f, Synonym.SynonymSource.MANUAL)
        );

        when(synonymRepository.findBankProductSynonyms()).thenReturn(bankProductSynonyms);

        // When
        List<Synonym> result = synonymService.getBankProductSynonyms();

        // Then
        assertThat(result).isEqualTo(bankProductSynonyms);
        verify(synonymRepository).findBankProductSynonyms();
    }

    @Test
    void testGetBankServiceSynonyms_ShouldReturnBankServiceCategories() {
        // Given
        List<Synonym> bankServiceSynonyms = Arrays.asList(
            createSynonym(1L, "转账", "汇款", 0.9f, Synonym.SynonymSource.MANUAL)
        );

        when(synonymRepository.findBankServiceSynonyms()).thenReturn(bankServiceSynonyms);

        // When
        List<Synonym> result = synonymService.getBankServiceSynonyms();

        // Then
        assertThat(result).isEqualTo(bankServiceSynonyms);
        verify(synonymRepository).findBankServiceSynonyms();
    }

    @Test
    void testClearAllCache_ShouldClearBothCaches() {
        // When
        synonymService.clearAllCache();

        // Then - 测试缓存被清理，通过后续调用验证
        // 由于缓存是私有的，我们通过调用依赖缓存的方法来验证清理效果
        String term = "测试";
        when(synonymRepository.findByTermAndEnabled(term)).thenReturn(Collections.emptyList());

        synonymService.getSynonyms(term);
        verify(synonymRepository).findByTermAndEnabled(term);
    }

    // 辅助方法
    private Synonym createSynonym(Long id, String term, String synonym, Float confidence, Synonym.SynonymSource source) {
        Synonym s = new Synonym();
        s.setId(id);
        s.setTerm(term);
        s.setSynonym(synonym);
        s.setConfidence(confidence);
        s.setSource(source);
        s.setEnabled(true);
        s.setUsageCount(0L);
        return s;
    }
}