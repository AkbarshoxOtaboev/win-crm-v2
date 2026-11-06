export const SALE_STATUSES = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'READY',
  'IN_DELIVERY',
  'DELIVERED',
  'WORK_DONE',
  'COMPLETED',
  'CANCELLED',
] as const

export type SaleStatus = (typeof SALE_STATUSES)[number]

/** Asosiy jarayon tartibi (CANCELLED alohida). Backend SalesOrderStatus.MAIN_FLOW bilan bir xil. */
export const SALE_MAIN_FLOW: SaleStatus[] = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'READY',
  'IN_DELIVERY',
  'DELIVERED',
  'WORK_DONE',
  'COMPLETED',
]

/** Must stay in sync with backend SalesOrderStatus.TRANSITIONS. */
const TRANSITIONS: Record<SaleStatus, SaleStatus[]> = {
  NEW: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PROCESSING', 'READY', 'CANCELLED'],
  PROCESSING: ['READY', 'CANCELLED'],
  READY: ['IN_DELIVERY', 'DELIVERED', 'CANCELLED'],
  IN_DELIVERY: ['DELIVERED', 'CANCELLED'],
  DELIVERED: ['WORK_DONE', 'CANCELLED'],
  WORK_DONE: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
}

export function nextSaleStatuses(status?: string | null): SaleStatus[] {
  if (!status || !(status in TRANSITIONS)) return []
  return TRANSITIONS[status as SaleStatus]
}

export function isFinalSaleStatus(status?: string | null): boolean {
  return status === 'COMPLETED' || status === 'CANCELLED'
}

/** Oldinga yoki orqaga (MAIN_FLOW ichida) o'tish mumkinmi. */
export function canChangeSaleStatus(from?: string | null, to?: string | null): boolean {
  if (!from || !to || from === to || isFinalSaleStatus(from)) return false
  if (!(from in TRANSITIONS) || !(to in TRANSITIONS)) return false
  const src = from as SaleStatus
  const dest = to as SaleStatus
  if (TRANSITIONS[src].includes(dest)) return true
  const fromIdx = SALE_MAIN_FLOW.indexOf(src)
  const toIdx = SALE_MAIN_FLOW.indexOf(dest)
  return fromIdx > 0 && toIdx >= 0 && toIdx < fromIdx
}
