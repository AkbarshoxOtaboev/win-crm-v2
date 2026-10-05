<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.paymentsDashboard')" />

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card mb-4 p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.paymentsDashboard') }}</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('paymentsDashboard.subtitle') }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <label class="date-range">
          <CalendarDays class="h-4 w-4 shrink-0 text-gray-400" />
          <input v-model="startDate" type="date" class="date-input" />
          <span class="text-gray-400">—</span>
          <input v-model="endDate" type="date" class="date-input" />
        </label>
        <ReportCurrencyToggle :model-value="display" class="ms-auto" @update:model-value="changeCurrency" />
        <button type="button" class="icon-btn" :title="t('common.refresh')" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article class="kpi-card" style="--accent: #f97316">
        <div class="flex items-center gap-2">
          <span class="kpi-icon"><Wallet class="h-4 w-4" /></span>
          <p class="text-sm text-gray-500">{{ t('paymentsDashboard.todayRevenue') }}</p>
        </div>
        <p class="mt-3 text-xl font-bold text-gray-800 dark:text-white/90">{{ fmt(today.total) }}</p>
        <p class="mt-1 text-xs" :class="today.diff >= 0 ? 'text-success-600' : 'text-error-600'">
          {{ t('paymentsDashboard.yesterdayDiff', { diff: diffLabel }) }}
        </p>
      </article>
      <article class="kpi-card" style="--accent: #465fff">
        <div class="flex items-center gap-2">
          <span class="kpi-icon"><TrendingUp class="h-4 w-4" /></span>
          <p class="text-sm text-gray-500">{{ t('paymentsDashboard.periodRevenue') }}</p>
        </div>
        <p class="mt-3 text-xl font-bold text-gray-800 dark:text-white/90">{{ fmt(periodTotal) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ t('paymentsDashboard.days', { n: daily.length }) }}</p>
      </article>
      <article class="kpi-card" style="--accent: #12b76a">
        <div class="flex items-center gap-2">
          <span class="kpi-icon"><Receipt class="h-4 w-4" /></span>
          <p class="text-sm text-gray-500">{{ t('paymentsDashboard.paymentsCount') }}</p>
        </div>
        <p class="mt-3 text-xl font-bold text-gray-800 dark:text-white/90">{{ paymentsCount }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ startDate }} — {{ endDate }}</p>
      </article>
      <article class="kpi-card" style="--accent: #8b5cf6">
        <div class="flex items-center gap-2">
          <span class="kpi-icon"><BarChart3 class="h-4 w-4" /></span>
          <p class="text-sm text-gray-500">{{ t('paymentsDashboard.avgDaily') }}</p>
        </div>
        <p class="mt-3 text-xl font-bold text-gray-800 dark:text-white/90">{{ fmt(avgDaily) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ t('paymentsDashboard.days', { n: daily.length }) }}</p>
      </article>
    </div>

    <div class="card mb-4 p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('paymentsDashboard.dailyRevenue') }}</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('paymentsDashboard.dailyRevenueHint') }}</p>
      </div>
      <VueApexCharts
        v-if="chartMounted"
        :key="`daily-${chartThemeKey}-${display}`"
        type="bar"
        height="300"
        :options="dailyOptions"
        :series="dailySeries"
      />
    </div>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
      <div class="card p-5 xl:col-span-2">
        <div class="mb-4">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('paymentsDashboard.byType') }}</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('paymentsDashboard.byTypeHint') }}</p>
        </div>
        <p v-if="typeStats.length === 0" class="py-10 text-center text-sm text-gray-500">{{ t('paymentsDashboard.noData') }}</p>
        <div v-else class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <article v-for="(s, i) in typeStats" :key="s.id ?? i" class="type-card" :style="{ '--accent': colorAt(i) }">
            <div class="flex items-center justify-between gap-2">
              <p class="truncate text-sm font-medium text-gray-700 dark:text-gray-200">{{ s.name }}</p>
              <span class="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-600 dark:bg-white/5 dark:text-gray-300">{{ s.currency }}</span>
            </div>
            <p class="mt-2 text-xl font-bold text-gray-800 dark:text-white/90">{{ moneyIn(s.amount, s.currency, t('common.currency')) }}</p>
            <div class="mt-2 flex items-center justify-between text-xs text-gray-500">
              <span>{{ t('paymentsDashboard.count', { n: s.count }) }}</span>
              <span class="font-medium text-gray-700 dark:text-gray-200">{{ s.share }}%</span>
            </div>
            <div class="mt-1.5 h-1.5 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
              <div class="h-full rounded-full" :style="{ width: `${s.share}%`, background: colorAt(i) }" />
            </div>
          </article>
        </div>
      </div>

      <div class="flex flex-col gap-4">
        <div class="card p-5">
          <h3 class="mb-2 text-base font-semibold text-gray-800 dark:text-white/90">{{ t('paymentsDashboard.countShare') }}</h3>
          <VueApexCharts
            v-if="chartMounted"
            :key="`donut-${chartThemeKey}`"
            type="donut"
            height="260"
            :options="donutOptions"
            :series="donutSeries"
          />
        </div>
        <div class="card p-5">
          <h3 class="mb-3 text-base font-semibold text-gray-800 dark:text-white/90">{{ t('paymentsDashboard.todayByType') }}</h3>
          <p v-if="today.byType.length === 0" class="py-6 text-center text-sm text-gray-500">{{ t('paymentsDashboard.noData') }}</p>
          <ul v-else class="space-y-2">
            <li v-for="(r, i) in today.byType" :key="r.paymentTypeId ?? i" class="flex items-center justify-between gap-3 text-sm">
              <span class="flex min-w-0 items-center gap-2 text-gray-700 dark:text-gray-300">
                <span class="h-2.5 w-2.5 shrink-0 rounded-full" :style="{ background: colorAt(i) }" />
                <span class="truncate">{{ r.paymentTypeName || '—' }}</span>
              </span>
              <span class="shrink-0 font-semibold text-gray-800 dark:text-white/90">
                {{ moneyIn(Number(r.amount || 0), r.currency || BASE_CURRENCY, t('common.currency')) }}
              </span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import VueApexCharts from 'vue3-apexcharts'
import { BarChart3, CalendarDays, Receipt, RefreshCw, TrendingUp, Wallet } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import ReportCurrencyToggle from '@/components/crm/ReportCurrencyToggle.vue'
import { useTheme } from '@/components/layout/ThemeProvider.vue'
import {
  fetchDailyPayments,
  fetchPaymentsByType,
  type DailyPaymentSummary,
  type DailyPaymentTypeAmount,
  type PaymentTypeSummary,
} from '@/api/dashboard'
import { formatApiError } from '@/api/http'
import { today as todayIso } from '@/utils/format'
import { BASE_CURRENCY, moneyIn, type CurrencyCode } from '@/utils/currency'
import { useReportCurrency } from '@/composables/useReportCurrency'

const COLORS = ['#465fff', '#12b76a', '#f97316', '#8b5cf6', '#06b6d4', '#ef4444', '#eab308', '#ec4899']

const { t } = useI18n()
const { isDarkMode } = useTheme()
const { display, setDisplay, fmt, compact } = useReportCurrency()
const chartThemeKey = computed(() => (isDarkMode.value ? 'dark' : 'light'))
const chartUi = computed(() => {
  const dark = isDarkMode.value
  return {
    fore: dark ? '#9ca3af' : '#6b7280',
    text: dark ? 'rgba(255,255,255,0.92)' : '#111827',
    grid: dark ? '#1f2937' : '#f1f5f9',
    mode: (dark ? 'dark' : 'light') as 'dark' | 'light',
  }
})

const error = ref<string | null>(null)
const loading = ref(false)
const chartMounted = ref(false)
const startDate = ref(monthStart())
const endDate = ref(todayIso())
const daily = ref<DailyPaymentSummary[]>([])
const byType = ref<PaymentTypeSummary[]>([])
const today = ref<{ total: number; diff: number; byType: DailyPaymentTypeAmount[] }>({ total: 0, diff: 0, byType: [] })

const periodTotal = computed(() => daily.value.reduce((s, d) => s + Number(d.totalAmount ?? d.amount ?? 0), 0))
const avgDaily = computed(() => (daily.value.length ? periodTotal.value / daily.value.length : 0))
const paymentsCount = computed(() => byType.value.reduce((s, x) => s + Number(x.paymentCount ?? x.count ?? 0), 0))

const diffLabel = computed(() => {
  const d = today.value.diff
  return `${d > 0 ? '+' : ''}${fmt(d)}`
})

const typeStats = computed(() => {
  const total = paymentsCount.value
  return byType.value
    .map((x) => {
      const count = Number(x.paymentCount ?? x.count ?? 0)
      return {
        id: x.paymentTypeId,
        name: x.paymentTypeName || '—',
        amount: Number(x.totalAmount ?? x.amount ?? 0),
        currency: (x.currency || BASE_CURRENCY) as CurrencyCode,
        count,
        share: total > 0 ? Math.round((count / total) * 100) : 0,
      }
    })
    .sort((a, b) => b.count - a.count)
})

function colorAt(i: number) {
  return COLORS[i % COLORS.length]
}

const dailySeries = computed(() => [
  { name: t('paymentsDashboard.revenue'), data: daily.value.map((d) => Number(d.totalAmount ?? d.amount ?? 0)) },
])

const dailyOptions = computed(() => ({
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'bar' as const,
    toolbar: { show: false },
    background: 'transparent',
    foreColor: chartUi.value.fore,
  },
  theme: { mode: chartUi.value.mode },
  colors: ['#f97316'],
  plotOptions: { bar: { columnWidth: '45%', borderRadius: 5, borderRadiusApplication: 'end' as const } },
  dataLabels: { enabled: false },
  xaxis: {
    categories: daily.value.map((d) => dayLabel(d.date)),
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: { style: { colors: chartUi.value.fore, fontSize: '11px' } },
  },
  yaxis: { labels: { style: { colors: chartUi.value.fore }, formatter: (v: number) => compact(v) } },
  grid: { borderColor: chartUi.value.grid },
  tooltip: { theme: chartUi.value.mode, y: { formatter: (v: number) => fmt(v) } },
}))

const donutSeries = computed(() => {
  const values = typeStats.value.map((s) => s.count)
  return values.length ? values : [0]
})

const donutOptions = computed(() => ({
  chart: { fontFamily: 'Outfit, sans-serif', type: 'donut' as const, background: 'transparent', foreColor: chartUi.value.fore },
  theme: { mode: chartUi.value.mode },
  labels: typeStats.value.length ? typeStats.value.map((s) => s.name) : [t('paymentsDashboard.noData')],
  colors: typeStats.value.length ? typeStats.value.map((_, i) => colorAt(i)) : ['#e5e7eb'],
  legend: { position: 'bottom' as const, fontSize: '12px', labels: { colors: chartUi.value.fore } },
  dataLabels: { enabled: false },
  stroke: { colors: [isDarkMode.value ? '#111827' : '#ffffff'] },
  plotOptions: {
    pie: {
      donut: {
        size: '70%',
        labels: {
          show: true,
          value: { show: true, fontSize: '20px', fontWeight: 700, color: chartUi.value.text },
          total: {
            show: true,
            label: t('paymentsDashboard.paymentsCount'),
            fontSize: '12px',
            color: chartUi.value.fore,
            formatter: () => String(paymentsCount.value),
          },
        },
      },
    },
  },
  tooltip: { theme: chartUi.value.mode, y: { formatter: (v: number) => t('paymentsDashboard.count', { n: v }) } },
}))

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function monthStart() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01`
}

function shiftIso(iso: string, days: number) {
  const d = new Date(`${iso}T00:00:00`)
  d.setDate(d.getDate() + days)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function dayLabel(raw?: string) {
  const m = String(raw || '').match(/(\d{4})-(\d{2})-(\d{2})/)
  return m ? `${m[3]}.${m[2]}` : String(raw || '—')
}

async function changeCurrency(c: CurrencyCode) {
  setDisplay(c)
  await load()
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const cur = display.value
    const now = todayIso()
    const [d, p, recent] = await Promise.all([
      fetchDailyPayments(startDate.value, endDate.value, cur),
      fetchPaymentsByType(startDate.value, endDate.value),
      fetchDailyPayments(shiftIso(now, -1), now, cur),
    ])
    daily.value = [...(d.data || [])].sort((a, b) => String(a.date || '').localeCompare(String(b.date || '')))
    byType.value = p.data || []

    const rows = recent.data || []
    const todayRow = rows.find((r) => String(r.date || '').slice(0, 10) === now)
    const yesterdayRow = rows.find((r) => String(r.date || '').slice(0, 10) === shiftIso(now, -1))
    const todayTotal = Number(todayRow?.totalAmount ?? 0)
    today.value = {
      total: todayTotal,
      diff: todayTotal - Number(yesterdayRow?.totalAmount ?? 0),
      byType: (todayRow?.byType || []).filter((x) => Number(x.amount || 0) !== 0),
    }
  } catch (e) {
    error.value = formatApiError(e, t('paymentsDashboard.loadError'))
  } finally {
    loading.value = false
  }
}

watch([startDate, endDate], () => {
  void load()
})

onMounted(async () => {
  await load()
  chartMounted.value = true
})
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.date-range { display: inline-flex; align-items: center; gap: 0.5rem; height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.75rem; }
.date-input { border: 0; background: transparent; font-size: 0.875rem; color: #374151; outline: none; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; color: #4b5563; }
.icon-btn:hover { background: #f9fafb; }
.icon-btn:disabled { opacity: 0.6; }
.kpi-card { border-radius: 1rem; border: 1px solid #e5e7eb; border-bottom: 3px solid var(--accent); background: #fff; padding: 1.25rem; }
.kpi-icon { display: inline-flex; height: 1.75rem; width: 1.75rem; align-items: center; justify-content: center; border-radius: 0.5rem; color: var(--accent); background: color-mix(in srgb, var(--accent) 12%, transparent); }
.type-card { border-radius: 0.875rem; border: 1px solid #e5e7eb; border-left: 3px solid var(--accent); background: #fff; padding: 1rem; }
:global(html.dark .card),
:global(html.dark .kpi-card),
:global(html.dark .type-card) { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
:global(html.dark .date-range),
:global(html.dark .icon-btn) { border-color: #374151; background: #111827; color: rgba(255, 255, 255, 0.92); }
:global(html.dark input.date-input) { background-color: transparent !important; border-color: transparent !important; color: rgba(255, 255, 255, 0.92) !important; color-scheme: dark; }
:global(html.dark .icon-btn:hover) { background: #1f2937; }
:global(html.dark .err) { border-color: rgba(248, 113, 113, 0.45); background: rgba(127, 29, 29, 0.35); color: #fecaca; }
</style>
