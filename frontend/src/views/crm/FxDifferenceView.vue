<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.fxDifference')" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <input v-model="startDate" type="date" class="field" @change="load" />
      <input v-model="endDate" type="date" class="field" @change="load" />
    </div>

    <p class="mb-4 text-sm text-gray-500 dark:text-gray-400">{{ t('fxDifference.hint') }}</p>
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="mb-4 grid grid-cols-1 gap-4 md:grid-cols-3">
      <article class="stat">
        <p class="stat-label">{{ t('fxDifference.total') }}</p>
        <h4 class="stat-value" :class="toneOf(report?.totalDifference)">{{ signed(report?.totalDifference) }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('fxDifference.gain') }}</p>
        <h4 class="stat-value gain">{{ signed(report?.totalGain) }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('fxDifference.loss') }}</p>
        <h4 class="stat-value loss">{{ signed(report?.totalLoss) }}</h4>
      </article>
    </div>

    <p v-if="report && report.unallocatedCount > 0" class="mb-4 text-sm text-amber-600">
      {{ t('fxDifference.unallocated', { n: report.unallocatedCount }) }}
    </p>

    <div class="card">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('fxDifference.payment') }}</th>
              <th class="th">{{ t('common.client') }}</th>
              <th class="th">{{ t('fxDifference.order') }}</th>
              <th class="th text-right">{{ t('fxDifference.applied') }}</th>
              <th class="th text-right">{{ t('fxDifference.orderRate') }}</th>
              <th class="th text-right">{{ t('fxDifference.paymentRate') }}</th>
              <th class="th text-right">{{ t('fxDifference.difference') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="rows.length === 0"><td colspan="7" class="empty">{{ t('fxDifference.empty') }}</td></tr>
            <tr v-for="r in rows" :key="r.paymentId" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td whitespace-nowrap">
                #{{ r.paymentId }} · {{ formatDate(r.paymentDate) }}
                <div v-if="r.paidCurrency !== r.debtCurrency" class="text-xs text-gray-500">
                  {{ t('fxDifference.paidIn', { amount: moneyIn(r.paidAmount, r.paidCurrency, som) }) }}
                </div>
              </td>
              <td class="td">
                <router-link v-if="r.clientId" :to="`/clients/${r.clientId}`" class="text-brand-500 hover:underline">
                  {{ r.clientFullName || '—' }}
                </router-link>
                <span v-else>—</span>
              </td>
              <td class="td whitespace-nowrap">
                <router-link :to="`/sales/${r.saleOrderId}`" class="text-brand-500 hover:underline">#{{ r.saleOrderId }}</router-link>
                <div class="text-xs text-gray-500">{{ formatDate(r.orderDate) }}</div>
              </td>
              <td class="td text-right whitespace-nowrap">{{ moneyIn(r.appliedAmount, r.debtCurrency, som) }}</td>
              <td class="td text-right whitespace-nowrap">
                {{ formatRate(r.orderRate) }}
                <div class="text-xs text-gray-500">{{ moneyIn(r.orderBase, 'UZS', som) }}</div>
              </td>
              <td class="td text-right whitespace-nowrap">
                {{ formatRate(r.paymentRate) }}
                <div class="text-xs text-gray-500">{{ moneyIn(r.paymentBase, 'UZS', som) }}</div>
              </td>
              <td class="td text-right whitespace-nowrap font-semibold" :class="toneOf(r.difference)">{{ signed(r.difference) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchFxDifference, type FxDifference } from '@/api/dashboard'
import { formatApiError } from '@/api/http'
import { formatDate, today } from '@/utils/format'
import { formatRate, moneyIn } from '@/utils/currency'

const { t } = useI18n()
const som = computed(() => t('common.currency'))
const startDate = ref(monthStart())
const endDate = ref(today())
const loading = ref(false)
const error = ref<string | null>(null)
const report = ref<FxDifference | null>(null)

const rows = computed(() => report.value?.rows || [])

function monthStart() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

function signed(v?: number | null) {
  const n = Number(v || 0)
  const text = moneyIn(Math.abs(n), 'UZS', som.value)
  if (n > 0) return `+${text}`
  if (n < 0) return `−${text}`
  return text
}

function toneOf(v?: number | null) {
  const n = Number(v || 0)
  return n > 0 ? 'gain' : n < 0 ? 'loss' : ''
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchFxDifference(startDate.value, endDate.value)
    report.value = res.data || null
  } catch (e) {
    error.value = formatApiError(e, t('fxDifference.loadError'))
    report.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.th.text-right { text-align: right; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.stat { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.stat-label { font-size: 0.875rem; color: #4b5563; }
.stat-value { margin-top: 0.25rem; font-size: 1.25rem; font-weight: 700; color: #1f2937; }
.gain { color: #059669; }
.loss { color: #dc2626; }
.dark .card, .dark .stat { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .stat-label { color: #9ca3af; }
.dark .stat-value { color: rgba(255, 255, 255, 0.92); }
.dark .gain { color: #34d399; }
.dark .loss { color: #f87171; }
</style>
