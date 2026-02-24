import apiClient from './api'
import type {
  ApiResponse,
  OrganizationSignupRequest,
  SignupResponse,
  Subscription,
  SubscriptionPlanInfo,
  UpgradeSubscriptionRequest,
  MicrosoftConfig,
  UpdateMicrosoftConfigRequest,
  ErpConfig,
  UpdateErpConfigRequest
} from '@/types'

/**
 * Service for organization signup (public, no auth required)
 */
export const signupService = {
  /**
   * Register a new organization with 15-day trial
   */
  async signup(data: OrganizationSignupRequest): Promise<SignupResponse> {
    const response = await apiClient.post<ApiResponse<SignupResponse>>(
      '/v1/signup',
      data
    )
    return response.data.data!
  },

  /**
   * Check if email is available
   */
  async checkEmailAvailability(email: string): Promise<boolean> {
    const response = await apiClient.get<ApiResponse<boolean>>(
      `/v1/signup/check-email?email=${encodeURIComponent(email)}`
    )
    return response.data.data!
  }
}

/**
 * Service for subscription management
 */
export const subscriptionService = {
  /**
   * Get current organization's subscription
   */
  async getCurrentSubscription(): Promise<Subscription> {
    const response = await apiClient.get<ApiResponse<Subscription>>(
      '/v1/subscriptions/current'
    )
    return response.data.data!
  },

  /**
   * Get available plans
   */
  async getAvailablePlans(): Promise<SubscriptionPlanInfo[]> {
    const response = await apiClient.get<ApiResponse<SubscriptionPlanInfo[]>>(
      '/v1/subscriptions/plans'
    )
    return response.data.data!
  },

  /**
   * Upgrade subscription
   */
  async upgradeSubscription(data: UpgradeSubscriptionRequest): Promise<Subscription> {
    const response = await apiClient.post<ApiResponse<Subscription>>(
      '/v1/subscriptions/upgrade',
      data
    )
    return response.data.data!
  },

  /**
   * Cancel subscription
   */
  async cancelSubscription(reason?: string): Promise<Subscription> {
    const response = await apiClient.post<ApiResponse<Subscription>>(
      `/v1/subscriptions/cancel${reason ? `?reason=${encodeURIComponent(reason)}` : ''}`
    )
    return response.data.data!
  },

  /**
   * Get subscription for specific org (Super Admin)
   */
  async getSubscriptionByOrg(orgId: number): Promise<Subscription> {
    const response = await apiClient.get<ApiResponse<Subscription>>(
      `/v1/subscriptions/admin/${orgId}`
    )
    return response.data.data!
  },

  /**
   * Reactivate subscription (Super Admin)
   */
  async reactivateSubscription(orgId: number, additionalDays = 15): Promise<Subscription> {
    const response = await apiClient.post<ApiResponse<Subscription>>(
      `/v1/subscriptions/admin/${orgId}/reactivate?additionalDays=${additionalDays}`
    )
    return response.data.data!
  },

  /**
   * Change an org's plan directly (Super Admin, bypasses Stripe)
   */
  async adminChangePlan(orgId: number, plan: string, billingCycle?: string, durationDays?: number): Promise<Subscription> {
    const response = await apiClient.put<ApiResponse<Subscription>>(
      `/v1/subscriptions/admin/${orgId}/plan`,
      { plan, billingCycle: billingCycle || 'MONTHLY', durationDays: durationDays || 365 }
    )
    return response.data.data!
  }
}

/**
 * Service for Microsoft Graph configuration
 */
export const microsoftConfigService = {
  /**
   * Get current Microsoft config (secrets masked)
   */
  async getMicrosoftConfig(): Promise<MicrosoftConfig> {
    const response = await apiClient.get<ApiResponse<MicrosoftConfig>>(
      '/v1/organization-settings/microsoft-config'
    )
    return response.data.data!
  },

  /**
   * Update Microsoft credentials
   */
  async updateMicrosoftConfig(data: UpdateMicrosoftConfigRequest): Promise<MicrosoftConfig> {
    const response = await apiClient.put<ApiResponse<MicrosoftConfig>>(
      '/v1/organization-settings/microsoft-config',
      data
    )
    return response.data.data!
  },

  /**
   * Verify Microsoft credentials
   */
  async verifyMicrosoftConfig(): Promise<MicrosoftConfig> {
    const response = await apiClient.post<ApiResponse<MicrosoftConfig>>(
      '/v1/organization-settings/microsoft-config/verify'
    )
    return response.data.data!
  },

  /**
   * Update Microsoft config for specific org (Super Admin)
   */
  async updateMicrosoftConfigForOrg(orgId: number, data: UpdateMicrosoftConfigRequest): Promise<MicrosoftConfig> {
    const response = await apiClient.put<ApiResponse<MicrosoftConfig>>(
      `/v1/organization-settings/admin/${orgId}/microsoft-config`,
      data
    )
    return response.data.data!
  }
}

/**
 * Service for ERP integration configuration
 */
export const erpConfigService = {
  /**
   * Get current ERP config (API key masked)
   */
  async getErpConfig(): Promise<ErpConfig> {
    const response = await apiClient.get<ApiResponse<ErpConfig>>(
      '/v1/organization-settings/erp-config'
    )
    return response.data.data!
  },

  /**
   * Update ERP integration settings
   */
  async updateErpConfig(data: UpdateErpConfigRequest): Promise<ErpConfig> {
    const response = await apiClient.put<ApiResponse<ErpConfig>>(
      '/v1/organization-settings/erp-config',
      data
    )
    return response.data.data!
  }
}

/**
 * Service for onboarding flow
 */
export const onboardingService = {
  /**
   * Complete onboarding – transitions org from ONBOARDING to TRIAL/ACTIVE
   */
  async completeOnboarding(): Promise<void> {
    await apiClient.post<ApiResponse<void>>(
      '/v1/organization-settings/complete-onboarding'
    )
  }
}
