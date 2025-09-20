---
created: 2025-09-20T02:51:13Z
last_updated: 2025-09-20T02:51:13Z
version: 1.0
author: Claude Code PM System
---

# Project Style Guide

## Code Style and Standards

### Java/Spring Boot Backend Standards

#### Naming Conventions
```java
// Classes: PascalCase
public class SearchService implements SearchServiceInterface

// Methods: camelCase, descriptive verbs
public List<SearchResult> performSemanticSearch(String query)
public void updateSearchIndex(Long resourceId)

// Variables: camelCase, descriptive nouns
private final VectorProcessor vectorProcessor;
private final ElasticsearchTemplate elasticsearchTemplate;

// Constants: UPPER_SNAKE_CASE
public static final String DEFAULT_SEARCH_INDEX = "deep_search_resources";
public static final int MAX_SEARCH_RESULTS = 100;

// Packages: lowercase, domain-driven
com.bank.search.service
com.bank.search.controller
com.bank.search.model
com.bank.search.repository
```

#### Code Structure
```java
// Service class example
@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final VectorService vectorService;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request, String userId) {
        // 1. Validate input
        validateSearchRequest(request);

        // 2. Check permissions
        List<String> allowedSpaces = permissionService.getAllowedSpaces(userId);

        // 3. Perform search
        List<SearchResult> results = performSearch(request, allowedSpaces);

        // 4. Log search activity
        logSearchActivity(request, userId, results.size());

        return SearchResponse.builder()
            .results(results)
            .total(results.size())
            .query(request.getQuery())
            .build();
    }

    // Private methods for internal logic
    private void validateSearchRequest(SearchRequest request) {
        if (StringUtils.isBlank(request.getQuery())) {
            throw new InvalidSearchException("Search query cannot be empty");
        }
    }
}
```

#### Error Handling
```java
// Custom exceptions with meaningful names
public class SearchEngineException extends RuntimeException {
    public SearchEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SearchEngineException.class)
    public ResponseEntity<ErrorResponse> handleSearchException(SearchEngineException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.builder()
                .error("SEARCH_ENGINE_ERROR")
                .message(e.getMessage())
                .timestamp(Instant.now())
                .build());
    }
}
```

### Vue.js/TypeScript Frontend Standards

#### Component Structure
```vue
<template>
  <!-- Use semantic HTML and clear structure -->
  <div class="search-page">
    <header class="search-header">
      <h1 class="sr-only">智能搜索</h1>
      <SearchBox
        v-model="searchQuery"
        :loading="isSearching"
        @search="handleSearch"
      />
    </header>

    <main class="search-content">
      <SearchResults
        :results="searchResults"
        :total="totalResults"
        @result-click="handleResultClick"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
// Use Composition API with TypeScript
import { ref, computed, onMounted } from 'vue'
import { useSearchStore } from '@/stores/search'
import type { SearchResult, SearchQuery } from '@/types/search'

// Props and emits with TypeScript
interface Props {
  initialQuery?: string
}

const props = withDefaults(defineProps<Props>(), {
  initialQuery: ''
})

// Store and state
const searchStore = useSearchStore()
const searchQuery = ref<string>(props.initialQuery)
const isSearching = ref<boolean>(false)

// Computed properties
const searchResults = computed((): SearchResult[] => searchStore.results)
const totalResults = computed((): number => searchStore.total)

// Methods
const handleSearch = async (query: SearchQuery): Promise<void> => {
  try {
    isSearching.value = true
    await searchStore.performSearch(query)
  } catch (error) {
    console.error('Search failed:', error)
    // Handle error appropriately
  } finally {
    isSearching.value = false
  }
}

const handleResultClick = (result: SearchResult): void => {
  searchStore.logResultClick(result.id)
  // Navigate or perform action
}

// Lifecycle
onMounted(() => {
  if (props.initialQuery) {
    handleSearch({ query: props.initialQuery })
  }
})
</script>

<style scoped>
/* Use Tailwind CSS classes primarily */
.search-page {
  @apply min-h-screen bg-gray-50;
}

.search-header {
  @apply bg-white shadow-sm border-b border-gray-200;
}

.search-content {
  @apply container mx-auto px-4 py-6;
}

/* Custom styles only when necessary */
.search-results-enter-active,
.search-results-leave-active {
  transition: opacity 0.3s ease;
}

.search-results-enter-from,
.search-results-leave-to {
  opacity: 0;
}
</style>
```

#### TypeScript Types
```typescript
// types/search.ts
export interface SearchQuery {
  query: string
  filters?: SearchFilters
  page?: number
  size?: number
}

export interface SearchFilters {
  categories?: string[]
  channels?: string[]
  dateRange?: DateRange
}

export interface SearchResult {
  id: string
  title: string
  content: string
  category: string
  relevanceScore: number
  metadata: Record<string, unknown>
  createdAt: string
  updatedAt: string
}

export interface SearchResponse {
  results: SearchResult[]
  total: number
  page: number
  size: number
  query: string
  took: number
}

// Store types
export interface SearchStore {
  results: SearchResult[]
  total: number
  isLoading: boolean
  error: string | null
  history: SearchQuery[]
}
```

#### Store Pattern (Pinia)
```typescript
// stores/search.ts
import { defineStore } from 'pinia'
import type { SearchQuery, SearchResult, SearchResponse } from '@/types/search'
import { searchApi } from '@/api/search'

export const useSearchStore = defineStore('search', {
  state: () => ({
    results: [] as SearchResult[],
    total: 0,
    isLoading: false,
    error: null as string | null,
    history: [] as SearchQuery[]
  }),

  getters: {
    hasResults: (state): boolean => state.results.length > 0,
    hasError: (state): boolean => state.error !== null,
    recentSearches: (state): SearchQuery[] => state.history.slice(0, 10)
  },

  actions: {
    async performSearch(query: SearchQuery): Promise<void> {
      this.isLoading = true
      this.error = null

      try {
        const response: SearchResponse = await searchApi.search(query)
        this.results = response.results
        this.total = response.total
        this.addToHistory(query)
      } catch (error) {
        this.error = error instanceof Error ? error.message : 'Search failed'
        console.error('Search error:', error)
      } finally {
        this.isLoading = false
      }
    },

    addToHistory(query: SearchQuery): void {
      const exists = this.history.some(h => h.query === query.query)
      if (!exists) {
        this.history.unshift(query)
        if (this.history.length > 50) {
          this.history = this.history.slice(0, 50)
        }
      }
    },

    clearResults(): void {
      this.results = []
      this.total = 0
      this.error = null
    }
  }
})
```

## API Design Standards

### RESTful API Design
```
Base URL: /api/v1

Search Endpoints:
GET    /api/v1/search                     # Search resources
GET    /api/v1/search/suggestions         # Search suggestions
GET    /api/v1/search/history            # Search history

Data Management:
POST   /api/v1/data/upload               # Upload JSON data
GET    /api/v1/data/sources              # List data sources
PUT    /api/v1/data/sources/{id}         # Update data source
DELETE /api/v1/data/sources/{id}         # Delete data source

Permission Management:
GET    /api/v1/permissions               # Get user permissions
POST   /api/v1/permissions/matrix        # Update permission matrix
GET    /api/v1/permissions/users         # List users

Analytics:
GET    /api/v1/analytics/search          # Search analytics
GET    /api/v1/analytics/usage           # Usage statistics
POST   /api/v1/analytics/export          # Export reports
```

### Request/Response Format
```json
// Standard API Response Format
{
  "success": true,
  "data": {
    "results": [...],
    "total": 156,
    "page": 1,
    "size": 20
  },
  "meta": {
    "took": 45,
    "timestamp": "2025-09-20T02:51:13Z",
    "version": "v1"
  }
}

// Error Response Format
{
  "success": false,
  "error": {
    "code": "INVALID_SEARCH_QUERY",
    "message": "Search query cannot be empty",
    "details": {
      "field": "query",
      "rejectedValue": ""
    }
  },
  "meta": {
    "timestamp": "2025-09-20T02:51:13Z",
    "requestId": "req_12345"
  }
}
```

## Database Design Standards

### Table Naming Conventions
```sql
-- Tables: lowercase with underscores
search_resources
user_permissions
search_analytics
permission_matrices

-- Columns: lowercase with underscores
user_id
created_at
updated_at
is_active

-- Foreign keys: table_name + "_id"
user_id
resource_id
permission_id

-- Indexes: idx_ + table_name + column_names
idx_search_resources_category
idx_user_permissions_user_id_space_id
```

### Schema Design Patterns
```sql
-- Standard columns for all tables
CREATE TABLE search_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(500) NOT NULL,
    content TEXT,
    category VARCHAR(100),

    -- Metadata JSON for flexible fields
    metadata JSON,

    -- Search optimization
    search_vector BLOB,

    -- Standard audit fields
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,

    -- Indexes
    INDEX idx_search_resources_category (category),
    INDEX idx_search_resources_created_at (created_at),
    FULLTEXT INDEX ft_search_resources_content (title, content)
);
```

## Testing Standards

### Unit Testing (Java)
```java
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private VectorService vectorService;

    @InjectMocks
    private SearchService searchService;

    @Test
    @DisplayName("Should return search results when query is valid")
    void shouldReturnSearchResultsWhenQueryIsValid() {
        // Given
        String query = "房贷";
        String userId = "user123";
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .build();

        List<SearchResult> expectedResults = List.of(
            createSearchResult("1", "住房贷款产品介绍"),
            createSearchResult("2", "房贷申请流程")
        );

        when(searchRepository.findBySemanticQuery(query, any()))
            .thenReturn(expectedResults);

        // When
        SearchResponse response = searchService.search(request, userId);

        // Then
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getQuery()).isEqualTo(query);
        assertThat(response.getResults().get(0).getTitle())
            .contains("住房贷款");

        verify(searchRepository).findBySemanticQuery(query, any());
    }

    private SearchResult createSearchResult(String id, String title) {
        return SearchResult.builder()
            .id(id)
            .title(title)
            .relevanceScore(0.95)
            .build();
    }
}
```

### Component Testing (Vue)
```typescript
// tests/components/SearchBox.test.ts
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import SearchBox from '@/components/SearchBox.vue'

describe('SearchBox', () => {
  it('emits search event when form is submitted', async () => {
    // Given
    const wrapper = mount(SearchBox)
    const searchInput = wrapper.find('input[type="search"]')
    const searchForm = wrapper.find('form')

    // When
    await searchInput.setValue('房贷')
    await searchForm.trigger('submit')

    // Then
    expect(wrapper.emitted()).toHaveProperty('search')
    expect(wrapper.emitted('search')[0]).toEqual([{
      query: '房贷',
      filters: {}
    }])
  })

  it('shows loading state when loading prop is true', () => {
    // Given
    const wrapper = mount(SearchBox, {
      props: { loading: true }
    })

    // Then
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
  })
})
```

## Documentation Standards

### Code Documentation
```java
/**
 * 执行语义搜索并返回相关结果
 *
 * @param request 包含搜索查询和过滤条件的请求对象
 * @param userId 执行搜索的用户ID，用于权限验证
 * @return 包含搜索结果和元数据的响应对象
 * @throws InvalidSearchException 当搜索查询为空或无效时
 * @throws PermissionDeniedException 当用户没有搜索权限时
 */
@Transactional(readOnly = true)
public SearchResponse search(SearchRequest request, String userId) {
    // Implementation
}
```

### API Documentation
```yaml
# OpenAPI 3.0 documentation example
/api/v1/search:
  get:
    summary: 执行语义搜索
    description: |
      基于用户查询进行语义搜索，返回相关的搜索结果。
      支持同义词匹配和上下文理解。
    parameters:
      - name: q
        in: query
        required: true
        description: 搜索查询字符串
        schema:
          type: string
          example: "房贷"
      - name: page
        in: query
        description: 页码（从1开始）
        schema:
          type: integer
          default: 1
          minimum: 1
    responses:
      200:
        description: 搜索成功
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SearchResponse'
      400:
        description: 请求参数无效
      401:
        description: 未授权访问
      500:
        description: 服务器内部错误
```

## Git Workflow Standards

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

#### Types
- `feat`: 新功能
- `fix`: 错误修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建工具或辅助工具的变动

#### Examples
```
feat(search): 添加语义搜索功能

实现基于向量的语义搜索，支持同义词匹配：
- 集成sentence-transformers模型
- 添加向量相似度计算
- 优化搜索结果排序算法

Closes #10

fix(permission): 修复权限缓存失效问题

修复Redis缓存在权限更新后不及时失效的问题：
- 添加权限更新事件监听
- 实现缓存自动失效机制
- 增加权限缓存一致性测试

Fixes #23

docs(api): 更新搜索API文档

更新搜索API的OpenAPI文档，添加新的参数说明和示例
```

### Branch Naming
```
# Feature branches
feature/semantic-search-integration
feature/permission-matrix-ui
feature/json-data-upload

# Bug fix branches
fix/search-performance-issue
fix/permission-cache-invalidation

# Release branches
release/v1.0.0
release/v1.1.0

# Hotfix branches
hotfix/critical-search-bug
hotfix/security-vulnerability
```

## Code Review Standards

### Review Checklist
- [ ] 代码符合项目编码规范
- [ ] 功能正确实现且满足需求
- [ ] 包含充分的单元测试
- [ ] 错误处理完善
- [ ] 性能考虑充分
- [ ] 安全性检查通过
- [ ] 文档更新完整
- [ ] 无代码重复
- [ ] 变量和方法命名清晰
- [ ] 注释适当且有用

### Review Comments Format
```
// 建议类型的评论
💡 建议：这里可以使用Optional.ofNullable()来避免空指针异常

// 必须修改的问题
❌ 问题：这个SQL查询存在注入风险，必须使用参数化查询

// 优化建议
⚡ 性能：这个循环可能有性能问题，建议使用Stream API优化

// 表扬优秀的代码
👍 好的：这个错误处理很完善，考虑了各种边界情况
```