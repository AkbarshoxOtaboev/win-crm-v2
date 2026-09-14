<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="To‘lov turlari" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">To‘lov turlari</h3>
        <button type="button" class="btn" @click="openCreate">+ Yangi</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">Nomi</th>
            <th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in items" :key="t.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ t.id }}</td>
            <td class="td font-medium">{{ t.name }}</td>
            <td class="td text-right"><RowActions @edit="openEdit(t)" @delete="onDelete(t)" /></td>
          </tr>
          <tr v-if="items.length === 0"><td colspan="3" class="empty">Tur yo‘q</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="modal" class="overlay" @click.self="modal = false">
      <div class="modal">
        <form class="space-y-3" @submit.prevent="onSave">
          <input v-model="name" required class="field" placeholder="Nomi *" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modal = false">Bekor</button>
            <button type="submit" class="btn">Saqlash</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createPaymentType,
  deletePaymentType,
  fetchPaymentTypes,
  updatePaymentType,
  type PaymentType,
} from '@/api/payments'
import { formatApiError } from '@/api/http'

const items = ref<PaymentType[]>([])
const error = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const name = ref('')

async function load() {
  try {
    const res = await fetchPaymentTypes()
    items.value = res.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openCreate() {
  editingId.value = null
  name.value = ''
  modal.value = true
}

function openEdit(t: PaymentType) {
  editingId.value = t.id
  name.value = t.name
  modal.value = true
}

async function onSave() {
  try {
    if (editingId.value) await updatePaymentType(editingId.value, name.value.trim())
    else await createPaymentType(name.value.trim())
    modal.value = false
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onDelete(t: PaymentType) {
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
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 24rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
