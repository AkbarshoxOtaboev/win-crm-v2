<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.transportDrivers')" />
    <div class="card">
      <div class="head flex-col gap-3 sm:flex-row sm:items-center">
        <div>
          <h3 class="title">{{ t('nav.transportDrivers') }}</h3>
          <p class="sub">{{ t('transport.drivers.subtitle') }}</p>
        </div>
        <div class="flex gap-2">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field sm:w-56" />
          <button type="button" class="btn whitespace-nowrap" :disabled="writeBlocked" @click="openCreate">{{ t('common.new') }}</button>
        </div>
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.fullName') }}</th>
              <th class="th">{{ t('common.phone') }}</th>
              <th class="th">{{ t('transport.drivers.car') }}</th>
              <th class="th">{{ t('transport.drivers.carNumber') }}</th>
              <th class="th">{{ t('common.status') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">{{ t('transport.drivers.empty') }}</td></tr>
            <tr v-for="d in filtered" :key="d.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ d.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">
                {{ d.fullName }}
                <div v-if="d.note" class="text-xs text-gray-500">{{ d.note }}</div>
              </td>
              <td class="td whitespace-nowrap">{{ d.phone ? formatUzPhone(d.phone) : '—' }}</td>
              <td class="td">{{ d.carModel || '—' }}</td>
              <td class="td whitespace-nowrap">
                <span v-if="d.carNumber" class="plate-badge">{{ formatCarNumber(d.carNumber) }}</span>
                <span v-else>—</span>
              </td>
              <td class="td">
                <button
                  type="button"
                  class="status-toggle"
                  :class="isActive(d.status) ? 'on' : 'off'"
                  :title="isActive(d.status) ? t('common.active') : t('common.inactive')"
                  @click="onStatus(d)"
                >
                  <span class="status-knob" />
                </button>
              </td>
              <td class="td text-right">
                <RowActions @edit="openEdit(d)" @delete="onDelete(d)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modalOpen" class="overlay">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="driver-modal-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge"><Truck class="h-5 w-5" /></span>
            <div>
              <h3 id="driver-modal-title" class="title">
                {{ editingId ? t('transport.drivers.editTitle') : t('transport.drivers.createTitle') }}
              </h3>
              <p class="sub">{{ t('transport.drivers.modalSubtitle') }}</p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="modalOpen = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label for="driver-name" class="lbl">{{ t('common.fullName') }} <span class="req">*</span></label>
            <div class="relative">
              <User class="field-icon" />
              <input
                id="driver-name"
                v-model="form.fullName"
                required
                class="field field-with-icon"
                :placeholder="t('clients.fullNamePlaceholder')"
              />
            </div>
          </div>
          <div>
            <label for="driver-phone" class="lbl">{{ t('common.phone') }}</label>
            <div class="relative">
              <Phone class="field-icon" />
              <input
                id="driver-phone"
                :value="form.phone"
                inputmode="tel"
                placeholder="+998-(97)-123-45-67"
                maxlength="19"
                class="field field-with-icon"
                @focus="onPhoneFocus"
                @input="onPhoneInput"
              />
            </div>
          </div>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div>
              <div class="field-head">
                <label for="driver-car" class="lbl">{{ t('transport.drivers.car') }}</label>
              </div>
              <div class="relative">
                <Car class="field-icon" />
                <input
                  id="driver-car"
                  v-model="form.carModel"
                  class="field field-with-icon"
                  :placeholder="t('transport.drivers.carModelPlaceholder')"
                />
              </div>
            </div>
            <div>
              <div class="field-head">
                <label for="driver-plate" class="lbl">{{ t('transport.drivers.carNumber') }}</label>
                <div class="plate-kind" role="radiogroup" :aria-label="t('transport.drivers.plateKindLabel')">
                  <button
                    v-for="k in PLATE_KINDS"
                    :key="k"
                    type="button"
                    role="radio"
                    :aria-checked="plateKind === k"
                    class="plate-kind-btn"
                    :class="{ active: plateKind === k }"
                    @click="setPlateKind(k)"
                  >
                    {{ t(`transport.drivers.plateKind.${k}`) }}
                  </button>
                </div>
              </div>
              <div class="relative">
                <RectangleHorizontal class="field-icon" />
                <input
                  id="driver-plate"
                  :value="form.carNumber"
                  autocapitalize="characters"
                  autocomplete="off"
                  :placeholder="carNumberExample(plateKind)"
                  maxlength="11"
                  class="field field-with-icon plate"
                  @input="onCarNumberInput"
                />
              </div>
            </div>
          </div>
          <div>
            <label for="driver-note" class="lbl">{{ t('common.note') }}</label>
            <div class="relative">
              <MessageSquare class="field-icon" />
              <input id="driver-note" v-model="form.note" class="field field-with-icon" />
            </div>
          </div>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="modalOpen = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn btn-with-icon" :disabled="saving">
              <Check class="h-4 w-4" />
              {{ saving ? t('common.saving') : t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Car, Check, MessageSquare, Phone, RectangleHorizontal, Truck, User, X } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  changeDriverStatus,
  createDriver,
  deleteDriver,
  fetchDrivers,
  updateDriver,
  type TransportDriver,
} from '@/api/transport'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatUzPhone, isCompleteUzPhone, phoneDigits } from '@/utils/phone'
import {
  carNumberExample,
  detectCarNumberKind,
  formatCarNumber,
  isCompleteCarNumber,
  type CarNumberKind,
} from '@/utils/carNumber'

const PLATE_KINDS: CarNumberKind[] = ['PERSONAL', 'COMPANY']

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const items = ref<TransportDriver[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ fullName: '', phone: '', carModel: '', carNumber: '', note: '' })
const plateKind = ref<CarNumberKind>('PERSONAL')

function setPlateKind(kind: CarNumberKind) {
  if (plateKind.value === kind) return
  plateKind.value = kind
  form.carNumber = formatCarNumber(form.carNumber.replace(/\s/g, '').slice(0, 2), kind)
}

function isActive(status?: string) {
  return !status || status === 'ACTIVE'
}

function onPhoneFocus() {
  if (!form.phone) form.phone = '+998-'
}

function onPhoneInput(e: Event) {
  const el = e.target as HTMLInputElement
  form.phone = formatUzPhone(el.value)
  el.value = form.phone
}

function onCarNumberInput(e: Event) {
  const el = e.target as HTMLInputElement
  form.carNumber = formatCarNumber(el.value, plateKind.value)
  el.value = form.carNumber
}

/** "+998-" bo'lsa ham bo'sh deb hisoblanadi */
function hasPhone(value: string) {
  return phoneDigits(value).length > 3
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((d) =>
    [d.fullName, d.phone, d.carModel, d.carNumber].some((v) => (v || '').toLowerCase().includes(q)),
  )
})

async function load() {
  loading.value = true
  error.value = null
  try {
    items.value = (await fetchDrivers()).data || []
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { fullName: '', phone: '', carModel: '', carNumber: '', note: '' })
  plateKind.value = 'PERSONAL'
  formError.value = null
  modalOpen.value = true
}

function openEdit(d: TransportDriver) {
  editingId.value = d.id
  plateKind.value = detectCarNumberKind(d.carNumber)
  Object.assign(form, {
    fullName: d.fullName,
    phone: d.phone ? formatUzPhone(d.phone) : '',
    carModel: d.carModel || '',
    carNumber: formatCarNumber(d.carNumber),
    note: d.note || '',
  })
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  formError.value = null
  const phone = hasPhone(form.phone) ? form.phone : ''
  if (phone && !isCompleteUzPhone(phone)) {
    formError.value = t('clients.phoneFormat')
    return
  }
  if (form.carNumber && !isCompleteCarNumber(form.carNumber, plateKind.value)) {
    formError.value = t('transport.drivers.carNumberFormat')
    return
  }
  saving.value = true
  const payload = {
    fullName: form.fullName.trim(),
    phone: phone || undefined,
    carModel: form.carModel.trim() || undefined,
    carNumber: form.carNumber || undefined,
    note: form.note.trim() || undefined,
  }
  try {
    if (editingId.value) await updateDriver(editingId.value, payload)
    else await createDriver(payload)
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e, t('common.saveError'))
  } finally {
    saving.value = false
  }
}

async function onDelete(d: TransportDriver) {
  if (!confirm(t('common.deleteConfirmNamed', { name: d.fullName }))) return
  try {
    await deleteDriver(d.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, t('common.deleteError'))
  }
}

async function onStatus(d: TransportDriver) {
  try {
    await changeDriverStatus(d.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e, t('common.statusError'))
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.head { display: flex; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.125rem; font-size: 0.875rem; color: #6b7280; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.field:focus { outline: none; border-color: #9cb0ff; box-shadow: 0 0 0 4px rgb(70 95 255 / 10%); }
.field-with-icon { height: 2.75rem; padding-left: 2.5rem; }
.field-icon { position: absolute; top: 50%; left: 0.75rem; z-index: 1; height: 1.1rem; width: 1.1rem; transform: translateY(-50%); color: #98a2b3; pointer-events: none; }
.plate { font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; }
.field-head { display: flex; height: 1.75rem; align-items: center; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.375rem; }
.field-head .lbl { margin-bottom: 0; white-space: nowrap; }
.plate-kind { display: inline-flex; flex-shrink: 0; gap: 0.125rem; border-radius: 0.5rem; background: #f2f4f7; padding: 0.125rem; }
.plate-kind-btn { border-radius: 0.375rem; padding: 0.125rem 0.5rem; font-size: 0.75rem; font-weight: 500; line-height: 1.25rem; color: #667085; white-space: nowrap; transition: background-color 0.15s, color 0.15s; }
.plate-kind-btn.active { background: #fff; color: #465fff; box-shadow: 0 1px 2px rgba(16, 24, 40, 0.08); }
.dark .plate-kind { background: rgb(255 255 255 / 5%); }
.dark .plate-kind-btn { color: #98a2b3; }
.dark .plate-kind-btn.active { background: #1d2939; color: #9cb0ff; }
.plate-badge { display: inline-flex; align-items: center; border-radius: 0.375rem; border: 1.5px solid #1f2937; padding: 0.1rem 0.5rem; font-size: 0.8125rem; font-weight: 700; letter-spacing: 0.06em; color: #1f2937; background: #fff; }
.modal-badge { display: flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.close-btn { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #9ca3af; }
.close-btn:hover { background: #f3f4f6; color: #374151; }
.btn-with-icon { gap: 0.4rem; }
.dark .lbl { color: #9ca3af; }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .modal-badge { background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .close-btn:hover { background: rgb(255 255 255 / 5%); color: rgba(255, 255, 255, 0.8); }
.dark .plate-badge { border-color: #d1d5db; background: transparent; color: #f3f4f6; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 32rem; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.status-toggle { position: relative; display: inline-flex; height: 1.5rem; width: 2.75rem; align-items: center; border-radius: 9999px; transition: background 0.15s ease; border: none; cursor: pointer; padding: 0; }
.status-toggle.on { background: #12b76a; }
.status-toggle.off { background: #d1d5db; }
.status-knob { position: absolute; height: 1.25rem; width: 1.25rem; border-radius: 9999px; background: #fff; transition: transform 0.15s ease; transform: translateX(0.125rem); }
.status-toggle.on .status-knob { transform: translateX(1.375rem); }
</style>
