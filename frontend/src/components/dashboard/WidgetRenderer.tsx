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
  Treemap,
} from 'recharts'
import {
  FileText, Clock, CheckCircle, XCircle, RefreshCw, AlertTriangle,
  DollarSign, Upload, Eye, TrendingUp, ArrowDownRight,
} from 'lucide-react'

// Color palette
const STATUS_COLORS = ['#ff6384', '#36a2eb', '#4bc0c0', '#ff9f40', '#9966ff', '#ffcd56', '#c9cbcf']

const AXIS_COLORS = ['#36a2eb', '#4bc0c0', '#ff9f40', '#ff6384', '#9966ff', '#ffcd56', '#c9cbcf']

const TREEMAP_COLORS = [
  '#36a2eb', '#4bc0c0', '#ff9f40', '#ff6384', '#9966ff',
  '#ffcd56', '#c9cbcf', '#e056a0', '#7c4dff', '#00bcd4',
]

// Grafana-style tooltip
const GRAFANA_TOOLTIP_STYLE = {
  backgroundColor: '#1e1e2f',
  border: '1px solid #333',
  borderRadius: '6px',
  boxShadow: '0 8px 32px rgba(0,0,0,0.4)',
  color: '#d0d0d0',
  fontSize: '12px',
  padding: '8px 12px',
}

// Grafana-style grid lines
const GRAFANA_GRID = {
  strokeDasharray: '3 3',
  stroke: '#2a2a3d',
  strokeOpacity: 0.6,
}

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

  // Vendor-focused analytics
  if (dataSource === 'vendorInvoiceCount') {
    return {
      value: 0,
      chartData: (stats.topVendors || []).slice(0, 10).map(v => ({
        name: v.vendorName.length > 18 ? v.vendorName.slice(0, 18) + '…' : v.vendorName,
        invoices: v.invoiceCount,
      })),
    }
  }

  if (dataSource === 'vendorSpendConcentration') {
    const vendors = (stats.topVendors || []).slice(0, 10)
    const totalSpend = vendors.reduce((sum, v) => sum + (v.totalAmount || 0), 0)
    return {
      value: totalSpend,
      chartData: vendors.map(v => ({
        name: v.vendorName.length > 18 ? v.vendorName.slice(0, 18) + '…' : v.vendorName,
        amount: v.totalAmount || 0,
        percentage: totalSpend > 0 ? Math.round(((v.totalAmount || 0) / totalSpend) * 100) : 0,
      })),
    }
  }

  if (dataSource === 'vendorStatusProcessed') {
    const raw = stats.vendorStatusBreakdown || []
    const vendorMap = new Map<string, {
      name: string
      pending: number
      underReview: number
      approved: number
      rejected: number
      synced: number
      syncFailed: number
      total: number
    }>()

    raw.forEach(item => {
      const vendorName = item.vendorName?.trim() || 'Unknown Vendor'
      const existing = vendorMap.get(vendorName) || {
        name: vendorName.length > 18 ? vendorName.slice(0, 18) + '…' : vendorName,
        pending: 0,
        underReview: 0,
        approved: 0,
        rejected: 0,
        synced: 0,
        syncFailed: 0,
        total: 0,
      }

      const count = item.invoiceCount || 0
      switch ((item.status || '').toUpperCase()) {
        case 'PENDING':
          existing.pending += count
          break
        case 'UNDER_REVIEW':
          existing.underReview += count
          break
        case 'APPROVED':
          existing.approved += count
          break
        case 'REJECTED':
          existing.rejected += count
          break
        case 'SYNCED':
          existing.synced += count
          break
        case 'SYNC_FAILED':
          existing.syncFailed += count
          break
        default:
          break
      }

      existing.total += count
      vendorMap.set(vendorName, existing)
    })

    return {
      value: 0,
      chartData: Array.from(vendorMap.values())
        .sort((a, b) => b.total - a.total)
        .slice(0, 8)
        .map(({ total, ...item }) => item),
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
  const color = widget.color || '#36a2eb'
  return (
    <div className="flex flex-col h-full justify-between">
      <div className="flex items-center justify-between mb-3">
        <div className="p-2 rounded-lg" style={{ backgroundColor: `${color}20`, color }}>
          {getIcon(widget.dataSource)}
        </div>
        <div className="w-12 h-1 rounded-full" style={{ backgroundColor: `${color}30` }}>
          <div className="h-full rounded-full" style={{ backgroundColor: color, width: '70%' }} />
        </div>
      </div>
      <div>
        <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-1">{widget.title}</p>
        <p className="text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r" style={{ backgroundImage: `linear-gradient(135deg, ${color}, ${color}cc)` }}>
          {formatValue(value, widget.dataSource)}
        </p>
        {widget.subtitle && <p className="text-xs text-gray-500 mt-1.5">{widget.subtitle}</p>}
      </div>
    </div>
  )
}

function RadialGaugeWidget({ widget, data, value }: { widget: WidgetConfig; data: { name: string; value: number; fill: string }[]; value: number }) {
  const color = widget.color || '#36a2eb'
  const gaugeData = data.length > 0 ? data : [{ name: 'rate', value, fill: color }]
  return (
    <div className="flex flex-col h-full justify-between">
      <div className="flex items-center justify-between mb-2">
        <div className="p-2 rounded-lg" style={{ backgroundColor: `${color}20`, color }}>
          {getIcon(widget.dataSource)}
        </div>
      </div>
      <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-1">{widget.title}</p>
      <div className="flex items-end justify-between">
        <p className="text-3xl font-bold" style={{ color }}>{formatValue(value, widget.dataSource)}</p>
        <div className="w-16 h-16">
          <ResponsiveContainer width="100%" height="100%">
            <RadialBarChart cx="50%" cy="50%" innerRadius="70%" outerRadius="100%" data={gaugeData} startAngle={90} endAngle={-270} barSize={6}>
              <RadialBar background={{ fill: '#1e293b' }} dataKey="value" cornerRadius={4} />
            </RadialBarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  )
}

function AreaChartWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  const color = widget.color || '#36a2eb'
  const gradientId = `grad_${widget.id}`
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.3} />
                <stop offset="100%" stopColor={color} stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid {...GRAFANA_GRID} />
            <XAxis dataKey={xAxisField} tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <Tooltip contentStyle={GRAFANA_TOOLTIP_STYLE} cursor={{ stroke: '#555', strokeDasharray: '3 3' }} />
            {yAxisFields.map((field, i) => (
              <Area
                key={field}
                type="monotone"
                dataKey={field}
                stroke={AXIS_COLORS[i % AXIS_COLORS.length]}
                strokeWidth={i === 0 ? 2.5 : 2}
                fill={i === 0 ? `url(#${gradientId})` : 'none'}
                dot={i === 0 ? { r: 3, fill: AXIS_COLORS[i % AXIS_COLORS.length], strokeWidth: 0 } : false}
                activeDot={i === 0 ? { r: 5, stroke: '#fff', strokeWidth: 2 } : undefined}
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
          <LineChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid {...GRAFANA_GRID} />
            <XAxis dataKey={xAxisField} tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <Tooltip contentStyle={GRAFANA_TOOLTIP_STYLE} cursor={{ stroke: '#555', strokeDasharray: '3 3' }} />
            {yAxisFields.map((field, i) => (
              <Line
                key={field}
                type="monotone"
                dataKey={field}
                stroke={AXIS_COLORS[i % AXIS_COLORS.length]}
                strokeWidth={i === 0 ? 2.5 : 2}
                dot={i === 0 ? { r: 3, strokeWidth: 0 } : false}
                activeDot={{ r: 5, stroke: '#fff', strokeWidth: 2 }}
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
  const color = widget.color || '#36a2eb'
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
            <CartesianGrid {...GRAFANA_GRID} />
            <XAxis dataKey={xAxisField} tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <YAxis tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <Tooltip contentStyle={GRAFANA_TOOLTIP_STYLE} cursor={{ fill: 'rgba(255,255,255,0.03)' }} />
            {yAxisFields.map((field, i) => (
              <Bar key={field} dataKey={field} fill={AXIS_COLORS[i % AXIS_COLORS.length]} radius={[3, 3, 0, 0]} barSize={yAxisFields.length > 1 ? 20 : 32} fillOpacity={0.85}>
                {yAxisFields.length === 1 && data.map((entry, j) => (
                  <Cell key={j} fill={(entry as { fill?: string }).fill || color} fillOpacity={0.85} />
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
  const color = widget.color || '#36a2eb'
  if (!data.length) return <EmptyState />
  const { xAxisField, yAxisFields } = getAxisConfig(widget)
  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} layout="vertical" margin={{ top: 0, right: 20, left: 0, bottom: 0 }}>
            <CartesianGrid {...GRAFANA_GRID} horizontal={false} />
            <XAxis type="number" tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} />
            <YAxis type="category" dataKey={xAxisField} tick={{ fontSize: 11, fill: '#8b949e' }} axisLine={{ stroke: '#333' }} tickLine={false} width={130} />
            <Tooltip contentStyle={GRAFANA_TOOLTIP_STYLE}
              formatter={(val: number, name: string) => [name === 'amount' ? formatCurrency(val) : val, name]}
              cursor={{ fill: 'rgba(255,255,255,0.03)' }}
            />
            {yAxisFields.map((field, i) => (
              <Bar key={field} dataKey={field} fill={AXIS_COLORS[i % AXIS_COLORS.length]} radius={[0, 3, 3, 0]} barSize={yAxisFields.length > 1 ? 14 : 18} fillOpacity={0.85}>
                {yAxisFields.length === 1 && data.map((entry, j) => (
                  <Cell key={j} fill={(entry as { fill?: string }).fill || color} fillOpacity={0.85} />
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
            <Pie data={data} cx="50%" cy="50%" outerRadius="80%" dataKey={valueKey} nameKey={labelKey} stroke="#1a1a2e" strokeWidth={2} label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
              {data.map((_entry, i) => <Cell key={i} fill={STATUS_COLORS[i % STATUS_COLORS.length]} fillOpacity={0.9} />)}
            </Pie>
            <Tooltip contentStyle={GRAFANA_TOOLTIP_STYLE} />
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
              <Pie data={data} cx="50%" cy="50%" innerRadius="55%" outerRadius="80%" paddingAngle={3} dataKey={valueKey} nameKey={labelKey} stroke="#1a1a2e" strokeWidth={2}>
                {data.map((_entry, i) => <Cell key={i} fill={STATUS_COLORS[i % STATUS_COLORS.length]} fillOpacity={0.9} />)}
              </Pie>
              <Tooltip contentStyle={GRAFANA_TOOLTIP_STYLE} />
            </PieChart>
          </ResponsiveContainer>
        </div>
        <div className="space-y-1.5 mt-2">
          {data.slice(0, 6).map((item, i) => (
            <div key={String((item as Record<string, unknown>)[labelKey])} className="flex items-center justify-between text-sm px-1">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: STATUS_COLORS[i % STATUS_COLORS.length] }} />
                <span className="text-gray-500 text-xs">{String((item as Record<string, unknown>)[labelKey])}</span>
              </div>
              <span className="font-semibold text-gray-300 text-xs">{String((item as Record<string, unknown>)[valueKey])}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function TreemapWidget({ widget, data }: { widget: WidgetConfig; data: Record<string, unknown>[] }) {
  if (!data.length) return <EmptyState />
  const { yAxisFields } = getAxisConfig(widget)
  const valueKey = yAxisFields[0] || 'amount'

  // Recharts Treemap expects { name, size, ... } shape or a 'children' tree
  const treemapData = data.map((d, i) => ({
    name: String(d.name || d.label || `Item ${i + 1}`),
    size: Number(d[valueKey]) || 0,
    fill: TREEMAP_COLORS[i % TREEMAP_COLORS.length],
  }))

  const CustomContent = (props: { x?: number; y?: number; width?: number; height?: number; name?: string; size?: number; fill?: string; index?: number }) => {
    const { x = 0, y = 0, width = 0, height = 0, name = '', size = 0, fill = '#3b82f6' } = props
    if (width < 40 || height < 30) return null

    return (
      <g>
        <rect x={x} y={y} width={width} height={height} fill={fill} fillOpacity={0.85} stroke="#1e293b" strokeWidth={2} rx={4} />
        <text x={x + width / 2} y={y + height / 2 - 7} textAnchor="middle" fill="#fff" fontSize={width < 80 ? 10 : 12} fontWeight={600}>
          {name}
        </text>
        <text x={x + width / 2} y={y + height / 2 + 10} textAnchor="middle" fill="rgba(255,255,255,0.8)" fontSize={width < 80 ? 9 : 11}>
          {formatCurrency(size)}
        </text>
      </g>
    )
  }

  return (
    <div className="h-full flex flex-col">
      <WidgetHeader widget={widget} />
      <div className="flex-1 min-h-0">
        <ResponsiveContainer width="100%" height="100%">
          <Treemap
            data={treemapData}
            dataKey="size"
            nameKey="name"
            stroke="#1e293b"
            content={<CustomContent />}
          >
            <Tooltip
              content={({ payload }) => {
                if (!payload || !payload.length) return null
                const item = payload[0]?.payload
                if (!item) return null
                const total = treemapData.reduce((s, d) => s + d.size, 0)
                const pct = total > 0 ? ((item.size / total) * 100).toFixed(1) : '0'
                return (
                  <div className="bg-gray-900 text-white px-3 py-2 rounded-lg shadow-lg text-sm border border-gray-700">
                    <p className="font-semibold">{item.name}</p>
                    <p className="text-gray-300">{formatCurrency(item.size)} ({pct}%)</p>
                  </div>
                )
              }}
            />
          </Treemap>
        </ResponsiveContainer>
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
            <tr className="border-b border-gray-700/50">
              {keys.map(k => (
                <th key={k} className="text-left py-2 px-2 text-gray-400 font-medium capitalize text-xs uppercase tracking-wider">{k}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((row, i) => (
              <tr key={i} className="border-b border-gray-800/30 hover:bg-white/[0.02]">
                {keys.map(k => (
                  <td key={k} className="py-2 px-2 text-gray-300 text-xs">
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
    { label: 'Rejected', value: stats.rejectedInvoices || 0, url: '/invoices?status=REJECTED', icon: <XCircle className="w-5 h-5" />, color: '#ff6384', bgClass: 'bg-red-500/10', textClass: 'text-red-400', badge: stats.rejectedInvoices > 0 ? 'Requires attention' : undefined },
    { label: 'Overdue', value: stats.overdueInvoices || 0, url: '/invoices?overdue=true', icon: <AlertTriangle className="w-5 h-5" />, color: '#ff9f40', bgClass: 'bg-orange-500/10', textClass: 'text-orange-400', badge: stats.overdueInvoices > 0 ? 'Critical' : undefined },
    { label: 'Failed Syncs', value: stats.failedSyncs || 0, url: '/invoices?status=SYNC_FAILED', icon: <RefreshCw className="w-5 h-5" />, color: '#ffcd56', bgClass: 'bg-amber-500/10', textClass: 'text-amber-400', badge: 'Retry available' },
    { label: 'Manual Review', value: stats.underReviewInvoices || 0, url: '/invoices?status=UNDER_REVIEW', icon: <Eye className="w-5 h-5" />, color: '#36a2eb', bgClass: 'bg-blue-500/10', textClass: 'text-blue-400', badge: 'Low confidence flags' },
  ]
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 h-full">
      {cards.map(c => (
        <div key={c.label} onClick={() => navigate(c.url)}
          className="bg-[#181824] rounded-lg border border-gray-700/40 p-5 cursor-pointer hover:border-gray-600/60 hover:shadow-lg transition-all"
          style={{ borderLeftWidth: '3px', borderLeftColor: c.color }}
        >
          <div className="flex items-start gap-3">
            <div className={`p-2 rounded-lg ${c.bgClass} ${c.textClass}`}>{c.icon}</div>
            <div>
              <p className="text-xs text-gray-400 uppercase tracking-wider">{c.label}</p>
              <p className="text-2xl font-bold text-gray-100">{c.value}</p>
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
    <div className="mb-3 pb-2 border-b border-gray-700/30">
      <div className="flex items-center gap-2">
        <div className="w-1 h-4 rounded-full" style={{ backgroundColor: widget.color || '#36a2eb' }} />
        <h3 className="text-sm font-semibold text-gray-200 tracking-wide">{widget.title}</h3>
      </div>
      {widget.subtitle && <p className="text-[11px] text-gray-500 mt-0.5 ml-3">{widget.subtitle}</p>}
    </div>
  )
}

function EmptyState() {
  return (
    <div className="h-full flex items-center justify-center text-gray-500 text-sm">
      <div className="text-center">
        <div className="w-10 h-10 rounded-lg bg-gray-800/50 flex items-center justify-center mx-auto mb-2">
          <FileText className="w-5 h-5 text-gray-600" />
        </div>
        <p>No data available</p>
      </div>
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
      case 'treemap':
        return <TreemapWidget widget={widget} data={chartData} />
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
      className={`bg-[#181824] border border-gray-700/40 rounded-lg shadow-lg p-5 h-full ${minHeight} ${widget.clickUrl && !editMode ? 'cursor-pointer hover:border-gray-600/60 hover:shadow-xl' : ''} transition-all`}
    >
      {content}
    </div>
  )
}
