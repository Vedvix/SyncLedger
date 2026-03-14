import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import { organizationService } from '@/services/organizationService'
import { emailService } from '@/services/emailService'
import {
  erpConfigService,
  microsoftConfigService,
} from '@/services/subscriptionService'
import {
  AlertTriangle,
  CheckCircle,
  Database,
  Eye,
  EyeOff,
  Info,
  Loader2,
  Mail,
  RefreshCw,
  Save,
  Settings2,
} from 'lucide-react'
import type {
  ErpConfig,
  Organization,
  UpdateErpConfigRequest,
  UpdateMicrosoftConfigRequest,
} from '@/types'
import { Link } from 'react-router-dom'

const ERP_TYPES = [
  { value: 'NONE', label: 'None' },
  { value: 'SAGE', label: 'Sage Intacct' },
  { value: 'NETSUITE', label: 'Oracle NetSuite' },
  { value: 'ORACLE', label: 'Oracle Fusion Cloud' },
  { value: 'QUICKBOOKS', label: 'QuickBooks' },
  { value: 'SAP', label: 'SAP S/4HANA' },
  { value: 'XERO', label: 'Xero' },
  { value: 'CUSTOM', label: 'Custom API' },
]

function erpFieldLabels(erpType?: string) {
  switch (erpType) {
    case 'SAGE':
      return {
        endpoint: 'Web Services URL',
        endpointPlaceholder: 'https://api.intacct.com/ia/xml/xmlgw.phtml',
        tenant: 'User ID',
        tenantPlaceholder: 'Your Sage Intacct user ID',
        key: 'Password',
        keyPlaceholder: 'Your Sage Intacct password',
        company: 'Company ID',
      }
    case 'NETSUITE':
      return {
        endpoint: 'Account Endpoint URL',
        endpointPlaceholder: 'https://<accountid>.suitetalk.api.netsuite.com',
        tenant: 'Account ID',
        tenantPlaceholder: 'Your NetSuite account ID',
        key: 'Consumer Secret / Token Secret',
        keyPlaceholder: 'Enter secret token',
        company: 'Subsidiary ID',
      }
    default:
      return {
        endpoint: 'API Endpoint URL',
        endpointPlaceholder: 'https://api.yourerp.com/v1',
        tenant: 'Tenant / Environment ID',
        tenantPlaceholder: 'Tenant or environment identifier',
        key: 'API Key / Secret',
        keyPlaceholder: 'API key or secret',
        company: 'Company ID',
      }
  }
}

export function ConfigurationPage() {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()

  const isSuperAdmin = user?.role === 'SUPER_ADMIN'
  const isOrgAdmin = user?.role === 'ADMIN'
  const isAdmin = isSuperAdmin || isOrgAdmin

  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null)

  const [showMsSecret, setShowMsSecret] = useState(false)
  const [showErpSecret, setShowErpSecret] = useState(false)

  const [msForm, setMsForm] = useState<UpdateMicrosoftConfigRequest>({
    msClientId: '',
    msClientSecret: '',
    msTenantId: '',
    msMailboxEmail: '',
  })

  const [erpForm, setErpForm] = useState<UpdateErpConfigRequest>({
    erpType: 'NONE',
    erpApiEndpoint: '',
    erpApiKey: '',
    erpTenantId: '',
    erpCompanyId: '',
    erpAutoSync: true,
  })

  const [connectionTestResult, setConnectionTestResult] = useState<boolean | null>(null)
  const [testingConnection, setTestingConnection] = useState(false)

  const { data: orgs = [], isLoading: orgsLoading } = useQuery({
    queryKey: ['configuration-orgs'],
    enabled: isSuperAdmin,
    queryFn: async (): Promise<Organization[]> => {
      const response = await organizationService.getOrganizations()
      return response.content
    },
  })

  useEffect(() => {
    if (isSuperAdmin && orgs.length > 0 && selectedOrgId === null) {
      setSelectedOrgId(orgs[0].id)
    }
  }, [isSuperAdmin, orgs, selectedOrgId])

  const selectedOrg = useMemo(
    () => orgs.find((o) => o.id === selectedOrgId),
    [orgs, selectedOrgId]
  )

  const { data: myMsConfig, isLoading: myMsLoading } = useQuery({
    queryKey: ['my-microsoft-config'],
    enabled: isOrgAdmin,
    queryFn: () => microsoftConfigService.getMicrosoftConfig(),
  })

  const { data: myErpConfig, isLoading: myErpLoading } = useQuery({
    queryKey: ['my-erp-config'],
    enabled: isOrgAdmin,
    queryFn: () => erpConfigService.getErpConfig(),
  })

  const { data: superAdminErpConfig, isLoading: superAdminErpLoading } = useQuery({
    queryKey: ['org-erp-config', selectedOrgId],
    enabled: isSuperAdmin && !!selectedOrgId,
    queryFn: () => erpConfigService.getErpConfigForOrg(selectedOrgId as number),
  })

  useEffect(() => {
    if (isOrgAdmin && myMsConfig) {
      setMsForm({
        msClientId: myMsConfig.msClientId || '',
        msClientSecret: '',
        msTenantId: myMsConfig.msTenantId || '',
        msMailboxEmail: myMsConfig.msMailboxEmail || '',
      })
    }

    if (isSuperAdmin && selectedOrg) {
      setMsForm((prev) => ({
        ...prev,
        msClientId: selectedOrg.msClientId || '',
        msClientSecret: '',
        msTenantId: selectedOrg.msTenantId || '',
        msMailboxEmail: selectedOrg.msMailboxEmail || '',
      }))
      setConnectionTestResult(null)
    }
  }, [isOrgAdmin, isSuperAdmin, myMsConfig, selectedOrg])

  useEffect(() => {
    const source: ErpConfig | undefined = isOrgAdmin ? myErpConfig : superAdminErpConfig
    if (!source) return

    setErpForm({
      erpType: source.erpType || 'NONE',
      erpApiEndpoint: source.erpApiEndpoint || '',
      erpApiKey: '',
      erpTenantId: source.erpTenantId || '',
      erpCompanyId: source.erpCompanyId || '',
      erpAutoSync: source.erpAutoSync ?? true,
    })
  }, [isOrgAdmin, myErpConfig, superAdminErpConfig])

  const saveMsMutation = useMutation({
    mutationFn: (data: UpdateMicrosoftConfigRequest) => {
      if (isSuperAdmin && selectedOrgId) {
        return microsoftConfigService.updateMicrosoftConfigForOrg(selectedOrgId, data)
      }
      return microsoftConfigService.updateMicrosoftConfig(data)
    },
    onSuccess: () => {
      if (isOrgAdmin) {
        queryClient.invalidateQueries({ queryKey: ['my-microsoft-config'] })
      } else {
        queryClient.invalidateQueries({ queryKey: ['configuration-orgs'] })
      }
    },
  })

  const verifyMsMutation = useMutation({
    mutationFn: () => microsoftConfigService.verifyMicrosoftConfig(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-microsoft-config'] })
    },
  })

  const saveErpMutation = useMutation({
    mutationFn: (data: UpdateErpConfigRequest) => {
      if (isSuperAdmin && selectedOrgId) {
        return erpConfigService.updateErpConfigForOrg(selectedOrgId, data)
      }
      return erpConfigService.updateErpConfig(data)
    },
    onSuccess: () => {
      if (isOrgAdmin) {
        queryClient.invalidateQueries({ queryKey: ['my-erp-config'] })
      } else {
        queryClient.invalidateQueries({ queryKey: ['org-erp-config', selectedOrgId] })
        queryClient.invalidateQueries({ queryKey: ['configuration-orgs'] })
      }
    },
  })

  const activeErpConfig: ErpConfig | undefined = isOrgAdmin ? myErpConfig : superAdminErpConfig
  const activeMsVerified = isOrgAdmin
    ? myMsConfig?.msCredentialsVerified
    : selectedOrg?.msCredentialsVerified

  const currentOrgName = isOrgAdmin ? user?.organizationName : selectedOrg?.name
  const currentOrgSlug = isOrgAdmin ? user?.organizationSlug : selectedOrg?.slug

  const labels = erpFieldLabels(erpForm.erpType)

  const testConnectionForSelectedOrg = async () => {
    if (!isSuperAdmin || !selectedOrgId) return
    try {
      setTestingConnection(true)
      const ok = await emailService.testEmailConnection(selectedOrgId)
      setConnectionTestResult(ok)
    } finally {
      setTestingConnection(false)
    }
  }

  if (!isAdmin) {
    return (
      <div className="p-6 bg-yellow-50 border border-yellow-200 rounded-xl text-yellow-800">
        <AlertTriangle className="w-5 h-5 inline mr-2" />
        Only admins can access organization configuration.
      </div>
    )
  }

  const pageLoading = isSuperAdmin ? orgsLoading || !selectedOrg : myMsLoading || myErpLoading
  if (pageLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="page-header">Configuration</h1>
          <p className="page-subtitle">
            Manage Microsoft email integration, ERP credentials, and mapping setup from one place.
          </p>
        </div>
        <div className="text-right text-xs text-gray-500">
          <div className="font-medium text-gray-700">Target Organization</div>
          <div>{currentOrgName || '-'}</div>
          {currentOrgSlug && <div>{currentOrgSlug}</div>}
        </div>
      </div>

      {isSuperAdmin && (
        <div className="bg-white rounded-xl shadow-sm p-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Select Organization
          </label>
          <select
            value={selectedOrgId || ''}
            onChange={(e) => setSelectedOrgId(Number(e.target.value))}
            className="w-full max-w-xl px-4 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
          >
            {orgs.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name} ({org.slug})
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2">
              <Mail className="w-5 h-5 text-primary-600" />
              <h2 className="text-lg font-semibold text-gray-900">Microsoft Email Integration</h2>
            </div>
            {activeMsVerified ? (
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-green-100 text-green-800 text-xs">
                <CheckCircle className="w-3 h-3" /> Verified
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-yellow-100 text-yellow-800 text-xs">
                <AlertTriangle className="w-3 h-3" /> Unverified
              </span>
            )}
          </div>

          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              saveMsMutation.mutate(msForm)
            }}
          >
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client ID</label>
              <input
                type="text"
                value={msForm.msClientId}
                onChange={(e) => setMsForm((prev) => ({ ...prev, msClientId: e.target.value }))}
                className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Client Secret</label>
              <div className="relative">
                <input
                  type={showMsSecret ? 'text' : 'password'}
                  value={msForm.msClientSecret}
                  onChange={(e) => setMsForm((prev) => ({ ...prev, msClientSecret: e.target.value }))}
                  className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-12"
                  placeholder="Enter client secret"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowMsSecret((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showMsSecret ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                </button>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Tenant ID</label>
              <input
                type="text"
                value={msForm.msTenantId}
                onChange={(e) => setMsForm((prev) => ({ ...prev, msTenantId: e.target.value }))}
                className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Mailbox Email</label>
              <input
                type="email"
                value={msForm.msMailboxEmail}
                onChange={(e) => setMsForm((prev) => ({ ...prev, msMailboxEmail: e.target.value }))}
                className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                required
              />
            </div>

            <div className="flex flex-wrap gap-2 pt-2">
              <button
                type="submit"
                disabled={saveMsMutation.isPending}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-gray-300 rounded-lg transition-colors"
              >
                {saveMsMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                Save Microsoft Config
              </button>

              {isOrgAdmin && (
                <button
                  type="button"
                  onClick={() => verifyMsMutation.mutate()}
                  disabled={verifyMsMutation.isPending}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-yellow-800 bg-yellow-100 hover:bg-yellow-200 disabled:opacity-60 rounded-lg transition-colors"
                >
                  {verifyMsMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
                  Verify
                </button>
              )}

              {isSuperAdmin && (
                <button
                  type="button"
                  onClick={testConnectionForSelectedOrg}
                  disabled={testingConnection || !selectedOrgId}
                  className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-blue-800 bg-blue-100 hover:bg-blue-200 disabled:opacity-60 rounded-lg transition-colors"
                >
                  {testingConnection ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
                  Test Connection
                </button>
              )}
            </div>

            {connectionTestResult !== null && (
              <div className={`text-sm ${connectionTestResult ? 'text-green-700' : 'text-red-700'}`}>
                {connectionTestResult ? 'Connection successful.' : 'Connection failed.'}
              </div>
            )}

            {saveMsMutation.isError && (
              <div className="text-sm text-red-600">
                {(saveMsMutation.error as Error)?.message || 'Failed to save Microsoft config.'}
              </div>
            )}
          </form>
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2">
              <Database className="w-5 h-5 text-primary-600" />
              <h2 className="text-lg font-semibold text-gray-900">ERP Integration</h2>
            </div>
            <span className={`inline-flex items-center gap-1 px-2 py-1 rounded text-xs ${activeErpConfig?.erpConfigured ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
              {activeErpConfig?.erpConfigured ? <CheckCircle className="w-3 h-3" /> : <Info className="w-3 h-3" />}
              {activeErpConfig?.erpConfigured ? 'Configured' : 'Not Configured'}
            </span>
          </div>

          <form
            className="space-y-4"
            onSubmit={(e) => {
              e.preventDefault()
              saveErpMutation.mutate(erpForm)
            }}
          >
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">ERP System</label>
              <select
                value={erpForm.erpType || 'NONE'}
                onChange={(e) => setErpForm((prev) => ({ ...prev, erpType: e.target.value }))}
                className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white"
              >
                {ERP_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>

            {erpForm.erpType && erpForm.erpType !== 'NONE' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{labels.endpoint}</label>
                  <input
                    type="url"
                    value={erpForm.erpApiEndpoint || ''}
                    onChange={(e) => setErpForm((prev) => ({ ...prev, erpApiEndpoint: e.target.value }))}
                    placeholder={labels.endpointPlaceholder}
                    className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{labels.tenant}</label>
                  <input
                    type="text"
                    value={erpForm.erpTenantId || ''}
                    onChange={(e) => setErpForm((prev) => ({ ...prev, erpTenantId: e.target.value }))}
                    placeholder={labels.tenantPlaceholder}
                    className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{labels.company}</label>
                  <input
                    type="text"
                    value={erpForm.erpCompanyId || ''}
                    onChange={(e) => setErpForm((prev) => ({ ...prev, erpCompanyId: e.target.value }))}
                    className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">{labels.key}</label>
                  <div className="relative">
                    <input
                      type={showErpSecret ? 'text' : 'password'}
                      value={erpForm.erpApiKey || ''}
                      onChange={(e) => setErpForm((prev) => ({ ...prev, erpApiKey: e.target.value }))}
                      placeholder={activeErpConfig?.erpApiKeyMasked ? `Current: ${activeErpConfig.erpApiKeyMasked} (leave blank to keep)` : labels.keyPlaceholder}
                      className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-12"
                    />
                    <button
                      type="button"
                      onClick={() => setShowErpSecret((v) => !v)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      {showErpSecret ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <input
                    id="erp-auto-sync"
                    type="checkbox"
                    checked={erpForm.erpAutoSync ?? true}
                    onChange={(e) => setErpForm((prev) => ({ ...prev, erpAutoSync: e.target.checked }))}
                    className="w-4 h-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
                  />
                  <label htmlFor="erp-auto-sync" className="text-sm text-gray-700">
                    Auto-sync approved invoices to ERP
                  </label>
                </div>
              </>
            )}

            <div className="pt-2">
              <button
                type="submit"
                disabled={saveErpMutation.isPending || (isSuperAdmin && superAdminErpLoading)}
                className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-gray-300 rounded-lg transition-colors"
              >
                {saveErpMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                Save ERP Config
              </button>
            </div>

            {saveErpMutation.isError && (
              <div className="text-sm text-red-600">
                {(saveErpMutation.error as Error)?.message || 'Failed to save ERP config.'}
              </div>
            )}
          </form>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm p-6">
        <div className="flex items-center gap-2 mb-2">
          <Settings2 className="w-5 h-5 text-primary-600" />
          <h3 className="text-base font-semibold text-gray-900">More Configuration</h3>
        </div>
        <p className="text-sm text-gray-600 mb-4">
          Field mapping and billing configuration remain available from dedicated pages.
        </p>
        <div className="flex flex-wrap gap-2">
          <Link
            to="/mapping"
            className="px-3 py-2 text-sm font-medium text-primary-700 bg-primary-50 hover:bg-primary-100 rounded-lg transition-colors"
          >
            Open Field Mapping
          </Link>
          <Link
            to="/subscription"
            className="px-3 py-2 text-sm font-medium text-primary-700 bg-primary-50 hover:bg-primary-100 rounded-lg transition-colors"
          >
            Open Subscription
          </Link>
        </div>
      </div>
    </div>
  )
}
