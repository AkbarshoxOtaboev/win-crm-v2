<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Xarajat kategoriyalari" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Kategoriyalar</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openCreate">+ Yangi</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">Nomi</th>
            <th class="th">Izoh</th>
            <th class="th">Status</th>
            <th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in items" :key="c.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ c.id }}</td>
            <td class="td font-medium">{{ c.name }}</td>
            <td class="td">{{ c.description || '—' }}</td>
            <td class="td">{{ c.status || 'ACTIVE' }}</td>
            <td class="td text-right"><RowActions @edit="openEdit(c)" @delete="onDelete(c)" /></td>
          </tr>
          <tr v-if="items.length === 0"><td colspan="5" class="empty">Kategoriya yo‘q</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="modal" class="overlay">
      <div class="modal">
        <form class="space-y-3" @submit.prevent="onSave">
          <input v-model="form.name" required class="field" placeholder="Nomi *" />
          <input v-model="form.description" class="field" placeholder="Izoh" />
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
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createExpenseCategory,
  deleteExpenseCategory,
  fetchExpenseCategories,
  updateExpenseCategory,
  type ExpenseCategory,
} from '@/api/expenses'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'

const { writeBlocked } = useFilialScope()
const items = ref<ExpenseCategory[]>([])
const error = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', description: '' })

async function load() {
  try {
    const res = await fetchExpenseCategories()
    items.value = res.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', description: '' })
  modal.value = true
}

function openEdit(c: ExpenseCategory) {
  editingId.value = c.id
  Object.assign(form, { name: c.name, description: c.description || '' })
  modal.value = true
}

async function onSave() {
  try {
    const payload = { name: form.name.trim(), description: form.description.trim() || undefined }
    if (editingId.value) await updateExpenseCategory(editingId.value, payload)
    else await createExpenseCategory(payload)
    modal.value = false
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onDelete(c: ExpenseCategory) {
  if (!confirm(`“${c.name}” o‘chirilsinmi?`)) return
  try {
    await deleteExpenseCategory(c.id)
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
