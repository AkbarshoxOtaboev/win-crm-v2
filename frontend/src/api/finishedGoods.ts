import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export type FinishedGoodsDocStatus = 'DRAFT' | 'POSTED'

export interface MaterialIssueItem {
  id?: number
  goodsId: number
  goodsName?: string
  goodsType?: string
  count: number
  pieceCount?: number | null
}

export interface MaterialIssue {
  id: number
  warehouseId: number
  warehouseName?: string
  workshopId?: number | null
  workshopName?: string | null
  comment?: string | null
  documentDate: string
  docStatus: FinishedGoodsDocStatus
  createdAt?: string
  createdUsername?: string
  items: MaterialIssueItem[]
}

export interface MaterialIssuePayload {
  warehouseId: number
  workshopId?: number | null
  comment?: string
  documentDate: string
  items: { goodsId: number; count: number; pieceCount?: number | null }[]
}

export interface FinishedGoodsReceiptItem {
  id?: number
  goodsId: number
  goodsName?: string
  count: number
  unitCost?: number | null
}

export interface FinishedGoodsReceipt {
  id: number
  warehouseId: number
  warehouseName?: string
  workshopId?: number | null
  workshopName?: string | null
  comment?: string | null
  documentDate: string
  docStatus: FinishedGoodsDocStatus
  createdAt?: string
  createdUsername?: string
  items: FinishedGoodsReceiptItem[]
}

export interface FinishedGoodsReceiptPayload {
  warehouseId: number
  workshopId?: number | null
  comment?: string
  documentDate: string
  items: { goodsId: number; count: number; unitCost?: number | null }[]
}

export function fetchMaterialIssues() {
  return apiRequest<RestApiResponse<MaterialIssue[]>>('/api/material-issues')
}

export function createMaterialIssue(payload: MaterialIssuePayload) {
  return apiRequest<RestApiResponse<MaterialIssue>>('/api/material-issues', {
    method: 'POST',
    body: payload,
  })
}

export function updateMaterialIssue(id: number, payload: MaterialIssuePayload) {
  return apiRequest<RestApiResponse<MaterialIssue>>(`/api/material-issues/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function postMaterialIssue(id: number) {
  return apiRequest<RestApiResponse<MaterialIssue>>(`/api/material-issues/${id}/post`, {
    method: 'POST',
  })
}

export function deleteMaterialIssue(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/material-issues/${id}`, {
    method: 'DELETE',
  })
}

export function fetchFinishedGoodsReceipts() {
  return apiRequest<RestApiResponse<FinishedGoodsReceipt[]>>('/api/finished-goods-receipts')
}

export function createFinishedGoodsReceipt(payload: FinishedGoodsReceiptPayload) {
  return apiRequest<RestApiResponse<FinishedGoodsReceipt>>('/api/finished-goods-receipts', {
    method: 'POST',
    body: payload,
  })
}

export function updateFinishedGoodsReceipt(id: number, payload: FinishedGoodsReceiptPayload) {
  return apiRequest<RestApiResponse<FinishedGoodsReceipt>>(`/api/finished-goods-receipts/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function postFinishedGoodsReceipt(id: number) {
  return apiRequest<RestApiResponse<FinishedGoodsReceipt>>(`/api/finished-goods-receipts/${id}/post`, {
    method: 'POST',
  })
}

export function deleteFinishedGoodsReceipt(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/finished-goods-receipts/${id}`, {
    method: 'DELETE',
  })
}
