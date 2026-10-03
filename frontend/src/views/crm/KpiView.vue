<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.kpi')" />

    <div class="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div class="flex flex-wrap gap-2">
        <button type="button" class="tab" :class="{ active: tab === 'employees' }" @click="tab = 'employees'">
          {{ t('kpi.tabEmployees') }}
        </button>
        <button type="button" class="tab" :class="{ active: tab === 'workshops' }" @click="tab = 'workshops'">
          {{ t('kpi.tabWorkshops') }}
        </button>
      </div>
      <div class="flex flex-wrap items-end gap-2">
        <div class="w-28">
          <label class="lbl">{{ t('kpi.year') }}</label>
          <select v-model.number="year" class="field">
            <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>
        <div class="w-40">
          <label class="lbl">{{ t('kpi.month') }}</label>
          <select v-model.number="month" class="field">
            <option v-for="m in 12" :key="m" :value="m">{{ monthName(m) }}</option>
          </select>
        </div>
        <button type="button" class="ghost btn-with-icon" :disabled="loading" :title="t('common.refresh')" @click="load">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ tab === 'employees' ? t('kpi.employee') : t('kpi.workshop') }}</th>
              <th class="th">{{ tab === 'employees' ? t('kpi.roles') : t('kpi.manager') }}</th>
              <th class="th text-right">{{ t('kpi.percent') }}</th>
              <th class="th text-right">{{ t('kpi.periodOrders') }}</th>
              <th class="th text-right">{{ t('kpi.periodAmount') }}</th>
              <th class="th text-right">{{ t('kpi.totalAmount') }}</th>
              <th class="th"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading && rows.length === 0"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="rows.length === 0">
              <td colspan="7" class="empty">{{ tab === 'employees' ? t('kpi.empty') : t('kpi.workshopsEmpty') }}</td>
            </tr>
            <tr
              v-for="r in rows"
              :key="r.id"
              class="row border-b border-gray-100 dark:border-gray-800"
              @click="openSverka(r)"
            >
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ r.name || '—' }}</td>
              <td class="td">{{ tab === 'employees' ? (r.roles || []).join(', ') || '—' : r.managerName || '—' }}</td>
              <td class="td text-right">{{ r.percent != null ? `${r.percent}%` : '—' }}</td>
              <td class="td text-right">{{ r.periodCount }}</td>
              <td class="td text-right font-semibold text-gray-800 dark:text-white/90">{{ money(r.periodAmount) }}</td>
              <td class="td text-right">{{ money(r.totalAmount) }}</td>
              <td class="td text-right">
                <button type="button" class="link" @click.stop="openSverka(r)">{{ t('kpi.sverka') }}</button>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="rows.length > 0">
            <tr class="sum-row">
              <td class="td" colspan="3">{{ t('kpi.total') }}</td>
              <td class="td text-right">{{ totals.count }}</td>
              <td class="td text-right">{{ money(totals.period) }}</td>
              <td class="td text-right">{{ money(totals.all) }}</td>
              <td class="td"></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <div v-if="sverka" class="overlay" @click.self="sverka = null">
      <div class="modal">
        <div class="mb-1 flex items-start justify-between gap-3">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">
            {{ t('kpi.sverkaTitle', { name: sverka.row.name || '—' }) }}
          </h3>
          <button type="button" class="ghost btn-sm" @click="sverka = null">{{ t('kpi.close') }}</button>
        </div>
        <p class="hint mb-4">{{ sverka.kind === 'employees' ? t('kpi.sverkaHint') : t('kpi.workshopSverkaHint') }}</p>

        <div class="mb-3 flex flex-wrap gap-2">
          <button type="button" class="tab" :class="{ active: !sverka.allPeriods }" @click="setSverkaPeriod(false)">
            {{ t('kpi.selectedMonth') }}: {{ monthName(month) }} {{ year }}
          </button>
          <button type="button" class="tab" :class="{ active: sverka.allPeriods }" @click="setSverkaPeriod(true)">
            {{ t('kpi.allPeriods') }}
          </button>
        </div>

        <div class="max-h-[60vh] overflow-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">{{ t('kpi.date') }}</th>
                <th class="th">{{ t('kpi.order') }}</th>
                <th class="th">{{ t('kpi.client') }}</th>
                <th class="th text-right">{{ t('kpi.orderSum') }}</th>
                <th class="th text-right">%</th>
                <th class="th text-right">{{ t('kpi.amount') }}</th>
                <th class="th text-right">{{ t('kpi.running') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="sverka.loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
              <tr v-else-if="sverkaRows.length === 0"><td colspan="7" class="empty">{{ t('kpi.noEntries') }}</td></tr>
              <tr v-for="e in sverkaRows" :key="e.id" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td whitespace-nowrap">{{ formatDate(e.earnedAt) }}</td>
                <td class="td">
                  <RouterLink :to="`/sales/${e.saleOrderId}`" class="link">#{{ e.saleOrderId }}</RouterLink>
                </td>
                <td class="td">{{ e.clientFullName || '—' }}</td>
                <td class="td text-right">
                  {{ money(e.baseAmount) }}
                  <div v-if="fxSnapshotText(e.sourceCurrency, e.sourceAmount, e.exchangeRate)" class="text-xs text-gray-500">
                    {{ fxSnapshotText(e.sourceCurrency, e.sourceAmount, e.exchangeRate) }}
                  </div>
                </td>
                <td class="td text-right">{{ e.percent != null ? `${e.percent}%` : '—' }}</td>
                <td class="td text-right font-medium text-gray-800 dark:text-white/90">{{ money(e.amount) }}</td>
                <td class="td text-right">{{ money(e.running) }}</td>
              </tr>
            </tbody>
            <tfoot v-if="sverkaRows.length > 0">
              <tr class="sum-row">
                <td class="td" colspan="3">{{ t('kpi.total') }}</td>
                <td class="td text-right">{{ money(sverkaTotals.base) }}</td>
                <td class="td"></td>
                <td class="td text-right">{{ money(sverkaTotals.amount) }}</td>
                <td class="td"></td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchKpiSummary,
  fetchKpiUserEntries,
  fetchWorkshopKpiEntries,
  fetchWorkshopKpiSummary,
  type KpiEntry,
  type KpiSummary,
} from '@/api/kpi'
import { formatApiError } from '@/api/http'
import { formatDate, money } from '@/utils/format'
import { fxSnapshotText } from '@/utils/currency'

type Tab = 'employees' | 'workshops'

interface SverkaState {
  kind: Tab
  row: KpiSummary
  allPeriods: boolean
  loading: boolean
  entries: KpiEntry[]
}

const { t, locale } = useI18n()

const now = new Date()
const tab = ref<Tab>('employees')
const year = ref(now.getFullYear())
const month = ref(now.getMonth() + 1)
const years = Array.from({ length: 6 }, (_, i) => now.getFullYear() - i)

const employees = ref<KpiSummary[]>([])
const workshops = ref<KpiSummary[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const sverka = ref<SverkaState | null>(null)

const rows = computed(() => (tab.value === 'employees' ? employees.value : workshops.value))

const totals = computed(() =>
  rows.value.reduce(
    (acc, r) => ({
      count: acc.count + Number(r.periodCount || 0),
      period: acc.period + Number(r.periodAmount || 0),
      all: acc.all + Number(r.totalAmount || 0),
    }),
    { count: 0, period: 0, all: 0 },
  ),
)

const sverkaRows = computed(() => {
  let running = 0
  return (sverka.value?.entries || []).map((e) => {
    running += Number(e.amount || 0)
    return { ...e, running }
  })
})

const sverkaTotals = computed(() =>
  sverkaRows.value.reduce(
    (acc, e) => ({ base: acc.base + Number(e.baseAmount || 0), amount: acc.amount + Number(e.amount || 0) }),
    { base: 0, amount: 0 },
  ),
)

function monthName(m: number) {
  const tag = locale.value === 'ru' ? 'ru-RU' : locale.value === 'en' ? 'en-US' : 'uz-UZ'
  const name = new Intl.DateTimeFormat(tag, { month: 'long' }).format(new Date(2000, m - 1, 1))
  return name.charAt(0).toUpperCase() + name.slice(1)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    if (tab.value === 'employees') {
      const res = await fetchKpiSummary(year.value, month.value)
      employees.value = res.data || []
    } else {
      const res = await fetchWorkshopKpiSummary(year.value, month.value)
      workshops.value = res.data || []
    }
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function loadSverka() {
  const s = sverka.value
  if (!s) return
  s.loading = true
  try {
    const m = s.allPeriods ? null : month.value
    const res =
      s.kind === 'employees'
        ? await fetchKpiUserEntries(s.row.id, year.value, m)
        : await fetchWorkshopKpiEntries(s.row.id, year.value, m)
    if (sverka.value === s) s.entries = res.data || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    s.loading = false
  }
}

function openSverka(row: KpiSummary) {
  sverka.value = { kind: tab.value, row, allPeriods: false, loading: false, entries: [] }
  loadSverka()
}

function setSverkaPeriod(all: boolean) {
  if (!sverka.value || sverka.value.allPeriods === all) return
  sverka.value.allPeriods = all
  loadSverka()
}

watch([tab, year, month], load)

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.th.text-right { text-align: right; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.row { cursor: pointer; }
.row:hover { background: #f9fafb; }
.sum-row .td { font-weight: 600; color: #1f2937; background: #f9fafb; }
.hint { font-size: 0.8125rem; color: #6b7280; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; gap: 0.375rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; color: #374151; }
.ghost:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-sm { height: 2rem; }
.link { font-size: 0.875rem; font-weight: 500; color: #465fff; }
.link:hover { text-decoration: underline; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 1rem; font-size: 0.875rem; color: #4b5563; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.4); padding: 1rem; }
.modal { width: 100%; max-width: 64rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }

.dark .card { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .th, .dark .empty, .dark .hint, .dark .lbl { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .row:hover { background: rgba(255, 255, 255, 0.03); }
.dark .sum-row .td { color: rgba(255, 255, 255, 0.9); background: rgba(255, 255, 255, 0.04); }
.dark .field, .dark .ghost, .dark .tab:not(.active) { border-color: #374151; color: #e5e7eb; }
.dark .field option { background: #111827; }
.dark .err { border-color: #7f1d1d; background: rgba(127, 29, 29, 0.2); color: #fca5a5; }
.dark .modal { background: #111827; border: 1px solid #1f2937; }
</style>
