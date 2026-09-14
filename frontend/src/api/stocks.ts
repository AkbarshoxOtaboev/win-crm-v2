import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface Stock {
  id: number
  goodsId?: number
  goodsName?: string
  warehouseId?: number
  warehouseName?: string
  count?: number
  status?: string
}

export interface StockTransferPayload {
  goodsId: number
  fromWarehouseId: number
  toWarehouseId: number
  count: number
  comment?: string
}

export interface StockTransfer {
  id: number
  goodsId?: number
  goodsName?: string
  fromWarehouseId?: number
  fromWarehouseName?: string
  toWarehouseId?: number
  toWarehouseName?: string
  count?: number
  comment?: string
  status?: string
}

export function fetchStocks() {
  return apiRequest<RestApiResponse<Stock[]>>('/api/stocks')
}

export function deleteStock(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/stocks/delete/${id}`, { method: 'DELETE' })
}

export function fetchStockTransfers() {
  return apiRequest<RestApiResponse<StockTransfer[]>>('/api/stock-transfers')
}

export function createStockTransfer(payload: StockTransferPayload) {
  return apiRequest<RestApiResponse<StockTransfer>>('/api/stock-transfers', {
    method: 'POST',
    body: payload,
  })
}

export function fetchStocksByWarehouse(warehouseId: number) {
  return apiRequest<RestApiResponse<Stock[]>>(`/api/stocks/by-warehouse/${warehouseId}`)
}

export function fetchStocksByGoods(goodsId: number) {
  return apiRequest<RestApiResponse<Stock[]>>(`/api/stocks/by-goods/${goodsId}`)
}

export interface StockHistory {
  id: number
  goodsId?: number
  goodsName?: string
  warehouseId?: number
  warehouseName?: string
  count?: number
  type?: string
  movementType?: string
  comment?: string
  createdAt?: string
  createdUsername?: string
}

export function fetchStockHistories() {
  return apiRequest<RestApiResponse<StockHistory[]>>('/api/stock-histories')
}

export function fetchStockHistoriesByWarehouse(warehouseId: number) {
  return apiRequest<RestApiResponse<StockHistory[]>>(
    `/api/stock-histories/by-warehouse/${warehouseId}`,
  )
}

export function fetchStockHistoriesByGoods(goodsId: number) {
  return apiRequest<RestApiResponse<StockHistory[]>>(`/api/stock-histories/by-goods/${goodsId}`)
}
