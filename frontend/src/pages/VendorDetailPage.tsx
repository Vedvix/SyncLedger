import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { vendorService } from '@/services/vendorService'
import { invoiceService } from '@/services/invoiceService'
import type { Vendor, VendorRequest, Invoice } from '@/types'
import {
  ArrowLeft,
  Building2,
  Mail,
  Phone,
  MapPin,
  Globe,
  FileText,
  DollarSign,
  TrendingUp,
  BarChart3,
  CheckCircle,
  Clock,
  AlertTriangle,
  Edit3,
  Save,
  X,
  ChevronRight,
  Hash,
} from 'lucide-react'

export function VendorDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState<VendorRequest>({ name: '' })

  // Fetch vendor with analytics
  const {
    data: vendor,
    isLoading,
    error,
  } = useQuery<Vendor>({
    queryKey: ['vendor', id],
    queryFn: () => vendorService.getVendor(Number(id)),
    enabled: !!id,
  })

  // Fetch invoices linked to this vendor
  const { data: invoicesData } = useQuery({
    queryKey: ['vendor-invoices', id],
    queryFn: () =>
      invoiceService.getInvoices(
        { vendorName: vendor?.name },
        { page: 0, size: 10, sort: 'createdAt', direction: 'desc' }
      ),
    enabled: !!vendor?.name,
  })

  // Update vendor mutation
  const updateMutation = useMutation({
    mutationFn: (data: VendorRequest) => vendorService.updateVendor(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendor', id] })
      setIsEditing(false)
    },
  })

  const startEditing = () => {
    if (!vendor) return
    setEditForm({
      name: vendor.name,
      code: vendor.code || '',
      address: vendor.address || '',
      email: vendor.email || '',
      phone: vendor.phone || '',
      contactPerson: vendor.contactPerson || '',
      website: vendor.website || '',
      taxId: vendor.taxId || '',
      paymentTerms: vendor.paymentTerms || '',
      currency: vendor.currency || 'USD',
      notes: vendor.notes || '',
    })
    setIsEditing(true)
  }

  const handleSave = () => {
    updateMutation.mutate(editForm)
  }

  const fmt = (n: number | undefined) =>
    n != null
      ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(n)
      : '$0.00'

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-primary-600 border-t-transparent rounded-full" />
      </div>
    )
  }

  if (error || !vendor) {
    return (
      <div className="text-center py-12">
        <AlertTriangle className="w-12 h-12 mx-auto mb-3 text-red-400" />
        <p className="text-lg font-medium text-gray-900">Vendor not found</p>
        <button
          onClick={() => navigate('/vendors')}
          className="mt-4 text-sm text-primary-600 hover:underline"
        >
          Back to vendors
        </button>
      </div>
    )
  }

  const analytics = vendor.analytics

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button onClick={() => navigate('/vendors')} className="p-2 hover:bg-gray-100 rounded-lg">
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-blue-100 flex items-center justify-center">
              <Building2 className="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{vendor.name}</h1>
              <div className="flex items-center gap-3 mt-1">
                <StatusBadge status={vendor.status} />
                {vendor.code && (
                  <span className="text-sm text-gray-500">Code: {vendor.code}</span>
                )}
                {vendor.organizationName && (
                  <span className="text-xs text-gray-400">Org: {vendor.organizationName}</span>
                )}
              </div>
            </div>
          </div>
        </div>
        <button
          onClick={isEditing ? handleSave : startEditing}
          disabled={updateMutation.isPending}
          className={`inline-flex items-center px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
            isEditing
              ? 'bg-green-600 text-white hover:bg-green-700'
              : 'bg-white border text-gray-700 hover:bg-gray-50'
          }`}
        >
          {isEditing ? (
            <>
              <Save className="w-4 h-4 mr-2" />
              {updateMutation.isPending ? 'Saving...' : 'Save'}
            </>
          ) : (
            <>
              <Edit3 className="w-4 h-4 mr-2" />
              Edit
            </>
          )}
        </button>
        {isEditing && (
          <button
            onClick={() => setIsEditing(false)}
            className="p-2 text-gray-400 hover:text-gray-600"
          >
            <X className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Analytics Cards */}
      {analytics && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
          <MetricCard
            icon={<FileText className="w-4 h-4 text-blue-600" />}
            label="Total Invoices"
            value={String(analytics.totalInvoices)}
            onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}`)}
          />
          <MetricCard
            icon={<DollarSign className="w-4 h-4 text-green-600" />}
            label="Total Amount"
            value={fmt(analytics.totalAmount)}
            onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}`)}
          />
          <MetricCard
            icon={<TrendingUp className="w-4 h-4 text-purple-600" />}
            label="Average Invoice"
            value={fmt(analytics.averageInvoiceAmount)}
            onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}`)}
          />
          <MetricCard
            icon={<Clock className="w-4 h-4 text-yellow-600" />}
            label="Pending"
            value={String(analytics.pendingInvoices)}
            onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}&status=PENDING`)}
          />
          <MetricCard
            icon={<CheckCircle className="w-4 h-4 text-green-600" />}
            label="Approved"
            value={String(analytics.approvedInvoices)}
            onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}&status=APPROVED`)}
          />
          <MetricCard
            icon={<BarChart3 className="w-4 h-4 text-indigo-600" />}
            label="Avg Confidence"
            value={`${(analytics.averageConfidenceScore * 100).toFixed(0)}%`}
          />
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Contact Info */}
        <div className="bg-white rounded-xl shadow-sm border p-6">
          <h3 className="text-sm font-semibold text-gray-900 mb-4">Contact Information</h3>

          {isEditing ? (
            <div className="space-y-3">
              <EditField label="Name" value={editForm.name} onChange={(v) => setEditForm({ ...editForm, name: v })} />
              <EditField label="Code" value={editForm.code || ''} onChange={(v) => setEditForm({ ...editForm, code: v })} />
              <EditField label="Email" value={editForm.email || ''} onChange={(v) => setEditForm({ ...editForm, email: v })} />
              <EditField label="Phone" value={editForm.phone || ''} onChange={(v) => setEditForm({ ...editForm, phone: v })} />
              <EditField label="Contact Person" value={editForm.contactPerson || ''} onChange={(v) => setEditForm({ ...editForm, contactPerson: v })} />
              <EditField label="Website" value={editForm.website || ''} onChange={(v) => setEditForm({ ...editForm, website: v })} />
              <EditField label="Address" value={editForm.address || ''} onChange={(v) => setEditForm({ ...editForm, address: v })} multiline />
              <EditField label="Tax ID" value={editForm.taxId || ''} onChange={(v) => setEditForm({ ...editForm, taxId: v })} />
              <EditField label="Payment Terms" value={editForm.paymentTerms || ''} onChange={(v) => setEditForm({ ...editForm, paymentTerms: v })} />
              <EditField label="Notes" value={editForm.notes || ''} onChange={(v) => setEditForm({ ...editForm, notes: v })} multiline />
            </div>
          ) : (
            <div className="space-y-3">
              <InfoRow icon={<Mail className="w-4 h-4" />} label="Email" value={vendor.email} />
              <InfoRow icon={<Phone className="w-4 h-4" />} label="Phone" value={vendor.phone} />
              <InfoRow icon={<MapPin className="w-4 h-4" />} label="Address" value={vendor.address} />
              <InfoRow icon={<Globe className="w-4 h-4" />} label="Website" value={vendor.website} />
              <InfoRow icon={<Hash className="w-4 h-4" />} label="Tax ID" value={vendor.taxId} />
              {vendor.contactPerson && (
                <InfoRow icon={<Building2 className="w-4 h-4" />} label="Contact" value={vendor.contactPerson} />
              )}
              {vendor.paymentTerms && (
                <InfoRow icon={<Clock className="w-4 h-4" />} label="Terms" value={vendor.paymentTerms} />
              )}
              {vendor.notes && (
                <div className="pt-3 border-t">
                  <p className="text-xs text-gray-500 mb-1">Notes</p>
                  <p className="text-sm text-gray-700">{vendor.notes}</p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Center: Financial Details */}
        {analytics && (
          <div className="bg-white rounded-xl shadow-sm border p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Financial Summary</h3>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Total Invoiced</span>
                <span className="text-sm font-bold text-gray-900">{fmt(analytics.totalAmount)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Average Invoice</span>
                <span className="text-sm font-semibold text-gray-900">{fmt(analytics.averageInvoiceAmount)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Min Invoice</span>
                <span className="text-sm text-gray-900">{fmt(analytics.minInvoiceAmount)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Max Invoice</span>
                <span className="text-sm text-gray-900">{fmt(analytics.maxInvoiceAmount)}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Total Tax</span>
                <span className="text-sm text-gray-900">{fmt(analytics.totalTaxAmount)}</span>
              </div>
              <hr />
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">First Invoice</span>
                <span className="text-sm text-gray-900">
                  {analytics.firstInvoiceDate || '—'}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Last Invoice</span>
                <span className="text-sm text-gray-900">
                  {analytics.lastInvoiceDate || '—'}
                </span>
              </div>

              {/* Invoice Status Breakdown */}
              <hr />
              <h4 className="text-xs font-semibold text-gray-500 uppercase">Status Breakdown</h4>
              <StatusBar label="Pending" count={analytics.pendingInvoices} total={analytics.totalInvoices} color="bg-yellow-400" onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}&status=PENDING`)} />
              <StatusBar label="Approved" count={analytics.approvedInvoices} total={analytics.totalInvoices} color="bg-green-400" onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}&status=APPROVED`)} />
              <StatusBar label="Rejected" count={analytics.rejectedInvoices} total={analytics.totalInvoices} color="bg-red-400" onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}&status=REJECTED`)} />
              <StatusBar label="Synced" count={analytics.syncedInvoices} total={analytics.totalInvoices} color="bg-blue-400" onClick={() => navigate(`/invoices?vendorName=${encodeURIComponent(vendor.name)}&status=SYNCED`)} />
            </div>
          </div>
        )}

        {/* Right: Monthly Trend */}
        {analytics && analytics.monthlyTotals && Object.keys(analytics.monthlyTotals).length > 0 && (
          <div className="bg-white rounded-xl shadow-sm border p-6">
            <h3 className="text-sm font-semibold text-gray-900 mb-4">Monthly Trend (12 months)</h3>
            <div className="space-y-2">
              {Object.entries(analytics.monthlyTotals).map(([month, amount]) => {
                const maxAmount = Math.max(...Object.values(analytics.monthlyTotals))
                const pct = maxAmount > 0 ? (amount / maxAmount) * 100 : 0
                return (
                  <div key={month} className="flex items-center gap-3">
                    <span className="text-xs text-gray-500 w-16">{month}</span>
                    <div className="flex-1 bg-gray-100 rounded h-5 overflow-hidden">
                      <div
                        className="h-full bg-primary-500 rounded"
                        style={{ width: `${Math.max(pct, 2)}%` }}
                      />
                    </div>
                    <span className="text-xs font-medium text-gray-700 w-20 text-right">
                      {fmt(amount)}
                    </span>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </div>

      {/* Recent Invoices */}
      {invoicesData && invoicesData.content?.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border">
          <div className="p-6 border-b">
            <h3 className="text-sm font-semibold text-gray-900">Recent Invoices</h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider border-b bg-gray-50">
                  <th className="px-6 py-3">Invoice #</th>
                  <th className="px-6 py-3">Date</th>
                  <th className="px-6 py-3">Amount</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Confidence</th>
                  <th className="px-6 py-3"></th>
                </tr>
              </thead>
              <tbody className="divide-y">
                {invoicesData.content.map((inv: Invoice) => (
                  <tr
                    key={inv.id}
                    onClick={() => navigate(`/invoices/${inv.id}`)}
                    className="hover:bg-gray-50 cursor-pointer transition-colors"
                  >
                    <td className="px-6 py-3 text-sm font-medium text-gray-900">{inv.invoiceNumber}</td>
                    <td className="px-6 py-3 text-sm text-gray-600">
                      {inv.invoiceDate ? new Date(inv.invoiceDate).toLocaleDateString() : '—'}
                    </td>
                    <td className="px-6 py-3 text-sm font-medium text-gray-900">{fmt(inv.totalAmount)}</td>
                    <td className="px-6 py-3">
                      <StatusBadge status={inv.status} />
                    </td>
                    <td className="px-6 py-3 text-sm text-gray-600">
                      {inv.confidenceScore != null ? `${(inv.confidenceScore * 100).toFixed(0)}%` : '—'}
                    </td>
                    <td className="px-6 py-3">
                      <ChevronRight className="w-4 h-4 text-gray-400" />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

// ─── Subcomponents ──────────────────────────────────────────────────

function MetricCard({ icon, label, value, onClick }: { icon: React.ReactNode; label: string; value: string; onClick?: () => void }) {
  return (
    <div
      className={`bg-white rounded-xl shadow-sm border p-4 transition-all ${onClick ? 'cursor-pointer hover:shadow-md hover:scale-[1.02] active:scale-[0.98]' : ''}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
    >
      <div className="flex items-center gap-2 mb-1">
        {icon}
        <span className="text-xs text-gray-500">{label}</span>
      </div>
      <p className="text-lg font-bold text-gray-900">{value}</p>
    </div>
  )
}

function InfoRow({ icon, label, value }: { icon: React.ReactNode; label: string; value?: string }) {
  return (
    <div className="flex items-start gap-3">
      <span className="text-gray-400 mt-0.5">{icon}</span>
      <div>
        <p className="text-xs text-gray-500">{label}</p>
        <p className="text-sm text-gray-900">{value || '—'}</p>
      </div>
    </div>
  )
}

function EditField({ label, value, onChange, multiline }: {
  label: string; value: string; onChange: (v: string) => void; multiline?: boolean
}) {
  return (
    <div>
      <label className="block text-xs text-gray-500 mb-1">{label}</label>
      {multiline ? (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full px-3 py-1.5 border rounded text-sm focus:ring-2 focus:ring-primary-500"
          rows={2}
        />
      ) : (
        <input
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="w-full px-3 py-1.5 border rounded text-sm focus:ring-2 focus:ring-primary-500"
        />
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    ACTIVE: 'bg-green-50 text-green-700',
    INACTIVE: 'bg-gray-100 text-gray-600',
    BLOCKED: 'bg-red-50 text-red-700',
    PENDING_REVIEW: 'bg-yellow-50 text-yellow-700',
    PENDING: 'bg-yellow-50 text-yellow-700',
    UNDER_REVIEW: 'bg-orange-50 text-orange-700',
    APPROVED: 'bg-green-50 text-green-700',
    REJECTED: 'bg-red-50 text-red-700',
    SYNCED: 'bg-blue-50 text-blue-700',
    SYNC_FAILED: 'bg-red-50 text-red-700',
  }
  return (
    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${styles[status] || 'bg-gray-100 text-gray-600'}`}>
      {status.replace(/_/g, ' ')}
    </span>
  )
}

function StatusBar({ label, count, total, color, onClick }: { label: string; count: number; total: number; color: string; onClick?: () => void }) {
  const pct = total > 0 ? (count / total) * 100 : 0
  return (
    <div
      className={`flex items-center gap-3 ${onClick ? 'cursor-pointer hover:bg-gray-50 rounded-lg px-2 py-1 -mx-2 transition-colors' : ''}`}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
    >
      <span className="text-xs text-gray-600 w-20">{label}</span>
      <div className="flex-1 bg-gray-100 rounded h-3 overflow-hidden">
        <div className={`h-full ${color} rounded`} style={{ width: `${Math.max(pct, 1)}%` }} />
      </div>
      <span className="text-xs font-medium text-gray-600 w-8 text-right">{count}</span>
    </div>
  )
}
