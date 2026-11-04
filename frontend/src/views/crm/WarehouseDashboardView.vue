<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.warehouseDashboard')" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <input
        v-model="startDate"
        type="date"
        class="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90"
      />
      <input
        v-model="endDate"
        type="date"
        class="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90"
      />
      <DateRangePresets v-model:start="startDate" v-model:end="endDate" />
      <button
        type="button"
        class="ms-auto inline-flex h-10 items-center gap-2 rounded-lg border border-gray-200 px-3 text-sm text-gray-600 hover:bg-gray-50 dark:border-gray-800 dark:text-gray-300 dark:hover:bg-white/[0.03]"
        :disabled="loading"
        :title="t('common.refresh')"
        @click="load"
      >
        <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
      </button>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
    >
      {{ error }}
    </div>

    <p class="mb-4 text-sm text-gray-500 dark:text-gray-400">{{ t('warehouseDashboard.subtitle') }}</p>

    <div class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5 md:gap-6">
      <component
        :is="card.to ? 'router-link' : 'article'"
        v-for="card in summaryCards"
        :key="card.key"
        :to="card.to"
        class="rounded-2xl border bg-gradient-to-br to-white p-5 dark:to-white/[0.03] md:p-6"
        :class="[card.box, card.to ? 'cursor-pointer transition hover:shadow-theme-sm' : '']"
      >
        <div class="flex h-12 w-12 items-center justify-center rounded-xl text-white shadow-theme-sm" :class="card.iconBox">
          <component :is="card.icon" class="h-6 w-6" />
        </div>
        <p class="mt-4 text-sm" :class="card.label">{{ t(card.titleKey) }}</p>
        <h4 class="mt-1 text-title-sm font-bold text-gray-800 dark:text-white/90">{{ card.value }}</h4>
        <p class="mt-0.5 text-xs text-gray-500 dark:text-gray-400">{{ t(card.hintKey) }}</p>
      </component>
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 xl:grid-cols-2 md:gap-6">
      <div class="rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 dark:border-gray-800 dark:bg-white/[0.03] sm:px-6 sm:pt-6">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-2.5">
            <span class="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-gray-100 text-gray-700 dark:bg-white/10 dark:text-gray-300">
              <PackageX class="h-4 w-4" />
            </span>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('warehouseDashboard.outOfStockTitle') }}</h3>
          </div>
          <router-link
            :to="{ path: '/stock', query: { availability: 'outOfStock' } }"
            class="text-sm font-medium text-brand-600 hover:underline dark:text-brand-400"
          >
            {{ t('nav.stock') }}
          </router-link>
        </div>
        <div class="h-[280px] overflow-auto">
          <p v-if="outOfStockRows.length === 0" class="py-8 text-center text-sm text-gray-500">
            {{ t('warehouseDashboard.outOfStockEmpty') }}
          </p>
          <table v-else class="min-w-full text-sm">
            <thead class="sticky top-0 bg-white dark:bg-[#0b1220]">
              <tr class="border-b border-gray-100 text-left text-xs text-gray-500 dark:border-gray-800">
                <th class="py-2 pe-4 font-medium">{{ t('warehouseDashboard.goods') }}</th>
                <th class="py-2 pe-4 text-right font-medium">{{ t('warehouseDashboard.qty') }}</th>
                <th class="py-2 font-medium">{{ t('warehouseDashboard.warehouses') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in outOfStockRows"
                :key="`out-${row.goodsId}`"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="py-2 pe-4">
                  <span class="font-medium text-gray-800 dark:text-white/90">{{ row.name }}</span>
                  <span v-if="row.unit" class="ms-1 text-xs text-gray-400">{{ row.unit }}</span>
                </td>
                <td class="py-2 pe-4 text-right font-semibold text-gray-700 dark:text-gray-300">
                  {{ row.qtyLabel }}
                </td>
                <td class="py-2 text-gray-600 dark:text-gray-400">{{ row.warehouses || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 dark:border-gray-800 dark:bg-white/[0.03] sm:px-6 sm:pt-6">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-2">
          <div class="flex items-center gap-2.5">
            <span class="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-400">
              <AlertTriangle class="h-4 w-4" />
            </span>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('warehouseDashboard.lowStockTitle') }}</h3>
          </div>
          <router-link
            :to="{ path: '/stock', query: { availability: 'lowStock' } }"
            class="text-sm font-medium text-brand-600 hover:underline dark:text-brand-400"
          >
            {{ t('stock.availability.lowStock') }}
          </router-link>
        </div>
        <div class="h-[280px] overflow-auto">
          <p v-if="lowStockRows.length === 0" class="py-8 text-center text-sm text-gray-500">
            {{ t('warehouseDashboard.lowStockEmpty') }}
          </p>
          <table v-else class="min-w-full text-sm">
            <thead class="sticky top-0 bg-white dark:bg-[#0b1220]">
              <tr class="border-b border-gray-100 text-left text-xs text-gray-500 dark:border-gray-800">
                <th class="py-2 pe-4 font-medium">{{ t('warehouseDashboard.goods') }}</th>
                <th class="py-2 pe-4 text-right font-medium">{{ t('warehouseDashboard.qty') }}</th>
                <th class="py-2 pe-4 text-right font-medium">{{ t('warehouseDashboard.minQty') }}</th>
                <th class="py-2 font-medium">{{ t('warehouseDashboard.warehouses') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in lowStockRows"
                :key="`low-${row.goodsId}`"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="py-2 pe-4">
                  <router-link
                    :to="{ path: '/stock', query: { availability: 'lowStock' } }"
                    class="font-medium text-brand-600 hover:underline dark:text-brand-400"
                  >
                    {{ row.name }}
                  </router-link>
                  <span v-if="row.unit" class="ms-1 text-xs text-gray-400">{{ row.unit }}</span>
                </td>
                <td class="py-2 pe-4 text-right font-semibold text-error-600 dark:text-error-400 whitespace-pre-line">
                  {{ row.qtyLabel }}
                </td>
                <td class="py-2 pe-4 text-right text-gray-600 dark:text-gray-400 whitespace-pre-line">
                  {{ row.minLabel }}
                </td>
                <td class="py-2 text-gray-600 dark:text-gray-400">{{ row.warehouses || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <CurrencyScopeToggle :model-value="scope" @update:model-value="changeScope" />
    </div>

    <div class="grid grid-cols-12 gap-4 md:gap-6">
      <div v-for="w in widgets" :key="w.id" class="col-span-12 xl:col-span-6">
        <DashboardWidget
          :title="t(w.titleKey)"
          :icon="w.icon"
          :name-label="t(w.nameKey)"
          :value-label="t(w.valueKey)"
          :rows="w.rows"
          :format-as="w.formatAs"
          :currency="display"
          :tone="w.tone"
        />
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import DashboardWidget, { type DashboardRow } from '@/components/crm/DashboardWidget.vue'
import CurrencyScopeToggle, { type CurrencyScope } from '@/components/crm/CurrencyScopeToggle.vue'
import DateRangePresets from '@/components/crm/DateRangePresets.vue'
import {
  fetchGoodsGroupSummary,
  fetchTopGoodsByAmount,
  fetchTopGoodsByQuantity,
} from '@/api/dashboard'
import { fetchStocks, type Stock } from '@/api/stocks'
import { formatApiError } from '@/api/http'
import { money } from '@/utils/format'
import { useReportCurrency } from '@/composables/useReportCurrency'
import { useStockAlerts } from '@/composables/useStockAlerts'
import {
  AlertTriangle,
  Boxes,
  Package,
  PackageX,
  RefreshCw,
  ShoppingBag,
  Trophy,
  Wallet,
} from 'lucide-vue-next'

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

const { t } = useI18n()
const { display, setDisplay, loadRates } = useReportCurrency()
const { outOfStockRows, lowStockRows, loadStockAlerts } = useStockAlerts()

const SCOPE_KEY = 'wincrm_warehouse_currency_scope'
const scope = ref<CurrencyScope>(restoreScope())
const only = computed(() => scope.value !== 'ALL')

const startDate = ref(isoDate(new Date(new Date().getFullYear(), new Date().getMonth(), 1)))
const endDate = ref(isoDate(new Date()))
const loading = ref(false)
const error = ref<string | null>(null)

const totalQty = ref(0)
const totalCost = ref(0)
const totalSelling = ref(0)

const widgets = ref<WidgetDef[]>([
  {
    id: 'topAmount',
    titleKey: 'home.topProducts',
    icon: Trophy,
    nameKey: 'home.name',
    valueKey: 'home.netRevenue',
    tone: 'blue',
    formatAs: 'money',
    rows: [],
  },
  {
    id: 'topQty',
    titleKey: 'home.topProducts',
    icon: Package,
    nameKey: 'home.name',
    valueKey: 'home.quantity',
    tone: 'indigo',
    formatAs: 'number',
    rows: [],
  },
  {
    id: 'groups',
    titleKey: 'home.productGroups',
    icon: Boxes,
    nameKey: 'home.name',
    valueKey: 'home.netRevenue',
    tone: 'violet',
    formatAs: 'money',
    rows: [],
  },
])

const summaryCards = computed(() => [
  {
    key: 'qty',
    titleKey: 'warehouseDashboard.totalQty',
    hintKey: 'warehouseDashboard.totalQtyHint',
    value: formatQty(totalQty.value),
    icon: Package,
    box: 'border-brand-200 from-brand-50 dark:border-brand-500/20 dark:from-brand-500/10',
    iconBox: 'bg-brand-500',
    label: 'text-brand-600 dark:text-brand-400',
  },
  {
    key: 'cost',
    titleKey: 'warehouseDashboard.totalCost',
    hintKey: 'warehouseDashboard.totalCostHint',
    value: money(totalCost.value),
    icon: Wallet,
    box: 'border-orange-200 from-orange-50 dark:border-orange-500/20 dark:from-orange-500/10',
    iconBox: 'bg-orange-500',
    label: 'text-orange-600 dark:text-orange-400',
  },
  {
    key: 'selling',
    titleKey: 'warehouseDashboard.totalSelling',
    hintKey: 'warehouseDashboard.totalSellingHint',
    value: money(totalSelling.value),
    icon: ShoppingBag,
    box: 'border-success-200 from-success-50 dark:border-success-500/20 dark:from-success-500/10',
    iconBox: 'bg-success-500',
    label: 'text-success-600 dark:text-success-400',
  },
  {
    key: 'out',
    titleKey: 'warehouseDashboard.outOfStockCount',
    hintKey: 'warehouseDashboard.outOfStockCountHint',
    value: String(outOfStockRows.value.length),
    icon: PackageX,
    box: 'border-gray-300 from-gray-50 dark:border-gray-600 dark:from-white/[0.04]',
    iconBox: 'bg-gray-700',
    label: 'text-gray-700 dark:text-gray-300',
    to: { path: '/stock', query: { availability: 'outOfStock' } },
  },
  {
    key: 'low',
    titleKey: 'warehouseDashboard.lowStockCount',
    hintKey: 'warehouseDashboard.lowStockCountHint',
    value: String(lowStockRows.value.length),
    icon: AlertTriangle,
    box: 'border-error-200 from-error-50 dark:border-error-500/20 dark:from-error-500/10',
    iconBox: 'bg-error-500',
    label: 'text-error-600 dark:text-error-400',
    to: { path: '/stock', query: { availability: 'lowStock' } },
  },
])

function restoreScope(): CurrencyScope {
  const raw = localStorage.getItem(SCOPE_KEY)
  return raw === 'UZS' || raw === 'USD' ? raw : 'ALL'
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function isoDate(d: Date) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function amt(v?: number | null) {
  return Number(v || 0)
}

function formatQty(v: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v || 0))
}

function applyScopeCurrency() {
  const currency = scope.value === 'USD' ? 'USD' : 'UZS'
  if (display.value !== currency) setDisplay(currency)
}

async function changeScope(next: CurrencyScope) {
  scope.value = next
  localStorage.setItem(SCOPE_KEY, next)
  applyScopeCurrency()
  await loadWidgets()
}

function setWidgetRows(id: string, rows: DashboardRow[]) {
  const w = widgets.value.find((x) => x.id === id)
  if (w) w.rows = rows
}

function stockSellingValue(s: Stock) {
  if (s.sellingBase != null) return amt(s.sellingBase) * amt(s.count)
  return amt(s.priceSelling) * amt(s.count)
}

async function loadStockCards() {
  const { stocks, goods } = await loadStockAlerts()
  // Mahsulotlar soni = turlar (SKU), jami dona/kv.m emas
  totalQty.value = goods.length
  totalCost.value = stocks.reduce((sum, s) => sum + amt(s.totalCost), 0)
  totalSelling.value = stocks.reduce((sum, s) => sum + stockSellingValue(s), 0)
}

async function loadWidgets() {
  const cur = display.value
  const one = only.value
  const [a, q, g] = await Promise.all([
    fetchTopGoodsByAmount(startDate.value, endDate.value, cur, one),
    fetchTopGoodsByQuantity(startDate.value, endDate.value, cur, one),
    fetchGoodsGroupSummary(startDate.value, endDate.value, cur, one),
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
}

async function load() {
  loading.value = true
  error.value = null
  try {
    applyScopeCurrency()
    await Promise.all([
      loadStockCards(),
      loadWidgets(),
      loadRates(startDate.value, endDate.value),
    ])
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

watch([startDate, endDate], ([from, to]) => {
  if (from && to && from <= to) void load()
})

onMounted(() => {
  applyScopeCurrency()
  void load()
})
</script>
