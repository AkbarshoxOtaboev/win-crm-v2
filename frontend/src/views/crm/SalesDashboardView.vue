<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Sotuv dashboard" />

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <!-- Header / filters -->
    <div class="card mb-4 p-5">
      <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Sotuv dashboard</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Buyurtmalar holati va sotuvchilar bo‘yicha statistika
          </p>
        </div>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <label class="date-range">
          <CalendarDays class="h-4 w-4 shrink-0 text-gray-400" />
          <input v-model="startDate" type="date" class="date-input" />
          <span class="text-gray-400">—</span>
          <input v-model="endDate" type="date" class="date-input" />
        </label>
        <button type="button" class="icon-btn" title="Filter" @click="showFilters = !showFilters">
          <ListFilter class="h-4 w-4" />
        </button>
        <button type="button" class="icon-btn" title="Tozalash" @click="clearFilters">
          <X class="h-4 w-4" />
        </button>
        <button
          type="button"
          class="icon-btn ms-auto"
          title="Yangilash"
          :disabled="loading"
          @click="load"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
      <div v-if="showFilters" class="mt-3 flex flex-wrap gap-2 border-t border-gray-100 pt-3 dark:border-gray-800">
        <select v-model="statusFilter" class="field sm:w-48" @change="load">
          <option value="">Barcha holatlar</option>
          <option v-for="s in STATUS_DEFS" :key="s.key" :value="s.key">{{ s.label }}</option>
        </select>
      </div>
    </div>

    <!-- Buyurtmalar holati -->
    <div class="card mb-4 p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Buyurtmalar holati</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Har bir holat bo‘yicha buyurtmalar soni, summa va ulushi
        </p>
      </div>

      <div class="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-6">
        <article
          v-for="s in statusStats"
          :key="s.key"
          class="status-card"
          :style="{ '--accent': s.color }"
        >
          <p class="text-sm font-medium text-gray-600 dark:text-gray-300">{{ s.label }}</p>
          <p class="mt-2 text-2xl font-bold text-gray-800 dark:text-white/90">{{ s.count }}</p>
          <p class="mt-0.5 text-xs text-gray-500">Buyurtmalar</p>
          <p class="mt-3 text-sm font-semibold text-gray-800 dark:text-white/90">
            {{ money(s.sum) }} <span class="font-normal text-gray-500">so‘m</span>
          </p>
          <div class="mt-3 flex items-center justify-between text-xs text-gray-500">
            <span>Ulushi</span>
            <span class="font-medium text-gray-700 dark:text-gray-200">{{ s.share }}%</span>
          </div>
          <div class="mt-1.5 h-1.5 overflow-hidden rounded-full bg-gray-100 dark:bg-gray-800">
            <div class="h-full rounded-full" :style="{ width: `${s.share}%`, background: s.color }" />
          </div>
        </article>
      </div>

      <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div>
          <h4 class="mb-2 text-sm font-semibold text-gray-700 dark:text-gray-200">Holat bo‘yicha summa</h4>
          <VueApexCharts
            v-if="chartMounted"
            :key="`status-bar-${chartThemeKey}`"
            type="bar"
            height="280"
            :options="statusBarOptions"
            :series="statusBarSeries"
          />
        </div>
        <div>
          <h4 class="mb-2 text-sm font-semibold text-gray-700 dark:text-gray-200">Holat bo‘yicha taqsimot</h4>
          <VueApexCharts
            v-if="chartMounted"
            :key="`status-donut-${chartThemeKey}`"
            type="donut"
            height="280"
            :options="statusDonutOptions"
            :series="statusDonutSeries"
          />
        </div>
      </div>
    </div>

    <!-- KPI summary -->
    <div class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <article class="kpi-card" style="--accent: #3b82f6">
        <div class="flex items-center gap-2">
          <span class="kpi-dot" />
          <p class="text-sm text-gray-500">Umumiy jami</p>
        </div>
        <p class="mt-3 text-xl font-bold" style="color: #3b82f6">{{ compactSom(kpi.total) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ kpi.count }} ta buyurtma</p>
      </article>
      <article class="kpi-card" style="--accent: #ef4444">
        <div class="flex items-center gap-2">
          <span class="kpi-dot" />
          <p class="text-sm text-gray-500">Jami qarz</p>
        </div>
        <p class="mt-3 text-xl font-bold" style="color: #ef4444">{{ compactSom(kpi.debt) }}</p>
        <p class="mt-1 text-xs text-gray-500">Tanlangan davrdagi barcha qarzlar</p>
      </article>
      <article class="kpi-card" style="--accent: #22c55e">
        <div class="flex items-center gap-2">
          <span class="kpi-dot" />
          <p class="text-sm text-gray-500">Yakunlangan sotuv</p>
        </div>
        <p class="mt-3 text-xl font-bold" style="color: #22c55e">{{ compactSom(kpi.completedSum) }}</p>
        <p class="mt-1 text-xs text-gray-500">{{ kpi.completedCount }} ta buyurtma</p>
      </article>
      <article class="kpi-card" style="--accent: #f59e0b">
        <div class="flex items-center gap-2">
          <span class="kpi-dot" />
          <p class="text-sm text-gray-500">Jarayondagi buyurtmalar</p>
        </div>
        <p class="mt-3 text-xl font-bold" style="color: #f59e0b">{{ kpi.inProgress }}</p>
        <p class="mt-1 text-xs text-gray-500">Yakunlanmagan buyurtmalar</p>
      </article>
    </div>

    <!-- Sotuv dinamikasi -->
    <div class="card mb-4 p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Sotuv dinamikasi</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">Kunlik sotuv va qarz o‘sishi</p>
      </div>
      <VueApexCharts
        v-if="chartMounted"
        :key="`dynamics-${chartThemeKey}`"
        type="area"
        height="320"
        :options="dynamicsOptions"
        :series="dynamicsSeries"
      />
    </div>

    <!-- Foydalanuvchilar -->
    <div class="card p-5">
      <div class="mb-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">
          Foydalanuvchilar bo‘yicha buyurtmalar
        </h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Har bir sotuvchiga tegishli buyurtmalar soni va summasi
        </p>
      </div>
      <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div>
          <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-200">Sotuvchilar: sotuv va qarz</h4>
          <p class="mb-2 text-xs text-gray-500">Har bir sotuvchi bo‘yicha solishtirma</p>
          <VueApexCharts
            v-if="chartMounted"
            :key="`seller-scatter-${chartThemeKey}`"
            type="scatter"
            height="300"
            :options="sellerScatterOptions"
            :series="sellerScatterSeries"
          />
        </div>
        <div>
          <h4 class="text-sm font-semibold text-gray-700 dark:text-gray-200">Buyurtmalar soni bo‘yicha</h4>
          <p class="mb-2 text-xs text-gray-500">Sotuvchilar kesimida buyurtmalar taqsimoti</p>
          <VueApexCharts
            v-if="chartMounted"
            :key="`seller-pie-${chartThemeKey}`"
            type="pie"
            height="300"
            :options="sellerPieOptions"
            :series="sellerPieSeries"
          />
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchSaleOrdersByDateRange, type SaleOrder } from '@/api/sales'
import { formatApiError } from '@/api/http'
import { money, today } from '@/utils/format'
import VueApexCharts from 'vue3-apexcharts'
import { CalendarDays, ListFilter, RefreshCw, X } from 'lucide-vue-next'
import { useTheme } from '@/components/layout/ThemeProvider.vue'

const { t } = useI18n()
const { isDarkMode } = useTheme()
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

const STATUS_KEYS = ['NEW', 'CONFIRMED', 'PROCESSING', 'DELIVERED', 'COMPLETED', 'CANCELLED'] as const
const STATUS_COLORS: Record<(typeof STATUS_KEYS)[number], string> = {
  NEW: '#3b82f6',
  CONFIRMED: '#fbbf24',
  PROCESSING: '#f97316',
  DELIVERED: '#34d399',
  COMPLETED: '#059669',
  CANCELLED: '#ef4444',
}

const STATUS_DEFS = computed(() =>
  STATUS_KEYS.map((key) => ({
    key,
    label: t(`saleStatus.${key}`),
    color: STATUS_COLORS[key],
  })),
)

const IN_PROGRESS = new Set(['NEW', 'CONFIRMED', 'PROCESSING', 'DELIVERED'])

const error = ref<string | null>(null)
const loading = ref(false)
const chartMounted = ref(false)
const showFilters = ref(false)
const startDate = ref(monthStart())
const endDate = ref(today())
const statusFilter = ref('')
const orders = ref<SaleOrder[]>([])

const filteredOrders = computed(() => {
  if (!statusFilter.value) return orders.value
  return orders.value.filter((o) => normStatus(o) === statusFilter.value)
})

const grandTotal = computed(() =>
  filteredOrders.value.reduce((s, o) => s + amt(o.totalSum), 0),
)

const statusStats = computed(() =>
  STATUS_DEFS.value.map((def) => {
    const list = filteredOrders.value.filter((o) => normStatus(o) === def.key)
    const sum = list.reduce((s, o) => s + amt(o.totalSum), 0)
    const share = grandTotal.value > 0 ? Math.round((sum / grandTotal.value) * 100) : 0
    return { ...def, count: list.length, sum, share }
  }),
)

const kpi = computed(() => {
  const list = filteredOrders.value
  const completed = list.filter((o) => normStatus(o) === 'COMPLETED')
  return {
    total: list.reduce((s, o) => s + amt(o.totalSum), 0),
    debt: list.reduce((s, o) => s + amt(o.debtSum), 0),
    count: list.length,
    completedSum: completed.reduce((s, o) => s + amt(o.totalSum), 0),
    completedCount: completed.length,
    inProgress: list.filter((o) => IN_PROGRESS.has(normStatus(o))).length,
  }
})

const statusBarSeries = computed(() => [
  { name: 'Summa', data: statusStats.value.map((s) => s.sum) },
])

const statusBarOptions = computed(() => ({
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'bar' as const,
    toolbar: { show: false },
    background: 'transparent',
    foreColor: chartUi.value.fore,
  },
  theme: { mode: chartUi.value.mode },
  plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '55%', distributed: true } },
  colors: STATUS_DEFS.value.map((s) => s.color),
  dataLabels: { enabled: false },
  legend: { show: false },
  xaxis: {
    categories: STATUS_DEFS.value.map((s) => s.label),
    labels: {
      formatter: (v: string) => compactNum(Number(v)),
      style: { colors: chartUi.value.fore, fontSize: '12px' },
    },
  },
  yaxis: { labels: { style: { colors: chartUi.value.fore, fontSize: '12px' } } },
  grid: { borderColor: chartUi.value.grid, xaxis: { lines: { show: true } }, yaxis: { lines: { show: false } } },
  tooltip: { theme: chartUi.value.mode, y: { formatter: (v: number) => `${money(v)} so‘m` } },
}))

const statusDonutSeries = computed(() => statusStats.value.map((s) => s.sum))

const statusDonutOptions = computed(() => ({
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'donut' as const,
    background: 'transparent',
    foreColor: chartUi.value.fore,
  },
  theme: { mode: chartUi.value.mode },
  labels: STATUS_DEFS.value.map((s) => s.label),
  colors: STATUS_DEFS.value.map((s) => s.color),
  legend: { position: 'bottom' as const, fontSize: '12px', labels: { colors: chartUi.value.fore } },
  dataLabels: { enabled: false },
  stroke: { colors: [isDarkMode.value ? '#111827' : '#ffffff'] },
  plotOptions: {
    pie: {
      donut: {
        size: '70%',
        labels: {
          show: true,
          name: { show: true, fontSize: '12px', color: chartUi.value.fore, offsetY: -8 },
          value: {
            show: true,
            fontSize: '20px',
            fontWeight: 700,
            color: chartUi.value.text,
            formatter: () => compactNum(grandTotal.value),
          },
          total: {
            show: true,
            label: 'Umumiy jami',
            fontSize: '12px',
            color: chartUi.value.fore,
            formatter: () => compactNum(grandTotal.value),
          },
        },
      },
    },
  },
  tooltip: { theme: chartUi.value.mode, y: { formatter: (v: number) => `${money(v)} so‘m` } },
}))

const dailyBuckets = computed(() => {
  const start = parseDate(startDate.value)
  const end = parseDate(endDate.value)
  const keys: string[] = []
  const labels: string[] = []
  if (!start || !end) return { keys, labels, sales: [] as number[], debt: [] as number[] }

  for (let d = new Date(start); d <= end; d = addDays(d, 1)) {
    keys.push(isoDate(d))
    labels.push(`${pad(d.getMonth() + 1)}-${pad(d.getDate())}`)
  }

  const salesMap = Object.fromEntries(keys.map((k) => [k, 0]))
  const debtMap = Object.fromEntries(keys.map((k) => [k, 0]))
  for (const o of filteredOrders.value) {
    const key = isoDate(new Date(o.orderDate || ''))
    if (key in salesMap) {
      salesMap[key] += amt(o.totalSum)
      debtMap[key] += amt(o.debtSum)
    }
  }
  return {
    keys,
    labels,
    sales: keys.map((k) => salesMap[k]),
    debt: keys.map((k) => debtMap[k]),
  }
})

const dynamicsSeries = computed(() => [
  { name: 'Sotilgan summa', data: dailyBuckets.value.sales },
  { name: 'Qarz summasi', data: dailyBuckets.value.debt },
])

const dynamicsOptions = computed(() => ({
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'area' as const,
    toolbar: { show: false },
    zoom: { enabled: false },
    background: 'transparent',
    foreColor: chartUi.value.fore,
  },
  theme: { mode: chartUi.value.mode },
  colors: ['#6366f1', '#ef4444'],
  dataLabels: { enabled: false },
  stroke: { curve: 'smooth' as const, width: 2.5 },
  fill: {
    type: 'gradient',
    gradient: { shadeIntensity: 1, opacityFrom: 0.35, opacityTo: 0.05, stops: [0, 90, 100] },
  },
  legend: { position: 'top' as const, horizontalAlign: 'right' as const, labels: { colors: chartUi.value.fore } },
  xaxis: {
    categories: dailyBuckets.value.labels,
    labels: { style: { colors: chartUi.value.fore, fontSize: '11px' } },
  },
  yaxis: { labels: { style: { colors: chartUi.value.fore }, formatter: (v: number) => compactNum(v) } },
  grid: { borderColor: chartUi.value.grid },
  tooltip: { theme: chartUi.value.mode, y: { formatter: (v: number) => `${money(v)} so‘m` } },
}))

const sellerStats = computed(() => {
  const map = new Map<string, { name: string; count: number; sales: number; debt: number }>()
  for (const o of filteredOrders.value) {
    const name = o.userFullName || `User #${o.userId || '?'}`
    const cur = map.get(name) || { name, count: 0, sales: 0, debt: 0 }
    cur.count += 1
    cur.sales += amt(o.totalSum)
    cur.debt += amt(o.debtSum)
    map.set(name, cur)
  }
  return [...map.values()].sort((a, b) => b.sales - a.sales)
})

const sellerScatterSeries = computed(() => [
  {
    name: 'Sotilgan summa',
    data: sellerStats.value.map((s, i) => ({ x: i + 1, y: s.sales })),
  },
  {
    name: 'Qarz summasi',
    data: sellerStats.value.map((s, i) => ({ x: i + 1, y: s.debt })),
  },
])

const sellerScatterOptions = computed(() => ({
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'scatter' as const,
    toolbar: { show: false },
    zoom: { enabled: false },
    background: 'transparent',
    foreColor: chartUi.value.fore,
  },
  theme: { mode: chartUi.value.mode },
  colors: ['#3b82f6', '#ef4444'],
  markers: { size: 8, strokeWidth: 0 },
  legend: { position: 'top' as const, horizontalAlign: 'right' as const, labels: { colors: chartUi.value.fore } },
  xaxis: {
    type: 'numeric' as const,
    tickAmount: Math.max(sellerStats.value.length, 1),
    min: 0.5,
    max: Math.max(sellerStats.value.length, 1) + 0.5,
    labels: {
      style: { colors: chartUi.value.fore },
      formatter: (v: string) => {
        const i = Math.round(Number(v)) - 1
        return sellerStats.value[i]?.name || ''
      },
    },
  },
  yaxis: { labels: { style: { colors: chartUi.value.fore }, formatter: (v: number) => compactNum(v) } },
  grid: { borderColor: chartUi.value.grid },
  tooltip: {
    theme: chartUi.value.mode,
    custom: ({ seriesIndex, dataPointIndex }: { seriesIndex: number; dataPointIndex: number }) => {
      const s = sellerStats.value[dataPointIndex]
      if (!s) return ''
      const label = seriesIndex === 0 ? 'Sotilgan summa' : 'Qarz summasi'
      const val = seriesIndex === 0 ? s.sales : s.debt
      const bg = isDarkMode.value ? '#111827' : '#ffffff'
      const fg = isDarkMode.value ? '#f3f4f6' : '#111827'
      const border = isDarkMode.value ? '#374151' : '#e5e7eb'
      return `<div style="padding:8px 10px;background:${bg};color:${fg};border:1px solid ${border};border-radius:8px"><b>${s.name}</b><br/>${label}: ${money(val)} so‘m</div>`
    },
  },
}))

const sellerPieSeries = computed(() => {
  const values = sellerStats.value.map((s) => s.count)
  return values.length ? values : [0]
})

const sellerPieOptions = computed(() => ({
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'pie' as const,
    background: 'transparent',
    foreColor: chartUi.value.fore,
  },
  theme: { mode: chartUi.value.mode },
  labels: sellerStats.value.length ? sellerStats.value.map((s) => s.name) : ['Ma’lumot yo‘q'],
  colors: sellerStats.value.length
    ? ['#3b82f6', '#22c55e', '#f59e0b', '#8b5cf6', '#06b6d4', '#ef4444', '#84cc16']
    : ['#e5e7eb'],
  stroke: { colors: [isDarkMode.value ? '#111827' : '#ffffff'] },
  legend: { position: 'bottom' as const, labels: { colors: chartUi.value.fore } },
  dataLabels: {
    enabled: sellerStats.value.length > 0,
    formatter: (val: number) => `${Math.round(val)}%`,
    style: { colors: [chartUi.value.text] },
  },
  tooltip: {
    theme: chartUi.value.mode,
    y: {
      formatter: (v: number) => `${v} ta buyurtma`,
    },
  },
}))

function monthStart() {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01`
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function isoDate(d: Date) {
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function parseDate(v: string) {
  if (!v) return null
  const d = new Date(`${v}T00:00:00`)
  return Number.isNaN(d.getTime()) ? null : d
}

function addDays(d: Date, n: number) {
  const x = new Date(d)
  x.setDate(x.getDate() + n)
  return x
}

function amt(v?: number | null) {
  return Number(v || 0)
}

function normStatus(o: SaleOrder) {
  return String(o.orderStatus || o.status || 'NEW').toUpperCase()
}

function compactNum(v: number) {
  if (Math.abs(v) >= 1_000_000_000) return `${(v / 1_000_000_000).toFixed(1)}B`
  if (Math.abs(v) >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M`
  if (Math.abs(v) >= 1000) return `${(v / 1000).toFixed(0)}K`
  return String(Math.round(v))
}

function compactSom(v: number) {
  return `${compactNum(v)} so‘m`
}

function clearFilters() {
  startDate.value = monthStart()
  endDate.value = today()
  statusFilter.value = ''
  showFilters.value = false
  void load()
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchSaleOrdersByDateRange(
      `${startDate.value}T00:00:00`,
      `${endDate.value}T23:59:59`,
    )
    orders.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e, 'Dashboard yuklanmadi')
    orders.value = []
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
.card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: #dc2626;
}
.field {
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
.date-range {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 0.75rem;
}
.date-input {
  border: 0;
  background: transparent;
  font-size: 0.875rem;
  color: #374151;
  outline: none;
}
.icon-btn {
  display: inline-flex;
  height: 2.5rem;
  width: 2.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #4b5563;
}
.icon-btn:hover {
  background: #f9fafb;
}
.icon-btn:disabled {
  opacity: 0.6;
}
.status-card {
  border-radius: 0.875rem;
  border: 1px solid #e5e7eb;
  border-bottom: 3px solid var(--accent);
  background: #fff;
  padding: 1rem;
}
.kpi-card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  border-bottom: 3px solid var(--accent);
  background: #fff;
  padding: 1.25rem;
}
.kpi-dot {
  display: inline-block;
  height: 0.5rem;
  width: 0.5rem;
  border-radius: 9999px;
  background: var(--accent);
}
:global(html.dark .status-card),
:global(html.dark .kpi-card) {
  border-color: #1f2937;
  background: rgba(255, 255, 255, 0.03);
}
:global(html.dark .date-range),
:global(html.dark .icon-btn),
:global(html.dark .field) {
  border-color: #374151;
  background: #111827;
  color: rgba(255, 255, 255, 0.92);
}
:global(html.dark input.date-input) {
  background-color: transparent !important;
  border-color: transparent !important;
  color: rgba(255, 255, 255, 0.92) !important;
  color-scheme: dark;
}
:global(html.dark .icon-btn:hover) {
  background: #1f2937;
}
:global(html.dark .err) {
  border-color: rgba(248, 113, 113, 0.45);
  background: rgba(127, 29, 29, 0.35);
  color: #fecaca;
}
</style>
