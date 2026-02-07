import apiClient from './api'
import type { 
  LoginRequest, 
  AuthResponse, 
  ApiResponse,
  CreateUserRequest,
  User
} from '@/types'

export const authService = {
  /**
   * Login user
   */
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/login', credentials)
    return response.data.data!
  },
  
  /**
   * Refresh access token
   */
  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/refresh', { refreshToken })
    return response.data.data!
  },
  
  /**
   * Logout user
   */
  async logout(): Promise<void> {
    await apiClient.post('/v1/auth/logout')
  },
  
  /**
   * Register first Super Admin (only works if no users exist)
   */
  async registerSuperAdmin(data: CreateUserRequest): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/register-super-admin', data)
    return response.data.data!
  },
  
  /**
   * Get current user info
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>('/v1/auth/me')
    return response.data.data!
  },
  
  /**
   * Request password reset
   */
  async forgotPassword(email: string): Promise<void> {
    await apiClient.post('/v1/auth/forgot-password', { email })
  },
  
  /**
   * Reset password with token
   */
  async resetPassword(token: string, newPassword: string): Promise<void> {
    await apiClient.post('/v1/auth/reset-password', { token, newPassword })
  },
  
  /**
   * Change password
   */
  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.post('/v1/auth/change-password', { currentPassword, newPassword })
  },
}
