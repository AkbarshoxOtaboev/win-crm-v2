<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Mijozlar" />
    <div class="mb-4 flex gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'list' }" @click="tab = 'list'">Mijozlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'groups' }" @click="tab = 'groups'">Guruhlar</button>
      <button type="button" class="tab" :class="{ active: tab === 'debtors' }" @click="loadDebtors(); tab = 'debtors'">Qarzdorlar</button>
    </div>

    <div v-show="tab === 'list'">
    <div
      class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div
        class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800"
      >
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Mijozlar</h3>
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
                <router-link :to="`/clients/${client.id}`" class="text-brand-500 hover:underline">
                  {{ client.fullName }}
                </router-link>
              </td>
              <td class="px-5 py-3 text-sm text-gray-600 dark:text-gray-300">
                {{ formatUzPhone(client.phone) }}
                <span
                  v-if="client.additionalPhone"
                  class="block text-xs text-gray-400"
                >{{ formatUzPhone(client.additionalPhone) }}</span>
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
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Guruh</label>
            <select v-model.number="form.clientGroupId" class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90">
              <option :value="0">— guruh yo‘q —</option>
              <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </div>
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
                :value="form.phone"
                required
                inputmode="tel"
                placeholder="+998-(97)-221-88-96"
                maxlength="19"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
                @input="onPhoneInput('phone', $event)"
              />
              <p v-if="phoneHint" class="mt-1 text-xs text-error-500">{{ phoneHint }}</p>
            </div>
            <div>
              <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">Qo‘shimcha tel</label>
              <input
                :value="form.additionalPhone"
                inputmode="tel"
                placeholder="+998-(97)-221-88-96"
                maxlength="19"
                class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
                @input="onPhoneInput('additionalPhone', $event)"
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
    </div>

    <div v-show="tab === 'groups'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex justify-between px-5 py-4">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Guruhlar</h3>
        <button type="button" class="h-10 rounded-lg bg-brand-500 px-4 text-sm text-white" @click="openGroupCreate">+ Guruh</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="px-5 py-3 text-left text-xs text-gray-500">#</th>
            <th class="px-5 py-3 text-left text-xs text-gray-500">Nomi</th>
            <th class="px-5 py-3 text-right text-xs text-gray-500">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="g in groups" :key="g.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="px-5 py-3 text-sm">{{ g.id }}</td>
            <td class="px-5 py-3 text-sm">{{ g.name }}</td>
            <td class="px-5 py-3 text-right"><RowActions @edit="openGroupEdit(g)" @delete="onGroupDelete(g)" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-show="tab === 'debtors'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="px-5 py-4 flex justify-between">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Qarzdorlar</h3>
        <button type="button" class="h-10 rounded-lg bg-brand-500 px-4 text-sm text-white" @click="sendSelectedSms">Tanlanganlarga SMS</button>
      </div>
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="px-5 py-3"></th>
            <th class="px-5 py-3 text-left text-xs text-gray-500">Mijoz</th>
            <th class="px-5 py-3 text-left text-xs text-gray-500">Telefon</th>
            <th class="px-5 py-3 text-left text-xs text-gray-500">Qarz</th>
            <th class="px-5 py-3 text-right text-xs text-gray-500">SMS</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in debtors" :key="d.clientId" class="border-b border-gray-100 dark:border-gray-800">
            <td class="px-5 py-3"><input v-model="selectedDebtors" type="checkbox" :value="d.clientId" /></td>
            <td class="px-5 py-3 text-sm">
              <router-link :to="`/clients/${d.clientId}`" class="text-brand-500">{{ d.clientFullName }}</router-link>
            </td>
            <td class="px-5 py-3 text-sm">{{ d.phone || '—' }}</td>
            <td class="px-5 py-3 text-sm">{{ d.totalDebt ?? d.debtSum }}</td>
            <td class="px-5 py-3 text-right">
              <button type="button" class="text-sm text-brand-500" @click="sendOneSms(d.clientId!)">SMS</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="groupModal" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="groupModal = false">
      <div class="w-full max-w-md rounded-2xl bg-white p-5 dark:bg-gray-900">
        <h3 class="mb-3 text-lg font-semibold">Guruh</h3>
        <form class="space-y-3" @submit.prevent="onGroupSubmit">
          <input v-model="groupName" required class="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm" />
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border px-4" @click="groupModal = false">Bekor</button>
            <button type="submit" class="h-10 rounded-lg bg-brand-500 px-4 text-white">Saqlash</button>
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
import {
  createClientGroup,
  deleteClientGroup,
  fetchClientGroups,
  updateClientGroup,
  type ClientGroup,
} from '@/api/clientGroups'
import { fetchDebtors, sendDebtSmsToClient, sendDebtSmsToClients, type DebtorClient } from '@/api/debt'
import { formatApiError } from '@/api/http'
import {
  formatUzPhone,
  isCompleteUzPhone,
  matchesClientQuery,
  phonesEqual,
} from '@/utils/phone'

const tab = ref<'list' | 'groups' | 'debtors'>('list')
const clients = ref<Client[]>([])
const groups = ref<ClientGroup[]>([])
const debtors = ref<DebtorClient[]>([])
const selectedDebtors = ref<number[]>([])
const groupModal = ref(false)
const groupEditingId = ref<number | null>(null)
const groupName = ref('')
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
  clientGroupId: 0,
})

const form = reactive<ClientPayload>(emptyForm())
const phoneHint = ref<string | null>(null)

const filtered = computed(() => {
  const q = search.value.trim()
  if (!q) return clients.value
  return clients.value.filter((c) =>
    matchesClientQuery(q, {
      fullName: c.fullName,
      phone: c.phone,
      additionalPhone: c.additionalPhone,
      address: c.address,
      inn: c.inn,
    }),
  )
})

function onPhoneInput(field: 'phone' | 'additionalPhone', e: Event) {
  const el = e.target as HTMLInputElement
  const formatted = formatUzPhone(el.value)
  form[field] = formatted
  el.value = formatted
  if (field === 'phone') checkPhoneUnique()
}

function checkPhoneUnique() {
  phoneHint.value = null
  if (!isCompleteUzPhone(form.phone)) return
  const dup = clients.value.find(
    (c) => phonesEqual(c.phone, form.phone) && c.id !== editingId.value,
  )
  if (dup) {
    phoneHint.value = `Bu raqam band: ${dup.fullName}`
  }
}

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
    const [res, g] = await Promise.all([fetchClients(), fetchClientGroups()])
    clients.value = res.data || []
    groups.value = g.data || []
  } catch (e) {
    error.value = formatApiError(e, 'Mijozlarni yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm(), { phone: '+998-' })
  formError.value = null
  phoneHint.value = null
  modalOpen.value = true
}

function openEdit(client: Client) {
  editingId.value = client.id
  Object.assign(form, {
    fullName: client.fullName || '',
    phone: formatUzPhone(client.phone || ''),
    address: client.address || '',
    inn: client.inn || '',
    additionalPhone: client.additionalPhone ? formatUzPhone(client.additionalPhone) : '',
    bankName: client.bankName || '',
    mfo: client.mfo || '',
    accountNumber: client.accountNumber || '',
    description: client.description || '',
    clientGroupId: client.clientGroupId || 0,
  })
  formError.value = null
  phoneHint.value = null
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
  const extra = clean(form.additionalPhone)
  return {
    fullName: form.fullName.trim(),
    phone: formatUzPhone(form.phone),
    address: form.address.trim(),
    inn: clean(form.inn),
    additionalPhone: extra ? formatUzPhone(extra) : undefined,
    bankName: clean(form.bankName),
    mfo: clean(form.mfo),
    accountNumber: clean(form.accountNumber),
    description: clean(form.description),
    clientGroupId: form.clientGroupId || null,
  }
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    if (!isCompleteUzPhone(form.phone)) {
      formError.value = 'Telefon +998-(XX)-XXX-XX-XX formatida to‘liq bo‘lishi kerak'
      return
    }
    const dup = clients.value.find(
      (c) => phonesEqual(c.phone, form.phone) && c.id !== editingId.value,
    )
    if (dup) {
      formError.value = `Bu telefon raqam allaqachon mavjud (${dup.fullName})`
      return
    }
    if (form.additionalPhone && isCompleteUzPhone(form.additionalPhone)) {
      /* ok */
    } else if (form.additionalPhone && form.additionalPhone.replace(/\D/g, '').length > 3) {
      formError.value = 'Qo‘shimcha telefon to‘liq formatda bo‘lishi kerak'
      return
    }
    const payload = payloadFromForm()
    if (editingId.value) {
      await updateClient(editingId.value, payload)
    } else {
      await createClient(payload)
    }
    closeModal()
    await load()
  } catch (e) {
    formError.value = formatApiError(e, 'Saqlashda xatolik')
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
    error.value = formatApiError(e, 'O‘chirishda xatolik')
  } finally {
    deletingId.value = null
  }
}

function openGroupCreate() {
  groupEditingId.value = null
  groupName.value = ''
  groupModal.value = true
}

function openGroupEdit(g: ClientGroup) {
  groupEditingId.value = g.id
  groupName.value = g.name
  groupModal.value = true
}

async function onGroupSubmit() {
  try {
    if (groupEditingId.value) await updateClientGroup(groupEditingId.value, { name: groupName.value.trim() })
    else await createClientGroup({ name: groupName.value.trim() })
    groupModal.value = false
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onGroupDelete(g: ClientGroup) {
  if (!confirm(`“${g.name}” o‘chirilsinmi?`)) return
  try {
    await deleteClientGroup(g.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function loadDebtors() {
  try {
    const res = await fetchDebtors()
    debtors.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function sendOneSms(clientId: number) {
  try {
    await sendDebtSmsToClient(clientId)
    alert('SMS yuborildi')
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function sendSelectedSms() {
  if (!selectedDebtors.value.length) return
  try {
    await sendDebtSmsToClients(selectedDebtors.value)
    alert('SMS yuborildi')
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
</style>
