<template>
  <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
    <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 lg:flex-row lg:items-center lg:justify-between dark:border-gray-800">
      <div>
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('expenses.importTitle') }}</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('expenses.importSubtitle') }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <button type="button" class="icon-btn" :title="t('expenses.refresh')" :disabled="loading" @click="load">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
        <button type="button" class="btn btn-with-icon" :disabled="writeBlocked" @click="openCreate">
          <Plus class="h-4 w-4" />
          {{ t('expenses.importCreate') }}
        </button>
      </div>
    </div>

    <div class="filters border-b border-gray-100 px-5 py-4 dark:border-gray-800">
      <div class="filter-row">
        <label class="lbl-block filter-date">
          {{ t('common.from') }}
          <input v-model="filterFrom" type="date" class="field field-plain" :max="filterTo || undefined" @change="load" />
        </label>
        <label class="lbl-block filter-date">
          {{ t('common.to') }}
          <input v-model="filterTo" type="date" class="field field-plain" :min="filterFrom || undefined" @change="load" />
        </label>
        <div class="filter-order">
          <span class="lbl-block-label">{{ t('expenses.importOrder') }}</span>
          <SearchableSelect
            v-model="filterOrderId"
            :options="filterOrderOptions"
            :placeholder="t('expenses.importAllOrders')"
            :search-placeholder="t('expenses.importOrderSearch')"
          />
        </div>
        <div>
          <span class="lbl-block-label">{{ t('expenses.importKind') }}</span>
          <div class="seg" role="radiogroup" :aria-label="t('expenses.importKind')">
            <button
              v-for="k in KIND_FILTERS"
              :key="k"
              type="button"
              role="radio"
              class="seg-btn"
              :class="{ active: filterKind === k }"
              :aria-checked="filterKind === k"
              @click="filterKind = k"
            >
              {{ kindLabel(k) }}
            </button>
          </div>
        </div>
      </div>
      <p class="mt-2 text-sm text-gray-500 dark:text-gray-400">{{ t('expenses.showing', { n: rows.length }) }}</p>
    </div>

    <div v-if="kindTotals.length" class="flex flex-wrap gap-2 border-b border-gray-100 px-5 py-3 dark:border-gray-800">
      <span v-for="tot in kindTotals" :key="tot.key" class="total-chip" :class="tot.kind === 'ROAD' ? 'road' : 'duty'">
        {{ kindLabel(tot.kind) }}:
        <b>{{ moneyIn(tot.amount, tot.currency, t('common.currency')) }}</b>
      </span>
    </div>

    <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
    <div class="overflow-x-auto">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">{{ t('common.date') }}</th>
            <th class="th">{{ t('expenses.importOrder') }}</th>
            <th class="th">{{ t('expenses.importAllocated') }}</th>
            <th class="th">{{ t('expenses.importKind') }}</th>
            <th class="th text-right">{{ t('common.amount') }}</th>
            <th class="th">{{ t('expenses.cashbox') }}</th>
            <th class="th">{{ t('common.note') }}</th>
            <th class="th text-right">{{ t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="8" class="empty">{{ t('common.loading') }}</td></tr>
          <tr v-else-if="rows.length === 0"><td colspan="8" class="empty">{{ t('expenses.importEmpty') }}</td></tr>
          <tr v-for="row in rows" :key="row.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td whitespace-nowrap">{{ formatDay(row.expenseDate) }}</td>
            <td class="td">
              <span class="font-medium text-gray-800 dark:text-white/90">{{ orderLine(row) }}</span>
              <span v-if="row.orderArrivalDate" class="mt-0.5 block text-xs text-gray-500">{{ formatDay(row.orderArrivalDate) }}</span>
            </td>
            <td class="td">
              <span class="block whitespace-nowrap">
                {{ moneyIn(row.allocatedAmount || 0, row.currency, t('common.currency')) }}
                <span class="text-gray-400"> / {{ moneyIn(groupTotal(row), row.currency, t('common.currency')) }}</span>
              </span>
              <span class="mt-0.5 block text-xs" :class="splitState(row).className">{{ splitState(row).label }}</span>
            </td>
            <td class="td">
              <span class="kind-badge" :class="row.kind === 'ROAD' ? 'road' : 'duty'">{{ kindLabel(row.kind) }}</span>
            </td>
            <td class="td text-right whitespace-nowrap font-medium">
              {{ moneyIn(row.amount, row.currency, t('common.currency')) }}
            </td>
            <td class="td">{{ row.paymentTypeName || '—' }}</td>
            <td class="td">{{ row.description || '—' }}</td>
            <td class="td text-right">
              <div class="inline-flex items-center justify-end gap-2">
                <button type="button" class="link-btn" :disabled="writeBlocked" @click="openSplit(row)">
                  {{ t('expenses.importSplit') }}
                </button>
                <RowActions :edit="!writeBlocked" :remove="!writeBlocked" @edit="openEdit(row)" @delete="onDelete(row)" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="modal" class="overlay">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="import-expense-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge expense"><Ship class="h-5 w-5" /></span>
            <div>
              <h3 id="import-expense-title" class="title">
                {{ editingId ? t('expenses.importEdit') : t('expenses.importCreate') }}
              </h3>
              <p class="sub">{{ t('expenses.importModalSubtitle') }}</p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="modal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <span class="lbl">{{ t('expenses.importOrder') }} <span class="req">*</span></span>
            <SearchableSelect
              v-model="form.orderId"
              :options="orderOptions"
              :placeholder="t('expenses.importOrderPlaceholder')"
              :search-placeholder="t('expenses.importOrderSearch')"
            />
          </div>

          <div>
            <span class="lbl">{{ t('expenses.importKind') }} <span class="req">*</span></span>
            <div class="cur-switch" role="radiogroup" :aria-label="t('expenses.importKind')">
              <button
                type="button"
                role="radio"
                class="cur-switch-btn"
                :class="{ active: form.kind === 'ROAD' }"
                :aria-checked="form.kind === 'ROAD'"
                @click="form.kind = 'ROAD'"
              >
                {{ t('expenses.importRoad') }}
              </button>
              <button
                type="button"
                role="radio"
                class="cur-switch-btn"
                :class="{ active: form.kind === 'DUTY' }"
                :aria-checked="form.kind === 'DUTY'"
                @click="form.kind = 'DUTY'"
              >
                {{ t('expenses.importDuty') }}
              </button>
            </div>
          </div>

          <div>
            <span class="lbl">{{ t('expenses.currencyLabel') }} <span class="req">*</span></span>
            <div class="cur-switch" role="radiogroup" :aria-label="t('expenses.currencyLabel')">
              <button
                v-for="c in CURRENCIES"
                :key="c"
                type="button"
                role="radio"
                class="cur-switch-btn"
                :class="{ active: form.currency === c }"
                :aria-checked="form.currency === c"
                @click="setCurrency(c)"
              >
                <component :is="c === 'USD' ? DollarSign : Coins" class="h-4 w-4" />
                {{ c === 'USD' ? t('expenses.usd') : t('expenses.uzs') }}
              </button>
            </div>
          </div>

          <div v-if="auth.isElevated">
            <span class="lbl">{{ t('expenses.cashSource') }} <span class="req">*</span></span>
            <div class="cur-switch" role="radiogroup">
              <button
                type="button"
                class="cur-switch-btn"
                :class="{ active: form.cashSource === 'OPERATIONAL' }"
                @click="setCashSource('OPERATIONAL')"
              >
                {{ t('expenses.operational') }}
              </button>
              <button
                type="button"
                class="cur-switch-btn"
                :class="{ active: form.cashSource === 'VAULT' }"
                @click="setCashSource('VAULT')"
              >
                {{ t('expenses.vault') }}
              </button>
            </div>
          </div>

          <div>
            <span class="lbl">{{ t('expenses.cashbox') }} <span class="req">*</span></span>
            <p v-if="cashOptions.length === 0" class="hint-box">{{ t('expenses.noCashbox') }}</p>
            <div v-else class="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <button
                v-for="c in cashOptions"
                :key="c.paymentTypeId"
                type="button"
                class="cash-card"
                :class="{ active: form.paymentTypeId === c.paymentTypeId, empty: c.available <= 0 }"
                @click="form.paymentTypeId = c.paymentTypeId"
              >
                <span class="min-w-0 text-start">
                  <span class="block truncate text-sm font-medium">{{ c.paymentTypeName }}</span>
                  <span class="cash-balance">{{ t('expenses.available', { value: cashMoney(c.available) }) }}</span>
                </span>
              </button>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <label for="import-amount" class="lbl">{{ t('common.amount') }} <span class="req">*</span></label>
              <div class="relative">
                <input
                  id="import-amount"
                  v-model="amountText"
                  type="text"
                  inputmode="decimal"
                  required
                  class="field"
                  :class="{ invalid: exceedsCash }"
                  :placeholder="t('expenses.amountPlaceholder')"
                  @input="onAmountInput"
                />
              </div>
              <p v-if="exceedsCash" class="field-err">{{ t('expenses.exceeds', { value: cashMoney(selectedAvailable) }) }}</p>
              <p v-else-if="selectedCash && Number(form.amount || 0) > 0" class="field-after">
                {{ t('expenses.afterExpense', { value: cashMoney(selectedAvailable - Number(form.amount || 0)) }) }}
              </p>
            </div>
            <div>
              <label for="import-date" class="lbl">{{ t('common.date') }} <span class="req">*</span></label>
              <input id="import-date" v-model="form.expenseDate" type="date" required class="field" />
            </div>
          </div>

          <div>
            <label for="import-note" class="lbl">{{ t('common.note') }}</label>
            <input id="import-note" v-model="form.description" maxlength="500" class="field" :placeholder="t('expenses.notePlaceholder')" />
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="modal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving || !canSubmit">
              <Check class="h-4 w-4" />
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="splitModal" class="overlay">
      <div class="modal modal-wide" role="dialog" aria-modal="true" aria-labelledby="import-split-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div>
            <h3 id="import-split-title" class="title">{{ t('expenses.importSplitTitle') }}</h3>
            <p class="sub">{{ splitHead }}</p>
            <p v-if="splitPaymentCount > 1" class="sub">{{ t('expenses.importPaymentCount', { n: splitPaymentCount }) }}</p>
            <p class="sub">{{ t('expenses.importSplitHint') }}</p>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="splitModal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div class="split-stats">
          <div class="stat-card pay">
            <span class="stat-label">{{ t('expenses.importPayments') }}</span>
            <span class="stat-value">{{ splitMoney(splitTotal) }}</span>
          </div>
          <div class="stat-card done">
            <span class="stat-label">{{ t('expenses.importAllocated') }}</span>
            <span class="stat-value">{{ splitMoney(splitUsed) }}</span>
          </div>
          <div class="stat-card left" :class="{ over: splitOver, clear: !splitOver && splitLeft < 0.01 }">
            <span class="stat-label">{{ t('expenses.importNotAllocated') }}</span>
            <span class="stat-value">{{ splitMoney(splitOver ? splitOverBy : Math.max(0, splitLeft)) }}</span>
          </div>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <div class="mb-3 flex justify-end">
          <button type="button" class="link-btn" :disabled="splitLines.length === 0" @click="spreadByQty">
            {{ t('expenses.importSpread') }}
          </button>
        </div>
        <p v-if="splitLoading" class="hint-box">{{ t('common.loading') }}</p>
        <p v-else-if="splitLines.length === 0" class="hint-box">{{ t('expenses.importNoPositions') }}</p>
        <div v-else class="split-list">
          <div v-for="(line, i) in splitLines" :key="line.itemId" class="split-row">
            <span class="min-w-0">
              <span class="block truncate text-sm font-medium text-gray-800 dark:text-white/90">{{ i + 1 }}. {{ line.label }}</span>
            </span>
            <input
              :value="line.amountText"
              type="text"
              inputmode="decimal"
              class="field split-amount"
              @input="onSplitAmountInput(line, $event)"
            />
          </div>
        </div>
        <p v-if="splitOver" class="field-err">{{ t('expenses.importOver', { value: splitMoney(splitOverBy) }) }}</p>
        <div class="mt-4 flex justify-end gap-2">
          <button type="button" class="ghost" @click="splitModal = false">{{ t('common.cancel') }}</button>
          <button type="button" class="btn" :disabled="splitSaving || splitOver || splitLoading" @click="onSplitSave">
            {{ splitSaving ? t('common.saving') : t('common.save') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Coins, DollarSign, Plus, RefreshCw, Ship, X } from 'lucide-vue-next'
import RowActions from '@/components/crm/RowActions.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import { fetchCashAvailable, type CashAvailable, type CashSource } from '@/api/expenses'
import {
  createImportExpense,
  deleteImportExpense,
  fetchImportExpenseOrders,
  fetchImportExpenseSplit,
  fetchImportExpenses,
  saveImportExpenseSplit,
  updateImportExpense,
  type ImportExpense,
  type ImportExpenseKind,
  type ImportExpenseOrderOption,
} from '@/api/importExpenses'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useFilialScope } from '@/composables/useFilialScope'
import { CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'
import { amountToText, formatAmountInput, formatDateOnly } from '@/utils/format'

const emit = defineEmits<{ changed: [] }>()

const { t } = useI18n()
const auth = useAuthStore()
const { writeBlocked } = useFilialScope()

const KIND_FILTERS = ['ALL', 'ROAD', 'DUTY'] as const
type KindFilter = (typeof KIND_FILTERS)[number]

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const rows = ref<ImportExpense[]>([])
const orders = ref<ImportExpenseOrderOption[]>([])
const cash = ref<CashAvailable[]>([])
const splitModal = ref(false)
const splitLoading = ref(false)
const splitSaving = ref(false)
const splitId = ref<number | null>(null)
const splitTotal = ref(0)
const splitPaymentCount = ref(1)
const splitCurrency = ref<CurrencyCode>('UZS')
const splitHead = ref('')
const splitLines = ref<{ itemId: number; label: string; qty: number; amount: number; amountText: string }[]>([])
const amountText = ref('')
const filterFrom = ref(monthStart())
const filterTo = ref(today())
const filterOrderId = ref(0)
const filterKind = ref<KindFilter>('ALL')

const form = reactive({
  orderId: 0,
  kind: 'ROAD' as ImportExpenseKind,
  amount: null as number | null,
  expenseDate: today(),
  description: '',
  currency: 'UZS' as CurrencyCode,
  paymentTypeId: 0,
  cashSource: 'OPERATIONAL' as CashSource,
})

const editingSpend = ref<{ paymentTypeId: number; currency: CurrencyCode; amount: number; cashSource: CashSource } | null>(null)

function today() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
function monthStart() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}
function formatDay(iso?: string | null) {
  if (!iso) return ''
  const v = formatDateOnly(iso)
  return v === '—' ? '' : v
}
function trimNum(v: number | null | undefined) {
  if (v == null || !Number.isFinite(Number(v))) return ''
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

type PositionBits = {
  positionGoodsName?: string | null
  positionWidth?: number | null
  positionHeight?: number | null
  positionPieces?: number | null
  positionQty?: number | null
}

function positionText(row: PositionBits) {
  const parts: string[] = []
  if (row.positionGoodsName) parts.push(row.positionGoodsName)
  const sized = Number(row.positionWidth) > 0 && Number(row.positionHeight) > 0
  if (sized) parts.push(`${trimNum(row.positionWidth)}×${trimNum(row.positionHeight)} sm`)
  if (row.positionPieces != null) parts.push(t('expenses.importPieces', { n: trimNum(row.positionPieces) }))
  if (row.positionQty != null) {
    parts.push(sized ? t('expenses.importArea', { n: trimNum(row.positionQty) }) : trimNum(row.positionQty))
  }
  return parts.join(' · ') || '—'
}

function orderLine(row: { warehouseOrderId: number; supplierName?: string | null }) {
  return t('expenses.importOrderLine', { id: row.warehouseOrderId, supplier: row.supplierName || '—' })
}

function orderOptionLabel(o: ImportExpenseOrderOption) {
  const date = formatDay(o.arrivalDate)
  const base = t('expenses.importOrderLine', { id: o.id, supplier: o.supplierName || '—' })
  return date ? `${base} · ${date}` : base
}

function kindLabel(kind: KindFilter | ImportExpenseKind) {
  if (kind === 'ROAD') return t('expenses.importRoad')
  if (kind === 'DUTY') return t('expenses.importDuty')
  return t('expenses.importAllKinds')
}

const filterOrderOptions = computed(() => [
  { value: 0, label: t('expenses.importAllOrders') },
  ...orders.value.map((o) => ({ value: o.id, label: orderOptionLabel(o) })),
])
const orderOptions = computed(() => orders.value.map((o) => ({ value: o.id, label: orderOptionLabel(o) })))

function round2(v: number) {
  return Math.round(v * 100) / 100
}

function groupTotal(row: ImportExpense) {
  return Number(row.groupAmount != null ? row.groupAmount : row.amount || 0)
}

function splitState(row: ImportExpense) {
  const total = groupTotal(row)
  const used = Number(row.allocatedAmount || 0)
  const left = round2(total - used)
  const count = Number(row.paymentCount || 1)
  const suffix = count > 1 ? ` · ${t('expenses.importPaymentCount', { n: count })}` : ''
  if (used <= 0) return { label: t('expenses.importNotAllocated') + suffix, className: 'text-gray-500' }
  if (Math.abs(left) < 0.01) return { label: t('expenses.importDone') + suffix, className: 'text-emerald-600' }
  if (left < 0) return { label: t('expenses.importOver', { value: moneyIn(Math.abs(left), row.currency, t('common.currency')) }) + suffix, className: 'text-error-500' }
  return { label: t('expenses.importRemaining', { value: moneyIn(left, row.currency, t('common.currency')) }) + suffix, className: 'text-amber-600' }
}

const kindTotals = computed(() => {
  const map = new Map<string, { key: string; kind: ImportExpenseKind; currency: CurrencyCode; amount: number }>()
  for (const row of rows.value) {
    const currency = row.currency || 'UZS'
    const key = `${row.kind}:${currency}`
    const cur = map.get(key) || { key, kind: row.kind, currency, amount: 0 }
    cur.amount += Number(row.amount || 0)
    map.set(key, cur)
  }
  return [...map.values()]
})

function poolAmount(c: CashAvailable, source: CashSource = form.cashSource) {
  if (source === 'VAULT') return Number(c.vaultAvailable ?? 0)
  return Number(c.operationalAvailable ?? c.available ?? 0)
}

function availableFor(c: CashAvailable) {
  const spend = editingSpend.value
  const returned =
    spend &&
    spend.paymentTypeId === c.paymentTypeId &&
    spend.currency === c.currency &&
    spend.cashSource === form.cashSource
      ? spend.amount
      : 0
  return poolAmount(c) + returned
}

const cashOptions = computed(() =>
  cash.value
    .filter((c) => c.currency === form.currency)
    .map((c) => ({ ...c, available: availableFor(c) })),
)
const selectedCash = computed(() => cashOptions.value.find((c) => c.paymentTypeId === form.paymentTypeId) || null)
const selectedAvailable = computed(() => selectedCash.value?.available ?? 0)
const exceedsCash = computed(() => !!selectedCash.value && Number(form.amount || 0) > selectedAvailable.value + 1e-9)
const canSubmit = computed(
  () =>
    form.orderId > 0 &&
    !!form.kind &&
    !!selectedCash.value &&
    Number(form.amount || 0) > 0 &&
    !exceedsCash.value &&
    !!form.expenseDate,
)

const splitUsed = computed(() => round2(splitLines.value.reduce((s, l) => s + (Number(l.amount) || 0), 0)))
const splitLeft = computed(() => round2(splitTotal.value - splitUsed.value))
const splitOver = computed(() => splitLeft.value < -0.009)
const splitOverBy = computed(() => round2(splitUsed.value - splitTotal.value))
function splitMoney(v: number) {
  return moneyIn(v, splitCurrency.value, t('common.currency'))
}

function cashMoney(v: number) {
  return moneyIn(v, form.currency, t('common.currency'))
}

function setAmount(v: number | null) {
  const n = Number(v || 0)
  form.amount = n > 0 ? n : null
  amountText.value = n > 0 ? amountToText(n) : ''
}

function onAmountInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  amountText.value = text
  el.value = text
  form.amount = value > 0 ? value : null
}

function onSplitAmountInput(line: { amount: number; amountText: string }, e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  line.amountText = text
  el.value = text
  line.amount = value
}

function pickCash() {
  if (selectedCash.value) return
  const best = [...cashOptions.value].sort((a, b) => b.available - a.available)[0]
  form.paymentTypeId = best?.paymentTypeId || 0
}

function setCurrency(c: CurrencyCode) {
  form.currency = c
  form.paymentTypeId = 0
  pickCash()
}

function setCashSource(source: CashSource) {
  form.cashSource = source
  if (!cashOptions.value.some((c) => c.paymentTypeId === form.paymentTypeId && c.available > 0)) {
    form.paymentTypeId = 0
    pickCash()
  }
}

async function loadCash() {
  try {
    const res = await fetchCashAvailable()
    cash.value = (res.data || []).map((c) => ({
      ...c,
      available: Number(c.available || 0),
      operationalAvailable: Number(c.operationalAvailable ?? c.available ?? 0),
      vaultAvailable: Number(c.vaultAvailable ?? 0),
    }))
  } catch {
    cash.value = []
  }
}

async function loadOrders() {
  try {
    const res = await fetchImportExpenseOrders()
    orders.value = res.data || []
  } catch {
    orders.value = []
  }
}

async function load() {
  if (!filterFrom.value || !filterTo.value) return
  loading.value = true
  error.value = null
  try {
    const res = await fetchImportExpenses({
      startDate: filterFrom.value,
      endDate: filterTo.value,
      warehouseOrderId: filterOrderId.value > 0 ? filterOrderId.value : undefined,
      kind: filterKind.value === 'ALL' ? undefined : filterKind.value,
    })
    rows.value = res.data || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.loadError')
    rows.value = []
  } finally {
    loading.value = false
  }
}

watch([filterOrderId, filterKind], () => {
  void load()
})

function resetForm() {
  editingId.value = null
  editingSpend.value = null
  form.orderId = 0
  form.kind = 'ROAD'
  setAmount(null)
  form.expenseDate = today()
  form.description = ''
  form.currency = 'UZS'
  form.paymentTypeId = 0
  form.cashSource = auth.isElevated ? 'VAULT' : 'OPERATIONAL'
  formError.value = null
}

async function openCreate() {
  resetForm()
  modal.value = true
  await loadCash()
  pickCash()
}

async function openEdit(row: ImportExpense) {
  resetForm()
  editingId.value = row.id
  const currency = row.currency || 'UZS'
  const source: CashSource = row.cashSource || 'OPERATIONAL'
  editingSpend.value = row.paymentTypeId
    ? { paymentTypeId: row.paymentTypeId, currency, amount: Number(row.amount), cashSource: source }
    : null
  form.kind = row.kind
  setAmount(Number(row.amount))
  form.expenseDate = row.expenseDate
  form.description = row.description || ''
  form.currency = currency
  form.paymentTypeId = row.paymentTypeId || 0
  form.cashSource = auth.isElevated ? source : 'OPERATIONAL'
  formError.value = null
  modal.value = true
  form.orderId = row.warehouseOrderId
  await loadCash()
}

async function onSubmit() {
  if (!canSubmit.value) return
  saving.value = true
  formError.value = null
  try {
    const payload = {
      warehouseOrderId: form.orderId,
      kind: form.kind,
      amount: Number(form.amount),
      expenseDate: form.expenseDate,
      description: form.description.trim() || undefined,
      paymentTypeId: form.paymentTypeId,
      cashSource: form.cashSource,
    }
    if (editingId.value) await updateImportExpense(editingId.value, payload)
    else await createImportExpense(payload)
    modal.value = false
    await load()
    emit('changed')
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.saveError')
    await loadCash()
  } finally {
    saving.value = false
  }
}

async function openSplit(row: ImportExpense) {
  splitId.value = row.id
  splitTotal.value = groupTotal(row)
  splitPaymentCount.value = Number(row.paymentCount || 1)
  splitCurrency.value = row.currency || 'UZS'
  splitHead.value = `${orderLine(row)} · ${kindLabel(row.kind)}`
  splitLines.value = []
  formError.value = null
  splitModal.value = true
  splitLoading.value = true
  try {
    const res = await fetchImportExpenseSplit(row.id)
    const data = res.data
    if (data) {
      splitTotal.value = Number(data.amount || 0)
      splitPaymentCount.value = Number(data.paymentCount || 1)
      splitCurrency.value = data.currency || splitCurrency.value
    }
    splitLines.value = (data?.lines || []).map((line) => ({
      itemId: line.itemId,
      qty: Number(line.qty || 0),
      amount: Number(line.amount || 0),
      amountText: Number(line.amount) > 0 ? amountToText(Number(line.amount)) : '',
      label: positionText({
        positionGoodsName: line.goodsName,
        positionWidth: line.width,
        positionHeight: line.height,
        positionPieces: line.pieces,
        positionQty: line.qty,
      }),
    }))
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.loadError')
  } finally {
    splitLoading.value = false
  }
}

function spreadByQty() {
  const lines = splitLines.value
  if (!lines.length) return
  const weights = lines.map((l) => Math.max(0, Number(l.qty) || 0))
  const totalW = weights.reduce((s, w) => s + w, 0)
  const total = splitTotal.value
  let used = 0
  lines.forEach((line, i) => {
    const last = i === lines.length - 1
    if (last) {
      const rest = round2(total - used)
      line.amount = rest
      line.amountText = rest > 0 ? amountToText(rest) : ''
      return
    }
    const share = totalW > 0 ? round2((total * weights[i]) / totalW) : round2(total / lines.length)
    line.amount = share
    line.amountText = share > 0 ? amountToText(share) : ''
    used = round2(used + share)
  })
}

async function onSplitSave() {
  if (splitId.value == null || splitOver.value) return
  splitSaving.value = true
  formError.value = null
  try {
    await saveImportExpenseSplit(
      splitId.value,
      splitLines.value.map((l) => ({ warehouseOrderItemId: l.itemId, amount: Number(l.amount) || 0 })),
    )
    splitModal.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.saveError')
  } finally {
    splitSaving.value = false
  }
}

async function onDelete(row: ImportExpense) {
  if (!confirm(t('expenses.importDeleteConfirm', { id: row.id }))) return
  try {
    await deleteImportExpense(row.id)
    await load()
    emit('changed')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.deleteError')
  }
}

onMounted(() => {
  void loadOrders()
  void load()
})
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.field:focus { outline: none; border-color: #9cb0ff; box-shadow: 0 0 0 4px rgb(70 95 255 / 10%); }
.field.invalid { border-color: #f04438; }
.field-err { margin-top: 0.375rem; font-size: 0.75rem; color: #d92d20; }
.field-after { margin-top: 0.375rem; font-size: 0.75rem; color: #667085; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { cursor: not-allowed; opacity: 0.6; }
.btn-with-icon { gap: 0.4rem; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.125rem; font-size: 0.875rem; color: #6b7280; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.4); padding: 1rem; }
.modal { width: 100%; max-width: 40rem; max-height: calc(100vh - 2rem); overflow-y: auto; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.modal-badge { display: flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.modal-badge.expense { background: #fff6ed; color: #ef6820; }
.close-btn { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #9ca3af; }
.close-btn:hover { background: #f3f4f6; color: #374151; }
.seg { display: inline-flex; gap: 0.125rem; border-radius: 0.5rem; background: #f2f4f7; padding: 0.1875rem; }
.seg-btn { border-radius: 0.375rem; padding: 0.25rem 0.75rem; font-size: 0.8125rem; font-weight: 500; color: #667085; }
.seg-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08); }
.cur-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 0.25rem; border-radius: 0.625rem; background: #f2f4f7; padding: 0.25rem; }
.cur-switch-btn { display: inline-flex; height: 2.25rem; align-items: center; justify-content: center; gap: 0.5rem; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 500; color: #667085; }
.cur-switch-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.1); }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #667085; background: transparent; }
.icon-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.filters { background: #fcfcfd; }
.filter-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem 1rem; }
.lbl-block { display: flex; flex-direction: column; gap: 0.375rem; font-size: 0.8125rem; font-weight: 500; color: #374151; }
.lbl-block-label { display: block; margin-bottom: 0.375rem; font-size: 0.8125rem; font-weight: 500; color: #374151; }
.filter-date { width: 9.5rem; }
.filter-order { min-width: 16rem; flex: 1 1 16rem; }
.field-plain { height: 2.5rem; }
.hint-box { border-radius: 0.5rem; border: 1px dashed #d0d5dd; padding: 0.75rem; text-align: center; font-size: 0.8125rem; color: #667085; }
.cash-card { display: flex; width: 100%; align-items: center; gap: 0.625rem; border-radius: 0.625rem; border: 1px solid #e4e7ec; padding: 0.625rem 0.75rem; color: #344054; text-align: start; }
.cash-card.active { border-color: #465fff; box-shadow: 0 0 0 3px rgb(70 95 255 / 12%); }
.cash-balance { display: block; font-size: 0.75rem; color: #039855; }
.cash-card.empty .cash-balance { color: #d92d20; }
.total-chip { display: inline-flex; align-items: center; gap: 0.375rem; border-radius: 9999px; padding: 0.25rem 0.75rem; font-size: 0.8125rem; }
.total-chip.road { background: #fff6ed; color: #b54708; }
.total-chip.duty { background: #eff8ff; color: #175cd3; }
.kind-badge { border-radius: 9999px; padding: 0.125rem 0.5rem; font-size: 0.75rem; font-weight: 500; }
.kind-badge.road { background: #fff6ed; color: #b54708; }
.kind-badge.duty { background: #eff8ff; color: #175cd3; }
.link-btn { font-size: 0.75rem; font-weight: 600; color: #465fff; }
.link-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.modal-wide { max-width: 76rem; width: min(96vw, 76rem); }
.split-stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.75rem; margin-bottom: 1rem; }
.stat-card { display: flex; flex-direction: column; gap: 0.25rem; border-radius: 0.875rem; border: 1px solid #e4e7ec; padding: 0.875rem 1rem; }
.stat-label { font-size: 0.75rem; font-weight: 600; letter-spacing: 0.01em; }
.stat-value { font-size: 1.25rem; font-weight: 600; line-height: 1.4; font-variant-numeric: tabular-nums; }
.stat-card.pay { background: #f5f8ff; border-color: #d1e0ff; color: #1849a9; }
.stat-card.done { background: #f6fef9; border-color: #abefc6; color: #067647; }
.stat-card.left { background: #fffaeb; border-color: #fedf89; color: #b54708; }
.stat-card.left.clear { background: #f6fef9; border-color: #abefc6; color: #067647; }
.stat-card.left.over { background: #fef3f2; border-color: #fecdca; color: #b42318; }
.split-list { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 1.25rem; max-height: calc(100vh - 22rem); overflow: auto; padding-inline-end: 0.25rem; }
.split-row { display: grid; grid-template-columns: minmax(0, 1fr) 9.5rem; gap: 0.75rem; align-items: center; }
.split-amount { text-align: end; }
@media (max-width: 800px) {
  .split-stats, .split-list { grid-template-columns: 1fr; }
}
.dark .lbl, .dark .lbl-block, .dark .lbl-block-label { color: #98a2b3; }
.dark .title { color: rgba(255, 255, 255, 0.9); }
.dark .sub, .dark .field-after, .dark .hint-box { color: #98a2b3; }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .modal { border-color: #1f2937; background: #101828; }
.dark .ghost { border-color: #344054; color: #d0d5dd; }
.dark .seg, .dark .cur-switch { background: rgb(255 255 255 / 5%); }
.dark .seg-btn, .dark .cur-switch-btn { color: #98a2b3; }
.dark .seg-btn.active, .dark .cur-switch-btn.active { background: #1d2939; color: #9cb0ff; }
.dark .icon-btn { border-color: #344054; color: #98a2b3; }
.dark .filters { background: rgb(255 255 255 / 2%); }
.dark .cash-card { border-color: #344054; color: #e4e7ec; }
.dark .cash-card.active { border-color: #7592ff; }
.dark .hint-box { border-color: #344054; }
.dark .total-chip.road, .dark .kind-badge.road { background: rgb(239 104 32 / 12%); color: #fd853a; }
.dark .total-chip.duty, .dark .kind-badge.duty { background: rgb(46 144 250 / 12%); color: #84caff; }
.dark .close-btn:hover { background: rgb(255 255 255 / 5%); color: rgba(255, 255, 255, 0.8); }
.dark .stat-card.pay { background: rgb(46 144 250 / 12%); border-color: rgb(46 144 250 / 28%); color: #84caff; }
.dark .stat-card.done, .dark .stat-card.left.clear { background: rgb(18 183 106 / 12%); border-color: rgb(18 183 106 / 28%); color: #6ce9a6; }
.dark .stat-card.left { background: rgb(247 144 9 / 12%); border-color: rgb(247 144 9 / 28%); color: #fec84b; }
.dark .stat-card.left.over { background: rgb(240 68 56 / 12%); border-color: rgb(240 68 56 / 28%); color: #fda29b; }
</style>
