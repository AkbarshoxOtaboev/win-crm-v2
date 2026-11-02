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
        @click="router.push('/stock/transfers/new')"
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
            <th class="th">{{ t('stockTransfers.movementColumn') }}</th>
            <th class="th">{{ t('common.comment') }}</th>
            <th v-if="auth.can('STOCK_EDIT')" class="th"></th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td :colspan="colspan" class="empty">{{ t('common.loading') }}</td></tr>
          <tr v-else-if="visibleTransfers.length === 0"><td :colspan="colspan" class="empty">{{ t('stockTransfers.empty') }}</td></tr>
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
            <td class="td font-semibold">{{ shownQty(tr) }}</td>
            <td class="td">
              <span class="badge" :class="`badge-${(tr.direction || 'INTERNAL').toLowerCase()}`">
                {{ t(`stockTransfers.direction.${tr.direction || 'INTERNAL'}`) }}
              </span>
            </td>
            <td class="td">
              <span class="badge" :class="tr.movement === 'RETURN' ? 'badge-return' : 'badge-transfer'">
                {{ t(tr.movement === 'RETURN' ? 'stockTransfers.movement.RETURN' : 'stockTransfers.movement.TRANSFER') }}
              </span>
            </td>
            <td class="td">
              <div>{{ tr.comment || '—' }}</div>
              <div v-if="tr.createdUsername" class="sub-line">{{ tr.createdUsername }}</div>
            </td>
            <td v-if="auth.can('STOCK_EDIT')" class="td">
              <button
                type="button"
                class="return-btn"
                :disabled="writeBlocked || returningId === tr.id || !canReturn(tr)"
                :title="canReturn(tr) ? t('stockTransfers.return') : t('stockTransfers.returnLocked')"
                @click="onReturn(tr)"
              >
                {{ t('stockTransfers.return') }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Search } from 'lucide-vue-next'
import Swal from 'sweetalert2'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  returnStockTransfer,
  fetchStockTransfers,
  type StockTransfer,
} from '@/api/stocks'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate } from '@/utils/format'

type DirectionFilter = 'ALL' | 'OUTGOING' | 'INCOMING' | 'INTERNAL'

const { t } = useI18n()
const router = useRouter()
const { auth, writeBlocked } = useFilialScope()
const transfers = ref<StockTransfer[]>([])
const loading = ref(false)
const returningId = ref<number | null>(null)
const error = ref<string | null>(null)
const search = ref('')
const directionFilter = ref<DirectionFilter>('ALL')
const directionFilters: DirectionFilter[] = ['ALL', 'OUTGOING', 'INCOMING', 'INTERNAL']
const colspan = computed(() => (auth.can('STOCK_EDIT') ? 10 : 9))

function canReturn(tr: StockTransfer) {
  return tr.movement !== 'RETURN' && !tr.returned
}

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

function qtyText(n?: number | null) {
  if (n == null || Number.isNaN(Number(n))) return '—'
  const rounded = Math.round(Number(n) * 10000) / 10000
  return Number.isInteger(rounded) ? String(rounded) : String(rounded)
}

function shownQty(tr: StockTransfer) {
  const n = tr.pieceCount != null ? tr.pieceCount : tr.count
  return tr.pieceCount != null ? `${qtyText(n)} ${t('stockTransfers.pieceUnit')}` : qtyText(n)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const trRes = await fetchStockTransfers()
    transfers.value = trRes.data || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function onReturn(tr: StockTransfer) {
  if (!canReturn(tr)) return
  const name = tr.direction === 'INCOMING' ? tr.toGoodsName || tr.goodsName : tr.goodsName || tr.goodsId
  const result = await Swal.fire({
    title: t('stockTransfers.returnTitle'),
    text: t('stockTransfers.returnText', {
      name,
      count: shownQty(tr),
      warehouse: tr.fromWarehouseName || tr.fromWarehouseId,
    }),
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: t('stockTransfers.return'),
    cancelButtonText: t('common.cancel'),
    confirmButtonColor: '#465fff',
    cancelButtonColor: '#98a2b3',
  })
  if (!result.isConfirmed) return
  returningId.value = tr.id
  error.value = null
  try {
    await returnStockTransfer(tr.id)
    await load()
    await Swal.fire({
      icon: 'success',
      title: t('stockTransfers.returned'),
      timer: 1400,
      showConfirmButton: false,
    })
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    returningId.value = null
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
.return-btn { height: 2rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.8125rem; color: #374151; white-space: nowrap; }
.return-btn:disabled { opacity: 0.5; cursor: not-allowed; }
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
.badge-transfer { background: #eff6ff; color: #1d4ed8; }
.badge-return { background: #f5f3ff; color: #6d28d9; }
.dark .card { border-color: #1f2937; background: rgba(255,255,255,.03); }
.dark .th, .dark .empty, .dark .lbl, .dark .hint { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .sub-line { color: #6b7280; }
.dark .field { border-color: #374151; color: #e5e7eb; }
.dark .field:disabled { background: #1f2937; color: #6b7280; }
.dark .field option, .dark .field optgroup { background: #111827; }
.dark .ghost { border-color: #374151; color: #d1d5db; }
.dark .return-btn { border-color: #374151; color: #d1d5db; }
.dark .modal { background: #111827; }
.dark .err { border-color: rgba(239,68,68,.3); background: rgba(239,68,68,.1); color: #fca5a5; }
.dark .info { border-color: rgba(59,130,246,.3); background: rgba(59,130,246,.1); color: #93c5fd; }
.dark .chip { border-color: #374151; background: #111827; color: #d1d5db; }
.dark .chip.active { border-color: #465fff; background: #465fff; color: #fff; }
.dark .chip-count { background: rgba(255,255,255,.08); }
.dark .badge-internal { background: rgba(255,255,255,.06); color: #d1d5db; }
.dark .badge-outgoing { background: rgba(249,115,22,.12); color: #fdba74; }
.dark .badge-incoming { background: rgba(16,185,129,.12); color: #6ee7b7; }
.dark .badge-transfer { background: rgba(59,130,246,.12); color: #93c5fd; }
.dark .badge-return { background: rgba(109,40,217,.15); color: #c4b5fd; }
</style>
