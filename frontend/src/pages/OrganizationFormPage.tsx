import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { 
  ArrowLeft, 
  Building2, 
  Mail, 
  Globe, 
  Key,
  Save,
  AlertCircle,
  CheckCircle
} from 'lucide-react'
import { organizationService } from '@/services/organizationService'
import type { CreateOrganizationRequest, UpdateOrganizationRequest, OrganizationStatus } from '@/types'

export function OrganizationFormPage() {
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const isEditing = !!id
  
  const [formData, setFormData] = useState<{
    name: string
    slug: string
    emailAddress: string
    sageApiEndpoint: string
    sageApiKey: string
    status?: OrganizationStatus
  }>({
    name: '',
    slug: '',
    emailAddress: '',
    sageApiEndpoint: '',
    sageApiKey: '',
  })
  
  const [errors, setErrors] = useState<string[]>([])
  const [success, setSuccess] = useState(false)
  
  // Fetch existing organization if editing
  const { data: existingOrg, isLoading: loadingOrg } = useQuery({
    queryKey: ['organization', id],
    queryFn: () => organizationService.getOrganization(Number(id)),
    enabled: isEditing,
  })
  
  // Pre-fill form when editing
  useEffect(() => {
    if (existingOrg) {
      setFormData({
        name: existingOrg.name,
        slug: existingOrg.slug,
        emailAddress: existingOrg.emailAddress || '',
        sageApiEndpoint: existingOrg.sageApiEndpoint || '',
        sageApiKey: '', // Don't show existing key
        status: existingOrg.status,
      })
    }
  }, [existingOrg])
  
  // Auto-generate slug from name
  const handleNameChange = (name: string) => {
    setFormData((prev) => ({
      ...prev,
      name,
      slug: isEditing ? prev.slug : name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
    }))
  }
  
  // Create mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateOrganizationRequest) => organizationService.createOrganization(data),
    onSuccess: () => {
      setSuccess(true)
      setErrors([])
      setTimeout(() => {
        navigate('/super-admin')
      }, 1500)
    },
    onError: (error: any) => {
      setErrors([error.response?.data?.message || error.message || 'Failed to create organization'])
      setSuccess(false)
    },
  })
  
  // Update mutation
  const updateMutation = useMutation({
    mutationFn: (data: UpdateOrganizationRequest) => organizationService.updateOrganization(Number(id), data),
    onSuccess: () => {
      setSuccess(true)
      setErrors([])
      setTimeout(() => {
        navigate(`/super-admin/organizations/${id}`)
      }, 1500)
    },
    onError: (error: any) => {
      setErrors([error.response?.data?.message || error.message || 'Failed to update organization'])
      setSuccess(false)
    },
  })
  
  const validateForm = (): string[] => {
    const validationErrors: string[] = []
    
    if (!formData.name.trim()) {
      validationErrors.push('Organization name is required')
    }
    
    if (!isEditing && !formData.slug.trim()) {
      validationErrors.push('Slug is required')
    }
    
    if (formData.slug && !/^[a-z0-9-]+$/.test(formData.slug)) {
      validationErrors.push('Slug can only contain lowercase letters, numbers, and hyphens')
    }
    
    if (formData.emailAddress && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.emailAddress)) {
      validationErrors.push('Invalid email address format')
    }
    
    return validationErrors
  }
  
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setErrors([])
    
    const validationErrors = validateForm()
    if (validationErrors.length > 0) {
      setErrors(validationErrors)
      return
    }
    
    if (isEditing) {
      const updateData: UpdateOrganizationRequest = {
        name: formData.name,
        emailAddress: formData.emailAddress || undefined,
        sageApiEndpoint: formData.sageApiEndpoint || undefined,
        status: formData.status,
      }
      // Only include API key if user entered a new one
      if (formData.sageApiKey) {
        updateData.sageApiKey = formData.sageApiKey
      }
      updateMutation.mutate(updateData)
    } else {
      createMutation.mutate({
        name: formData.name,
        slug: formData.slug,
        emailAddress: formData.emailAddress || undefined,
        sageApiEndpoint: formData.sageApiEndpoint || undefined,
        sageApiKey: formData.sageApiKey || undefined,
      })
    }
  }
  
  const isSubmitting = createMutation.isPending || updateMutation.isPending
  
  if (loadingOrg) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    )
  }
  
  return (
    <div className="max-w-2xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button
          onClick={() => navigate('/super-admin')}
          className="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {isEditing ? 'Edit Organization' : 'Create New Organization'}
          </h1>
          <p className="text-gray-500 text-sm mt-1">
            {isEditing 
              ? 'Update organization details and configuration'
              : 'Set up a new organization with their configuration'}
          </p>
        </div>
      </div>
      
      {/* Alerts */}
      {errors.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex gap-2">
            <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-red-800 font-medium">Please fix the following errors:</p>
              <ul className="list-disc list-inside text-sm text-red-700 mt-1 space-y-1">
                {errors.map((error, index) => (
                  <li key={index}>{error}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
      
      {success && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex gap-2 items-center">
            <CheckCircle className="w-5 h-5 text-green-500" />
            <p className="text-green-800 font-medium">
              Organization {isEditing ? 'updated' : 'created'} successfully!
            </p>
          </div>
        </div>
      )}
      
      {/* Form */}
      <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm p-6 space-y-6">
        {/* Basic Information */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Building2 className="w-5 h-5 text-gray-400" />
            Basic Information
          </h2>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Organization Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => handleNameChange(e.target.value)}
                placeholder="Acme Corporation"
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                disabled={isSubmitting}
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                URL Slug {!isEditing && '*'}
              </label>
              <div className="flex items-center">
                <span className="px-3 py-2 bg-gray-100 border border-r-0 rounded-l-lg text-gray-500 text-sm">
                  syncledger.local/
                </span>
                <input
                  type="text"
                  value={formData.slug}
                  onChange={(e) => setFormData((prev) => ({ ...prev, slug: e.target.value.toLowerCase() }))}
                  placeholder="acme-corp"
                  className="flex-1 px-4 py-2 border rounded-r-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                  disabled={isEditing || isSubmitting}
                />
              </div>
              <p className="mt-1 text-sm text-gray-500">
                {isEditing 
                  ? 'Slug cannot be changed after creation'
                  : 'Used in URLs. Only lowercase letters, numbers, and hyphens allowed.'}
              </p>
            </div>
          </div>
        </div>
        
        {/* Contact Information */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Mail className="w-5 h-5 text-gray-400" />
            Contact Information
          </h2>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Organization Email
            </label>
            <input
              type="email"
              value={formData.emailAddress}
              onChange={(e) => setFormData((prev) => ({ ...prev, emailAddress: e.target.value }))}
              placeholder="invoices@acme.com"
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              disabled={isSubmitting}
            />
            <p className="mt-1 text-sm text-gray-500">
              This email will receive invoice notifications and alerts.
            </p>
          </div>
        </div>
        
        {/* Sage Integration */}
        <div>
          <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Globe className="w-5 h-5 text-gray-400" />
            Sage Integration (Optional)
          </h2>
          
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Sage API Endpoint
              </label>
              <input
                type="url"
                value={formData.sageApiEndpoint}
                onChange={(e) => setFormData((prev) => ({ ...prev, sageApiEndpoint: e.target.value }))}
                placeholder="https://api.sage.com/v1"
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                disabled={isSubmitting}
              />
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1 flex items-center gap-2">
                <Key className="w-4 h-4" />
                Sage API Key
              </label>
              <input
                type="password"
                value={formData.sageApiKey}
                onChange={(e) => setFormData((prev) => ({ ...prev, sageApiKey: e.target.value }))}
                placeholder={isEditing ? "••••••••••••" : "Enter API key"}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                disabled={isSubmitting}
              />
              {isEditing && (
                <p className="mt-1 text-sm text-gray-500">
                  Leave blank to keep the existing API key.
                </p>
              )}
            </div>
          </div>
        </div>
        
        {/* Status (only for editing) */}
        {isEditing && (
          <div>
            <h2 className="text-lg font-semibold text-gray-900 mb-4">
              Organization Status
            </h2>
            
            <select
              value={formData.status}
              onChange={(e) => setFormData((prev) => ({ ...prev, status: e.target.value as OrganizationStatus }))}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              disabled={isSubmitting}
            >
              <option value="ONBOARDING">Onboarding</option>
              <option value="ACTIVE">Active</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        )}
        
        {/* Actions */}
        <div className="flex justify-end gap-3 pt-4 border-t">
          <button
            type="button"
            onClick={() => navigate('/super-admin')}
            className="px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {isSubmitting ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
            ) : (
              <Save className="w-4 h-4" />
            )}
            {isEditing ? 'Update Organization' : 'Create Organization'}
          </button>
        </div>
      </form>
    </div>
  )
}
