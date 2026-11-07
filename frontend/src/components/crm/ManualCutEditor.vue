<template>
  <Teleport to="body">
    <div class="mce" role="dialog" aria-modal="true" :aria-label="t('cutEditor.title')">
      <header class="mce-bar">
        <button type="button" class="mce-icon" :title="t('cutEditor.back')" @click="requestClose">←</button>
        <h2 class="mce-title">{{ t('cutEditor.title') }}</h2>
        <span class="mce-pct" :class="tone(total)" :title="t('cutEditor.total')">▦ {{ pct(total) }}%</span>
        <span class="mce-muted">{{ t('cutEditor.kerf', { n: pct(kerf * 10) }) }}</span>
        <span class="mce-chip useful">{{ t('cutEditor.usefulArea', { area: m2(totalFree.useful), pct: shareOf(totalFree.useful, totalFree.area) }) }}</span>
        <span class="mce-chip scrap">{{ t('cutEditor.scrapArea', { area: m2(totalFree.scrap), pct: shareOf(totalFree.scrap, totalFree.area) }) }}</span>
        <span class="mce-grow" />
        <button type="button" class="mce-icon" :disabled="!history.length" :title="t('cutEditor.undo')" @click="undo">↶</button>
        <button type="button" class="mce-primary" :disabled="saving || readonly" @click="trySave">✓ {{ t('cutEditor.save') }}</button>
      </header>

      <div class="mce-tools">
        <button type="button" class="mce-tool" :disabled="!canRotate" @click="rotateSelected">↻ {{ t('cutEditor.rotate') }}</button>
        <button type="button" class="mce-tool danger" :disabled="!selectedOnBoard" @click="removeSelected">🗑 {{ t('cutEditor.remove') }}</button>
        <span class="mce-sep" />
        <button type="button" class="mce-tool" :title="t('cutEditor.zoomOut')" @click="zoomBy(-1)">−</button>
        <button type="button" class="mce-tool" :title="t('cutEditor.zoomIn')" @click="zoomBy(1)">+</button>
        <button type="button" class="mce-tool" :title="t('cutEditor.fit')" @click="fit">⛶</button>
        <div class="mce-zoom" :title="t('cutEditor.zoomManual')">
          <input
            :value="Math.round(zoom * 100)"
            type="number"
            :min="ZOOM_MIN * 100"
            :max="ZOOM_MAX * 100"
            step="5"
            class="mce-zoom-input"
            :aria-label="t('cutEditor.zoomManual')"
            @change="setZoomPercent(($event.target as HTMLInputElement).value)"
            @keydown.enter="setZoomPercent(($event.target as HTMLInputElement).value)"
          />
          <span>%</span>
          <select class="mce-zoom-select" :value="''" :aria-label="t('cutEditor.zoomPresets')" @change="onZoomPreset">
            <option value="" disabled hidden>▾</option>
            <option v-for="p in ZOOM_PRESETS" :key="p" :value="p">{{ p }}%</option>
          </select>
        </div>
        <span class="mce-sep" />
        <label class="mce-label">
          {{ t('cutEditor.grid') }}
          <select v-model.number="gridStep" class="mce-select">
            <option :value="0">—</option>
            <option :value="1">1 sm</option>
            <option :value="5">5 sm</option>
            <option :value="10">10 sm</option>
          </select>
        </label>
        <span class="mce-sep" />
        <label class="mce-label" :title="t('cutEditor.maxScrapHint')">
          {{ t('cutEditor.maxScrap') }}
          <input v-model.number="maxScrap" class="mce-num" type="number" min="0" max="100" step="1" :aria-label="t('cutEditor.maxScrap')" />
          %
        </label>
        <label class="mce-label" :title="t('cutEditor.wasteStripHint')">
          {{ t('cutEditor.wasteStrip') }}
          <input v-model="wasteW" class="mce-num" type="number" min="0" step="0.1" placeholder="212" :aria-label="t('cutEditor.wasteWidth')" />
          ×
          <input v-model="wasteH" class="mce-num" type="number" min="0" step="0.1" placeholder="15" :aria-label="t('cutEditor.wasteHeight')" />
          sm
        </label>
        <template v-if="!readonly">
          <span class="mce-sep" />
          <SearchableSelect
            class="mce-sheet-pick"
            floating
            :model-value="addGoodsId"
            :options="sheetPickOptions"
            :placeholder="t('cutEditor.addSheet')"
            :search-placeholder="t('cutEditor.sheetSearch')"
            @update:model-value="(v: number) => (addGoodsId = v)"
          />
          <button type="button" class="mce-tool" :disabled="!addGoodsId" @click="addBoard">+ {{ t('cutEditor.addSheet') }}</button>
          <button type="button" class="mce-tool" :disabled="!unplaced.length" @click="autoPlaceAll">{{ t('cutEditor.autoPlace') }}</button>
          <button type="button" class="mce-tool" @click="rebuild">{{ t('cutEditor.reset') }}</button>
        </template>
        <span class="mce-legend">
          <b class="ok">ok</b> {{ t('cutEditor.statusOk').replace(/^ok — /, '') }}
          · <b class="bad">✕</b> {{ t('cutEditor.statusBad').replace(/^✕ — /, '') }}
        </span>
      </div>

      <div ref="scroller" class="mce-body" @wheel="onWheel" @pointerdown.self="selectedId = null">
        <section
          v-for="(board, bi) in boards"
          :key="board.id"
          class="mce-sheet"
          :class="{ target: drag?.started && drag.boardId === board.id }"
          :style="{ width: `${board.w * scale + 36}px` }"
        >
          <div class="mce-head">
            <span class="mce-head-title">{{ t('cutEditor.sheetTitle', { n: bi + 1, total: boards.length, w: fmt(board.w), h: fmt(board.h) }) }}</span>
            <span v-if="board.parts.length" class="mce-pct" :class="tone(usage(board))">{{ pct(usage(board)) }}%</span>
            <span v-else class="mce-pct bad">{{ t('cutEditor.emptySheet') }}</span>
            <span class="mce-muted">{{ kindLabel(board) }}<template v-if="board.name"> · {{ board.name }}</template></span>
            <span class="mce-chip useful">{{ t('cutEditor.usefulArea', { area: m2(freeArea(board, true)), pct: shareOf(freeArea(board, true), board.w * board.h) }) }}</span>
            <span class="mce-chip scrap">{{ t('cutEditor.scrapArea', { area: m2(freeArea(board, false)), pct: shareOf(freeArea(board, false), board.w * board.h) }) }}</span>
            <button
              v-if="!board.parts.length && !readonly"
              type="button"
              class="mce-tool danger small"
              @click="removeBoard(board)"
            >
              {{ t('cutEditor.removeEmpty') }}
            </button>
          </div>
          <div class="mce-frame">
            <svg
              class="mce-svg"
              :data-board="board.id"
              :width="board.w * scale"
              :height="board.h * scale"
              :viewBox="`0 0 ${board.w} ${board.h}`"
              @pointerdown="selectedId = null"
            >
              <defs>
                <pattern :id="`g-${board.id}`" width="10" height="10" patternUnits="userSpaceOnUse">
                  <path d="M 10 0 L 0 0 0 10" class="mce-grid" vector-effect="non-scaling-stroke" />
                </pattern>
              </defs>
              <rect class="mce-bg" x="0" y="0" :width="board.w" :height="board.h" />
              <rect x="0" y="0" :width="board.w" :height="board.h" :fill="`url(#g-${board.id})`" />
              <g v-for="(free, fi) in freeOf(board)" :key="`f-${fi}`" class="mce-free" :class="free.useful ? 'useful' : 'scrap'">
                <rect :x="free.x" :y="free.y" :width="free.w" :height="free.h" vector-effect="non-scaling-stroke" />
                <text
                  v-if="Math.min(free.w, free.h) * scale >= 12"
                  :x="free.x + free.w / 2"
                  :y="free.y + free.h / 2"
                  :font-size="fontOf(free)"
                  text-anchor="middle"
                  dominant-baseline="middle"
                >{{ fmt(free.w) }}×{{ fmt(free.h) }}</text>
                <title>{{ fmt(free.w) }}×{{ fmt(free.h) }} sm · {{ free.useful ? t('cutEditor.useful') : t('cutEditor.scrap') }}</title>
              </g>
              <g
                v-for="part in board.parts"
                :key="part.id"
                class="mce-part"
                :class="{ sel: part.id === selectedId, bad: badIds.has(part.id), lifted: drag?.started && drag.partId === part.id }"
                @pointerdown.stop="startDrag($event, part, board)"
                @dblclick.stop="rotatePart(part)"
              >
                <rect
                  :x="part.x"
                  :y="part.y"
                  :width="part.w"
                  :height="part.h"
                  :fill="colorOf(part.itemId)"
                  vector-effect="non-scaling-stroke"
                />
                <text
                  :x="part.x + part.w / 2"
                  :y="part.y + part.h / 2"
                  :font-size="fontOf(part)"
                  class="mce-text"
                  text-anchor="middle"
                  dominant-baseline="middle"
                >{{ fmt(part.w) }}×{{ fmt(part.h) }}</text>
                <text
                  v-if="part.h * scale >= 30"
                  :x="part.x + part.w / 2"
                  :y="part.y + part.h / 2 + fontOf(part) * 1.15"
                  :font-size="fontOf(part) * 0.8"
                  class="mce-status"
                  :class="badIds.has(part.id) ? 'bad' : 'ok'"
                  text-anchor="middle"
                  dominant-baseline="middle"
                >
                  {{ badIds.has(part.id) ? '✕' : 'ok' }}
                  <title>{{ badIds.has(part.id) ? t('cutEditor.statusBad') : t('cutEditor.statusOk') }}</title>
                </text>
                <g
                  v-if="part.rotatable && !readonly && Math.min(part.w, part.h) * scale >= 16"
                  class="mce-rot"
                  @pointerdown.stop
                  @click.stop="rotatePart(part)"
                >
                  <title>{{ t('cutEditor.rotate') }} 90°</title>
                  <rect
                    :x="part.x + part.w - 15 / scale"
                    :y="part.y + 1 / scale"
                    :width="14 / scale"
                    :height="14 / scale"
                    :rx="3 / scale"
                  />
                  <text
                    :x="part.x + part.w - 8 / scale"
                    :y="part.y + 8.5 / scale"
                    :font-size="11 / scale"
                    text-anchor="middle"
                    dominant-baseline="middle"
                  >↻</text>
                </g>
              </g>
              <rect
                v-if="drag?.started && drag.boardId === board.id"
                class="mce-ghost"
                :class="drag.check === 'ok' ? 'ok' : 'bad'"
                :x="drag.x"
                :y="drag.y"
                :width="drag.w"
                :height="drag.h"
                vector-effect="non-scaling-stroke"
              />
              <template v-if="drag?.started && drag.boardId === board.id && drag.check === 'ok'">
                <line
                  v-if="drag.guideX != null"
                  class="mce-guide"
                  :x1="drag.guideX"
                  :x2="drag.guideX"
                  y1="0"
                  :y2="board.h"
                  vector-effect="non-scaling-stroke"
                />
                <line
                  v-if="drag.guideY != null"
                  class="mce-guide"
                  :y1="drag.guideY"
                  :y2="drag.guideY"
                  x1="0"
                  :x2="board.w"
                  vector-effect="non-scaling-stroke"
                />
              </template>
            </svg>
            <span class="mce-dim-h">{{ fmt(board.h) }} sm</span>
          </div>
          <div class="mce-dim-w" :style="{ width: `${board.w * scale}px` }">{{ fmt(board.w) }} sm</div>
        </section>
      </div>

      <footer
        v-if="unplaced.length || drag?.started"
        class="mce-unplaced"
        :class="{ target: drag?.started && drag.overTray }"
        data-unplaced
      >
        <button type="button" class="mce-tray-toggle" @click="trayOpen = !trayOpen">
          {{ t('cutEditor.unplaced', { n: unplaced.length }) }} {{ trayOpen ? '▾' : '▴' }}
        </button>
        <div v-show="trayOpen" class="mce-tray">
          <div
            v-for="part in unplaced"
            :key="part.id"
            class="mce-mini"
            :class="{ sel: part.id === selectedId }"
            @pointerdown="startDrag($event, part, null)"
          >
            <svg :width="miniW(part)" :height="miniH(part)" :viewBox="`0 0 ${part.w} ${part.h}`">
              <rect x="0" y="0" :width="part.w" :height="part.h" :fill="colorOf(part.itemId)" class="mce-mini-rect" vector-effect="non-scaling-stroke" />
            </svg>
            <span>{{ fmt(part.w) }}×{{ fmt(part.h) }} sm</span>
          </div>
          <p v-if="!unplaced.length" class="mce-muted">{{ t('cutEditor.dropHere') }}</p>
        </div>
      </footer>

      <div v-if="toast" class="mce-toast" role="status">{{ toast }}</div>

      <div v-if="dialog" class="mce-modal" @pointerdown.self="dialog = null">
        <div class="mce-dialog">
          <template v-if="dialog === 'errors'">
            <h3>{{ t('cutEditor.errorsTitle') }}</h3>
            <ul class="mce-errors">
              <li v-for="(err, i) in errorList" :key="i">{{ t('cutEditor.errorRow', err) }}</li>
            </ul>
            <div class="mce-actions">
              <button type="button" class="mce-tool" @click="dialog = null">{{ t('cutEditor.cancel') }}</button>
            </div>
          </template>
          <template v-else-if="dialog === 'unplaced'">
            <h3>{{ t('cutEditor.unplacedTitle', { n: unplaced.length }) }}</h3>
            <div class="mce-actions">
              <button type="button" class="mce-primary" @click="doSave">{{ t('cutEditor.save') }}</button>
              <button type="button" class="mce-tool" @click="dialog = null; autoPlaceAll()">{{ t('cutEditor.autoPlace') }}</button>
              <button type="button" class="mce-tool" @click="dialog = null; closeAfterSave = false">{{ t('cutEditor.cancel') }}</button>
            </div>
          </template>
          <template v-else>
            <h3>{{ t('cutEditor.leaveTitle') }}</h3>
            <div class="mce-actions">
              <button type="button" class="mce-primary" @click="closeAfterSave = true; dialog = null; trySave()">{{ t('cutEditor.leaveSave') }}</button>
              <button type="button" class="mce-tool danger" @click="discard">{{ t('cutEditor.leaveDiscard') }}</button>
              <button type="button" class="mce-tool" @click="dialog = null">{{ t('cutEditor.cancel') }}</button>
            </div>
          </template>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { saveManualLayout, type ManualLayout } from '@/api/cutting'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import { formatApiError } from '@/api/http'
import {
  badParts,
  checkPlace,
  findSpot,
  freeRects,
  matchesOrder,
  newId,
  PART_PALETTE,
  parseLayout,
  rotateSpot,
  round,
  snap,
  totalUsage,
  usage,
  type EdBoard,
  type EdLayout,
  type EdPart,
  type EditorSheetOption,
  type FreeRect,
  type PlaceCheck,
} from './manualCut'

const props = defineProps<{
  orderId: number
  initial: EdLayout
  planLayout: EdLayout
  expected: Map<number, number>
  allowedGoods: Record<number, number[]>
  sheetOptions: EditorSheetOption[]
  minRemnantWidthCm: number
  minRemnantHeightCm: number
  planRevision?: number
  readonly?: boolean
}>()

const emit = defineEmits<{
  close: []
  saved: [ManualLayout]
  replan: [payload: { maxScrapPercent: number; wasteWidthCm: number | null; wasteHeightCm: number | null }]
}>()

const { t } = useI18n()

const BASE_PX_PER_CM = 2.5
const DEFAULT_ZOOM = 0.7
const ZOOM_MIN = 0.1
const ZOOM_MAX = 4
const ZOOM_PRESETS = [10, 20, 30, 40, 50, 60, 70]
const PER_ROW = 3
const HISTORY_LIMIT = 50
const PALETTE = PART_PALETTE

const draftKey = `wincrm_cut_draft_${props.orderId}`
const kerf = Number(props.initial.kerfCm) || 0
const boards = ref<EdBoard[]>(clone(props.initial.boards))
const unplaced = ref<EdPart[]>(clone(props.initial.unplaced))
const history = ref<string[]>([])
const savedSnapshot = ref(snapshot())
const selectedId = ref<string | null>(null)
const zoom = ref(DEFAULT_ZOOM)
const gridStep = ref(1)
const maxScrap = ref(8)
const wasteW = ref('')
const wasteH = ref('')
let replanTimer = 0
let replanArmed = false
const addGoodsId = ref(props.sheetOptions[0]?.goodsId || 0)
const sheetPickOptions = computed(() =>
  props.sheetOptions.map((o) => ({ value: o.goodsId, label: `${o.name} · ${o.w}×${o.h}` })),
)
const trayOpen = ref(true)
const toast = ref('')
const saving = ref(false)
const dialog = ref<null | 'errors' | 'unplaced' | 'leave'>(null)
const closeAfterSave = ref(false)
const scroller = ref<HTMLElement | null>(null)

interface DragState {
  partId: string
  fromBoardId: string | null
  offX: number
  offY: number
  w: number
  h: number
  startX: number
  startY: number
  clientY: number
  started: boolean
  boardId: string | null
  overTray: boolean
  x: number
  y: number
  check: PlaceCheck | 'material' | null
  guideX: number | null
  guideY: number | null
}
const drag = ref<DragState | null>(null)

const scale = computed(() => BASE_PX_PER_CM * zoom.value)
const total = computed(() => totalUsage(boards.value))
const badIds = computed(() => {
  const all = new Set<string>()
  for (const b of boards.value) {
    for (const id of badParts(b, kerf)) all.add(id)
  }
  return all
})
const freeByBoard = computed(() => {
  const map = new Map<string, FreeRect[]>()
  const minW = Number(props.minRemnantWidthCm) || 0
  const minH = Number(props.minRemnantHeightCm) || 0
  for (const b of boards.value) map.set(b.id, freeRects(b, minW, minH))
  return map
})
const totalFree = computed(() => {
  let useful = 0
  let scrap = 0
  let area = 0
  for (const b of boards.value) {
    if (!b.parts.length) continue
    useful += freeArea(b, true)
    scrap += freeArea(b, false)
    area += b.w * b.h
  }
  return { useful, scrap, area }
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
const areaFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 })
function m2(cm2: number) {
  return areaFormat.format(cm2 / 10_000)
}
const errorList = computed(() => {
  const list: { n: number; w: string; h: string }[] = []
  boards.value.forEach((b, i) => {
    for (const p of b.parts) {
      if (badIds.value.has(p.id)) list.push({ n: i + 1, w: fmt(p.w), h: fmt(p.h) })
    }
  })
  return list
})
const colorIndex = computed(() => {
  const map = new Map<number, number>()
  ;[...props.expected.keys()].forEach((id, i) => map.set(id, i))
  return map
})
const located = computed(() => (selectedId.value ? locate(selectedId.value) : null))
const selectedOnBoard = computed(() => Boolean(!props.readonly && located.value?.board))
const canRotate = computed(() => {
  const part = located.value?.part
  return Boolean(!props.readonly && part && part.rotatable && part.w !== part.h)
})
const current = computed(() => snapshot())
const dirty = computed(() => current.value !== savedSnapshot.value)

const numberFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 1 })
function fmt(v: number) {
  return numberFormat.format(Number(v) || 0)
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
  return PALETTE[(colorIndex.value.get(itemId) ?? 0) % PALETTE.length]
}
function fontOf(part: { w: number; h: number }) {
  const label = `${fmt(part.w)}×${fmt(part.h)}`
  return Math.max(Math.min(12 / scale.value, part.w / (label.length * 0.62), part.h * 0.4), 0.5)
}
function miniW(part: EdPart) {
  const k = 56 / Math.max(part.w, part.h)
  return Math.max(part.w * k, 6)
}
function miniH(part: EdPart) {
  const k = 56 / Math.max(part.w, part.h)
  return Math.max(part.h * k, 6)
}

function clone<T>(v: T): T {
  return JSON.parse(JSON.stringify(v)) as T
}
function snapshot() {
  return JSON.stringify({ boards: boards.value, unplaced: unplaced.value })
}
function restore(raw: string) {
  const data = JSON.parse(raw) as { boards: EdBoard[]; unplaced: EdPart[] }
  boards.value = data.boards
  unplaced.value = data.unplaced
}
function commit(fn: () => void) {
  history.value.push(snapshot())
  if (history.value.length > HISTORY_LIMIT) history.value.shift()
  fn()
}
function undo() {
  const prev = history.value.pop()
  if (prev) restore(prev)
  if (selectedId.value && !locate(selectedId.value)) selectedId.value = null
}

let toastTimer: ReturnType<typeof setTimeout> | undefined
function say(text: string) {
  toast.value = text
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2200)
}

function locate(partId: string) {
  for (const board of boards.value) {
    const index = board.parts.findIndex((p) => p.id === partId)
    if (index >= 0) return { board, part: board.parts[index]!, index }
  }
  const index = unplaced.value.findIndex((p) => p.id === partId)
  if (index >= 0) return { board: null, part: unplaced.value[index]!, index }
  return null
}

function allowed(board: EdBoard, itemId: number) {
  const list = props.allowedGoods[itemId]
  return !list?.length || list.includes(board.goodsId)
}

function takeOut(partId: string) {
  const loc = locate(partId)
  if (!loc) return null
  if (loc.board) loc.board.parts.splice(loc.index, 1)
  else unplaced.value.splice(loc.index, 1)
  return loc.part
}

function rotateSelected() {
  const loc = located.value
  if (!loc || props.readonly) return
  const { part, board } = loc
  if (!part.rotatable) {
    say(t('cutEditor.noRotate'))
    return
  }
  const spot = board ? rotateSpot(board, part, kerf) : null
  if (board && !spot) {
    say(t('cutEditor.rotateFail'))
    return
  }
  const moved = Boolean(spot && (spot.x !== part.x || spot.y !== part.y))
  commit(() => {
    const w = part.w
    part.w = part.h
    part.h = w
    if (spot) {
      part.x = spot.x
      part.y = spot.y
    }
  })
  if (moved) say(t('cutEditor.rotateMoved'))
}

function rotatePart(part: EdPart) {
  selectedId.value = part.id
  rotateSelected()
}

function removeSelected() {
  const loc = located.value
  if (!loc?.board || props.readonly) return
  commit(() => {
    const part = takeOut(loc.part.id)
    if (part) unplaced.value.push(part)
  })
  trayOpen.value = true
}

function addBoard() {
  const opt = props.sheetOptions.find((o) => o.goodsId === addGoodsId.value)
  if (!opt) return
  commit(() => {
    boards.value.push({ id: newId('b'), kind: 'sheet', goodsId: opt.goodsId, name: opt.name, w: opt.w, h: opt.h, parts: [] })
  })
  nextTick(() => scroller.value?.scrollTo({ top: scroller.value.scrollHeight, behavior: 'smooth' }))
}

function removeBoard(board: EdBoard) {
  if (board.parts.length) return
  commit(() => {
    boards.value = boards.value.filter((b) => b.id !== board.id)
  })
}

function autoPlaceAll() {
  if (!unplaced.value.length) return
  const before = snapshot()
  let placed = 0
  for (const part of [...unplaced.value]) {
    for (const board of boards.value) {
      if (!allowed(board, part.itemId)) continue
      const spot = findSpot(board, part, kerf)
      if (!spot) continue
      unplaced.value.splice(unplaced.value.indexOf(part), 1)
      Object.assign(part, spot)
      board.parts.push(part)
      placed += 1
      break
    }
  }
  if (placed) {
    history.value.push(before)
    if (history.value.length > HISTORY_LIMIT) history.value.shift()
    say(t('cutEditor.autoPlaced', { n: placed }))
  } else {
    say(t('cutEditor.autoPlaceNone'))
  }
}

function wasteCm(raw: string) {
  const n = Number(raw)
  return raw === '' || !Number.isFinite(n) || n <= 0 ? null : n
}

function queueReplan() {
  if (!replanArmed || props.readonly) return
  window.clearTimeout(replanTimer)
  replanTimer = window.setTimeout(() => {
    const pct = Number(maxScrap.value)
    emit('replan', {
      maxScrapPercent: Number.isFinite(pct) ? Math.min(100, Math.max(0, pct)) : 8,
      wasteWidthCm: wasteCm(wasteW.value),
      wasteHeightCm: wasteCm(wasteH.value),
    })
  }, 400)
}

watch([maxScrap, wasteW, wasteH], queueReplan)

watch(() => props.planRevision, (rev, prev) => {
  if (!rev || rev === prev) return
  commit(() => {
    const fresh = clone(props.planLayout)
    boards.value = fresh.boards
    unplaced.value = fresh.unplaced
  })
  selectedId.value = null
  say(t('cutEditor.replanned'))
})

function rebuild() {
  commit(() => {
    const fresh = clone(props.planLayout)
    boards.value = fresh.boards
    unplaced.value = fresh.unplaced
  })
  selectedId.value = null
}

function toCm(svg: Element, board: EdBoard, clientX: number, clientY: number) {
  const rect = svg.getBoundingClientRect()
  return {
    x: ((clientX - rect.left) / rect.width) * board.w,
    y: ((clientY - rect.top) / rect.height) * board.h,
  }
}

function startDrag(e: PointerEvent, part: EdPart, board: EdBoard | null) {
  if (e.button !== 0) return
  selectedId.value = part.id
  if (props.readonly) return
  e.preventDefault()
  let offX = part.w / 2
  let offY = part.h / 2
  const svg = (e.currentTarget as Element | null)?.closest('svg[data-board]')
  if (board && svg) {
    const pt = toCm(svg, board, e.clientX, e.clientY)
    offX = pt.x - part.x
    offY = pt.y - part.y
  }
  drag.value = {
    partId: part.id,
    fromBoardId: board?.id ?? null,
    offX,
    offY,
    w: part.w,
    h: part.h,
    startX: e.clientX,
    startY: e.clientY,
    clientY: e.clientY,
    started: false,
    boardId: null,
    overTray: false,
    x: 0,
    y: 0,
    check: null,
    guideX: null,
    guideY: null,
  }
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', onUp)
  window.addEventListener('pointercancel', onCancel)
}

function onMove(e: PointerEvent) {
  const d = drag.value
  if (!d) return
  d.clientY = e.clientY
  if (!d.started) {
    if (Math.hypot(e.clientX - d.startX, e.clientY - d.startY) < 4) return
    d.started = true
    startAutoScroll()
  }
  const hit = document.elementFromPoint(e.clientX, e.clientY)
  if (hit?.closest('[data-unplaced]')) {
    d.overTray = true
    d.boardId = null
    return
  }
  d.overTray = false
  const svg = hit?.closest('svg[data-board]') as SVGSVGElement | null
  const board = svg ? boards.value.find((b) => b.id === svg.dataset.board) : undefined
  if (!svg || !board) {
    d.boardId = null
    return
  }
  const pt = toCm(svg, board, e.clientX, e.clientY)
  const raw = { x: pt.x - d.offX, y: pt.y - d.offY, w: d.w, h: d.h }
  if (raw.w <= board.w) raw.x = Math.min(Math.max(raw.x, 0), board.w - raw.w)
  if (raw.h <= board.h) raw.y = Math.min(Math.max(raw.y, 0), board.h - raw.h)
  const snapped = snap(board, raw, kerf, 8 / scale.value, gridStep.value, d.partId)
  let rect = { x: snapped.x, y: snapped.y, w: d.w, h: d.h }
  let check: DragState['check'] = checkPlace(board, rect, kerf, d.partId)
  let guideX = snapped.guideX
  let guideY = snapped.guideY
  if (check !== 'ok') {
    const plain = { x: round(raw.x), y: round(raw.y), w: d.w, h: d.h }
    if (checkPlace(board, plain, kerf, d.partId) === 'ok') {
      rect = plain
      check = 'ok'
      guideX = null
      guideY = null
    }
  }
  const part = locate(d.partId)?.part
  if (part && !allowed(board, part.itemId)) check = 'material'
  d.boardId = board.id
  d.x = rect.x
  d.y = rect.y
  d.check = check
  d.guideX = guideX
  d.guideY = guideY
}

function stopListening() {
  window.removeEventListener('pointermove', onMove)
  window.removeEventListener('pointerup', onUp)
  window.removeEventListener('pointercancel', onCancel)
  stopAutoScroll()
}

function onCancel() {
  stopListening()
  drag.value = null
}

function onUp() {
  stopListening()
  const d = drag.value
  drag.value = null
  if (!d?.started) return
  if (d.overTray) {
    if (!d.fromBoardId) return
    commit(() => {
      const part = takeOut(d.partId)
      if (part) unplaced.value.push(part)
    })
    trayOpen.value = true
    return
  }
  if (!d.boardId) return
  if (d.check !== 'ok') {
    if (d.check === 'material') say(t('cutEditor.material'))
    else say(d.check === 'busy' ? t('cutEditor.busy') : t('cutEditor.outside'))
    return
  }
  const target = boards.value.find((b) => b.id === d.boardId)
  if (!target) return
  commit(() => {
    const part = takeOut(d.partId)
    if (!part) return
    part.x = d.x
    part.y = d.y
    target.parts.push(part)
  })
}

let scrollFrame = 0
function startAutoScroll() {
  const tick = () => {
    const el = scroller.value
    const d = drag.value
    if (el && d?.started) {
      const rect = el.getBoundingClientRect()
      if (d.clientY < rect.top + 56) el.scrollTop -= 14
      else if (d.clientY > rect.bottom - 56) el.scrollTop += 14
    }
    scrollFrame = requestAnimationFrame(tick)
  }
  scrollFrame = requestAnimationFrame(tick)
}
function stopAutoScroll() {
  cancelAnimationFrame(scrollFrame)
}

function clampZoom(v: number) {
  return Math.round(Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, v)) * 100) / 100
}
function zoomBy(dir: number) {
  zoom.value = clampZoom(dir > 0 ? zoom.value * 1.25 : zoom.value / 1.25)
}
function setZoomPercent(raw: string) {
  const n = Number(String(raw).replace(',', '.'))
  if (Number.isFinite(n) && n > 0) zoom.value = clampZoom(n / 100)
}
function onZoomPreset(e: Event) {
  const el = e.target as HTMLSelectElement
  setZoomPercent(el.value)
  el.value = ''
}
function fit() {
  const el = scroller.value
  const widest = Math.max(...boards.value.map((b) => b.w), 1)
  if (!el) return
  const room = el.clientWidth - 40 - PER_ROW * 60
  zoom.value = Math.floor(Math.min(ZOOM_MAX, Math.max(ZOOM_MIN, room / (PER_ROW * widest * BASE_PX_PER_CM))) * 100) / 100
}
function onWheel(e: WheelEvent) {
  if (!e.ctrlKey) return
  e.preventDefault()
  zoomBy(e.deltaY < 0 ? 1 : -1)
}

function trySave() {
  if (props.readonly) return
  if (badIds.value.size) {
    dialog.value = 'errors'
    closeAfterSave.value = false
    return
  }
  if (unplaced.value.length) {
    dialog.value = 'unplaced'
    return
  }
  doSave()
}

async function doSave() {
  dialog.value = null
  saving.value = true
  try {
    const layout: EdLayout = { version: 1, kerfCm: kerf, boards: boards.value, unplaced: unplaced.value }
    const res = await saveManualLayout(props.orderId, JSON.stringify(layout))
    savedSnapshot.value = snapshot()
    localStorage.removeItem(draftKey)
    say(t('cutEditor.saved'))
    if (res.data) emit('saved', res.data)
    if (closeAfterSave.value) emit('close')
  } catch (e) {
    say(formatApiError(e))
  } finally {
    saving.value = false
    closeAfterSave.value = false
  }
}

function requestClose() {
  if (dirty.value && !props.readonly) {
    dialog.value = 'leave'
    return
  }
  emit('close')
}

function discard() {
  localStorage.removeItem(draftKey)
  dialog.value = null
  emit('close')
}

function onKey(e: KeyboardEvent) {
  const el = e.target as HTMLElement | null
  if (el && ['INPUT', 'SELECT', 'TEXTAREA'].includes(el.tagName)) return
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault()
    undo()
  } else if (e.key === 'Delete' || e.key === 'Backspace') {
    if (selectedOnBoard.value) {
      e.preventDefault()
      removeSelected()
    }
  } else if (e.key.toLowerCase() === 'r' && !e.ctrlKey && !e.metaKey) {
    if (canRotate.value) rotateSelected()
  } else if (e.key === 'Escape') {
    if (dialog.value) dialog.value = null
    else if (selectedId.value) selectedId.value = null
    else requestClose()
  }
}

watch(current, (value) => {
  if (props.readonly) return
  if (value === savedSnapshot.value) localStorage.removeItem(draftKey)
  else localStorage.setItem(draftKey, JSON.stringify({ version: 1, kerfCm: kerf, ...JSON.parse(value) }))
})

onMounted(() => {
  const draft = props.readonly ? null : parseLayout(localStorage.getItem(draftKey))
  if (draft && matchesOrder(draft, props.expected)) {
    const raw = JSON.stringify({ boards: draft.boards, unplaced: draft.unplaced })
    if (raw !== savedSnapshot.value) {
      restore(raw)
      say(t('cutEditor.draftRestored'))
    }
  } else if (draft) {
    localStorage.removeItem(draftKey)
  }
  window.addEventListener('keydown', onKey)
  document.body.style.overflow = 'hidden'
  replanArmed = true
})

onBeforeUnmount(() => {
  stopListening()
  clearTimeout(toastTimer)
  window.clearTimeout(replanTimer)
  window.removeEventListener('keydown', onKey)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.mce {
  position: fixed;
  inset: 0;
  z-index: 100000;
  display: flex;
  flex-direction: column;
  background: #f2f4f7;
  color: #101828;
  user-select: none;
}
.mce-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 1rem;
  background: #fff;
  border-bottom: 1px solid #e4e7ec;
}
.mce-title { font-size: 1.05rem; font-weight: 600; margin: 0; }
.mce-grow { flex: 1; }
.mce-muted { color: #667085; font-size: 0.8125rem; }
.mce-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  font-size: 1.05rem;
}
.mce-icon:disabled { opacity: 0.4; cursor: default; }
.mce-primary {
  height: 2.25rem;
  border-radius: 0.5rem;
  background: #465fff;
  color: #fff;
  padding: 0 1rem;
  font-weight: 500;
  font-size: 0.875rem;
}
.mce-primary:disabled { opacity: 0.6; }
.mce-pct {
  border-radius: 999px;
  padding: 0.15rem 0.6rem;
  font-size: 0.8125rem;
  font-weight: 600;
}
.mce-pct.good { background: #ecfdf3; color: #067647; }
.mce-pct.warn { background: #fffaeb; color: #b54708; }
.mce-pct.bad { background: #fef3f2; color: #b42318; }
.mce-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.4rem;
  padding: 0.5rem 1rem;
  background: #fff;
  border-bottom: 1px solid #e4e7ec;
}
.mce-tool {
  height: 2rem;
  border-radius: 0.45rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  padding: 0 0.7rem;
  font-size: 0.8125rem;
  color: #344054;
}
.mce-tool.small { height: 1.75rem; }
.mce-tool:disabled { opacity: 0.45; cursor: default; }
.mce-tool.danger:not(:disabled) { border-color: #fda29b; color: #b42318; }
.mce-zoom {
  display: inline-flex;
  align-items: center;
  gap: 0.15rem;
  height: 2rem;
  border-radius: 0.45rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  padding-left: 0.4rem;
  font-size: 0.8125rem;
  color: #344054;
}
.mce-zoom-input {
  width: 3.2rem;
  border: 0;
  background: transparent;
  text-align: right;
  font-size: 0.8125rem;
  outline: none;
}
.mce-zoom-select {
  height: 100%;
  border: 0;
  border-left: 1px solid #d0d5dd;
  background: transparent;
  padding: 0 0.3rem;
  font-size: 0.8125rem;
  cursor: pointer;
}
.mce-sep { width: 1px; height: 1.4rem; background: #e4e7ec; margin: 0 0.2rem; }
.mce-label { display: inline-flex; align-items: center; gap: 0.35rem; font-size: 0.8125rem; color: #475467; }
.mce-sheet-pick { width: 16rem; }
.mce-sheet-pick :deep(.field-btn) { height: 2rem; border-radius: 0.45rem; padding: 0 0.5rem; font-size: 0.8125rem; }
.mce-select,
.mce-num {
  height: 2rem;
  border-radius: 0.45rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  padding: 0 0.4rem;
  font-size: 0.8125rem;
}
.mce-num { width: 4.2rem; }
.mce-legend { margin-left: auto; font-size: 0.75rem; color: #667085; }
.mce-legend .ok { color: #067647; }
.mce-legend .bad { color: #b42318; }
.mce-body {
  flex: 1;
  overflow: auto;
  padding: 1rem 1.25rem 2rem;
  display: grid;
  grid-template-columns: repeat(3, max-content);
  align-content: start;
  align-items: start;
  gap: 1rem 0.75rem;
}
.mce-sheet {
  padding: 0.5rem;
  border-radius: 0.75rem;
  border: 2px solid transparent;
  box-sizing: content-box;
}
.mce-sheet.target { border-color: #465fff; background: rgb(70 95 255 / 6%); }
.mce-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
  font-size: 0.875rem;
}
.mce-head-title { font-weight: 600; }
.mce-frame { display: flex; align-items: center; gap: 0.35rem; }
.mce-svg { display: block; touch-action: none; box-shadow: 0 1px 3px rgb(16 24 40 / 12%); }
.mce-bg { fill: #fff; }
.mce-grid { fill: none; stroke: #eaecf0; stroke-width: 1; }
.mce-dim-h { writing-mode: vertical-rl; font-size: 0.75rem; color: #667085; }
.mce-dim-w { text-align: center; font-size: 0.75rem; color: #667085; margin-top: 0.2rem; }
.mce-free { pointer-events: none; }
.mce-free rect { stroke-width: 1; stroke-dasharray: 4 3; }
.mce-free.useful rect { fill: rgb(18 183 106 / 16%); stroke: #12b76a; }
.mce-free.scrap rect { fill: rgb(240 68 56 / 12%); stroke: #f97066; }
.mce-free text { font-weight: 600; }
.mce-free.useful text { fill: #067647; }
.mce-free.scrap text { fill: #b42318; }
.mce-chip {
  border-radius: 999px;
  padding: 0.1rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 500;
}
.mce-chip.useful { background: #ecfdf3; color: #067647; }
.mce-chip.scrap { background: #fef3f2; color: #b42318; }
.mce-part { cursor: grab; touch-action: none; }
.mce-part rect { stroke: #475467; stroke-width: 1; }
.mce-part.sel rect { stroke: #2563eb; stroke-width: 3; filter: brightness(0.9); }
.mce-part.bad rect { stroke: #d92d20; stroke-width: 2; }
.mce-part.lifted { opacity: 0.35; }
.mce-text { fill: #101828; font-weight: 600; pointer-events: none; }
.mce-status { pointer-events: auto; font-weight: 600; }
.mce-status.ok { fill: #067647; }
.mce-status.bad { fill: #d92d20; }
.mce-rot { cursor: pointer; }
.mce-part .mce-rot rect { fill: rgb(255 255 255 / 85%); stroke: #98a2b3; stroke-width: 1; filter: none; }
.mce-rot text { fill: #344054; font-weight: 700; pointer-events: none; }
.mce-part .mce-rot:hover rect { fill: #465fff; stroke: #465fff; }
.mce-rot:hover text { fill: #fff; }
.mce-ghost { pointer-events: none; stroke-width: 2; stroke-dasharray: 6 4; }
.mce-ghost.ok { fill: rgb(34 197 94 / 35%); stroke: #16a34a; }
.mce-ghost.bad { fill: rgb(239 68 68 / 35%); stroke: #dc2626; }
.mce-guide { stroke: #f79009; stroke-width: 1; stroke-dasharray: 4 3; pointer-events: none; }
.mce-unplaced {
  background: #fff;
  border-top: 1px solid #e4e7ec;
  padding: 0.5rem 1rem calc(0.75rem + env(safe-area-inset-bottom));
  min-height: 3rem;
}
.mce-unplaced.target { background: #fef3f2; border-top-color: #f97066; }
.mce-tray-toggle { font-weight: 600; font-size: 0.875rem; color: #344054; }
.mce-tray { display: flex; gap: 0.75rem; overflow-x: auto; padding-top: 0.5rem; min-height: 5.5rem; align-items: flex-end; }
.mce-mini {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.25rem;
  padding: 0.4rem;
  border-radius: 0.5rem;
  border: 2px solid transparent;
  cursor: grab;
  touch-action: none;
  font-size: 0.75rem;
  color: #475467;
  flex: 0 0 auto;
}
.mce-mini.sel { border-color: #2563eb; }
.mce-mini-rect { stroke: #475467; stroke-width: 1; }
.mce-toast {
  position: fixed;
  left: 50%;
  bottom: 7.5rem;
  transform: translateX(-50%);
  background: #101828;
  color: #fff;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  z-index: 2;
}
.mce-modal {
  position: fixed;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgb(16 24 40 / 45%);
  z-index: 3;
}
.mce-dialog {
  width: min(28rem, calc(100vw - 2rem));
  border-radius: 0.75rem;
  background: #fff;
  padding: 1.25rem;
}
.mce-dialog h3 { margin: 0 0 0.75rem; font-size: 1rem; font-weight: 600; }
.mce-errors { max-height: 14rem; overflow: auto; margin: 0 0 0.75rem; padding-left: 1.1rem; list-style: disc; font-size: 0.875rem; color: #b42318; }
.mce-actions { display: flex; flex-wrap: wrap; gap: 0.5rem; justify-content: flex-end; }

:global(html.dark) .mce { background: #0c111d; color: rgba(255, 255, 255, 0.9); }
:global(html.dark) .mce-bar,
:global(html.dark) .mce-tools,
:global(html.dark) .mce-unplaced,
:global(html.dark) .mce-dialog { background: #161b26; border-color: #1f242f; }
:global(html.dark) .mce-icon,
:global(html.dark) .mce-tool,
:global(html.dark) .mce-select,
:global(html.dark) .mce-num,
:global(html.dark) .mce-zoom { background: #1f242f; border-color: #333741; color: rgba(255, 255, 255, 0.85); }
:global(html.dark) .mce-bg { fill: #1f242f; }
:global(html.dark) .mce-grid { stroke: #2b313d; }
:global(html.dark) .mce-tray-toggle { color: rgba(255, 255, 255, 0.85); }
:global(html.dark) .mce-unplaced.target { background: rgb(240 68 56 / 15%); }
</style>
