<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Mijozlar" />

    <div
      class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div
        class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800"
      >
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Mijozlar</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">
            Ro‘yxat, qo‘shish, tahrirlash va o‘chirish
          </p>
        </div>
        <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
          <input
            v-model="search"
            type="search"
            placeholder="Qidiruv: ism, telefon, manzil..."
            class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90 sm:w-64"
          />
          <button
            type="button"
            class="inline-flex h-10 items-center justify-center rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600"
            @click="openCreate"
          >
            + Yangi mijoz
          </button>
        </div>
      </div>

      <div
        v-if="error"
        class="mx-5 mt-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
      >
        {{ error }}
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">#</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">F.I.Sh</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Telefon</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Manzil</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">INN</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Status</th>
              <th class="px-5 py-3 text-right text-xs font-medium text-gray-500">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="7" class="px-5 py-10 text-center text-sm text-gray-500">
                Yuklanmoqda...
              </td>
            </tr>
            <tr v-else-if="filtered.length === 0">
              <td colspan="7" class="px-5 py-10 text-center text-sm text-gray-500">
                Mijoz topilmadi
              </td>
            </tr>
            <tr
              v-for="client in filtered"
              :key="client.id"
              class="border-b border-gray-100 dark:border-gray-800"
            >
              <td class="px-5 py-3 text-sm text-gray-500">{{ client.id }}</td>
              <td class="px-5 py-3 text-sm font-medium text-gray-800 dark:text-white/90">
                {{ client.fullName }}
              </td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">
                {{ client.phone }}
                <span
                  v-if="client.additionalPhone"
                  class="block text-xs text-gray-400"
                >{{ client.additionalPhone }}</span>
              </td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">
                {{ client.address }}
              </td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">
                {{ client.inn || '—' }}
              </td>
              <td class="px-5 py-3 text-sm">
                <span
                  class="rounded-full px-2.5 py-0.5 text-xs font-medium"
                  :class="statusClass(client.status)"
                >
                  {{ client.status || 'ACTIVE' }}
                </span>
              </td>
              <td class="px-5 py-3 text-right text-sm">
                <RowActions @edit="openEdit(client)" @delete="onDelete(client)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <div
      v-if="modalOpen"
      class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4"
      @click.self="closeModal"
    >
      <div
        class="w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 shadow-xl dark:border-gray-800 dark:bg-gray-900"
      >
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? 'Mijozni tahrirlash' : 'Yangi mijoz' }}
        </h3>

        <div
          v-if="formError"
          class="mb-3 rounded-lg border border-error-200 bg-error-50 px-3 py-2 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
        >
          {{ formError }}
        </div>

        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">F.I.Sh *</label>
            <input
              v-model="form.fullName"
              required
              class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
            />
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Telefon *</label>
              <input
                v-model="form.phone"
                required
                maxlength="20"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
              />
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Qo‘shimcha tel</label>
              <input
                v-model="form.additionalPhone"
                maxlength="20"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
              />
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Manzil *</label>
            <input
              v-model="form.address"
              required
              class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
            />
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">INN</label>
              <input
                v-model="form.inn"
                maxlength="20"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
              />
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Bank</label>
              <input
                v-model="form.bankName"
                maxlength="150"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
              />
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">MFO</label>
              <input
                v-model="form.mfo"
                maxlength="10"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
              />
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Hisob raqam</label>
              <input
                v-model="form.accountNumber"
                maxlength="30"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
              />
            </div>
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Izoh</label>
            <textarea
              v-model="form.description"
              rows="2"
              class="w-full rounded-lg border border-gray-300 bg-transparent px-3 py-2 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
            />
          </div>

          <div class="flex justify-end gap-2 pt-2">
            <button
              type="button"
              class="h-10 rounded-lg border border-gray-300 px-4 text-sm text-gray-700 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300"
              @click="closeModal"
            >
              Bekor
            </button>
            <button
              type="submit"
              :disabled="saving"
              class="h-10 rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60"
            >
              {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
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
  createClient,
  deleteClient,
  fetchClients,
  updateClient,
  type Client,
  type ClientPayload,
} from '@/api/clients'
import { ApiError } from '@/api/http'

const clients = ref<Client[]>([])
const loading = ref(false)
const saving = ref(false)
const deletingId = ref<number | null>(null)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)

const emptyForm = (): ClientPayload => ({
  fullName: '',
  phone: '',
  address: '',
  inn: '',
  additionalPhone: '',
  bankName: '',
  mfo: '',
  accountNumber: '',
  description: '',
})

const form = reactive<ClientPayload>(emptyForm())

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return clients.value
  return clients.value.filter((c) =>
    [c.fullName, c.phone, c.additionalPhone, c.address, c.inn]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

function statusClass(status?: string) {
  if (status === 'DISABLED') {
    return 'bg-warning-50 text-warning-600 dark:bg-warning-500/10 dark:text-warning-400'
  }
  return 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400'
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchClients()
    clients.value = res.data || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Mijozlarni yuklab bo‘lmadi'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formError.value = null
  modalOpen.value = true
}

function openEdit(client: Client) {
  editingId.value = client.id
  Object.assign(form, {
    fullName: client.fullName || '',
    phone: client.phone || '',
    address: client.address || '',
    inn: client.inn || '',
    additionalPhone: client.additionalPhone || '',
    bankName: client.bankName || '',
    mfo: client.mfo || '',
    accountNumber: client.accountNumber || '',
    description: client.description || '',
  })
  formError.value = null
  modalOpen.value = true
}

function closeModal() {
  modalOpen.value = false
}

function payloadFromForm(): ClientPayload {
  const clean = (v?: string) => {
    const t = (v || '').trim()
    return t.length ? t : undefined
  }
  return {
    fullName: form.fullName.trim(),
    phone: form.phone.trim(),
    address: form.address.trim(),
    inn: clean(form.inn),
    additionalPhone: clean(form.additionalPhone),
    bankName: clean(form.bankName),
    mfo: clean(form.mfo),
    accountNumber: clean(form.accountNumber),
    description: clean(form.description),
  }
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    const payload = payloadFromForm()
    if (editingId.value) {
      await updateClient(editingId.value, payload)
    } else {
      await createClient(payload)
    }
    closeModal()
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Saqlashda xatolik'
  } finally {
    saving.value = false
  }
}

async function onDelete(client: Client) {
  if (!confirm(`“${client.fullName}” o‘chirilsinmi?`)) return
  deletingId.value = client.id
  error.value = null
  try {
    await deleteClient(client.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'O‘chirishda xatolik'
  } finally {
    deletingId.value = null
  }
}

onMounted(load)
</script>
