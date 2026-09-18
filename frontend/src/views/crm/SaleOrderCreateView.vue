<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Yangi savdo" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">Yangi savdo buyurtmasi</h3>
          <p class="sub">Mijoz, ombor, sotuvchi, sana va izoh — keyin pozitsiyalar qo‘shing</p>
        </div>
        <router-link to="/sales" class="ghost">Orqaga</router-link>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="p-5">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.4]">
            Mijoz
            <div class="client-row">
              <SearchableSelect
                v-model="form.clientId"
                :options="clientOptions"
                placeholder="Mijozni tanlang..."
                search-placeholder="Ism yoki telefon..."
                :disabled="!!orderId"
              />
              <button
                type="button"
                class="ghost client-add"
                :disabled="!!orderId"
                title="Yangi mijoz"
                @click="openClientModal"
              >
                + Yangi mijoz
              </button>
            </div>
          </label>

          <label class="lbl min-w-0 flex-1">
            Ombor
            <select v-model.number="form.warehouseId" required class="field" :disabled="!!orderId">
              <option :value="0" disabled>Omborni tanlang</option>
              <option v-for="w in activeWarehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 flex-1">
            Sotuvchi
            <select v-model.number="form.userId" required class="field" :disabled="!!orderId">
              <option :value="0" disabled>Sotuvchini tanlang</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            Sana
            <input v-model="form.orderDate" type="date" required class="field" :disabled="!!orderId" />
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
          <p class="sub">
            WINDOW mahsulotlarda kv.m = (eni×bo‘yi×soni)/10000 (eni/bo‘yi sm da), summa = kv.m × sotish
          </p>
        </div>
      </div>

      <div class="p-5 border-b border-gray-100">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.4]">
            Mahsulot
            <SearchableSelect
              v-model="itemForm.goodsId"
              :options="goodsOptions"
              placeholder="Mahsulotni tanlang..."
              search-placeholder="Mahsulot qidirish..."
            />
          </label>

          <template v-if="isWindowGoods">
            <label class="lbl min-w-0 w-28 sm:flex-none">
              Eni (sm)
              <input v-model.number="itemForm.width" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              Bo‘yi (sm)
              <input v-model.number="itemForm.height" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl min-w-0 w-24 sm:flex-none">
              Soni
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              Kv.m
              <input :value="formatNum(computedKvm)" class="field" readonly />
            </label>
            <label class="lbl min-w-0 w-36 sm:flex-none">
              Sotish narxi
              <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-40 sm:flex-none">
              Umumiy summa
              <input :value="money(computedSum)" class="field" readonly />
            </label>
          </template>

          <template v-else>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              Soni
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-36 sm:flex-none">
              Tannarx
              <input v-model.number="itemForm.priceCost" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-36 sm:flex-none">
              Sotish
              <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-40 sm:flex-none">
              Umumiy summa
              <input :value="money(computedSum)" class="field" readonly />
            </label>
          </template>

          <div class="flex items-end">
            <button type="button" class="btn" :disabled="saving || !canAddItem" @click="onAddItem">
              {{ saving ? '...' : '+ Qo‘shish' }}
            </button>
          </div>
        </div>
        <p v-if="!headerReady" class="mt-2 text-xs text-amber-600">
          Avval mijoz, ombor, sotuvchi va sanani tanlang
        </p>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="th">#</th>
              <th class="th">Mahsulot</th>
              <th class="th">Eni (sm)</th>
              <th class="th">Bo‘yi (sm)</th>
              <th class="th">Soni</th>
              <th class="th">Kv.m</th>
              <th class="th">Sotish</th>
              <th class="th">Summa</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="displayItems.length === 0">
              <td colspan="9" class="empty">Hali pozitsiya yo‘q</td>
            </tr>
            <tr v-for="(row, idx) in displayItems" :key="row.id" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ row.goodsName }}</td>
              <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
              <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
              <td class="td">{{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}</td>
              <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
              <td class="td">{{ money(row.priceSelling) }}</td>
              <td class="td">{{ money(row.sum) }}</td>
              <td class="td text-right">
                <button type="button" class="danger" @click="onRemoveItem(row.id)">O‘chirish</button>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="displayItems.length">
            <tr class="border-t border-gray-200 bg-gray-50">
              <td class="td font-semibold" colspan="7">Jami</td>
              <td class="td font-semibold">{{ money(itemsTotalSum) }}</td>
              <td class="td" />
            </tr>
          </tfoot>
        </table>
      </div>

      <div class="flex justify-end gap-2 border-t border-gray-100 p-5">
        <router-link to="/sales" class="ghost">Bekor</router-link>
        <button
          type="button"
          class="btn"
          :disabled="saving || !orderId || displayItems.length === 0"
          @click="onSave"
        >
          {{ saving ? '...' : 'Saqlash' }}
        </button>
      </div>
    </div>

    <div v-if="clientModal" class="overlay" @click.self="clientModal = false">
      <div class="modal">
        <h3 class="title mb-4">Yangi mijoz</h3>
        <div v-if="clientError" class="err mb-3">{{ clientError }}</div>
        <form class="space-y-3" @submit.prevent="onCreateClient">
          <label class="lbl">
            F.I.Sh *
            <input v-model="clientForm.fullName" required class="field" />
          </label>
          <label class="lbl">
            Telefon *
            <input
              :value="clientForm.phone"
              required
              inputmode="tel"
              placeholder="+998-(97)-221-88-96"
              maxlength="19"
              class="field"
              @input="onClientPhoneInput"
            />
          </label>
          <label class="lbl">
            Manzil *
            <input v-model="clientForm.address" required class="field" />
          </label>
          <label class="lbl">
            Qo‘shimcha tel
            <input
              :value="clientForm.additionalPhone"
              inputmode="tel"
              placeholder="+998-(97)-221-88-96"
              maxlength="19"
              class="field"
              @input="onClientAdditionalPhoneInput"
            />
          </label>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="clientModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="clientSaving">
              {{ clientSaving ? '...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import { createSaleOrder, updateSaleOrder } from '@/api/sales'
import {
  createSaleOrderItem,
  deleteSaleOrderItem,
  fetchSaleOrderItems,
  type SaleOrderItem,
} from '@/api/saleOrderItems'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { createClient, fetchClients, type Client } from '@/api/clients'
import { fetchUsers, type UserItem } from '@/api/users'
import { fetchGoods, type Goods } from '@/api/goods'
import { fetchStocksByWarehouse, type Stock } from '@/api/stocks'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import { money } from '@/utils/format'
import { formatUzPhone, isCompleteUzPhone } from '@/utils/phone'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const warehouses = ref<Warehouse[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const goods = ref<Goods[]>([])
const stocks = ref<Stock[]>([])
const items = ref<SaleOrderItem[]>([])
const orderId = ref<number | null>(null)
const saving = ref(false)
const error = ref<string | null>(null)
const clientModal = ref(false)
const clientSaving = ref(false)
const clientError = ref<string | null>(null)

const form = reactive({
  clientId: 0,
  warehouseId: 0,
  userId: 0,
  orderDate: todayLocal(),
  comment: '',
})

const clientForm = reactive({
  fullName: '',
  phone: '+998-',
  address: '',
  additionalPhone: '',
})

const itemForm = reactive({
  goodsId: 0,
  count: 1,
  width: 0,
  height: 0,
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

function formatNum(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

function isWindow(g?: Goods | null) {
  return (g?.type || '').toUpperCase() === 'WINDOW'
}

const selectedGoods = computed(() => goods.value.find((g) => g.id === itemForm.goodsId) || null)
const isWindowGoods = computed(() => isWindow(selectedGoods.value))

const computedKvm = computed(() => {
  if (!isWindowGoods.value) return 0
  return (Number(itemForm.width || 0) * Number(itemForm.height || 0) * Number(itemForm.count || 0)) / 10000
})

const computedSum = computed(() => {
  if (isWindowGoods.value) {
    return computedKvm.value * Number(itemForm.priceSelling || 0)
  }
  return Number(itemForm.count || 0) * Number(itemForm.priceSelling || 0)
})

const activeWarehouses = computed(() =>
  warehouses.value.filter((w) => !w.status || w.status === 'ACTIVE'),
)

const clientOptions = computed(() =>
  clients.value
    .filter((c) => !c.status || c.status === 'ACTIVE')
    .map((c) => ({ value: c.id, label: c.fullName })),
)

const stockByGoodsId = computed(() => {
  const map = new Map<number, number>()
  for (const s of stocks.value) {
    if (!s.goodsId) continue
    const qty = Number(s.pieceCount != null ? s.pieceCount : s.count || 0)
    map.set(s.goodsId, (map.get(s.goodsId) || 0) + qty)
  }
  return map
})

function hasWarehouseStock(g: Goods) {
  if ((g.type || '').toUpperCase() === 'SERVICE') return true
  return (stockByGoodsId.value.get(g.id) || 0) > 0
}

const goodsOptions = computed(() =>
  goods.value
    .filter((g) => (!g.status || g.status === 'ACTIVE') && hasWarehouseStock(g))
    .map((g) => ({ value: g.id, label: g.name })),
)

const headerReady = computed(
  () =>
    form.clientId > 0 &&
    form.warehouseId > 0 &&
    form.userId > 0 &&
    !!form.orderDate,
)

const canAddItem = computed(() => {
  if (!headerReady.value || itemForm.goodsId <= 0 || Number(itemForm.count) <= 0) return false
  if (isWindowGoods.value) {
    return (
      Number(itemForm.width) > 0 &&
      Number(itemForm.height) > 0 &&
      Number(itemForm.priceSelling) > 0
    )
  }
  return Number(itemForm.priceCost) > 0 && Number(itemForm.priceSelling) > 0
})

const displayItems = computed(() =>
  items.value.map((it) => {
    const g = goods.value.find((x) => x.id === it.goodsId)
    const windowItem = isWindow(g)
    const width = it.width != null ? Number(it.width) : null
    const height = it.height != null ? Number(it.height) : null
    const count = Number(it.count || 0)
    const priceSelling = Number(it.priceSelling || 0)
    let pieces: number | null = null
    if (windowItem && width && height && width > 0 && height > 0) {
      pieces = (count * 10000) / (width * height)
    }
    return {
      id: it.id,
      goodsName: it.goodsName || String(it.goodsId || ''),
      width,
      height,
      pieces,
      count,
      priceSelling,
      isWindow: windowItem,
      sum: count * priceSelling,
    }
  }),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))

watch(
  () => itemForm.goodsId,
  (id) => {
    const g = goods.value.find((x) => x.id === id)
    if (!g) return
    itemForm.priceCost = Number(g.priceCost || 0)
    itemForm.priceSelling = Number(g.priceSelling || 0)
    itemForm.count = 1
    if (isWindow(g)) {
      itemForm.width = Number(g.width || 0)
      itemForm.height = Number(g.height || 0)
      itemForm.priceCost = Number(g.priceSelling || 0)
    } else {
      itemForm.width = 0
      itemForm.height = 0
    }
  },
)

watch(
  () => form.warehouseId,
  async (warehouseId) => {
    itemForm.goodsId = 0
    stocks.value = []
    if (!warehouseId) return
    try {
      const res = await fetchStocksByWarehouse(warehouseId)
      stocks.value = res.data || []
    } catch (e) {
      error.value = formatApiError(e)
    }
  },
)

async function load() {
  error.value = null
  try {
    const [w, c, u, g] = await Promise.all([
      fetchWarehouses(),
      fetchClients(),
      fetchUsers(),
      fetchGoods(),
    ])
    warehouses.value = w.data || []
    clients.value = c.data || []
    users.value = u.data || []
    goods.value = g.data || []
    if (!form.warehouseId && activeWarehouses.value[0]) {
      form.warehouseId = activeWarehouses.value[0].id
    }
    if (form.warehouseId) {
      const stockRes = await fetchStocksByWarehouse(form.warehouseId)
      stocks.value = stockRes.data || []
    }
    const me = users.value.find((x) => x.username === auth.username)
    if (!form.userId) {
      form.userId = me?.id || users.value[0]?.id || 0
    }
    const qClientId = Number(route.query.clientId)
    if (qClientId > 0 && clients.value.some((c) => c.id === qClientId)) {
      form.clientId = qClientId
    }
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function ensureOrder(): Promise<number> {
  if (orderId.value) return orderId.value
  if (!headerReady.value) throw new Error('Mijoz, ombor, sotuvchi va sana majburiy')
  const res = await createSaleOrder({
    warehouseId: form.warehouseId,
    userId: form.userId,
    orderDate: dateToApi(form.orderDate),
    totalSum: 0,
    clientId: form.clientId,
    comment: form.comment.trim() || undefined,
  })
  const id = res.data?.id
  if (!id) throw new Error('Savdo yaratilmadi')
  orderId.value = id
  return id
}

async function syncOrderTotal() {
  if (!orderId.value) return
  await updateSaleOrder(orderId.value, {
    warehouseId: form.warehouseId,
    userId: form.userId,
    orderDate: dateToApi(form.orderDate),
    totalSum: itemsTotalSum.value,
    clientId: form.clientId,
    comment: form.comment.trim() || undefined,
  })
}

async function reloadItems() {
  if (!orderId.value) {
    items.value = []
    return
  }
  items.value = (await fetchSaleOrderItems(orderId.value)).data || []
}

async function onAddItem() {
  if (!canAddItem.value) return
  saving.value = true
  error.value = null
  try {
    const id = await ensureOrder()
    const windowMode = isWindowGoods.value
    const selling = Number(itemForm.priceSelling)
    await createSaleOrderItem({
      warehouseId: form.warehouseId,
      saleOrderId: id,
      clientId: form.clientId,
      goodsId: itemForm.goodsId,
      priceCost: windowMode ? selling : Number(itemForm.priceCost),
      priceSelling: selling,
      count: Number(itemForm.count),
      width: windowMode ? Number(itemForm.width) : undefined,
      height: windowMode ? Number(itemForm.height) : undefined,
      arrivalDate: dateToApi(form.orderDate),
    })
    itemForm.goodsId = 0
    itemForm.count = 1
    itemForm.width = 0
    itemForm.height = 0
    itemForm.priceCost = 0
    itemForm.priceSelling = 0
    await reloadItems()
    await syncOrderTotal()
    if (form.warehouseId) {
      const stockRes = await fetchStocksByWarehouse(form.warehouseId)
      stocks.value = stockRes.data || []
    }
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onRemoveItem(id: number) {
  if (!confirm('Pozitsiya o‘chirilsinmi?')) return
  try {
    await deleteSaleOrderItem(id)
    await reloadItems()
    await syncOrderTotal()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onSave() {
  if (!orderId.value || displayItems.value.length === 0) return
  saving.value = true
  error.value = null
  try {
    await syncOrderTotal()
    void router.push(`/sales/${orderId.value}`)
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

onMounted(load)

function openClientModal() {
  clientForm.fullName = ''
  clientForm.phone = '+998-'
  clientForm.address = ''
  clientForm.additionalPhone = ''
  clientError.value = null
  clientModal.value = true
}

function onClientPhoneInput(e: Event) {
  const el = e.target as HTMLInputElement
  clientForm.phone = formatUzPhone(el.value)
}

function onClientAdditionalPhoneInput(e: Event) {
  const el = e.target as HTMLInputElement
  clientForm.additionalPhone = formatUzPhone(el.value)
}

async function onCreateClient() {
  clientSaving.value = true
  clientError.value = null
  try {
    if (!clientForm.fullName.trim()) {
      clientError.value = 'F.I.Sh majburiy'
      return
    }
    if (!isCompleteUzPhone(clientForm.phone)) {
      clientError.value = 'Telefon +998-(XX)-XXX-XX-XX formatida to‘liq bo‘lishi kerak'
      return
    }
    if (!clientForm.address.trim()) {
      clientError.value = 'Manzil majburiy'
      return
    }
    const extra = clientForm.additionalPhone.trim()
    const res = await createClient({
      fullName: clientForm.fullName.trim(),
      phone: formatUzPhone(clientForm.phone),
      address: clientForm.address.trim(),
      additionalPhone: extra && isCompleteUzPhone(extra) ? formatUzPhone(extra) : undefined,
    })
    const created = res.data
    const list = await fetchClients()
    clients.value = list.data || []
    if (created?.id) {
      form.clientId = created.id
    }
    clientModal.value = false
  } catch (e) {
    clientError.value = formatApiError(e)
  } finally {
    clientSaving.value = false
  }
}
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 1.25rem 1.25rem 1rem; border-bottom: 1px solid #f3f4f6; }
.form-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.client-row { display: flex; align-items: stretch; gap: 0.5rem; }
.client-row > :first-child { flex: 1; min-width: 0; }
.client-add { flex-shrink: 0; white-space: nowrap; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.75rem; font-size: 0.875rem; }
.field:disabled, .field[readonly] { background: #f9fafb; color: #374151; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; color: #374151; background: #fff; }
.ghost:disabled { opacity: 0.55; cursor: not-allowed; }
.danger { font-size: 0.8125rem; font-weight: 500; color: #dc2626; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
@media (max-width: 640px) {
  .form-row > * { flex: 1 1 100%; }
  .client-row { flex-direction: column; }
}
</style>
