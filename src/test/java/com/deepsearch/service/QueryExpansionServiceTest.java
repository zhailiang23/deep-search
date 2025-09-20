package com.deepsearch.service;

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
 * 查询扩展服务测试类
 */
@ExtendWith(MockitoExtension.class)
class QueryExpansionServiceTest {

    @Mock
    private SynonymService synonymService;

    @InjectMocks
    private QueryExpansionService queryExpansionService;

    @BeforeEach
    void setUp() {
        // 设置测试配置
        ReflectionTestUtils.setField(queryExpansionService, "maxExpansionTerms", 10);
        ReflectionTestUtils.setField(queryExpansionService, "minTermLength", 2);
        ReflectionTestUtils.setField(queryExpansionService, "confidenceThreshold", 0.7f);
        ReflectionTestUtils.setField(queryExpansionService, "enablePhoneticMatching", true);
    }

    @Test
    void testExpandQuery_WithBankProductQuery_ShouldIdentifyProductType() {
        // Given
        String query = "房贷利率查询";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(
            Set.of("房贷利率查询", "住房贷款利率查询", "按揭利率查询")
        );
        when(synonymService.getSynonyms("房贷")).thenReturn(Arrays.asList("住房贷款", "按揭贷款"));
        when(synonymService.getSynonyms("利率")).thenReturn(Arrays.asList("贷款利率", "年利率"));
        when(synonymService.getSynonyms("查询")).thenReturn(Arrays.asList("查看", "了解"));
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalQuery()).isEqualTo(query);
        assertThat(result.getQueryType()).isEqualTo(QueryExpansionService.QueryType.PRODUCT_QUERY);
        assertThat(result.getExpandedTerms()).isNotEmpty();
        assertThat(result.getAllTerms()).contains(query);
    }

    @Test
    void testExpandQuery_WithServiceQuery_ShouldIdentifyServiceType() {
        // Given
        String query = "如何转账到其他银行";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(
            Set.of("如何转账到其他银行", "怎么汇款到其他银行")
        );
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        assertThat(result.getQueryType()).isEqualTo(QueryExpansionService.QueryType.SERVICE_QUERY);
        assertThat(result.getExpandedTerms()).contains("汇款", "转钱", "付款", "跨行转账", "同行转账", "实时转账");
    }

    @Test
    void testExpandQuery_WithProcedureQuery_ShouldIdentifyProcedureType() {
        // Given
        String query = "如何办理信用卡";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(
            Set.of("如何办理信用卡", "怎么申请信用卡")
        );
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        assertThat(result.getQueryType()).isEqualTo(QueryExpansionService.QueryType.PROCEDURE_QUERY);
    }

    @Test
    void testExpandQuery_WithGeneralQuery_ShouldIdentifyGeneralType() {
        // Given
        String query = "银行营业时间";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(
            Set.of("银行营业时间", "银行上班时间")
        );
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        assertThat(result.getQueryType()).isEqualTo(QueryExpansionService.QueryType.GENERAL_QUERY);
    }

    @Test
    void testExpandQuery_WithNullOrEmptyQuery_ShouldReturnEmptyResult() {
        // When & Then
        QueryExpansionService.QueryExpansionResult nullResult =
            queryExpansionService.expandQuery(null, null);
        assertThat(nullResult.getOriginalQuery()).isNull();
        assertThat(nullResult.getExpandedTerms()).isEmpty();

        QueryExpansionService.QueryExpansionResult emptyResult =
            queryExpansionService.expandQuery("", null);
        assertThat(emptyResult.getOriginalQuery()).isEmpty();
        assertThat(emptyResult.getExpandedTerms()).isEmpty();

        QueryExpansionService.QueryExpansionResult spacesResult =
            queryExpansionService.expandQuery("   ", null);
        assertThat(spacesResult.getOriginalQuery()).isBlank();
        assertThat(spacesResult.getExpandedTerms()).isEmpty();

        verifyNoInteractions(synonymService);
    }

    @Test
    void testExpandQuery_WithBankingAbbreviations_ShouldExpandCorrectly() {
        // Given
        String query = "网银转账限额";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        Set<String> expandedTerms = result.getExpandedTerms();
        // 验证网银缩写扩展
        assertThat(expandedTerms).anyMatch(term -> term.contains("网上银行"));
        // 验证转账服务扩展
        assertThat(expandedTerms).anyMatch(term -> term.contains("汇款"));
    }

    @Test
    void testExpandQuery_WithChineseNumbers_ShouldConvertNumbers() {
        // Given
        String query = "一年期定期存款";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        Set<String> expandedTerms = result.getExpandedTerms();
        // 验证中文数字转换
        assertThat(expandedTerms).anyMatch(term -> term.contains("1年期"));
    }

    @Test
    void testExpandQuery_WithMaxTermsLimit_ShouldLimitResults() {
        // Given
        String query = "银行";
        Map<String, Object> contextHints = new HashMap<>();
        ReflectionTestUtils.setField(queryExpansionService, "maxExpansionTerms", 3);

        // 模拟返回大量同义词
        when(synonymService.expandQuery(query)).thenReturn(
            Set.of("银行", "金融机构", "银行业", "银行系统", "金融银行", "银行服务", "银行业务")
        );
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        assertThat(result.getExpandedTerms()).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void testExpandQuery_WithContextHints_ShouldConsiderContext() {
        // Given
        String query = "查询余额";
        Map<String, Object> contextHints = new HashMap<>();
        contextHints.put("spaceId", 1L);
        contextHints.put("channels", Arrays.asList("mobile", "web"));
        contextHints.put("searchHistory", Arrays.asList("账户查询", "余额查询"));

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getQueryType()).isEqualTo(QueryExpansionService.QueryType.SERVICE_QUERY);
        verify(synonymService).expandQuery(query);
    }

    @Test
    void testExpandQuery_WithLoanQuery_ShouldExpandLoanTerms() {
        // Given
        String query = "贷款申请";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        Set<String> expandedTerms = result.getExpandedTerms();
        // 验证贷款产品扩展
        assertThat(expandedTerms).anyMatch(term ->
            term.contains("个人贷款") || term.contains("企业贷款") || term.contains("消费贷")
        );
    }

    @Test
    void testExpandQuery_WithInsuranceQuery_ShouldExpandInsuranceTerms() {
        // Given
        String query = "保险产品";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        Set<String> expandedTerms = result.getExpandedTerms();
        // 验证保险产品扩展
        assertThat(expandedTerms).anyMatch(term ->
            term.contains("人寿保险") || term.contains("意外保险") || term.contains("健康保险")
        );
    }

    @Test
    void testExpandQuery_WithInvestmentQuery_ShouldExpandInvestmentTerms() {
        // Given
        String query = "投资理财";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        Set<String> expandedTerms = result.getExpandedTerms();
        // 验证理财产品扩展
        assertThat(expandedTerms).anyMatch(term ->
            term.contains("理财产品") || term.contains("财富管理") || term.contains("资产配置")
        );

        // 验证语义场扩展
        assertThat(expandedTerms).anyMatch(term ->
            term.contains("收益") || term.contains("风险") || term.contains("回报")
        );
    }

    @Test
    void testExpandQuery_WithSecurityQuery_ShouldExpandSecurityTerms() {
        // Given
        String query = "安全设置";
        Map<String, Object> contextHints = new HashMap<>();

        when(synonymService.expandQuery(query)).thenReturn(Set.of(query));
        when(synonymService.getSynonyms(anyString())).thenReturn(Collections.emptyList());
        when(synonymService.getBidirectionalSynonyms(anyString())).thenReturn(Collections.emptySet());

        // When
        QueryExpansionService.QueryExpansionResult result =
            queryExpansionService.expandQuery(query, contextHints);

        // Then
        Set<String> expandedTerms = result.getExpandedTerms();
        // 验证安全相关语义场扩展
        assertThat(expandedTerms).anyMatch(term ->
            term.contains("保障") || term.contains("风控") || term.contains("防护")
        );
    }

    @Test
    void testQueryExpansionResult_GetAllTerms_ShouldIncludeOriginalAndExpanded() {
        // Given
        String originalQuery = "测试查询";
        Set<String> expandedTerms = Set.of("扩展词1", "扩展词2");
        QueryExpansionService.QueryType queryType = QueryExpansionService.QueryType.GENERAL_QUERY;

        // When
        QueryExpansionService.QueryExpansionResult result =
            new QueryExpansionService.QueryExpansionResult(originalQuery, expandedTerms, queryType);

        // Then
        Set<String> allTerms = result.getAllTerms();
        assertThat(allTerms).contains(originalQuery);
        assertThat(allTerms).containsAll(expandedTerms);
        assertThat(allTerms).hasSize(expandedTerms.size() + 1);
    }

    @Test
    void testQueryExpansionResult_ToString_ShouldProvideReadableFormat() {
        // Given
        String originalQuery = "测试查询";
        Set<String> expandedTerms = Set.of("扩展词1", "扩展词2");
        QueryExpansionService.QueryType queryType = QueryExpansionService.QueryType.PRODUCT_QUERY;

        // When
        QueryExpansionService.QueryExpansionResult result =
            new QueryExpansionService.QueryExpansionResult(originalQuery, expandedTerms, queryType);

        // Then
        String toString = result.toString();
        assertThat(toString).contains(originalQuery);
        assertThat(toString).contains("2 terms");
        assertThat(toString).contains("PRODUCT_QUERY");
    }
}