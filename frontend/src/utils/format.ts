export function money(v?: number | null) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(Number(v))
}

/** Group digits while typing: "2000000.5" -> "2 000 000.5" (max 2 decimals). */
export function formatAmountInput(raw: string): { text: string; value: number } {
  const cleaned = raw.replace(/\s/g, '').replace(',', '.').replace(/[^\d.]/g, '')
  const [intPart = '', ...rest] = cleaned.split('.')
  const fraction = rest.join('').slice(0, 2)
  const grouped = intPart.replace(/^0+(?=\d)/, '').replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  return {
    text: cleaned.includes('.') ? `${grouped}.${fraction}` : grouped,
    value: Number(`${intPart || '0'}.${fraction || '0'}`),
  }
}

export function amountToText(v?: number | null): string {
  if (!v) return ''
  return formatAmountInput(String(v)).text
}

const pad2 = (n: number) => String(n).padStart(2, '0')

/**
 * ISO/local datetime → "11.04.2026 10:43"
 * Vaqt bo‘lmasa → "11.04.2026"
 */
export function formatDate(v?: string | null) {
  if (!v) return '—'
  const s = String(v).trim()
  const m = s.match(
    /^(\d{4})-(\d{2})-(\d{2})(?:[T\s](\d{2}):(\d{2})(?::\d{2}(?:\.\d+)?)?)?/,
  )
  if (m) {
    const [, y, mo, d, hh, mm] = m
    if (hh != null && mm != null) return `${d}.${mo}.${y} ${hh}:${mm}`
    return `${d}.${mo}.${y}`
  }
  const dt = new Date(s)
  if (!Number.isNaN(dt.getTime())) {
    return `${pad2(dt.getDate())}.${pad2(dt.getMonth() + 1)}.${dt.getFullYear()} ${pad2(dt.getHours())}:${pad2(dt.getMinutes())}`
  }
  return s
}

/** Faqat sana: "11.04.2026" */
export function formatDateOnly(v?: string | null) {
  if (!v) return '—'
  const full = formatDate(v)
  if (full === '—') return full
  return full.slice(0, 10)
}

/** Buyurtma sanasi yaralganda 00:00 bo‘lib saqlangan bo‘lsa, haqiqiy soatni ko‘rsatadi. */
export function formatOrderDate(orderDate?: string | null, createdAt?: string | null) {
  if (!orderDate) return formatDate(createdAt)
  const normalized = orderDate.replace('T', ' ')
  const clock = normalized.length >= 16 ? normalized.slice(11, 16) : ''
  const created = createdAt?.replace('T', ' ')
  if ((!clock || clock === '00:00') && created && created.length >= 16) {
    return formatDate(`${normalized.slice(0, 10)}T${created.slice(11, 16)}:00`)
  }
  return formatDate(orderDate)
}

export function formatKg(v?: number | null) {
  const n = Number(v)
  if (!Number.isFinite(n) || n <= 0) return '—'
  return `${new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 }).format(n)} kg`
}

/** @deprecated formatDate bilan bir xil — "11.04.2026 10:43" */
export function formatDmyTime(v?: string | null) {
  return formatDate(v)
}

export function toApiDate(local: string) {
  if (!local) return local
  return local.length === 16 ? `${local}:00` : local
}

export function nowLocal() {
  const now = new Date()
  return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}T${pad2(now.getHours())}:${pad2(now.getMinutes())}`
}

export function today() {
  const now = new Date()
  return `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`
}
