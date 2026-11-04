import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchGoods, type Goods } from '@/api/goods'
import { fetchStocks, type Stock } from '@/api/stocks'

export interface StockAlertRow {
  goodsId: number
  name: string
  unit?: string
  qty: number
  qtyLabel: string
  minQty: number
  minLabel: string
  warehouses: string
  isWindow?: boolean
}

function amt(v?: number | null) {
  return Number(v || 0)
}

function formatQty(v: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v || 0))
}

function stockQty(s: Stock) {
  const pieces = Number(s.pieceCount)
  if (Number.isFinite(pieces) && pieces > 0) return pieces
  return amt(s.count)
}

function stockKvm(s: Stock) {
  if ((s.goodsType || '').toUpperCase() === 'WINDOW') return amt(s.kvm ?? s.count)
  return 0
}

function stockPieces(s: Stock) {
  const pieces = Number(s.pieceCount)
  if (Number.isFinite(pieces)) return pieces
  return amt(s.count)
}

function aggregateByGoods(stocks: Stock[]) {
  const byGoods = new Map<
    number,
    { qty: number; kvm: number; pieces: number; warehouses: Set<string>; name: string; unit?: string }
  >()
  for (const s of stocks) {
    const id = Number(s.goodsId || 0)
    if (!id) continue
    const cur = byGoods.get(id) || {
      qty: 0,
      kvm: 0,
      pieces: 0,
      warehouses: new Set<string>(),
      name: s.goodsName || '—',
      unit: s.unitTypeName,
    }
    cur.qty += stockQty(s)
    cur.kvm += stockKvm(s)
    cur.pieces += stockPieces(s)
    if (s.warehouseName) cur.warehouses.add(s.warehouseName)
    byGoods.set(id, cur)
  }
  return byGoods
}

/** Ombor qoldiqlaridan «qolmagan» va «kam qolgan» ro‘yxatlarni hisoblaydi. */
export function useStockAlerts() {
  const { t } = useI18n()
  const outOfStockRows = ref<StockAlertRow[]>([])
  const lowStockRows = ref<StockAlertRow[]>([])
  const loading = ref(false)

  function formatQtyPair(kvm: number, pieces: number, isWindow: boolean) {
    if (!isWindow) return formatQty(pieces || kvm)
    return `${formatQty(kvm)} ${t('stock.kvm')}\n${formatQty(pieces)} ${t('goods.minPiecesUnit')}`
  }

  function formatMinPair(g: Goods, isWindow: boolean) {
    if (!isWindow) {
      return g.minQuantity != null ? formatQty(Number(g.minQuantity)) : '—'
    }
    const parts: string[] = []
    if (g.minQuantity != null) parts.push(`${formatQty(Number(g.minQuantity))} ${t('stock.kvm')}`)
    if (g.minPieceQuantity != null) parts.push(`${formatQty(Number(g.minPieceQuantity))} ${t('goods.minPiecesUnit')}`)
    return parts.length ? parts.join('\n') : '—'
  }

  function buildOutOfStock(stocks: Stock[], goods: Goods[]): StockAlertRow[] {
    const byGoods = aggregateByGoods(stocks)
    const goodsMap = new Map(goods.map((g) => [g.id, g]))
    const rows: StockAlertRow[] = []
    for (const [goodsId, agg] of byGoods) {
      if (agg.qty > 0 || agg.kvm > 0 || agg.pieces > 0) continue
      const g = goodsMap.get(goodsId)
      const isWindow = (g?.type || '').toUpperCase() === 'WINDOW'
      rows.push({
        goodsId,
        name: agg.name || g?.name || '—',
        unit: agg.unit || g?.unitTypeName,
        qty: 0,
        qtyLabel: formatQtyPair(0, 0, isWindow),
        minQty: Number(g?.minQuantity ?? 0),
        minLabel: g ? formatMinPair(g, isWindow) : '—',
        warehouses: [...agg.warehouses].join(', '),
        isWindow,
      })
    }
    return rows.sort((a, b) => a.name.localeCompare(b.name))
  }

  function buildLowStock(stocks: Stock[], goods: Goods[]): StockAlertRow[] {
    const byGoods = aggregateByGoods(stocks)
    const rows: StockAlertRow[] = []
    for (const g of goods) {
      const isWindow = (g.type || '').toUpperCase() === 'WINDOW'
      const agg = byGoods.get(g.id) || {
        qty: 0,
        kvm: 0,
        pieces: 0,
        warehouses: new Set<string>(),
        name: g.name || '—',
        unit: g.unitTypeName,
      }
      let low = false
      if (isWindow) {
        const minKvm = g.minQuantity != null ? Number(g.minQuantity) : null
        const minPieces = g.minPieceQuantity != null ? Number(g.minPieceQuantity) : null
        const byKvm = minKvm != null && Number.isFinite(minKvm) && agg.kvm > 0 && agg.kvm <= minKvm
        const byPieces =
          minPieces != null && Number.isFinite(minPieces) && agg.pieces > 0 && agg.pieces <= minPieces
        low = byKvm || byPieces
      } else if (g.minQuantity != null) {
        const minQty = Number(g.minQuantity)
        low = Number.isFinite(minQty) && minQty >= 0 && agg.qty > 0 && agg.qty <= minQty
      }
      if (!low) continue
      rows.push({
        goodsId: g.id,
        name: agg.name || g.name || '—',
        unit: agg.unit || g.unitTypeName,
        qty: isWindow ? agg.kvm : agg.qty,
        qtyLabel: formatQtyPair(agg.kvm, agg.pieces, isWindow),
        minQty: Number(g.minQuantity ?? g.minPieceQuantity ?? 0),
        minLabel: formatMinPair(g, isWindow),
        warehouses: [...agg.warehouses].join(', '),
        isWindow,
      })
    }
    return rows.sort((a, b) => a.qty - b.qty || a.name.localeCompare(b.name))
  }

  async function loadStockAlerts() {
    loading.value = true
    try {
      const [stockRes, goodsRes] = await Promise.all([fetchStocks(), fetchGoods()])
      const stocks = (stockRes.data || []).filter((s) => (s.goodsType || '').toUpperCase() !== 'SERVICE')
      const goods = (goodsRes.data || []).filter((g) => (g.type || '').toUpperCase() !== 'SERVICE')
      outOfStockRows.value = buildOutOfStock(stocks, goods)
      lowStockRows.value = buildLowStock(stocks, goods)
      return { stocks, goods }
    } finally {
      loading.value = false
    }
  }

  return {
    outOfStockRows,
    lowStockRows,
    loading,
    loadStockAlerts,
    buildOutOfStock,
    buildLowStock,
  }
}
