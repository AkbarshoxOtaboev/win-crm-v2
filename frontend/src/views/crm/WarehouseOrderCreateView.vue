<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Yangi kirim" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">Yangi kirim buyurtmasi</h3>
          <p class="sub">Yetkazuvchi, ombor, sana va izoh — keyin pozitsiyalar qo‘shing</p>
        </div>
        <div class="flex gap-2">
          <router-link to="/warehouse-orders" class="ghost">Orqaga</router-link>
          <button
            v-if="orderId"
            type="button"
            class="btn"
            @click="finish"
          >
            Yakunlash
          </button>
        </div>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="p-5">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.4]">
            Yetkazib beruvchi
            <SearchableSelect
              v-model="form.supplierId"
              :options="supplierOptions"
              placeholder="Yetkazuvchini tanlang..."
              search-placeholder="Nom bo‘yicha qidirish..."
              :disabled="!!orderId"
            />
          </label>

          <label class="lbl min-w-0 flex-1">
            Ombor
            <select
              v-model.number="form.warehouseId"
              required
              class="field"
              :disabled="!!orderId"
            >
              <option :value="0" disabled>Omborni tanlang</option>
              <option v-for="w in activeWarehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            Sana
            <input
              v-model="form.arrivalDate"
              type="date"
              required
              class="field"
              :disabled="!!orderId"
            />
          </label>

          <label class="lbl min-w-0 flex-1">
            Izoh
            <input
              v-model="form.comment"
              type="text"
              class="field"
              placeholder="Izoh (ixtiyoriy)"
              :disabled="!!orderId"
            />
          </label>
        </div>
      </div>
    </div>

    <div class="card mt-4">
      <div class="head">
        <div>
          <h3 class="title">Pozitsiyalar</h3>
          <p class="sub">Mahsulot, miqdor va narxlarni qo‘shing</p>
        </div>
      </div>

      <div class="p-5 border-b border-gray-100">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.6]">
            Mahsulot
            <SearchableSelect
              v-model="itemForm.goodsId"
              :options="goodsOptions"
              placeholder="Mahsulotni tanlang..."
              search-placeholder="Mahsulot qidirish..."
            />
          </label>
          <label class="lbl min-w-0 w-28 sm:flex-none">
            Soni
            <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
          </label>
          <label class="lbl min-w-0 w-36 sm:flex-none">
            Tannarx
            <input v-model.number="itemForm.priceCost" type="number" min="0" step="0.01" class="field" />
          </label>
          <label class="lbl min-w-0 w-36 sm:flex-none">
            Sotish
            <input v-model.number="itemForm.priceSelling" type="number" min="0" step="0.01" class="field" />
          </label>
          <div class="flex items-end">
            <button
              type="button"
              class="btn"
              :disabled="saving || !canAddItem"
              @click="onAddItem"
            >
              {{ saving ? '...' : '+ Qo‘shish' }}
            </button>
          </div>
        </div>
        <p v-if="!headerReady" class="mt-2 text-xs text-amber-600">
          Avval yetkazuvchi, ombor va sanani tanlang
        </p>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="th">#</th>
              <th class="th">Mahsulot</th>
              <th class="th">Soni</th>
              <th class="th">Tannarx</th>
              <th class="th">Sotish</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="items.length === 0">
              <td colspan="6" class="empty">Hali pozitsiya yo‘q</td>
            </tr>
            <tr v-for="(it, idx) in items" :key="it.id" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ it.goodsName || it.goodsId }}</td>
              <td class="td">{{ it.count }}</td>
              <td class="td">{{ money(it.priceCost) }}</td>
              <td class="td">{{ money(it.priceSelling) }}</td>
              <td class="td text-right">
                <button type="button" class="danger" @click="onRemoveItem(it)">O‘chirish</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import {
  createWarehouseOrder,
  createWarehouseOrderItem,
  deleteWarehouseOrderItem,
  fetchWarehouseOrderItems,
  type WarehouseOrderItem,
} from '@/api/warehouseOrders'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchSuppliers, type Supplier } from '@/api/suppliers'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { money } from '@/utils/format'

const router = useRouter()
const warehouses = ref<Warehouse[]>([])
const suppliers = ref<Supplier[]>([])
const goods = ref<Goods[]>([])
const items = ref<WarehouseOrderItem[]>([])
const orderId = ref<number | null>(null)
const saving = ref(false)
const error = ref<string | null>(null)

const form = reactive({
  supplierId: 0,
  warehouseId: 0,
  arrivalDate: todayLocal(),
  comment: '',
})

const itemForm = reactive({
  goodsId: 0,
  count: 1,
  priceCost: 0,
  priceSelling: 0,
})

function todayLocal() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

function dateToApi(date: string) {
  return date.length === 10 ? `${date}T00:00:00` : date
}

const activeWarehouses = computed(() =>
  warehouses.value.filter((w) => !w.status || w.status === 'ACTIVE'),
)

const supplierOptions = computed(() =>
  suppliers.value
    .filter((s) => !s.status || s.status === 'ACTIVE')
    .map((s) => ({ value: s.id, label: s.name })),
)

const goodsOptions = computed(() =>
  goods.value
    .filter((g) => !g.status || g.status === 'ACTIVE')
    .map((g) => ({ value: g.id, label: g.name })),
)

const headerReady = computed(
  () => form.supplierId > 0 && form.warehouseId > 0 && !!form.arrivalDate,
)

const canAddItem = computed(
  () =>
    headerReady.value &&
    itemForm.goodsId > 0 &&
    Number(itemForm.count) > 0 &&
    Number(itemForm.priceCost) >= 0 &&
    Number(itemForm.priceSelling) >= 0,
)

watch(
  () => itemForm.goodsId,
  (id) => {
    const g = goods.value.find((x) => x.id === id)
    if (!g) return
    itemForm.priceCost = Number(g.priceCost || 0)
    itemForm.priceSelling = Number(g.priceSelling || 0)
  },
)

async function load() {
  error.value = null
  try {
    const [w, s, g] = await Promise.all([
      fetchWarehouses(),
      fetchSuppliers(0, 500),
      fetchGoods(),
    ])
    warehouses.value = w.data || []
    suppliers.value = s.data?.content || []
    goods.value = g.data || []
    if (!form.warehouseId && activeWarehouses.value[0]) {
      form.warehouseId = activeWarehouses.value[0].id
    }
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function ensureOrder(): Promise<number> {
  if (orderId.value) return orderId.value
  if (!headerReady.value) throw new Error('Yetkazuvchi, ombor va sana majburiy')
  const res = await createWarehouseOrder({
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    arrivalDate: dateToApi(form.arrivalDate),
    comment: form.comment.trim() || undefined,
  })
  const id = res.data?.id
  if (!id) throw new Error('Buyurtma yaratilmadi')
  orderId.value = id
  return id
}

async function reloadItems() {
  if (!orderId.value) {
    items.value = []
    return
  }
  items.value = (await fetchWarehouseOrderItems(orderId.value)).data || []
}

async function onAddItem() {
  if (!canAddItem.value) return
  saving.value = true
  error.value = null
  try {
    const id = await ensureOrder()
    await createWarehouseOrderItem({
      warehouseId: form.warehouseId,
      warehouseOrderId: id,
      supplierId: form.supplierId,
      goodsId: itemForm.goodsId,
      priceCost: Number(itemForm.priceCost),
      priceSelling: Number(itemForm.priceSelling),
      count: Number(itemForm.count),
      arrivalDate: dateToApi(form.arrivalDate),
    })
    itemForm.goodsId = 0
    itemForm.count = 1
    itemForm.priceCost = 0
    itemForm.priceSelling = 0
    await reloadItems()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onRemoveItem(it: WarehouseOrderItem) {
  if (!confirm('Pozitsiya o‘chirilsinmi?')) return
  try {
    await deleteWarehouseOrderItem(it.id)
    await reloadItems()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function finish() {
  if (!orderId.value) {
    void router.push('/warehouse-orders')
    return
  }
  void router.push(`/warehouse-orders?open=${orderId.value}`)
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 1.25rem 1.25rem 1rem; border-bottom: 1px solid #f3f4f6; }
.form-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.75rem; font-size: 0.875rem; }
.field:disabled { background: #f9fafb; color: #6b7280; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; color: #374151; }
.danger { font-size: 0.8125rem; font-weight: 500; color: #dc2626; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
@media (max-width: 640px) {
  .form-row > * { flex: 1 1 100%; }
}
</style>
