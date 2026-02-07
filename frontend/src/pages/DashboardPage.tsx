import { useQuery } from '@tanstack/react-query'
import { dashboardService } from '@/services/dashboardService'
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
  Download,
  Sparkles,
  Eye,
  BarChart3,
  PieChart
} from 'lucide-react'

export function DashboardPage() {
  const { data: stats, isLoading, error, refetch } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: () => dashboardService.getStats(),
    refetchInterval: 30000, // Refresh every 30 seconds
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
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
        >
          Retry
        </button>
      </div>
    )
  }
  
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount)
  }

  // Calculate auto-approved (high confidence) vs manual review needed
  const autoApproved = stats?.approvedInvoices || 0
  const needsReview = (stats?.pendingInvoices || 0) + (stats?.underReviewInvoices || 0)
  const totalProcessed = (stats?.totalInvoices || 0)
  const autoApprovalRate = totalProcessed > 0 ? ((autoApproved / totalProcessed) * 100).toFixed(1) : '0'

  // Status cards for main metrics
  const statusCards = [
    {
      label: 'Total Invoices',
      value: stats?.totalInvoices || 0,
      icon: FileText,
      color: 'bg-gradient-to-br from-blue-500 to-blue-600',
      textColor: 'text-blue-600',
      bgLight: 'bg-blue-50',
    },
    {
      label: 'Pending Review',
      value: needsReview,
      icon: Clock,
      color: 'bg-gradient-to-br from-yellow-500 to-orange-500',
      textColor: 'text-yellow-600',
      bgLight: 'bg-yellow-50',
      badge: needsReview > 0 ? 'Needs Attention' : null,
    },
    {
      label: 'Approved',
      value: stats?.approvedInvoices || 0,
      icon: CheckCircle,
      color: 'bg-gradient-to-br from-green-500 to-emerald-600',
      textColor: 'text-green-600',
      bgLight: 'bg-green-50',
    },
    {
      label: 'Synced to Sage',
      value: stats?.syncedInvoices || 0,
      icon: Upload,
      color: 'bg-gradient-to-br from-purple-500 to-indigo-600',
      textColor: 'text-purple-600',
      bgLight: 'bg-purple-50',
    },
    {
      label: 'Rejected',
      value: stats?.rejectedInvoices || 0,
      icon: XCircle,
      color: 'bg-gradient-to-br from-red-500 to-red-600',
      textColor: 'text-red-600',
      bgLight: 'bg-red-50',
    },
    {
      label: 'Overdue',
      value: stats?.overdueInvoices || 0,
      icon: AlertTriangle,
      color: 'bg-gradient-to-br from-orange-500 to-red-500',
      textColor: 'text-orange-600',
      bgLight: 'bg-orange-50',
      badge: (stats?.overdueInvoices || 0) > 0 ? 'Critical' : null,
    },
  ]

  // Processing pipeline visualization
  const pipelineStages = [
    { label: 'Received', value: stats?.totalInvoices || 0, icon: Download, color: 'bg-blue-500' },
    { label: 'Extracted', value: stats?.totalInvoices || 0, icon: Eye, color: 'bg-indigo-500' },
    { label: 'Auto-Approved', value: autoApproved, icon: Sparkles, color: 'bg-green-500' },
    { label: 'Pushed to Sage', value: stats?.syncedInvoices || 0, icon: Upload, color: 'bg-purple-500' },
  ]

  return (
    <div className="space-y-6">
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
          <button
            onClick={() => refetch()}
            className="flex items-center px-3 py-2 text-sm border rounded-lg hover:bg-gray-50"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </button>
        </div>
      </div>

      {/* Quick Stats Banner */}
      <div className="bg-gradient-to-r from-primary-600 to-primary-800 rounded-xl p-6 text-white">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div>
            <p className="text-primary-200 text-sm">Auto-Approval Rate</p>
            <p className="text-3xl font-bold">{autoApprovalRate}%</p>
            <p className="text-primary-200 text-xs mt-1">High confidence invoices</p>
          </div>
          <div>
            <p className="text-primary-200 text-sm">Processed Today</p>
            <p className="text-3xl font-bold">{stats?.invoicesProcessedToday || 0}</p>
            <p className="text-primary-200 text-xs mt-1">invoices</p>
          </div>
          <div>
            <p className="text-primary-200 text-sm">Sync Success Rate</p>
            <p className="text-3xl font-bold">{((stats?.syncSuccessRate || 0) * 100).toFixed(1)}%</p>
            <p className="text-primary-200 text-xs mt-1">to Sage Intacct</p>
          </div>
          <div>
            <p className="text-primary-200 text-sm">Total Value</p>
            <p className="text-3xl font-bold">{formatCurrency(stats?.totalAmount || 0)}</p>
            <p className="text-primary-200 text-xs mt-1">all invoices</p>
          </div>
        </div>
      </div>

      {/* Processing Pipeline */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <BarChart3 className="w-5 h-5 mr-2 text-primary-500" />
          Invoice Processing Pipeline
        </h2>
        <div className="flex items-center justify-between">
          {pipelineStages.map((stage, index) => (
            <div key={stage.label} className="flex items-center flex-1">
              <div className="flex flex-col items-center flex-1">
                <div className={`${stage.color} p-3 rounded-full mb-2`}>
                  <stage.icon className="w-6 h-6 text-white" />
                </div>
                <p className="text-2xl font-bold text-gray-900">{stage.value}</p>
                <p className="text-sm text-gray-500">{stage.label}</p>
              </div>
              {index < pipelineStages.length - 1 && (
                <div className="flex-shrink-0 w-16 h-1 bg-gray-200 mx-2">
                  <div 
                    className="h-full bg-primary-500 transition-all"
                    style={{ 
                      width: `${stage.value > 0 ? (pipelineStages[index + 1].value / stage.value) * 100 : 0}%` 
                    }}
                  />
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {statusCards.map((card) => (
          <div key={card.label} className="bg-white rounded-xl shadow-sm p-6 hover:shadow-md transition-shadow">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <div className={`${card.color} p-3 rounded-lg`}>
                  <card.icon className="w-6 h-6 text-white" />
                </div>
                <div className="ml-4">
                  <p className="text-sm text-gray-500">{card.label}</p>
                  <p className="text-2xl font-bold text-gray-900">{card.value}</p>
                </div>
              </div>
              {card.badge && (
                <span className={`px-2 py-1 text-xs rounded-full ${
                  card.badge === 'Critical' 
                    ? 'bg-red-100 text-red-700' 
                    : 'bg-yellow-100 text-yellow-700'
                }`}>
                  {card.badge}
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
      
      {/* Financial Summary & Processing Metrics */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <DollarSign className="w-5 h-5 mr-2 text-green-500" />
            Financial Summary
          </h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center py-3 border-b">
              <span className="text-gray-600">Total Invoice Value</span>
              <span className="font-semibold text-lg">{formatCurrency(stats?.totalAmount || 0)}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b">
              <div className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-yellow-500 mr-2" />
                <span className="text-gray-600">Pending Approval</span>
              </div>
              <span className="font-semibold text-yellow-600">
                {formatCurrency(stats?.pendingAmount || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center py-3 border-b">
              <div className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-green-500 mr-2" />
                <span className="text-gray-600">Approved Value</span>
              </div>
              <span className="font-semibold text-green-600">
                {formatCurrency(stats?.approvedAmount || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center py-3">
              <div className="flex items-center">
                <div className="w-3 h-3 rounded-full bg-purple-500 mr-2" />
                <span className="text-gray-600">Synced to Sage</span>
              </div>
              <span className="font-semibold text-purple-600">
                {formatCurrency(stats?.syncedAmount || 0)}
              </span>
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <TrendingUp className="w-5 h-5 mr-2 text-blue-500" />
            Processing Metrics
          </h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center py-3 border-b">
              <span className="text-gray-600">Processed Today</span>
              <span className="font-semibold">{stats?.invoicesProcessedToday || 0}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b">
              <span className="text-gray-600">This Week</span>
              <span className="font-semibold">{stats?.invoicesProcessedThisWeek || 0}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b">
              <span className="text-gray-600">This Month</span>
              <span className="font-semibold">{stats?.invoicesProcessedThisMonth || 0}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b">
              <span className="text-gray-600">Avg Processing Time</span>
              <span className="font-semibold">
                {stats?.averageProcessingTimeMs 
                  ? `${(stats.averageProcessingTimeMs / 1000).toFixed(1)}s` 
                  : 'N/A'}
              </span>
            </div>
            <div className="flex justify-between items-center py-3">
              <span className="text-gray-600">Sync Success Rate</span>
              <div className="flex items-center">
                <div className="w-24 h-2 bg-gray-200 rounded-full mr-2">
                  <div 
                    className="h-full bg-green-500 rounded-full"
                    style={{ width: `${(stats?.syncSuccessRate || 0) * 100}%` }}
                  />
                </div>
                <span className="font-semibold text-green-600">
                  {((stats?.syncSuccessRate || 0) * 100).toFixed(1)}%
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Status Distribution Chart (Simple) */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <PieChart className="w-5 h-5 mr-2 text-primary-500" />
          Invoice Status Distribution
        </h2>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
          {[
            { label: 'Pending', value: stats?.pendingInvoices || 0, color: 'bg-yellow-500' },
            { label: 'Under Review', value: stats?.underReviewInvoices || 0, color: 'bg-blue-500' },
            { label: 'Approved', value: stats?.approvedInvoices || 0, color: 'bg-green-500' },
            { label: 'Rejected', value: stats?.rejectedInvoices || 0, color: 'bg-red-500' },
            { label: 'Synced', value: stats?.syncedInvoices || 0, color: 'bg-purple-500' },
            { label: 'Sync Failed', value: stats?.failedSyncs || 0, color: 'bg-orange-500' },
            { label: 'Overdue', value: stats?.overdueInvoices || 0, color: 'bg-red-600' },
          ].map((item) => (
            <div key={item.label} className="text-center p-4 rounded-lg bg-gray-50">
              <div className={`w-4 h-4 ${item.color} rounded-full mx-auto mb-2`} />
              <p className="text-2xl font-bold text-gray-900">{item.value}</p>
              <p className="text-xs text-gray-500">{item.label}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <a 
            href="/invoices?status=PENDING"
            className="flex items-center p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <Clock className="w-8 h-8 text-yellow-500 mr-3" />
            <div>
              <p className="font-medium">Review Pending</p>
              <p className="text-sm text-gray-500">{stats?.pendingInvoices || 0} invoices</p>
            </div>
          </a>
          <a 
            href="/invoices?requiresManualReview=true"
            className="flex items-center p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <Eye className="w-8 h-8 text-blue-500 mr-3" />
            <div>
              <p className="font-medium">Manual Review</p>
              <p className="text-sm text-gray-500">Low confidence</p>
            </div>
          </a>
          <a 
            href="/invoices?syncStatus=FAILED"
            className="flex items-center p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <AlertTriangle className="w-8 h-8 text-red-500 mr-3" />
            <div>
              <p className="font-medium">Failed Syncs</p>
              <p className="text-sm text-gray-500">{stats?.failedSyncs || 0} to retry</p>
            </div>
          </a>
          <a 
            href="/invoices"
            className="flex items-center p-4 border rounded-lg hover:bg-gray-50 transition-colors"
          >
            <FileText className="w-8 h-8 text-primary-500 mr-3" />
            <div>
              <p className="font-medium">All Invoices</p>
              <p className="text-sm text-gray-500">View all</p>
            </div>
          </a>
        </div>
      </div>
    </div>
  )
}
