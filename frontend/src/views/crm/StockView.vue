<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Qoldiq" />
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
            <th class="th">#</th><th class="th">Mahsulot</th><th class="th">Ombor</th><th class="th">Miqdor</th><th class="th text-right">Amallar</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="empty">Yuklanmoqda...</td></tr>
          <tr v-else-if="items.length === 0"><td colspan="5" class="empty">Qoldiq yo‘q</td></tr>
          <tr v-for="s in items" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ s.id }}</td>
            <td class="td">{{ s.goodsName || s.goodsId }}</td>
            <td class="td">{{ s.warehouseName || s.warehouseId }}</td>
            <td class="td">{{ s.count }}</td>
            <td class="td text-right"><RowActions :edit="false" @delete="onDelete(s)" /></td>
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
import RowActions from '@/components/crm/RowActions.vue'
import {
  deleteStock,
  fetchStocks,
  fetchStocksByGoods,
  fetchStocksByWarehouse,
  type Stock,
} from '@/api/stocks'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'

const items = ref<Stock[]>([])
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

    let stockRes
    if (warehouseFilter.value) stockRes = await fetchStocksByWarehouse(warehouseFilter.value)
    else if (goodsFilter.value) stockRes = await fetchStocksByGoods(goodsFilter.value)
    else stockRes = await fetchStocks()
    items.value = stockRes.data || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function onDelete(s: Stock) {
  if (!confirm('Qoldiq yozuvi o‘chirilsinmi?')) return
  try {
    await deleteStock(s.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
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
