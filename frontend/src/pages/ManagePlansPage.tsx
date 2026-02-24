import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Package,
  Plus,
  Pencil,
  Trash2,
  Star,
  Loader2,
  ArrowLeft,
  Save,
  X,
  DollarSign,
  ToggleLeft,
  ToggleRight,
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
import { planDefinitionService } from '@/services/adminCatalogService'
import type { PlanDefinition, PlanDefinitionRequest } from '@/types'

// ---------- helpers ----------
function centsToDisplay(cents: number): string {
  return `$${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

// ---------- blank form state ----------
const BLANK_FORM: PlanDefinitionRequest = {
  planKey: '',
  displayName: '',
  description: '',
  monthlyPrice: 0,
  annualPrice: 0,
  invoicesPerMonth: '0',
  maxUsers: '0',
  maxOrganizations: '0',
  maxEmailInboxes: '0',
  storage: '0',
  approvalType: 'Basic',
  supportLevel: 'Email',
  uptimeSla: '99.5%',
  highlight: false,
  sortOrder: 0,
  active: true,
}

export default function ManagePlansPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { toast } = useToast()
  const [plans, setPlans] = useState<PlanDefinition[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  // Form state
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [form, setForm] = useState<PlanDefinitionRequest>(BLANK_FORM)

  useEffect(() => {
    if (user?.role !== 'SUPER_ADMIN') { navigate('/dashboard'); return }
    loadPlans()
  }, [user, navigate])

  const loadPlans = useCallback(async () => {
    try {
      setLoading(true)
      const data = await planDefinitionService.getAllPlans()
      setPlans(data)
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to load plans')
    } finally {
      setLoading(false)
    }
  }, [toast])

  const openCreate = () => {
    setEditingId(null)
    setForm({ ...BLANK_FORM, sortOrder: plans.length + 1 })
    setShowForm(true)
  }

  const openEdit = (plan: PlanDefinition) => {
    setEditingId(plan.id)
    setForm({
      planKey: plan.planKey,
      displayName: plan.displayName,
      description: plan.description || '',
      monthlyPrice: plan.monthlyPrice,
      annualPrice: plan.annualPrice,
      invoicesPerMonth: plan.invoicesPerMonth,
      maxUsers: plan.maxUsers,
      maxOrganizations: plan.maxOrganizations,
      maxEmailInboxes: plan.maxEmailInboxes,
      storage: plan.storage,
      approvalType: plan.approvalType,
      supportLevel: plan.supportLevel,
      uptimeSla: plan.uptimeSla,
      highlight: plan.highlight,
      sortOrder: plan.sortOrder,
      active: plan.active,
    })
    setShowForm(true)
  }

  const handleSave = async () => {
    if (!form.planKey || !form.displayName || form.monthlyPrice == null) {
      toast.error('Plan key, display name, and monthly price are required')
      return
    }
    try {
      setSaving(true)
      if (editingId) {
        await planDefinitionService.updatePlan(editingId, form)
        toast.success('Plan updated')
      } else {
        await planDefinitionService.createPlan(form)
        toast.success('Plan created')
      }
      setShowForm(false)
      loadPlans()
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to save plan')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('Deactivate this plan? It will no longer appear on the pricing page.')) return
    try {
      await planDefinitionService.deletePlan(id)
      toast.success('Plan deactivated')
      loadPlans()
    } catch (e: any) {
      toast.error(e?.response?.data?.message || 'Failed to deactivate plan')
    }
  }

  const set = (field: keyof PlanDefinitionRequest, value: any) =>
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
              <Package className="w-6 h-6 text-primary-600" />
              Manage Plans
            </h1>
            <p className="text-muted-foreground text-sm">
              Create, edit, or deactivate subscription plans — changes are reflected on the pricing page immediately.
            </p>
          </div>
        </div>
        {!showForm && (
          <Button onClick={openCreate}>
            <Plus className="w-4 h-4 mr-1" /> New Plan
          </Button>
        )}
      </div>

      {/* ========== Edit / Create Form ========== */}
      {showForm && (
        <Card>
          <CardHeader>
            <CardTitle>{editingId ? 'Edit Plan' : 'New Plan'}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            {/* Row 1 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Plan Key *</label>
                <input
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  placeholder="e.g. STARTER"
                  value={form.planKey}
                  onChange={(e) => set('planKey', e.target.value.toUpperCase())}
                  disabled={!!editingId}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Display Name *</label>
                <input
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  placeholder="e.g. Starter"
                  value={form.displayName}
                  onChange={(e) => set('displayName', e.target.value)}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <input
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  value={form.description || ''}
                  onChange={(e) => set('description', e.target.value)}
                />
              </div>
            </div>

            {/* Row 2 – Pricing */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Monthly Price (cents) *</label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="number"
                    className="w-full pl-8 pr-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                    value={form.monthlyPrice}
                    onChange={(e) => set('monthlyPrice', Number(e.target.value))}
                  />
                </div>
                <p className="text-xs text-gray-400 mt-0.5">{centsToDisplay(form.monthlyPrice)}/mo</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Annual Price (cents) *</label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                  <input
                    type="number"
                    className="w-full pl-8 pr-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                    value={form.annualPrice}
                    onChange={(e) => set('annualPrice', Number(e.target.value))}
                  />
                </div>
                <p className="text-xs text-gray-400 mt-0.5">{centsToDisplay(form.annualPrice)}/yr</p>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Sort Order</label>
                <input
                  type="number"
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  value={form.sortOrder ?? 0}
                  onChange={(e) => set('sortOrder', Number(e.target.value))}
                />
              </div>
              <div className="flex items-end gap-4 pb-0.5">
                <label className="flex items-center gap-2 cursor-pointer">
                  <button
                    type="button"
                    onClick={() => set('highlight', !form.highlight)}
                    className="text-primary-600"
                  >
                    {form.highlight ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6 text-gray-400" />}
                  </button>
                  <span className="text-sm">Highlight</span>
                </label>
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

            {/* Row 3 – Limits */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {([
                ['invoicesPerMonth', 'Invoices / mo'],
                ['maxUsers',        'Users'],
                ['maxOrganizations','Organizations'],
                ['maxEmailInboxes', 'Email Inboxes'],
                ['storage',         'Storage'],
                ['approvalType',    'Approval'],
                ['supportLevel',    'Support'],
                ['uptimeSla',       'Uptime SLA'],
              ] as [keyof PlanDefinitionRequest, string][]).map(([key, label]) => (
                <div key={key}>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                  <input
                    className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                    value={(form as any)[key] || ''}
                    onChange={(e) => set(key, e.target.value)}
                  />
                </div>
              ))}
            </div>

            {/* Actions */}
            <div className="flex gap-3 pt-2">
              <Button onClick={handleSave} disabled={saving}>
                {saving ? <Loader2 className="w-4 h-4 animate-spin mr-1" /> : <Save className="w-4 h-4 mr-1" />}
                {editingId ? 'Update Plan' : 'Create Plan'}
              </Button>
              <Button variant="outline" onClick={() => setShowForm(false)}>
                <X className="w-4 h-4 mr-1" /> Cancel
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* ========== Plans Table ========== */}
      <Card>
        <CardHeader>
          <CardTitle>All Plans</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-10">#</TableHead>
                  <TableHead>Plan Key</TableHead>
                  <TableHead>Display Name</TableHead>
                  <TableHead>Monthly</TableHead>
                  <TableHead>Annual</TableHead>
                  <TableHead>Invoices</TableHead>
                  <TableHead>Users</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="w-28">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {plans.map((p) => (
                  <TableRow key={p.id} className={!p.active ? 'opacity-50' : ''}>
                    <TableCell>{p.sortOrder}</TableCell>
                    <TableCell className="font-mono text-xs">{p.planKey}</TableCell>
                    <TableCell className="font-medium">
                      {p.displayName}
                      {p.highlight && (
                        <Star className="w-3.5 h-3.5 text-yellow-500 inline ml-1 -mt-0.5" />
                      )}
                    </TableCell>
                    <TableCell>{centsToDisplay(p.monthlyPrice)}</TableCell>
                    <TableCell>{centsToDisplay(p.annualPrice)}</TableCell>
                    <TableCell>{p.invoicesPerMonth}</TableCell>
                    <TableCell>{p.maxUsers}</TableCell>
                    <TableCell>
                      {p.active ? (
                        <Badge className="bg-green-100 text-green-800">Active</Badge>
                      ) : (
                        <Badge className="bg-gray-100 text-gray-600">Inactive</Badge>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1">
                        <Button variant="ghost" size="sm" onClick={() => openEdit(p)}>
                          <Pencil className="w-4 h-4" />
                        </Button>
                        {p.active && (
                          <Button variant="ghost" size="sm" onClick={() => handleDelete(p.id)} className="text-red-500 hover:text-red-700">
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
                {plans.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={9} className="text-center py-8 text-muted-foreground">
                      No plans defined yet. Click "New Plan" to create one.
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
