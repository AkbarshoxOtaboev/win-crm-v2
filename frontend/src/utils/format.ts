export function money(v?: number | null) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(Number(v))
}

export function formatDate(v?: string | null) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 16)
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
