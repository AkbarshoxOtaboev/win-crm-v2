import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface DiscountRule {
  roleId: number
  roleName: string
  /** null - cheklanmagan. */
  maxDiscountPercent: number | null
  locked: boolean
}

export interface DiscountRuleItem {
  roleId: number
  maxDiscountPercent: number | null
}

export function fetchDiscountRules() {
  return apiRequest<RestApiResponse<DiscountRule[]>>('/api/discount-rules')
}

export function saveDiscountRules(rules: DiscountRuleItem[]) {
  return apiRequest<RestApiResponse<DiscountRule[]>>('/api/discount-rules', {
    method: 'PUT',
    body: { rules },
  })
}

export function fetchMyDiscountLimit() {
  return apiRequest<RestApiResponse<{ maxDiscountPercent: number | null }>>('/api/discount-rules/me')
}
