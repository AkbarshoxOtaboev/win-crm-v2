<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Transferlar" />
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <button type="button" class="btn ms-auto" @click="showTransfer = true">+ Transfer</button>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">Mahsulot</th>
            <th class="th">Dan</th>
            <th class="th">Ga</th>
            <th class="th">Soni</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="empty">Yuklanmoqda...</td></tr>
          <tr v-else-if="transfers.length === 0"><td colspan="5" class="empty">Transfer yo‘q</td></tr>
          <tr v-for="t in transfers" :key="t.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ t.id }}</td>
            <td class="td">{{ t.goodsName || t.goodsId }}</td>
            <td class="td">{{ t.fromWarehouseName || t.fromWarehouseId }}</td>
            <td class="td">{{ t.toWarehouseName || t.toWarehouseId }}</td>
            <td class="td">{{ t.count }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showTransfer" class="overlay" @click.self="showTransfer = false">
      <div class="modal">
        <h3 class="mb-4 text-lg font-semibold">Stock transfer</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onTransfer">
          <select v-model.number="transfer.goodsId" required class="field">
            <option :value="0" disabled>Mahsulot</option>
            <option v-for="g in goodsOptions" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
          <select v-model.number="transfer.fromWarehouseId" required class="field">
            <option :value="0" disabled>Qayerdan</option>
            <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
          <select v-model.number="transfer.toWarehouseId" required class="field">
            <option :value="0" disabled>Qayerga</option>
            <option v-for="w in warehouses" :key="'t' + w.id" :value="w.id">{{ w.name }}</option>
          </select>
          <input v-model.number="transfer.count" type="number" min="0.01" step="0.01" required class="field" />
          <input v-model="transfer.comment" class="field" placeholder="Izoh" />
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="showTransfer = false">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">Transfer</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  createStockTransfer,
  fetchStockTransfers,
  type StockTransfer,
} from '@/api/stocks'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchGoods, type Goods } from '@/api/goods'
import { formatApiError } from '@/api/http'

const transfers = ref<StockTransfer[]>([])
const warehouses = ref<Warehouse[]>([])
const goodsOptions = ref<Goods[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const showTransfer = ref(false)
const transfer = reactive({
  goodsId: 0,
  fromWarehouseId: 0,
  toWarehouseId: 0,
  count: 1,
  comment: '',
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const [whRes, goodsRes, trRes] = await Promise.all([
      fetchWarehouses(),
      fetchGoods(),
      fetchStockTransfers(),
    ])
    warehouses.value = whRes.data || []
    goodsOptions.value = goodsRes.data || []
    transfers.value = trRes.data || []
  } catch (e) {
    error.value = formatApiError(e)
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
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
