import apiClient from './api'
import { useAuthStore } from '@/store/authStore'
import type { 
  LoginRequest, 
  AuthResponse, 
  ApiResponse,
  CreateUserRequest,
  User,
  Session
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
   * Logout user - revokes the refresh token server-side
   */
  async logout(): Promise<void> {
    const refreshToken = useAuthStore.getState().refreshToken
    try {
      await apiClient.post('/v1/auth/logout', { refreshToken })
    } catch {
      // Ignore errors on logout - we'll clear local state anyway
      console.warn('Logout API call failed, clearing local state')
    }
    // Always clear local state
    useAuthStore.getState().logout()
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

  // ==================== Session Management ====================

  /**
   * Get all active sessions for the current user
   */
  async getActiveSessions(): Promise<Session[]> {
    const response = await apiClient.get<ApiResponse<Session[]>>('/v1/auth/sessions')
    return response.data.data!
  },

  /**
   * Revoke a specific session (logout from that device)
   */
  async revokeSession(sessionId: number): Promise<void> {
    await apiClient.delete(`/v1/auth/sessions/${sessionId}`)
  },

  /**
   * Logout from all devices (revoke all sessions)
   */
  async logoutAllDevices(): Promise<{ sessionsRevoked: number }> {
    const response = await apiClient.post<ApiResponse<{ sessionsRevoked: number }>>('/v1/auth/logout-all')
    // Clear local state after logging out all devices
    useAuthStore.getState().logout()
    return response.data.data!
  },
}
