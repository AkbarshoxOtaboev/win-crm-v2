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
        <div class="form-row header-row">
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
            <select v-model.number="form.warehouseId" required class="field" :disabled="headerLocked || isTransferred">
              <option :value="0" disabled>{{ t('warehouseOrders.selectWarehouse') }}</option>
              <option v-for="w in activeWarehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 header-date">
            {{ t('common.date') }}
            <input v-model="form.arrivalDate" type="datetime-local" required class="field" :disabled="headerLocked" />
          </label>

          <div class="lbl min-w-0 header-currency">
            <span class="lbl-text" aria-hidden="true">&nbsp;</span>
            <div
              class="type-toggle cur-toggle"
              role="radiogroup"
              :aria-label="t('warehouseOrders.currency')"
              :title="items.length > 0 ? t('warehouseOrders.currencyLocked') : undefined"
            >
              <button
                v-for="c in CURRENCIES"
                :key="c"
                type="button"
                role="radio"
                class="type-btn"
                :class="{ active: form.currency === c }"
                :aria-checked="form.currency === c"
                :aria-label="t(`exchangeRates.currencies.${c}`)"
                :title="t(`exchangeRates.currencies.${c}`)"
                :disabled="headerLocked || items.length > 0"
                @click="form.currency = c"
              >
                <Banknote v-if="c === 'UZS'" class="cur-icon" />
                <DollarSign v-else class="cur-icon" />
              </button>
            </div>
          </div>

          <label v-if="isForeign" class="lbl min-w-0 header-rate">
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
        <div v-if="isForeign" class="form-row mt-3">
          <p class="cur-hint">
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
      </div>

      <div class="border-t border-gray-100 p-5">
        <div class="form-row">
          <div class="lbl product-col">
            <span class="lbl-text">{{ t('common.product') }}</span>
            <SearchableSelect
              v-model="itemForm.goodsId"
              :options="goodsOptions"
              :placeholder="t('warehouseOrders.selectGoods')"
              :search-placeholder="t('warehouseOrders.searchGoods')"
            />
            <span v-if="selectedGoods" class="field-note unit-hint">
              {{ t('warehouseOrders.unitLabel') }}: <b>{{ selectedUnit || '—' }}</b>
            </span>
          </div>

          <template v-if="isWindowGoods">
            <label class="lbl col-sm">
              <span class="lbl-text">{{ t('warehouseOrders.widthCm') }}</span>
              <input v-model.number="itemForm.width" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl col-sm">
              <span class="lbl-text">{{ t('warehouseOrders.heightCm') }}</span>
              <input v-model.number="itemForm.height" type="number" min="1" step="1" class="field" />
            </label>
          </template>

          <label class="lbl col-sm">
            <span class="lbl-text">{{ t('common.count') }}</span>
            <span class="pct-field">
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" :class="{ 'has-unit': selectedUnit }" />
              <span v-if="selectedUnit" class="unit-sign">{{ selectedUnit }}</span>
            </span>
          </label>

          <label v-if="isWindowGoods" class="lbl col-sm">
            <span class="lbl-text">{{ t('warehouseOrders.kvm') }}</span>
            <input :value="formatNum(computedKvm)" class="field" readonly tabindex="-1" />
          </label>

          <label class="lbl col-md">
            <span class="lbl-text" :title="priceCostLabel">{{ priceCostLabel }}</span>
            <input
              :value="priceCostText"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="field"
              @input="onPriceCostInput"
            />
          </label>
          <label class="lbl col-md" :class="{ 'total-with-cash': isWindowGoods }">
            <span class="lbl-text">{{ t('warehouseOrders.totalSum') }}</span>
            <span class="total-line">
              <input :value="amt(computedSum)" class="field total-field" readonly tabindex="-1" />
              <button
                v-if="isWindowGoods"
                type="button"
                class="cash-toggle"
                :class="{ on: showCashFields }"
                :aria-expanded="showCashFields"
                :aria-label="showCashFields ? t('warehouseOrders.hideCash') : t('warehouseOrders.showCash')"
                :title="showCashFields ? t('warehouseOrders.hideCash') : t('warehouseOrders.showCash')"
                @click="showCashFields = !showCashFields"
              >
                <ChevronRight class="cash-toggle-icon" />
              </button>
            </span>
          </label>
          <template v-if="isWindowGoods && showCashFields">
            <label class="lbl col-md">
              <span class="lbl-text">{{ t('warehouseOrders.cashSum') }}</span>
              <input
                :value="cashSumText"
                inputmode="decimal"
                autocomplete="off"
                placeholder="0"
                class="field"
                @input="onCashSumInput"
              />
            </label>
            <label class="lbl col-md">
              <span class="lbl-text">{{ t('warehouseOrders.cashTotal') }}</span>
              <input :value="amt(computedCashTotal)" class="field total-field" readonly tabindex="-1" />
            </label>
            <label class="lbl col-md">
              <span class="lbl-text">{{ t('warehouseOrders.cashPlusTotal') }}</span>
              <input :value="amt(computedCashPlusTotal)" class="field total-field" readonly tabindex="-1" />
            </label>
          </template>
          <label class="lbl col-md">
            <span class="lbl-text" :title="sellingLabel">{{ sellingLabel }}</span>
            <input
              :value="sellingText"
              inputmode="decimal"
              autocomplete="off"
              placeholder="0"
              class="field"
              :class="{ 'text-red-600': sellingBelowCost }"
              @input="onSellingInput"
            />
          </label>

          <div class="lbl col-btn">
            <span class="lbl-text" aria-hidden="true">&nbsp;</span>
            <span class="inline-flex items-center gap-2">
              <button
                v-if="editingItemId"
                type="button"
                class="ghost"
                :disabled="saving"
                @click="resetItemForm"
              >
                {{ t('warehouseOrders.cancelAction') }}
              </button>
              <button type="button" class="btn" :disabled="saving || !canAddItem" @click="onAddItem">
                {{ saving ? '...' : editingItemId ? t('common.save') : t('warehouseOrders.addItem') }}
              </button>
            </span>
          </div>
        </div>
        <p v-if="!headerReady" class="mt-2 text-xs text-amber-600">
          {{ t('warehouseOrders.headerRequiredHint') }}
        </p>
        <p v-else-if="itemForm.goodsId > 0 && itemForm.priceCost > 0 && !(itemForm.priceSelling > 0)" class="mt-2 text-xs text-amber-600">
          {{ t('warehouseOrders.sellingRequired') }}
        </p>
        <p v-else-if="sellingBelowCost" class="mt-2 text-xs text-red-600">
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
              <th class="th">{{ isForeign ? t('warehouseOrders.colSumUsd') : t('common.sum') }}</th>
              <th class="th">{{ t('warehouseOrders.cashSum') }}</th>
              <th class="th">{{ t('warehouseOrders.cashTotal') }}</th>
              <th class="th">{{ t('warehouseOrders.cashPlusTotal') }}</th>
              <th class="th">{{ t('warehouseOrders.sellingPrice') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="displayItems.length === 0">
              <td colspan="13" class="empty">{{ t('warehouseOrders.noItemsYet') }}</td>
            </tr>
            <tr v-for="(row, idx) in displayItems" :key="row.id" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ row.goodsName }}</td>
              <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
              <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
              <td class="td whitespace-nowrap">
                {{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}
                <span v-if="row.unitName" class="text-gray-400">{{ row.unitName }}</span>
              </td>
              <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
              <td class="td">{{ amt(row.priceCost) }}</td>
              <td class="td">{{ amt(row.sum) }}</td>
              <td class="td">{{ row.cashSum > 0 ? amt(row.cashSum) : '—' }}</td>
              <td class="td">{{ row.cashTotal > 0 ? amt(row.cashTotal) : '—' }}</td>
              <td class="td">{{ amt(row.cashPlus) }}</td>
              <td class="td">{{ row.priceSelling > 0 ? amt(row.priceSelling) : '—' }}</td>
              <td class="td text-right">
                <div class="inline-flex items-center justify-end gap-2">
                  <button
                    type="button"
                    class="icon-btn edit-btn"
                    :aria-label="t('common.edit')"
                    :title="t('common.edit')"
                    @click="onEditItem(row.id)"
                  >
                    <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M8 20l10.5 -10.5a2.828 2.828 0 1 0 -4 -4l-10.5 10.5v4h4z" />
                      <path d="M13.5 6.5l4 4" />
                      <path d="M16 18h4m-2 -2v4" />
                    </svg>
                  </button>
                  <button
                    type="button"
                    class="icon-btn trash-btn"
                    :aria-label="t('common.delete')"
                    :title="t('common.delete')"
                    @click="onRemoveItem(row.id, row.goodsName)"
                  >
                    <svg class="row-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M4 7l16 0" />
                      <path d="M10 11l0 6" />
                      <path d="M14 11l0 6" />
                      <path d="M5 7l1 12a2 2 0 0 0 2 2h8a2 2 0 0 0 2 -2l1 -12" />
                      <path d="M9 7v-3a1 1 0 0 1 1 -1h4a1 1 0 0 1 1 1v3" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="displayItems.length">
            <tr class="border-t border-gray-200 bg-gray-50">
              <td class="td font-semibold" colspan="7">{{ t('common.total') }}</td>
              <td class="td font-semibold">{{ amt(itemsTotalSum) }}</td>
              <td class="td" />
              <td class="td font-semibold">{{ amt(itemsTotalCash) }}</td>
              <td class="td font-semibold">{{ amt(itemsTotalCashPlus) }}</td>
              <td class="td" />
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
          :disabled="saving || !orderId || displayItems.length === 0"
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
import { Banknote, ChevronRight, DollarSign } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import Swal from 'sweetalert2'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import {
  createWarehouseOrder,
  createWarehouseOrderItem,
  updateWarehouseOrderItem,
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
const headerLocked = computed(() => !!orderId.value && !isEditMode.value)
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
  arrivalDate: nowLocal(),
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
    (saved.day.slice(0, 10) === form.arrivalDate.slice(0, 10) || isTransferred.value)
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
const editingItemId = ref<number | null>(null)
const skipGoodsWatch = ref(false)

function nowLocal() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function toLocalInput(value?: string | null) {
  if (!value) return nowLocal()
  const normalized = value.replace(' ', 'T')
  if (normalized.length >= 16) return normalized.slice(0, 16)
  return `${normalized.slice(0, 10)}T00:00`
}

function dateToApi(date: string) {
  if (date.length === 16) return `${date}:00`
  if (date.length === 10) return `${date}T${nowLocal().slice(11, 16)}:00`
  return date
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
const selectedUnit = computed(() => selectedGoods.value?.unitTypeName || '')
const priceCostLabel = computed(() => {
  if (isForeign.value) return t(isWindowGoods.value ? 'warehouseOrders.priceUsdKvm' : 'warehouseOrders.priceUsd')
  return t(isWindowGoods.value ? 'warehouseOrders.arrivalPriceKvm' : 'warehouseOrders.arrivalPrice')
})
const sellingLabel = computed(() =>
  t(isWindowGoods.value ? 'warehouseOrders.sellingPriceKvm' : 'warehouseOrders.sellingPrice'),
)

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
const cashSumText = ref('')
const cashSum = ref(0)
const showCashFields = ref(false)

const round2 = (v: number) => Math.round(v * 100) / 100

/** Naqd jami = naqd summa × kv.m */
const computedCashTotal = computed(() => round2(Number(cashSum.value || 0) * computedKvm.value))
/** Naqd jami + umumiy summa */
const computedCashPlusTotal = computed(() => round2(computedCashTotal.value + Number(computedSum.value || 0)))

const sellingBelowCost = computed(
  () => itemForm.priceCost > 0 && itemForm.priceSelling > 0 && itemForm.priceSelling < itemForm.priceCost,
)

function setSelling(v: number) {
  itemForm.priceSelling = v > 0 ? round2(v) : 0
  sellingText.value = amountToText(itemForm.priceSelling)
}

function onPriceCostInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  priceCostText.value = text
  el.value = text
  itemForm.priceCost = value
}

function onSellingInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  sellingText.value = text
  el.value = text
  itemForm.priceSelling = value
}

function onCashSumInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  cashSumText.value = text
  el.value = text
  cashSum.value = value
}

function resetCashSum() {
  cashSum.value = 0
  cashSumText.value = ''
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
    .filter((g) => (!g.status || g.status === 'ACTIVE') && (g.type || '').toUpperCase() !== 'SERVICE')
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
      unitName: g?.unitTypeName || '',
      width,
      height,
      pieces,
      count,
      priceCost,
      priceSelling,
      isWindow: windowItem,
      sum: count * priceCost,
      cashSum: Number(it.cashPrice || 0),
      cashTotal: count * Number(it.cashPrice || 0),
      cashPlus: count * (priceCost + Number(it.cashPrice || 0)),
    }
  }),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))
const itemsTotalCash = computed(() => displayItems.value.reduce((acc, r) => acc + r.cashTotal, 0))
const itemsTotalCashPlus = computed(() => displayItems.value.reduce((acc, r) => acc + r.cashPlus, 0))

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
    if (skipGoodsWatch.value) return
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
    resetCashSum()
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
  form.arrivalDate = toLocalInput(order.arrivalDate)
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
    const payload = {
      warehouseId: form.warehouseId,
      warehouseOrderId: id,
      supplierId: form.supplierId,
      goodsId: itemForm.goodsId,
      priceCost: Number(itemForm.priceCost),
      priceSelling: Number(itemForm.priceSelling),
      cashPrice: windowMode ? Number(cashSum.value || 0) : 0,
      count: Number(itemForm.count),
      weight: windowMode ? Number(itemForm.width) : undefined,
      height: windowMode ? Number(itemForm.height) : undefined,
      arrivalDate: dateToApi(form.arrivalDate),
    }
    if (editingItemId.value) await updateWarehouseOrderItem(editingItemId.value, payload)
    else await createWarehouseOrderItem(payload)
    editingItemId.value = null
    itemForm.goodsId = 0
    itemForm.count = 1
    itemForm.width = 0
    itemForm.height = 0
    itemForm.priceCost = 0
    priceCostText.value = ''
    setSelling(0)
    resetCashSum()
    await reloadItems()
    goods.value = (await fetchGoods()).data || goods.value
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

function resetItemForm() {
  editingItemId.value = null
  itemForm.goodsId = 0
  itemForm.count = 1
  itemForm.width = 0
  itemForm.height = 0
  itemForm.priceCost = 0
  priceCostText.value = ''
  setSelling(0)
  resetCashSum()
  showCashFields.value = false
}

function onEditItem(id: number) {
  const it = items.value.find((row) => row.id === id)
  if (!it?.goodsId) return
  const g = goods.value.find((row) => row.id === it.goodsId)
  const windowItem = isWindow(g)
  skipGoodsWatch.value = true
  editingItemId.value = it.id
  itemForm.goodsId = it.goodsId
  itemForm.priceCost = Number(it.priceCost || 0)
  priceCostText.value = amountToText(itemForm.priceCost)
  setSelling(Number(it.priceSelling || 0))
  const cash = Number(it.cashPrice || 0)
  cashSum.value = cash
  cashSumText.value = cash > 0 ? amountToText(cash) : ''
  showCashFields.value = windowItem && cash > 0
  if (windowItem) {
    itemForm.width = Number(it.weight || 0)
    itemForm.height = Number(it.height || 0)
    const pieces = it.pieceCount != null ? Number(it.pieceCount) : 0
    if (pieces > 0) itemForm.count = pieces
    else if (itemForm.width > 0 && itemForm.height > 0) {
      itemForm.count = (Number(it.count || 0) * 10000) / (itemForm.width * itemForm.height)
    } else itemForm.count = 1
  } else {
    itemForm.count = Number(it.count || 1)
    itemForm.width = 0
    itemForm.height = 0
  }
  skipGoodsWatch.value = false
}

async function onRemoveItem(id: number, name: string) {
  const result = await Swal.fire({
    title: t('warehouseOrders.removeItemConfirm'),
    text: t('warehouseOrders.removeItemText', { name }),
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
    await deleteWarehouseOrderItem(id)
    if (editingItemId.value === id) resetItemForm()
    await reloadItems()
    await Swal.fire({
      title: t('warehouseOrders.itemRemoved'),
      icon: 'success',
      timer: 1500,
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

async function onSave() {
  if (!orderId.value) return
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
.form-row { display: flex; flex-wrap: wrap; align-items: flex-start; gap: 0.75rem; }
.header-row { flex-wrap: nowrap; }
.header-date { flex: 0 0 14.5rem; }
.header-currency { flex: 0 0 auto; }
.header-rate { flex: 0 0 9rem; }
.type-toggle { display: inline-grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.15rem; border-radius: 0.6rem; background: #f2f4f7; padding: 0.15rem; width: auto; }
.type-btn { display: inline-flex; height: 2.2rem; width: 2.2rem; align-items: center; justify-content: center; border-radius: 0.45rem; padding: 0; color: #667085; transition: background-color 0.15s, color 0.15s, box-shadow 0.15s; }
.cur-icon { width: 1rem; height: 1rem; }
.type-btn:hover:not(:disabled):not(.active) { color: #344054; background: rgb(255 255 255 / 60%); }
.type-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.12); }
.type-btn:disabled { cursor: not-allowed; }
.type-btn:disabled:not(.active) { opacity: 0.5; }
.dark .type-toggle { background: rgb(255 255 255 / 5%); }
.dark .type-btn { color: #98a2b3; }
.dark .type-btn:hover:not(:disabled):not(.active) { color: #e4e7ec; background: rgb(255 255 255 / 5%); }
.dark .type-btn.active { background: #1d2939; color: #9cb0ff; }
.product-col { flex: 2.2 1 15rem; min-width: 15rem; }
.col-sm { flex: 1 1 6.5rem; min-width: 6.5rem; }
.col-md { flex: 1.3 1 8.5rem; min-width: 8.5rem; }
.col-btn { flex: 0 0 auto; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.lbl-text { display: block; height: 1rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; line-height: 1rem; }
.field-note { line-height: 1rem; }
.product-col :deep(.field-btn) { height: 2.5rem; }
.form-row input[type='number'] { appearance: textfield; -moz-appearance: textfield; }
.form-row input[type='number']::-webkit-inner-spin-button,
.form-row input[type='number']::-webkit-outer-spin-button { margin: 0; -webkit-appearance: none; }
.total-field { font-weight: 600; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.75rem; font-size: 0.875rem; }
.field:disabled, .field[readonly] { background: #f9fafb; color: #374151; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.ghost { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; color: #374151; }
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  flex: 0 0 auto;
  border-radius: 0.5rem;
  background: transparent;
  border: 1px solid currentColor;
}
.row-icon { width: 1rem; height: 1rem; }
.edit-btn { color: #f79009; }
.edit-btn:hover { background: #fff7ed; }
.trash-btn { color: #dc2626; }
.trash-btn:hover { background: #fef2f2; }
.cur-hint { flex: 1 1 16rem; align-self: center; font-size: 0.75rem; color: #6b7280; }
.base-eq { font-size: 0.6875rem; font-weight: 400; color: #6b7280; }
.link-btn { font-weight: 500; color: #465fff; text-decoration: underline; }
.pct-field { position: relative; display: block; }
.pct-field .field { padding-right: 1.75rem; }
.pct-field .field.has-unit { padding-right: 3rem; }
.total-with-cash { flex: 1.8 1 13rem; min-width: 13rem; }
.total-line { display: flex; align-items: center; gap: 0.4rem; }
.total-line .field { flex: 1; min-width: 0; }
.cash-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  flex: 0 0 auto;
  border-radius: 0.5rem;
  border: 1px solid #c7d2fe;
  background: #eef2ff;
  color: #465fff;
  cursor: pointer;
}
.cash-toggle-icon { width: 1.15rem; height: 1.15rem; transition: transform 0.15s ease; }
.cash-toggle.on { background: #465fff; border-color: #465fff; color: #fff; }
.cash-toggle.on .cash-toggle-icon { transform: rotate(90deg); }
.unit-sign { position: absolute; right: 0.6rem; top: 50%; max-width: 2.6rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; transform: translateY(-50%); font-size: 0.75rem; font-weight: 500; color: #6b7280; pointer-events: none; }
.unit-hint { font-size: 0.75rem; font-weight: 400; color: #6b7280; }
.unit-hint b { font-weight: 600; color: #465fff; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
@media (max-width: 640px) {
  .form-row:not(.header-row) > * { flex: 1 1 100%; }
  .header-row { overflow-x: auto; }
  .header-row > * { flex: 1 0 9rem; }
}
</style>
