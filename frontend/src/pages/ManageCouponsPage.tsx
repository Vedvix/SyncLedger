import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Ticket,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  ArrowLeft,
  Save,
  X,
  CheckCircle2,
  XCircle,
  ToggleLeft,
  ToggleRight,
  Copy,
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/components/ui/Toaster'
import { couponService } from '@/services/adminCatalogService'
import type { Coupon, CouponRequest, DiscountType } from '@/types'

// ---------- helpers ----------
function fmtDate(dateStr?: string) {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

const BLANK_FORM: CouponRequest = {
  code: '',
  description: '',
  discountType: 'PERCENTAGE',
  discountValue: 0,
  applicablePlans: '',
  maxRedemptions: undefined,
  validFrom: undefined,
  validUntil: undefined,
  active: true,
}

export default function ManageCouponsPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { toast } = useToast()
  const [coupons, setCoupons] = useState<Coupon[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<CouponRequest>(BLANK_FORM)

  useEffect(() => {
    if (user?.role !== 'SUPER_ADMIN') { navigate('/dashboard'); return }
    loadCoupons()
  }, [user, navigate])

  const loadCoupons = useCallback(async () => {
    try {
      setLoading(true)
      const data = await couponService.getAllCoupons()
      setCoupons(data)
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to load coupons')
    } finally {
      setLoading(false)
    }
  }, [toast])

  const openCreate = () => {
    setEditingId(null)
    setForm({ ...BLANK_FORM })
    setShowForm(true)
  }

  const openEdit = (c: Coupon) => {
    setEditingId(c.id)
    setForm({
      code: c.code,
      description: c.description || '',
      discountType: c.discountType,
      discountValue: c.discountValue,
      applicablePlans: c.applicablePlans || '',
      maxRedemptions: c.maxRedemptions,
      validFrom: c.validFrom?.substring(0, 10) || undefined,
      validUntil: c.validUntil?.substring(0, 10) || undefined,
      active: c.active,
    })
    setShowForm(true)
  }

  const handleSave = async () => {
    if (!form.code || form.discountValue == null) {
      toast.error('Code and discount value are required')
      return
    }
    try {
      setSaving(true)
      const payload: CouponRequest = {
        ...form,
        code: form.code.toUpperCase().trim(),
        validFrom: form.validFrom || undefined,
        validUntil: form.validUntil || undefined,
        maxRedemptions: form.maxRedemptions || undefined,
      }
      if (editingId) {
        await couponService.updateCoupon(editingId, payload)
        toast.success('Coupon updated')
      } else {
        await couponService.createCoupon(payload)
        toast.success('Coupon created')
      }
      setShowForm(false)
      loadCoupons()
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to save coupon')
    } finally {
      setSaving(false)
    }
  }

  const handleDeactivate = async (id: number) => {
    if (!confirm('Deactivate this coupon? It will no longer be redeemable.')) return
    try {
      await couponService.deactivateCoupon(id)
      toast.success('Coupon deactivated')
      loadCoupons()
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to deactivate coupon')
    }
  }

  const copyCode = (code: string) => {
    navigator.clipboard.writeText(code)
    toast.success(`Copied ${code}`)
  }

  const set = (field: keyof CouponRequest, value: any) =>
    setForm((prev) => ({ ...prev, [field]: value }))

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate('/super-admin')}>
            <ArrowLeft className="w-4 h-4 mr-1" /> Back
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
              <Ticket className="w-6 h-6 text-primary-600" />
              Manage Coupons
            </h1>
            <p className="text-muted-foreground text-sm">
              Create promotional vouchers and coupon codes to offer discounts on subscription plans.
            </p>
          </div>
        </div>
        {!showForm && (
          <Button onClick={openCreate}>
            <Plus className="w-4 h-4 mr-1" /> New Coupon
          </Button>
        )}
      </div>

      {/* ========== Create / Edit Form ========== */}
      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle>{editingId ? 'Edit Coupon' : 'New Coupon'}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            {/* Row 1 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Code *</label>
                <input
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none font-mono uppercase"
                  placeholder="e.g. SAVE20"
                  value={form.code}
                  onChange={(e) => set('code', e.target.value.toUpperCase())}
                  disabled={!!editingId}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Discount Type</label>
                <select
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none bg-white"
                  value={form.discountType}
                  onChange={(e) => set('discountType', e.target.value as DiscountType)}
                >
                  <option value="PERCENTAGE">Percentage (%)</option>
                  <option value="FIXED_AMOUNT">Fixed Amount (cents)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Discount Value * {form.discountType === 'PERCENTAGE' ? '(%)' : '(cents)'}
                </label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  value={form.discountValue}
                  onChange={(e) => set('discountValue', Number(e.target.value))}
                />
                {form.discountType === 'PERCENTAGE' && (
                  <p className="text-xs text-gray-400 mt-0.5">{form.discountValue}% off</p>
                )}
                {form.discountType === 'FIXED_AMOUNT' && (
                  <p className="text-xs text-gray-400 mt-0.5">${(form.discountValue / 100).toFixed(2)} off</p>
                )}
              </div>
            </div>

            {/* Row 2 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <input
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  placeholder="Internal note or promo description"
                  value={form.description || ''}
                  onChange={(e) => set('description', e.target.value)}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Applicable Plans</label>
                <input
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  placeholder="e.g. STARTER,PROFESSIONAL or leave blank for all"
                  value={form.applicablePlans || ''}
                  onChange={(e) => set('applicablePlans', e.target.value.toUpperCase())}
                />
                <p className="text-xs text-gray-400 mt-0.5">Comma-separated plan keys. Blank = all plans.</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Max Redemptions</label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  placeholder="Unlimited if blank"
                  value={form.maxRedemptions ?? ''}
                  onChange={(e) => set('maxRedemptions', e.target.value ? Number(e.target.value) : null)}
                />
              </div>
            </div>

            {/* Row 3 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Valid From</label>
                <input
                  type="date"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  value={form.validFrom || ''}
                  onChange={(e) => set('validFrom', e.target.value || null)}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Valid Until</label>
                <input
                  type="date"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  value={form.validUntil || ''}
                  onChange={(e) => set('validUntil', e.target.value || null)}
                />
              </div>
              <div className="flex items-end pb-0.5">
                <label className="flex items-center gap-2 cursor-pointer">
                  <button
                    type="button"
                    onClick={() => set('active', !form.active)}
                    className={form.active ? 'text-green-600' : 'text-gray-400'}
                  >
                    {form.active ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}
                  </button>
                  <span className="text-sm">Active</span>
                </label>
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <Button onClick={handleSave} disabled={saving}>
                {saving ? <Loader2 className="w-4 h-4 animate-spin mr-1" /> : <Save className="w-4 h-4 mr-1" />}
                {editingId ? 'Update Coupon' : 'Create Coupon'}
              </Button>
              <Button variant="outline" onClick={() => setShowForm(false)}>
                <X className="w-4 h-4 mr-1" /> Cancel
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ========== Coupons Table ========== */}
      <Card>
        <CardHeader>
          <CardTitle>All Coupons</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Code</TableHead>
                  <TableHead>Discount</TableHead>
                  <TableHead>Plans</TableHead>
                  <TableHead>Redemptions</TableHead>
                  <TableHead>Valid From</TableHead>
                  <TableHead>Valid Until</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-28">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {coupons.map((c) => (
                  <TableRow key={c.id} className={!c.active ? 'opacity-50' : ''}>
                    <TableCell>
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono font-semibold text-sm">{c.code}</span>
                        <button onClick={() => copyCode(c.code)} className="text-gray-400 hover:text-gray-600">
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </TableCell>
                    <TableCell>
                      {c.discountType === 'PERCENTAGE'
                        ? `${c.discountValue}%`
                        : `$${(c.discountValue / 100).toFixed(2)}`}
                    </TableCell>
                    <TableCell>
                      {c.applicablePlans
                        ? c.applicablePlans.split(',').map((p) => (
                            <Badge key={p} variant="outline" className="mr-1 text-xs">
                              {p.trim()}
                            </Badge>
                          ))
                        : <span className="text-gray-400">All</span>}
                    </TableCell>
                    <TableCell>
                      {c.currentRedemptions}
                      {c.maxRedemptions ? ` / ${c.maxRedemptions}` : ''}
                    </TableCell>
                    <TableCell>{fmtDate(c.validFrom)}</TableCell>
                    <TableCell>{fmtDate(c.validUntil)}</TableCell>
                    <TableCell>
                      {c.valid ? (
                        <Badge className="bg-green-100 text-green-800">
                          <CheckCircle2 className="w-3 h-3 mr-1" /> Valid
                        </Badge>
                      ) : (
                        <Badge className="bg-red-100 text-red-800">
                          <XCircle className="w-3 h-3 mr-1" /> Expired
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(c)}>
                          <Pencil className="w-4 h-4" />
                        </Button>
                        {c.active && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeactivate(c.id)}
                            className="text-red-500 hover:text-red-700"
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
                {coupons.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                      No coupons yet. Click "New Coupon" to create a promotional code.
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
