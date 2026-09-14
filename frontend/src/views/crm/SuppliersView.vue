<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Yetkazib beruvchilar" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Yetkazib beruvchilar</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">Supplier CRUD</p>
        </div>
        <div class="flex gap-2">
          <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
          <button type="button" class="btn" @click="openCreate">+ Yangi</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th">Telefon</th>
              <th class="th">Manzil</th>
              <th class="th">INN</th>
              <th class="th">Status</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">Yetkazib beruvchi yo‘q</td></tr>
            <tr v-for="s in filtered" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ s.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ s.name }}</td>
              <td class="td">
                {{ s.phone }}
                <span v-if="s.additionalPhone" class="block text-xs text-gray-400">{{ s.additionalPhone }}</span>
              </td>
              <td class="td">{{ s.address || '—' }}</td>
              <td class="td">{{ s.inn || '—' }}</td>
              <td class="td">{{ s.status || 'ACTIVE' }}</td>
              <td class="td text-right">
                <RowActions @edit="openEdit(s)" @delete="onDelete(s)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="modalOpen = false">
      <div class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? 'Tahrirlash' : 'Yangi yetkazib beruvchi' }}
        </h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="lbl">Nomi *</label>
            <input v-model="form.name" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Telefon *</label>
              <input v-model="form.phone" required placeholder="+998901234567" class="field" />
            </div>
            <div>
              <label class="lbl">Qo‘shimcha telefon</label>
              <input v-model="form.additionalPhone" class="field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">INN</label>
              <input v-model="form.inn" class="field" />
            </div>
            <div>
              <label class="lbl">Manzil</label>
              <input v-model="form.address" class="field" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">Bank</label>
              <input v-model="form.bankName" class="field" />
            </div>
            <div>
              <label class="lbl">MFO</label>
              <input v-model="form.mfo" class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">Hisob raqam</label>
            <input v-model="form.accountNumber" class="field" />
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <input v-model="form.description" class="field" />
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
  createSupplier,
  deleteSupplier,
  fetchSuppliers,
  updateSupplier,
  type Supplier,
} from '@/api/suppliers'
import { ApiError } from '@/api/http'

const items = ref<Supplier[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const form = reactive({
  name: '',
  phone: '',
  additionalPhone: '',
  inn: '',
  address: '',
  bankName: '',
  mfo: '',
  accountNumber: '',
  description: '',
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((s) =>
    [s.name, s.phone, s.address, s.inn].filter(Boolean).some((v) => String(v).toLowerCase().includes(q)),
  )
})

function resetForm() {
  form.name = ''
  form.phone = ''
  form.additionalPhone = ''
  form.inn = ''
  form.address = ''
  form.bankName = ''
  form.mfo = ''
  form.accountNumber = ''
  form.description = ''
}

function payload() {
  return {
    name: form.name.trim(),
    phone: form.phone.trim(),
    additionalPhone: form.additionalPhone.trim() || undefined,
    inn: form.inn.trim() || undefined,
    address: form.address.trim() || undefined,
    bankName: form.bankName.trim() || undefined,
    mfo: form.mfo.trim() || undefined,
    accountNumber: form.accountNumber.trim() || undefined,
    description: form.description.trim() || undefined,
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchSuppliers()
    items.value = res.data?.content || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  formError.value = null
  modalOpen.value = true
}

function openEdit(s: Supplier) {
  editingId.value = s.id
  form.name = s.name
  form.phone = s.phone
  form.additionalPhone = s.additionalPhone || ''
  form.inn = s.inn || ''
  form.address = s.address || ''
  form.bankName = s.bankName || ''
  form.mfo = s.mfo || ''
  form.accountNumber = s.accountNumber || ''
  form.description = s.description || ''
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    if (editingId.value) await updateSupplier(editingId.value, payload())
    else await createSupplier(payload())
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onDelete(s: Supplier) {
  if (!confirm(`“${s.name}” o‘chirilsinmi?`)) return
  try {
    await deleteSupplier(s.id)
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
