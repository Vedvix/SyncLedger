import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { invoiceService } from '@/services/invoiceService'
import { useAuthStore } from '@/store/authStore'
import type { InvoiceStatus } from '@/types'
import { 
  ArrowLeft, 
  RefreshCw, 
  CheckCircle, 
  XCircle, 
  Download,
  Send,
  FileText,
  Calendar,
  Building,
  DollarSign,
  Eye,
  EyeOff,
  ZoomIn,
  ZoomOut,
  Maximize2,
  AlertTriangle
} from 'lucide-react'

const STATUS_COLORS: Record<InvoiceStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  UNDER_REVIEW: 'bg-blue-100 text-blue-800 border-blue-300',
  APPROVED: 'bg-green-100 text-green-800 border-green-300',
  REJECTED: 'bg-red-100 text-red-800 border-red-300',
  SYNCED: 'bg-purple-100 text-purple-800 border-purple-300',
  SYNC_FAILED: 'bg-orange-100 text-orange-800 border-orange-300',
  ARCHIVED: 'bg-gray-100 text-gray-800 border-gray-300',
}

// Confidence level indicators
const getConfidenceColor = (score?: number) => {
  if (!score) return 'text-gray-400'
  if (score >= 0.9) return 'text-green-600'
  if (score >= 0.7) return 'text-yellow-600'
  return 'text-red-600'
}

const getConfidenceLabel = (score?: number) => {
  if (!score) return 'Unknown'
  if (score >= 0.9) return 'High'
  if (score >= 0.7) return 'Medium'
  return 'Low'
}

export function InvoiceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const [pdfZoom, setPdfZoom] = useState(100)
  const [showPdf, setShowPdf] = useState(true)
  const [pdfFullscreen, setPdfFullscreen] = useState(false)
  
  const { data: invoice, isLoading, error } = useQuery({
    queryKey: ['invoice', id],
    queryFn: () => invoiceService.getInvoice(Number(id)),
    enabled: !!id,
  })
  
  const approveMutation = useMutation({
    mutationFn: (action: 'APPROVED' | 'REJECTED') => 
      invoiceService.submitApproval(Number(id), { action }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] })
    },
  })
  
  const syncMutation = useMutation({
    mutationFn: () => invoiceService.syncToSage(Number(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', id] })
    },
  })
  
  const canApprove = user?.role === 'ADMIN' || user?.role === 'APPROVER'
  const canSync = user?.role === 'ADMIN' && invoice?.status === 'APPROVED'
  
  // Build PDF URL - use backend preview endpoint for both local and S3 storage
  const getPdfUrl = () => {
    if (!invoice?.s3Url) return null
    const apiBase = import.meta.env.VITE_API_URL || '/api'
    const token = useAuthStore.getState().accessToken
    return `${apiBase}/v1/invoices/${id}/preview?token=${token}`
  }
  
  const pdfUrl = getPdfUrl()
  
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: invoice?.currency || 'USD',
    }).format(amount)
  }
  
  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString()
  }
  
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <RefreshCw className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    )
  }
  
  if (error || !invoice) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <p className="text-red-600">Failed to load invoice details</p>
        <button
          onClick={() => navigate('/invoices')}
          className="mt-4 text-primary-600 hover:underline"
        >
          Back to Invoices
        </button>
      </div>
    )
  }

  // Fullscreen PDF viewer
  if (pdfFullscreen && pdfUrl) {
    return (
      <div className="fixed inset-0 z-50 bg-gray-900 flex flex-col">
        <div className="flex items-center justify-between p-4 bg-gray-800 text-white">
          <h2 className="font-semibold">Invoice #{invoice.invoiceNumber}</h2>
          <button 
            onClick={() => setPdfFullscreen(false)}
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 rounded-lg"
          >
            Exit Fullscreen
          </button>
        </div>
        <div className="flex-1">
          <iframe
            src={`${pdfUrl}#toolbar=1&navpanes=0`}
            className="w-full h-full"
            title="Invoice PDF"
          />
        </div>
      </div>
    )
  }
  
  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center">
          <button
            onClick={() => navigate('/invoices')}
            className="mr-4 p-2 hover:bg-gray-100 rounded-lg"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Invoice #{invoice.invoiceNumber}
            </h1>
            <p className="text-gray-500">{invoice.vendorName}</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <span className={`px-4 py-2 rounded-lg border ${STATUS_COLORS[invoice.status]}`}>
            {invoice.status.replace('_', ' ')}
          </span>
          {invoice.confidenceScore !== undefined && (
            <div className={`flex items-center gap-1 ${getConfidenceColor(invoice.confidenceScore)}`}>
              <AlertTriangle className="w-4 h-4" />
              <span className="text-sm font-medium">
                {getConfidenceLabel(invoice.confidenceScore)} confidence
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex gap-4 mb-4">
        {canApprove && (invoice.status === 'PENDING' || invoice.status === 'UNDER_REVIEW') && (
          <>
            <button
              onClick={() => approveMutation.mutate('APPROVED')}
              disabled={approveMutation.isPending}
              className="flex items-center px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
            >
              <CheckCircle className="w-5 h-5 mr-2" />
              Approve
            </button>
            <button
              onClick={() => approveMutation.mutate('REJECTED')}
              disabled={approveMutation.isPending}
              className="flex items-center px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
            >
              <XCircle className="w-5 h-5 mr-2" />
              Reject
            </button>
          </>
        )}
        {canSync && (
          <button
            onClick={() => syncMutation.mutate()}
            disabled={syncMutation.isPending}
            className="flex items-center px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50"
          >
            <Send className="w-5 h-5 mr-2" />
            Sync to Sage
          </button>
        )}
        <button
          onClick={async () => {
            const blob = await invoiceService.downloadPdf(Number(id))
            const url = window.URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = invoice.originalFileName || `invoice-${invoice.invoiceNumber}.pdf`
            a.click()
            window.URL.revokeObjectURL(url)
          }}
          className="flex items-center px-4 py-2 border rounded-lg hover:bg-gray-50"
        >
          <Download className="w-5 h-5 mr-2" />
          Download
        </button>
        <button
          onClick={() => setShowPdf(!showPdf)}
          className="flex items-center px-4 py-2 border rounded-lg hover:bg-gray-50"
        >
          {showPdf ? <EyeOff className="w-5 h-5 mr-2" /> : <Eye className="w-5 h-5 mr-2" />}
          {showPdf ? 'Hide PDF' : 'Show PDF'}
        </button>
      </div>
      
      {/* Split View: PDF | Extracted Data */}
      <div className={`flex-1 grid gap-6 ${showPdf ? 'grid-cols-1 lg:grid-cols-2' : 'grid-cols-1'}`} style={{ minHeight: 0 }}>
        
        {/* Left Side: PDF Viewer */}
        {showPdf && (
          <div className="bg-white rounded-xl shadow-sm flex flex-col overflow-hidden" style={{ minHeight: '600px' }}>
            <div className="flex items-center justify-between px-4 py-3 border-b bg-gray-50">
              <h2 className="font-semibold text-gray-700 flex items-center">
                <FileText className="w-5 h-5 mr-2" />
                Original Invoice
              </h2>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPdfZoom(Math.max(50, pdfZoom - 25))}
                  className="p-1 hover:bg-gray-200 rounded"
                  title="Zoom Out"
                >
                  <ZoomOut className="w-4 h-4" />
                </button>
                <span className="text-sm text-gray-500 w-12 text-center">{pdfZoom}%</span>
                <button
                  onClick={() => setPdfZoom(Math.min(200, pdfZoom + 25))}
                  className="p-1 hover:bg-gray-200 rounded"
                  title="Zoom In"
                >
                  <ZoomIn className="w-4 h-4" />
                </button>
                {pdfUrl && (
                  <button
                    onClick={() => setPdfFullscreen(true)}
                    className="p-1 hover:bg-gray-200 rounded ml-2"
                    title="Fullscreen"
                  >
                    <Maximize2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            </div>
            <div className="flex-1 overflow-auto bg-gray-100 p-4">
              {pdfUrl ? (
                <iframe
                  src={`${pdfUrl}#toolbar=0&navpanes=0&view=FitH`}
                  className="w-full bg-white shadow-lg rounded"
                  style={{ 
                    height: `${600 * (pdfZoom / 100)}px`,
                    minHeight: '500px',
                    transform: `scale(${pdfZoom / 100})`,
                    transformOrigin: 'top left',
                    width: `${100 / (pdfZoom / 100)}%`
                  }}
                  title="Invoice PDF"
                />
              ) : (
                <div className="flex flex-col items-center justify-center h-full text-gray-500">
                  <FileText className="w-16 h-16 mb-4 opacity-30" />
                  <p>PDF not available</p>
                  <p className="text-sm text-gray-400 mt-1">The invoice file could not be loaded</p>
                </div>
              )}
            </div>
          </div>
        )}
        
        {/* Right Side: Extracted Values */}
        <div className="space-y-4 overflow-auto" style={{ minHeight: showPdf ? '600px' : 'auto' }}>
          {/* Confidence Banner */}
          {invoice.requiresManualReview && (
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-yellow-800">Manual Review Required</p>
                <p className="text-sm text-yellow-700 mt-1">
                  Some fields may have low confidence scores. Please verify the extracted data against the PDF.
                </p>
              </div>
            </div>
          )}

          {/* Invoice Details */}
          <div className="bg-white rounded-xl shadow-sm p-5">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <FileText className="w-5 h-5 mr-2 text-primary-500" />
              Invoice Details
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <ExtractedField label="Invoice Number" value={invoice.invoiceNumber} />
              <ExtractedField label="PO Number" value={invoice.poNumber} />
              <ExtractedField label="Invoice Date" value={formatDate(invoice.invoiceDate)} />
              <ExtractedField label="Due Date" value={formatDate(invoice.dueDate)} />
            </div>
          </div>
          
          {/* Vendor Information */}
          <div className="bg-white rounded-xl shadow-sm p-5">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Building className="w-5 h-5 mr-2 text-primary-500" />
              Vendor Information
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <ExtractedField label="Name" value={invoice.vendorName} highlight />
              <ExtractedField label="Email" value={invoice.vendorEmail} />
              <ExtractedField label="Phone" value={invoice.vendorPhone} />
              <ExtractedField label="Tax ID" value={invoice.vendorTaxId} />
              <div className="col-span-2">
                <ExtractedField label="Address" value={invoice.vendorAddress} />
              </div>
            </div>
          </div>
          
          {/* Financial Summary */}
          <div className="bg-white rounded-xl shadow-sm p-5">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <DollarSign className="w-5 h-5 mr-2 text-green-500" />
              Financial Summary
            </h2>
            <div className="space-y-3">
              <div className="flex justify-between items-center py-2 border-b">
                <span className="text-gray-600">Subtotal</span>
                <span className="font-medium">{formatCurrency(invoice.subtotal)}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b">
                <span className="text-gray-600">Tax Amount</span>
                <span className="font-medium">{formatCurrency(invoice.taxAmount)}</span>
              </div>
              {invoice.discountAmount > 0 && (
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="text-gray-600">Discount</span>
                  <span className="font-medium text-green-600">-{formatCurrency(invoice.discountAmount)}</span>
                </div>
              )}
              {invoice.shippingAmount > 0 && (
                <div className="flex justify-between items-center py-2 border-b">
                  <span className="text-gray-600">Shipping</span>
                  <span className="font-medium">{formatCurrency(invoice.shippingAmount)}</span>
                </div>
              )}
              <div className="flex justify-between items-center py-3 bg-gray-50 -mx-5 px-5 mt-4 rounded-b-xl">
                <span className="text-lg font-semibold">Total Amount</span>
                <span className="text-xl font-bold text-primary-600">{formatCurrency(invoice.totalAmount)}</span>
              </div>
            </div>
          </div>
          
          {/* Line Items */}
          <div className="bg-white rounded-xl shadow-sm p-5">
            <h2 className="text-lg font-semibold mb-4">Line Items ({invoice.lineItems.length})</h2>
            {invoice.lineItems.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="border-b bg-gray-50">
                    <tr>
                      <th className="text-left py-2 px-2 text-sm text-gray-500">#</th>
                      <th className="text-left py-2 px-2 text-sm text-gray-500">Description</th>
                      <th className="text-right py-2 px-2 text-sm text-gray-500">Qty</th>
                      <th className="text-right py-2 px-2 text-sm text-gray-500">Unit Price</th>
                      <th className="text-right py-2 px-2 text-sm text-gray-500">GL Code</th>
                      <th className="text-right py-2 px-2 text-sm text-gray-500">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoice.lineItems.map((item) => (
                      <tr key={item.lineNumber} className="border-b hover:bg-gray-50">
                        <td className="py-2 px-2 text-sm">{item.lineNumber}</td>
                        <td className="py-2 px-2 text-sm">{item.description || '-'}</td>
                        <td className="py-2 px-2 text-sm text-right">{item.quantity || '-'}</td>
                        <td className="py-2 px-2 text-sm text-right">{item.unitPrice ? formatCurrency(item.unitPrice) : '-'}</td>
                        <td className="py-2 px-2 text-sm text-right">{item.glAccountCode || '-'}</td>
                        <td className="py-2 px-2 text-sm text-right font-medium">{formatCurrency(item.lineTotal)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-sm text-gray-400 italic">No line items extracted</p>
            )}
          </div>

          {/* Allocation */}
          <div className="bg-white rounded-xl shadow-sm p-5">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <FileText className="w-5 h-5 mr-2 text-indigo-500" />
              Allocation
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <ExtractedField label="GL Account" value={invoice.glAccount} highlight />
              <ExtractedField label="Project / Opportunity" value={invoice.project} />
              <ExtractedField label="Item Category" value={invoice.itemCategory} />
              <ExtractedField label="Cost Center" value={invoice.costCenter} />
              <div className="col-span-2">
                <ExtractedField label="Location" value={invoice.location} />
              </div>
            </div>
            {invoice.mappingProfileId && (
              <div className="mt-3 pt-3 border-t text-xs text-gray-500">
                Profile: <span className="font-medium text-gray-700">{invoice.mappingProfileId}</span>
              </div>
            )}
          </div>
          
          {/* Source & Metadata */}
          <div className="bg-white rounded-xl shadow-sm p-5">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Calendar className="w-5 h-5 mr-2 text-primary-500" />
              Source & Metadata
            </h2>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <p className="text-gray-500">Extraction Method</p>
                <p className="font-medium capitalize">{invoice.extractionMethod || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Confidence Score</p>
                <p className={`font-medium ${getConfidenceColor(invoice.confidenceScore)}`}>
                  {invoice.confidenceScore ? `${(invoice.confidenceScore * 100).toFixed(0)}%` : '-'}
                </p>
              </div>
              <div>
                <p className="text-gray-500">Source Email</p>
                <p className="font-medium truncate">{invoice.sourceEmailFrom || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Email Subject</p>
                <p className="font-medium truncate">{invoice.sourceEmailSubject || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Original File</p>
                <p className="font-medium truncate">{invoice.originalFileName}</p>
              </div>
              <div>
                <p className="text-gray-500">Pages</p>
                <p className="font-medium">{invoice.pageCount || '-'}</p>
              </div>
              <div>
                <p className="text-gray-500">Created</p>
                <p className="font-medium">{formatDate(invoice.createdAt)}</p>
              </div>
              <div>
                <p className="text-gray-500">Last Updated</p>
                <p className="font-medium">{formatDate(invoice.updatedAt)}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// Helper component for extracted fields with visual treatment
function ExtractedField({ 
  label, 
  value, 
  highlight = false 
}: { 
  label: string
  value?: string | null
  highlight?: boolean 
}) {
  const hasValue = value && value !== '-'
  
  return (
    <div className={`${highlight ? 'bg-primary-50 -mx-2 px-2 py-1 rounded' : ''}`}>
      <p className="text-sm text-gray-500">{label}</p>
      <p className={`font-medium ${!hasValue ? 'text-gray-400 italic' : ''} ${highlight ? 'text-primary-700' : ''}`}>
        {hasValue ? value : 'Not extracted'}
      </p>
    </div>
  )
}
