import apiClient from './api'
import type { 
  ApiResponse, 
  PagedResponse,
  Organization,
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
  OrganizationStats,
  PlatformStats
} from '@/types'

/**
 * Service for Super Admin organization management
 */
export const organizationService = {
  /**
   * Get all organizations (Super Admin only)
   */
  async getOrganizations(page = 0, size = 20): Promise<PagedResponse<Organization>> {
    const response = await apiClient.get<ApiResponse<PagedResponse<Organization>>>(
      `/v1/super-admin/organizations?page=${page}&size=${size}`
    )
    return response.data.data!
  },

  /**
   * Get organization by ID
   */
  async getOrganization(id: number): Promise<Organization> {
    const response = await apiClient.get<ApiResponse<Organization>>(
      `/v1/super-admin/organizations/${id}`
    )
    return response.data.data!
  },

  /**
   * Get organization by slug
   */
  async getOrganizationBySlug(slug: string): Promise<Organization> {
    const response = await apiClient.get<ApiResponse<Organization>>(
      `/v1/super-admin/organizations/slug/${slug}`
    )
    return response.data.data!
  },

  /**
   * Create new organization
   */
  async createOrganization(data: CreateOrganizationRequest): Promise<Organization> {
    const response = await apiClient.post<ApiResponse<Organization>>(
      '/v1/super-admin/organizations',
      data
    )
    return response.data.data!
  },

  /**
   * Update organization
   */
  async updateOrganization(id: number, data: UpdateOrganizationRequest): Promise<Organization> {
    const response = await apiClient.put<ApiResponse<Organization>>(
      `/v1/super-admin/organizations/${id}`,
      data
    )
    return response.data.data!
  },

  /**
   * Activate organization
   */
  async activateOrganization(id: number): Promise<Organization> {
    const response = await apiClient.post<ApiResponse<Organization>>(
      `/v1/super-admin/organizations/${id}/activate`
    )
    return response.data.data!
  },

  /**
   * Suspend organization
   */
  async suspendOrganization(id: number): Promise<Organization> {
    const response = await apiClient.post<ApiResponse<Organization>>(
      `/v1/super-admin/organizations/${id}/suspend`
    )
    return response.data.data!
  },

  /**
   * Get organization statistics
   */
  async getOrganizationStats(id: number): Promise<OrganizationStats> {
    const response = await apiClient.get<ApiResponse<OrganizationStats>>(
      `/v1/super-admin/organizations/${id}/stats`
    )
    return response.data.data!
  },

  /**
   * Get platform-wide statistics
   */
  async getPlatformStats(): Promise<PlatformStats> {
    const response = await apiClient.get<ApiResponse<PlatformStats>>(
      '/v1/super-admin/dashboard/stats'
    )
    return response.data.data!
  },
}
