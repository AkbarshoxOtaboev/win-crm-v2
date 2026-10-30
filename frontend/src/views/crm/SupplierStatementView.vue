<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="supplier?.name || t('suppliers.statement')" />

    <div class="mb-4">
      <router-link to="/suppliers" class="back">{{ t('suppliers.backToList') }}</router-link>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="loading" class="card empty">{{ t('common.loading') }}</div>
    <div v-else-if="!supplier" class="card empty">{{ t('suppliers.notFound') }}</div>

    <template v-else>
      <div class="card profile mb-4">
        <div class="profile-top">
          <div>
            <h3 class="title">{{ supplier.name }}</h3>
            <p class="sub">
              {{ supplier.phone ? formatUzPhone(supplier.phone) : '—' }}
              <template v-if="supplier.inn"> · {{ t('common.inn') }} {{ supplier.inn }}</template>
              <template v-if="supplier.address"> · {{ supplier.address }}</template>
            </p>
          </div>
          <div class="period-toggle" role="radiogroup" :aria-label="t('common.currency')">
            <button
              v-for="mode in CURRENCY_MODES"
              :key="mode"
              type="button"
              class="period-btn"
              :class="{ active: viewMode === mode }"
              role="radio"
              :aria-checked="viewMode === mode"
              @click="viewMode = mode"
            >
              {{ t(`suppliers.currencyMode.${mode}`) }}
            </button>
          </div>
        </div>
      </div>

      <div class="card summary mb-4">
        <div v-for="row in summaryRows" :key="row.currency" class="summary-block">
          <div v-if="viewMode === 'all'" class="cur-name">{{ t(`suppliers.currencyMode.${row.currency}`) }}</div>
          <div class="buckets">
            <div class="bucket">
              <div class="bucket-title">{{ t('common.sum') }}</div>
              <div class="metrics">
                <span class="metric">
                  <span class="metric-label">{{ t('suppliers.purchase') }}</span>
                  <span class="metric-value">{{ amt(row.sumPurchase, row.currency) }}</span>
                </span>
                <span class="metric">
                  <span class="metric-label">{{ t('suppliers.paid') }}</span>
                  <span class="metric-value paid">{{ amt(row.sumPaid, row.currency) }}</span>
                </span>
                <span class="metric">
                  <span class="metric-label">{{ t('suppliers.debt') }}</span>
                  <span class="metric-value debt">{{ amt(row.sumDebt, row.currency) }}</span>
                </span>
              </div>
            </div>
            <div class="bucket">
              <div class="bucket-title">{{ t('warehouseOrders.cashSum') }}</div>
              <div class="metrics">
                <span class="metric">
                  <span class="metric-label">{{ t('suppliers.purchase') }}</span>
                  <span class="metric-value">{{ amt(row.cashPurchase, row.currency) }}</span>
                </span>
                <span class="metric">
                  <span class="metric-label">{{ t('suppliers.paid') }}</span>
                  <span class="metric-value paid">{{ amt(row.cashPaid, row.currency) }}</span>
                </span>
                <span class="metric">
                  <span class="metric-label">{{ t('suppliers.debt') }}</span>
                  <span class="metric-value debt">{{ amt(row.cashDebt, row.currency) }}</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="tabs" role="tablist">
        <button type="button" role="tab" class="tab" :class="{ on: tab === 'akt' }" :aria-selected="tab === 'akt'" @click="tab = 'akt'">
          {{ t('suppliers.statement') }}
        </button>
        <button type="button" role="tab" class="tab" :class="{ on: tab === 'inbounds' }" :aria-selected="tab === 'inbounds'" @click="tab = 'inbounds'">
          {{ t('suppliers.inboundsHistory') }}
        </button>
        <button type="button" role="tab" class="tab" :class="{ on: tab === 'payments' }" :aria-selected="tab === 'payments'" @click="tab = 'payments'">
          {{ t('suppliers.paymentsHistory') }}
        </button>
      </div>

      <div v-show="tab === 'akt'" class="card">
        <div class="toolbar">
          <div class="period-toggle" role="radiogroup" :aria-label="t('common.period')">
            <button
              v-for="mode in PERIODS"
              :key="mode"
              type="button"
              class="period-btn"
              :class="{ active: aktMode === mode }"
              role="radio"
              :aria-checked="aktMode === mode"
              @click="setAktPeriod(mode)"
            >
              {{ t(`suppliers.period.${mode}`) }}
            </button>
          </div>
          <input v-if="aktMode === 'day'" v-model="aktDay" type="date" class="field sm:w-40" />
          <input v-else-if="aktMode === 'month'" v-model="aktMonth" type="month" class="field sm:w-40" />
          <input v-else v-model.number="aktYear" type="number" min="2000" max="2100" class="field year-field" />
          <label class="range-field">
            <span>{{ t('common.from') }}</span>
            <input v-model="aktFrom" type="date" class="field" />
          </label>
          <label class="range-field">
            <span>{{ t('common.to') }}</span>
            <input v-model="aktTo" type="date" class="field" />
          </label>
          <select v-model.number="aktPageSize" class="field pager-size">
            <option v-for="n in PAGE_SIZES" :key="n" :value="n">{{ t('common.perPageN', { n }) }}</option>
          </select>
          <button type="button" class="excel-btn" :disabled="aktRows.length === 0" @click="downloadAktExcel">
            {{ t('suppliers.excel') }}
          </button>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100">
                <th class="th">{{ t('common.date') }}</th>
                <th class="th">{{ t('suppliers.doc') }}</th>
                <th class="th">{{ t('suppliers.paymentType') }}</th>
                <th class="th">{{ t('common.sum') }}</th>
                <th class="th">{{ t('warehouseOrders.cashTotal') }}</th>
                <th class="th">{{ t('common.total') }}</th>
                <th class="th">{{ t('suppliers.credit') }}</th>
                <th class="th">{{ t('suppliers.balance') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="aktRows.length === 0"><td colspan="8" class="empty">{{ t('suppliers.emptyAkt') }}</td></tr>
              <tr v-for="row in aktPaged" :key="row.key" class="border-b border-gray-100">
                <td class="td">{{ formatDate(row.date) }}</td>
                <td class="td">
                  <router-link v-if="row.orderId" class="link" :to="`/warehouse-orders?open=${row.orderId}`">
                    {{ row.doc }}
                  </router-link>
                  <span v-else>{{ row.doc }}</span>
                </td>
                <td class="td">{{ row.paymentType || '—' }}</td>
                <td class="td">{{ row.sum ? amt(row.sum, row.currency) : '—' }}</td>
                <td class="td">{{ row.cash ? amt(row.cash, row.currency) : '—' }}</td>
                <td class="td">{{ row.total ? amt(row.total, row.currency) : '—' }}</td>
                <td class="td">{{ row.paid ? amt(row.paid, row.currency) : '—' }}</td>
                <td class="td font-medium">{{ amt(row.balance, row.currency) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="aktRows.length" class="pager">
          <span class="pager-info">{{ aktPage + 1 }} / {{ aktTotalPages }}</span>
          <button type="button" class="ghost" :disabled="aktPage <= 0" @click="aktPage -= 1">{{ t('common.prev') }}</button>
          <button type="button" class="ghost" :disabled="aktPage >= aktTotalPages - 1" @click="aktPage += 1">{{ t('common.next') }}</button>
        </div>
      </div>

      <div v-show="tab === 'inbounds'" class="card">
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100">
                <th class="th">{{ t('common.date') }}</th>
                <th class="th">{{ t('suppliers.doc') }}</th>
                <th class="th">{{ t('common.warehouse') }}</th>
                <th class="th">{{ t('suppliers.products') }}</th>
                <th class="th">{{ t('common.sum') }}</th>
                <th class="th">{{ t('warehouseOrders.cashTotal') }}</th>
                <th class="th">{{ t('common.total') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="currencyOrders.length === 0"><td colspan="7" class="empty">{{ t('suppliers.emptyInbounds') }}</td></tr>
              <tr v-for="block in currencyOrders" :key="block.order.id" class="border-b border-gray-100">
                <td class="td">{{ formatDate(block.order.arrivalDate) }}</td>
                <td class="td">
                  <router-link class="link" :to="`/warehouse-orders?open=${block.order.id}`">
                    {{ t('suppliers.inboundDoc', { id: block.order.id }) }}
                  </router-link>
                </td>
                <td class="td">{{ block.order.warehouseName || '—' }}</td>
                <td class="td">
                  <button type="button" class="view-btn" @click="openItems(block.order.id)">
                    {{ t('suppliers.products') }}
                  </button>
                </td>
                <td class="td">{{ amt(orderSum(block.order), block.order.currency) }}</td>
                <td class="td">{{ orderCash(block.order) ? amt(orderCash(block.order), block.order.currency) : '—' }}</td>
                <td class="td">{{ amt(orderCombined(block.order), block.order.currency) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div v-show="tab === 'payments'" class="card">
        <div class="toolbar">
          <select v-model.number="paymentTypeFilter" class="field type-field">
            <option :value="0">{{ t('suppliers.allPaymentTypes') }}</option>
            <option v-for="pt in paymentTypeOptions" :key="pt.id" :value="pt.id">{{ pt.name }}</option>
          </select>
          <div class="period-toggle" role="radiogroup" :aria-label="t('common.period')">
            <button
              v-for="mode in PERIODS"
              :key="mode"
              type="button"
              class="period-btn"
              :class="{ active: payMode === mode }"
              role="radio"
              :aria-checked="payMode === mode"
              @click="setPayPeriod(mode)"
            >
              {{ t(`suppliers.period.${mode}`) }}
            </button>
          </div>
          <input v-if="payMode === 'day'" v-model="payDay" type="date" class="field sm:w-40" />
          <input v-else-if="payMode === 'month'" v-model="payMonth" type="month" class="field sm:w-40" />
          <input v-else v-model.number="payYear" type="number" min="2000" max="2100" class="field year-field" />
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100">
                <th class="th">{{ t('common.date') }}</th>
                <th class="th">{{ t('suppliers.doc') }}</th>
                <th class="th">{{ t('suppliers.paymentType') }}</th>
                <th class="th">{{ t('suppliers.somColumn') }}</th>
                <th class="th">{{ t('suppliers.rateColumn') }}</th>
                <th class="th">{{ t('suppliers.usdColumn') }}</th>
                <th class="th">{{ t('common.comment') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="filteredPayments.length === 0"><td colspan="7" class="empty">{{ t('suppliers.emptyPayments') }}</td></tr>
              <tr v-for="p in filteredPayments" :key="p.id" class="border-b border-gray-100">
                <td class="td">{{ formatDate(p.paidDate) }}</td>
                <td class="td">{{ t('suppliers.paymentDoc', { id: p.id }) }}</td>
                <td class="td">{{ p.paymentTypeName || '—' }}</td>
                <td class="td">{{ somPaid(p) ? amt(somPaid(p), 'UZS') : '—' }}</td>
                <td class="td">{{ ratePaid(p) ? formatRate(ratePaid(p)) : '—' }}</td>
                <td class="td">{{ usdPaid(p) ? amt(usdPaid(p), 'USD') : '—' }}</td>
                <td class="td">{{ p.comment || '—' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <Teleport to="body">
        <div v-if="itemsOpen" class="overlay" @click.self="closeItems">
          <div class="modal modal-wide" role="dialog" aria-modal="true">
            <div class="modal-head">
              <h3 class="title">{{ t('suppliers.inboundDoc', { id: itemsBlock?.order.id }) }}</h3>
              <button type="button" class="ghost" @click="closeItems">{{ t('common.close') }}</button>
            </div>
            <div class="overflow-x-auto">
              <table class="min-w-full">
                <thead>
                  <tr class="border-b border-gray-100">
                    <th class="th">{{ t('common.product') }}</th>
                    <th class="th">{{ t('common.count') }}</th>
                    <th class="th">{{ t('warehouseOrders.arrivalPrice') }}</th>
                    <th class="th">{{ t('common.sum') }}</th>
                    <th class="th">{{ t('warehouseOrders.cashSum') }}</th>
                    <th class="th">{{ t('warehouseOrders.cashTotal') }}</th>
                    <th class="th">{{ t('warehouseOrders.cashPlusTotal') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="!itemsBlock?.items.length"><td colspan="7" class="empty">{{ t('suppliers.noProducts') }}</td></tr>
                  <tr v-for="item in itemsBlock?.items || []" :key="item.id" class="border-b border-gray-100">
                    <td class="td">{{ item.goodsName || item.goodsId }}</td>
                    <td class="td">{{ formatNum(item.pieceCount ?? item.count) }}</td>
                    <td class="td">{{ amt(item.priceCost, itemsBlock?.order.currency) }}</td>
                    <td class="td">{{ amt(lineSum(item), itemsBlock?.order.currency) }}</td>
                    <td class="td">{{ itemCashUnit(item) ? amt(itemCashUnit(item), itemsBlock?.order.currency) : '—' }}</td>
                    <td class="td">{{ itemCashTotal(item) ? amt(itemCashTotal(item), itemsBlock?.order.currency) : '—' }}</td>
                    <td class="td">{{ amt(itemCombined(item), itemsBlock?.order.currency) }}</td>
                  </tr>
                </tbody>
                <tfoot v-if="itemsBlock?.items.length">
                  <tr class="total-row">
                    <td class="td">{{ t('common.total') }}</td>
                    <td class="td"></td>
                    <td class="td"></td>
                    <td class="td">{{ amt(itemsSumTotal, itemsBlock.order.currency) }}</td>
                    <td class="td"></td>
                    <td class="td">{{ itemsCashGrand ? amt(itemsCashGrand, itemsBlock.order.currency) : '—' }}</td>
                    <td class="td">{{ amt(itemsCombinedGrand, itemsBlock.order.currency) }}</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
      </Teleport>
    </template>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchSupplier,
  fetchSupplierBalance,
  filterSupplierPayments,
  type Supplier,
  type SupplierBalance,
  type SupplierPayment,
} from '@/api/suppliers'
import {
  fetchWarehouseOrderItems,
  fetchWarehouseOrdersBySupplier,
  type WarehouseOrder,
  type WarehouseOrderItem,
} from '@/api/warehouseOrders'
import { fetchPaymentTypes, type PaymentType } from '@/api/payments'
import { formatApiError } from '@/api/http'
import { formatUzPhone } from '@/utils/phone'
import { formatDate } from '@/utils/format'
import { formatRate, moneyIn, type CurrencyCode } from '@/utils/currency'

const { t } = useI18n()
const route = useRoute()

const loading = ref(true)
const error = ref<string | null>(null)
const supplier = ref<Supplier | null>(null)
const balances = ref<SupplierBalance[]>([])
const orders = ref<WarehouseOrder[]>([])
const itemsByOrder = ref<Record<number, WarehouseOrderItem[]>>({})
const payments = ref<SupplierPayment[]>([])
const CURRENCY_MODES = ['USD', 'UZS', 'all'] as const
type ViewMode = (typeof CURRENCY_MODES)[number]
const viewMode = ref<ViewMode>('all')
const tab = ref<'akt' | 'inbounds' | 'payments'>('akt')
const paymentTypes = ref<PaymentType[]>([])
const paymentTypeFilter = ref(0)
const itemsOpen = ref(false)
const itemsOrderId = ref<number | null>(null)

const PERIODS = ['day', 'month', 'year'] as const
const PAGE_SIZES = [10, 20, 30, 50] as const
type PeriodMode = (typeof PERIODS)[number]
const aktMode = ref<PeriodMode>('month')
const aktDay = ref(isoDate(new Date()))
const aktMonth = ref(isoMonth(new Date()))
const aktYear = ref(new Date().getFullYear())
const payMode = ref<PeriodMode>('month')
const payDay = ref(isoDate(new Date()))
const payMonth = ref(isoMonth(new Date()))
const payYear = ref(new Date().getFullYear())
const aktPage = ref(0)
const aktPageSize = ref(20)

function isoDate(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function isoMonth(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}
function buildRange(mode: PeriodMode, dayValue: string, monthValue: string, yearValue: number) {
  const today = new Date()
  const todayIso = isoDate(today)
  if (mode === 'day') {
    const day = dayValue || todayIso
    return { from: day, to: day }
  }
  if (mode === 'year') {
    const year = Number(yearValue) || today.getFullYear()
    const end = year === today.getFullYear() ? today : new Date(year, 11, 31)
    return { from: isoDate(new Date(year, 0, 1)), to: isoDate(end) }
  }
  const [yearText, monthText] = (monthValue || isoMonth(today)).split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  const monthEnd = new Date(year, month, 0)
  const end = year === today.getFullYear() && month - 1 === today.getMonth() && monthEnd > today ? today : monthEnd
  return { from: isoDate(new Date(year, month - 1, 1)), to: isoDate(end) }
}
function switchPeriod(
  current: PeriodMode,
  next: PeriodMode,
  day: { value: string },
  month: { value: string },
  year: { value: number },
) {
  if (next === current) return
  const today = new Date()
  if (next === 'day') {
    if (current === 'month' && month.value) {
      const [y, m] = month.value.split('-').map(Number)
      day.value = y === today.getFullYear() && m - 1 === today.getMonth()
        ? isoDate(today)
        : isoDate(new Date(y, m - 1, 1))
    } else {
      const y = Number(year.value) || today.getFullYear()
      day.value = y === today.getFullYear() ? isoDate(today) : isoDate(new Date(y, 0, 1))
    }
  } else if (next === 'month') {
    if (current === 'day' && day.value) {
      month.value = day.value.slice(0, 7)
    } else {
      const y = Number(year.value) || today.getFullYear()
      const m = y === today.getFullYear() ? today.getMonth() + 1 : 1
      month.value = `${y}-${String(m).padStart(2, '0')}`
    }
  } else if (current === 'day' && day.value) {
    year.value = Number(day.value.slice(0, 4))
  } else if (month.value) {
    year.value = Number(month.value.slice(0, 4))
  }
}
function setAktPeriod(mode: PeriodMode) {
  switchPeriod(aktMode.value, mode, aktDay, aktMonth, aktYear)
  aktMode.value = mode
}
function setPayPeriod(mode: PeriodMode) {
  switchPeriod(payMode.value, mode, payDay, payMonth, payYear)
  payMode.value = mode
}
const seededAkt = buildRange(aktMode.value, aktDay.value, aktMonth.value, aktYear.value)
const aktFrom = ref(seededAkt.from)
const aktTo = ref(seededAkt.to)
watch([aktMode, aktDay, aktMonth, aktYear], () => {
  const range = buildRange(aktMode.value, aktDay.value, aktMonth.value, aktYear.value)
  aktFrom.value = range.from
  aktTo.value = range.to
})
const aktRange = computed(() => {
  const from = aktFrom.value
  const to = aktTo.value
  if (from && to && from > to) return { from: to, to: from }
  return { from, to }
})
const payRange = computed(() => buildRange(payMode.value, payDay.value, payMonth.value, payYear.value))
function inRange(value: string | null | undefined, range: { from: string; to: string }) {
  const time = stamp(value)
  if (!time) return false
  return time >= new Date(`${range.from}T00:00:00`).getTime() && time <= new Date(`${range.to}T23:59:59`).getTime()
}

const supplierId = computed(() => Number(route.params.id || 0))

function orderCurrency(o: WarehouseOrder): CurrencyCode {
  return (o.currency || 'UZS') as CurrencyCode
}
function paymentDebtCurrency(p: SupplierPayment): CurrencyCode {
  return (p.debtCurrency || p.currency || 'UZS') as CurrencyCode
}
function paymentAmount(p: SupplierPayment) {
  return Number(p.appliedAmount ?? p.paidSumm ?? 0)
}
function isCashPayment(p: SupplierPayment) {
  return p.debtTarget === 'CASH'
}
function somPaid(p: SupplierPayment) {
  return (p.currency || 'UZS') === 'UZS' ? Number(p.paidSumm || 0) : 0
}
function usdPaid(p: SupplierPayment) {
  if ((p.currency || 'UZS') === 'USD') return Number(p.paidSumm || 0)
  return paymentDebtCurrency(p) === 'USD' ? paymentAmount(p) : 0
}
function ratePaid(p: SupplierPayment) {
  const rate = Number(p.exchangeRate || 0)
  if ((p.currency || 'UZS') === paymentDebtCurrency(p) || rate <= 0) return 0
  return rate
}
function lineSum(item: WarehouseOrderItem) {
  return Number(item.count || 0) * Number(item.priceCost || 0)
}
function itemCashUnit(item: WarehouseOrderItem) {
  return Number(item.cashPrice || 0)
}
function itemCashTotal(item: WarehouseOrderItem) {
  return Number(item.count || 0) * itemCashUnit(item)
}
function itemCombined(item: WarehouseOrderItem) {
  return Number(item.count || 0) * (Number(item.priceCost || 0) + itemCashUnit(item))
}
function orderSum(order: WarehouseOrder) {
  return Number(order.totalSum || 0)
}
function orderCash(order: WarehouseOrder) {
  return Number(order.cashTotal || 0)
}
function orderCombined(order: WarehouseOrder) {
  if (order.combinedTotal != null) return Number(order.combinedTotal)
  return orderSum(order) + orderCash(order)
}
function formatNum(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}
function amt(v: number | null | undefined, currency?: string | null) {
  return moneyIn(v, (currency || 'UZS') as CurrencyCode, t('common.currency'))
}
function stamp(v?: string | null) {
  const time = v ? new Date(v).getTime() : 0
  return Number.isNaN(time) ? 0 : time
}

const currencies = computed(() => {
  const set = new Set<CurrencyCode>()
  for (const b of balances.value) set.add((b.currency || 'UZS') as CurrencyCode)
  for (const o of orders.value) set.add(orderCurrency(o))
  for (const p of payments.value) set.add(paymentDebtCurrency(p))
  const list = [...set]
  list.sort((a, b) => (a === 'UZS' ? -1 : b === 'UZS' ? 1 : a.localeCompare(b)))
  return list.length ? list : (['UZS'] as CurrencyCode[])
})

function matchesView(currency: CurrencyCode) {
  return viewMode.value === 'all' || viewMode.value === currency
}

const summaryRows = computed(() => {
  const list: CurrencyCode[] = viewMode.value === 'all' ? currencies.value : [viewMode.value]
  const rows = list.map((currency) => {
    const balance = balances.value.find((b) => (b.currency || 'UZS') === currency)
    const ords = orders.value.filter((o) => orderCurrency(o) === currency)
    const pays = payments.value.filter((p) => paymentDebtCurrency(p) === currency)
    const sumPurchase = balance
      ? Number(balance.totalPurchase || 0)
      : ords.reduce((sum, o) => sum + orderSum(o), 0)
    const sumPaid = balance
      ? Number(balance.totalPaid || 0)
      : pays.filter((p) => !isCashPayment(p)).reduce((sum, p) => sum + paymentAmount(p), 0)
    const sumDebt = balance?.totalDebt != null
      ? Number(balance.totalDebt)
      : balance?.balance != null
        ? Number(balance.balance)
        : sumPurchase - sumPaid
    const cashPurchase = ords.reduce((sum, o) => sum + orderCash(o), 0)
    const cashPaid = pays.filter(isCashPayment).reduce((sum, p) => sum + paymentAmount(p), 0)
    return {
      currency,
      sumPurchase,
      sumPaid,
      sumDebt,
      cashPurchase,
      cashPaid,
      cashDebt: cashPurchase - cashPaid,
    }
  })
  if (viewMode.value !== 'all') return rows
  const visible = rows.filter(
    (row) => row.sumPurchase || row.sumPaid || row.sumDebt || row.cashPurchase || row.cashPaid || row.cashDebt,
  )
  return visible.length ? visible : rows.slice(0, 1)
})

const currencyOrders = computed(() =>
  orders.value
    .filter((o) => matchesView(orderCurrency(o)))
    .map((order) => ({ order, items: itemsByOrder.value[order.id] || [] }))
    .sort((a, b) => b.order.id - a.order.id),
)

const currencyPayments = computed(() =>
  payments.value
    .filter((p) => matchesView(paymentDebtCurrency(p)))
    .slice()
    .sort((a, b) => stamp(b.paidDate) - stamp(a.paidDate)),
)

const paymentTypeOptions = computed(() => {
  if (paymentTypes.value.length) return paymentTypes.value
  const seen = new Map<number, string>()
  for (const p of payments.value) {
    if (p.paymentTypeId) seen.set(p.paymentTypeId, p.paymentTypeName || String(p.paymentTypeId))
  }
  return [...seen].map(([id, name]) => ({ id, name }))
})

const filteredPayments = computed(() =>
  currencyPayments.value.filter((p) => {
    if (paymentTypeFilter.value && p.paymentTypeId !== paymentTypeFilter.value) return false
    return inRange(p.paidDate, payRange.value)
  }),
)

type AktEvent = {
  key: string
  id: number
  date?: string
  doc: string
  paymentType: string
  currency: CurrencyCode
  sum: number
  cash: number
  total: number
  debit: number
  credit: number
  paid: number
  orderId?: number
}

function movementsFor(currency: CurrencyCode) {
  const events: AktEvent[] = []
  for (const order of orders.value.filter((o) => orderCurrency(o) === currency)) {
    events.push({
      key: `in-${order.id}`,
      id: order.id,
      date: order.arrivalDate,
      doc: t('suppliers.inboundDoc', { id: order.id }),
      paymentType: '',
      currency,
      sum: orderSum(order),
      cash: orderCash(order),
      total: orderCombined(order),
      debit: orderSum(order),
      credit: 0,
      paid: 0,
      orderId: order.id,
    })
  }
  for (const p of payments.value.filter((p) => paymentDebtCurrency(p) === currency)) {
    const amount = paymentAmount(p)
    const cash = isCashPayment(p)
    events.push({
      key: `pay-${p.id}`,
      id: p.id,
      date: p.paidDate,
      doc: t('suppliers.paymentDoc', { id: p.id }),
      paymentType: p.paymentTypeName || '',
      currency,
      sum: cash ? 0 : amount,
      cash: cash ? amount : 0,
      total: 0,
      debit: 0,
      credit: cash ? 0 : amount,
      paid: amount,
    })
  }
  events.sort((a, b) => {
    const diff = stamp(a.date) - stamp(b.date)
    if (diff) return diff
    if (a.id !== b.id) return a.id - b.id
    return a.debit >= b.debit ? -1 : 1
  })
  const range = aktRange.value
  const from = new Date(`${range.from}T00:00:00`).getTime()
  let opening = 0
  const inPeriod: AktEvent[] = []
  for (const row of events) {
    const time = stamp(row.date)
    if (time && time < from) {
      opening += row.debit - row.credit
      continue
    }
    if (inRange(row.date, range)) inPeriod.push(row)
  }
  let balance = opening
  const movements = inPeriod.map((row) => {
    balance += row.debit - row.credit
    return { ...row, balance }
  })
  if (opening === 0) return movements
  return [
    ...movements,
    {
      key: `opening-${currency}`,
      id: 0,
      date: range.from,
      doc: t('suppliers.openingBalance'),
      paymentType: '',
      currency,
      sum: 0,
      cash: 0,
      total: 0,
      debit: 0,
      credit: 0,
      paid: 0,
      balance: opening,
    },
  ]
}

const aktRows = computed(() => {
  const list: CurrencyCode[] = viewMode.value === 'all' ? currencies.value : [viewMode.value]
  const rows = list.flatMap((currency) => movementsFor(currency))
  const openings = rows.filter((row) => row.key.startsWith('opening-'))
  const rest = rows.filter((row) => !row.key.startsWith('opening-'))
  rest.sort((a, b) => stamp(b.date) - stamp(a.date) || b.id - a.id)
  return [...rest, ...openings]
})

const aktTotalPages = computed(() => Math.max(1, Math.ceil(aktRows.value.length / aktPageSize.value)))
const aktPaged = computed(() => {
  const start = aktPage.value * aktPageSize.value
  return aktRows.value.slice(start, start + aktPageSize.value)
})

const itemsBlock = computed(() =>
  currencyOrders.value.find((block) => block.order.id === itemsOrderId.value) || null,
)
const itemsSumTotal = computed(() =>
  (itemsBlock.value?.items || []).reduce((sum, item) => sum + lineSum(item), 0),
)
const itemsCashGrand = computed(() =>
  (itemsBlock.value?.items || []).reduce((sum, item) => sum + itemCashTotal(item), 0),
)
const itemsCombinedGrand = computed(() =>
  (itemsBlock.value?.items || []).reduce((sum, item) => sum + itemCombined(item), 0),
)

function openItems(orderId: number) {
  itemsOrderId.value = orderId
  itemsOpen.value = true
}
function closeItems() {
  itemsOpen.value = false
  itemsOrderId.value = null
}
function onItemsKey(event: KeyboardEvent) {
  if (event.key === 'Escape' && itemsOpen.value) closeItems()
}

function xmlEscape(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
function excelCell(value: string | number) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return `<Cell><Data ss:Type="Number">${value}</Data></Cell>`
  }
  return `<Cell><Data ss:Type="String">${xmlEscape(String(value))}</Data></Cell>`
}
function downloadAktExcel() {
  const range = aktRange.value
  const headers = [
    t('common.date'),
    t('suppliers.doc'),
    t('suppliers.paymentType'),
    t('common.sum'),
    t('warehouseOrders.cashTotal'),
    t('common.total'),
    t('suppliers.credit'),
    t('suppliers.balance'),
  ]
  const lines = aktRows.value.map((row) => [
    formatDate(row.date),
    row.doc,
    row.paymentType || '',
    row.sum || '',
    row.cash || '',
    row.total || '',
    row.paid || '',
    row.balance,
  ])
  const xml = `<?xml version="1.0"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
<Worksheet ss:Name="Akt">
<Table>
<Row>${excelCell(`${supplier.value?.name || ''} · ${t(`suppliers.currencyMode.${viewMode.value}`)} · ${range.from} — ${range.to}`)}</Row>
<Row>${headers.map((h) => excelCell(h)).join('')}</Row>
${lines.map((line) => `<Row>${line.map((cell) => excelCell(cell)).join('')}</Row>`).join('\n')}
</Table>
</Worksheet>
</Workbook>`
  const blob = new Blob([xml], { type: 'application/vnd.ms-excel' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `akt-sverka-${supplierId.value}-${viewMode.value}-${range.from}-${range.to}.xls`
  link.click()
  URL.revokeObjectURL(link.href)
}

watch([viewMode, aktFrom, aktTo, aktPageSize], () => {
  aktPage.value = 0
})
watch(aktTotalPages, (total) => {
  if (aktPage.value > total - 1) aktPage.value = Math.max(0, total - 1)
})
watch(itemsOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})
window.addEventListener('keydown', onItemsKey)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onItemsKey)
  document.body.style.overflow = ''
})

async function load() {
  const id = supplierId.value
  if (!id) {
    supplier.value = null
    loading.value = false
    return
  }
  loading.value = true
  error.value = null
  try {
    const [s, o] = await Promise.all([
      fetchSupplier(id),
      fetchWarehouseOrdersBySupplier(id),
    ])
    const [b, p, types] = await Promise.all([
      fetchSupplierBalance(id).catch(() => null),
      filterSupplierPayments({ supplierId: id }, 0, 1000).catch(() => null),
      fetchPaymentTypes().catch(() => null),
    ])
    supplier.value = s.data || null
    balances.value = b?.data || []
    orders.value = o.data || []
    payments.value = p?.data?.content || []
    paymentTypes.value = types?.data?.content || []
    const itemLists = await Promise.all(
      orders.value.map(async (order) => {
        try {
          const res = await fetchWarehouseOrderItems(order.id)
          return [order.id, res.data || []] as const
        } catch {
          return [order.id, []] as const
        }
      }),
    )
    itemsByOrder.value = Object.fromEntries(itemLists)
  } catch (e) {
    error.value = formatApiError(e)
    supplier.value = null
  } finally {
    loading.value = false
  }
}

watch(supplierId, () => void load(), { immediate: true })
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.head { padding: 1.25rem 1.25rem 0.75rem; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.back { font-size: 0.875rem; font-weight: 500; color: #465fff; }
.profile { padding: 1.25rem; }
.profile-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.summary { padding: 1rem; display: flex; flex-direction: column; gap: 1rem; }
.summary-block { display: flex; flex-direction: column; gap: 0.65rem; }
.summary-block + .summary-block { border-top: 1px solid #e5e7eb; padding-top: 1rem; }
.cur-name { font-size: 0.875rem; font-weight: 600; color: #1f2937; }
.buckets { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
@media (max-width: 720px) { .buckets { grid-template-columns: 1fr; } }
.bucket { border: 1px solid #e5e7eb; border-radius: 0.85rem; padding: 0.85rem; }
.bucket-title { font-size: 0.8rem; font-weight: 700; color: #344054; margin-bottom: 0.55rem; }
.metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.5rem; }
.metric { display: flex; flex-direction: column; gap: 0.25rem; border-radius: 0.75rem; background: #f9fafb; padding: 0.7rem 0.75rem; min-width: 0; }
.metric-label { font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.metric-value { font-size: 0.95rem; font-weight: 700; color: #1f2937; overflow: hidden; text-overflow: ellipsis; }
.metric-value.paid { color: #039855; }
.metric-value.debt { color: #dc2626; }
.tabs { display: inline-flex; gap: 0.25rem; margin-bottom: 1rem; border-radius: 0.75rem; background: #f2f4f7; padding: 0.25rem; }
.tab { height: 2.25rem; border: 0; border-radius: 0.5rem; background: transparent; padding: 0 1rem; font-size: 0.875rem; font-weight: 600; color: #667085; white-space: nowrap; cursor: pointer; }
.tab.on { background: #fff; color: #465fff; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08); }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.total-row td { font-weight: 700; color: #1f2937; background: #f9fafb; border-top: 1px solid #e5e7eb; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.link { color: #465fff; font-weight: 500; }
.toolbar { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; padding: 0.75rem 1rem; border-bottom: 1px solid #f3f4f6; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.year-field { width: 6.5rem; }
.type-field { min-width: 12rem; }
.pager-size { width: 6.5rem; }
.period-toggle { display: inline-grid; grid-template-columns: repeat(3, minmax(4.25rem, 1fr)); gap: 0.15rem; height: 2.5rem; border-radius: 0.6rem; background: #f2f4f7; padding: 0.15rem; }
.period-btn { display: inline-flex; height: 100%; align-items: center; justify-content: center; border-radius: 0.45rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 600; color: #667085; white-space: nowrap; cursor: pointer; }
.period-btn:hover:not(.active) { color: #344054; background: rgb(255 255 255 / 60%); }
.period-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.12); }
.excel-btn { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; background: #039855; padding: 0 1rem; font-size: 0.875rem; font-weight: 600; color: #fff; }
.excel-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ghost { height: 2.25rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; white-space: nowrap; }
.ghost:disabled { opacity: 0.5; cursor: not-allowed; }
.pager { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 0.5rem; border-top: 1px solid #f3f4f6; padding: 0.75rem 1rem; }
.pager-info { font-size: 0.875rem; color: #6b7280; }
.view-btn { height: 2rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #c7d2fe; background: #eef2ff; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 600; color: #465fff; white-space: nowrap; cursor: pointer; }
.view-btn:hover { background: #e0e7ff; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgb(17 24 39 / 45%); padding: 1rem; }
.modal { width: min(40rem, 100%); max-height: min(80vh, 36rem); overflow: auto; border-radius: 1rem; background: #fff; box-shadow: 0 20px 40px rgb(16 24 40 / 18%); }
.modal-wide { width: min(72rem, 100%); }
.range-field { display: inline-flex; align-items: center; gap: 0.35rem; font-size: 0.75rem; font-weight: 600; color: #667085; }
.range-field .field { width: 10.5rem; }
.modal-head { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 1rem 1rem 0.75rem; }
:global(html.dark .bucket) { border-color: #344054; }
:global(html.dark .bucket-title) { color: #e4e7ec; }
:global(html.dark .summary-block + .summary-block) { border-color: #344054; }
:global(html.dark .period-toggle) { background: rgb(255 255 255 / 5%); }
:global(html.dark .period-btn) { color: #98a2b3; }
:global(html.dark .period-btn.active) { background: #1d2939; color: #9cb0ff; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.mb-4 { margin-bottom: 1rem; }
</style>
