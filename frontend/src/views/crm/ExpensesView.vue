<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.expenses')" />

    <section class="mb-5">
      <div class="mb-3 flex items-center justify-between gap-3">
        <div class="min-w-0">
          <h3 class="flex items-center gap-2 text-base font-semibold text-gray-800 dark:text-white/90">
            <Wallet class="h-4 w-4 text-brand-500" />
            {{ t('expenses.cashTitle') }}
          </h3>
          <p class="text-xs text-gray-500 dark:text-gray-400">{{ t('expenses.cashHint') }}</p>
        </div>
        <button
          type="button"
          class="close-btn"
          :title="t('expenses.refresh')"
          :aria-label="t('expenses.refresh')"
          :disabled="cashLoading"
          @click="loadCash()"
        >
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': cashLoading }" />
        </button>
      </div>
      <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        <button
          v-for="c in cash"
          :key="c.paymentTypeId"
          type="button"
          class="kassa-card"
          :class="[`tone-${cashKind(c)}`, { negative: c.available < 0, flash: cashDelta[c.paymentTypeId] }]"
          :disabled="writeBlocked"
          :title="t('expenses.spendFrom')"
          @click="openExpenseCreate(c)"
        >
          <span class="flex items-start justify-between gap-2">
            <span class="kassa-icon"><component :is="cashIcon(c)" class="h-5 w-5" /></span>
            <span class="cur-badge" :class="c.currency === 'USD' ? 'usd' : 'uzs'">
              {{ c.currency === 'USD' ? '$' : t('expenses.uzs') }}
            </span>
          </span>
          <span class="mt-3 block truncate text-sm text-gray-500 dark:text-gray-400">{{ c.paymentTypeName }}</span>
          <span class="kassa-amount">{{ moneyIn(c.available, c.currency, t('common.currency')) }}</span>
          <span v-if="cashDelta[c.paymentTypeId]" class="kassa-delta" :class="{ up: cashDelta[c.paymentTypeId] > 0 }">
            {{ cashDelta[c.paymentTypeId] > 0 ? '+' : '−' }}{{ moneyIn(Math.abs(cashDelta[c.paymentTypeId]), c.currency, t('common.currency')) }}
          </span>
        </button>
        <button
          v-for="tot in currencyTotals"
          :key="tot.currency"
          type="button"
          class="kassa-card total"
          :class="[tot.currency === 'USD' ? 'total-usd' : 'total-uzs', { negative: tot.amount < 0, flash: tot.delta }]"
          :disabled="writeBlocked || tot.count === 0"
          :title="t('expenses.spendFrom')"
          @click="openExpenseCreate({ currency: tot.currency })"
        >
          <span class="flex items-start justify-between gap-2">
            <span class="kassa-icon"><component :is="tot.currency === 'USD' ? DollarSign : Coins" class="h-5 w-5" /></span>
            <span class="kassa-count">{{ t('expenses.cashCount', { n: tot.count }) }}</span>
          </span>
          <span class="mt-3 block truncate text-sm text-gray-500 dark:text-gray-400">
            {{ tot.currency === 'USD' ? t('expenses.totalUsd') : t('expenses.totalUzs') }}
          </span>
          <span class="kassa-amount">{{ moneyIn(tot.amount, tot.currency, t('common.currency')) }}</span>
          <span v-if="tot.delta" class="kassa-delta" :class="{ up: tot.delta > 0 }">
            {{ tot.delta > 0 ? '+' : '−' }}{{ moneyIn(Math.abs(tot.delta), tot.currency, t('common.currency')) }}
          </span>
        </button>
      </div>
    </section>

    <div class="mb-4 flex gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'expenses' }" @click="tab = 'expenses'">{{ t('nav.expenses') }}</button>
      <button type="button" class="tab" :class="{ active: tab === 'categories' }" @click="tab = 'categories'">{{ t('expenses.categories') }}</button>
    </div>

    <!-- Expenses -->
    <div v-show="tab === 'expenses'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 lg:flex-row lg:items-center lg:justify-between dark:border-gray-800">
        <div class="flex flex-wrap items-center gap-3">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.expenses') }}</h3>
          <div class="seg" role="radiogroup" :aria-label="t('expenses.currencyLabel')">
            <button
              v-for="o in listCurrencyOptions"
              :key="o.id"
              type="button"
              role="radio"
              class="seg-btn"
              :class="{ active: listCurrency === o.id }"
              :aria-checked="listCurrency === o.id"
              @click="listCurrency = o.id"
            >
              {{ o.label }}
            </button>
          </div>
        </div>
        <div class="flex flex-wrap gap-2">
          <input v-model="expenseSearch" type="search" :placeholder="t('common.search')" class="field sm:w-56" />
          <button type="button" class="btn btn-with-icon" :disabled="writeBlocked" @click="openExpenseCreate()">
            <Plus class="h-4 w-4" />
            {{ t('expenses.createExpense') }}
          </button>
        </div>
      </div>
      <div v-if="listTotals.length" class="flex flex-wrap gap-2 border-b border-gray-100 px-5 py-3 dark:border-gray-800">
        <span v-for="tot in listTotals" :key="tot.currency" class="total-chip" :class="tot.currency === 'USD' ? 'usd' : 'uzs'">
          <component :is="tot.currency === 'USD' ? DollarSign : Coins" class="h-4 w-4" />
          {{ t('expenses.totalIn') }}: <b>{{ moneyIn(tot.amount, tot.currency, t('common.currency')) }}</b>
        </span>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('expenses.category') }}</th>
              <th class="th">{{ t('expenses.cashbox') }}</th>
              <th class="th text-right">{{ t('common.amount') }}</th>
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('common.note') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filteredExpenses.length === 0"><td colspan="7" class="empty">{{ t('expenses.empty') }}</td></tr>
            <tr v-for="e in filteredExpenses" :key="e.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ e.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ e.categoryName || '—' }}</td>
              <td class="td">
                <span v-if="e.paymentTypeName" class="inline-flex items-center gap-1.5">
                  <Wallet class="h-3.5 w-3.5 text-gray-400" />
                  {{ e.paymentTypeName }}
                </span>
                <span v-else class="text-xs text-gray-400" :title="t('expenses.noCashHint')">{{ t('expenses.noCash') }}</span>
              </td>
              <td class="td whitespace-nowrap text-right font-medium text-gray-800 dark:text-white/90">
                {{ moneyIn(e.amount, e.currency, t('common.currency')) }}
                <span class="cur-badge" :class="e.currency === 'USD' ? 'usd' : 'uzs'">{{ e.currency || 'UZS' }}</span>
              </td>
              <td class="td whitespace-nowrap">{{ e.expenseDate }}</td>
              <td class="td">
                {{ e.description || '—' }}
                <router-link
                  v-if="e.supplierPaymentId"
                  to="/suppliers/balances"
                  class="ms-1 inline-flex rounded-md bg-brand-50 px-1.5 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/10 dark:text-brand-400"
                  :title="t('expenses.supplierPaymentHint')"
                >
                  {{ t('expenses.supplierPaymentBadge') }}
                </router-link>
              </td>
              <td class="td text-right">
                <RowActions
                  :edit="!e.supplierPaymentId"
                  :remove="!e.supplierPaymentId"
                  @edit="openExpenseEdit(e)"
                  @delete="onExpenseDelete(e)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Categories -->
    <div v-show="tab === 'categories'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('expenses.categories') }}</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openCategoryCreate">{{ t('expenses.newCategory') }}</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.name') }}</th>
              <th class="th">{{ t('common.note') }}</th>
              <th class="th">{{ t('expenses.status') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="categories.length === 0"><td colspan="5" class="empty">{{ t('expenses.emptyCategories') }}</td></tr>
            <tr v-for="c in categories" :key="c.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ c.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ c.name }}</td>
              <td class="td">{{ c.description || '—' }}</td>
              <td class="td">{{ c.status || 'ACTIVE' }}</td>
              <td class="td text-right">
                <RowActions @edit="openCategoryEdit(c)" @delete="onCategoryDelete(c)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Expense modal -->
    <div v-if="expenseModal" class="overlay">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="expense-modal-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge expense"><Receipt class="h-5 w-5" /></span>
            <div>
              <h3 id="expense-modal-title" class="title">
                {{ expenseEditingId ? t('expenses.editExpense') : t('expenses.createExpense') }}
              </h3>
              <p class="sub">{{ t('expenses.modalSubtitle') }}</p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="expenseModal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-4" @submit.prevent="onExpenseSubmit">
          <div>
            <span class="lbl">{{ t('expenses.currencyLabel') }} <span class="req">*</span></span>
            <div class="cur-switch" role="radiogroup" :aria-label="t('expenses.currencyLabel')">
              <button
                v-for="c in CURRENCIES"
                :key="c"
                type="button"
                role="radio"
                class="cur-switch-btn"
                :class="{ active: expenseForm.currency === c }"
                :aria-checked="expenseForm.currency === c"
                @click="setFormCurrency(c)"
              >
                <component :is="c === 'USD' ? DollarSign : Coins" class="h-4 w-4" />
                {{ c === 'USD' ? t('expenses.usd') : t('expenses.uzs') }}
              </button>
            </div>
          </div>

          <div>
            <span class="lbl">{{ t('expenses.cashbox') }} <span class="req">*</span></span>
            <p v-if="cashOptions.length === 0" class="hint-box">{{ t('expenses.noCashbox') }}</p>
            <div v-else class="grid grid-cols-1 gap-2 sm:grid-cols-2">
              <button
                v-for="c in cashOptions"
                :key="c.paymentTypeId"
                type="button"
                class="cash-card"
                :class="{ active: expenseForm.paymentTypeId === c.paymentTypeId, empty: c.available <= 0 }"
                @click="expenseForm.paymentTypeId = c.paymentTypeId"
              >
                <span class="cash-icon"><component :is="cashIcon(c)" class="h-4 w-4" /></span>
                <span class="min-w-0 text-start">
                  <span class="block truncate text-sm font-medium">{{ c.paymentTypeName }}</span>
                  <span class="cash-balance">{{ t('expenses.available', { value: cashMoney(c.available) }) }}</span>
                </span>
                <CheckCircle2 v-if="expenseForm.paymentTypeId === c.paymentTypeId" class="ms-auto h-4 w-4 shrink-0 text-brand-500" />
              </button>
            </div>
          </div>

          <div>
            <label for="expense-category" class="lbl">{{ t('expenses.category') }} <span class="req">*</span></label>
            <div class="relative">
              <Tag class="field-icon" />
              <select id="expense-category" v-model.number="expenseForm.categoryId" required class="field field-with-icon">
                <option :value="0" disabled>{{ t('common.select') }}</option>
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <div class="field-head">
                <label for="expense-amount" class="lbl">{{ t('common.amount') }} <span class="req">*</span></label>
                <button
                  v-if="selectedCash && selectedAvailable > 0"
                  type="button"
                  class="link-btn"
                  @click="expenseForm.amount = selectedAvailable"
                >
                  {{ t('expenses.useAll') }}
                </button>
              </div>
              <div class="relative">
                <Banknote class="field-icon" />
                <input
                  id="expense-amount"
                  v-model.number="expenseForm.amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  required
                  class="field field-with-icon field-with-suffix"
                  :class="{ invalid: exceedsCash }"
                  :placeholder="t('expenses.amountPlaceholder')"
                />
                <span class="field-suffix">{{ expenseForm.currency === 'USD' ? '$' : t('common.currency') }}</span>
              </div>
              <p v-if="exceedsCash" class="field-err">{{ t('expenses.exceeds', { value: cashMoney(selectedAvailable) }) }}</p>
              <p v-else-if="selectedCash && Number(expenseForm.amount || 0) > 0" class="field-after">
                {{ t('expenses.afterExpense', { value: cashMoney(selectedAvailable - Number(expenseForm.amount || 0)) }) }}
              </p>
            </div>
            <div>
              <div class="field-head">
                <label for="expense-date" class="lbl">{{ t('common.date') }} <span class="req">*</span></label>
              </div>
              <div class="relative">
                <CalendarDays class="field-icon" />
                <input id="expense-date" v-model="expenseForm.expenseDate" type="date" required class="field field-with-icon" />
              </div>
            </div>
          </div>

          <div>
            <label for="expense-note" class="lbl">{{ t('common.note') }}</label>
            <div class="relative">
              <MessageSquare class="field-icon" />
              <input
                id="expense-note"
                v-model="expenseForm.description"
                maxlength="500"
                class="field field-with-icon"
                :placeholder="t('expenses.notePlaceholder')"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="expenseModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving || !canSubmitExpense">
              <Check class="h-4 w-4" />
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Category modal -->
    <div v-if="categoryModal" class="overlay">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="category-modal-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge"><Tag class="h-5 w-5" /></span>
            <h3 id="category-modal-title" class="title self-center">
              {{ categoryEditingId ? t('expenses.editCategory') : t('expenses.createCategory') }}
            </h3>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="categoryModal = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-4" @submit.prevent="onCategorySubmit">
          <div>
            <label for="category-name" class="lbl">{{ t('common.name') }} <span class="req">*</span></label>
            <div class="relative">
              <Tag class="field-icon" />
              <input id="category-name" v-model="categoryForm.name" required class="field field-with-icon" />
            </div>
          </div>
          <div>
            <label for="category-note" class="lbl">{{ t('common.note') }}</label>
            <div class="relative">
              <MessageSquare class="field-icon" />
              <input id="category-note" v-model="categoryForm.description" class="field field-with-icon" />
            </div>
          </div>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="categoryModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving">
              <Check class="h-4 w-4" />
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Banknote,
  CalendarDays,
  Check,
  CheckCircle2,
  Coins,
  DollarSign,
  MessageSquare,
  Plus,
  Receipt,
  RefreshCw,
  Tag,
  Wallet,
  X,
} from 'lucide-vue-next'
import { paymentTypeIcon } from '@/utils/paymentTypeIcons'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createExpense,
  createExpenseCategory,
  deleteExpense,
  deleteExpenseCategory,
  fetchCashAvailable,
  fetchExpenseCategories,
  fetchExpenses,
  updateExpense,
  updateExpenseCategory,
  type CashAvailable,
  type Expense,
  type ExpenseCategory,
} from '@/api/expenses'
import { ApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const tab = ref<'expenses' | 'categories'>('expenses')
const expenses = ref<Expense[]>([])
const categories = ref<ExpenseCategory[]>([])
const cash = ref<CashAvailable[]>([])
const cashLoading = ref(false)
/** Oxirgi saqlash/o'chirishdan keyin kassa qoldig'i qanchaga o'zgargani (paymentTypeId bo'yicha). */
const cashDelta = ref<Record<number, number>>({})
let cashDeltaTimer: ReturnType<typeof setTimeout> | undefined
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const expenseSearch = ref('')
const listCurrency = ref<'ALL' | CurrencyCode>('ALL')

const expenseModal = ref(false)
const expenseEditingId = ref<number | null>(null)
/** Tahrirlanayotgan xarajat oldin qaysi kassadan qancha olgan: shu summa tanlov uchun qoldiqqa qaytariladi. */
const editingSpend = ref<{ paymentTypeId: number; currency: CurrencyCode; amount: number } | null>(null)
const expenseForm = reactive({
  categoryId: 0,
  currency: 'UZS' as CurrencyCode,
  paymentTypeId: 0,
  amount: null as number | null,
  expenseDate: '',
  description: '',
})

const categoryModal = ref(false)
const categoryEditingId = ref<number | null>(null)
const categoryForm = reactive({ name: '', description: '' })

const listCurrencyOptions = computed(() => [
  { id: 'ALL' as const, label: t('common.all') },
  { id: 'UZS' as const, label: t('expenses.uzs') },
  { id: 'USD' as const, label: '$' },
])

const filteredExpenses = computed(() => {
  const q = expenseSearch.value.trim().toLowerCase()
  return expenses.value.filter((e) => {
    if (listCurrency.value !== 'ALL' && (e.currency || 'UZS') !== listCurrency.value) return false
    if (!q) return true
    return [e.categoryName, e.paymentTypeName, e.description, String(e.amount)]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q))
  })
})

/** Valyutalar qo'shilmaydi: har biri o'z jami bilan. */
const listTotals = computed(() => {
  const sums = new Map<CurrencyCode, number>()
  for (const e of filteredExpenses.value) {
    const c = e.currency || 'UZS'
    sums.set(c, (sums.get(c) || 0) + Number(e.amount || 0))
  }
  return CURRENCIES.filter((c) => sums.has(c)).map((c) => ({ currency: c, amount: sums.get(c) || 0 }))
})

const currencyTotals = computed(() =>
  CURRENCIES.map((currency) => {
    const list = cash.value.filter((c) => c.currency === currency)
    return {
      currency,
      count: list.length,
      amount: list.reduce((s, c) => s + c.available, 0),
      delta: list.reduce((s, c) => s + (cashDelta.value[c.paymentTypeId] || 0), 0),
    }
  }),
)

function availableFor(c: CashAvailable) {
  const spend = editingSpend.value
  const returned = spend && spend.paymentTypeId === c.paymentTypeId && spend.currency === c.currency ? spend.amount : 0
  return Number(c.available || 0) + returned
}

const cashOptions = computed(() =>
  cash.value
    .filter((c) => c.currency === expenseForm.currency)
    .map((c) => ({ ...c, available: availableFor(c) })),
)
const selectedCash = computed(() => cashOptions.value.find((c) => c.paymentTypeId === expenseForm.paymentTypeId) || null)
const selectedAvailable = computed(() => selectedCash.value?.available ?? 0)
const exceedsCash = computed(
  () => !!selectedCash.value && Number(expenseForm.amount || 0) > selectedAvailable.value + 1e-9,
)
const canSubmitExpense = computed(
  () => !!selectedCash.value && Number(expenseForm.amount || 0) > 0 && !exceedsCash.value && expenseForm.categoryId > 0,
)

function cashMoney(v: number) {
  return moneyIn(v, expenseForm.currency, t('common.currency'))
}

function today() {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Valyuta almashtirilganda shu valyutadagi eng ko'p qoldiqli kassa tanlanadi. */
function setFormCurrency(c: CurrencyCode) {
  expenseForm.currency = c
  if (selectedCash.value) return
  const best = [...cashOptions.value].sort((a, b) => b.available - a.available)[0]
  expenseForm.paymentTypeId = best?.paymentTypeId || 0
}

/** Belgi tanlanmagan eski kassalar uchun nomidan taxmin qilinadi. */
function cashKind(c: CashAvailable) {
  if (c.paymentTypeIcon) return c.paymentTypeIcon.toLowerCase()
  const name = c.paymentTypeName.toLowerCase()
  if (/naqd|нал|cash/.test(name)) return 'cash'
  if (/bank|банк|perech|transfer|o.tkazma/.test(name)) return 'bank'
  return 'card'
}

function cashIcon(c: CashAvailable) {
  return paymentTypeIcon(cashKind(c).toUpperCase())
}

async function loadCash(showDelta = false) {
  const before = new Map(cash.value.map((c) => [c.paymentTypeId, c.available]))
  cashLoading.value = true
  try {
    const res = await fetchCashAvailable()
    cash.value = (res.data || []).map((c) => ({ ...c, available: Number(c.available || 0) }))
  } catch {
    cash.value = []
  } finally {
    cashLoading.value = false
  }
  if (!showDelta) return
  const delta: Record<number, number> = {}
  for (const c of cash.value) {
    const diff = c.available - (before.get(c.paymentTypeId) ?? c.available)
    if (Math.abs(diff) > 1e-9) delta[c.paymentTypeId] = diff
  }
  cashDelta.value = delta
  clearTimeout(cashDeltaTimer)
  cashDeltaTimer = setTimeout(() => (cashDelta.value = {}), 6000)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [expRes, catRes] = await Promise.all([fetchExpenses(), fetchExpenseCategories()])
    expenses.value = expRes.data?.content || []
    categories.value = catRes.data?.content || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.loadError')
  } finally {
    loading.value = false
  }
}

async function openExpenseCreate(from?: { paymentTypeId?: number; currency: CurrencyCode }) {
  expenseEditingId.value = null
  editingSpend.value = null
  expenseForm.categoryId = categories.value[0]?.id || 0
  expenseForm.amount = null
  expenseForm.expenseDate = today()
  expenseForm.description = ''
  expenseForm.paymentTypeId = from?.paymentTypeId || 0
  formError.value = null
  expenseModal.value = true
  await loadCash()
  setFormCurrency(from?.currency || (listCurrency.value === 'USD' ? 'USD' : 'UZS'))
}

async function openExpenseEdit(e: Expense) {
  expenseEditingId.value = e.id
  const currency = e.currency || 'UZS'
  editingSpend.value = e.paymentTypeId ? { paymentTypeId: e.paymentTypeId, currency, amount: Number(e.amount) } : null
  expenseForm.categoryId = e.categoryId
  expenseForm.amount = Number(e.amount)
  expenseForm.expenseDate = e.expenseDate
  expenseForm.description = e.description || ''
  expenseForm.paymentTypeId = e.paymentTypeId || 0
  formError.value = null
  expenseModal.value = true
  await loadCash()
  setFormCurrency(currency)
}

async function onExpenseSubmit() {
  if (!canSubmitExpense.value) return
  saving.value = true
  formError.value = null
  try {
    const payload = {
      categoryId: expenseForm.categoryId,
      paymentTypeId: expenseForm.paymentTypeId,
      amount: Number(expenseForm.amount),
      expenseDate: expenseForm.expenseDate,
      description: expenseForm.description.trim() || undefined,
    }
    if (expenseEditingId.value) await updateExpense(expenseEditingId.value, payload)
    else await createExpense(payload)
    expenseModal.value = false
    await Promise.all([load(), loadCash(true)])
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.saveError')
    await loadCash()
  } finally {
    saving.value = false
  }
}

async function onExpenseDelete(e: Expense) {
  if (!confirm(t('expenses.deleteConfirm', { id: e.id }))) return
  try {
    await deleteExpense(e.id)
    await Promise.all([load(), loadCash(true)])
  } catch (err) {
    error.value = err instanceof ApiError ? err.message : t('common.deleteError')
  }
}

function openCategoryCreate() {
  categoryEditingId.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  formError.value = null
  categoryModal.value = true
}

function openCategoryEdit(c: ExpenseCategory) {
  categoryEditingId.value = c.id
  categoryForm.name = c.name
  categoryForm.description = c.description || ''
  formError.value = null
  categoryModal.value = true
}

async function onCategorySubmit() {
  saving.value = true
  formError.value = null
  try {
    const payload = {
      name: categoryForm.name.trim(),
      description: categoryForm.description.trim() || undefined,
    }
    if (categoryEditingId.value) await updateExpenseCategory(categoryEditingId.value, payload)
    else await createExpenseCategory(payload)
    categoryModal.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.saveError')
  } finally {
    saving.value = false
  }
}

async function onCategoryDelete(c: ExpenseCategory) {
  if (!confirm(t('common.deleteConfirmNamed', { name: c.name }))) return
  try {
    await deleteExpenseCategory(c.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.deleteError')
  }
}

onMounted(() => {
  load()
  loadCash()
})
onBeforeUnmount(() => clearTimeout(cashDeltaTimer))
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.field:focus { outline: none; border-color: #9cb0ff; box-shadow: 0 0 0 4px rgb(70 95 255 / 10%); }
.field.invalid { border-color: #f04438; }
.field.invalid:focus { box-shadow: 0 0 0 4px rgb(240 68 56 / 10%); }
.field-with-icon { height: 2.75rem; padding-inline-start: 2.5rem; }
.field-with-suffix { padding-inline-end: 3.5rem; }
.field-icon { position: absolute; top: 50%; inset-inline-start: 0.75rem; z-index: 1; height: 1.1rem; width: 1.1rem; transform: translateY(-50%); color: #98a2b3; pointer-events: none; }
.field-suffix { position: absolute; top: 50%; inset-inline-end: 0.75rem; transform: translateY(-50%); font-size: 0.8125rem; font-weight: 500; color: #98a2b3; pointer-events: none; }
.field-err { margin-top: 0.375rem; font-size: 0.75rem; color: #d92d20; }
.field-head { display: flex; height: 1.75rem; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.375rem; }
.field-head .lbl { margin-bottom: 0; white-space: nowrap; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { cursor: not-allowed; opacity: 0.6; }
.btn-with-icon { gap: 0.4rem; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.link-btn { font-size: 0.75rem; font-weight: 500; color: #465fff; }
.link-btn:hover { text-decoration: underline; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.125rem; font-size: 0.875rem; color: #6b7280; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 1rem; font-size: 0.875rem; color: #4b5563; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }

.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0, 0, 0, 0.4); padding: 1rem; }
.modal { width: 100%; max-width: 34rem; max-height: calc(100vh - 2rem); overflow-y: auto; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.modal-badge { display: flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.modal-badge.expense { background: #fff6ed; color: #ef6820; }
.close-btn { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #9ca3af; }
.close-btn:hover { background: #f3f4f6; color: #374151; }

.seg { display: inline-flex; gap: 0.125rem; border-radius: 0.5rem; background: #f2f4f7; padding: 0.1875rem; }
.seg-btn { border-radius: 0.375rem; padding: 0.25rem 0.75rem; font-size: 0.8125rem; font-weight: 500; color: #667085; transition: background-color 0.15s, color 0.15s; }
.seg-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08); }

.cur-switch { display: grid; grid-template-columns: 1fr 1fr; gap: 0.25rem; border-radius: 0.625rem; background: #f2f4f7; padding: 0.25rem; }
.cur-switch-btn { display: inline-flex; height: 2.25rem; align-items: center; justify-content: center; gap: 0.5rem; border-radius: 0.5rem; font-size: 0.875rem; font-weight: 500; color: #667085; transition: background-color 0.15s, color 0.15s; }
.cur-switch-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 3px rgba(16, 24, 40, 0.1); }

.cash-card { display: flex; width: 100%; align-items: center; gap: 0.625rem; border-radius: 0.625rem; border: 1px solid #e4e7ec; padding: 0.625rem 0.75rem; color: #344054; transition: border-color 0.15s, box-shadow 0.15s; }
.cash-card:hover { border-color: #9cb0ff; }
.cash-card.active { border-color: #465fff; box-shadow: 0 0 0 3px rgb(70 95 255 / 12%); }
.cash-icon { display: flex; height: 2rem; width: 2rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; background: #ecfdf3; color: #039855; }
.cash-card.empty .cash-icon { background: #fef3f2; color: #d92d20; }
.cash-balance { display: block; font-size: 0.75rem; color: #039855; }
.cash-card.empty .cash-balance { color: #d92d20; }
.hint-box { border-radius: 0.5rem; border: 1px dashed #d0d5dd; padding: 0.75rem; text-align: center; font-size: 0.8125rem; color: #667085; }
.field-after { margin-top: 0.375rem; font-size: 0.75rem; color: #667085; }
.close-btn:disabled { cursor: default; opacity: 0.6; }

.kassa-card { position: relative; display: flex; flex-direction: column; border-radius: 1rem; border: 1px solid #e4e7ec; background: #fff; padding: 1rem; text-align: start; transition: border-color 0.15s, box-shadow 0.15s, transform 0.15s; }
.kassa-card:not(:disabled):hover { border-color: #9cb0ff; box-shadow: 0 4px 12px rgba(16, 24, 40, 0.06); transform: translateY(-1px); }
.kassa-card:disabled { cursor: default; }
.kassa-card.flash { border-color: #fda29b; box-shadow: 0 0 0 3px rgb(240 68 56 / 10%); }
.kassa-icon { display: flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.tone-cash .kassa-icon { background: #ecfdf3; color: #039855; }
.tone-card .kassa-icon { background: #f4f3ff; color: #7a5af8; }
.tone-bank .kassa-icon { background: #eff8ff; color: #1570ef; }
.tone-online .kassa-icon { background: #f0f9ff; color: #0086c9; }
.tone-transfer .kassa-icon { background: #fff6ed; color: #ef6820; }
.tone-wallet .kassa-icon { background: #fffaeb; color: #dc6803; }
.kassa-amount { margin-top: 0.125rem; font-size: 1.25rem; font-weight: 600; line-height: 1.75rem; color: #101828; font-variant-numeric: tabular-nums; }
.kassa-card.negative .kassa-amount { color: #d92d20; }
.kassa-delta { align-self: flex-start; margin-top: 0.375rem; border-radius: 9999px; background: #fef3f2; padding: 0.125rem 0.5rem; font-size: 0.75rem; font-weight: 500; color: #d92d20; }
.kassa-delta.up { background: #ecfdf3; color: #039855; }
.kassa-card.total-uzs { background: linear-gradient(135deg, #f6fef9 0%, #fff 70%); border-color: #d1fadf; }
.kassa-card.total-usd { background: linear-gradient(135deg, #f5faff 0%, #fff 70%); border-color: #d1e9ff; }
.total-uzs .kassa-icon { background: #d1fadf; color: #027a48; }
.total-usd .kassa-icon { background: #d1e9ff; color: #175cd3; }
.kassa-count { border-radius: 9999px; background: #f2f4f7; padding: 0.0625rem 0.5rem; font-size: 0.6875rem; font-weight: 500; color: #667085; }

.total-chip { display: inline-flex; align-items: center; gap: 0.375rem; border-radius: 9999px; padding: 0.25rem 0.75rem; font-size: 0.8125rem; }
.total-chip.uzs { background: #ecfdf3; color: #027a48; }
.total-chip.usd { background: #eff8ff; color: #175cd3; }
.cur-badge { margin-inline-start: 0.375rem; border-radius: 0.25rem; padding: 0.0625rem 0.375rem; font-size: 0.6875rem; font-weight: 500; }
.cur-badge.uzs { background: #f2f4f7; color: #667085; }
.cur-badge.usd { background: #eff8ff; color: #175cd3; }

.dark .lbl { color: #9ca3af; }
.dark .title { color: rgba(255, 255, 255, 0.9); }
.dark .sub { color: #98a2b3; }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .field option { background: #101828; }
.dark .modal { border-color: #1f2937; background: #101828; }
.dark .modal-badge { background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .modal-badge.expense { background: rgb(239 104 32 / 12%); color: #fd853a; }
.dark .close-btn:hover { background: rgb(255 255 255 / 5%); color: rgba(255, 255, 255, 0.8); }
.dark .ghost { border-color: #344054; color: #d0d5dd; }
.dark .tab:not(.active) { border-color: #344054; color: #d0d5dd; }
.dark .seg,
.dark .cur-switch { background: rgb(255 255 255 / 5%); }
.dark .seg-btn,
.dark .cur-switch-btn { color: #98a2b3; }
.dark .seg-btn.active,
.dark .cur-switch-btn.active { background: #1d2939; color: #9cb0ff; }
.dark .cash-card { border-color: #344054; color: #e4e7ec; }
.dark .cash-card.active { border-color: #7592ff; }
.dark .cash-icon { background: rgb(18 183 106 / 12%); color: #32d583; }
.dark .cash-card.empty .cash-icon { background: rgb(240 68 56 / 12%); color: #f97066; }
.dark .cash-balance { color: #32d583; }
.dark .cash-card.empty .cash-balance { color: #f97066; }
.dark .hint-box { border-color: #344054; color: #98a2b3; }
.dark .field-after { color: #98a2b3; }
.dark .kassa-card { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .kassa-card:not(:disabled):hover { border-color: #7592ff; }
.dark .kassa-card.flash { border-color: #f97066; }
.dark .kassa-icon { background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .tone-cash .kassa-icon { background: rgb(18 183 106 / 12%); color: #32d583; }
.dark .tone-card .kassa-icon { background: rgb(122 90 248 / 14%); color: #bdb4fe; }
.dark .tone-bank .kassa-icon { background: rgb(46 144 250 / 12%); color: #84caff; }
.dark .tone-online .kassa-icon { background: rgb(11 165 236 / 12%); color: #7cd4fd; }
.dark .tone-transfer .kassa-icon { background: rgb(239 104 32 / 12%); color: #fd853a; }
.dark .tone-wallet .kassa-icon { background: rgb(247 144 9 / 12%); color: #fdb022; }
.dark .kassa-amount { color: rgba(255, 255, 255, 0.92); }
.dark .kassa-card.negative .kassa-amount { color: #f97066; }
.dark .kassa-delta { background: rgb(240 68 56 / 14%); color: #f97066; }
.dark .kassa-delta.up { background: rgb(18 183 106 / 12%); color: #32d583; }
.dark .kassa-card.total-uzs { background: linear-gradient(135deg, rgb(18 183 106 / 10%) 0%, rgb(255 255 255 / 3%) 70%); border-color: rgb(18 183 106 / 25%); }
.dark .kassa-card.total-usd { background: linear-gradient(135deg, rgb(46 144 250 / 10%) 0%, rgb(255 255 255 / 3%) 70%); border-color: rgb(46 144 250 / 25%); }
.dark .total-uzs .kassa-icon { background: rgb(18 183 106 / 16%); color: #6ce9a6; }
.dark .total-usd .kassa-icon { background: rgb(46 144 250 / 16%); color: #84caff; }
.dark .kassa-count { background: rgb(255 255 255 / 6%); color: #98a2b3; }
.dark .total-chip.uzs { background: rgb(18 183 106 / 12%); color: #6ce9a6; }
.dark .total-chip.usd { background: rgb(46 144 250 / 12%); color: #84caff; }
.dark .cur-badge.uzs { background: rgb(255 255 255 / 5%); color: #98a2b3; }
.dark .cur-badge.usd { background: rgb(46 144 250 / 12%); color: #84caff; }
</style>
