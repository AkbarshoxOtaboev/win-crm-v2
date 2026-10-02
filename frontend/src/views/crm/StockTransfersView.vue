<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('stockTransfers.breadcrumb')" />
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <button
        v-for="d in directionFilters"
        :key="d"
        type="button"
        class="chip"
        :class="{ active: directionFilter === d }"
        @click="directionFilter = d"
      >
        {{ t(`stockTransfers.direction.${d}`) }}
        <span class="chip-count">{{ countByDirection(d) }}</span>
      </button>
      <div class="relative ms-auto">
        <Search class="pointer-events-none absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
        <input v-model="search" type="search" class="field ps-9 sm:w-64" :placeholder="t('stockTransfers.searchPlaceholder')" />
      </div>
      <button
        v-if="auth.can('STOCK_EDIT')"
        type="button"
        class="btn"
        :disabled="writeBlocked"
        @click="openTransfer"
      >
        {{ t('stockTransfers.newTransfer') }}
      </button>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card overflow-x-auto">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">{{ t('common.date') }}</th>
            <th class="th">{{ t('common.product') }}</th>
            <th class="th">{{ t('common.from') }}</th>
            <th class="th">{{ t('stockTransfers.to') }}</th>
            <th class="th">{{ t('common.count') }}</th>
            <th class="th">{{ t('stockTransfers.type') }}</th>
            <th class="th">{{ t('common.comment') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="8" class="empty">{{ t('common.loading') }}</td></tr>
          <tr v-else-if="visibleTransfers.length === 0"><td colspan="8" class="empty">{{ t('stockTransfers.empty') }}</td></tr>
          <tr v-for="tr in visibleTransfers" :key="tr.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ tr.id }}</td>
            <td class="td whitespace-nowrap">{{ formatDate(tr.createdAt) }}</td>
            <td class="td font-medium text-gray-800 dark:text-white/90">
              {{ tr.direction === 'INCOMING' ? tr.toGoodsName || tr.goodsName : tr.goodsName || tr.goodsId }}
            </td>
            <td class="td">
              <div>{{ tr.fromWarehouseName || tr.fromWarehouseId }}</div>
              <div v-if="tr.direction !== 'INTERNAL' && tr.fromFilialName" class="sub-line">{{ tr.fromFilialName }}</div>
            </td>
            <td class="td">
              <div>{{ tr.toWarehouseName || tr.toWarehouseId }}</div>
              <div v-if="tr.direction !== 'INTERNAL' && tr.toFilialName" class="sub-line">{{ tr.toFilialName }}</div>
            </td>
            <td class="td font-semibold">{{ tr.count }}</td>
            <td class="td">
              <span class="badge" :class="`badge-${(tr.direction || 'INTERNAL').toLowerCase()}`">
                {{ t(`stockTransfers.direction.${tr.direction || 'INTERNAL'}`) }}
              </span>
            </td>
            <td class="td">
              <div>{{ tr.comment || '—' }}</div>
              <div v-if="tr.createdUsername" class="sub-line">{{ tr.createdUsername }}</div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showTransfer" class="overlay">
      <div class="modal">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('stockTransfers.modalTitle') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onTransfer">
          <label class="lbl">
            {{ t('stockTransfers.fromPlaceholder') }} *
            <select v-model.number="transfer.fromWarehouseId" required class="field" @change="transfer.goodsId = 0">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
            </select>
          </label>
          <label class="lbl">
            {{ t('common.product') }} *
            <select v-model.number="transfer.goodsId" required class="field" :disabled="!transfer.fromWarehouseId">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option v-for="s in sourceStocks" :key="s.goodsId" :value="s.goodsId">
                {{ s.goodsName }} — {{ s.count }} {{ s.unitTypeName || '' }}
              </option>
            </select>
            <span v-if="transfer.fromWarehouseId && sourceStocks.length === 0" class="hint">{{ t('stockTransfers.noStock') }}</span>
          </label>
          <label class="lbl">
            {{ t('stockTransfers.toPlaceholder') }} *
            <select v-model.number="transfer.toWarehouseId" required class="field">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <optgroup v-for="g in targetGroups" :key="g.key" :label="g.label">
                <option
                  v-for="w in g.items"
                  :key="w.id"
                  :value="w.id"
                  :disabled="w.id === transfer.fromWarehouseId"
                >
                  {{ w.name }}
                </option>
              </optgroup>
            </select>
          </label>
          <div v-if="crossFilialTarget" class="info">
            {{ t('stockTransfers.crossFilialNotice', { filial: crossFilialTarget }) }}
          </div>
          <label class="lbl">
            {{ t('common.count') }} *
            <input v-model.number="transfer.count" type="number" min="0.01" step="0.01" :max="available || undefined" required class="field" />
            <span v-if="transfer.goodsId" class="hint">{{ t('stockTransfers.available') }}: {{ available }}</span>
          </label>
          <label class="lbl">
            {{ t('common.comment') }}
            <input v-model="transfer.comment" class="field" maxlength="500" />
          </label>
          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="showTransfer = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving || writeBlocked">{{ t('stockTransfers.submit') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  createStockTransfer,
  fetchStocks,
  fetchStockTransfers,
  fetchTransferTargetWarehouses,
  type Stock,
  type StockTransfer,
  type TransferTargetWarehouse,
} from '@/api/stocks'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate } from '@/utils/format'

type DirectionFilter = 'ALL' | 'OUTGOING' | 'INCOMING' | 'INTERNAL'

const { t } = useI18n()
const { auth, writeBlocked } = useFilialScope()
const transfers = ref<StockTransfer[]>([])
const warehouses = ref<Warehouse[]>([])
const stocks = ref<Stock[]>([])
const targets = ref<TransferTargetWarehouse[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const showTransfer = ref(false)
const search = ref('')
const directionFilter = ref<DirectionFilter>('ALL')
const directionFilters: DirectionFilter[] = ['ALL', 'OUTGOING', 'INCOMING', 'INTERNAL']
const transfer = reactive({
  goodsId: 0,
  fromWarehouseId: 0,
  toWarehouseId: 0,
  count: 1,
  comment: '',
})

function countByDirection(d: DirectionFilter) {
  if (d === 'ALL') return transfers.value.length
  return transfers.value.filter((tr) => (tr.direction || 'INTERNAL') === d).length
}

const visibleTransfers = computed(() => {
  const q = search.value.trim().toLowerCase()
  return transfers.value.filter((tr) => {
    if (directionFilter.value !== 'ALL' && (tr.direction || 'INTERNAL') !== directionFilter.value) return false
    if (!q) return true
    return [
      tr.id,
      tr.goodsName,
      tr.toGoodsName,
      tr.fromWarehouseName,
      tr.toWarehouseName,
      tr.fromFilialName,
      tr.toFilialName,
      tr.comment,
      tr.createdUsername,
    ].some((v) => String(v ?? '').toLowerCase().includes(q))
  })
})

const sourceStocks = computed(() =>
  stocks.value
    .filter((s) => s.warehouseId === transfer.fromWarehouseId && Number(s.count || 0) > 0)
    .sort((a, b) => (a.goodsName || '').localeCompare(b.goodsName || '')),
)

const available = computed(() => {
  const s = sourceStocks.value.find((x) => x.goodsId === transfer.goodsId)
  return Number(s?.count || 0)
})

const sourceFilialId = computed(() => {
  const own = targets.value.find((w) => w.id === transfer.fromWarehouseId)
  return own?.filialId ?? auth.selectedFilialId ?? null
})

const targetGroups = computed(() => {
  const list = targets.value.length
    ? targets.value
    : warehouses.value.map((w) => ({ id: w.id, name: w.name, filialId: sourceFilialId.value, filialName: null }))
  const groups = new Map<string, { key: string; label: string; items: TransferTargetWarehouse[] }>()
  for (const w of list) {
    const own = w.filialId === sourceFilialId.value
    const key = own ? 'own' : String(w.filialId ?? 'none')
    const label = own
      ? t('stockTransfers.ownFilial')
      : t('stockTransfers.otherFilial', { name: w.filialName || '—' })
    if (!groups.has(key)) groups.set(key, { key, label, items: [] })
    groups.get(key)!.items.push(w)
  }
  return [...groups.values()].sort((a, b) => (a.key === 'own' ? -1 : b.key === 'own' ? 1 : a.label.localeCompare(b.label)))
})

const crossFilialTarget = computed(() => {
  const w = targets.value.find((x) => x.id === transfer.toWarehouseId)
  if (!w || w.filialId === sourceFilialId.value) return null
  return w.filialName || String(w.filialId)
})

function openTransfer() {
  formError.value = null
  transfer.goodsId = 0
  transfer.fromWarehouseId = warehouses.value.length === 1 ? warehouses.value[0].id : 0
  transfer.toWarehouseId = 0
  transfer.count = 1
  transfer.comment = ''
  showTransfer.value = true
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [whRes, stockRes, trRes] = await Promise.all([
      fetchWarehouses(),
      fetchStocks(),
      fetchStockTransfers(),
    ])
    warehouses.value = whRes.data || []
    stocks.value = stockRes.data || []
    transfers.value = trRes.data || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
  if (auth.can('STOCK_EDIT')) {
    try {
      const res = await fetchTransferTargetWarehouses()
      targets.value = res.data || []
    } catch {
      targets.value = []
    }
  }
}

async function onTransfer() {
  formError.value = null
  if (transfer.count > available.value) {
    formError.value = t('stockTransfers.notEnough', { available: available.value })
    return
  }
  saving.value = true
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
.th { padding: 0.75rem 1.25rem; text-align: start; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; vertical-align: top; }
.sub-line { margin-top: 0.15rem; font-size: 0.75rem; color: #9ca3af; }
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.hint { font-size: 0.75rem; font-weight: 400; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding-inline: 0.75rem; font-size: 0.875rem; color: #1f2937; }
.field.ps-9 { padding-inline-start: 2.25rem; }
.field:disabled { background: #f9fafb; color: #9ca3af; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.info { border-radius: 0.5rem; border: 1px solid #bfdbfe; background: #eff6ff; padding: 0.6rem 0.75rem; font-size: 0.8125rem; color: #1d4ed8; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 30rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
.chip { display: inline-flex; align-items: center; gap: 0.4rem; height: 2.25rem; border-radius: 9999px; border: 1px solid #e5e7eb; background: #fff; padding-inline: 0.9rem; font-size: 0.8125rem; font-weight: 500; color: #4b5563; }
.chip.active { border-color: #465fff; background: #465fff; color: #fff; }
.chip-count { border-radius: 9999px; background: rgba(0,0,0,.06); padding: 0 0.45rem; font-size: 0.75rem; }
.chip.active .chip-count { background: rgba(255,255,255,.25); }
.badge { display: inline-flex; border-radius: 9999px; padding: 0.15rem 0.6rem; font-size: 0.75rem; font-weight: 500; white-space: nowrap; }
.badge-internal { background: #f3f4f6; color: #4b5563; }
.badge-outgoing { background: #fff7ed; color: #c2410c; }
.badge-incoming { background: #ecfdf5; color: #047857; }
.dark .card { border-color: #1f2937; background: rgba(255,255,255,.03); }
.dark .th, .dark .empty, .dark .lbl, .dark .hint { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .sub-line { color: #6b7280; }
.dark .field { border-color: #374151; color: #e5e7eb; }
.dark .field:disabled { background: #1f2937; color: #6b7280; }
.dark .field option, .dark .field optgroup { background: #111827; }
.dark .ghost { border-color: #374151; color: #d1d5db; }
.dark .modal { background: #111827; }
.dark .err { border-color: rgba(239,68,68,.3); background: rgba(239,68,68,.1); color: #fca5a5; }
.dark .info { border-color: rgba(59,130,246,.3); background: rgba(59,130,246,.1); color: #93c5fd; }
.dark .chip { border-color: #374151; background: #111827; color: #d1d5db; }
.dark .chip.active { border-color: #465fff; background: #465fff; color: #fff; }
.dark .chip-count { background: rgba(255,255,255,.08); }
.dark .badge-internal { background: rgba(255,255,255,.06); color: #d1d5db; }
.dark .badge-outgoing { background: rgba(249,115,22,.12); color: #fdba74; }
.dark .badge-incoming { background: rgba(16,185,129,.12); color: #6ee7b7; }
</style>
