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
        <div><span class="lbl">Holat</span><SaleStatusBadge :status="order.orderStatus" /></div>
        <div><span class="lbl">Sana</span>{{ formatDate(order.orderDate) }}</div>
      </div>
    </div>

    <div v-if="order" class="card mb-4 p-5">
      <div class="status-head">
        <div>
          <h3 class="title">Holatni o‘zgartirish</h3>
          <p class="sub">Joriy holat: <SaleStatusBadge :status="order.orderStatus" /></p>
        </div>
      </div>
      <div class="status-flow">
        <template v-for="(st, i) in flowStatuses" :key="st">
          <span v-if="i > 0" class="flow-sep" :class="{ done: doneIndex >= i }" />
          <span class="flow-step" :class="{ done: doneIndex >= i, current: flowIndex === i, cancelled: st === 'CANCELLED' }">
            {{ statusLabel(st) }}
          </span>
        </template>
      </div>
      <div v-if="nextStatuses.length" class="mt-4 flex flex-wrap gap-2">
        <button
          v-for="st in nextStatuses"
          :key="st"
          type="button"
          class="status-btn"
          :class="st === 'CANCELLED' ? 'status-btn-danger' : 'status-btn-primary'"
          :disabled="statusSaving || writeBlocked"
          @click="onStatus(st)"
        >
          {{ st === 'PROCESSING' ? 'Ishlab chiqarishga yuborish' : `${statusLabel(st)} holatiga o‘tkazish` }}
        </button>
      </div>
      <p v-else class="mt-4 text-sm text-gray-500 dark:text-gray-400">Bu holatdan keyin o‘zgartirish mumkin emas.</p>
      <p v-if="order.orderStatus === 'PROCESSING'" class="mt-2 text-xs text-gray-500 dark:text-gray-400">
        Ishlab chiqarish yakunlanganda buyurtma avtomatik «{{ statusLabel('READY') }}» holatiga o‘tadi.
      </p>
    </div>

    <div class="mb-4 flex flex-wrap gap-2">
      <button v-for="t in tabs" :key="t.id" type="button" class="tab" :class="{ active: tab === t.id }" @click="tab = t.id">{{ t.label }}</button>
    </div>

    <div v-show="tab === 'items'" class="card">
      <div class="head">
        <h3 class="title">Qatorlar</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openItemCreate">+ Qator</button>
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
            <td class="td">{{ itemQtyText(it) }}</td>
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
            <td class="td"><SaleStatusBadge v-if="h.fromStatus" :status="h.fromStatus" /><span v-else>—</span></td>
            <td class="td"><SaleStatusBadge :status="h.toStatus" /></td>
            <td class="td">{{ h.createdUsername || '—' }}</td>
            <td class="td">{{ formatDate(h.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'images'" class="card p-5 space-y-4">
      <div class="flex flex-wrap items-center gap-3">
        <label class="btn cursor-pointer" :class="{ 'pointer-events-none opacity-60': uploading || writeBlocked }">
          <ImagePlus :size="16" class="mr-2" />
          {{ uploading ? 'Yuklanmoqda...' : 'Rasm yuklash' }}
          <input
            :key="fileInputKey"
            type="file"
            multiple
            accept="image/jpeg,image/png,image/webp,image/gif"
            class="hidden"
            :disabled="uploading || writeBlocked"
            @change="onUploadImages"
          />
        </label>
        <span class="text-xs text-gray-500 dark:text-gray-400">JPG, PNG, WEBP yoki GIF. Bir nechta rasm tanlash mumkin.</span>
      </div>
      <div v-if="images.length" class="image-grid">
        <figure v-for="img in images" :key="img.id" class="image-card">
          <button type="button" class="image-open" :title="img.originalFileName || ''" @click="openPreview(img)">
            <AuthImage :src="imageSrc(img)" :alt="img.originalFileName || ''" class="image-thumb" />
          </button>
          <figcaption class="image-meta">
            <span class="truncate" :title="img.originalFileName || ''">{{ img.originalFileName || img.fileName }}</span>
            <button type="button" class="image-delete" title="O‘chirish" @click="onDeleteImage(img)">
              <Trash2 :size="14" />
            </button>
          </figcaption>
        </figure>
      </div>
      <p v-else class="text-sm text-gray-500 dark:text-gray-400">Rasm yo‘q</p>
    </div>

    <div v-if="preview" class="overlay" @click.self="preview = null">
      <div class="preview-box">
        <button type="button" class="preview-close" aria-label="Yopish" @click="preview = null"><X :size="18" /></button>
        <AuthImage :src="imageSrc(preview)" :alt="preview.originalFileName || ''" class="preview-img" />
        <p class="preview-name">{{ preview.originalFileName || preview.fileName }}</p>
      </div>
    </div>

    <div v-if="prodModal" class="overlay">
      <div class="modal">
        <h3 class="title mb-4">Ishlab chiqarishga yuborish #{{ order?.id }}</h3>
        <div v-if="prodError" class="err mb-3">{{ prodError }}</div>
        <form class="space-y-3" @submit.prevent="onSendToProduction">
          <label class="lbl">
            Birinchi sex *
            <select v-model.number="prodWorkshopId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="w in activeWorkshops" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>
          <label class="lbl">
            Izoh
            <input v-model="prodNote" class="field" />
          </label>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="prodModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="prodSaving || !prodWorkshopId">{{ prodSaving ? '...' : 'Yuborish' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-show="tab === 'waste'" class="card">
      <div class="head">
        <h3 class="title">Chiqindi</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openWasteCreate">+ Chiqindi</button>
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

    <div v-if="orderModal" class="overlay">
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

    <div v-if="itemModal" class="overlay">
      <div class="modal">
        <h3 class="title mb-4">{{ itemEditingId ? 'Qatorni tahrirlash' : 'Yangi qator' }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onItemSave">
          <select v-model.number="itemForm.goodsId" required class="field">
            <option :value="0" disabled>Mahsulot</option>
            <option v-for="g in goods" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <div v-if="itemIsWindow" class="grid grid-cols-2 gap-3">
            <label class="modal-lbl">
              Eni (sm)
              <input v-model.number="itemForm.width" type="number" min="1" step="1" required class="field" />
            </label>
            <label class="modal-lbl">
              Bo‘yi (sm)
              <input v-model.number="itemForm.height" type="number" min="1" step="1" required class="field" />
            </label>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <label class="modal-lbl">
              {{ itemIsWindow ? 'Soni (dona)' : 'Soni' }}
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" required class="field" />
            </label>
            <label class="modal-lbl">
              {{ itemIsWindow ? 'Sotish (1 kv.m)' : 'Sotish' }}
              <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" required class="field" />
            </label>
          </div>
          <p v-if="itemIsWindow" class="modal-hint">
            Kv.m: {{ formatQty(itemKvm) }} · Summa: {{ money(itemKvm * Number(itemForm.priceSelling || 0)) }}
          </p>
          <label class="modal-lbl">
            Tannarx
            <input v-model.number="itemForm.priceCost" type="number" min="0" step="0.01" required class="field" />
          </label>
          <p v-if="itemEditingId" class="modal-hint">
            Miqdor yoki mahsulot o‘zgarsa, eski miqdor omborga qaytariladi va yangisi ombordan ayiriladi.
          </p>
          <input v-model="itemForm.arrivalDate" type="datetime-local" required class="field" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="itemModal = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Saqlash</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="wasteModal" class="overlay">
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ImagePlus, Trash2, X } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SaleStatusBadge from '@/components/crm/SaleStatusBadge.vue'
import AuthImage from '@/components/crm/AuthImage.vue'
import { nextSaleStatuses, type SaleStatus } from '@/utils/saleStatus'
import { sendToProduction } from '@/api/production'
import { fetchActiveWorkshops, type Workshop } from '@/api/workshops'
import {
  applyDiscount,
  changeSaleOrderStatus,
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
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate, money, nowLocal, toApiDate } from '@/utils/format'

const { writeBlocked } = useFilialScope()
const { t, te } = useI18n()

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
const itemForm = reactive({ goodsId: 0, count: 1, width: 0, height: 0, priceCost: 0, priceSelling: 0, arrivalDate: '' })

function isWindowGoodsId(goodsId?: number | null) {
  const g = goods.value.find((x) => x.id === goodsId)
  return (g?.type || '').toUpperCase() === 'WINDOW'
}

const itemIsWindow = computed(() => isWindowGoodsId(itemForm.goodsId))
const itemKvm = computed(() =>
  itemIsWindow.value
    ? (Number(itemForm.width || 0) * Number(itemForm.height || 0) * Number(itemForm.count || 0)) / 10000
    : 0,
)

function formatQty(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

function windowPieces(it: SaleOrderItem) {
  const w = Number(it.width || 0)
  const h = Number(it.height || 0)
  if (!(w > 0 && h > 0)) return null
  return (Number(it.count || 0) * 10000) / (w * h)
}

function itemQtyText(it: SaleOrderItem) {
  if (!isWindowGoodsId(it.goodsId)) return formatQty(it.count)
  const pieces = windowPieces(it)
  const kvm = `${formatQty(it.count)} kv.m`
  return pieces == null ? kvm : `${kvm} (${formatQty(it.width)}×${formatQty(it.height)}, ${formatQty(pieces)} dona)`
}

watch(
  () => itemForm.goodsId,
  (id) => {
    if (!itemModal.value || !isWindowGoodsId(id)) return
    const g = goods.value.find((x) => x.id === id)
    if (!itemForm.width) itemForm.width = Number(g?.width || 0)
    if (!itemForm.height) itemForm.height = Number(g?.height || 0)
  },
)
const wasteModal = ref(false)
const wasteForm = reactive({ goodsId: 0, quantity: 1, comment: '' })
const statusSaving = ref(false)
const prodModal = ref(false)
const prodWorkshopId = ref(0)
const prodNote = ref('')
const prodError = ref<string | null>(null)
const prodSaving = ref(false)
const activeWorkshops = ref<Workshop[]>([])
const uploading = ref(false)
const fileInputKey = ref(0)
const preview = ref<SaleOrderImage | null>(null)

const MAIN_FLOW: SaleStatus[] = ['NEW', 'CONFIRMED', 'PROCESSING', 'READY', 'DELIVERED', 'COMPLETED']
const flowStatuses = computed<SaleStatus[]>(() =>
  order.value?.orderStatus === 'CANCELLED' ? [...MAIN_FLOW, 'CANCELLED'] : MAIN_FLOW,
)
const flowIndex = computed(() => flowStatuses.value.indexOf(order.value?.orderStatus as SaleStatus))
const doneIndex = computed(() => (order.value?.orderStatus === 'CANCELLED' ? -1 : flowIndex.value))
const nextStatuses = computed(() => nextSaleStatuses(order.value?.orderStatus))

function statusLabel(status: string) {
  const key = `saleStatus.${status}`
  return te(key) ? t(key) : status
}

function imageSrc(img: SaleOrderImage) {
  return img.downloadUrl || (img.fileName ? `/api/files/${img.fileName}` : '')
}

function openPreview(img: SaleOrderImage) {
  preview.value = img
}

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
  const g = goods.value.find((x) => x.id === itemForm.goodsId)
  itemForm.width = isWindowGoodsId(itemForm.goodsId) ? Number(g?.width || 0) : 0
  itemForm.height = isWindowGoodsId(itemForm.goodsId) ? Number(g?.height || 0) : 0
  itemForm.arrivalDate = nowLocal()
  fillItemFromGoods()
  formError.value = null
  itemModal.value = true
}

function openItemEdit(it: SaleOrderItem) {
  itemEditingId.value = it.id
  itemForm.goodsId = it.goodsId || 0
  const pieces = isWindowGoodsId(it.goodsId) ? windowPieces(it) : null
  itemForm.count = pieces != null ? Math.round(pieces * 10000) / 10000 : Number(it.count || 1)
  itemForm.width = Number(it.width || 0)
  itemForm.height = Number(it.height || 0)
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
      width: itemIsWindow.value ? itemForm.width : undefined,
      height: itemIsWindow.value ? itemForm.height : undefined,
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

async function reloadImages() {
  images.value = (await fetchSaleOrderImages(id())).data || []
}

async function onUploadImages(e: Event) {
  const files = Array.from((e.target as HTMLInputElement).files || [])
  if (!files.length) return
  uploading.value = true
  error.value = null
  try {
    await uploadSaleOrderImages(id(), files)
    await reloadImages()
  } catch (err) {
    error.value = formatApiError(err, 'Rasm yuklashda xatolik')
  } finally {
    uploading.value = false
    fileInputKey.value += 1
  }
}

async function onDeleteImage(img: SaleOrderImage) {
  if (!confirm('Rasm o‘chirilsinmi?')) return
  try {
    await deleteSaleOrderImage(id(), img.id)
    if (preview.value?.id === img.id) preview.value = null
    await reloadImages()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onStatus(status: SaleStatus) {
  if (!order.value) return
  if (status === 'PROCESSING') {
    await openProdModal()
    return
  }
  if (status === 'CANCELLED' && !confirm(`Savdo #${order.value.id} bekor qilinsinmi? Tovarlar omborga qaytariladi.`)) return
  statusSaving.value = true
  error.value = null
  try {
    await changeSaleOrderStatus(order.value.id, status)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'Holat o‘zgartirishda xatolik')
  } finally {
    statusSaving.value = false
  }
}

async function openProdModal() {
  prodWorkshopId.value = 0
  prodNote.value = ''
  prodError.value = null
  prodModal.value = true
  try {
    activeWorkshops.value = (await fetchActiveWorkshops()).data || []
    if (activeWorkshops.value[0]) prodWorkshopId.value = activeWorkshops.value[0].id
  } catch (e) {
    prodError.value = formatApiError(e, 'Sexlar yuklanmadi')
  }
}

async function onSendToProduction() {
  if (!order.value || !prodWorkshopId.value) return
  prodSaving.value = true
  prodError.value = null
  try {
    await sendToProduction({
      saleOrderId: order.value.id,
      workshopId: prodWorkshopId.value,
      note: prodNote.value.trim() || undefined,
    })
    prodModal.value = false
    await load()
  } catch (e) {
    prodError.value = formatApiError(e, 'Yuborishda xatolik')
  } finally {
    prodSaving.value = false
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

watch(
  () => route.params.id,
  (next, prev) => {
    if (next && next !== prev) {
      preview.value = null
      load()
    }
  },
)
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
.modal-lbl { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.modal-hint { font-size: 0.75rem; color: #6b7280; }
.dark .modal-lbl, .dark .modal-hint { color: #9ca3af; }
.sub { display: flex; align-items: center; gap: 0.5rem; margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.status-head { display: flex; justify-content: space-between; gap: 1rem; }
.status-flow { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; margin-top: 1rem; }
.flow-step { border-radius: 9999px; border: 1px solid #e5e7eb; padding: 0.25rem 0.75rem; font-size: 0.75rem; color: #6b7280; background: #f9fafb; }
.flow-step.done { border-color: #c7d2fe; background: #eef2ff; color: #4338ca; }
.flow-step.current { border-color: #465fff; background: #465fff; color: #fff; font-weight: 600; }
.flow-step.current.cancelled { border-color: #dc2626; background: #dc2626; }
.flow-sep { height: 2px; width: 1.25rem; background: #e5e7eb; }
.flow-sep.done { background: #465fff; }
.status-btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; }
.status-btn:disabled { opacity: 0.55; cursor: not-allowed; }
.status-btn-primary { background: #465fff; color: #fff; }
.status-btn-primary:hover:not(:disabled) { background: #3641f5; }
.status-btn-danger { border: 1px solid #fecaca; background: #fef2f2; color: #dc2626; }
.status-btn-danger:hover:not(:disabled) { background: #fee2e2; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(9.5rem, 1fr)); gap: 0.75rem; }
.image-card { overflow: hidden; border-radius: 0.75rem; border: 1px solid #e5e7eb; background: #fff; }
.image-open { display: block; width: 100%; aspect-ratio: 1 / 1; overflow: hidden; }
.image-thumb { height: 100%; width: 100%; object-fit: cover; transition: transform 0.2s; }
.image-open:hover .image-thumb { transform: scale(1.04); }
.image-meta { display: flex; align-items: center; justify-content: space-between; gap: 0.5rem; padding: 0.4rem 0.6rem; font-size: 0.75rem; color: #4b5563; }
.image-delete { display: inline-flex; flex-shrink: 0; height: 1.75rem; width: 1.75rem; align-items: center; justify-content: center; border-radius: 0.375rem; color: #dc2626; }
.image-delete:hover { background: #fef2f2; }
.preview-box { position: relative; max-width: min(90vw, 1100px); border-radius: 1rem; background: #111827; padding: 0.75rem; }
.preview-img { display: block; max-height: 80vh; max-width: 100%; margin: 0 auto; border-radius: 0.5rem; object-fit: contain; min-height: 12rem; min-width: 12rem; }
.preview-name { margin-top: 0.5rem; text-align: center; font-size: 0.8125rem; color: #d1d5db; }
.preview-close { position: absolute; top: -0.75rem; right: -0.75rem; display: inline-flex; height: 2rem; width: 2rem; align-items: center; justify-content: center; border-radius: 9999px; background: #fff; color: #111827; box-shadow: 0 2px 8px rgb(0 0 0 / 25%); }
.dark .sub { color: #9ca3af; }
.dark .flow-step { border-color: #374151; background: #1f2937; color: #9ca3af; }
.dark .flow-step.done { border-color: rgb(70 95 255 / 45%); background: rgb(70 95 255 / 15%); color: #a5b4fc; }
.dark .flow-step.current { border-color: #465fff; background: #465fff; color: #fff; }
.dark .flow-step.current.cancelled { border-color: #dc2626; background: #dc2626; }
.dark .flow-sep { background: #374151; }
.dark .flow-sep.done { background: #465fff; }
.dark .status-btn-danger { border-color: rgb(248 113 113 / 40%); background: rgb(127 29 29 / 30%); color: #fca5a5; }
.dark .status-btn-danger:hover:not(:disabled) { background: rgb(127 29 29 / 50%); }
.dark .image-card { border-color: #1f2937; background: #111827; }
.dark .image-meta { color: #d1d5db; }
.dark .image-delete { color: #f87171; }
.dark .image-delete:hover { background: rgb(127 29 29 / 30%); }
</style>
