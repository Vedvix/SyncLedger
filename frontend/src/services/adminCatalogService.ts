import apiClient from './api'
import type {
  ApiResponse,
  PlanDefinition,
  PlanDefinitionRequest,
  Coupon,
  CouponRequest,
  CouponValidationResponse,
} from '@/types'

/**
 * Service for Super Admin plan definition management.
 */
export const planDefinitionService = {
  /** Public: get active plans for pricing page */
  async getActivePlans(): Promise<PlanDefinition[]> {
    const response = await apiClient.get<ApiResponse<PlanDefinition[]>>(
      '/v1/plan-definitions/active'
    )
    return response.data.data!
  },

  /** Admin: get ALL plans (including inactive) */
  async getAllPlans(): Promise<PlanDefinition[]> {
    const response = await apiClient.get<ApiResponse<PlanDefinition[]>>(
      '/v1/plan-definitions'
    )
    return response.data.data!
  },

  /** Admin: get single plan */
  async getPlan(id: number): Promise<PlanDefinition> {
    const response = await apiClient.get<ApiResponse<PlanDefinition>>(
      `/v1/plan-definitions/${id}`
    )
    return response.data.data!
  },

  /** Admin: create plan */
  async createPlan(data: PlanDefinitionRequest): Promise<PlanDefinition> {
    const response = await apiClient.post<ApiResponse<PlanDefinition>>(
      '/v1/plan-definitions',
      data
    )
    return response.data.data!
  },

  /** Admin: update plan */
  async updatePlan(id: number, data: PlanDefinitionRequest): Promise<PlanDefinition> {
    const response = await apiClient.put<ApiResponse<PlanDefinition>>(
      `/v1/plan-definitions/${id}`,
      data
    )
    return response.data.data!
  },

  /** Admin: deactivate (soft-delete) plan */
  async deletePlan(id: number): Promise<void> {
    await apiClient.delete(`/v1/plan-definitions/${id}`)
  },
}

/**
 * Service for Super Admin coupon management.
 */
export const couponService = {
  /** Admin: get all coupons */
  async getAllCoupons(): Promise<Coupon[]> {
    const response = await apiClient.get<ApiResponse<Coupon[]>>('/v1/coupons')
    return response.data.data!
  },

  /** Admin: get single coupon */
  async getCoupon(id: number): Promise<Coupon> {
    const response = await apiClient.get<ApiResponse<Coupon>>(
      `/v1/coupons/${id}`
    )
    return response.data.data!
  },

  /** Admin: create coupon */
  async createCoupon(data: CouponRequest): Promise<Coupon> {
    const response = await apiClient.post<ApiResponse<Coupon>>(
      '/v1/coupons',
      data
    )
    return response.data.data!
  },

  /** Admin: update coupon */
  async updateCoupon(id: number, data: CouponRequest): Promise<Coupon> {
    const response = await apiClient.put<ApiResponse<Coupon>>(
      `/v1/coupons/${id}`,
      data
    )
    return response.data.data!
  },

  /** Admin: deactivate coupon */
  async deactivateCoupon(id: number): Promise<void> {
    await apiClient.delete(`/v1/coupons/${id}`)
  },

  /** Public: validate coupon at checkout */
  async validateCoupon(
    code: string,
    planKey: string,
    billingCycle: string = 'MONTHLY'
  ): Promise<CouponValidationResponse> {
    const response = await apiClient.post<
      ApiResponse<CouponValidationResponse>
    >(`/v1/coupons/validate?billingCycle=${billingCycle}`, {
      code,
      planKey,
    })
    return response.data.data!
  },
}
