<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('cuttingOptimize.title')" />
    <div class="mb-4 flex flex-wrap items-center gap-2">
      <router-link :to="`/sales/${orderId}`" class="ghost">{{ t('cuttingOptimize.backToOrder') }}</router-link>
    </div>
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <p v-if="plan" class="mb-4 text-sm text-gray-500">
      {{ t('cuttingOptimize.order', { id: plan.saleOrderId }) }}
      <template v-if="plan.warehouseName"> · {{ plan.warehouseName }}</template>
    </p>
    <p class="mb-4 text-sm text-gray-500">{{ t('cuttingOptimize.subtitle') }}</p>

    <div v-if="plan && rows.length === 0" class="card p-5 text-sm text-gray-500">
      {{ t('cuttingOptimize.noWindows') }}
    </div>

    <div v-for="row in rows" :key="row.itemId" class="card mb-4 p-5">
      <div class="mb-3 flex flex-wrap items-baseline justify-between gap-2">
        <h3 class="text-base font-semibold text-gray-800 dark:text-white/90">{{ row.goodsName }}</h3>
        <span class="text-sm text-gray-500">
          {{ t('cuttingOptimize.size') }} {{ row.width }}×{{ row.height }} · {{ t('cuttingOptimize.pieces') }} {{ row.count }}
        </span>
      </div>

      <div class="mb-3 flex flex-wrap items-end gap-3">
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.fromSheet') }}
          <input v-model.number="row.sheetQty" type="number" min="0" :max="row.count" class="field" :disabled="locked" />
        </label>
        <button v-if="!locked" type="button" class="ghost" @click="addPick(row)">{{ t('cuttingOptimize.addRemnant') }}</button>
      </div>

      <div v-for="(pick, idx) in row.picks" :key="idx" class="mb-2 flex flex-wrap items-end gap-2">
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.remnant') }}
          <select v-model.number="pick.remnantId" class="field" :disabled="locked">
            <option :value="0" disabled>{{ t('cuttingOptimize.remnant') }}</option>
            <option v-for="r in remnantsFor(row.goodsId)" :key="r.id" :value="r.id">
              {{ r.width }}×{{ r.height }} · {{ r.pieceCount }}
            </option>
          </select>
        </label>
        <label class="text-sm text-gray-600">
          {{ t('cuttingOptimize.pieces') }}
          <input v-model.number="pick.quantity" type="number" min="1" class="field w-24" :disabled="locked" />
        </label>
        <button v-if="!locked" type="button" class="ghost" @click="row.picks.splice(idx, 1)">×</button>
      </div>
      <p v-if="remnantsFor(row.goodsId).length === 0" class="text-xs text-gray-400">{{ t('cuttingOptimize.noRemnants') }}</p>
      <p class="mt-2 text-sm" :class="left(row) === 0 ? 'text-emerald-600' : 'text-amber-600'">
        {{ left(row) === 0 ? t('cuttingOptimize.ready') : t('cuttingOptimize.left', { n: left(row) }) }}
      </p>

      <CuttingPreview
        v-if="Number(row.sheetQty) > 0"
        :goods-id="row.goodsId"
        :piece-width="row.width"
        :piece-height="row.height"
        :quantity="Number(row.sheetQty)"
        readonly
        :title="t('cuttingOptimize.sheetPlan')"
        v-model:kerf="row.kerf"
        v-model:allow-rotate="row.allowRotate"
      />
      <template v-for="(pick, idx) in row.picks" :key="`plan-${row.itemId}-${idx}`">
        <CuttingPreview
          v-if="sheetOf(pick) && Number(pick.quantity) > 0"
          :goods-id="row.goodsId"
          :sheet-width="sheetOf(pick)!.width"
          :sheet-height="sheetOf(pick)!.height"
          :piece-width="row.width"
          :piece-height="row.height"
          :quantity="Number(pick.quantity)"
          readonly
          :title="t('cuttingOptimize.remnantPlan', { w: sheetOf(pick)!.width, h: sheetOf(pick)!.height })"
          v-model:kerf="row.kerf"
          v-model:allow-rotate="row.allowRotate"
        />
      </template>
    </div>

    <button
      v-if="rows.length && !locked"
      type="button"
      class="btn"
      :disabled="saving || !complete"
      @click="onSave"
    >
      {{ t('cuttingOptimize.confirm') }}
    </button>
    <p v-if="locked" class="text-sm text-emerald-600">{{ t('cuttingOptimize.saved') }}</p>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import CuttingPreview from '@/components/crm/CuttingPreview.vue'
import {
  confirmCuttingOptimize,
  fetchCuttingOptimize,
  type OptimizePlan,
  type OptimizeRemnant,
} from '@/api/cutting'
import { formatApiError } from '@/api/http'

interface Pick {
  remnantId: number
  quantity: number
  width?: number
  height?: number
}
interface Row {
  itemId: number
  goodsId: number
  goodsName: string
  width: number
  height: number
  count: number
  sheetQty: number
  picks: Pick[]
  kerf: number | ''
  allowRotate: boolean
}

const { t } = useI18n()
const route = useRoute()
const orderId = Number(route.params.id)
const plan = ref<OptimizePlan | null>(null)
const rows = ref<Row[]>([])
const remnants = ref<OptimizeRemnant[]>([])
const locked = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

function remnantsFor(goodsId: number) {
  return remnants.value.filter((r) => r.goodsId === goodsId)
}

function sheetOf(pick: Pick) {
  const live = remnants.value.find((r) => r.id === pick.remnantId)
  if (live) return { width: Number(live.width), height: Number(live.height) }
  if (pick.width && pick.height) return { width: Number(pick.width), height: Number(pick.height) }
  return null
}

function used(row: Row) {
  const fromRemnant = row.picks.reduce((sum, p) => sum + Number(p.quantity || 0), 0)
  return Number(row.sheetQty || 0) + fromRemnant
}

function left(row: Row) {
  return row.count - used(row)
}

const complete = computed(() => rows.value.length > 0 && rows.value.every((row) => left(row) === 0 && used(row) === row.count))

function addPick(row: Row) {
  const first = remnantsFor(row.goodsId)[0]
  row.picks.push({ remnantId: first?.id || 0, quantity: 1 })
}

async function load() {
  error.value = null
  try {
    const res = await fetchCuttingOptimize(orderId)
    plan.value = res.data || null
    remnants.value = res.data?.remnants || []
    locked.value = Boolean(res.data?.optimized)
    rows.value = (res.data?.lines || []).map((line) => ({
      itemId: line.itemId,
      goodsId: line.goodsId,
      goodsName: line.goodsName || '',
      width: Number(line.width),
      height: Number(line.height),
      count: line.count,
      sheetQty: line.sheetQuantity ?? line.count,
      picks: (line.picks || []).map((p) => ({
        remnantId: p.remnantId,
        quantity: p.quantity,
        width: p.width != null ? Number(p.width) : undefined,
        height: p.height != null ? Number(p.height) : undefined,
      })),
      kerf: line.kerfMm != null ? Number(line.kerfMm) : '',
      allowRotate: line.allowRotate !== false,
    }))
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onSave() {
  if (!complete.value || saving.value) return
  saving.value = true
  error.value = null
  try {
    await confirmCuttingOptimize(
      orderId,
      rows.value.map((row) => ({
        saleOrderItemId: row.itemId,
        sheetQuantity: Number(row.sheetQty || 0),
        remnants: row.picks
          .filter((p) => p.remnantId > 0 && p.quantity > 0)
          .map((p) => ({ remnantId: p.remnantId, quantity: p.quantity })),
      })),
    )
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.field {
  display: block;
  margin-top: 0.25rem;
  height: 2.5rem;
  min-width: 8rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem;
  font-size: 0.875rem;
}
.btn { height: 2.75rem; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; color: #fff; font-weight: 500; }
.btn:disabled { opacity: 0.6; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.9rem; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
</style>
