<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Yangi savdo" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">Yangi savdo buyurtmasi</h3>
          <p class="sub">
            Mijoz, ombor, sotuvchi va buyurtma summasini kiriting, pozitsiyalarni qo‘shing.
            Tovarlar ombordan faqat «Saqlash» bosilganda ayiriladi.
          </p>
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
              />
              <button
                type="button"
                class="ghost client-add"
                title="Yangi mijoz"
                @click="openClientModal"
              >
                + Yangi mijoz
              </button>
            </div>
          </label>

          <label class="lbl min-w-0 flex-1">
            Ombor
            <select
              v-model.number="form.warehouseId"
              required
              class="field"
              :disabled="draftItems.length > 0"
              :title="draftItems.length > 0 ? 'Omborni almashtirish uchun avval pozitsiyalarni o‘chiring' : undefined"
            >
              <option :value="0" disabled>Omborni tanlang</option>
              <option v-for="w in activeWarehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 flex-1">
            Sotuvchi
            <select v-model.number="form.userId" required class="field">
              <option :value="0" disabled>Sotuvchini tanlang</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            Sana
            <input v-model="form.orderDate" type="date" required class="field" />
          </label>

          <label class="lbl min-w-0 flex-1">
            Izoh
            <input
              v-model="form.comment"
              type="text"
              class="field"
              placeholder="Izoh (ixtiyoriy)"
            />
          </label>
        </div>

        <div class="form-row mt-3">
          <label class="lbl min-w-0 w-full sm:w-72 sm:flex-none">
            Buyurtma summasi *
            <input
              :value="totalSumText"
              inputmode="decimal"
              class="field total-field"
              placeholder="0"
              @input="onTotalSumInput"
            />
          </label>
          <p class="total-hint">
            Mijoz bilan kelishilgan summa. Pozitsiyalar summasiga bog‘liq emas.
          </p>
        </div>

        <div v-if="form.clientId > 0" class="client-summary">
          <div class="summary-item">
            <span class="summary-label">Umumiy buyurtma summasi</span>
            <span class="summary-value">{{ balanceText(clientBalance?.totalPurchase) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">To‘lov summasi</span>
            <span class="summary-value paid">{{ balanceText(clientBalance?.totalPaid) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ Number(clientBalance?.totalDebt || 0) < 0 ? 'Haqdorlik (ortiqcha to‘lov)' : 'Qarz' }}</span>
            <span class="summary-value" :class="Number(clientBalance?.totalDebt || 0) > 0 ? 'debt' : 'paid'">
              {{ balanceText(clientBalance ? Math.abs(Number(clientBalance.totalDebt || 0)) : undefined) }}
            </span>
          </div>
          <p v-if="balanceError" class="summary-note">{{ balanceError }}</p>
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
              + Qo‘shish
            </button>
          </div>
        </div>
        <p v-if="!headerReady" class="mt-2 text-xs text-amber-600">
          Avval mijoz, ombor, sotuvchi va sanani tanlang
        </p>
        <p v-else-if="selectedGoods && !isServiceGoods" class="stock-hint">
          Omborda qolgan: {{ formatNum(remainingStock(selectedGoods.id)) }}{{ isWindowGoods ? ' kv.m' : '' }}
        </p>
        <p v-if="itemError" class="mt-2 text-xs text-red-600">{{ itemError }}</p>
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
            <tr v-for="(row, idx) in displayItems" :key="row.key" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ row.goodsName }}</td>
              <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
              <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
              <td class="td">{{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}</td>
              <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
              <td class="td">{{ money(row.priceSelling) }}</td>
              <td class="td">{{ money(row.sum) }}</td>
              <td class="td text-right">
                <button type="button" class="danger" :disabled="saving" @click="onRemoveItem(row.key)">O‘chirish</button>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="displayItems.length">
            <tr class="border-t border-gray-200 bg-gray-50">
              <td class="td font-semibold" colspan="7">Pozitsiyalar jami</td>
              <td class="td font-semibold">{{ money(itemsTotalSum) }}</td>
              <td class="td" />
            </tr>
          </tfoot>
        </table>
      </div>

      <div class="save-bar border-t border-gray-100 p-5">
        <div class="save-info">
          <span v-if="saveBlockReason" class="save-warn">{{ saveBlockReason }}</span>
          <template v-else>
            <span class="save-label">Buyurtma summasi:</span>
            <span class="save-total">{{ money(form.totalSum) }}</span>
          </template>
        </div>
        <div class="flex gap-2">
          <router-link to="/sales" class="ghost">Bekor</router-link>
          <button type="button" class="btn" :disabled="saving || !!saveBlockReason" @click="onSave">
            {{ saving ? '...' : 'Saqlash' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="clientModal" class="overlay">
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
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import { createSaleOrder } from '@/api/sales'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { createClient, fetchClients, type Client } from '@/api/clients'
import { fetchClientBalance, type ClientBalance } from '@/api/clientBalances'
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

interface DraftItem {
  key: number
  goodsId: number
  goodsName: string
  isWindow: boolean
  isService: boolean
  width: number | null
  height: number | null
  pieces: number | null
  count: number
  priceCost: number
  priceSelling: number
}

const draftItems = ref<DraftItem[]>([])
let draftSeq = 0
const saving = ref(false)
const saved = ref(false)
const error = ref<string | null>(null)
const itemError = ref<string | null>(null)
const clientModal = ref(false)
const clientSaving = ref(false)
const clientError = ref<string | null>(null)
const clientBalance = ref<ClientBalance | null>(null)
const balanceLoading = ref(false)
const balanceError = ref<string | null>(null)
let balanceRequest = 0

const form = reactive({
  clientId: 0,
  warehouseId: 0,
  userId: 0,
  orderDate: todayLocal(),
  comment: '',
  totalSum: 0,
})

const totalSumText = ref('')

function onTotalSumInput(e: Event) {
  const el = e.target as HTMLInputElement
  const cleaned = el.value.replace(/\s/g, '').replace(',', '.').replace(/[^\d.]/g, '')
  const [intPart = '', ...rest] = cleaned.split('.')
  const fraction = rest.join('').slice(0, 2)
  const hasDot = cleaned.includes('.')
  const grouped = intPart.replace(/^0+(?=\d)/, '').replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  totalSumText.value = hasDot ? `${grouped}.${fraction}` : grouped
  el.value = totalSumText.value
  form.totalSum = Number(`${intPart || '0'}.${fraction || '0'}`)
}

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

function isService(g?: Goods | null) {
  return (g?.type || '').toUpperCase() === 'SERVICE'
}

const selectedGoods = computed(() => goods.value.find((g) => g.id === itemForm.goodsId) || null)
const isWindowGoods = computed(() => isWindow(selectedGoods.value))
const isServiceGoods = computed(() => isService(selectedGoods.value))

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
  if (isService(g)) return true
  return (stockByGoodsId.value.get(g.id) || 0) > 0
}

const availableByGoodsId = computed(() => {
  const map = new Map<number, number>()
  for (const s of stocks.value) {
    if (!s.goodsId) continue
    map.set(s.goodsId, (map.get(s.goodsId) || 0) + Number(s.count || 0))
  }
  return map
})

function remainingStock(goodsId: number) {
  const reserved = draftItems.value
    .filter((d) => d.goodsId === goodsId)
    .reduce((acc, d) => acc + d.count, 0)
  return (availableByGoodsId.value.get(goodsId) || 0) - reserved
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
  draftItems.value.map((d) => ({ ...d, sum: d.count * d.priceSelling })),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))

const saveBlockReason = computed(() => {
  if (!headerReady.value) return 'Mijoz, ombor, sotuvchi va sanani tanlang'
  if (!(form.totalSum > 0)) return 'Buyurtma summasini kiriting'
  if (draftItems.value.length === 0) return 'Kamida bitta pozitsiya qo‘shing'
  return null
})

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

function balanceText(v?: number | null) {
  if (balanceLoading.value && !clientBalance.value) return '...'
  if (v == null) return '—'
  return money(v)
}

async function loadClientBalance() {
  const clientId = form.clientId
  const current = ++balanceRequest
  balanceError.value = null
  if (!clientId) {
    clientBalance.value = null
    return
  }
  balanceLoading.value = true
  try {
    const res = await fetchClientBalance(clientId, { fromDate: '2000-01-01', toDate: '2100-12-31' })
    if (current === balanceRequest) clientBalance.value = res.data || null
  } catch (e) {
    if (current === balanceRequest) {
      clientBalance.value = null
      balanceError.value = formatApiError(e, 'Mijoz balansini yuklab bo‘lmadi')
    }
  } finally {
    if (current === balanceRequest) balanceLoading.value = false
  }
}

watch(
  () => form.clientId,
  () => {
    clientBalance.value = null
    void loadClientBalance()
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

function onAddItem() {
  if (!canAddItem.value || !selectedGoods.value) return
  itemError.value = null
  const g = selectedGoods.value
  const windowMode = isWindowGoods.value
  const selling = Number(itemForm.priceSelling)
  const pieces = Number(itemForm.count)
  const count = windowMode ? computedKvm.value : pieces

  if (!isService(g)) {
    const left = remainingStock(g.id)
    if (count > left + 1e-9) {
      itemError.value = `Omborda yetarli emas: qolgan ${formatNum(Math.max(left, 0))}${windowMode ? ' kv.m' : ''}, so‘ralgan ${formatNum(count)}${windowMode ? ' kv.m' : ''}`
      return
    }
  }

  draftItems.value.push({
    key: ++draftSeq,
    goodsId: g.id,
    goodsName: g.name,
    isWindow: windowMode,
    isService: isService(g),
    width: windowMode ? Number(itemForm.width) : null,
    height: windowMode ? Number(itemForm.height) : null,
    pieces: windowMode ? pieces : null,
    count,
    priceCost: windowMode ? selling : Number(itemForm.priceCost),
    priceSelling: selling,
  })
  itemForm.goodsId = 0
  itemForm.count = 1
  itemForm.width = 0
  itemForm.height = 0
  itemForm.priceCost = 0
  itemForm.priceSelling = 0
}

function onRemoveItem(key: number) {
  draftItems.value = draftItems.value.filter((d) => d.key !== key)
  itemError.value = null
}

async function onSave() {
  if (saving.value || saveBlockReason.value) return
  saving.value = true
  error.value = null
  try {
    const res = await createSaleOrder({
      warehouseId: form.warehouseId,
      userId: form.userId,
      orderDate: dateToApi(form.orderDate),
      totalSum: form.totalSum,
      clientId: form.clientId,
      comment: form.comment.trim() || undefined,
      items: draftItems.value.map((d) => ({
        goodsId: d.goodsId,
        priceCost: d.priceCost,
        priceSelling: d.priceSelling,
        count: d.isWindow ? Number(d.pieces) : d.count,
        width: d.width ?? undefined,
        height: d.height ?? undefined,
      })),
    })
    const id = res.data?.id
    if (!id) throw new Error('Savdo yaratilmadi')
    saved.value = true
    void router.push(`/sales/${id}`)
  } catch (e) {
    error.value = formatApiError(e)
    window.scrollTo({ top: 0, behavior: 'smooth' })
    if (form.warehouseId) {
      fetchStocksByWarehouse(form.warehouseId)
        .then((r) => (stocks.value = r.data || []))
        .catch(() => {})
    }
  } finally {
    saving.value = false
  }
}

onBeforeRouteLeave(() => {
  if (saved.value || draftItems.value.length === 0) return true
  return confirm('Saqlanmagan pozitsiyalar bor. Sahifadan chiqilsinmi?')
})

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
.client-summary { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0.75rem; margin-top: 1rem; }
.summary-item { display: flex; flex-direction: column; gap: 0.25rem; border-radius: 0.75rem; border: 1px solid #e5e7eb; background: #f9fafb; padding: 0.75rem 1rem; }
.summary-label { font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.summary-value { font-size: 1.125rem; font-weight: 700; color: #1f2937; }
.summary-value.paid { color: #059669; }
.summary-value.debt { color: #dc2626; }
.summary-note { grid-column: 1 / -1; font-size: 0.75rem; color: #b45309; }
.dark .summary-item { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .summary-label { color: #9ca3af; }
.dark .summary-value { color: rgba(255, 255, 255, 0.92); }
.dark .summary-value.paid { color: #34d399; }
.dark .summary-value.debt { color: #f87171; }
.dark .summary-note { color: #fbbf24; }
.total-field { font-size: 1rem; font-weight: 600; }
.total-hint { align-self: center; font-size: 0.75rem; color: #6b7280; }
.stock-hint { margin-top: 0.5rem; font-size: 0.75rem; color: #6b7280; }
.save-bar { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.75rem; }
.save-info { display: flex; align-items: baseline; gap: 0.5rem; font-size: 0.875rem; }
.save-label { color: #6b7280; }
.save-total { font-size: 1.125rem; font-weight: 700; color: #1f2937; }
.save-warn { font-size: 0.8125rem; color: #b45309; }
.danger:disabled { opacity: 0.5; cursor: not-allowed; }
.dark .total-hint, .dark .stock-hint, .dark .save-label { color: #9ca3af; }
.dark .save-total { color: rgba(255, 255, 255, 0.92); }
.dark .save-warn { color: #fbbf24; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
@media (max-width: 640px) {
  .form-row > * { flex: 1 1 100%; }
  .client-summary { grid-template-columns: 1fr; }
  .client-row { flex-direction: column; }
}
</style>
