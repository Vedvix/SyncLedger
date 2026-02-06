import apiClient from './api'
import type { 
  User, 
  CreateUserRequest,
  UpdateUserRequest,
  ApiResponse, 
  PagedResponse,
  PaginationParams
} from '@/types'

export const userService = {
  /**
   * Get paginated list of users (Admin only)
   */
  async getUsers(
    search?: string,
    pagination?: PaginationParams
  ): Promise<PagedResponse<User>> {
    const params = new URLSearchParams()
    
    if (pagination) {
      params.set('page', String(pagination.page))
      params.set('size', String(pagination.size))
      if (pagination.sort) {
        params.set('sort', `${pagination.sort},${pagination.direction || 'asc'}`)
      }
    }
    
    if (search) {
      params.set('search', search)
    }
    
    const response = await apiClient.get<ApiResponse<PagedResponse<User>>>(`/users?${params}`)
    return response.data.data!
  },
  
  /**
   * Get user by ID
   */
  async getUser(id: number): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>(`/users/${id}`)
    return response.data.data!
  },
  
  /**
   * Create new user (Admin only)
   */
  async createUser(data: CreateUserRequest): Promise<User> {
    const response = await apiClient.post<ApiResponse<User>>('/users', data)
    return response.data.data!
  },
  
  /**
   * Update user
   */
  async updateUser(id: number, data: UpdateUserRequest): Promise<User> {
    const response = await apiClient.put<ApiResponse<User>>(`/users/${id}`, data)
    return response.data.data!
  },
  
  /**
   * Delete/deactivate user (Admin only)
   */
  async deleteUser(id: number): Promise<void> {
    await apiClient.delete(`/users/${id}`)
  },
  
  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>('/users/me')
    return response.data.data!
  },
  
  /**
   * Update current user profile
   */
  async updateProfile(data: Partial<User>): Promise<User> {
    const response = await apiClient.put<ApiResponse<User>>('/users/me', data)
    return response.data.data!
  },
  
  /**
   * Get all approvers (for assignment dropdown)
   */
  async getApprovers(): Promise<User[]> {
    const response = await apiClient.get<ApiResponse<User[]>>('/users/approvers')
    return response.data.data!
  },
}
