<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="client?.fullName || 'Mijoz'" />
    <div class="mb-4"><router-link to="/clients" class="text-sm text-brand-500">← Mijozlar</router-link></div>
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="client" class="card mb-4 p-5 grid grid-cols-2 gap-3 text-sm md:grid-cols-4">
      <div><b>Telefon:</b> {{ client.phone }}</div>
      <div><b>Manzil:</b> {{ client.address }}</div>
      <div><b>Guruh:</b> {{ client.clientGroupName || '—' }}</div>
      <div><b>INN:</b> {{ client.inn || '—' }}</div>
    </div>
    <div class="card mb-4 p-5 flex flex-wrap items-end gap-3">
      <div><span class="lbl">Sotib olish</span>{{ money(balance?.totalPurchase) }}</div>
      <div><span class="lbl">To‘langan</span>{{ money(balance?.totalPaid) }}</div>
      <div><span class="lbl">Qarz</span>{{ money(balance?.totalDebt) }}</div>
      <button type="button" class="btn" @click="onRecalc">Qayta hisoblash</button>
      <form class="flex gap-2" @submit.prevent="onAdjust">
        <input v-model.number="adjPurchase" type="number" class="field w-32" placeholder="Purchase" />
        <input v-model.number="adjPaid" type="number" class="field w-32" placeholder="Paid" />
        <button type="submit" class="ghost">Adjust</button>
      </form>
    </div>

    <div class="mb-4 flex gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'notes' }" @click="tab = 'notes'">Izohlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'sales' }" @click="tab = 'sales'">Savdolar</button>
      <button type="button" class="tab" :class="{ active: tab === 'payments' }" @click="tab = 'payments'">To‘lovlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'sms' }" @click="tab = 'sms'">SMS tarix</button>
    </div>

    <div v-show="tab === 'notes'" class="card">
      <div class="p-4 border-b">
        <form class="grid gap-2 md:grid-cols-4" @submit.prevent="onNoteSave">
          <select v-model="noteForm.type" class="field">
            <option>CALL</option>
            <option>MEETING</option>
            <option>SMS</option>
            <option>PAYMENT_PROMISE</option>
            <option>OTHER</option>
          </select>
          <input v-model="noteForm.content" required class="field md:col-span-2" placeholder="Mazmun" />
          <button type="submit" class="btn">Qo‘shish</button>
        </form>
      </div>
      <table class="min-w-full">
        <thead><tr><th class="th">Tur</th><th class="th">Mazmun</th><th class="th">Status</th><th class="th text-right">Amallar</th></tr></thead>
        <tbody>
          <tr v-for="n in notes" :key="n.id" class="border-b border-gray-100">
            <td class="td">{{ n.type }}</td>
            <td class="td">{{ n.content }}</td>
            <td class="td">
              <select :value="n.reminderStatus" class="field" @change="onReminder(n.id, ($event.target as HTMLSelectElement).value)">
                <option>NONE</option>
                <option>PENDING</option>
                <option>DONE</option>
                <option>BROKEN</option>
              </select>
            </td>
            <td class="td text-right"><RowActions :edit="false" @delete="onNoteDelete(n.id)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'sales'" class="card">
      <table class="min-w-full">
        <thead><tr><th class="th">#</th><th class="th">Jami</th><th class="th">Qarz</th><th class="th">Holat</th></tr></thead>
        <tbody>
          <tr v-for="o in sales" :key="o.id" class="border-b border-gray-100">
            <td class="td"><router-link :to="`/sales/${o.id}`" class="text-brand-500">#{{ o.id }}</router-link></td>
            <td class="td">{{ money(o.totalSum) }}</td>
            <td class="td">{{ money(o.debtSum) }}</td>
            <td class="td">{{ o.orderStatus }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'payments'" class="card">
      <table class="min-w-full">
        <thead><tr><th class="th">#</th><th class="th">Summa</th><th class="th">Tur</th><th class="th">Sana</th></tr></thead>
        <tbody>
          <tr v-for="p in payments" :key="p.id" class="border-b border-gray-100">
            <td class="td">{{ p.id }}</td>
            <td class="td">{{ money(p.paymentAmount) }}</td>
            <td class="td">{{ p.paymentTypeName }}</td>
            <td class="td">{{ formatDate(p.paymentDate) }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'sms'" class="card">
      <table class="min-w-full">
        <thead><tr><th class="th">#</th><th class="th">Status</th><th class="th">Xabar</th><th class="th">Sana</th></tr></thead>
        <tbody>
          <tr v-for="h in sms" :key="h.id" class="border-b border-gray-100">
            <td class="td">{{ h.id }}</td>
            <td class="td">{{ h.status }}</td>
            <td class="td">{{ h.message }}</td>
            <td class="td">{{ formatDate(h.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import { fetchClient, type Client } from '@/api/clients'
import {
  adjustClientBalance,
  fetchClientBalance,
  recalculateClientBalance,
  type ClientBalance,
} from '@/api/clientBalances'
import {
  createClientNote,
  deleteClientNote,
  fetchClientNotes,
  updateReminderStatus,
  type ClientNote,
} from '@/api/clientNotes'
import { fetchSaleOrdersByClient, type SaleOrder } from '@/api/sales'
import { fetchPaymentsByClient, type Payment } from '@/api/payments'
import { fetchDebtHistoryByClient, type DebtNotificationHistory } from '@/api/debt'
import { formatApiError } from '@/api/http'
import { formatDate, money } from '@/utils/format'

const route = useRoute()
const tab = ref<'notes' | 'sales' | 'payments' | 'sms'>('notes')
const client = ref<Client | null>(null)
const balance = ref<ClientBalance | null>(null)
const notes = ref<ClientNote[]>([])
const sales = ref<SaleOrder[]>([])
const payments = ref<Payment[]>([])
const sms = ref<DebtNotificationHistory[]>([])
const error = ref<string | null>(null)
const adjPurchase = ref(0)
const adjPaid = ref(0)
const noteForm = reactive({ type: 'CALL', content: '' })

function id() {
  return Number(route.params.id)
}

async function load() {
  error.value = null
  try {
    const [c, b, n, s, p, h] = await Promise.all([
      fetchClient(id()),
      fetchClientBalance(id()),
      fetchClientNotes(id()),
      fetchSaleOrdersByClient(id()),
      fetchPaymentsByClient(id()),
      fetchDebtHistoryByClient(id()),
    ])
    client.value = c.data
    balance.value = b.data
    notes.value = n.data || []
    sales.value = s.data?.content || []
    payments.value = p.data?.content || []
    sms.value = h.data?.content || []
    adjPurchase.value = Number(balance.value?.totalPurchase || 0)
    adjPaid.value = Number(balance.value?.totalPaid || 0)
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onRecalc() {
  try {
    const res = await recalculateClientBalance(id())
    balance.value = res.data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onAdjust() {
  try {
    const res = await adjustClientBalance(id(), {
      totalPurchase: adjPurchase.value,
      totalPaid: adjPaid.value,
    })
    balance.value = res.data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onNoteSave() {
  try {
    await createClientNote({
      clientId: id(),
      type: noteForm.type,
      content: noteForm.content,
    })
    noteForm.content = ''
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onReminder(noteId: number, status: string) {
  try {
    await updateReminderStatus(noteId, status)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onNoteDelete(noteId: number) {
  if (!confirm('Izoh o‘chirilsinmi?')) return
  try {
    await deleteClientNote(noteId)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; font-size: 0.875rem; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; }
.lbl { display: block; font-size: 0.75rem; color: #6b7280; }
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.tab.active { background: #465fff; color: #fff; border-color: #465fff; }
</style>
