import { useState, useMemo, useRef, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { invoiceService } from '@/services/invoiceService'
import { useAuthStore } from '@/store/authStore'
import type { InvoiceStatus, InvoiceFilters } from '@/types'
import { InvoiceSidePanel } from '@/components/InvoiceSidePanel'
import { ExportPanel } from '@/components/ExportPanel'
import {
  Search,
  RefreshCw,
  FileText,
  ChevronLeft,
  ChevronRight,
  Calendar,
  Building,
  Shield,
  Upload,
  Loader2,
  Download,
  X,
} from 'lucide-react'

// ─── Status tab configuration matching the reference UI ─────────────────────

interface StatusTab {
  label: string
  statuses: InvoiceStatus[]
}

const STATUS_TABS: StatusTab[] = [
  { label: 'Pending Review', statuses: ['PENDING', 'UNDER_REVIEW'] },
  { label: 'Rejected', statuses: ['REJECTED'] },
  { label: 'Approved', statuses: ['APPROVED'] },
  { label: 'Sync Failed', statuses: ['SYNC_FAILED'] },
  { label: 'Completed', statuses: ['SYNCED'] },
  { label: 'Archived', statuses: ['ARCHIVED'] },
  { label: 'All', statuses: [] },
]

const STATUS_BADGE: Record<InvoiceStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700 border-yellow-300',
  UNDER_REVIEW: 'bg-blue-100 text-blue-700 border-blue-300',
  APPROVED: 'bg-green-100 text-green-700 border-green-300',
  REJECTED: 'bg-red-100 text-red-700 border-red-300',
  SYNCED: 'bg-purple-100 text-purple-700 border-purple-300',
  SYNC_FAILED: 'bg-orange-100 text-orange-700 border-orange-300',
  ARCHIVED: 'bg-gray-100 text-gray-700 border-gray-300',
}

const STATUS_LABEL: Record<InvoiceStatus, string> = {
  PENDING: 'Pending Review',
  UNDER_REVIEW: 'Under Review',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  SYNCED: 'Completed',
  SYNC_FAILED: 'Failure',
  ARCHIVED: 'Archived',
}

const PAGE_SIZE = 10

export function InvoicesPage() {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [searchParams, setSearchParams] = useSearchParams()
  const [page, setPage] = useState(0)
  const [activeTab, setActiveTab] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<number | null>(null)
  const [dateRange, setDateRange] = useState<{ from?: string; to?: string }>({})
  const [exportPanelOpen, setExportPanelOpen] = useState(false)
  const [vendorFilter, setVendorFilter] = useState('')

  // Sync URL search params to component state
  useEffect(() => {
    const statusParam = searchParams.get('status')
    const vendorParam = searchParams.get('vendorName')
    const overdueParam = searchParams.get('overdue')

    if (!statusParam && !vendorParam && !overdueParam) return

    if (statusParam) {
      const idx = STATUS_TABS.findIndex((tab) =>
        tab.statuses.includes(statusParam as InvoiceStatus)
      )
      if (idx >= 0) {
        setActiveTab(idx)
      }
    }

    if (vendorParam) {
      setVendorFilter(vendorParam)
    }

    setPage(0)
    // Clear URL params after applying to avoid stale state on re-renders
    setSearchParams({}, { replace: true })
  }, [searchParams, setSearchParams])

  const uploadMutation = useMutation({
    mutationFn: (file: File) => invoiceService.uploadInvoice(file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
    },
  })

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (files) {
      Array.from(files).forEach((file) => {
        if (file.type === 'application/pdf') {
          uploadMutation.mutate(file)
        }
      })
    }
    // Reset input so the same file can be re-selected
    if (fileInputRef.current) fileInputRef.current.value = ''
  }
  const currentTab = STATUS_TABS[activeTab]

  const filters: InvoiceFilters = useMemo(
    () => ({
      search: appliedSearch || undefined,
      status: currentTab.statuses.length > 0 ? currentTab.statuses : undefined,
      vendorName: vendorFilter || undefined,
      dateFrom: dateRange.from || undefined,
      dateTo: dateRange.to || undefined,
    }),
    [appliedSearch, currentTab, vendorFilter, dateRange]
  )

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['invoices', page, filters],
    queryFn: () =>
      invoiceService.getInvoices(filters, {
        page,
        size: PAGE_SIZE,
        sort: 'createdAt',
        direction: 'desc',
      }),
  })

  const invoiceIds = useMemo(
    () => data?.content.map((inv) => inv.id) || [],
    [data]
  )

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setAppliedSearch(searchQuery)
    setPage(0)
  }

  const handleTabChange = (idx: number) => {
    setActiveTab(idx)
    setPage(0)
    setSelectedIds(new Set())
  }

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }

  const toggleSelectAll = () => {
    if (!data) return
    if (selectedIds.size === data.content.length) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(new Set(data.content.map((i) => i.id)))
    }
  }

  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)

  const formatDateTime = (dateStr: string) => {
    const d = new Date(dateStr)
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }) + ' @ ' + d.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      hour12: true,
    })
  }

  const formatDate = (dateStr?: string | null) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  }

  // Pagination helpers
  const totalPages = data ? data.totalPages : 0
  const showingFrom = data && data.totalElements > 0 ? page * PAGE_SIZE + 1 : 0
  const showingTo = data ? Math.min((page + 1) * PAGE_SIZE, data.totalElements) : 0

  // Build page numbers for pagination
  const pageNumbers = useMemo(() => {
    if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i)
    const pages: (number | 'ellipsis')[] = [0, 1, 2]
    if (page > 3) pages.push('ellipsis')
    const mid = Math.max(3, Math.min(page, totalPages - 4))
    if (mid > 2 && mid < totalPages - 3) pages.push(mid)
    if (page < totalPages - 4) pages.push('ellipsis')
    pages.push(totalPages - 3, totalPages - 2, totalPages - 1)
    // Deduplicate
    const unique: (number | 'ellipsis')[] = []
    for (const p of pages) {
      if (p === 'ellipsis') {
        if (unique[unique.length - 1] !== 'ellipsis') unique.push(p)
      } else if (!unique.includes(p)) {
        unique.push(p)
      }
    }
    return unique
  }, [totalPages, page])

  return (
    <div className="h-full flex flex-col">
      {/* Organization scope banner */}
      {user?.role === 'SUPER_ADMIN' ? (
        <div className="flex items-center px-4 py-2 mb-4 bg-indigo-50 border border-indigo-200 rounded-lg text-sm text-indigo-700">
          <Shield className="w-4 h-4 mr-2 flex-shrink-0" />
          Viewing invoices across <strong className="mx-1">all organizations</strong>.
        </div>
      ) : user?.organizationName ? (
        <div className="flex items-center px-4 py-2 mb-4 bg-primary-50 border border-primary-200 rounded-lg text-sm text-primary-700">
          <Building className="w-4 h-4 mr-2 flex-shrink-0" />
          Showing invoices for <strong className="mx-1">{user.organizationName}</strong> only.
        </div>
      ) : null}

      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-5">
        <div>
          <h1 className="page-header">Invoices</h1>
          <p className="page-subtitle">Manage, review, and process your invoice documents</p>
        </div>
        <div className="flex items-center gap-2">
          <input
            ref={fileInputRef}
            type="file"
            accept="application/pdf"
            multiple
            className="hidden"
            onChange={handleFileUpload}
          />
          <button
            onClick={() => refetch()}
            className="flex items-center gap-1.5 px-3 py-2 text-sm bg-white border border-gray-200 text-gray-600 rounded-xl hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm"
          >
            <RefreshCw className="w-4 h-4" />
            <span className="hidden sm:inline">Refresh</span>
          </button>
          <button
            onClick={() => setExportPanelOpen(true)}
            className="flex items-center gap-1.5 px-3 py-2 text-sm bg-white border border-gray-200 text-gray-600 rounded-xl hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm"
          >
            <Download className="w-4 h-4" />
            <span className="hidden sm:inline">Export</span>
          </button>
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadMutation.isPending}
            className="flex items-center gap-1.5 px-4 py-2 text-sm bg-gradient-to-r from-primary-600 to-primary-500 text-white rounded-xl hover:from-primary-700 hover:to-primary-600 disabled:opacity-50 shadow-sm shadow-primary-200 transition-all"
          >
            {uploadMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Upload className="w-4 h-4" />
            )}
            {uploadMutation.isPending ? 'Uploading...' : 'Upload PDF'}
          </button>
        </div>
      </div>

      {/* Upload status messages */}
      {uploadMutation.isSuccess && (
        <div className="mb-3 px-4 py-2 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700 flex items-center">
          <FileText className="w-4 h-4 mr-2" />
          Invoice uploaded and processing started. Extraction data will appear shortly.
        </div>
      )}
      {uploadMutation.isError && (
        <div className="mb-3 px-4 py-2 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
          Failed to upload invoice: {(uploadMutation.error as Error)?.message || 'Unknown error'}
        </div>
      )}

      {/* Active filter badges */}
      {vendorFilter && (
        <div className="flex items-center gap-2 mb-3">
          <span className="text-sm text-gray-500">Filtered by:</span>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-50 border border-blue-200 text-blue-700 text-sm rounded-full">
            Vendor: {vendorFilter}
            <button
              onClick={() => { setVendorFilter(''); setPage(0); }}
              className="ml-1 hover:bg-blue-200 rounded-full p-0.5 transition-colors"
            >
              <X className="w-3 h-3" />
            </button>
          </span>
        </div>
      )}

      {/* Search & Filters Bar */}
      <div className="bg-white rounded-t-2xl shadow-sm px-5 py-4 border border-b-0 border-gray-100">
        <form onSubmit={handleSearch} className="flex flex-wrap gap-3 items-center">
          <div className="relative flex-1 min-w-[200px] max-w-md">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search by vendor, invoice number, or amount..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-400 transition-all bg-gray-50 hover:bg-white"
            />
          </div>

          {/* Date range */}
          <div className="flex items-center gap-2 border border-gray-200 rounded-xl px-3 py-2 text-sm text-gray-600 bg-gray-50 hover:bg-white transition-colors">
            <Calendar className="w-4 h-4 text-gray-400" />
            <input
              type="date"
              value={dateRange.from || ''}
              onChange={(e) => {
                setDateRange((d) => ({ ...d, from: e.target.value }))
                setPage(0)
              }}
              className="border-none outline-none bg-transparent text-sm w-28"
            />
            <span className="text-gray-300">—</span>
            <input
              type="date"
              value={dateRange.to || ''}
              onChange={(e) => {
                setDateRange((d) => ({ ...d, to: e.target.value }))
                setPage(0)
              }}
              className="border-none outline-none bg-transparent text-sm w-28"
            />
          </div>

          <button
            type="submit"
            className="px-5 py-2.5 bg-primary-600 text-white text-sm rounded-xl hover:bg-primary-700 transition-colors font-medium shadow-sm"
          >
            Search
          </button>
        </form>
      </div>

      {/* Status Tabs */}
      <div className="bg-white shadow-sm border-x border-b border-gray-100 px-5">
        <div className="flex gap-1 overflow-x-auto -mb-px">
          {STATUS_TABS.map((tab, idx) => (
            <button
              key={tab.label}
              onClick={() => handleTabChange(idx)}
              className={`px-4 py-3 text-sm font-medium whitespace-nowrap border-b-2 transition-all ${
                activeTab === idx
                  ? 'border-primary-500 text-primary-600 bg-primary-50/50'
                  : 'border-transparent text-gray-400 hover:text-gray-600 hover:bg-gray-50'
              } rounded-t-lg`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white shadow-sm flex-1 flex flex-col overflow-hidden rounded-b-2xl border-x border-b border-gray-100">
        {isLoading ? (
          <div className="flex-1 flex items-center justify-center py-20">
            <div className="text-center">
              <RefreshCw className="w-8 h-8 animate-spin text-primary-400 mx-auto mb-3" />
              <p className="text-sm text-gray-400">Loading invoices...</p>
            </div>
          </div>
        ) : !data?.content.length ? (
          <div className="empty-state">
            <div className="w-20 h-20 rounded-2xl bg-gray-100 flex items-center justify-center mb-4">
              <FileText className="w-10 h-10 text-gray-300" />
            </div>
            <h3 className="text-lg font-semibold text-gray-700 mb-1">No invoices found</h3>
            <p className="text-sm text-gray-500 max-w-sm">
              {activeTab === 0
                ? 'No invoices are pending review. Upload a PDF to get started.'
                : 'No invoices match this filter. Try a different status or search term.'
              }
            </p>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50/80 border-b border-gray-100 sticky top-0 z-10">
                  <tr>
                    <th className="w-10 px-4 py-3">
                      <input
                        type="checkbox"
                        checked={selectedIds.size === data.content.length && data.content.length > 0}
                        onChange={toggleSelectAll}
                        className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                      />
                    </th>
                    <th className="table-header">Invoice</th>
                    <th className="table-header">Status</th>
                    <th className="table-header">Vendor</th>
                    <th className="table-header">Amount</th>
                    <th className="table-header">Approval</th>
                    <th className="table-header">Reviewed By</th>
                    <th className="table-header">Reviewed At</th>
                    <th className="table-header">Imported</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {data.content.map((invoice) => (
                    <tr
                      key={invoice.id}
                      onClick={() => setSelectedInvoiceId(invoice.id)}
                      className={`hover:bg-blue-50/50 cursor-pointer transition-colors ${
                        selectedInvoiceId === invoice.id ? 'bg-blue-50' : ''
                      }`}
                    >
                      <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                        <input
                          type="checkbox"
                          checked={selectedIds.has(invoice.id)}
                          onChange={() => toggleSelect(invoice.id)}
                          className="rounded border-gray-300 text-primary-600"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2.5">
                          <div className="w-8 h-8 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0">
                            <FileText className="w-4 h-4 text-primary-500" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm font-medium text-gray-900 truncate max-w-[200px]">
                              {invoice.originalFileName || `INV-${invoice.invoiceNumber}`}
                            </p>
                            {invoice.invoiceNumber && (
                              <p className="text-[11px] text-gray-400">#{invoice.invoiceNumber}</p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full border ${STATUS_BADGE[invoice.status]}`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${
                            invoice.status === 'PENDING' || invoice.status === 'UNDER_REVIEW' ? 'bg-yellow-500' :
                            invoice.status === 'APPROVED' ? 'bg-green-500' :
                            invoice.status === 'REJECTED' ? 'bg-red-500' :
                            invoice.status === 'SYNCED' ? 'bg-purple-500' :
                            invoice.status === 'SYNC_FAILED' ? 'bg-orange-500' : 'bg-gray-500'
                          }`} />
                          {STATUS_LABEL[invoice.status]}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-gray-700 max-w-[200px] truncate">
                        {invoice.vendorName}
                      </td>
                      <td className="px-4 py-3">
                        <span className="font-semibold text-gray-900">{formatCurrency(invoice.totalAmount)}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {invoice.assignedToName || '-'}
                      </td>
                      <td className="px-4 py-3 text-gray-500">
                        {invoice.updatedAt && invoice.status !== 'PENDING'
                          ? formatDate(invoice.updatedAt)
                          : '-'}
                      </td>
                      <td className="px-4 py-3 text-gray-500 whitespace-nowrap">
                        {formatDateTime(invoice.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="px-5 py-3.5 border-t border-gray-100 flex items-center justify-between flex-shrink-0 bg-gray-50/50">
              <div className="flex items-center gap-1.5">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={data.first}
                  className="p-2 border border-gray-200 rounded-xl disabled:opacity-30 hover:bg-white hover:border-gray-300 transition-all"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>

                {pageNumbers.map((pn, i) =>
                  pn === 'ellipsis' ? (
                    <span key={`e${i}`} className="px-2 text-gray-400 text-sm">
                      …
                    </span>
                  ) : (
                    <button
                      key={pn}
                      onClick={() => setPage(pn as number)}
                      className={`w-9 h-9 text-sm rounded-xl transition-all ${
                        page === pn
                          ? 'bg-primary-600 text-white font-semibold shadow-sm shadow-primary-200'
                          : 'hover:bg-white hover:border-gray-200 border border-transparent text-gray-600'
                      }`}
                    >
                      {(pn as number) + 1}
                    </button>
                  )
                )}

                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.last}
                  className="p-2 border border-gray-200 rounded-xl disabled:opacity-30 hover:bg-white hover:border-gray-300 transition-all"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              <p className="text-sm text-gray-500">
                <span className="font-medium text-gray-700">{showingFrom}–{showingTo}</span> of {data.totalElements} invoices
              </p>
            </div>
          </>
        )}
      </div>

      {/* Side Panel */}
      {selectedInvoiceId && (
        <InvoiceSidePanel
          invoiceId={selectedInvoiceId}
          invoiceIds={invoiceIds}
          onClose={() => setSelectedInvoiceId(null)}
          onNavigate={(id) => setSelectedInvoiceId(id)}
        />
      )}

      {/* Export Panel */}
      <ExportPanel
        open={exportPanelOpen}
        onClose={() => setExportPanelOpen(false)}
        initialFilters={{
          search: appliedSearch || undefined,
          statuses: currentTab.statuses.length > 0 ? currentTab.statuses : undefined,
          invoiceDateFrom: dateRange.from || undefined,
          invoiceDateTo: dateRange.to || undefined,
        }}
      />
    </div>
  )
}
