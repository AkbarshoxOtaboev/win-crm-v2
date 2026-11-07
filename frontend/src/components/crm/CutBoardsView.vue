<template>
  <div class="cbv">
    <p v-if="layout.unplaced.length" class="cbv-warn">{{ t('cutEditor.unplaced', { n: layout.unplaced.length }) }}</p>
    <div class="cbv-grid">
      <section v-for="(board, bi) in layout.boards" :key="board.id" class="cbv-sheet">
        <div class="cbv-head">
          <span class="cbv-title">{{ t('cutEditor.sheetTitle', { n: bi + 1, total: layout.boards.length, w: fmt(board.w), h: fmt(board.h) }) }}</span>
          <span v-if="board.parts.length" class="cbv-pct" :class="tone(usage(board))">{{ pct(usage(board)) }}%</span>
          <span v-else class="cbv-pct bad">{{ t('cutEditor.emptySheet') }}</span>
        </div>
        <div class="cbv-sub">
          <span>{{ kindLabel(board) }}<template v-if="board.name"> · {{ board.name }}</template></span>
          <span class="cbv-chip useful">{{ t('cutEditor.usefulArea', { area: m2(freeArea(board, true)), pct: shareOf(freeArea(board, true), board.w * board.h) }) }}</span>
          <span class="cbv-chip scrap">{{ t('cutEditor.scrapArea', { area: m2(freeArea(board, false)), pct: shareOf(freeArea(board, false), board.w * board.h) }) }}</span>
        </div>
        <div class="cbv-frame">
          <svg class="cbv-svg" :viewBox="`0 0 ${board.w} ${board.h}`" preserveAspectRatio="xMinYMin meet" @mouseleave="hover = null">
            <rect class="cbv-bg" x="0" y="0" :width="board.w" :height="board.h" />
            <g
              v-for="(free, fi) in freeOf(board)"
              :key="`f-${fi}`"
              class="cbv-free"
              :class="free.useful ? 'useful' : 'scrap'"
              @mousemove="show($event, `${fmt(free.w)}×${fmt(free.h)} sm`, free.useful ? t('cutEditor.useful') : t('cutEditor.scrap'))"
            >
              <rect :x="free.x" :y="free.y" :width="free.w" :height="free.h" vector-effect="non-scaling-stroke" />
              <text
                v-if="fits(board, free)"
                :x="free.x + free.w / 2"
                :y="free.y + free.h / 2"
                :font-size="fontOf(board, free)"
                text-anchor="middle"
                dominant-baseline="middle"
              >{{ fmt(free.w) }}×{{ fmt(free.h) }}</text>
            </g>
            <g
              v-for="part in board.parts"
              :key="part.id"
              class="cbv-part"
              @mousemove="show($event, `${fmt(part.w)}×${fmt(part.h)} sm`, itemLabels[part.itemId] || '')"
            >
              <rect :x="part.x" :y="part.y" :width="part.w" :height="part.h" :fill="colorOf(part.itemId)" vector-effect="non-scaling-stroke" />
              <text
                v-if="fits(board, part)"
                :x="part.x + part.w / 2"
                :y="part.y + part.h / 2"
                :font-size="fontOf(board, part)"
                class="cbv-text"
                text-anchor="middle"
                dominant-baseline="middle"
              >{{ fmt(part.w) }}×{{ fmt(part.h) }}</text>
            </g>
          </svg>
          <span class="cbv-dim-h">{{ fmt(board.h) }}</span>
        </div>
        <div class="cbv-dim-w">{{ fmt(board.w) }} sm</div>
      </section>
    </div>
    <div v-if="hover" class="cbv-tip" :style="{ left: `${hover.x + 14}px`, top: `${hover.y + 14}px` }">
      <b>{{ hover.title }}</b>
      <span v-if="hover.sub">{{ hover.sub }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { freeRects, PART_PALETTE, usage, type EdBoard, type EdLayout, type FreeRect, type Rect } from './manualCut'

const props = defineProps<{
  layout: EdLayout
  itemOrder: number[]
  itemLabels: Record<number, string>
  minRemnantWidthCm: number
  minRemnantHeightCm: number
}>()

const { t } = useI18n()
const hover = ref<{ x: number; y: number; title: string; sub: string } | null>(null)

const freeByBoard = computed(() => {
  const map = new Map<string, FreeRect[]>()
  for (const b of props.layout.boards) {
    map.set(b.id, freeRects(b, Number(props.minRemnantWidthCm) || 0, Number(props.minRemnantHeightCm) || 0))
  }
  return map
})

function freeOf(board: EdBoard) {
  return freeByBoard.value.get(board.id) || []
}
function freeArea(board: EdBoard, useful: boolean) {
  return freeOf(board).filter((f) => f.useful === useful).reduce((sum, f) => sum + f.w * f.h, 0)
}
function shareOf(cm2: number, area: number) {
  return pct(area > 0 ? (cm2 / area) * 100 : 0)
}

const numberFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 1 })
const areaFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 })
function fmt(v: number) {
  return numberFormat.format(Number(v) || 0)
}
function m2(cm2: number) {
  return areaFormat.format(cm2 / 10_000)
}
function pct(v: number) {
  return (Math.round((Number(v) || 0) * 10) / 10).toFixed(1)
}
function tone(v: number) {
  if (v >= 75) return 'good'
  if (v >= 50) return 'warn'
  return 'bad'
}
function kindLabel(board: EdBoard) {
  if (board.kind === 'stock') return t('cutEditor.stockKind')
  if (board.kind === 'remnant') return t('cutEditor.remnantKind')
  return t('cutEditor.sheetKind')
}
function colorOf(itemId: number) {
  const i = props.itemOrder.indexOf(itemId)
  return PART_PALETTE[(i < 0 ? 0 : i) % PART_PALETTE.length]
}
/** Yozuv varaq kengligining ~2.6% i: kichik ko'rinishda ham o'qiladi. */
function fontOf(board: EdBoard, r: Rect) {
  const label = `${fmt(r.w)}×${fmt(r.h)}`
  return Math.min(board.w * 0.026, r.w / (label.length * 0.62), r.h * 0.6)
}
function fits(board: EdBoard, r: Rect) {
  return fontOf(board, r) >= board.w * 0.014
}
function show(e: MouseEvent, title: string, sub: string) {
  hover.value = { x: e.clientX, y: e.clientY, title, sub }
}
</script>

<style scoped>
.cbv-warn { margin-bottom: 0.75rem; font-size: 0.875rem; color: #b54708; }
.cbv-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1.25rem 1rem;
}
@media (max-width: 1100px) {
  .cbv-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 700px) {
  .cbv-grid { grid-template-columns: minmax(0, 1fr); }
}
.cbv-sheet { min-width: 0; }
.cbv-head { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; font-size: 0.8125rem; }
.cbv-title { font-weight: 600; color: #1d2939; }
.cbv-sub { display: flex; flex-wrap: wrap; align-items: center; gap: 0.35rem; margin: 0.25rem 0 0.4rem; font-size: 0.75rem; color: #667085; }
.cbv-pct { border-radius: 999px; padding: 0.1rem 0.5rem; font-size: 0.75rem; font-weight: 600; }
.cbv-pct.good { background: #ecfdf3; color: #067647; }
.cbv-pct.warn { background: #fffaeb; color: #b54708; }
.cbv-pct.bad { background: #fef3f2; color: #b42318; }
.cbv-chip { border-radius: 999px; padding: 0.05rem 0.45rem; font-weight: 500; }
.cbv-chip.useful { background: #ecfdf3; color: #067647; }
.cbv-chip.scrap { background: #fef3f2; color: #b42318; }
.cbv-frame { display: flex; align-items: center; gap: 0.25rem; }
.cbv-svg { display: block; width: 100%; height: auto; min-width: 0; box-shadow: 0 1px 3px rgb(16 24 40 / 12%); }
.cbv-bg { fill: #fff; }
.cbv-part rect { stroke: #475467; stroke-width: 1; }
.cbv-part:hover rect { stroke: #2563eb; stroke-width: 2; filter: brightness(0.94); }
.cbv-text { fill: #101828; font-weight: 600; pointer-events: none; }
.cbv-free rect { stroke-width: 1; stroke-dasharray: 4 3; }
.cbv-free.useful rect { fill: rgb(18 183 106 / 16%); stroke: #12b76a; }
.cbv-free.scrap rect { fill: rgb(240 68 56 / 12%); stroke: #f97066; }
.cbv-free text { font-weight: 600; pointer-events: none; }
.cbv-free.useful text { fill: #067647; }
.cbv-free.scrap text { fill: #b42318; }
.cbv-dim-h { writing-mode: vertical-rl; font-size: 0.7rem; color: #667085; }
.cbv-dim-w { text-align: center; font-size: 0.7rem; color: #667085; margin-top: 0.15rem; padding-right: 1rem; }
.cbv-tip {
  position: fixed;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  pointer-events: none;
  border-radius: 0.45rem;
  background: #101828;
  color: #fff;
  padding: 0.35rem 0.6rem;
  font-size: 0.8125rem;
  box-shadow: 0 4px 12px rgb(16 24 40 / 25%);
}
.cbv-tip span { font-size: 0.75rem; color: #d0d5dd; }
:global(html.dark) .cbv-title { color: rgba(255, 255, 255, 0.9); }
:global(html.dark) .cbv-bg { fill: #1f242f; }
</style>
