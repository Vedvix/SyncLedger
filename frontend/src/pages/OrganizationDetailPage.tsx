import { useNavigate, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { 
  ArrowLeft, 
  Building2, 
  Mail, 
  Globe, 
  Calendar,
  Users,
  FileText,
  Edit,
  CheckCircle,
  XCircle,
  AlertCircle,
  Activity,
  Plug,
  Shield,
} from 'lucide-react'
import { organizationService } from '@/services/organizationService'
import { ERP_TYPE_LABELS } from '@/types'
import type { ErpType } from '@/types'

export function OrganizationDetailPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  
  const { data: organization, isLoading: loadingOrg } = useQuery({
    queryKey: ['organization', id],
    queryFn: () => organizationService.getOrganization(Number(id)),
    enabled: !!id,
  })
  
  const { data: stats, isLoading: loadingStats } = useQuery({
    queryKey: ['organization-stats', id],
    queryFn: () => organizationService.getOrganizationStats(Number(id)),
    enabled: !!id,
  })
  
  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm font-medium">
            <CheckCircle className="w-4 h-4" />
            Active
          </span>
        )
      case 'SUSPENDED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-red-100 text-red-800 rounded-full text-sm font-medium">
            <XCircle className="w-4 h-4" />
            Suspended
          </span>
        )
      case 'ONBOARDING':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-yellow-100 text-yellow-800 rounded-full text-sm font-medium">
            <AlertCircle className="w-4 h-4" />
            Onboarding
          </span>
        )
      case 'INACTIVE':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm font-medium">
            Inactive
          </span>
        )
      default:
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 bg-gray-100 text-gray-800 rounded-full text-sm font-medium">
            {status}
          </span>
        )
    }
  }
  
  if (loadingOrg) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    )
  }
  
  if (!organization) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-4">
        <AlertCircle className="w-12 h-12 text-red-500" />
        <p className="text-gray-600">Organization not found</p>
        <button
          onClick={() => navigate('/super-admin')}
          className="px-4 py-2 text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
        >
          Back to Platform Admin
        </button>
      </div>
    )
  }
  
  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/super-admin')}
            className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900">{organization.name}</h1>
              {getStatusBadge(organization.status)}
            </div>
            <p className="text-gray-500 text-sm mt-1">/{organization.slug}</p>
          </div>
        </div>
        
        <button
          onClick={() => navigate(`/super-admin/organizations/${id}/edit`)}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors flex items-center gap-2"
        >
          <Edit className="w-4 h-4" />
          Edit Organization
        </button>
      </div>
      
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Users</p>
              <p className="text-2xl font-bold text-gray-900">
                {loadingStats ? '...' : stats?.totalUsers || 0}
              </p>
            </div>
            <div className="p-3 bg-blue-100 rounded-lg">
              <Users className="w-6 h-6 text-blue-600" />
            </div>
          </div>
          {!loadingStats && stats && (
            <p className="text-sm text-gray-500 mt-2">
              {stats.activeUsers} active
            </p>
          )}
        </div>
        
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Total Invoices</p>
              <p className="text-2xl font-bold text-gray-900">
                {loadingStats ? '...' : stats?.totalInvoices || 0}
              </p>
            </div>
            <div className="p-3 bg-green-100 rounded-lg">
              <FileText className="w-6 h-6 text-green-600" />
            </div>
          </div>
        </div>
        
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Status</p>
              <p className="text-lg font-semibold text-gray-900 mt-1">
                {organization.status}
              </p>
            </div>
            <div className="p-3 bg-purple-100 rounded-lg">
              <Activity className="w-6 h-6 text-purple-600" />
            </div>
          </div>
        </div>
      </div>
      
      {/* Details */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="px-6 py-4 border-b">
          <h2 className="text-lg font-semibold text-gray-900">Organization Details</h2>
        </div>
        
        <div className="p-6 space-y-6">
          {/* Basic Information */}
          <div>
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-3">
              Basic Information
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-start gap-3">
                <Building2 className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Organization Name</p>
                  <p className="font-medium text-gray-900">{organization.name}</p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Globe className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">URL Slug</p>
                  <p className="font-medium text-gray-900">{organization.slug}</p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Mail className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Email Address</p>
                  <p className="font-medium text-gray-900">
                    {organization.emailAddress || <span className="text-gray-400 italic">Not set</span>}
                  </p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Calendar className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Created</p>
                  <p className="font-medium text-gray-900">
                    {new Date(organization.createdAt).toLocaleDateString('en-US', {
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })}
                  </p>
                </div>
              </div>
            </div>
          </div>
          
          {/* Microsoft Email Integration */}
          <div className="pt-4 border-t">
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-3">
              Microsoft Email Integration
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-start gap-3">
                <Shield className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Credentials Status</p>
                  <p className="font-medium">
                    {organization.msCredentialsConfigured ? (
                      organization.msCredentialsVerified ? (
                        <span className="text-green-600 flex items-center gap-1">
                          <CheckCircle className="w-4 h-4" /> Configured &amp; Verified
                        </span>
                      ) : (
                        <span className="text-yellow-600 flex items-center gap-1">
                          <AlertCircle className="w-4 h-4" /> Configured (Unverified)
                        </span>
                      )
                    ) : (
                      <span className="text-gray-400 italic flex items-center gap-1">
                        <XCircle className="w-4 h-4" /> Not configured
                      </span>
                    )}
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Mail className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Monitored Mailbox</p>
                  <p className="font-medium text-gray-900">
                    {organization.msMailboxEmail || <span className="text-gray-400 italic">Not set</span>}
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Globe className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Tenant ID</p>
                  <p className="font-medium text-gray-900">
                    {organization.msTenantId || <span className="text-gray-400 italic">Not set</span>}
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Activity className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Client ID</p>
                  <p className="font-medium text-gray-900">
                    {organization.msClientId || <span className="text-gray-400 italic">Not set</span>}
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Integration Settings */}
          <div className="pt-4 border-t">
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-3">
              ERP Integration
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex items-start gap-3">
                <Plug className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">ERP System</p>
                  <p className="font-medium text-gray-900">
                    {organization.erpType && organization.erpType !== 'NONE'
                      ? ERP_TYPE_LABELS[organization.erpType as ErpType] || organization.erpType
                      : <span className="text-gray-400 italic">Not configured</span>}
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Globe className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">API Endpoint</p>
                  <p className="font-medium text-gray-900">
                    {organization.erpApiEndpoint || organization.sageApiEndpoint || <span className="text-gray-400 italic">Not configured</span>}
                  </p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Activity className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Integration Status</p>
                  <p className="font-medium text-gray-900">
                    {organization.erpConfigured
                      ? <span className="text-green-600">Configured</span>
                      : <span className="text-gray-400 italic">Not configured</span>}
                  </p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <Activity className="w-5 h-5 text-gray-400 mt-0.5" />
                <div>
                  <p className="text-sm text-gray-500">Auto Sync</p>
                  <p className="font-medium text-gray-900">
                    {organization.erpAutoSync !== false
                      ? <span className="text-green-600">Enabled</span>
                      : <span className="text-gray-500">Disabled</span>}
                  </p>
                </div>
              </div>
            </div>
          </div>
          
          {/* AWS Settings */}
          <div className="pt-4 border-t">
            <h3 className="text-sm font-medium text-gray-500 uppercase tracking-wide mb-3">
              AWS Configuration
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500">S3 Folder Path</p>
                <p className="font-medium text-gray-900">
                  {organization.s3FolderPath || <span className="text-gray-400 italic">Not set</span>}
                </p>
              </div>
              
              <div>
                <p className="text-sm text-gray-500">SQS Queue Name</p>
                <p className="font-medium text-gray-900">
                  {organization.sqsQueueName || <span className="text-gray-400 italic">Not set</span>}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      {/* Timestamps */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="px-6 py-4 border-b">
          <h2 className="text-lg font-semibold text-gray-900">Activity</h2>
        </div>
        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <p className="text-sm text-gray-500">Created At</p>
              <p className="font-medium text-gray-900">
                {new Date(organization.createdAt).toLocaleString()}
              </p>
            </div>
            <div>
              <p className="text-sm text-gray-500">Last Updated</p>
              <p className="font-medium text-gray-900">
                {new Date(organization.updatedAt).toLocaleString()}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
