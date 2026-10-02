<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('suppliersBalance.title')" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'balances' }" @click="tab = 'balances'">
        {{ t('suppliersBalance.tabBalances') }}
      </button>
      <button
        v-if="auth.can('SUPPLIER_PAYMENT_VIEW')"
        type="button"
        class="tab"
        :class="{ active: tab === 'payments' }"
        @click="tab = 'payments'"
      >
        {{ t('suppliersBalance.tabPayments') }}
      </button>
      <button
        v-if="auth.can('SUPPLIER_PAYMENT_CREATE')"
        type="button"
        class="btn ms-auto"
        :disabled="writeBlocked"
        @click="openPayment()"
      >
        <Banknote class="h-4 w-4" />
        {{ t('suppliersBalance.newPayment') }}
      </button>
    </div>

    <div v-show="tab === 'balances'" class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ t('suppliersBalance.title') }}</h3>
          <p class="sub">{{ t('suppliersBalance.subtitle') }}</p>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="icon-btn" :title="t('common.filter')" @click="showFilters = !showFilters">
            <ListFilter class="h-4 w-4" />
          </button>
          <button type="button" class="icon-btn" :title="t('common.refresh')" :disabled="loading" @click="load">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
        </div>
      </div>

      <div v-if="showFilters" class="filters">
        <div class="flex flex-wrap items-end gap-3">
          <label class="lbl min-w-[16rem] flex-1">
            {{ t('suppliersBalance.search') }}
            <input
              v-model="search"
              type="search"
              class="field"
              :placeholder="t('suppliersBalance.searchPlaceholder')"
            />
          </label>
          <label class="check">
            <input v-model="onlyDebtors" type="checkbox" class="h-4 w-4 rounded border-gray-300" />
            {{ t('suppliersBalance.onlyDebtors') }}
          </label>
        </div>
      </div>

      <div class="stats">
        <div class="stat stat-blue">
          <div class="stat-label">{{ t('suppliersBalance.purchaseSum') }}</div>
          <div class="stat-value">{{ moneySom(totals.purchase) }}</div>
        </div>
        <div class="stat stat-green">
          <div class="stat-label">{{ t('suppliersBalance.paid') }}</div>
          <div class="stat-value paid">{{ moneySom(totals.paid) }}</div>
        </div>
        <div class="stat stat-red">
          <div class="stat-label">{{ t('suppliersBalance.debt') }}</div>
          <div class="stat-value debt">{{ moneySom(totals.debt) }}</div>
        </div>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">ID</th>
              <th class="th">{{ t('common.supplier') }}</th>
              <th class="th">{{ t('suppliersBalance.purchaseSum') }}</th>
              <th class="th">{{ t('suppliersBalance.paid') }}</th>
              <th class="th">{{ t('suppliersBalance.debt') }}</th>
              <th class="th">{{ t('suppliersBalance.lastUpdated') }}</th>
              <th class="th text-end">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">{{ t('suppliersBalance.empty') }}</td></tr>
            <tr v-for="b in filtered" :key="b.supplierId || b.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ b.supplierId || b.id }}</td>
              <td class="td">
                <router-link class="link" :to="{ path: '/suppliers', query: { id: String(b.supplierId || '') } }">
                  {{ b.supplierName || b.supplierId }}
                </router-link>
              </td>
              <td class="td">{{ moneySom(b.totalPurchase) }}</td>
              <td class="td text-emerald-600 dark:text-emerald-400">{{ moneySom(b.totalPaid) }}</td>
              <td class="td text-red-600 dark:text-red-400">{{ moneySom(debtOf(b)) }}</td>
              <td class="td">{{ formatDateTime(b.lastUpdated || b.updatedAt) }}</td>
              <td class="td text-end">
                <div class="inline-flex items-center gap-3">
                  <button
                    v-if="auth.can('SUPPLIER_PAYMENT_CREATE') && b.supplierId"
                    type="button"
                    class="pay-btn"
                    :disabled="writeBlocked"
                    @click="openPayment(b.supplierId, debtOf(b))"
                  >
                    {{ t('suppliersBalance.pay') }}
                  </button>
                  <button
                    v-if="auth.can('SUPPLIER_PAYMENT_VIEW') && b.supplierId"
                    type="button"
                    class="link"
                    @click="showHistory(b.supplierId)"
                  >
                    {{ t('suppliersBalance.history') }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="tab === 'payments'" class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ t('suppliersBalance.paymentsTitle') }}</h3>
          <p class="sub">{{ t('suppliersBalance.paymentsSubtitle') }}</p>
        </div>
        <button type="button" class="icon-btn" :title="t('common.refresh')" :disabled="paymentsLoading" @click="loadPayments">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': paymentsLoading }" />
        </button>
      </div>

      <div class="filters">
        <div class="flex flex-wrap items-end gap-3">
          <label class="lbl min-w-[14rem] flex-1">
            {{ t('common.supplier') }}
            <select v-model.number="paymentFilter.supplierId" class="field" @change="loadPayments">
              <option :value="0">{{ t('suppliersBalance.allSuppliers') }}</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </label>
          <label class="lbl">
            {{ t('suppliersBalance.fromDate') }}
            <input v-model="paymentFilter.from" type="date" class="field" @change="loadPayments" />
          </label>
          <label class="lbl">
            {{ t('suppliersBalance.toDate') }}
            <input v-model="paymentFilter.to" type="date" class="field" @change="loadPayments" />
          </label>
          <label class="lbl min-w-[14rem] flex-1">
            {{ t('suppliersBalance.search') }}
            <input v-model="paymentSearch" type="search" class="field" :placeholder="t('suppliersBalance.paymentSearchPlaceholder')" />
          </label>
        </div>
      </div>

      <div class="px-5 pt-4 text-sm text-gray-600 dark:text-gray-400">
        {{ t('suppliersBalance.paymentsTotal') }}:
        <span class="font-semibold text-gray-800 dark:text-white/90">{{ moneySom(paymentsTotal) }}</span>
      </div>

      <div v-if="paymentsError" class="err mx-5 mt-4">{{ paymentsError }}</div>

      <div class="overflow-x-auto pt-2">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('common.supplier') }}</th>
              <th class="th">{{ t('common.amount') }}</th>
              <th class="th">{{ t('suppliersBalance.paymentType') }}</th>
              <th class="th">{{ t('common.note') }}</th>
              <th class="th">{{ t('suppliersBalance.createdBy') }}</th>
              <th class="th text-end">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="paymentsLoading"><td colspan="8" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filteredPayments.length === 0"><td colspan="8" class="empty">{{ t('suppliersBalance.noPayments') }}</td></tr>
            <tr v-for="p in filteredPayments" :key="p.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ p.id }}</td>
              <td class="td">{{ formatDate(p.paidDate) }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ p.supplierName || p.supplierId }}</td>
              <td class="td font-semibold text-emerald-600 dark:text-emerald-400">{{ moneySom(p.paidSumm) }}</td>
              <td class="td">{{ p.paymentTypeName || '—' }}</td>
              <td class="td">{{ p.comment || '—' }}</td>
              <td class="td">{{ p.createdUsername || '—' }}</td>
              <td class="td text-end">
                <RowActions
                  :edit="auth.can('SUPPLIER_PAYMENT_EDIT') && !writeBlocked"
                  :remove="auth.can('SUPPLIER_PAYMENT_DELETE') && !writeBlocked"
                  @edit="openPaymentEdit(p)"
                  @delete="onPaymentDelete(p)"
                />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="paymentModal" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-1 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingPaymentId ? t('suppliersBalance.editPayment') : t('suppliersBalance.newPaymentTitle') }}
        </h3>
        <p class="mb-4 text-xs text-gray-500 dark:text-gray-400">{{ t('suppliersBalance.expenseNotice') }}</p>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onPaymentSubmit">
          <label class="lbl">
            {{ t('common.supplier') }} *
            <select v-model.number="form.supplierId" required class="field" :disabled="!!editingPaymentId">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="s in suppliers" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </label>
          <div v-if="form.supplierId && !editingPaymentId" class="debt-hint">
            {{ t('suppliersBalance.currentDebt') }}:
            <strong>{{ moneySom(selectedSupplierDebt) }}</strong>
            <button
              v-if="selectedSupplierDebt > 0"
              type="button"
              class="link ms-2 text-xs"
              @click="setAmount(selectedSupplierDebt)"
            >
              {{ t('suppliersBalance.payFull') }}
            </button>
          </div>
          <label class="lbl">
            {{ t('common.amount') }} *
            <input :value="amountText" inputmode="decimal" required class="field" placeholder="0" @input="onAmountInput" />
          </label>
          <div class="grid grid-cols-2 gap-3">
            <label class="lbl">
              {{ t('common.date') }} *
              <input v-model="form.paidDate" type="datetime-local" required class="field" />
            </label>
            <label class="lbl">
              {{ t('suppliersBalance.paymentType') }} *
              <select v-model.number="form.paymentTypeId" required class="field">
                <option :value="0" disabled>{{ t('common.select') }}</option>
                <option v-for="pt in paymentTypes" :key="pt.id" :value="pt.id">{{ pt.name }}</option>
              </select>
            </label>
          </div>
          <label class="lbl">
            {{ t('common.note') }}
            <textarea v-model="form.comment" rows="2" class="field textarea" maxlength="400" />
          </label>
          <div class="flex justify-end gap-2 pt-2">
            <button type="button" class="btn-outline" @click="paymentModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Banknote, ListFilter, RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createSupplierPayment,
  deleteSupplierPayment,
  fetchSupplierBalances,
  fetchSuppliers,
  filterSupplierPayments,
  updateSupplierPayment,
  type Supplier,
  type SupplierBalance,
  type SupplierPayment,
} from '@/api/suppliers'
import { fetchPaymentTypes, type PaymentType } from '@/api/payments'
import { formatApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatAmountInput, formatDate, money, nowLocal, toApiDate } from '@/utils/format'

const { t } = useI18n()
const auth = useAuthStore()
const { writeBlocked } = useFilialScope()

const tab = ref<'balances' | 'payments'>('balances')
const items = ref<SupplierBalance[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const showFilters = ref(true)
const search = ref('')
const onlyDebtors = ref(false)

const suppliers = ref<Supplier[]>([])
const paymentTypes = ref<PaymentType[]>([])

const payments = ref<SupplierPayment[]>([])
const paymentsLoading = ref(false)
const paymentsError = ref<string | null>(null)
const paymentSearch = ref('')
const paymentFilter = reactive({ supplierId: 0, from: '', to: '' })

const paymentModal = ref(false)
const editingPaymentId = ref<number | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)
const amountText = ref('')
const form = reactive({
  supplierId: 0,
  paidSumm: 0,
  paidDate: nowLocal(),
  paymentTypeId: 0,
  comment: '',
})

function debtOf(b: SupplierBalance) {
  if (b.totalDebt != null) return Number(b.totalDebt)
  if (b.balance != null) return Number(b.balance)
  return Math.max(0, Number(b.totalPurchase || 0) - Number(b.totalPaid || 0))
}

function moneySom(v?: number | null) {
  return `${money(Number(v || 0))} ${t('common.currency')}`
}

function formatDateTime(v?: string | null) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v.replace('T', ' ').slice(0, 19)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return [...items.value]
    .filter((b) => {
      if (onlyDebtors.value && debtOf(b) <= 0) return false
      if (!q) return true
      const id = String(b.supplierId || b.id || '')
      const name = (b.supplierName || '').toLowerCase()
      return id.includes(q) || name.includes(q)
    })
    .sort((a, b) => Number(a.supplierId || a.id || 0) - Number(b.supplierId || b.id || 0))
})

const totals = computed(() => {
  return filtered.value.reduce(
    (acc, b) => {
      acc.purchase += Number(b.totalPurchase || 0)
      acc.paid += Number(b.totalPaid || 0)
      acc.debt += debtOf(b)
      return acc
    },
    { purchase: 0, paid: 0, debt: 0 },
  )
})

const filteredPayments = computed(() => {
  const q = paymentSearch.value.trim().toLowerCase()
  if (!q) return payments.value
  return payments.value.filter((p) =>
    [p.id, p.supplierName, p.comment, p.paymentTypeName, p.createdUsername, p.paidSumm]
      .some((v) => String(v ?? '').toLowerCase().includes(q)),
  )
})

const paymentsTotal = computed(() =>
  filteredPayments.value.reduce((sum, p) => sum + Number(p.paidSumm || 0), 0),
)

const selectedSupplierDebt = computed(() => {
  const b = items.value.find((x) => x.supplierId === form.supplierId)
  return b ? debtOf(b) : 0
})

function setAmount(value: number) {
  form.paidSumm = value
  amountText.value = formatAmountInput(String(value)).text
}

function onAmountInput(e: Event) {
  const el = e.target as HTMLInputElement
  const { text, value } = formatAmountInput(el.value)
  amountText.value = text
  el.value = text
  form.paidSumm = value
}

function openPayment(supplierId?: number, debt?: number) {
  editingPaymentId.value = null
  formError.value = null
  form.supplierId = supplierId || 0
  form.paidDate = nowLocal()
  form.paymentTypeId = paymentTypes.value[0]?.id || 0
  form.comment = ''
  setAmount(debt && debt > 0 ? debt : 0)
  if (!debt) amountText.value = ''
  paymentModal.value = true
}

function openPaymentEdit(p: SupplierPayment) {
  editingPaymentId.value = p.id
  formError.value = null
  form.supplierId = p.supplierId || 0
  form.paidDate = p.paidDate ? p.paidDate.slice(0, 16) : nowLocal()
  form.paymentTypeId = p.paymentTypeId || 0
  form.comment = p.comment || ''
  setAmount(Number(p.paidSumm || 0))
  paymentModal.value = true
}

async function onPaymentSubmit() {
  formError.value = null
  if (!form.supplierId || !form.paymentTypeId) {
    formError.value = t('suppliersBalance.fillRequired')
    return
  }
  if (!(form.paidSumm > 0)) {
    formError.value = t('suppliersBalance.amountRequired')
    return
  }
  const payload = {
    supplierId: form.supplierId,
    paidSumm: form.paidSumm,
    paidDate: toApiDate(form.paidDate),
    paymentTypeId: form.paymentTypeId,
    comment: form.comment.trim() || undefined,
  }
  saving.value = true
  try {
    if (editingPaymentId.value) await updateSupplierPayment(editingPaymentId.value, payload)
    else await createSupplierPayment(payload)
    paymentModal.value = false
    await Promise.all([load(), tab.value === 'payments' ? loadPayments() : Promise.resolve()])
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onPaymentDelete(p: SupplierPayment) {
  if (!confirm(t('suppliersBalance.deleteConfirm', { id: p.id }))) return
  try {
    await deleteSupplierPayment(p.id)
    await Promise.all([load(), loadPayments()])
  } catch (e) {
    paymentsError.value = formatApiError(e)
  }
}

function showHistory(supplierId: number) {
  paymentFilter.supplierId = supplierId
  tab.value = 'payments'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchSupplierBalances(0, 500)
    items.value = res.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function loadPayments() {
  paymentsLoading.value = true
  paymentsError.value = null
  try {
    const res = await filterSupplierPayments({
      supplierId: paymentFilter.supplierId || undefined,
      fromDate: paymentFilter.from ? `${paymentFilter.from}T00:00:00` : undefined,
      toDate: paymentFilter.to ? `${paymentFilter.to}T23:59:59` : undefined,
    })
    payments.value = res.data?.content || []
  } catch (e) {
    paymentsError.value = formatApiError(e)
  } finally {
    paymentsLoading.value = false
  }
}

async function loadLookups() {
  const canPay = auth.can('SUPPLIER_PAYMENT_CREATE') || auth.can('SUPPLIER_PAYMENT_VIEW')
  if (!canPay) return
  const [s, pt] = await Promise.allSettled([fetchSuppliers(0, 500), fetchPaymentTypes()])
  if (s.status === 'fulfilled') suppliers.value = s.value.data?.content || []
  if (pt.status === 'fulfilled') paymentTypes.value = pt.value.data?.content || []
}

watch(tab, (v) => {
  if (v === 'payments') loadPayments()
})

onMounted(() => {
  load()
  loadLookups()
})
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 1.25rem 1.25rem 1rem; border-bottom: 1px solid #f3f4f6; }
.filters { padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.check { display: inline-flex; align-items: center; gap: 0.5rem; height: 2.5rem; font-size: 0.875rem; color: #374151; white-space: nowrap; }
.stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; padding: 1.25rem; }
.stat { border-radius: 0.75rem; border: 1px solid #e5e7eb; background: #fff; padding: 1rem 1.1rem; border-bottom-width: 3px; }
.stat-blue { border-bottom-color: #465fff; }
.stat-green { border-bottom-color: #10b981; }
.stat-red { border-bottom-color: #ef4444; }
.stat-label { font-size: 0.8125rem; color: #6b7280; margin-bottom: 0.35rem; }
.stat-value { font-size: 1.25rem; font-weight: 700; color: #111827; }
.stat-value.paid { color: #059669; }
.stat-value.debt { color: #dc2626; }
.th { padding: 0.75rem 1rem; text-align: start; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.th.text-end { text-align: end; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; background: #fff; color: #1f2937; }
.field.textarea { height: auto; padding-block: 0.5rem; }
.field:disabled { background: #f9fafb; color: #6b7280; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #4b5563; background: #fff; }
.link { color: #465fff; font-weight: 500; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.tab { border-radius: 0.5rem; border: 1px solid #e5e7eb; background: #fff; padding: 0.5rem 1rem; font-size: 0.875rem; font-weight: 500; color: #4b5563; }
.tab.active { border-color: #465fff; background: #465fff; color: #fff; }
.btn { display: inline-flex; align-items: center; gap: 0.4rem; border-radius: 0.5rem; background: #465fff; padding: 0.55rem 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-outline { border-radius: 0.5rem; border: 1px solid #d1d5db; background: #fff; padding: 0.55rem 1rem; font-size: 0.875rem; color: #374151; }
.pay-btn { border-radius: 0.5rem; background: #ecfdf5; padding: 0.3rem 0.75rem; font-size: 0.8125rem; font-weight: 600; color: #059669; }
.pay-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.debt-hint { border-radius: 0.5rem; background: #fef2f2; padding: 0.5rem 0.75rem; font-size: 0.8125rem; color: #b91c1c; }
.dark .card { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .head, .dark .filters { border-bottom-color: #1f2937; }
.dark .title { color: rgba(255, 255, 255, 0.9); }
.dark .sub, .dark .lbl, .dark .th, .dark .empty { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .check { color: #d1d5db; }
.dark .stat { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .stat-blue { border-bottom-color: #465fff !important; }
.dark .stat-green { border-bottom-color: #10b981 !important; }
.dark .stat-red { border-bottom-color: #ef4444 !important; }
.dark .stat-label { color: #9ca3af; }
.dark .stat-value { color: rgba(255, 255, 255, 0.92); }
.dark .stat-value.paid { color: #34d399; }
.dark .stat-value.debt { color: #f87171; }
.dark .field, .dark .icon-btn, .dark .btn-outline { border-color: #374151; background: #111827; color: #e5e7eb; }
.dark .field:disabled { background: #1f2937; color: #9ca3af; }
.dark .tab { border-color: #374151; background: #111827; color: #d1d5db; }
.dark .tab.active { border-color: #465fff; background: #465fff; color: #fff; }
.dark .pay-btn { background: rgba(16, 185, 129, 0.12); color: #34d399; }
.dark .debt-hint { background: rgba(239, 68, 68, 0.12); color: #fca5a5; }
.dark .err { border-color: rgba(239, 68, 68, 0.3); background: rgba(239, 68, 68, 0.1); color: #fca5a5; }
@media (max-width: 768px) {
  .stats { grid-template-columns: 1fr; }
}
</style>
