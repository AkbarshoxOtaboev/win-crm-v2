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
          <button type="button" class="btn-plus" title="Yangi" @click="openCreate">
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

    <div v-if="modalOpen" class="overlay" @click.self="modalOpen = false">
      <div class="modal">
        <h3 class="title mb-3">{{ editingId ? 'Tahrirlash' : 'Yangi yetkazib beruvchi' }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <div>
            <label class="form-lbl">Nomi *</label>
            <input v-model="form.name" required class="field" placeholder="Nomi" />
          </div>
          <div>
            <label class="form-lbl">Telefon *</label>
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
          <div>
            <label class="form-lbl">Qo‘shimcha tel</label>
            <input
              :value="form.additionalPhone"
              inputmode="tel"
              placeholder="+998-(97)-221-88-96"
              maxlength="19"
              class="field"
              @input="onPhoneInput('additionalPhone', $event)"
            />
          </div>
          <input v-model="form.inn" class="field" placeholder="INN" />
          <input v-model="form.address" class="field" placeholder="Manzil" />
          <input v-model="form.bankName" class="field" placeholder="Bank" />
          <input v-model="form.mfo" class="field" placeholder="MFO" />
          <input v-model="form.accountNumber" class="field" placeholder="Hisob" />
          <input v-model="form.description" class="field" placeholder="Izoh" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modalOpen = false">Bekor</button>
            <button type="submit" class="btn">Saqlash</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ListFilter, Plus, RefreshCw } from 'lucide-vue-next'
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
import {
  formatUzPhone,
  isCompleteUzPhone,
  phoneDigits,
  phonesEqual,
} from '@/utils/phone'

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
    inn: s.inn || '',
    address: s.address || '',
    bankName: s.bankName || '',
    mfo: s.mfo || '',
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
  const dup = items.value.find(
    (s) => phonesEqual(s.phone, form.phone) && s.id !== editingId.value,
  )
  if (dup) {
    formError.value = `Bu raqam band: ${dup.name}`
    return
  }
  try {
    if (editingId.value) await updateSupplier(editingId.value, payload())
    else await createSupplier(payload())
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
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
.form-lbl { display: block; margin-bottom: 0.35rem; font-size: 0.8125rem; font-weight: 500; color: #6b7280; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; font-size: 0.875rem; font-weight: 500; }
.btn-plus { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; color: #fff; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #4b5563; background: #fff; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; }
.link { color: #465fff; font-weight: 500; }
.status-link { font-size: 0.8125rem; font-weight: 500; color: #f97316; white-space: nowrap; }
.badge { display: inline-flex; border-radius: 9999px; padding: 0.15rem 0.55rem; font-size: 0.75rem; font-weight: 600; }
.badge-ok { background: #ecfdf5; color: #059669; }
.badge-off { background: #f3f4f6; color: #6b7280; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
