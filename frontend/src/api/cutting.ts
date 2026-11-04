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
}

export interface CuttingResult {
  sheetWidth: number
  sheetHeight: number
  pieceWidth: number
  pieceHeight: number
  quantity: number
  kerfMm: number
  minRemnantCm: number
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
}

export interface OptimizePick {
  remnantId: number
  quantity: number
  width?: number
  height?: number
}

export interface OptimizeLine {
  itemId: number
  goodsId: number
  goodsName?: string
  width: number
  height: number
  count: number
  kerfMm?: number | null
  allowRotate?: boolean | null
  sheetQuantity?: number | null
  picks?: OptimizePick[]
}

export interface OptimizePlan {
  saleOrderId: number
  warehouseId?: number | null
  warehouseName?: string | null
  optimized: boolean
  lines: OptimizeLine[]
  remnants: OptimizeRemnant[]
}

export function fetchCuttingOptimize(saleOrderId: number) {
  return apiRequest<RestApiResponse<OptimizePlan>>(`/api/cutting/optimize/${saleOrderId}`)
}

export function confirmCuttingOptimize(
  saleOrderId: number,
  lines: { saleOrderItemId: number; sheetQuantity: number; remnants: { remnantId: number; quantity: number }[] }[],
) {
  return apiRequest<RestApiResponse<OptimizePlan>>(`/api/cutting/optimize/${saleOrderId}`, {
    method: 'POST',
    body: { lines },
  })
}
