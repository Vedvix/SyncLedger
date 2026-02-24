import axios from 'axios'
import { useAuthStore } from '@/store/authStore'
import type {
  MappingProfile,
  MappingProfileCreateRequest,
  MappingProfileUpdateRequest,
  MappingFieldInfo,
  FieldMappingRule,
} from '@/types'

/**
 * Axios client for PDF Microservice (mapping profiles).
 * In Docker, nginx proxies /pdf-api/ → pdf-service:8001/
 * In local dev, override with VITE_PDF_API_URL env var.
 */
const PDF_API_BASE = import.meta.env.VITE_PDF_API_URL || '/pdf-api'

const pdfApiClient = axios.create({
  baseURL: PDF_API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

// Attach auth token (same JWT used for backend)
pdfApiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ─── Transform helpers (snake_case API ↔ camelCase Frontend) ───────────────

/** Convert a single API rule (snake_case) → frontend rule (camelCase) */
function ruleFromApi(r: Record<string, unknown>): FieldMappingRule {
  return {
    source: (r.source_field ?? r.source ?? '') as string,
    target: (r.target_field ?? r.target ?? '') as string,
    fallbackSource: (r.fallback_source ?? r.fallbackSource ?? undefined) as string | undefined,
    defaultValue: (r.default_value ?? r.defaultValue ?? undefined) as string | undefined,
    dateTransform: ((r.date_transform ?? r.dateTransform ?? undefined) as string | undefined)
      ?.toUpperCase() as FieldMappingRule['dateTransform'],
    description: (r.description ?? undefined) as string | undefined,
  }
}

/** Convert a full API profile (snake_case) → frontend MappingProfile */
function profileFromApi(p: Record<string, unknown>): MappingProfile {
  const rules = (p.rules as Record<string, unknown>[]) ?? []
  return {
    id: p.id as string,
    name: p.name as string,
    description: (p.description ?? undefined) as string | undefined,
    vendorPattern: (p.vendor_pattern ?? p.vendorPattern ?? undefined) as string | undefined,
    organizationId: (p.organization_id ?? p.organizationId ?? undefined) as number | undefined,
    organizationName: (p.organization_name ?? p.organizationName ?? undefined) as string | undefined,
    isDefault: !!(p.is_default ?? p.isDefault),
    isBuiltin: !!(p.is_builtin ?? p.isBuiltin),
    erpType: (p.erp_type ?? p.erpType ?? undefined) as MappingProfile['erpType'],
    rules: rules.map(ruleFromApi),
    createdAt: (p.created_at ?? p.createdAt ?? undefined) as string | undefined,
    updatedAt: (p.updated_at ?? p.updatedAt ?? undefined) as string | undefined,
  }
}

/** Convert a frontend rule (camelCase) → API rule (snake_case) */
function ruleToApi(r: FieldMappingRule): Record<string, unknown> {
  return {
    source_field: r.source || null,
    target_field: r.target,
    fallback_source: r.fallbackSource || null,
    default_value: r.defaultValue || null,
    date_transform: r.dateTransform && r.dateTransform !== 'NONE'
      ? r.dateTransform.toLowerCase()
      : null,
    description: r.description || null,
  }
}

/** Convert a frontend create/update request → API payload (snake_case) */
function profileToApi(
  data: MappingProfileCreateRequest | MappingProfileUpdateRequest
): Record<string, unknown> {
  const payload: Record<string, unknown> = {}
  if ('name' in data && data.name !== undefined) payload.name = data.name
  if ('description' in data && data.description !== undefined) payload.description = data.description
  if ('vendorPattern' in data && data.vendorPattern !== undefined) payload.vendor_pattern = data.vendorPattern
  if ('isDefault' in data && data.isDefault !== undefined) payload.is_default = data.isDefault
  if ('organizationId' in data && (data as MappingProfileCreateRequest).organizationId !== undefined)
    payload.organization_id = (data as MappingProfileCreateRequest).organizationId
  if ('rules' in data && data.rules !== undefined)
    payload.rules = data.rules.map(ruleToApi)
  return payload
}

// ─── Service ───────────────────────────────────────────────────────────────

export const mappingService = {
  /**
   * List all mapping profiles, optionally filtered by organization.
   */
  async getProfiles(organizationId?: number): Promise<MappingProfile[]> {
    const params = organizationId ? { organization_id: organizationId } : {}
    const response = await pdfApiClient.get('/api/v1/mapping/profiles', { params })
    const rawProfiles = response.data.profiles ?? response.data ?? []
    return rawProfiles.map(profileFromApi)
  },

  /**
   * Get a single mapping profile by ID.
   */
  async getProfile(profileId: string): Promise<MappingProfile> {
    const response = await pdfApiClient.get(`/api/v1/mapping/profiles/${profileId}`)
    const raw = response.data.profile ?? response.data
    return profileFromApi(raw)
  },

  /**
   * Create a new mapping profile.
   */
  async createProfile(data: MappingProfileCreateRequest): Promise<MappingProfile> {
    const response = await pdfApiClient.post('/api/v1/mapping/profiles', profileToApi(data))
    const raw = response.data.profile ?? response.data
    return profileFromApi(raw)
  },

  /**
   * Update an existing mapping profile.
   */
  async updateProfile(profileId: string, data: MappingProfileUpdateRequest): Promise<MappingProfile> {
    const response = await pdfApiClient.put(`/api/v1/mapping/profiles/${profileId}`, profileToApi(data))
    const raw = response.data.profile ?? response.data
    return profileFromApi(raw)
  },

  /**
   * Delete a mapping profile.
   */
  async deleteProfile(profileId: string): Promise<void> {
    await pdfApiClient.delete(`/api/v1/mapping/profiles/${profileId}`)
  },

  /**
   * Get available source fields, target fields, and date transforms.
   * API returns { source_fields: [{value, label}], ... } — we flatten to string arrays.
   */
  async getAvailableFields(): Promise<MappingFieldInfo> {
    const response = await pdfApiClient.get('/api/v1/mapping/fields')
    const d = response.data

    const extractValues = (arr: unknown): string[] => {
      if (!Array.isArray(arr)) return []
      return arr.map((item: { value?: string } | string) =>
        typeof item === 'string' ? item : (item.value ?? '')
      )
    }

    return {
      sourceFields: extractValues(d.source_fields ?? d.sourceFields),
      targetFields: extractValues(d.target_fields ?? d.targetFields),
      dateTransforms: extractValues(d.date_transforms ?? d.dateTransforms),
    }
  },

  /**
   * Preview a mapping profile against raw fields without saving.
   */
  async previewMapping(
    profileId: string,
    rawFields: Record<string, string>
  ): Promise<Record<string, unknown>> {
    const response = await pdfApiClient.post('/api/v1/mapping/preview', {
      profile_id: profileId,
      raw_fields: rawFields,
    })
    return response.data
  },
}
