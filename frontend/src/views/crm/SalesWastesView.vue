<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.salesWaste')" />

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card mb-4">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('salesWastes.totalByProduct') }}</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('common.product') }}</th>
              <th class="th">{{ t('salesWastes.totalQuantity') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="2" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="summary.length === 0"><td colspan="2" class="empty">{{ t('salesWastes.noWaste') }}</td></tr>
            <tr v-for="row in summary" :key="row.goodsId || row.goodsName" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ row.goodsName || row.goodsId || '—' }}</td>
              <td class="td">{{ formatQty(row.totalQuantity) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('salesWastes.records') }}</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">{{ t('salesWastes.order') }}</th>
              <th class="th">{{ t('common.product') }}</th>
              <th class="th">{{ t('salesWastes.quantity') }}</th>
              <th class="th">{{ t('salesWastes.size') }}</th>
              <th class="th">{{ t('common.comment') }}</th>
              <th class="th">{{ t('common.date') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="items.length === 0"><td colspan="7" class="empty">{{ t('salesWastes.noRecords') }}</td></tr>
            <tr v-for="w in items" :key="w.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ w.id }}</td>
              <td class="td">
                <router-link v-if="w.saleOrderId" :to="`/sales/${w.saleOrderId}`" class="text-brand-500 hover:underline">
                  #{{ w.saleOrderId }}
                </router-link>
                <span v-else>—</span>
              </td>
              <td class="td">{{ w.goodsName || w.goodsId || '—' }}</td>
              <td class="td">{{ formatQty(w.quantity) }} {{ w.unitName || '' }}</td>
              <td class="td">{{ sizeLabel(w) }}</td>
              <td class="td">{{ w.comment || '—' }}</td>
              <td class="td">{{ formatDate(w.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchSaleOrderWastePage,
  fetchSaleOrderWasteSummary,
  type SaleOrderWaste,
  type SaleOrderWasteSummary,
} from '@/api/saleOrderWastes'
import { formatApiError } from '@/api/http'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const loading = ref(false)
const error = ref<string | null>(null)
const summary = ref<SaleOrderWasteSummary[]>([])
const items = ref<SaleOrderWaste[]>([])

function formatQty(v?: number | null) {
  if (v == null) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 }).format(Number(v))
}

function sizeLabel(w: SaleOrderWaste) {
  if (w.width == null && w.height == null) return '—'
  return `${formatQty(w.width)} × ${formatQty(w.height)}`
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [sumRes, pageRes] = await Promise.all([
      fetchSaleOrderWasteSummary(),
      fetchSaleOrderWastePage(0, 200),
    ])
    summary.value = sumRes.data || []
    items.value = pageRes.data?.content || []
  } catch (e) {
    error.value = formatApiError(e, t('salesWastes.loadError'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
</style>
