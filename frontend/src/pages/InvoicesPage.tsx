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
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold text-gray-900">Invoices</h1>
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
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadMutation.isPending}
            className="flex items-center px-3 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {uploadMutation.isPending ? (
              <Loader2 className="w-4 h-4 mr-1.5 animate-spin" />
            ) : (
              <Upload className="w-4 h-4 mr-1.5" />
            )}
            {uploadMutation.isPending ? 'Uploading...' : 'Upload PDF'}
          </button>
          <button
            onClick={() => setExportPanelOpen(true)}
            className="flex items-center px-3 py-2 text-sm bg-emerald-600 text-white rounded-lg hover:bg-emerald-700"
          >
            <Download className="w-4 h-4 mr-1.5" />
            Export
          </button>
          <button
            onClick={() => refetch()}
            className="flex items-center px-3 py-2 text-sm bg-white border rounded-lg hover:bg-gray-50"
          >
            <RefreshCw className="w-4 h-4 mr-1.5" />
            Refresh
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
      <div className="bg-white rounded-t-xl shadow-sm px-4 py-3 border-b">
        <form onSubmit={handleSearch} className="flex gap-3 items-center">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search card"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-4 py-2 border rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {/* Date range */}
          <div className="flex items-center gap-2 border rounded-lg px-3 py-2 text-sm text-gray-600">
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
            <span className="text-gray-400">-</span>
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
            className="px-5 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700"
          >
            Search
          </button>
        </form>
      </div>

      {/* Status Tabs */}
      <div className="bg-white shadow-sm border-b px-4">
        <div className="flex gap-0 overflow-x-auto">
          {STATUS_TABS.map((tab, idx) => (
            <button
              key={tab.label}
              onClick={() => handleTabChange(idx)}
              className={`px-4 py-3 text-sm font-medium whitespace-nowrap border-b-2 transition-colors ${
                activeTab === idx
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table */}
      <div className="bg-white shadow-sm flex-1 flex flex-col overflow-hidden rounded-b-xl">
        {isLoading ? (
          <div className="flex-1 flex items-center justify-center">
            <RefreshCw className="w-8 h-8 animate-spin text-primary-500" />
          </div>
        ) : !data?.content.length ? (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
            <FileText className="w-12 h-12 mb-3 opacity-30" />
            <p>No invoices found</p>
          </div>
        ) : (
          <>
            <div className="flex-1 overflow-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b sticky top-0 z-10">
                  <tr>
                    <th className="w-10 px-4 py-3">
                      <input
                        type="checkbox"
                        checked={selectedIds.size === data.content.length && data.content.length > 0}
                        onChange={toggleSelectAll}
                        className="rounded border-gray-300 text-primary-600"
                      />
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      File Name
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Status
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Vendor
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Amount
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Approved
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Approved By
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Reviewed At
                    </th>
                    <th className="text-left px-4 py-3 text-xs font-medium text-gray-500 uppercase">
                      Date Imported
                    </th>
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
                        <span className="text-primary-600 hover:underline font-medium">
                          {invoice.originalFileName || `Invoice-${invoice.invoiceNumber}`}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="text-xs text-gray-500">Analyzed</span>
                      </td>
                      <td className="px-4 py-3 text-gray-700 max-w-[200px] truncate">
                        {invoice.vendorName}
                      </td>
                      <td className="px-4 py-3 font-medium text-gray-900">
                        {formatCurrency(invoice.totalAmount)}
                      </td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-flex px-2.5 py-1 text-xs font-medium rounded border ${
                            STATUS_BADGE[invoice.status]
                          }`}
                        >
                          {STATUS_LABEL[invoice.status]}
                        </span>
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
            <div className="px-4 py-3 border-t flex items-center justify-between flex-shrink-0 bg-white">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={data.first}
                  className="p-1.5 border rounded-lg disabled:opacity-30 hover:bg-gray-50"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>

                {pageNumbers.map((pn, i) =>
                  pn === 'ellipsis' ? (
                    <span key={`e${i}`} className="px-2 text-gray-400 text-sm">
                      ...
                    </span>
                  ) : (
                    <button
                      key={pn}
                      onClick={() => setPage(pn as number)}
                      className={`w-8 h-8 text-sm rounded-lg ${
                        page === pn
                          ? 'bg-primary-600 text-white font-medium'
                          : 'hover:bg-gray-100 text-gray-600'
                      }`}
                    >
                      {(pn as number) + 1}
                    </button>
                  )
                )}

                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={data.last}
                  className="p-1.5 border rounded-lg disabled:opacity-30 hover:bg-gray-50"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              <div className="flex items-center gap-4">
                <select
                  className="border rounded-lg px-2 py-1 text-sm"
                  defaultValue={PAGE_SIZE}
                  disabled
                >
                  <option>{PAGE_SIZE}</option>
                </select>
                <span className="text-sm text-gray-500">
                  Showing {showingFrom} - {showingTo} of {data.totalElements}
                </span>
              </div>
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
