<template>
  <section class="cut">
    <header class="cut-head">
      <div class="cut-title">
        <Scissors class="h-4 w-4" />
        <span>{{ t('cutting.title') }}</span>
        <span class="cut-hint">{{ t('cutting.hint') }}</span>
      </div>
      <div class="cut-controls">
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
            />
            <rect
              v-for="r in sheet.remnants"
              :key="`r-${r.x}-${r.y}`"
              :x="r.x"
              :y="r.y"
              :width="r.width"
              :height="r.height"
              :class="r.useful ? 'svg-useful' : 'svg-scrap'"
              vector-effect="non-scaling-stroke"
            />
            <rect
              v-for="(p, i) in piecesOf(sheet)"
              :key="`p-${i}`"
              :x="p.x"
              :y="p.y"
              :width="result.layoutPieceWidth"
              :height="result.layoutPieceHeight"
              class="svg-piece"
              vector-effect="non-scaling-stroke"
            />
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
        {{ t('cutting.minRemnant', { n: num(result.minRemnantCm) }) }}
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

const props = defineProps<{
  goodsId: number
  pieceWidth: number
  pieceHeight: number
  quantity: number
}>()

const { t } = useI18n()
const kerf = ref<number | ''>('')
const defaultKerf = ref<number | null>(null)
const allowRotate = ref(true)
const result = ref<CuttingResult | null>(null)
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
  const stepX = r.layoutPieceWidth + kerfCm.value
  const stepY = r.layoutPieceHeight + kerfCm.value
  return Array.from({ length: sheet.pieces }, (_, i) => ({
    x: (i % sheet.columns) * stepX,
    y: Math.floor(i / sheet.columns) * stepY,
  }))
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
      pieceWidth: Number(props.pieceWidth),
      pieceHeight: Number(props.pieceHeight),
      quantity: pieces.value,
      kerfMm: kerf.value === '' ? undefined : Number(kerf.value),
      allowRotate: allowRotate.value,
    })
    if (seq !== requestSeq) return
    result.value = res.data
    error.value = null
    if (kerf.value === '') defaultKerf.value = Number(res.data.kerfMm)
  } catch (e) {
    if (seq !== requestSeq) return
    result.value = null
    error.value = formatApiError(e)
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

watch(
  () => [props.goodsId, props.pieceWidth, props.pieceHeight, pieces.value, kerf.value, allowRotate.value],
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
.cut { margin-top: 0.85rem; border: 1px solid #e4e7ec; border-radius: 0.75rem; background: #fcfcfd; padding: 0.85rem 1rem; }
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
.svg-useful { fill: #d1fadf; stroke: #12b76a; stroke-width: 1; stroke-dasharray: 4 3; }
.svg-scrap { fill: #fee4e2; stroke: #f97066; stroke-width: 1; stroke-dasharray: 2 2; }
.svg-label { fill: #344054; font-weight: 600; pointer-events: none; }
.cut-remnants { margin: 0.45rem 0 0; padding: 0; list-style: none; font-size: 0.76rem; color: #475467; }
.cut-remnants li { display: flex; align-items: center; gap: 0.35rem; flex-wrap: wrap; padding: 0.1rem 0; }
.cut-remnants .dot { display: inline-block; width: 0.55rem; height: 0.55rem; border-radius: 0.15rem; }
.cut-remnants .good .dot { background: #12b76a; }
.cut-remnants .bad .dot { background: #f97066; }
.cut-muted { margin-top: 0.45rem; font-size: 0.75rem; color: #667085; }
.cut-error { margin-top: 0.6rem; font-size: 0.8rem; color: #d92d20; }
</style>
