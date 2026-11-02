<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('stockTransfers.createTitle')" />
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card mb-4 p-4">
      <div class="grid gap-3 md:grid-cols-3">
        <label class="lbl">
          {{ t('stockTransfers.fromPlaceholder') }} *
          <select v-model.number="fromWarehouseId" class="field" @change="onSourceChange">
            <option :value="0" disabled>{{ t('common.select') }}</option>
            <option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </label>
        <label class="lbl">
          {{ t('stockTransfers.toPlaceholder') }} *
          <select v-model.number="toWarehouseId" class="field">
            <option :value="0" disabled>{{ t('common.select') }}</option>
            <optgroup v-for="g in targetGroups" :key="g.key" :label="g.label">
              <option v-for="w in g.items" :key="w.id" :value="w.id" :disabled="w.id === fromWarehouseId">
                {{ w.name }}
              </option>
            </optgroup>
          </select>
        </label>
        <label class="lbl">
          {{ t('stockTransfers.goodsType') }} *
          <select v-model="goodsType" class="field" @change="resetQty">
            <option value="" disabled>{{ t('common.select') }}</option>
            <option v-for="type in GOODS_TYPES" :key="type" :value="type">{{ t(`goods.types.${type}`) }}</option>
          </select>
        </label>
      </div>
      <div v-if="crossFilialTarget" class="info mt-3">
        {{ t('stockTransfers.crossFilialNotice', { filial: crossFilialTarget }) }}
      </div>
      <label class="lbl mt-3">
        {{ t('common.comment') }}
        <input v-model="comment" class="field" maxlength="500" />
      </label>
    </div>

    <div v-if="!ready" class="card empty">{{ t('stockTransfers.pickFirst') }}</div>
    <div v-else class="card overflow-x-auto">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">{{ t('common.product') }}</th>
            <th class="th">{{ t('stockTransfers.available') }}</th>
            <th class="th">{{ t('stock.kvm') }}</th>
            <th class="th">{{ t('stockTransfers.moveQty') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="5" class="empty">{{ t('common.loading') }}</td></tr>
          <tr v-else-if="rows.length === 0"><td colspan="5" class="empty">{{ t('stockTransfers.noStock') }}</td></tr>
          <tr v-for="s in rows" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
            <td class="td">{{ s.goodsId }}</td>
            <td class="td font-medium text-gray-800 dark:text-white/90">
              <div>{{ s.goodsName }}</div>
              <div v-if="isWindow(s) && s.width && s.height" class="hint">{{ qtyText(s.width) }}×{{ qtyText(s.height) }} sm</div>
            </td>
            <td class="td font-semibold">{{ qtyText(availableQty(s)) }} {{ unitOf(s) }}</td>
            <td class="td">{{ kvmOf(s) == null ? '—' : qtyText(kvmOf(s)) }}</td>
            <td class="td">
              <input
                class="qty"
                :class="{ bad: over(s) }"
                type="number"
                min="0"
                step="any"
                :max="availableQty(s)"
                :value="qtyOf(s.goodsId)"
                @input="onQtyInput(s.goodsId, $event)"
              />
              <div v-if="over(s)" class="hint bad-text">{{ t('stockTransfers.notEnough', { available: qtyText(availableQty(s)) }) }}</div>
              <div v-else-if="plannedKvm(s) != null" class="hint">{{ t('stockTransfers.kvmMoved', { kvm: qtyText(plannedKvm(s)) }) }}</div>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="flex flex-wrap items-center justify-end gap-2 border-t border-gray-100 p-4 dark:border-gray-800">
        <span class="me-auto text-sm text-gray-500">{{ t('stockTransfers.selectedLines', { n: selected.length }) }}</span>
        <button type="button" class="ghost" @click="router.push('/stock/transfers')">{{ t('common.cancel') }}</button>
        <button type="button" class="btn" :disabled="saving || writeBlocked || !selected.length" @click="submit">
          {{ t('stockTransfers.submit') }}
        </button>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import Swal from 'sweetalert2'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  createStockTransfer,
  fetchStocks,
  fetchTransferTargetWarehouses,
  type Stock,
  type TransferTargetWarehouse,
} from '@/api/stocks'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'

const GOODS_TYPES = ['WINDOW', 'PRODUCT', 'SERVICE'] as const

const { t } = useI18n()
const router = useRouter()
const { auth, writeBlocked } = useFilialScope()
const warehouses = ref<Warehouse[]>([])
const stocks = ref<Stock[]>([])
const targets = ref<TransferTargetWarehouse[]>([])
const fromWarehouseId = ref(0)
const toWarehouseId = ref(0)
const goodsType = ref('')
const comment = ref('')
const qty = ref<Record<number, number>>({})
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

const ready = computed(() => fromWarehouseId.value > 0 && toWarehouseId.value > 0 && fromWarehouseId.value !== toWarehouseId.value && !!goodsType.value)

function isWindow(s: Stock) {
  return (s.goodsType || '').toUpperCase() === 'WINDOW'
}

function qtyText(n?: number | null) {
  if (n == null || Number.isNaN(Number(n))) return '—'
  const rounded = Math.round(Number(n) * 10000) / 10000
  return Number.isInteger(rounded) ? String(rounded) : String(rounded)
}

function pieceQty(s: Stock) {
  if (s.pieceCount != null && Number(s.pieceCount) > 0) return Number(s.pieceCount)
  const w = Number(s.width || 0)
  const h = Number(s.height || 0)
  const kvm = Number(s.count || 0)
  if (w > 0 && h > 0 && kvm > 0) return (kvm * 10000) / (w * h)
  return Number(s.pieceCount || 0)
}

function availableQty(s: Stock) {
  return isWindow(s) ? pieceQty(s) : Number(s.count || 0)
}

function unitOf(s: Stock) {
  return isWindow(s) ? t('stockTransfers.pieceUnit') : s.unitTypeName || ''
}

function kvmOf(s: Stock) {
  if (!isWindow(s)) return null
  return Number(s.kvm ?? s.count ?? 0)
}

function qtyOf(goodsId?: number) {
  if (!goodsId) return 0
  return qty.value[goodsId] ?? 0
}

function setQty(goodsId: number | undefined, raw: string) {
  if (!goodsId) return
  const n = raw.trim() === '' ? 0 : Number(raw)
  qty.value = { ...qty.value, [goodsId]: Number.isFinite(n) && n > 0 ? n : 0 }
}

function onQtyInput(goodsId: number | undefined, event: Event) {
  setQty(goodsId, (event.target as HTMLInputElement).value)
}

function plannedKvm(s: Stock) {
  const pieces = qtyOf(s.goodsId)
  if (!isWindow(s) || pieces <= 0) return null
  const w = Number(s.width || 0)
  const h = Number(s.height || 0)
  if (w <= 0 || h <= 0) return null
  return (pieces * w * h) / 10000
}

function over(s: Stock) {
  return qtyOf(s.goodsId) > availableQty(s)
}

function resetQty() {
  qty.value = {}
}

function onSourceChange() {
  if (toWarehouseId.value === fromWarehouseId.value) toWarehouseId.value = 0
  resetQty()
}

const rows = computed(() =>
  stocks.value
    .filter((s) => s.warehouseId === fromWarehouseId.value && (s.goodsType || '').toUpperCase() === goodsType.value && Number(s.count || 0) > 0)
    .sort((a, b) => (a.goodsId || 0) - (b.goodsId || 0) || a.id - b.id),
)

const selected = computed(() => rows.value.filter((s) => !!s.goodsId && qtyOf(s.goodsId) > 0))

const sourceFilialId = computed(() => {
  const own = targets.value.find((w) => w.id === fromWarehouseId.value)
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
    const label = own ? t('stockTransfers.ownFilial') : t('stockTransfers.otherFilial', { name: w.filialName || '—' })
    if (!groups.has(key)) groups.set(key, { key, label, items: [] })
    groups.get(key)!.items.push(w)
  }
  return [...groups.values()].sort((a, b) => (a.key === 'own' ? -1 : b.key === 'own' ? 1 : a.label.localeCompare(b.label)))
})

const crossFilialTarget = computed(() => {
  const w = targets.value.find((x) => x.id === toWarehouseId.value)
  if (!w || w.filialId === sourceFilialId.value) return null
  return w.filialName || String(w.filialId)
})

watch(fromWarehouseId, resetQty)
watch(goodsType, resetQty)

async function load() {
  loading.value = true
  error.value = null
  try {
    const [whRes, stockRes] = await Promise.all([fetchWarehouses(), fetchStocks()])
    warehouses.value = whRes.data || []
    stocks.value = stockRes.data || []
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

async function submit() {
  error.value = null
  const lines = selected.value
  if (!lines.length) {
    error.value = t('stockTransfers.noneSelected')
    return
  }
  const tooMuch = lines.find((s) => over(s))
  if (tooMuch) {
    error.value = t('stockTransfers.notEnough', { available: qtyText(availableQty(tooMuch)) })
    return
  }
  saving.value = true
  try {
    for (const s of lines) {
      if (!s.goodsId) continue
      await createStockTransfer({
        goodsId: s.goodsId,
        fromWarehouseId: fromWarehouseId.value,
        toWarehouseId: toWarehouseId.value,
        count: qtyOf(s.goodsId),
        comment: comment.value.trim() || undefined,
      })
      if (s.goodsId) qty.value = { ...qty.value, [s.goodsId]: 0 }
    }
    await Swal.fire({ icon: 'success', title: t('stockTransfers.sent'), timer: 1400, showConfirmButton: false })
    await router.push('/stock/transfers')
  } catch (e) {
    error.value = formatApiError(e)
    await load()
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
.empty { padding: 2rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.hint { margin-top: 0.2rem; font-size: 0.75rem; font-weight: 400; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding-inline: 0.75rem; font-size: 0.875rem; color: #1f2937; }
.qty { height: 2.25rem; width: 6.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding-inline: 0.6rem; text-align: end; font-size: 0.875rem; color: #1f2937; }
.qty.bad { border-color: #dc2626; }
.bad-text { color: #dc2626; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.info { border-radius: 0.5rem; border: 1px solid #bfdbfe; background: #eff6ff; padding: 0.6rem 0.75rem; font-size: 0.8125rem; color: #1d4ed8; }
.dark .card { border-color: #1f2937; background: rgba(255,255,255,.03); }
.dark .th, .dark .empty, .dark .lbl, .dark .hint { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .field, .dark .qty { border-color: #374151; color: #e5e7eb; }
.dark .field option, .dark .field optgroup { background: #111827; }
.dark .ghost { border-color: #374151; color: #d1d5db; }
.dark .err { border-color: rgba(239,68,68,.3); background: rgba(239,68,68,.1); color: #fca5a5; }
.dark .info { border-color: rgba(59,130,246,.3); background: rgba(59,130,246,.1); color: #93c5fd; }
</style>
