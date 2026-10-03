<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="pageTitle" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ pageHeading }}</h3>
          <p class="sub">{{ t('warehouseOrders.headerHint') }}</p>
        </div>
        <router-link to="/warehouse-orders" class="ghost">{{ t('common.back') }}</router-link>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="p-5">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.4]">
            {{ t('common.supplier') }}
            <SearchableSelect
              v-model="form.supplierId"
              :options="supplierOptions"
              :placeholder="t('warehouseOrders.selectSupplier')"
              :search-placeholder="t('warehouseOrders.searchByName')"
              :disabled="headerLocked"
            />
          </label>

          <label class="lbl min-w-0 flex-1">
            {{ t('common.warehouse') }}
            <select v-model.number="form.warehouseId" required class="field" :disabled="headerLocked">
              <option :value="0" disabled>{{ t('warehouseOrders.selectWarehouse') }}</option>
              <option v-for="w in activeWarehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            {{ t('common.date') }}
            <input v-model="form.arrivalDate" type="date" required class="field" :disabled="headerLocked" />
          </label>

          <label class="lbl min-w-0 flex-1">
            {{ t('common.comment') }}
            <input
              v-model="form.comment"
              type="text"
              class="field"
              :placeholder="t('warehouseOrders.commentOptional')"
              :disabled="headerLocked"
            />
          </label>
        </div>

        <div class="form-row mt-3">
          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            {{ t('warehouseOrders.currency') }}
            <select
              v-model="form.currency"
              class="field"
              :disabled="headerLocked || items.length > 0"
              :title="items.length > 0 ? t('warehouseOrders.currencyLocked') : undefined"
            >
              <option v-for="c in CURRENCIES" :key="c" :value="c">{{ t(`exchangeRates.currencies.${c}`) }}</option>
            </select>
          </label>
          <label v-if="isForeign" class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            {{ t('warehouseOrders.rate') }}
            <input
              :value="rateText"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="field"
              :disabled="isTransferred || (!!orderId && !isEditMode)"
              @input="onRateInput"
            />
          </label>
          <p v-if="isForeign" class="cur-hint">
            <span v-if="!(form.exchangeRate > 0)" class="text-amber-600">{{ t('warehouseOrders.rateMissing') }}</span>
            <span v-else>{{ t('warehouseOrders.currencyHint') }}</span>
          </p>
        </div>
        <p v-if="isTransferred" class="mt-3 text-xs text-amber-600">
          {{ t('warehouseOrders.transferredLocked') }}
        </p>
      </div>
    </div>

    <div class="card mt-4">
      <div class="head">
        <div>
          <h3 class="title">{{ t('warehouseOrders.items') }}</h3>
          <p class="sub">
            {{ t('warehouseOrders.kvmHint') }}
          </p>
        </div>
      </div>

      <div class="p-5 border-b border-gray-100">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.4]">
            {{ t('common.product') }}
            <SearchableSelect
              v-model="itemForm.goodsId"
              :options="goodsOptions"
              :placeholder="t('warehouseOrders.selectGoods')"
              :search-placeholder="t('warehouseOrders.searchGoods')"
              :disabled="isTransferred"
            />
          </label>

          <template v-if="isWindowGoods">
            <label class="lbl min-w-0 w-28 sm:flex-none">
              {{ t('warehouseOrders.widthCm') }}
              <input v-model.number="itemForm.width" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              {{ t('warehouseOrders.heightCm') }}
              <input v-model.number="itemForm.height" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl min-w-0 w-24 sm:flex-none">
              {{ t('common.count') }}
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              {{ t('warehouseOrders.kvm') }}
              <input :value="formatNum(computedKvm)" class="field" readonly />
            </label>
          </template>

          <label v-else class="lbl min-w-0 w-28 sm:flex-none">
            {{ t('common.count') }}
            <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
          </label>

          <label class="lbl min-w-0 w-40 sm:flex-none">
            {{ isWindowGoods ? t('warehouseOrders.arrivalPriceKvm') : t('warehouseOrders.arrivalPrice') }}
            <input
              :value="priceCostText"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="field"
              @input="onPriceCostInput"
            />
          </label>
          <label class="lbl min-w-0 w-40 sm:flex-none">
            {{ t('warehouseOrders.totalSum') }}
            <input :value="amt(computedSum)" class="field" readonly />
            <span v-if="isForeign && computedSum > 0 && form.exchangeRate > 0" class="base-eq">
              {{ t('warehouseOrders.baseEquivalent', { value: moneyIn(computedSum * form.exchangeRate, 'UZS', t('common.currency')) }) }}
            </span>
          </label>

          <div v-if="!isTransferred" class="flex items-end">
            <button type="button" class="btn" :disabled="saving || !canAddItem" @click="onAddItem">
              {{ saving ? '...' : t('warehouseOrders.addItem') }}
            </button>
          </div>
        </div>
        <p v-if="!headerReady" class="mt-2 text-xs text-amber-600">
          {{ t('warehouseOrders.headerRequiredHint') }}
        </p>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="th">#</th>
              <th class="th">{{ t('common.product') }}</th>
              <th class="th">{{ t('warehouseOrders.widthCm') }}</th>
              <th class="th">{{ t('warehouseOrders.heightCm') }}</th>
              <th class="th">{{ t('common.count') }}</th>
              <th class="th">{{ t('warehouseOrders.kvm') }}</th>
              <th class="th">{{ t('warehouseOrders.arrivalPrice') }}</th>
              <th class="th">{{ t('common.sum') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="displayItems.length === 0">
              <td colspan="9" class="empty">{{ t('warehouseOrders.noItemsYet') }}</td>
            </tr>
            <tr v-for="(row, idx) in displayItems" :key="row.id" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ row.goodsName }}</td>
              <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
              <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
              <td class="td">{{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}</td>
              <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
              <td class="td">{{ amt(row.priceCost) }}</td>
              <td class="td">{{ amt(row.sum) }}</td>
              <td class="td text-right">
                <button
                  v-if="!isTransferred"
                  type="button"
                  class="danger"
                  @click="onRemoveItem(row.id)"
                >
                  {{ t('common.delete') }}
                </button>
                <span v-else class="text-gray-400">—</span>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="displayItems.length">
            <tr class="border-t border-gray-200 bg-gray-50">
              <td class="td font-semibold" colspan="7">{{ t('common.total') }}</td>
              <td class="td font-semibold">
                {{ amt(itemsTotalSum) }}
                <div v-if="isForeign && form.exchangeRate > 0" class="base-eq">
                  {{ t('warehouseOrders.baseEquivalent', { value: moneyIn(itemsTotalSum * form.exchangeRate, 'UZS', t('common.currency')) }) }}
                </div>
              </td>
              <td class="td" />
            </tr>
          </tfoot>
        </table>
      </div>

      <div class="flex justify-end gap-2 border-t border-gray-100 p-5">
        <router-link to="/warehouse-orders" class="ghost">{{ t('common.cancel') }}</router-link>
        <button
          type="button"
          class="btn"
          :disabled="saving || !orderId || isTransferred || displayItems.length === 0"
          @click="onSave"
        >
          {{ saving ? '...' : t('common.save') }}
        </button>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import {
  createWarehouseOrder,
  createWarehouseOrderItem,
  deleteWarehouseOrderItem,
  fetchWarehouseOrder,
  fetchWarehouseOrderItems,
  updateWarehouseOrder,
  type WarehouseOrderItem,
} from '@/api/warehouseOrders'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchSuppliers, type Supplier } from '@/api/suppliers'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { amountToText, formatAmountInput, money } from '@/utils/format'
import { CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'
import { fetchCurrentRate } from '@/api/exchangeRates'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const warehouses = ref<Warehouse[]>([])
const suppliers = ref<Supplier[]>([])
const goods = ref<Goods[]>([])
const items = ref<WarehouseOrderItem[]>([])
const orderId = ref<number | null>(null)
const orderStatus = ref<string | null>(null)
const saving = ref(false)
const error = ref<string | null>(null)

const editOrderId = computed(() => {
  const id = Number(route.params.id)
  return Number.isFinite(id) && id > 0 ? id : null
})
const isEditMode = computed(() => editOrderId.value != null)
const isTransferred = computed(() => orderStatus.value === 'TRANSFERRED')
const headerLocked = computed(() => isTransferred.value || (!!orderId.value && !isEditMode.value))
const pageTitle = computed(() =>
  isEditMode.value ? t('warehouseOrders.editPageTitle') : t('warehouseOrders.createPageTitle'),
)
const pageHeading = computed(() =>
  isEditMode.value
    ? t('warehouseOrders.editHeading', { id: editOrderId.value })
    : t('warehouseOrders.createHeading'),
)

const form = reactive({
  supplierId: 0,
  warehouseId: 0,
  arrivalDate: todayLocal(),
  comment: '',
  currency: 'UZS' as CurrencyCode,
  exchangeRate: 0,
})

const isForeign = computed(() => form.currency !== 'UZS')
const rateText = ref('')

function amt(v?: number | null) {
  return isForeign.value ? moneyIn(v, form.currency) : money(v)
}

function onRateInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  rateText.value = text
  el.value = text
  form.exchangeRate = value
}

watch(
  () => form.currency,
  async (cur) => {
    if (cur === 'UZS' || form.exchangeRate > 0) return
    try {
      const rate = (await fetchCurrentRate(cur)).data?.rate
      if (rate && !(form.exchangeRate > 0)) {
        form.exchangeRate = Number(rate)
        rateText.value = amountToText(form.exchangeRate)
      }
    } catch {
      /* kurs yo'q — foydalanuvchi qo'lda kiritadi */
    }
  },
)

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
  // sm → m²: (eni_sm * boyi_sm * soni) / 10000
  return (Number(itemForm.width || 0) * Number(itemForm.height || 0) * Number(itemForm.count || 0)) / 10000
})

const computedSum = computed(() => {
  if (isWindowGoods.value) {
    return computedKvm.value * Number(itemForm.priceCost || 0)
  }
  return Number(itemForm.count || 0) * Number(itemForm.priceCost || 0)
})

const priceCostText = ref('')

function onPriceCostInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  priceCostText.value = text
  el.value = text
  itemForm.priceCost = value
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
  () =>
    form.supplierId > 0 &&
    form.warehouseId > 0 &&
    !!form.arrivalDate &&
    (!isForeign.value || form.exchangeRate > 0),
)

const canAddItem = computed(() => {
  if (!headerReady.value || itemForm.goodsId <= 0 || Number(itemForm.count) <= 0) return false
  if (!(Number(itemForm.priceCost) > 0)) return false
  if (isWindowGoods.value) {
    return Number(itemForm.width) > 0 && Number(itemForm.height) > 0
  }
  return true
})

const displayItems = computed(() =>
  items.value.map((it) => {
    const g = goods.value.find((x) => x.id === it.goodsId)
    const windowItem = isWindow(g)
    const width = it.weight != null ? Number(it.weight) : null
    const height = it.height != null ? Number(it.height) : null
    const count = Number(it.count || 0)
    const priceCost = Number(it.priceCost || 0)
    let pieces: number | null = it.pieceCount != null ? Number(it.pieceCount) : null
    if (pieces == null && windowItem && width && height && width > 0 && height > 0) {
      pieces = (count * 10000) / (width * height)
    }
    return {
      id: it.id,
      goodsName: it.goodsName || String(it.goodsId || ''),
      width,
      height,
      pieces,
      count,
      priceCost,
      isWindow: windowItem,
      sum: count * priceCost,
    }
  }),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))

function buildOrderPayload() {
  return {
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    arrivalDate: dateToApi(form.arrivalDate),
    comment: form.comment.trim() || undefined,
    serviceFee: 0,
    currency: form.currency,
    exchangeRate: isForeign.value ? form.exchangeRate : undefined,
  }
}

watch(
  () => itemForm.goodsId,
  (id) => {
    const g = goods.value.find((x) => x.id === id)
    if (!g) return
    const baseCost = Number(g.priceCost || 0)
    itemForm.priceCost = isForeign.value
      ? form.exchangeRate > 0
        ? Math.round((baseCost / form.exchangeRate) * 100) / 100
        : 0
      : baseCost
    itemForm.priceSelling = Number(g.priceSelling || 0)
    priceCostText.value = amountToText(itemForm.priceCost)
    itemForm.count = 1
    if (isWindow(g)) {
      itemForm.width = Number(g.width || 0)
      itemForm.height = Number(g.height || 0)
    } else {
      itemForm.width = 0
      itemForm.height = 0
    }
  },
)

function applyOrderToForm(order: {
  id?: number
  supplierId?: number
  warehouseId?: number
  arrivalDate?: string
  comment?: string
  orderStatus?: string
  currency?: CurrencyCode
  exchangeRate?: number
}) {
  orderId.value = order.id ?? null
  orderStatus.value = order.orderStatus || null
  form.supplierId = order.supplierId || 0
  form.warehouseId = order.warehouseId || 0
  form.arrivalDate = (order.arrivalDate || '').slice(0, 10) || todayLocal()
  form.comment = order.comment || ''
  form.exchangeRate = order.currency && order.currency !== 'UZS' ? Number(order.exchangeRate || 0) : 0
  rateText.value = amountToText(form.exchangeRate)
  form.currency = order.currency || 'UZS'
}

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
    if (editOrderId.value) {
      const res = await fetchWarehouseOrder(editOrderId.value)
      if (!res.data) throw new Error(t('warehouseOrders.notFound'))
      applyOrderToForm(res.data)
      await reloadItems()
    }
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function ensureOrder(): Promise<number> {
  if (orderId.value) return orderId.value
  if (!headerReady.value) throw new Error(t('warehouseOrders.headerRequired'))
  const res = await createWarehouseOrder(buildOrderPayload())
  const id = res.data?.id
  if (!id) throw new Error(t('warehouseOrders.orderNotCreated'))
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
    const windowMode = isWindowGoods.value
    await createWarehouseOrderItem({
      warehouseId: form.warehouseId,
      warehouseOrderId: id,
      supplierId: form.supplierId,
      goodsId: itemForm.goodsId,
      priceCost: Number(itemForm.priceCost),
      priceSelling: Number(itemForm.priceSelling || 0),
      count: Number(itemForm.count),
      weight: windowMode ? Number(itemForm.width) : undefined,
      height: windowMode ? Number(itemForm.height) : undefined,
      arrivalDate: dateToApi(form.arrivalDate),
    })
    itemForm.goodsId = 0
    itemForm.count = 1
    itemForm.width = 0
    itemForm.height = 0
    itemForm.priceCost = 0
    itemForm.priceSelling = 0
    priceCostText.value = ''
    await reloadItems()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onRemoveItem(id: number) {
  if (!confirm(t('warehouseOrders.removeItemConfirm'))) return
  try {
    await deleteWarehouseOrderItem(id)
    await reloadItems()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onSave() {
  if (!orderId.value || isTransferred.value) return
  saving.value = true
  error.value = null
  try {
    await updateWarehouseOrder(orderId.value, buildOrderPayload())
    void router.push(`/warehouse-orders?open=${orderId.value}`)
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
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
.field:disabled, .field[readonly] { background: #f9fafb; color: #374151; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; color: #374151; }
.danger { font-size: 0.8125rem; font-weight: 500; color: #dc2626; }
.cur-hint { flex: 1 1 16rem; align-self: center; font-size: 0.75rem; color: #6b7280; }
.base-eq { font-size: 0.6875rem; font-weight: 400; color: #6b7280; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
@media (max-width: 640px) {
  .form-row > * { flex: 1 1 100%; }
}
</style>
