<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Yetkazib beruvchilar" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">Yetkazib beruvchilar</h3>
          <p class="sub">Yetkazib beruvchilarni boshqarish, filtrlash va sahifalash</p>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="icon-btn" title="Filter" @click="showFilters = !showFilters">
            <ListFilter class="h-4 w-4" />
          </button>
          <button type="button" class="icon-btn" title="Yangilash" :disabled="loading" @click="load">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
          <button type="button" class="btn-plus" title="Yangi" :disabled="writeBlocked" @click="openCreate">
            <Plus class="h-5 w-5" />
          </button>
        </div>
      </div>

      <div v-if="showFilters" class="filters">
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <label class="lbl">
            Nomi
            <input v-model="filters.name" type="search" class="field" placeholder="Nomi..." />
          </label>
          <label class="lbl">
            INN
            <input v-model="filters.inn" type="search" class="field" placeholder="INN..." />
          </label>
          <label class="lbl">
            Telefon
            <input v-model="filters.phone" type="search" class="field" placeholder="Telefon..." />
          </label>
          <label class="lbl">
            Holati
            <select v-model="filters.status" class="field">
              <option value="">Barchasi</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="DISABLED">DISABLED</option>
            </select>
          </label>
          <label class="lbl">
            Sahifada
            <select v-model.number="pageSize" class="field">
              <option :value="10">10 ta</option>
              <option :value="20">20 ta</option>
              <option :value="50">50 ta</option>
              <option :value="100">100 ta</option>
            </select>
          </label>
        </div>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <button type="button" class="ghost" @click="clearFilters">Filterni tozalash</button>
          <span class="text-sm text-gray-500">Jami: {{ filtered.length }} ta</span>
        </div>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="th">ID</th>
              <th class="th">Nomi</th>
              <th class="th">INN</th>
              <th class="th">Telefon</th>
              <th class="th">Qo‘shimcha tel.</th>
              <th class="th">Manzil</th>
              <th class="th">Holati</th>
              <th class="th">Yaratilgan sana</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="9" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="paged.length === 0"><td colspan="9" class="empty">Yetkazib beruvchi yo‘q</td></tr>
            <tr v-for="s in paged" :key="s.id" class="border-b border-gray-100">
              <td class="td">{{ s.id }}</td>
              <td class="td">
                <button type="button" class="link" @click="selectSupplier(s)">{{ s.name }}</button>
              </td>
              <td class="td">{{ s.inn || '—' }}</td>
              <td class="td">{{ s.phone ? formatUzPhone(s.phone) : '—' }}</td>
              <td class="td">{{ s.additionalPhone ? formatUzPhone(s.additionalPhone) : '—' }}</td>
              <td class="td">{{ s.address || '—' }}</td>
              <td class="td">
                <span class="badge" :class="s.status === 'ACTIVE' ? 'badge-ok' : 'badge-off'">
                  {{ s.status || '—' }}
                </span>
              </td>
              <td class="td">{{ formatDateTime(s.createdAt) }}</td>
              <td class="td text-right">
                <div class="inline-flex items-center justify-end gap-2">
                  <button
                    type="button"
                    class="status-link"
                    @click="toggleStatus(s)"
                  >
                    {{ s.status === 'ACTIVE' ? 'Nofaollashtirish' : 'Faollashtirish' }}
                  </button>
                  <RowActions @edit="openEdit(s)" @delete="onDelete(s)" />
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="selected" class="card mt-4 p-4 text-sm">
      <b>{{ selected.name }} — kirimlar:</b>
      <span v-if="inbound.length === 0"> yo‘q</span>
      <router-link
        v-for="o in inbound"
        :key="o.id"
        :to="'/warehouse-orders'"
        class="mr-2 text-brand-500"
      >
        #{{ o.id }}
      </router-link>
    </div>

    <div
      v-if="modalOpen"
      class="fixed inset-0 z-99999 flex items-center justify-center overflow-y-auto bg-black/40 p-4"
    >
      <div
        class="my-6 w-full max-w-lg rounded-2xl border border-gray-200 bg-white p-5 shadow-xl dark:border-gray-800 dark:bg-gray-900"
      >
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">
          {{ editingId ? 'Yetkazib beruvchini tahrirlash' : 'Yangi yetkazib beruvchi' }}
        </h3>

        <div
          v-if="formError"
          class="mb-3 rounded-lg border border-error-200 bg-error-50 px-3 py-2 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10 dark:text-error-400"
        >
          {{ formError }}
        </div>

        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="m-lbl">Nomi <span class="req">*</span></label>
            <div class="relative">
              <Building2 class="field-icon" />
              <input v-model="form.name" required class="m-field" placeholder="Tashkilot nomi" />
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="m-lbl">Telefon <span class="req">*</span></label>
              <div class="relative">
                <Phone class="field-icon" />
                <input
                  :value="form.phone"
                  required
                  inputmode="tel"
                  placeholder="+998-(97)-221-88-96"
                  maxlength="19"
                  class="m-field"
                  @input="onPhoneInput('phone', $event)"
                />
              </div>
            </div>
            <div>
              <label class="m-lbl">Qo‘shimcha tel</label>
              <div class="relative">
                <Phone class="field-icon" />
                <input
                  :value="form.additionalPhone"
                  inputmode="tel"
                  placeholder="+998-(97)-221-88-96"
                  maxlength="19"
                  class="m-field"
                  @input="onPhoneInput('additionalPhone', $event)"
                />
              </div>
            </div>
          </div>
          <div>
            <label class="m-lbl">Manzil</label>
            <div class="relative">
              <MapPin class="field-icon" />
              <input v-model="form.address" class="m-field" placeholder="Ko‘cha, tuman, shahar" />
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="m-lbl">INN</label>
              <div class="relative">
                <Hash class="field-icon" />
                <input
                  :value="form.inn"
                  inputmode="numeric"
                  maxlength="9"
                  placeholder="123456789"
                  class="m-field"
                  @input="onDigitsInput('inn', 9, $event)"
                />
              </div>
              <p class="mt-1 text-xs text-gray-400">9 ta raqam (ixtiyoriy)</p>
            </div>
            <div>
              <label class="m-lbl">Bank</label>
              <div class="relative">
                <Landmark class="field-icon" />
                <input v-model="form.bankName" maxlength="150" class="m-field" placeholder="Bank nomi" />
              </div>
            </div>
          </div>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div>
              <label class="m-lbl">MFO</label>
              <div class="relative">
                <Landmark class="field-icon" />
                <input
                  :value="form.mfo"
                  inputmode="numeric"
                  maxlength="5"
                  placeholder="00000"
                  class="m-field"
                  @input="onDigitsInput('mfo', 5, $event)"
                />
              </div>
              <p class="mt-1 text-xs text-gray-400">5 ta raqam (ixtiyoriy)</p>
            </div>
            <div>
              <label class="m-lbl">Hisob raqam</label>
              <div class="relative">
                <CreditCard class="field-icon" />
                <input v-model="form.accountNumber" maxlength="30" class="m-field" placeholder="20208000..." />
              </div>
            </div>
          </div>
          <div>
            <label class="m-lbl">Izoh</label>
            <div class="relative">
              <AlignLeft class="field-icon field-icon-top" />
              <textarea
                v-model="form.description"
                rows="2"
                class="m-field m-textarea"
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
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  AlignLeft,
  Building2,
  CreditCard,
  Hash,
  Landmark,
  ListFilter,
  MapPin,
  Phone,
  Plus,
  RefreshCw,
} from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  changeSupplierStatus,
  createSupplier,
  deleteSupplier,
  fetchSuppliers,
  updateSupplier,
  type Supplier,
} from '@/api/suppliers'
import { fetchWarehouseOrdersBySupplier, type WarehouseOrder } from '@/api/warehouseOrders'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import {
  formatUzPhone,
  isCompleteUzPhone,
  phoneDigits,
  phonesEqual,
} from '@/utils/phone'

const { writeBlocked } = useFilialScope()
const route = useRoute()
const items = ref<Supplier[]>([])
const inbound = ref<WarehouseOrder[]>([])
const selected = ref<Supplier | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const showFilters = ref(true)
const pageSize = ref(20)
const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const filters = reactive({ name: '', inn: '', phone: '', status: '' })
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

function formatDateTime(v?: string | null) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v.replace('T', ' ').slice(0, 19)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const filtered = computed(() => {
  const name = filters.name.trim().toLowerCase()
  const inn = filters.inn.trim().toLowerCase()
  const phoneQ = filters.phone.trim()
  const phoneDigitsQ = phoneDigits(phoneQ)
  return [...items.value]
    .filter((s) => {
      if (name && !(s.name || '').toLowerCase().includes(name)) return false
      if (inn && !(s.inn || '').toLowerCase().includes(inn)) return false
      if (phoneQ) {
        if (phoneDigitsQ.length >= 2) {
          const hit =
            phoneDigits(s.phone).includes(phoneDigitsQ) ||
            phoneDigits(s.additionalPhone).includes(phoneDigitsQ)
          if (!hit) return false
        } else {
          const q = phoneQ.toLowerCase()
          if (
            !(s.phone || '').toLowerCase().includes(q) &&
            !(s.additionalPhone || '').toLowerCase().includes(q)
          ) {
            return false
          }
        }
      }
      if (filters.status && s.status !== filters.status) return false
      return true
    })
    .sort((a, b) => Number(a.id) - Number(b.id))
})

const paged = computed(() => filtered.value.slice(0, pageSize.value))

function clearFilters() {
  filters.name = ''
  filters.inn = ''
  filters.phone = ''
  filters.status = ''
}

function onPhoneInput(field: 'phone' | 'additionalPhone', e: Event) {
  const el = e.target as HTMLInputElement
  form[field] = formatUzPhone(el.value)
  el.value = form[field]
}

function onDigitsInput(field: 'inn' | 'mfo', max: number, e: Event) {
  const el = e.target as HTMLInputElement
  const digits = el.value.replace(/\D/g, '').slice(0, max)
  form[field] = digits
  el.value = digits
}

function closeModal() {
  if (saving.value) return
  modalOpen.value = false
}

function payload() {
  const additional = form.additionalPhone.trim()
  return {
    name: form.name.trim(),
    phone: formatUzPhone(form.phone),
    additionalPhone:
      additional && isCompleteUzPhone(additional) ? formatUzPhone(additional) : undefined,
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
    const s = await fetchSuppliers(0, 500)
    items.value = s.data?.content || []
    const focusId = Number(route.query.id || 0)
    if (focusId) {
      const found = items.value.find((x) => x.id === focusId)
      if (found) await selectSupplier(found)
    }
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    name: '',
    phone: '+998-',
    additionalPhone: '',
    inn: '',
    address: '',
    bankName: '',
    mfo: '',
    accountNumber: '',
    description: '',
  })
  formError.value = null
  modalOpen.value = true
}

function openEdit(s: Supplier) {
  editingId.value = s.id
  Object.assign(form, {
    name: s.name,
    phone: formatUzPhone(s.phone || ''),
    additionalPhone: s.additionalPhone ? formatUzPhone(s.additionalPhone) : '',
    inn: (s.inn || '').replace(/\D/g, '').slice(0, 9),
    address: s.address || '',
    bankName: s.bankName || '',
    mfo: (s.mfo || '').replace(/\D/g, '').slice(0, 5),
    accountNumber: s.accountNumber || '',
    description: s.description || '',
  })
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  formError.value = null
  if (!form.name.trim()) {
    formError.value = 'Nomi majburiy'
    return
  }
  if (!isCompleteUzPhone(form.phone)) {
    formError.value = 'Telefon +998-(97)-221-88-96 ko‘rinishida to‘liq kiriting'
    return
  }
  if (form.additionalPhone.trim() && !isCompleteUzPhone(form.additionalPhone)) {
    formError.value = 'Qo‘shimcha telefon to‘liq formatda bo‘lishi kerak'
    return
  }
  const inn = form.inn.trim()
  if (inn && inn.length !== 9) {
    formError.value = 'INN 9 ta belgidan iborat bo‘lishi kerak'
    return
  }
  const mfo = form.mfo.trim()
  if (mfo && mfo.length !== 5) {
    formError.value = 'MFO 5 ta belgidan iborat bo‘lishi kerak'
    return
  }
  const dup = items.value.find(
    (s) => phonesEqual(s.phone, form.phone) && s.id !== editingId.value,
  )
  if (dup) {
    formError.value = `Bu raqam band: ${dup.name}`
    return
  }
  saving.value = true
  try {
    if (editingId.value) await updateSupplier(editingId.value, payload())
    else await createSupplier(payload())
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(s: Supplier) {
  if (!confirm(`“${s.name}” o‘chirilsinmi?`)) return
  try {
    await deleteSupplier(s.id)
    if (selected.value?.id === s.id) {
      selected.value = null
      inbound.value = []
    }
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function toggleStatus(s: Supplier) {
  const next = s.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  try {
    await changeSupplierStatus(s.id, next)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function selectSupplier(s: Supplier) {
  selected.value = s
  try {
    const res = await fetchWarehouseOrdersBySupplier(s.id)
    inbound.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

watch(() => route.query.id, () => {
  const focusId = Number(route.query.id || 0)
  if (!focusId) return
  const found = items.value.find((x) => x.id === focusId)
  if (found) void selectSupplier(found)
})

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 1.25rem 1.25rem 1rem; border-bottom: 1px solid #f3f4f6; }
.filters { padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; background: #fff; }
.m-lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
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
.field-icon-top { top: 0.9rem; transform: none; }
.m-field {
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
.m-field:focus { border-color: #9cb0ff; box-shadow: 0 0 0 4px rgb(70 95 255 / 10%); }
.m-textarea { height: auto; min-height: 4.5rem; padding-top: 0.75rem; padding-bottom: 0.75rem; resize: vertical; }
.btn { height: 2.75rem; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; color: #fff; font-size: 0.875rem; font-weight: 500; }
.btn:disabled { opacity: 0.6; }
.btn-plus { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; color: #fff; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #4b5563; background: #fff; }
.ghost { height: 2.75rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1.25rem; font-size: 0.875rem; color: #374151; }
.link { color: #465fff; font-weight: 500; }
.status-link { font-size: 0.8125rem; font-weight: 500; color: #f97316; white-space: nowrap; }
.badge { display: inline-flex; border-radius: 9999px; padding: 0.15rem 0.55rem; font-size: 0.75rem; font-weight: 600; }
.badge-ok { background: #ecfdf5; color: #059669; }
.badge-off { background: #f3f4f6; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.dark .m-lbl { color: #9ca3af; }
.dark .m-field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .m-field::placeholder { color: rgba(255, 255, 255, 0.3); }
.dark .ghost { border-color: #344054; color: #d1d5db; }
</style>
