import apiClient from './api'
import type {
  ApiResponse,
  PlatformAiUsageSummary,
  OrgAiUsageDetail
} from '@/types'

/**
 * Service for AI usage tracking (Super Admin only)
 */
export const aiUsageService = {
  /**
   * Get platform-wide AI usage summary with per-org breakdown
   */
  async getPlatformSummary(startDate?: string, endDate?: string): Promise<PlatformAiUsageSummary> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)
    const query = params.toString() ? `?${params.toString()}` : ''
    const response = await apiClient.get<ApiResponse<PlatformAiUsageSummary>>(
      `/v1/ai-usage/summary${query}`
    )
    return response.data.data!
  },

  /**
   * Get detailed usage for a specific organization
   */
  async getOrganizationUsage(orgId: number, startDate?: string, endDate?: string): Promise<OrgAiUsageDetail> {
    const params = new URLSearchParams()
    if (startDate) params.append('startDate', startDate)
    if (endDate) params.append('endDate', endDate)
    const query = params.toString() ? `?${params.toString()}` : ''
    const response = await apiClient.get<ApiResponse<OrgAiUsageDetail>>(
      `/v1/ai-usage/organizations/${orgId}${query}`
    )
    return response.data.data!
  }
}
