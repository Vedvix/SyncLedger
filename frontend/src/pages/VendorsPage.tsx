import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { vendorService } from '@/services/vendorService'
import type { VendorRequest, VendorSummary, TopVendor, Vendor } from '@/types'
import {
  Building2,
  Search,
  Plus,
  TrendingUp,
  DollarSign,
  FileText,
  ChevronRight,
  X,
  BarChart3,
} from 'lucide-react'

export function VendorsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createForm, setCreateForm] = useState<VendorRequest>({
    name: '',
    code: '',
    address: '',
    email: '',
    phone: '',
    taxId: '',
    paymentTerms: '',
    notes: '',
  })

  // Fetch vendor list
  const { data: vendorsData, isLoading: vendorsLoading } = useQuery({
    queryKey: ['vendors', search, page],
    queryFn: () =>
      vendorService.getVendors(search || undefined, {
        page,
        size: 20,
        sort: 'createdAt',
        direction: 'desc',
      }),
  })

  // Fetch vendor summary analytics
  const { data: summary } = useQuery<VendorSummary>({
    queryKey: ['vendor-summary'],
    queryFn: () => vendorService.getVendorSummary(),
  })

  // Create vendor mutation
  const createMutation = useMutation({
    mutationFn: (data: VendorRequest) => vendorService.createVendor(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendors'] })
      queryClient.invalidateQueries({ queryKey: ['vendor-summary'] })
      setShowCreateModal(false)
      setCreateForm({ name: '', code: '', address: '', email: '', phone: '', taxId: '', paymentTerms: '', notes: '' })
    },
  })

  const fmt = (n: number | undefined) =>
    n != null
      ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n)
      : '$0.00'

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Vendor Management</h1>
          <p className="text-sm text-gray-500 mt-1">
            Manage vendors and view analytics across your organization
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors"
        >
          <Plus className="w-4 h-4 mr-2" />
          Add Vendor
        </button>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <SummaryCard
            icon={<Building2 className="w-5 h-5 text-blue-600" />}
            label="Total Vendors"
            value={String(summary.totalVendors)}
            sub={`${summary.activeVendors} active`}
            bgColor="bg-blue-50"
          />
          <SummaryCard
            icon={<FileText className="w-5 h-5 text-green-600" />}
            label="Total Invoices"
            value={String(summary.totalInvoicesAcrossVendors)}
            sub="across all vendors"
            bgColor="bg-green-50"
          />
          <SummaryCard
            icon={<DollarSign className="w-5 h-5 text-purple-600" />}
            label="Total Amount"
            value={fmt(summary.totalAmountAcrossVendors)}
            sub="all vendor invoices"
            bgColor="bg-purple-50"
          />
          <SummaryCard
            icon={<TrendingUp className="w-5 h-5 text-orange-600" />}
            label="Avg per Vendor"
            value={fmt(summary.averageAmountPerVendor)}
            sub="average spend"
            bgColor="bg-orange-50"
          />
        </div>
      )}

      {/* Top Vendors Section */}
      {summary && (summary.topVendorsByCount?.length > 0 || summary.topVendorsByAmount?.length > 0) && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Top by Count */}
          {summary.topVendorsByCount?.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border p-6">
              <div className="flex items-center gap-2 mb-4">
                <BarChart3 className="w-5 h-5 text-blue-600" />
                <h3 className="text-sm font-semibold text-gray-900">Top Vendors by Invoice Count</h3>
              </div>
              <div className="space-y-3">
                {summary.topVendorsByCount.map((v: TopVendor, i: number) => (
                  <TopVendorRow key={v.vendorId} vendor={v} rank={i + 1} onClick={() => navigate(`/vendors/${v.vendorId}`)} />
                ))}
              </div>
            </div>
          )}
          {/* Top by Amount */}
          {summary.topVendorsByAmount?.length > 0 && (
            <div className="bg-white rounded-xl shadow-sm border p-6">
              <div className="flex items-center gap-2 mb-4">
                <DollarSign className="w-5 h-5 text-green-600" />
                <h3 className="text-sm font-semibold text-gray-900">Top Vendors by Total Amount</h3>
              </div>
              <div className="space-y-3">
                {summary.topVendorsByAmount.map((v: TopVendor, i: number) => (
                  <TopVendorRow key={v.vendorId} vendor={v} rank={i + 1} onClick={() => navigate(`/vendors/${v.vendorId}`)} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Search + Vendor List */}
      <div className="bg-white rounded-xl shadow-sm border">
        <div className="p-4 border-b">
          <div className="relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search vendors by name, email, code..."
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
              className="w-full pl-10 pr-4 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            />
          </div>
        </div>

        {vendorsLoading ? (
          <div className="p-8 text-center text-gray-500">Loading vendors...</div>
        ) : !vendorsData?.content?.length ? (
          <div className="p-8 text-center text-gray-500">
            <Building2 className="w-12 h-12 mx-auto mb-3 text-gray-300" />
            <p className="font-medium">No vendors found</p>
            <p className="text-sm mt-1">
              {search ? 'Try a different search term' : 'Vendors are auto-created when invoices are processed'}
            </p>
          </div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider border-b bg-gray-50">
                    <th className="px-6 py-3">Vendor</th>
                    <th className="px-6 py-3">Contact</th>
                    <th className="px-6 py-3">Tax ID</th>
                    <th className="px-6 py-3">Status</th>
                    <th className="px-6 py-3">Created</th>
                    <th className="px-6 py-3"></th>
                  </tr>
                </thead>
                <tbody className="divide-y">
                  {vendorsData.content.map((vendor: Vendor) => (
                    <tr
                      key={vendor.id}
                      onClick={() => navigate(`/vendors/${vendor.id}`)}
                      className="hover:bg-gray-50 cursor-pointer transition-colors"
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                            <Building2 className="w-4 h-4 text-blue-600" />
                          </div>
                          <div>
                            <p className="text-sm font-medium text-gray-900">{vendor.name}</p>
                            {vendor.code && (
                              <p className="text-xs text-gray-500">Code: {vendor.code}</p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600">
                        {vendor.email || vendor.phone || '—'}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-600">
                        {vendor.taxId || '—'}
                      </td>
                      <td className="px-6 py-4">
                        <StatusBadge status={vendor.status} />
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {new Date(vendor.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4">
                        <ChevronRight className="w-4 h-4 text-gray-400" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {vendorsData.totalPages > 1 && (
              <div className="p-4 border-t flex items-center justify-between">
                <span className="text-sm text-gray-500">
                  Page {vendorsData.page + 1} of {vendorsData.totalPages} ({vendorsData.totalElements} vendors)
                </span>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={vendorsData.first}
                    className="px-3 py-1 text-sm border rounded hover:bg-gray-50 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage((p) => p + 1)}
                    disabled={vendorsData.last}
                    className="px-3 py-1 text-sm border rounded hover:bg-gray-50 disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Create Vendor Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="text-lg font-semibold text-gray-900">Add New Vendor</h2>
              <button onClick={() => setShowCreateModal(false)}>
                <X className="w-5 h-5 text-gray-400 hover:text-gray-600" />
              </button>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault()
                createMutation.mutate(createForm)
              }}
              className="p-6 space-y-4"
            >
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Vendor Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={createForm.name}
                  onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                  placeholder="e.g., ABC Roofing Inc."
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Vendor Code</label>
                  <input
                    type="text"
                    value={createForm.code || ''}
                    onChange={(e) => setCreateForm({ ...createForm, code: e.target.value })}
                    className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                    placeholder="e.g., VND-001"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Tax ID</label>
                  <input
                    type="text"
                    value={createForm.taxId || ''}
                    onChange={(e) => setCreateForm({ ...createForm, taxId: e.target.value })}
                    className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                    placeholder="EIN / TIN"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <input
                  type="email"
                  value={createForm.email || ''}
                  onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                  placeholder="vendor@example.com"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
                <input
                  type="text"
                  value={createForm.phone || ''}
                  onChange={(e) => setCreateForm({ ...createForm, phone: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                <textarea
                  value={createForm.address || ''}
                  onChange={(e) => setCreateForm({ ...createForm, address: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                  rows={2}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Payment Terms</label>
                <input
                  type="text"
                  value={createForm.paymentTerms || ''}
                  onChange={(e) => setCreateForm({ ...createForm, paymentTerms: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                  placeholder="e.g., Net 30"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                <textarea
                  value={createForm.notes || ''}
                  onChange={(e) => setCreateForm({ ...createForm, notes: e.target.value })}
                  className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-primary-500"
                  rows={2}
                />
              </div>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="px-4 py-2 text-sm text-white bg-primary-600 rounded-lg hover:bg-primary-700 disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Creating...' : 'Create Vendor'}
                </button>
              </div>
              {createMutation.isError && (
                <p className="text-sm text-red-600 mt-2">
                  {(createMutation.error as any)?.response?.data?.message || 'Failed to create vendor'}
                </p>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Subcomponents ──────────────────────────────────────────────────

function SummaryCard({ icon, label, value, sub, bgColor }: {
  icon: React.ReactNode; label: string; value: string; sub: string; bgColor: string
}) {
  return (
    <div className="bg-white rounded-xl shadow-sm border p-5">
      <div className="flex items-center gap-3">
        <div className={`w-10 h-10 rounded-lg ${bgColor} flex items-center justify-center`}>
          {icon}
        </div>
        <div>
          <p className="text-xs text-gray-500 font-medium">{label}</p>
          <p className="text-lg font-bold text-gray-900">{value}</p>
          <p className="text-xs text-gray-400">{sub}</p>
        </div>
      </div>
    </div>
  )
}

function TopVendorRow({ vendor, rank, onClick }: {
  vendor: TopVendor; rank: number; onClick: () => void
}) {
  const fmt = (n: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(n)

  return (
    <div
      onClick={onClick}
      className="flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
    >
      <div className="flex items-center gap-3">
        <span className="w-6 h-6 rounded-full bg-gray-100 flex items-center justify-center text-xs font-bold text-gray-600">
          {rank}
        </span>
        <div>
          <p className="text-sm font-medium text-gray-900">{vendor.vendorName}</p>
          <p className="text-xs text-gray-500">
            {vendor.invoiceCount} invoice{vendor.invoiceCount !== 1 ? 's' : ''}
          </p>
        </div>
      </div>
      <div className="text-right">
        <p className="text-sm font-semibold text-gray-900">{fmt(vendor.totalAmount)}</p>
        <p className="text-xs text-gray-500">avg {fmt(vendor.averageAmount)}</p>
      </div>
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    ACTIVE: 'bg-green-50 text-green-700',
    INACTIVE: 'bg-gray-100 text-gray-600',
    BLOCKED: 'bg-red-50 text-red-700',
    PENDING_REVIEW: 'bg-yellow-50 text-yellow-700',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status] || 'bg-gray-100 text-gray-600'}`}>
      {status.replace('_', ' ')}
    </span>
  )
}
