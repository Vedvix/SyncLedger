import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
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
  Sparkles,
} from 'lucide-react'

export function DashboardPage() {
  const { user } = useAuthStore()
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
  if (isLoading) {
    return (
      <div className="space-y-6 animate-fade-in">
        {/* Skeleton greeting */}
        <div className="h-24 rounded-2xl bg-white border border-gray-200 animate-pulse" />
        {/* Skeleton widgets */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => (
            <div key={i} className="h-48 rounded-lg bg-white border border-gray-200 animate-pulse" style={{ animationDelay: `${i * 100}ms` }} />
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-8 text-center animate-fade-in">
        <div className="w-16 h-16 rounded-2xl bg-red-100 flex items-center justify-center mx-auto mb-4">
          <AlertTriangle className="w-8 h-8 text-red-600" />
        </div>
        <h3 className="text-lg font-semibold text-red-800 mb-2">Failed to load dashboard</h3>
        <p className="text-red-600 mb-6 text-sm">We couldn't fetch your dashboard data. Please try again.</p>
        <button onClick={() => refetch()} className="px-6 py-2.5 bg-red-600 text-white rounded-lg hover:bg-red-700 font-medium text-sm transition-colors">
          Retry
        </button>
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in -m-6 p-6 min-h-screen bg-slate-50">
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

      {/* ─── Organization Context Banner ─── */}
      {user?.role === 'SUPER_ADMIN' ? (
        <div className="flex items-center gap-2 px-4 py-2.5 bg-indigo-50 border border-indigo-200 rounded-lg text-sm text-indigo-700">
          <Shield className="w-4 h-4 flex-shrink-0" />
          Viewing <strong className="mx-1">platform-wide</strong> data across all organizations.
        </div>
      ) : user?.organizationName ? (
        <div className="flex items-center gap-2 px-4 py-2.5 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-700">
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
        <div className="bg-blue-50 border border-blue-200 rounded-lg px-4 py-3 text-sm text-blue-700 flex items-center gap-2">
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
