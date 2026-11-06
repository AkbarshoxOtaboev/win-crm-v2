<template>
  <section class="cut">
    <div v-if="pieceTip" class="piece-tip" :style="{ left: `${pieceTip.x}px`, top: `${pieceTip.y}px` }">
      {{ pieceTip.text }}
    </div>
    <header class="cut-head">
      <div class="cut-title">
        <Scissors class="h-4 w-4" />
        <span>{{ title || t('cutting.title') }}</span>
        <span class="cut-hint">{{ readonly ? t('cutting.savedHint') : t('cutting.hint') }}</span>
      </div>
      <div v-if="readonly" class="cut-controls">
        <span class="chip">{{ t('cutting.kerf') }}: <b class="ms-1">{{ result ? num(result.kerfMm) : kerf === '' ? '—' : kerf }}</b></span>
        <span class="chip">{{ allowRotate ? t('cutting.rotateOn') : t('cutting.rotateOff') }}</span>
      </div>
      <div v-else class="cut-controls">
        <label class="cut-kerf">
          {{ t('cutting.kerf') }}
          <input
            v-model.number="kerf"
            type="number"
            min="0"
            max="50"
            step="0.5"
            class="cut-input"
            :placeholder="defaultKerf != null ? String(defaultKerf) : ''"
          />
        </label>
        <label class="cut-check">
          <input v-model="allowRotate" type="checkbox" />
          {{ t('cutting.rotate') }}
        </label>
      </div>
    </header>

    <p v-if="error" class="cut-error">{{ error }}</p>
    <p v-else-if="!result" class="cut-muted">{{ t('cutting.loading') }}</p>

    <template v-else>
      <div class="cut-chips" :class="{ stale: loading }">
        <span class="chip strong">{{ t('cutting.sheetsNeeded', { n: result.sheetsNeeded }) }}</span>
        <span class="chip">{{ t('cutting.perSheet', { n: result.perSheet, cols: result.columns, rows: result.rows }) }}</span>
        <span class="chip">{{ t('cutting.leftover', { area: num(result.leftoverArea), pct: num(result.leftoverPercent) }) }}</span>
        <span class="chip good">{{ t('cutting.useful', { area: num(result.usefulArea) }) }}</span>
        <span class="chip bad">{{ t('cutting.scrap', { area: num(result.scrapArea) }) }}</span>
      </div>
      <p v-if="roundedQty" class="cut-muted">{{ t('cutting.roundedQty', { n: roundedQty }) }}</p>
      <p v-if="result.rotated" class="cut-muted">
        {{ t('cutting.rotated', { w: num(result.layoutPieceWidth), h: num(result.layoutPieceHeight) }) }}
      </p>

      <div class="cut-sheets">
        <figure v-for="(sheet, idx) in result.sheets" :key="idx" class="cut-sheet">
          <figcaption class="cut-caption">
            <strong>{{ sheetLabel(sheet, idx) }}</strong>
            <span>{{ t('cutting.piecesOnSheet', { n: sheet.pieces }) }}</span>
          </figcaption>
          <svg
            class="cut-svg"
            :viewBox="`0 0 ${result.sheetWidth} ${result.sheetHeight}`"
            preserveAspectRatio="xMidYMid meet"
            role="img"
            :aria-label="t('cutting.sheetSize', { w: num(result.sheetWidth), h: num(result.sheetHeight) })"
          >
            <rect
              x="0"
              y="0"
              :width="result.sheetWidth"
              :height="result.sheetHeight"
              class="svg-sheet"
              vector-effect="non-scaling-stroke"
              :data-cut-drop="movable ? 'sheet' : undefined"
              :data-item-id="movable ? itemId : undefined"
              :data-source="movable ? source : undefined"
              :data-index="movable ? sourceIndex : undefined"
            />
            <rect
              v-for="r in sheet.remnants"
              :key="`r-${r.x}-${r.y}`"
              :x="r.x"
              :y="r.y"
              :width="r.width"
              :height="r.height"
              :class="remnantClass(r)"
              vector-effect="non-scaling-stroke"
              :data-cut-drop="dropKind(sheet, r)"
              :data-item-id="movable && r.useful ? itemId : undefined"
              :data-source="movable && r.useful ? source : undefined"
              :data-index="movable && r.useful ? sourceIndex : undefined"
              :data-w="movable && r.useful ? r.width : undefined"
              :data-h="movable && r.useful ? r.height : undefined"
              :data-goods-id="movable && r.useful ? goodsId : undefined"
              :data-fits="movable && r.useful ? r.fitsPieces : undefined"
              :role="dropKind(sheet, r) ? 'button' : undefined"
              :tabindex="dropKind(sheet, r) ? 0 : undefined"
              :aria-label="dropKind(sheet, r) ? t('cuttingOptimize.dropRemnant') : undefined"
            />
            <rect
              v-for="(p, i) in piecesOf(sheet)"
              :key="`p-${i}`"
              :x="p.x"
              :y="p.y"
              :width="p.w"
              :height="p.h"
              :class="movable ? 'svg-piece can-drag' : 'svg-piece'"
              vector-effect="non-scaling-stroke"
              :draggable="movable ? 'true' : undefined"
              :role="movable ? 'button' : undefined"
              :tabindex="movable ? 0 : undefined"
              :aria-label="pieceSizeLabel(p)"
              @pointerdown="onPiecePointerDown"
              @pointerenter="showPieceTip($event, p)"
              @pointermove="showPieceTip($event, p)"
              @pointerleave="pieceTip = null"
              @dragstart="onDragStart"
            >
              <title>{{ pieceSizeLabel(p) }}</title>
            </rect>
            <template v-for="r in sheet.remnants" :key="`t-${r.x}-${r.y}`">
              <text
                v-if="labelFits(r)"
                :x="r.x + r.width / 2"
                :y="r.y + r.height / 2"
                :font-size="fontSize"
                class="svg-label"
                text-anchor="middle"
                dominant-baseline="middle"
              >
                {{ num(r.width) }}×{{ num(r.height) }}
              </text>
            </template>
          </svg>
          <ul class="cut-remnants">
            <li v-if="!sheet.remnants.length" class="cut-muted">{{ t('cutting.noRemnants') }}</li>
            <li v-for="r in sheet.remnants" :key="`l-${r.x}-${r.y}`" :class="r.useful ? 'good' : 'bad'">
              <span class="dot" />
              {{ num(r.width) }}×{{ num(r.height) }} {{ t('cutting.cm') }} · {{ num(r.area) }} m² ·
              {{ r.useful ? t('cutting.usefulRemnant') : t('cutting.scrapRemnant') }}
              <template v-if="r.useful && r.fitsPieces > 0"> · {{ t('cutting.fitsMore', { n: r.fitsPieces }) }}</template>
            </li>
          </ul>
        </figure>
      </div>
      <p class="cut-muted">
        {{ t('cutting.sheetSize', { w: num(result.sheetWidth), h: num(result.sheetHeight) }) }} ·
        {{ t('cutting.minRemnantSize', { w: num(result.minRemnantWidthCm ?? result.minRemnantCm), h: num(result.minRemnantHeightCm ?? result.minRemnantCm) }) }}
      </p>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Scissors } from 'lucide-vue-next'
import { calculateCutting, type CuttingRemnant, type CuttingResult, type CuttingSheetLayout } from '@/api/cutting'
import { formatApiError } from '@/api/http'
import { cutDrag } from '@/components/crm/cutDrag'

const props = defineProps<{
  goodsId: number
  pieceWidth: number
  pieceHeight: number
  quantity: number
  /** Berilsa list o‘lchami shu (foydali qoldiq). Bo‘lmasa mahsulotning butun listi. */
  sheetWidth?: number
  sheetHeight?: number
  /** Saqlangan qoidalarni faqat ko'rsatish (ishlab chiqarish): sozlab bo'lmaydi. */
  readonly?: boolean
  title?: string
  /** Foydali qoldiqning minimal tomoni, sm. En va bo'y berilmasa shu qiymat. */
  minRemnantCm?: number | null
  minRemnantWidthCm?: number | null
  minRemnantHeightCm?: number | null
  /** Bo'lakni sichqoncha bilan boshqa qoldiqqa olib o'tish. */
  movable?: boolean
  itemId?: number
  source?: 'sheet' | 'pick'
  sourceIndex?: number
  /** Reja almashtirilganda eski hisobni tashlab yuborish. */
  stamp?: number
}>()

const emit = defineEmits<{
  layout: [stamp: number, result: CuttingResult]
}>()

const { t } = useI18n()
/** '' - sozlamadagi standart qiymat. */
const kerf = defineModel<number | ''>('kerf', { default: '' })
const allowRotate = defineModel<boolean>('allowRotate', { default: true })
const defaultKerf = ref<number | null>(null)
const result = ref<CuttingResult | null>(null)
const pieceTip = ref<{ x: number; y: number; text: string } | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
let timer: ReturnType<typeof setTimeout> | undefined
let requestSeq = 0

const pieces = computed(() => Math.ceil(Number(props.quantity) || 0))
const roundedQty = computed(() => (pieces.value !== Number(props.quantity) ? pieces.value : 0))
const kerfCm = computed(() => Number(result.value?.kerfMm || 0) / 10)
const fontSize = computed(() => {
  const r = result.value
  return r ? Math.max(Math.min(r.sheetWidth, r.sheetHeight) * 0.07, 4) : 10
})

const numberFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 })
function num(v?: number | null) {
  return numberFormat.format(Number(v || 0))
}

function sheetLabel(sheet: CuttingSheetLayout, idx: number) {
  const r = result.value
  if (!r || r.sheets.length === 1) {
    return sheet.count > 1 ? t('cutting.fullSheets', { n: sheet.count }) : t('cutting.singleSheet')
  }
  return idx === r.sheets.length - 1 ? t('cutting.lastSheet') : t('cutting.fullSheets', { n: sheet.count })
}

function piecesOf(sheet: CuttingSheetLayout) {
  const r = result.value
  if (!r) return []
  const extra = sheet.placed ?? []
  const gridCount = Math.max(0, sheet.pieces - extra.length)
  const stepX = r.layoutPieceWidth + kerfCm.value
  const stepY = r.layoutPieceHeight + kerfCm.value
  return [
    ...Array.from({ length: gridCount }, (_, i) => ({
      x: (i % sheet.columns) * stepX,
      y: Math.floor(i / sheet.columns) * stepY,
      w: r.layoutPieceWidth,
      h: r.layoutPieceHeight,
    })),
    ...extra.map((p) => ({ x: p.x, y: p.y, w: p.width, h: p.height })),
  ]
}

function remnantClass(r: CuttingRemnant) {
  const hot = r.useful && props.movable && cutDrag.piece && cutDrag.piece.itemId === props.itemId
  return [r.useful ? 'svg-useful' : 'svg-scrap', hot ? 'drop-hot' : '']
}

function nextCell(sheet: CuttingSheetLayout, r: CuttingRemnant) {
  const res = result.value
  if (!res || sheet.pieces >= res.perSheet) return false
  const i = sheet.pieces
  const x = (i % sheet.columns) * (res.layoutPieceWidth + kerfCm.value)
  const y = Math.floor(i / sheet.columns) * (res.layoutPieceHeight + kerfCm.value)
  return x >= r.x - 0.05
    && y >= r.y - 0.05
    && x + res.layoutPieceWidth <= r.x + r.width + 0.05
    && y + res.layoutPieceHeight <= r.y + r.height + 0.05
}

function dropKind(sheet: CuttingSheetLayout, r: CuttingRemnant) {
  if (!props.movable || !r.useful) return undefined
  if (nextCell(sheet, r)) return 'next'
  return r.fitsPieces > 0 ? 'offcut' : undefined
}

function rememberPiece() {
  if (!props.movable || props.itemId == null || !props.source) return false
  cutDrag.piece = { itemId: props.itemId, source: props.source, index: props.sourceIndex ?? 0 }
  return true
}

function pieceSizeLabel(p?: { w: number; h: number }) {
  const drawn = result.value
  if (!drawn) return ''
  return `${num(p?.w ?? drawn.layoutPieceWidth)}×${num(p?.h ?? drawn.layoutPieceHeight)} ${t('cutting.cm')}`
}

function showPieceTip(e: PointerEvent, p: { w: number; h: number }) {
  const host = (e.currentTarget as Element | null)?.closest('.cut') as HTMLElement | null
  if (!host) return
  const box = host.getBoundingClientRect()
  pieceTip.value = {
    x: Math.min(Math.max(e.clientX - box.left + 12, 8), host.clientWidth - 8),
    y: Math.min(Math.max(e.clientY - box.top + 14, 8), host.clientHeight - 8),
    text: pieceSizeLabel(p),
  }
}

function onPiecePointerDown() {
  rememberPiece()
}

function onDragStart(e: DragEvent) {
  if (!rememberPiece()) {
    e.preventDefault()
    return
  }
  e.dataTransfer?.setData('text/plain', 'piece')
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function labelFits(r: CuttingRemnant) {
  const size = fontSize.value
  return r.height >= size * 1.4 && r.width >= size * 4
}

async function run() {
  const seq = ++requestSeq
  loading.value = true
  try {
    const res = await calculateCutting({
      goodsId: props.goodsId,
      sheetWidth: props.sheetWidth || undefined,
      sheetHeight: props.sheetHeight || undefined,
      pieceWidth: Number(props.pieceWidth),
      pieceHeight: Number(props.pieceHeight),
      quantity: pieces.value,
      kerfMm: kerf.value === '' ? undefined : Number(kerf.value),
      allowRotate: allowRotate.value,
      minRemnantCm: props.minRemnantCm != null && !Number.isNaN(Number(props.minRemnantCm)) ? Number(props.minRemnantCm) : undefined,
      minRemnantWidthCm: props.minRemnantWidthCm != null && !Number.isNaN(Number(props.minRemnantWidthCm)) ? Number(props.minRemnantWidthCm) : undefined,
      minRemnantHeightCm: props.minRemnantHeightCm != null && !Number.isNaN(Number(props.minRemnantHeightCm)) ? Number(props.minRemnantHeightCm) : undefined,
    })
    if (seq !== requestSeq) return
    result.value = res.data
    error.value = null
    if (kerf.value === '') defaultKerf.value = Number(res.data.kerfMm)
    emit('layout', props.stamp ?? 0, res.data)
  } catch (e) {
    if (seq !== requestSeq) return
    result.value = null
    error.value = formatApiError(e)
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

watch(
  () => [props.goodsId, props.sheetWidth, props.sheetHeight, props.pieceWidth, props.pieceHeight, pieces.value, kerf.value, allowRotate.value, props.minRemnantCm, props.minRemnantWidthCm, props.minRemnantHeightCm, props.stamp],
  () => {
    clearTimeout(timer)
    timer = setTimeout(run, 350)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  clearTimeout(timer)
  requestSeq++
})
</script>

<style scoped>
.cut { position: relative; margin-top: 0.85rem; border: 1px solid #e4e7ec; border-radius: 0.75rem; background: #fcfcfd; padding: 0.85rem 1rem; }
.piece-tip {
  position: absolute;
  z-index: 4;
  transform: translate(-50%, 0);
  pointer-events: none;
  border-radius: 0.4rem;
  background: #101828;
  color: #fff;
  padding: 0.15rem 0.45rem;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}
.cut-head { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.6rem; }
.cut-title { display: flex; flex-wrap: wrap; align-items: center; gap: 0.45rem; font-size: 0.875rem; font-weight: 600; color: #344054; }
.cut-hint { font-size: 0.75rem; font-weight: 400; color: #98a2b3; }
.cut-controls { display: flex; flex-wrap: wrap; align-items: center; gap: 0.9rem; font-size: 0.8rem; color: #475467; }
.cut-kerf { display: inline-flex; align-items: center; gap: 0.4rem; }
.cut-input { width: 4.5rem; height: 2rem; border: 1px solid #d0d5dd; border-radius: 0.45rem; background: #fff; padding: 0 0.5rem; font-size: 0.8rem; }
.cut-check { display: inline-flex; align-items: center; gap: 0.35rem; cursor: pointer; }
.cut-chips { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 0.7rem; transition: opacity 0.15s; }
.cut-chips.stale { opacity: 0.55; }
.chip { display: inline-flex; align-items: center; border-radius: 9999px; background: #f2f4f7; padding: 0.2rem 0.65rem; font-size: 0.78rem; color: #344054; }
.chip.strong { background: #eef2ff; color: #3538cd; font-weight: 600; }
.chip.good { background: #ecfdf3; color: #067647; }
.chip.bad { background: #fef3f2; color: #b42318; }
.cut-sheets { display: grid; grid-template-columns: repeat(auto-fill, minmax(16rem, 1fr)); gap: 0.85rem; margin-top: 0.75rem; }
.cut-sheet { margin: 0; }
.cut-caption { display: flex; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.35rem; font-size: 0.78rem; color: #475467; }
.cut-svg { display: block; width: 100%; max-height: 14rem; }
.svg-sheet { fill: #fff; stroke: #667085; stroke-width: 1.5; }
.svg-piece { fill: #c7d7fe; stroke: #465fff; stroke-width: 1; }
.svg-piece.can-drag { cursor: grab; touch-action: none; }
.svg-useful { fill: #d1fadf; stroke: #12b76a; stroke-width: 1; stroke-dasharray: 4 3; }
.svg-useful.drop-hot { fill: #a6f4c5; stroke: #067647; stroke-width: 2.5; }
.svg-scrap { fill: #fee4e2; stroke: #f97066; stroke-width: 1; stroke-dasharray: 2 2; }
.svg-label { fill: #344054; font-weight: 600; pointer-events: none; }
.cut-remnants { margin: 0.45rem 0 0; padding: 0; list-style: none; font-size: 0.76rem; color: #475467; }
.cut-remnants li { display: flex; align-items: center; gap: 0.35rem; flex-wrap: wrap; padding: 0.1rem 0; }
.cut-remnants .dot { display: inline-block; width: 0.55rem; height: 0.55rem; border-radius: 0.15rem; }
.cut-remnants .good .dot { background: #12b76a; }
.cut-remnants .bad .dot { background: #f97066; }
.cut-muted { margin-top: 0.45rem; font-size: 0.75rem; color: #667085; }
.cut-error { margin-top: 0.6rem; font-size: 0.8rem; color: #d92d20; }

.dark .cut { border-color: #1f2937; background: rgb(255 255 255 / 2%); }
.dark .cut-title { color: rgba(255, 255, 255, 0.9); }
.dark .cut-controls,
.dark .cut-caption,
.dark .cut-remnants,
.dark .cut-muted { color: #98a2b3; }
.dark .cut-input { border-color: #344054; background: transparent; color: rgba(255, 255, 255, 0.9); }
.dark .chip { background: rgb(255 255 255 / 6%); color: #d0d5dd; }
.dark .chip.strong { background: rgb(70 95 255 / 15%); color: #9cb0ff; }
.dark .chip.good { background: rgb(18 183 106 / 12%); color: #6ce9a6; }
.dark .chip.bad { background: rgb(240 68 56 / 12%); color: #fda29b; }
.dark .svg-sheet { fill: #1d2939; stroke: #98a2b3; }
</style>
