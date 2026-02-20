import apiClient from './api'
import type { 
  Invoice, 
  UpdateInvoiceRequest,
  ApprovalRequest,
  Approval,
  InvoiceFilters,
  PaginationParams,
  ApiResponse, 
  PagedResponse,
  ExportRequest
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
    
    const response = await apiClient.get<ApiResponse<PagedResponse<Invoice>>>(`/v1/invoices?${params}`)
    return response.data.data!
  },
  
  /**
   * Get invoice by ID
   */
  async getInvoice(id: number): Promise<Invoice> {
    const response = await apiClient.get<ApiResponse<Invoice>>(`/v1/invoices/${id}`)
    return response.data.data!
  },
  
  /**
   * Update invoice
   */
  async updateInvoice(id: number, data: UpdateInvoiceRequest): Promise<Invoice> {
    const response = await apiClient.put<ApiResponse<Invoice>>(`/v1/invoices/${id}`, data)
    return response.data.data!
  },
  
  /**
   * Delete invoice
   */
  async deleteInvoice(id: number): Promise<void> {
    await apiClient.delete(`/v1/invoices/${id}`)
  },
  
  /**
   * Approve or reject invoice
   */
  async submitApproval(id: number, approval: ApprovalRequest): Promise<Approval> {
    const response = await apiClient.post<ApiResponse<Approval>>(`/v1/invoices/${id}/approve`, approval)
    return response.data.data!
  },
  
  /**
   * Get approval history for invoice
   */
  async getApprovalHistory(id: number): Promise<Approval[]> {
    const response = await apiClient.get<ApiResponse<Approval[]>>(`/v1/invoices/${id}/approvals`)
    return response.data.data!
  },
  
  /**
   * Sync invoice to Sage
   */
  async syncToSage(id: number): Promise<void> {
    await apiClient.post(`/v1/invoices/${id}/sync`)
  },
  
  /**
   * Download invoice PDF
   */
  async downloadPdf(id: number): Promise<Blob> {
    const response = await apiClient.get(`/v1/invoices/${id}/download`, {
      responseType: 'blob',
    })
    return response.data
  },
  
  /**
   * Assign invoice to user
   */
  async assignInvoice(id: number, userId: number): Promise<Invoice> {
    const response = await apiClient.post<ApiResponse<Invoice>>(`/v1/invoices/${id}/assign`, { userId })
    return response.data.data!
  },

  /**
   * Upload a PDF invoice for extraction and processing
   */
  async uploadInvoice(file: File): Promise<Invoice> {
    const formData = new FormData()
    formData.append('file', file)
    const response = await apiClient.post<ApiResponse<Invoice>>('/v1/invoices/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data.data!
  },

  // ==================== Export ====================

  /**
   * Export invoices to Excel with advanced filtering
   */
  async exportToExcel(request: ExportRequest): Promise<void> {
    const response = await apiClient.post('/v1/invoices/export', request, {
      responseType: 'blob',
      timeout: 120000, // 2 min timeout for large exports
    })

    // Extract filename from Content-Disposition header or use default
    const contentDisposition = response.headers['content-disposition']
    let filename = 'SyncLedger_Invoices.xlsx'
    if (contentDisposition) {
      const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
      if (match?.[1]) {
        filename = match[1].replace(/['"]/g, '')
      }
    }

    // Create download link
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  },

  /**
   * Get available export columns
   */
  async getExportColumns(): Promise<Record<string, string>> {
    const response = await apiClient.get<ApiResponse<Record<string, string>>>('/v1/invoices/export/columns')
    return response.data.data!
  },
}
