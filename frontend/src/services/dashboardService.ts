import apiClient from './api'
import type { DashboardStats, ApiResponse } from '@/types'

export const dashboardService = {
  /**
   * Get dashboard statistics
   */
  async getStats(): Promise<DashboardStats> {
    const response = await apiClient.get<ApiResponse<DashboardStats>>('/v1/dashboard/stats')
    return response.data.data!
  },
  
  /**
   * Get recent activity
   */
  async getRecentActivity(): Promise<unknown[]> {
    const response = await apiClient.get<ApiResponse<unknown[]>>('/v1/dashboard/activity')
    return response.data.data!
  },
}
