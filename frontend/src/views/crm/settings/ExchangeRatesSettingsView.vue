<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.exchangeRates')" />
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="mb-6 grid gap-6 lg:grid-cols-2">
      <div class="card p-5">
        <div class="mb-3 flex items-center justify-between gap-3">
          <h3 class="card-title">{{ t('exchangeRates.todayRate') }}</h3>
          <select v-if="foreignCurrencies.length > 1" v-model="currency" class="field w-40">
            <option v-for="c in foreignCurrencies" :key="c" :value="c">{{ t(`exchangeRates.currencies.${c}`) }}</option>
          </select>
        </div>
        <template v-if="current">
          <div class="rate-big">
            1 {{ currency }} = {{ formatRate(current.rate) }} <span class="rate-unit">{{ t('common.currency') }}</span>
          </div>
          <div class="hint mt-1">
            {{ t('exchangeRates.effectiveFrom', { date: current.rateDate }) }} ·
            {{ t(`exchangeRates.sources.${current.source}`) }}
          </div>
          <div v-if="current.rateDate !== todayStr" class="warn mt-3">
            {{ t('exchangeRates.staleRate', { date: current.rateDate }) }}
          </div>
        </template>
        <template v-else-if="!loading">
          <div class="rate-big muted">{{ t('exchangeRates.noRate') }}</div>
          <div class="warn mt-3">{{ t('exchangeRates.noRateHint') }}</div>
        </template>
      </div>

      <div class="card p-5">
        <h3 class="card-title mb-3">{{ t('exchangeRates.formTitle') }}</h3>
        <form class="space-y-3" @submit.prevent="onSave">
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('exchangeRates.rateDate') }}</label>
              <input v-model="form.rateDate" type="date" required :max="tomorrowStr" class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('exchangeRates.rate', { currency }) }}</label>
              <input
                v-model.number="form.rate"
                type="number"
                min="0.0001"
                step="0.0001"
                required
                class="field"
                @input="form.source = 'MANUAL'"
              />
            </div>
          </div>

          <div class="cbu-box">
            <div class="flex flex-wrap items-center justify-between gap-2">
              <div>
                <div class="text-sm font-medium text-gray-800 dark:text-white/90">{{ t('exchangeRates.cbuTitle') }}</div>
                <div class="hint">{{ t('exchangeRates.cbuHint') }}</div>
              </div>
              <button type="button" class="ghost btn-sm" :disabled="cbuLoading" @click="loadCbu">
                {{ cbuLoading ? '...' : t('exchangeRates.cbuLoad') }}
              </button>
            </div>
            <div v-if="cbu" class="mt-3 flex flex-wrap items-center justify-between gap-2">
              <div class="text-sm">
                <strong>1 {{ cbu.currency }} = {{ formatRate(cbu.rate) }} {{ t('common.currency') }}</strong>
                <span class="hint ms-2">{{ cbu.rateDate }}</span>
                <span v-if="cbu.diff != null" class="ms-2" :class="Number(cbu.diff) >= 0 ? 'up' : 'down'">
                  {{ t('exchangeRates.cbuDiff', { diff: signed(cbu.diff) }) }}
                </span>
              </div>
              <button type="button" class="link" @click="useCbu">{{ t('exchangeRates.cbuUse') }}</button>
            </div>
          </div>

          <div v-if="savedMessage" class="ok">{{ savedMessage }}</div>
          <div class="flex justify-end">
            <button type="submit" class="btn" :disabled="saving || !form.rate">{{ saving ? '...' : t('exchangeRates.save') }}</button>
          </div>
        </form>
      </div>
    </div>

    <div class="card">
      <div class="flex flex-wrap items-end justify-between gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="card-title">{{ t('exchangeRates.historyTitle') }}</h3>
        <div class="flex flex-wrap items-end gap-2">
          <div>
            <label class="lbl">{{ t('exchangeRates.from') }}</label>
            <input v-model="filter.from" type="date" class="field w-40" />
          </div>
          <div>
            <label class="lbl">{{ t('exchangeRates.to') }}</label>
            <input v-model="filter.to" type="date" class="field w-40" />
          </div>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('exchangeRates.colDate') }}</th>
              <th class="th text-right">{{ t('exchangeRates.colRate') }}</th>
              <th class="th text-right">{{ t('exchangeRates.colChange') }}</th>
              <th class="th">{{ t('exchangeRates.colSource') }}</th>
              <th class="th">{{ t('exchangeRates.colUser') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="6" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="rows.length === 0"><td colspan="6" class="empty">{{ t('exchangeRates.empty') }}</td></tr>
            <tr v-for="r in rows" :key="r.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ r.rateDate }}</td>
              <td class="td text-right">{{ formatRate(r.rate) }}</td>
              <td class="td text-right">
                <span v-if="r.change != null" :class="r.change > 0 ? 'up' : r.change < 0 ? 'down' : 'muted'">{{ signed(r.change) }}</span>
                <span v-else class="muted">—</span>
              </td>
              <td class="td">{{ t(`exchangeRates.sources.${r.source}`) }}</td>
              <td class="td">{{ r.createdUsername || '—' }}</td>
              <td class="td text-right">
                <RowActions @edit="editRow(r)" @delete="onDelete(r)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  deleteExchangeRate,
  fetchCbuRate,
  fetchCurrentRate,
  fetchExchangeRates,
  saveExchangeRate,
  type CbuRate,
  type ExchangeRate,
  type ExchangeRateSource,
} from '@/api/exchangeRates'
import { formatApiError } from '@/api/http'
import { BASE_CURRENCY, CURRENCIES, formatRate, type CurrencyCode } from '@/utils/currency'

const { t } = useI18n()

function isoDate(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const now = new Date()
const todayStr = isoDate(now)
const tomorrowStr = isoDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1))

const foreignCurrencies = CURRENCIES.filter((c) => c !== BASE_CURRENCY)
const currency = ref<CurrencyCode>(foreignCurrencies[0] ?? 'USD')

const history = ref<ExchangeRate[]>([])
const current = ref<ExchangeRate | null>(null)
const cbu = ref<CbuRate | null>(null)
const loading = ref(false)
const cbuLoading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const savedMessage = ref<string | null>(null)

const form = reactive<{ rateDate: string; rate: number | null; source: ExchangeRateSource }>({
  rateDate: todayStr,
  rate: null,
  source: 'MANUAL',
})

const filter = reactive({
  from: isoDate(new Date(now.getFullYear(), now.getMonth(), now.getDate() - 60)),
  to: todayStr,
})

/** History comes newest first; change is measured against the previous (older) entry. */
const rows = computed(() =>
  history.value.map((r, i) => {
    const older = history.value[i + 1]
    return { ...r, change: older ? Number(r.rate) - Number(older.rate) : null }
  }),
)

function signed(v: number | null | undefined) {
  if (v == null) return '—'
  const n = Number(v)
  const abs = formatRate(Math.abs(n))
  return n > 0 ? `+${abs}` : n < 0 ? `−${abs}` : '0'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [historyRes, currentRes] = await Promise.all([
      fetchExchangeRates(currency.value, filter.from || undefined, filter.to || undefined),
      fetchCurrentRate(currency.value),
    ])
    history.value = historyRes.data || []
    current.value = currentRes.data ?? null
    if (form.rate == null && current.value) form.rate = Number(current.value.rate)
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function loadCbu() {
  cbuLoading.value = true
  error.value = null
  try {
    const res = await fetchCbuRate(currency.value, form.rateDate || undefined)
    cbu.value = res.data
  } catch (e) {
    cbu.value = null
    error.value = formatApiError(e)
  } finally {
    cbuLoading.value = false
  }
}

function useCbu() {
  if (!cbu.value) return
  form.rate = Number(cbu.value.rate)
  form.source = 'CBU'
}

function editRow(r: ExchangeRate) {
  form.rateDate = r.rateDate
  form.rate = Number(r.rate)
  form.source = r.source
  savedMessage.value = null
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function onSave() {
  if (!form.rate || form.rate <= 0) return
  saving.value = true
  error.value = null
  savedMessage.value = null
  try {
    await saveExchangeRate({ currency: currency.value, rateDate: form.rateDate, rate: form.rate, source: form.source })
    savedMessage.value = t('exchangeRates.saved')
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(r: ExchangeRate) {
  if (!confirm(t('exchangeRates.deleteConfirm', { date: r.rateDate }))) return
  try {
    await deleteExchangeRate(r.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

watch(currency, () => {
  form.rate = null
  cbu.value = null
  load()
})
watch(() => [filter.from, filter.to], load)
watch(() => form.rateDate, () => {
  cbu.value = null
})

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.card-title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.rate-big { font-size: 1.875rem; font-weight: 700; color: #1f2937; line-height: 1.2; }
.rate-big.muted { font-size: 1.25rem; font-weight: 600; }
.rate-unit { font-size: 1.125rem; font-weight: 500; color: #6b7280; }
.hint { font-size: 0.8125rem; color: #6b7280; }
.muted { color: #9ca3af; }
.up { color: #dc2626; }
.down { color: #16a34a; }
.cbu-box { border-radius: 0.75rem; border: 1px dashed #c7d2fe; background: #f5f7ff; padding: 0.75rem 1rem; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.th.text-right { text-align: right; }
.td { padding: 0.625rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.field.w-40 { width: 10rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled, .ghost:disabled { opacity: 0.5; cursor: not-allowed; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.875rem; font-size: 0.875rem; color: #374151; }
.btn-sm { height: 2.25rem; }
.link { font-size: 0.875rem; font-weight: 500; color: #465fff; }
.link:hover { text-decoration: underline; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.warn { border-radius: 0.5rem; border: 1px solid #fde68a; background: #fffbeb; padding: 0.5rem 0.75rem; font-size: 0.8125rem; color: #b45309; }
.ok { border-radius: 0.5rem; border: 1px solid #bbf7d0; background: #f0fdf4; padding: 0.5rem 0.75rem; font-size: 0.875rem; color: #15803d; }

.dark .card { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .card-title, .dark .rate-big { color: rgba(255, 255, 255, 0.9); }
.dark .hint, .dark .th, .dark .empty, .dark .lbl, .dark .rate-unit { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .field, .dark .ghost { border-color: #374151; background: transparent; color: #e5e7eb; }
.dark .field option { background: #111827; }
.dark .cbu-box { border-color: #3730a3; background: rgba(70, 95, 255, 0.08); }
.dark .err { border-color: #7f1d1d; background: rgba(127, 29, 29, 0.2); color: #fca5a5; }
.dark .warn { border-color: #78350f; background: rgba(120, 53, 15, 0.2); color: #fcd34d; }
.dark .ok { border-color: #14532d; background: rgba(20, 83, 45, 0.2); color: #86efac; }
</style>
