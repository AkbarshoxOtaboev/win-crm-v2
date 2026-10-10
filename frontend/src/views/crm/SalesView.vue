<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.salesOrders')" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.salesOrders') }}</h3>
        <div class="toolbar mt-3">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field search" />
          <select v-model="statusFilter" class="field status-select" :aria-label="t('common.status')">
            <option value="">{{ t('sales.allStatuses') }}</option>
            <option v-for="s in SALE_STATUSES" :key="s" :value="s">{{ t(`saleStatus.${s}`) }}</option>
          </select>
          <div class="range">
            <CalendarDays class="h-4 w-4 shrink-0 text-gray-400" />
            <input
              v-model="dateFrom"
              type="date"
              class="range-input"
              :max="dateTo || undefined"
              :title="t('common.from')"
              :aria-label="t('common.from')"
            />
            <span class="text-gray-400">—</span>
            <input
              v-model="dateTo"
              type="date"
              class="range-input"
              :min="dateFrom || undefined"
              :title="t('common.to')"
              :aria-label="t('common.to')"
            />
            <button
              v-if="!isDefaultRange"
              type="button"
              class="range-reset"
              :title="t('sales.thisMonth')"
              @click="resetRange"
            >
              <RotateCcw class="h-3.5 w-3.5" />
            </button>
          </div>
          <button type="button" class="btn create-btn" :disabled="writeBlocked" @click="goCreate">{{ t('sales.newSale') }}</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.client') }}</th>
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('common.total') }}</th>
              <th class="th">{{ t('sales.paid') }}</th>
              <th class="th">{{ t('sales.debt') }}</th>
              <th class="th">{{ t('common.status') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="8" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="8" class="empty">{{ t('sales.noSales') }}</td></tr>
            <tr v-for="o in filtered" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">
                <router-link :to="`/sales/${o.id}`" class="text-brand-500 hover:underline">#{{ o.id }}</router-link>
              </td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ o.clientFullName || '—' }}</td>
              <td class="td">{{ formatDate(o.orderDate) }}</td>
              <td class="td whitespace-nowrap">{{ amt(o, o.totalSum) }}</td>
              <td class="td whitespace-nowrap">{{ amt(o, o.paidSum) }}</td>
              <td class="td whitespace-nowrap">{{ amt(o, o.debtSum) }}</td>
              <td class="td">
                <div class="flex flex-wrap items-center gap-1.5">
                  <SaleStatusBadge :status="o.orderStatus" />
                  <span v-if="o.saleType === 'WHOLESALE'" class="wholesale-tag">{{ t('sales.wholesale') }}</span>
                </div>
              </td>
              <td class="td text-right">
                <RowActions :remove="false" @edit="openEdit(o)">
                  <button
                    type="button"
                    class="view-btn"
                    :title="t('common.view')"
                    :aria-label="t('common.view')"
                    @click="goDetail(o)"
                  >
                    <Eye :size="16" />
                  </button>
                  <button
                    v-if="canCancel"
                    type="button"
                    class="cancel-btn"
                    :disabled="writeBlocked || !isCancellable(o)"
                    :title="isCancellable(o) ? t('sales.cancelOrder') : t('sales.cancelNotAllowed')"
                    :aria-label="t('sales.cancelOrder')"
                    @click="openCancel(o)"
                  >
                    <Ban :size="16" />
                  </button>
                </RowActions>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('sales.editOrder') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">{{ t('common.warehouse') }} *</label>
            <select v-model.number="form.warehouseId" required class="field">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">{{ t('common.client') }}</label>
            <select v-model.number="form.clientId" class="field">
              <option :value="0">{{ t('sales.optional') }}</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">{{ t('sales.seller') }} *</label>
            <select v-model.number="form.userId" required class="field">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('common.date') }} *</label>
              <input v-model="form.orderDate" type="datetime-local" required class="field" />
            </div>
            <div>
              <label class="lbl">{{ t('sales.totalSum') }} *</label>
              <input v-model.number="form.totalSum" type="number" min="0" step="0.01" required class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">{{ t('common.comment') }}</label>
            <input v-model="form.comment" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="modalOpen = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('common.save') }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="cancelTarget" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div
        class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900"
        role="dialog"
        aria-modal="true"
        aria-labelledby="sales-cancel-title"
      >
        <h3 id="sales-cancel-title" class="text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ t('saleOrderDetail.cancelTitle', { id: cancelTarget.id }) }}
        </h3>
        <p class="mb-4 mt-1 text-sm text-gray-500 dark:text-gray-400">{{ t('saleOrderDetail.cancelHint') }}</p>
        <div v-if="cancelError" class="err mb-3">{{ cancelError }}</div>
        <form class="space-y-3" @submit.prevent="onCancelConfirm">
          <div>
            <label for="sales-cancel-reason" class="lbl">{{ t('saleOrderDetail.cancelReason') }} *</label>
            <textarea
              id="sales-cancel-reason"
              v-model="cancelReason"
              rows="3"
              required
              class="field textarea"
              :placeholder="t('saleOrderDetail.cancelReasonPlaceholder')"
            />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="cancelTarget = null">
              {{ t('common.cancel') }}
            </button>
            <button type="submit" class="btn danger" :disabled="cancelSaving || !cancelReason.trim()">
              {{ cancelSaving ? t('common.saving') : t('saleOrderDetail.cancelSubmit') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Ban, CalendarDays, Eye, RotateCcw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SaleStatusBadge from '@/components/crm/SaleStatusBadge.vue'
import {
  changeSaleOrderStatus,
  fetchSaleOrdersByDateRange,
  updateSaleOrder,
  type SaleOrder,
} from '@/api/sales'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchClients, type Client } from '@/api/clients'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate, money, toApiDate } from '@/utils/format'
import { moneyIn } from '@/utils/currency'
import { nextSaleStatuses } from '@/utils/saleStatus'

const SALE_STATUSES = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'READY',
  'IN_DELIVERY',
  'DELIVERED',
  'WORK_DONE',
  'COMPLETED',
  'CANCELLED',
] as const

const router = useRouter()
const route = useRoute()
const { auth, writeBlocked } = useFilialScope()
const { t, te } = useI18n()

const items = ref<SaleOrder[]>([])
const warehouses = ref<Warehouse[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')

function localIso(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function monthStartIso() {
  const d = new Date()
  return localIso(new Date(d.getFullYear(), d.getMonth(), 1))
}

function queryStr(key: string) {
  const v = route.query[key]
  return typeof v === 'string' ? v : ''
}

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/
const statusFilter = ref<string>(
  (SALE_STATUSES as readonly string[]).includes(queryStr('status')) ? queryStr('status') : '',
)
const dateFrom = ref(ISO_DATE.test(queryStr('from')) ? queryStr('from') : monthStartIso())
const dateTo = ref(ISO_DATE.test(queryStr('to')) ? queryStr('to') : localIso(new Date()))

watch(statusFilter, (s) => {
  const query = { ...route.query }
  if (s) query.status = s
  else delete query.status
  void router.replace({ query })
})
const isDefaultRange = computed(() => dateFrom.value === monthStartIso() && dateTo.value === localIso(new Date()))

function resetRange() {
  dateFrom.value = monthStartIso()
  dateTo.value = localIso(new Date())
}
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  warehouseId: 0,
  clientId: 0,
  userId: 0,
  orderDate: '',
  totalSum: 0,
  comment: '',
})

function amt(o: SaleOrder, v?: number | null) {
  return o.currency && o.currency !== 'UZS' ? moneyIn(v, o.currency) : money(v)
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  const byStatus = statusFilter.value
    ? items.value.filter((o) => String(o.orderStatus || 'NEW').toUpperCase() === statusFilter.value)
    : items.value
  if (!q) return byStatus
  return byStatus.filter((o) => {
    const statusText = o.orderStatus && te(`saleStatus.${o.orderStatus}`) ? t(`saleStatus.${o.orderStatus}`) : ''
    return [o.clientFullName, o.orderStatus, statusText, String(o.id)]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q))
  })
})

let salesSeq = 0

async function loadSales() {
  const id = ++salesSeq
  const from = dateFrom.value || '2000-01-01'
  const to = dateTo.value || '2100-12-31'
  if (from > to) {
    error.value = t('sales.invalidRange')
    items.value = []
    return
  }
  const res = await fetchSaleOrdersByDateRange(`${from}T00:00:00`, `${to}T23:59:59`)
  if (id !== salesSeq) return
  items.value = [...(res.data || [])].sort((a, b) => b.id - a.id)
}

async function reloadSales() {
  loading.value = true
  error.value = null
  try {
    await loadSales()
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

watch([dateFrom, dateTo], () => void reloadSales())

async function load() {
  loading.value = true
  error.value = null
  try {
    const [, whRes, clientsRes, usersRes] = await Promise.all([
      loadSales(),
      fetchWarehouses(),
      fetchClients(),
      fetchUserOptions(),
    ])
    warehouses.value = whRes.data || []
    clients.value = clientsRes.data || []
    users.value = usersRes.data || []
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

function goCreate() {
  if (writeBlocked.value) return
  void router.push('/sales/create')
}

function openEdit(o: SaleOrder) {
  editingId.value = o.id
  form.warehouseId = o.warehouseId || 0
  form.clientId = o.clientId || 0
  form.userId = o.userId || 0
  form.orderDate = (o.orderDate || '').slice(0, 16)
  form.totalSum = Number(o.totalSum || 0)
  form.comment = o.comment || ''
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  if (!editingId.value) return
  saving.value = true
  formError.value = null
  try {
    await updateSaleOrder(editingId.value, {
      warehouseId: form.warehouseId,
      userId: form.userId,
      orderDate: toApiDate(form.orderDate),
      totalSum: form.totalSum,
      clientId: form.clientId || null,
      comment: form.comment || undefined,
    })
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, t('common.saveError'))
  } finally {
    saving.value = false
  }
}

function goDetail(o: SaleOrder) {
  void router.push(`/sales/${o.id}`)
}

const canCancel = computed(() => auth.can('SALE_ORDER_EDIT'))
const cancelTarget = ref<SaleOrder | null>(null)
const cancelReason = ref('')
const cancelError = ref<string | null>(null)
const cancelSaving = ref(false)

function isCancellable(o: SaleOrder) {
  return nextSaleStatuses(o.orderStatus).includes('CANCELLED')
}

function openCancel(o: SaleOrder) {
  if (writeBlocked.value || !isCancellable(o)) return
  cancelTarget.value = o
  cancelReason.value = ''
  cancelError.value = null
}

async function onCancelConfirm() {
  if (!cancelTarget.value) return
  const reason = cancelReason.value.trim()
  if (!reason) {
    cancelError.value = t('saleOrderDetail.cancelReasonRequired')
    return
  }
  cancelSaving.value = true
  cancelError.value = null
  try {
    await changeSaleOrderStatus(cancelTarget.value.id, 'CANCELLED', reason)
    cancelTarget.value = null
    await reloadSales()
  } catch (e) {
    cancelError.value = formatApiError(e, t('saleOrderDetail.statusError'))
  } finally {
    cancelSaving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.toolbar { display: flex; align-items: center; gap: 0.75rem; width: 100%; flex-wrap: wrap; }
.toolbar .search { width: 14rem; max-width: 100%; flex: 0 0 auto; }
.toolbar .status-select { width: 12rem; max-width: 100%; flex: 0 0 auto; }
.dark .status-select { border-color: #374151; background: #111827; color: rgba(255, 255, 255, 0.92); }
.wholesale-tag { display: inline-flex; border-radius: 9999px; background: #ecfdf3; padding: 0.125rem 0.5rem; font-size: 0.6875rem; font-weight: 600; color: #027a48; }
.dark .wholesale-tag { background: rgba(18, 183, 106, 0.15); color: #6ce9a6; }
.range { display: inline-flex; height: 2.5rem; align-items: center; gap: 0.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.625rem; font-size: 0.875rem; }
.range-input { width: 8.25rem; border: 0; background: transparent; padding: 0; font-size: 0.875rem; color: inherit; outline: none; }
.range-reset { display: inline-flex; height: 1.5rem; width: 1.5rem; align-items: center; justify-content: center; border-radius: 0.375rem; color: #6b7280; }
.range-reset:hover { background: #f3f4f6; color: #465fff; }
.dark .range { border-color: #374151; color: #e5e7eb; }
.dark .range-input { color-scheme: dark; }
.dark .range-reset:hover { background: rgba(255, 255, 255, 0.06); }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.create-btn { margin-left: auto; min-width: 11.5rem; padding: 0 1.5rem; flex-shrink: 0; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.875rem; color: #4b5563; }
.view-btn { display: inline-flex; height: 2rem; width: 2rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; color: #fff; transition: background-color 0.15s; }
.view-btn:hover { background: #3641f5; }
.cancel-btn { display: inline-flex; height: 2rem; width: 2rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #ef4444; transition: background-color 0.15s; }
.cancel-btn:hover:not(:disabled) { background: #fef2f2; }
.cancel-btn:disabled { cursor: not-allowed; opacity: 0.35; }
.dark .cancel-btn { color: #f87171; }
.dark .cancel-btn:hover:not(:disabled) { background: rgba(239, 68, 68, 0.1); }
.textarea { height: auto; padding: 0.5rem 0.75rem; resize: vertical; }
.btn.danger { background: #dc2626; }
.btn.danger:hover:not(:disabled) { background: #b91c1c; }
.btn:disabled { cursor: not-allowed; opacity: 0.6; }
</style>
