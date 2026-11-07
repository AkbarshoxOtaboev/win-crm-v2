import { apiRequest } from './http'
import type { RestApiResponse } from './types'

/** Lengths in cm, areas in m². */
export interface CuttingRemnant {
  x: number
  y: number
  width: number
  height: number
  area: number
  useful: boolean
  /** How many more of the same pieces fit into this remnant */
  fitsPieces: number
}

export interface CuttingPlacedPiece {
  x: number
  y: number
  width: number
  height: number
}

export interface CuttingSheetLayout {
  /** Number of sheets cut with this layout */
  count: number
  pieces: number
  columns: number
  rows: number
  lastRowPieces: number
  usedWidth: number
  usedHeight: number
  remnants: CuttingRemnant[]
  /** Pieces cut inside a remnant, in a different orientation from the main grid */
  placed?: CuttingPlacedPiece[]
}

export interface CuttingResult {
  sheetWidth: number
  sheetHeight: number
  pieceWidth: number
  pieceHeight: number
  quantity: number
  kerfMm: number
  minRemnantCm: number
  minRemnantWidthCm?: number
  minRemnantHeightCm?: number
  rotated: boolean
  layoutPieceWidth: number
  layoutPieceHeight: number
  columns: number
  rows: number
  perSheet: number
  sheetsNeeded: number
  sheetArea: number
  sheetsArea: number
  piecesArea: number
  leftoverArea: number
  usefulArea: number
  scrapArea: number
  usagePercent: number
  leftoverPercent: number
  scrapPercent: number
  sheets: CuttingSheetLayout[]
}

export interface CuttingPayload {
  goodsId?: number
  sheetWidth?: number
  sheetHeight?: number
  pieceWidth: number
  pieceHeight: number
  quantity: number
  kerfMm?: number
  allowRotate?: boolean
  minRemnantCm?: number
  minRemnantWidthCm?: number
  minRemnantHeightCm?: number
  maxScrapPercent?: number
  wasteWidthCm?: number
  wasteHeightCm?: number
}

export function calculateCutting(body: CuttingPayload) {
  return apiRequest<RestApiResponse<CuttingResult>>('/api/cutting/calculate', { method: 'POST', body })
}

export interface OptimizeRemnant {
  id: number
  goodsId: number
  goodsName?: string
  width: number
  height: number
  pieceCount: number
  warehouseName?: string | null
  sourceSaleOrderId?: number | null
}

export interface OptimizePick {
  remnantId?: number | null
  goodsId?: number | null
  quantity: number
  width?: number
  height?: number
  /** Shu buyurtma kesishidan chiqadigan, omborda hali yo'q qoldiq. */
  planned?: boolean
}

export interface SheetOption {
  goodsId: number
  goodsName?: string
  width?: number
  height?: number
}

export interface SheetUse {
  goodsId: number
  goodsName?: string
  width?: number
  height?: number
  quantity: number
}

export interface OptimizeLine {
  itemId: number
  goodsId: number
  goodsName?: string
  width: number
  height: number
  count: number
  kerfMm?: number | null
  minRemnantCm?: number | null
  allowRotate?: boolean | null
  sheetQuantity?: number | null
  candidateGoodsIds?: number[]
  candidates?: SheetOption[]
  sheetUses?: SheetUse[]
  picks?: OptimizePick[]
}

export interface OptimizePlan {
  saleOrderId: number
  warehouseId?: number | null
  warehouseName?: string | null
  optimized: boolean
  kerfMm?: number | null
  minRemnantCm?: number | null
  minRemnantWidthCm?: number | null
  minRemnantHeightCm?: number | null
  source?: 'REMNANT' | 'SHEET' | null
  lines: OptimizeLine[]
  remnants: OptimizeRemnant[]
}

export function fetchCuttingOptimize(saleOrderId: number) {
  return apiRequest<RestApiResponse<OptimizePlan>>(`/api/cutting/optimize/${saleOrderId}`)
}

export function suggestCuttingOptimize(
  saleOrderId: number,
  body: {
    kerfMm: number
    minRemnantCm?: number
    minRemnantWidthCm?: number
    minRemnantHeightCm?: number
    source?: 'REMNANT' | 'SHEET'
    allowRotate?: boolean
    maxScrapPercent?: number
    wasteWidthCm?: number
    wasteHeightCm?: number
  },
) {
  return apiRequest<RestApiResponse<OptimizePlan>>(`/api/cutting/optimize/${saleOrderId}/suggest`, {
    method: 'POST',
    body,
  })
}

export function fetchGlassRemnants() {
  return apiRequest<RestApiResponse<OptimizeRemnant[]>>('/api/cutting/remnants')
}

export function deleteGlassRemnant(id: number) {
  return apiRequest<void>(`/api/cutting/remnants/${id}`, { method: 'DELETE' })
}

export function deleteGlassRemnants(ids: number[]) {
  return apiRequest<void>('/api/cutting/remnants/delete', {
    method: 'POST',
    body: { ids },
  })
}

export interface ManualLayout {
  layout: string | null
  savedAt?: string | null
  hasPrevious: boolean
}

export function fetchManualLayout(saleOrderId: number) {
  return apiRequest<RestApiResponse<ManualLayout>>(`/api/cutting/optimize/${saleOrderId}/manual`)
}

export function saveManualLayout(saleOrderId: number, layout: string) {
  return apiRequest<RestApiResponse<ManualLayout>>(`/api/cutting/optimize/${saleOrderId}/manual`, {
    method: 'PUT',
    body: { layout },
  })
}

export function restoreManualLayout(saleOrderId: number) {
  return apiRequest<RestApiResponse<ManualLayout>>(`/api/cutting/optimize/${saleOrderId}/manual/restore`, {
    method: 'POST',
  })
}

export function confirmCuttingOptimize(
  saleOrderId: number,
  body: {
    kerfMm: number
    minRemnantCm: number
    allowRotate?: boolean
    lines: {
      saleOrderItemId: number
      sheetQuantity: number
      sheets: { goodsId: number; quantity: number }[]
      remnants: { remnantId: number; quantity: number }[]
      planned: { goodsId?: number; width: number; height: number; quantity: number }[]
    }[]
  },
) {
  return apiRequest<RestApiResponse<OptimizePlan>>(`/api/cutting/optimize/${saleOrderId}`, {
    method: 'POST',
    body,
  })
}
