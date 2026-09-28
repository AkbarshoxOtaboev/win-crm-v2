<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.payments')" />

    <div class="mb-4 flex flex-wrap gap-2">
      <button
        type="button"
        class="tab"
        :class="{ active: tab === 'payments' }"
        @click="tab = 'payments'"
      >
        <Wallet class="h-4 w-4" />
        {{ t('nav.payments') }}
        <span class="tab-count">{{ items.length }}</span>
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: tab === 'types' }"
        @click="tab = 'types'"
      >
        <CreditCard class="h-4 w-4" />
        {{ t('nav.paymentTypes') }}
        <span class="tab-count">{{ paymentTypes.length }}</span>
      </button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <!-- Payments -->
    <div
      v-show="tab === 'payments'"
      class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div
        class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800 lg:flex-row lg:items-center lg:justify-between"
      >
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.payments') }}</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {{ t('payments.subtitle') }}
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <button
            type="button"
            class="icon-btn"
            :title="t('payments.filters')"
            @click="showFilters = !showFilters"
          >
            <ListFilter class="h-4 w-4" />
          </button>
          <button
            type="button"
            class="icon-btn"
            :title="t('common.refresh')"
            :disabled="loading"
            @click="load"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
          <button
            v-if="auth.can('PAYMENT_CREATE')"
            type="button"
            class="btn btn-with-icon"
            :disabled="writeBlocked"
            @click="openCreate"
          >
            <Plus class="h-4 w-4" />
            {{ t('payments.newPayment') }}
          </button>
        </div>
      </div>

      <div v-if="showFilters" class="filters border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div class="grid gap-3 sm:grid-cols-2">
          <label class="lbl-block">
            {{ t('common.client') }}
            <SearchableSelect
              :model-value="filterClientId"
              :options="filterClientOptions"
              :placeholder="t('payments.allClients')"
              :search-placeholder="t('payments.clientSearchPlaceholder')"
              @update:model-value="onFilterClientChange"
            />
          </label>
          <label class="lbl-block">
            {{ t('payments.search') }}
            <div class="relative">
              <Search class="field-icon" />
              <input
                v-model="search"
                type="search"
                :placeholder="t('payments.searchPlaceholder')"
                class="field"
              />
            </div>
          </label>
        </div>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <button type="button" class="ghost" @click="clearFilters">{{ t('common.clearFilter') }}</button>
          <span class="text-sm text-gray-500 dark:text-gray-400">
            {{ t('payments.showing', { n: filtered.length }) }}
          </span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.client') }}</th>
              <th class="th">{{ t('common.type') }}</th>
              <th class="th">{{ t('common.sum') }}</th>
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('payments.order') }}</th>
              <th class="th th-actions">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="empty">{{ t('common.loading') }}</td>
            </tr>
            <tr v-else-if="filtered.length === 0">
              <td colspan="7" class="empty">
                <div class="flex flex-col items-center gap-2 py-2">
                  <Wallet class="h-8 w-8 text-gray-300 dark:text-gray-600" />
                  <span>{{ t('payments.notFound') }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="p in filtered"
              :key="p.id"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50/80 dark:border-gray-800 dark:hover:bg-white/[0.02]"
            >
              <td class="td text-gray-400">{{ p.id }}</td>
              <td class="td">
                <div class="font-medium text-gray-800 dark:text-white/90">
                  {{ p.clientFullName || '—' }}
                </div>
                <div v-if="p.comment" class="mt-0.5 max-w-48 truncate text-xs text-gray-400">
                  {{ p.comment }}
                </div>
              </td>
              <td class="td">
                <span class="type-badge">
                  <component :is="iconForTypeId(p.paymentTypeId)" class="h-3.5 w-3.5 shrink-0 opacity-70" />
                  {{ p.paymentTypeName || '—' }}
                </span>
              </td>
              <td class="td">
                <span class="font-semibold text-success-600 dark:text-success-400">
                  {{ money(p.paymentAmount) }}
                </span>
              </td>
              <td class="td whitespace-nowrap">{{ formatDate(p.paymentDate) }}</td>
              <td class="td">
                <span
                  v-if="p.saleOrderId"
                  class="inline-flex items-center gap-1 rounded-md bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/10 dark:text-brand-400"
                >
                  #{{ p.saleOrderId }}
                </span>
                <span v-else class="text-xs text-gray-400">{{ t('payments.advance') }}</span>
              </td>
              <td class="td td-actions">
                <RowActions
                  :edit="auth.can('PAYMENT_EDIT')"
                  :remove="auth.can('PAYMENT_DELETE')"
                  @edit="openEdit(p)"
                  @delete="onDelete(p)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Payment types -->
    <div
      v-show="tab === 'types'"
      class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div
        class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800"
      >
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.paymentTypes') }}</h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {{ t('payments.typesSubtitle') }}
          </p>
        </div>
        <button
          v-if="auth.can('PAYMENT_TYPE_CREATE')"
          type="button"
          class="btn btn-with-icon"
          :disabled="writeBlocked"
          @click="openTypeCreate"
        >
          <Plus class="h-4 w-4" />
          {{ t('payments.newType') }}
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.name') }}</th>
              <th class="th th-actions">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="3" class="empty">{{ t('common.loading') }}</td>
            </tr>
            <tr v-else-if="paymentTypes.length === 0">
              <td colspan="3" class="empty">
                <div class="flex flex-col items-center gap-2 py-2">
                  <CreditCard class="h-8 w-8 text-gray-300 dark:text-gray-600" />
                  <span>{{ t('payments.noTypes') }}</span>
                </div>
              </td>
            </tr>
            <tr
              v-for="pt in paymentTypes"
              :key="pt.id"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50/80 dark:border-gray-800 dark:hover:bg-white/[0.02]"
            >
              <td class="td text-gray-400">{{ pt.id }}</td>
              <td class="td">
                <div class="flex items-center gap-3">
                  <span
                    class="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-400"
                  >
                    <component :is="paymentTypeIcon(pt.icon)" class="h-4 w-4" />
                  </span>
                  <span class="font-medium text-gray-800 dark:text-white/90">{{ pt.name }}</span>
                </div>
              </td>
              <td class="td td-actions">
                <RowActions
                  :edit="auth.can('PAYMENT_TYPE_EDIT')"
                  :remove="auth.can('PAYMENT_TYPE_DELETE')"
                  @edit="openTypeEdit(pt)"
                  @delete="onTypeDelete(pt)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Payment modal -->
    <div v-if="modalOpen" class="overlay">
      <div
        class="modal modal-lg"
        role="dialog"
        aria-modal="true"
        aria-labelledby="payment-modal-title"
      >
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-400"
            >
              <Banknote class="h-5 w-5" />
            </span>
            <div>
              <h3
                id="payment-modal-title"
                class="text-lg font-semibold text-gray-800 dark:text-white/90"
              >
                {{ editingId ? t('payments.editPayment') : t('payments.newPayment') }}
              </h3>
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {{ t('payments.modalSubtitle') }}
              </p>
            </div>
          </div>
          <button
            type="button"
            class="close-btn"
            :aria-label="t('common.close')"
            @click="closePaymentModal"
          >
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="formError" class="err mb-4">{{ formError }}</div>

        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">{{ t('common.client') }} <span class="req">*</span></label>
            <SearchableSelect
              :model-value="form.clientId"
              :options="clientOptions"
              :placeholder="t('payments.selectClientPlaceholder')"
              :search-placeholder="t('payments.clientSearchPlaceholder')"
              @update:model-value="onFormClientChange"
            />
          </div>

          <div v-if="form.clientId && (clientSummaryLoading || clientBalance)" class="debt-card">
            <div v-if="clientSummaryLoading" class="debt-loading">{{ t('payments.debtLoading') }}</div>
            <template v-else-if="clientBalance">
              <div class="debt-head">
                <span class="debt-title">{{ t('payments.clientBalance') }}</span>
                <span class="debt-orders">{{ t('payments.ordersCount', { n: clientSaleOrders.length }) }}</span>
              </div>
              <div class="debt-grid">
                <div class="debt-item">
                  <span class="debt-lbl">{{ t('payments.totalSales') }}</span>
                  <span class="debt-val">{{ money(clientBalance.totalPurchase) }}</span>
                </div>
                <div class="debt-item">
                  <span class="debt-lbl">{{ t('payments.totalPayments') }}</span>
                  <span class="debt-val text-success-600 dark:text-success-400">
                    {{ money(clientBalance.totalPaid) }}
                    <span v-if="clientPaymentsCount" class="debt-sub">{{ t('payments.paymentsCount', { n: clientPaymentsCount }) }}</span>
                  </span>
                </div>
                <div class="debt-item debt-item-accent">
                  <span class="debt-lbl">{{ t('payments.totalDebt') }}</span>
                  <span
                    class="debt-val"
                    :class="Number(clientBalance.totalDebt) > 0 ? 'text-error-600 dark:text-error-400' : 'text-success-600'"
                  >
                    {{ money(clientBalance.totalDebt) }}
                  </span>
                </div>
              </div>
              <div v-if="clientOrdersWithDebt.length" class="debt-orders-list">
                <div
                  v-for="o in clientOrdersWithDebt"
                  :key="o.id"
                  class="debt-order-row"
                  :class="{ active: form.saleOrderId === o.id }"
                  role="button"
                  tabindex="0"
                  @click="form.saleOrderId = o.id"
                  @keydown.enter.prevent="form.saleOrderId = o.id"
                >
                  <span>#{{ o.id }}</span>
                  <span class="text-gray-400">{{ money(o.totalSum) }}</span>
                  <span class="font-medium text-error-600 dark:text-error-400">
                    {{ t('payments.debtLabel', { sum: money(o.debtSum) }) }}
                  </span>
                </div>
              </div>
              <p v-else-if="clientSaleOrders.length" class="debt-hint">
                {{ t('payments.noDebtOrders') }}
              </p>
            </template>
          </div>

          <div>
            <label for="pay-user" class="lbl">{{ t('payments.receiver') }} <span class="req">*</span></label>
            <div class="relative">
              <UserCog class="field-icon" />
              <select id="pay-user" v-model.number="form.userId" required class="field field-select">
                <option v-for="u in users" :key="u.id" :value="u.id">
                  {{ u.fullName || u.username }}
                </option>
              </select>
              <ChevronDown class="select-chevron" />
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="pay-type" class="lbl">{{ t('payments.paymentType') }} <span class="req">*</span></label>
              <div class="relative">
                <component :is="iconForTypeId(form.paymentTypeId)" class="field-icon" />
                <select
                  id="pay-type"
                  v-model.number="form.paymentTypeId"
                  required
                  class="field field-select"
                >
                  <option v-for="pt in paymentTypes" :key="pt.id" :value="pt.id">{{ pt.name }}</option>
                </select>
                <ChevronDown class="select-chevron" />
              </div>
            </div>

            <div>
              <label for="pay-amount" class="lbl">{{ t('common.sum') }} <span class="req">*</span></label>
              <div class="relative">
                <Banknote class="field-icon" />
                <input
                  id="pay-amount"
                  :value="amountText"
                  inputmode="decimal"
                  autocomplete="off"
                  required
                  placeholder="0"
                  class="field"
                  @input="onAmountInput"
                />
              </div>
            </div>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="pay-date" class="lbl">{{ t('common.date') }} <span class="req">*</span></label>
              <div class="relative">
                <CalendarDays class="field-icon" />
                <input
                  id="pay-date"
                  v-model="form.paymentDate"
                  type="datetime-local"
                  required
                  class="field"
                />
              </div>
            </div>

            <div>
              <label for="pay-order" class="lbl">{{ t('payments.saleOrder') }}</label>
              <div class="relative">
                <ShoppingCart class="field-icon" />
                <select
                  id="pay-order"
                  v-model.number="form.saleOrderId"
                  class="field field-select"
                  :disabled="!form.clientId"
                >
                  <option :value="0">{{ t('payments.advanceOption') }}</option>
                  <option v-for="o in clientSaleOrders" :key="o.id" :value="o.id">
                    {{ formatOrderOption(o) }}
                  </option>
                </select>
                <ChevronDown class="select-chevron" />
              </div>
              <p v-if="!form.clientId" class="mt-1 text-xs text-gray-400">{{ t('payments.selectClientFirst') }}</p>
            </div>
          </div>

          <div>
            <label for="pay-comment" class="lbl">{{ t('common.note') }}</label>
            <div class="relative">
              <MessageSquare class="field-icon field-icon-top" />
              <textarea
                id="pay-comment"
                v-model="form.comment"
                rows="2"
                :placeholder="t('payments.commentPlaceholder')"
                class="field field-textarea"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="closePaymentModal">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving">
              <Check class="h-4 w-4" />
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Payment type modal -->
    <div v-if="typeModal" class="overlay">
      <div
        class="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="type-modal-title"
      >
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-50 text-brand-500 dark:bg-brand-500/10 dark:text-brand-400"
            >
              <component :is="paymentTypeIcon(typeIcon)" class="h-5 w-5" />
            </span>
            <div>
              <h3
                id="type-modal-title"
                class="text-lg font-semibold text-gray-800 dark:text-white/90"
              >
                {{ typeEditingId ? t('payments.editType') : t('payments.newPaymentType') }}
              </h3>
              <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                {{ t('payments.typeExample') }}
              </p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="closeTypeModal">
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="typeFormError" class="err mb-4">{{ typeFormError }}</div>

        <form class="space-y-4" @submit.prevent="onTypeSubmit">
          <div>
            <label for="type-name" class="lbl">{{ t('payments.typeName') }} <span class="req">*</span></label>
            <div class="relative">
              <Tag class="field-icon" />
              <input
                id="type-name"
                v-model="typeName"
                required
                :placeholder="t('payments.typeNamePlaceholder')"
                class="field"
              />
            </div>
          </div>
          <div>
            <span class="lbl">{{ t('payments.typeIcon') }}</span>
            <div class="icon-grid" role="radiogroup" :aria-label="t('payments.typeIcon')">
              <button
                v-for="opt in PAYMENT_TYPE_ICONS"
                :key="opt.key"
                type="button"
                role="radio"
                class="icon-opt"
                :class="{ active: typeIcon === opt.key }"
                :aria-checked="typeIcon === opt.key"
                @click="typeIcon = opt.key"
              >
                <component :is="opt.icon" class="h-5 w-5" />
                <span>{{ t(`payments.icons.${opt.key}`) }}</span>
              </button>
            </div>
          </div>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="closeTypeModal">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="typeSaving">
              <Check class="h-4 w-4" />
              {{ typeSaving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Banknote,
  CalendarDays,
  Check,
  ChevronDown,
  CreditCard,
  ListFilter,
  MessageSquare,
  Plus,
  RefreshCw,
  Search,
  ShoppingCart,
  Tag,
  UserCog,
  Wallet,
  X,
} from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import {
  createPayment,
  createPaymentType,
  deletePayment,
  deletePaymentType,
  fetchPayments,
  fetchPaymentsByClient,
  fetchPaymentTypes,
  updatePayment,
  updatePaymentType,
  type Payment,
  type PaymentType,
} from '@/api/payments'
import { fetchClients, type Client } from '@/api/clients'
import { fetchClientBalance, type ClientBalance } from '@/api/clientBalances'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { fetchSaleOrdersByClient, type SaleOrder } from '@/api/sales'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { amountToText, formatAmountInput, formatDate, money, nowLocal, toApiDate, today } from '@/utils/format'
import { formatUzPhone } from '@/utils/phone'
import {
  DEFAULT_PAYMENT_TYPE_ICON,
  PAYMENT_TYPE_ICONS,
  paymentTypeIcon,
  type PaymentTypeIconKey,
} from '@/utils/paymentTypeIcons'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const tab = ref<'payments' | 'types'>('payments')
const items = ref<Payment[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const paymentTypes = ref<PaymentType[]>([])
const clientSaleOrders = ref<SaleOrder[]>([])
const clientBalance = ref<ClientBalance | null>(null)
const clientPaymentsCount = ref(0)
const clientSummaryLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const typeSaving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const typeFormError = ref<string | null>(null)
const search = ref('')
const showFilters = ref(true)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const filterClientId = ref(0)
const typeModal = ref(false)
const typeEditingId = ref<number | null>(null)
const typeName = ref('')
const typeIcon = ref<PaymentTypeIconKey>(DEFAULT_PAYMENT_TYPE_ICON)
const amountText = ref('')
const auth = useAuthStore()
const form = reactive({
  clientId: 0,
  userId: 0,
  paymentTypeId: 0,
  paymentAmount: 0,
  paymentDate: '',
  saleOrderId: 0,
  comment: '',
})

function iconForTypeId(id?: number | null) {
  return paymentTypeIcon(paymentTypes.value.find((pt) => pt.id === id)?.icon)
}

function onAmountInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  amountText.value = text
  el.value = text
  form.paymentAmount = value
}

function clientLabel(c: Client) {
  const phone = c.phone ? formatUzPhone(c.phone) : ''
  return phone ? `${c.fullName} · ${phone}` : c.fullName
}

function formatOrderOption(o: SaleOrder) {
  const debt = Number(o.debtSum || 0)
  const base = `#${o.id} · ${money(o.totalSum)}`
  return debt > 0 ? `${base} (${t('payments.debtLabel', { sum: money(debt) })})` : base
}

const clientOptions = computed(() =>
  clients.value.map((c) => ({
    value: c.id,
    label: clientLabel(c),
    searchText: `${c.fullName} ${c.phone || ''}`,
  })),
)

const filterClientOptions = computed(() => [
  { value: 0, label: t('payments.allClients'), searchText: '' },
  ...clientOptions.value,
])

const clientOrdersWithDebt = computed(() =>
  clientSaleOrders.value.filter((o) => Number(o.debtSum || 0) > 0),
)

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((p) =>
    [p.clientFullName, p.paymentTypeName, String(p.saleOrderId || ''), p.comment].some((v) =>
      String(v || '')
        .toLowerCase()
        .includes(q),
    ),
  )
})

function onFilterClientChange(id: number) {
  filterClientId.value = id
  load()
}

function clearFilters() {
  filterClientId.value = 0
  search.value = ''
  load()
}

function clearClientSummary() {
  clientBalance.value = null
  clientSaleOrders.value = []
  clientPaymentsCount.value = 0
  clientSummaryLoading.value = false
}

async function loadClientSummary(clientId: number) {
  if (!clientId) {
    clearClientSummary()
    return
  }
  clientSummaryLoading.value = true
  try {
    const [balRes, ordersRes, paysRes] = await Promise.all([
      fetchClientBalance(clientId, { fromDate: '2000-01-01', toDate: today() }),
      fetchSaleOrdersByClient(clientId, 0, 200),
      fetchPaymentsByClient(clientId, 0, 200),
    ])
    clientBalance.value = balRes.data || null
    clientSaleOrders.value = ordersRes.data?.content || []
    const pays = paysRes.data?.content || []
    clientPaymentsCount.value = paysRes.data?.totalElements ?? pays.length
    if (
      form.saleOrderId &&
      !clientSaleOrders.value.some((o) => o.id === form.saleOrderId)
    ) {
      form.saleOrderId = 0
    }
  } catch (e) {
    formError.value = formatApiError(e)
    clearClientSummary()
  } finally {
    clientSummaryLoading.value = false
  }
}

function onFormClientChange(id: number) {
  form.clientId = id
  form.saleOrderId = 0
  formError.value = null
  loadClientSummary(id)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [clientsRes, usersRes, typesRes] = await Promise.all([
      fetchClients(),
      fetchUserOptions(),
      fetchPaymentTypes(),
    ])
    clients.value = clientsRes.data || []
    users.value = usersRes.data || []
    paymentTypes.value = typesRes.data?.content || []
    const payRes = filterClientId.value
      ? await fetchPaymentsByClient(filterClientId.value)
      : await fetchPayments()
    items.value = payRes.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

function fillForm(p?: Payment) {
  const me = users.value.find((u) => u.username === auth.username)
  form.clientId = p?.clientId || 0
  form.userId = p?.userId || me?.id || users.value[0]?.id || 0
  form.paymentTypeId = p?.paymentTypeId || paymentTypes.value[0]?.id || 0
  form.paymentAmount = Number(p?.paymentAmount || 0)
  amountText.value = amountToText(form.paymentAmount)
  form.paymentDate = p?.paymentDate ? p.paymentDate.slice(0, 16) : nowLocal()
  form.saleOrderId = p?.saleOrderId || 0
  form.comment = p?.comment || ''
}

function closePaymentModal() {
  modalOpen.value = false
  clearClientSummary()
}

function openCreate() {
  editingId.value = null
  fillForm()
  clearClientSummary()
  formError.value = null
  modalOpen.value = true
}

function openEdit(p: Payment) {
  editingId.value = p.id
  fillForm(p)
  formError.value = null
  modalOpen.value = true
  if (form.clientId) loadClientSummary(form.clientId)
  else clearClientSummary()
}

async function onSubmit() {
  if (!form.clientId) {
    formError.value = t('payments.selectClient')
    return
  }
  if (!(form.paymentAmount > 0)) {
    formError.value = t('payments.amountRequired')
    return
  }
  saving.value = true
  formError.value = null
  try {
    const payload = {
      clientId: form.clientId,
      userId: form.userId,
      paymentTypeId: form.paymentTypeId,
      paymentAmount: form.paymentAmount,
      paymentDate: toApiDate(form.paymentDate),
      saleOrderId: form.saleOrderId || null,
      comment: form.comment || undefined,
    }
    if (editingId.value) await updatePayment(editingId.value, payload)
    else await createPayment(payload)
    modalOpen.value = false
    clearClientSummary()
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(p: Payment) {
  if (!confirm(t('payments.deleteConfirm', { id: p.id }))) return
  try {
    await deletePayment(p.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function closeTypeModal() {
  typeModal.value = false
}

function openTypeCreate() {
  typeEditingId.value = null
  typeName.value = ''
  typeIcon.value = DEFAULT_PAYMENT_TYPE_ICON
  typeFormError.value = null
  typeModal.value = true
}

function openTypeEdit(t: PaymentType) {
  typeEditingId.value = t.id
  typeName.value = t.name
  typeIcon.value = (t.icon as PaymentTypeIconKey) || DEFAULT_PAYMENT_TYPE_ICON
  typeFormError.value = null
  typeModal.value = true
}

async function onTypeSubmit() {
  typeSaving.value = true
  typeFormError.value = null
  try {
    if (typeEditingId.value) await updatePaymentType(typeEditingId.value, typeName.value.trim(), typeIcon.value)
    else await createPaymentType(typeName.value.trim(), typeIcon.value)
    typeModal.value = false
    await load()
  } catch (e) {
    typeFormError.value = formatApiError(e)
  } finally {
    typeSaving.value = false
  }
}

async function onTypeDelete(pt: PaymentType) {
  if (!confirm(t('common.deleteConfirmNamed', { name: pt.name }))) return
  try {
    await deletePaymentType(pt.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.th {
  padding: 0.75rem 1.25rem;
  text-align: start;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
}
.th-actions {
  width: 6.5rem;
  text-align: end;
  white-space: nowrap;
}
.td {
  padding: 0.875rem 1.25rem;
  font-size: 0.875rem;
  color: #4b5563;
}
.td-actions {
  width: 6.5rem;
  text-align: end;
  white-space: nowrap;
}
.empty {
  padding: 2.5rem 1rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.lbl {
  display: block;
  margin-bottom: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}
.lbl-block {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
}
.req {
  color: #ef4444;
}
.field-icon {
  position: absolute;
  top: 50%;
  inset-inline-start: 0.75rem;
  z-index: 10;
  height: 1.1rem;
  width: 1.1rem;
  transform: translateY(-50%);
  color: #98a2b3;
  pointer-events: none;
}
.field-icon-top {
  top: 0.85rem;
  transform: none;
}
.select-chevron {
  pointer-events: none;
  position: absolute;
  top: 50%;
  inset-inline-end: 0.75rem;
  height: 1rem;
  width: 1rem;
  transform: translateY(-50%);
  color: #9ca3af;
}
.field {
  height: 2.75rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem 0 2.5rem;
  font-size: 0.875rem;
  color: #1f2937;
  outline: none;
}
.field:focus {
  border-color: #9cb0ff;
  box-shadow: 0 0 0 4px rgb(70 95 255 / 10%);
}
.field-select {
  appearance: none;
  padding-inline-end: 2.5rem;
}
.field-textarea {
  height: auto;
  min-height: 4.5rem;
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
  resize: vertical;
}
.btn {
  display: inline-flex;
  height: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1.25rem;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
}
.btn:disabled {
  opacity: 0.6;
}
.btn-with-icon {
  gap: 0.4rem;
}
.ghost {
  display: inline-flex;
  height: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1.25rem;
  font-size: 0.875rem;
  color: #374151;
}
.icon-btn {
  display: inline-flex;
  height: 2.75rem;
  width: 2.75rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  color: #4b5563;
  background: transparent;
}
.icon-btn:hover {
  background: #f9fafb;
}
.icon-btn:disabled {
  opacity: 0.5;
}
.close-btn {
  display: flex;
  height: 2.25rem;
  width: 2.25rem;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  color: #9ca3af;
}
.close-btn:hover {
  background: #f3f4f6;
  color: #374151;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
  font-size: 0.875rem;
}
.tab {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.5rem;
  border-radius: 0.625rem;
  border: 1px solid #d1d5db;
  padding: 0 0.875rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
  background: #fff;
}
.tab.active {
  background: #465fff;
  color: #fff;
  border-color: #465fff;
}
.tab-count {
  display: inline-flex;
  min-width: 1.25rem;
  height: 1.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 0 0.35rem;
  font-size: 0.7rem;
  font-weight: 600;
  background: rgb(0 0 0 / 8%);
}
.tab.active .tab-count {
  background: rgb(255 255 255 / 20%);
}
.type-badge {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 0.35rem;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #374151;
}
.overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  padding: 1rem;
}
.modal {
  width: 100%;
  max-width: 28rem;
  max-height: calc(100vh - 2rem);
  overflow-y: auto;
  border-radius: 1rem;
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 20px 40px rgb(0 0 0 / 12%);
}
.modal-lg {
  max-width: 36rem;
}
.debt-card {
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 0.875rem 1rem;
}
.debt-loading {
  font-size: 0.8125rem;
  color: #6b7280;
}
.debt-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
.debt-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #374151;
}
.debt-orders {
  font-size: 0.75rem;
  color: #6b7280;
}
.debt-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}
.debt-item {
  border-radius: 0.5rem;
  background: #fff;
  border: 1px solid #eef0f3;
  padding: 0.5rem 0.65rem;
}
.debt-item-accent {
  border-color: #fecaca;
  background: #fff7f7;
}
.debt-lbl {
  display: block;
  font-size: 0.7rem;
  color: #6b7280;
  margin-bottom: 0.15rem;
}
.debt-val {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #1f2937;
  word-break: break-word;
}
.debt-sub {
  font-weight: 400;
  font-size: 0.7rem;
  color: #9ca3af;
}
.debt-orders-list {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-height: 8rem;
  overflow-y: auto;
}
.debt-order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 0.45rem 0.65rem;
  font-size: 0.75rem;
  color: #374151;
  cursor: pointer;
}
.debt-order-row:hover,
.debt-order-row.active {
  border-color: #9cb0ff;
  background: #eff4ff;
}
.debt-hint {
  margin-top: 0.65rem;
  font-size: 0.75rem;
  color: #6b7280;
}
.dark .debt-card {
  border-color: #344054;
  background: rgb(255 255 255 / 4%);
}
.dark .debt-title {
  color: rgba(255, 255, 255, 0.9);
}
.dark .debt-item {
  background: #101828;
  border-color: #344054;
}
.dark .debt-item-accent {
  background: rgb(240 68 56 / 8%);
  border-color: rgb(240 68 56 / 30%);
}
.dark .debt-val {
  color: rgba(255, 255, 255, 0.9);
}
.dark .debt-order-row {
  background: #101828;
  border-color: #344054;
  color: #d1d5db;
}
.dark .debt-order-row:hover,
.dark .debt-order-row.active {
  border-color: #465fff;
  background: rgb(70 95 255 / 12%);
}
.dark .lbl,
.dark .lbl-block {
  color: #9ca3af;
}
.dark .field {
  border-color: #344054;
  color: rgba(255, 255, 255, 0.9);
}
.dark .ghost,
.dark .icon-btn,
.dark .tab {
  border-color: #344054;
  color: #d1d5db;
  background: transparent;
}
.dark .icon-btn:hover,
.dark .close-btn:hover {
  background: rgb(255 255 255 / 5%);
  color: rgba(255, 255, 255, 0.8);
}
.dark .modal {
  background: #101828;
  border: 1px solid #344054;
}
.dark .type-badge {
  border-color: #344054;
  background: rgb(255 255 255 / 4%);
  color: #d1d5db;
}
.dark .td {
  color: #9ca3af;
}
.icon-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.5rem;
}
.icon-opt {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  border-radius: 0.625rem;
  border: 1px solid #e5e7eb;
  padding: 0.65rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #4b5563;
  transition: border-color 0.15s, background 0.15s, color 0.15s;
}
.icon-opt:hover {
  border-color: #9cb0ff;
}
.icon-opt.active {
  border-color: #465fff;
  background: #eff4ff;
  color: #465fff;
  box-shadow: 0 0 0 3px rgb(70 95 255 / 12%);
}
.dark .icon-opt {
  border-color: #344054;
  color: #d1d5db;
}
.dark .icon-opt.active {
  border-color: #465fff;
  background: rgb(70 95 255 / 15%);
  color: #9cb0ff;
}
</style>
