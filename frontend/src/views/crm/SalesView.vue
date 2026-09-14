<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Savdolar" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Savdolar</h3>
        <div class="flex gap-2">
          <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
          <button type="button" class="btn" @click="openCreate">+ Yangi savdo</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Mijoz</th>
              <th class="th">Ombor</th>
              <th class="th">Jami</th>
              <th class="th">To‘langan</th>
              <th class="th">Qarz</th>
              <th class="th">Holat</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="8" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="8" class="empty">Savdo yo‘q</td></tr>
            <tr v-for="o in filtered" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">
                <router-link :to="`/sales/${o.id}`" class="text-brand-500 hover:underline">#{{ o.id }}</router-link>
              </td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ o.clientFullName || '—' }}</td>
              <td class="td">{{ o.warehouseName || '—' }}</td>
              <td class="td">{{ money(o.totalSum) }}</td>
              <td class="td">{{ money(o.paidSum) }}</td>
              <td class="td">{{ money(o.debtSum) }}</td>
              <td class="td">{{ o.orderStatus || '—' }}</td>
              <td class="td text-right">
                <div class="inline-flex items-center gap-1.5 justify-end">
                  <select class="field" style="width: auto; display: inline-block" :value="o.orderStatus" @change="onStatus(o, ($event.target as HTMLSelectElement).value)">
                    <option v-for="st in statuses" :key="st" :value="st">{{ st }}</option>
                  </select>
                  <RowActions @edit="openEdit(o)" @delete="onDelete(o)" />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="modalOpen = false">
      <div class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? 'Savdoni tahrirlash' : 'Yangi savdo' }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">Ombor *</label>
            <select v-model.number="form.warehouseId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">Mijoz</label>
            <select v-model.number="form.clientId" class="field">
              <option :value="0">— ixtiyoriy —</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">Sotuvchi *</label>
            <select v-model.number="form.userId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Sana *</label>
              <input v-model="form.orderDate" type="datetime-local" required class="field" />
            </div>
            <div>
              <label class="lbl">Jami summa *</label>
              <input v-model.number="form.totalSum" type="number" min="0" step="0.01" required class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <input v-model="form.comment" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="modalOpen = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Saqlash' }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  changeSaleOrderStatus,
  createSaleOrder,
  deleteSaleOrder,
  fetchSaleOrders,
  updateSaleOrder,
  type SaleOrder,
} from '@/api/sales'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchClients, type Client } from '@/api/clients'
import { fetchUsers, type UserItem } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import { money, nowLocal, toApiDate } from '@/utils/format'

const statuses = ['NEW', 'CONFIRMED', 'PROCESSING', 'DELIVERED', 'COMPLETED', 'CANCELLED']

const items = ref<SaleOrder[]>([])
const warehouses = ref<Warehouse[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const auth = useAuthStore()

const form = reactive({
  warehouseId: 0,
  clientId: 0,
  userId: 0,
  orderDate: '',
  totalSum: 0,
  comment: '',
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((o) =>
    [o.clientFullName, o.warehouseName, o.orderStatus, String(o.id)]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const [salesRes, whRes, clientsRes, usersRes] = await Promise.all([
      fetchSaleOrders(),
      fetchWarehouses(),
      fetchClients(),
      fetchUsers(),
    ])
    items.value = salesRes.data?.content || []
    warehouses.value = whRes.data || []
    clients.value = clientsRes.data || []
    users.value = usersRes.data || []
  } catch (e) {
    error.value = formatApiError(e, 'Yuklashda xatolik')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  const me = users.value.find((u) => u.username === auth.username)
  editingId.value = null
  form.warehouseId = warehouses.value[0]?.id || 0
  form.clientId = 0
  form.userId = me?.id || users.value[0]?.id || 0
  form.orderDate = nowLocal()
  form.totalSum = 0
  form.comment = ''
  formError.value = null
  modalOpen.value = true
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
  saving.value = true
  formError.value = null
  try {
    const payload = {
      warehouseId: form.warehouseId,
      userId: form.userId,
      orderDate: toApiDate(form.orderDate),
      totalSum: form.totalSum,
      clientId: form.clientId || null,
      comment: form.comment || undefined,
    }
    if (editingId.value) await updateSaleOrder(editingId.value, payload)
    else await createSaleOrder(payload)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, 'Saqlashda xatolik')
  } finally {
    saving.value = false
  }
}

async function onStatus(o: SaleOrder, status: string) {
  try {
    await changeSaleOrderStatus(o.id, status)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'Holat o‘zgartirishda xatolik')
  }
}

async function onDelete(o: SaleOrder) {
  if (!confirm(`Savdo #${o.id} o‘chirilsinmi?`)) return
  try {
    await deleteSaleOrder(o.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, 'O‘chirishda xatolik')
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.lbl { display: block; margin-bottom: 0.25rem; font-size: 0.875rem; color: #4b5563; }
</style>
