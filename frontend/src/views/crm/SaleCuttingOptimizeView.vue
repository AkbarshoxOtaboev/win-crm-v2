<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('cuttingOptimize.title')" />
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <router-link :to="`/sales/${orderId}`" class="ghost">{{ t('cuttingOptimize.backToOrder') }}</router-link>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <p v-if="plan" class="mb-2 text-sm text-gray-500">
      {{ t('cuttingOptimize.order', { id: plan.saleOrderId }) }}
      <template v-if="plan.warehouseName"> · {{ plan.warehouseName }}</template>
    </p>
    <p class="mb-4 text-sm text-gray-500">{{ t('cuttingOptimize.subtitle') }}</p>

    <section v-if="rows.length" class="summary mb-4">
      <div class="summary-head">
        <div>
          <h3 class="text-base font-semibold text-gray-800 dark:text-white/90">{{ t('cuttingOptimize.summaryTitle') }}</h3>
          <p v-if="manualSaved" class="summary-note">
            ✓ {{ t('cutEditor.savedInfo', { date: fmtDate(manual?.savedAt), pct: manualSaved.pct }) }}
          </p>
          <p v-else-if="!editorSources.ready" class="summary-note muted">{{ t('cutEditor.notReady') }}</p>
        </div>
        <div class="summary-actions">
          <button v-if="manual?.hasPrevious" type="button" class="ghost" @click="restorePrevious">
            ↺ {{ t('cutEditor.restorePrevious') }}
          </button>
          <button
            type="button"
            class="btn"
            :disabled="!editorSources.ready"
            :title="t('cutEditor.openHint')"
            @click="openEditor"
          >
            ✎ {{ t('cutEditor.open') }}
          </button>
        </div>
      </div>
      <div class="stat-grid">
        <div class="stat">
          <span class="stat-label">{{ t('cuttingOptimize.statSheets') }}</span>
          <strong class="stat-value">{{ stats.sheets }}</strong>
          <span v-if="stats.remnantBoards" class="stat-sub">{{ t('cuttingOptimize.statSheetsSub', { n: stats.remnantBoards }) }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">{{ t('cuttingOptimize.statFromSheets') }}</span>
          <strong class="stat-value">{{ totals.sheetPieces }} <small>{{ t('cuttingOptimize.pcs') }}</small></strong>
          <span class="stat-sub">{{ fmtArea(totals.sheetArea) }} m²</span>
        </div>
        <div class="stat">
          <span class="stat-label">{{ t('cuttingOptimize.statFromRemnants') }}</span>
          <strong class="stat-value">{{ totals.remnantPieces }} <small>{{ t('cuttingOptimize.pcs') }}</small></strong>
          <span class="stat-sub">{{ fmtArea(totals.remnantArea) }} m²</span>
        </div>
        <div class="stat" :class="stats.usage >= 75 ? 'good' : stats.usage >= 50 ? 'warn' : 'bad'">
          <span class="stat-label">{{ t('cuttingOptimize.statUsage') }}</span>
          <strong class="stat-value">{{ stats.usage.toFixed(1) }}%</strong>
          <span class="stat-bar"><i :style="{ width: `${Math.min(stats.usage, 100)}%` }" /></span>
        </div>
        <div class="stat good">
          <span class="stat-label">{{ t('cuttingOptimize.statUseful') }}</span>
          <strong class="stat-value">{{ fmtArea(stats.useful) }} <small>m²</small></strong>
          <span class="stat-sub">{{ t('cuttingOptimize.statUsefulSub', { n: stats.usefulCount }) }}</span>
        </div>
        <div class="stat bad">
          <span class="stat-label">{{ t('cuttingOptimize.statScrap') }}</span>
          <strong class="stat-value">{{ fmtArea(stats.scrap) }} <small>m² · {{ stats.scrapPct.toFixed(1) }}%</small></strong>
          <span class="stat-sub">{{ t('cuttingOptimize.statScrapSub') }}</span>
        </div>
      </div>
    </section>

    <ManualCutEditor
      v-if="editor"
      :order-id="orderId"
      :initial="editor.initial"
      :plan-layout="editor.plan"
      :expected="editor.expected"
      :allowed-goods="editor.allowedGoods"
      :sheet-options="editor.sheetOptions"
      :min-remnant-width-cm="Number(edgeWidthCm) || 0"
      :min-remnant-height-cm="Number(edgeHeightCm) || 0"
      :plan-revision="editor.planRevision"
      @saved="(v) => (manual = v)"
      @close="editor = null"
      @replan="onReplan"
    />


    <div v-if="plan && rows.length === 0" class="card p-5 text-sm text-gray-500">
      {{ t('cuttingOptimize.noWindows') }}
    </div>

    <div v-if="rows.length" class="card mb-4 p-5">
      <div class="mb-3 flex flex-wrap items-baseline justify-between gap-2">
        <h3 class="text-base font-semibold text-gray-800 dark:text-white/90">{{ t('cutEditor.boardsTitle') }}</h3>
        <span class="text-xs text-gray-400">{{ t('cutEditor.boardsHint') }}</span>
      </div>
      <CutBoardsView
        v-if="viewLayout"
        :layout="viewLayout"
        :item-order="rows.map((r) => r.itemId)"
        :item-labels="itemLabels"
        :min-remnant-width-cm="Number(edgeWidthCm) || 0"
        :min-remnant-height-cm="Number(edgeHeightCm) || 0"
      />
      <p v-else class="text-sm text-gray-400">{{ t('cutEditor.notReady') }}</p>
    </div>

    <div class="calc-previews" aria-hidden="true">
    <div v-for="row in rows" :key="row.itemId">
      <div class="cut-sequence">
      <template v-for="(use, idx) in row.sheets" :key="`plan-${row.itemId}-${idx}`">
        <CuttingPreview
          v-if="use.width && use.height && (Number(use.quantity) > 0 || !locked)"
          :goods-id="use.goodsId"
          :sheet-width="use.width"
          :sheet-height="use.height"
          :piece-width="previewWidth(row)"
          :piece-height="previewHeight(row)"
          :quantity="Number(use.quantity)"
          :min-remnant-width-cm="edgeWidthCm"
          :min-remnant-height-cm="edgeHeightCm"
          :max-scrap-percent="maxScrapPercent"
          :waste-width-cm="wasteWidthCm"
          :waste-height-cm="wasteHeightCm"
          :movable="!locked"
          :editable="!locked"
          :item-id="row.itemId"
          source="sheet"
          :source-index="idx"
          :stamp="planStamp"
          :force-flip="row.forceFlip"
          :can-remove="!locked && row.sheets.length > 1"
          :sheet-options="sheetOptionsOf(row)"
          :title="t('cuttingOptimize.sheetPlanNamed', { name: use.name || use.goodsId, w: use.width, h: use.height })"
          v-model:kerf="kerfMm"
          :allow-rotate="row.forceFlip ? false : row.allowRotate"
          @update:allow-rotate="(v) => { if (!row.forceFlip) row.allowRotate = v }"
          @update:quantity="(v) => { use.quantity = v; absorbArmed = false }"
          @update:goods-id="(v) => { use.goodsId = v; syncSheetSize(row, use); absorbArmed = false }"
          @remove="row.sheets.splice(idx, 1)"
          @layout="(stamp, result) => onLayout(stamp, row, 'sheet', idx, result)"
          @flip="flipRow(row)"
        />
      </template>
      <template v-for="(pick, idx) in row.picks" :key="`rp-${row.itemId}-${idx}`">
        <CuttingPreview
          v-if="pick.width && pick.height && (Number(pick.quantity) > 0 || !locked)"
          :goods-id="pick.goodsId || row.goodsId"
          :sheet-width="pick.width"
          :sheet-height="pick.height"
          :piece-width="previewWidth(row)"
          :piece-height="previewHeight(row)"
          :quantity="Number(pick.quantity)"
          :min-remnant-width-cm="edgeWidthCm"
          :min-remnant-height-cm="edgeHeightCm"
          :max-scrap-percent="maxScrapPercent"
          :waste-width-cm="wasteWidthCm"
          :waste-height-cm="wasteHeightCm"
          :movable="!locked"
          :editable="!locked"
          :item-id="row.itemId"
          source="pick"
          :source-index="idx"
          :stamp="planStamp"
          :force-flip="row.forceFlip"
          :can-remove="!locked"
          :remnant-options="pickOptions(row)"
          :remnant-key="pick.key"
          :title="t('cuttingOptimize.remnantPlan', { w: pick.width, h: pick.height })"
          v-model:kerf="kerfMm"
          :allow-rotate="row.forceFlip ? false : row.allowRotate"
          @update:allow-rotate="(v) => { if (!row.forceFlip) row.allowRotate = v }"
          @update:quantity="(v) => { pick.quantity = v; absorbArmed = false }"
          @update:remnant-key="(v) => { pick.key = v; applyPickKey(row, pick); absorbArmed = false }"
          @remove="row.picks.splice(idx, 1)"
          @layout="(stamp, result) => onLayout(stamp, row, 'pick', idx, result)"
          @flip="flipRow(row)"
        />
      </template>
      </div>
    </div>
    </div>

    <button
      v-if="rows.length && !locked"
      type="button"
      class="btn"
      :disabled="saving || !complete"
      @click="onSave"
    >
      {{ t('cuttingOptimize.confirm') }}
    </button>
    <p v-if="locked" class="text-sm text-emerald-600">{{ t('cuttingOptimize.saved') }}</p>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import CuttingPreview from '@/components/crm/CuttingPreview.vue'
import ManualCutEditor from '@/components/crm/ManualCutEditor.vue'
import CutBoardsView from '@/components/crm/CutBoardsView.vue'
import {
  buildBoards,
  freeRects,
  matchesOrder,
  parseLayout,
  totalUsage,
  type EdLayout,
  type EditorSheetOption,
  type PlanSource,
} from '@/components/crm/manualCut'
import {
  confirmCuttingOptimize,
  fetchCuttingOptimize,
  fetchManualLayout,
  restoreManualLayout,
  suggestCuttingOptimize,
  type CuttingResult,
  type ManualLayout,
  type OptimizePlan,
  type OptimizeRemnant,
  type SheetOption,
} from '@/api/cutting'
import { formatApiError } from '@/api/http'
import { cutDrag, type CutSource } from '@/components/crm/cutDrag'

interface PickRow {
  key: string
  remnantId: number
  goodsId: number
  quantity: number
  width?: number
  height?: number
  planned: boolean
}
interface SheetRow {
  goodsId: number
  name: string
  width: number
  height: number
  quantity: number
}
interface Row {
  itemId: number
  goodsId: number
  goodsName: string
  width: number
  height: number
  count: number
  sheets: SheetRow[]
  picks: PickRow[]
  candidates: SheetOption[]
  plannedPool: { goodsId: number; width: number; height: number }[]
  allowRotate: boolean
  /** Previewda bo‘laklarni 90° burib joylashtirish. */
  forceFlip: boolean
}

const { t } = useI18n()
const route = useRoute()
const orderId = Number(route.params.id)
const plan = ref<OptimizePlan | null>(null)
const rows = ref<Row[]>([])
const remnants = ref<OptimizeRemnant[]>([])
const locked = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const kerfMm = ref(0)
const edgeWidthCm = ref(20)
const edgeHeightCm = ref(20)
const maxScrapPercent = ref<number | null>(null)
const wasteWidthCm = ref<number | null>(null)
const wasteHeightCm = ref<number | null>(null)
const cutSource = ref<'remnant' | 'sheet'>('remnant')
const planStamp = ref(0)
const absorbArmed = ref(true)
const caps = ref(new Map<string, number>())
const layouts = ref(new Map<string, CuttingResult>())

function num(v: unknown) {
  return Number(v || 0)
}

function fmtArea(v: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 }).format(v || 0)
}

const totals = computed(() => {
  let sheetPieces = 0
  let remnantPieces = 0
  let sheetArea = 0
  let remnantArea = 0
  let usefulArea = 0
  let scrapArea = 0
  for (const row of rows.value) {
    const pieceArea = (num(row.width) * num(row.height)) / 10_000
    for (const use of row.sheets) {
      const qty = num(use.quantity)
      sheetPieces += qty
      sheetArea += pieceArea * qty
    }
    for (const pick of row.picks) {
      const qty = num(pick.quantity)
      remnantPieces += qty
      remnantArea += pieceArea * qty
    }
  }
  for (const result of layouts.value.values()) {
    usefulArea += num(result.usefulArea)
    scrapArea += num(result.scrapArea)
  }
  return { sheetPieces, remnantPieces, sheetArea, remnantArea, usefulArea, scrapArea }
})

function flipRow(row: Row) {
  if (locked.value) return
  const w = num(row.width)
  const h = num(row.height)
  if (!w || !h || w === h) return
  row.forceFlip = !row.forceFlip
  row.allowRotate = true
  absorbArmed.value = false
  planStamp.value += 1
}

function previewWidth(row: Row) {
  return row.forceFlip ? num(row.height) : num(row.width)
}

function previewHeight(row: Row) {
  return row.forceFlip ? num(row.width) : num(row.height)
}

function planKey(goodsId: number, width: number, height: number) {
  return `plan:${goodsId}:${num(width)}x${num(height)}`
}

function applyPlan(data: OptimizePlan | null) {
  planStamp.value += 1
  absorbArmed.value = true
  caps.value = new Map()
  plan.value = data
  remnants.value = data?.remnants || []
  locked.value = Boolean(data?.optimized)
  if (data?.kerfMm != null) kerfMm.value = Number(data.kerfMm)
  edgeWidthCm.value = Number(data?.minRemnantWidthCm ?? data?.minRemnantCm ?? edgeWidthCm.value)
  edgeHeightCm.value = Number(data?.minRemnantHeightCm ?? data?.minRemnantCm ?? edgeHeightCm.value)
  if (data?.source === 'SHEET' || data?.source === 'REMNANT') {
    cutSource.value = data.source === 'SHEET' ? 'sheet' : 'remnant'
  }
  rows.value = (data?.lines || []).map((line) => {
    const candidates = line.candidates?.length
      ? line.candidates
      : [{ goodsId: line.goodsId, goodsName: line.goodsName, width: undefined, height: undefined }]
    const sheets: SheetRow[] = (line.sheetUses?.length ? line.sheetUses : [{
      goodsId: line.goodsId,
      goodsName: line.goodsName,
      quantity: line.sheetQuantity ?? line.count,
    }]).map((use) => {
      const known = candidates.find((c) => c.goodsId === use.goodsId)
      return {
        goodsId: use.goodsId,
        name: use.goodsName || known?.goodsName || '',
        width: num(use.width ?? known?.width),
        height: num(use.height ?? known?.height),
        quantity: num(use.quantity),
      }
    })
    const picks: PickRow[] = (line.picks || []).map((p) => {
      const planned = Boolean(p.planned) || !p.remnantId
      const goodsId = num(p.goodsId || line.goodsId)
      const width = p.width != null ? num(p.width) : undefined
      const height = p.height != null ? num(p.height) : undefined
      const live = remnants.value.find((r) => r.id === p.remnantId)
      return {
        key: planned ? planKey(goodsId, num(width), num(height)) : `rem:${p.remnantId}`,
        remnantId: planned ? 0 : num(p.remnantId),
        goodsId,
        quantity: num(p.quantity),
        width: live ? num(live.width) : width,
        height: live ? num(live.height) : height,
        planned,
      }
    })
    const plannedPool = picks
      .filter((p) => p.planned && p.width && p.height)
      .map((p) => ({ goodsId: p.goodsId, width: num(p.width), height: num(p.height) }))
    return {
      itemId: line.itemId,
      goodsId: line.goodsId,
      goodsName: line.goodsName || '',
      width: num(line.width),
      height: num(line.height),
      count: line.count,
      sheets,
      picks,
      candidates,
      plannedPool,
      allowRotate: line.allowRotate !== false,
      forceFlip: false,
    }
  })
  layouts.value = new Map()
}

function remnantsFor(row: Row) {
  const ids = new Set(row.candidates.map((c) => c.goodsId))
  if (ids.size === 0) ids.add(row.goodsId)
  return remnants.value.filter((r) => ids.has(r.goodsId))
}

function pickOptions(row: Row) {
  const options: { key: string; label: string }[] = remnantsFor(row).map((r) => ({
    key: `rem:${r.id}`,
    label: `${r.goodsName || ''} ${r.width}×${r.height} · ${r.pieceCount}`,
  }))
  const seen = new Set<string>()
  for (const p of row.plannedPool) {
    const key = planKey(p.goodsId, p.width, p.height)
    if (seen.has(key)) continue
    seen.add(key)
    const name = row.candidates.find((c) => c.goodsId === p.goodsId)?.goodsName || ''
    options.push({
      key,
      label: t('cuttingOptimize.plannedRemnant', { name, w: p.width, h: p.height }),
    })
  }
  return options
}

function sheetOptionsOf(row: Row) {
  return row.candidates.map((c) => ({
    goodsId: c.goodsId,
    label: `${c.goodsName || c.goodsId} · ${c.width}×${c.height}`,
  }))
}

function applyPickKey(row: Row, pick: PickRow) {
  if (pick.key.startsWith('rem:')) {
    const id = Number(pick.key.slice(4))
    const live = remnants.value.find((r) => r.id === id)
    pick.remnantId = id
    pick.planned = false
    pick.goodsId = live?.goodsId || row.goodsId
    pick.width = live ? num(live.width) : pick.width
    pick.height = live ? num(live.height) : pick.height
    return
  }
  const pool = row.plannedPool.find((p) => planKey(p.goodsId, p.width, p.height) === pick.key)
  pick.remnantId = 0
  pick.planned = true
  if (pool) {
    pick.goodsId = pool.goodsId
    pick.width = pool.width
    pick.height = pool.height
  }
}

function syncSheetSize(row: Row, use: SheetRow) {
  const known = row.candidates.find((c) => c.goodsId === use.goodsId)
  if (!known) return
  use.name = known.goodsName || ''
  use.width = num(known.width)
  use.height = num(known.height)
}

function used(row: Row) {
  const fromSheet = row.sheets.reduce((sum, s) => sum + num(s.quantity), 0)
  const fromRemnant = row.picks.reduce((sum, p) => sum + num(p.quantity), 0)
  return fromSheet + fromRemnant
}

function left(row: Row) {
  return row.count - used(row)
}

function capKey(itemId: number, source: CutSource, index: number) {
  return `${itemId}:${source}:${index}`
}

function pieceFits(row: Row, width: number, height: number) {
  const pw = num(row.width)
  const ph = num(row.height)
  return (pw <= width && ph <= height) || (ph <= width && pw <= height)
}

function onLayout(stamp: number, row: Row, source: CutSource, index: number, result: CuttingResult) {
  if (stamp !== planStamp.value) return
  layouts.value.set(capKey(row.itemId, source, index), result)
  if (locked.value) return
  const qty = source === 'sheet' ? num(row.sheets[index]?.quantity) : num(row.picks[index]?.quantity)
  if (Number(result.quantity) !== qty) return
  if (Number(result.kerfMm) !== Number(kerfMm.value)) return
  if (Number(result.minRemnantWidthCm) !== Number(edgeWidthCm.value)) return
  if (Number(result.minRemnantHeightCm) !== Number(edgeHeightCm.value)) return
  caps.value.set(capKey(row.itemId, source, index), Number(result.perSheet) || 0)
  if (absorbArmed.value) absorbRow(row)
}

/** To'liqsiz listdagi bo'lakni, sig'adigan qoldiq bo'sh joyiga o'tkazadi. */
function absorbRow(row: Row) {
  for (const [sIdx, use] of row.sheets.entries()) {
    const per = caps.value.get(capKey(row.itemId, 'sheet', sIdx)) || 0
    if (per < 1) continue
    let remainder = num(use.quantity) % per
    if (remainder === 0) continue
    const order = row.picks
      .map((pick, index) => ({ pick, index }))
      .sort((a, b) => roomOf(row, b.index) - roomOf(row, a.index))
    for (const { pick, index } of order) {
      const room = roomOf(row, index)
      if (room < 1) continue
      const move = Math.min(room, remainder)
      pick.quantity = num(pick.quantity) + move
      use.quantity = num(use.quantity) - move
      remainder -= move
      if (remainder === 0) break
    }
  }
}

function roomOf(row: Row, index: number) {
  const cap = caps.value.get(capKey(row.itemId, 'pick', index)) || 0
  if (cap < 1) return 0
  return cap - num(row.picks[index]?.quantity)
}

function onPointerUp(e: { clientX: number; clientY: number; target?: EventTarget | null }) {
  const drag = cutDrag.piece
  if (!drag) return
  cutDrag.piece = null
  if (locked.value) return
  const pointed = document.elementFromPoint(e.clientX, e.clientY)
  const raw = (e.target instanceof Element ? e.target : null) ?? pointed
  const target = (raw?.closest('[data-cut-drop]') ?? pointed?.closest('[data-cut-drop]')) as HTMLElement | null
  if (!target) return
  const itemId = Number(target.dataset.itemId)
  const source = target.dataset.source as CutSource | undefined
  const index = Number(target.dataset.index)
  if (!source || Number.isNaN(index)) return
  const fromRow = rows.value.find((r) => r.itemId === drag.itemId)
  const toRow = rows.value.find((r) => r.itemId === itemId)
  if (!fromRow || !toRow) return
  absorbArmed.value = false
  const kind = target.dataset.cutDrop
  // Bir xil o‘lcham ichida list/qoldiq o‘rtasida ko‘chirish.
  if (itemId === drag.itemId && (kind === 'next' || kind === 'sheet')) {
    if (source === drag.source && index === drag.index) return
    shiftPiece(fromRow, drag.source, drag.index, source, index)
    return
  }
  // Boshqa o‘lchamning yashil qoldig‘iga tashlash — shu qoldiqdan kesiladi.
  if (kind !== 'offcut') return
  const width = num(target.dataset.w)
  const height = num(target.dataset.h)
  const goodsId = Number(target.dataset.goodsId) || toRow.goodsId
  if (!width || !height || !pieceFits(fromRow, width, height)) return
  if (!takePiece(fromRow, drag.source, drag.index)) return
  const key = planKey(goodsId, width, height)
  const existing = fromRow.picks.find((p) => p.planned && p.key === key)
  if (existing) {
    existing.quantity = num(existing.quantity) + 1
    return
  }
  if (!fromRow.plannedPool.some((p) => planKey(p.goodsId, p.width, p.height) === key)) {
    fromRow.plannedPool.push({ goodsId, width, height })
  }
  fromRow.picks.push({
    key,
    remnantId: 0,
    goodsId,
    quantity: 1,
    width,
    height,
    planned: true,
  })
}

function shiftPiece(row: Row, from: CutSource, fromIndex: number, to: CutSource, toIndex: number) {
  const target = to === 'sheet' ? row.sheets[toIndex] : row.picks[toIndex]
  if (!target) return false
  if (!takePiece(row, from, fromIndex)) return false
  target.quantity = num(target.quantity) + 1
  return true
}

function takePiece(row: Row, source: CutSource, index: number) {
  if (source === 'sheet') {
    const use = row.sheets[index]
    if (!use || num(use.quantity) < 1) return false
    use.quantity = num(use.quantity) - 1
    return true
  }
  const pick = row.picks[index]
  if (!pick || num(pick.quantity) < 1) return false
  pick.quantity = num(pick.quantity) - 1
  if (pick.quantity <= 0) row.picks.splice(index, 1)
  return true
}

const complete = computed(() => rows.value.length > 0 && rows.value.every((row) => left(row) === 0 && used(row) === row.count))

const manual = ref<ManualLayout | null>(null)
const editor = ref<{
  initial: EdLayout
  plan: EdLayout
  expected: Map<number, number>
  allowedGoods: Record<number, number[]>
  sheetOptions: EditorSheetOption[]
  planRevision: number
} | null>(null)

const manualSaved = computed(() => {
  const layout = parseLayout(manual.value?.layout)
  if (!layout) return null
  return { layout, pct: (Math.round(totalUsage(layout.boards) * 10) / 10).toFixed(1) }
})

/** Har bir chizma hisoblangach, qo‘lda tahrirlash uchun manbalar. */
const editorSources = computed(() => {
  const list: PlanSource[] = []
  let ready = rows.value.length > 0
  for (const row of rows.value) {
    const rotatable = row.allowRotate !== false
    row.sheets.forEach((use, idx) => {
      const qty = num(use.quantity)
      if (qty < 1) return
      const result = layouts.value.get(capKey(row.itemId, 'sheet', idx))
      if (!result || Number(result.quantity) !== qty) {
        ready = false
        return
      }
      list.push({ itemId: row.itemId, kind: 'sheet', goodsId: use.goodsId, name: use.name, rotatable, result })
    })
    row.picks.forEach((pick, idx) => {
      const qty = num(pick.quantity)
      if (qty < 1) return
      const result = layouts.value.get(capKey(row.itemId, 'pick', idx))
      if (!result || Number(result.quantity) !== qty) {
        ready = false
        return
      }
      const name = row.candidates.find((c) => c.goodsId === pick.goodsId)?.goodsName || ''
      list.push({
        itemId: row.itemId,
        kind: pick.planned ? 'planned' : 'stock',
        goodsId: pick.goodsId || row.goodsId,
        name: pick.planned ? name : `${name} #${pick.remnantId}`,
        rotatable,
        result,
      })
    })
  }
  return { list, ready }
})

function expectedCounts() {
  const expected = new Map<number, number>()
  for (const row of rows.value) expected.set(row.itemId, row.count)
  return expected
}

const planLayout = computed(() => {
  if (!editorSources.value.ready) return null
  return buildBoards(editorSources.value.list, Number(kerfMm.value || 0) / 10)
})

/** Sahifada ko‘rsatiladigan karta: saqlangan qo‘lda tahrir, bo‘lmasa avtomatik reja. */
const viewLayout = computed(() => {
  const saved = manualSaved.value?.layout ?? null
  if (saved && matchesOrder(saved, expectedCounts())) return saved
  return planLayout.value
})

/** Jami ko‘rsatkichlar sahifadagi kesish kartasidan: qoldiqlar ikki marta sanalmaydi. */
const stats = computed(() => {
  const layout = viewLayout.value
  const out = { sheets: 0, remnantBoards: 0, usage: 0, useful: 0, scrap: 0, scrapPct: 0, usefulCount: 0, area: 0 }
  if (!layout) return out
  const minW = Number(edgeWidthCm.value) || 0
  const minH = Number(edgeHeightCm.value) || 0
  for (const board of layout.boards) {
    if (!board.parts.length) continue
    if (board.kind === 'sheet') out.sheets += 1
    else out.remnantBoards += 1
    out.area += (board.w * board.h) / 10_000
    for (const free of freeRects(board, minW, minH)) {
      const area = (free.w * free.h) / 10_000
      if (free.useful) {
        out.useful += area
        out.usefulCount += 1
      } else {
        out.scrap += area
      }
    }
  }
  out.usage = totalUsage(layout.boards)
  out.scrapPct = out.area > 0 ? (out.scrap / out.area) * 100 : 0
  return out
})

const itemLabels = computed(() => {
  const labels: Record<number, string> = {}
  for (const row of rows.value) {
    labels[row.itemId] = t('cutEditor.itemLabel', { name: row.goodsName, w: row.width, h: row.height, n: row.count })
  }
  return labels
})

function openEditor() {
  if (!editorSources.value.ready) return
  const expected = new Map<number, number>()
  const allowedGoods: Record<number, number[]> = {}
  const sheetOptions: EditorSheetOption[] = []
  for (const row of rows.value) {
    expected.set(row.itemId, row.count)
    allowedGoods[row.itemId] = row.candidates.map((c) => c.goodsId)
    for (const c of row.candidates) {
      if (!c.width || !c.height || sheetOptions.some((o) => o.goodsId === c.goodsId)) continue
      sheetOptions.push({ goodsId: c.goodsId, name: c.goodsName || String(c.goodsId), w: num(c.width), h: num(c.height) })
    }
  }
  const plan = planLayout.value ?? buildBoards(editorSources.value.list, Number(kerfMm.value || 0) / 10)
  const saved = manualSaved.value?.layout ?? null
  editor.value = {
    initial: matchesOrder(saved, expected) ? saved! : plan,
    plan,
    expected,
    allowedGoods,
    sheetOptions,
    planRevision: 0,
  }
}

let replanSeq = 0

function waitForPlan() {
  return new Promise<void>((resolve, reject) => {
    const timer = window.setTimeout(() => {
      stop()
      reject(new Error('timeout'))
    }, 20000)
    const stop = watch(editorSources, (value) => {
      if (!value.ready) return
      window.clearTimeout(timer)
      stop()
      resolve()
    })
  })
}

async function onReplan(payload: { maxScrapPercent: number; wasteWidthCm: number | null; wasteHeightCm: number | null }) {
  const seq = ++replanSeq
  maxScrapPercent.value = payload.maxScrapPercent
  wasteWidthCm.value = payload.wasteWidthCm
  wasteHeightCm.value = payload.wasteHeightCm
  if (!locked.value) {
    try {
      const res = await suggestCuttingOptimize(orderId, {
        kerfMm: Number(kerfMm.value) || 0,
        minRemnantCm: Number(edgeWidthCm.value) || 0,
        minRemnantWidthCm: Number(edgeWidthCm.value) || 0,
        minRemnantHeightCm: Number(edgeHeightCm.value) || 0,
        source: cutSource.value === 'sheet' ? 'SHEET' : 'REMNANT',
        allowRotate: true,
        maxScrapPercent: payload.maxScrapPercent,
        wasteWidthCm: payload.wasteWidthCm ?? undefined,
        wasteHeightCm: payload.wasteHeightCm ?? undefined,
      })
      if (seq !== replanSeq) return
      applyPlan(res.data || null)
      absorbArmed.value = false
    } catch (e) {
      if (seq !== replanSeq) return
      error.value = formatApiError(e)
      layouts.value = new Map()
      planStamp.value += 1
    }
  } else {
    layouts.value = new Map()
    planStamp.value += 1
  }
  try {
    await waitForPlan()
  } catch {
    return
  }
  if (seq !== replanSeq || !editor.value || !planLayout.value) return
  editor.value = {
    ...editor.value,
    plan: planLayout.value,
    planRevision: editor.value.planRevision + 1,
  }
}

function fmtDate(value?: string | null) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString('uz-UZ', { dateStyle: 'short', timeStyle: 'short' })
}

async function loadManual() {
  try {
    const res = await fetchManualLayout(orderId)
    manual.value = res.data || null
  } catch {
    manual.value = null
  }
}

async function restorePrevious() {
  error.value = null
  try {
    const res = await restoreManualLayout(orderId)
    manual.value = res.data || null
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function load() {
  error.value = null
  try {
    const res = await fetchCuttingOptimize(orderId)
    applyPlan(res.data || null)
  } catch (e) {
    error.value = formatApiError(e)
  }
  loadManual()
}

async function onSave() {
  if (!complete.value || saving.value) return
  saving.value = true
  error.value = null
  try {
    await confirmCuttingOptimize(orderId, {
      kerfMm: Number(kerfMm.value) >= 0 ? Number(kerfMm.value) : 0,
      minRemnantCm: Number(edgeWidthCm.value) >= 0 ? Number(edgeWidthCm.value) : 0,
      allowRotate: true,
      lines: rows.value.map((row) => ({
        saleOrderItemId: row.itemId,
        sheetQuantity: row.sheets.reduce((sum, s) => sum + num(s.quantity), 0),
        sheets: row.sheets.filter((s) => s.goodsId > 0 && num(s.quantity) > 0).map((s) => ({
          goodsId: s.goodsId,
          quantity: num(s.quantity),
        })),
        remnants: row.picks
          .filter((p) => !p.planned && p.remnantId > 0 && num(p.quantity) > 0)
          .map((p) => ({ remnantId: p.remnantId, quantity: num(p.quantity) })),
        planned: row.picks
          .filter((p) => p.planned && p.width && p.height && num(p.quantity) > 0)
          .map((p) => ({
            goodsId: p.goodsId,
            width: num(p.width),
            height: num(p.height),
            quantity: num(p.quantity),
          })),
      })),
    })
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

watch([kerfMm, edgeWidthCm, edgeHeightCm, cutSource], () => {
  planStamp.value += 1
  absorbArmed.value = true
  caps.value = new Map()
})

function onDragOver(e: DragEvent) {
  if (!cutDrag.piece) return
  const target = (e.target as Element | null)?.closest?.('[data-cut-drop]')
  if (!target) return
  e.preventDefault()
}

function onDrop(e: DragEvent) {
  if (!cutDrag.piece) return
  e.preventDefault()
  onPointerUp(e)
}

onMounted(() => {
  window.addEventListener('pointerup', onPointerUp)
  window.addEventListener('dragover', onDragOver)
  window.addEventListener('drop', onDrop)
  load()
})

onBeforeUnmount(() => {
  window.removeEventListener('pointerup', onPointerUp)
  window.removeEventListener('dragover', onDragOver)
  window.removeEventListener('drop', onDrop)
  cutDrag.piece = null
})
</script>

<style scoped>
.btn { height: 2.75rem; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; color: #fff; font-weight: 500; }
.btn:disabled { opacity: 0.6; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.9rem; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.summary {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 1.25rem;
}
.summary-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.summary-note { margin-top: 0.25rem; font-size: 0.8125rem; color: #067647; }
.summary-note.muted { color: #98a2b3; }
.summary-actions { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.summary-actions .btn { height: 2.5rem; }
.stat-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0.75rem;
}
@media (max-width: 1280px) {
  .stat-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
  border-radius: 0.75rem;
  border: 1px solid #eaecf0;
  border-top: 3px solid #465fff;
  background: #f9fafb;
  padding: 0.75rem 0.9rem;
}
.stat.good { border-top-color: #12b76a; background: #f6fef9; }
.stat.warn { border-top-color: #f79009; background: #fffcf5; }
.stat.bad { border-top-color: #f04438; background: #fffbfa; }
.stat-label { font-size: 0.75rem; font-weight: 500; color: #667085; }
.stat-value { font-size: 1.375rem; line-height: 1.2; font-weight: 700; color: #101828; }
.stat-value small { font-size: 0.8125rem; font-weight: 500; color: #667085; }
.stat-sub { font-size: 0.75rem; color: #98a2b3; }
.stat.good .stat-value { color: #067647; }
.stat.warn .stat-value { color: #b54708; }
.stat.bad .stat-value { color: #b42318; }
.stat-bar { display: block; height: 0.35rem; border-radius: 999px; background: #eaecf0; overflow: hidden; margin-top: 0.2rem; }
.stat-bar i { display: block; height: 100%; border-radius: inherit; background: currentColor; }
.stat.good .stat-bar i { background: #12b76a; }
.stat.warn .stat-bar i { background: #f79009; }
.stat.bad .stat-bar i { background: #f04438; }
:global(html.dark) .summary { background: #161b26; border-color: #1f242f; }
:global(html.dark) .stat { background: rgb(255 255 255 / 3%); border-color: #1f242f; }
:global(html.dark) .stat-value { color: rgba(255, 255, 255, 0.92); }
:global(html.dark) .stat-bar { background: rgb(255 255 255 / 8%); }
.calc-previews { display: none; }
.cut-sequence {
  display: flex;
  gap: 0.75rem;
  align-items: flex-start;
  overflow-x: auto;
  margin-top: 0.85rem;
  padding-bottom: 0.35rem;
}
.cut-sequence :deep(.cut) {
  flex: 0 0 24rem;
  width: 24rem;
  margin-top: 0;
}
</style>
