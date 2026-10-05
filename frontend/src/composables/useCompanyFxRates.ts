import { computed, ref } from 'vue'
import { fetchCompanyRates, type CompanyFxRate } from '@/api/exchangeRates'
import { useAuthStore } from '@/stores/auth'

const STALE_MS = 5 * 60 * 1000

/**
 * Kompaniya kurslari barcha komponentlar uchun bitta nusxada saqlanadi: header har sahifada qayta yaratilganda
 * vidjet kursni qayta so'ramaydi va yo'qolib-paydo bo'lmaydi.
 */
const rates = ref<CompanyFxRate[]>([])
const loading = ref(false)
let loadedAt = 0
let loadedFor: string | null = null
let pending: Promise<void> | null = null

export function useCompanyFxRates() {
  const auth = useAuthStore()

  const usd = computed(() => rates.value.find((r) => r.currency === 'USD') ?? null)

  function load(): Promise<void> {
    if (pending) return pending
    loading.value = true
    pending = (async () => {
      try {
        rates.value = (await fetchCompanyRates()).data || []
        loadedAt = Date.now()
        loadedFor = auth.username
      } finally {
        loading.value = false
        pending = null
      }
    })()
    return pending
  }

  function loadIfStale(): Promise<void> {
    if (loadedFor !== auth.username) rates.value = []
    if (loadedFor === auth.username && Date.now() - loadedAt < STALE_MS) return Promise.resolve()
    return load()
  }

  function upsert(row: CompanyFxRate) {
    rates.value = [...rates.value.filter((r) => r.currency !== row.currency), row]
  }

  return { rates, usd, loading, load, loadIfStale, upsert }
}
