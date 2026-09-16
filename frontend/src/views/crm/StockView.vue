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
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Mahsulot</th>
              <th class="th">Mahsulot turi</th>
              <th class="th">Ombor</th>
              <th class="th">O‘lchov birligi</th>
              <th class="th">Miqdor</th>
              <th class="th">Kv.m</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="8" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="items.length === 0"><td colspan="8" class="empty">Qoldiq yo‘q</td></tr>
            <tr v-for="s in items" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ s.id }}</td>
              <td class="td">
                <div>{{ s.goodsName || s.goodsId }}</div>
                <div v-if="isWindow(s) && s.width && s.height" class="hint">
                  {{ formatNum(s.width) }}×{{ formatNum(s.height) }} sm
                </div>
              </td>
              <td class="td">{{ goodsTypeLabel(s) }}</td>
              <td class="td">{{ s.warehouseName || s.warehouseId }}</td>
              <td class="td">{{ unitLabel(s) }}</td>
              <td class="td">{{ formatNum(pieceQty(s)) }}</td>
              <td class="td">{{ isWindow(s) ? formatNum(s.kvm ?? s.count) : '—' }}</td>
              <td class="td text-right"><RowActions :edit="false" @delete="onDelete(s)" /></td>
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
import Swal from 'sweetalert2'

const items = ref<Stock[]>([])
const warehouses = ref<Warehouse[]>([])
const goodsOptions = ref<Goods[]>([])
const warehouseFilter = ref(0)
const goodsFilter = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)

function isWindow(s: Stock) {
  return (s.goodsType || '').toUpperCase() === 'WINDOW'
}

function goodsTypeLabel(s: Stock) {
  const t = (s.goodsType || '').toUpperCase()
  if (t === 'WINDOW') return 'Oyna'
  if (t === 'SERVICE') return 'Xizmat'
  if (t === 'PRODUCT') return 'Tovar'
  return s.goodsType || '—'
}

function unitLabel(s: Stock) {
  return s.unitTypeName || 'dona'
}

function pieceQty(s: Stock) {
  if (s.pieceCount != null) return Number(s.pieceCount)
  return Number(s.count || 0)
}

function formatNum(v?: number | null) {
  if (v == null || Number.isNaN(Number(v))) return '—'
  return new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 4 }).format(Number(v))
}

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
  const result = await Swal.fire({
    title: 'Qoldiqni o‘chirish',
    text: `"${s.goodsName || s.goodsId}" qoldiq yozuvi o‘chirilsinmi?`,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: 'Ha, o‘chirish',
    cancelButtonText: 'Bekor qilish',
    confirmButtonColor: '#dc2626',
    cancelButtonColor: '#98a2b3',
  })
  if (!result.isConfirmed) return

  try {
    await deleteStock(s.id)
    await load()
    await Swal.fire({
      title: 'O‘chirildi',
      text: 'Qoldiq yozuvi muvaffaqiyatli o‘chirildi',
      icon: 'success',
      confirmButtonColor: '#465fff',
      timer: 2000,
      showConfirmButton: false,
    })
  } catch (e) {
    error.value = formatApiError(e)
    await Swal.fire({
      title: 'Xatolik',
      text: formatApiError(e),
      icon: 'error',
      confirmButtonColor: '#465fff',
    })
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.hint { margin-top: 0.15rem; font-size: 0.75rem; color: #9ca3af; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
</style>
