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
  DollarSign
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

export function InvoiceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  
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
  
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
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
        <span className={`px-4 py-2 rounded-lg border ${STATUS_COLORS[invoice.status]}`}>
          {invoice.status.replace('_', ' ')}
        </span>
      </div>
      
      {/* Action Buttons */}
      {(canApprove || canSync) && invoice.isEditable && (
        <div className="flex gap-4">
          {canApprove && invoice.status === 'PENDING' && (
            <>
              <button
                onClick={() => approveMutation.mutate('APPROVED')}
                disabled={approveMutation.isPending}
                className="flex items-center px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
              >
                <CheckCircle className="w-5 h-5 mr-2" />
                Approve
              </button>
              <button
                onClick={() => approveMutation.mutate('REJECTED')}
                disabled={approveMutation.isPending}
                className="flex items-center px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
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
              className="flex items-center px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50"
            >
              <Send className="w-5 h-5 mr-2" />
              Sync to Sage
            </button>
          )}
          <button
            onClick={() => invoiceService.downloadPdf(Number(id))}
            className="flex items-center px-6 py-2 border rounded-lg hover:bg-gray-50"
          >
            <Download className="w-5 h-5 mr-2" />
            Download PDF
          </button>
        </div>
      )}
      
      {/* Invoice Details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 space-y-6">
          {/* Basic Info */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <FileText className="w-5 h-5 mr-2 text-primary-500" />
              Invoice Details
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500">Invoice Number</p>
                <p className="font-medium">{invoice.invoiceNumber}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">PO Number</p>
                <p className="font-medium">{invoice.poNumber || '-'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Invoice Date</p>
                <p className="font-medium">{formatDate(invoice.invoiceDate)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Due Date</p>
                <p className="font-medium">{formatDate(invoice.dueDate)}</p>
              </div>
            </div>
          </div>
          
          {/* Vendor Info */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Building className="w-5 h-5 mr-2 text-primary-500" />
              Vendor Information
            </h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500">Name</p>
                <p className="font-medium">{invoice.vendorName}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Email</p>
                <p className="font-medium">{invoice.vendorEmail || '-'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Phone</p>
                <p className="font-medium">{invoice.vendorPhone || '-'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Address</p>
                <p className="font-medium">{invoice.vendorAddress || '-'}</p>
              </div>
            </div>
          </div>
          
          {/* Line Items */}
          {invoice.lineItems.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm p-6">
              <h2 className="text-lg font-semibold mb-4">Line Items</h2>
              <table className="w-full">
                <thead className="border-b">
                  <tr>
                    <th className="text-left py-2 text-sm text-gray-500">#</th>
                    <th className="text-left py-2 text-sm text-gray-500">Description</th>
                    <th className="text-right py-2 text-sm text-gray-500">Qty</th>
                    <th className="text-right py-2 text-sm text-gray-500">Unit Price</th>
                    <th className="text-right py-2 text-sm text-gray-500">Total</th>
                  </tr>
                </thead>
                <tbody>
                  {invoice.lineItems.map((item) => (
                    <tr key={item.lineNumber} className="border-b">
                      <td className="py-3">{item.lineNumber}</td>
                      <td className="py-3">{item.description}</td>
                      <td className="py-3 text-right">{item.quantity}</td>
                      <td className="py-3 text-right">{formatCurrency(item.unitPrice || 0)}</td>
                      <td className="py-3 text-right font-medium">{formatCurrency(item.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        
        {/* Sidebar */}
        <div className="space-y-6">
          {/* Financial Summary */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <DollarSign className="w-5 h-5 mr-2 text-green-500" />
              Financial Summary
            </h2>
            <div className="space-y-3">
              <div className="flex justify-between">
                <span className="text-gray-500">Subtotal</span>
                <span>{formatCurrency(invoice.subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Tax</span>
                <span>{formatCurrency(invoice.taxAmount)}</span>
              </div>
              {invoice.discountAmount > 0 && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Discount</span>
                  <span className="text-green-600">-{formatCurrency(invoice.discountAmount)}</span>
                </div>
              )}
              {invoice.shippingAmount > 0 && (
                <div className="flex justify-between">
                  <span className="text-gray-500">Shipping</span>
                  <span>{formatCurrency(invoice.shippingAmount)}</span>
                </div>
              )}
              <div className="flex justify-between pt-3 border-t font-semibold text-lg">
                <span>Total</span>
                <span>{formatCurrency(invoice.totalAmount)}</span>
              </div>
            </div>
          </div>
          
          {/* Metadata */}
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Calendar className="w-5 h-5 mr-2 text-primary-500" />
              Metadata
            </h2>
            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-500">Extraction Method</span>
                <span>{invoice.extractionMethod || '-'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Confidence Score</span>
                <span>{invoice.confidenceScore ? `${(invoice.confidenceScore * 100).toFixed(0)}%` : '-'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Created</span>
                <span>{formatDate(invoice.createdAt)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Last Updated</span>
                <span>{formatDate(invoice.updatedAt)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
