import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { microsoftConfigService } from '@/services/subscriptionService'
import { organizationService } from '@/services/organizationService'
import { emailService } from '@/services/emailService'
import { useAuthStore } from '@/store/authStore'
import {
  Mail,
  Key,
  Eye,
  EyeOff,
  Shield,
  CheckCircle,
  AlertTriangle,
  Loader2,
  Save,
  RefreshCw,
  ExternalLink,
  Info,
} from 'lucide-react'
import type { Organization, UpdateMicrosoftConfigRequest } from '@/types'

export function MicrosoftConfigPage() {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const isSuperAdmin = user?.role === 'SUPER_ADMIN'
  const isAdmin = user?.role === 'ADMIN' || isSuperAdmin
  const isOrgAdmin = user?.role === 'ADMIN'

  const [showSecret, setShowSecret] = useState(false)
  const [testingOrgId, setTestingOrgId] = useState<number | null>(null)
  const [testResults, setTestResults] = useState<Record<number, boolean>>({})
  const [formData, setFormData] = useState<UpdateMicrosoftConfigRequest>({
    msClientId: '',
    msClientSecret: '',
    msTenantId: '',
    msMailboxEmail: '',
  })
  const [isEditing, setIsEditing] = useState(false)

  const {
    data: config,
    isLoading,
    error: _error,
  } = useQuery({
    queryKey: ['microsoft-config'],
    queryFn: () => microsoftConfigService.getMicrosoftConfig(),
    enabled: isOrgAdmin,
  })

  const {
    data: superAdminOrgs = [],
    isLoading: superAdminOrgsLoading,
  } = useQuery({
    queryKey: ['super-admin-orgs-email-config'],
    queryFn: async (): Promise<Organization[]> => {
      const response = await organizationService.getOrganizations()
      return response.content
    },
    enabled: isSuperAdmin,
  })

  const updateMutation = useMutation({
    mutationFn: (data: UpdateMicrosoftConfigRequest) =>
      microsoftConfigService.updateMicrosoftConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['microsoft-config'] })
      setIsEditing(false)
      setFormData({ msClientId: '', msClientSecret: '', msTenantId: '', msMailboxEmail: '' })
    },
  })

  const verifyMutation = useMutation({
    mutationFn: () => microsoftConfigService.verifyMicrosoftConfig(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['microsoft-config'] })
    },
  })

  const handleEdit = () => {
    setFormData({
      msClientId: config?.msClientId || '',
      msClientSecret: '', // Secret is masked, user must re-enter
      msTenantId: config?.msTenantId || '',
      msMailboxEmail: config?.msMailboxEmail || '',
    })
    setIsEditing(true)
  }

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault()
    updateMutation.mutate(formData)
  }

  const handleSuperAdminTestConnection = async (org: Organization) => {
    try {
      setTestingOrgId(org.id)
      const connected = await emailService.testEmailConnection(org.id)
      setTestResults((prev) => ({ ...prev, [org.id]: connected }))
    } finally {
      setTestingOrgId(null)
    }
  }

  const maskValue = (value?: string) => {
    if (!value) return '-'
    if (value.length <= 8) return value
    return `${value.slice(0, 4)}...${value.slice(-4)}`
  }

  if (!isAdmin) {
    return (
      <div className="p-6 bg-yellow-50 border border-yellow-200 rounded-xl text-yellow-800">
        <AlertTriangle className="w-5 h-5 inline mr-2" />
        Only admins can manage Microsoft Graph integration settings.
      </div>
    )
  }

  if (isSuperAdmin) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="page-header">Organization Email Configurations</h1>
          <p className="page-subtitle">
            View Microsoft Graph configuration and test mailbox connection for each organization
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6 overflow-x-auto">
          {superAdminOrgsLoading ? (
            <div className="flex items-center justify-center h-32">
              <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left">
                  <th className="py-3 pr-4 font-medium text-gray-600">Organization</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Status</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Mailbox</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Tenant ID</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Client ID</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Action</th>
                </tr>
              </thead>
              <tbody>
                {superAdminOrgs.map((org) => {
                  const hasConfig = !!(org.msClientId && org.msTenantId && org.msMailboxEmail)
                  const testResult = testResults[org.id]
                  return (
                    <tr key={org.id} className="border-b last:border-b-0">
                      <td className="py-3 pr-4">
                        <div className="font-medium text-gray-900">{org.name}</div>
                        <div className="text-xs text-gray-500">{org.slug}</div>
                      </td>
                      <td className="py-3 pr-4">
                        {hasConfig ? (
                          org.msCredentialsVerified ? (
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-green-100 text-green-800 text-xs">
                              <CheckCircle className="w-3 h-3" /> Verified
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-yellow-100 text-yellow-800 text-xs">
                              <AlertTriangle className="w-3 h-3" /> Unverified
                            </span>
                          )
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-gray-100 text-gray-600 text-xs">
                            <Info className="w-3 h-3" /> Not Configured
                          </span>
                        )}
                      </td>
                      <td className="py-3 pr-4 text-gray-700">{org.msMailboxEmail || '-'}</td>
                      <td className="py-3 pr-4 font-mono text-xs text-gray-700">{maskValue(org.msTenantId)}</td>
                      <td className="py-3 pr-4 font-mono text-xs text-gray-700">{maskValue(org.msClientId)}</td>
                      <td className="py-3 pr-4">
                        <button
                          onClick={() => handleSuperAdminTestConnection(org)}
                          disabled={!hasConfig || testingOrgId === org.id}
                          className="px-3 py-1.5 text-xs font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-gray-300 rounded-lg transition-colors flex items-center gap-1"
                        >
                          {testingOrgId === org.id ? (
                            <Loader2 className="w-3 h-3 animate-spin" />
                          ) : (
                            <RefreshCw className="w-3 h-3" />
                          )}
                          Test Connection
                        </button>
                        {testResult !== undefined && (
                          <div className={`mt-1 text-xs ${testResult ? 'text-green-600' : 'text-red-600'}`}>
                            {testResult ? 'Connection successful' : 'Connection failed'}
                          </div>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    )
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
      </div>
    )
  }

  const isConfigured = config?.msClientId && config?.msTenantId && config?.msMailboxEmail
  const isVerified = config?.msCredentialsVerified

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-header">Microsoft Integration</h1>
        <p className="page-subtitle">
          Configure Microsoft Graph API credentials for email polling
        </p>
      </div>

      {/* Status Banner */}
      {isConfigured ? (
        isVerified ? (
          <div className="flex items-center gap-3 p-4 bg-green-50 border border-green-200 rounded-xl">
            <CheckCircle className="w-5 h-5 text-green-600" />
            <div>
              <p className="font-medium text-green-800">
                Microsoft Graph integration is active
              </p>
              <p className="text-green-600 text-sm">
                Credentials verified on{' '}
                {config?.msCredentialsVerifiedAt
                  ? new Date(config.msCredentialsVerifiedAt).toLocaleString()
                  : 'N/A'}
              </p>
            </div>
          </div>
        ) : (
          <div className="flex items-center gap-3 p-4 bg-yellow-50 border border-yellow-200 rounded-xl">
            <AlertTriangle className="w-5 h-5 text-yellow-600" />
            <div>
              <p className="font-medium text-yellow-800">
                Credentials configured but not verified
              </p>
              <p className="text-yellow-600 text-sm">
                Click &quot;Verify Connection&quot; to test your credentials.
              </p>
            </div>
            <button
              onClick={() => verifyMutation.mutate()}
              disabled={verifyMutation.isPending}
              className="ml-auto px-4 py-2 text-sm font-medium text-white bg-yellow-600 hover:bg-yellow-700 rounded-lg transition-colors flex items-center gap-2"
            >
              {verifyMutation.isPending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              Verify Connection
            </button>
          </div>
        )
      ) : (
        <div className="flex items-center gap-3 p-4 bg-blue-50 border border-blue-200 rounded-xl">
          <Info className="w-5 h-5 text-blue-600" />
          <div>
            <p className="font-medium text-blue-800">
              Microsoft Graph not configured
            </p>
            <p className="text-blue-600 text-sm">
              Set up your Azure AD application credentials to enable email
              invoice polling.
            </p>
          </div>
        </div>
      )}

      {/* Setup Instructions */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-3">
          Setup Instructions
        </h2>
        <div className="bg-gray-50 rounded-lg p-4 text-sm text-gray-600 space-y-2">
          <p>
            To enable email polling, you need to register an application in Azure
            Active Directory:
          </p>
          <ol className="list-decimal list-inside space-y-1 ml-2">
            <li>
              Go to{' '}
              <a
                href="https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade"
                target="_blank"
                rel="noopener noreferrer"
                className="text-primary-600 hover:underline inline-flex items-center gap-1"
              >
                Azure Portal - App Registrations
                <ExternalLink className="w-3 h-3" />
              </a>
            </li>
            <li>Click &quot;New registration&quot; and give it a name (e.g., SyncLedger)</li>
            <li>Set the supported account type to &quot;Single tenant&quot;</li>
            <li>
              Under &quot;API Permissions&quot;, add <code className="bg-gray-200 px-1 rounded">Mail.Read</code>{' '}
              (Application type) and grant admin consent
            </li>
            <li>
              Under &quot;Certificates & secrets&quot;, create a new client secret
            </li>
            <li>Copy the Application (client) ID, Directory (tenant) ID, and client secret below</li>
          </ol>
        </div>
      </div>

      {/* Configuration Form */}
      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">
            Credentials
          </h2>
          {!isEditing && (
            <button
              onClick={handleEdit}
              className="px-4 py-2 text-sm font-medium text-primary-600 bg-primary-50 hover:bg-primary-100 rounded-lg transition-colors"
            >
              {isConfigured ? 'Edit Credentials' : 'Set Up Credentials'}
            </button>
          )}
        </div>

        {isEditing ? (
          <form onSubmit={handleSave} className="space-y-5">
            {updateMutation.isError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                {(updateMutation.error as Error)?.message ||
                  'Failed to update credentials.'}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Key className="w-4 h-4 inline mr-1" />
                Application (Client) ID *
              </label>
              <input
                type="text"
                value={formData.msClientId}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, msClientId: e.target.value }))
                }
                className="w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Shield className="w-4 h-4 inline mr-1" />
                Client Secret *
              </label>
              <div className="relative">
                <input
                  type={showSecret ? 'text' : 'password'}
                  value={formData.msClientSecret}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      msClientSecret: e.target.value,
                    }))
                  }
                  className="w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-12"
                  placeholder="Enter your client secret"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowSecret(!showSecret)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showSecret ? (
                    <EyeOff className="w-5 h-5" />
                  ) : (
                    <Eye className="w-5 h-5" />
                  )}
                </button>
              </div>
              <p className="mt-1 text-xs text-gray-500">
                Stored encrypted with AES-256-GCM. Never displayed in plain text.
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Key className="w-4 h-4 inline mr-1" />
                Directory (Tenant) ID *
              </label>
              <input
                type="text"
                value={formData.msTenantId}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, msTenantId: e.target.value }))
                }
                className="w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                <Mail className="w-4 h-4 inline mr-1" />
                Mailbox Email Address *
              </label>
              <input
                type="email"
                value={formData.msMailboxEmail}
                onChange={(e) =>
                  setFormData((prev) => ({
                    ...prev,
                    msMailboxEmail: e.target.value,
                  }))
                }
                className="w-full px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                placeholder="invoices@yourdomain.com"
                required
              />
              <p className="mt-1 text-xs text-gray-500">
                The mailbox that SyncLedger will poll for invoice emails.
              </p>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                type="button"
                onClick={() => setIsEditing(false)}
                className="px-6 py-2.5 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={updateMutation.isPending}
                className="px-6 py-2.5 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-lg transition-colors flex items-center gap-2"
              >
                {updateMutation.isPending ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                Save Credentials
              </button>
            </div>
          </form>
        ) : (
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">
                  Client ID
                </p>
                <p className="text-sm text-gray-900 font-mono">
                  {config?.msClientId || (
                    <span className="text-gray-400">Not configured</span>
                  )}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">
                  Client Secret
                </p>
                <p className="text-sm text-gray-900 font-mono">
                  {config?.msClientSecretMasked || (
                    <span className="text-gray-400">Not configured</span>
                  )}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">
                  Tenant ID
                </p>
                <p className="text-sm text-gray-900 font-mono">
                  {config?.msTenantId || (
                    <span className="text-gray-400">Not configured</span>
                  )}
                </p>
              </div>
              <div>
                <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-1">
                  Mailbox Email
                </p>
                <p className="text-sm text-gray-900">
                  {config?.msMailboxEmail || (
                    <span className="text-gray-400">Not configured</span>
                  )}
                </p>
              </div>
            </div>

            {isConfigured && (
              <div className="flex items-center gap-3 pt-4 border-t">
                <button
                  onClick={() => verifyMutation.mutate()}
                  disabled={verifyMutation.isPending}
                  className="px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-lg transition-colors flex items-center gap-2"
                >
                  {verifyMutation.isPending ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : (
                    <RefreshCw className="w-4 h-4" />
                  )}
                  Verify Connection
                </button>

                {verifyMutation.isSuccess && (
                  <span className="text-sm text-green-600 flex items-center gap-1">
                    <CheckCircle className="w-4 h-4" />
                    Connection verified successfully!
                  </span>
                )}

                {verifyMutation.isError && (
                  <span className="text-sm text-red-600 flex items-center gap-1">
                    <AlertTriangle className="w-4 h-4" />
                    {(verifyMutation.error as Error)?.message ||
                      'Verification failed'}
                  </span>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
