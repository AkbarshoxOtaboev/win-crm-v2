/** Uzbek phone: +998-(97)-221-88-96 */

export function phoneDigits(value?: string | null): string {
  return String(value || '').replace(/\D/g, '')
}

/** Normalize to 12 digits (998XXXXXXXXX) when possible */
export function normalizeUzPhoneDigits(value?: string | null): string {
  let d = phoneDigits(value)
  if (d.startsWith('998')) return d.slice(0, 12)
  if (d.length <= 9) return d ? `998${d}`.slice(0, 12) : ''
  return d.slice(0, 12)
}

/**
 * Format as +998-(XX)-XXX-XX-XX while typing.
 * Accepts raw input or already formatted strings.
 */
export function formatUzPhone(value?: string | null): string {
  let d = phoneDigits(value)
  if (d.startsWith('998')) d = d.slice(3)
  d = d.slice(0, 9)

  if (!d.length) return '+998-'

  const op = d.slice(0, 2)
  const mid = d.slice(2, 5)
  const a = d.slice(5, 7)
  const b = d.slice(7, 9)

  let out = '+998-(' + op
  if (op.length < 2) return out
  out += ')'
  if (mid.length) out += '-' + mid
  if (a.length) out += '-' + a
  if (b.length) out += '-' + b
  return out
}

export function isCompleteUzPhone(value?: string | null): boolean {
  return normalizeUzPhoneDigits(value).length === 12
}

/** True if phones refer to the same number (digit-normalized). */
export function phonesEqual(a?: string | null, b?: string | null): boolean {
  const da = normalizeUzPhoneDigits(a)
  const db = normalizeUzPhoneDigits(b)
  return da.length === 12 && da === db
}

/**
 * Match client phone/name against search query.
 * Digits match ignoring formatting: 97, 97221, +998-(97) all work.
 */
export function matchesClientQuery(
  query: string,
  fields: { fullName?: string; phone?: string; additionalPhone?: string; address?: string; inn?: string },
): boolean {
  const q = query.trim().toLowerCase()
  if (!q) return true

  const qDigits = phoneDigits(q)
  const textHit = [fields.fullName, fields.phone, fields.additionalPhone, fields.address, fields.inn]
    .filter(Boolean)
    .some((v) => String(v).toLowerCase().includes(q))

  if (textHit) return true

  if (qDigits.length >= 2) {
    const p1 = phoneDigits(fields.phone)
    const p2 = phoneDigits(fields.additionalPhone)
    return p1.includes(qDigits) || p2.includes(qDigits)
  }

  return false
}
