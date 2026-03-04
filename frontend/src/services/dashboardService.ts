import apiClient from './api'
import type { DashboardStats, ApiResponse } from '@/types'

export const dashboardService = {
  /**
   * Get dashboard statistics, optionally filtered by date range
   */
  async getStats(startDate?: string, endDate?: string): Promise<DashboardStats> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    const response = await apiClient.get<ApiResponse<DashboardStats>>('/v1/dashboard/stats', { params })
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
