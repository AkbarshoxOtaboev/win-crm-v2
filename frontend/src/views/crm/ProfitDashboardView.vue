<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.profitDashboard')" />

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card mb-4 p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.profitDashboard') }}</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('profitDashboard.subtitle') }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <label class="date-range">
          <CalendarDays class="h-4 w-4 shrink-0 text-gray-400" />
          <input v-model="startDate" type="date" class="date-input" />
          <span class="text-gray-400">—</span>
          <input v-model="endDate" type="date" class="date-input" />
        </label>
        <div class="presets">
          <button
            v-for="p in PRESETS"
            :key="p"
            type="button"
            class="preset"
            :class="{ active: activePreset === p }"
            @click="applyPreset(p)"
          >
            {{ t(`profitDashboard.${p}`) }}
          </button>
        </div>
        <ReportCurrencyToggle :model-value="display" class="ms-auto" @update:model-value="changeCurrency" />
        <button type="button" class="icon-btn" :title="t('common.refresh')" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article class="kpi-card" style="--accent: #6366f1">
        <p class="kpi-label"><Wallet class="h-4 w-4" />{{ t('profitDashboard.revenue') }}</p>
        <p class="kpi-value">{{ fmt(report?.revenue) }}</p>
        <p class="kpi-hint">{{ t('profitDashboard.revenueHint', { n: report?.orderCount ?? 0 }) }}</p>
      </article>
      <article class="kpi-card" style="--accent: #f59e0b">
        <p class="kpi-label"><Package class="h-4 w-4" />{{ t('profitDashboard.cost') }}</p>
        <p class="kpi-value">{{ fmt(report?.cost) }}</p>
        <p class="kpi-hint">{{ t('profitDashboard.costHint') }}</p>
      </article>
      <article class="kpi-card" :style="{ '--accent': profitTone === 'loss' ? '#ef4444' : '#10b981' }">
        <p class="kpi-label"><TrendingUp class="h-4 w-4" />{{ t('profitDashboard.profit') }}</p>
        <p class="kpi-value" :class="profitTone">{{ fmt(report?.profit) }}</p>
        <p class="kpi-hint">
          {{ t('profitDashboard.margin') }}:
          <b :class="profitTone">{{ pct(report?.marginPercent) }}</b>
        </p>
      </article>
      <article class="kpi-card" style="--accent: #ec4899">
        <p class="kpi-label"><Percent class="h-4 w-4" />{{ t('profitDashboard.discount') }}</p>
        <p class="kpi-value">{{ fmt(report?.discount) }}</p>
        <p class="kpi-hint">{{ t('profitDashboard.discountHint') }}</p>
      </article>
    </div>

    <div v-if="report && (report.deliveryFee > 0 || report.itemlessOrderCount > 0)" class="mb-4 space-y-1 text-sm">
      <p v-if="report.deliveryFee > 0" class="text-gray-500 dark:text-gray-400">
        {{ t('profitDashboard.deliveryNote', { amount: fmt(report.deliveryFee) }) }}
      </p>
      <p v-if="report.itemlessOrderCount > 0" class="text-amber-600">
        {{ t('profitDashboard.itemlessNote', { n: report.itemlessOrderCount }) }}
      </p>
    </div>

    <div class="card mb-4 p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('profitDashboard.dynamics') }}</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('profitDashboard.dynamicsHint') }}</p>
      </div>
      <VueApexCharts
        v-if="chartMounted"
        :key="`profit-${chartThemeKey}-${display}`"
        type="area"
        height="320"
        :options="chartOptions"
        :series="chartSeries"
      />
    </div>

    <div class="grid grid-cols-1 gap-4 2xl:grid-cols-5">
      <div class="card 2xl:col-span-3">
        <div class="p-5 pb-3">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('profitDashboard.byGoods') }}</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('profitDashboard.byGoodsHint') }}</p>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">{{ t('profitDashboard.goods') }}</th>
                <th class="th text-right">{{ t('profitDashboard.quantity') }}</th>
                <th class="th text-right">{{ t('profitDashboard.revenue') }}</th>
                <th class="th text-right">{{ t('profitDashboard.cost') }}</th>
                <th class="th text-right">{{ t('profitDashboard.profit') }}</th>
                <th class="th">{{ t('profitDashboard.margin') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading && !report"><td colspan="6" class="empty">{{ t('common.loading') }}</td></tr>
              <tr v-else-if="goodsRows.length === 0"><td colspan="6" class="empty">{{ t('profitDashboard.empty') }}</td></tr>
              <tr v-for="g in goodsRows" :key="g.goodsId" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td">
                  <span class="font-medium text-gray-800 dark:text-white/90">{{ g.goodsName }}</span>
                  <span v-if="g.goodsType === 'SERVICE'" class="tag tag-service">{{ t('profitDashboard.service') }}</span>
                  <span v-else-if="g.goodsType === 'WINDOW'" class="tag">{{ t('profitDashboard.window') }}</span>
                </td>
                <td class="td text-right whitespace-nowrap">
                  {{ qty(g.count) }} <span class="text-xs text-gray-400">{{ g.unitName || '' }}</span>
                </td>
                <td class="td text-right whitespace-nowrap">{{ fmt(g.revenue) }}</td>
                <td class="td text-right whitespace-nowrap">{{ fmt(g.cost) }}</td>
                <td class="td text-right whitespace-nowrap font-semibold" :class="toneOf(g.profit)">{{ fmt(g.profit) }}</td>
                <td class="td">
                  <div class="margin-cell">
                    <div class="margin-bar">
                      <span :class="toneOf(g.profit)" :style="{ width: `${barWidth(g.marginPercent)}%` }" />
                    </div>
                    <span class="text-xs" :class="toneOf(g.profit)">{{ pct(g.marginPercent) }}</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card 2xl:col-span-2">
        <div class="p-5 pb-3">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('profitDashboard.bySellers') }}</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">{{ t('profitDashboard.seller') }}</th>
                <th class="th text-right">{{ t('profitDashboard.orders') }}</th>
                <th class="th text-right">{{ t('profitDashboard.revenue') }}</th>
                <th class="th text-right">{{ t('profitDashboard.profit') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading && !report"><td colspan="4" class="empty">{{ t('common.loading') }}</td></tr>
              <tr v-else-if="sellerRows.length === 0"><td colspan="4" class="empty">{{ t('profitDashboard.empty') }}</td></tr>
              <tr v-for="s in sellerRows" :key="s.userId" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td font-medium text-gray-800 dark:text-white/90">{{ s.fullName || `#${s.userId}` }}</td>
                <td class="td text-right">{{ s.orderCount }}</td>
                <td class="td text-right whitespace-nowrap">{{ fmt(s.revenue) }}</td>
                <td class="td text-right whitespace-nowrap">
                  <span class="font-semibold" :class="toneOf(s.profit)">{{ fmt(s.profit) }}</span>
                  <div class="text-xs text-gray-400">{{ pct(s.marginPercent) }}</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import ReportCurrencyToggle from '@/components/crm/ReportCurrencyToggle.vue'
import { fetchProfitReport, type ProfitReport } from '@/api/dashboard'
import { formatApiError } from '@/api/http'
import { today } from '@/utils/format'
import type { CurrencyCode } from '@/utils/currency'
import { useReportCurrency } from '@/composables/useReportCurrency'
import VueApexCharts from 'vue3-apexcharts'
import type { ApexOptions } from 'apexcharts'
import { CalendarDays, Package, Percent, RefreshCw, TrendingUp, Wallet } from 'lucide-vue-next'
import { useTheme } from '@/components/layout/ThemeProvider.vue'

const PRESETS = ['today', 'week', 'month', 'year'] as const
type Preset = (typeof PRESETS)[number]

const { t, locale } = useI18n()
const { isDarkMode } = useTheme() as { isDarkMode: Ref<boolean> }
const { display, setDisplay, fmt, compact } = useReportCurrency()
const chartThemeKey = computed(() => (isDarkMode.value ? 'dark' : 'light'))

const startDate = ref(presetStart('month'))
const endDate = ref(today())
const loading = ref(false)
const error = ref<string | null>(null)
const report = ref<ProfitReport | null>(null)
const chartMounted = ref(false)

const goodsRows = computed(() => report.value?.goods || [])
const sellerRows = computed(() => report.value?.sellers || [])
const profitTone = computed(() => toneOf(report.value?.profit))
const activePreset = computed<Preset | null>(() =>
  endDate.value === today() ? PRESETS.find((p) => presetStart(p) === startDate.value) || null : null,
)

function iso(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function presetStart(p: Preset) {
  const d = new Date()
  if (p === 'week') d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  if (p === 'month') d.setDate(1)
  if (p === 'year') d.setMonth(0, 1)
  return iso(d)
}

function applyPreset(p: Preset) {
  startDate.value = presetStart(p)
  endDate.value = today()
}

function toneOf(v?: number | null) {
  const n = Number(v || 0)
  return n > 0 ? 'gain' : n < 0 ? 'loss' : ''
}

function pct(v?: number | null) {
  return `${Number(v || 0).toLocaleString(locale.value, { maximumFractionDigits: 1 })}%`
}

function qty(v?: number | null) {
  return Number(v || 0).toLocaleString(locale.value, { maximumFractionDigits: 2 })
}

function barWidth(margin?: number | null) {
  return Math.min(100, Math.abs(Number(margin || 0)))
}

const chartSeries = computed(() => {
  const days = report.value?.days || []
  return [
    { name: t('profitDashboard.revenue'), data: days.map((d) => Number(d.revenue)) },
    { name: t('profitDashboard.cost'), data: days.map((d) => Number(d.cost)) },
    { name: t('profitDashboard.profit'), data: days.map((d) => Number(d.profit)) },
  ]
})

const chartOptions = computed<ApexOptions>(() => {
  const dark = isDarkMode.value
  const fore = dark ? '#9ca3af' : '#6b7280'
  return {
    chart: {
      fontFamily: 'Outfit, sans-serif',
      type: 'area' as const,
      toolbar: { show: false },
      zoom: { enabled: false },
      background: 'transparent',
      foreColor: fore,
    },
    theme: { mode: (dark ? 'dark' : 'light') as 'dark' | 'light' },
    colors: ['#6366f1', '#f59e0b', '#10b981'],
    dataLabels: { enabled: false },
    stroke: { curve: 'smooth' as const, width: 2.5 },
    fill: {
      type: 'gradient',
      gradient: { shadeIntensity: 1, opacityFrom: 0.3, opacityTo: 0.03, stops: [0, 90, 100] },
    },
    legend: { position: 'top' as const, horizontalAlign: 'right' as const, labels: { colors: fore } },
    xaxis: {
      categories: (report.value?.days || []).map((d) => d.date.slice(8, 10) + '.' + d.date.slice(5, 7)),
      labels: { style: { colors: fore, fontSize: '11px' }, rotate: -45, hideOverlappingLabels: true },
      tickAmount: 15,
    },
    yaxis: { labels: { style: { colors: fore }, formatter: (v: number) => compact(v) } },
    grid: { borderColor: dark ? '#1f2937' : '#f1f5f9' },
    tooltip: { theme: dark ? 'dark' : 'light', y: { formatter: (v: number) => fmt(v) } },
  }
})

function changeCurrency(c: CurrencyCode) {
  setDisplay(c)
  void load()
}

async function load() {
  if (!startDate.value || !endDate.value || startDate.value > endDate.value) return
  loading.value = true
  error.value = null
  try {
    const res = await fetchProfitReport(startDate.value, endDate.value, display.value)
    report.value = res.data || null
  } catch (e) {
    error.value = formatApiError(e, t('profitDashboard.loadError'))
    report.value = null
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
.presets { display: inline-flex; height: 2.5rem; align-items: center; gap: 0.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0.25rem; }
.preset { height: 100%; border-radius: 0.375rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 500; color: #6b7280; }
.preset:hover:not(.active) { background: #f3f4f6; }
.preset.active { background: #465fff; color: #fff; }
.kpi-card { border-radius: 1rem; border: 1px solid #e5e7eb; border-bottom: 3px solid var(--accent); background: #fff; padding: 1.25rem; }
.kpi-label { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; color: #6b7280; }
.kpi-label svg { color: var(--accent); }
.kpi-value { margin-top: 0.5rem; font-size: 1.375rem; font-weight: 700; color: #1f2937; }
.kpi-hint { margin-top: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.th.text-right { text-align: right; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.tag { margin-left: 0.5rem; border-radius: 9999px; background: #eef2ff; padding: 0.125rem 0.5rem; font-size: 0.6875rem; font-weight: 500; color: #4f46e5; }
.tag-service { background: #fdf2f8; color: #db2777; }
.margin-cell { display: flex; min-width: 7rem; align-items: center; gap: 0.5rem; }
.margin-bar { height: 0.375rem; flex: 1; overflow: hidden; border-radius: 9999px; background: #f1f5f9; }
.margin-bar span { display: block; height: 100%; border-radius: 9999px; background: currentColor; }
.gain { color: #059669; }
.loss { color: #dc2626; }
:global(html.dark .card),
:global(html.dark .kpi-card) { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
:global(html.dark .kpi-value) { color: rgba(255, 255, 255, 0.92); }
:global(html.dark .date-range),
:global(html.dark .icon-btn) { border-color: #374151; background: #111827; color: rgba(255, 255, 255, 0.92); }
:global(html.dark input.date-input) { background-color: transparent !important; color: rgba(255, 255, 255, 0.92) !important; color-scheme: dark; }
:global(html.dark .icon-btn:hover) { background: #1f2937; }
:global(html.dark .presets) { border-color: #374151; }
:global(html.dark .preset:not(.active)) { color: #9ca3af; }
:global(html.dark .preset:hover:not(.active)) { background: #1f2937; }
:global(html.dark .margin-bar) { background: #1f2937; }
:global(html.dark .tag) { background: rgba(99, 102, 241, 0.15); color: #a5b4fc; }
:global(html.dark .tag-service) { background: rgba(236, 72, 153, 0.15); color: #f9a8d4; }
:global(html.dark .gain) { color: #34d399; }
:global(html.dark .loss) { color: #f87171; }
:global(html.dark .err) { border-color: rgba(248, 113, 113, 0.45); background: rgba(127, 29, 29, 0.35); color: #fecaca; }
</style>
