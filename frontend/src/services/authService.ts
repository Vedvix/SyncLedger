import apiClient from './api'
import type { 
  LoginRequest, 
  AuthResponse, 
  ApiResponse 
} from '@/types'

export const authService = {
  /**
   * Login user
   */
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', credentials)
    return response.data.data!
  },
  
  /**
   * Refresh access token
   */
  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken })
    return response.data.data!
  },
  
  /**
   * Logout user
   */
  async logout(): Promise<void> {
    await apiClient.post('/auth/logout')
  },
  
  /**
   * Request password reset
   */
  async forgotPassword(email: string): Promise<void> {
    await apiClient.post('/auth/forgot-password', { email })
  },
  
  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    await apiClient.post('/auth/reset-password', { token, newPassword })
  },
  
  /**
   * Change password
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.post('/auth/change-password', { currentPassword, newPassword })
  },
}
