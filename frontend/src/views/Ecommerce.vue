<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.home')" />

    <div
      v-if="error"
      class="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
    >
      {{ error }}
    </div>

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <input
        v-model="startDate"
        type="date"
        class="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90"
        @change="load"
      />
      <input
        v-model="endDate"
        type="date"
        class="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90"
        @change="load"
      />
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 md:grid-cols-3 md:gap-6">
      <article
        class="rounded-2xl border border-brand-200 bg-gradient-to-br from-brand-50 to-white p-5 dark:border-brand-500/20 dark:from-brand-500/10 dark:to-white/[0.03] md:p-6"
      >
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-500 text-white shadow-theme-sm">
          <ShoppingBag class="h-6 w-6" />
        </div>
        <p class="mt-4 text-sm text-brand-600 dark:text-brand-400">{{ t('home.totalSales') }}</p>
        <h4 class="mt-1 text-title-sm font-bold text-gray-800 dark:text-white/90">{{ money(totals.sales) }}</h4>
      </article>
      <article
        class="rounded-2xl border border-success-200 bg-gradient-to-br from-success-50 to-white p-5 dark:border-success-500/20 dark:from-success-500/10 dark:to-white/[0.03] md:p-6"
      >
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-success-500 text-white shadow-theme-sm">
          <Wallet class="h-6 w-6" />
        </div>
        <p class="mt-4 text-sm text-success-600 dark:text-success-400">{{ t('home.totalPayments') }}</p>
        <h4 class="mt-1 text-title-sm font-bold text-gray-800 dark:text-white/90">{{ money(totals.payments) }}</h4>
      </article>
      <article
        class="rounded-2xl border border-orange-200 bg-gradient-to-br from-orange-50 to-white p-5 dark:border-orange-500/20 dark:from-orange-500/10 dark:to-white/[0.03] md:p-6"
      >
        <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-orange-500 text-white shadow-theme-sm">
          <Receipt class="h-6 w-6" />
        </div>
        <p class="mt-4 text-sm text-orange-600 dark:text-orange-400">{{ t('home.totalExpenses') }}</p>
        <h4 class="mt-1 text-title-sm font-bold text-gray-800 dark:text-white/90">{{ money(totals.expenses) }}</h4>
      </article>
    </div>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 dark:border-gray-800 dark:bg-white/[0.03] sm:px-6 sm:pt-6">
      <div class="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div class="flex items-center gap-2.5">
          <span class="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">
            <BarChart3 class="h-4 w-4" />
          </span>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('home.salesChart') }}</h3>
        </div>
        <div class="inline-flex rounded-lg border border-gray-200 p-1 dark:border-gray-800">
          <button
            v-for="p in chartPeriods"
            :key="p.id"
            type="button"
            class="h-8 rounded-md px-3 text-xs font-medium transition"
            :class="
              chartPeriod === p.id
                ? 'bg-brand-500 text-white'
                : 'text-gray-500 hover:text-gray-800 dark:text-gray-400'
            "
            @click="setChartPeriod(p.id)"
          >
            {{ p.label }}
          </button>
        </div>
      </div>
      <div v-if="chartMounted">
        <VueApexCharts :key="`sales-${locale}`" type="bar" height="280" :options="chartOptions" :series="chartSeries" />
      </div>
    </div>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 dark:border-gray-800 dark:bg-white/[0.03] sm:px-6 sm:pt-6">
      <div class="mb-4 flex items-center gap-2.5">
        <span class="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-orange-50 text-orange-600 dark:bg-orange-500/15 dark:text-orange-400">
          <Wallet class="h-4 w-4" />
        </span>
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('home.dailyPayments') }}</h3>
      </div>
      <div v-if="chartMounted">
        <VueApexCharts :key="`payments-${locale}`" type="bar" height="280" :options="paymentsChartOptions" :series="paymentsChartSeries" />
      </div>
    </div>

    <draggable
      v-model="widgets"
      item-key="id"
      handle=".widget-drag-handle"
      class="grid grid-cols-12 gap-4 md:gap-6"
      :animation="200"
      @end="persistWidgets"
    >
      <template #item="{ element }">
        <div class="col-span-12 xl:col-span-6" data-draggable="true">
          <DashboardWidget
            :title="t(element.titleKey)"
            :icon="element.icon"
            :name-label="t(element.nameKey)"
            :value-label="t(element.valueKey)"
            :rows="element.rows"
            :format-as="element.formatAs"
            :tone="element.tone"
          />
        </div>
      </template>
    </draggable>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import DashboardWidget, { type DashboardRow } from '@/components/crm/DashboardWidget.vue'
import {
  fetchDailyPayments,
  fetchExpenseInfo,
  fetchGoodsGroupSummary,
  fetchPaymentsByType,
  fetchTopGoodsByAmount,
  fetchTopGoodsByQuantity,
  fetchTopSellers,
} from '@/api/dashboard'
import { fetchSaleOrdersByDateRange, type SaleOrder } from '@/api/sales'
import { formatApiError } from '@/api/http'
import { money, today } from '@/utils/format'
import VueApexCharts from 'vue3-apexcharts'
import draggable from 'vuedraggable'
import {
  Banknote,
  BarChart3,
  Boxes,
  Package,
  Receipt,
  ShoppingBag,
  Trophy,
  Users,
  Wallet,
} from 'lucide-vue-next'
import type { Component } from 'vue'

type ChartPeriod = 'daily' | 'weekly' | 'monthly'
type WidgetTone = 'blue' | 'indigo' | 'violet' | 'green' | 'teal' | 'orange'

interface WidgetDef {
  id: string
  titleKey: string
  icon: Component
  nameKey: string
  valueKey: string
  tone: WidgetTone
  formatAs: 'money' | 'number'
  rows: DashboardRow[]
}

const STORAGE_KEY = 'wincrm_dashboard_widgets'
const { t, tm, locale } = useI18n()
const chartPeriods = computed(() => [
  { id: 'daily' as const, label: t('home.daily') },
  { id: 'weekly' as const, label: t('home.weekly') },
  { id: 'monthly' as const, label: t('home.monthly') },
])

const error = ref<string | null>(null)
const startDate = ref(monthStart())
const endDate = ref(today())
const chartPeriod = ref<ChartPeriod>('daily')
const chartMounted = ref(false)
const chartCategories = ref<string[]>([])
const chartValues = ref<number[]>([])
const paymentsCategories = ref<string[]>([])
const paymentsValues = ref<number[]>([])
const totals = ref({ sales: 0, payments: 0, expenses: 0 })
const widgets = ref<WidgetDef[]>(defaultWidgets())

function barChartOptions(categories: string[], color: string) {
  return {
    colors: [color],
    chart: {
      fontFamily: 'Outfit, sans-serif',
      type: 'bar' as const,
      toolbar: { show: false },
    },
    plotOptions: {
      bar: {
        horizontal: false,
        columnWidth: '42%',
        borderRadius: 6,
        borderRadiusApplication: 'end' as const,
      },
    },
    dataLabels: { enabled: false },
    stroke: { show: true, width: 3, colors: ['transparent'] },
    xaxis: {
      categories,
      axisBorder: { show: false },
      axisTicks: { show: false },
      labels: { style: { colors: '#6b7280', fontSize: '12px' } },
    },
    yaxis: {
      labels: {
        formatter: (v: number) => compact(v),
        style: { colors: '#6b7280', fontSize: '12px' },
      },
    },
    grid: { yaxis: { lines: { show: true } }, xaxis: { lines: { show: false } } },
    fill: { opacity: 1 },
    tooltip: {
      y: { formatter: (v: number) => money(v) },
    },
  }
}

const chartSeries = computed(() => [{ name: t('home.sale'), data: chartValues.value }])
const chartOptions = computed(() => {
  void locale.value
  return barChartOptions(chartCategories.value, '#465fff')
})
const paymentsChartSeries = computed(() => [{ name: t('home.payment'), data: paymentsValues.value }])
const paymentsChartOptions = computed(() => {
  void locale.value
  return barChartOptions(paymentsCategories.value, '#f97316')
})

function monthStart() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function isoDate(d: Date) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function addDays(d: Date, n: number) {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}

function amt(v?: number | null) {
  return Number(v || 0)
}

function compact(v: number) {
  if (Math.abs(v) >= 1_000_000_000) return `${(v / 1_000_000_000).toFixed(1)} ${t('home.billion')}`
  if (Math.abs(v) >= 1_000_000) return `${(v / 1_000_000).toFixed(1)} ${t('home.million')}`
  if (Math.abs(v) >= 1000) return `${(v / 1000).toFixed(0)} ${t('home.thousand')}`
  return String(Math.round(v))
}

function formatDayLabel(raw?: string) {
  if (!raw) return '—'
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) {
    const m = String(raw).match(/(\d{4})-(\d{2})-(\d{2})/)
    return m ? `${m[3]}.${m[2]}` : String(raw)
  }
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}`
}

function defaultWidgets(): WidgetDef[] {
  return [
    { id: 'topAmount', titleKey: 'home.topProducts', icon: Trophy, nameKey: 'home.name', valueKey: 'home.netRevenue', tone: 'blue', formatAs: 'money', rows: [] },
    { id: 'topQty', titleKey: 'home.topProducts', icon: Package, nameKey: 'home.name', valueKey: 'home.quantity', tone: 'indigo', formatAs: 'number', rows: [] },
    { id: 'groups', titleKey: 'home.productGroups', icon: Boxes, nameKey: 'home.name', valueKey: 'home.netRevenue', tone: 'violet', formatAs: 'money', rows: [] },
    { id: 'sellers', titleKey: 'home.topSellers', icon: Users, nameKey: 'home.name', valueKey: 'home.netRevenue', tone: 'green', formatAs: 'money', rows: [] },
    { id: 'payTypes', titleKey: 'home.paymentTypes', icon: Banknote, nameKey: 'home.name', valueKey: 'home.netRevenue', tone: 'teal', formatAs: 'money', rows: [] },
  ]
}

function restoreWidgets() {
  const base = defaultWidgets()
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return base
    const ids = JSON.parse(raw) as string[]
    const map = new Map(base.map((w) => [w.id, w]))
    const ordered = ids.map((id) => map.get(id)).filter(Boolean) as WidgetDef[]
    base.forEach((w) => {
      if (!ordered.some((o) => o.id === w.id)) ordered.push(w)
    })
    return ordered
  } catch {
    return base
  }
}

function persistWidgets() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(widgets.value.map((w) => w.id)))
}

function setWidgetRows(id: string, rows: DashboardRow[]) {
  const w = widgets.value.find((x) => x.id === id)
  if (w) w.rows = rows
}

function chartBounds(period: ChartPeriod) {
  const end = new Date()
  end.setHours(23, 59, 59, 0)
  if (period === 'daily') return { start: addDays(end, -13), end }
  if (period === 'weekly') return { start: addDays(end, -55), end }
  return { start: new Date(end.getFullYear(), end.getMonth() - 11, 1), end }
}

function weekKey(d: Date) {
  const tmp = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()))
  const day = tmp.getUTCDay() || 7
  tmp.setUTCDate(tmp.getUTCDate() + 4 - day)
  const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1))
  const week = Math.ceil(((tmp.getTime() - yearStart.getTime()) / 86400000 + 1) / 7)
  return `${tmp.getUTCFullYear()}-W${pad(week)}`
}

function buckets(period: ChartPeriod, start: Date, end: Date) {
  const labels: string[] = []
  const keys: string[] = []
  if (period === 'daily') {
    for (let d = new Date(start); d <= end; d = addDays(d, 1)) {
      keys.push(isoDate(d))
      labels.push(`${pad(d.getDate())}.${pad(d.getMonth() + 1)}`)
    }
  } else if (period === 'weekly') {
    const cursor = new Date(start)
    while (cursor <= end) {
      const key = weekKey(cursor)
      if (!keys.includes(key)) {
        keys.push(key)
        labels.push(`${key.slice(6)}-${t('home.week')}`)
      }
      cursor.setDate(cursor.getDate() + 1)
    }
  } else {
    const months = (tm('home.months') as string[]) || []
    const cursor = new Date(start.getFullYear(), start.getMonth(), 1)
    const last = new Date(end.getFullYear(), end.getMonth(), 1)
    while (cursor <= last) {
      keys.push(`${cursor.getFullYear()}-${pad(cursor.getMonth() + 1)}`)
      labels.push(months[cursor.getMonth()])
      cursor.setMonth(cursor.getMonth() + 1)
    }
  }
  return { keys, labels }
}

function orderKey(order: SaleOrder, period: ChartPeriod) {
  const raw = order.orderDate || ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return ''
  if (period === 'daily') return isoDate(d)
  if (period === 'weekly') return weekKey(d)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

async function loadChart() {
  const { start, end } = chartBounds(chartPeriod.value)
  const { keys, labels } = buckets(chartPeriod.value, start, end)
  const sums = Object.fromEntries(keys.map((k) => [k, 0]))
  try {
    const res = await fetchSaleOrdersByDateRange(
      `${isoDate(start)}T00:00:00`,
      `${isoDate(end)}T23:59:59`,
    )
    for (const o of res.data || []) {
      const key = orderKey(o, chartPeriod.value)
      if (key in sums) sums[key] += amt(o.totalSum)
    }
  } catch {
    /* keep zeros */
  }
  chartCategories.value = labels
  chartValues.value = keys.map((k) => sums[k] || 0)
}

async function setChartPeriod(period: ChartPeriod) {
  chartPeriod.value = period
  await loadChart()
}

async function load() {
  error.value = null
  try {
    const [a, q, g, s, p, d, exp, sales] = await Promise.all([
      fetchTopGoodsByAmount(startDate.value, endDate.value),
      fetchTopGoodsByQuantity(startDate.value, endDate.value),
      fetchGoodsGroupSummary(startDate.value, endDate.value),
      fetchTopSellers(startDate.value, endDate.value),
      fetchPaymentsByType(startDate.value, endDate.value),
      fetchDailyPayments(startDate.value, endDate.value),
      fetchExpenseInfo(startDate.value, endDate.value),
      fetchSaleOrdersByDateRange(`${startDate.value}T00:00:00`, `${endDate.value}T23:59:59`),
    ])

    setWidgetRows(
      'topAmount',
      (a.data || []).map((x) => ({
        id: x.goodsId,
        name: x.goodsName || '—',
        value: amt(x.totalAmount ?? x.amount),
      })),
    )
    setWidgetRows(
      'topQty',
      (q.data || []).map((x) => ({
        id: x.goodsId,
        name: x.goodsName || '—',
        value: amt(x.totalCount ?? x.quantity),
      })),
    )
    setWidgetRows(
      'groups',
      (g.data || []).map((x) => ({
        id: x.goodsGroupId,
        name: x.goodsGroupName || '—',
        value: amt(x.totalAmount ?? x.amount),
      })),
    )
    setWidgetRows(
      'sellers',
      (s.data || []).map((x) => ({
        id: x.userId,
        name: x.userName || x.fullName || x.username || '—',
        value: amt(x.totalAmount ?? x.amount),
      })),
    )
    setWidgetRows(
      'payTypes',
      (p.data || []).map((x) => ({
        id: x.paymentTypeId,
        name: x.paymentTypeName || '—',
        value: amt(x.totalAmount ?? x.amount),
      })),
    )

    const dailyRows = [...(d.data || [])].sort((a, b) =>
      String(a.date || '').localeCompare(String(b.date || '')),
    )
    paymentsCategories.value = dailyRows.map((x) => formatDayLabel(x.date))
    paymentsValues.value = dailyRows.map((x) => amt(x.totalAmount ?? x.amount))

    totals.value = {
      sales: (sales.data || []).reduce((sum, o) => sum + amt(o.totalSum), 0),
      payments: (p.data || []).reduce((sum, x) => sum + amt(x.totalAmount ?? x.amount), 0),
      expenses: (exp.data || []).reduce((sum, x) => sum + amt(x.totalAmount ?? x.amount), 0),
    }
    await loadChart()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

watch(widgets, persistWidgets, { deep: false })
watch(locale, () => {
  loadChart()
})

onMounted(async () => {
  widgets.value = restoreWidgets()
  await load()
  chartMounted.value = true
})
</script>
