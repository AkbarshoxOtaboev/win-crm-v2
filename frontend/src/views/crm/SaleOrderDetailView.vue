<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="order ? `Savdo #${order.id}` : 'Savdo'" />
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <router-link to="/sales" class="ghost">← Ro‘yxat</router-link>
      <button type="button" class="ghost" @click="openEdit">Tahrirlash</button>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="order" class="card mb-4 p-5">
      <div class="grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
        <div><span class="lbl">Mijoz</span>{{ order.clientFullName || '—' }}</div>
        <div><span class="lbl">Ombor</span>{{ order.warehouseName || '—' }}</div>
        <div><span class="lbl">Jami</span>{{ money(order.totalSum) }}</div>
        <div><span class="lbl">Qarz</span>{{ money(order.debtSum) }}</div>
        <div><span class="lbl">To‘langan</span>{{ money(order.paidSum) }}</div>
        <div><span class="lbl">Chegirma</span>{{ order.discountType || '—' }} {{ order.discountValue || '' }}</div>
        <div><span class="lbl">Holat</span>{{ order.orderStatus || order.status || '—' }}</div>
        <div><span class="lbl">Sana</span>{{ formatDate(order.orderDate) }}</div>
      </div>
    </div>

    <div class="mb-4 flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.id" type="button" class="tab" :class="{ active: tab === t.id }" @click="tab = t.id">{{ t.label }}</button>
    </div>

    <div v-show="tab === 'items'" class="card">
      <div class="head">
        <h3 class="title">Qatorlar</h3>
        <button type="button" class="btn" @click="openItemCreate">+ Qator</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">Mahsulot</th><th class="th">Soni</th><th class="th">Sotish</th><th class="th">Sana</th><th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="items.length === 0"><td colspan="5" class="empty">Qator yo‘q</td></tr>
          <tr v-for="it in items" :key="it.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ it.goodsName || it.goodsId }}</td>
            <td class="td">{{ it.count }}</td>
            <td class="td">{{ money(it.priceSelling) }}</td>
            <td class="td">{{ formatDate(it.arrivalDate) }}</td>
            <td class="td text-right"><RowActions @edit="openItemEdit(it)" @delete="onItemDelete(it)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'discount'" class="card p-5 space-y-4">
      <form class="flex flex-wrap items-end gap-2" @submit.prevent="onDiscount">
        <div>
          <label class="lbl">Tur</label>
          <select v-model="discountType" class="field w-40">
            <option value="PERCENTAGE">PERCENTAGE</option>
            <option value="FIXED_AMOUNT">FIXED_AMOUNT</option>
          </select>
        </div>
        <div>
          <label class="lbl">Qiymat</label>
          <input v-model.number="discountValue" type="number" min="0.01" step="0.01" class="field w-32" />
        </div>
        <button type="submit" class="btn" :disabled="saving">Qo‘llash</button>
      </form>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">Tur</th><th class="th">Qiymat</th><th class="th">Summa</th><th class="th">Sana</th></tr></thead>
        <tbody>
          <tr v-if="discounts.length === 0"><td colspan="4" class="empty">Tarix yo‘q</td></tr>
          <tr v-for="d in discounts" :key="d.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ d.discountType }}</td>
            <td class="td">{{ d.discountValue }}</td>
            <td class="td">{{ money(d.discountAmount) }}</td>
            <td class="td">{{ formatDate(d.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'history'" class="card">
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">Dan</th><th class="th">Ga</th><th class="th">Kim</th><th class="th">Sana</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td colspan="4" class="empty">Tarix yo‘q</td></tr>
          <tr v-for="h in history" :key="h.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ h.fromStatus || '—' }}</td>
            <td class="td">{{ h.toStatus || '—' }}</td>
            <td class="td">{{ h.createdUsername || '—' }}</td>
            <td class="td">{{ formatDate(h.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'images'" class="card p-5 space-y-3">
      <input type="file" multiple accept="image/*" @change="onUploadImages" />
      <div class="flex flex-wrap gap-3">
        <div v-for="img in images" :key="img.id" class="relative">
          <img :src="img.url || `/uploads/${img.fileName}`" class="h-24 w-24 rounded-lg object-cover" alt="" />
          <button type="button" class="mt-1 text-xs text-error-500" @click="onDeleteImage(img)">O‘chirish</button>
        </div>
        <p v-if="images.length === 0" class="text-sm text-gray-500">Rasm yo‘q</p>
      </div>
    </div>

    <div v-show="tab === 'waste'" class="card">
      <div class="head">
        <h3 class="title">Chiqindi</h3>
        <button type="button" class="btn" @click="openWasteCreate">+ Chiqindi</button>
      </div>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100 dark:border-gray-800"><th class="th">Mahsulot</th><th class="th">Miqdor</th><th class="th">Izoh</th><th class="th text-right">Amallar</th></tr></thead>
        <tbody>
          <tr v-if="wastes.length === 0"><td colspan="4" class="empty">Yo‘q</td></tr>
          <tr v-for="w in wastes" :key="w.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ w.goodsName || w.goodsId }}</td>
            <td class="td">{{ w.quantity }}</td>
            <td class="td">{{ w.comment || '—' }}</td>
            <td class="td text-right"><RowActions :edit="false" @delete="onWasteDelete(w)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="orderModal" class="overlay" @click.self="orderModal = false">
      <div class="modal">
        <h3 class="title mb-4">Savdoni tahrirlash</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onOrderSave">
          <select v-model.number="orderForm.warehouseId" required class="field">
            <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
          <select v-model.number="orderForm.clientId" class="field">
            <option :value="0">— mijoz —</option>
            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
          </select>
          <select v-model.number="orderForm.userId" required class="field">
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
          <input v-model="orderForm.orderDate" type="datetime-local" required class="field" />
          <input v-model.number="orderForm.totalSum" type="number" min="0" step="0.01" required class="field" />
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
        <h3 class="title mb-4">{{ itemEditingId ? 'Qatorni tahrirlash' : 'Yangi qator' }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onItemSave">
          <select v-model.number="itemForm.goodsId" required class="field">
            <option :value="0" disabled>Mahsulot</option>
            <option v-for="g in goods" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <div class="grid grid-cols-2 gap-3">
            <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" required class="field" placeholder="Soni" />
            <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" required class="field" placeholder="Sotish" />
          </div>
          <input v-model.number="itemForm.priceCost" type="number" min="0.01" step="0.01" required class="field" placeholder="Tannarx" />
          <input v-model="itemForm.arrivalDate" type="datetime-local" required class="field" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="itemModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Saqlash</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="wasteModal" class="overlay" @click.self="wasteModal = false">
      <div class="modal">
        <h3 class="title mb-4">Chiqindi</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onWasteSave">
          <select v-model.number="wasteForm.goodsId" required class="field">
            <option :value="0" disabled>Mahsulot</option>
            <option v-for="g in goods" :key="'w'+g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <input v-model.number="wasteForm.quantity" type="number" min="0.01" step="0.01" required class="field" />
          <input v-model="wasteForm.comment" class="field" placeholder="Izoh" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="wasteModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Saqlash</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  applyDiscount,
  fetchDiscountHistory,
  fetchSaleOrder,
  fetchSaleOrderHistory,
  fetchSaleOrderImages,
  updateSaleOrder,
  uploadSaleOrderImages,
  deleteSaleOrderImage,
  type SaleOrder,
  type SaleOrderDiscountHistory,
  type SaleOrderHistory,
  type SaleOrderImage,
} from '@/api/sales'
import {
  createSaleOrderItem,
  deleteSaleOrderItem,
  fetchSaleOrderItems,
  updateSaleOrderItem,
  type SaleOrderItem,
} from '@/api/saleOrderItems'
import {
  createSaleOrderWaste,
  deleteSaleOrderWaste,
  fetchSaleOrderWastes,
  type SaleOrderWaste,
} from '@/api/saleOrderWastes'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchClients, type Client } from '@/api/clients'
import { fetchUsers, type UserItem } from '@/api/users'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { formatDate, money, nowLocal, toApiDate } from '@/utils/format'

const route = useRoute()
const tabs = [
  { id: 'items', label: 'Qatorlar' },
  { id: 'discount', label: 'Chegirma' },
  { id: 'history', label: 'Tarix' },
  { id: 'images', label: 'Rasmlar' },
  { id: 'waste', label: 'Chiqindi' },
] as const
const tab = ref<(typeof tabs)[number]['id']>('items')

const order = ref<SaleOrder | null>(null)
const items = ref<SaleOrderItem[]>([])
const discounts = ref<SaleOrderDiscountHistory[]>([])
const history = ref<SaleOrderHistory[]>([])
const images = ref<SaleOrderImage[]>([])
const wastes = ref<SaleOrderWaste[]>([])
const warehouses = ref<Warehouse[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const goods = ref<Goods[]>([])
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const saving = ref(false)
const discountType = ref('PERCENTAGE')
const discountValue = ref(10)

const orderModal = ref(false)
const orderForm = reactive({ warehouseId: 0, clientId: 0, userId: 0, orderDate: '', totalSum: 0, comment: '' })
const itemModal = ref(false)
const itemEditingId = ref<number | null>(null)
const itemForm = reactive({ goodsId: 0, count: 1, priceCost: 0, priceSelling: 0, arrivalDate: '' })
const wasteModal = ref(false)
const wasteForm = reactive({ goodsId: 0, quantity: 1, comment: '' })

function id() {
  return Number(route.params.id)
}

async function load() {
  error.value = null
  try {
    const [o, its, disc, hist, imgs, w, wh, cl, us, gs] = await Promise.all([
      fetchSaleOrder(id()),
      fetchSaleOrderItems(id()),
      fetchDiscountHistory(id()),
      fetchSaleOrderHistory(id()),
      fetchSaleOrderImages(id()),
      fetchSaleOrderWastes(id()),
      fetchWarehouses(),
      fetchClients(),
      fetchUsers(),
      fetchGoods(),
    ])
    order.value = o.data
    items.value = its.data || []
    discounts.value = disc.data || []
    history.value = hist.data || []
    images.value = imgs.data || []
    wastes.value = w.data || []
    warehouses.value = wh.data || []
    clients.value = cl.data || []
    users.value = us.data || []
    goods.value = gs.data || []
  } catch (e) {
    error.value = formatApiError(e, 'Yuklashda xatolik')
  }
}

function openEdit() {
  if (!order.value) return
  orderForm.warehouseId = order.value.warehouseId || 0
  orderForm.clientId = order.value.clientId || 0
  orderForm.userId = order.value.userId || 0
  orderForm.orderDate = (order.value.orderDate || '').slice(0, 16)
  orderForm.totalSum = Number(order.value.totalSum || 0)
  orderForm.comment = order.value.comment || ''
  formError.value = null
  orderModal.value = true
}

async function onOrderSave() {
  saving.value = true
  formError.value = null
  try {
    await updateSaleOrder(id(), {
      warehouseId: orderForm.warehouseId,
      userId: orderForm.userId,
      orderDate: toApiDate(orderForm.orderDate),
      totalSum: orderForm.totalSum,
      clientId: orderForm.clientId || null,
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

function fillItemFromGoods() {
  const g = goods.value.find((x) => x.id === itemForm.goodsId)
  if (g) {
    itemForm.priceCost = Number(g.priceCost || 0)
    itemForm.priceSelling = Number(g.priceSelling || 0)
  }
}

function openItemCreate() {
  itemEditingId.value = null
  itemForm.goodsId = goods.value[0]?.id || 0
  itemForm.count = 1
  itemForm.arrivalDate = nowLocal()
  fillItemFromGoods()
  formError.value = null
  itemModal.value = true
}

function openItemEdit(it: SaleOrderItem) {
  itemEditingId.value = it.id
  itemForm.goodsId = it.goodsId || 0
  itemForm.count = Number(it.count || 1)
  itemForm.priceCost = Number(it.priceCost || 0)
  itemForm.priceSelling = Number(it.priceSelling || 0)
  itemForm.arrivalDate = (it.arrivalDate || '').slice(0, 16)
  formError.value = null
  itemModal.value = true
}

async function onItemSave() {
  if (!order.value) return
  saving.value = true
  formError.value = null
  try {
    const payload = {
      warehouseId: order.value.warehouseId || 0,
      saleOrderId: order.value.id,
      clientId: order.value.clientId || 0,
      goodsId: itemForm.goodsId,
      priceCost: itemForm.priceCost,
      priceSelling: itemForm.priceSelling,
      count: itemForm.count,
      arrivalDate: toApiDate(itemForm.arrivalDate),
    }
    if (itemEditingId.value) await updateSaleOrderItem(itemEditingId.value, payload)
    else await createSaleOrderItem(payload)
    itemModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, 'Stock yoki validatsiya xatosi')
  } finally {
    saving.value = false
  }
}

async function onItemDelete(it: SaleOrderItem) {
  if (!confirm('Qator o‘chirilsinmi?')) return
  try {
    await deleteSaleOrderItem(it.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onDiscount() {
  saving.value = true
  try {
    await applyDiscount(id(), discountType.value, discountValue.value)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onUploadImages(e: Event) {
  const files = Array.from((e.target as HTMLInputElement).files || [])
  if (!files.length) return
  try {
    await uploadSaleOrderImages(id(), files)
    await load()
  } catch (err) {
    error.value = formatApiError(err)
  }
}

async function onDeleteImage(img: SaleOrderImage) {
  if (!confirm('Rasm o‘chirilsinmi?')) return
  try {
    await deleteSaleOrderImage(id(), img.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openWasteCreate() {
  wasteForm.goodsId = goods.value[0]?.id || 0
  wasteForm.quantity = 1
  wasteForm.comment = ''
  formError.value = null
  wasteModal.value = true
}

async function onWasteSave() {
  saving.value = true
  formError.value = null
  try {
    await createSaleOrderWaste({
      saleOrderId: id(),
      goodsId: wasteForm.goodsId,
      quantity: wasteForm.quantity,
      comment: wasteForm.comment || undefined,
    })
    wasteModal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onWasteDelete(w: SaleOrderWaste) {
  if (!confirm('Chiqindi o‘chirilsinmi?')) return
  try {
    await deleteSaleOrderWaste(w.id)
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
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.75rem; color: #6b7280; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
