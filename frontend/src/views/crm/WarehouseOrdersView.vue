<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Kirim" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <input
        v-model="search"
        type="search"
        placeholder="Yetkazuvchi nomi..."
        class="field sm:w-48"
      />
      <select v-model.number="warehouseFilter" class="field sm:w-44">
        <option :value="0">Barcha omborlar</option>
        <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
      </select>
      <input v-model="dateFrom" type="date" class="field sm:w-40" title="Dan" />
      <input v-model="dateTo" type="date" class="field sm:w-40" title="Gacha" />
      <button type="button" class="ghost" @click="clearFilters">Filterni tozalash</button>
      <button type="button" class="btn ms-auto" @click="goCreate">+ Yangi kirim</button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Yetkazuvchi</th>
              <th class="th">Ombor</th>
              <th class="th">Sana</th>
              <th class="th">Summa</th>
              <th class="th">To‘lov</th>
              <th class="th">Qarz</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="8" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="8" class="empty">Kirim yo‘q</td></tr>
            <tr v-for="o in filtered" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">
                <button type="button" class="text-brand-500" @click="openDetail(o)">#{{ o.id }}</button>
              </td>
              <td class="td">{{ o.supplierName || o.supplierId }}</td>
              <td class="td">{{ o.warehouseName || o.warehouseId }}</td>
              <td class="td">{{ formatDate(o.arrivalDate) }}</td>
              <td class="td">{{ money(orderSum(o)) }}</td>
              <td class="td">{{ money(orderPaid(o)) }}</td>
              <td class="td">{{ money(orderDebt(o)) }}</td>
              <td class="td text-right">
                <div class="inline-flex items-center justify-end gap-1.5">
                  <button
                    v-if="!isTransferred(o)"
                    type="button"
                    class="transfer-btn"
                    @click="onTransfer(o)"
                  >
                    Omborga o‘tkazish
                  </button>
                  <span v-else class="transferred-badge">Omborga o‘tkazildi</span>
                  <RowActions @edit="openEdit(o)" @delete="onDelete(o)" />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="detail" class="card mt-4">
      <div class="head">
        <h3 class="title">Kirim #{{ detail.id }} qatorlari</h3>
        <button type="button" class="btn" @click="openItemCreate">+ Qator</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">Mahsulot</th><th class="th">Soni</th><th class="th">Tannarx</th><th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="items.length === 0"><td colspan="4" class="empty">Qator yo‘q</td></tr>
          <tr v-for="it in items" :key="it.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ it.goodsName || it.goodsId }}</td>
            <td class="td">{{ it.count }}</td>
            <td class="td">{{ money(it.priceCost) }}</td>
            <td class="td text-right"><RowActions :edit="false" @delete="onItemDelete(it)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="orderModal" class="overlay" @click.self="orderModal = false">
      <div class="modal">
        <h3 class="title mb-4">Kirimni tahrirlash</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onOrderSave">
          <select v-model.number="orderForm.supplierId" required class="field">
            <option :value="0" disabled>Yetkazuvchi</option>
            <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
          <select v-model.number="orderForm.warehouseId" required class="field">
            <option :value="0" disabled>Ombor</option>
            <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
          <input v-model="orderForm.arrivalDate" type="datetime-local" required class="field" />
          <input v-model="orderForm.comment" class="field" placeholder="Izoh" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="orderModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Saqlash</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="itemModal" class="overlay" @click.self="itemModal = false">
      <div class="modal">
        <h3 class="title mb-4">Kirim qatori</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onItemSave">
          <select v-model.number="itemForm.goodsId" required class="field">
            <option :value="0" disabled>Mahsulot</option>
            <option v-for="g in goods" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" required class="field" />
          <input v-model.number="itemForm.priceCost" type="number" min="0" step="0.01" required class="field" placeholder="Tannarx" />
          <input v-model.number="itemForm.priceSelling" type="number" min="0" step="0.01" required class="field" placeholder="Sotish" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="itemModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Saqlash</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createWarehouseOrderItem,
  deleteWarehouseOrder,
  deleteWarehouseOrderItem,
  fetchWarehouseOrderItems,
  fetchWarehouseOrders,
  transferWarehouseOrder,
  updateWarehouseOrder,
  type WarehouseOrder,
  type WarehouseOrderItem,
} from '@/api/warehouseOrders'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchSuppliers, type Supplier } from '@/api/suppliers'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { formatDate, money, nowLocal, toApiDate } from '@/utils/format'
import Swal from 'sweetalert2'

const route = useRoute()
const router = useRouter()
const orders = ref<WarehouseOrder[]>([])
const items = ref<WarehouseOrderItem[]>([])
const warehouses = ref<Warehouse[]>([])
const suppliers = ref<Supplier[]>([])
const goods = ref<Goods[]>([])
const detail = ref<WarehouseOrder | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const warehouseFilter = ref(0)
const dateFrom = ref('')
const dateTo = ref('')
const orderModal = ref(false)
const itemModal = ref(false)
const editingId = ref<number | null>(null)
const orderForm = reactive({ supplierId: 0, warehouseId: 0, arrivalDate: '', comment: '' })
const itemForm = reactive({ goodsId: 0, count: 1, priceCost: 0, priceSelling: 0 })

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
    const [o, w, s, g] = await Promise.all([
      fetchWarehouseOrders(),
      fetchWarehouses(),
      fetchSuppliers(),
      fetchGoods(),
    ])
    orders.value = o.data || []
    warehouses.value = w.data || []
    suppliers.value = s.data?.content || []
    goods.value = g.data || []
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
  const found = orders.value.find((o) => o.id === openId)
  if (found) await openDetail(found)
  const nextQuery = { ...route.query }
  delete nextQuery.open
  await router.replace({ path: '/warehouse-orders', query: nextQuery })
}

async function openDetail(o: WarehouseOrder) {
  detail.value = o
  try {
    items.value = (await fetchWarehouseOrderItems(o.id)).data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function goCreate() {
  void router.push('/warehouse-orders/create')
}

function openEdit(o: WarehouseOrder) {
  editingId.value = o.id
  orderForm.supplierId = o.supplierId || 0
  orderForm.warehouseId = o.warehouseId || 0
  orderForm.arrivalDate = (o.arrivalDate || '').slice(0, 16)
  orderForm.comment = o.comment || ''
  formError.value = null
  orderModal.value = true
}

async function onOrderSave() {
  if (!editingId.value) return
  saving.value = true
  formError.value = null
  try {
    await updateWarehouseOrder(editingId.value, {
      supplierId: orderForm.supplierId,
      warehouseId: orderForm.warehouseId,
      arrivalDate: toApiDate(orderForm.arrivalDate),
      comment: orderForm.comment || undefined,
    })
    orderModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(o: WarehouseOrder) {
  if (!confirm(`Kirim #${o.id} o‘chirilsinmi?`)) return
  try {
    await deleteWarehouseOrder(o.id)
    if (detail.value?.id === o.id) {
      detail.value = null
      items.value = []
    }
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onTransfer(o: WarehouseOrder) {
  const result = await Swal.fire({
    title: 'Omborga o‘tkazish',
    text: `Kirim #${o.id} omborga o‘tkazilsinmi?`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: 'Ha, o‘tkazish',
    cancelButtonText: 'Bekor qilish',
    confirmButtonColor: '#465fff',
    cancelButtonColor: '#98a2b3',
  })
  if (!result.isConfirmed) return

  try {
    await transferWarehouseOrder(o.id)
    await load()
    if (detail.value?.id === o.id) await openDetail(o)
    await Swal.fire({
      title: 'Muvaffaqiyatli',
      text: 'Kirim omborga o‘tkazildi',
      icon: 'success',
      confirmButtonColor: '#465fff',
      timer: 2000,
      showConfirmButton: false,
    })
  } catch (e) {
    error.value = formatApiError(e)
    await Swal.fire({
      title: 'Xatolik',
      text: formatApiError(e),
      icon: 'error',
      confirmButtonColor: '#465fff',
    })
  }
}

function openItemCreate() {
  if (!detail.value) return
  itemForm.goodsId = goods.value[0]?.id || 0
  itemForm.count = 1
  const g = goods.value[0]
  itemForm.priceCost = Number(g?.priceCost || 0)
  itemForm.priceSelling = Number(g?.priceSelling || 0)
  formError.value = null
  itemModal.value = true
}

async function onItemSave() {
  if (!detail.value) return
  saving.value = true
  formError.value = null
  try {
    await createWarehouseOrderItem({
      warehouseId: detail.value.warehouseId || 0,
      warehouseOrderId: detail.value.id,
      supplierId: detail.value.supplierId || 0,
      goodsId: itemForm.goodsId,
      priceCost: itemForm.priceCost,
      priceSelling: itemForm.priceSelling,
      count: itemForm.count,
      arrivalDate: toApiDate(detail.value.arrivalDate || nowLocal()),
    })
    itemModal.value = false
    await openDetail(detail.value)
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onItemDelete(it: WarehouseOrderItem) {
  if (!confirm('Qator o‘chirilsinmi?')) return
  try {
    await deleteWarehouseOrderItem(it.id)
    if (detail.value) await openDetail(detail.value)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.head { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; white-space: nowrap; }
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
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
