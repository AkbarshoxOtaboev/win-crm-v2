import { apiRequest } from './http'
import type { RestApiResponse } from './types'
import type { CurrencyCode } from '@/utils/currency'
import type { CashSource } from './expenses'

export type ImportExpenseKind = 'ROAD' | 'DUTY'

export interface ImportExpense {
  id: number
  warehouseOrderId: number
  warehouseOrderItemId: number
  supplierName?: string
  orderArrivalDate?: string | null
  kind: ImportExpenseKind
  amount: number
  /** Shu kirim + tur + valyutadagi barcha to‘lovlar jamisi. */
  groupAmount?: number
  /** Shu guruh bo‘yicha taqsimlangan jami. */
  allocatedAmount?: number
  paymentCount?: number
  currency?: CurrencyCode
  exchangeRate?: number | null
  paymentTypeId?: number | null
  paymentTypeName?: string | null
  cashSource?: CashSource
  expenseDate: string
  description?: string | null
  positionGoodsName?: string
  positionWidth?: number | null
  positionHeight?: number | null
  positionPieces?: number | null
  positionQty?: number | null
  status?: string
}

export interface ImportExpensePayload {
  warehouseOrderId: number
  kind: ImportExpenseKind
  amount: number
  expenseDate: string
  description?: string
  paymentTypeId: number
  cashSource?: CashSource
}

export interface ImportExpenseRecorded {
  currency: CurrencyCode
  amount: number
}

export interface ImportExpenseOrderOption {
  id: number
  supplierName?: string
  arrivalDate?: string | null
}

export interface ImportExpenseSplitLine {
  itemId: number
  goodsName?: string
  width?: number | null
  height?: number | null
  pieces?: number | null
  qty?: number | null
  amount?: number | null
}

export interface ImportExpenseSplit {
  expenseId: number
  warehouseOrderId: number
  supplierName?: string
  kind: ImportExpenseKind
  /** Shu kirim + tur + valyutadagi barcha to‘lovlar jamisi. */
  amount: number
  allocatedAmount?: number
  paymentCount?: number
  currency?: CurrencyCode
  lines: ImportExpenseSplitLine[]
}

export interface ImportExpenseItemOption {
  id: number
  goodsName?: string
  width?: number | null
  height?: number | null
  pieceCount?: number | null
  count?: number | null
}

export function fetchImportExpenseOrders() {
  return apiRequest<RestApiResponse<ImportExpenseOrderOption[]>>('/api/import-expenses/orders')
}

export function fetchImportExpenseItems(orderId: number) {
  return apiRequest<RestApiResponse<ImportExpenseItemOption[]>>(`/api/import-expenses/orders/${orderId}/items`)
}

export function fetchImportExpenses(params: {
  startDate: string
  endDate: string
  warehouseOrderId?: number
  kind?: ImportExpenseKind
}) {
  const q = new URLSearchParams({
    startDate: params.startDate,
    endDate: params.endDate,
  })
  if (params.warehouseOrderId) q.set('warehouseOrderId', String(params.warehouseOrderId))
  if (params.kind) q.set('kind', params.kind)
  return apiRequest<RestApiResponse<ImportExpense[]>>(`/api/import-expenses?${q}`)
}

export function fetchImportExpenseRecorded(warehouseOrderItemId: number, kind: ImportExpenseKind, excludeId?: number) {
  const q = new URLSearchParams({
    warehouseOrderItemId: String(warehouseOrderItemId),
    kind,
  })
  if (excludeId) q.set('excludeId', String(excludeId))
  return apiRequest<RestApiResponse<ImportExpenseRecorded[]>>(`/api/import-expenses/recorded?${q}`)
}

export function createImportExpense(payload: ImportExpensePayload) {
  return apiRequest<RestApiResponse<ImportExpense>>('/api/import-expenses/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateImportExpense(id: number, payload: ImportExpensePayload) {
  return apiRequest<RestApiResponse<ImportExpense>>(`/api/import-expenses/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function fetchImportExpenseSplit(id: number) {
  return apiRequest<RestApiResponse<ImportExpenseSplit>>(`/api/import-expenses/${id}/split`)
}

export function saveImportExpenseSplit(id: number, lines: { warehouseOrderItemId: number; amount: number }[]) {
  return apiRequest<RestApiResponse<ImportExpenseSplit>>(`/api/import-expenses/${id}/split`, {
    method: 'PUT',
    body: { lines },
  })
}

export function deleteImportExpense(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/import-expenses/delete/${id}`, {
    method: 'DELETE',
  })
}
