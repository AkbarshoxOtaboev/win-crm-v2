<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="To‘lovlar" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">To‘lovlar</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">Mijoz to‘lovlari</p>
        </div>
        <div class="flex gap-2">
          <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
          <button type="button" class="btn" @click="openCreate">+ Yangi to‘lov</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Mijoz</th>
              <th class="th">Tur</th>
              <th class="th">Summa</th>
              <th class="th">Sana</th>
              <th class="th">Buyurtma</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">To‘lov yo‘q</td></tr>
            <tr v-for="p in filtered" :key="p.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ p.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ p.clientFullName || '—' }}</td>
              <td class="td">{{ p.paymentTypeName || '—' }}</td>
              <td class="td">{{ money(p.paymentAmount) }}</td>
              <td class="td">{{ formatDate(p.paymentDate) }}</td>
              <td class="td">{{ p.saleOrderId || '—' }}</td>
              <td class="td text-right">
                <RowActions :edit="false" @delete="onDelete(p)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="modalOpen = false">
      <div class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">Yangi to‘lov</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">Mijoz *</label>
            <select v-model.number="form.clientId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">To‘lov turi *</label>
            <select v-model.number="form.paymentTypeId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="t in paymentTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">Qabul qiluvchi *</label>
            <select v-model.number="form.userId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Summa *</label>
              <input v-model.number="form.paymentAmount" type="number" min="0.01" step="0.01" required class="field" />
            </div>
            <div>
              <label class="lbl">Sana *</label>
              <input v-model="form.paymentDate" type="datetime-local" required class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Savdo buyurtmasi (ixtiyoriy)</label>
            <select v-model.number="form.saleOrderId" class="field">
              <option :value="0">— avans / umumiy —</option>
              <option v-for="o in saleOrders" :key="o.id" :value="o.id">
                #{{ o.id }} — {{ o.clientFullName || '' }} ({{ money(o.debtSum) }})
              </option>
            </select>
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
  createPayment,
  deletePayment,
  fetchPayments,
  fetchPaymentTypes,
  type Payment,
  type PaymentType,
} from '@/api/payments'
import { fetchClients, type Client } from '@/api/clients'
import { fetchUsers, type UserItem } from '@/api/users'
import { fetchSaleOrders, type SaleOrder } from '@/api/sales'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/http'

const items = ref<Payment[]>([])
const clients = ref<Client[]>([])
const users = ref<UserItem[]>([])
const paymentTypes = ref<PaymentType[]>([])
const saleOrders = ref<SaleOrder[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
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

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((p) =>
    [p.clientFullName, p.paymentTypeName, String(p.saleOrderId || '')]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

function money(v?: number) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(Number(v))
}

function formatDate(v?: string) {
  if (!v) return '—'
  return v.replace('T', ' ').slice(0, 16)
}

function toApiDate(local: string) {
  if (!local) return local
  return local.length === 16 ? `${local}:00` : local
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [payRes, clientsRes, usersRes, typesRes, salesRes] = await Promise.all([
      fetchPayments(),
      fetchClients(),
      fetchUsers(),
      fetchPaymentTypes(),
      fetchSaleOrders(),
    ])
    items.value = payRes.data?.content || []
    clients.value = clientsRes.data || []
    users.value = usersRes.data || []
    paymentTypes.value = typesRes.data?.content || []
    saleOrders.value = salesRes.data?.content || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  const me = users.value.find((u) => u.username === auth.username)
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  form.clientId = clients.value[0]?.id || 0
  form.userId = me?.id || users.value[0]?.id || 0
  form.paymentTypeId = paymentTypes.value[0]?.id || 0
  form.paymentAmount = 0
  form.paymentDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
  form.saleOrderId = 0
  form.comment = ''
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    await createPayment({
      clientId: form.clientId,
      userId: form.userId,
      paymentTypeId: form.paymentTypeId,
      paymentAmount: form.paymentAmount,
      paymentDate: toApiDate(form.paymentDate),
      saleOrderId: form.saleOrderId || null,
      comment: form.comment || undefined,
    })
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onDelete(p: Payment) {
  if (!confirm(`To‘lov #${p.id} o‘chirilsinmi?`)) return
  try {
    await deletePayment(p.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'O‘chirishda xatolik'
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
