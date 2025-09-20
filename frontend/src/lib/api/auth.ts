import type { AxiosResponse } from 'axios'
import type { LoginCredentials, LoginResponse, User, PasswordChangeData, ProfileUpdateData } from '@/types/auth'

// Auth API class with all required methods
class AuthAPI {
  private authToken: string | null = null

  // Token management
  setAuthToken(token: string) {
    this.authToken = token
    // TODO: Set axios default authorization header
  }

  clearAuthToken() {
    this.authToken = null
    // TODO: Clear axios default authorization header
  }

  // API methods
  async login(credentials: LoginCredentials): Promise<AxiosResponse<LoginResponse>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async logout(): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async refreshToken(refreshToken: string): Promise<AxiosResponse<{ token: string; refreshToken: string; expiresIn: number }>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async getCurrentUser(): Promise<AxiosResponse<User>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async getUserProfile(): Promise<AxiosResponse<User>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async updateProfile(data: ProfileUpdateData): Promise<AxiosResponse<User>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async changePassword(data: PasswordChangeData): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async trackSearch(query: string, resultCount: number): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }

  async trackClick(resultId: string, query: string): Promise<AxiosResponse<void>> {
    // TODO: Implement actual API call
    throw new Error('Auth API not implemented')
  }
}

// Export singleton instance
export const authApi = new AuthAPI()

export default authApi