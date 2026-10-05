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
              :placeholder="rateLoading ? t('exchangeRates.rateLoading') : '0'"
              class="field"
              :readonly="!canEditRate"
              :title="canEditRate ? t('warehouseOrders.rateEditHint') : isTransferred ? t('warehouseOrders.transferredLocked') : t('warehouseOrders.rateNoPermission')"
              @input="onRateInput"
              @change="persistRate"
            />
          </label>
          <p v-if="isForeign" class="cur-hint">
            <span v-if="!rateLoading && !(form.exchangeRate > 0)" class="text-amber-600">{{ t('warehouseOrders.rateMissing') }}</span>
            <template v-else>
              <span v-if="rateIsManual" class="text-amber-600">
                {{ t('warehouseOrders.rateManual', { rate: formatRate(cbuRate) }) }}
                <button v-if="canEditRate" type="button" class="link-btn" @click="resetRate">{{ t('warehouseOrders.rateReset') }}</button>.
              </span>
              <span v-else-if="cbuRateDate">{{ t('exchangeRates.cbuRate') }} · {{ cbuRateDate.split('-').reverse().join('.') }}. </span>
              <span>{{ canEditRate ? t('warehouseOrders.rateEditHint') : t('warehouseOrders.currencyHint') }}</span>
              <span v-if="rateSaved" class="text-emerald-600"> {{ t('warehouseOrders.rateSaved') }}</span>
            </template>
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
          <label class="lbl product-col">
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
            <template v-if="isForeign">{{ isWindowGoods ? t('warehouseOrders.priceUsdKvm') : t('warehouseOrders.priceUsd') }}</template>
            <template v-else>{{ isWindowGoods ? t('warehouseOrders.arrivalPriceKvm') : t('warehouseOrders.arrivalPrice') }}</template>
            <input
              :value="priceCostText"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="field"
              @input="onPriceCostInput"
            />
          </label>
          <label v-if="isForeign" class="lbl min-w-0 w-40 sm:flex-none">
            {{ isWindowGoods ? t('warehouseOrders.priceSomKvm') : t('warehouseOrders.priceSom') }}
            <input
              :value="itemForm.priceCost > 0 && form.exchangeRate > 0 ? money(toSom(itemForm.priceCost)) : ''"
              class="field"
              placeholder="0"
              readonly
              tabindex="-1"
            />
          </label>
          <label class="lbl min-w-0 w-40 sm:flex-none">
            {{ isWindowGoods ? t('warehouseOrders.sellingPriceKvm') : t('warehouseOrders.sellingPrice') }}
            <input
              :value="sellingText"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="field"
              @input="onSellingInput"
            />
          </label>
          <label class="lbl min-w-0 w-28 sm:flex-none">
            {{ t('warehouseOrders.markupPercent') }}
            <div class="pct-field">
              <input
                :value="markupText"
                inputmode="decimal"
                autocomplete="off"
                placeholder="0"
                class="field"
                :class="{ 'text-red-600': markupNegative }"
                :disabled="!(itemForm.priceCost > 0)"
                @input="onMarkupInput"
              />
              <span class="pct-sign">%</span>
            </div>
          </label>
          <label class="lbl min-w-0 w-40 sm:flex-none">
            {{ t('warehouseOrders.totalSum') }}
            <input :value="amt(computedSum)" class="field" readonly />
            <span v-if="isForeign && computedSum > 0 && form.exchangeRate > 0" class="base-eq">
              {{ t('warehouseOrders.baseEquivalent', { value: moneyIn(Math.round(computedSum * form.exchangeRate * 100) / 100, 'UZS', t('common.currency')) }) }}
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
        <p v-else-if="itemForm.goodsId > 0 && itemForm.priceCost > 0 && !(itemForm.priceSelling > 0)" class="mt-2 text-xs text-amber-600">
          {{ t('warehouseOrders.sellingRequired') }}
        </p>
        <p v-else-if="markupNegative" class="mt-2 text-xs text-red-600">
          {{ t('warehouseOrders.sellingBelowCost') }}
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
              <th class="th">{{ isForeign ? t('warehouseOrders.colPriceUsd') : t('warehouseOrders.arrivalPrice') }}</th>
              <th v-if="isForeign" class="th">{{ t('warehouseOrders.colPriceSom') }}</th>
              <th class="th">{{ t('warehouseOrders.sellingPrice') }}</th>
              <th class="th">{{ t('warehouseOrders.markupPercent') }}</th>
              <th class="th">{{ isForeign ? t('warehouseOrders.colSumUsd') : t('common.sum') }}</th>
              <th v-if="isForeign" class="th">{{ t('warehouseOrders.colSumSom') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="displayItems.length === 0">
              <td :colspan="isForeign ? 13 : 11" class="empty">{{ t('warehouseOrders.noItemsYet') }}</td>
            </tr>
            <tr v-for="(row, idx) in displayItems" :key="row.id" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ row.goodsName }}</td>
              <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
              <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
              <td class="td">{{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}</td>
              <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
              <td class="td">{{ amt(row.priceCost) }}</td>
              <td v-if="isForeign" class="td">{{ money(row.priceSom) }}</td>
              <td class="td">{{ row.priceSelling > 0 ? amt(row.priceSelling) : '—' }}</td>
              <td class="td" :class="row.markup != null && row.markup < 0 ? 'text-red-600' : 'text-emerald-600'">
                {{ row.markup != null ? `${formatPct(row.markup)}%` : '—' }}
              </td>
              <td class="td">{{ amt(row.sum) }}</td>
              <td v-if="isForeign" class="td">{{ money(row.sumSom) }}</td>
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
              <td class="td font-semibold" :colspan="isForeign ? 10 : 9">{{ t('common.total') }}</td>
              <td class="td font-semibold">{{ amt(itemsTotalSum) }}</td>
              <td v-if="isForeign" class="td font-semibold">{{ moneyIn(itemsTotalSom, 'UZS', t('common.currency')) }}</td>
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
import { CURRENCIES, formatRate, moneyIn, type CurrencyCode } from '@/utils/currency'
import { useCbuRate } from '@/composables/useCbuRate'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
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
/** Saqlangan hujjat kursi: sana va valyuta o'zgarmasa yoki hujjat omborga o'tkazilgan bo'lsa server uni qoldiradi. */
const savedRate = ref<{ currency: CurrencyCode; rate: number; day: string } | null>(null)

const { rate: cbuRate, rateDate: cbuRateDate, loading: rateLoading } = useCbuRate(
  () => (isForeign.value ? form.currency : null),
  () => form.arrivalDate,
)

const keepsSavedRate = computed(() => {
  const saved = savedRate.value
  return (
    !!saved &&
    saved.rate > 0 &&
    saved.currency === form.currency &&
    (saved.day === form.arrivalDate || isTransferred.value)
  )
})

/** Kirimga ruxsati bor xodim kiritgan kurs; null - Markaziy bank (yoki saqlangan hujjat) kursi. */
const manualRate = ref<number | null>(null)
const rateText = ref('')
const rateSaved = ref(false)
const canEditRate = computed(
  () => !isTransferred.value && auth.can(orderId.value ? 'WAREHOUSE_ORDER_EDIT' : 'WAREHOUSE_ORDER_CREATE'),
)
const rateIsManual = computed(
  () => isForeign.value && cbuRate.value > 0 && form.exchangeRate > 0 && Math.abs(form.exchangeRate - cbuRate.value) >= 0.0001,
)

watch(
  [isForeign, manualRate, keepsSavedRate, cbuRate],
  () => {
    if (!isForeign.value) form.exchangeRate = 0
    else if (manualRate.value != null) form.exchangeRate = manualRate.value
    else form.exchangeRate = keepsSavedRate.value ? savedRate.value!.rate : cbuRate.value
  },
  { immediate: true },
)

watch(
  () => form.exchangeRate,
  (v) => {
    if (formatAmountInput(rateText.value).value !== v) rateText.value = v > 0 ? amountToText(v) : ''
  },
  { immediate: true },
)

function onRateInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  rateText.value = text
  el.value = text
  manualRate.value = value
  rateSaved.value = false
}

function resetRate() {
  manualRate.value = null
  savedRate.value = null
  void persistRate()
}

/** Hujjat yaratilgan bo'lsa yangi kurs darhol saqlanadi - pozitsiyalar so'mdagi qiymati shu kurs bilan hisoblanadi. */
async function persistRate() {
  if (!orderId.value || !canEditRate.value || !(form.exchangeRate > 0)) return
  error.value = null
  try {
    await updateWarehouseOrder(orderId.value, buildOrderPayload())
    savedRate.value = { currency: form.currency, rate: form.exchangeRate, day: form.arrivalDate }
    manualRate.value = null
    rateSaved.value = true
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function toSom(v: number) {
  return form.exchangeRate > 0 ? Math.round(v * form.exchangeRate * 100) / 100 : 0
}

function amt(v?: number | null) {
  return isForeign.value ? moneyIn(v, form.currency) : money(v)
}

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
const sellingText = ref('')
const markupText = ref('')
/** null - foiz kiritilmagan, sotish narxidan hisoblanadi. */
const markupPercent = ref<number | null>(null)

const round2 = (v: number) => Math.round(v * 100) / 100

function formatPct(v: number) {
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 }).format(v)
}

function markupOf(cost: number, selling: number) {
  return cost > 0 && selling > 0 ? round2(((selling - cost) / cost) * 100) : null
}

const markupNegative = computed(() => markupPercent.value != null && markupPercent.value < 0)

function setSelling(v: number) {
  itemForm.priceSelling = v > 0 ? round2(v) : 0
  sellingText.value = amountToText(itemForm.priceSelling)
}

function setMarkup(v: number | null) {
  markupPercent.value = v
  markupText.value = v == null ? '' : String(v)
}

function syncMarkupFromSelling() {
  setMarkup(markupOf(Number(itemForm.priceCost), Number(itemForm.priceSelling)))
}

function onPriceCostInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  priceCostText.value = text
  el.value = text
  itemForm.priceCost = value
  if (markupPercent.value != null && value > 0) setSelling(value * (1 + markupPercent.value / 100))
  else syncMarkupFromSelling()
}

function onSellingInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  sellingText.value = text
  el.value = text
  itemForm.priceSelling = value
  syncMarkupFromSelling()
}

function onMarkupInput(e: Event) {
  const el = e.target as HTMLInputElement
  const cleaned = el.value.replace(',', '.').replace(/[^\d.-]/g, '')
  markupText.value = cleaned
  el.value = cleaned
  const pct = cleaned === '' || cleaned === '-' ? null : Number(cleaned)
  markupPercent.value = pct != null && Number.isFinite(pct) ? pct : null
  if (markupPercent.value != null && itemForm.priceCost > 0) {
    setSelling(Number(itemForm.priceCost) * (1 + markupPercent.value / 100))
  }
}

/** Mahsulotning oxirgi sotish narxi kirim valyutasida. */
function goodsSellingIn(g: Goods) {
  const price = Number(g.priceSelling || 0)
  const from = g.priceCurrency || 'UZS'
  if (from === form.currency || price <= 0) return price
  if (!(form.exchangeRate > 0)) return 0
  return from === 'UZS' ? price / form.exchangeRate : price * form.exchangeRate
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
  if (!(Number(itemForm.priceCost) > 0) || !(Number(itemForm.priceSelling) > 0)) return false
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
    const priceSelling = Number(it.priceSelling || 0)
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
      priceSelling,
      markup: markupOf(priceCost, priceSelling),
      isWindow: windowItem,
      sum: count * priceCost,
      priceSom: toSom(priceCost),
      sumSom: Math.round(toSom(priceCost) * count * 100) / 100,
    }
  }),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))
const itemsTotalSom = computed(() => displayItems.value.reduce((acc, r) => acc + r.sumSom, 0))

function buildOrderPayload() {
  return {
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    arrivalDate: dateToApi(form.arrivalDate),
    comment: form.comment.trim() || undefined,
    serviceFee: 0,
    currency: form.currency,
    exchangeRate: isForeign.value && form.exchangeRate > 0 ? form.exchangeRate : undefined,
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
    priceCostText.value = amountToText(itemForm.priceCost)
    setSelling(goodsSellingIn(g))
    syncMarkupFromSelling()
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
  form.currency = order.currency || 'UZS'
  manualRate.value = null
  savedRate.value =
    order.currency && order.currency !== 'UZS'
      ? { currency: order.currency, rate: Number(order.exchangeRate || 0), day: form.arrivalDate }
      : null
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
      priceSelling: Number(itemForm.priceSelling),
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
    priceCostText.value = ''
    setSelling(0)
    setMarkup(null)
    await reloadItems()
    goods.value = (await fetchGoods()).data || goods.value
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
.product-col { flex: 1.4 1 14rem; min-width: 14rem; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.75rem; font-size: 0.875rem; }
.field:disabled, .field[readonly] { background: #f9fafb; color: #374151; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; color: #374151; }
.danger { font-size: 0.8125rem; font-weight: 500; color: #dc2626; }
.cur-hint { flex: 1 1 16rem; align-self: center; font-size: 0.75rem; color: #6b7280; }
.base-eq { font-size: 0.6875rem; font-weight: 400; color: #6b7280; }
.link-btn { font-weight: 500; color: #465fff; text-decoration: underline; }
.pct-field { position: relative; }
.pct-field .field { padding-right: 1.75rem; }
.pct-sign { position: absolute; right: 0.75rem; top: 50%; transform: translateY(-50%); font-size: 0.875rem; color: #9ca3af; pointer-events: none; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
@media (max-width: 640px) {
  .form-row > * { flex: 1 1 100%; }
}
</style>
