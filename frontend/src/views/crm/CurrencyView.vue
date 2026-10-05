<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('exchangeRates.title')" />

    <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
      <div class="max-w-3xl">
        <p class="text-sm text-gray-500 dark:text-gray-400">{{ t('exchangeRates.subtitle') }}</p>
        <a
          href="https://cbu.uz/uz/arkhiv-kursov-valyut/"
          target="_blank"
          rel="noopener noreferrer"
          class="mt-1 inline-block text-xs text-brand-500 hover:underline"
        >{{ t('exchangeRates.source') }}</a>
      </div>
      <button type="button" class="btn" :disabled="syncing" @click="refresh">
        <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': syncing }" />
        {{ t('exchangeRates.refresh') }}
      </button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="notice" class="ok mb-4">{{ notice }}</div>

    <div class="card mb-4 p-5">
      <div class="mb-3 flex flex-wrap items-start justify-between gap-2">
        <div>
          <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-200">{{ t('exchangeRates.companyTitle') }}</h4>
          <p class="mt-0.5 text-xs text-gray-500 dark:text-gray-400">{{ t('exchangeRates.companyHint') }}</p>
        </div>
        <p v-if="company?.updatedAt" class="text-xs text-gray-500 dark:text-gray-400">
          {{ t('exchangeRates.updatedBy', { date: formatDmyTime(company.updatedAt), user: company.updatedUsername || '—' }) }}
        </p>
      </div>
      <form v-if="canEditCompany" class="flex flex-wrap items-end gap-3" @submit.prevent="saveCompany">
        <label class="fx-lbl">
          <span class="flex items-center gap-1"><ArrowDownLeft class="h-3.5 w-3.5 gain" />{{ t('exchangeRates.buyRate') }}</span>
          <input
            :value="companyText.buyRate"
            inputmode="decimal"
            autocomplete="off"
            placeholder="0"
            class="fx-input"
            required
            @input="onCompanyRateInput('buyRate', $event)"
          />
        </label>
        <label class="fx-lbl">
          <span class="flex items-center gap-1"><ArrowUpRight class="h-3.5 w-3.5 loss" />{{ t('exchangeRates.sellRate') }}</span>
          <input
            :value="companyText.sellRate"
            inputmode="decimal"
            autocomplete="off"
            placeholder="0"
            class="fx-input"
            required
            @input="onCompanyRateInput('sellRate', $event)"
          />
        </label>
        <button type="submit" class="btn" :disabled="companySaving">{{ t('exchangeRates.save') }}</button>
        <p v-if="spread != null" class="pb-2.5 text-xs text-gray-500 dark:text-gray-400">
          {{ t('exchangeRates.spread', { value: rateText(spread) }) }}
        </p>
      </form>
      <div v-else class="flex flex-wrap gap-6">
        <div>
          <p class="stat-label flex items-center gap-1"><ArrowDownLeft class="h-3.5 w-3.5 gain" />{{ t('exchangeRates.buyRate') }}</p>
          <p class="stat-value">{{ company?.buyRate != null ? rateText(company.buyRate) : t('exchangeRates.notSet') }}</p>
        </div>
        <div>
          <p class="stat-label flex items-center gap-1"><ArrowUpRight class="h-3.5 w-3.5 loss" />{{ t('exchangeRates.sellRate') }}</p>
          <p class="stat-value">{{ company?.sellRate != null ? rateText(company.sellRate) : t('exchangeRates.notSet') }}</p>
        </div>
      </div>
      <p v-if="companyError" class="mt-2 text-xs text-red-600">{{ companyError }}</p>
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article class="stat today">
        <p class="stat-label">{{ t('exchangeRates.todayRate') }}</p>
        <template v-if="current">
          <h3 class="today-value">{{ rateText(current.rate) }} <span class="unit">{{ som }}</span></h3>
          <p class="mt-1 flex flex-wrap items-center gap-2 text-sm">
            <span class="badge" :class="toneOf(current.change)">
              <component :is="trendIcon(current.change)" class="h-3.5 w-3.5" />
              {{ signedRate(current.change) }}
              <template v-if="pctOf(current.change, current.rate) != null">({{ signedPct(pctOf(current.change, current.rate)) }})</template>
            </span>
            <span class="text-gray-500 dark:text-gray-400">{{ t('exchangeRates.vsPrevious') }}</span>
          </p>
          <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">{{ t('exchangeRates.setOn', { date: dmy(current.rateDate) }) }}</p>
        </template>
        <template v-else-if="!loading">
          <h3 class="today-value text-amber-600">{{ t('exchangeRates.noRate') }}</h3>
          <p class="mt-1 text-xs text-gray-500 dark:text-gray-400">{{ t('exchangeRates.noRateHint') }}</p>
        </template>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('exchangeRates.periodMin') }}</p>
        <h4 class="stat-value">{{ stats ? rateText(stats.min.rate) : '—' }}</h4>
        <p v-if="stats" class="stat-sub">{{ dmy(stats.min.rateDate) }}</p>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('exchangeRates.periodMax') }}</p>
        <h4 class="stat-value">{{ stats ? rateText(stats.max.rate) : '—' }}</h4>
        <p v-if="stats" class="stat-sub">{{ dmy(stats.max.rateDate) }}</p>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('exchangeRates.periodChange') }}</p>
        <h4 class="stat-value" :class="toneOf(stats?.change)">
          {{ stats ? signedRate(stats.change) : '—' }}
          <span v-if="stats && stats.changePct != null" class="text-sm font-medium">({{ signedPct(stats.changePct) }})</span>
        </h4>
        <p v-if="stats" class="stat-sub">{{ t('exchangeRates.periodAvg', { value: rateText(stats.avg) }) }}</p>
      </article>
    </div>

    <div class="mb-4 flex flex-wrap gap-2">
      <button
        v-for="r in RANGES"
        :key="r"
        type="button"
        class="pill"
        :class="{ active: range === r }"
        @click="setRange(r)"
      >{{ t(`exchangeRates.ranges.d${r}`) }}</button>
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
      <div class="card p-4 xl:col-span-2">
        <h4 class="mb-2 text-sm font-semibold text-gray-700 dark:text-gray-200">{{ t('exchangeRates.chartTitle') }}</h4>
        <VueApexCharts
          v-if="ascending.length > 0"
          :key="`rate-${chartThemeKey}`"
          type="area"
          height="300"
          :options="rateChartOptions"
          :series="rateSeries"
        />
        <p v-else class="empty">{{ loading ? t('common.loading') : t('exchangeRates.empty') }}</p>
      </div>
      <div class="card p-4">
        <h4 class="mb-1 text-sm font-semibold text-gray-700 dark:text-gray-200">{{ t('exchangeRates.changesTitle') }}</h4>
        <p v-if="stats" class="mb-2 flex gap-3 text-xs">
          <span class="gain">{{ t('exchangeRates.upDays', { n: stats.upDays }) }}</span>
          <span class="loss">{{ t('exchangeRates.downDays', { n: stats.downDays }) }}</span>
        </p>
        <VueApexCharts
          v-if="changeRows.length > 0"
          :key="`change-${chartThemeKey}`"
          type="bar"
          height="276"
          :options="changeChartOptions"
          :series="changeSeries"
        />
        <p v-else class="empty">{{ loading ? t('common.loading') : t('exchangeRates.empty') }}</p>
      </div>
    </div>

    <div class="card">
      <div class="border-b border-gray-100 px-5 py-3 dark:border-gray-800">
        <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-200">{{ t('exchangeRates.historyTitle') }}</h4>
      </div>
      <div class="max-h-[32rem] overflow-auto">
        <table class="min-w-full">
          <thead class="sticky top-0 bg-white dark:bg-gray-900">
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('exchangeRates.colDate') }}</th>
              <th class="th text-right">{{ t('exchangeRates.colRate') }}</th>
              <th class="th text-right">{{ t('exchangeRates.colChange') }}</th>
              <th class="th text-right">{{ t('exchangeRates.colChangePct') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading && rows.length === 0"><td colspan="4" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="rows.length === 0"><td colspan="4" class="empty">{{ t('exchangeRates.empty') }}</td></tr>
            <tr v-for="r in rows" :key="r.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td whitespace-nowrap">{{ dmy(r.rateDate) }}</td>
              <td class="td text-right font-medium whitespace-nowrap text-gray-800 dark:text-white/90">{{ rateText(r.rate) }}</td>
              <td class="td text-right whitespace-nowrap" :class="toneOf(r.change)">
                <span class="inline-flex items-center gap-1">
                  <component :is="trendIcon(r.change)" class="h-3.5 w-3.5" />
                  {{ r.change == null ? '—' : signedRate(r.change) }}
                </span>
              </td>
              <td class="td text-right whitespace-nowrap" :class="toneOf(r.change)">
                {{ pctOf(r.change, r.rate) == null ? '—' : signedPct(pctOf(r.change, r.rate)) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import VueApexCharts from 'vue3-apexcharts'
import { ArrowDownLeft, ArrowUpRight, Minus, RefreshCw, TrendingDown, TrendingUp } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { useTheme } from '@/components/layout/ThemeProvider.vue'
import {
  fetchCurrentRate,
  fetchExchangeRates,
  saveCompanyRate,
  syncExchangeRate,
  type CompanyFxRate,
  type ExchangeRate,
} from '@/api/exchangeRates'
import { formatApiError } from '@/api/http'
import { useCompanyFxRates } from '@/composables/useCompanyFxRates'
import { useAuthStore } from '@/stores/auth'
import { amountToText, formatAmountInput, formatDmyTime } from '@/utils/format'

const RANGES = [7, 14, 30] as const
type Range = (typeof RANGES)[number]
const AUTO_RELOAD_MS = 10 * 60 * 1000

const { t } = useI18n()
const { isDarkMode } = useTheme() as { isDarkMode: ComputedRef<boolean> }
const chartThemeKey = computed(() => (isDarkMode.value ? 'dark' : 'light'))
const som = computed(() => t('common.currency'))

const range = ref<Range>(30)
const rows = ref<ExchangeRate[]>([])
const current = ref<ExchangeRate | null>(null)
const loading = ref(false)
const syncing = ref(false)
const error = ref<string | null>(null)
const notice = ref<string | null>(null)
let timer: ReturnType<typeof setInterval> | undefined

const rateFormat = new Intl.NumberFormat('uz-UZ', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

function rateText(v?: number | null) {
  return v == null ? '—' : rateFormat.format(Number(v))
}

function signedRate(v?: number | null) {
  const n = Number(v || 0)
  if (n > 0) return `+${rateFormat.format(n)}`
  if (n < 0) return `−${rateFormat.format(Math.abs(n))}`
  return rateFormat.format(0)
}

function pctOf(change?: number | null, rate?: number | null) {
  if (change == null || rate == null) return null
  const previous = Number(rate) - Number(change)
  return previous > 0 ? (Number(change) / previous) * 100 : null
}

function signedPct(v: number | null) {
  if (v == null) return '—'
  const text = `${Math.abs(v).toFixed(2)}%`
  return v > 0 ? `+${text}` : v < 0 ? `−${text}` : text
}

function toneOf(v?: number | null) {
  const n = Number(v || 0)
  return n > 0 ? 'gain' : n < 0 ? 'loss' : 'flat'
}

function trendIcon(v?: number | null) {
  const n = Number(v || 0)
  return n > 0 ? TrendingUp : n < 0 ? TrendingDown : Minus
}

function dmy(iso?: string | null) {
  if (!iso) return '—'
  const [y, m, d] = iso.slice(0, 10).split('-')
  return `${d}.${m}.${y}`
}

function localIso(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const ascending = computed(() => [...rows.value].reverse())
const changeRows = computed(() => ascending.value.filter((r) => r.change != null))

const stats = computed(() => {
  const list = ascending.value
  if (list.length === 0) return null
  let min = list[0]
  let max = list[0]
  let sum = 0
  let upDays = 0
  let downDays = 0
  for (const r of list) {
    if (r.rate < min.rate) min = r
    if (r.rate > max.rate) max = r
    sum += Number(r.rate)
    if (Number(r.change) > 0) upDays++
    if (Number(r.change) < 0) downDays++
  }
  const first = list[0]
  const last = list[list.length - 1]
  const start = first.change != null ? Number(first.rate) - Number(first.change) : Number(first.rate)
  const change = Number(last.rate) - start
  return {
    min,
    max,
    avg: sum / list.length,
    change,
    changePct: start > 0 ? (change / start) * 100 : null,
    upDays,
    downDays,
  }
})

const chartUi = computed(() => ({
  fore: isDarkMode.value ? '#9ca3af' : '#6b7280',
  grid: isDarkMode.value ? '#1f2937' : '#f1f5f9',
  theme: (isDarkMode.value ? 'dark' : 'light') as 'dark' | 'light',
}))

const rateSeries = computed(() => [
  { name: t('exchangeRates.colRate'), data: ascending.value.map((r) => ({ x: r.rateDate, y: Number(r.rate) })) },
])

const rateChartOptions = computed(() => ({
  chart: { toolbar: { show: false }, zoom: { enabled: false }, fontFamily: 'inherit', background: 'transparent' },
  theme: { mode: chartUi.value.theme },
  colors: ['#465fff'],
  stroke: { curve: 'straight' as const, width: 2 },
  fill: { type: 'gradient', gradient: { opacityFrom: 0.35, opacityTo: 0.02 } },
  dataLabels: { enabled: false },
  markers: { size: ascending.value.length <= 31 ? 3 : 0, hover: { size: 5 } },
  grid: { borderColor: chartUi.value.grid, strokeDashArray: 4 },
  xaxis: {
    type: 'datetime' as const,
    labels: { style: { colors: chartUi.value.fore }, datetimeUTC: false, format: 'dd.MM' },
    axisBorder: { show: false },
    axisTicks: { show: false },
  },
  yaxis: {
    labels: { style: { colors: chartUi.value.fore }, formatter: (v: number) => rateFormat.format(v) },
  },
  tooltip: {
    theme: chartUi.value.theme,
    x: { format: 'dd.MM.yyyy' },
    y: { formatter: (v: number) => `${rateFormat.format(v)} ${som.value}` },
  },
}))

const changeSeries = computed(() => [
  { name: t('exchangeRates.colChange'), data: changeRows.value.map((r) => ({ x: r.rateDate, y: Number(r.change) })) },
])

const changeChartOptions = computed(() => ({
  chart: { toolbar: { show: false }, zoom: { enabled: false }, fontFamily: 'inherit', background: 'transparent' },
  theme: { mode: chartUi.value.theme },
  plotOptions: {
    bar: {
      columnWidth: '70%',
      colors: {
        ranges: [
          { from: -1e9, to: -0.0001, color: '#ef4444' },
          { from: 0, to: 1e9, color: '#10b981' },
        ],
      },
    },
  },
  dataLabels: { enabled: false },
  grid: { borderColor: chartUi.value.grid, strokeDashArray: 4 },
  xaxis: {
    type: 'datetime' as const,
    labels: { style: { colors: chartUi.value.fore }, datetimeUTC: false, format: 'dd.MM' },
    axisBorder: { show: false },
    axisTicks: { show: false },
  },
  yaxis: {
    labels: { style: { colors: chartUi.value.fore }, formatter: (v: number) => signedRate(v) },
  },
  tooltip: {
    theme: chartUi.value.theme,
    x: { format: 'dd.MM.yyyy' },
    y: { formatter: (v: number) => `${signedRate(v)} ${som.value}` },
  },
}))

async function load() {
  loading.value = true
  error.value = null
  const to = new Date()
  const from = new Date()
  from.setDate(from.getDate() - range.value)
  try {
    const [history, today] = await Promise.all([
      fetchExchangeRates('USD', localIso(from), localIso(to)),
      fetchCurrentRate('USD'),
    ])
    rows.value = history.data || []
    current.value = today.data || null
  } catch (e) {
    error.value = formatApiError(e, t('exchangeRates.loadError'))
  } finally {
    loading.value = false
  }
}

function setRange(r: Range) {
  if (range.value === r) return
  range.value = r
  void load()
}

async function refresh() {
  syncing.value = true
  error.value = null
  notice.value = null
  try {
    await syncExchangeRate('USD')
    await load()
    notice.value = t('exchangeRates.refreshed')
  } catch (e) {
    error.value = formatApiError(e, t('exchangeRates.loadError'))
  } finally {
    syncing.value = false
  }
}

const auth = useAuthStore()
const canEditCompany = computed(
  () => auth.superAdmin || ['SUPER_ADMIN', 'ADMIN', 'DIRECTOR'].some((r) => auth.hasRole(r)),
)
const companyFx = useCompanyFxRates()
const company = ref<CompanyFxRate | null>(null)
const companyForm = reactive<{ buyRate: number | null; sellRate: number | null }>({ buyRate: null, sellRate: null })
const companyText = reactive({ buyRate: '', sellRate: '' })
const companySaving = ref(false)
const companyError = ref<string | null>(null)

const spread = computed(() =>
  companyForm.buyRate && companyForm.sellRate ? companyForm.sellRate - companyForm.buyRate : null,
)

function applyCompany(row: CompanyFxRate | null) {
  company.value = row
  companyForm.buyRate = row?.buyRate ?? null
  companyForm.sellRate = row?.sellRate ?? null
  companyText.buyRate = companyForm.buyRate != null ? amountToText(companyForm.buyRate) : ''
  companyText.sellRate = companyForm.sellRate != null ? amountToText(companyForm.sellRate) : ''
}

function onCompanyRateInput(field: 'buyRate' | 'sellRate', e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  companyText[field] = text
  el.value = text
  companyForm[field] = value > 0 ? value : null
}

async function loadCompany() {
  try {
    await companyFx.load()
    applyCompany(companyFx.usd.value)
  } catch (e) {
    companyError.value = formatApiError(e, t('exchangeRates.loadError'))
  }
}

async function saveCompany() {
  companyError.value = null
  notice.value = null
  const buy = Number(companyForm.buyRate)
  const sell = Number(companyForm.sellRate)
  if (!(buy > 0) || !(sell > 0)) {
    companyError.value = t('exchangeRates.ratesRequired')
    return
  }
  if (buy > sell) {
    companyError.value = t('exchangeRates.buyGtSell')
    return
  }
  companySaving.value = true
  try {
    const saved = (await saveCompanyRate('USD', buy, sell)).data
    applyCompany(saved)
    companyFx.upsert(saved)
    notice.value = t('exchangeRates.saved')
  } catch (e) {
    companyError.value = formatApiError(e)
  } finally {
    companySaving.value = false
  }
}

onMounted(() => {
  void load()
  void loadCompany()
  timer = setInterval(() => void load(), AUTO_RELOAD_MS)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.btn { display: inline-flex; align-items: center; gap: 0.5rem; height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:hover { background: #3641f5; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.ok { border-radius: 0.5rem; border: 1px solid #a7f3d0; background: #ecfdf5; padding: 0.75rem 1rem; font-size: 0.875rem; color: #047857; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.fx-lbl { display: flex; flex-direction: column; gap: 0.375rem; font-size: 0.8125rem; font-weight: 500; color: #4b5563; }
.fx-input { height: 2.5rem; width: 11rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.75rem; font-size: 0.875rem; color: #1f2937; }
.dark .fx-lbl { color: #d1d5db; }
.dark .fx-input { border-color: #374151; background: #111827; color: #f3f4f6; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.th.text-right { text-align: right; }
.td { padding: 0.625rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.stat { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.stat-label { font-size: 0.875rem; color: #4b5563; }
.stat-value { margin-top: 0.25rem; font-size: 1.25rem; font-weight: 700; color: #1f2937; }
.stat-sub { margin-top: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.today-value { margin-top: 0.25rem; font-size: 1.75rem; font-weight: 700; color: #1f2937; line-height: 1.2; }
.unit { font-size: 0.875rem; font-weight: 500; color: #6b7280; }
.badge { display: inline-flex; align-items: center; gap: 0.25rem; border-radius: 9999px; padding: 0.125rem 0.5rem; font-size: 0.75rem; font-weight: 600; }
.badge.gain { background: #ecfdf5; }
.badge.loss { background: #fef2f2; }
.badge.flat { background: #f3f4f6; color: #6b7280; }
.pill { height: 2rem; border-radius: 9999px; border: 1px solid #e5e7eb; padding: 0 0.875rem; font-size: 0.8125rem; color: #4b5563; }
.pill.active { border-color: #465fff; background: #465fff; color: #fff; }
.gain { color: #059669; }
.loss { color: #dc2626; }
.flat { color: #6b7280; }
.dark .card, .dark .stat { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .stat-label, .dark .stat-sub, .dark .unit { color: #9ca3af; }
.dark .stat-value, .dark .today-value { color: rgba(255, 255, 255, 0.92); }
.dark .pill { border-color: #374151; color: #d1d5db; }
.dark .pill.active { border-color: #465fff; color: #fff; }
.dark .badge.gain { background: rgba(16, 185, 129, 0.12); }
.dark .badge.loss { background: rgba(239, 68, 68, 0.12); }
.dark .badge.flat { background: rgba(255, 255, 255, 0.06); color: #9ca3af; }
.dark .gain { color: #34d399; }
.dark .loss { color: #f87171; }
</style>
