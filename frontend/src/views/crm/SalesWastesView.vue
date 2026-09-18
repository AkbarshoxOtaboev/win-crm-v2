<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Ortiqcha material" />

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card mb-4">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Mahsulot bo‘yicha jami</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">Mahsulot</th>
              <th class="th">Jami miqdor</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="2" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="summary.length === 0"><td colspan="2" class="empty">Ortiqcha material yo‘q</td></tr>
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
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Yozuvlar</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Buyurtma</th>
              <th class="th">Mahsulot</th>
              <th class="th">Miqdor</th>
              <th class="th">O‘lcham</th>
              <th class="th">Izoh</th>
              <th class="th">Sana</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="items.length === 0"><td colspan="7" class="empty">Yozuv yo‘q</td></tr>
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
    error.value = formatApiError(e, 'Ortiqcha materialni yuklab bo‘lmadi')
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
