import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { dashboardService } from '@/services/dashboardService'
import { useAuthStore } from '@/store/authStore'
import { 
  FileText, 
  Clock, 
  CheckCircle, 
  XCircle, 
  RefreshCw,
  AlertTriangle,
  DollarSign,
  TrendingUp,
  Upload,
  Eye,
  Building,
  Shield,
  ArrowUpRight,
  ArrowDownRight,
} from 'lucide-react'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell,
  BarChart, Bar,
  RadialBarChart, RadialBar,
} from 'recharts'

// Color palette aligned with primary blue theme
const COLORS = {
  primary: '#3b82f6',
  primaryLight: '#93c5fd',
  success: '#22c55e',
  successLight: '#bbf7d0',
  warning: '#f59e0b',
  warningLight: '#fde68a',
  danger: '#ef4444',
  dangerLight: '#fecaca',
  purple: '#8b5cf6',
  purpleLight: '#c4b5fd',
  indigo: '#6366f1',
  cyan: '#06b6d4',
  slate: '#64748b',
}

const STATUS_COLORS = [
  COLORS.warning,   // Pending
  COLORS.primary,   // Under Review
  COLORS.success,   // Approved
  COLORS.danger,    // Rejected
  COLORS.purple,    // Synced
  '#f97316',        // Sync Failed (orange)
  '#dc2626',        // Overdue (dark red)
]

export function DashboardPage() {
  const { user } = useAuthStore()
  const navigate = useNavigate()
  const { data: stats, isLoading, error, refetch } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: () => dashboardService.getStats(),
    refetchInterval: 30000,
  })
  
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <RefreshCw className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    )
  }
  
  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
        <p className="text-red-600 mb-4">Failed to load dashboard statistics</p>
        <button onClick={() => refetch()} className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700">
          Retry
        </button>
      </div>
    )
  }
  
  const formatCurrency = (amount: number) =>
    new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(amount)

  const formatCompact = (n: number) =>
    n >= 1000 ? `${(n / 1000).toFixed(1)}k` : String(n)

  const totalProcessed = stats?.totalInvoices || 0
  const autoApproved = stats?.approvedInvoices || 0
  const needsReview = (stats?.pendingInvoices || 0) + (stats?.underReviewInvoices || 0)
  const autoApprovalRate = totalProcessed > 0 ? (autoApproved / totalProcessed) * 100 : 0
  const syncRate = (stats?.syncSuccessRate || 0) * 100

  // --- Chart Data ---

  // Status distribution for donut
  const statusData = [
    { name: 'Pending', value: stats?.pendingInvoices || 0 },
    { name: 'Under Review', value: stats?.underReviewInvoices || 0 },
    { name: 'Approved', value: stats?.approvedInvoices || 0 },
    { name: 'Rejected', value: stats?.rejectedInvoices || 0 },
    { name: 'Synced', value: stats?.syncedInvoices || 0 },
    { name: 'Sync Failed', value: stats?.failedSyncs || 0 },
    { name: 'Overdue', value: stats?.overdueInvoices || 0 },
  ].filter(d => d.value > 0)

  // Monthly trends for area chart
  const trendData = (stats?.monthlyTrends || []).map(m => ({
    name: m.monthName?.slice(0, 3) || `${m.month}`,
    invoices: m.invoiceCount,
    amount: m.totalAmount,
  }))

  // Top vendors for bar chart
  const vendorData = (stats?.topVendors || []).slice(0, 8).map(v => ({
    name: v.vendorName.length > 20 ? v.vendorName.slice(0, 20) + '…' : v.vendorName,
    invoices: v.invoiceCount,
    amount: v.totalAmount,
  }))

  // Financial breakdown for horizontal bar
  const financialData = [
    { name: 'Pending', value: stats?.pendingAmount || 0, fill: COLORS.warning },
    { name: 'Approved', value: stats?.approvedAmount || 0, fill: COLORS.success },
    { name: 'Synced', value: stats?.syncedAmount || 0, fill: COLORS.purple },
  ]

  // Radial gauge data for KPI cards
  const approvalGauge = [{ name: 'rate', value: autoApprovalRate, fill: COLORS.success }]
  const syncGauge = [{ name: 'rate', value: syncRate, fill: COLORS.primary }]

  // Processing throughput mini-bar data
  const throughputData = [
    { name: 'Today', value: stats?.invoicesProcessedToday || 0 },
    { name: 'Week', value: stats?.invoicesProcessedThisWeek || 0 },
    { name: 'Month', value: stats?.invoicesProcessedThisMonth || 0 },
  ]

  return (
    <div className="space-y-6">
      {/* Organization scope banner */}
      {user?.role === 'SUPER_ADMIN' ? (
        <div className="flex items-center px-4 py-2 bg-indigo-50 border border-indigo-200 rounded-lg text-sm text-indigo-700">
          <Shield className="w-4 h-4 mr-2 flex-shrink-0" />
          Viewing <strong className="mx-1">platform-wide</strong> data across all organizations.
        </div>
      ) : user?.organizationName ? (
        <div className="flex items-center px-4 py-2 bg-primary-50 border border-primary-200 rounded-lg text-sm text-primary-700">
          <Building className="w-4 h-4 mr-2 flex-shrink-0" />
          Showing data for <strong className="mx-1">{user.organizationName}</strong> only.
        </div>
      ) : null}

      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="text-gray-500 text-sm mt-1">Invoice processing overview and analytics</p>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-500">
            Last updated: {new Date().toLocaleTimeString()}
          </span>
          <button onClick={() => refetch()} className="flex items-center px-3 py-2 text-sm border rounded-lg hover:bg-gray-50">
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </button>
        </div>
      </div>

      {/* ─── KPI Cards Row with Mini Charts ─── */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-5">
        {/* Total Invoices with trend sparkline */}
        <div
          onClick={() => navigate('/invoices')}
          className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all group"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="p-2 rounded-lg bg-blue-50 text-primary-500">
              <FileText className="w-5 h-5" />
            </div>
            {trendData.length > 1 && (
              <span className="flex items-center text-xs font-medium text-green-600">
                <ArrowUpRight className="w-3.5 h-3.5 mr-0.5" />
                {trendData.length > 0 ? trendData[trendData.length - 1].invoices : 0} latest
              </span>
            )}
          </div>
          <p className="text-sm text-gray-500 mb-0.5">Total Invoices</p>
          <p className="text-3xl font-bold text-gray-900">{formatCompact(totalProcessed)}</p>
          {trendData.length > 1 && (
            <div className="mt-3 h-12">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trendData}>
                  <defs>
                    <linearGradient id="sparkBlue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={COLORS.primary} stopOpacity={0.3} />
                      <stop offset="100%" stopColor={COLORS.primary} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <Area type="monotone" dataKey="invoices" stroke={COLORS.primary} strokeWidth={2} fill="url(#sparkBlue)" dot={false} />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>

        {/* Needs Review with radial indicator */}
        <div
          onClick={() => navigate('/invoices?status=PENDING')}
          className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="p-2 rounded-lg bg-amber-50 text-amber-500">
              <Clock className="w-5 h-5" />
            </div>
            {needsReview > 0 && (
              <span className="px-2 py-0.5 text-[10px] font-semibold bg-amber-100 text-amber-700 rounded-full uppercase tracking-wide">
                Needs Attention
              </span>
            )}
          </div>
          <p className="text-sm text-gray-500 mb-0.5">Pending Review</p>
          <div className="flex items-end justify-between">
            <p className="text-3xl font-bold text-gray-900">{needsReview}</p>
            <div className="w-14 h-14">
              <ResponsiveContainer width="100%" height="100%">
                <RadialBarChart cx="50%" cy="50%" innerRadius="70%" outerRadius="100%" data={[{ value: totalProcessed > 0 ? (needsReview / totalProcessed) * 100 : 0, fill: COLORS.warning }]} startAngle={90} endAngle={-270} barSize={6}>
                  <RadialBar background={{ fill: '#f3f4f6' }} dataKey="value" cornerRadius={4} />
                </RadialBarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Approval Rate gauge */}
        <div
          onClick={() => navigate('/invoices?status=APPROVED')}
          className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="p-2 rounded-lg bg-green-50 text-green-500">
              <CheckCircle className="w-5 h-5" />
            </div>
            <span className="flex items-center text-xs font-medium text-green-600">
              {autoApproved} approved
            </span>
          </div>
          <p className="text-sm text-gray-500 mb-0.5">Approval Rate</p>
          <div className="flex items-end justify-between">
            <p className="text-3xl font-bold text-gray-900">{autoApprovalRate.toFixed(1)}%</p>
            <div className="w-14 h-14">
              <ResponsiveContainer width="100%" height="100%">
                <RadialBarChart cx="50%" cy="50%" innerRadius="70%" outerRadius="100%" data={approvalGauge} startAngle={90} endAngle={-270} barSize={6}>
                  <RadialBar background={{ fill: '#f3f4f6' }} dataKey="value" cornerRadius={4} />
                </RadialBarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        {/* Sync Rate gauge */}
        <div
          onClick={() => navigate('/invoices?status=SYNCED')}
          className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all"
        >
          <div className="flex items-center justify-between mb-3">
            <div className="p-2 rounded-lg bg-purple-50 text-purple-500">
              <Upload className="w-5 h-5" />
            </div>
            <span className="flex items-center text-xs font-medium text-purple-600">
              {stats?.syncedInvoices || 0} synced
            </span>
          </div>
          <p className="text-sm text-gray-500 mb-0.5">Sync Success Rate</p>
          <div className="flex items-end justify-between">
            <p className="text-3xl font-bold text-gray-900">{syncRate.toFixed(1)}%</p>
            <div className="w-14 h-14">
              <ResponsiveContainer width="100%" height="100%">
                <RadialBarChart cx="50%" cy="50%" innerRadius="70%" outerRadius="100%" data={syncGauge} startAngle={90} endAngle={-270} barSize={6}>
                  <RadialBar background={{ fill: '#f3f4f6' }} dataKey="value" cornerRadius={4} />
                </RadialBarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </div>

      {/* ─── Row 2: Monthly Trend + Status Distribution ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monthly Volume Trend - Area chart */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-1">Invoice Volume Trend</h2>
          <p className="text-xs text-gray-400 mb-4">Monthly invoice count & value</p>
          {trendData.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={trendData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                  <defs>
                    <linearGradient id="gradBlue" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={COLORS.primary} stopOpacity={0.2} />
                      <stop offset="100%" stopColor={COLORS.primary} stopOpacity={0} />
                    </linearGradient>
                    <linearGradient id="gradGreen" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor={COLORS.success} stopOpacity={0.15} />
                      <stop offset="100%" stopColor={COLORS.success} stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                  <YAxis yAxisId="left" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                  <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} tickFormatter={(v: number) => `$${(v/1000).toFixed(0)}k`} />
                  <Tooltip
                    contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
                    formatter={(value: number, name: string) => [name === 'amount' ? formatCurrency(value) : value, name === 'amount' ? 'Value' : 'Invoices']}
                  />
                  <Area yAxisId="left" type="monotone" dataKey="invoices" stroke={COLORS.primary} strokeWidth={2.5} fill="url(#gradBlue)" dot={{ r: 3, fill: COLORS.primary }} activeDot={{ r: 5 }} />
                  <Area yAxisId="right" type="monotone" dataKey="amount" stroke={COLORS.success} strokeWidth={2} fill="url(#gradGreen)" dot={false} strokeDasharray="5 3" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-72 flex items-center justify-center text-gray-400 text-sm">
              No trend data available yet
            </div>
          )}
        </div>

        {/* Status Distribution - Donut */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-1">Status Distribution</h2>
          <p className="text-xs text-gray-400 mb-4">Current invoice breakdown</p>
          {statusData.length > 0 ? (
            <>
              <div className="h-52">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie
                      data={statusData}
                      cx="50%"
                      cy="50%"
                      innerRadius={55}
                      outerRadius={80}
                      paddingAngle={3}
                      dataKey="value"
                      stroke="none"
                    >
                      {statusData.map((_entry, index) => (
                        <Cell key={`cell-${index}`} fill={STATUS_COLORS[
                          ['Pending', 'Under Review', 'Approved', 'Rejected', 'Synced', 'Sync Failed', 'Overdue'].indexOf(_entry.name)
                        ] || COLORS.slate} />
                      ))}
                    </Pie>
                    <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }} />
                  </PieChart>
                </ResponsiveContainer>
              </div>
              <div className="space-y-2 mt-2">
                {statusData.map((item) => (
                  <div
                    key={item.name}
                    onClick={() => {
                      const routes: Record<string, string> = {
                        'Pending': '/invoices?status=PENDING',
                        'Under Review': '/invoices?status=UNDER_REVIEW',
                        'Approved': '/invoices?status=APPROVED',
                        'Rejected': '/invoices?status=REJECTED',
                        'Synced': '/invoices?status=SYNCED',
                        'Sync Failed': '/invoices?status=SYNC_FAILED',
                        'Overdue': '/invoices?overdue=true',
                      }
                      navigate(routes[item.name] || '/invoices')
                    }}
                    className="flex items-center justify-between text-sm cursor-pointer hover:bg-gray-50 rounded px-2 py-1 -mx-2 transition-colors"
                  >
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: STATUS_COLORS[['Pending', 'Under Review', 'Approved', 'Rejected', 'Synced', 'Sync Failed', 'Overdue'].indexOf(item.name)] || COLORS.slate }} />
                      <span className="text-gray-600">{item.name}</span>
                    </div>
                    <span className="font-semibold text-gray-900">{item.value}</span>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <div className="h-52 flex items-center justify-center text-gray-400 text-sm">
              No invoice data yet
            </div>
          )}
        </div>
      </div>

      {/* ─── Row 3: Top Vendors + Financial Breakdown ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Vendors bar chart */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-1">Top Vendors</h2>
          <p className="text-xs text-gray-400 mb-4">By invoice count</p>
          {vendorData.length > 0 ? (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={vendorData} layout="vertical" margin={{ top: 0, right: 20, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" horizontal={false} />
                  <XAxis type="number" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                  <YAxis type="category" dataKey="name" tick={{ fontSize: 11, fill: '#64748b' }} axisLine={false} tickLine={false} width={130} />
                  <Tooltip
                    contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
                    formatter={(value: number, name: string) => [name === 'amount' ? formatCurrency(value) : value, name === 'amount' ? 'Total Value' : 'Invoices']}
                  />
                  <Bar dataKey="invoices" fill={COLORS.primary} radius={[0, 4, 4, 0]} barSize={18} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="h-72 flex items-center justify-center text-gray-400 text-sm">
              No vendor data yet
            </div>
          )}
        </div>

        {/* Financial Breakdown */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-1">
            <h2 className="text-base font-semibold text-gray-900">Financial Summary</h2>
            <DollarSign className="w-5 h-5 text-green-500" />
          </div>
          <p className="text-xs text-gray-400 mb-5">Value by invoice status</p>

          <div className="mb-6 text-center">
            <p className="text-xs text-gray-400 uppercase tracking-wider mb-1">Total Invoice Value</p>
            <p className="text-3xl font-bold text-gray-900">{formatCurrency(stats?.totalAmount || 0)}</p>
          </div>

          <div className="space-y-4">
            {financialData.map(item => {
              const pct = (stats?.totalAmount || 0) > 0 ? (item.value / (stats?.totalAmount || 1)) * 100 : 0
              return (
                <div key={item.name}>
                  <div className="flex items-center justify-between mb-1.5">
                    <div className="flex items-center gap-2">
                      <div className="w-2.5 h-2.5 rounded-full" style={{ backgroundColor: item.fill }} />
                      <span className="text-sm text-gray-600">{item.name}</span>
                    </div>
                    <span className="text-sm font-semibold text-gray-900">{formatCurrency(item.value)}</span>
                  </div>
                  <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className="h-full rounded-full transition-all duration-700"
                      style={{ width: `${pct}%`, backgroundColor: item.fill }}
                    />
                  </div>
                  <p className="text-[11px] text-gray-400 mt-0.5 text-right">{pct.toFixed(1)}%</p>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {/* ─── Row 4: Processing Throughput + Alert Cards ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Processing Throughput */}
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-1">
            <h2 className="text-base font-semibold text-gray-900">Processing Throughput</h2>
            <TrendingUp className="w-5 h-5 text-primary-500" />
          </div>
          <p className="text-xs text-gray-400 mb-4">Invoices processed</p>
          <div className="h-40">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={throughputData} margin={{ top: 5, right: 5, left: 5, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0' }} />
                <Bar dataKey="value" fill={COLORS.indigo} radius={[4, 4, 0, 0]} barSize={32} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          {stats?.averageProcessingTimeMs != null && stats.averageProcessingTimeMs > 0 && (
            <p className="text-xs text-gray-400 mt-3 text-center">
              Avg processing time: <span className="font-medium text-gray-600">{(stats.averageProcessingTimeMs / 1000).toFixed(1)}s</span>
            </p>
          )}
        </div>

        {/* Alert / Action Cards */}
        <div className="lg:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div
            onClick={() => navigate('/invoices?status=REJECTED')}
            className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all border-l-4 border-red-400"
          >
            <div className="flex items-start gap-3">
              <div className="p-2 bg-red-50 rounded-lg text-red-500">
                <XCircle className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Rejected Invoices</p>
                <p className="text-2xl font-bold text-gray-900">{stats?.rejectedInvoices || 0}</p>
                {(stats?.rejectedInvoices || 0) > 0 && (
                  <p className="text-xs text-red-500 mt-1 flex items-center gap-1">
                    <ArrowDownRight className="w-3 h-3" /> Requires attention
                  </p>
                )}
              </div>
            </div>
          </div>

          <div
            onClick={() => navigate('/invoices?overdue=true')}
            className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all border-l-4 border-orange-400"
          >
            <div className="flex items-start gap-3">
              <div className="p-2 bg-orange-50 rounded-lg text-orange-500">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Overdue Invoices</p>
                <p className="text-2xl font-bold text-gray-900">{stats?.overdueInvoices || 0}</p>
                {(stats?.overdueInvoices || 0) > 0 && (
                  <span className="inline-block mt-1 px-2 py-0.5 text-[10px] font-semibold bg-red-100 text-red-700 rounded-full uppercase">Critical</span>
                )}
              </div>
            </div>
          </div>

          <div
            onClick={() => navigate('/invoices?status=SYNC_FAILED')}
            className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all border-l-4 border-amber-400"
          >
            <div className="flex items-start gap-3">
              <div className="p-2 bg-amber-50 rounded-lg text-amber-500">
                <RefreshCw className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Failed Syncs</p>
                <p className="text-2xl font-bold text-gray-900">{stats?.failedSyncs || 0}</p>
                <p className="text-xs text-gray-400 mt-1">Retry available</p>
              </div>
            </div>
          </div>

          <div
            onClick={() => navigate('/invoices?status=UNDER_REVIEW')}
            className="bg-white rounded-xl shadow-sm p-5 cursor-pointer hover:shadow-md transition-all border-l-4 border-blue-400"
          >
            <div className="flex items-start gap-3">
              <div className="p-2 bg-blue-50 rounded-lg text-blue-500">
                <Eye className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Manual Review</p>
                <p className="text-2xl font-bold text-gray-900">{stats?.underReviewInvoices || 0}</p>
                <p className="text-xs text-gray-400 mt-1">Low confidence flags</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
