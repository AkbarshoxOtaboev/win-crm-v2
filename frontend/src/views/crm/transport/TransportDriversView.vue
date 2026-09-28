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
              <td class="td">{{ d.phone || '—' }}</td>
              <td class="td">{{ d.carModel || '—' }}</td>
              <td class="td whitespace-nowrap">{{ d.carNumber || '—' }}</td>
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
      <div class="modal">
        <h3 class="title mb-4">{{ editingId ? t('transport.drivers.editTitle') : t('transport.drivers.createTitle') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSubmit">
          <label class="lbl">{{ t('common.fullName') }} *<input v-model="form.fullName" required class="field" /></label>
          <label class="lbl">{{ t('common.phone') }}<input v-model="form.phone" class="field" placeholder="+998" /></label>
          <div class="grid grid-cols-2 gap-3">
            <label class="lbl">{{ t('transport.drivers.car') }}<input v-model="form.carModel" class="field" placeholder="Damas" /></label>
            <label class="lbl">{{ t('transport.drivers.carNumber') }}<input v-model="form.carNumber" class="field" placeholder="01 A 123 BC" /></label>
          </div>
          <label class="lbl">{{ t('common.note') }}<input v-model="form.note" class="field" /></label>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modalOpen = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('common.save') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
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

function isActive(status?: string) {
  return !status || status === 'ACTIVE'
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
  formError.value = null
  modalOpen.value = true
}

function openEdit(d: TransportDriver) {
  editingId.value = d.id
  Object.assign(form, {
    fullName: d.fullName,
    phone: d.phone || '',
    carModel: d.carModel || '',
    carNumber: d.carNumber || '',
    note: d.note || '',
  })
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  const payload = {
    fullName: form.fullName.trim(),
    phone: form.phone.trim() || undefined,
    carModel: form.carModel.trim() || undefined,
    carNumber: form.carNumber.trim() || undefined,
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
.lbl { display: flex; flex-direction: column; gap: 0.3rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.status-toggle { position: relative; display: inline-flex; height: 1.5rem; width: 2.75rem; align-items: center; border-radius: 9999px; transition: background 0.15s ease; border: none; cursor: pointer; padding: 0; }
.status-toggle.on { background: #12b76a; }
.status-toggle.off { background: #d1d5db; }
.status-knob { position: absolute; height: 1.25rem; width: 1.25rem; border-radius: 9999px; background: #fff; transition: transform 0.15s ease; transform: translateX(0.125rem); }
.status-toggle.on .status-knob { transform: translateX(1.375rem); }
</style>
