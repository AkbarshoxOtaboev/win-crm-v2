<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="isEdit ? t('saleOrderCreate.editPageTitle') : t('saleOrderCreate.pageTitle')">
      <router-link :to="backTo" class="ghost">{{ t('common.back') }}</router-link>
    </PageBreadcrumb>

    <div class="card">

      <div v-if="error" class="err mx-5 mt-5 mb-4">{{ error }}</div>
      <div v-if="orderFinal" class="warn mx-5 mt-5 mb-4">{{ t('saleOrderCreate.finalOrderHint') }}</div>

      <div class="p-5">
        <div class="step-head">
          <span class="step-num" :class="{ done: !!form.saleType }">
            <Check v-if="form.saleType" class="h-3.5 w-3.5" />
            <template v-else>1</template>
          </span>
          <span class="step-title">{{ t('saleOrderCreate.saleTypeTitle') }}</span>
        </div>
        <div class="type-row">
          <div
            class="type-toggle"
            role="radiogroup"
            :aria-label="t('saleOrderCreate.saleTypeTitle')"
            :title="isEdit ? t('saleOrderCreate.saleTypeLocked') : undefined"
          >
            <button
              type="button"
              role="radio"
              class="type-btn"
              :class="{ active: form.saleType === 'RETAIL' }"
              :aria-checked="form.saleType === 'RETAIL'"
              :disabled="isEdit"
              @click="form.saleType = 'RETAIL'"
            >
              <ShoppingBag class="h-4 w-4 flex-shrink-0" />
              {{ t('saleOrderCreate.retail') }}
            </button>
            <button
              type="button"
              role="radio"
              class="type-btn wholesale"
              :class="{ active: form.saleType === 'WHOLESALE' }"
              :aria-checked="form.saleType === 'WHOLESALE'"
              :disabled="isEdit"
              @click="form.saleType = 'WHOLESALE'"
            >
              <Boxes class="h-4 w-4 flex-shrink-0" />
              {{ t('saleOrderCreate.wholesale') }}
            </button>
          </div>
          <div
            class="type-toggle cur-toggle"
            role="radiogroup"
            :aria-label="t('saleOrderCreate.currency')"
            :title="draftItems.length > 0 ? t('saleOrderCreate.currencyLocked') : undefined"
          >
            <button
              v-for="c in CURRENCIES"
              :key="c"
              type="button"
              role="radio"
              class="type-btn"
              :class="{ active: form.currency === c }"
              :aria-checked="form.currency === c"
              :disabled="draftItems.length > 0"
              @click="form.currency = c"
            >
              {{ t(`exchangeRates.currencies.${c}`) }}
            </button>
          </div>
          <label class="seller-inline">
            <span>{{ t('saleOrderCreate.seller') }}</span>
            <select v-model.number="form.userId" required class="field" :disabled="!auth.isElevated && sellerOptions.length === 1">
              <option :value="0" disabled>{{ t('saleOrderCreate.selectSeller') }}</option>
              <option v-for="u in sellerOptions" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </label>
        </div>

        <div v-if="!form.saleType && !isEdit" class="type-empty">
          <span class="type-empty-icon"><UserRound class="h-5 w-5" /></span>
          {{ t('saleOrderCreate.chooseSaleTypeFirst') }}
        </div>

        <Transition name="reveal">
        <div v-if="form.saleType" class="mt-6">
        <div class="details-grid">
          <label class="lbl span-2">
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
            <p v-if="form.clientId > 0 && balanceError" class="client-debt">{{ balanceError }}</p>
            <p v-else-if="form.clientId > 0" class="client-debt">
              {{ t('saleOrderCreate.debt') }}: {{ clientDebtLine }}
            </p>
          </label>

          <label class="lbl">
            {{ t('common.date') }}
            <input v-model="form.orderDate" type="date" required class="field" />
          </label>

          <label class="lbl">
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
        </div>
        </div>
        </Transition>
      </div>

      <template v-if="form.saleType">
      <p v-if="orderFinal" class="items-locked">{{ t('saleOrderCreate.itemsLocked') }}</p>
      <div v-else class="p-5 border-b border-gray-100">
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
        <CuttingPreview
          v-if="showCuttingPreview"
          :goods-id="itemForm.goodsId"
          :piece-width="Number(itemForm.width)"
          :piece-height="Number(itemForm.height)"
          :quantity="Number(itemForm.count)"
          v-model:kerf="itemForm.cutKerfMm"
          v-model:allow-rotate="itemForm.cutAllowRotate"
        />
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
              <th class="th w-10" />
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
              <td colspan="11" class="empty">{{ t('saleOrderCreate.noItems') }}</td>
            </tr>
            <tr v-for="(row, idx) in displayItems" :key="row.key" class="border-b border-gray-100" :class="{ 'row-picked': selectedItemKeys.includes(row.key) }">
              <td class="td">
                <input
                  type="checkbox"
                  class="row-check"
                  :checked="selectedItemKeys.includes(row.key)"
                  :disabled="orderFinal"
                  :aria-label="row.goodsName"
                  @change="toggleItemSelect(row.key)"
                />
              </td>
              <td class="td">{{ idx + 1 }}</td>
              <td class="td">
                <div class="product-cell">
                  <select
                    class="product-select"
                    :class="{ short: stockIsShort(row) }"
                    :value="row.goodsId"
                    :disabled="saving || orderFinal"
                    :aria-label="row.goodsName"
                    @change="onRowGoodsChange(row.key, $event)"
                  >
                    <option v-for="opt in rowGoodsOptions(row)" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </option>
                  </select>
                  <span v-if="isEdit && !row.itemId" class="new-badge">{{ t('saleOrderCreate.newBadge') }}</span>
                </div>
              </td>
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
                <button type="button" class="danger" :disabled="saving || orderFinal" @click="onRemoveItem(row.key)">{{ t('common.delete') }}</button>
              </td>
            </tr>
          </tbody>
          <tfoot v-if="displayItems.length">
            <tr class="border-t border-gray-200 bg-gray-50">
              <td class="td font-semibold" colspan="9">{{ t('saleOrderCreate.itemsTotal') }}</td>
              <td class="td font-semibold">{{ amt(itemsTotalSum) }}</td>
              <td class="td" />
            </tr>
          </tfoot>
        </table>
      </div>

      <div v-if="!orderFinal && displayItems.length" class="copy-bar">
        <button
          type="button"
          class="ghost"
          :disabled="saving || selectedItemKeys.length === 0"
          :title="t('saleOrderCreate.copyItemHint')"
          @click="copySelectedItems"
        >
          {{ t('saleOrderCreate.copyItem') }}
        </button>
        <p v-if="itemError" class="copy-note">{{ itemError }}</p>
      </div>

      <div class="border-t border-gray-100 p-5">
        <div class="details-grid">
          <label v-if="needsRate" class="lbl">
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

          <div class="lbl span-2">
            <span>{{ t('saleOrderCreate.deliveryTitle') }}</span>
            <div
              class="type-toggle delivery-toggle"
              role="radiogroup"
              :aria-label="t('saleOrderCreate.deliveryTitle')"
            >
              <button
                type="button"
                role="radio"
                class="type-btn"
                :class="{ active: form.deliveryType === 'PICKUP' }"
                :aria-checked="form.deliveryType === 'PICKUP'"
                :disabled="deliveryTypeLocked"
                @click="setDeliveryType('PICKUP')"
              >
                {{ t('saleOrderCreate.pickup') }}
              </button>
              <button
                type="button"
                role="radio"
                class="type-btn"
                :class="{ active: form.deliveryType === 'DELIVERY' }"
                :aria-checked="form.deliveryType === 'DELIVERY'"
                :disabled="deliveryTypeLocked"
                @click="setDeliveryType('DELIVERY')"
              >
                {{ t('saleOrderCreate.delivery') }}
              </button>
            </div>
          </div>

          <label v-if="form.deliveryType === 'DELIVERY'" class="lbl">
            {{ t('saleOrderCreate.deliveryFee') }} ({{ curLabel }})
            <input
              :value="deliveryFeeText"
              inputmode="decimal"
              class="field total-field"
              placeholder="0"
              @input="onDeliveryFeeInput"
            />
          </label>

          <div class="lbl" :class="{ 'span-2': !needsRate }">
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

        <div class="details-notes">
          <p class="total-hint">{{ t('saleOrderCreate.orderSumHint') }}</p>
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
            <span v-if="discountText" class="save-label">· {{ t('saleOrderCreate.discountKept', { value: discountText }) }}</span>
          </template>
        </div>
        <div class="flex gap-2">
          <router-link :to="backTo" class="ghost">{{ t('common.cancel') }}</router-link>
          <button
            type="button"
            class="btn"
            :class="{ 'wholesale-btn': !isEdit && form.saleType === 'WHOLESALE' }"
            :disabled="saving || !!saveBlockReason"
            @click="onSave"
          >
            <CheckCheck v-if="!isEdit && form.saleType === 'WHOLESALE'" class="me-1.5 h-4 w-4" />
            {{
              saving
                ? '...'
                : !isEdit && form.saleType === 'WHOLESALE'
                  ? t('saleOrderCreate.saveWholesale')
                  : t('common.save')
            }}
          </button>
        </div>
      </div>
      </template>
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
import {
  Boxes,
  Check,
  CheckCheck,
  Magnet,
  Package,
  ShoppingBag,
  UserRound,
  Warehouse as WarehouseIcon,
  Wrench,
} from 'lucide-vue-next'
import Swal from 'sweetalert2'
import {
  createSaleOrder,
  fetchSaleOrder,
  updateSaleOrder,
  type DeliveryType,
  type SaleOrder,
  type SaleOrderInitialItem,
  type SaleType,
} from '@/api/sales'
import { fetchSaleOrderItems, type SaleOrderItem } from '@/api/saleOrderItems'
import CuttingPreview from '@/components/crm/CuttingPreview.vue'
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

const editId = route.name === 'SaleOrderEdit' ? Number(route.params.id) || 0 : 0
const isEdit = editId > 0
const backTo = isEdit ? `/sales/${editId}` : '/sales'
const editOrder = ref<SaleOrder | null>(null)
const orderFinal = computed(() => ['COMPLETED', 'CANCELLED'].includes(editOrder.value?.orderStatus || ''))
const deliveryTypeLocked = computed(() =>
  ['IN_DELIVERY', 'DELIVERED', 'WORK_DONE', 'COMPLETED', 'CANCELLED'].includes(editOrder.value?.orderStatus || ''),
)

interface DraftItem {
  key: number
  /** Saqlangan pozitsiya ID'si; yangi qo'shilganda yo'q. */
  itemId?: number
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
  cutKerfMm: number | null
  cutAllowRotate: boolean | null
}

const draftItems = ref<DraftItem[]>([])
/** Tahrirlashda olib tashlangan saqlangan pozitsiyalar: saqlanganda o'chiriladi, qoldig'i omborga qaytadi. */
const removedItems = ref<DraftItem[]>([])
const selectedItemKeys = ref<number[]>([])
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
  saleType: '' as SaleType | '',
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
  cutKerfMm: '' as number | '',
  cutAllowRotate: true,
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
const showCuttingPreview = computed(
  () =>
    isWindowGoods.value &&
    Number(itemForm.width) > 0 &&
    Number(itemForm.height) > 0 &&
    Number(itemForm.count) > 0,
)

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
  return (stockByGoodsId.value.get(g.id) || 0) > 0 || releasedStock(g.id) > 0
}

function releasedStock(goodsId: number) {
  if (!editOrder.value || form.warehouseId !== editOrder.value.warehouseId) return 0
  return removedItems.value.filter((d) => d.goodsId === goodsId).reduce((acc, d) => acc + d.count, 0)
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
    .filter((d) => d.goodsId === goodsId && !d.itemId)
    .reduce((acc, d) => acc + d.count, 0)
  return (availableByGoodsId.value.get(goodsId) || 0) - reserved + releasedStock(goodsId)
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

const sellerOptions = computed(() =>
  auth.isElevated
    ? users.value
    : users.value.filter((u) => u.username === auth.username || u.id === editOrder.value?.userId),
)

const itemKind = ref<'PRODUCT' | 'SERVICE'>('PRODUCT')

const goodsOptions = computed(() =>
  goods.value
    .filter((g) => (!g.status || g.status === 'ACTIVE') && (hasWarehouseStock(g) || g.id === itemForm.goodsId))
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
  return Number(itemForm.priceSelling) > 0
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
  if (!form.saleType) return t('saleOrderCreate.chooseSaleTypeFirst')
  if (!headerReady.value) return t('saleOrderCreate.selectHeader')
  if (!(form.totalSum > 0)) return t('saleOrderCreate.enterSum')
  if (isForeign.value && !(form.exchangeRate > 0)) return t('saleOrderCreate.rateMissing')
  if (!isEdit && draftItems.value.length === 0) return t('saleOrderCreate.addAtLeastOne')
  return null
})

const discountText = computed(() => {
  const o = editOrder.value
  if (!o?.discountType || !(Number(o.discountValue) > 0)) return ''
  return o.discountType === 'PERCENTAGE' ? `${Number(o.discountValue)}%` : amt(Number(o.discountValue))
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

const clientDebtLine = computed(() => {
  if (balanceLoading.value && clientBalances.value.length === 0) return '...'
  return CURRENCIES.map((c) => {
    const debt = Number(balanceIn(clientBalances.value, c)?.totalDebt || 0)
    return moneyIn(debt, c, t('common.currency'))
  }).join(' · ')
})

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
    if (isEdit) {
      await fillFromOrder()
    } else if (!form.warehouseId && activeWarehouses.value[0]) {
      form.warehouseId = activeWarehouses.value[0].id
    }
    if (form.warehouseId) {
      const stockRes = await fetchStocksByWarehouse(form.warehouseId)
      stocks.value = stockRes.data || []
    }
    if (isEdit) return
    const me = users.value.find((x) => x.username === auth.username)
    if (!form.userId || !sellerOptions.value.some((u) => u.id === form.userId)) {
      form.userId = me?.id || sellerOptions.value[0]?.id || 0
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
    priceCost: Number(itemForm.priceCost),
    priceSelling: selling,
    cutKerfMm: windowMode && itemForm.cutKerfMm !== '' ? Number(itemForm.cutKerfMm) : null,
    cutAllowRotate: windowMode ? itemForm.cutAllowRotate : null,
  })
  itemForm.width = 0
  itemForm.height = 0
  itemForm.count = 0
}

function sameKind(g: Goods, row: { isWindow: boolean; isService: boolean }) {
  if (row.isService) return isService(g)
  if (row.isWindow) return isWindow(g)
  return !isService(g) && !isWindow(g)
}

function rowGoodsOptions(row: DraftItem) {
  return goods.value
    .filter((g) => (!g.status || g.status === 'ACTIVE') && sameKind(g, row))
    .filter((g) => hasWarehouseStock(g) || g.id === row.goodsId)
    .map((g) => ({ value: g.id, label: g.name }))
}

function stockIsShort(row: { goodsId: number; isService: boolean; itemId?: number }) {
  if (row.isService || row.itemId) return false
  return remainingStock(row.goodsId) < -1e-9
}

function refreshStockError() {
  itemError.value = draftItems.value.some((d) => stockIsShort(d)) ? t('saleOrderCreate.copyStockShort') : null
}

function onRowGoodsChange(key: number, e: Event) {
  changeRowGoods(key, Number((e.target as HTMLSelectElement).value))
}

function changeRowGoods(key: number, goodsId: number) {
  const row = draftItems.value.find((d) => d.key === key)
  const g = goods.value.find((x) => x.id === goodsId)
  if (!row || !g || row.goodsId === goodsId || orderFinal.value) return
  if (row.itemId) {
    removedItems.value.push({ ...row })
    row.itemId = undefined
  }
  const windowMode = isWindow(g)
  row.goodsId = g.id
  row.goodsName = g.name
  row.isWindow = windowMode
  row.isService = isService(g)
  row.priceCost = Number(g.priceCost || 0)
  const nextPrice = goodsPriceIn(g)
  if (nextPrice > 0) row.priceSelling = nextPrice
  if (!windowMode) {
    row.width = null
    row.height = null
    row.pieces = null
    row.cutKerfMm = null
    row.cutAllowRotate = null
  }
  refreshStockError()
}

function toggleItemSelect(key: number) {
  selectedItemKeys.value = selectedItemKeys.value.includes(key)
    ? selectedItemKeys.value.filter((k) => k !== key)
    : [...selectedItemKeys.value, key]
}

function copySelectedItems() {
  if (orderFinal.value || saving.value || selectedItemKeys.value.length === 0) return
  const picked = draftItems.value.filter((d) => selectedItemKeys.value.includes(d.key))
  for (const row of picked) {
    draftItems.value.push({
      key: ++draftSeq,
      goodsId: row.goodsId,
      goodsName: row.goodsName,
      isWindow: row.isWindow,
      isService: row.isService,
      width: row.width,
      height: row.height,
      pieces: row.pieces,
      count: row.count,
      priceCost: row.priceCost,
      priceSelling: row.priceSelling,
      cutKerfMm: row.cutKerfMm,
      cutAllowRotate: row.cutAllowRotate,
    })
  }
  refreshStockError()
}

function onRemoveItem(key: number) {
  const row = draftItems.value.find((d) => d.key === key)
  if (row?.itemId) removedItems.value.push(row)
  draftItems.value = draftItems.value.filter((d) => d.key !== key)
  if (selectedItemKeys.value.includes(key)) {
    selectedItemKeys.value = selectedItemKeys.value.filter((k) => k !== key)
  }
  refreshStockError()
}

/** Saqlangan buyurtma vaqtini saqlab qoladi: kun o'zgarmasa, server uchun asl sana-vaqt yuboriladi. */
let originalOrderDate = ''

function toDraft(it: SaleOrderItem): DraftItem {
  const g = goods.value.find((x) => x.id === it.goodsId)
  const windowMode = isWindow(g)
  const width = Number(it.width || 0)
  const height = Number(it.height || 0)
  const count = Number(it.count || 0)
  return {
    key: ++draftSeq,
    itemId: it.id,
    goodsId: Number(it.goodsId),
    goodsName: it.goodsName || g?.name || `#${it.goodsId}`,
    isWindow: windowMode,
    isService: isService(g),
    width: windowMode ? width : null,
    height: windowMode ? height : null,
    pieces: windowMode && width > 0 && height > 0 ? Math.round(((count * 10000) / (width * height)) * 10000) / 10000 : null,
    count,
    priceCost: Number(it.priceCost || 0),
    priceSelling: Number(it.priceSelling || 0),
    cutKerfMm: it.cutKerfMm != null ? Number(it.cutKerfMm) : null,
    cutAllowRotate: it.cutAllowRotate ?? null,
  }
}

async function fillFromOrder() {
  const [o, its] = await Promise.all([fetchSaleOrder(editId), fetchSaleOrderItems(editId)])
  const order = o.data
  if (!order) throw new Error(t('saleOrderCreate.notFound'))
  editOrder.value = order
  originalOrderDate = order.orderDate || ''
  form.clientId = order.clientId || 0
  form.warehouseId = order.warehouseId || 0
  form.userId = order.userId || 0
  form.orderDate = originalOrderDate.slice(0, 10) || todayLocal()
  form.comment = order.comment || ''
  form.totalSum = Number(order.originalTotalSum ?? order.totalSum ?? 0)
  totalSumText.value = form.totalSum > 0 ? amountToText(form.totalSum) : ''
  form.deliveryType = order.deliveryType || 'PICKUP'
  form.deliveryFee = Number(order.deliveryFee || 0)
  deliveryFeeText.value = form.deliveryFee > 0 ? amountToText(form.deliveryFee) : ''
  form.saleType = order.saleType || 'RETAIL'
  form.currency = order.currency || BASE_CURRENCY
  const rate = Number(order.exchangeRate || 0)
  manualRate.value = form.currency !== BASE_CURRENCY && rate > 0 ? rate : null
  draftItems.value = (its.data || []).filter((it) => it.status !== 'DELETED' && it.goodsId).map(toDraft)
  removedItems.value = []
}

watch(
  () => form.orderDate,
  (date) => {
    if (!isEdit || !editOrder.value || date === originalOrderDate.slice(0, 10)) return
    if (manualRate.value === Number(editOrder.value.exchangeRate)) manualRate.value = null
  },
)

function itemPayload(d: DraftItem): SaleOrderInitialItem {
  return {
    goodsId: d.goodsId,
    priceCost: d.priceCost,
    priceSelling: d.priceSelling,
    count: d.isWindow ? Number(d.pieces) : d.count,
    width: d.width ?? undefined,
    height: d.height ?? undefined,
    cutKerfMm: d.cutKerfMm ?? undefined,
    cutAllowRotate: d.cutAllowRotate ?? undefined,
  }
}

async function saveEdit() {
  saving.value = true
  error.value = null
  try {
    const sameDay = !!originalOrderDate && form.orderDate === originalOrderDate.slice(0, 10)
    await updateSaleOrder(editId, {
      warehouseId: form.warehouseId,
      userId: form.userId,
      orderDate: sameDay ? originalOrderDate : dateToApi(form.orderDate),
      totalSum: form.totalSum,
      clientId: form.clientId,
      comment: form.comment.trim(),
      deliveryType: form.deliveryType,
      deliveryFee: deliveryFeeValue.value,
      currency: form.currency,
      exchangeRate: isForeign.value && manualRate.value != null ? form.exchangeRate : undefined,
      items: draftItems.value.filter((d) => !d.itemId).map(itemPayload),
      removedItemIds: removedItems.value.map((d) => Number(d.itemId)),
    })
    saved.value = true
    void router.push(backTo)
  } catch (e) {
    error.value = formatApiError(e)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } finally {
    saving.value = false
  }
}

async function onSave() {
  if (saving.value || saveBlockReason.value) return
  if (isEdit) {
    await saveEdit()
    return
  }
  if (form.saleType === 'WHOLESALE') {
    const result = await Swal.fire({
      title: t('saleOrderCreate.wholesaleConfirmTitle'),
      text: t('saleOrderCreate.wholesaleConfirmText', { sum: amt(grandTotal.value) }),
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: t('saleOrderCreate.saveWholesale'),
      cancelButtonText: t('common.cancel'),
      confirmButtonColor: '#12b76a',
      cancelButtonColor: '#98a2b3',
      reverseButtons: true,
    })
    if (!result.isConfirmed) return
  }
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
      saleType: form.saleType || 'RETAIL',
      items: draftItems.value.map(itemPayload),
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
  const dirty = isEdit
    ? removedItems.value.length > 0 || draftItems.value.some((d) => !d.itemId)
    : draftItems.value.length > 0
  if (saved.value || !dirty) return true
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
.warn { border-radius: 0.5rem; border: 1px solid #fde68a; background: #fffbeb; padding: 0.75rem 1rem; font-size: 0.875rem; color: #b45309; }
.dark .warn { border-color: rgba(245, 158, 11, 0.35); background: rgba(245, 158, 11, 0.1); color: #fbbf24; }
.items-locked { border-bottom: 1px solid #f3f4f6; padding: 1rem 1.25rem; font-size: 0.875rem; color: #b45309; }
.dark .items-locked { border-color: #1f2937; color: #fbbf24; }
.new-badge { margin-inline-start: 0.375rem; border-radius: 9999px; background: #ecfdf3; padding: 0.0625rem 0.5rem; font-size: 0.6875rem; font-weight: 600; color: #027a48; }
.dark .new-badge { background: rgba(18, 183, 106, 0.15); color: #6ce9a6; }
.row-check { width: 1rem; height: 1rem; accent-color: #465fff; }
.product-cell { display: flex; align-items: center; gap: 0.375rem; min-width: 12rem; }
.product-select { height: 2.25rem; width: 100%; min-width: 11rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0 0.5rem; font-size: 0.8125rem; color: #374151; }
.product-select:disabled { background: #f9fafb; cursor: not-allowed; }
.product-select.short { border-color: #dc2626; }
.dark .product-select { border-color: #374151; background: #111827; color: #e5e7eb; }
.dark .product-select.short { border-color: #f87171; }
.row-picked { background: #f5f8ff; }
.dark .row-picked { background: rgb(70 95 255 / 8%); }
.copy-bar { display: flex; flex-wrap: wrap; align-items: center; justify-content: flex-end; gap: 0.75rem; padding: 0.75rem 1.25rem; border-top: 1px solid #f3f4f6; }
.copy-note { margin: 0; font-size: 0.75rem; font-weight: 600; color: #dc2626; }
.dark .client-debt { color: #f87171; }
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
.delivery-toggle { width: 100%; min-width: 0; }
.delivery-toggle .type-btn { height: auto; min-height: 2.5rem; padding: 0.35rem 0.6rem; font-size: 0.75rem; line-height: 1.2; white-space: normal; text-align: center; }
.step-head { display: flex; align-items: center; gap: 0.625rem; margin-bottom: 0.75rem; }
.step-num { display: inline-flex; height: 1.5rem; width: 1.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 9999px; background: #eff4ff; font-size: 0.75rem; font-weight: 700; color: #465fff; }
.step-num.done { background: #465fff; color: #fff; }
.step-title { font-size: 0.9375rem; font-weight: 600; color: #1f2937; }
.dark .step-num { background: rgb(70 95 255 / 15%); color: #9cb0ff; }
.dark .step-num.done { background: #465fff; color: #fff; }
.dark .step-title { color: rgba(255, 255, 255, 0.9); }
.type-row { display: flex; flex-wrap: wrap; align-items: center; gap: 0.75rem 1rem; }
.type-toggle { display: inline-grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.25rem; border-radius: 0.75rem; background: #f2f4f7; padding: 0.25rem; min-width: min(100%, 20rem); }
.type-btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; gap: 0.5rem; border-radius: 0.5rem; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 600; color: #667085; white-space: nowrap; transition: background-color 0.15s, color 0.15s, box-shadow 0.15s; }
.type-btn:hover:not(:disabled):not(.active) { color: #344054; background: rgb(255 255 255 / 60%); }
.type-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.12); }
.type-btn.wholesale.active { background: #12b76a; color: #fff; }
.type-btn:disabled { cursor: not-allowed; }
.type-btn:disabled:not(.active) { opacity: 0.5; }
.cur-toggle { min-width: min(100%, 16rem); }
.seller-inline { display: flex; align-items: center; gap: 0.5rem; flex: 1 1 18rem; min-width: min(100%, 16rem); margin: 0; font-size: 0.8125rem; font-weight: 600; color: #344054; }
.seller-inline .field { flex: 1; min-width: 0; }
.type-empty { margin-top: 1.25rem; display: flex; align-items: center; gap: 0.75rem; border-radius: 0.75rem; border: 1px dashed #d0d5dd; background: #f9fafb; padding: 1rem 1.25rem; font-size: 0.875rem; color: #667085; }
.type-empty-icon { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 9999px; background: #fff; color: #98a2b3; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.06); }
.dark .type-toggle { background: rgb(255 255 255 / 5%); }
.dark .type-btn { color: #98a2b3; }
.dark .type-btn:hover:not(:disabled):not(.active) { color: #e4e7ec; background: rgb(255 255 255 / 5%); }
.dark .type-btn.active { background: #1d2939; color: #9cb0ff; }
.dark .type-btn.wholesale.active { background: #12b76a; color: #fff; }
.dark .seller-inline { color: #e4e7ec; }
.dark .type-empty { border-color: #344054; background: rgb(255 255 255 / 2%); color: #98a2b3; }
.dark .type-empty-icon { background: rgb(255 255 255 / 5%); color: #98a2b3; }
.details-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; }
.details-grid > * { min-width: 0; }
.details-grid .span-2 { grid-column: span 2 / span 2; }
.details-notes { margin-top: 0.5rem; display: flex; flex-wrap: wrap; gap: 0.25rem 1.5rem; }
.reveal-enter-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.reveal-enter-from { opacity: 0; transform: translateY(-0.375rem); }
@media (max-width: 1024px) {
  .details-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .details-grid { grid-template-columns: 1fr; }
  .details-grid .span-2 { grid-column: auto; }
  .type-toggle, .seller-inline { width: 100%; }
}
.btn.wholesale-btn { background: #12b76a; }
.btn.wholesale-btn:hover:not(:disabled) { background: #039855; }
.save-part { font-weight: 600; color: #374151; }
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
  .client-row { flex-direction: column; }
}
</style>
