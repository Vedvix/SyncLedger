import { useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import type { WidgetConfig } from '@/types/dashboard'
import { DATA_SOURCES } from '@/types/dashboard'
import type { DashboardStats } from '@/types'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
  BarChart, Bar,
  LineChart, Line,
  RadialBarChart, RadialBar,
} from 'recharts'
import {
  FileText, Clock, CheckCircle, XCircle, RefreshCw, AlertTriangle,
  DollarSign, Upload, Eye, TrendingUp, ArrowDownRight,
} from 'lucide-react'

// Color palette
const STATUS_COLORS = ['#f59e0b', '#3b82f6', '#22c55e', '#ef4444', '#8b5cf6', '#f97316', '#dc2626']

const AXIS_COLORS = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#6366f1', '#06b6d4']

// ─── Resolve axis fields for a widget ───
function getAxisConfig(widget: WidgetConfig) {
  const source = DATA_SOURCES.find(s => s.key === widget.dataSource)
  const xAxisField = widget.xAxisField || source?.defaultXAxis || 'name'
  const yAxisFields = (widget.yAxisFields && widget.yAxisFields.length > 0)
    ? widget.yAxisFields
    : source?.defaultYAxis || ['value']
  return { xAxisField, yAxisFields }
}

interface WidgetRendererProps {
  widget: WidgetConfig
  stats: DashboardStats | undefined
  editMode?: boolean
}

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount)

const formatCompact = (n: number) =>
  n >= 1_000_000 ? `${(n / 1_000_000).toFixed(1)}M` : n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n)

// ─── Data extraction helpers ───
function extractData(widget: WidgetConfig, stats: DashboardStats | undefined) {
  if (!stats) return { value: 0, chartData: [] }

  const { dataSource } = widget

  // Single-value metrics
  const singleValues: Record<string, number | undefined> = {
    totalInvoices: stats.totalInvoices,
    pendingInvoices: stats.pendingInvoices,
    underReviewInvoices: stats.underReviewInvoices,
    approvedInvoices: stats.approvedInvoices,
    rejectedInvoices: stats.rejectedInvoices,
    syncedInvoices: stats.syncedInvoices,
    overdueInvoices: stats.overdueInvoices,
    totalAmount: stats.totalAmount,
    pendingAmount: stats.pendingAmount,
    approvedAmount: stats.approvedAmount,
    syncedAmount: stats.syncedAmount,
    invoicesProcessedToday: stats.invoicesProcessedToday,
    invoicesProcessedThisWeek: stats.invoicesProcessedThisWeek,
    invoicesProcessedThisMonth: stats.invoicesProcessedThisMonth,
    averageProcessingTimeMs: stats.averageProcessingTimeMs,
    emailsProcessedToday: stats.emailsProcessedToday,
    unprocessedEmails: stats.unprocessedEmails,
    emailsWithErrors: stats.emailsWithErrors,
    pendingSyncs: stats.pendingSyncs,
    failedSyncs: stats.failedSyncs,
    successfulSyncsToday: stats.successfulSyncsToday,
    syncSuccessRate: stats.syncSuccessRate,
    activeUsers: stats.activeUsers,
    totalUsers: stats.totalUsers,
  }

  if (dataSource in singleValues) {
    return { value: singleValues[dataSource] || 0, chartData: [] }
  }

  // Computed rates
  if (dataSource === 'approvalRate') {
    const total = stats.totalInvoices || 0
    const rate = total > 0 ? (stats.approvedInvoices / total) * 100 : 0
    return { value: rate, chartData: [{ name: 'rate', value: rate, fill: widget.color || '#22c55e' }] }
  }
  if (dataSource === 'syncRate') {
    const rate = (stats.syncSuccessRate || 0) * 100
    return { value: rate, chartData: [{ name: 'rate', value: rate, fill: widget.color || '#8b5cf6' }] }
  }

  // Time series
  if (dataSource === 'monthlyTrends') {
    return {
      value: 0,
      chartData: (stats.monthlyTrends || []).map(m => ({
        name: m.monthName?.slice(0, 3) || `${m.month}`,
        invoices: m.invoiceCount,
        amount: m.totalAmount,
      })),
    }
  }

  // Breakdowns
  if (dataSource === 'statusDistribution') {
    return {
      value: 0,
      chartData: [
        { name: 'Pending', value: stats.pendingInvoices || 0 },
        { name: 'Under Review', value: stats.underReviewInvoices || 0 },
        { name: 'Approved', value: stats.approvedInvoices || 0 },
        { name: 'Rejected', value: stats.rejectedInvoices || 0 },
        { name: 'Synced', value: stats.syncedInvoices || 0 },
        { name: 'Sync Failed', value: stats.failedSyncs || 0 },
        { name: 'Overdue', value: stats.overdueInvoices || 0 },
      ].filter(d => d.value > 0),
    }
  }

  if (dataSource === 'topVendors') {
    return {
      value: 0,
      chartData: (stats.topVendors || []).slice(0, 8).map(v => ({
        name: v.vendorName.length > 20 ? v.vendorName.slice(0, 20) + '…' : v.vendorName,
        invoices: v.invoiceCount,
        amount: v.totalAmount,
      })),
    }
  }

  if (dataSource === 'financialBreakdown') {
    return {
      value: 0,
      chartData: [
        { name: 'Pending', value: stats.pendingAmount || 0, fill: '#f59e0b' },
        { name: 'Approved', value: stats.approvedAmount || 0, fill: '#22c55e' },
        { name: 'Synced', value: stats.syncedAmount || 0, fill: '#8b5cf6' },
      ],
    }
  }

  if (dataSource === 'processingThroughput') {
    return {
      value: 0,
      chartData: [
        { name: 'Today', value: stats.invoicesProcessedToday || 0 },
        { name: 'Week', value: stats.invoicesProcessedThisWeek || 0 },
        { name: 'Month', value: stats.invoicesProcessedThisMonth || 0 },
      ],
    }
  }

  return { value: 0, chartData: [] }
}

// ─── Format value by data source ───
function formatValue(value: number, dataSource: string): string {
  if (['totalAmount', 'pendingAmount', 'approvedAmount', 'syncedAmount'].includes(dataSource)) {
    return formatCurrency(value)
  }
  if (['approvalRate', 'syncRate'].includes(dataSource)) {
    return `${value.toFixed(1)}%`
  }
  if (dataSource === 'syncSuccessRate') {
    return `${(value * 100).toFixed(1)}%`
  }
  if (dataSource === 'averageProcessingTimeMs') {
    return `${(value / 1000).toFixed(1)}s`
  }
  return formatCompact(value)
}

// Icon mapping
function getIcon(dataSource: string) {
  const map: Record<string, React.ReactNode> = {
    totalInvoices: <FileText className="w-5 h-5" />,
    pendingInvoices: <Clock className="w-5 h-5" />,
    underReviewInvoices: <Eye className="w-5 h-5" />,
    approvedInvoices: <CheckCircle className="w-5 h-5" />,
    rejectedInvoices: <XCircle className="w-5 h-5" />,
    syncedInvoices: <Upload className="w-5 h-5" />,
    overdueInvoices: <AlertTriangle className="w-5 h-5" />,
    totalAmount: <DollarSign className="w-5 h-5" />,
    pendingAmount: <DollarSign className="w-5 h-5" />,
    approvedAmount: <DollarSign className="w-5 h-5" />,
    syncedAmount: <DollarSign className="w-5 h-5" />,
    failedSyncs: <RefreshCw className="w-5 h-5" />,
    approvalRate: <CheckCircle className="w-5 h-5" />,
    syncRate: <Upload className="w-5 h-5" />,
    processingThroughput: <TrendingUp className="w-5 h-5" />,
  }
  return map[dataSource] || <FileText className="w-5 h-5" />
}

// ─── Chart components ───

function KpiCard({ widget, value }: { widget: WidgetConfig; value: number }) {
  const color = widget.color || '#3b82f6'
  return (
    <div className="flex flex-col h-full justify-between">
      <div className="flex items-center justify-between mb-3">
        <div className="p-2 rounded-lg" style={{ backgroundColor: `${color}15`, color }}>
          {getIcon(widget.dataSource)}
        </div>
      </div>
      <div>
        <p className="text-sm text-gray-500 mb-0.5">{widget.title}</p>
        <p className="text-3xl font-bold text-gray-900">{formatValue(value, widget.dataSource)}</p>
        {widget.subtitle && <p className="text-xs text-gray-400 mt-1">{widget.subtitle}</p>}
      </div>
    </div>
  )
}

function RadialGaugeWidget({ widget, data, value }: { widget: WidgetConfig; data: { name: string; value: number; fill: string }[]; value: number }) {
  const color = widget.color || '#3b82f6'
  const gaugeData = data.length > 0 ? data : [{ name: 'rate', value, fill: color }]
  return (
    <div className="flex flex-col h-full justify-between">
      <div className="flex items-center justify-between mb-3">
        <div className="p-2 rounded-lg" style={{ backgroundColor: `${color}15`, color }}>
          {getIcon(widget.dataSource)}
        </div>
      </div>
      <p className="text-sm text-gray-500 mb-0.5">{widget.title}</p>
      <div className="flex items-end justify-between">
        <p className="text-3xl font-bold text-gray-900">{formatValue(value, widget.dataSource)}</p>
        <div className="w-16 h-16">
          <ResponsiveContainer width="100%" height="100%">
            <RadialBarChart cx="50%" cy="50%" innerRadius="70%" outerRadius="100%" data={gaugeData} startAngle={90} endAngle={-270} barSize={6}>
              <RadialBar background={{ fill: '#f3f4f6' }} dataKey="value" cornerRadius={4} />
            </RadialBarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}

function AreaChartWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  const color = widget.color || '#3b82f6'
  const gradientId = `grad_${widget.id}`
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.2} />
                <stop offset="100%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey={xAxisField} tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} />
            {yAxisFields.map((field, i) => (
              <Area
                key={field}
                type="monotone"
                dataKey={field}
                stroke={AXIS_COLORS[i % AXIS_COLORS.length]}
                strokeWidth={i === 0 ? 2.5 : 2}
                fill={i === 0 ? `url(#${gradientId})` : 'none'}
                dot={i === 0 ? { r: 3, fill: AXIS_COLORS[i % AXIS_COLORS.length] } : false}
                activeDot={i === 0 ? { r: 5 } : undefined}
                strokeDasharray={i > 0 ? '5 3' : undefined}
              />
            ))}
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function LineChartWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey={xAxisField} tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }} />
            {yAxisFields.map((field, i) => (
              <Line
                key={field}
                type="monotone"
                dataKey={field}
                stroke={AXIS_COLORS[i % AXIS_COLORS.length]}
                strokeWidth={i === 0 ? 2.5 : 2}
                dot={i === 0 ? { r: 3 } : false}
                strokeDasharray={i > 0 ? '5 3' : undefined}
              />
            ))}
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function BarChartWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  const color = widget.color || '#3b82f6'
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey={xAxisField} tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }} />
            {yAxisFields.map((field, i) => (
              <Bar key={field} dataKey={field} fill={AXIS_COLORS[i % AXIS_COLORS.length]} radius={[4, 4, 0, 0]} barSize={yAxisFields.length > 1 ? 20 : 32}>
                {yAxisFields.length === 1 && data.map((entry, j) => (
                  <Cell key={j} fill={(entry as { fill?: string }).fill || color} />
                ))}
              </Bar>
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function HorizontalBarWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  const color = widget.color || '#3b82f6'
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ top: 0, right: 20, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false} />
            <XAxis type="number" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
            <YAxis type="category" dataKey={xAxisField} tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} width={130} />
            <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }}
              formatter={(val: number, name: string) => [name === 'amount' ? formatCurrency(val) : val, name]}
            />
            {yAxisFields.map((field, i) => (
              <Bar key={field} dataKey={field} fill={AXIS_COLORS[i % AXIS_COLORS.length]} radius={[0, 4, 4, 0]} barSize={yAxisFields.length > 1 ? 14 : 18}>
                {yAxisFields.length === 1 && data.map((entry, j) => (
                  <Cell key={j} fill={(entry as { fill?: string }).fill || color} />
                ))}
              </Bar>
            ))}
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function PieChartWidget({ widget, data }: { widget: WidgetConfig; data: { name: string; value: number }[] }) {
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  const labelKey = xAxisField || 'name'
  const valueKey = yAxisFields[0] || 'value'
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={data} cx="50%" cy="50%" outerRadius="80%" dataKey={valueKey} nameKey={labelKey} stroke="none" label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
              {data.map((_entry, i) => <Cell key={i} fill={STATUS_COLORS[i % STATUS_COLORS.length]} />)}
            </Pie>
            <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }} />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  )
}

function DonutChartWidget({ widget, data }: { widget: WidgetConfig; data: { name: string; value: number }[] }) {
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  const labelKey = xAxisField || 'name'
  const valueKey = yAxisFields[0] || 'value'
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0 flex flex-col">
        <div className="flex-1 min-h-0">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie data={data} cx="50%" cy="50%" innerRadius="55%" outerRadius="80%" paddingAngle={3} dataKey={valueKey} nameKey={labelKey} stroke="none">
                {data.map((_entry, i) => <Cell key={i} fill={STATUS_COLORS[i % STATUS_COLORS.length]} />)}
              </Pie>
              <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }} />
            </PieChart>
          </ResponsiveContainer>
        </div>
        <div className="space-y-1.5 mt-2">
          {data.slice(0, 6).map((item, i) => (
            <div key={String((item as Record<string, unknown>)[labelKey])} className="flex items-center justify-between text-sm px-1">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: STATUS_COLORS[i % STATUS_COLORS.length] }} />
                <span className="text-gray-600 text-xs">{String((item as Record<string, unknown>)[labelKey])}</span>
              </div>
              <span className="font-semibold text-gray-900 text-xs">{String((item as Record<string, unknown>)[valueKey])}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function TableWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  if (!data.length) return <EmptyState />
  const keys = Object.keys(data[0]).filter(k => k !== 'fill')
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0 overflow-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-gray-100">
              {keys.map(k => (
                <th key={k} className="text-left py-2 px-2 text-gray-500 font-medium capitalize">{k}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row, i) => (
              <tr key={i} className="border-b border-gray-50 hover:bg-gray-50">
                {keys.map(k => (
                  <td key={k} className="py-2 px-2 text-gray-700">
                    {typeof row[k] === 'number' ? formatCompact(row[k] as number) : String(row[k])}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function AlertCardsWidget({ stats }: { stats: DashboardStats | undefined }) {
  const navigate = useNavigate()
  if (!stats) return <EmptyState />
  const cards = [
    { label: 'Rejected', value: stats.rejectedInvoices || 0, url: '/invoices?status=REJECTED', icon: <XCircle className="w-5 h-5" />, color: '#ef4444', borderColor: 'border-red-400', bgColor: 'bg-red-50', textColor: 'text-red-500', badge: stats.rejectedInvoices > 0 ? 'Requires attention' : undefined },
    { label: 'Overdue', value: stats.overdueInvoices || 0, url: '/invoices?overdue=true', icon: <AlertTriangle className="w-5 h-5" />, color: '#f97316', borderColor: 'border-orange-400', bgColor: 'bg-orange-50', textColor: 'text-orange-500', badge: stats.overdueInvoices > 0 ? 'Critical' : undefined },
    { label: 'Failed Syncs', value: stats.failedSyncs || 0, url: '/invoices?status=SYNC_FAILED', icon: <RefreshCw className="w-5 h-5" />, color: '#f59e0b', borderColor: 'border-amber-400', bgColor: 'bg-amber-50', textColor: 'text-amber-500', badge: 'Retry available' },
    { label: 'Manual Review', value: stats.underReviewInvoices || 0, url: '/invoices?status=UNDER_REVIEW', icon: <Eye className="w-5 h-5" />, color: '#3b82f6', borderColor: 'border-blue-400', bgColor: 'bg-blue-50', textColor: 'text-blue-500', badge: 'Low confidence flags' },
  ]
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 h-full">
      {cards.map(c => (
        <div key={c.label} onClick={() => navigate(c.url)}
          className={`bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all border-l-4 ${c.borderColor}`}>
          <div className="flex items-start gap-3">
            <div className={`p-2 rounded-lg ${c.bgColor} ${c.textColor}`}>{c.icon}</div>
            <div>
              <p className="text-sm text-gray-500">{c.label}</p>
              <p className="text-2xl font-bold text-gray-900">{c.value}</p>
              {c.badge && c.value > 0 && (
                <p className="text-xs mt-1 flex items-center gap-1" style={{ color: c.color }}>
                  <ArrowDownRight className="w-3 h-3" /> {c.badge}
                </p>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

// Shared sub-components
function WidgetHeader({ widget }: { widget: WidgetConfig }) {
  return (
    <div className="mb-3">
      <h3 className="text-base font-semibold text-gray-900">{widget.title}</h3>
      {widget.subtitle && <p className="text-xs text-gray-400">{widget.subtitle}</p>}
    </div>
  )
}

function EmptyState() {
  return (
    <div className="h-full flex items-center justify-center text-gray-400 text-sm">
      No data available
    </div>
  )
}

// ─── Main Renderer ───
export function WidgetRenderer({ widget, stats, editMode }: WidgetRendererProps) {
  const navigate = useNavigate()

  const { value, chartData } = useMemo(() => extractData(widget, stats), [widget, stats])

  const handleClick = () => {
    if (editMode) return
    if (widget.clickUrl) navigate(widget.clickUrl)
  }

  const minHeight = ['kpiCard', 'radialGauge'].includes(widget.chartType) ? 'min-h-[140px]' : 'min-h-[280px]'

  const content = (() => {
    switch (widget.chartType) {
      case 'kpiCard':
        return <KpiCard widget={widget} value={value} />
      case 'radialGauge':
        return <RadialGaugeWidget widget={widget} data={chartData as { name: string; value: number; fill: string }[]} value={value} />
      case 'area':
        return <AreaChartWidget widget={widget} data={chartData} />
      case 'line':
        return <LineChartWidget widget={widget} data={chartData} />
      case 'bar':
        return <BarChartWidget widget={widget} data={chartData} />
      case 'horizontalBar':
        return <HorizontalBarWidget widget={widget} data={chartData} />
      case 'pie':
        return <PieChartWidget widget={widget} data={chartData as { name: string; value: number }[]} />
      case 'donut':
        return <DonutChartWidget widget={widget} data={chartData as { name: string; value: number }[]} />
      case 'table':
        return <TableWidget widget={widget} data={chartData} />
      case 'statusCards':
        return <AlertCardsWidget stats={stats} />
      default:
        return <EmptyState />
    }
  })()

  if (widget.chartType === 'statusCards') {
    return <div className={`${minHeight}`}>{content}</div>
  }

  return (
    <div
      onClick={handleClick}
      className={`bg-white rounded-xl shadow-sm p-5 h-full ${minHeight} ${widget.clickUrl && !editMode ? 'cursor-pointer hover:shadow-md' : ''} transition-all`}
    >
      {content}
    </div>
  )
}
