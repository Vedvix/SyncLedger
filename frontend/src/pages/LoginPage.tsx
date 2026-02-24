import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuthStore } from '@/store/authStore'
import { authService } from '@/services/authService'
import { Eye, EyeOff, Loader2 } from 'lucide-react'

const loginSchema = z.object({
  email: z.string().email('Invalid email address'),
  password: z.string().min(1, 'Password is required'),
})

type LoginFormData = z.infer<typeof loginSchema>

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
      // Redirect ONBOARDING orgs to the setup wizard
      if (response.user?.organizationStatus === 'ONBOARDING') {
        navigate('/onboarding')
      } else {
        navigate('/dashboard')
      }
    } catch (err: unknown) {
      // Extract the user-friendly message from the API response if available
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
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-500 to-primary-700 px-4">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl p-8">
        {/* Logo */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-primary-600">SyncLedger</h1>
          <p className="text-gray-500 mt-2">Invoice Processing Portal</p>
        </div>
        
        {/* Login Form */}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
              {error}
            </div>
          )}
          
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              Email Address
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
              className={`
                w-full px-4 py-3 rounded-lg border transition-colors
                focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
                ${errors.email ? 'border-red-300' : 'border-gray-300'}
              `}
              placeholder="you@vedvix.com"
            />
            {errors.email && (
              <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>
            )}
          </div>
          
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              Password
            </label>
            <div className="relative">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                {...register('password')}
                className={`
                  w-full px-4 py-3 rounded-lg border transition-colors
                  focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
                  ${errors.password ? 'border-red-300' : 'border-gray-300'}
                `}
                placeholder="••••••••"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
              </button>
            </div>
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
            )}
          </div>
          
          <button
            type="submit"
            disabled={isSubmitting}
            className={`
              w-full py-3 px-4 rounded-lg font-medium text-white transition-colors
              ${isSubmitting 
                ? 'bg-primary-400 cursor-not-allowed' 
                : 'bg-primary-600 hover:bg-primary-700'
              }
            `}
          >
            {isSubmitting ? (
              <span className="flex items-center justify-center">
                <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                Signing in...
              </span>
            ) : (
              'Sign In'
            )}
          </button>
        </form>
        
        {/* Footer */}
        <p className="mt-8 text-center text-sm text-gray-500">
          Don't have an account?{' '}
          <Link to="/signup" className="text-primary-600 hover:text-primary-700 font-medium">
            Sign up for free
          </Link>
        </p>
        
        <div className="mt-6 text-center text-xs text-gray-400">
          © {new Date().getFullYear()} vedvix. All rights reserved.
        </div>
      </div>
    </div>
  )
}
