import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { erpConfigService } from '@/services/subscriptionService'
import { organizationService } from '@/services/organizationService'
import { useAuthStore } from '@/store/authStore'
import {
  Database,
  Key,
  Eye,
  EyeOff,
  CheckCircle,
  AlertTriangle,
  Loader2,
  Save,
  Info,
  X,
  Edit2,
  RefreshCw,
} from 'lucide-react'
import type { Organization, UpdateErpConfigRequest } from '@/types'

// ─── ERP type metadata ────────────────────────────────────────────────────────

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

/**
 * Return contextual field labels so the form makes sense for each ERP system.
 * Sage Intacct uses User ID / Password terminology; others use tenant / API key.
 */
function erpFieldLabels(erpType: string) {
  switch (erpType) {
    case 'SAGE':
      return {
        endpoint: 'Web Services URL',
        endpointPlaceholder: 'https://api.intacct.com/ia/xml/xmlgw.phtml',
        tenantId: 'User ID',
        tenantIdPlaceholder: 'Your Sage Intacct user ID',
        apiKey: 'Password',
        apiKeyPlaceholder: 'Your Sage Intacct password',
        companyId: 'Company ID',
        companyIdPlaceholder: 'Your Sage company ID (e.g. longroofing)',
        apiKeyHint: 'Stored encrypted with AES-256-GCM.',
      }
    case 'NETSUITE':
      return {
        endpoint: 'Account Endpoint URL',
        endpointPlaceholder: 'https://<accountid>.suitetalk.api.netsuite.com',
        tenantId: 'Account ID',
        tenantIdPlaceholder: 'Your NetSuite account ID',
        apiKey: 'Consumer Secret / Token Secret',
        apiKeyPlaceholder: 'Token secret or consumer secret',
        companyId: 'Subsidiary ID',
        companyIdPlaceholder: 'Internal ID of the subsidiary (optional)',
        apiKeyHint: 'Stored encrypted with AES-256-GCM.',
      }
    case 'QUICKBOOKS':
      return {
        endpoint: 'Base URL',
        endpointPlaceholder: 'https://quickbooks.api.intuit.com',
        tenantId: 'Realm ID (Company ID)',
        tenantIdPlaceholder: 'Your QuickBooks realm ID',
        apiKey: 'Client Secret',
        apiKeyPlaceholder: 'OAuth 2.0 client secret',
        companyId: 'Company Name',
        companyIdPlaceholder: 'Display name for reference (optional)',
        apiKeyHint: 'Stored encrypted with AES-256-GCM.',
      }
    default:
      return {
        endpoint: 'API Endpoint URL',
        endpointPlaceholder: 'https://api.yourerp.com/v1',
        tenantId: 'Tenant / Environment ID',
        tenantIdPlaceholder: 'Tenant or environment identifier',
        apiKey: 'API Key / Secret',
        apiKeyPlaceholder: 'API key or secret token',
        companyId: 'Company ID',
        companyIdPlaceholder: 'Company identifier within the ERP',
        apiKeyHint: 'Stored encrypted with AES-256-GCM.',
      }
  }
}

// ─── Shared form component ────────────────────────────────────────────────────

interface ErpFormProps {
  initial: UpdateErpConfigRequest
  maskedApiKey?: string
  onSave: (data: UpdateErpConfigRequest) => void
  isSaving: boolean
  saveError?: string | null
  onCancel?: () => void
  compact?: boolean
}

function ErpForm({ initial, maskedApiKey, onSave, isSaving, saveError, onCancel, compact }: ErpFormProps) {
  const [form, setForm] = useState<UpdateErpConfigRequest>(initial)
  const [showKey, setShowKey] = useState(false)
  const labels = erpFieldLabels(form.erpType || 'NONE')

  const set = (patch: Partial<UpdateErpConfigRequest>) =>
    setForm((prev) => ({ ...prev, ...patch }))

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSave(form)
  }

  return (
    <form onSubmit={handleSubmit} className={compact ? 'space-y-4' : 'space-y-5'}>
      {saveError && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {saveError}
        </div>
      )}

      {/* ERP Type */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          ERP System
        </label>
        <select
          value={form.erpType || 'NONE'}
          onChange={(e) => set({ erpType: e.target.value })}
          className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 bg-white text-sm"
        >
          {ERP_TYPES.map((t) => (
            <option key={t.value} value={t.value}>{t.label}</option>
          ))}
        </select>
      </div>

      {form.erpType && form.erpType !== 'NONE' && (
        <>
          {/* Endpoint */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {labels.endpoint}
            </label>
            <input
              type="url"
              value={form.erpApiEndpoint || ''}
              onChange={(e) => set({ erpApiEndpoint: e.target.value })}
              className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              placeholder={labels.endpointPlaceholder}
            />
          </div>

          {/* Tenant / User ID */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {labels.tenantId}
            </label>
            <input
              type="text"
              value={form.erpTenantId || ''}
              onChange={(e) => set({ erpTenantId: e.target.value })}
              className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              placeholder={labels.tenantIdPlaceholder}
            />
          </div>

          {/* Company ID */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {labels.companyId}
            </label>
            <input
              type="text"
              value={form.erpCompanyId || ''}
              onChange={(e) => set({ erpCompanyId: e.target.value })}
              className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 text-sm"
              placeholder={labels.companyIdPlaceholder}
            />
          </div>

          {/* API Key / Password */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Key className="w-4 h-4 inline mr-1" />
              {labels.apiKey}
            </label>
            <div className="relative">
              <input
                type={showKey ? 'text' : 'password'}
                value={form.erpApiKey || ''}
                onChange={(e) => set({ erpApiKey: e.target.value })}
                className="w-full px-3 py-2.5 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-12 text-sm"
                placeholder={maskedApiKey ? `Current: ${maskedApiKey} (leave blank to keep)` : labels.apiKeyPlaceholder}
                autoComplete="new-password"
              />
              <button
                type="button"
                onClick={() => setShowKey(!showKey)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            <p className="mt-1 text-xs text-gray-500">{labels.apiKeyHint}</p>
          </div>

          {/* Auto Sync */}
          <div className="flex items-center gap-3">
            <input
              type="checkbox"
              id="erp-auto-sync"
              checked={form.erpAutoSync ?? true}
              onChange={(e) => set({ erpAutoSync: e.target.checked })}
              className="w-4 h-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
            />
            <label htmlFor="erp-auto-sync" className="text-sm text-gray-700">
              Auto-sync approved invoices to ERP
            </label>
          </div>
        </>
      )}

      {/* Actions */}
      <div className={`flex gap-3 ${compact ? 'pt-1' : 'pt-2'}`}>
        <button
          type="submit"
          disabled={isSaving}
          className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary-600 hover:bg-primary-700 disabled:bg-gray-300 rounded-lg transition-colors"
        >
          {isSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
          {isSaving ? 'Saving…' : 'Save Configuration'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  )
}

// ─── Super Admin modal ────────────────────────────────────────────────────────

interface OrgErpModalProps {
  org: Organization
  onClose: () => void
}

function OrgErpModal({ org, onClose }: OrgErpModalProps) {
  const queryClient = useQueryClient()

  const { data: config, isLoading } = useQuery({
    queryKey: ['super-admin-erp-config', org.id],
    queryFn: () => erpConfigService.getErpConfigForOrg(org.id),
  })

  const mutation = useMutation({
    mutationFn: (data: UpdateErpConfigRequest) =>
      erpConfigService.updateErpConfigForOrg(org.id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['super-admin-erp-config', org.id] })
      queryClient.invalidateQueries({ queryKey: ['super-admin-orgs-erp-config'] })
    },
  })

  const initialForm: UpdateErpConfigRequest = {
    erpType: config?.erpType || 'NONE',
    erpApiEndpoint: config?.erpApiEndpoint || '',
    erpTenantId: config?.erpTenantId || '',
    erpCompanyId: config?.erpCompanyId || '',
    erpApiKey: '',
    erpAutoSync: config?.erpAutoSync ?? true,
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between p-5 border-b">
          <div>
            <h3 className="text-base font-semibold text-gray-900">
              ERP Configuration — {org.name}
            </h3>
            <p className="text-xs text-gray-500 mt-0.5">{org.slug}</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5">
          {isLoading ? (
            <div className="flex items-center justify-center h-32">
              <Loader2 className="w-6 h-6 animate-spin text-primary-600" />
            </div>
          ) : (
            <>
              {mutation.isSuccess && (
                <div className="flex items-center gap-2 mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">
                  <CheckCircle className="w-4 h-4" />
                  ERP configuration saved successfully.
                </div>
              )}
              <ErpForm
                key={config?.erpType}
                initial={initialForm}
                maskedApiKey={config?.erpApiKeyMasked || undefined}
                onSave={mutation.mutate}
                isSaving={mutation.isPending}
                saveError={mutation.isError ? (mutation.error as Error)?.message : null}
                onCancel={onClose}
                compact
              />
            </>
          )}
        </div>
      </div>
    </div>
  )
}

// ─── Main page ────────────────────────────────────────────────────────────────

export function ErpConfigPage() {
  const { user } = useAuthStore()
  const queryClient = useQueryClient()
  const isSuperAdmin = user?.role === 'SUPER_ADMIN'
  const isAdmin = user?.role === 'ADMIN' || isSuperAdmin
  const isOrgAdmin = user?.role === 'ADMIN'

  const [isEditing, setIsEditing] = useState(false)
  const [editingOrg, setEditingOrg] = useState<Organization | null>(null)

  // Org admin: fetch own ERP config
  const { data: config, isLoading } = useQuery({
    queryKey: ['erp-config'],
    queryFn: () => erpConfigService.getErpConfig(),
    enabled: isOrgAdmin,
  })

  // Super admin: fetch all orgs
  const { data: superAdminOrgs = [], isLoading: orgsLoading } = useQuery({
    queryKey: ['super-admin-orgs-erp-config'],
    queryFn: async (): Promise<Organization[]> => {
      const response = await organizationService.getOrganizations()
      return response.content
    },
    enabled: isSuperAdmin,
  })

  // Org admin: update mutation
  const updateMutation = useMutation({
    mutationFn: (data: UpdateErpConfigRequest) => erpConfigService.updateErpConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['erp-config'] })
      setIsEditing(false)
    },
  })

  if (!isAdmin) {
    return (
      <div className="p-6 bg-yellow-50 border border-yellow-200 rounded-xl text-yellow-800">
        <AlertTriangle className="w-5 h-5 inline mr-2" />
        Only admins can manage ERP integration settings.
      </div>
    )
  }

  // ── Super Admin view ──────────────────────────────────────────────────────────
  if (isSuperAdmin) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="page-header">Organization ERP Configurations</h1>
          <p className="page-subtitle">
            View and manage ERP integration credentials for all organizations
          </p>
        </div>

        <div className="bg-white rounded-xl shadow-sm p-6 overflow-x-auto">
          {orgsLoading ? (
            <div className="flex items-center justify-center h-32">
              <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left">
                  <th className="py-3 pr-4 font-medium text-gray-600">Organization</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">ERP System</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Status</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Endpoint</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Company ID</th>
                  <th className="py-3 pr-4 font-medium text-gray-600">Auto Sync</th>
                  <th className="py-3 font-medium text-gray-600">Action</th>
                </tr>
              </thead>
              <tbody>
                {superAdminOrgs.map((org) => {
                  const erpLabel =
                    ERP_TYPES.find((t) => t.value === org.erpType)?.label || org.erpType || 'None'
                  const configured = org.erpConfigured
                  return (
                    <tr key={org.id} className="border-b last:border-b-0">
                      <td className="py-3 pr-4">
                        <div className="font-medium text-gray-900">{org.name}</div>
                        <div className="text-xs text-gray-500">{org.slug}</div>
                      </td>
                      <td className="py-3 pr-4 text-gray-700">{erpLabel}</td>
                      <td className="py-3 pr-4">
                        {configured ? (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-green-100 text-green-800 text-xs">
                            <CheckCircle className="w-3 h-3" /> Configured
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-gray-100 text-gray-600 text-xs">
                            <Info className="w-3 h-3" /> Not Configured
                          </span>
                        )}
                      </td>
                      <td className="py-3 pr-4 text-xs text-gray-600 max-w-[160px] truncate">
                        {org.erpApiEndpoint || '—'}
                      </td>
                      <td className="py-3 pr-4 text-gray-700">
                        {org.erpCompanyId || '—'}
                      </td>
                      <td className="py-3 pr-4">
                        {org.erpAutoSync ? (
                          <span className="text-green-700 text-xs">On</span>
                        ) : (
                          <span className="text-gray-500 text-xs">Off</span>
                        )}
                      </td>
                      <td className="py-3">
                        <button
                          onClick={() => setEditingOrg(org)}
                          className="px-3 py-1.5 text-xs font-medium text-white bg-primary-600 hover:bg-primary-700 rounded-lg transition-colors flex items-center gap-1"
                        >
                          <Edit2 className="w-3 h-3" />
                          {configured ? 'Edit' : 'Configure'}
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>

        {editingOrg && (
          <OrgErpModal
            org={editingOrg}
            onClose={() => setEditingOrg(null)}
          />
        )}
      </div>
    )
  }

  // ── Org Admin view ────────────────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary-600" />
      </div>
    )
  }

  const isConfigured = config?.erpConfigured
  const erpLabel = ERP_TYPES.find((t) => t.value === config?.erpType)?.label || config?.erpType || 'None'

  const initialForm: UpdateErpConfigRequest = {
    erpType: config?.erpType || 'NONE',
    erpApiEndpoint: config?.erpApiEndpoint || '',
    erpTenantId: config?.erpTenantId || '',
    erpCompanyId: config?.erpCompanyId || '',
    erpApiKey: '',
    erpAutoSync: config?.erpAutoSync ?? true,
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-header">ERP Integration</h1>
        <p className="page-subtitle">
          Configure credentials to sync approved invoices directly to your ERP system
        </p>
      </div>

      {/* Status banner */}
      {isConfigured ? (
        <div className="flex items-center gap-3 p-4 bg-green-50 border border-green-200 rounded-xl">
          <CheckCircle className="w-5 h-5 text-green-600 shrink-0" />
          <div>
            <p className="font-medium text-green-800">ERP integration is active</p>
            <p className="text-green-600 text-sm">
              Connected to <strong>{erpLabel}</strong>
              {config?.erpCompanyId ? ` — Company: ${config.erpCompanyId}` : ''}
              {config?.erpAutoSync ? ' · Auto-sync enabled' : ' · Auto-sync disabled'}
            </p>
          </div>
          {!isEditing && (
            <button
              onClick={() => setIsEditing(true)}
              className="ml-auto flex items-center gap-2 px-4 py-2 text-sm font-medium text-green-700 bg-green-100 hover:bg-green-200 rounded-lg transition-colors"
            >
              <Edit2 className="w-4 h-4" />
              Edit Credentials
            </button>
          )}
        </div>
      ) : (
        <div className="flex items-center gap-3 p-4 bg-blue-50 border border-blue-200 rounded-xl">
          <Info className="w-5 h-5 text-blue-600 shrink-0" />
          <div>
            <p className="font-medium text-blue-800">ERP integration not configured</p>
            <p className="text-blue-600 text-sm">
              Set up your ERP credentials to enable automatic invoice syncing.
            </p>
          </div>
          {!isEditing && (
            <button
              onClick={() => setIsEditing(true)}
              className="ml-auto flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
              Set Up Integration
            </button>
          )}
        </div>
      )}

      {/* Current config summary (read-only) */}
      {isConfigured && !isEditing && (
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-4">Current Configuration</h2>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-4 text-sm">
            <div>
              <dt className="text-gray-500 mb-0.5">ERP System</dt>
              <dd className="font-medium text-gray-900">{erpLabel}</dd>
            </div>
            {config?.erpApiEndpoint && (
              <div>
                <dt className="text-gray-500 mb-0.5">Endpoint</dt>
                <dd className="font-medium text-gray-900 break-all">{config.erpApiEndpoint}</dd>
              </div>
            )}
            {config?.erpTenantId && (
              <div>
                <dt className="text-gray-500 mb-0.5">{erpFieldLabels(config.erpType || '').tenantId}</dt>
                <dd className="font-medium text-gray-900">{config.erpTenantId}</dd>
              </div>
            )}
            {config?.erpCompanyId && (
              <div>
                <dt className="text-gray-500 mb-0.5">Company ID</dt>
                <dd className="font-medium text-gray-900">{config.erpCompanyId}</dd>
              </div>
            )}
            {config?.erpApiKeyMasked && (
              <div>
                <dt className="text-gray-500 mb-0.5">{erpFieldLabels(config.erpType || '').apiKey}</dt>
                <dd className="font-mono text-gray-900">{config.erpApiKeyMasked}</dd>
              </div>
            )}
            <div>
              <dt className="text-gray-500 mb-0.5">Auto Sync</dt>
              <dd className="font-medium text-gray-900">
                {config?.erpAutoSync ? 'Enabled' : 'Disabled'}
              </dd>
            </div>
          </dl>
        </div>
      )}

      {/* Edit / setup form */}
      {isEditing && (
        <div className="bg-white rounded-xl shadow-sm p-6">
          <div className="flex items-center gap-2 mb-6">
            <Database className="w-5 h-5 text-primary-600" />
            <h2 className="text-base font-semibold text-gray-900">
              {isConfigured ? 'Update ERP Credentials' : 'Configure ERP Integration'}
            </h2>
          </div>
          {updateMutation.isSuccess && (
            <div className="flex items-center gap-2 mb-4 p-3 bg-green-50 border border-green-200 rounded-lg text-green-700 text-sm">
              <CheckCircle className="w-4 h-4" />
              ERP configuration saved successfully.
            </div>
          )}
          <ErpForm
            key={isEditing ? 'editing' : 'idle'}
            initial={initialForm}
            maskedApiKey={config?.erpApiKeyMasked || undefined}
            onSave={updateMutation.mutate}
            isSaving={updateMutation.isPending}
            saveError={updateMutation.isError ? (updateMutation.error as Error)?.message : null}
            onCancel={() => setIsEditing(false)}
          />
        </div>
      )}

      {/* Help section */}
      {!isEditing && (
        <div className="bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-base font-semibold text-gray-900 mb-3">Supported ERP Systems</h2>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-sm text-gray-600">
            {ERP_TYPES.filter((t) => t.value !== 'NONE').map((t) => (
              <div key={t.value} className="flex items-center gap-2 p-3 bg-gray-50 rounded-lg">
                <Database className="w-4 h-4 text-primary-400 shrink-0" />
                {t.label}
              </div>
            ))}
          </div>
          <p className="mt-4 text-xs text-gray-500">
            All API keys and passwords are encrypted at rest using AES-256-GCM.
            Credentials are never stored in plain text or exposed in logs.
          </p>
        </div>
      )}
    </div>
  )
}
