import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuthStore } from '@/store/authStore'
import { authService } from '@/services/authService'
import { Eye, EyeOff, Loader2, Zap, FileText, Shield, BarChart3, ArrowRight } from 'lucide-react'

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormData = z.infer<typeof loginSchema>

const FEATURES = [
  { icon: FileText, title: 'AI-Powered Extraction', desc: 'Automatically extract invoice data from PDFs with 99%+ accuracy using advanced AI models.' },
  { icon: Shield, title: 'Approval Workflows', desc: 'Multi-level approval workflows with role-based access control for your entire team.' },
  { icon: BarChart3, title: 'Real-time Analytics', desc: 'Customizable dashboards with real-time insights into your AP processing pipeline.' },
]

export function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuthStore()
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  })
  
  const onSubmit = async (data: LoginFormData) => {
    setError(null)
    try {
      const response = await authService.login(data)
      login(response)
      if (response.user?.organizationStatus === 'ONBOARDING') {
        navigate('/onboarding')
      } else {
        navigate('/dashboard')
      }
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number; data?: { message?: string } } }
      const apiMessage = axiosError?.response?.data?.message
      if (apiMessage) {
        setError(apiMessage)
      } else if (axiosError?.response?.status === 401) {
        setError('The email or password you entered is incorrect. Please try again.')
      } else if (axiosError?.response?.status === 403) {
        setError('Your account has been locked. Please contact your administrator.')
      } else {
        setError('Unable to connect to the server. Please check your internet connection and try again.')
      }
    }
  }
  
  return (
    <div className="min-h-screen flex">
      {/* ─── Left: Marketing Showcase ─── */}
      <div className="hidden lg:flex lg:w-[55%] bg-gradient-to-br from-slate-900 via-primary-900 to-indigo-900 relative overflow-hidden">
        {/* Decorative shapes */}
        <div className="absolute inset-0">
          <div className="absolute top-20 left-20 w-72 h-72 bg-primary-500/10 rounded-full blur-3xl" />
          <div className="absolute bottom-20 right-20 w-96 h-96 bg-indigo-500/10 rounded-full blur-3xl" />
          <div className="absolute top-1/2 left-1/3 w-40 h-40 bg-cyan-500/5 rounded-full blur-2xl" />
          {/* Grid pattern */}
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
            <h2 className="text-4xl font-bold text-white leading-tight mb-4">
              Automate your<br />
              <span className="bg-gradient-to-r from-primary-400 to-cyan-400 bg-clip-text text-transparent">
                invoice processing
              </span>
            </h2>
            <p className="text-lg text-slate-300 max-w-md mb-10">
              From email to ERP in minutes, not days. AI-powered extraction, smart approval workflows, and seamless integrations.
            </p>

            {/* Feature cards */}
            <div className="space-y-4">
              {FEATURES.map((f) => (
                <div key={f.title} className="flex items-start gap-4 p-4 rounded-2xl bg-white/5 backdrop-blur-sm border border-white/10 hover:bg-white/10 transition-colors">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500/20 to-indigo-500/20 flex items-center justify-center flex-shrink-0">
                    <f.icon className="w-5 h-5 text-primary-300" />
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-white mb-0.5">{f.title}</h3>
                    <p className="text-xs text-slate-400 leading-relaxed">{f.desc}</p>
                  </div>
                </div>
              ))}
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
            <div className="w-px h-10 bg-slate-700" />
            <div>
              <p className="text-2xl font-bold text-white">24/7</p>
              <p className="text-xs">Auto email polling</p>
            </div>
          </div>
        </div>
      </div>

      {/* ─── Right: Login Form ─── */}
      <div className="flex-1 flex items-center justify-center bg-gray-50 px-4 py-12">
        <div className="max-w-[420px] w-full">
          {/* Mobile logo */}
          <div className="lg:hidden text-center mb-8">
            <div className="w-12 h-12 rounded-xl bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center mx-auto shadow-lg shadow-primary-500/20 mb-3">
              <Zap className="w-6 h-6 text-white" />
            </div>
            <h1 className="text-2xl font-bold">
              <span className="text-primary-600">Sync</span><span className="text-gray-900">Ledger</span>
            </h1>
          </div>

          <div className="bg-white rounded-2xl shadow-xl shadow-gray-200/50 border border-gray-100 p-8">
            <div className="mb-8">
              <h2 className="text-2xl font-bold text-gray-900">Welcome back</h2>
              <p className="text-gray-500 mt-1 text-sm">Sign in to your account to continue</p>
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
              
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1.5">
                  Email Address
                </label>
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  {...register('email')}
                  className={`
                    w-full px-4 py-3 rounded-xl border transition-all text-sm
                    focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-400
                    bg-gray-50 hover:bg-white
                    ${errors.email ? 'border-red-300 bg-red-50/50' : 'border-gray-200'}
                  `}
                  placeholder="you@company.com"
                />
                {errors.email && (
                  <p className="mt-1.5 text-sm text-red-600">{errors.email.message}</p>
                )}
              </div>
              
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                    Password
                  </label>
                  <button type="button" className="text-xs text-primary-600 hover:text-primary-700 font-medium">
                    Forgot password?
                  </button>
                </div>
                <div className="relative">
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    {...register('password')}
                    className={`
                      w-full px-4 py-3 rounded-xl border transition-all text-sm
                      focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-400
                      bg-gray-50 hover:bg-white
                      ${errors.password ? 'border-red-300 bg-red-50/50' : 'border-gray-200'}
                    `}
                    placeholder="Enter your password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-1 rounded-lg transition-colors"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.password && (
                  <p className="mt-1.5 text-sm text-red-600">{errors.password.message}</p>
                )}
              </div>
              
              <button
                type="submit"
                disabled={isSubmitting}
                className={`
                  w-full py-3 px-4 rounded-xl font-semibold text-white transition-all text-sm flex items-center justify-center gap-2
                  shadow-sm shadow-primary-200
                  ${isSubmitting 
                    ? 'bg-primary-400 cursor-not-allowed' 
                    : 'bg-gradient-to-r from-primary-600 to-primary-500 hover:from-primary-700 hover:to-primary-600 active:scale-[0.99]'
                  }
                `}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Signing in...
                  </>
                ) : (
                  <>
                    Sign In
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>
          </div>
          
          {/* Footer */}
          <p className="mt-6 text-center text-sm text-gray-500">
            Don't have an account?{' '}
            <Link to="/signup" className="text-primary-600 hover:text-primary-700 font-semibold">
              Get started free
            </Link>
          </p>
          
          <div className="mt-4 text-center text-xs text-gray-400">
            © {new Date().getFullYear()} SyncLedger by Vedvix. All rights reserved.
          </div>
        </div>
      </div>
    </div>
  )
}
