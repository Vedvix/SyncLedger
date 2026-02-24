import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import {
  microsoftConfigService,
  erpConfigService,
  onboardingService,
} from '@/services/subscriptionService'
import {
  Mail,
  Key,
  Eye,
  EyeOff,
  Shield,
  CheckCircle,
  ArrowRight,
  ArrowLeft,
  Loader2,
  Server,
  Globe,
  SkipForward,
  Rocket,
} from 'lucide-react'
import type { UpdateMicrosoftConfigRequest, UpdateErpConfigRequest, ErpType } from '@/types'
import { ERP_TYPE_LABELS } from '@/types'

type OnboardingStep = 'microsoft' | 'erp' | 'complete'

const STEPS: { key: OnboardingStep; label: string; icon: React.ElementType }[] = [
  { key: 'microsoft', label: 'Email Integration', icon: Mail },
  { key: 'erp', label: 'ERP Configuration', icon: Server },
  { key: 'complete', label: 'All Set!', icon: Rocket },
]

export function OnboardingPage() {
  const navigate = useNavigate()
  const { user, updateUser } = useAuthStore()
  const queryClient = useQueryClient()

  const [currentStep, setCurrentStep] = useState<OnboardingStep>('microsoft')
  const [showSecret, setShowSecret] = useState(false)
  const [showErpKey, setShowErpKey] = useState(false)
  const [msError, setMsError] = useState<string | null>(null)
  const [erpError, setErpError] = useState<string | null>(null)
  const [msSaved, setMsSaved] = useState(false)
  const [erpSaved, setErpSaved] = useState(false)

  const [msForm, setMsForm] = useState<UpdateMicrosoftConfigRequest>({
    msClientId: '',
    msClientSecret: '',
    msTenantId: '',
    msMailboxEmail: '',
  })

  const [erpForm, setErpForm] = useState<UpdateErpConfigRequest>({
    erpType: undefined,
    erpApiEndpoint: '',
    erpApiKey: '',
    erpTenantId: '',
    erpCompanyId: '',
  })

  // If user is not in ONBOARDING status, redirect to dashboard
  useEffect(() => {
    if (user?.organizationStatus && user.organizationStatus !== 'ONBOARDING') {
      navigate('/dashboard', { replace: true })
    }
  }, [user, navigate])

  const msMutation = useMutation({
    mutationFn: (data: UpdateMicrosoftConfigRequest) =>
      microsoftConfigService.updateMicrosoftConfig(data),
    onSuccess: () => {
      setMsSaved(true)
      setMsError(null)
      queryClient.invalidateQueries({ queryKey: ['microsoft-config'] })
    },
    onError: (err: unknown) => {
      const axiosError = err as { response?: { data?: { message?: string } } }
      setMsError(axiosError?.response?.data?.message || 'Failed to save Microsoft credentials')
    },
  })

  const erpMutation = useMutation({
    mutationFn: (data: UpdateErpConfigRequest) => erpConfigService.updateErpConfig(data),
    onSuccess: () => {
      setErpSaved(true)
      setErpError(null)
      queryClient.invalidateQueries({ queryKey: ['erp-config'] })
    },
    onError: (err: unknown) => {
      const axiosError = err as { response?: { data?: { message?: string } } }
      setErpError(axiosError?.response?.data?.message || 'Failed to save ERP configuration')
    },
  })

  const completeMutation = useMutation({
    mutationFn: () => onboardingService.completeOnboarding(),
    onSuccess: () => {
      // Update stored user status so redirect guard stops
      updateUser({ organizationStatus: 'TRIAL' } as Partial<typeof user & { organizationStatus: string }>)
      navigate('/dashboard', { replace: true })
    },
    onError: (err: unknown) => {
      const axiosError = err as { response?: { data?: { message?: string } } }
      setMsError(axiosError?.response?.data?.message || 'Failed to complete onboarding')
      // Go back to Microsoft step if MS creds not configured
      setCurrentStep('microsoft')
    },
  })

  const handleMsSave = (e: React.FormEvent) => {
    e.preventDefault()
    if (!msForm.msClientId || !msForm.msClientSecret || !msForm.msTenantId || !msForm.msMailboxEmail) {
      setMsError('All Microsoft credentials fields are required')
      return
    }
    msMutation.mutate(msForm)
  }

  const handleErpSave = (e: React.FormEvent) => {
    e.preventDefault()
    if (erpForm.erpType && !erpForm.erpApiEndpoint) {
      setErpError('API Endpoint is required when ERP type is selected')
      return
    }
    erpMutation.mutate(erpForm)
  }

  const stepIndex = STEPS.findIndex((s) => s.key === currentStep)

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-50 via-white to-primary-50">
      {/* Header */}
      <div className="border-b bg-white/80 backdrop-blur-sm">
        <div className="max-w-3xl mx-auto px-6 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-primary-600">SyncLedger</h1>
            <p className="text-sm text-gray-500">Setup your organization</p>
          </div>
          <div className="text-sm text-gray-500">
            Welcome, <span className="font-medium text-gray-700">{user?.firstName}</span>
          </div>
        </div>
      </div>

      {/* Progress Steps */}
      <div className="max-w-3xl mx-auto px-6 pt-8 pb-4">
        <div className="flex items-center justify-between">
          {STEPS.map((step, idx) => {
            const Icon = step.icon
            const isActive = idx === stepIndex
            const isCompleted = idx < stepIndex
            return (
              <div key={step.key} className="flex items-center flex-1 last:flex-none">
                <div className="flex flex-col items-center">
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-all ${
                      isCompleted
                        ? 'bg-green-500 border-green-500 text-white'
                        : isActive
                        ? 'bg-primary-600 border-primary-600 text-white'
                        : 'bg-white border-gray-300 text-gray-400'
                    }`}
                  >
                    {isCompleted ? <CheckCircle className="w-5 h-5" /> : <Icon className="w-5 h-5" />}
                  </div>
                  <span
                    className={`text-xs mt-1 font-medium ${
                      isActive ? 'text-primary-700' : isCompleted ? 'text-green-600' : 'text-gray-400'
                    }`}
                  >
                    {step.label}
                  </span>
                </div>
                {idx < STEPS.length - 1 && (
                  <div
                    className={`flex-1 h-0.5 mx-3 mt-[-20px] ${
                      idx < stepIndex ? 'bg-green-400' : 'bg-gray-200'
                    }`}
                  />
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Step Content */}
      <div className="max-w-3xl mx-auto px-6 py-6">
        {/* Step 1: Microsoft Credentials */}
        {currentStep === 'microsoft' && (
          <div className="bg-white rounded-2xl shadow-lg border p-8">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-blue-100 rounded-lg">
                <Mail className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900">Microsoft Email Integration</h2>
                <p className="text-sm text-gray-500">
                  Connect your Microsoft 365 mailbox to automatically process invoices from emails
                </p>
              </div>
            </div>

            <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg text-sm text-blue-800">
              <Shield className="w-4 h-4 inline mr-1" />
              Your credentials are encrypted with AES-256 and stored securely. You'll need an Azure
              App Registration with <strong>Mail.Read</strong> permissions.
            </div>

            {msError && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                {msError}
              </div>
            )}

            {msSaved && (
              <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700 flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                Microsoft credentials saved successfully!
              </div>
            )}

            <form onSubmit={handleMsSave} className="mt-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Key className="w-4 h-4 inline mr-1" />
                  Application (Client) ID <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={msForm.msClientId}
                  onChange={(e) => setMsForm({ ...msForm, msClientId: e.target.value })}
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Shield className="w-4 h-4 inline mr-1" />
                  Client Secret <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <input
                    type={showSecret ? 'text' : 'password'}
                    value={msForm.msClientSecret}
                    onChange={(e) => setMsForm({ ...msForm, msClientSecret: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none pr-10"
                    placeholder="Enter your client secret"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowSecret(!showSecret)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showSecret ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Globe className="w-4 h-4 inline mr-1" />
                  Directory (Tenant) ID <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={msForm.msTenantId}
                  onChange={(e) => setMsForm({ ...msForm, msTenantId: e.target.value })}
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Mail className="w-4 h-4 inline mr-1" />
                  Mailbox Email Address <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  value={msForm.msMailboxEmail}
                  onChange={(e) => setMsForm({ ...msForm, msMailboxEmail: e.target.value })}
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  placeholder="invoices@yourcompany.com"
                  required
                />
                <p className="text-xs text-gray-400 mt-1">
                  This is the email address SyncLedger will monitor for incoming invoices
                </p>
              </div>

              <div className="flex items-center justify-between pt-4 border-t">
                <div />
                <div className="flex items-center gap-3">
                  <button
                    type="submit"
                    disabled={msMutation.isPending}
                    className="px-5 py-2.5 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 flex items-center gap-2 font-medium"
                  >
                    {msMutation.isPending ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      <CheckCircle className="w-4 h-4" />
                    )}
                    Save Credentials
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (!msSaved) {
                        setMsError('Please save your Microsoft credentials first')
                        return
                      }
                      setCurrentStep('erp')
                    }}
                    className="px-5 py-2.5 bg-gray-900 text-white rounded-lg hover:bg-gray-800 flex items-center gap-2 font-medium"
                  >
                    Next
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </form>
          </div>
        )}

        {/* Step 2: ERP Configuration */}
        {currentStep === 'erp' && (
          <div className="bg-white rounded-2xl shadow-lg border p-8">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 bg-purple-100 rounded-lg">
                <Server className="w-6 h-6 text-purple-600" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-gray-900">ERP Integration</h2>
                <p className="text-sm text-gray-500">
                  Connect your ERP system to automatically sync processed invoices
                </p>
              </div>
            </div>

            <div className="mt-4 p-4 bg-purple-50 border border-purple-200 rounded-lg text-sm text-purple-800">
              <Server className="w-4 h-4 inline mr-1" />
              This step is optional. You can configure ERP integration later from Settings.
            </div>

            {erpError && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                {erpError}
              </div>
            )}

            {erpSaved && (
              <div className="mt-4 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700 flex items-center gap-2">
                <CheckCircle className="w-4 h-4" />
                ERP configuration saved successfully!
              </div>
            )}

            <form onSubmit={handleErpSave} className="mt-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  ERP System Type
                </label>
                <select
                  value={erpForm.erpType || ''}
                  onChange={(e) =>
                    setErpForm({ ...erpForm, erpType: (e.target.value as ErpType) || undefined })
                  }
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none bg-white"
                >
                  <option value="">Select ERP system...</option>
                  {Object.entries(ERP_TYPE_LABELS).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Globe className="w-4 h-4 inline mr-1" />
                  API Endpoint URL
                </label>
                <input
                  type="url"
                  value={erpForm.erpApiEndpoint}
                  onChange={(e) => setErpForm({ ...erpForm, erpApiEndpoint: e.target.value })}
                  className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  placeholder="https://api.yourerp.com/v1"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  <Key className="w-4 h-4 inline mr-1" />
                  API Key / Secret
                </label>
                <div className="relative">
                  <input
                    type={showErpKey ? 'text' : 'password'}
                    value={erpForm.erpApiKey}
                    onChange={(e) => setErpForm({ ...erpForm, erpApiKey: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none pr-10"
                    placeholder="Enter your API key"
                  />
                  <button
                    type="button"
                    onClick={() => setShowErpKey(!showErpKey)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    {showErpKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Tenant / Environment ID
                  </label>
                  <input
                    type="text"
                    value={erpForm.erpTenantId}
                    onChange={(e) => setErpForm({ ...erpForm, erpTenantId: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                    placeholder="Optional"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Company ID
                  </label>
                  <input
                    type="text"
                    value={erpForm.erpCompanyId}
                    onChange={(e) => setErpForm({ ...erpForm, erpCompanyId: e.target.value })}
                    className="w-full px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                    placeholder="Optional"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between pt-4 border-t">
                <button
                  type="button"
                  onClick={() => setCurrentStep('microsoft')}
                  className="px-5 py-2.5 text-gray-600 hover:text-gray-800 flex items-center gap-2 font-medium"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Back
                </button>
                <div className="flex items-center gap-3">
                  {erpForm.erpType && (
                    <button
                      type="submit"
                      disabled={erpMutation.isPending}
                      className="px-5 py-2.5 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50 flex items-center gap-2 font-medium"
                    >
                      {erpMutation.isPending ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <CheckCircle className="w-4 h-4" />
                      )}
                      Save ERP Config
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => setCurrentStep('complete')}
                    className="px-5 py-2.5 bg-gray-900 text-white rounded-lg hover:bg-gray-800 flex items-center gap-2 font-medium"
                  >
                    {!erpForm.erpType ? (
                      <>
                        <SkipForward className="w-4 h-4" />
                        Skip for Now
                      </>
                    ) : (
                      <>
                        Next
                        <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </div>
              </div>
            </form>
          </div>
        )}

        {/* Step 3: Complete */}
        {currentStep === 'complete' && (
          <div className="bg-white rounded-2xl shadow-lg border p-8 text-center">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Rocket className="w-8 h-8 text-green-600" />
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">You're All Set!</h2>
            <p className="text-gray-500 mb-6 max-w-md mx-auto">
              Your organization is configured and ready to go. SyncLedger will start monitoring your
              mailbox for incoming invoices.
            </p>

            <div className="bg-gray-50 rounded-xl p-4 text-left max-w-sm mx-auto mb-6 space-y-2">
              <div className="flex items-center gap-2 text-sm">
                <CheckCircle className="w-4 h-4 text-green-500" />
                <span className="text-gray-700">Microsoft email integration configured</span>
              </div>
              <div className="flex items-center gap-2 text-sm">
                {erpSaved ? (
                  <CheckCircle className="w-4 h-4 text-green-500" />
                ) : (
                  <div className="w-4 h-4 rounded-full border-2 border-gray-300" />
                )}
                <span className={erpSaved ? 'text-gray-700' : 'text-gray-400'}>
                  ERP integration {erpSaved ? 'configured' : 'skipped (configure later in Settings)'}
                </span>
              </div>
            </div>

            <div className="flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => setCurrentStep('erp')}
                className="px-5 py-2.5 text-gray-600 hover:text-gray-800 flex items-center gap-2 font-medium"
              >
                <ArrowLeft className="w-4 h-4" />
                Back
              </button>
              <button
                onClick={() => completeMutation.mutate()}
                disabled={completeMutation.isPending}
                className="px-8 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 flex items-center gap-2 font-medium text-lg"
              >
                {completeMutation.isPending ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <Rocket className="w-5 h-5" />
                )}
                Launch Dashboard
              </button>
            </div>

            {msError && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
                {msError}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
