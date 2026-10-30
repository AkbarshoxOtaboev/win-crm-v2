<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('warehouseOrders.breadcrumb')" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <input
        v-model="search"
        type="search"
        :placeholder="t('warehouseOrders.supplierSearch')"
        class="field sm:w-48"
      />
      <select v-model.number="warehouseFilter" class="field sm:w-44">
        <option :value="0">{{ t('warehouseOrders.allWarehouses') }}</option>
        <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
      </select>
      <div class="period-toggle" role="radiogroup" :aria-label="t('common.period')">
        <button
          v-for="mode in PERIODS"
          :key="mode"
          type="button"
          class="period-btn"
          :class="{ active: periodMode === mode }"
          role="radio"
          :aria-checked="periodMode === mode"
          @click="setPeriod(mode)"
        >
          {{ t(`warehouseOrders.period.${mode}`) }}
        </button>
      </div>
      <input v-if="periodMode === 'day'" v-model="dayValue" type="date" class="field sm:w-40" />
      <input v-else-if="periodMode === 'month'" v-model="monthValue" type="month" class="field sm:w-40" />
      <input v-else v-model.number="yearValue" type="number" min="2000" max="2100" class="field year-field" />
      <button type="button" class="btn ms-auto" :disabled="writeBlocked" @click="goCreate">{{ t('warehouseOrders.newInbound') }}</button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('warehouseOrders.supplier') }}</th>
              <th class="th">{{ t('common.warehouse') }}</th>
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('common.sum') }}</th>
              <th class="th">{{ t('warehouseOrders.debt') }}</th>
              <th class="th">{{ t('warehouseOrders.cashTotal') }}</th>
              <th class="th">{{ t('common.total') }}</th>
              <th class="th">{{ t('warehouseOrders.payment') }}</th>
              <th class="th text-center">{{ t('common.view') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="11" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="11" class="empty">{{ t('warehouseOrders.empty') }}</td></tr>
            <tr v-for="o in paged" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">#{{ o.id }}</td>
              <td class="td">{{ o.supplierName || o.supplierId }}</td>
              <td class="td">{{ o.warehouseName || o.warehouseId }}</td>
              <td class="td">{{ formatDate(o.arrivalDate) }}</td>
              <td class="td">
                {{ amt(orderSum(o), o) }}
                <span v-if="isForeign(o)" class="rate-sub">{{ t('warehouseOrders.rateLabel', { value: formatRate(o.exchangeRate) }) }}</span>
              </td>
              <td class="td">{{ amt(orderDebt(o), o) }}</td>
              <td class="td">{{ orderCash(o) > 0 ? amt(orderCash(o), o) : '—' }}</td>
              <td class="td">{{ amt(orderCombined(o), o) }}</td>
              <td class="td">{{ amt(orderPaid(o), o) }}</td>
              <td class="td text-center">
                <button type="button" class="view-btn" @click="openView(o)">
                  <EyeIcon :size="16" />
                  <span>{{ t('common.view') }}</span>
                </button>
              </td>
              <td class="td text-right">
                <div class="inline-flex items-center justify-end gap-1.5">
                  <button
                    v-if="!isTransferred(o)"
                    type="button"
                    class="transfer-btn"
                    @click="onTransfer(o)"
                  >
                    {{ t('warehouseOrders.transfer') }}
                  </button>
                  <span v-else class="transferred-badge">{{ t('warehouseOrders.transferred') }}</span>
                  <RowActions
                    @edit="openEdit(o)"
                    @delete="onDelete(o)"
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="filtered.length" class="pager">
        <span class="pager-info">{{ page + 1 }} / {{ totalPages }}</span>
        <select v-model.number="pageSize" class="field pager-size">
          <option :value="10">{{ t('common.perPageN', { n: 10 }) }}</option>
          <option :value="20">{{ t('common.perPageN', { n: 20 }) }}</option>
          <option :value="50">{{ t('common.perPageN', { n: 50 }) }}</option>
        </select>
        <button type="button" class="ghost" :disabled="page <= 0" @click="page -= 1">{{ t('common.prev') }}</button>
        <button type="button" class="ghost" :disabled="page >= totalPages - 1" @click="page += 1">{{ t('common.next') }}</button>
      </div>
    </div>

    <WarehouseOrderViewModal
      :open="viewOpen"
      :order-id="viewOrderId"
      :supplier="viewSupplier"
      @close="closeView"
    />
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import WarehouseOrderViewModal from '@/components/crm/WarehouseOrderViewModal.vue'
import EyeIcon from '@/icons/EyeIcon.vue'
import {
  deleteWarehouseOrder,
  fetchWarehouseOrders,
  transferWarehouseOrder,
  type WarehouseOrder,
} from '@/api/warehouseOrders'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchSuppliers, type Supplier } from '@/api/suppliers'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate, money } from '@/utils/format'
import { formatRate, moneyIn } from '@/utils/currency'
import Swal from 'sweetalert2'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const route = useRoute()
const router = useRouter()
const orders = ref<WarehouseOrder[]>([])
const warehouses = ref<Warehouse[]>([])
const suppliers = ref<Supplier[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')
const warehouseFilter = ref(0)
const PERIODS = ['day', 'month', 'year'] as const
type PeriodMode = (typeof PERIODS)[number]
const periodMode = ref<PeriodMode>('month')
const dayValue = ref(isoDate(new Date()))
const monthValue = ref(isoMonth(new Date()))
const yearValue = ref(new Date().getFullYear())

function isoDate(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function isoMonth(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}
function periodRange() {
  const today = new Date()
  const todayIso = isoDate(today)
  if (periodMode.value === 'day') {
    const day = dayValue.value || todayIso
    return { from: day, to: day }
  }
  if (periodMode.value === 'year') {
    const year = Number(yearValue.value) || today.getFullYear()
    const end = year === today.getFullYear() ? today : new Date(year, 11, 31)
    return { from: isoDate(new Date(year, 0, 1)), to: isoDate(end) }
  }
  const [yearText, monthText] = (monthValue.value || isoMonth(today)).split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  const start = new Date(year, month - 1, 1)
  const monthEnd = new Date(year, month, 0)
  const end = year === today.getFullYear() && month - 1 === today.getMonth() && monthEnd > today ? today : monthEnd
  return { from: isoDate(start), to: isoDate(end) }
}
function setPeriod(mode: PeriodMode) {
  if (mode === periodMode.value) return
  const today = new Date()
  if (mode === 'day') {
    if (periodMode.value === 'month' && monthValue.value) {
      const [year, month] = monthValue.value.split('-').map(Number)
      dayValue.value = year === today.getFullYear() && month - 1 === today.getMonth()
        ? isoDate(today)
        : isoDate(new Date(year, month - 1, 1))
    } else {
      const year = Number(yearValue.value) || today.getFullYear()
      dayValue.value = year === today.getFullYear() ? isoDate(today) : isoDate(new Date(year, 0, 1))
    }
  } else if (mode === 'month') {
    if (periodMode.value === 'day' && dayValue.value) {
      monthValue.value = dayValue.value.slice(0, 7)
    } else {
      const year = Number(yearValue.value) || today.getFullYear()
      const month = year === today.getFullYear() ? today.getMonth() + 1 : 1
      monthValue.value = `${year}-${String(month).padStart(2, '0')}`
    }
  } else if (periodMode.value === 'day' && dayValue.value) {
    yearValue.value = Number(dayValue.value.slice(0, 4))
  } else if (monthValue.value) {
    yearValue.value = Number(monthValue.value.slice(0, 4))
  }
  periodMode.value = mode
}
const page = ref(0)
const pageSize = ref(20)
const viewOpen = ref(false)
const viewOrderId = ref<number | null>(null)

const viewSupplier = computed(() => {
  if (!viewOrderId.value) return null
  const order = orders.value.find((o) => o.id === viewOrderId.value)
  if (!order?.supplierId) return null
  return suppliers.value.find((s) => s.id === order.supplierId) || null
})

function isForeign(o: WarehouseOrder) {
  return !!o.currency && o.currency !== 'UZS'
}
function amt(v: number, o: WarehouseOrder) {
  return isForeign(o) ? moneyIn(v, o.currency) : money(v)
}
function orderSum(o: WarehouseOrder) {
  return Number(o.totalSum || 0)
}
function orderCash(o: WarehouseOrder) {
  return Number(o.cashTotal || 0)
}
function orderCombined(o: WarehouseOrder) {
  if (o.combinedTotal != null) return Number(o.combinedTotal)
  return orderSum(o) + orderCash(o)
}
function orderPaid(o: WarehouseOrder) {
  return Number(o.paidSum || 0)
}
function orderDebt(o: WarehouseOrder) {
  if (o.debtSum != null) return Number(o.debtSum)
  return Math.max(0, orderSum(o) - orderPaid(o))
}
function isTransferred(o: WarehouseOrder) {
  return o.transferred === true || o.orderStatus === 'TRANSFERRED'
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  const range = periodRange()
  const from = new Date(`${range.from}T00:00:00`)
  const to = new Date(`${range.to}T23:59:59`)
  return [...orders.value]
    .filter((o) => {
      if (q && !(o.supplierName || '').toLowerCase().includes(q)) return false
      if (warehouseFilter.value && o.warehouseId !== warehouseFilter.value) return false
      if (from || to) {
        const d = o.arrivalDate ? new Date(o.arrivalDate) : null
        if (!d || Number.isNaN(d.getTime())) return false
        if (from && d < from) return false
        if (to && d > to) return false
      }
      return true
    })
    .sort((a, b) => Number(b.id) - Number(a.id))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => {
  const start = page.value * pageSize.value
  return filtered.value.slice(start, start + pageSize.value)
})

watch([search, warehouseFilter, periodMode, dayValue, monthValue, yearValue, pageSize], () => {
  page.value = 0
})
watch(totalPages, (total) => {
  if (page.value > total - 1) page.value = Math.max(0, total - 1)
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const [o, w, s] = await Promise.all([
      fetchWarehouseOrders(),
      fetchWarehouses(),
      fetchSuppliers(0, 500),
    ])
    orders.value = o.data || []
    warehouses.value = w.data || []
    suppliers.value = s.data?.content || []
    await openFromQuery()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function openFromQuery() {
  const openId = Number(route.query.open || 0)
  if (!openId) return
  const nextQuery = { ...route.query }
  delete nextQuery.open
  await router.replace({ path: '/warehouse-orders', query: nextQuery })
  viewOrderId.value = openId
  viewOpen.value = true
}

function goCreate() {
  void router.push('/warehouse-orders/create')
}

function openView(o: WarehouseOrder) {
  viewOrderId.value = o.id
  viewOpen.value = true
}

function closeView() {
  viewOpen.value = false
  viewOrderId.value = null
}

function openEdit(o: WarehouseOrder) {
  void router.push(`/warehouse-orders/${o.id}/edit`)
}

async function onDelete(o: WarehouseOrder) {
  const result = await Swal.fire({
    title: t('warehouseOrders.deleteConfirm', { id: o.id }),
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: t('warehouseOrders.removeItemYes'),
    cancelButtonText: t('warehouseOrders.cancelAction'),
    confirmButtonColor: '#d92d20',
    cancelButtonColor: '#98a2b3',
    reverseButtons: true,
  })
  if (!result.isConfirmed) return
  try {
    await deleteWarehouseOrder(o.id)
    if (viewOrderId.value === o.id) closeView()
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onTransfer(o: WarehouseOrder) {
  const result = await Swal.fire({
    title: t('warehouseOrders.transfer'),
    text: t('warehouseOrders.transferConfirm', { id: o.id }),
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: t('warehouseOrders.transferYes'),
    cancelButtonText: t('warehouseOrders.cancelAction'),
    confirmButtonColor: '#465fff',
    cancelButtonColor: '#98a2b3',
  })
  if (!result.isConfirmed) return

  try {
    await transferWarehouseOrder(o.id)
    await load()
    await Swal.fire({
      title: t('warehouseOrders.success'),
      text: t('warehouseOrders.transferSuccess'),
      icon: 'success',
      confirmButtonColor: '#465fff',
      timer: 2000,
      showConfirmButton: false,
    })
  } catch (e) {
    error.value = formatApiError(e)
    await Swal.fire({
      title: t('warehouseOrders.error'),
      text: formatApiError(e),
      icon: 'error',
      confirmButtonColor: '#465fff',
    })
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.rate-sub { display: block; font-size: 0.6875rem; color: #9ca3af; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.year-field { width: 6.5rem; }
.period-toggle { display: inline-grid; grid-template-columns: repeat(3, minmax(4.25rem, 1fr)); gap: 0.15rem; height: 2.5rem; border-radius: 0.6rem; background: #f2f4f7; padding: 0.15rem; }
.period-btn { display: inline-flex; height: 100%; align-items: center; justify-content: center; border-radius: 0.45rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 600; color: #667085; white-space: nowrap; }
.period-btn:hover:not(.active) { color: #344054; background: rgb(255 255 255 / 60%); }
.period-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.12); }
:global(html.dark .period-toggle) { background: rgb(255 255 255 / 5%); }
:global(html.dark .period-btn) { color: #98a2b3; }
:global(html.dark .period-btn.active) { background: #1d2939; color: #9cb0ff; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; white-space: nowrap; }
.view-btn {
  display: inline-flex;
  height: 2.25rem;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  border-radius: 0.5rem;
  padding: 0 0.65rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #465fff;
  transition: background 0.15s;
  white-space: nowrap;
}
.view-btn:hover { background: #eef2ff; }
.transfer-btn {
  height: 2.25rem;
  display: inline-flex;
  align-items: center;
  border-radius: 0.5rem;
  background: #12b76a;
  padding: 0 0.75rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
}
.transferred-badge {
  display: inline-flex;
  align-items: center;
  border-radius: 9999px;
  background: #ecfdf5;
  padding: 0.25rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: #059669;
  white-space: nowrap;
}
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.pager { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 0.5rem; border-top: 1px solid #f3f4f6; padding: 0.75rem 1.25rem; }
.pager-info { font-size: 0.875rem; color: #6b7280; }
.pager-size { width: 6.5rem; }
</style>
