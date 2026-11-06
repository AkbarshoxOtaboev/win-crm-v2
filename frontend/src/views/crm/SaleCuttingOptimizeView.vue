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

    <div v-if="plan && rows.length" class="card mb-4 p-5">
      <div class="flex flex-wrap items-end gap-4">
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.kerfChoice') }} (mm)
          <input v-model.number="kerfMm" type="number" min="0" max="50" step="0.5" class="field w-28" :disabled="locked" />
        </label>
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.edgeWidth') }}
          <input v-model.number="edgeWidthCm" type="number" min="0" step="1" class="field w-28" :disabled="locked" />
        </label>
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.edgeHeight') }}
          <input v-model.number="edgeHeightCm" type="number" min="0" step="1" class="field w-28" :disabled="locked" />
        </label>
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.cutSource') }}
          <select v-model="cutSource" class="field" :disabled="locked">
            <option value="remnant">{{ t('cuttingOptimize.sourceRemnant') }}</option>
            <option value="sheet">{{ t('cuttingOptimize.sourceSheet') }}</option>
          </select>
        </label>
        <button v-if="!locked" type="button" class="ghost" :disabled="saving" @click="recalc">
          {{ t('cuttingOptimize.recalc') }}
        </button>
      </div>
      <p class="mt-2 text-xs text-gray-400">{{ t('cuttingOptimize.edgeKeepHint') }}</p>
      <p class="mt-1 text-xs text-gray-400">{{ t('cuttingOptimize.manualHint') }}</p>
    </div>

    <div v-if="plan && rows.length === 0" class="card p-5 text-sm text-gray-500">
      {{ t('cuttingOptimize.noWindows') }}
    </div>

    <div v-for="row in rows" :key="row.itemId" class="card mb-4 p-5">
      <div class="mb-3 flex flex-wrap items-baseline justify-between gap-2">
        <h3 class="text-base font-semibold text-gray-800 dark:text-white/90">{{ row.goodsName }}</h3>
        <span class="text-sm text-gray-500">
          {{ t('cuttingOptimize.size') }} {{ row.width }}×{{ row.height }} · {{ t('cuttingOptimize.pieces') }} {{ row.count }}
        </span>
      </div>

      <div v-for="(use, idx) in row.sheets" :key="`s-${idx}`" class="mb-2 flex flex-wrap items-end gap-3">
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.sheetSource') }}
          <select v-model.number="use.goodsId" class="field" :disabled="locked" @change="syncSheetSize(row, use)">
            <option v-for="c in row.candidates" :key="c.goodsId" :value="c.goodsId">
              {{ c.goodsName }} · {{ c.width }}×{{ c.height }}
            </option>
          </select>
        </label>
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.fromSheet') }}
          <input v-model.number="use.quantity" type="number" min="0" class="field w-24" :disabled="locked" @input="absorbArmed = false" />
        </label>
        <button v-if="!locked && row.sheets.length > 1" type="button" class="ghost" @click="row.sheets.splice(idx, 1)">×</button>
      </div>
      <button v-if="!locked" type="button" class="ghost mb-3" @click="addSheet(row)">{{ t('cuttingOptimize.addSheet') }}</button>

      <div class="mb-2">
        <button v-if="!locked" type="button" class="ghost" @click="addPick(row)">{{ t('cuttingOptimize.addRemnant') }}</button>
      </div>

      <div v-for="(pick, idx) in row.picks" :key="`p-${idx}`" class="mb-2 flex flex-wrap items-end gap-2">
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.remnant') }}
          <select v-model="pick.key" class="field" :disabled="locked" @change="applyPickKey(row, pick)">
            <option value="" disabled>{{ t('cuttingOptimize.remnant') }}</option>
            <option v-for="opt in pickOptions(row)" :key="opt.key" :value="opt.key">{{ opt.label }}</option>
          </select>
        </label>
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.pieces') }}
          <input v-model.number="pick.quantity" type="number" min="1" class="field w-24" :disabled="locked" @input="absorbArmed = false" />
        </label>
        <button v-if="!locked" type="button" class="ghost" @click="row.picks.splice(idx, 1)">×</button>
      </div>
      <p v-if="pickOptions(row).length === 0" class="text-xs text-gray-400">{{ t('cuttingOptimize.noRemnants') }}</p>
      <p class="mt-2 text-sm" :class="left(row) === 0 ? 'text-emerald-600' : 'text-amber-600'">
        {{ left(row) === 0 ? t('cuttingOptimize.ready') : t('cuttingOptimize.left', { n: left(row) }) }}
      </p>

      <div class="cut-sequence">
      <template v-for="(use, idx) in row.sheets" :key="`plan-${row.itemId}-${idx}`">
        <CuttingPreview
          v-if="Number(use.quantity) > 0 && use.width && use.height"
          :goods-id="use.goodsId"
          :sheet-width="use.width"
          :sheet-height="use.height"
          :piece-width="row.width"
          :piece-height="row.height"
          :quantity="Number(use.quantity)"
          :min-remnant-width-cm="edgeWidthCm"
          :min-remnant-height-cm="edgeHeightCm"
          :movable="!locked"
          :item-id="row.itemId"
          source="sheet"
          :source-index="idx"
          :stamp="planStamp"
          readonly
          :title="t('cuttingOptimize.sheetPlanNamed', { name: use.name || use.goodsId, w: use.width, h: use.height })"
          v-model:kerf="kerfMm"
          v-model:allow-rotate="row.allowRotate"
          @layout="(stamp, result) => onLayout(stamp, row, 'sheet', idx, result)"
        />
      </template>
      <template v-for="(pick, idx) in row.picks" :key="`rp-${row.itemId}-${idx}`">
        <CuttingPreview
          v-if="pick.width && pick.height && Number(pick.quantity) > 0"
          :goods-id="pick.goodsId || row.goodsId"
          :sheet-width="pick.width"
          :sheet-height="pick.height"
          :piece-width="row.width"
          :piece-height="row.height"
          :quantity="Number(pick.quantity)"
          :min-remnant-width-cm="edgeWidthCm"
          :min-remnant-height-cm="edgeHeightCm"
          :movable="!locked"
          :item-id="row.itemId"
          source="pick"
          :source-index="idx"
          :stamp="planStamp"
          readonly
          :title="t('cuttingOptimize.remnantPlan', { w: pick.width, h: pick.height })"
          v-model:kerf="kerfMm"
          v-model:allow-rotate="row.allowRotate"
          @layout="(stamp, result) => onLayout(stamp, row, 'pick', idx, result)"
        />
      </template>
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
import {
  confirmCuttingOptimize,
  fetchCuttingOptimize,
  suggestCuttingOptimize,
  type CuttingResult,
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
const cutSource = ref<'remnant' | 'sheet'>('remnant')
const planStamp = ref(0)
const absorbArmed = ref(true)
const caps = ref(new Map<string, number>())

function num(v: unknown) {
  return Number(v || 0)
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
    }
  })
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
  if (stamp !== planStamp.value || locked.value) return
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
  if (!source || itemId !== drag.itemId || Number.isNaN(index)) return
  if (source === drag.source && index === drag.index) return
  const row = rows.value.find((r) => r.itemId === drag.itemId)
  if (!row) return
  absorbArmed.value = false
  const kind = target.dataset.cutDrop
  if (kind === 'next' || kind === 'sheet') {
    if (!shiftPiece(row, drag.source, drag.index, source, index)) return
    return
  }
  if (kind !== 'offcut' || Number(target.dataset.fits) < 1) return
  const width = num(target.dataset.w)
  const height = num(target.dataset.h)
  const goodsId = Number(target.dataset.goodsId) || row.goodsId
  if (!width || !height || !pieceFits(row, width, height)) return
  if (!takePiece(row, drag.source, drag.index)) return
  const key = planKey(goodsId, width, height)
  const existing = row.picks.find((p) => p.planned && p.key === key)
  if (existing) {
    existing.quantity = num(existing.quantity) + 1
    return
  }
  if (!row.plannedPool.some((p) => planKey(p.goodsId, p.width, p.height) === key)) {
    row.plannedPool.push({ goodsId, width, height })
  }
  row.picks.push({
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

function addSheet(row: Row) {
  const first = row.candidates[0]
  row.sheets.push({
    goodsId: first?.goodsId || row.goodsId,
    name: first?.goodsName || '',
    width: num(first?.width),
    height: num(first?.height),
    quantity: 0,
  })
}

function addPick(row: Row) {
  const opt = pickOptions(row)[0]
  const pick: PickRow = { key: opt?.key || '', remnantId: 0, goodsId: row.goodsId, quantity: 1, planned: false }
  if (opt) applyPickKey(row, pick)
  row.picks.push(pick)
}

async function load() {
  error.value = null
  try {
    const res = await fetchCuttingOptimize(orderId)
    applyPlan(res.data || null)
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function recalc() {
  saving.value = true
  error.value = null
  try {
    const res = await suggestCuttingOptimize(orderId, {
      kerfMm: Number(kerfMm.value) >= 0 ? Number(kerfMm.value) : 0,
      minRemnantWidthCm: Number(edgeWidthCm.value) >= 0 ? Number(edgeWidthCm.value) : 0,
      minRemnantHeightCm: Number(edgeHeightCm.value) >= 0 ? Number(edgeHeightCm.value) : 0,
      source: cutSource.value === 'sheet' ? 'SHEET' : 'REMNANT',
      allowRotate: true,
    })
    applyPlan(res.data || null)
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
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
.field {
  display: block;
  margin-top: 0.25rem;
  height: 2.5rem;
  min-width: 8rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
.btn { height: 2.75rem; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; color: #fff; font-weight: 500; }
.btn:disabled { opacity: 0.6; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.9rem; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
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
