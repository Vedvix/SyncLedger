import apiClient from './api'
import type { 
  ApiResponse, 
  PagedResponse,
  EmailPollingStatus,
  EmailLogDTO,
  EmailStatsDTO
} from '@/types'

/**
 * Service for Super Admin email polling operations
 */
export const emailService = {
  /**
   * Get email polling status
   */
  async getPollingStatus(): Promise<EmailPollingStatus> {
    const response = await apiClient.get<ApiResponse<EmailPollingStatus>>(
      '/v1/emails/status'
    )
    return response.data.data!
  },

  /**
   * Trigger email polling for all organizations
   */
  async triggerPollAll(): Promise<number> {
    const response = await apiClient.post<ApiResponse<number>>(
      '/v1/emails/poll'
    )
    return response.data.data!
  },

  /**
   * Trigger email polling for a specific organization
   */
  async triggerPollForOrganization(orgId: number): Promise<number> {
    const response = await apiClient.post<ApiResponse<number>>(
      `/v1/emails/poll/organization/${orgId}`
    )
    return response.data.data!
  },

  /**
   * Test email connection for an organization
   */
  async testEmailConnection(orgId: number): Promise<boolean> {
    const response = await apiClient.get<ApiResponse<boolean>>(
      `/v1/emails/test-connection/organization/${orgId}`
    )
    return response.data.data!
  },

  /**
   * Get all email logs (paginated)
   */
  async getEmailLogs(page = 0, size = 20, processed?: boolean, hasError?: boolean): Promise<PagedResponse<EmailLogDTO>> {
    let url = `/v1/emails/logs?page=${page}&size=${size}`
    if (processed !== undefined) url += `&processed=${processed}`
    if (hasError !== undefined) url += `&hasError=${hasError}`
    
    const response = await apiClient.get<ApiResponse<PagedResponse<EmailLogDTO>>>(url)
    return response.data.data!
  },

  /**
   * Get email stats
   */
  async getEmailStats(): Promise<EmailStatsDTO> {
    const response = await apiClient.get<ApiResponse<EmailStatsDTO>>(
      '/v1/emails/stats'
    )
    return response.data.data!
  },

  /**
   * Get recent emails
   */
  async getRecentEmails(): Promise<EmailLogDTO[]> {
    const response = await apiClient.get<ApiResponse<EmailLogDTO[]>>(
      '/v1/emails/recent'
    )
    return response.data.data!
  }
}
