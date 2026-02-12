import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { invoiceService } from '@/services/invoiceService'
import { useAuthStore } from '@/store/authStore'
import type { Invoice, InvoiceStatus, UpdateInvoiceRequest } from '@/types'
import {
  X,
  ChevronLeft,
  ChevronRight,
  XCircle,
  Save,
  Download,
  Send,
  FileText,
  Building,
  DollarSign,
  Calendar,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Minimize2,
  AlertTriangle,
  Loader2,
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

interface InvoiceSidePanelProps {
  invoiceId: number
  /** IDs of all invoices in current list for prev/next navigation */
  invoiceIds: number[]
  onClose: () => void
  onNavigate: (id: number) => void
}

export function InvoiceSidePanel({
  invoiceId,
  invoiceIds,
  onClose,
  onNavigate,
}: InvoiceSidePanelProps) {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const [activeTab, setActiveTab] = useState<'summary' | 'lineItems' | 'attachments'>('summary')
  const [pdfZoom, setPdfZoom] = useState(100)
  const [pdfMaximized, setPdfMaximized] = useState(false)
  const [editMode, setEditMode] = useState(false)
  const [editData, setEditData] = useState<UpdateInvoiceRequest>({})
  const [rejectReason, setRejectReason] = useState('')
  const [showRejectDialog, setShowRejectDialog] = useState(false)

  const { data: invoice, isLoading } = useQuery({
    queryKey: ['invoice', invoiceId],
    queryFn: () => invoiceService.getInvoice(invoiceId),
    enabled: !!invoiceId,
  })

  // Reset state when invoice changes
  useEffect(() => {
    setActiveTab('summary')
    setEditMode(false)
    setEditData({})
    setShowRejectDialog(false)
    setRejectReason('')
  }, [invoiceId])

  // Populate edit data from invoice
  useEffect(() => {
    if (invoice && editMode) {
      setEditData({
        invoiceNumber: invoice.invoiceNumber,
        poNumber: invoice.poNumber,
        vendorName: invoice.vendorName,
        vendorAddress: invoice.vendorAddress,
        vendorEmail: invoice.vendorEmail,
        vendorPhone: invoice.vendorPhone,
        subtotal: invoice.subtotal,
        taxAmount: invoice.taxAmount,
        discountAmount: invoice.discountAmount,
        shippingAmount: invoice.shippingAmount,
        totalAmount: invoice.totalAmount,
        invoiceDate: invoice.invoiceDate,
        dueDate: invoice.dueDate || undefined,
        reviewNotes: invoice.reviewNotes || undefined,
        glAccount: invoice.glAccount || undefined,
        project: invoice.project || undefined,
        itemCategory: invoice.itemCategory || undefined,
        location: invoice.location || undefined,
        costCenter: invoice.costCenter || undefined,
      })
    }
  }, [invoice, editMode])

  const approveMutation = useMutation({
    mutationFn: (action: 'APPROVED' | 'REJECTED') =>
      invoiceService.submitApproval(invoiceId, {
        action,
        ...(action === 'REJECTED' ? { rejectionReason: rejectReason } : {}),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      setShowRejectDialog(false)
      setRejectReason('')
    },
  })

  const saveMutation = useMutation({
    mutationFn: (data: UpdateInvoiceRequest) =>
      invoiceService.updateInvoice(invoiceId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      setEditMode(false)
    },
  })

  const syncMutation = useMutation({
    mutationFn: () => invoiceService.syncToSage(invoiceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
    },
  })

  // Navigation
  const currentIdx = invoiceIds.indexOf(invoiceId)
  const hasPrev = currentIdx > 0
  const hasNext = currentIdx < invoiceIds.length - 1
  const goToPrev = () => hasPrev && onNavigate(invoiceIds[currentIdx - 1])
  const goToNext = () => hasNext && onNavigate(invoiceIds[currentIdx + 1])

  const canApprove = user?.role === 'ADMIN' || user?.role === 'APPROVER' || user?.role === 'SUPER_ADMIN'
  const canEdit = invoice?.isEditable !== false
  const canSync = (user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN') && invoice?.status === 'APPROVED'
  const isPending = invoice?.status === 'PENDING' || invoice?.status === 'UNDER_REVIEW'

  // Build PDF URL - use backend preview endpoint to avoid CORS issues with S3 presigned URLs
  const getPdfUrl = () => {
    if (!invoice?.s3Url) return null
    // Always use the backend preview endpoint (works for both local and S3 storage)
    const apiBase = import.meta.env.VITE_API_URL || '/api'
    const token = useAuthStore.getState().accessToken
    return `${apiBase}/v1/invoices/${invoiceId}/preview?token=${token}`
  }
  const pdfUrl = getPdfUrl()

  const formatCurrency = (amount?: number) => {
    if (amount == null) return '-'
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: invoice?.currency || 'USD',
    }).format(amount)
  }

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  const handleDownload = async () => {
    const blob = await invoiceService.downloadPdf(invoiceId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = invoice?.originalFileName || `invoice-${invoiceId}.pdf`
    a.click()
    window.URL.revokeObjectURL(url)
  }

  // ─── Full-screen PDF mode ────────────────────────────────────────────────
  if (pdfMaximized) {
    return (
      <div className="fixed inset-0 z-[60] bg-gray-900 flex flex-col">
        <div className="flex items-center justify-between px-4 py-3 bg-gray-800 text-white">
          <h2 className="font-semibold truncate">
            {invoice?.originalFileName || `Invoice #${invoice?.invoiceNumber}`}
          </h2>
          <button
            onClick={() => setPdfMaximized(false)}
            className="flex items-center px-3 py-1.5 bg-gray-700 hover:bg-gray-600 rounded-lg text-sm"
          >
            <Minimize2 className="w-4 h-4 mr-1" /> Exit Fullscreen
          </button>
        </div>
        <div className="flex-1">
          {pdfUrl ? (
            <iframe
              src={`${pdfUrl}#toolbar=1&navpanes=0`}
              className="w-full h-full"
              title="Invoice PDF"
            />
          ) : (
            <div className="flex items-center justify-center h-full text-gray-400">
              PDF not available
            </div>
          )}
        </div>
      </div>
    )
  }

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/40 z-40 transition-opacity"
        onClick={onClose}
      />

      {/* Side Panel */}
      <div className="fixed inset-y-0 right-0 z-50 w-full max-w-[90vw] xl:max-w-[85vw] bg-white shadow-2xl flex flex-col animate-slide-in">
        {/* ── Header ──────────────────────────────────────────────────── */}
        <div className="flex items-center justify-between px-5 py-3 border-b bg-gray-50 flex-shrink-0">
          {/* Left: Nav + Title */}
          <div className="flex items-center gap-3 min-w-0">
            {/* Prev / Next arrows */}
            <div className="flex items-center border rounded-lg overflow-hidden">
              <button
                onClick={goToPrev}
                disabled={!hasPrev}
                className="p-1.5 hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed"
                title="Previous invoice"
              >
                <ChevronLeft className="w-4 h-4" />
              </button>
              <button
                onClick={goToNext}
                disabled={!hasNext}
                className="p-1.5 hover:bg-gray-200 disabled:opacity-30 disabled:cursor-not-allowed border-l"
                title="Next invoice"
              >
                <ChevronRight className="w-4 h-4" />
              </button>
            </div>

            <h2 className="text-lg font-bold text-gray-900 truncate">Invoice</h2>

            {invoice && (
              <span
                className={`px-2.5 py-1 text-xs font-medium rounded-full border ${STATUS_COLORS[invoice.status]}`}
              >
                {invoice.status.replace('_', ' ')}
              </span>
            )}
          </div>

          {/* Right: Actions */}
          <div className="flex items-center gap-2 flex-shrink-0">
            {canApprove && isPending && (
              <>
                <button
                  onClick={() => setShowRejectDialog(true)}
                  disabled={approveMutation.isPending}
                  className="px-4 py-1.5 text-sm font-medium text-red-600 border border-red-300 rounded-lg hover:bg-red-50 disabled:opacity-50"
                >
                  Reject
                </button>
                <button
                  onClick={() => approveMutation.mutate('APPROVED')}
                  disabled={approveMutation.isPending}
                  className="px-4 py-1.5 text-sm font-medium text-green-700 border border-green-400 rounded-lg hover:bg-green-50 disabled:opacity-50"
                >
                  Approve
                </button>
              </>
            )}
            {canSync && (
              <button
                onClick={() => syncMutation.mutate()}
                disabled={syncMutation.isPending}
                className="px-4 py-1.5 text-sm font-medium text-purple-700 border border-purple-300 rounded-lg hover:bg-purple-50 disabled:opacity-50"
              >
                <Send className="w-3.5 h-3.5 inline mr-1" /> Sync
              </button>
            )}
            {canEdit && !editMode && (
              <button
                onClick={() => setEditMode(true)}
                className="px-4 py-1.5 text-sm font-medium text-primary-700 border border-primary-300 rounded-lg hover:bg-primary-50"
              >
                Edit
              </button>
            )}
            {editMode && (
              <>
                <button
                  onClick={() => setEditMode(false)}
                  className="px-4 py-1.5 text-sm text-gray-600 border rounded-lg hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={() => saveMutation.mutate(editData)}
                  disabled={saveMutation.isPending}
                  className="flex items-center px-4 py-1.5 text-sm font-medium bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                >
                  <Save className="w-3.5 h-3.5 mr-1" />
                  {saveMutation.isPending ? 'Saving...' : 'Save'}
                </button>
              </>
            )}
            <button
              onClick={handleDownload}
              className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg"
              title="Download PDF"
            >
              <Download className="w-4 h-4" />
            </button>
            <button
              onClick={onClose}
              className="p-2 text-gray-500 hover:bg-gray-100 rounded-lg"
              title="Close panel"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Subheader: filename + uploaded by */}
        {invoice && (
          <div className="px-5 py-2 text-sm text-gray-500 border-b bg-white flex-shrink-0">
            {invoice.originalFileName}
            {invoice.sourceEmailFrom && (
              <span className="ml-2">| Uploaded by {invoice.sourceEmailFrom}</span>
            )}
          </div>
        )}

        {/* ── Body ────────────────────────────────────────────────────── */}
        {isLoading ? (
          <div className="flex-1 flex items-center justify-center">
            <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
          </div>
        ) : !invoice ? (
          <div className="flex-1 flex items-center justify-center text-gray-400">
            Invoice not found
          </div>
        ) : (
          <div className="flex-1 flex overflow-hidden">
            {/* ── Left: Invoice Data ──────────────────────────────── */}
            <div className="w-[420px] xl:w-[480px] flex-shrink-0 border-r flex flex-col overflow-hidden">
              {/* Tabs */}
              <div className="flex border-b px-5 flex-shrink-0">
                {(['summary', 'lineItems', 'attachments'] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                      activeTab === tab
                        ? 'border-primary-600 text-primary-600'
                        : 'border-transparent text-gray-500 hover:text-gray-700'
                    }`}
                  >
                    {tab === 'summary'
                      ? 'Summary'
                      : tab === 'lineItems'
                      ? `Line Items (${invoice.lineItems?.length || 0})`
                      : 'Attachments (0)'}
                  </button>
                ))}
              </div>

              {/* Tab Content */}
              <div className="flex-1 overflow-y-auto p-5 space-y-5">
                {activeTab === 'summary' && (
                  <SummaryTab
                    invoice={invoice}
                    editMode={editMode}
                    editData={editData}
                    onChange={(field, value) =>
                      setEditData((d) => ({ ...d, [field]: value }))
                    }
                    formatCurrency={formatCurrency}
                    formatDate={formatDate}
                  />
                )}
                {activeTab === 'lineItems' && (
                  <LineItemsTab
                    invoice={invoice}
                    formatCurrency={formatCurrency}
                  />
                )}
                {activeTab === 'attachments' && (
                  <div className="text-center text-gray-400 py-12">
                    <FileText className="w-10 h-10 mx-auto mb-3 opacity-40" />
                    No attachments
                  </div>
                )}
              </div>
            </div>

            {/* ── Right: PDF Viewer ────────────────────────────── */}
            <div className="flex-1 flex flex-col bg-gray-100 min-w-0">
              {/* PDF toolbar */}
              <div className="flex items-center justify-between px-4 py-2 bg-gray-200 border-b flex-shrink-0">
                <span className="text-sm text-gray-600 truncate">
                  {invoice.originalFileName}
                </span>
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => setPdfZoom((z) => Math.max(50, z - 25))}
                    className="p-1 hover:bg-gray-300 rounded"
                  >
                    <ZoomOut className="w-4 h-4" />
                  </button>
                  <span className="text-xs text-gray-600 w-10 text-center">{pdfZoom}%</span>
                  <button
                    onClick={() => setPdfZoom((z) => Math.min(200, z + 25))}
                    className="p-1 hover:bg-gray-300 rounded"
                  >
                    <ZoomIn className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setPdfMaximized(true)}
                    className="p-1 hover:bg-gray-300 rounded ml-1"
                  >
                    <Maximize2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* PDF Frame */}
              <div className="flex-1 overflow-auto p-4">
                {pdfUrl ? (
                  <iframe
                    src={`${pdfUrl}#toolbar=0&navpanes=0&view=FitH`}
                    className="w-full bg-white shadow-lg rounded"
                    style={{
                      height: `${Math.max(600, 800 * (pdfZoom / 100))}px`,
                      transform: `scale(${pdfZoom / 100})`,
                      transformOrigin: 'top left',
                      width: `${100 / (pdfZoom / 100)}%`,
                    }}
                    title="Invoice PDF"
                  />
                ) : (
                  <div className="flex flex-col items-center justify-center h-full text-gray-400">
                    <FileText className="w-16 h-16 mb-4 opacity-30" />
                    <p>PDF not available</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* ── Reject Dialog ──────────────────────────────────────────── */}
        {showRejectDialog && (
          <>
            <div
              className="fixed inset-0 bg-black/30 z-[55]"
              onClick={() => setShowRejectDialog(false)}
            />
            <div className="fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[56] bg-white rounded-xl shadow-2xl w-[420px] p-6">
              <h3 className="text-lg font-semibold mb-3">Reject Invoice</h3>
              <p className="text-sm text-gray-500 mb-4">
                Provide a reason for rejecting invoice #{invoice?.invoiceNumber}.
              </p>
              <textarea
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="Reason for rejection..."
                className="w-full border rounded-lg p-3 text-sm h-24 resize-none focus:ring-2 focus:ring-red-500 focus:border-red-500"
              />
              <div className="flex justify-end gap-3 mt-4">
                <button
                  onClick={() => setShowRejectDialog(false)}
                  className="px-4 py-2 text-sm border rounded-lg hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  onClick={() => approveMutation.mutate('REJECTED')}
                  disabled={approveMutation.isPending}
                  className="flex items-center px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
                >
                  <XCircle className="w-4 h-4 mr-1" />
                  {approveMutation.isPending ? 'Rejecting...' : 'Reject'}
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </>
  )
}

// ─── Summary Tab ─────────────────────────────────────────────────────────────

function SummaryTab({
  invoice,
  editMode,
  editData,
  onChange,
  formatCurrency,
  formatDate,
}: {
  invoice: Invoice
  editMode: boolean
  editData: UpdateInvoiceRequest
  onChange: (field: string, value: string | number | undefined) => void
  formatCurrency: (n?: number) => string
  formatDate: (s?: string | null) => string
}) {
  return (
    <>
      {/* Confidence Banner */}
      {invoice.requiresManualReview && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 flex items-start gap-2">
          <AlertTriangle className="w-4 h-4 text-yellow-600 flex-shrink-0 mt-0.5" />
          <p className="text-sm text-yellow-700">Manual review required — some fields may have low confidence.</p>
        </div>
      )}

      {/* Vendor */}
      <section>
        <SectionTitle icon={Building} label="Vendor" />
        <div className="space-y-3 mt-2">
          <Field
            label="Vendor"
            value={invoice.vendorName}
            editMode={editMode}
            editValue={editData.vendorName}
            onChange={(v) => onChange('vendorName', v)}
            highlight
          />
          <Field
            label="Email"
            value={invoice.vendorEmail}
            editMode={editMode}
            editValue={editData.vendorEmail}
            onChange={(v) => onChange('vendorEmail', v)}
          />
          <Field
            label="Phone"
            value={invoice.vendorPhone}
            editMode={editMode}
            editValue={editData.vendorPhone}
            onChange={(v) => onChange('vendorPhone', v)}
          />
          <Field
            label="Address"
            value={invoice.vendorAddress}
            editMode={editMode}
            editValue={editData.vendorAddress}
            onChange={(v) => onChange('vendorAddress', v)}
          />
        </div>
      </section>

      {/* Invoice Identification */}
      <section>
        <SectionTitle icon={FileText} label="Invoice Details" />
        <div className="grid grid-cols-2 gap-3 mt-2">
          <Field
            label="Invoice Number"
            value={invoice.invoiceNumber}
            editMode={editMode}
            editValue={editData.invoiceNumber}
            onChange={(v) => onChange('invoiceNumber', v)}
            required
          />
          <Field
            label="PO Number"
            value={invoice.poNumber}
            editMode={editMode}
            editValue={editData.poNumber}
            onChange={(v) => onChange('poNumber', v)}
          />
          <Field
            label="Invoice Date"
            value={formatDate(invoice.invoiceDate)}
            editMode={editMode}
            editValue={editData.invoiceDate}
            onChange={(v) => onChange('invoiceDate', v)}
            inputType="date"
            required
          />
          <Field
            label="Payment Due Date"
            value={formatDate(invoice.dueDate)}
            editMode={editMode}
            editValue={editData.dueDate}
            onChange={(v) => onChange('dueDate', v)}
            inputType="date"
          />
        </div>
      </section>

      {/* Financials */}
      <section>
        <SectionTitle icon={DollarSign} label="Financial Summary" color="text-green-500" />
        <div className="space-y-2 mt-2">
          <FinancialRow
            label="Subtotal"
            value={invoice.subtotal}
            editMode={editMode}
            editValue={editData.subtotal}
            onChange={(v) => onChange('subtotal', v)}
            formatCurrency={formatCurrency}
          />
          <FinancialRow
            label="Tax"
            value={invoice.taxAmount}
            editMode={editMode}
            editValue={editData.taxAmount}
            onChange={(v) => onChange('taxAmount', v)}
            formatCurrency={formatCurrency}
          />
          {(invoice.discountAmount > 0 || editMode) && (
            <FinancialRow
              label="Discount"
              value={invoice.discountAmount}
              editMode={editMode}
              editValue={editData.discountAmount}
              onChange={(v) => onChange('discountAmount', v)}
              formatCurrency={formatCurrency}
              isDiscount
            />
          )}
          {(invoice.shippingAmount > 0 || editMode) && (
            <FinancialRow
              label="Shipping"
              value={invoice.shippingAmount}
              editMode={editMode}
              editValue={editData.shippingAmount}
              onChange={(v) => onChange('shippingAmount', v)}
              formatCurrency={formatCurrency}
            />
          )}
          <div className="flex justify-between items-center pt-2 mt-2 border-t-2 border-gray-200">
            <span className="font-semibold text-gray-900">Total Amount</span>
            {editMode ? (
              <input
                type="number"
                step="0.01"
                value={editData.totalAmount ?? ''}
                onChange={(e) =>
                  onChange('totalAmount', e.target.value ? Number(e.target.value) : undefined)
                }
                className="w-32 px-2 py-1 border rounded text-right font-bold text-primary-700"
              />
            ) : (
              <span className="text-lg font-bold text-primary-700">
                {formatCurrency(invoice.totalAmount)}
              </span>
            )}
          </div>
        </div>
      </section>

      {/* Allocation */}
      <section>
        <SectionTitle icon={Calendar} label="Allocation" color="text-indigo-500" />
        <div className="grid grid-cols-2 gap-3 mt-2">
          <Field
            label="GL Account"
            value={invoice.glAccount}
            editMode={editMode}
            editValue={editData.glAccount}
            onChange={(v) => onChange('glAccount', v)}
          />
          <Field
            label="Project"
            value={invoice.project}
            editMode={editMode}
            editValue={editData.project}
            onChange={(v) => onChange('project', v)}
          />
          <Field
            label="Item Category"
            value={invoice.itemCategory}
            editMode={editMode}
            editValue={editData.itemCategory}
            onChange={(v) => onChange('itemCategory', v)}
          />
          <Field
            label="Cost Center"
            value={invoice.costCenter}
            editMode={editMode}
            editValue={editData.costCenter}
            onChange={(v) => onChange('costCenter', v)}
          />
          <div className="col-span-2">
            <Field
              label="Location"
              value={invoice.location}
              editMode={editMode}
              editValue={editData.location}
              onChange={(v) => onChange('location', v)}
            />
          </div>
        </div>
      </section>

      {/* Review Notes */}
      <section>
        <SectionTitle icon={FileText} label="Review Notes" />
        {editMode ? (
          <textarea
            value={editData.reviewNotes || ''}
            onChange={(e) => onChange('reviewNotes', e.target.value)}
            placeholder="Add review notes..."
            className="w-full border rounded-lg p-3 text-sm h-20 resize-none mt-2"
          />
        ) : (
          <p className="text-sm text-gray-600 mt-2">{invoice.reviewNotes || 'No notes'}</p>
        )}
      </section>

      {/* Metadata */}
      <section className="pb-4">
        <SectionTitle icon={Calendar} label="Metadata" />
        <div className="grid grid-cols-2 gap-3 mt-2 text-sm">
          <div>
            <span className="text-gray-500 block">Extraction</span>
            <span className="font-medium capitalize">{invoice.extractionMethod || '-'}</span>
          </div>
          <div>
            <span className="text-gray-500 block">Confidence</span>
            <span className="font-medium">
              {invoice.confidenceScore ? `${(invoice.confidenceScore * 100).toFixed(0)}%` : '-'}
            </span>
          </div>
          <div>
            <span className="text-gray-500 block">Created</span>
            <span className="font-medium">{formatDate(invoice.createdAt)}</span>
          </div>
          <div>
            <span className="text-gray-500 block">Updated</span>
            <span className="font-medium">{formatDate(invoice.updatedAt)}</span>
          </div>
          {invoice.mappingProfileId && (
            <div className="col-span-2">
              <span className="text-gray-500 block">Mapping Profile</span>
              <span className="font-medium">{invoice.mappingProfileId}</span>
            </div>
          )}
        </div>
      </section>
    </>
  )
}

// ─── Line Items Tab ──────────────────────────────────────────────────────────

function LineItemsTab({
  invoice,
  formatCurrency,
}: {
  invoice: Invoice
  formatCurrency: (n?: number) => string
}) {
  if (!invoice.lineItems?.length) {
    return (
      <div className="text-center text-gray-400 py-12">
        <FileText className="w-10 h-10 mx-auto mb-3 opacity-40" />
        No line items
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {invoice.lineItems.map((item, idx) => (
        <div
          key={item.id || idx}
          className="bg-gray-50 border rounded-lg p-4"
        >
          <div className="flex justify-between items-start mb-2">
            <div>
              <span className="text-xs text-gray-400 font-medium">#{item.lineNumber}</span>
              {item.itemCode && (
                <span className="text-xs bg-gray-200 text-gray-600 px-1.5 py-0.5 rounded ml-2">
                  {item.itemCode}
                </span>
              )}
            </div>
            <span className="font-semibold text-gray-900">
              {formatCurrency(item.lineTotal)}
            </span>
          </div>
          <p className="text-sm text-gray-700 mb-2">{item.description || 'No description'}</p>
          <div className="flex gap-4 text-xs text-gray-500">
            <span>Qty: {item.quantity ?? '-'}</span>
            <span>Unit: {item.unitPrice != null ? formatCurrency(item.unitPrice) : '-'}</span>
            {item.glAccountCode && <span>GL: {item.glAccountCode}</span>}
            {item.costCenter && <span>CC: {item.costCenter}</span>}
          </div>
        </div>
      ))}
      <div className="flex justify-end pt-2 border-t">
        <span className="font-semibold text-gray-900">
          Total: {formatCurrency(invoice.totalAmount)}
        </span>
      </div>
    </div>
  )
}

// ─── Reusable pieces ────────────────────────────────────────────────────────

function SectionTitle({
  icon: Icon,
  label,
  color = 'text-primary-500',
}: {
  icon: React.ElementType
  label: string
  color?: string
}) {
  return (
    <h3 className="flex items-center text-sm font-semibold text-gray-900 uppercase tracking-wide">
      <Icon className={`w-4 h-4 mr-2 ${color}`} />
      {label}
    </h3>
  )
}

function Field({
  label,
  value,
  editMode = false,
  editValue,
  onChange,
  highlight = false,
  required = false,
  inputType = 'text',
}: {
  label: string
  value?: string | null
  editMode?: boolean
  editValue?: string
  onChange?: (v: string) => void
  highlight?: boolean
  required?: boolean
  inputType?: string
}) {
  if (editMode && onChange) {
    return (
      <div>
        <label className="block text-xs text-gray-500 mb-1">
          {label} {required && <span className="text-red-500">*</span>}
        </label>
        <input
          type={inputType}
          value={editValue ?? ''}
          onChange={(e) => onChange(e.target.value)}
          className="w-full px-2.5 py-1.5 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
        />
      </div>
    )
  }

  return (
    <div>
      <span className="block text-xs text-gray-500">{label}</span>
      <span
        className={`text-sm font-medium ${
          highlight ? 'text-primary-700' : value ? 'text-gray-900' : 'text-gray-400 italic'
        }`}
      >
        {value || 'Not extracted'}
      </span>
    </div>
  )
}

function FinancialRow({
  label,
  value,
  editMode,
  editValue,
  onChange,
  formatCurrency,
  isDiscount = false,
}: {
  label: string
  value?: number
  editMode: boolean
  editValue?: number
  onChange: (v: number | undefined) => void
  formatCurrency: (n?: number) => string
  isDiscount?: boolean
}) {
  return (
    <div className="flex justify-between items-center py-1">
      <span className="text-sm text-gray-600">{label}</span>
      {editMode ? (
        <input
          type="number"
          step="0.01"
          value={editValue ?? ''}
          onChange={(e) => onChange(e.target.value ? Number(e.target.value) : undefined)}
          className="w-32 px-2 py-1 border rounded text-right text-sm"
          placeholder="$ Enter..."
        />
      ) : (
        <span className={`text-sm font-medium ${isDiscount ? 'text-green-600' : ''}`}>
          {isDiscount && value ? '-' : ''}
          {formatCurrency(value)}
        </span>
      )}
    </div>
  )
}
