import type { AxiosResponse } from 'axios'
import type {
  SearchResult,
  SearchSuggestion,
  SearchFilters,
  SearchParams,
  SearchResponse as TypeSearchResponse,
  SearchAnalytics,
  AutocompleteResponse
} from '@/types/search'

// Search API request types
export interface SearchRequest extends SearchParams {}

// Rename to avoid conflict
export interface APISearchResponse extends TypeSearchResponse {}

// Search API class with all required methods
class SearchAPI {
  async search(data: SearchRequest): Promise<AxiosResponse<APISearchResponse>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }

  async getSuggestions(query: string): Promise<AxiosResponse<AutocompleteResponse>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }

  async getPopularQueries(limit?: number): Promise<AxiosResponse<string[]>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }

  async recordSearchAnalytics(data: SearchAnalytics): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }

  async getSearchHistory(userId: string): Promise<AxiosResponse<string[]>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }

  async trackSearch(query: string, resultCount: number): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }

  async trackClick(resultId: string, query: string): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Search API not implemented')
  }
}

// Export singleton instance
export const searchApi = new SearchAPI()

export default searchApi