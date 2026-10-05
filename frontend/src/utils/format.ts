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

export function formatDate(v?: string | null) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 16)
}

/** "2026-10-05T10:16:00" -> "05.10.2026 10:16" */
export function formatDmyTime(v?: string | null) {
  if (!v) return '—'
  const [y, m, d] = v.slice(0, 10).split('-')
  const time = v.slice(11, 16)
  return `${d}.${m}.${y}${time ? ` ${time}` : ''}`
}

export function toApiDate(local: string) {
  if (!local) return local
  return local.length === 16 ? `${local}:00` : local
}

export function nowLocal() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

export function today() {
  return new Date().toISOString().slice(0, 10)
}
