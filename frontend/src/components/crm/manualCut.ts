import type { CuttingResult, CuttingSheetLayout } from '@/api/cutting'

/** Lengths in cm, origin at the sheet's top-left corner. */
export interface EdPart {
  id: string
  itemId: number
  w: number
  h: number
  x: number
  y: number
  rotatable: boolean
}

export type BoardKind = 'sheet' | 'remnant' | 'stock'

export interface EdBoard {
  id: string
  kind: BoardKind
  goodsId: number
  name: string
  w: number
  h: number
  parts: EdPart[]
}

export interface EdLayout {
  version: 1
  kerfCm: number
  boards: EdBoard[]
  unplaced: EdPart[]
}

export interface PlanSource {
  itemId: number
  kind: 'sheet' | 'stock' | 'planned'
  goodsId: number
  name: string
  rotatable: boolean
  result: CuttingResult
}

export interface EditorSheetOption {
  goodsId: number
  name: string
  w: number
  h: number
}

export interface Rect {
  x: number
  y: number
  w: number
  h: number
}

export type PlaceCheck = 'ok' | 'out' | 'busy'

const EPS = 0.01

export const PART_PALETTE = ['#9BE7EA', '#B7E4A8', '#FFD8A8', '#D0BFFF', '#FFC9C9', '#A5D8FF', '#FFEC99', '#C3FAE8', '#EEBEFA', '#D8F5A2', '#FCC2D7', '#BAC8FF']

export function newId(prefix: string) {
  return `${prefix}${Math.random().toString(36).slice(2, 10)}`
}

function partsOf(result: CuttingResult, sheet: CuttingSheetLayout) {
  const kerf = Number(result.kerfMm || 0) / 10
  const extra = sheet.placed ?? []
  const gridCount = Math.max(0, sheet.pieces - extra.length)
  const stepX = result.layoutPieceWidth + kerf
  const stepY = result.layoutPieceHeight + kerf
  return [
    ...Array.from({ length: gridCount }, (_, i) => ({
      x: (i % sheet.columns) * stepX,
      y: Math.floor(i / sheet.columns) * stepY,
      w: result.layoutPieceWidth,
      h: result.layoutPieceHeight,
    })),
    ...extra.map((p) => ({ x: p.x, y: p.y, w: p.width, h: p.height })),
  ]
}

interface Host extends Rect {
  board: EdBoard
  goodsId: number
  used: boolean
}

/**
 * Avtomatik rejadan jismoniy varaqlar: rejadagi qoldiqdan kesilgan detal
 * o'sha qoldiq chiqqan varaqqa, qoldiqning o'z joyiga qo'yiladi.
 */
export function buildBoards(sources: PlanSource[], kerfCm: number): EdLayout {
  const boards: EdBoard[] = []
  const hosts: Host[] = []

  const lay = (board: EdBoard, offX: number, offY: number, src: PlanSource, sheet: CuttingSheetLayout) => {
    for (const p of partsOf(src.result, sheet)) {
      board.parts.push({ id: newId('p'), itemId: src.itemId, rotatable: src.rotatable, ...p, x: p.x + offX, y: p.y + offY })
    }
    for (const r of sheet.remnants) {
      if (!r.useful) continue
      hosts.push({ board, goodsId: src.goodsId, used: false, x: r.x + offX, y: r.y + offY, w: r.width, h: r.height })
    }
  }

  const standalone = (src: PlanSource, sheet: CuttingSheetLayout) => {
    const kind: BoardKind = src.kind === 'sheet' ? 'sheet' : src.kind === 'stock' ? 'stock' : 'remnant'
    const board: EdBoard = {
      id: newId('b'),
      kind,
      goodsId: src.goodsId,
      name: src.name,
      w: Number(src.result.sheetWidth),
      h: Number(src.result.sheetHeight),
      parts: [],
    }
    boards.push(board)
    lay(board, 0, 0, src, sheet)
  }

  const pending: { src: PlanSource; sheet: CuttingSheetLayout }[] = []
  for (const src of sources) {
    for (const sheet of src.result.sheets || []) {
      for (let c = 0; c < Math.max(1, sheet.count); c += 1) {
        if (src.kind === 'planned') pending.push({ src, sheet })
        else standalone(src, sheet)
      }
    }
  }

  let progress = true
  while (pending.length && progress) {
    progress = false
    for (let i = 0; i < pending.length; i += 1) {
      const { src, sheet } = pending[i]!
      const w = Number(src.result.sheetWidth)
      const h = Number(src.result.sheetHeight)
      const free = hosts.filter((host) => !host.used && host.goodsId === src.goodsId && host.w >= w - EPS && host.h >= h - EPS)
      if (!free.length) continue
      const host = free.find((f) => Math.abs(f.w - w) < EPS && Math.abs(f.h - h) < EPS)
        ?? free.sort((a, b) => a.w * a.h - b.w * b.h)[0]!
      host.used = true
      lay(host.board, host.x, host.y, src, sheet)
      pending.splice(i, 1)
      i -= 1
      progress = true
    }
  }
  for (const { src, sheet } of pending) standalone(src, sheet)

  return { version: 1, kerfCm, boards, unplaced: [] }
}

function clash(a: Rect, b: Rect, kerf: number) {
  return a.x < b.x + b.w + kerf - EPS
    && b.x < a.x + a.w + kerf - EPS
    && a.y < b.y + b.h + kerf - EPS
    && b.y < a.y + a.h + kerf - EPS
}

export function inside(board: EdBoard, r: Rect) {
  return r.x >= -EPS && r.y >= -EPS && r.x + r.w <= board.w + EPS && r.y + r.h <= board.h + EPS
}

export function checkPlace(board: EdBoard, r: Rect, kerf: number, ignoreId?: string): PlaceCheck {
  if (!inside(board, r)) return 'out'
  for (const p of board.parts) {
    if (p.id === ignoreId) continue
    if (clash(r, p, kerf)) return 'busy'
  }
  return 'ok'
}

/** Varaqdagi xatoli (to'qnashgan yoki chiqib ketgan) detallar. */
export function badParts(board: EdBoard, kerf: number) {
  const bad = new Set<string>()
  for (const p of board.parts) {
    if (!inside(board, p)) bad.add(p.id)
  }
  for (let i = 0; i < board.parts.length; i += 1) {
    for (let j = i + 1; j < board.parts.length; j += 1) {
      const a = board.parts[i]!
      const b = board.parts[j]!
      if (clash(a, b, kerf)) {
        bad.add(a.id)
        bad.add(b.id)
      }
    }
  }
  return bad
}

export interface Snapped {
  x: number
  y: number
  guideX: number | null
  guideY: number | null
}

/** Varaq chetlari, qo'shni detal qirralari (propil bilan) va to'r qadamiga yopishtirish. */
export function snap(board: EdBoard, r: Rect, kerf: number, threshold: number, grid: number, ignoreId?: string): Snapped {
  const xs: { v: number; line: number }[] = [{ v: 0, line: 0 }, { v: board.w - r.w, line: board.w }]
  const ys: { v: number; line: number }[] = [{ v: 0, line: 0 }, { v: board.h - r.h, line: board.h }]
  for (const p of board.parts) {
    if (p.id === ignoreId) continue
    xs.push(
      { v: p.x + p.w + kerf, line: p.x + p.w + kerf },
      { v: p.x - r.w - kerf, line: p.x - kerf },
      { v: p.x, line: p.x },
      { v: p.x + p.w - r.w, line: p.x + p.w },
    )
    ys.push(
      { v: p.y + p.h + kerf, line: p.y + p.h + kerf },
      { v: p.y - r.h - kerf, line: p.y - kerf },
      { v: p.y, line: p.y },
      { v: p.y + p.h - r.h, line: p.y + p.h },
    )
  }
  const pick = (value: number, list: { v: number; line: number }[]) => {
    let best: { v: number; line: number } | null = null
    for (const c of list) {
      const d = Math.abs(c.v - value)
      if (d <= threshold && (!best || d < Math.abs(best.v - value))) best = c
    }
    return best
  }
  const bx = pick(r.x, xs)
  const by = pick(r.y, ys)
  const onGrid = (v: number) => (grid > 0 ? Math.round(v / grid) * grid : v)
  return {
    x: round(bx ? bx.v : onGrid(r.x)),
    y: round(by ? by.v : onGrid(r.y)),
    guideX: bx ? bx.line : null,
    guideY: by ? by.line : null,
  }
}

export function round(v: number) {
  return Math.round(v * 100) / 100
}

/** Detal uchun varaqdagi birinchi bo'sh joy (yuqoridan pastga, chapdan o'ngga). */
export function findSpot(board: EdBoard, part: Pick<EdPart, 'w' | 'h' | 'rotatable'>, kerf: number) {
  const points: { x: number; y: number }[] = [{ x: 0, y: 0 }]
  for (const p of board.parts) {
    points.push(
      { x: p.x + p.w + kerf, y: p.y },
      { x: p.x, y: p.y + p.h + kerf },
      { x: p.x + p.w + kerf, y: 0 },
      { x: 0, y: p.y + p.h + kerf },
    )
  }
  points.sort((a, b) => a.y - b.y || a.x - b.x)
  const sizes = part.rotatable && part.w !== part.h ? [[part.w, part.h], [part.h, part.w]] : [[part.w, part.h]]
  for (const pt of points) {
    for (const [w, h] of sizes) {
      const rect = { x: round(pt.x), y: round(pt.y), w: w!, h: h! }
      if (checkPlace(board, rect, kerf) === 'ok') return rect
    }
  }
  return null
}

export interface FreeRect extends Rect {
  useful: boolean
}

/**
 * Varaqdagi bo'sh joylar: detallar qirralari bo'yicha katakchalarga bo'linadi,
 * bo'sh katakchalar avval eniga, keyin bo'yiga qo'shilib to'rtburchak bo'ladi.
 */
export function freeRects(board: EdBoard, minW: number, minH: number): FreeRect[] {
  const xs = uniq([0, board.w, ...board.parts.flatMap((p) => [p.x, p.x + p.w])], board.w)
  const ys = uniq([0, board.h, ...board.parts.flatMap((p) => [p.y, p.y + p.h])], board.h)
  const nx = xs.length - 1
  const ny = ys.length - 1
  const busy: boolean[][] = []
  for (let j = 0; j < ny; j += 1) {
    const cy = (ys[j]! + ys[j + 1]!) / 2
    busy.push(Array.from({ length: nx }, (_, i) => {
      const cx = (xs[i]! + xs[i + 1]!) / 2
      return board.parts.some((p) => cx > p.x && cx < p.x + p.w && cy > p.y && cy < p.y + p.h)
    }))
  }
  const out: FreeRect[] = []
  for (let j = 0; j < ny; j += 1) {
    for (let i = 0; i < nx; i += 1) {
      if (busy[j]![i]) continue
      let i2 = i
      while (i2 + 1 < nx && !busy[j]![i2 + 1]) i2 += 1
      let j2 = j
      while (j2 + 1 < ny && busy[j2 + 1]!.slice(i, i2 + 1).every((v) => !v)) j2 += 1
      for (let jj = j; jj <= j2; jj += 1) {
        for (let ii = i; ii <= i2; ii += 1) busy[jj]![ii] = true
      }
      const w = round(xs[i2 + 1]! - xs[i]!)
      const h = round(ys[j2 + 1]! - ys[j]!)
      if (w < 0.5 || h < 0.5) continue
      const useful = (w >= minW && h >= minH) || (h >= minW && w >= minH)
      out.push({ x: xs[i]!, y: ys[j]!, w, h, useful })
    }
  }
  return out
}

function uniq(values: number[], max: number) {
  return [...new Set(values.map((v) => round(Math.min(Math.max(v, 0), max))))].sort((a, b) => a - b)
}

/**
 * 90° burilgan detal uchun joy: avval o'z burchaklari va markazi atrofida,
 * bo'lmasa varaqdagi eng yaqin bo'sh joy.
 */
export function rotateSpot(board: EdBoard, part: EdPart, kerf: number): Rect | null {
  const w = part.h
  const h = part.w
  const clampX = (v: number) => round(Math.min(Math.max(v, 0), Math.max(board.w - w, 0)))
  const clampY = (v: number) => round(Math.min(Math.max(v, 0), Math.max(board.h - h, 0)))
  const anchors = [
    { x: part.x, y: part.y },
    { x: part.x + (part.w - w) / 2, y: part.y + (part.h - h) / 2 },
    { x: part.x + part.w - w, y: part.y },
    { x: part.x, y: part.y + part.h - h },
    { x: part.x + part.w - w, y: part.y + part.h - h },
  ]
  for (const a of anchors) {
    const rect = { x: clampX(a.x), y: clampY(a.y), w, h }
    if (checkPlace(board, rect, kerf, part.id) === 'ok') return rect
  }
  const points: { x: number; y: number }[] = [{ x: 0, y: 0 }, { x: board.w - w, y: 0 }, { x: 0, y: board.h - h }]
  for (const p of board.parts) {
    if (p.id === part.id) continue
    points.push(
      { x: p.x + p.w + kerf, y: p.y },
      { x: p.x, y: p.y + p.h + kerf },
      { x: p.x - w - kerf, y: p.y },
      { x: p.x, y: p.y - h - kerf },
      { x: p.x + p.w + kerf, y: 0 },
      { x: 0, y: p.y + p.h + kerf },
      { x: p.x + p.w + kerf, y: board.h - h },
      { x: board.w - w, y: p.y + p.h + kerf },
    )
  }
  const cx = part.x + part.w / 2
  const cy = part.y + part.h / 2
  let best: Rect | null = null
  let bestD = Infinity
  for (const pt of points) {
    const rect = { x: round(pt.x), y: round(pt.y), w, h }
    if (checkPlace(board, rect, kerf, part.id) !== 'ok') continue
    const d = Math.hypot(rect.x + w / 2 - cx, rect.y + h / 2 - cy)
    if (d < bestD) {
      bestD = d
      best = rect
    }
  }
  return best
}

export function partsArea(board: EdBoard) {
  return board.parts.reduce((sum, p) => sum + p.w * p.h, 0)
}

export function usage(board: EdBoard) {
  const area = board.w * board.h
  return area > 0 ? (partsArea(board) / area) * 100 : 0
}

/** Faqat ishlatilgan (bo'sh bo'lmagan) varaqlar bo'yicha. */
export function totalUsage(boards: EdBoard[]) {
  let parts = 0
  let area = 0
  for (const b of boards) {
    if (!b.parts.length) continue
    parts += partsArea(b)
    area += b.w * b.h
  }
  return area > 0 ? (parts / area) * 100 : 0
}

export function countsByItem(layout: Pick<EdLayout, 'boards' | 'unplaced'>) {
  const counts = new Map<number, number>()
  for (const b of layout.boards) {
    for (const p of b.parts) counts.set(p.itemId, (counts.get(p.itemId) || 0) + 1)
  }
  for (const p of layout.unplaced) counts.set(p.itemId, (counts.get(p.itemId) || 0) + 1)
  return counts
}

export function matchesOrder(layout: EdLayout | null, expected: Map<number, number>) {
  if (!layout || !Array.isArray(layout.boards) || !Array.isArray(layout.unplaced)) return false
  const counts = countsByItem(layout)
  if (counts.size !== expected.size) return false
  for (const [itemId, n] of expected) {
    if (counts.get(itemId) !== n) return false
  }
  return true
}

export function parseLayout(raw: string | null | undefined): EdLayout | null {
  if (!raw) return null
  try {
    const data = JSON.parse(raw) as EdLayout
    return data && Array.isArray(data.boards) ? { ...data, unplaced: data.unplaced || [] } : null
  } catch {
    return null
  }
}
