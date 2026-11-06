<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.managerCash')" />

    <section class="mb-5">
      <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div class="min-w-0">
          <h3 class="flex items-center gap-2 text-base font-semibold text-gray-800 dark:text-white/90">
            <Landmark class="h-4 w-4 text-brand-500" />
            {{ t('managerCash.title') }}
          </h3>
          <p class="text-xs text-gray-500 dark:text-gray-400">{{ t('managerCash.subtitle') }}</p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <div class="period-toggle cols-3" role="radiogroup" :aria-label="t('managerCash.title')">
            <button
              v-for="mode in POOLS"
              :key="mode"
              type="button"
              class="period-btn"
              :class="{ active: cashPool === mode }"
              role="radio"
              :aria-checked="cashPool === mode"
              @click="cashPool = mode"
            >
              {{ t(`managerCash.${mode === 'OPERATIONAL' ? 'operational' : mode === 'VAULT' ? 'vault' : 'all'}`) }}
            </button>
          </div>
          <button
            type="button"
            class="icon-btn"
            :title="t('managerCash.refresh')"
            :disabled="cashLoading"
            @click="reload"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': cashLoading }" />
          </button>
        </div>
      </div>
      <p class="mb-3 text-xs text-gray-500 dark:text-gray-400">{{ poolHint }}</p>

      <div v-if="cashError" class="err mb-3">{{ cashError }}</div>
      <div v-else-if="cashLoading && displayCash.length === 0" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="displayCash.length === 0 && currencyTotals.length === 0" class="empty">
        {{ t('managerCash.empty') }}
      </div>
      <div v-else class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <div
          v-for="c in displayCash"
          :key="c.paymentTypeId"
          class="kassa-card"
          :class="[`tone-${cashKind(c)}`, { negative: poolAmount(c) < 0 }]"
        >
          <span class="flex items-start justify-between gap-2">
            <span class="kassa-icon"><component :is="cashIcon(c)" class="h-5 w-5" /></span>
            <span class="cur-badge" :class="c.currency === 'USD' ? 'usd' : 'uzs'">
              {{ c.currency === 'USD' ? '$' : t('expenses.uzs') }}
            </span>
          </span>
          <span class="mt-3 block truncate text-sm text-gray-500 dark:text-gray-400">{{ c.paymentTypeName }}</span>
          <span class="kassa-amount">{{ moneyIn(poolAmount(c), c.currency, t('common.currency')) }}</span>
          <span v-if="cashPool === 'ALL'" class="pool-split">
            {{ t('managerCash.operational') }}: {{ moneyIn(opsOf(c), c.currency, t('common.currency')) }}
            · {{ t('managerCash.vault') }}: {{ moneyIn(vaultOf(c), c.currency, t('common.currency')) }}
          </span>
        </div>
        <div
          v-for="tot in currencyTotals"
          :key="tot.currency"
          class="kassa-card total"
          :class="[tot.currency === 'USD' ? 'total-usd' : 'total-uzs', { negative: tot.amount < 0 }]"
        >
          <span class="flex items-start justify-between gap-2">
            <span class="kassa-icon"><component :is="tot.currency === 'USD' ? DollarSign : Coins" class="h-5 w-5" /></span>
            <span class="kassa-count">{{ t('managerCash.cashCount', { n: tot.count }) }}</span>
          </span>
          <span class="mt-3 block truncate text-sm text-gray-500 dark:text-gray-400">
            {{ tot.currency === 'USD' ? t('managerCash.totalUsd') : t('managerCash.totalUzs') }}
          </span>
          <span class="kassa-amount">{{ moneyIn(tot.amount, tot.currency, t('common.currency')) }}</span>
        </div>
      </div>
    </section>

    <div class="card mb-5">
      <div class="head">
        <div>
          <h3 class="title">{{ t('managerCash.periodBalances') }}</h3>
          <p class="sub">{{ t('managerCash.periodBalancesHint') }}</p>
        </div>
      </div>
      <div class="filters">
        <div class="flex flex-wrap items-end gap-3">
          <label class="lbl">
            {{ t('common.from') }}
            <input v-model="fromDate" type="date" class="field" />
          </label>
          <label class="lbl">
            {{ t('common.to') }}
            <input v-model="toDate" type="date" class="field" />
          </label>
          <DateRangePresets v-model:start="fromDate" v-model:end="toDate" />
        </div>
      </div>
      <div v-if="balancesError" class="err mx-5 mt-4">{{ balancesError }}</div>
      <div v-else-if="balancesLoading && periodBalances.length === 0" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="periodBalances.length === 0" class="empty">{{ t('managerCash.emptyBalances') }}</div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('managerCash.cashbox') }}</th>
              <th class="th text-end">{{ t('managerCash.cashIn') }}</th>
              <th class="th text-end">{{ t('managerCash.cashOut') }}</th>
              <th class="th text-end">{{ t('managerCash.cashExpenses') }}</th>
              <th class="th text-end">{{ t('managerCash.cashHanded') }}</th>
              <th class="th text-end">{{ t('managerCash.cashClosing') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="c in periodBalances"
              :key="`${c.paymentTypeId}-${c.currency}`"
              class="border-b border-gray-100 dark:border-gray-800"
            >
              <td class="td">
                {{ c.paymentTypeName }}
                <span class="cur-badge" :class="c.currency === 'USD' ? 'usd' : 'uzs'">{{ c.currency }}</span>
              </td>
              <td class="td text-end text-success-600 dark:text-success-400">+{{ cashMoney(c.incoming, c.currency) }}</td>
              <td class="td text-end text-error-600 dark:text-error-400">−{{ cashMoney(c.outgoing, c.currency) }}</td>
              <td class="td text-end text-error-600 dark:text-error-400">−{{ cashMoney(c.expenses, c.currency) }}</td>
              <td class="td text-end text-orange-600 dark:text-orange-400">−{{ cashMoney(c.handedOver, c.currency) }}</td>
              <td class="td text-end font-semibold text-gray-800 dark:text-white/90">{{ cashMoney(c.closing, c.currency) }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr v-for="tot in balanceTotals" :key="tot.currency" class="border-t border-gray-200 dark:border-gray-700">
              <td class="td font-semibold">{{ t('managerCash.cashTotal', { currency: tot.currency }) }}</td>
              <td class="td text-end">+{{ cashMoney(tot.incoming, tot.currency) }}</td>
              <td class="td text-end">−{{ cashMoney(tot.outgoing, tot.currency) }}</td>
              <td class="td text-end">−{{ cashMoney(tot.expenses, tot.currency) }}</td>
              <td class="td text-end">−{{ cashMoney(tot.handedOver, tot.currency) }}</td>
              <td class="td text-end font-semibold">{{ cashMoney(tot.closing, tot.currency) }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ t('managerCash.historyTitle') }}</h3>
          <p class="sub">{{ t('managerCash.historySubtitle') }}</p>
        </div>
      </div>

      <div v-if="historyError" class="err mx-5 mt-4">{{ historyError }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('managerCash.cashier') }}</th>
              <th class="th">{{ t('cashHandover.cashbox') }}</th>
              <th class="th text-end">{{ t('common.amount') }}</th>
              <th class="th">{{ t('managerCash.reviewer') }}</th>
              <th class="th">{{ t('managerCash.acceptedAt') }}</th>
              <th class="th">{{ t('common.comment') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="historyLoading"><td colspan="8" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="history.length === 0"><td colspan="8" class="empty">{{ t('managerCash.emptyHistory') }}</td></tr>
            <tr v-for="h in history" :key="h.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td text-gray-400">{{ h.id }}</td>
              <td class="td">{{ formatDate(h.handoverDate) }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ h.cashierName || h.cashierId }}</td>
              <td class="td">
                {{ h.paymentTypeName || '—' }}
                <span class="cur-badge" :class="h.currency === 'USD' ? 'usd' : 'uzs'">{{ h.currency }}</span>
              </td>
              <td class="td text-end font-semibold text-emerald-600 dark:text-emerald-400">
                {{ moneyIn(h.amount, h.currency, t('common.currency')) }}
              </td>
              <td class="td">{{ h.reviewerName || '—' }}</td>
              <td class="td">{{ formatDate(h.reviewedAt) }}</td>
              <td class="td">{{ h.reviewComment || h.comment || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Coins, DollarSign, Landmark, RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import DateRangePresets from '@/components/crm/DateRangePresets.vue'
import { fetchCashAvailable, type CashAvailable } from '@/api/expenses'
import { fetchCashHandovers, type CashHandover } from '@/api/cashHandovers'
import { fetchCashBalances, type CashBalance } from '@/api/dashboard'
import { formatApiError } from '@/api/http'
import { formatDate } from '@/utils/format'
import { CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'
import { paymentTypeIcon } from '@/utils/paymentTypeIcons'

const { t } = useI18n()

const POOLS = ['OPERATIONAL', 'VAULT', 'ALL'] as const
type CashPool = (typeof POOLS)[number]
const cashPool = ref<CashPool>('VAULT')

const cash = ref<CashAvailable[]>([])
const cashLoading = ref(false)
const cashError = ref<string | null>(null)

const periodBalances = ref<CashBalance[]>([])
const balancesLoading = ref(false)
const balancesError = ref<string | null>(null)

const history = ref<CashHandover[]>([])
const historyLoading = ref(false)
const historyError = ref<string | null>(null)

function monthStart() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-01`
}
function today() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const fromDate = ref(monthStart())
const toDate = ref(today())

function opsOf(c: CashAvailable) {
  return Number(c.operationalAvailable ?? c.available ?? 0)
}
function vaultOf(c: CashAvailable) {
  return Number(c.vaultAvailable ?? 0)
}
function poolAmount(c: CashAvailable) {
  if (cashPool.value === 'OPERATIONAL') return opsOf(c)
  if (cashPool.value === 'VAULT') return vaultOf(c)
  return opsOf(c) + vaultOf(c)
}

const poolHint = computed(() => {
  if (cashPool.value === 'OPERATIONAL') return t('managerCash.operationalHint')
  if (cashPool.value === 'VAULT') return t('managerCash.vaultHint')
  return t('managerCash.allHint')
})

const displayCash = computed(() =>
  cash.value
    .filter((c) => Math.abs(poolAmount(c)) > 1e-9)
    .sort((a, b) => poolAmount(b) - poolAmount(a)),
)

const currencyTotals = computed(() =>
  CURRENCIES.map((currency) => {
    const list = cash.value.filter((c) => c.currency === currency)
    const visible = list.filter((c) => Math.abs(poolAmount(c)) > 1e-9)
    return {
      currency,
      count: visible.length,
      amount: list.reduce((s, c) => s + poolAmount(c), 0),
    }
  }).filter((tot) => tot.count > 0 || Math.abs(tot.amount) > 1e-9),
)

function cashKind(c: CashAvailable) {
  if (c.paymentTypeIcon) return c.paymentTypeIcon.toLowerCase()
  const name = c.paymentTypeName.toLowerCase()
  if (/naqd|нал|cash/.test(name)) return 'cash'
  if (/bank|банк|perech|transfer|o.tkazma/.test(name)) return 'bank'
  return 'card'
}

function cashIcon(c: CashAvailable) {
  return paymentTypeIcon(cashKind(c).toUpperCase())
}

function cashMoney(v: number | null | undefined, currency: CurrencyCode) {
  return moneyIn(Number(v || 0), currency, t('common.currency'))
}

const balanceTotals = computed(() => {
  const map = new Map<
    CurrencyCode,
    {
      currency: CurrencyCode
      incoming: number
      outgoing: number
      expenses: number
      handedOver: number
      closing: number
    }
  >()
  for (const c of periodBalances.value) {
    const currency = (c.currency || 'UZS') as CurrencyCode
    const cur = map.get(currency) || {
      currency,
      incoming: 0,
      outgoing: 0,
      expenses: 0,
      handedOver: 0,
      closing: 0,
    }
    cur.incoming += Number(c.incoming || 0)
    cur.outgoing += Number(c.outgoing || 0)
    cur.expenses += Number(c.expenses || 0)
    cur.handedOver += Number(c.handedOver || 0)
    cur.closing += Number(c.closing || 0)
    map.set(currency, cur)
  }
  return [...map.values()]
})

async function loadCash() {
  cashLoading.value = true
  cashError.value = null
  try {
    const res = await fetchCashAvailable()
    cash.value = (res.data || []).map((c) => ({
      ...c,
      available: Number(c.available || 0),
      operationalAvailable: Number(c.operationalAvailable ?? c.available ?? 0),
      vaultAvailable: Number(c.vaultAvailable ?? 0),
    }))
  } catch (e) {
    cashError.value = formatApiError(e)
    cash.value = []
  } finally {
    cashLoading.value = false
  }
}

async function loadBalances() {
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) return
  balancesLoading.value = true
  balancesError.value = null
  try {
    const res = await fetchCashBalances(fromDate.value, toDate.value)
    periodBalances.value = res.data || []
  } catch (e) {
    balancesError.value = formatApiError(e)
    periodBalances.value = []
  } finally {
    balancesLoading.value = false
  }
}

async function loadHistory() {
  historyLoading.value = true
  historyError.value = null
  try {
    const res = await fetchCashHandovers({
      fromDate: fromDate.value || undefined,
      toDate: toDate.value || undefined,
      status: 'ACCEPTED',
    })
    history.value = (res.data || []).slice().sort((a, b) => {
      const ta = a.reviewedAt || a.handoverDate || ''
      const tb = b.reviewedAt || b.handoverDate || ''
      return tb.localeCompare(ta) || b.id - a.id
    })
  } catch (e) {
    historyError.value = formatApiError(e)
    history.value = []
  } finally {
    historyLoading.value = false
  }
}

async function onPeriodChange() {
  await Promise.all([loadBalances(), loadHistory()])
}

async function reload() {
  await Promise.all([loadCash(), loadBalances(), loadHistory()])
}

watch([fromDate, toDate], ([from, to]) => {
  if (from && to && from <= to) void onPeriodChange()
})

onMounted(() => {
  void reload()
})
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 1.25rem 1.25rem 1rem; border-bottom: 1px solid #f3f4f6; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.filters { padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #667085; background: transparent; }
.icon-btn:hover:not(:disabled) { background: #f9fafb; color: #344054; }
.icon-btn:disabled { opacity: 0.55; cursor: not-allowed; }

.period-toggle { display: inline-grid; grid-template-columns: repeat(2, minmax(5rem, 1fr)); gap: 0.15rem; height: 2.5rem; border-radius: 0.6rem; background: #f2f4f7; padding: 0.15rem; }
.period-toggle.cols-3 { grid-template-columns: repeat(3, minmax(5rem, 1fr)); }
.period-btn { display: inline-flex; height: 100%; align-items: center; justify-content: center; border-radius: 0.45rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 600; color: #667085; white-space: nowrap; cursor: pointer; }
.period-btn:hover:not(.active) { color: #344054; background: rgb(255 255 255 / 60%); }
.period-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.12); }

.kassa-card { position: relative; display: flex; flex-direction: column; border-radius: 1rem; border: 1px solid #e4e7ec; background: #fff; padding: 1rem; text-align: start; }
.kassa-card.negative .kassa-amount { color: #d92d20; }
.pool-split { margin-top: 0.375rem; font-size: 0.6875rem; color: #667085; }
.kassa-icon { display: flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.tone-cash .kassa-icon { background: #ecfdf3; color: #039855; }
.tone-card .kassa-icon { background: #f4f3ff; color: #7a5af8; }
.tone-bank .kassa-icon { background: #eff8ff; color: #1570ef; }
.tone-online .kassa-icon { background: #f0f9ff; color: #0086c9; }
.tone-transfer .kassa-icon { background: #fff6ed; color: #ef6820; }
.tone-wallet .kassa-icon { background: #fffaeb; color: #dc6803; }
.kassa-amount { margin-top: 0.125rem; font-size: 1.25rem; font-weight: 600; line-height: 1.75rem; color: #101828; font-variant-numeric: tabular-nums; }
.kassa-card.total-uzs { background: linear-gradient(135deg, #f6fef9 0%, #fff 70%); border-color: #d1fadf; }
.kassa-card.total-usd { background: linear-gradient(135deg, #f5faff 0%, #fff 70%); border-color: #d1e9ff; }
.total-uzs .kassa-icon { background: #d1fadf; color: #027a48; }
.total-usd .kassa-icon { background: #d1e9ff; color: #175cd3; }
.kassa-count { border-radius: 9999px; background: #f2f4f7; padding: 0.0625rem 0.5rem; font-size: 0.6875rem; font-weight: 500; color: #667085; }
.cur-badge { margin-inline-start: 0.375rem; border-radius: 0.25rem; padding: 0.0625rem 0.375rem; font-size: 0.6875rem; font-weight: 500; }
.cur-badge.uzs { background: #f2f4f7; color: #667085; }
.cur-badge.usd { background: #eff8ff; color: #175cd3; }

.dark .card { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .head, .dark .filters { border-color: #1f2937; }
.dark .title { color: rgba(255, 255, 255, 0.9); }
.dark .sub, .dark .lbl { color: #98a2b3; }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .td { color: #d1d5db; }
.dark .icon-btn { border-color: #344054; color: #98a2b3; }
.dark .period-toggle { background: rgb(255 255 255 / 5%); }
.dark .period-btn { color: #98a2b3; }
.dark .period-btn.active { background: #1d2939; color: #9cb0ff; }
.dark .kassa-card { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .kassa-amount { color: rgba(255, 255, 255, 0.92); }
.dark .kassa-card.negative .kassa-amount { color: #f97066; }
.dark .pool-split { color: #98a2b3; }
.dark .kassa-card.total-uzs { background: linear-gradient(135deg, rgb(18 183 106 / 10%) 0%, rgb(255 255 255 / 3%) 70%); border-color: rgb(18 183 106 / 25%); }
.dark .kassa-card.total-usd { background: linear-gradient(135deg, rgb(46 144 250 / 10%) 0%, rgb(255 255 255 / 3%) 70%); border-color: rgb(46 144 250 / 25%); }
.dark .kassa-count { background: rgb(255 255 255 / 6%); color: #98a2b3; }
.dark .cur-badge.uzs { background: rgb(255 255 255 / 5%); color: #98a2b3; }
.dark .cur-badge.usd { background: rgb(46 144 250 / 12%); color: #84caff; }
</style>
