export const SALE_STATUSES = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'READY',
  'IN_DELIVERY',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
] as const

export type SaleStatus = (typeof SALE_STATUSES)[number]

/** Must stay in sync with backend SalesOrderStatus.TRANSITIONS. */
const TRANSITIONS: Record<SaleStatus, SaleStatus[]> = {
  NEW: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'READY', 'CANCELLED'],
  PROCESSING: ['READY', 'CANCELLED'],
  READY: ['IN_DELIVERY', 'DELIVERED', 'CANCELLED'],
  IN_DELIVERY: ['DELIVERED', 'CANCELLED'],
  DELIVERED: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

export function nextSaleStatuses(status?: string | null): SaleStatus[] {
  if (!status || !(status in TRANSITIONS)) return []
  return TRANSITIONS[status as SaleStatus]
}
