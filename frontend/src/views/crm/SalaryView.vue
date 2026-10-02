<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.salary')" />

    <div class="mb-4 flex flex-wrap gap-2">
      <button type="button" class="tab" :class="{ active: tab === 'configs' }" @click="tab = 'configs'">{{ t('salary.tabConfigs') }}</button>
      <button type="button" class="tab" :class="{ active: tab === 'adjust' }" @click="tab = 'adjust'">{{ t('salary.tabAdjust') }}</button>
      <button type="button" class="tab" :class="{ active: tab === 'slip' }" @click="tab = 'slip'">{{ t('salary.tabSlip') }}</button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <!-- Configs -->
    <div v-show="tab === 'configs'" class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('salary.configsTitle') }}</h3>
        <button type="button" class="btn" :disabled="writeBlocked" @click="openConfigCreate">{{ t('salary.newConfig') }}</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('salary.employee') }}</th>
              <th class="th">{{ t('salary.baseSalary') }}</th>
              <th class="th">{{ t('salary.commission') }}</th>
              <th class="th">{{ t('common.from') }}</th>
              <th class="th">{{ t('common.to') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="configs.length === 0"><td colspan="7" class="empty">{{ t('salary.noConfigs') }}</td></tr>
            <tr v-for="c in configs" :key="c.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ c.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ c.userFullName || c.userId }}</td>
              <td class="td">{{ money(c.baseSalary) }}</td>
              <td class="td">{{ enumLabel('salary.commissionTypes', c.commissionType) }} {{ c.commissionValue }}</td>
              <td class="td">{{ c.effectiveFrom }}</td>
              <td class="td">{{ c.effectiveTo || '—' }}</td>
              <td class="td text-right">
                <RowActions :edit="false" @delete="onConfigDelete(c)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Adjustment -->
    <div v-show="tab === 'adjust'" class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03]">
      <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('salary.manualAdjust') }}</h3>
      <div v-if="formError" class="err mb-3">{{ formError }}</div>
      <form class="max-w-lg space-y-3" @submit.prevent="onAdjustSubmit">
        <div>
          <label class="lbl">{{ t('salary.employee') }} *</label>
          <select v-model.number="adjustForm.userId" required class="field">
            <option :value="0" disabled>{{ t('common.select') }}</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="lbl">{{ t('common.type') }} *</label>
            <select v-model="adjustForm.entryType" required class="field">
              <option value="BONUS">{{ t('salary.entryTypes.BONUS') }}</option>
              <option value="DEDUCTION">{{ t('salary.entryTypes.DEDUCTION') }}</option>
              <option value="ADVANCE">{{ t('salary.entryTypes.ADVANCE') }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">{{ t('common.sum') }} *</label>
            <input v-model.number="adjustForm.amount" type="number" min="0.01" step="0.01" required class="field" />
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="lbl">{{ t('common.year') }}</label>
            <input v-model.number="adjustForm.periodYear" type="number" min="2000" class="field" />
          </div>
          <div>
            <label class="lbl">{{ t('common.month') }}</label>
            <input v-model.number="adjustForm.periodMonth" type="number" min="1" max="12" class="field" />
          </div>
        </div>
        <div>
          <label class="lbl">{{ t('common.note') }}</label>
          <input v-model="adjustForm.comment" class="field" />
        </div>
        <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('common.add') }}</button>
      </form>

      <div v-if="adjustForm.userId" class="mt-8">
        <div class="mb-3 flex items-center justify-between">
          <h4 class="font-medium text-gray-800 dark:text-white/90">{{ t('salary.ledgerTitle') }}</h4>
          <button type="button" class="text-sm text-brand-500" @click="loadTx">{{ t('common.refresh') }}</button>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">#</th>
                <th class="th">{{ t('common.type') }}</th>
                <th class="th">{{ t('common.sum') }}</th>
                <th class="th">{{ t('common.period') }}</th>
                <th class="th">{{ t('common.note') }}</th>
                <th class="th text-right">{{ t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="txLoading"><td colspan="6" class="empty">{{ t('common.loading') }}</td></tr>
              <tr v-else-if="transactions.length === 0"><td colspan="6" class="empty">{{ t('salary.noEntries') }}</td></tr>
              <tr v-for="tx in transactions" :key="tx.id" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td">{{ tx.id }}</td>
                <td class="td">{{ enumLabel('salary.entryTypes', tx.entryType) }}</td>
                <td class="td">{{ money(tx.amount) }}</td>
                <td class="td">{{ tx.periodYear }}-{{ tx.periodMonth }}</td>
                <td class="td">{{ tx.comment || '—' }}</td>
                <td class="td text-right">
                  <RowActions
                    v-if="isManualTx(tx.entryType)"
                    :edit="false"
                    @delete="onTxDelete(tx)"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Slip -->
    <div v-show="tab === 'slip'" class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03]">
      <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('salary.slipTitle') }}</h3>
      <div class="mb-4 flex flex-wrap gap-3">
        <div class="w-48">
          <label class="lbl">{{ t('salary.employee') }}</label>
          <select v-model.number="slipForm.userId" class="field">
            <option :value="0" disabled>{{ t('common.select') }}</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
        </div>
        <div class="w-28">
          <label class="lbl">{{ t('common.year') }}</label>
          <input v-model.number="slipForm.year" type="number" class="field" />
        </div>
        <div class="w-24">
          <label class="lbl">{{ t('common.month') }}</label>
          <input v-model.number="slipForm.month" type="number" min="1" max="12" class="field" />
        </div>
        <div class="flex items-end">
          <button type="button" class="btn" :disabled="slipLoading" @click="loadSlip">{{ slipLoading ? '...' : t('common.view') }}</button>
        </div>
      </div>
      <div v-if="slip" class="grid max-w-lg gap-2 text-sm text-gray-700 dark:text-gray-300">
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.employee') }}</span><strong>{{ slip.userFullName || slip.userId }}</strong>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('common.period') }}</span><strong>{{ slip.periodYear }}-{{ String(slip.periodMonth).padStart(2, '0') }}</strong>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.baseSalary') }}</span><span>{{ money(slip.baseSalary) }}</span>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.commission') }}</span><span>{{ money(slip.totalCommission) }}</span>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.commissionReversal') }}</span><span>{{ money(slip.totalCommissionReversal) }}</span>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.kpi') }}</span><span>{{ money(slip.totalKpi) }}</span>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.bonus') }}</span><span>{{ money(slip.totalBonus) }}</span>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.deduction') }}</span><span>{{ money(slip.totalDeduction) }}</span>
        </div>
        <div class="flex justify-between border-b border-gray-100 py-2 dark:border-gray-800">
          <span>{{ t('salary.advance') }}</span><span>{{ money(slip.totalAdvance) }}</span>
        </div>
        <div class="flex justify-between py-2 text-base font-semibold text-gray-800 dark:text-white/90">
          <span>{{ t('salary.net') }}</span><span>{{ money(slip.netSalary) }}</span>
        </div>
      </div>
    </div>

    <!-- Config modal -->
    <div v-if="configModal" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('salary.newConfigTitle') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onConfigSubmit">
          <div>
            <label class="lbl">{{ t('salary.employee') }} *</label>
            <select v-model.number="configForm.userId" required class="field">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">{{ t('salary.baseSalary') }} *</label>
            <input v-model.number="configForm.baseSalary" type="number" min="0" step="0.01" required class="field" />
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="lbl">{{ t('salary.commissionType') }} *</label>
              <select v-model="configForm.commissionType" required class="field">
                <option value="PERCENT">{{ t('salary.commissionTypes.PERCENT') }}</option>
                <option value="FIXED">{{ t('salary.commissionTypes.FIXED') }}</option>
              </select>
            </div>
            <div>
              <label class="lbl">{{ t('salary.value') }} *</label>
              <input v-model.number="configForm.commissionValue" type="number" min="0" step="0.01" required class="field" />
            </div>
          </div>
          <div>
            <label class="lbl">{{ t('salary.effectiveFrom') }} *</label>
            <input v-model="configForm.effectiveFrom" type="date" required class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="configModal = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('common.save') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  addSalaryAdjustment,
  createSalaryConfig,
  deleteSalaryConfig,
  deleteSalaryTransaction,
  fetchSalaryConfigs,
  fetchSalarySlip,
  fetchSalaryTransactions,
  type SalaryConfig,
  type SalarySlip,
  type SalaryTransaction,
} from '@/api/salary'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { ApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'

const { t, te } = useI18n()
const { writeBlocked } = useFilialScope()
const tab = ref<'configs' | 'adjust' | 'slip'>('configs')
const configs = ref<SalaryConfig[]>([])
const users = ref<UserItem[]>([])
const transactions = ref<SalaryTransaction[]>([])
const slip = ref<SalarySlip | null>(null)
const loading = ref(false)
const txLoading = ref(false)
const slipLoading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)

const configModal = ref(false)
const configForm = reactive({
  userId: 0,
  baseSalary: 0,
  commissionType: 'PERCENT' as 'PERCENT' | 'FIXED',
  commissionValue: 0,
  effectiveFrom: '',
})

const now = new Date()
const adjustForm = reactive({
  userId: 0,
  entryType: 'BONUS' as 'BONUS' | 'DEDUCTION' | 'ADVANCE',
  amount: 0,
  periodYear: now.getFullYear(),
  periodMonth: now.getMonth() + 1,
  comment: '',
})

const slipForm = reactive({
  userId: 0,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
})

function money(v?: number) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ').format(Number(v))
}

function enumLabel(prefix: string, value: string) {
  const key = `${prefix}.${value}`
  return te(key) ? t(key) : value
}

function isManualTx(type: string) {
  return type === 'BONUS' || type === 'DEDUCTION' || type === 'ADVANCE'
}

function today() {
  return new Date().toISOString().slice(0, 10)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [cfgRes, usersRes] = await Promise.all([fetchSalaryConfigs(), fetchUserOptions()])
    configs.value = cfgRes.data?.content || []
    users.value = usersRes.data || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.loadError')
  } finally {
    loading.value = false
  }
}

function openConfigCreate() {
  configForm.userId = users.value[0]?.id || 0
  configForm.baseSalary = 0
  configForm.commissionType = 'PERCENT'
  configForm.commissionValue = 0
  configForm.effectiveFrom = today()
  formError.value = null
  configModal.value = true
}

async function onConfigSubmit() {
  saving.value = true
  formError.value = null
  try {
    await createSalaryConfig({ ...configForm })
    configModal.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.saveError')
  } finally {
    saving.value = false
  }
}

async function onConfigDelete(c: SalaryConfig) {
  if (!confirm(t('salary.configDeleteConfirm', { id: c.id }))) return
  try {
    await deleteSalaryConfig(c.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.deleteError')
  }
}

async function loadTx() {
  if (!adjustForm.userId) return
  txLoading.value = true
  try {
    const res = await fetchSalaryTransactions(adjustForm.userId)
    transactions.value = res.data?.content || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('salary.ledgerLoadError')
  } finally {
    txLoading.value = false
  }
}

async function onAdjustSubmit() {
  saving.value = true
  formError.value = null
  try {
    await addSalaryAdjustment({
      userId: adjustForm.userId,
      entryType: adjustForm.entryType,
      amount: adjustForm.amount,
      periodYear: adjustForm.periodYear || undefined,
      periodMonth: adjustForm.periodMonth || undefined,
      comment: adjustForm.comment.trim() || undefined,
    })
    adjustForm.amount = 0
    adjustForm.comment = ''
    await loadTx()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : t('common.saveError')
  } finally {
    saving.value = false
  }
}

async function onTxDelete(tx: SalaryTransaction) {
  if (!confirm(t('salary.txDeleteConfirm', { id: tx.id }))) return
  try {
    await deleteSalaryTransaction(tx.id)
    await loadTx()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : t('common.deleteError')
  }
}

async function loadSlip() {
  if (!slipForm.userId) return
  slipLoading.value = true
  error.value = null
  try {
    const res = await fetchSalarySlip(slipForm.userId, slipForm.year, slipForm.month)
    slip.value = res.data
  } catch (e) {
    slip.value = null
    error.value = e instanceof ApiError ? e.message : t('salary.slipLoadError')
  } finally {
    slipLoading.value = false
  }
}

watch(() => adjustForm.userId, () => {
  transactions.value = []
  if (adjustForm.userId) loadTx()
})

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
.tab { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 1rem; font-size: 0.875rem; color: #4b5563; }
.tab.active { background: #465fff; border-color: #465fff; color: #fff; }
</style>
