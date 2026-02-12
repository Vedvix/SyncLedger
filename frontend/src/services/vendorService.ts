import apiClient from './api'
import type {
  Vendor,
  VendorRequest,
  VendorAnalytics,
  VendorSummary,
  ApiResponse,
  PagedResponse,
  PaginationParams
} from '@/types'

export const vendorService = {
  /**
   * Get paginated list of vendors
   */
  async getVendors(
    search?: string,
    pagination?: PaginationParams
  ): Promise<PagedResponse<Vendor>> {
    const params = new URLSearchParams()

    if (pagination) {
      params.set('page', String(pagination.page))
      params.set('size', String(pagination.size))
      if (pagination.sort) {
        params.set('sort', `${pagination.sort},${pagination.direction || 'desc'}`)
      }
    }

    if (search) params.set('search', search)

    const response = await apiClient.get<ApiResponse<PagedResponse<Vendor>>>(`/v1/vendors?${params}`)
    return response.data.data!
  },

  /**
   * Get vendor by ID (with analytics)
   */
  async getVendor(id: number): Promise<Vendor> {
    const response = await apiClient.get<ApiResponse<Vendor>>(`/v1/vendors/${id}`)
    return response.data.data!
  },

  /**
   * Create a new vendor
   */
  async createVendor(data: VendorRequest): Promise<Vendor> {
    const response = await apiClient.post<ApiResponse<Vendor>>('/v1/vendors', data)
    return response.data.data!
  },

  /**
   * Update a vendor
   */
  async updateVendor(id: number, data: VendorRequest): Promise<Vendor> {
    const response = await apiClient.put<ApiResponse<Vendor>>(`/v1/vendors/${id}`, data)
    return response.data.data!
  },

  /**
   * Delete a vendor
   */
  async deleteVendor(id: number): Promise<void> {
    await apiClient.delete(`/v1/vendors/${id}`)
  },

  /**
   * Get analytics for a specific vendor
   */
  async getVendorAnalytics(id: number): Promise<VendorAnalytics> {
    const response = await apiClient.get<ApiResponse<VendorAnalytics>>(`/v1/vendors/${id}/analytics`)
    return response.data.data!
  },

  /**
   * Get organization-wide vendor summary
   */
  async getVendorSummary(): Promise<VendorSummary> {
    const response = await apiClient.get<ApiResponse<VendorSummary>>('/v1/vendors/summary')
    return response.data.data!
  },
}
