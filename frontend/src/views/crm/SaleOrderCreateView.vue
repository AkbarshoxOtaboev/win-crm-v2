<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('saleOrderCreate.pageTitle')" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ t('saleOrderCreate.title') }}</h3>
          <p class="sub">
            {{ t('saleOrderCreate.subtitle') }}
          </p>
        </div>
        <router-link to="/sales" class="ghost">{{ t('common.back') }}</router-link>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="p-5">
        <div class="form-row">
          <label class="lbl min-w-0 flex-[1.4]">
            {{ t('common.client') }}
            <div class="client-row">
              <SearchableSelect
                v-model="form.clientId"
                :options="clientOptions"
                :placeholder="t('saleOrderCreate.selectClient')"
                :search-placeholder="t('saleOrderCreate.clientSearch')"
              />
              <button
                type="button"
                class="ghost client-add"
                :title="t('saleOrderCreate.newClient')"
                @click="openClientModal"
              >
                {{ t('saleOrderCreate.addClient') }}
              </button>
            </div>
          </label>

          <label class="lbl min-w-0 flex-1">
            {{ t('common.warehouse') }}
            <select
              v-model.number="form.warehouseId"
              required
              class="field"
              :disabled="draftItems.length > 0"
              :title="draftItems.length > 0 ? t('saleOrderCreate.warehouseLocked') : undefined"
            >
              <option :value="0" disabled>{{ t('saleOrderCreate.selectWarehouse') }}</option>
              <option v-for="w in activeWarehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 flex-1">
            {{ t('saleOrderCreate.seller') }}
            <select v-model.number="form.userId" required class="field">
              <option :value="0" disabled>{{ t('saleOrderCreate.selectSeller') }}</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </label>

          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            {{ t('common.date') }}
            <input v-model="form.orderDate" type="date" required class="field" />
          </label>

          <div class="lbl min-w-0 flex-1">
            <label for="order-total-sum">{{ t('saleOrderCreate.orderSumRequired') }} ({{ curLabel }})</label>
            <div class="total-row">
              <input
                id="order-total-sum"
                :value="totalSumText"
                inputmode="decimal"
                class="field total-field"
                placeholder="0"
                @input="onTotalSumInput"
              />
              <button
                type="button"
                class="pull-total-btn"
                :disabled="!(itemsTotalSum > 0)"
                :title="t('saleOrderCreate.pullItemsTotal')"
                :aria-label="t('saleOrderCreate.pullItemsTotal')"
                @click="pullItemsTotal"
              >
                <Magnet class="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
        <p class="total-hint mt-1.5">{{ t('saleOrderCreate.orderSumHint') }}</p>

        <div class="form-row mt-3">
          <label class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            {{ t('saleOrderCreate.currency') }}
            <select
              v-model="form.currency"
              class="field"
              :disabled="draftItems.length > 0"
              :title="draftItems.length > 0 ? t('saleOrderCreate.currencyLocked') : undefined"
            >
              <option v-for="c in CURRENCIES" :key="c" :value="c">{{ t(`exchangeRates.currencies.${c}`) }}</option>
            </select>
          </label>
          <label v-if="needsRate" class="lbl min-w-0 w-full sm:w-44 sm:flex-none">
            {{ t('saleOrderCreate.rate') }}
            <input
              :value="rateText"
              inputmode="decimal"
              autocomplete="off"
              :placeholder="rateLoading ? t('exchangeRates.rateLoading') : '0'"
              class="field"
              :class="{ 'rate-manual': rateIsManual, 'cursor-default bg-gray-50 dark:bg-white/[0.03]': !rateEditable }"
              :readonly="!rateEditable"
              :tabindex="rateEditable ? undefined : -1"
              :title="
                !isForeign
                  ? t('exchangeRates.rateAuto')
                  : draftItems.length > 0
                    ? t('saleOrderCreate.rateLocked')
                    : t('saleOrderCreate.rateEditHint')
              "
              @input="onRateInput"
            />
          </label>
          <p v-if="needsRate" class="cur-hint">
            <span v-if="!rateLoading && !(form.exchangeRate > 0)" class="text-amber-600">{{ t('saleOrderCreate.rateMissing') }}</span>
            <template v-else>
              <span v-if="rateIsManual" class="text-amber-600">
                {{ t('saleOrderCreate.rateManual', { rate: formatRate(cbuRate) }) }}
                <button v-if="rateEditable" type="button" class="link-btn" @click="resetRate">{{ t('saleOrderCreate.rateReset') }}</button>.
              </span>
              <span v-else-if="cbuRateDate">{{ t('exchangeRates.cbuRate') }} · {{ cbuRateDate.split('-').reverse().join('.') }}. </span>
              <span v-if="isForeign">{{ t('saleOrderCreate.currencyHint') }}</span>
              <span v-else>{{ t('saleOrderCreate.rateForGoodsHint') }}</span>
            </template>
          </p>
        </div>

        <div class="delivery-block mt-4">
          <span class="lbl">{{ t('saleOrderCreate.deliveryTitle') }}</span>
          <div class="delivery-row">
            <div class="delivery-options">
              <button
                type="button"
                class="delivery-opt"
                :class="{ active: form.deliveryType === 'PICKUP' }"
                @click="setDeliveryType('PICKUP')"
              >
                <PackageCheck class="h-5 w-5 flex-shrink-0" />
                <span class="text-left">
                  <span class="opt-title">{{ t('saleOrderCreate.pickup') }}</span>
                  <span class="opt-sub">{{ t('saleOrderCreate.pickupHint') }}</span>
                </span>
              </button>
              <button
                type="button"
                class="delivery-opt"
                :class="{ active: form.deliveryType === 'DELIVERY' }"
                @click="setDeliveryType('DELIVERY')"
              >
                <Truck class="h-5 w-5 flex-shrink-0" />
                <span class="text-left">
                  <span class="opt-title">{{ t('saleOrderCreate.delivery') }}</span>
                  <span class="opt-sub">{{ t('saleOrderCreate.deliveryHint') }}</span>
                </span>
              </button>
            </div>
            <label v-if="form.deliveryType === 'DELIVERY'" class="lbl min-w-0 w-full sm:w-64 sm:flex-none">
              {{ t('saleOrderCreate.deliveryFee') }} ({{ curLabel }})
              <input
                :value="deliveryFeeText"
                inputmode="decimal"
                class="field total-field"
                placeholder="0"
                @input="onDeliveryFeeInput"
              />
            </label>
          </div>
        </div>

        <label class="lbl mt-4">
          {{ t('common.comment') }}
          <textarea
            v-model="form.comment"
            rows="2"
            class="field textarea-field"
            :placeholder="t('saleOrderCreate.commentPlaceholder')"
          />
        </label>

        <div v-if="form.clientId > 0" class="client-summary">
          <div class="summary-item">
            <span class="summary-label">{{ t('saleOrderCreate.totalPurchase') }}</span>
            <span class="summary-value">{{ balanceText(clientBalance?.totalPurchase) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ t('saleOrderCreate.totalPaid') }}</span>
            <span class="summary-value paid">{{ balanceText(clientBalance?.totalPaid) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">{{ Number(clientBalance?.totalDebt || 0) < 0 ? t('saleOrderCreate.overpaid') : t('saleOrderCreate.debt') }}</span>
            <span class="summary-value" :class="Number(clientBalance?.totalDebt || 0) > 0 ? 'debt' : 'paid'">
              {{ balanceText(clientBalance ? Math.abs(Number(clientBalance.totalDebt || 0)) : undefined) }}
            </span>
          </div>
          <p v-if="otherCurrencyDebts" class="summary-note">
            {{ t('saleOrderCreate.otherCurrencyDebt', { value: otherCurrencyDebts }) }}
          </p>
          <p v-if="balanceError" class="summary-note">{{ balanceError }}</p>
        </div>
      </div>
    </div>

    <div class="card mt-4">
      <div class="head">
        <div>
          <h3 class="title">{{ t('saleOrderCreate.items') }}</h3>
          <p class="sub">
            {{ t('saleOrderCreate.itemsHint') }}
          </p>
        </div>
      </div>

      <div class="p-5 border-b border-gray-100">
        <div class="form-row">
          <div class="lbl min-w-0 w-full sm:w-auto sm:flex-none">
            {{ t('saleOrderCreate.itemType') }}
            <div class="kind-toggle" role="group">
              <button
                type="button"
                class="kind-btn"
                :class="{ active: itemKind === 'PRODUCT' }"
                :aria-pressed="itemKind === 'PRODUCT'"
                @click="itemKind = 'PRODUCT'"
              >
                <Package class="h-4 w-4" />
                {{ t('saleOrderCreate.kindProduct') }}
              </button>
              <button
                type="button"
                class="kind-btn"
                :class="{ active: itemKind === 'SERVICE' }"
                :aria-pressed="itemKind === 'SERVICE'"
                @click="itemKind = 'SERVICE'"
              >
                <Wrench class="h-4 w-4" />
                {{ t('saleOrderCreate.kindService') }}
              </button>
            </div>
          </div>

          <label class="lbl min-w-0 flex-[1.4]">
            {{ itemKind === 'SERVICE' ? t('saleOrderCreate.service') : t('common.product') }}
            <SearchableSelect
              :key="itemKind"
              v-model="itemForm.goodsId"
              :options="goodsOptions"
              :placeholder="itemKind === 'SERVICE' ? t('saleOrderCreate.selectService') : t('saleOrderCreate.selectProduct')"
              :search-placeholder="itemKind === 'SERVICE' ? t('saleOrderCreate.serviceSearch') : t('saleOrderCreate.productSearch')"
            />
          </label>

          <template v-if="isWindowGoods">
            <label class="lbl min-w-0 w-28 sm:flex-none">
              {{ t('saleOrderCreate.width') }}
              <input v-model.number="itemForm.width" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              {{ t('saleOrderCreate.height') }}
              <input v-model.number="itemForm.height" type="number" min="1" step="1" class="field" />
            </label>
            <label class="lbl min-w-0 w-24 sm:flex-none">
              {{ t('common.count') }}
              <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-28 sm:flex-none">
              {{ t('saleOrderCreate.kvm') }}
              <input :value="formatNum(computedKvm)" class="field" readonly />
            </label>
            <label class="lbl min-w-0 w-36 sm:flex-none">
              {{ t('saleOrderCreate.sellingPrice') }} ({{ curLabel }})
              <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" class="field" />
            </label>
            <label class="lbl min-w-0 w-40 sm:flex-none">
              {{ t('saleOrderCreate.totalSum') }}
              <input :value="amt(computedSum)" class="field" readonly />
            </label>
          </template>

          <template v-else>
            <template v-if="itemKind !== 'SERVICE'">
              <label class="lbl min-w-0 w-28 sm:flex-none">
                {{ t('common.count') }}
                <input v-model.number="itemForm.count" type="number" min="0.01" step="0.01" class="field" />
              </label>
              <label class="lbl min-w-0 w-36 sm:flex-none">
                {{ t('saleOrderCreate.costPrice') }} ({{ t('common.currency') }})
                <input v-model.number="itemForm.priceCost" type="number" min="0.01" step="0.01" class="field" />
              </label>
              <label class="lbl min-w-0 w-36 sm:flex-none">
                {{ t('saleOrderCreate.selling') }} ({{ curLabel }})
                <input v-model.number="itemForm.priceSelling" type="number" min="0.01" step="0.01" class="field" />
              </label>
            </template>
            <label class="lbl min-w-0 w-40 sm:flex-none">
              {{ t('saleOrderCreate.totalSum') }} ({{ curLabel }})
              <input :value="amt(computedSum)" class="field" readonly />
            </label>
          </template>

          <div class="flex items-end">
            <button type="button" class="btn" :disabled="saving || !canAddItem" @click="onAddItem">
              {{ t('saleOrderCreate.addItem') }}
            </button>
          </div>
        </div>
        <p v-if="!headerReady" class="mt-2 text-xs text-amber-600">
          {{ t('saleOrderCreate.headerRequired') }}
        </p>
        <p
          v-else-if="selectedGoods && !isServiceGoods"
          class="stock-chip"
          :class="{ empty: remainingStock(selectedGoods.id) <= 1e-9 }"
        >
          <WarehouseIcon class="h-4 w-4 shrink-0" />
          {{ t('saleOrderCreate.stockLeft', { qty: stockLeftText(selectedGoods) }) }}
        </p>
        <p v-if="selectedGoods && goodsCurrency(selectedGoods) !== form.currency" class="stock-hint">
          {{ t('saleOrderCreate.priceConverted', {
            from: moneyIn(selectedGoods.priceSelling, goodsCurrency(selectedGoods), t('common.currency')),
            rate: formatRate(form.exchangeRate),
          }) }}
        </p>
        <p v-if="itemError" class="mt-2 text-xs text-red-600">{{ itemError }}</p>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="th">#</th>
              <th class="th">{{ t('common.product') }}</th>
              <th class="th">{{ t('saleOrderCreate.itemType') }}</th>
              <th class="th">{{ t('saleOrderCreate.width') }}</th>
              <th class="th">{{ t('saleOrderCreate.height') }}</th>
              <th class="th">{{ t('common.count') }}</th>
              <th class="th">{{ t('saleOrderCreate.kvm') }}</th>
              <th class="th">{{ t('saleOrderCreate.selling') }}</th>
              <th class="th">{{ t('common.sum') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="displayItems.length === 0">
              <td colspan="10" class="empty">{{ t('saleOrderCreate.noItems') }}</td>
            </tr>
            <tr v-for="(row, idx) in displayItems" :key="row.key" class="border-b border-gray-100">
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">{{ row.goodsName }}</td>
              <td class="td">
                <span class="kind-badge" :class="row.isService ? 'service' : 'product'">
                  {{ row.isService ? t('saleOrderCreate.kindService') : t('saleOrderCreate.kindProduct') }}
                </span>
              </td>
              <td class="td">{{ row.width != null ? formatNum(row.width) : '—' }}</td>
              <td class="td">{{ row.height != null ? formatNum(row.height) : '—' }}</td>
              <td class="td">{{ row.pieces != null ? formatNum(row.pieces) : formatNum(row.count) }}</td>
              <td class="td">{{ row.isWindow ? formatNum(row.count) : '—' }}</td>
              <td class="td">{{ amt(row.priceSelling) }}</td>
              <td class="td">{{ amt(row.sum) }}</td>
              <td class="td text-right">
                <button type="button" class="danger" :disabled="saving" @click="onRemoveItem(row.key)">{{ t('common.delete') }}</button>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="displayItems.length">
            <tr class="border-t border-gray-200 bg-gray-50">
              <td class="td font-semibold" colspan="8">{{ t('saleOrderCreate.itemsTotal') }}</td>
              <td class="td font-semibold">{{ amt(itemsTotalSum) }}</td>
              <td class="td" />
            </tr>
          </tfoot>
        </table>
      </div>

      <div class="save-bar border-t border-gray-100 p-5">
        <div class="save-info">
          <span v-if="saveBlockReason" class="save-warn">{{ saveBlockReason }}</span>
          <template v-else>
            <template v-if="deliveryFeeValue > 0">
              <span class="save-label">{{ t('saleOrderCreate.orderSum') }}</span>
              <span class="save-part">{{ amt(form.totalSum) }}</span>
              <span class="save-label">+ {{ t('saleOrderCreate.deliveryFeeShort') }}</span>
              <span class="save-part">{{ amt(deliveryFeeValue) }}</span>
              <span class="save-label">=</span>
            </template>
            <span v-else class="save-label">{{ t('saleOrderCreate.orderSum') }}</span>
            <span class="save-total">{{ amt(grandTotal) }}</span>
            <template v-if="isForeign && form.exchangeRate > 0">
              <span class="save-label" :title="t('saleOrderCreate.rate')">× {{ formatRate(form.exchangeRate) }}</span>
              <span class="save-label">=</span>
              <span class="save-part">{{ moneyIn(grandTotal * form.exchangeRate, 'UZS', t('common.currency')) }}</span>
            </template>
          </template>
        </div>
        <div class="flex gap-2">
          <router-link to="/sales" class="ghost">{{ t('common.cancel') }}</router-link>
          <button type="button" class="btn" :disabled="saving || !!saveBlockReason" @click="onSave">
            {{ saving ? '...' : t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="clientModal" class="overlay">
      <div class="modal">
        <h3 class="title mb-4">{{ t('saleOrderCreate.newClient') }}</h3>
        <div v-if="clientError" class="err mb-3">{{ clientError }}</div>
        <form class="space-y-3" @submit.prevent="onCreateClient">
          <label class="lbl">
            {{ t('common.fullName') }} *
            <input v-model="clientForm.fullName" required class="field" />
          </label>
          <label class="lbl">
            {{ t('common.phone') }} *
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
            {{ t('common.address') }} *
            <input v-model="clientForm.address" required class="field" />
          </label>
          <label class="lbl">
            {{ t('saleOrderCreate.additionalPhone') }}
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
            <button type="button" class="ghost" @click="clientModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="clientSaving">
              {{ clientSaving ? '...' : t('common.save') }}
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
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import { Magnet, Package, PackageCheck, Truck, Warehouse as WarehouseIcon, Wrench } from 'lucide-vue-next'
import { createSaleOrder, type DeliveryType } from '@/api/sales'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { createClient, fetchClients, type Client } from '@/api/clients'
import { balanceIn, fetchClientBalance, type ClientBalance } from '@/api/clientBalances'
import { useCbuRate } from '@/composables/useCbuRate'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { fetchGoods, type Goods } from '@/api/goods'
import { fetchStocksByWarehouse, type Stock } from '@/api/stocks'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import { amountToText, formatAmountInput, money } from '@/utils/format'
import { BASE_CURRENCY, CURRENCIES, currencySymbol, formatRate, moneyIn, type CurrencyCode } from '@/utils/currency'
import { formatUzPhone, isCompleteUzPhone } from '@/utils/phone'

const { t } = useI18n()
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
const clientBalances = ref<ClientBalance[]>([])
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
  deliveryType: 'PICKUP' as DeliveryType,
  deliveryFee: 0,
  currency: BASE_CURRENCY as CurrencyCode,
  /** Xorijiy buyurtmada buyurtma kursi; so'mdagi buyurtmada faqat xorijiy narxli mahsulotni o'girish uchun. */
  exchangeRate: 0,
})

const isForeign = computed(() => form.currency !== BASE_CURRENCY)
const hasForeignGoods = computed(() => goods.value.some((g) => goodsCurrency(g) !== BASE_CURRENCY))
const needsRate = computed(() => isForeign.value || hasForeignGoods.value)
const curLabel = computed(() => currencySymbol(form.currency, t('common.currency')))
const { rate: cbuRate, rateDate: cbuRateDate, loading: rateLoading } = useCbuRate(
  () => (isForeign.value ? form.currency : needsRate.value ? 'USD' : null),
  () => form.orderDate,
)
/** Xodim qo'lda kiritgan kurs; null - Markaziy bank kursi. Server uni faqat valyutali buyurtmada qabul qiladi. */
const manualRate = ref<number | null>(null)
const rateText = ref('')
const rateEditable = computed(() => isForeign.value && draftItems.value.length === 0)

watch(isForeign, (foreign) => {
  if (!foreign) manualRate.value = null
})
const rateIsManual = computed(
  () => manualRate.value != null && cbuRate.value > 0 && Math.abs(manualRate.value - cbuRate.value) >= 0.0001,
)

watch(
  [cbuRate, manualRate],
  () => {
    form.exchangeRate = manualRate.value ?? cbuRate.value
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
  if (!rateEditable.value) return
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  rateText.value = text
  el.value = text
  manualRate.value = value > 0 ? value : null
}

function resetRate() {
  manualRate.value = null
}

function amt(v?: number | null) {
  return isForeign.value ? moneyIn(v, form.currency) : money(v)
}

function goodsCurrency(g: Goods): CurrencyCode {
  return g.priceCurrency || BASE_CURRENCY
}

/** Katalog narxini buyurtma valyutasiga o'giradi; kurs bo'lmasa 0 (qo'lda kiritiladi). */
function goodsPriceIn(g: Goods) {
  const price = Number(g.priceSelling || 0)
  const from = goodsCurrency(g)
  if (from === form.currency) return price
  if (!(form.exchangeRate > 0)) return 0
  const converted = from === BASE_CURRENCY ? price / form.exchangeRate : price * form.exchangeRate
  return Math.round(converted * 100) / 100
}

const totalSumText = ref('')
const deliveryFeeText = ref('')
const deliveryFeeValue = computed(() => (form.deliveryType === 'DELIVERY' ? form.deliveryFee : 0))
const grandTotal = computed(() => form.totalSum + deliveryFeeValue.value)

function setDeliveryType(type: DeliveryType) {
  form.deliveryType = type
}

function onDeliveryFeeInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  deliveryFeeText.value = text
  el.value = text
  form.deliveryFee = value
}

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

function qtyWithUnit(v: number, windowMode: boolean) {
  return windowMode ? `${formatNum(v)} ${t('saleOrderCreate.kvmUnit')}` : formatNum(v)
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
  if (isServiceGoods.value) return Number(itemForm.priceSelling || 0)
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

/** Oyna qoldig'i donada: backend StockPieces bilan bir xil - mahsulot o'lchamidan, bo'lmasa ombordagi nisbatdan. */
function remainingWindowPieces(g: Goods, kvm: number) {
  const w = Number(g.width || 0)
  const h = Number(g.height || 0)
  if (w > 0 && h > 0) return (kvm * 10000) / (w * h)
  const stockKvm = availableByGoodsId.value.get(g.id) || 0
  return stockKvm > 0 ? ((stockByGoodsId.value.get(g.id) || 0) * kvm) / stockKvm : 0
}

const pieceFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 })

function stockLeftText(g: Goods) {
  const left = Math.max(remainingStock(g.id), 0)
  if (!isWindow(g)) return `${formatNum(left)} ${g.unitTypeName || t('saleOrderCreate.pieceUnit')}`
  const pieces = pieceFormat.format(remainingWindowPieces(g, left))
  return `${pieces} ${t('saleOrderCreate.pieceUnit')} · ${qtyWithUnit(left, true)}`
}

const itemKind = ref<'PRODUCT' | 'SERVICE'>('PRODUCT')

const addedGoodsIds = computed(() => new Set(draftItems.value.map((d) => d.goodsId)))

const goodsOptions = computed(() =>
  goods.value
    .filter((g) => (!g.status || g.status === 'ACTIVE') && hasWarehouseStock(g))
    .filter((g) => !addedGoodsIds.value.has(g.id))
    .filter((g) => (itemKind.value === 'SERVICE') === isService(g))
    .map((g) => ({ value: g.id, label: g.name })),
)

watch(itemKind, () => {
  itemForm.goodsId = 0
  itemError.value = null
})

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
  if (isServiceGoods.value) return Number(itemForm.priceSelling) > 0
  return Number(itemForm.priceCost) > 0 && Number(itemForm.priceSelling) > 0
})

const displayItems = computed(() =>
  draftItems.value.map((d) => ({ ...d, sum: d.count * d.priceSelling })),
)

const itemsTotalSum = computed(() => displayItems.value.reduce((acc, r) => acc + r.sum, 0))

function pullItemsTotal() {
  const total = Math.round(itemsTotalSum.value * 100) / 100
  if (!(total > 0)) return
  form.totalSum = total
  totalSumText.value = amountToText(total)
}

const saveBlockReason = computed(() => {
  if (!headerReady.value) return t('saleOrderCreate.selectHeader')
  if (!(form.totalSum > 0)) return t('saleOrderCreate.enterSum')
  if (isForeign.value && !(form.exchangeRate > 0)) return t('saleOrderCreate.rateMissing')
  if (draftItems.value.length === 0) return t('saleOrderCreate.addAtLeastOne')
  return null
})

watch(
  () => itemForm.goodsId,
  (id) => {
    const g = goods.value.find((x) => x.id === id)
    if (!g) return
    itemForm.priceCost = Number(g.priceCost || 0)
    itemForm.priceSelling = goodsPriceIn(g)
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

watch(
  () => [form.exchangeRate, form.currency] as const,
  ([, cur], [, prevCur]) => {
    const g = selectedGoods.value
    if (!g) return
    if (cur !== prevCur || goodsCurrency(g) !== cur) itemForm.priceSelling = goodsPriceIn(g)
  },
)

const clientBalance = computed(() => balanceIn(clientBalances.value, form.currency) || null)

const otherCurrencyDebts = computed(() =>
  clientBalances.value
    .filter((b) => (b.currency || BASE_CURRENCY) !== form.currency && Number(b.totalDebt || 0) > 0)
    .map((b) => moneyIn(b.totalDebt, b.currency || BASE_CURRENCY, t('common.currency')))
    .join(', '),
)

function balanceText(v?: number | null) {
  if (balanceLoading.value && clientBalances.value.length === 0) return '...'
  if (v == null) return clientBalances.value.length ? amt(0) : '—'
  return amt(v)
}

async function loadClientBalance() {
  const clientId = form.clientId
  const current = ++balanceRequest
  balanceError.value = null
  if (!clientId) {
    clientBalances.value = []
    return
  }
  balanceLoading.value = true
  try {
    const res = await fetchClientBalance(clientId, { fromDate: '2000-01-01', toDate: '2100-12-31' })
    if (current === balanceRequest) clientBalances.value = res.data || []
  } catch (e) {
    if (current === balanceRequest) {
      clientBalances.value = []
      balanceError.value = formatApiError(e, t('saleOrderCreate.balanceError'))
    }
  } finally {
    if (current === balanceRequest) balanceLoading.value = false
  }
}

watch(
  () => form.clientId,
  () => {
    clientBalances.value = []
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
      fetchUserOptions(),
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
  const pieces = isService(g) ? 1 : Number(itemForm.count)
  const count = windowMode ? computedKvm.value : pieces

  if (!isService(g)) {
    const left = remainingStock(g.id)
    if (count > left + 1e-9) {
      itemError.value = t('saleOrderCreate.notEnoughStock', {
        left: qtyWithUnit(Math.max(left, 0), windowMode),
        requested: qtyWithUnit(count, windowMode),
      })
      return
    }
  }

  if (addedGoodsIds.value.has(g.id)) return

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
    priceCost: windowMode ? toBase(selling) : Number(itemForm.priceCost),
    priceSelling: selling,
  })
  itemForm.goodsId = 0
  itemForm.count = 1
  itemForm.width = 0
  itemForm.height = 0
  itemForm.priceCost = 0
  itemForm.priceSelling = 0
}

/** Pozitsiya tannarxi doim so'mda saqlanadi. */
function toBase(v: number) {
  return isForeign.value && form.exchangeRate > 0 ? Math.round(v * form.exchangeRate * 100) / 100 : v
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
      deliveryType: form.deliveryType,
      deliveryFee: deliveryFeeValue.value,
      currency: form.currency,
      exchangeRate: isForeign.value && manualRate.value != null ? form.exchangeRate : undefined,
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
    if (!id) throw new Error(t('saleOrderCreate.notCreated'))
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
  return confirm(t('saleOrderCreate.leaveConfirm'))
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
      clientError.value = t('saleOrderCreate.fullNameRequired')
      return
    }
    if (!isCompleteUzPhone(clientForm.phone)) {
      clientError.value = t('saleOrderCreate.phoneInvalid')
      return
    }
    if (!clientForm.address.trim()) {
      clientError.value = t('saleOrderCreate.addressRequired')
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
.kind-toggle { display: inline-flex; height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #f9fafb; padding: 0.2rem; gap: 0.2rem; }
.kind-btn { display: inline-flex; align-items: center; gap: 0.35rem; border-radius: 0.375rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 500; color: #6b7280; white-space: nowrap; transition: background 0.15s ease, color 0.15s ease; }
.kind-btn:hover:not(.active) { color: #374151; }
.kind-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.1); }
.kind-badge { display: inline-flex; border-radius: 9999px; padding: 0.125rem 0.6rem; font-size: 0.75rem; font-weight: 500; white-space: nowrap; }
.kind-badge.product { background: #eef2ff; color: #465fff; }
.kind-badge.service { background: #fff7ed; color: #c2410c; }
:global(html.dark .kind-toggle) { border-color: #374151; background: #111827; }
:global(html.dark .kind-btn.active) { background: #1f2937; color: #a5b4fc; }
:global(html.dark .kind-badge.product) { background: rgba(70, 95, 255, 0.15); color: #a5b4fc; }
:global(html.dark .kind-badge.service) { background: rgba(249, 115, 22, 0.15); color: #fdba74; }
.total-row { display: flex; align-items: stretch; gap: 0.5rem; }
.total-row > .field { flex: 1; min-width: 0; }
.pull-total-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #c7d2fe; background: #eef2ff; color: #465fff; transition: background 0.15s ease; }
.pull-total-btn:hover:not(:disabled) { background: #e0e7ff; }
.pull-total-btn:disabled { opacity: 0.45; cursor: not-allowed; }
:global(html.dark .pull-total-btn) { border-color: rgba(70, 95, 255, 0.4); background: rgba(70, 95, 255, 0.15); color: #a5b4fc; }
.total-hint { font-size: 0.75rem; color: #6b7280; }
.textarea-field { height: auto; min-height: 4rem; padding: 0.6rem 0.75rem; resize: vertical; line-height: 1.4; }
.delivery-block { display: flex; flex-direction: column; gap: 0.35rem; }
.delivery-row { display: flex; flex-wrap: wrap; align-items: flex-end; gap: 0.75rem; }
.delivery-options { display: grid; flex: 1 1 28rem; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.75rem; }
.delivery-opt { display: flex; align-items: center; gap: 0.75rem; min-height: 3.5rem; border-radius: 0.75rem; border: 1px solid #d1d5db; background: #fff; padding: 0.6rem 0.9rem; color: #6b7280; transition: border-color .15s, background-color .15s, color .15s; }
.delivery-opt:hover { border-color: #9cb0ff; }
.delivery-opt.active { border-color: #465fff; background: #eff4ff; color: #465fff; box-shadow: 0 0 0 3px rgb(70 95 255 / 10%); }
.opt-title { display: block; font-size: 0.875rem; font-weight: 600; color: #1f2937; }
.opt-sub { display: block; font-size: 0.75rem; color: #6b7280; }
.delivery-opt.active .opt-title { color: #465fff; }
.save-part { font-weight: 600; color: #374151; }
.dark .delivery-opt { border-color: #344054; background: transparent; color: #9ca3af; }
.dark .delivery-opt.active { border-color: #7592ff; background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .opt-title { color: rgba(255, 255, 255, 0.9); }
.dark .delivery-opt.active .opt-title { color: #9cb0ff; }
.dark .opt-sub { color: #9ca3af; }
.dark .save-part { color: rgba(255, 255, 255, 0.8); }
.stock-hint { margin-top: 0.5rem; font-size: 0.75rem; color: #6b7280; }
.stock-chip { margin-top: 0.75rem; display: inline-flex; align-items: center; gap: 0.5rem; border-radius: 0.5rem; border: 1px solid #a7f3d0; background: #ecfdf5; padding: 0.375rem 0.75rem; font-size: 0.875rem; font-weight: 500; color: #047857; }
.stock-chip.empty { border-color: #fecaca; background: #fef2f2; color: #dc2626; }
.dark .stock-chip { border-color: rgba(16, 185, 129, 0.3); background: rgba(16, 185, 129, 0.1); color: #34d399; }
.dark .stock-chip.empty { border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.1); color: #f87171; }
.cur-hint { flex: 1 1 16rem; align-self: center; font-size: 0.75rem; color: #6b7280; }
.link-btn { font-weight: 500; color: #465fff; text-decoration: underline; }
.rate-manual { border-color: #f59e0b; }
.dark .cur-hint { color: #9ca3af; }
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
  .delivery-options { grid-template-columns: 1fr; }
}
</style>
