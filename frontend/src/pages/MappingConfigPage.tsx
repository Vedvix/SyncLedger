import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { mappingService } from '@/services/mappingService'
import type {
  MappingProfile,
  MappingProfileCreateRequest,
  FieldMappingRule,
  DateTransform,
} from '@/types'
import {
  Plus,
  Pencil,
  Trash2,
  Save,
  X,
  ChevronDown,
  ChevronUp,
  Settings2,
  ArrowRight,
  Copy,
  AlertCircle,
} from 'lucide-react'

// ─── Constants ───────────────────────────────────────────────────────────────

const DATE_TRANSFORMS: { value: DateTransform; label: string }[] = [
  { value: 'NONE', label: 'None' },
  { value: 'NEXT_FRIDAY', label: 'Next Friday' },
  { value: 'NEXT_MONDAY', label: 'Next Monday' },
  { value: 'NEXT_BUSINESS_DAY', label: 'Next Business Day' },
  { value: 'END_OF_MONTH', label: 'End of Month' },
  { value: 'ADD_30_DAYS', label: 'Net 30 (Add 30 Days)' },
  { value: 'ADD_60_DAYS', label: 'Net 60 (Add 60 Days)' },
  { value: 'ADD_90_DAYS', label: 'Net 90 (Add 90 Days)' },
  { value: 'NET_30', label: 'Net 30' },
  { value: 'NET_60', label: 'Net 60' },
]

// ─── Component ───────────────────────────────────────────────────────────────

export function MappingConfigPage() {
  const queryClient = useQueryClient()
  const [editingProfile, setEditingProfile] = useState<MappingProfile | null>(null)
  const [isCreating, setIsCreating] = useState(false)
  const [expandedProfile, setExpandedProfile] = useState<string | null>(null)

  // Fetch profiles
  const { data: profiles = [], isLoading, error } = useQuery({
    queryKey: ['mappingProfiles'],
    queryFn: () => mappingService.getProfiles(),
  })

  // Fetch available fields
  const { data: fieldInfo } = useQuery({
    queryKey: ['mappingFields'],
    queryFn: () => mappingService.getAvailableFields(),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => mappingService.deleteProfile(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mappingProfiles'] })
    },
  })

  const handleDelete = (profile: MappingProfile) => {
    if (window.confirm(`Delete profile "${profile.name}"? This cannot be undone.`)) {
      deleteMutation.mutate(profile.id)
    }
  }

  const handleDuplicate = (profile: MappingProfile) => {
    setIsCreating(true)
    setEditingProfile({
      ...profile,
      id: '',
      name: `${profile.name} (Copy)`,
      isDefault: false,
    })
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <AlertCircle className="w-8 h-8 text-red-500 mx-auto mb-2" />
        <p className="text-red-600">Failed to load mapping profiles</p>
        <p className="text-sm text-red-500 mt-1">
          Make sure the PDF microservice is running.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Mapping Profiles</h1>
          <p className="text-gray-500 mt-1">
            Configure how extracted invoice fields are mapped to your system fields.
          </p>
        </div>
        <button
          onClick={() => {
            setIsCreating(true)
            setEditingProfile({
              id: '',
              name: '',
              description: '',
              vendorPattern: '',
              isDefault: false,
              rules: [],
            })
          }}
          className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
        >
          <Plus className="w-5 h-5 mr-2" />
          New Profile
        </button>
      </div>

      {/* Profile Editor */}
      {(isCreating || editingProfile) && (
        <ProfileEditor
          profile={editingProfile!}
          isNew={isCreating}
          sourceFields={fieldInfo?.sourceFields || []}
          targetFields={fieldInfo?.targetFields || []}
          onSave={() => {
            setEditingProfile(null)
            setIsCreating(false)
            queryClient.invalidateQueries({ queryKey: ['mappingProfiles'] })
          }}
          onCancel={() => {
            setEditingProfile(null)
            setIsCreating(false)
          }}
        />
      )}

      {/* Profile List */}
      <div className="space-y-4">
        {profiles.length === 0 && !isCreating && (
          <div className="bg-white rounded-xl shadow-sm p-12 text-center">
            <Settings2 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-600">No mapping profiles yet</h3>
            <p className="text-gray-400 mt-1">
              Create your first profile to configure field mappings.
            </p>
          </div>
        )}

        {profiles.map((profile) => (
          <div key={profile.id} className="bg-white rounded-xl shadow-sm overflow-hidden border border-gray-200">
            {/* Profile Header */}
            <div
              className="flex items-center justify-between px-6 py-4 cursor-pointer hover:bg-blue-50 transition-colors border-b border-gray-200"
              onClick={() =>
                setExpandedProfile(expandedProfile === profile.id ? null : profile.id)
              }
            >
              <div className="flex items-center gap-4 flex-1">
                <Settings2 className="w-5 h-5 text-primary-500 flex-shrink-0" />
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-semibold text-gray-900">{profile.name}</h3>
                    {profile.isDefault && (
                      <span className="px-2 py-0.5 bg-primary-100 text-primary-700 text-xs rounded-full font-medium">
                        Default
                      </span>
                    )}
                    {profile.rules.length === 0 && (
                      <span className="px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs rounded-full font-medium">
                        No rules configured
                      </span>
                    )}
                  </div>
                  {profile.description && (
                    <p className="text-sm text-gray-500 mt-0.5">{profile.description}</p>
                  )}
                </div>
              </div>
              <div className="flex items-center gap-4 flex-shrink-0">
                {profile.vendorPattern && (
                  <span className="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                    Pattern: {profile.vendorPattern}
                  </span>
                )}
                <div className="text-right">
                  <span className={`text-sm font-semibold ${profile.rules.length === 0 ? 'text-yellow-600' : 'text-green-600'}`}>
                    {profile.rules.length} rule{profile.rules.length !== 1 ? 's' : ''}
                  </span>
                  <p className="text-xs text-gray-400">configured</p>
                </div>
                {expandedProfile === profile.id ? (
                  <ChevronUp className="w-5 h-5 text-gray-400" />
                ) : (
                  <ChevronDown className="w-5 h-5 text-gray-400" />
                )}
              </div>
            </div>

            {/* Expanded Details */}
            {expandedProfile === profile.id && (
              <div className="border-t px-6 py-4 bg-gray-50">
                {/* Rules Table or Empty State */}
                {profile.rules.length > 0 ? (
                  <div className="overflow-x-auto mb-4">
                    <table className="w-full text-sm">
                      <thead className="bg-white border-b-2 border-gray-300">
                        <tr>
                          <th className="text-left py-3 px-3 text-gray-700 font-semibold">#</th>
                          <th className="text-left py-3 px-3 text-gray-700 font-semibold">Source Field</th>
                          <th className="text-center py-3 px-3 text-gray-700 font-semibold w-8"></th>
                          <th className="text-left py-3 px-3 text-gray-700 font-semibold">Target Field</th>
                          <th className="text-left py-3 px-3 text-gray-700 font-semibold">Fallback</th>
                          <th className="text-left py-3 px-3 text-gray-700 font-semibold">Default Value</th>
                          <th className="text-left py-3 px-3 text-gray-700 font-semibold">Transform</th>
                        </tr>
                      </thead>
                      <tbody>
                        {profile.rules.map((rule, idx) => (
                          <tr key={idx} className="border-b bg-white hover:bg-blue-50 transition-colors">
                            <td className="py-3 px-3 text-gray-500 font-medium text-xs">{idx + 1}</td>
                            <td className="py-3 px-3 font-mono text-xs bg-gray-100 text-gray-800 rounded">
                              {rule.source}
                            </td>
                            <td className="py-3 px-3 text-center">
                              <ArrowRight className="w-4 h-4 text-primary-500 inline" />
                            </td>
                            <td className="py-3 px-3 font-mono text-xs bg-primary-50 text-primary-800 rounded font-semibold">
                              {rule.target}
                            </td>
                            <td className="py-3 px-3 font-mono text-xs">
                              {rule.fallbackSource ? (
                                <span className="bg-orange-100 text-orange-700 px-2 py-1 rounded">
                                  {rule.fallbackSource}
                                </span>
                              ) : (
                                <span className="text-gray-400">-</span>
                              )}
                            </td>
                            <td className="py-3 px-3 text-xs">
                              {rule.defaultValue ? (
                                <span className="bg-green-100 text-green-700 px-2 py-1 rounded">
                                  {rule.defaultValue}
                                </span>
                              ) : (
                                <span className="text-gray-400">-</span>
                              )}
                            </td>
                            <td className="py-3 px-3 text-xs">
                              {rule.dateTransform && rule.dateTransform !== 'NONE' ? (
                                <span className="bg-blue-100 text-blue-700 px-2 py-1 rounded font-medium">
                                  {rule.dateTransform}
                                </span>
                              ) : (
                                <span className="text-gray-400">No transform</span>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="bg-white rounded-lg p-8 text-center border-2 border-dashed border-yellow-200">
                    <AlertCircle className="w-12 h-12 text-yellow-400 mx-auto mb-3" />
                    <h4 className="font-semibold text-gray-900 mb-1">No mapping rules configured</h4>
                    <p className="text-sm text-gray-600 mb-4">
                      Add field mapping rules to define how PDF invoice fields are transformed for your system.
                    </p>
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        setIsCreating(false)
                        setEditingProfile(profile)
                      }}
                      className="inline-flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 text-sm font-medium"
                    >
                      <Plus className="w-4 h-4 mr-2" />
                      Add Rules
                    </button>
                  </div>
                )}

                {/* Actions */}
                <div className="flex gap-2 justify-end">
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      handleDuplicate(profile)
                    }}
                    className="flex items-center px-3 py-1.5 text-sm text-gray-600 border rounded-lg hover:bg-gray-50"
                  >
                    <Copy className="w-4 h-4 mr-1" />
                    Duplicate
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      setIsCreating(false)
                      setEditingProfile(profile)
                    }}
                    className="flex items-center px-3 py-1.5 text-sm text-primary-600 border border-primary-200 rounded-lg hover:bg-primary-50"
                  >
                    <Pencil className="w-4 h-4 mr-1" />
                    Edit
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      handleDelete(profile)
                    }}
                    disabled={deleteMutation.isPending}
                    className="flex items-center px-3 py-1.5 text-sm text-red-600 border border-red-200 rounded-lg hover:bg-red-50 disabled:opacity-50"
                  >
                    <Trash2 className="w-4 h-4 mr-1" />
                    Delete
                  </button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}

// ─── Profile Editor Component ────────────────────────────────────────────────

function ProfileEditor({
  profile,
  isNew,
  sourceFields,
  targetFields,
  onSave,
  onCancel,
}: {
  profile: MappingProfile
  isNew: boolean
  sourceFields: string[]
  targetFields: string[]
  onSave: () => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<MappingProfileCreateRequest>({
    id: profile.id || undefined,
    name: profile.name,
    description: profile.description || '',
    vendorPattern: profile.vendorPattern || '',
    isDefault: profile.isDefault,
    rules: profile.rules.length > 0 ? [...profile.rules] : [],
  })
  const [error, setError] = useState<string | null>(null)

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (isNew) {
        return mappingService.createProfile(form)
      } else {
        return mappingService.updateProfile(profile.id, {
          name: form.name,
          description: form.description,
          vendorPattern: form.vendorPattern,
          isDefault: form.isDefault,
          rules: form.rules,
        })
      }
    },
    onSuccess: () => onSave(),
    onError: (err: Error) => setError(err.message),
  })

  const addRule = () => {
    setForm((prev) => ({
      ...prev,
      rules: [
        ...prev.rules,
        {
          source: sourceFields[0] || '',
          target: targetFields[0] || '',
          fallbackSource: '',
          defaultValue: '',
          dateTransform: 'NONE' as DateTransform,
        },
      ],
    }))
  }

  const updateRule = (index: number, updates: Partial<FieldMappingRule>) => {
    setForm((prev) => ({
      ...prev,
      rules: prev.rules.map((r, i) => (i === index ? { ...r, ...updates } : r)),
    }))
  }

  const removeRule = (index: number) => {
    setForm((prev) => ({
      ...prev,
      rules: prev.rules.filter((_, i) => i !== index),
    }))
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border-2 border-primary-200 overflow-hidden">
      <div className="px-6 py-4 bg-primary-50 border-b border-primary-200">
        <h2 className="text-lg font-semibold text-primary-900">
          {isNew ? 'Create New Profile' : `Edit: ${profile.name}`}
        </h2>
      </div>

      <div className="px-6 py-5 space-y-5">
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3 text-sm text-red-600">
            {error}
          </div>
        )}

        {/* Profile Basics */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Profile Name *</label>
            <input
              type="text"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              placeholder="e.g., Subcontractor Invoices"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Vendor Pattern (regex)
            </label>
            <input
              type="text"
              value={form.vendorPattern || ''}
              onChange={(e) => setForm((f) => ({ ...f, vendorPattern: e.target.value }))}
              className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              placeholder="e.g., .*construction.*"
            />
          </div>
          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <input
              type="text"
              value={form.description || ''}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              className="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              placeholder="What this profile is for..."
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="isDefault"
              checked={form.isDefault}
              onChange={(e) => setForm((f) => ({ ...f, isDefault: e.target.checked }))}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            <label htmlFor="isDefault" className="text-sm text-gray-700">
              Set as default profile
            </label>
          </div>
        </div>

        {/* Mapping Rules */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold text-gray-900">Mapping Rules</h3>
              <p className="text-xs text-gray-500 mt-0.5">
                {form.rules.length} rule{form.rules.length !== 1 ? 's' : ''} defined
              </p>
            </div>
            <button
              onClick={addRule}
              className="flex items-center px-3 py-2 text-sm text-white bg-primary-600 rounded-lg hover:bg-primary-700 font-medium transition-colors"
            >
              <Plus className="w-4 h-4 mr-1" />
              Add Rule
            </button>
          </div>

          {form.rules.length === 0 ? (
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center bg-gray-50">
              <AlertCircle className="w-8 h-8 text-gray-400 mx-auto mb-2" />
              <p className="text-sm text-gray-600 font-medium">No rules defined yet</p>
              <p className="text-xs text-gray-500 mt-1">Add your first mapping rule to get started</p>
            </div>
          ) : (
            <>
              {/* Rules Preview Table */}
              <div className="bg-gray-50 rounded-lg overflow-hidden border border-gray-200 mb-3">
                <table className="w-full text-xs">
                  <thead className="bg-white border-b border-gray-300">
                    <tr>
                      <th className="text-left py-2 px-3 text-gray-700 font-semibold">#</th>
                      <th className="text-left py-2 px-3 text-gray-700 font-semibold">Source</th>
                      <th className="text-center py-2 px-1 w-6"></th>
                      <th className="text-left py-2 px-3 text-gray-700 font-semibold">Target</th>
                      <th className="text-left py-2 px-3 text-gray-700 font-semibold">Fallback</th>
                      <th className="text-left py-2 px-3 text-gray-700 font-semibold">Default</th>
                      <th className="text-left py-2 px-3 text-gray-700 font-semibold">Transform</th>
                    </tr>
                  </thead>
                  <tbody>
                    {form.rules.map((rule, idx) => (
                      <tr key={idx} className="border-b border-gray-200 bg-white hover:bg-blue-50">
                        <td className="py-2 px-3 text-gray-600">{idx + 1}</td>
                        <td className="py-2 px-3 font-mono text-gray-700">{rule.source || '—'}</td>
                        <td className="py-2 px-1 text-center"><ArrowRight className="w-3 h-3 text-gray-400 inline" /></td>
                        <td className="py-2 px-3 font-mono text-primary-700 font-medium">{rule.target || '—'}</td>
                        <td className="py-2 px-3 text-gray-600 text-xs">{rule.fallbackSource || '—'}</td>
                        <td className="py-2 px-3 text-gray-600 text-xs">{rule.defaultValue || '—'}</td>
                        <td className="py-2 px-3 text-xs"><span className={rule.dateTransform && rule.dateTransform !== 'NONE' ? 'text-blue-600 font-medium' : 'text-gray-400'}>{rule.dateTransform && rule.dateTransform !== 'NONE' ? rule.dateTransform : '—'}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Rule Editors */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-3">
                <h4 className="text-sm font-semibold text-blue-900 mb-3">Edit Rules</h4>
                <div className="space-y-3">
                  {form.rules.map((rule, idx) => (
                <div
                  key={idx}
                  className="bg-white border border-gray-200 rounded-lg p-4 hover:border-primary-300 transition-colors"
                >
                  <div className="flex items-center justify-between mb-3">
                    <h5 className="font-medium text-gray-900">Rule #{idx + 1}</h5>
                    <button
                      onClick={() => removeRule(idx)}
                      className="p-1 text-red-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
                      title="Remove rule"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* Source */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-700 mb-2">Source Field *</label>
                      <select
                        value={rule.source}
                        onChange={(e) => updateRule(idx, { source: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white hover:border-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
                      >
                        <option value="">Select source field...</option>
                        {sourceFields.map((f) => (
                          <option key={f} value={f}>
                            {f}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Target */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-700 mb-2">Target Field *</label>
                      <select
                        value={rule.target}
                        onChange={(e) => updateRule(idx, { target: e.target.value })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white hover:border-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
                      >
                        <option value="">Select target field...</option>
                        {targetFields.map((f) => (
                          <option key={f} value={f}>
                            {f}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Date Transform */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-700 mb-2">Date Transform</label>
                      <select
                        value={rule.dateTransform || 'NONE'}
                        onChange={(e) =>
                          updateRule(idx, { dateTransform: e.target.value as DateTransform })
                        }
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white hover:border-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
                      >
                        {DATE_TRANSFORMS.map((t) => (
                          <option key={t.value} value={t.value}>
                            {t.label}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Fallback */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-700 mb-2">Fallback Source</label>
                      <select
                        value={rule.fallbackSource || ''}
                        onChange={(e) =>
                          updateRule(idx, { fallbackSource: e.target.value || undefined })
                        }
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white hover:border-gray-400 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
                      >
                        <option value="">None</option>
                        {sourceFields.map((f) => (
                          <option key={f} value={f}>
                            {f}
                          </option>
                        ))}
                      </select>
                    </div>

                    {/* Default Value */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-700 mb-2">Default Value</label>
                      <input
                        type="text"
                        value={rule.defaultValue || ''}
                        onChange={(e) =>
                          updateRule(idx, { defaultValue: e.target.value || undefined })
                        }
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
                        placeholder="e.g., Invoice"
                      />
                    </div>

                    {/* Description */}
                    <div>
                      <label className="block text-xs font-semibold text-gray-700 mb-2">Description</label>
                      <input
                        type="text"
                        value={rule.description || ''}
                        onChange={(e) =>
                          updateRule(idx, { description: e.target.value || undefined })
                        }
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors"
                        placeholder="Optional note about this mapping"
                      />
                    </div>
                  </div>
                </div>
              ))}
                </div>
              </div>
            </>
          )}
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-4 border-t">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-gray-600 border rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onClick={() => saveMutation.mutate()}
            disabled={saveMutation.isPending || !form.name.trim()}
            className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            <Save className="w-4 h-4 mr-2" />
            {saveMutation.isPending ? 'Saving...' : isNew ? 'Create Profile' : 'Save Changes'}
          </button>
        </div>
      </div>
    </div>
  )
}
