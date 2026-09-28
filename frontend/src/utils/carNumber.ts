/**
 * Uzbek license plates, always upper-case:
 *  - personal: 90 B 123 BB  (region, letter, 3 digits, 2 letters)
 *  - company:  01 123 ABC   (region, 3 digits, 3 letters)
 */
const PERSONAL = { pattern: 'DDLDDDLL', groups: [2, 1, 3, 2] }
const COMPANY = { pattern: 'DDDDDLLL', groups: [2, 3, 3] }

export function formatCarNumber(raw?: string | null): string {
  const chars = String(raw || '')
    .toUpperCase()
    .replace(/[^0-9A-Z]/g, '')
  const { pattern, groups } = /^\d{3}/.test(chars) ? COMPANY : PERSONAL

  let out = ''
  for (const c of chars) {
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

export function isCompleteCarNumber(value?: string | null): boolean {
  return formatCarNumber(value).replace(/\s/g, '').length === 8
}
