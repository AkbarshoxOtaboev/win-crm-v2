<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Omborlar" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Omborlar</h3>
        <div class="flex gap-2">
          <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
          <button type="button" class="btn" @click="openCreate">+ Yangi ombor</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th">Status</th>
              <th class="th">Yoqish / o‘chirish</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="5" class="empty">Ombor yo‘q</td></tr>
            <tr v-for="w in filtered" :key="w.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ w.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ w.name }}</td>
              <td class="td">
                <span
                  class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium"
                  :class="isActiveStatus(w.status) ? 'bg-success-50 text-success-600' : 'bg-gray-100 text-gray-500'"
                >
                  {{ isActiveStatus(w.status) ? 'Active' : 'No active' }}
                </span>
              </td>
              <td class="td">
                <button
                  type="button"
                  class="status-toggle"
                  :class="isActiveStatus(w.status) ? 'on' : 'off'"
                  :aria-pressed="isActiveStatus(w.status)"
                  :title="isActiveStatus(w.status) ? 'O‘chirish' : 'Yoqish'"
                  @click="onStatus(w)"
                >
                  <span class="status-knob" />
                </button>
              </td>
              <td class="td text-right">
                <RowActions @edit="openEdit(w)" @delete="onDelete(w)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="modalOpen = false">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? 'Omborni tahrirlash' : 'Yangi ombor' }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Nomi *</label>
            <input v-model="name" required class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm dark:border-gray-700" @click="modalOpen = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Saqlash' }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  changeWarehouseStatus,
  createWarehouse,
  deleteWarehouse,
  fetchWarehouses,
  updateWarehouse,
  type Warehouse,
} from '@/api/warehouses'
import { ApiError } from '@/api/http'

const items = ref<Warehouse[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const name = ref('')

function isActiveStatus(status?: string) {
  return !status || status === 'ACTIVE'
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return [...items.value]
    .filter((w) => !q || w.name.toLowerCase().includes(q))
    .sort((a, b) => Number(a.id) - Number(b.id))
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchWarehouses()
    items.value = res.data || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  name.value = ''
  formError.value = null
  modalOpen.value = true
}

function openEdit(w: Warehouse) {
  editingId.value = w.id
  name.value = w.name
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    if (editingId.value) await updateWarehouse(editingId.value, name.value.trim())
    else await createWarehouse(name.value.trim())
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onDelete(w: Warehouse) {
  if (!confirm(`“${w.name}” o‘chirilsinmi?`)) return
  try {
    await deleteWarehouse(w.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'O‘chirishda xatolik'
  }
}

async function onStatus(w: Warehouse) {
  try {
    await changeWarehouseStatus(w.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Statusni o‘zgartirishda xatolik'
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
.status-toggle {
  position: relative;
  display: inline-flex;
  height: 1.5rem;
  width: 2.75rem;
  align-items: center;
  border-radius: 9999px;
  transition: background 0.15s ease;
  border: none;
  cursor: pointer;
  padding: 0;
}
.status-toggle.on { background: #12b76a; }
.status-toggle.off { background: #d1d5db; }
.status-knob {
  position: absolute;
  left: 0.15rem;
  height: 1.2rem;
  width: 1.2rem;
  border-radius: 9999px;
  background: #fff;
  transition: transform 0.15s ease;
  box-shadow: 0 1px 2px rgba(0,0,0,.15);
}
.status-toggle.off .status-knob { transform: translateX(0); }
.status-toggle.on .status-knob { transform: translateX(1.25rem); }
:global(.dark) .status-toggle.off { background: #4b5563; }
</style>
