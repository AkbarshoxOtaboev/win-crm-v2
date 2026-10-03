import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchCurrentRate, fetchExchangeRates } from '@/api/exchangeRates'
import { BASE_CURRENCY, moneyIn, type CurrencyCode } from '@/utils/currency'

const STORAGE_KEY = 'wincrm_report_currency'
const RATE_LOOKBACK_DAYS = 45

const display = ref<CurrencyCode>(localStorage.getItem(STORAGE_KEY) === 'USD' ? 'USD' : BASE_CURRENCY)

/**
 * Hisobot va dashboardlarda summalarni tanlangan valyutada ko'rsatish (tanlov barcha sahifalar uchun umumiy).
 * Backenddagi ReportFx bilan bir xil qoida: hujjat o'z kursi bilan so'mga, so'mdan dollarga esa hujjat
 * kunidagi kompaniya kursi bilan (undan oldin kurs bo'lmasa - joriy kurs) o'giriladi.
 */
export function useReportCurrency() {
  const { t } = useI18n()
  const rates = ref<{ date: string; rate: number }[]>([])
  const currentRate = ref<number | null>(null)
  const rateMissing = ref(false)

  function setDisplay(currency: CurrencyCode) {
    display.value = currency
    localStorage.setItem(STORAGE_KEY, currency)
  }

  async function loadRates(from: string, to: string) {
    rateMissing.value = false
    if (display.value === BASE_CURRENCY) return
    const [hist, cur] = await Promise.all([
      fetchExchangeRates('USD', shiftDays(from, -RATE_LOOKBACK_DAYS), to).catch(() => null),
      fetchCurrentRate('USD').catch(() => null),
    ])
    rates.value = (hist?.data || [])
      .map((r) => ({ date: String(r.rateDate).slice(0, 10), rate: Number(r.rate) }))
      .sort((a, b) => a.date.localeCompare(b.date))
    currentRate.value = cur?.data?.rate != null ? Number(cur.data.rate) : null
  }

  function rateOn(date?: string | null): number | null {
    const day = String(date || '').slice(0, 10)
    let found: number | null = null
    for (const r of rates.value) {
      if (r.date > day) break
      found = r.rate
    }
    return found ?? currentRate.value
  }

  /** docRate - hujjatda qotirilgan kurs (1 birlik `currency` = docRate so'm). */
  function conv(
    amount: number | null | undefined,
    currency: CurrencyCode | null | undefined,
    docRate?: number | null,
    date?: string | null,
  ): number {
    const v = Number(amount || 0)
    const from = currency || BASE_CURRENCY
    if (from === display.value) return v
    const base = from === BASE_CURRENCY ? v : v * (docRate && docRate > 0 ? Number(docRate) : rateOn(date) || 0)
    if (display.value === BASE_CURRENCY) return base
    const rate = rateOn(date)
    if (!rate) {
      rateMissing.value = true
      return 0
    }
    return base / rate
  }

  function fmt(v: number | null | undefined) {
    const n = Number(v || 0)
    return moneyIn(display.value === 'USD' ? Math.round(n * 100) / 100 : Math.round(n), display.value, t('common.currency'))
  }

  /** Grafik o'qlari uchun qisqa ko'rinish: "1.2M so'm" yoki "$12K". */
  function compact(v: number) {
    const abs = Math.abs(v)
    const num =
      abs >= 1_000_000_000
        ? `${(v / 1_000_000_000).toFixed(1)}B`
        : abs >= 1_000_000
          ? `${(v / 1_000_000).toFixed(1)}M`
          : abs >= 1000
            ? `${(v / 1000).toFixed(0)}K`
            : String(Math.round(v))
    return display.value === 'USD' ? `$${num}` : `${num} ${t('common.currency')}`
  }

  const symbol = computed(() => (display.value === 'USD' ? '$' : t('common.currency')))

  return { display, setDisplay, loadRates, conv, fmt, compact, symbol, rateMissing }
}

function shiftDays(iso: string, days: number) {
  const d = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(d.getTime())) return iso
  d.setDate(d.getDate() + days)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
