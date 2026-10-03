import { ref, watch } from 'vue'
import { fetchCurrentRate } from '@/api/exchangeRates'
import { BASE_CURRENCY, type CurrencyCode } from '@/utils/currency'

/**
 * Hujjat sanasidagi Markaziy bank kursi (server hujjatga aynan shu kursni yozadi, qo'lda kiritilmaydi).
 * Valyuta null yoki so'm bo'lsa kurs so'ralmaydi.
 */
export function useCbuRate(
  currency: () => CurrencyCode | null | undefined,
  date: () => string | null | undefined,
) {
  const rate = ref(0)
  const rateDate = ref<string | null>(null)
  const loading = ref(false)
  const missing = ref(false)
  let seq = 0

  async function load() {
    const id = ++seq
    const c = currency()
    if (!c || c === BASE_CURRENCY) {
      rate.value = 0
      rateDate.value = null
      missing.value = false
      loading.value = false
      return
    }
    const day = (date() || '').slice(0, 10)
    loading.value = true
    try {
      const r = (await fetchCurrentRate(c, day || undefined)).data
      if (id !== seq) return
      rate.value = Number(r?.rate || 0)
      rateDate.value = r?.rateDate ?? null
      missing.value = !(rate.value > 0)
    } catch {
      if (id !== seq) return
      rate.value = 0
      rateDate.value = null
      missing.value = true
    } finally {
      if (id === seq) loading.value = false
    }
  }

  watch([currency, date], () => void load(), { immediate: true })

  return { rate, rateDate, loading, missing, reload: load }
}
