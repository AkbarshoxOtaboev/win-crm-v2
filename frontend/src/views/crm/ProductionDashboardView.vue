<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.productionDashboard')" />

    <div class="card mb-4 p-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">
            {{ t('nav.productionDashboard') }}
          </h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {{ t('productionDashboard.subtitle') }}
          </p>
        </div>
        <div class="flex flex-wrap items-center gap-2">
          <select v-model.number="workshopId" class="field sm:w-64" @change="load">
            <option :value="0" disabled>{{ t('productionDashboard.selectWorkshop') }}</option>
            <option v-for="w in workshops" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
          <button type="button" class="icon-btn" :disabled="loading" :title="t('common.loading')" @click="load">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
        </div>
      </div>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <template v-if="dash">
      <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <article class="stat-card" style="--accent: #3b82f6">
          <p class="label">{{ t('productionDashboard.queued') }}</p>
          <p class="count">{{ dash.queuedCount }}</p>
          <p class="sum">{{ money(dash.queuedSum) }} <span>so‘m</span></p>
        </article>
        <article class="stat-card" style="--accent: #f97316">
          <p class="label">{{ t('productionDashboard.inProgress') }}</p>
          <p class="count">{{ dash.inProgressCount }}</p>
          <p class="sum">{{ money(dash.inProgressSum) }} <span>so‘m</span></p>
        </article>
        <article class="stat-card" style="--accent: #059669">
          <p class="label">{{ t('productionDashboard.doneSum') }}</p>
          <p class="count">{{ dash.doneCount }}</p>
          <p class="sum">{{ money(dash.doneSum) }} <span>so‘m</span></p>
        </article>
        <article class="stat-card" style="--accent: #7c3aed">
          <p class="label">{{ t('productionDashboard.balance') }}</p>
          <p class="count text-xl!">{{ money(dash.balance) }}</p>
          <p class="sum">{{ t('productionDashboard.earned') }}: {{ money(dash.totalEarned) }}</p>
        </article>
      </div>

      <div class="card p-5">
        <div class="mb-4 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">
              {{ t('productionDashboard.completedTitle') }}
            </h3>
            <p class="mt-1 text-sm text-gray-500">
              {{ t('productionDashboard.defaultFee') }}:
              <strong>{{ dash.feePercent ?? 0 }}%</strong>
            </p>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">#</th>
                <th class="th">{{ t('productionDashboard.sale') }}</th>
                <th class="th">{{ t('productionDashboard.client') }}</th>
                <th class="th">{{ t('productionDashboard.orderSum') }}</th>
                <th class="th">{{ t('production.acceptedAt') }}</th>
                <th class="th">{{ t('production.submittedAt') }}</th>
                <th class="th">{{ t('productionDashboard.feePercent') }}</th>
                <th class="th">{{ t('productionDashboard.earnedAmount') }}</th>
                <th v-if="auth.superAdmin" class="th text-right">{{ t('productionDashboard.setFee') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading"><td :colspan="auth.superAdmin ? 9 : 8" class="empty">{{ t('common.loading') }}</td></tr>
              <tr v-else-if="!dash.completedWorks?.length">
                <td :colspan="auth.superAdmin ? 9 : 8" class="empty">{{ t('productionDashboard.noCompleted') }}</td>
              </tr>
              <tr
                v-for="w in dash.completedWorks"
                :key="w.assignmentId"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ w.assignmentId }}</td>
                <td class="td">
                  <router-link
                    v-if="w.saleOrderId"
                    :to="`/sales/${w.saleOrderId}`"
                    class="text-brand-500 hover:underline"
                  >
                    #{{ w.saleOrderId }}
                  </router-link>
                  <span v-else>—</span>
                </td>
                <td class="td">{{ w.clientFullName || '—' }}</td>
                <td class="td whitespace-nowrap">{{ money(w.orderTotalSum) }}</td>
                <td class="td whitespace-nowrap">{{ formatDate(w.acceptedAt) }}</td>
                <td class="td whitespace-nowrap">{{ formatDate(w.submittedAt) }}</td>
                <td class="td">{{ w.feePercent ?? 0 }}%</td>
                <td class="td whitespace-nowrap">{{ money(w.earnedAmount) }}</td>
                <td v-if="auth.superAdmin" class="td text-right">
                  <div class="inline-flex items-center gap-1.5">
                    <input
                      v-model.number="feeDraft[w.assignmentId]"
                      type="number"
                      min="0"
                      max="100"
                      step="0.01"
                      class="field fee-input"
                    />
                    <button
                      type="button"
                      class="btn-sm"
                      :disabled="savingId === w.assignmentId"
                      @click="saveFee(w)"
                    >
                      {{ savingId === w.assignmentId ? '...' : t('common.save') }}
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchActiveWorkshops,
  fetchWorkshopDashboard,
  setAssignmentFeePercent,
  type Workshop,
  type WorkshopDashboard,
  type WorkshopDashboardCompletedWork,
} from '@/api/workshops'
import { formatApiError } from '@/api/http'
import { formatDate, money } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()

const workshops = ref<Workshop[]>([])
const workshopId = ref(0)
const dash = ref<WorkshopDashboard | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const feeDraft = reactive<Record<number, number>>({})
const savingId = ref<number | null>(null)

watch(
  () => dash.value?.completedWorks,
  (list) => {
    for (const w of list || []) {
      feeDraft[w.assignmentId] = Number(w.feePercent ?? 0)
    }
  },
)

async function loadWorkshops() {
  const res = await fetchActiveWorkshops()
  workshops.value = res.data || []
  if (!workshopId.value && workshops.value[0]) {
    workshopId.value = workshops.value[0].id
  }
}

async function load() {
  if (!workshopId.value) {
    dash.value = null
    return
  }
  loading.value = true
  error.value = null
  try {
    const res = await fetchWorkshopDashboard(workshopId.value)
    dash.value = res.data
  } catch (e) {
    error.value = formatApiError(e)
    dash.value = null
  } finally {
    loading.value = false
  }
}

async function saveFee(w: WorkshopDashboardCompletedWork) {
  const fee = Number(feeDraft[w.assignmentId])
  if (Number.isNaN(fee) || fee < 0 || fee > 100) {
    error.value = t('productionDashboard.invalidFee')
    return
  }
  savingId.value = w.assignmentId
  error.value = null
  try {
    await setAssignmentFeePercent(w.assignmentId, fee)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    savingId.value = null
  }
}

onMounted(async () => {
  try {
    await loadWorkshops()
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
})
</script>

<style scoped>
.card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}
:global(.dark) .card {
  border-color: #1f2937;
  background: rgb(255 255 255 / 0.03);
}
.stat-card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 1.25rem;
  border-top: 3px solid var(--accent);
}
:global(.dark) .stat-card {
  border-color: #1f2937;
  background: rgb(255 255 255 / 0.03);
}
.stat-card .label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #4b5563;
}
.stat-card .count {
  margin-top: 0.5rem;
  font-size: 1.75rem;
  font-weight: 700;
  color: #1f2937;
}
:global(.dark) .stat-card .count {
  color: rgb(255 255 255 / 0.9);
}
.stat-card .sum {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  font-weight: 600;
  color: #1f2937;
}
.stat-card .sum span {
  font-weight: 400;
  color: #6b7280;
}
.th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
}
.td {
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: #4b5563;
}
.empty {
  padding: 2.5rem 1rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.field {
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
.fee-input {
  width: 5.5rem;
  height: 2rem;
}
.btn-sm {
  display: inline-flex;
  height: 2rem;
  align-items: center;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 0.75rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #fff;
}
.icon-btn {
  display: inline-flex;
  height: 2.5rem;
  width: 2.5rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  color: #4b5563;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: #dc2626;
}
</style>
