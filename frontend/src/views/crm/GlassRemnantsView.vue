<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('glassRemnants.title')" />
    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">{{ t('glassRemnants.title') }}</h3>
          <p class="hint">{{ t('glassRemnants.hint') }}</p>
        </div>
        <div class="tools">
          <input v-model="search" type="search" :placeholder="t('common.search')" class="field search" />
          <span class="count">{{ t('glassRemnants.count', { n: filtered.length }) }}</span>
        </div>
      </div>

      <div v-if="error" class="err">{{ error }}</div>

      <div v-if="confirm" class="confirm">
        <p>{{ confirm.text }}</p>
        <div class="confirm-actions">
          <button type="button" class="ghost" :disabled="deleting" @click="confirm = null">{{ t('common.cancel') }}</button>
          <button type="button" class="danger" :disabled="deleting" @click="runDelete">
            {{ deleting ? '...' : t('glassRemnants.confirmDelete') }}
          </button>
        </div>
      </div>
      <div v-else-if="selectedIds.length > 0" class="bar">
        <span>{{ t('glassRemnants.selected', { n: selectedIds.length }) }}</span>
        <button type="button" class="danger" :disabled="deleting || writeBlocked" @click="askDeleteSelected">
          {{ t('glassRemnants.deleteSelected') }}
        </button>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th check">
                <input
                  ref="allBox"
                  type="checkbox"
                  :checked="allChecked"
                  :disabled="filtered.length === 0 || deleting"
                  :aria-label="t('glassRemnants.deleteSelected')"
                  @change="toggleAll"
                />
              </th>
              <th class="th">{{ t('glassRemnants.goods') }}</th>
              <th class="th">{{ t('glassRemnants.size') }}</th>
              <th class="th num">{{ t('glassRemnants.pieces') }}</th>
              <th class="th">{{ t('glassRemnants.warehouse') }}</th>
              <th class="th">{{ t('glassRemnants.order') }}</th>
              <th class="th text-right">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">{{ t('glassRemnants.empty') }}</td></tr>
            <tr
              v-for="row in filtered"
              :key="row.id"
              class="border-b border-gray-100 dark:border-gray-800"
              :class="{ picked: selectedIds.includes(row.id) }"
            >
              <td class="td check">
                <input
                  type="checkbox"
                  :checked="selectedIds.includes(row.id)"
                  :disabled="deleting"
                  :aria-label="sizeLabel(row)"
                  @change="toggleRow(row.id)"
                />
              </td>
              <td class="td name">{{ row.goodsName }}</td>
              <td class="td size">{{ sizeLabel(row) }}</td>
              <td class="td num">{{ row.pieceCount }}</td>
              <td class="td">{{ row.warehouseName || '—' }}</td>
              <td class="td">
                <router-link v-if="row.sourceSaleOrderId" :to="`/sales/${row.sourceSaleOrderId}`" class="link">
                  #{{ row.sourceSaleOrderId }}
                </router-link>
                <span v-else>—</span>
              </td>
              <td class="td text-right">
                <RowActions :edit="false" :remove="!writeBlocked" @delete="askDeleteOne(row)" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import { deleteGlassRemnant, deleteGlassRemnants, fetchGlassRemnants, type OptimizeRemnant } from '@/api/cutting'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'

const { t } = useI18n()
const { writeBlocked } = useFilialScope()
const rows = ref<OptimizeRemnant[]>([])
const search = ref('')
const loading = ref(true)
const deleting = ref(false)
const error = ref<string | null>(null)
const selectedIds = ref<number[]>([])
const allBox = ref<HTMLInputElement | null>(null)
const confirm = ref<{ ids: number[]; text: string } | null>(null)

const numberFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 })
function num(v?: number | null) {
  return numberFormat.format(Number(v || 0))
}
function sizeLabel(row: OptimizeRemnant) {
  return `${num(row.width)}×${num(row.height)}`
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  const list = [...rows.value].sort((a, b) => {
    const byWidth = Number(a.width) - Number(b.width)
    return byWidth !== 0 ? byWidth : Number(a.height) - Number(b.height)
  })
  if (!q) return list
  return list.filter((row) =>
    `${row.goodsName || ''} ${row.width} ${row.height} ${row.warehouseName || ''} ${row.sourceSaleOrderId || ''}`
      .toLowerCase()
      .includes(q),
  )
})

const allChecked = computed(
  () => filtered.value.length > 0 && filtered.value.every((row) => selectedIds.value.includes(row.id)),
)
const someChecked = computed(
  () => !allChecked.value && filtered.value.some((row) => selectedIds.value.includes(row.id)),
)

watch([allChecked, someChecked, allBox], () => {
  if (allBox.value) allBox.value.indeterminate = someChecked.value
})

function toggleAll() {
  const visible = filtered.value.map((row) => row.id)
  if (allChecked.value) {
    selectedIds.value = selectedIds.value.filter((id) => !visible.includes(id))
  } else {
    selectedIds.value = [...new Set([...selectedIds.value, ...visible])]
  }
}

function toggleRow(id: number) {
  selectedIds.value = selectedIds.value.includes(id)
    ? selectedIds.value.filter((item) => item !== id)
    : [...selectedIds.value, id]
}

function askDeleteOne(row: OptimizeRemnant) {
  if (writeBlocked.value || deleting.value) return
  confirm.value = {
    ids: [row.id],
    text: t('glassRemnants.deleteOne', { size: sizeLabel(row) }),
  }
}

function askDeleteSelected() {
  if (writeBlocked.value || deleting.value || selectedIds.value.length === 0) return
  confirm.value = {
    ids: [...selectedIds.value],
    text: t('glassRemnants.deleteMany', { n: selectedIds.value.length }),
  }
}

async function load() {
  const res = await fetchGlassRemnants()
  rows.value = res.data || []
  const alive = new Set(rows.value.map((row) => row.id))
  selectedIds.value = selectedIds.value.filter((id) => alive.has(id))
}

async function runDelete() {
  if (!confirm.value || deleting.value) return
  deleting.value = true
  error.value = null
  const ids = confirm.value.ids
  try {
    if (ids.length === 1) await deleteGlassRemnant(ids[0])
    else await deleteGlassRemnants(ids)
    confirm.value = null
    selectedIds.value = selectedIds.value.filter((id) => !ids.includes(id))
    await load()
  } catch (e) {
    error.value = formatApiError(e, t('common.deleteError'))
  } finally {
    deleting.value = false
  }
}

onMounted(async () => {
  try {
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.hint { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.head { display: flex; flex-wrap: wrap; gap: 0.75rem; justify-content: space-between; align-items: center; padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.tools { display: flex; align-items: center; gap: 0.75rem; }
.search { width: 14rem; }
.count { font-size: 0.8125rem; color: #6b7280; white-space: nowrap; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; vertical-align: middle; }
.th.check, .td.check { width: 2.75rem; padding-right: 0; }
.th.num, .td.num { text-align: right; width: 5rem; }
.name { font-weight: 500; color: #1f2937; }
.size { font-variant-numeric: tabular-nums; white-space: nowrap; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.err { margin: 1rem 1.25rem 0; border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.bar, .confirm { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 0.75rem; margin: 0; padding: 0.75rem 1.25rem; background: #f8fafc; border-bottom: 1px solid #f3f4f6; font-size: 0.875rem; color: #374151; }
.confirm { background: #fff7ed; }
.confirm p { margin: 0; }
.confirm-actions { display: flex; gap: 0.5rem; }
.danger, .ghost { height: 2.25rem; border-radius: 0.5rem; padding: 0 0.9rem; font-size: 0.875rem; font-weight: 500; }
.danger { background: #d92d20; color: #fff; }
.danger:disabled, .ghost:disabled { opacity: 0.6; }
.ghost { border: 1px solid #d1d5db; background: #fff; color: #374151; }
.link { color: #465fff; }
tr.picked { background: #f5f8ff; }
input[type='checkbox'] { width: 1rem; height: 1rem; accent-color: #465fff; }
:global(.dark) .card { background: rgba(255, 255, 255, 0.03); border-color: #1f2937; }
:global(.dark) .title, :global(.dark) .name { color: rgba(255, 255, 255, 0.9); }
:global(.dark) .hint, :global(.dark) .count, :global(.dark) .th, :global(.dark) .empty { color: #9ca3af; }
:global(.dark) .td { color: #d1d5db; }
:global(.dark) .head { border-color: #1f2937; }
:global(.dark) .field, :global(.dark) .ghost { border-color: #374151; color: #e5e7eb; background: transparent; }
:global(.dark) .bar { background: transparent; border-color: #1f2937; color: #e5e7eb; }
:global(.dark) .confirm { background: rgba(217, 45, 32, 0.12); color: #e5e7eb; }
:global(.dark) tr.picked { background: rgba(70, 95, 255, 0.12); }
</style>
