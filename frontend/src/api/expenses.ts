import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'
import type { CurrencyCode } from '@/utils/currency'

export interface ExpenseCategory {
  id: number
  name: string
  description?: string
  status?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: number
}

export interface ExpenseCategoryPayload {
  name: string
  description?: string
}

export interface Expense {
  id: number
  categoryId: number
  categoryName?: string
  amount: number
  /** amount valyutasi (kassa valyutasi). */
  currency?: CurrencyCode
  exchangeRate?: number | null
  paymentTypeId?: number | null
  paymentTypeName?: string | null
  expenseDate: string
  description?: string
  status?: string
  createdAt?: string
  updatedAt?: string
  createdBy?: number
  supplierPaymentId?: number | null
}

export interface ExpensePayload {
  categoryId: number
  amount: number
  expenseDate: string
  description?: string
  /** Pul chiqadigan kassa; xarajat valyutasi shu kassa valyutasi. */
  paymentTypeId: number
}

/** Kassadagi joriy qoldiq o'z valyutasida. */
export interface CashAvailable {
  paymentTypeId: number
  paymentTypeName: string
  paymentTypeIcon?: string | null
  currency: CurrencyCode
  available: number
}

export function fetchCashAvailable() {
  return apiRequest<RestApiResponse<CashAvailable[]>>('/api/expenses/cash-available')
}

export function fetchExpenseCategories(page = 0, size = 100) {
  return apiRequest<RestApiResponse<SpringPage<ExpenseCategory>>>(
    `/api/expense-categories?page=${page}&size=${size}`,
  )
}

export function createExpenseCategory(payload: ExpenseCategoryPayload) {
  return apiRequest<RestApiResponse<ExpenseCategory>>('/api/expense-categories/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateExpenseCategory(id: number, payload: ExpenseCategoryPayload) {
  return apiRequest<RestApiResponse<ExpenseCategory>>(`/api/expense-categories/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteExpenseCategory(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/expense-categories/delete/${id}`, {
    method: 'DELETE',
  })
}

export function fetchExpenses(page = 0, size = 100) {
  return apiRequest<RestApiResponse<SpringPage<Expense>>>(
    `/api/expenses?page=${page}&size=${size}`,
  )
}

export function createExpense(payload: ExpensePayload) {
  return apiRequest<RestApiResponse<Expense>>('/api/expenses/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateExpense(id: number, payload: ExpensePayload) {
  return apiRequest<RestApiResponse<Expense>>(`/api/expenses/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deleteExpense(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/expenses/delete/${id}`, {
    method: 'DELETE',
  })
}
