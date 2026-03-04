import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuthStore } from '@/store/authStore'
import { signupService } from '@/services/subscriptionService'
import {
  Eye,
  EyeOff,
  Loader2,
  Building2,
  CheckCircle,
  ArrowRight,
  ArrowLeft,
  Shield,
  Zap,
  Clock,
} from 'lucide-react'

const signupSchema = z.object({
  organizationName: z
    .string()
    .min(2, 'Organization name must be at least 2 characters')
    .max(100, 'Organization name must be less than 100 characters'),
  organizationEmail: z.string().email('Invalid organization email'),
  firstName: z.string().min(1, 'First name is required').max(50),
  lastName: z.string().min(1, 'Last name is required').max(50),
  adminEmail: z.string().email('Invalid email address'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Must contain an uppercase letter')
    .regex(/[a-z]/, 'Must contain a lowercase letter')
    .regex(/[0-9]/, 'Must contain a number')
    .regex(/[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/, 'Must contain a special character'),
  confirmPassword: z.string(),
  phone: z.string().optional(),
  companyWebsite: z.string().url('Invalid URL').optional().or(z.literal('')),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type SignupFormData = z.infer<typeof signupSchema>

export function SignupPage() {
  const navigate = useNavigate()
  const { login } = useAuthStore()
  const [error, setError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [step, setStep] = useState<1 | 2>(1)

  const {
    register,
    handleSubmit,
    trigger,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormData>({
    resolver: zodResolver(signupSchema),
    mode: 'onBlur',
  })

  const handleNextStep = async () => {
    const isValid = await trigger([
      'organizationName',
      'organizationEmail',
      'companyWebsite',
    ])
    if (isValid) setStep(2)
  }

  const onSubmit = async (data: SignupFormData) => {
    setError(null)
    try {
      const response = await signupService.signup({
        organizationName: data.organizationName,
        organizationEmail: data.organizationEmail,
        firstName: data.firstName,
        lastName: data.lastName,
        adminEmail: data.adminEmail,
        password: data.password,
        phone: data.phone,
        companyWebsite: data.companyWebsite || undefined,
      })

      // Store auth tokens
      login(response.auth)

      navigate('/onboarding')
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number; data?: { message?: string } } }
      const apiMessage = axiosError?.response?.data?.message

      if (apiMessage) {
        setError(apiMessage)
      } else if (axiosError?.response?.status === 409) {
        setError('An account with this email already exists. Please sign in instead.')
      } else {
        setError('Something went wrong during sign up. Please check your details and try again.')
      }
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left Panel - Benefits */}
      <div className="hidden lg:flex lg:w-[50%] bg-gradient-to-br from-slate-900 via-primary-900 to-indigo-900 relative overflow-hidden">
        {/* Decorative shapes */}
        <div className="absolute inset-0">
          <div className="absolute top-20 left-20 w-72 h-72 bg-primary-500/10 rounded-full blur-3xl" />
          <div className="absolute bottom-20 right-20 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl" />
          <div className="absolute inset-0 opacity-[0.03]" style={{
            backgroundImage: 'radial-gradient(circle, white 1px, transparent 1px)',
            backgroundSize: '32px 32px'
          }} />
        </div>

        <div className="relative flex flex-col justify-between p-12 w-full">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/30">
              <Zap className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-white">
                <span className="text-primary-400">Sync</span>Ledger
              </h1>
              <p className="text-[10px] tracking-[0.2em] text-slate-400 uppercase">Accounts Payable Automation</p>
            </div>
          </div>

          {/* Hero content */}
          <div>
            <h2 className="text-3xl font-bold text-white leading-tight mb-4">
              Start automating<br />
              <span className="bg-gradient-to-r from-primary-400 to-cyan-400 bg-clip-text text-transparent">
                in minutes
              </span>
            </h2>
            <p className="text-lg text-slate-300 max-w-md mb-10">
              Join hundreds of companies saving time on accounts payable with AI-powered invoice processing.
            </p>

            <div className="space-y-4">
              <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
                  <Clock className="w-5 h-5 text-primary-300" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-white mb-0.5">15-Day Free Trial</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">Full access to all features. No credit card required.</p>
                </div>
              </div>

              <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
                  <Zap className="w-5 h-5 text-primary-300" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-white mb-0.5">AI-Powered OCR</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">Extract invoice data automatically with industry-leading accuracy.</p>
                </div>
              </div>

              <div className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
                  <Shield className="w-5 h-5 text-primary-300" />
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-white mb-0.5">Enterprise Security</h3>
                  <p className="text-xs text-slate-400 leading-relaxed">AES-256 encryption at rest, SOC 2 compliant infrastructure.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Social proof */}
          <div className="flex items-center gap-6 text-slate-400 text-sm">
            <div>
              <p className="text-2xl font-bold text-white">99%+</p>
              <p className="text-xs">Extraction accuracy</p>
            </div>
            <div className="w-px h-10 bg-slate-700" />
            <div>
              <p className="text-2xl font-bold text-white">80%</p>
              <p className="text-xs">Time saved on AP</p>
            </div>
          </div>
        </div>
      </div>

      {/* Right Panel - Sign Up Form */}
      <div className="flex-1 flex items-center justify-center bg-gray-50 p-6">
        <div className="max-w-lg w-full">
          {/* Mobile logo */}
          <div className="lg:hidden text-center mb-6">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center mx-auto shadow-lg shadow-primary-200 mb-3">
              <Zap className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-2xl font-bold">
              <span className="text-primary-600">Sync</span><span className="text-gray-900">Ledger</span>
            </h1>
          </div>

          <div className="bg-white rounded-2xl shadow-xl shadow-gray-200/50 border border-gray-100 p-8">
            {/* Header */}
            <div className="text-center mb-6">
              <div className="w-12 h-12 bg-primary-50 rounded-2xl flex items-center justify-center mx-auto mb-3">
                <Building2 className="w-6 h-6 text-primary-600" />
              </div>
              <h2 className="text-xl font-bold text-gray-900">Create your account</h2>
              <p className="text-gray-500 text-sm mt-1">
                Start your 15-day free trial. No credit card needed.
              </p>
            </div>

          {/* Step indicators */}
          <div className="flex items-center justify-center mb-8">
            <div className="flex items-center gap-2">
              <div
                className={`w-8 h-8 rounded-xl flex items-center justify-center text-sm font-medium transition-colors ${
                  step >= 1
                    ? 'bg-gradient-to-br from-primary-600 to-primary-500 text-white shadow-sm'
                    : 'bg-gray-100 text-gray-500'
                }`}
              >
                {step > 1 ? <CheckCircle className="w-5 h-5" /> : '1'}
              </div>
              <span className="text-xs font-medium text-gray-500">Organization</span>
              <div className="w-12 h-0.5 bg-gray-200 mx-1 rounded-full overflow-hidden">
                <div
                  className={`h-full transition-all bg-primary-500 rounded-full ${
                    step >= 2 ? 'w-full' : 'w-0'
                  }`}
                />
              </div>
              <div
                className={`w-8 h-8 rounded-xl flex items-center justify-center text-sm font-medium transition-colors ${
                  step >= 2
                    ? 'bg-gradient-to-br from-primary-600 to-primary-500 text-white shadow-sm'
                    : 'bg-gray-100 text-gray-500'
                }`}
              >
                2
              </div>
              <span className="text-xs font-medium text-gray-500">Admin Account</span>
            </div>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {error && (
              <div className="p-4 bg-red-50 border border-red-100 rounded-xl text-red-600 text-sm flex items-start gap-2">
                <div className="w-5 h-5 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0 mt-0.5">
                  <span className="text-xs font-bold">!</span>
                </div>
                <span>{error}</span>
              </div>
            )}

            {/* Step 1: Organization Details */}
            {step === 1 && (
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Organization Name *
                  </label>
                  <input
                    type="text"
                    {...register('organizationName')}
                    className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                      errors.organizationName ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                    }`}
                    placeholder="Acme Corporation"
                  />
                  {errors.organizationName && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.organizationName.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Organization Email *
                  </label>
                  <input
                    type="email"
                    {...register('organizationEmail')}
                    className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                      errors.organizationEmail ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                    }`}
                    placeholder="billing@acmecorp.com"
                  />
                  {errors.organizationEmail && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.organizationEmail.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Company Website
                  </label>
                  <input
                    type="url"
                    {...register('companyWebsite')}
                    className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                      errors.companyWebsite ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                    }`}
                    placeholder="https://acmecorp.com"
                  />
                  {errors.companyWebsite && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.companyWebsite.message}
                    </p>
                  )}
                </div>

                <button
                  type="button"
                  onClick={handleNextStep}
                  className="w-full py-3 px-4 rounded-xl font-semibold text-white bg-gradient-to-r from-primary-600 to-primary-500 hover:from-primary-700 hover:to-primary-600 shadow-lg shadow-primary-200 transition-all active:scale-[0.98] flex items-center justify-center"
                >
                  Continue
                  <ArrowRight className="w-4 h-4 ml-2" />
                </button>
              </div>
            )}

            {/* Step 2: Admin Account Details */}
            {step === 2 && (
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      First Name *
                    </label>
                    <input
                      type="text"
                      {...register('firstName')}
                      className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                        errors.firstName ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                      }`}
                      placeholder="John"
                    />
                    {errors.firstName && (
                      <p className="mt-1 text-sm text-red-600">
                        {errors.firstName.message}
                      </p>
                    )}
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Last Name *
                    </label>
                    <input
                      type="text"
                      {...register('lastName')}
                      className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                        errors.lastName ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                      }`}
                      placeholder="Doe"
                    />
                    {errors.lastName && (
                      <p className="mt-1 text-sm text-red-600">
                        {errors.lastName.message}
                      </p>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Admin Email *
                  </label>
                  <input
                    type="email"
                    {...register('adminEmail')}
                    className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                      errors.adminEmail ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                    }`}
                    placeholder="john@acmecorp.com"
                  />
                  {errors.adminEmail && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.adminEmail.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Phone
                  </label>
                  <input
                    type="tel"
                    {...register('phone')}
                    className="w-full px-4 py-3 rounded-xl bg-gray-50 border border-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white"
                    placeholder="+1 (555) 000-0000"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Password *
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? 'text' : 'password'}
                      {...register('password')}
                      className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                        errors.password ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                      }`}
                      placeholder="••••••••"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-1 rounded-lg hover:bg-gray-100 transition-colors"
                    >
                      {showPassword ? (
                        <EyeOff className="w-5 h-5" />
                      ) : (
                        <Eye className="w-5 h-5" />
                      )}
                    </button>
                  </div>
                  {errors.password && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.password.message}
                    </p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Confirm Password *
                  </label>
                  <div className="relative">
                    <input
                      type={showConfirmPassword ? 'text' : 'password'}
                      {...register('confirmPassword')}
                      className={`w-full px-4 py-3 rounded-xl bg-gray-50 border transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent focus:bg-white ${
                        errors.confirmPassword ? 'border-red-300 bg-red-50/30' : 'border-gray-200'
                      }`}
                      placeholder="••••••••"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-1 rounded-lg hover:bg-gray-100 transition-colors"
                    >
                      {showConfirmPassword ? (
                        <EyeOff className="w-5 h-5" />
                      ) : (
                        <Eye className="w-5 h-5" />
                      )}
                    </button>
                  </div>
                  {errors.confirmPassword && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.confirmPassword.message}
                    </p>
                  )}
                </div>

                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={() => setStep(1)}
                    className="px-6 py-3 rounded-xl font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 transition-all active:scale-[0.98] flex items-center"
                  >
                    <ArrowLeft className="w-4 h-4 mr-2" />
                    Back
                  </button>
                  <button
                    type="submit"
                    disabled={isSubmitting}
                    className={`flex-1 py-3 px-4 rounded-xl font-semibold text-white transition-all active:scale-[0.98] ${
                      isSubmitting
                        ? 'bg-primary-400 cursor-not-allowed'
                        : 'bg-gradient-to-r from-primary-600 to-primary-500 hover:from-primary-700 hover:to-primary-600 shadow-lg shadow-primary-200'
                    }`}
                  >
                    {isSubmitting ? (
                      <span className="flex items-center justify-center">
                        <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                        Creating account...
                      </span>
                    ) : (
                      'Start Free Trial'
                    )}
                  </button>
                </div>
              </div>
            )}
          </form>
          </div>

          {/* Footer */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              Already have an account?{' '}
              <Link
                to="/login"
                className="text-primary-600 hover:text-primary-700 font-semibold"
              >
                Sign in
              </Link>
            </p>
          </div>

          <div className="mt-3 text-center text-xs text-gray-400">
            By signing up, you agree to our Terms of Service and Privacy Policy.
          </div>

          <div className="mt-3 text-center text-xs text-gray-400">
            &copy; {new Date().getFullYear()} SyncLedger by Vedvix. All rights reserved.
          </div>
        </div>
      </div>
    </div>
  )
}
