<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="To‘lovlar" />
    <div class="mb-4 flex gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'payments' }" @click="tab = 'payments'">To‘lovlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'types' }" @click="tab = 'types'">To‘lov turlari</button>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div v-show="tab === 'payments'" class="card">
      <div class="head">
        <h3 class="title">To‘lovlar</h3>
        <div class="flex flex-wrap gap-2">
          <select v-model.number="filterClientId" class="field w-44" @change="load">
            <option :value="0">Barcha mijozlar</option>
            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
          </select>
          <select v-model.number="filterOrderId" class="field w-44" @change="load">
            <option :value="0">Barcha savdolar</option>
            <option v-for="o in saleOrders" :key="o.id" :value="o.id">#{{ o.id }}</option>
          </select>
          <input v-model="search" type="search" placeholder="Qidiruv..." class="field w-40" />
          <button type="button" class="btn" @click="openCreate">+ Yangi to‘lov</button>
        </div>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100">
            <th class="th">#</th><th class="th">Mijoz</th><th class="th">Tur</th><th class="th">Summa</th><th class="th">Sana</th><th class="th">Buyurtma</th><th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="empty">Yuklanmoqda...</td></tr>
          <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">To‘lov yo‘q</td></tr>
          <tr v-for="p in filtered" :key="p.id" class="border-b border-gray-100">
            <td class="td">{{ p.id }}</td>
            <td class="td">{{ p.clientFullName || '—' }}</td>
            <td class="td">{{ p.paymentTypeName || '—' }}</td>
            <td class="td">{{ money(p.paymentAmount) }}</td>
            <td class="td">{{ formatDate(p.paymentDate) }}</td>
            <td class="td">{{ p.saleOrderId || '—' }}</td>
            <td class="td text-right"><RowActions @edit="openEdit(p)" @delete="onDelete(p)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'types'" class="card">
      <div class="head">
        <h3 class="title">To‘lov turlari</h3>
        <button type="button" class="btn" @click="openTypeCreate">+ Tur</button>
      </div>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100"><th class="th">#</th><th class="th">Nomi</th><th class="th text-right">Amallar</th></tr></thead>
        <tbody>
          <tr v-for="t in paymentTypes" :key="t.id" class="border-b border-gray-100">
            <td class="td">{{ t.id }}</td>
            <td class="td">{{ t.name }}</td>
            <td class="td text-right"><RowActions @edit="openTypeEdit(t)" @delete="onTypeDelete(t)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="modalOpen" class="overlay" @click.self="modalOpen = false">
      <div class="modal">
        <h3 class="title mb-4">{{ editingId ? 'Tahrirlash' : 'Yangi to‘lov' }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <select v-model.number="form.clientId" required class="field">
            <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
          </select>
          <select v-model.number="form.paymentTypeId" required class="field">
            <option v-for="t in paymentTypes" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
          <select v-model.number="form.userId" required class="field">
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
          <input v-model.number="form.paymentAmount" type="number" min="0.01" step="0.01" required class="field" />
          <input v-model="form.paymentDate" type="datetime-local" required class="field" />
          <select v-model.number="form.saleOrderId" class="field">
            <option :value="0">— avans —</option>
            <option v-for="o in saleOrders" :key="o.id" :value="o.id">#{{ o.id }} ({{ money(o.debtSum) }})</option>
          </select>
          <input v-model="form.comment" class="field" placeholder="Izoh" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modalOpen = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Saqlash</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="typeModal" class="overlay" @click.self="typeModal = false">
      <div class="modal">
        <form class="space-y-3" @submit.prevent="onTypeSubmit">
          <input v-model="typeName" required class="field" placeholder="Nomi" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="typeModal = false">Bekor</button>
            <button type="submit" class="btn">Saqlash</button>
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
  createPaymentType,
  deletePayment,
  deletePaymentType,
  fetchPayments,
  fetchPaymentsByClient,
  fetchPaymentsBySaleOrder,
  fetchPaymentTypes,
  updatePayment,
  updatePaymentType,
  type Payment,
  type PaymentType,
} from '@/api/payments'
import { fetchClients, type Client } from '@/api/clients'
import { fetchUsers, type UserItem } from '@/api/users'
import { fetchSaleOrders, type SaleOrder } from '@/api/sales'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import { formatDate, money, nowLocal, toApiDate } from '@/utils/format'

const tab = ref<'payments' | 'types'>('payments')
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
const editingId = ref<number | null>(null)
const filterClientId = ref(0)
const filterOrderId = ref(0)
const typeModal = ref(false)
const typeEditingId = ref<number | null>(null)
const typeName = ref('')
const auth = useAuthStore()
const form = reactive({
  clientId: 0, userId: 0, paymentTypeId: 0, paymentAmount: 0, paymentDate: '', saleOrderId: 0, comment: '',
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((p) =>
    [p.clientFullName, p.paymentTypeName, String(p.saleOrderId || '')].some((v) => String(v || '').toLowerCase().includes(q)),
  )
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const [clientsRes, usersRes, typesRes, salesRes] = await Promise.all([
      fetchClients(), fetchUsers(), fetchPaymentTypes(), fetchSaleOrders(),
    ])
    clients.value = clientsRes.data || []
    users.value = usersRes.data || []
    paymentTypes.value = typesRes.data?.content || []
    saleOrders.value = salesRes.data?.content || []
    let payRes
    if (filterClientId.value) payRes = await fetchPaymentsByClient(filterClientId.value)
    else if (filterOrderId.value) payRes = await fetchPaymentsBySaleOrder(filterOrderId.value)
    else payRes = await fetchPayments()
    items.value = payRes.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

function fillForm(p?: Payment) {
  const me = users.value.find((u) => u.username === auth.username)
  form.clientId = p?.clientId || clients.value[0]?.id || 0
  form.userId = p?.userId || me?.id || users.value[0]?.id || 0
  form.paymentTypeId = p?.paymentTypeId || paymentTypes.value[0]?.id || 0
  form.paymentAmount = Number(p?.paymentAmount || 0)
  form.paymentDate = p?.paymentDate ? p.paymentDate.slice(0, 16) : nowLocal()
  form.saleOrderId = p?.saleOrderId || 0
  form.comment = p?.comment || ''
}

function openCreate() {
  editingId.value = null
  fillForm()
  formError.value = null
  modalOpen.value = true
}

function openEdit(p: Payment) {
  editingId.value = p.id
  fillForm(p)
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
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
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
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
    error.value = formatApiError(e)
  }
}

function openTypeCreate() {
  typeEditingId.value = null
  typeName.value = ''
  typeModal.value = true
}

function openTypeEdit(t: PaymentType) {
  typeEditingId.value = t.id
  typeName.value = t.name
  typeModal.value = true
}

async function onTypeSubmit() {
  try {
    if (typeEditingId.value) await updatePaymentType(typeEditingId.value, typeName.value.trim())
    else await createPaymentType(typeName.value.trim())
    typeModal.value = false
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onTypeDelete(t: PaymentType) {
  if (!confirm(`“${t.name}” o‘chirilsinmi?`)) return
  try {
    await deletePaymentType(t.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; }
.head { display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; font-size: 0.875rem; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.tab.active { background: #465fff; color: #fff; border-color: #465fff; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
