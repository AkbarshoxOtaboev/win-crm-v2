<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.transportWorkers')" />

    <div class="mb-4 flex flex-wrap gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'workers' }" @click="tab = 'workers'">{{ t('transport.fields.workers') }}</button>
      <button type="button" class="tab" :class="{ active: tab === 'salary' }" @click="tab = 'salary'">
        {{ t('transport.salary.tab') }}
        <span v-if="pendingCount" class="tab-count">{{ pendingCount }}</span>
      </button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <!-- Ishchilar -->
    <div v-show="tab === 'workers'" class="card">
      <div class="head flex-col gap-3 sm:flex-row sm:items-center">
        <div>
          <h3 class="title">{{ t('transport.fields.workers') }}</h3>
          <p class="sub">{{ t('transport.workers.subtitle') }}</p>
        </div>
        <div class="flex gap-2">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field sm:w-56" />
          <button type="button" class="btn whitespace-nowrap" :disabled="writeBlocked" @click="openCreate">{{ t('common.new') }}</button>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('common.fullName') }}</th>
              <th class="th">{{ t('common.phone') }}</th>
              <th class="th">{{ t('common.status') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filteredWorkers.length === 0"><td colspan="5" class="empty">{{ t('transport.workers.empty') }}</td></tr>
            <tr v-for="w in filteredWorkers" :key="w.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ w.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">
                {{ w.fullName }}
                <div v-if="w.note" class="text-xs text-gray-500">{{ w.note }}</div>
              </td>
              <td class="td whitespace-nowrap">{{ w.phone ? formatUzPhone(w.phone) : '—' }}</td>
              <td class="td">
                <button
                  type="button"
                  class="status-toggle"
                  :class="isActive(w.status) ? 'on' : 'off'"
                  :title="isActive(w.status) ? t('common.active') : t('common.inactive')"
                  @click="onStatus(w)"
                >
                  <span class="status-knob" />
                </button>
              </td>
              <td class="td text-right"><RowActions @edit="openEdit(w)" @delete="onDelete(w)" /></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Oylik -->
    <div v-show="tab === 'salary'" class="space-y-4">
      <div class="card p-5">
        <div class="flex flex-wrap items-end justify-between gap-4">
          <div class="flex flex-wrap items-end gap-3">
            <label class="lbl">
              {{ t('common.month') }}
              <input v-model="period" type="month" class="field w-44" @change="loadSalary" />
            </label>
            <label class="lbl">
              {{ t('common.status') }}
              <select v-model="statusFilter" class="field w-48">
                <option value="">{{ t('transport.fields.all') }}</option>
                <option value="PENDING">{{ t('workerSalaryStatus.PENDING') }}</option>
                <option value="APPROVED">{{ t('workerSalaryStatus.APPROVED') }}</option>
                <option value="REJECTED">{{ t('workerSalaryStatus.REJECTED') }}</option>
              </select>
            </label>
          </div>
          <div class="percent-box">
            <span class="k">{{ t('transport.salary.percentLabel') }}</span>
            <div v-if="auth.canApproveTransportSalary" class="flex items-center gap-2">
              <input v-model.number="percentDraft" type="number" min="0" max="100" step="0.01" class="field w-24" />
              <span class="text-sm text-gray-500">%</span>
              <button type="button" class="btn-sm" :disabled="percentSaving || writeBlocked" @click="savePercent">
                {{ percentSaving ? '...' : t('common.save') }}
              </button>
            </div>
            <strong v-else class="text-lg text-gray-800 dark:text-white/90">{{ percent }}%</strong>
          </div>
        </div>
        <p class="sub mt-3">
          {{ t('transport.salary.infoCalc') }}
          {{ t('transport.salary.infoApprove') }}
        </p>
      </div>

      <div class="card">
        <div class="head"><h3 class="title">{{ t('transport.salary.summaryTitle') }}</h3></div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">{{ t('transport.fields.worker') }}</th>
                <th class="th">{{ t('transport.salary.deliveries') }}</th>
                <th class="th">{{ t('transport.salary.pending') }}</th>
                <th class="th">{{ t('transport.salary.approved') }}</th>
                <th class="th">{{ t('transport.salary.total') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!summary.length"><td colspan="5" class="empty">{{ t('transport.salary.emptySummary') }}</td></tr>
              <tr v-for="s in summary" :key="s.workerId" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td font-medium text-gray-800 dark:text-white/90">{{ s.workerFullName }}</td>
                <td class="td">{{ s.deliveriesCount }}</td>
                <td class="td whitespace-nowrap">{{ money(s.pendingAmount) }}</td>
                <td class="td whitespace-nowrap">{{ money(s.approvedAmount) }}</td>
                <td class="td whitespace-nowrap font-medium">{{ money(Number(s.pendingAmount) + Number(s.approvedAmount)) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card">
        <div class="head flex-col gap-3 sm:flex-row sm:items-center">
          <h3 class="title">{{ t('transport.salary.listTitle') }}</h3>
          <div v-if="auth.canApproveTransportSalary" class="flex flex-wrap gap-2">
            <button
              type="button"
              class="btn-sm btn-success"
              :disabled="!selected.length || deciding || writeBlocked"
              @click="decide('approve')"
            >
              {{ t('transport.salary.approve', { n: selected.length }) }}
            </button>
            <button
              type="button"
              class="btn-sm btn-danger"
              :disabled="!selected.length || deciding || writeBlocked"
              @click="decide('reject')"
            >
              {{ t('transport.salary.reject') }}
            </button>
          </div>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th v-if="auth.canApproveTransportSalary" class="th w-10">
                  <input type="checkbox" :checked="allPendingSelected" :disabled="!pendingRows.length" @change="toggleAll" />
                </th>
                <th class="th">{{ t('common.date') }}</th>
                <th class="th">{{ t('transport.fields.worker') }}</th>
                <th class="th">{{ t('transport.fields.order') }}</th>
                <th class="th">{{ t('transport.salary.orderSum') }}</th>
                <th class="th">{{ t('transport.salary.calc') }}</th>
                <th class="th">{{ t('common.sum') }}</th>
                <th class="th">{{ t('common.status') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="salaryLoading"><td :colspan="salaryCols" class="empty">{{ t('common.loading') }}</td></tr>
              <tr v-else-if="!visibleSalaries.length"><td :colspan="salaryCols" class="empty">{{ t('transport.salary.empty') }}</td></tr>
              <tr v-for="s in visibleSalaries" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
                <td v-if="auth.canApproveTransportSalary" class="td">
                  <input v-if="s.salaryStatus === 'PENDING'" v-model="selected" type="checkbox" :value="s.id" />
                </td>
                <td class="td whitespace-nowrap">{{ formatDate(s.earnedAt) }}</td>
                <td class="td font-medium text-gray-800 dark:text-white/90">{{ s.workerFullName }}</td>
                <td class="td whitespace-nowrap">
                  #{{ s.saleOrderId }}
                  <div v-if="s.clientFullName" class="text-xs text-gray-500">{{ s.clientFullName }}</div>
                </td>
                <td class="td whitespace-nowrap">{{ money(s.orderTotalSnapshot) }}</td>
                <td class="td whitespace-nowrap text-xs">{{ t('transport.salary.split', { percent: s.percentSnapshot, n: s.workersCount }) }}</td>
                <td class="td whitespace-nowrap font-medium">{{ money(s.amount) }}</td>
                <td class="td">
                  <span class="badge" :class="`badge-${s.salaryStatus.toLowerCase()}`">{{ t(`workerSalaryStatus.${s.salaryStatus}`) }}</span>
                  <div v-if="s.decidedByName" class="mt-1 text-xs text-gray-500">
                    {{ s.decidedByName }} · {{ formatDate(s.decidedAt) }}
                  </div>
                  <div v-if="s.comment" class="text-xs text-gray-500">{{ s.comment }}</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div v-if="modalOpen" class="overlay">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="worker-modal-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div class="flex items-start gap-3">
            <span class="modal-badge"><HardHat class="h-5 w-5" /></span>
            <div>
              <h3 id="worker-modal-title" class="title">
                {{ editingId ? t('transport.workers.editTitle') : t('transport.workers.createTitle') }}
              </h3>
              <p class="sub">{{ t('transport.workers.modalSubtitle') }}</p>
            </div>
          </div>
          <button type="button" class="close-btn" :aria-label="t('common.close')" @click="modalOpen = false">
            <X class="h-5 w-5" />
          </button>
        </div>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label for="worker-name" class="mlbl">{{ t('common.fullName') }} <span class="req">*</span></label>
            <div class="relative">
              <User class="field-icon" />
              <input
                id="worker-name"
                v-model="form.fullName"
                required
                class="field field-with-icon"
                :placeholder="t('clients.fullNamePlaceholder')"
              />
            </div>
          </div>
          <div>
            <label for="worker-phone" class="mlbl">{{ t('common.phone') }}</label>
            <div class="relative">
              <Phone class="field-icon" />
              <input
                id="worker-phone"
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
          <div>
            <label for="worker-note" class="mlbl">{{ t('common.note') }}</label>
            <div class="relative">
              <MessageSquare class="field-icon" />
              <input id="worker-note" v-model="form.note" class="field field-with-icon" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, HardHat, MessageSquare, Phone, User, X } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  approveWorkerSalaries,
  changeWorkerStatus,
  createWorker,
  deleteWorker,
  fetchTransportSetting,
  fetchWorkerSalaries,
  fetchWorkerSalarySummary,
  fetchWorkers,
  rejectWorkerSalaries,
  updateTransportSetting,
  updateWorker,
  type TransportWorker,
  type WorkerSalary,
  type WorkerSalarySummary,
} from '@/api/transport'
import { formatApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate, money } from '@/utils/format'
import { formatUzPhone, isCompleteUzPhone, phoneDigits } from '@/utils/phone'

const { t } = useI18n()
const auth = useAuthStore()
const { writeBlocked } = useFilialScope()

const tab = ref<'workers' | 'salary'>('workers')
const error = ref<string | null>(null)

// --- workers ---
const workers = ref<TransportWorker[]>([])
const loading = ref(false)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)
const form = reactive({ fullName: '', phone: '', note: '' })

const filteredWorkers = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return workers.value
  return workers.value.filter((w) => [w.fullName, w.phone].some((v) => (v || '').toLowerCase().includes(q)))
})

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

async function loadWorkers() {
  loading.value = true
  try {
    workers.value = (await fetchWorkers()).data || []
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { fullName: '', phone: '', note: '' })
  formError.value = null
  modalOpen.value = true
}

function openEdit(w: TransportWorker) {
  editingId.value = w.id
  Object.assign(form, { fullName: w.fullName, phone: w.phone ? formatUzPhone(w.phone) : '', note: w.note || '' })
  formError.value = null
  modalOpen.value = true
}

async function onSubmit() {
  formError.value = null
  const phone = phoneDigits(form.phone).length > 3 ? form.phone : ''
  if (phone && !isCompleteUzPhone(phone)) {
    formError.value = t('clients.phoneFormat')
    return
  }
  saving.value = true
  const payload = {
    fullName: form.fullName.trim(),
    phone: phone || undefined,
    note: form.note.trim() || undefined,
  }
  try {
    if (editingId.value) await updateWorker(editingId.value, payload)
    else await createWorker(payload)
    modalOpen.value = false
    await loadWorkers()
  } catch (e) {
    formError.value = formatApiError(e, t('common.saveError'))
  } finally {
    saving.value = false
  }
}

async function onDelete(w: TransportWorker) {
  if (!confirm(t('common.deleteConfirmNamed', { name: w.fullName }))) return
  try {
    await deleteWorker(w.id)
    await loadWorkers()
  } catch (e) {
    error.value = formatApiError(e, t('common.deleteError'))
  }
}

async function onStatus(w: TransportWorker) {
  try {
    await changeWorkerStatus(w.id)
    await loadWorkers()
  } catch (e) {
    error.value = formatApiError(e, t('common.statusError'))
  }
}

// --- salary ---
const now = new Date()
const period = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const statusFilter = ref<'' | 'PENDING' | 'APPROVED' | 'REJECTED'>('')
const salaries = ref<WorkerSalary[]>([])
const summary = ref<WorkerSalarySummary[]>([])
const salaryLoading = ref(false)
const selected = ref<number[]>([])
const deciding = ref(false)
const percent = ref(0)
const percentDraft = ref(0)
const percentSaving = ref(false)

const salaryCols = computed(() => (auth.canApproveTransportSalary ? 8 : 7))
const visibleSalaries = computed(() =>
  statusFilter.value ? salaries.value.filter((s) => s.salaryStatus === statusFilter.value) : salaries.value,
)
const pendingRows = computed(() => visibleSalaries.value.filter((s) => s.salaryStatus === 'PENDING'))
const pendingCount = computed(() => salaries.value.filter((s) => s.salaryStatus === 'PENDING').length)
const allPendingSelected = computed(
  () => pendingRows.value.length > 0 && pendingRows.value.every((s) => selected.value.includes(s.id)),
)

watch(statusFilter, () => {
  selected.value = []
})

function periodParts() {
  const [y, m] = period.value.split('-').map(Number)
  return { year: y || now.getFullYear(), month: m || now.getMonth() + 1 }
}

async function loadSalary() {
  salaryLoading.value = true
  selected.value = []
  const { year, month } = periodParts()
  try {
    const [list, sum, setting] = await Promise.all([
      fetchWorkerSalaries({ year, month }),
      fetchWorkerSalarySummary(year, month),
      fetchTransportSetting(),
    ])
    salaries.value = list.data || []
    summary.value = sum.data || []
    percent.value = Number(setting.data?.workerSalaryPercent ?? 0)
    percentDraft.value = percent.value
  } catch (e) {
    error.value = formatApiError(e, t('transport.salary.loadError'))
  } finally {
    salaryLoading.value = false
  }
}

function toggleAll() {
  selected.value = allPendingSelected.value ? [] : pendingRows.value.map((s) => s.id)
}

async function decide(kind: 'approve' | 'reject') {
  if (!selected.value.length) return
  let comment: string | undefined
  if (kind === 'reject') {
    const reason = prompt(t('transport.salary.rejectPrompt'))
    if (reason === null) return
    comment = reason.trim() || undefined
  } else if (!confirm(t('transport.salary.approveConfirm', { n: selected.value.length }))) {
    return
  }
  deciding.value = true
  error.value = null
  try {
    if (kind === 'approve') await approveWorkerSalaries(selected.value)
    else await rejectWorkerSalaries(selected.value, comment)
    await loadSalary()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    deciding.value = false
  }
}

async function savePercent() {
  const value = Number(percentDraft.value)
  if (Number.isNaN(value) || value < 0 || value > 100) {
    error.value = t('transport.salary.percentRange')
    return
  }
  percentSaving.value = true
  error.value = null
  try {
    percent.value = Number((await updateTransportSetting(value)).data?.workerSalaryPercent ?? value)
    percentDraft.value = percent.value
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    percentSaving.value = false
  }
}

onMounted(() => {
  loadWorkers()
  loadSalary()
})
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
.k { display: block; margin-bottom: 0.3rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn-sm { display: inline-flex; height: 2.25rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 0.875rem; font-size: 0.8125rem; font-weight: 500; color: #fff; white-space: nowrap; }
.btn-sm:disabled { opacity: 0.55; cursor: not-allowed; }
.btn-success { background: #059669; }
.btn-danger { background: #dc2626; }
.ghost { height: 2.5rem; display: inline-flex; align-items: center; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.tab { display: inline-flex; align-items: center; gap: 0.375rem; height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; font-size: 0.875rem; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
.tab-count { min-width: 1.25rem; border-radius: 9999px; background: #f59e0b; padding: 0 0.375rem; font-size: 0.75rem; color: #fff; }
.percent-box { border-radius: 0.75rem; border: 1px dashed #d1d5db; padding: 0.75rem 1rem; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.badge { display: inline-flex; border-radius: 9999px; padding: 0.125rem 0.625rem; font-size: 0.75rem; font-weight: 500; white-space: nowrap; }
.badge-pending { background: #fffbeb; color: #b45309; }
.badge-approved { background: #ecfdf5; color: #059669; }
.badge-rejected { background: #fef2f2; color: #dc2626; }
.status-toggle { position: relative; display: inline-flex; height: 1.5rem; width: 2.75rem; align-items: center; border-radius: 9999px; transition: background 0.15s ease; border: none; cursor: pointer; padding: 0; }
.status-toggle.on { background: #12b76a; }
.status-toggle.off { background: #d1d5db; }
.status-knob { position: absolute; height: 1.25rem; width: 1.25rem; border-radius: 9999px; background: #fff; transition: transform 0.15s ease; transform: translateX(0.125rem); }
.status-toggle.on .status-knob { transform: translateX(1.375rem); }
.mlbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.field:focus { outline: none; border-color: #9cb0ff; box-shadow: 0 0 0 4px rgb(70 95 255 / 10%); }
.field-with-icon { height: 2.75rem; padding-left: 2.5rem; }
.field-icon { position: absolute; top: 50%; left: 0.75rem; z-index: 1; height: 1.1rem; width: 1.1rem; transform: translateY(-50%); color: #98a2b3; pointer-events: none; }
.modal-badge { display: flex; height: 2.5rem; width: 2.5rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.75rem; background: #eff4ff; color: #465fff; }
.close-btn { display: flex; height: 2.25rem; width: 2.25rem; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 0.5rem; color: #9ca3af; }
.close-btn:hover { background: #f3f4f6; color: #374151; }
.btn-with-icon { gap: 0.4rem; }
.dark .mlbl { color: #9ca3af; }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .modal-badge { background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.dark .close-btn:hover { background: rgb(255 255 255 / 5%); color: rgba(255, 255, 255, 0.8); }
.dark .percent-box { border-color: #374151; }
.dark .badge-pending { background: rgb(245 158 11 / 15%); color: #fcd34d; }
.dark .badge-approved { background: rgb(16 185 129 / 15%); color: #6ee7b7; }
.dark .badge-rejected { background: rgb(239 68 68 / 15%); color: #fca5a5; }
</style>
