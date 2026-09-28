<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.transportDashboard')" />

    <div class="card mb-4 p-5">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 class="title">{{ t('nav.transportMenu') }} — {{ t('nav.transportDashboard') }}</h3>
          <p class="sub">{{ t('transport.dashboard.subtitle') }}</p>
        </div>
        <button type="button" class="icon-btn" :disabled="loading" :title="t('common.refresh')" @click="load">
          <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
        </button>
      </div>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <template v-if="dash">
      <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <router-link to="/transport/orders" class="stat-card" style="--accent: #f59e0b">
          <p class="label">{{ t('deliveryStatus.PENDING') }}</p>
          <p class="count">{{ dash.pendingCount }}</p>
          <p class="hint">{{ t('transport.dashboard.pendingHint') }}</p>
        </router-link>
        <article class="stat-card" style="--accent: #3b82f6">
          <p class="label">{{ t('deliveryStatus.ACCEPTED') }}</p>
          <p class="count">{{ dash.acceptedCount }}</p>
          <p class="hint">{{ t('transport.dashboard.acceptedHint') }}</p>
        </article>
        <article class="stat-card" style="--accent: #06b6d4">
          <p class="label">{{ t('deliveryStatus.IN_TRANSIT') }}</p>
          <p class="count">{{ dash.inTransitCount }}</p>
          <p class="hint">{{ t('transport.dashboard.inTransitHint') }}</p>
        </article>
        <article class="stat-card" style="--accent: #7c3aed">
          <p class="label">{{ t('deliveryStatus.ARRIVED') }}</p>
          <p class="count">{{ dash.arrivedCount }}</p>
          <p class="hint">{{ t('transport.dashboard.arrivedHint') }}</p>
        </article>
      </div>

      <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <article class="stat-card" style="--accent: #059669">
          <p class="label">{{ t('transport.dashboard.confirmedMonth') }}</p>
          <p class="count">{{ dash.confirmedMonthCount }}</p>
          <p class="sum">{{ money(dash.confirmedMonthSum) }} <span>{{ t('common.currency') }}</span></p>
        </article>
        <router-link to="/transport/workers" class="stat-card" style="--accent: #f97316">
          <p class="label">{{ t('transport.dashboard.salaryPending') }}</p>
          <p class="count text-xl!">{{ money(dash.salaryPendingAmount) }}</p>
          <p class="hint">{{ t('transport.dashboard.workerShare', { percent: dash.workerSalaryPercent ?? 0 }) }}</p>
        </router-link>
        <article class="stat-card" style="--accent: #10b981">
          <p class="label">{{ t('transport.dashboard.salaryApprovedMonth') }}</p>
          <p class="count text-xl!">{{ money(dash.salaryApprovedMonthAmount) }}</p>
          <p class="hint">{{ t('common.currency') }}</p>
        </article>
        <article class="stat-card" style="--accent: #64748b">
          <p class="label">{{ t('transport.dashboard.activeCrew') }}</p>
          <p class="count">{{ dash.activeDrivers }} / {{ dash.activeWorkers }}</p>
          <p class="hint">{{ t('transport.dashboard.crewHint') }}</p>
        </article>
      </div>

      <div class="card">
        <div class="head">
          <h3 class="title">{{ t('transport.dashboard.activeDeliveries') }}</h3>
          <router-link to="/transport/orders" class="link text-sm">{{ t('common.all') }} →</router-link>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">{{ t('transport.fields.order') }}</th>
                <th class="th">{{ t('common.client') }}</th>
                <th class="th">{{ t('common.address') }}</th>
                <th class="th">{{ t('common.sum') }}</th>
                <th class="th">{{ t('transport.fields.driver') }}</th>
                <th class="th">{{ t('common.status') }}</th>
                <th class="th">{{ t('transport.fields.sent') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!dash.activeDeliveries.length"><td colspan="7" class="empty">{{ t('transport.dashboard.noActive') }}</td></tr>
              <tr v-for="d in dash.activeDeliveries" :key="d.id" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td whitespace-nowrap">#{{ d.saleOrderId }}</td>
                <td class="td">{{ d.clientFullName || '—' }}</td>
                <td class="td">{{ d.address || '—' }}</td>
                <td class="td whitespace-nowrap">{{ money(d.orderTotalSum) }}</td>
                <td class="td">{{ d.driverFullName || '—' }}</td>
                <td class="td"><DeliveryStatusBadge :status="d.deliveryStatus" /></td>
                <td class="td whitespace-nowrap">{{ formatDate(d.sentAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import DeliveryStatusBadge from '@/components/crm/DeliveryStatusBadge.vue'
import { fetchTransportDashboard, type TransportDashboard } from '@/api/transport'
import { formatApiError } from '@/api/http'
import { formatDate, money } from '@/utils/format'

const { t } = useI18n()
const dash = ref<TransportDashboard | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    dash.value = (await fetchTransportDashboard()).data
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.head { display: flex; align-items: center; justify-content: space-between; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.stat-card { display: block; border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; border-top: 3px solid var(--accent); }
.dark .stat-card { border-color: #1f2937; background: rgb(255 255 255 / 0.03); }
.stat-card .label { font-size: 0.875rem; font-weight: 500; color: #4b5563; }
.stat-card .count { margin-top: 0.5rem; font-size: 1.75rem; font-weight: 700; color: #1f2937; }
.stat-card .sum { margin-top: 0.5rem; font-size: 0.875rem; font-weight: 600; color: #1f2937; }
.stat-card .sum span, .stat-card .hint { font-weight: 400; color: #6b7280; }
.stat-card .hint { margin-top: 0.5rem; font-size: 0.8125rem; }
.dark .stat-card .label { color: #9ca3af; }
.dark .stat-card .count, .dark .stat-card .sum { color: rgb(255 255 255 / 0.9); }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #e5e7eb; color: #4b5563; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.link { color: #465fff; font-weight: 500; }
.link:hover { text-decoration: underline; }
</style>
