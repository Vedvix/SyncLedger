import { useState, useEffect, useMemo } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { invoiceService } from '@/services/invoiceService'
import type { ExportRequest, ExportColumn, InvoiceStatus } from '@/types'
import {
  X,
  Download,
  Loader2,
  ChevronDown,
  ChevronUp,
  Filter,
  Columns,
  Settings2,
  CheckSquare,
  Square,
  AlertCircle,
} from 'lucide-react'

interface ExportPanelProps {
  open: boolean
  onClose: () => void
  /** Pre‑fill filters from the current invoices page state */
  initialFilters?: Partial<ExportRequest>
}

const ALL_STATUSES: { value: InvoiceStatus; label: string }[] = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'UNDER_REVIEW', label: 'Under Review' },
  { value: 'APPROVED', label: 'Approved' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'SYNCED', label: 'Synced' },
  { value: 'SYNC_FAILED', label: 'Sync Failed' },
  { value: 'ARCHIVED', label: 'Archived' },
]

// Sensible default column selection
const DEFAULT_COLUMNS = [
  'invoiceNumber',
  'vendorName',
  'invoiceDate',
  'dueDate',
  'totalAmount',
  'currency',
  'status',
  'glAccount',
  'costCenter',
  'project',
  'originalFileName',
  'createdAt',
]

export function ExportPanel({ open, onClose, initialFilters }: ExportPanelProps) {
  // ── Filter state ────────────────────────────────────────
  const [search, setSearch] = useState('')
  const [statuses, setStatuses] = useState<string[]>([])
  const [invoiceDateFrom, setInvoiceDateFrom] = useState('')
  const [invoiceDateTo, setInvoiceDateTo] = useState('')
  const [dueDateFrom, setDueDateFrom] = useState('')
  const [dueDateTo, setDueDateTo] = useState('')
  const [createdDateFrom, setCreatedDateFrom] = useState('')
  const [createdDateTo, setCreatedDateTo] = useState('')
  const [minAmount, setMinAmount] = useState('')
  const [maxAmount, setMaxAmount] = useState('')
  const [currency, setCurrency] = useState('')
  const [vendorName, setVendorName] = useState('')
  const [glAccount, setGlAccount] = useState('')
  const [costCenter, setCostCenter] = useState('')
  const [project, setProject] = useState('')
  const [itemCategory, setItemCategory] = useState('')
  const [overdueOnly, setOverdueOnly] = useState(false)
  const [requiresManualReview, setRequiresManualReview] = useState(false)
  const [minConfidence, setMinConfidence] = useState('')
  const [maxConfidence, setMaxConfidence] = useState('')

  // ── Column state ────────────────────────────────────────
  const [columns, setColumns] = useState<ExportColumn[]>([])
  const [includeLineItems, setIncludeLineItems] = useState(false)
  const [includeSummary, setIncludeSummary] = useState(true)
  const [sortBy, setSortBy] = useState('createdAt')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc')

  // ── UI state ────────────────────────────────────────────
  const [expandedSection, setExpandedSection] = useState<string | null>('filters')

  // ── Fetch available columns ─────────────────────────────
  const { data: columnMap } = useQuery({
    queryKey: ['export-columns'],
    queryFn: () => invoiceService.getExportColumns(),
    enabled: open,
    staleTime: 300_000,
  })

  // Initialize columns from the fetched map
  useEffect(() => {
    if (columnMap && columns.length === 0) {
      const cols: ExportColumn[] = Object.entries(columnMap).map(([key, label]) => ({
        key,
        label,
        selected: DEFAULT_COLUMNS.includes(key),
      }))
      setColumns(cols)
    }
  }, [columnMap, columns.length])

  // Pre-fill filters from parent (e.g. current search/status)
  useEffect(() => {
    if (initialFilters) {
      if (initialFilters.search) setSearch(initialFilters.search)
      if (initialFilters.statuses) setStatuses(initialFilters.statuses)
      if (initialFilters.invoiceDateFrom) setInvoiceDateFrom(initialFilters.invoiceDateFrom)
      if (initialFilters.invoiceDateTo) setInvoiceDateTo(initialFilters.invoiceDateTo)
    }
  }, [initialFilters])

  // ── Build request ──────────────────────────────────────
  const exportRequest = useMemo((): ExportRequest => {
    const req: ExportRequest = {}
    if (search) req.search = search
    if (statuses.length) req.statuses = statuses
    if (invoiceDateFrom) req.invoiceDateFrom = invoiceDateFrom
    if (invoiceDateTo) req.invoiceDateTo = invoiceDateTo
    if (dueDateFrom) req.dueDateFrom = dueDateFrom
    if (dueDateTo) req.dueDateTo = dueDateTo
    if (createdDateFrom) req.createdDateFrom = createdDateFrom
    if (createdDateTo) req.createdDateTo = createdDateTo
    if (minAmount) req.minAmount = parseFloat(minAmount)
    if (maxAmount) req.maxAmount = parseFloat(maxAmount)
    if (currency) req.currency = currency
    if (vendorName) req.vendorNames = vendorName.split(',').map((v) => v.trim()).filter(Boolean)
    if (glAccount) req.glAccount = glAccount
    if (costCenter) req.costCenter = costCenter
    if (project) req.project = project
    if (itemCategory) req.itemCategory = itemCategory
    if (overdueOnly) req.overdueOnly = true
    if (requiresManualReview) req.requiresManualReview = true
    if (minConfidence) req.minConfidenceScore = parseFloat(minConfidence)
    if (maxConfidence) req.maxConfidenceScore = parseFloat(maxConfidence)

    const selectedCols = columns.filter((c) => c.selected).map((c) => c.key)
    if (selectedCols.length > 0 && selectedCols.length < columns.length) {
      req.columns = selectedCols
    }

    req.includeLineItems = includeLineItems
    req.includeSummary = includeSummary
    req.sortBy = sortBy
    req.sortDirection = sortDir

    return req
  }, [
    search, statuses, invoiceDateFrom, invoiceDateTo, dueDateFrom, dueDateTo,
    createdDateFrom, createdDateTo, minAmount, maxAmount, currency, vendorName,
    glAccount, costCenter, project, itemCategory, overdueOnly, requiresManualReview,
    minConfidence, maxConfidence, columns, includeLineItems, includeSummary, sortBy, sortDir,
  ])

  // ── Export mutation ────────────────────────────────────
  const exportMutation = useMutation({
    mutationFn: (req: ExportRequest) => invoiceService.exportToExcel(req),
    onSuccess: () => {
      // optionally close panel after download
    },
  })

  const handleExport = () => {
    exportMutation.mutate(exportRequest)
  }

  const handleReset = () => {
    setSearch('')
    setStatuses([])
    setInvoiceDateFrom('')
    setInvoiceDateTo('')
    setDueDateFrom('')
    setDueDateTo('')
    setCreatedDateFrom('')
    setCreatedDateTo('')
    setMinAmount('')
    setMaxAmount('')
    setCurrency('')
    setVendorName('')
    setGlAccount('')
    setCostCenter('')
    setProject('')
    setItemCategory('')
    setOverdueOnly(false)
    setRequiresManualReview(false)
    setMinConfidence('')
    setMaxConfidence('')
    setIncludeLineItems(false)
    setIncludeSummary(true)
    setSortBy('createdAt')
    setSortDir('desc')
    if (columnMap) {
      setColumns(
        Object.entries(columnMap).map(([key, label]) => ({
          key,
          label,
          selected: DEFAULT_COLUMNS.includes(key),
        }))
      )
    }
  }

  const toggleStatus = (status: string) => {
    setStatuses((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    )
  }

  const toggleColumn = (key: string) => {
    setColumns((prev) =>
      prev.map((c) => (c.key === key ? { ...c, selected: !c.selected } : c))
    )
  }

  const selectAllColumns = () =>
    setColumns((prev) => prev.map((c) => ({ ...c, selected: true })))

  const deselectAllColumns = () =>
    setColumns((prev) => prev.map((c) => ({ ...c, selected: false })))

  const selectedColumnCount = columns.filter((c) => c.selected).length

  const toggleSection = (section: string) =>
    setExpandedSection((prev) => (prev === section ? null : section))

  if (!open) return null

  return (
    <>
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-black/30 z-40 transition-opacity"
        onClick={onClose}
      />

      {/* Panel */}
      <div className="fixed right-0 top-0 h-full w-[520px] max-w-[90vw] bg-white shadow-2xl z-50 flex flex-col overflow-hidden animate-in slide-in-from-right">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b bg-gradient-to-r from-primary-600 to-primary-700">
          <div className="flex items-center gap-3 text-white">
            <Download className="w-5 h-5" />
            <div>
              <h2 className="text-lg font-semibold">Export to Excel</h2>
              <p className="text-sm text-primary-100">Configure filters and columns</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-white/80 hover:text-white hover:bg-white/10 rounded-lg"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable body */}
        <div className="flex-1 overflow-y-auto">
          {/* ── FILTERS SECTION ──────────────────────────── */}
          <CollapsibleSection
            title="Filters"
            icon={<Filter className="w-4 h-4" />}
            expanded={expandedSection === 'filters'}
            onToggle={() => toggleSection('filters')}
          >
            <div className="space-y-4">
              {/* Search */}
              <Field label="Search">
                <input
                  type="text"
                  placeholder="Invoice number, vendor, PO…"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="input-sm"
                />
              </Field>

              {/* Status */}
              <Field label="Status">
                <div className="flex flex-wrap gap-2">
                  {ALL_STATUSES.map((s) => (
                    <button
                      key={s.value}
                      onClick={() => toggleStatus(s.value)}
                      className={`px-2.5 py-1 text-xs rounded-full border transition-colors ${
                        statuses.includes(s.value)
                          ? 'bg-primary-100 border-primary-300 text-primary-700 font-medium'
                          : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
                      }`}
                    >
                      {s.label}
                    </button>
                  ))}
                </div>
              </Field>

              {/* Invoice date range */}
              <Field label="Invoice Date">
                <div className="flex gap-2 items-center">
                  <input
                    type="date"
                    value={invoiceDateFrom}
                    onChange={(e) => setInvoiceDateFrom(e.target.value)}
                    className="input-sm flex-1"
                  />
                  <span className="text-gray-400 text-xs">to</span>
                  <input
                    type="date"
                    value={invoiceDateTo}
                    onChange={(e) => setInvoiceDateTo(e.target.value)}
                    className="input-sm flex-1"
                  />
                </div>
              </Field>

              {/* Due date range */}
              <Field label="Due Date">
                <div className="flex gap-2 items-center">
                  <input
                    type="date"
                    value={dueDateFrom}
                    onChange={(e) => setDueDateFrom(e.target.value)}
                    className="input-sm flex-1"
                  />
                  <span className="text-gray-400 text-xs">to</span>
                  <input
                    type="date"
                    value={dueDateTo}
                    onChange={(e) => setDueDateTo(e.target.value)}
                    className="input-sm flex-1"
                  />
                </div>
              </Field>

              {/* Created date range */}
              <Field label="Date Imported">
                <div className="flex gap-2 items-center">
                  <input
                    type="date"
                    value={createdDateFrom}
                    onChange={(e) => setCreatedDateFrom(e.target.value)}
                    className="input-sm flex-1"
                  />
                  <span className="text-gray-400 text-xs">to</span>
                  <input
                    type="date"
                    value={createdDateTo}
                    onChange={(e) => setCreatedDateTo(e.target.value)}
                    className="input-sm flex-1"
                  />
                </div>
              </Field>

              {/* Amount range */}
              <Field label="Amount Range">
                <div className="flex gap-2 items-center">
                  <input
                    type="number"
                    placeholder="Min"
                    value={minAmount}
                    onChange={(e) => setMinAmount(e.target.value)}
                    className="input-sm flex-1"
                    min={0}
                    step="0.01"
                  />
                  <span className="text-gray-400 text-xs">to</span>
                  <input
                    type="number"
                    placeholder="Max"
                    value={maxAmount}
                    onChange={(e) => setMaxAmount(e.target.value)}
                    className="input-sm flex-1"
                    min={0}
                    step="0.01"
                  />
                </div>
              </Field>

              {/* Currency */}
              <Field label="Currency">
                <input
                  type="text"
                  placeholder="e.g. USD, EUR, GBP"
                  value={currency}
                  onChange={(e) => setCurrency(e.target.value.toUpperCase())}
                  className="input-sm"
                  maxLength={3}
                />
              </Field>

              {/* Vendor */}
              <Field label="Vendor Name(s)">
                <input
                  type="text"
                  placeholder="Comma-separated: Vendor A, Vendor B"
                  value={vendorName}
                  onChange={(e) => setVendorName(e.target.value)}
                  className="input-sm"
                />
              </Field>

              {/* Accounting fields */}
              <div className="grid grid-cols-2 gap-3">
                <Field label="GL Account">
                  <input
                    type="text"
                    placeholder="e.g. 5000"
                    value={glAccount}
                    onChange={(e) => setGlAccount(e.target.value)}
                    className="input-sm"
                  />
                </Field>
                <Field label="Cost Center">
                  <input
                    type="text"
                    placeholder="e.g. CC-100"
                    value={costCenter}
                    onChange={(e) => setCostCenter(e.target.value)}
                    className="input-sm"
                  />
                </Field>
                <Field label="Project">
                  <input
                    type="text"
                    placeholder="Project code"
                    value={project}
                    onChange={(e) => setProject(e.target.value)}
                    className="input-sm"
                  />
                </Field>
                <Field label="Item Category">
                  <input
                    type="text"
                    placeholder="Category"
                    value={itemCategory}
                    onChange={(e) => setItemCategory(e.target.value)}
                    className="input-sm"
                  />
                </Field>
              </div>

              {/* Confidence score */}
              <Field label="Confidence Score (%)">
                <div className="flex gap-2 items-center">
                  <input
                    type="number"
                    placeholder="Min"
                    value={minConfidence}
                    onChange={(e) => setMinConfidence(e.target.value)}
                    className="input-sm flex-1"
                    min={0}
                    max={100}
                  />
                  <span className="text-gray-400 text-xs">to</span>
                  <input
                    type="number"
                    placeholder="Max"
                    value={maxConfidence}
                    onChange={(e) => setMaxConfidence(e.target.value)}
                    className="input-sm flex-1"
                    min={0}
                    max={100}
                  />
                </div>
              </Field>

              {/* Toggles */}
              <div className="flex gap-4">
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={overdueOnly}
                    onChange={(e) => setOverdueOnly(e.target.checked)}
                    className="rounded border-gray-300 text-primary-600"
                  />
                  Overdue only
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={requiresManualReview}
                    onChange={(e) => setRequiresManualReview(e.target.checked)}
                    className="rounded border-gray-300 text-primary-600"
                  />
                  Needs review
                </label>
              </div>
            </div>
          </CollapsibleSection>

          {/* ── COLUMNS SECTION ──────────────────────────── */}
          <CollapsibleSection
            title={`Columns (${selectedColumnCount}/${columns.length})`}
            icon={<Columns className="w-4 h-4" />}
            expanded={expandedSection === 'columns'}
            onToggle={() => toggleSection('columns')}
          >
            <div className="space-y-3">
              <div className="flex gap-2 mb-2">
                <button
                  onClick={selectAllColumns}
                  className="text-xs text-primary-600 hover:underline"
                >
                  Select all
                </button>
                <span className="text-gray-300">|</span>
                <button
                  onClick={deselectAllColumns}
                  className="text-xs text-primary-600 hover:underline"
                >
                  Deselect all
                </button>
              </div>
              <div className="grid grid-cols-2 gap-1 max-h-[280px] overflow-y-auto pr-1">
                {columns.map((col) => (
                  <label
                    key={col.key}
                    className="flex items-center gap-2 px-2 py-1.5 rounded hover:bg-gray-50 cursor-pointer"
                  >
                    {col.selected ? (
                      <CheckSquare
                        className="w-4 h-4 text-primary-600 flex-shrink-0"
                        onClick={() => toggleColumn(col.key)}
                      />
                    ) : (
                      <Square
                        className="w-4 h-4 text-gray-400 flex-shrink-0"
                        onClick={() => toggleColumn(col.key)}
                      />
                    )}
                    <span className="text-sm text-gray-700 truncate">{col.label}</span>
                  </label>
                ))}
              </div>
            </div>
          </CollapsibleSection>

          {/* ── OPTIONS SECTION ───────────────────────────── */}
          <CollapsibleSection
            title="Options"
            icon={<Settings2 className="w-4 h-4" />}
            expanded={expandedSection === 'options'}
            onToggle={() => toggleSection('options')}
          >
            <div className="space-y-4">
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={includeLineItems}
                  onChange={(e) => setIncludeLineItems(e.target.checked)}
                  className="rounded border-gray-300 text-primary-600"
                />
                Include Line Items sheet
              </label>
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={includeSummary}
                  onChange={(e) => setIncludeSummary(e.target.checked)}
                  className="rounded border-gray-300 text-primary-600"
                />
                Include Summary sheet (totals by status, vendor, month)
              </label>

              <Field label="Sort By">
                <div className="flex gap-2">
                  <select
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    className="input-sm flex-1"
                  >
                    <option value="createdAt">Date Imported</option>
                    <option value="invoiceDate">Invoice Date</option>
                    <option value="dueDate">Due Date</option>
                    <option value="totalAmount">Total Amount</option>
                    <option value="vendorName">Vendor Name</option>
                    <option value="invoiceNumber">Invoice Number</option>
                    <option value="status">Status</option>
                    <option value="confidenceScore">Confidence</option>
                  </select>
                  <button
                    onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
                    className="px-3 py-2 border rounded-lg text-sm hover:bg-gray-50 flex items-center gap-1"
                  >
                    {sortDir === 'asc' ? '↑ Asc' : '↓ Desc'}
                  </button>
                </div>
              </Field>
            </div>
          </CollapsibleSection>
        </div>

        {/* Footer */}
        <div className="border-t px-6 py-4 bg-gray-50 flex items-center justify-between gap-3">
          <button
            onClick={handleReset}
            className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg"
          >
            Reset All
          </button>
          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 text-sm border rounded-lg text-gray-700 hover:bg-gray-100"
            >
              Cancel
            </button>
            <button
              onClick={handleExport}
              disabled={exportMutation.isPending || selectedColumnCount === 0}
              className="flex items-center gap-2 px-5 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {exportMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Generating…
                </>
              ) : (
                <>
                  <Download className="w-4 h-4" />
                  Export Excel
                </>
              )}
            </button>
          </div>
        </div>

        {/* Error message */}
        {exportMutation.isError && (
          <div className="px-6 py-3 bg-red-50 border-t border-red-100 flex items-center gap-2 text-sm text-red-700">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            Export failed: {(exportMutation.error as Error)?.message || 'Unknown error'}
          </div>
        )}

        {/* Success message */}
        {exportMutation.isSuccess && (
          <div className="px-6 py-3 bg-green-50 border-t border-green-100 flex items-center gap-2 text-sm text-green-700">
            <Download className="w-4 h-4 flex-shrink-0" />
            Excel file downloaded successfully!
          </div>
        )}
      </div>
    </>
  )
}

// ── Reusable sub-components ──────────────────────────────────

function CollapsibleSection({
  title,
  icon,
  expanded,
  onToggle,
  children,
}: {
  title: string
  icon: React.ReactNode
  expanded: boolean
  onToggle: () => void
  children: React.ReactNode
}) {
  return (
    <div className="border-b">
      <button
        onClick={onToggle}
        className="w-full flex items-center justify-between px-6 py-3 hover:bg-gray-50 transition-colors"
      >
        <div className="flex items-center gap-2 text-sm font-medium text-gray-800">
          {icon}
          {title}
        </div>
        {expanded ? (
          <ChevronUp className="w-4 h-4 text-gray-400" />
        ) : (
          <ChevronDown className="w-4 h-4 text-gray-400" />
        )}
      </button>
      {expanded && <div className="px-6 pb-4">{children}</div>}
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-medium text-gray-500 mb-1">{label}</label>
      {children}
    </div>
  )
}
