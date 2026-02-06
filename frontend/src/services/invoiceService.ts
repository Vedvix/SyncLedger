import apiClient from './api'
import type { 
  Invoice, 
  UpdateInvoiceRequest,
  ApprovalRequest,
  Approval,
  InvoiceFilters,
  PaginationParams,
  ApiResponse, 
  PagedResponse 
} from '@/types'

export const invoiceService = {
  /**
   * Get paginated list of invoices
   */
  async getInvoices(
    filters?: InvoiceFilters,
    pagination?: PaginationParams
  ): Promise<PagedResponse<Invoice>> {
    const params = new URLSearchParams()
    
    if (pagination) {
      params.set('page', String(pagination.page))
      params.set('size', String(pagination.size))
      if (pagination.sort) {
        params.set('sort', `${pagination.sort},${pagination.direction || 'desc'}`)
      }
    }
    
    if (filters) {
      if (filters.search) params.set('search', filters.search)
      if (filters.status?.length) params.set('status', filters.status.join(','))
      if (filters.vendorName) params.set('vendorName', filters.vendorName)
      if (filters.dateFrom) params.set('dateFrom', filters.dateFrom)
      if (filters.dateTo) params.set('dateTo', filters.dateTo)
      if (filters.minAmount) params.set('minAmount', String(filters.minAmount))
      if (filters.maxAmount) params.set('maxAmount', String(filters.maxAmount))
    }
    
    const response = await apiClient.get<ApiResponse<PagedResponse<Invoice>>>(`/invoices?${params}`)
    return response.data.data!
  },
  
  /**
   * Get invoice by ID
   */
  async getInvoice(id: number): Promise<Invoice> {
    const response = await apiClient.get<ApiResponse<Invoice>>(`/invoices/${id}`)
    return response.data.data!
  },
  
  /**
   * Update invoice
   */
  async updateInvoice(id: number, data: UpdateInvoiceRequest): Promise<Invoice> {
    const response = await apiClient.put<ApiResponse<Invoice>>(`/invoices/${id}`, data)
    return response.data.data!
  },
  
  /**
   * Delete invoice
   */
  async deleteInvoice(id: number): Promise<void> {
    await apiClient.delete(`/invoices/${id}`)
  },
  
  /**
   * Approve or reject invoice
   */
  async submitApproval(id: number, approval: ApprovalRequest): Promise<Approval> {
    const response = await apiClient.post<ApiResponse<Approval>>(`/invoices/${id}/approve`, approval)
    return response.data.data!
  },
  
  /**
   * Get approval history for invoice
   */
  async getApprovalHistory(id: number): Promise<Approval[]> {
    const response = await apiClient.get<ApiResponse<Approval[]>>(`/invoices/${id}/approvals`)
    return response.data.data!
  },
  
  /**
   * Sync invoice to Sage
   */
  async syncToSage(id: number): Promise<void> {
    await apiClient.post(`/invoices/${id}/sync`)
  },
  
  /**
   * Download invoice PDF
   */
  async downloadPdf(id: number): Promise<Blob> {
    const response = await apiClient.get(`/invoices/${id}/download`, {
      responseType: 'blob',
    })
    return response.data
  },
  
  /**
   * Assign invoice to user
   */
  async assignInvoice(id: number, userId: number): Promise<Invoice> {
    const response = await apiClient.post<ApiResponse<Invoice>>(`/invoices/${id}/assign`, { userId })
    return response.data.data!
  },
}
