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
          <select
            v-model.number="groupFilter"
            class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90 sm:w-48"
          >
            <option :value="0">Barcha guruhlar</option>
            <option :value="-1">Guruhsiz</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <input
            v-model="search"
            type="search"
            placeholder="Qidiruv: ism, telefon, manzil..."
            class="h-10 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90 sm:w-64"
          />
          <button
            type="button"
            class="inline-flex h-10 items-center justify-center rounded-lg bg-brand-500 px-4 text-sm font-medium text-white hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-55"
            :disabled="writeBlocked"
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
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Guruh</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Telefon</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Manzil</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">INN</th>
              <th class="px-5 py-3 text-left text-xs font-medium text-gray-500">Status</th>
              <th class="px-5 py-3 text-right text-xs font-medium text-gray-500">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="px-5 py-10 text-center text-sm text-gray-500">
                Yuklanmoqda...
              </td>
            </tr>
            <tr v-else-if="filtered.length === 0">
              <td colspan="8" class="px-5 py-10 text-center text-sm text-gray-500">
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
                {{ client.clientGroupName || '—' }}
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
            <label class="lbl">Guruh</label>
            <div class="relative">
              <Users class="field-icon" />
              <select v-model.number="form.clientGroupId" class="field field-select">
                <option :value="0">— guruh yo‘q —</option>
                <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
              </select>
              <ChevronDown class="pointer-events-none absolute top-1/2 right-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
            </div>
          </div>
          <div>
            <label class="lbl">F.I.Sh <span class="req">*</span></label>
            <div class="relative">
              <User class="field-icon" />
              <input v-model="form.fullName" required class="field" placeholder="Ism familiya" />
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="lbl">Telefon <span class="req">*</span></label>
              <div class="relative">
                <Phone class="field-icon" />
                <input
                  :value="form.phone"
                  required
                  inputmode="tel"
                  placeholder="+998-(97)-221-88-96"
                  maxlength="19"
                  class="field"
                  @input="onPhoneInput('phone', $event)"
                />
              </div>
              <p v-if="phoneHint" class="mt-1 text-xs text-error-500">{{ phoneHint }}</p>
            </div>
            <div>
              <label class="lbl">Qo‘shimcha tel</label>
              <div class="relative">
                <Phone class="field-icon" />
                <input
                  :value="form.additionalPhone"
                  inputmode="tel"
                  placeholder="+998-(97)-221-88-96"
                  maxlength="19"
                  class="field"
                  @input="onPhoneInput('additionalPhone', $event)"
                />
              </div>
            </div>
          </div>
          <div>
            <label class="lbl">Manzil <span class="req">*</span></label>
            <div class="relative">
              <MapPin class="field-icon" />
              <input v-model="form.address" required class="field" placeholder="Ko‘cha, tuman, shahar" />
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="lbl">INN</label>
              <div class="relative">
                <Hash class="field-icon" />
                <input
                  :value="form.inn"
                  inputmode="numeric"
                  pattern="[0-9]{9}"
                  minlength="9"
                  maxlength="9"
                  placeholder="123456789"
                  class="field"
                  @input="onDigitsInput('inn', 9, $event)"
                />
              </div>
              <p class="mt-1 text-xs text-gray-400">9 ta raqam (ixtiyoriy)</p>
            </div>
            <div>
              <label class="lbl">Bank</label>
              <div class="relative">
                <Landmark class="field-icon" />
                <input
                  v-model="form.bankName"
                  maxlength="150"
                  class="field"
                  placeholder="Bank nomi"
                />
              </div>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="lbl">MFO</label>
              <div class="relative">
                <Landmark class="field-icon" />
                <input
                  :value="form.mfo"
                  inputmode="numeric"
                  pattern="[0-9]{5}"
                  minlength="5"
                  maxlength="5"
                  placeholder="00000"
                  class="field"
                  @input="onDigitsInput('mfo', 5, $event)"
                />
              </div>
              <p class="mt-1 text-xs text-gray-400">5 ta raqam (ixtiyoriy)</p>
            </div>
            <div>
              <label class="lbl">Hisob raqam</label>
              <div class="relative">
                <CreditCard class="field-icon" />
                <input
                  v-model="form.accountNumber"
                  maxlength="30"
                  class="field"
                  placeholder="20208000..."
                />
              </div>
            </div>
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <div class="relative">
              <AlignLeft class="field-icon field-icon-top" />
              <textarea
                v-model="form.description"
                rows="2"
                class="field field-textarea"
                placeholder="Qo‘shimcha ma’lumot"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-2">
            <button type="button" class="ghost" @click="closeModal">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">
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
        <button type="button" class="h-10 rounded-lg bg-brand-500 px-4 text-sm text-white disabled:cursor-not-allowed disabled:opacity-55" :disabled="writeBlocked" @click="openGroupCreate">+ Guruh</button>
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

    <div v-if="groupModal" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
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
import {
  AlignLeft,
  ChevronDown,
  CreditCard,
  Hash,
  Landmark,
  MapPin,
  Phone,
  User,
  Users,
} from 'lucide-vue-next'
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
import { useFilialScope } from '@/composables/useFilialScope'
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
const groupFilter = ref(0)
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
  let list = clients.value
  if (groupFilter.value === -1) {
    list = list.filter((c) => !c.clientGroupId)
  } else if (groupFilter.value > 0) {
    list = list.filter((c) => c.clientGroupId === groupFilter.value)
  }
  const q = search.value.trim()
  if (!q) return list
  return list.filter((c) =>
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

function onDigitsInput(field: 'inn' | 'mfo', maxLen: number, e: Event) {
  const el = e.target as HTMLInputElement
  const digits = el.value.replace(/\D/g, '').slice(0, maxLen)
  form[field] = digits
  el.value = digits
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

const { writeBlocked } = useFilialScope()

function openCreate() {
  if (writeBlocked.value) return
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
    inn: (client.inn || '').replace(/\D/g, '').slice(0, 9),
    additionalPhone: client.additionalPhone ? formatUzPhone(client.additionalPhone) : '',
    bankName: client.bankName || '',
    mfo: (client.mfo || '').replace(/\D/g, '').slice(0, 5),
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
    clientGroupId:
      form.clientGroupId != null && Number(form.clientGroupId) > 0
        ? Number(form.clientGroupId)
        : null,
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
    const inn = (form.inn || '').trim()
    if (inn && inn.length !== 9) {
      formError.value = 'INN 9 ta belgidan iborat bo‘lishi kerak'
      return
    }
    const mfo = (form.mfo || '').trim()
    if (mfo && mfo.length !== 5) {
      formError.value = 'MFO 5 ta belgidan iborat bo‘lishi kerak'
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
  if (writeBlocked.value) return
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
.lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.field-icon {
  position: absolute;
  top: 50%;
  left: 0.75rem;
  z-index: 10;
  height: 1.1rem;
  width: 1.1rem;
  transform: translateY(-50%);
  color: #98a2b3;
  pointer-events: none;
}
.field-icon-top {
  top: 0.9rem;
  transform: none;
}
.field {
  height: 2.75rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem 0 2.5rem;
  font-size: 0.875rem;
  color: #1f2937;
  outline: none;
}
.field:focus {
  border-color: #9cb0ff;
  box-shadow: 0 0 0 4px rgb(70 95 255 / 10%);
}
.field-select {
  appearance: none;
  padding-right: 2.5rem;
}
.field-textarea {
  height: auto;
  min-height: 4.5rem;
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
  resize: vertical;
}
.btn {
  height: 2.75rem;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1.25rem;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
}
.btn:disabled { opacity: 0.6; }
.ghost {
  height: 2.75rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1.25rem;
  font-size: 0.875rem;
  color: #374151;
}
.dark .lbl { color: #9ca3af; }
.dark .field {
  border-color: #344054;
  color: rgba(255, 255, 255, 0.9);
}
.dark .field::placeholder { color: rgba(255, 255, 255, 0.3); }
.dark .ghost { border-color: #344054; color: #d1d5db; }
</style>
