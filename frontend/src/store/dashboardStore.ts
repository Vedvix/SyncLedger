import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { DashboardConfig, WidgetConfig, WidgetSize } from '@/types/dashboard'
import { SIZE_OPTIONS } from '@/types/dashboard'

// Generate unique id
const uid = () => `w_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`

// ─── Default Dashboard ───
const DEFAULT_WIDGETS: WidgetConfig[] = [
  {
    id: uid(), title: 'Total Invoices', chartType: 'kpiCard', dataSource: 'totalInvoices',
    size: 'small', colSpan: 3, rowSpan: 1, order: 0, color: '#3b82f6', visible: true,
    clickUrl: '/invoices',
  },
  {
    id: uid(), title: 'Pending Review', chartType: 'kpiCard', dataSource: 'pendingInvoices',
    size: 'small', colSpan: 3, rowSpan: 1, order: 1, color: '#f59e0b', visible: true,
    clickUrl: '/invoices?status=PENDING',
  },
  {
    id: uid(), title: 'Approval Rate', chartType: 'radialGauge', dataSource: 'approvalRate',
    size: 'small', colSpan: 3, rowSpan: 1, order: 2, color: '#22c55e', visible: true,
    clickUrl: '/invoices?status=APPROVED',
  },
  {
    id: uid(), title: 'Sync Success Rate', chartType: 'radialGauge', dataSource: 'syncRate',
    size: 'small', colSpan: 3, rowSpan: 1, order: 3, color: '#8b5cf6', visible: true,
    clickUrl: '/invoices?status=SYNCED',
  },
  {
    id: uid(), title: 'Invoice Volume Trend', chartType: 'area', dataSource: 'monthlyTrends',
    size: 'large', colSpan: 8, rowSpan: 1, order: 4, color: '#3b82f6', visible: true,
    subtitle: 'Monthly invoice count & value',
  },
  {
    id: uid(), title: 'Status Distribution', chartType: 'donut', dataSource: 'statusDistribution',
    size: 'medium', colSpan: 4, rowSpan: 1, order: 5, color: '#6366f1', visible: true,
    subtitle: 'Current invoice breakdown',
  },
  {
    id: uid(), title: 'Top Vendors', chartType: 'horizontalBar', dataSource: 'topVendors',
    size: 'large', colSpan: 6, rowSpan: 1, order: 6, color: '#3b82f6', visible: true,
    subtitle: 'By invoice count',
  },
  {
    id: uid(), title: 'Financial Summary', chartType: 'horizontalBar', dataSource: 'financialBreakdown',
    size: 'large', colSpan: 6, rowSpan: 1, order: 7, color: '#22c55e', visible: true,
    subtitle: 'Value by invoice status',
  },
  {
    id: uid(), title: 'Processing Throughput', chartType: 'bar', dataSource: 'processingThroughput',
    size: 'medium', colSpan: 4, rowSpan: 1, order: 8, color: '#6366f1', visible: true,
    subtitle: 'Invoices processed',
  },
  {
    id: uid(), title: 'Alerts & Actions', chartType: 'statusCards', dataSource: 'alertCards',
    size: 'large', colSpan: 8, rowSpan: 1, order: 9, visible: true,
  },
]

function createDefaultDashboard(): DashboardConfig {
  return {
    id: 'default',
    name: 'Default Dashboard',
    description: 'Standard invoice processing dashboard',
    widgets: DEFAULT_WIDGETS,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    isDefault: true,
  }
}

// ─── Store ───
interface DashboardStore {
  dashboards: DashboardConfig[]
  activeDashboardId: string
  editMode: boolean

  // Getters
  getActiveDashboard: () => DashboardConfig
  getDashboardList: () => { id: string; name: string }[]

  // Dashboard CRUD
  setActiveDashboard: (id: string) => void
  createDashboard: (name: string, description?: string) => string
  duplicateDashboard: (id: string) => string
  deleteDashboard: (id: string) => void
  renameDashboard: (id: string, name: string) => void

  // Edit mode
  toggleEditMode: () => void
  setEditMode: (on: boolean) => void

  // Widget CRUD
  addWidget: (widget: Omit<WidgetConfig, 'id' | 'order'>) => void
  updateWidget: (widgetId: string, updates: Partial<WidgetConfig>) => void
  removeWidget: (widgetId: string) => void
  reorderWidgets: (widgetIds: string[]) => void
  resizeWidget: (widgetId: string, size: WidgetSize) => void

  // Reset
  resetToDefault: () => void
}

export const useDashboardStore = create<DashboardStore>()(
  persist(
    (set, get) => ({
      dashboards: [createDefaultDashboard()],
      activeDashboardId: 'default',
      editMode: false,

      getActiveDashboard: () => {
        const state = get()
        return state.dashboards.find(d => d.id === state.activeDashboardId) || state.dashboards[0]
      },

      getDashboardList: () => get().dashboards.map(d => ({ id: d.id, name: d.name })),

      setActiveDashboard: (id) => set({ activeDashboardId: id }),

      createDashboard: (name, description) => {
        const id = `dash_${Date.now()}`
        set(state => ({
          dashboards: [...state.dashboards, {
            id,
            name,
            description,
            widgets: [],
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }],
          activeDashboardId: id,
          editMode: true,
        }))
        return id
      },

      duplicateDashboard: (id) => {
        const source = get().dashboards.find(d => d.id === id)
        if (!source) return id
        const newId = `dash_${Date.now()}`
        set(state => ({
          dashboards: [...state.dashboards, {
            ...source,
            id: newId,
            name: `${source.name} (copy)`,
            isDefault: false,
            widgets: source.widgets.map(w => ({ ...w, id: uid() })),
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          }],
          activeDashboardId: newId,
        }))
        return newId
      },

      deleteDashboard: (id) => {
        set(state => {
          const remaining = state.dashboards.filter(d => d.id !== id)
          if (remaining.length === 0) remaining.push(createDefaultDashboard())
          return {
            dashboards: remaining,
            activeDashboardId: state.activeDashboardId === id ? remaining[0].id : state.activeDashboardId,
          }
        })
      },

      renameDashboard: (id, name) => {
        set(state => ({
          dashboards: state.dashboards.map(d =>
            d.id === id ? { ...d, name, updatedAt: new Date().toISOString() } : d
          ),
        }))
      },

      toggleEditMode: () => set(state => ({ editMode: !state.editMode })),
      setEditMode: (on) => set({ editMode: on }),

      addWidget: (widget) => {
        set(state => {
          const dashboard = state.dashboards.find(d => d.id === state.activeDashboardId)
          if (!dashboard) return state
          const maxOrder = dashboard.widgets.reduce((max, w) => Math.max(max, w.order), -1)
          const newWidget: WidgetConfig = { ...widget, id: uid(), order: maxOrder + 1 }
          return {
            dashboards: state.dashboards.map(d =>
              d.id === state.activeDashboardId
                ? { ...d, widgets: [...d.widgets, newWidget], updatedAt: new Date().toISOString() }
                : d
            ),
          }
        })
      },

      updateWidget: (widgetId, updates) => {
        // If size changed, adjust colSpan automatically
        if (updates.size && !updates.colSpan) {
          const sizeOpt = SIZE_OPTIONS.find(s => s.value === updates.size)
          if (sizeOpt) updates.colSpan = sizeOpt.colSpan
        }
        set(state => ({
          dashboards: state.dashboards.map(d =>
            d.id === state.activeDashboardId
              ? {
                  ...d,
                  widgets: d.widgets.map(w => w.id === widgetId ? { ...w, ...updates } : w),
                  updatedAt: new Date().toISOString(),
                }
              : d
          ),
        }))
      },

      removeWidget: (widgetId) => {
        set(state => ({
          dashboards: state.dashboards.map(d =>
            d.id === state.activeDashboardId
              ? {
                  ...d,
                  widgets: d.widgets.filter(w => w.id !== widgetId),
                  updatedAt: new Date().toISOString(),
                }
              : d
          ),
        }))
      },

      reorderWidgets: (widgetIds) => {
        set(state => ({
          dashboards: state.dashboards.map(d =>
            d.id === state.activeDashboardId
              ? {
                  ...d,
                  widgets: d.widgets.map(w => ({
                    ...w,
                    order: widgetIds.indexOf(w.id),
                  })),
                  updatedAt: new Date().toISOString(),
                }
              : d
          ),
        }))
      },

      resizeWidget: (widgetId, size) => {
        const sizeOpt = SIZE_OPTIONS.find(s => s.value === size)
        if (!sizeOpt) return
        set(state => ({
          dashboards: state.dashboards.map(d =>
            d.id === state.activeDashboardId
              ? {
                  ...d,
                  widgets: d.widgets.map(w =>
                    w.id === widgetId ? { ...w, size, colSpan: sizeOpt.colSpan } : w
                  ),
                  updatedAt: new Date().toISOString(),
                }
              : d
          ),
        }))
      },

      resetToDefault: () => {
        set(state => ({
          dashboards: state.dashboards.map(d =>
            d.id === state.activeDashboardId
              ? { ...createDefaultDashboard(), id: d.id, name: d.name }
              : d
          ),
        }))
      },
    }),
    {
      name: 'syncledger-dashboard-config',
      version: 1,
    }
  )
)
