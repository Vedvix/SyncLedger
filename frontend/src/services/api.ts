import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/store/authStore'

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api'

// Token refresh configuration
const TOKEN_REFRESH_THRESHOLD_MS = 5 * 60 * 1000 // 5 minutes before expiry

// Track refresh state to prevent concurrent refresh attempts
let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

// Add subscriber to wait for token refresh
const subscribeToTokenRefresh = (callback: (token: string) => void) => {
  refreshSubscribers.push(callback)
}

// Notify all subscribers with new token
const onTokenRefreshed = (newToken: string) => {
  refreshSubscribers.forEach(callback => callback(newToken))
  refreshSubscribers = []
}

// Clear subscribers on refresh failure
const onRefreshFailed = () => {
  refreshSubscribers = []
}

// Parse JWT token to get expiration time
const getTokenExpiration = (token: string): number | null => {
  try {
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload))
    return decoded.exp ? decoded.exp * 1000 : null // Convert to milliseconds
  } catch {
    return null
  }
}

// Check if token is about to expire
const isTokenExpiringSoon = (token: string): boolean => {
  const expiration = getTokenExpiration(token)
  if (!expiration) return true
  return expiration - Date.now() < TOKEN_REFRESH_THRESHOLD_MS
}

// Perform token refresh
const performTokenRefresh = async (): Promise<string | null> => {
  const refreshToken = useAuthStore.getState().refreshToken
  if (!refreshToken) return null

  try {
    const response = await axios.post(`${API_BASE_URL}/v1/auth/refresh`, {
      refreshToken,
    })
    
    const { accessToken, refreshToken: newRefreshToken } = response.data.data
    useAuthStore.getState().setTokens(accessToken, newRefreshToken)
    return accessToken
  } catch (error) {
    console.error('Token refresh failed:', error)
    return null
  }
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token and proactively refresh if expiring
apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const { accessToken, refreshToken } = useAuthStore.getState()
    
    // Skip token handling for auth endpoints
    const isAuthEndpoint = config.url?.includes('/v1/auth/')
    if (isAuthEndpoint && !config.url?.includes('/v1/auth/me') && !config.url?.includes('/v1/auth/sessions')) {
      return config
    }
    
    if (accessToken) {
      // Check if token is expiring soon
      if (isTokenExpiringSoon(accessToken) && refreshToken) {
        // If already refreshing, wait for that to complete
        if (isRefreshing) {
          const newToken = await new Promise<string>((resolve) => {
            subscribeToTokenRefresh(resolve)
          })
          config.headers.Authorization = `Bearer ${newToken}`
          return config
        }

        // Start refresh process
        isRefreshing = true
        try {
          const newToken = await performTokenRefresh()
          if (newToken) {
            onTokenRefreshed(newToken)
            config.headers.Authorization = `Bearer ${newToken}`
          } else {
            // Refresh failed, use old token (will likely get 401)
            config.headers.Authorization = `Bearer ${accessToken}`
          }
        } finally {
          isRefreshing = false
        }
      } else {
        config.headers.Authorization = `Bearer ${accessToken}`
      }
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor for handling 401 errors (fallback if proactive refresh missed)
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    
    if (!originalRequest) {
      return Promise.reject(error)
    }
    
    // Handle 401 errors (token expired)
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true
      
      // Skip retry for auth endpoints (login, refresh)
      const isAuthEndpoint = originalRequest.url?.includes('/v1/auth/login') || 
                            originalRequest.url?.includes('/v1/auth/refresh')
      if (isAuthEndpoint) {
        return Promise.reject(error)
      }
      
      // If already refreshing, wait for it
      if (isRefreshing) {
        try {
          const newToken = await new Promise<string>((resolve, reject) => {
            subscribeToTokenRefresh(resolve)
            // Set timeout to prevent infinite wait
            setTimeout(() => reject(new Error('Token refresh timeout')), 10000)
          })
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return apiClient(originalRequest)
        } catch {
          useAuthStore.getState().logout()
          window.location.href = '/login'
          return Promise.reject(error)
        }
      }
      
      // Try to refresh
      isRefreshing = true
      try {
        const newToken = await performTokenRefresh()
        if (newToken) {
          onTokenRefreshed(newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return apiClient(originalRequest)
        } else {
          onRefreshFailed()
          useAuthStore.getState().logout()
          window.location.href = '/login'
          return Promise.reject(error)
        }
      } catch (refreshError) {
        onRefreshFailed()
        useAuthStore.getState().logout()
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    
    // Extract backend error message from response body
    if (error.response?.data) {
      const data = error.response.data as Record<string, unknown>
      const backendMessage = data.message as string | undefined
      if (backendMessage) {
        error.message = backendMessage
      }
    }

    return Promise.reject(error)
  }
)

export default apiClient
