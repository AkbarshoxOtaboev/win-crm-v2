<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Qoldiq" />
    <div class="mb-4 flex flex-wrap gap-2">
      <button type="button" class="btn" @click="showTransfer = true">+ Transfer</button>
      <input v-model="search" type="search" placeholder="Qidiruv..." class="field sm:w-56" />
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Ombor qoldiqlari</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Mahsulot</th>
              <th class="th">Ombor</th>
              <th class="th">Miqdor</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="5" class="empty">Qoldiq yo‘q</td></tr>
            <tr v-for="s in filtered" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ s.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ s.goodsName || s.goodsId }}</td>
              <td class="td">{{ s.warehouseName || s.warehouseId }}</td>
              <td class="td">{{ s.count }}</td>
              <td class="td text-right">
                <RowActions :edit="false" @delete="onDelete(s)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="showTransfer" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4" @click.self="showTransfer = false">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">Stock transfer</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onTransfer">
          <div>
            <label class="lbl">Mahsulot *</label>
            <select v-model.number="transfer.goodsId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="g in goodsOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">Qayerdan *</label>
            <select v-model.number="transfer.fromWarehouseId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">Qayerga *</label>
            <select v-model.number="transfer.toWarehouseId" required class="field">
              <option :value="0" disabled>Tanlang</option>
              <option v-for="w in warehouses" :key="'t-' + w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </div>
          <div>
            <label class="lbl">Miqdor *</label>
            <input v-model.number="transfer.count" type="number" min="0.01" step="0.01" required class="field" />
          </div>
          <div>
            <label class="lbl">Izoh</label>
            <input v-model="transfer.comment" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="showTransfer = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : 'Transfer' }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import { createStockTransfer, deleteStock, fetchStocks, type Stock } from '@/api/stocks'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchGoods, type Goods } from '@/api/goods'
import { ApiError } from '@/api/http'

const items = ref<Stock[]>([])
const warehouses = ref<Warehouse[]>([])
const goodsOptions = ref<Goods[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const search = ref('')
const showTransfer = ref(false)

const transfer = reactive({
  goodsId: 0,
  fromWarehouseId: 0,
  toWarehouseId: 0,
  count: 1,
  comment: '',
})

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return items.value
  return items.value.filter((s) =>
    [s.goodsName, s.warehouseName].filter(Boolean).some((v) => String(v).toLowerCase().includes(q)),
  )
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const [stockRes, whRes, goodsRes] = await Promise.all([
      fetchStocks(),
      fetchWarehouses(),
      fetchGoods(),
    ])
    items.value = stockRes.data || []
    warehouses.value = whRes.data || []
    goodsOptions.value = goodsRes.data || []
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'Yuklashda xatolik'
  } finally {
    loading.value = false
  }
}

async function onTransfer() {
  saving.value = true
  formError.value = null
  try {
    await createStockTransfer({
      goodsId: transfer.goodsId,
      fromWarehouseId: transfer.fromWarehouseId,
      toWarehouseId: transfer.toWarehouseId,
      count: transfer.count,
      comment: transfer.comment || undefined,
    })
    showTransfer.value = false
    await load()
  } catch (e) {
    formError.value = e instanceof ApiError ? e.message : 'Transfer xatosi'
  } finally {
    saving.value = false
  }
}

async function onDelete(s: Stock) {
  if (!confirm('Qoldiq yozuvi o‘chirilsinmi?')) return
  try {
    await deleteStock(s.id)
    await load()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : 'O‘chirishda xatolik'
  }
}

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
</style>
