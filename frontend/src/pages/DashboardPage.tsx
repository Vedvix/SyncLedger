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
  TrendingUp
} from 'lucide-react'

export function DashboardPage() {
  const { data: stats, isLoading, error } = useQuery({
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
        <p className="text-red-600">Failed to load dashboard statistics</p>
      </div>
    )
  }
  
  const statCards = [
    {
      label: 'Total Invoices',
      value: stats?.totalInvoices || 0,
      icon: FileText,
      color: 'bg-blue-500',
    },
    {
      label: 'Pending Review',
      value: stats?.pendingInvoices || 0,
      icon: Clock,
      color: 'bg-yellow-500',
    },
    {
      label: 'Approved',
      value: stats?.approvedInvoices || 0,
      icon: CheckCircle,
      color: 'bg-green-500',
    },
    {
      label: 'Rejected',
      value: stats?.rejectedInvoices || 0,
      icon: XCircle,
      color: 'bg-red-500',
    },
    {
      label: 'Synced to Sage',
      value: stats?.syncedInvoices || 0,
      icon: RefreshCw,
      color: 'bg-purple-500',
    },
    {
      label: 'Overdue',
      value: stats?.overdueInvoices || 0,
      icon: AlertTriangle,
      color: 'bg-orange-500',
    },
  ]
  
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount)
  }
  
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <span className="text-sm text-gray-500">
          Last updated: {new Date().toLocaleTimeString()}
        </span>
      </div>
      
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {statCards.map((card) => (
          <div key={card.label} className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center">
              <div className={`${card.color} p-3 rounded-lg`}>
                <card.icon className="w-6 h-6 text-white" />
              </div>
              <div className="ml-4">
                <p className="text-sm text-gray-500">{card.label}</p>
                <p className="text-2xl font-bold text-gray-900">{card.value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>
      
      {/* Financial Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
            <DollarSign className="w-5 h-5 mr-2 text-green-500" />
            Financial Summary
          </h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center py-2 border-b">
              <span className="text-gray-600">Total Invoice Value</span>
              <span className="font-semibold">{formatCurrency(stats?.totalAmount || 0)}</span>
            </div>
            <div className="flex justify-between items-center py-2 border-b">
              <span className="text-gray-600">Pending Approval</span>
              <span className="font-semibold text-yellow-600">
                {formatCurrency(stats?.pendingAmount || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center py-2 border-b">
              <span className="text-gray-600">Approved Value</span>
              <span className="font-semibold text-green-600">
                {formatCurrency(stats?.approvedAmount || 0)}
              </span>
            </div>
            <div className="flex justify-between items-center py-2">
              <span className="text-gray-600">Synced to Sage</span>
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
            <div className="flex justify-between items-center py-2 border-b">
              <span className="text-gray-600">Processed Today</span>
              <span className="font-semibold">{stats?.invoicesProcessedToday || 0}</span>
            </div>
            <div className="flex justify-between items-center py-2 border-b">
              <span className="text-gray-600">This Week</span>
              <span className="font-semibold">{stats?.invoicesProcessedThisWeek || 0}</span>
            </div>
            <div className="flex justify-between items-center py-2 border-b">
              <span className="text-gray-600">This Month</span>
              <span className="font-semibold">{stats?.invoicesProcessedThisMonth || 0}</span>
            </div>
            <div className="flex justify-between items-center py-2">
              <span className="text-gray-600">Sync Success Rate</span>
              <span className="font-semibold text-green-600">
                {((stats?.syncSuccessRate || 0) * 100).toFixed(1)}%
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
