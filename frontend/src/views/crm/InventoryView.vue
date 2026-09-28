<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.inventory')" />
    <div class="mb-4 flex gap-2">
      <select v-model.number="startWarehouseId" class="field w-56">
        <option :value="0" disabled>{{ t('common.warehouse') }}</option>
        <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
      </select>
      <input v-model="startComment" class="field w-56" :placeholder="t('common.comment')" />
      <button type="button" class="btn" :disabled="writeBlocked" @click="onStart">{{ t('inventory.start') }}</button>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="card mb-4">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100">
            <th class="th">#</th><th class="th">{{ t('common.warehouse') }}</th><th class="th">{{ t('common.status') }}</th><th class="th">{{ t('common.date') }}</th><th class="th text-right">{{ t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="empty">{{ t('common.loading') }}</td></tr>
          <tr v-for="c in checks" :key="c.id" class="border-b border-gray-100">
            <td class="td"><button type="button" class="text-brand-500" @click="openCheck(c.id)">#{{ c.id }}</button></td>
            <td class="td">{{ c.warehouseName || c.warehouseId }}</td>
            <td class="td">{{ c.checkStatus }}</td>
            <td class="td">{{ formatDate(c.createdAt) }}</td>
            <td class="td text-right">
              <div class="inline-flex items-center justify-end gap-1.5">
                <button v-if="c.checkStatus === 'IN_PROGRESS'" type="button" class="ghost" @click="onConfirm(c.id)">{{ t('common.confirm') }}</button>
                <button v-if="c.checkStatus === 'IN_PROGRESS'" type="button" class="ghost" @click="onCancel(c.id)">{{ t('common.cancel') }}</button>
                <RowActions :edit="false" @delete="onDelete(c.id)" />
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="current" class="card">
      <div class="head"><h3 class="title">{{ t('inventory.checkTitle', { id: current.id, status: current.checkStatus }) }}</h3></div>
      <table class="min-w-full">
        <thead><tr class="border-b border-gray-100"><th class="th">{{ t('common.product') }}</th><th class="th">{{ t('inventory.system') }}</th><th class="th">{{ t('inventory.actual') }}</th><th class="th">{{ t('inventory.difference') }}</th></tr></thead>
        <tbody>
          <tr v-for="it in current.items || []" :key="it.id" class="border-b border-gray-100">
            <td class="td">{{ it.goodsName || it.goodsId }}</td>
            <td class="td">{{ it.systemCount }}</td>
            <td class="td">
              <input
                :value="it.actualCount"
                type="number"
                class="field w-28"
                :disabled="current.checkStatus !== 'IN_PROGRESS'"
                @change="onActual(it.id, Number(($event.target as HTMLInputElement).value))"
              />
            </td>
            <td class="td">{{ it.difference }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  cancelInventoryCheck,
  confirmInventoryCheck,
  deleteInventoryCheck,
  fetchInventoryCheck,
  fetchInventoryChecks,
  startInventoryCheck,
  updateInventoryCheckItem,
  type InventoryCheck,
} from '@/api/inventory'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const checks = ref<InventoryCheck[]>([])
const current = ref<InventoryCheck | null>(null)
const warehouses = ref<Warehouse[]>([])
const startWarehouseId = ref(0)
const startComment = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    const [c, w] = await Promise.all([fetchInventoryChecks(), fetchWarehouses()])
    checks.value = c.data || []
    warehouses.value = w.data || []
    if (!startWarehouseId.value) startWarehouseId.value = warehouses.value[0]?.id || 0
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function openCheck(id: number) {
  try {
    const res = await fetchInventoryCheck(id)
    current.value = res.data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onStart() {
  try {
    await startInventoryCheck(startWarehouseId.value, startComment.value || undefined)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onActual(itemId: number, actualCount: number) {
  if (!current.value) return
  try {
    const res = await updateInventoryCheckItem(current.value.id, itemId, actualCount)
    current.value = res.data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onConfirm(id: number) {
  if (!confirm(t('inventory.confirmApply'))) return
  try {
    await confirmInventoryCheck(id)
    await load()
    await openCheck(id)
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onCancel(id: number) {
  try {
    await cancelInventoryCheck(id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onDelete(id: number) {
  if (!confirm(t('common.deleteConfirm'))) return
  try {
    await deleteInventoryCheck(id)
    if (current.value?.id === id) current.value = null
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; }
.head { padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; }
.ghost { height: 2.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.75rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; }
</style>
