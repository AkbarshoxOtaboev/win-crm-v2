/**
 * Uzbek license plates, always upper-case:
 *  - personal: 90 B 123 BB  (region, letter, 3 digits, 2 letters)
 *  - company:  90 123 BBB   (region, 3 digits, 3 letters)
 */
export type CarNumberKind = 'PERSONAL' | 'COMPANY'

const FORMATS: Record<CarNumberKind, { pattern: string; groups: number[]; example: string }> = {
  PERSONAL: { pattern: 'DDLDDDLL', groups: [2, 1, 3, 2], example: '90 B 123 BB' },
  COMPANY: { pattern: 'DDDDDLLL', groups: [2, 3, 3], example: '90 123 BBB' },
}

function cleanCarNumber(raw?: string | null) {
  return String(raw || '')
    .toUpperCase()
    .replace(/[^0-9A-Z]/g, '')
}

/** Saqlangan raqamdan turini aniqlash: region'dan keyin raqam kelsa - firma. */
export function detectCarNumberKind(raw?: string | null): CarNumberKind {
  return /^\d{3}/.test(cleanCarNumber(raw)) ? 'COMPANY' : 'PERSONAL'
}

export function carNumberExample(kind: CarNumberKind) {
  return FORMATS[kind].example
}

export function formatCarNumber(raw?: string | null, kind: CarNumberKind = detectCarNumberKind(raw)): string {
  const { pattern, groups } = FORMATS[kind]

  let out = ''
  for (const c of cleanCarNumber(raw)) {
    if (out.length >= pattern.length) break
    const needDigit = pattern[out.length] === 'D'
    if (needDigit ? /\d/.test(c) : /[A-Z]/.test(c)) out += c
  }

  const parts: string[] = []
  let i = 0
  for (const g of groups) {
    if (i >= out.length) break
    parts.push(out.slice(i, i + g))
    i += g
  }
  return parts.join(' ')
}

export function isCompleteCarNumber(value?: string | null, kind: CarNumberKind = detectCarNumberKind(value)): boolean {
  return formatCarNumber(value, kind).replace(/\s/g, '').length === FORMATS[kind].pattern.length
}
