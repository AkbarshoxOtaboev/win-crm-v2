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
      <input v-model="dateFrom" type="date" class="field sm:w-40" :title="t('common.from')" />
      <input v-model="dateTo" type="date" class="field sm:w-40" :title="t('common.to')" />
      <button type="button" class="ghost" @click="clearFilters">{{ t('common.clearFilter') }}</button>
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
              <th class="th">{{ t('warehouseOrders.payment') }}</th>
              <th class="th">{{ t('warehouseOrders.debt') }}</th>
              <th class="th text-center">{{ t('common.view') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="9" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="9" class="empty">{{ t('warehouseOrders.empty') }}</td></tr>
            <tr v-for="o in filtered" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">#{{ o.id }}</td>
              <td class="td">{{ o.supplierName || o.supplierId }}</td>
              <td class="td">{{ o.warehouseName || o.warehouseId }}</td>
              <td class="td">{{ formatDate(o.arrivalDate) }}</td>
              <td class="td">{{ money(orderSum(o)) }}</td>
              <td class="td">{{ money(orderPaid(o)) }}</td>
              <td class="td">{{ money(orderDebt(o)) }}</td>
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
                    :edit="!isTransferred(o)"
                    @edit="openEdit(o)"
                    @delete="onDelete(o)"
                  />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
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
import { computed, onMounted, ref } from 'vue'
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
const dateFrom = ref('')
const dateTo = ref('')
const viewOpen = ref(false)
const viewOrderId = ref<number | null>(null)

const viewSupplier = computed(() => {
  if (!viewOrderId.value) return null
  const order = orders.value.find((o) => o.id === viewOrderId.value)
  if (!order?.supplierId) return null
  return suppliers.value.find((s) => s.id === order.supplierId) || null
})

function orderSum(o: WarehouseOrder) {
  return Number(o.totalSum || 0)
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
  const from = dateFrom.value ? new Date(`${dateFrom.value}T00:00:00`) : null
  const to = dateTo.value ? new Date(`${dateTo.value}T23:59:59`) : null
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
    .sort((a, b) => Number(a.id) - Number(b.id))
})

function clearFilters() {
  search.value = ''
  warehouseFilter.value = 0
  dateFrom.value = ''
  dateTo.value = ''
}

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
  if (!confirm(t('warehouseOrders.deleteConfirm', { id: o.id }))) return
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
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
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
</style>
