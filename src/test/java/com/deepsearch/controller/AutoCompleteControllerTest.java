package com.deepsearch.controller;

import com.deepsearch.dto.Suggestion;
import com.deepsearch.dto.SuggestionType;
import com.deepsearch.dto.SearchSuggestionResponse;
import com.deepsearch.service.AutoCompleteService;
import com.deepsearch.service.SearchSuggestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AutoCompleteController集成测试
 */
@WebMvcTest(AutoCompleteController.class)
class AutoCompleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutoCompleteService autoCompleteService;

    @MockBean
    private SearchSuggestionService searchSuggestionService;

    private List<Suggestion> mockSuggestions;
    private SearchSuggestionResponse mockSearchSuggestionResponse;

    @BeforeEach
    void setUp() {
        // 准备模拟数据
        mockSuggestions = Arrays.asList(
                Suggestion.of("银行产品", SuggestionType.PREFIX_MATCH, 0.9f),
                Suggestion.of("银行服务", SuggestionType.POPULAR, 0.8f),
                Suggestion.of("银行卡申请", SuggestionType.PERSONALIZED, 1.0f)
        );

        mockSearchSuggestionResponse = new SearchSuggestionResponse();
        mockSearchSuggestionResponse.setOriginalQuery("银行");
        mockSearchSuggestionResponse.setExpansionSuggestions(Arrays.asList("银行产品介绍", "银行服务指南"));
        mockSearchSuggestionResponse.setRelatedQueries(Arrays.asList("理财产品", "贷款服务"));
        mockSearchSuggestionResponse.setTotalSuggestions(4);
        mockSearchSuggestionResponse.setResponseTimeMs(50L);
    }

    @Test
    void testGetSuggestionsSuccess() throws Exception {
        // 模拟服务调用
        when(autoCompleteService.getSuggestions(eq("银行"), isNull(), eq(10), eq(true)))
                .thenReturn(mockSuggestions);

        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "银行")
                        .param("limit", "10")
                        .param("includePersonalized", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].text").value("银行产品"))
                .andExpect(jsonPath("$.data[0].type").value("PREFIX_MATCH"))
                .andExpect(jsonPath("$.data[0].score").value(0.9))
                .andExpect(jsonPath("$.data[1].text").value("银行服务"))
                .andExpect(jsonPath("$.data[1].type").value("POPULAR"))
                .andExpect(jsonPath("$.data[2].text").value("银行卡申请"))
                .andExpect(jsonPath("$.data[2].type").value("PERSONALIZED"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetSuggestionsWithAuthenticatedUser() throws Exception {
        // 模拟认证用户的调用
        when(autoCompleteService.getSuggestions(eq("银行"), eq(1L), eq(5), eq(false)))
                .thenReturn(mockSuggestions.subList(0, 2));

        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "银行")
                        .param("limit", "5")
                        .param("includePersonalized", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void testGetSuggestionsWithDefaultParameters() throws Exception {
        // 测试默认参数
        when(autoCompleteService.getSuggestions(eq("test"), isNull(), eq(10), eq(true)))
                .thenReturn(mockSuggestions);

        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(autoCompleteService).getSuggestions("test", null, 10, true);
    }

    @Test
    void testGetSuggestionsWithMissingQuery() throws Exception {
        // 测试缺少查询参数
        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetSuggestionsServiceError() throws Exception {
        // 测试服务层错误
        when(autoCompleteService.getSuggestions(anyString(), any(), anyInt(), anyBoolean()))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "银行")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("自动补全请求失败")));
    }

    @Test
    void testGetSearchSuggestionsSuccess() throws Exception {
        // 测试搜索建议成功
        when(searchSuggestionService.generateSuggestions(eq("银行"), eq(5), eq(5)))
                .thenReturn(mockSearchSuggestionResponse);

        mockMvc.perform(get("/api/autocomplete/search-suggestions")
                        .param("query", "银行")
                        .param("currentResultCount", "5")
                        .param("limit", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalQuery").value("银行"))
                .andExpect(jsonPath("$.data.totalSuggestions").value(4))
                .andExpect(jsonPath("$.data.responseTimeMs").exists())
                .andExpect(jsonPath("$.data.expansionSuggestions").isArray())
                .andExpect(jsonPath("$.data.relatedQueries").isArray());
    }

    @Test
    void testGetSearchSuggestionsWithDefaultParameters() throws Exception {
        // 测试搜索建议默认参数
        when(searchSuggestionService.generateSuggestions(eq("test"), eq(0), eq(5)))
                .thenReturn(mockSearchSuggestionResponse);

        mockMvc.perform(get("/api/autocomplete/search-suggestions")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(searchSuggestionService).generateSuggestions("test", 0, 5);
    }

    @Test
    void testGetPopularQueriesSuccess() throws Exception {
        // 测试获取热门查询成功
        List<Suggestion> popularQueries = Arrays.asList(
                Suggestion.of("理财产品", SuggestionType.POPULAR, 0.9f),
                Suggestion.of("贷款申请", SuggestionType.POPULAR, 0.8f)
        );

        when(autoCompleteService.getPopularQueries(eq(10), eq(7)))
                .thenReturn(popularQueries);

        mockMvc.perform(get("/api/autocomplete/popular")
                        .param("limit", "10")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].text").value("理财产品"))
                .andExpect(jsonPath("$.data[0].type").value("POPULAR"))
                .andExpect(jsonPath("$.data[1].text").value("贷款申请"));
    }

    @Test
    void testGetPopularQueriesWithDefaultParameters() throws Exception {
        // 测试热门查询默认参数
        when(autoCompleteService.getPopularQueries(eq(10), eq(7)))
                .thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/autocomplete/popular")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(autoCompleteService).getPopularQueries(10, 7);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testRecordSelectionSuccess() throws Exception {
        // 测试记录选择成功
        doNothing().when(autoCompleteService)
                .recordSelection(eq("银行"), eq("银行产品"), eq("PREFIX_MATCH"), eq(1L));

        mockMvc.perform(post("/api/autocomplete/record-selection")
                        .param("originalQuery", "银行")
                        .param("selectedSuggestion", "银行产品")
                        .param("suggestionType", "PREFIX_MATCH")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("搜索选择记录成功"));

        verify(autoCompleteService).recordSelection("银行", "银行产品", "PREFIX_MATCH", 1L);
    }

    @Test
    void testRecordSelectionWithoutAuthentication() throws Exception {
        // 测试未认证用户记录选择
        doNothing().when(autoCompleteService)
                .recordSelection(eq("银行"), eq("银行产品"), eq("PREFIX_MATCH"), isNull());

        mockMvc.perform(post("/api/autocomplete/record-selection")
                        .param("originalQuery", "银行")
                        .param("selectedSuggestion", "银行产品")
                        .param("suggestionType", "PREFIX_MATCH")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(autoCompleteService).recordSelection("银行", "银行产品", "PREFIX_MATCH", null);
    }

    @Test
    void testRecordSelectionWithMissingParameters() throws Exception {
        // 测试缺少参数的记录选择
        mockMvc.perform(post("/api/autocomplete/record-selection")
                        .param("originalQuery", "银行")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecordSelectionServiceError() throws Exception {
        // 测试记录选择服务错误
        doThrow(new RuntimeException("Service error"))
                .when(autoCompleteService)
                .recordSelection(anyString(), anyString(), anyString(), any());

        mockMvc.perform(post("/api/autocomplete/record-selection")
                        .param("originalQuery", "银行")
                        .param("selectedSuggestion", "银行产品")
                        .param("suggestionType", "PREFIX_MATCH")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("搜索选择记录失败")));
    }

    @Test
    void testParameterValidation() throws Exception {
        // 测试参数验证 - 负数限制
        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "test")
                        .param("limit", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // 测试参数验证 - 过大的限制
        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "test")
                        .param("limit", "1000")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testResponseFormat() throws Exception {
        // 测试响应格式
        when(autoCompleteService.getSuggestions(anyString(), any(), anyInt(), anyBoolean()))
                .thenReturn(mockSuggestions);

        mockMvc.perform(get("/api/autocomplete/suggestions")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testConcurrentRequests() throws Exception {
        // 测试并发请求处理
        when(autoCompleteService.getSuggestions(anyString(), any(), anyInt(), anyBoolean()))
                .thenReturn(mockSuggestions);

        // 模拟多个并发请求
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/api/autocomplete/suggestions")
                                    .param("query", "test" + index)
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证服务被调用了5次
        verify(autoCompleteService, times(5))
                .getSuggestions(anyString(), any(), anyInt(), anyBoolean());
    }
}