<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Ishlab chiqarish" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 sm:flex-row sm:items-center sm:justify-between dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Ishlab chiqarish buyurtmalari</h3>
        <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
      </div>
      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Savdo</th>
              <th class="th">Mijoz</th>
              <th class="th">Joriy sex</th>
              <th class="th">{{ t('production.assignment') }}</th>
              <th class="th">{{ t('production.status') }}</th>
              <th class="th">{{ t('production.acceptedAt') }}</th>
              <th class="th">{{ t('production.submittedAt') }}</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="9" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="9" class="empty">Buyurtma yo‘q</td></tr>
            <tr v-for="o in filtered" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ o.id }}</td>
              <td class="td">
                <router-link v-if="o.saleOrderId" :to="`/sales/${o.saleOrderId}`" class="text-brand-500 hover:underline">
                  #{{ o.saleOrderId }}
                </router-link>
                <span v-else>—</span>
              </td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ o.clientFullName || '—' }}</td>
              <td class="td">{{ o.currentWorkshopName || '—' }}</td>
              <td class="td">{{ assignmentLabel(o.currentAssignmentStatus) }}</td>
              <td class="td">{{ productionStatusLabel(o.productionStatus) }}</td>
              <td class="td whitespace-nowrap">{{ formatDt(o.startedAt) }}</td>
              <td class="td whitespace-nowrap">{{ formatDt(o.doneAt) }}</td>
              <td class="td text-right">
                <button type="button" class="link-btn" @click="openDetail(o)">{{ t('production.timeline') }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="detailOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="detailOpen = false">
      <div class="w-full max-w-xl max-h-[85vh] overflow-y-auto rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">
            {{ t('production.timeline') }} #{{ selected?.id }}
          </h3>
          <button type="button" class="text-sm text-gray-500" @click="detailOpen = false">Yopish</button>
        </div>
        <div v-if="timelineError" class="err mb-3">{{ timelineError }}</div>
        <div v-if="timelineLoading" class="empty">Yuklanmoqda...</div>
        <ol v-else class="space-y-3">
          <li
            v-for="ev in timeline"
            :key="ev.id"
            class="rounded-lg border border-gray-100 p-3 dark:border-gray-800"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="text-sm font-semibold text-gray-800 dark:text-white/90">{{ eventTypeLabel(ev.eventType) }}</span>
              <span class="text-xs text-gray-500">{{ formatDt(ev.occurredAt) }}</span>
            </div>
            <div class="mt-1 text-sm text-gray-600 dark:text-gray-400">
              <span v-if="ev.fromWorkshopName">{{ ev.fromWorkshopName }} → </span>
              <span>{{ ev.toWorkshopName || '—' }}</span>
            </div>
            <div class="mt-1 text-xs text-gray-500">{{ ev.actorName || '—' }}</div>
            <div v-if="ev.comment" class="mt-1 text-sm text-gray-700 dark:text-gray-300">{{ ev.comment }}</div>
          </li>
        </ol>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchProductionOrders,
  fetchProductionTimeline,
  type ProductionEvent,
  type ProductionOrder,
} from '@/api/production'
import { formatApiError } from '@/api/http'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const items = ref<ProductionOrder[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')
const detailOpen = ref(false)
const selected = ref<ProductionOrder | null>(null)
const timeline = ref<ProductionEvent[]>([])
const timelineLoading = ref(false)
const timelineError = ref<string | null>(null)

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return [...items.value].filter((o) => {
    if (!q) return true
    return (
      String(o.id).includes(q)
      || String(o.saleOrderId || '').includes(q)
      || (o.clientFullName || '').toLowerCase().includes(q)
      || (o.currentWorkshopName || '').toLowerCase().includes(q)
    )
  })
})

function formatDt(v?: string) {
  return formatDate(v)
}

function productionStatusLabel(status?: string | null) {
  if (!status) return '—'
  const key = `productionStatus.${status}`
  const label = t(key)
  return label !== key ? String(label) : status
}

function assignmentLabel(status?: string | null) {
  if (!status) return '—'
  const key = `assignmentStatus.${status}`
  const label = t(key)
  return label !== key ? String(label) : status
}

function eventTypeLabel(eventType?: string | null) {
  if (!eventType) return '—'
  const key = `productionEvent.${eventType}`
  const label = t(key)
  return label !== key ? String(label) : eventType
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchProductionOrders()
    items.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e, 'Yuklashda xatolik')
  } finally {
    loading.value = false
  }
}

async function openDetail(o: ProductionOrder) {
  selected.value = o
  detailOpen.value = true
  timelineLoading.value = true
  timelineError.value = null
  timeline.value = []
  try {
    const res = await fetchProductionTimeline(o.id)
    timeline.value = res.data || []
  } catch (e) {
    timelineError.value = formatApiError(e, 'Timeline xatolik')
  } finally {
    timelineLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.link-btn { color: #465fff; font-size: 0.875rem; font-weight: 500; }
</style>
