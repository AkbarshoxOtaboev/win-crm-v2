export type CurrencyCode = 'UZS' | 'USD'

export const BASE_CURRENCY: CurrencyCode = 'UZS'
export const CURRENCIES: CurrencyCode[] = ['UZS', 'USD']

/** "$1 250.50" yoki "1 250 000 so'm"; valyuta berilmasa UZS. */
export function moneyIn(v: number | null | undefined, currency: CurrencyCode | null | undefined, somLabel = 'so‘m') {
  if (v == null) return '—'
  const n = Number(v)
  if ((currency || BASE_CURRENCY) === 'USD') {
    const abs = new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Math.abs(n))
    return `${n < 0 ? '−' : ''}$${abs}`
  }
  return `${new Intl.NumberFormat('uz-UZ').format(n)} ${somLabel}`
}

export function currencySymbol(currency: CurrencyCode | null | undefined, somLabel = 'so‘m') {
  return currency === 'USD' ? '$' : somLabel
}

/** Maosh yozuvidagi o'girish izohi: "$800 × 12 850"; so'mdagi yozuvda bo'sh. */
export function fxSnapshotText(
  currency: CurrencyCode | null | undefined,
  amount: number | null | undefined,
  rate: number | null | undefined,
) {
  if (!currency || currency === BASE_CURRENCY || amount == null) return ''
  return `${moneyIn(amount, currency)} × ${formatRate(rate)}`
}

/** Kurs: "1 USD = 11 772.95 so'm". */
export function formatRate(rate: number | null | undefined) {
  if (rate == null) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(rate))
}
