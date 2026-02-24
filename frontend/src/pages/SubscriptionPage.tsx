import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { subscriptionService } from '@/services/subscriptionService'
import { organizationService } from '@/services/organizationService'
import { planDefinitionService, couponService } from '@/services/adminCatalogService'
import { useAuthStore } from '@/store/authStore'
import type { PlanDefinition, CouponValidationResponse, Organization, Subscription } from '@/types'
import {
  Crown,
  Clock,
  CheckCircle,
  AlertTriangle,
  XCircle,
  Loader2,
  Zap,
  CreditCard,
  Calendar,
  Shield,
  ExternalLink,
  Rocket,
  Building2,
  BadgeDollarSign,
  Tag,
  Eye,
  RefreshCw,
  ArrowUpDown,
  Search,
} from 'lucide-react'

// ---------------------------------------------------------------------------
// Status badge config
// ---------------------------------------------------------------------------
const STATUS_CONFIG: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
  TRIAL: { label: 'Trial', color: 'bg-blue-100 text-blue-800', icon: <Clock className="w-4 h-4" /> },
  ACTIVE: { label: 'Active', color: 'bg-green-100 text-green-800', icon: <CheckCircle className="w-4 h-4" /> },
  PAST_DUE: { label: 'Past Due', color: 'bg-yellow-100 text-yellow-800', icon: <AlertTriangle className="w-4 h-4" /> },
  CANCELLED: { label: 'Cancelled', color: 'bg-gray-100 text-gray-800', icon: <XCircle className="w-4 h-4" /> },
  EXPIRED: { label: 'Expired', color: 'bg-red-100 text-red-800', icon: <XCircle className="w-4 h-4" /> },
  SUSPENDED: { label: 'Suspended', color: 'bg-red-100 text-red-800', icon: <Shield className="w-4 h-4" /> },
}

// ---------------------------------------------------------------------------
// Plan icon resolver
// ---------------------------------------------------------------------------
function planIcon(key: string) {
  switch (key) {
    case 'STARTER': return <Rocket className="w-5 h-5" />
    case 'PROFESSIONAL': return <Zap className="w-5 h-5" />
    case 'BUSINESS': return <Building2 className="w-5 h-5" />
    case 'ENTERPRISE': return <Crown className="w-5 h-5" />
    default: return <Zap className="w-5 h-5" />
  }
}

function centsToDisplay(cents: number): string {
  return `$${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`
}

/** Comparison-table row definitions derived from PlanDefinition */
const TABLE_ROWS: { label: string; getValue: (p: PlanDefinition, cycle: BillingCycle) => string }[] = [
  { label: 'Monthly', getValue: (p) => centsToDisplay(p.monthlyPrice) },
  { label: 'Annual', getValue: (p) => centsToDisplay(p.annualPrice) },
  { label: 'Invoices / mo', getValue: (p) => p.invoicesPerMonth },
  { label: 'Users', getValue: (p) => p.maxUsers },
  { label: 'Organizations', getValue: (p) => p.maxOrganizations },
  { label: 'Email Inboxes', getValue: (p) => p.maxEmailInboxes },
  { label: 'Storage', getValue: (p) => p.storage },
  { label: 'Approval', getValue: (p) => p.approvalType },
  { label: 'Support', getValue: (p) => p.supportLevel },
  { label: 'Uptime SLA', getValue: (p) => p.uptimeSla },
]

// ---------------------------------------------------------------------------
// Billing toggle type
// ---------------------------------------------------------------------------
type BillingCycle = 'MONTHLY' | 'ANNUAL'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function formatDate(dateStr?: string): string {
  if (!dateStr) return 'N/A'
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------
export function SubscriptionPage() {
  const { user } = useAuthStore()
  const isSuperAdmin = user?.role === 'SUPER_ADMIN'

  if (isSuperAdmin) {
    return <SuperAdminSubscriptionView />
  }
  return <OrgAdminSubscriptionView />
}

// ===========================================================================
// SUPER ADMIN VIEW — manage all organizations' subscriptions
// ===========================================================================

function SuperAdminSubscriptionView() {
  const queryClient = useQueryClient()
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null)
  const [orgSubscription, setOrgSubscription] = useState<Subscription | null>(null)
  const [loadingSub, setLoadingSub] = useState(false)
  const [changePlanOrg, setChangePlanOrg] = useState<Organization | null>(null)
  const [changePlanValue, setChangePlanValue] = useState('')
  const [changePlanCycle, setChangePlanCycle] = useState('MONTHLY')
  const [changePlanDays, setChangePlanDays] = useState('365')
  const [reactivateOrg, setReactivateOrg] = useState<Organization | null>(null)
  const [reactivateDays, setReactivateDays] = useState('30')

  // Fetch all organizations
  const { data: orgsData, isLoading: orgsLoading } = useQuery({
    queryKey: ['super-admin-orgs-subscriptions'],
    queryFn: () => organizationService.getOrganizations(0, 200),
  })

  // Fetch plans for reference
  const { data: plans = [] } = useQuery<PlanDefinition[]>({
    queryKey: ['plan-definitions-active'],
    queryFn: () => planDefinitionService.getActivePlans(),
  })

  const organizations = orgsData?.content ?? []
  const filtered = organizations.filter((o) =>
    o.name.toLowerCase().includes(searchTerm.toLowerCase())
  )

  // View subscription details for an org
  const handleViewSubscription = async (org: Organization) => {
    setSelectedOrg(org)
    setLoadingSub(true)
    try {
      const sub = await subscriptionService.getSubscriptionByOrg(org.id)
      setOrgSubscription(sub)
    } catch {
      setOrgSubscription(null)
    } finally {
      setLoadingSub(false)
    }
  }

  // Change plan mutation
  const changePlanMutation = useMutation({
    mutationFn: () => {
      if (!changePlanOrg) throw new Error('No org selected')
      return subscriptionService.adminChangePlan(
        changePlanOrg.id,
        changePlanValue,
        changePlanCycle,
        parseInt(changePlanDays) || 365,
      )
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['super-admin-orgs-subscriptions'] })
      setChangePlanOrg(null)
      setChangePlanValue('')
    },
  })

  // Reactivate mutation
  const reactivateMutation = useMutation({
    mutationFn: () => {
      if (!reactivateOrg) throw new Error('No org selected')
      return subscriptionService.reactivateSubscription(
        reactivateOrg.id,
        parseInt(reactivateDays) || 30,
      )
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['super-admin-orgs-subscriptions'] })
      setReactivateOrg(null)
    },
  })

  if (orgsLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <BadgeDollarSign className="w-7 h-7 text-primary-600" />
          Subscription Management
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          View and manage subscriptions for all organizations.
        </p>
      </div>

      {/* Search */}
      <div className="relative max-w-md">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search organizations..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-10 pr-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
        />
      </div>

      {/* Organizations Table */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-gray-50">
                <th className="text-left py-3 px-4 font-semibold text-gray-700">Organization</th>
                <th className="text-left py-3 px-4 font-semibold text-gray-700">Plan</th>
                <th className="text-left py-3 px-4 font-semibold text-gray-700">Status</th>
                <th className="text-left py-3 px-4 font-semibold text-gray-700">Expires / Trial Ends</th>
                <th className="text-left py-3 px-4 font-semibold text-gray-700">Access</th>
                <th className="text-right py-3 px-4 font-semibold text-gray-700">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-8 text-center text-gray-400">
                    No organizations found
                  </td>
                </tr>
              ) : (
                filtered.map((org) => {
                  const statusCfg = STATUS_CONFIG[org.subscriptionStatus || 'TRIAL'] || STATUS_CONFIG.TRIAL
                  return (
                    <tr key={org.id} className="border-b hover:bg-gray-50 transition-colors">
                      <td className="py-3 px-4">
                        <div className="font-medium text-gray-900">{org.name}</div>
                        <div className="text-xs text-gray-400">{org.slug}</div>
                      </td>
                      <td className="py-3 px-4">
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium bg-indigo-100 text-indigo-700">
                          {planIcon(org.subscriptionPlan || 'TRIAL')}
                          {org.subscriptionPlan || 'TRIAL'}
                        </span>
                      </td>
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${statusCfg.color}`}
                        >
                          {statusCfg.icon}
                          {statusCfg.label}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-gray-600 text-xs">
                        {org.subscriptionExpiresAt
                          ? formatDate(org.subscriptionExpiresAt)
                          : org.remainingTrialDays !== undefined
                          ? `${org.remainingTrialDays} days left`
                          : 'N/A'}
                      </td>
                      <td className="py-3 px-4">
                        {org.hasAccess ? (
                          <span className="inline-flex items-center gap-1 text-green-600 text-xs font-medium">
                            <CheckCircle className="w-3.5 h-3.5" /> Yes
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-red-500 text-xs font-medium">
                            <XCircle className="w-3.5 h-3.5" /> No
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => handleViewSubscription(org)}
                            title="View Details"
                            className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-primary-50 rounded transition-colors"
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => {
                              setChangePlanOrg(org)
                              setChangePlanValue(org.subscriptionPlan || 'STARTER')
                            }}
                            title="Change Plan"
                            className="p-1.5 text-gray-500 hover:text-indigo-600 hover:bg-indigo-50 rounded transition-colors"
                          >
                            <ArrowUpDown className="w-4 h-4" />
                          </button>
                          {(org.subscriptionStatus === 'EXPIRED' ||
                            org.subscriptionStatus === 'CANCELLED' ||
                            org.subscriptionStatus === 'SUSPENDED') && (
                            <button
                              onClick={() => setReactivateOrg(org)}
                              title="Reactivate"
                              className="p-1.5 text-gray-500 hover:text-green-600 hover:bg-green-50 rounded transition-colors"
                            >
                              <RefreshCw className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Summary stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {(['TRIAL', 'ACTIVE', 'EXPIRED', 'CANCELLED'] as const).map((status) => {
          const count = organizations.filter((o) => o.subscriptionStatus === status).length
          const cfg = STATUS_CONFIG[status]
          return (
            <div key={status} className="bg-white rounded-xl shadow-sm p-4">
              <div className="flex items-center gap-2 mb-1">
                {cfg.icon}
                <span className="text-sm font-medium text-gray-600">{cfg.label}</span>
              </div>
              <p className="text-2xl font-bold text-gray-900">{count}</p>
            </div>
          )
        })}
      </div>

      {/* ========== View Subscription Detail Modal ========== */}
      {selectedOrg && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setSelectedOrg(null)}>
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-lg w-full mx-4" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
              <Building2 className="w-5 h-5 text-primary-600" />
              {selectedOrg.name} — Subscription
            </h3>
            {loadingSub ? (
              <div className="flex justify-center py-8">
                <Loader2 className="w-6 h-6 animate-spin text-primary-600" />
              </div>
            ) : orgSubscription ? (
              <div className="space-y-3 text-sm">
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-500 text-xs block mb-1">Plan</span>
                    <span className="font-semibold">{orgSubscription.planDisplayName}</span>
                  </div>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-500 text-xs block mb-1">Status</span>
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${(STATUS_CONFIG[orgSubscription.status] || STATUS_CONFIG.TRIAL).color}`}>
                      {(STATUS_CONFIG[orgSubscription.status] || STATUS_CONFIG.TRIAL).icon}
                      {orgSubscription.statusDisplayName}
                    </span>
                  </div>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-500 text-xs block mb-1">Billing Cycle</span>
                    <span className="font-medium">{orgSubscription.billingCycle || 'N/A'}</span>
                  </div>
                  <div className="p-3 bg-gray-50 rounded-lg">
                    <span className="text-gray-500 text-xs block mb-1">Has Access</span>
                    <span className={`font-medium ${orgSubscription.hasAccess ? 'text-green-600' : 'text-red-500'}`}>
                      {orgSubscription.hasAccess ? 'Yes' : 'No'}
                    </span>
                  </div>
                  {orgSubscription.trialEndDate && (
                    <div className="p-3 bg-gray-50 rounded-lg">
                      <span className="text-gray-500 text-xs block mb-1">Trial Ends</span>
                      <span className="font-medium">{formatDate(orgSubscription.trialEndDate)}</span>
                    </div>
                  )}
                  {orgSubscription.subscriptionEndDate && (
                    <div className="p-3 bg-gray-50 rounded-lg">
                      <span className="text-gray-500 text-xs block mb-1">Renewal Date</span>
                      <span className="font-medium">{formatDate(orgSubscription.subscriptionEndDate)}</span>
                    </div>
                  )}
                  {orgSubscription.remainingTrialDays !== undefined && orgSubscription.status === 'TRIAL' && (
                    <div className="p-3 bg-blue-50 rounded-lg col-span-2">
                      <span className="text-blue-500 text-xs block mb-1">Trial Remaining</span>
                      <span className="font-semibold text-blue-700">{orgSubscription.remainingTrialDays} days</span>
                    </div>
                  )}
                  {orgSubscription.cancelledAt && (
                    <div className="p-3 bg-red-50 rounded-lg col-span-2">
                      <span className="text-red-500 text-xs block mb-1">Cancelled</span>
                      <span className="font-medium text-red-700">
                        {formatDate(orgSubscription.cancelledAt)}
                        {orgSubscription.cancellationReason && ` — ${orgSubscription.cancellationReason}`}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <p className="text-gray-500 text-center py-4">No subscription data available.</p>
            )}
            <div className="flex justify-end mt-5">
              <button
                onClick={() => setSelectedOrg(null)}
                className="px-4 py-2 rounded-lg font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== Change Plan Modal ========== */}
      {changePlanOrg && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setChangePlanOrg(null)}>
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-md w-full mx-4" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Change Plan — {changePlanOrg.name}
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">New Plan</label>
                <select
                  value={changePlanValue}
                  onChange={(e) => setChangePlanValue(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                >
                  <option value="">Select plan...</option>
                  {plans.map((p) => (
                    <option key={p.planKey} value={p.planKey}>
                      {p.displayName} ({centsToDisplay(p.monthlyPrice)}/mo)
                    </option>
                  ))}
                  <option value="TRIAL">Trial (Free)</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Billing Cycle</label>
                <select
                  value={changePlanCycle}
                  onChange={(e) => setChangePlanCycle(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="ANNUAL">Annual</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Duration (days)</label>
                <input
                  type="number"
                  value={changePlanDays}
                  onChange={(e) => setChangePlanDays(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  min="1"
                />
              </div>
              {changePlanMutation.isError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                  {(changePlanMutation.error as Error)?.message || 'Failed to change plan'}
                </div>
              )}
            </div>
            <div className="flex gap-3 mt-5">
              <button
                onClick={() => setChangePlanOrg(null)}
                className="flex-1 py-2.5 px-4 rounded-lg font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => changePlanMutation.mutate()}
                disabled={changePlanMutation.isPending || !changePlanValue}
                className="flex-1 py-2.5 px-4 rounded-lg font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:opacity-50 transition-colors"
              >
                {changePlanMutation.isPending ? (
                  <span className="flex items-center justify-center">
                    <Loader2 className="w-4 h-4 animate-spin mr-2" />
                    Updating…
                  </span>
                ) : (
                  'Update Plan'
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ========== Reactivate Modal ========== */}
      {reactivateOrg && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50" onClick={() => setReactivateOrg(null)}>
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-sm w-full mx-4" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              Reactivate — {reactivateOrg.name}
            </h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Additional Days</label>
                <input
                  type="number"
                  value={reactivateDays}
                  onChange={(e) => setReactivateDays(e.target.value)}
                  className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none"
                  min="1"
                />
              </div>
              {reactivateMutation.isError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                  {(reactivateMutation.error as Error)?.message || 'Failed to reactivate'}
                </div>
              )}
            </div>
            <div className="flex gap-3 mt-5">
              <button
                onClick={() => setReactivateOrg(null)}
                className="flex-1 py-2.5 px-4 rounded-lg font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => reactivateMutation.mutate()}
                disabled={reactivateMutation.isPending}
                className="flex-1 py-2.5 px-4 rounded-lg font-medium text-white bg-green-600 hover:bg-green-700 disabled:opacity-50 transition-colors"
              >
                {reactivateMutation.isPending ? (
                  <span className="flex items-center justify-center">
                    <Loader2 className="w-4 h-4 animate-spin mr-2" />
                    Reactivating…
                  </span>
                ) : (
                  'Reactivate'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// ===========================================================================
// ORG ADMIN VIEW — existing subscription page for normal org admins
// ===========================================================================

function OrgAdminSubscriptionView() {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const [searchParams, setSearchParams] = useSearchParams()
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null)
  const [billingCycle, setBillingCycle] = useState<BillingCycle>('MONTHLY')
  const [showCancelConfirm, setShowCancelConfirm] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [checkoutStatus, setCheckoutStatus] = useState<'success' | 'cancelled' | null>(null)

  // Coupon state
  const [couponCode, setCouponCode] = useState('')
  const [couponResult, setCouponResult] = useState<CouponValidationResponse | null>(null)
  const [validatingCoupon, setValidatingCoupon] = useState(false)

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN'

  // Handle Stripe Checkout redirect back
  // Backend uses: ?success=true&session_id=xxx  or  ?canceled=true
  useEffect(() => {
    const success = searchParams.get('success')
    const canceled = searchParams.get('canceled')
    if (success === 'true') {
      setCheckoutStatus('success')
      setSearchParams({}, { replace: true })
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
    } else if (canceled === 'true') {
      setCheckoutStatus('cancelled')
      setSearchParams({}, { replace: true })
    }
  }, [searchParams, setSearchParams, queryClient])

  // Fetch plans from API
  const {
    data: plans = [],
    isLoading: plansLoading,
  } = useQuery<PlanDefinition[]>({
    queryKey: ['plan-definitions-active'],
    queryFn: () => planDefinitionService.getActivePlans(),
  })

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) return
    setValidatingCoupon(true)
    setCouponResult(null)
    try {
      const result = await couponService.validateCoupon(
        couponCode.trim().toUpperCase(),
        selectedPlan || plans[0]?.planKey || 'STARTER',
        billingCycle,
      )
      setCouponResult(result)
    } catch {
      setCouponResult({ valid: false, message: 'Invalid coupon code', code: '', discountType: 'PERCENTAGE', discountValue: 0, originalPrice: 0, discountedPrice: 0, discountAmount: 0 })
    } finally {
      setValidatingCoupon(false)
    }
  }

  const {
    data: subscription,
    isLoading: subLoading,
    error: subError,
  } = useQuery({
    queryKey: ['subscription'],
    queryFn: () => subscriptionService.getCurrentSubscription(),
  })

  const upgradeMutation = useMutation({
    mutationFn: (plan: string) =>
      subscriptionService.upgradeSubscription({
        plan: plan as any,
        billingCycle,
      }),
    onSuccess: (data) => {
      if (data.checkoutUrl) {
        window.location.href = data.checkoutUrl
        return
      }
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      setSelectedPlan(null)
    },
  })

  const cancelMutation = useMutation({
    mutationFn: (reason?: string) => subscriptionService.cancelSubscription(reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscription'] })
      setShowCancelConfirm(false)
      setCancelReason('')
    },
  })

  if (subLoading || plansLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
      </div>
    )
  }

  if (subError) {
    return (
      <div className="p-6 bg-red-50 border border-red-200 rounded-xl text-red-600">
        Failed to load subscription information. Please try again.
      </div>
    )
  }

  const statusConfig = STATUS_CONFIG[subscription?.status || 'TRIAL']
  const currentPlan = subscription?.plan || 'TRIAL'

  return (
    <div className="space-y-8">
      {/* ============ Page Header ============ */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
          <BadgeDollarSign className="w-7 h-7 text-primary-600" />
          Pricing Plans
        </h1>
        <p className="text-gray-500 text-sm mt-1">
          Choose the plan that fits your business. Upgrade, downgrade, or cancel
          at any time.
        </p>
      </div>

      {/* ============ Checkout Status Banners ============ */}
      {checkoutStatus === 'success' && (
        <div className="p-4 bg-green-50 border border-green-200 rounded-xl flex items-center gap-3">
          <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0" />
          <div>
            <p className="text-green-800 font-medium">Payment successful!</p>
            <p className="text-green-600 text-sm">
              Your subscription is being activated. This may take a moment.
            </p>
          </div>
        </div>
      )}
      {checkoutStatus === 'cancelled' && (
        <div className="p-4 bg-yellow-50 border border-yellow-200 rounded-xl flex items-center gap-3">
          <AlertTriangle className="w-5 h-5 text-yellow-600 flex-shrink-0" />
          <div>
            <p className="text-yellow-800 font-medium">Checkout cancelled</p>
            <p className="text-yellow-600 text-sm">
              You can try upgrading again when you're ready.
            </p>
          </div>
        </div>
      )}

      {/* ============ Current Subscription Status Card ============ */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <Crown className="w-6 h-6 text-primary-600" />
              <h2 className="text-lg font-semibold text-gray-900">
                {currentPlan === 'TRIAL' ? 'Free Trial' : `${currentPlan} Plan`}
              </h2>
              <span
                className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium ${statusConfig.color}`}
              >
                {statusConfig.icon}
                {statusConfig.label}
              </span>
            </div>

            {subscription?.status === 'TRIAL' && (
              <div className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded-lg">
                <div className="flex items-center gap-2 text-blue-800">
                  <Clock className="w-4 h-4" />
                  <span className="text-sm font-medium">
                    {subscription.remainingTrialDays !== undefined
                      ? `${subscription.remainingTrialDays} days remaining in trial`
                      : 'Trial period active'}
                  </span>
                </div>
                <p className="text-blue-600 text-xs mt-1">
                  Trial ends on {formatDate(subscription.trialEndDate)}
                </p>
              </div>
            )}

            {subscription?.status === 'EXPIRED' && (
              <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg">
                <div className="flex items-center gap-2 text-red-800">
                  <AlertTriangle className="w-4 h-4" />
                  <span className="text-sm font-medium">
                    Your subscription has expired
                  </span>
                </div>
                <p className="text-red-600 text-xs mt-1">
                  Choose a plan below to continue using SyncLedger.
                </p>
              </div>
            )}
          </div>

          {isAdmin && subscription?.status !== 'CANCELLED' && currentPlan !== 'TRIAL' && (
            <button
              onClick={() => setShowCancelConfirm(true)}
              className="text-sm text-red-500 hover:text-red-700"
            >
              Cancel subscription
            </button>
          )}
        </div>

        {/* Subscription Details */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
          <div className="p-4 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-2 text-gray-500 text-sm mb-1">
              <Calendar className="w-4 h-4" />
              Start Date
            </div>
            <p className="font-medium text-gray-900">
              {formatDate(
                subscription?.status === 'TRIAL'
                  ? subscription.trialStartDate
                  : subscription?.subscriptionStartDate
              )}
            </p>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-2 text-gray-500 text-sm mb-1">
              <Calendar className="w-4 h-4" />
              {subscription?.status === 'TRIAL' ? 'Trial Ends' : 'Renewal Date'}
            </div>
            <p className="font-medium text-gray-900">
              {formatDate(
                subscription?.status === 'TRIAL'
                  ? subscription.trialEndDate
                  : subscription?.subscriptionEndDate
              )}
            </p>
          </div>
          <div className="p-4 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-2 text-gray-500 text-sm mb-1">
              <CreditCard className="w-4 h-4" />
              Billing
            </div>
            <p className="font-medium text-gray-900">
              {subscription?.status === 'TRIAL'
                ? 'Free Trial'
                : subscription?.billingCycle || 'N/A'}
            </p>
          </div>
        </div>
      </div>

      {/* ============ Billing cycle toggle ============ */}
      {isAdmin && (
        <div className="flex items-center justify-center gap-3">
          <span
            className={`text-sm font-medium ${billingCycle === 'MONTHLY' ? 'text-gray-900' : 'text-gray-400'}`}
          >
            Monthly
          </span>
          <button
            onClick={() =>
              setBillingCycle((c) => (c === 'MONTHLY' ? 'ANNUAL' : 'MONTHLY'))
            }
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              billingCycle === 'ANNUAL' ? 'bg-primary-600' : 'bg-gray-300'
            }`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                billingCycle === 'ANNUAL' ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
          <span
            className={`text-sm font-medium ${billingCycle === 'ANNUAL' ? 'text-gray-900' : 'text-gray-400'}`}
          >
            Annual
          </span>
          {billingCycle === 'ANNUAL' && (
            <span className="ml-1 text-xs font-semibold text-green-700 bg-green-100 px-2 py-0.5 rounded-full">
              Save ~17%
            </span>
          )}
        </div>
      )}

      {/* ============ Pricing Comparison Table ============ */}
      {isAdmin && (
        <div className="bg-white rounded-xl shadow-sm overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              {/* ---- Header row ---- */}
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-4 px-5 font-semibold text-gray-700 w-44" />
                  {plans.map((p) => (
                    <th
                      key={p.planKey}
                      className={`py-4 px-4 text-center ${
                        p.highlight
                          ? 'bg-primary-50'
                          : ''
                      }`}
                    >
                      <div className="flex flex-col items-center gap-1">
                        <span
                          className={`flex items-center gap-1.5 font-bold text-base ${
                            p.highlight ? 'text-primary-700' : 'text-gray-900'
                          }`}
                        >
                          {planIcon(p.planKey)}
                          {p.displayName}
                        </span>
                        {p.planKey === currentPlan && (
                          <span className="text-[10px] font-medium text-primary-600 bg-primary-100 px-2 py-0.5 rounded-full">
                            Current
                          </span>
                        )}
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>

              {/* ---- Body rows ---- */}
              <tbody>
                {TABLE_ROWS.map((row, idx) => (
                  <tr
                    key={row.label}
                    className={idx % 2 === 0 ? 'bg-gray-50/60' : ''}
                  >
                    <td className="py-3 px-5 font-semibold text-gray-700">
                      {row.label}
                    </td>
                    {plans.map((p) => (
                      <td
                        key={p.planKey}
                        className={`py-3 px-4 text-center text-gray-700 ${
                          p.highlight ? 'bg-primary-50/60' : ''
                        }`}
                      >
                        {/* Show the right price based on billing toggle */}
                        {row.label === 'Monthly' && billingCycle === 'ANNUAL'
                          ? <span className="text-gray-400 line-through text-xs">{centsToDisplay(p.monthlyPrice)}</span>
                          : row.label === 'Annual' && billingCycle === 'MONTHLY'
                          ? <span className="text-gray-400 text-xs">{centsToDisplay(p.annualPrice)}/yr</span>
                          : row.getValue(p, billingCycle)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>

              {/* ---- CTA row ---- */}
              <tfoot>
                <tr className="border-t border-gray-200">
                  <td className="py-5 px-5" />
                  {plans.map((p) => {
                    const isCurrent = p.planKey === currentPlan
                    const isEnterprise = p.planKey === 'ENTERPRISE'
                    return (
                      <td key={p.planKey} className={`py-5 px-4 text-center ${p.highlight ? 'bg-primary-50/60' : ''}`}>
                        {isCurrent ? (
                          <span className="inline-flex items-center gap-1 px-4 py-2 rounded-lg font-medium text-primary-600 bg-primary-100 text-sm cursor-default">
                            <CheckCircle className="w-4 h-4" />
                            Current Plan
                          </span>
                        ) : (
                          <button
                            onClick={() => {
                              setSelectedPlan(p.planKey)
                              setCheckoutStatus(null)
                              if (isEnterprise) {
                                window.open('mailto:sales@vedvix.com?subject=SyncLedger%20Enterprise%20Inquiry', '_blank')
                              } else {
                                upgradeMutation.mutate(p.planKey)
                              }
                            }}
                            disabled={upgradeMutation.isPending && selectedPlan === p.planKey}
                            className={`inline-flex items-center gap-1.5 px-5 py-2 rounded-lg font-medium text-sm transition-colors ${
                              p.highlight
                                ? 'bg-primary-600 text-white hover:bg-primary-700'
                                : 'bg-gray-800 text-white hover:bg-gray-900'
                            }`}
                          >
                            {upgradeMutation.isPending && selectedPlan === p.planKey ? (
                              <>
                                <Loader2 className="w-4 h-4 animate-spin" />
                                Processing…
                              </>
                            ) : (
                              <>
                                {isEnterprise ? (
                                  <ExternalLink className="w-4 h-4" />
                                ) : (
                                  <Zap className="w-4 h-4" />
                                )}
                                {isEnterprise ? 'Contact Sales' : 'Get Started'}
                              </>
                            )}
                          </button>
                        )}
                      </td>
                    )
                  })}
                </tr>
              </tfoot>
            </table>
          </div>

          {upgradeMutation.isError && (
            <div className="mx-5 mb-5 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
              {(upgradeMutation.error as Error)?.message || 'Failed to process upgrade. Please try again.'}
            </div>
          )}
        </div>
      )}

      {/* ============ Coupon / Voucher Code ============ */}
      {isAdmin && (
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-base font-semibold text-gray-900 flex items-center gap-2 mb-3">
            <Tag className="w-5 h-5 text-primary-600" />
            Have a coupon or voucher code?
          </h3>
          <div className="flex gap-3 items-start">
            <div className="flex-1 max-w-xs">
              <input
                className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:outline-none font-mono uppercase"
                placeholder="Enter code"
                value={couponCode}
                onChange={(e) => { setCouponCode(e.target.value.toUpperCase()); setCouponResult(null) }}
              />
            </div>
            <button
              onClick={handleApplyCoupon}
              disabled={validatingCoupon || !couponCode.trim()}
              className="px-4 py-2 rounded-lg font-medium text-sm bg-primary-600 text-white hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {validatingCoupon ? <Loader2 className="w-4 h-4 animate-spin" /> : 'Apply'}
            </button>
          </div>
          {couponResult && (
            <div className={`mt-3 text-sm flex items-start gap-2 ${couponResult.valid ? 'text-green-700' : 'text-red-600'}`}>
              {couponResult.valid ? <CheckCircle className="w-4 h-4 mt-0.5 flex-shrink-0" /> : <XCircle className="w-4 h-4 mt-0.5 flex-shrink-0" />}
              <div>
                <p className="font-medium">{couponResult.message}</p>
                {couponResult.valid && (couponResult.discountAmount ?? 0) > 0 && (
                  <p className="text-xs mt-0.5 text-green-600">
                    {centsToDisplay(couponResult.originalPrice ?? 0)} → {centsToDisplay(couponResult.discountedPrice ?? 0)} (save {centsToDisplay(couponResult.discountAmount ?? 0)})
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {/* ============ Cancel Confirmation Modal ============ */}
      {showCancelConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-md w-full mx-4">
            <h3 className="text-lg font-semibold text-gray-900 mb-2">
              Cancel Subscription
            </h3>
            <p className="text-gray-500 text-sm mb-4">
              Are you sure you want to cancel? You'll lose access to premium
              features at the end of your current billing period.
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Reason for cancellation (optional)
              </label>
              <textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                rows={3}
                placeholder="Tell us why you're leaving..."
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowCancelConfirm(false)
                  setCancelReason('')
                }}
                className="flex-1 py-2.5 px-4 rounded-lg font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
              >
                Keep Subscription
              </button>
              <button
                onClick={() => cancelMutation.mutate(cancelReason || undefined)}
                disabled={cancelMutation.isPending}
                className="flex-1 py-2.5 px-4 rounded-lg font-medium text-white bg-red-600 hover:bg-red-700 transition-colors"
              >
                {cancelMutation.isPending ? (
                  <span className="flex items-center justify-center">
                    <Loader2 className="w-4 h-4 animate-spin mr-2" />
                    Cancelling...
                  </span>
                ) : (
                  'Yes, Cancel'
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
