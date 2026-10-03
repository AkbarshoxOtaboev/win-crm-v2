<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.salesOrders')" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.salesOrders') }}</h3>
        <div class="toolbar mt-3">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field search" />
          <input v-model="dateFrom" type="date" class="field date" :title="t('common.from')" />
          <input v-model="dateTo" type="date" class="field date" :title="t('common.to')" />
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
              <td class="td"><SaleStatusBadge :status="o.orderStatus" /></td>
              <td class="td text-right">
                <RowActions @edit="openEdit(o)" @delete="onDelete(o)">
                  <button
                    type="button"
                    class="view-btn"
                    :title="t('common.view')"
                    :aria-label="t('common.view')"
                    @click="goDetail(o)"
                  >
                    <Eye :size="16" />
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
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Eye } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import SaleStatusBadge from '@/components/crm/SaleStatusBadge.vue'
import {
  deleteSaleOrder,
  fetchSaleOrders,
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

const router = useRouter()
const { writeBlocked } = useFilialScope()
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
const dateFrom = ref('')
const dateTo = ref('')
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
  const from = dateFrom.value ? new Date(`${dateFrom.value}T00:00:00`) : null
  const to = dateTo.value ? new Date(`${dateTo.value}T23:59:59`) : null
  return items.value.filter((o) => {
    if (q) {
      const statusText = o.orderStatus && te(`saleStatus.${o.orderStatus}`) ? t(`saleStatus.${o.orderStatus}`) : ''
      const hit = [o.clientFullName, o.orderStatus, statusText, String(o.id)]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(q))
      if (!hit) return false
    }
    if (from || to) {
      const d = o.orderDate ? new Date(o.orderDate) : null
      if (!d || Number.isNaN(d.getTime())) return false
      if (from && d < from) return false
      if (to && d > to) return false
    }
    return true
  })
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const [salesRes, whRes, clientsRes, usersRes] = await Promise.all([
      fetchSaleOrders(),
      fetchWarehouses(),
      fetchClients(),
      fetchUserOptions(),
    ])
    items.value = salesRes.data?.content || []
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

async function onDelete(o: SaleOrder) {
  if (!confirm(t('sales.deleteConfirm', { id: o.id }))) return
  try {
    await deleteSaleOrder(o.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, t('common.deleteError'))
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
.toolbar .date { width: 10rem; flex: 0 0 auto; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; white-space: nowrap; }
.create-btn { margin-left: auto; min-width: 11.5rem; padding: 0 1.5rem; flex-shrink: 0; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.875rem; color: #4b5563; }
.view-btn { display: inline-flex; height: 2rem; width: 2rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; color: #fff; transition: background-color 0.15s; }
.view-btn:hover { background: #3641f5; }
</style>
