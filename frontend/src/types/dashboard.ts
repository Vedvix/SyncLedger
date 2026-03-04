// ==================== Configurable Dashboard Types ====================

export type ChartType =
  | 'area'
  | 'bar'
  | 'horizontalBar'
  | 'line'
  | 'pie'
  | 'donut'
  | 'radialGauge'
  | 'kpiCard'
  | 'statusCards'
  | 'table'

export type DataSourceKey =
  // Single-value metrics
  | 'totalInvoices'
  | 'pendingInvoices'
  | 'underReviewInvoices'
  | 'approvedInvoices'
  | 'rejectedInvoices'
  | 'syncedInvoices'
  | 'overdueInvoices'
  | 'totalAmount'
  | 'pendingAmount'
  | 'approvedAmount'
  | 'syncedAmount'
  | 'invoicesProcessedToday'
  | 'invoicesProcessedThisWeek'
  | 'invoicesProcessedThisMonth'
  | 'averageProcessingTimeMs'
  | 'emailsProcessedToday'
  | 'unprocessedEmails'
  | 'emailsWithErrors'
  | 'pendingSyncs'
  | 'failedSyncs'
  | 'successfulSyncsToday'
  | 'syncSuccessRate'
  | 'activeUsers'
  | 'totalUsers'
  // Array / composite sources
  | 'monthlyTrends'
  | 'topVendors'
  | 'statusDistribution'
  | 'financialBreakdown'
  | 'processingThroughput'
  | 'approvalRate'
  | 'syncRate'
  | 'alertCards'

export type WidgetSize = 'small' | 'medium' | 'large' | 'full'

/** Describes a selectable axis field */
export interface AxisFieldMeta {
  key: string
  label: string
  type: 'category' | 'numeric'
}

export interface WidgetConfig {
  id: string
  title: string
  chartType: ChartType
  dataSource: DataSourceKey
  size: WidgetSize
  /** Grid column span (1-12) */
  colSpan: number
  /** Grid row span */
  rowSpan: number
  /** Position in grid (0-based order) */
  order: number
  /** Optional color override */
  color?: string
  /** Optional subtitle */
  subtitle?: string
  /** Navigate to URL on click */
  clickUrl?: string
  /** Show/hide this widget */
  visible: boolean
  /** Selected field for X-axis (for axis-based charts) */
  xAxisField?: string
  /** Selected field(s) for Y-axis (for axis-based charts) */
  yAxisFields?: string[]
}

export interface DashboardConfig {
  id: string
  name: string
  description?: string
  widgets: WidgetConfig[]
  createdAt: string
  updatedAt: string
  isDefault?: boolean
}

// Available data source metadata (for the widget editor UI)
export interface DataSourceMeta {
  key: DataSourceKey
  label: string
  description: string
  category: 'count' | 'amount' | 'rate' | 'time-series' | 'breakdown' | 'composite'
  /** Chart types that make sense for this data source */
  compatibleCharts: ChartType[]
  /** Default chart type */
  defaultChart: ChartType
  /** Unit for display */
  unit?: 'count' | 'currency' | 'percent' | 'ms'
  /** Available fields for axis selection (only for chart-able sources) */
  axisFields?: AxisFieldMeta[]
  /** Default X-axis field key */
  defaultXAxis?: string
  /** Default Y-axis field key(s) */
  defaultYAxis?: string[]
}

// All available data sources
export const DATA_SOURCES: DataSourceMeta[] = [
  // Single values - KPI / Gauge
  { key: 'totalInvoices', label: 'Total Invoices', description: 'Total number of invoices', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'pendingInvoices', label: 'Pending Invoices', description: 'Invoices awaiting review', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'underReviewInvoices', label: 'Under Review', description: 'Invoices being reviewed', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'approvedInvoices', label: 'Approved Invoices', description: 'Approved invoices', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'rejectedInvoices', label: 'Rejected Invoices', description: 'Rejected invoices', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'syncedInvoices', label: 'Synced Invoices', description: 'Successfully synced', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'overdueInvoices', label: 'Overdue Invoices', description: 'Past due date', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'totalAmount', label: 'Total Amount', description: 'Sum of all invoice values', category: 'amount', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'currency' },
  { key: 'pendingAmount', label: 'Pending Amount', description: 'Value of pending invoices', category: 'amount', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'currency' },
  { key: 'approvedAmount', label: 'Approved Amount', description: 'Value of approved invoices', category: 'amount', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'currency' },
  { key: 'syncedAmount', label: 'Synced Amount', description: 'Value of synced invoices', category: 'amount', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'currency' },
  { key: 'invoicesProcessedToday', label: 'Processed Today', description: 'Invoices processed today', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'invoicesProcessedThisWeek', label: 'Processed This Week', description: 'Invoices processed this week', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'invoicesProcessedThisMonth', label: 'Processed This Month', description: 'Invoices processed this month', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'averageProcessingTimeMs', label: 'Avg Processing Time', description: 'Average time to process', category: 'count', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'ms' },
  { key: 'emailsProcessedToday', label: 'Emails Processed Today', description: 'Emails processed today', category: 'count', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'unprocessedEmails', label: 'Unprocessed Emails', description: 'Emails waiting to be processed', category: 'count', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'failedSyncs', label: 'Failed Syncs', description: 'Sync failures', category: 'count', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'activeUsers', label: 'Active Users', description: 'Currently active users', category: 'count', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'count' },
  { key: 'totalUsers', label: 'Total Users', description: 'Total user count', category: 'count', compatibleCharts: ['kpiCard'], defaultChart: 'kpiCard', unit: 'count' },
  
  // Rates
  { key: 'approvalRate', label: 'Approval Rate', description: 'Percentage of approved invoices', category: 'rate', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'radialGauge', unit: 'percent' },
  { key: 'syncRate', label: 'Sync Success Rate', description: 'Percentage of successful syncs', category: 'rate', compatibleCharts: ['kpiCard', 'radialGauge'], defaultChart: 'radialGauge', unit: 'percent' },
  
  // Time Series
  {
    key: 'monthlyTrends', label: 'Monthly Trends', description: 'Invoice volume & value over time',
    category: 'time-series', compatibleCharts: ['area', 'line', 'bar'], defaultChart: 'area',
    axisFields: [
      { key: 'name', label: 'Month', type: 'category' },
      { key: 'invoices', label: 'Invoice Count', type: 'numeric' },
      { key: 'amount', label: 'Total Amount ($)', type: 'numeric' },
    ],
    defaultXAxis: 'name', defaultYAxis: ['invoices'],
  },
  
  // Breakdowns
  {
    key: 'statusDistribution', label: 'Status Distribution', description: 'Invoices by status',
    category: 'breakdown', compatibleCharts: ['pie', 'donut', 'bar', 'horizontalBar'], defaultChart: 'donut',
    axisFields: [
      { key: 'name', label: 'Status', type: 'category' },
      { key: 'value', label: 'Count', type: 'numeric' },
    ],
    defaultXAxis: 'name', defaultYAxis: ['value'],
  },
  {
    key: 'topVendors', label: 'Top Vendors', description: 'Vendors by invoice count',
    category: 'breakdown', compatibleCharts: ['bar', 'horizontalBar', 'pie', 'table'], defaultChart: 'horizontalBar',
    axisFields: [
      { key: 'name', label: 'Vendor Name', type: 'category' },
      { key: 'invoices', label: 'Invoice Count', type: 'numeric' },
      { key: 'amount', label: 'Total Amount ($)', type: 'numeric' },
    ],
    defaultXAxis: 'name', defaultYAxis: ['invoices'],
  },
  {
    key: 'financialBreakdown', label: 'Financial Breakdown', description: 'Value by invoice status',
    category: 'breakdown', compatibleCharts: ['bar', 'horizontalBar', 'pie', 'donut'], defaultChart: 'horizontalBar',
    axisFields: [
      { key: 'name', label: 'Status', type: 'category' },
      { key: 'value', label: 'Amount ($)', type: 'numeric' },
    ],
    defaultXAxis: 'name', defaultYAxis: ['value'],
  },
  {
    key: 'processingThroughput', label: 'Processing Throughput', description: 'Invoices processed by period',
    category: 'breakdown', compatibleCharts: ['bar', 'horizontalBar'], defaultChart: 'bar',
    axisFields: [
      { key: 'name', label: 'Period', type: 'category' },
      { key: 'value', label: 'Count', type: 'numeric' },
    ],
    defaultXAxis: 'name', defaultYAxis: ['value'],
  },
  
  // Composite
  { key: 'alertCards', label: 'Alert Cards', description: 'Action items requiring attention', category: 'composite', compatibleCharts: ['statusCards'], defaultChart: 'statusCards' },
]

export const CHART_TYPE_LABELS: Record<ChartType, string> = {
  area: 'Area Chart',
  bar: 'Bar Chart',
  horizontalBar: 'Horizontal Bar',
  line: 'Line Chart',
  pie: 'Pie Chart',
  donut: 'Donut Chart',
  radialGauge: 'Radial Gauge',
  kpiCard: 'KPI Card',
  statusCards: 'Status Cards',
  table: 'Data Table',
}

export const SIZE_OPTIONS: { value: WidgetSize; label: string; colSpan: number }[] = [
  { value: 'small', label: 'Small (1/4)', colSpan: 3 },
  { value: 'medium', label: 'Medium (1/3)', colSpan: 4 },
  { value: 'large', label: 'Large (1/2)', colSpan: 6 },
  { value: 'full', label: 'Full Width', colSpan: 12 },
]

export const WIDGET_COLORS = [
  '#3b82f6', // blue
  '#22c55e', // green
  '#f59e0b', // amber
  '#ef4444', // red
  '#8b5cf6', // purple
  '#6366f1', // indigo
  '#06b6d4', // cyan
  '#ec4899', // pink
  '#f97316', // orange
  '#14b8a6', // teal
  '#64748b', // slate
]

// ==================== Date Range Filter Types ====================

export type DateFilterPreset = 'today' | 'thisWeek' | 'thisMonth' | 'thisYear' | 'custom' | 'all'

export interface DateFilter {
  preset: DateFilterPreset
  startDate?: string // ISO date string yyyy-MM-dd
  endDate?: string   // ISO date string yyyy-MM-dd
}

export const DATE_FILTER_LABELS: Record<DateFilterPreset, string> = {
  today: 'Today',
  thisWeek: 'This Week',
  thisMonth: 'This Month',
  thisYear: 'This Year',
  custom: 'Custom',
  all: 'All Time',
}

/** Compute the start/end dates for a given preset.  Returns undefined for 'all'. */
export function resolveDateFilter(filter: DateFilter): { startDate?: string; endDate?: string } {
  if (filter.preset === 'all') return {}
  if (filter.preset === 'custom') {
    return { startDate: filter.startDate, endDate: filter.endDate }
  }

  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = now.getMonth()
  const dd = now.getDate()
  const pad = (n: number) => n.toString().padStart(2, '0')
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

  const endDate = fmt(now)
  let start: Date

  switch (filter.preset) {
    case 'today':
      start = new Date(yyyy, mm, dd)
      break
    case 'thisWeek': {
      const day = now.getDay()
      const diff = day === 0 ? 6 : day - 1 // Monday-based
      start = new Date(yyyy, mm, dd - diff)
      break
    }
    case 'thisMonth':
      start = new Date(yyyy, mm, 1)
      break
    case 'thisYear':
      start = new Date(yyyy, 0, 1)
      break
    default:
      return {}
  }

  return { startDate: fmt(start), endDate }
}
