<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Tarix" />
    <div class="mb-4 flex flex-wrap gap-2">
      <select v-model.number="warehouseFilter" class="field w-48" @change="load">
        <option :value="0">Barcha omborlar</option>
        <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
      </select>
      <select v-model.number="goodsFilter" class="field w-48" @change="load">
        <option :value="0">Barcha mahsulotlar</option>
        <option v-for="g in goodsOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
      </select>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">Mahsulot</th>
            <th class="th">Ombor</th>
            <th class="th">Tur</th>
            <th class="th">Soni</th>
            <th class="th">Sana</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="6" class="empty">Yuklanmoqda...</td></tr>
          <tr v-else-if="histories.length === 0"><td colspan="6" class="empty">Tarix yo‘q</td></tr>
          <tr v-for="h in histories" :key="h.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ h.id }}</td>
            <td class="td">{{ h.goodsName || h.goodsId }}</td>
            <td class="td">{{ h.warehouseName || h.warehouseId }}</td>
            <td class="td">{{ h.movementType || h.type || '—' }}</td>
            <td class="td">{{ h.count }}</td>
            <td class="td">{{ formatDate(h.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchStockHistories,
  fetchStockHistoriesByGoods,
  fetchStockHistoriesByWarehouse,
  type StockHistory,
} from '@/api/stocks'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'
import { formatDate } from '@/utils/format'

const histories = ref<StockHistory[]>([])
const warehouses = ref<Warehouse[]>([])
const goodsOptions = ref<Goods[]>([])
const warehouseFilter = ref(0)
const goodsFilter = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const [whRes, goodsRes] = await Promise.all([fetchWarehouses(), fetchGoods()])
    warehouses.value = whRes.data || []
    goodsOptions.value = goodsRes.data || []

    let histRes
    if (warehouseFilter.value) histRes = await fetchStockHistoriesByWarehouse(warehouseFilter.value)
    else if (goodsFilter.value) histRes = await fetchStockHistoriesByGoods(goodsFilter.value)
    else histRes = await fetchStockHistories()
    histories.value = histRes.data || []
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
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
</style>
