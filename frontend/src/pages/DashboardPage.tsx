import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { dashboardService } from '@/services/dashboardService'
import { useAuthStore } from '@/store/authStore'
import { useDashboardStore } from '@/store/dashboardStore'
import { DashboardGrid, DashboardToolbar, DateRangeFilter, WidgetEditor } from '@/components/dashboard'
import type { WidgetConfig, DateFilter } from '@/types/dashboard'
import { resolveDateFilter } from '@/types/dashboard'
import {
  AlertTriangle,
  Building,
  Shield,
  FileText,
  Upload,
  Users,
  Mail,
  ArrowRight,
  TrendingUp,
  Clock,
  CheckCircle2,
  XCircle,
  Sparkles,
} from 'lucide-react'

// ─── Quick action definitions per role ───────────────────────────
const QUICK_ACTIONS = {
  SUPER_ADMIN: [
    { label: 'Organizations', desc: 'Manage organizations', icon: Building, to: '/super-admin', color: 'from-indigo-500 to-purple-500' },
    { label: 'AI Usage', desc: 'View platform analytics', icon: TrendingUp, to: '/super-admin/ai-usage', color: 'from-cyan-500 to-blue-500' },
    { label: 'Email Config', desc: 'Check email status', icon: Mail, to: '/microsoft-config', color: 'from-emerald-500 to-teal-500' },
    { label: 'All Invoices', desc: 'Platform-wide view', icon: FileText, to: '/invoices', color: 'from-orange-500 to-red-500' },
  ],
  ADMIN: [
    { label: 'Upload Invoice', desc: 'Process a new PDF', icon: Upload, to: '/invoices', color: 'from-blue-500 to-indigo-500' },
    { label: 'Team', desc: 'Manage team members', icon: Users, to: '/users', color: 'from-emerald-500 to-teal-500' },
    { label: 'Email Config', desc: 'Configure email polling', icon: Mail, to: '/microsoft-config', color: 'from-violet-500 to-purple-500' },
    { label: 'View Invoices', desc: 'Review & approve', icon: FileText, to: '/invoices', color: 'from-orange-500 to-amber-500' },
  ],
  APPROVER: [
    { label: 'Pending Review', desc: 'Invoices awaiting approval', icon: Clock, to: '/invoices?status=PENDING', color: 'from-amber-500 to-orange-500' },
    { label: 'Approved', desc: 'Recently approved', icon: CheckCircle2, to: '/invoices?status=APPROVED', color: 'from-emerald-500 to-green-500' },
    { label: 'Rejected', desc: 'Rejected invoices', icon: XCircle, to: '/invoices?status=REJECTED', color: 'from-red-500 to-rose-500' },
    { label: 'All Invoices', desc: 'View everything', icon: FileText, to: '/invoices', color: 'from-blue-500 to-indigo-500' },
  ],
  VIEWER: [
    { label: 'View Invoices', desc: 'Browse all invoices', icon: FileText, to: '/invoices', color: 'from-blue-500 to-indigo-500' },
    { label: 'Vendors', desc: 'View vendor list', icon: Building, to: '/vendors', color: 'from-emerald-500 to-teal-500' },
  ],
}

export function DashboardPage() {
  const { user } = useAuthStore()
  const navigate = useNavigate()
  const { getActiveDashboard, editMode } = useDashboardStore()
  const activeDashboard = getActiveDashboard()

  // Date filter state
  const [dateFilter, setDateFilter] = useState<DateFilter>({ preset: 'all' })
  const resolved = resolveDateFilter(dateFilter)

  const { data: stats, isLoading, error, refetch } = useQuery({
    queryKey: ['dashboardStats', resolved.startDate, resolved.endDate],
    queryFn: () => dashboardService.getStats(resolved.startDate, resolved.endDate),
    refetchInterval: 30000,
  })

  // Widget editor state
  const [editorOpen, setEditorOpen] = useState(false)
  const [editingWidget, setEditingWidget] = useState<WidgetConfig | undefined>()

  const handleAddWidget = () => {
    setEditingWidget(undefined)
    setEditorOpen(true)
  }

  const handleEditWidget = (widget: WidgetConfig) => {
    setEditingWidget(widget)
    setEditorOpen(true)
  }

  // Greeting based on time of day
  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'
  const userRole = (user?.role || 'VIEWER') as keyof typeof QUICK_ACTIONS
  const quickActions = QUICK_ACTIONS[userRole] || QUICK_ACTIONS.VIEWER

  if (isLoading) {
    return (
      <div className="space-y-6 animate-fade-in">
        {/* Skeleton greeting */}
        <div className="h-24 rounded-2xl animate-shimmer" />
        {/* Skeleton quick actions */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-28 rounded-xl animate-shimmer" style={{ animationDelay: `${i * 100}ms` }} />
          ))}
        </div>
        {/* Skeleton widgets */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-48 rounded-xl animate-shimmer" style={{ animationDelay: `${i * 100}ms` }} />
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-2xl p-8 text-center animate-fade-in">
        <div className="w-16 h-16 rounded-2xl bg-red-100 flex items-center justify-center mx-auto mb-4">
          <AlertTriangle className="w-8 h-8 text-red-500" />
        </div>
        <h3 className="text-lg font-semibold text-red-900 mb-2">Failed to load dashboard</h3>
        <p className="text-red-600 mb-6 text-sm">We couldn't fetch your dashboard data. Please try again.</p>
        <button onClick={() => refetch()} className="px-6 py-2.5 bg-red-600 text-white rounded-xl hover:bg-red-700 font-medium text-sm transition-colors">
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* ─── Welcome Banner ─── */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-primary-600 via-primary-500 to-indigo-500 text-white p-6 md:p-8">
        {/* Decorative elements */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/3" />
        <div className="absolute bottom-0 left-1/3 w-32 h-32 bg-white/5 rounded-full translate-y-1/2" />
        <Sparkles className="absolute top-4 right-4 w-6 h-6 text-white/20" />
        
        <div className="relative">
          <h1 className="text-2xl md:text-3xl font-bold mb-1">
            {greeting}, {user?.firstName}!
          </h1>
          <p className="text-primary-100 text-sm md:text-base max-w-lg">
            {user?.role === 'SUPER_ADMIN' 
              ? 'Here\'s an overview of the entire platform. Manage organizations, monitor usage, and keep everything running smoothly.'
              : user?.role === 'ADMIN'
              ? 'Manage your invoices, team, and configurations from your central hub.'
              : user?.role === 'APPROVER'
              ? 'Review pending invoices and keep the approval workflow moving.'
              : 'View invoices and track the status of your documents.'
            }
          </p>
          
          {/* Mini stats in banner */}
          {stats && (
            <div className="flex flex-wrap gap-6 mt-5">
              <div>
                <p className="text-2xl font-bold">{stats.totalInvoices || 0}</p>
                <p className="text-xs text-primary-200">Total Invoices</p>
              </div>
              <div>
                <p className="text-2xl font-bold">{stats.pendingInvoices || 0}</p>
                <p className="text-xs text-primary-200">Pending Review</p>
              </div>
              <div>
                <p className="text-2xl font-bold">{stats.approvedInvoices || 0}</p>
                <p className="text-xs text-primary-200">Approved</p>
              </div>
              {stats.totalAmount != null && (
                <div>
                  <p className="text-2xl font-bold">
                    ${(stats.totalAmount / 1000).toFixed(1)}k
                  </p>
                  <p className="text-xs text-primary-200">Total Value</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* ─── Quick Actions ─── */}
      <div>
        <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-3">Quick Actions</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 stagger-fade-in">
          {quickActions.map((action) => (
            <button
              key={action.label}
              onClick={() => navigate(action.to)}
              className="group relative overflow-hidden bg-white rounded-xl border border-gray-100 p-4 text-left hover:shadow-lg hover:border-gray-200 hover:-translate-y-0.5 transition-all duration-200"
            >
              <div className={`w-10 h-10 rounded-xl bg-gradient-to-br ${action.color} flex items-center justify-center mb-3 shadow-sm group-hover:shadow-md transition-shadow`}>
                <action.icon className="w-5 h-5 text-white" />
              </div>
              <p className="text-sm font-semibold text-gray-900 mb-0.5">{action.label}</p>
              <p className="text-xs text-gray-500">{action.desc}</p>
              <ArrowRight className="absolute top-4 right-4 w-4 h-4 text-gray-300 group-hover:text-gray-500 group-hover:translate-x-0.5 transition-all" />
            </button>
          ))}
        </div>
      </div>

      {/* ─── Organization Context Banner ─── */}
      {user?.role === 'SUPER_ADMIN' ? (
        <div className="flex items-center gap-2 px-4 py-2.5 bg-indigo-50 border border-indigo-100 rounded-xl text-sm text-indigo-700">
          <Shield className="w-4 h-4 flex-shrink-0" />
          Viewing <strong className="mx-1">platform-wide</strong> data across all organizations.
        </div>
      ) : user?.organizationName ? (
        <div className="flex items-center gap-2 px-4 py-2.5 bg-primary-50 border border-primary-100 rounded-xl text-sm text-primary-700">
          <Building className="w-4 h-4 flex-shrink-0" />
          Showing data for <strong className="mx-1">{user.organizationName}</strong>.
        </div>
      ) : null}

      {/* ─── Dashboard Toolbar & Widgets ─── */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-bold text-gray-900">Analytics</h2>
            <p className="text-sm text-gray-500 mt-0.5">
              {editMode
                ? 'Drag widgets to reorder, resize, or add new ones.'
                : 'Customize your dashboard with widgets and date filters.'
              }
            </p>
          </div>
        </div>
        <DashboardToolbar onAddWidget={handleAddWidget} onRefresh={() => refetch()} />
      </div>

      {/* Date Range Filter */}
      <DateRangeFilter value={dateFilter} onChange={setDateFilter} />

      {/* Edit mode hint */}
      {editMode && (
        <div className="bg-blue-50 border border-blue-200 rounded-xl px-4 py-3 text-sm text-blue-700 flex items-center gap-2">
          <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>
            <strong>Edit Mode:</strong> Hover over widgets to see controls. Drag the grip handle to reorder. 
            Use +/- to resize. All changes are auto-saved.
          </span>
        </div>
      )}

      {/* Widget Grid */}
      <DashboardGrid
        widgets={activeDashboard.widgets}
        stats={stats}
        editMode={editMode}
        onEditWidget={handleEditWidget}
      />

      {/* Widget Editor Dialog */}
      <WidgetEditor
        isOpen={editorOpen}
        onClose={() => { setEditorOpen(false); setEditingWidget(undefined) }}
        existingWidget={editingWidget}
      />
    </div>
  )
}
