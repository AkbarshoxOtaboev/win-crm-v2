<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.finishedGoodsReceipts')" />

    <div class="page-head">
      <div>
        <h3 class="page-title">{{ t('finishedGoods.receiptsTitle') }}</h3>
        <p class="page-sub">{{ t('finishedGoods.receiptsSubtitle') }}</p>
      </div>
      <button
        type="button"
        class="btn primary"
        :disabled="writeBlocked || !auth.can('FINISHED_GOODS_RECEIPT_CREATE')"
        @click="openCreate"
      >
        <Plus class="h-4 w-4" />
        {{ t('finishedGoods.newReceipt') }}
      </button>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card overflow-x-auto">
      <table class="min-w-full">
        <thead>
          <tr class="border-b border-gray-100 dark:border-gray-800">
            <th class="th">#</th>
            <th class="th">{{ t('finishedGoods.documentDate') }}</th>
            <th class="th">{{ t('finishedGoods.warehouse') }}</th>
            <th class="th">{{ t('finishedGoods.workshop') }}</th>
            <th class="th">{{ t('common.status') }}</th>
            <th class="th">{{ t('finishedGoods.items') }}</th>
            <th class="th text-right">{{ t('common.actions') }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td colspan="7" class="empty">{{ t('common.loading') }}</td></tr>
          <tr v-else-if="rows.length === 0"><td colspan="7" class="empty">{{ t('finishedGoods.emptyReceipts') }}</td></tr>
          <tr v-for="row in rows" :key="row.id" class="row">
            <td class="td">{{ row.id }}</td>
            <td class="td whitespace-nowrap">{{ formatDate(row.documentDate) }}</td>
            <td class="td">{{ row.warehouseName || row.warehouseId }}</td>
            <td class="td">{{ row.workshopName || '—' }}</td>
            <td class="td">
              <span class="badge" :class="row.docStatus === 'POSTED' ? 'ok' : 'draft'">
                {{ row.docStatus === 'POSTED' ? t('finishedGoods.statusPosted') : t('finishedGoods.statusDraft') }}
              </span>
            </td>
            <td class="td">{{ row.items?.length || 0 }}</td>
            <td class="td text-right">
              <div class="actions">
                <button
                  v-if="row.docStatus === 'DRAFT' && auth.can('FINISHED_GOODS_RECEIPT_EDIT')"
                  type="button"
                  class="action"
                  :disabled="writeBlocked"
                  @click="openEdit(row)"
                >
                  {{ t('common.edit') }}
                </button>
                <button
                  v-if="row.docStatus === 'DRAFT' && (auth.can('FINISHED_GOODS_RECEIPT_CREATE') || auth.can('FINISHED_GOODS_RECEIPT_EDIT'))"
                  type="button"
                  class="action strong"
                  :disabled="writeBlocked || postingId === row.id"
                  @click="onPost(row)"
                >
                  {{ t('finishedGoods.post') }}
                </button>
                <button
                  v-if="row.docStatus === 'DRAFT' && auth.can('FINISHED_GOODS_RECEIPT_DELETE')"
                  type="button"
                  class="action danger"
                  :disabled="writeBlocked"
                  @click="onDelete(row)"
                >
                  {{ t('common.delete') }}
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <Teleport to="body">
      <div v-if="modalOpen" class="overlay" @mousedown.self="closeModal">
        <div
          class="sheet"
          role="dialog"
          aria-modal="true"
          :aria-label="editingId ? t('finishedGoods.editReceipt') : t('finishedGoods.newReceipt')"
        >
          <header class="sheet-head">
            <div class="min-w-0">
              <p class="sheet-kicker">{{ t('finishedGoods.receiptsTitle') }}</p>
              <h3 class="sheet-title">
                {{ editingId ? t('finishedGoods.editReceipt') : t('finishedGoods.newReceipt') }}
              </h3>
              <p class="sheet-sub">{{ t('finishedGoods.receiptModalHint') }}</p>
            </div>
            <button type="button" class="icon-btn" :aria-label="t('common.close')" @click="closeModal">
              <X class="h-5 w-5" />
            </button>
          </header>

          <form class="sheet-body" @submit.prevent="onSubmit">
            <div v-if="formError" class="err">{{ formError }}</div>

            <section class="block">
              <h4 class="block-title">{{ t('finishedGoods.docSection') }}</h4>
              <div class="grid-3">
                <label class="field-wrap">
                  <span class="lbl">{{ t('finishedGoods.warehouse') }} <em>*</em></span>
                  <SearchableSelect
                    v-model="form.warehouseId"
                    :options="warehouseOptions"
                    :placeholder="t('finishedGoods.selectWarehouse')"
                    :search-placeholder="t('common.search')"
                  />
                </label>
                <label class="field-wrap">
                  <span class="lbl">{{ t('finishedGoods.workshop') }}</span>
                  <SearchableSelect
                    v-model="form.workshopId"
                    :options="workshopOptions"
                    :placeholder="t('finishedGoods.selectWorkshop')"
                    :search-placeholder="t('common.search')"
                  />
                </label>
                <label class="field-wrap">
                  <span class="lbl">{{ t('finishedGoods.documentDate') }} <em>*</em></span>
                  <input v-model="form.documentDate" type="datetime-local" required class="field" />
                </label>
              </div>
              <label class="field-wrap mt-3">
                <span class="lbl">{{ t('finishedGoods.comment') }}</span>
                <input v-model="form.comment" class="field" :placeholder="t('finishedGoods.commentPlaceholder')" />
              </label>
            </section>

            <section class="block">
              <div class="block-head">
                <div>
                  <h4 class="block-title mb-0">{{ t('finishedGoods.items') }}</h4>
                  <p class="block-meta">{{ t('finishedGoods.linesCount', { n: filledLinesCount }) }}</p>
                </div>
                <button type="button" class="btn ghost" @click="addLine">
                  <Plus class="h-4 w-4" />
                  {{ t('finishedGoods.addItem') }}
                </button>
              </div>

              <div class="lines">
                <div class="lines-head">
                  <span>{{ t('finishedGoods.selectGoods') }}</span>
                  <span>{{ t('finishedGoods.count') }}</span>
                  <span>{{ t('finishedGoods.unitCost') }}</span>
                  <span></span>
                </div>

                <div v-if="form.items.length === 0" class="lines-empty">
                  {{ t('finishedGoods.noLinesYet') }}
                  <button type="button" class="action strong" @click="addLine">{{ t('finishedGoods.addItem') }}</button>
                </div>

                <div v-for="(line, idx) in form.items" :key="line.key" class="line">
                  <SearchableSelect
                    v-model="line.goodsId"
                    :options="productOptions"
                    :placeholder="t('finishedGoods.selectGoods')"
                    :search-placeholder="t('common.search')"
                    :limit="40"
                  />
                  <input
                    v-model.number="line.count"
                    type="number"
                    min="0.0001"
                    step="any"
                    required
                    class="field qty"
                    :placeholder="t('finishedGoods.count')"
                  />
                  <input
                    v-model.number="line.unitCost"
                    type="number"
                    min="0"
                    step="any"
                    class="field qty"
                    :placeholder="t('finishedGoods.unitCost')"
                  />
                  <button
                    type="button"
                    class="icon-btn danger"
                    :disabled="form.items.length <= 1"
                    :title="t('finishedGoods.removeLine')"
                    @click="removeLine(idx)"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </div>
            </section>
          </form>

          <footer class="sheet-foot">
            <button type="button" class="btn ghost" @click="closeModal">{{ t('common.cancel') }}</button>
            <button type="button" class="btn primary" :disabled="saving" @click="onSubmit">
              <Loader2 v-if="saving" class="h-4 w-4 animate-spin" />
              {{ saving ? t('common.saving') : t('finishedGoods.saveDraft') }}
            </button>
          </footer>
        </div>
      </div>
    </Teleport>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Loader2, Plus, Trash2, X } from 'lucide-vue-next'
import Swal from 'sweetalert2'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import SearchableSelect from '@/components/crm/SearchableSelect.vue'
import {
  createFinishedGoodsReceipt,
  deleteFinishedGoodsReceipt,
  fetchFinishedGoodsReceipts,
  postFinishedGoodsReceipt,
  updateFinishedGoodsReceipt,
  type FinishedGoodsReceipt,
} from '@/api/finishedGoods'
import { fetchGoods, type Goods } from '@/api/goods'
import { fetchWarehouses, type Warehouse } from '@/api/warehouses'
import { fetchWorkshops, type Workshop } from '@/api/workshops'
import { formatApiError } from '@/api/http'
import { useFilialScope } from '@/composables/useFilialScope'
import { formatDate } from '@/utils/format'

const { t } = useI18n()
const { auth, writeBlocked } = useFilialScope()

const rows = ref<FinishedGoodsReceipt[]>([])
const warehouses = ref<Warehouse[]>([])
const workshops = ref<Workshop[]>([])
const finishedProducts = ref<Goods[]>([])
const loading = ref(false)
const saving = ref(false)
const postingId = ref<number | null>(null)
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
let lineSeq = 0

const form = reactive({
  warehouseId: 0,
  workshopId: 0,
  documentDate: '',
  comment: '',
  items: [] as { key: number; goodsId: number; count: number; unitCost: number | null }[],
})

const warehouseOptions = computed(() =>
  warehouses.value.map((w) => ({ value: w.id, label: w.name })),
)
const workshopOptions = computed(() => [
  { value: 0, label: t('finishedGoods.workshopOptional') },
  ...workshops.value.map((w) => ({ value: w.id, label: w.name })),
])
const productOptions = computed(() =>
  finishedProducts.value.map((g) => ({
    value: g.id,
    label: g.name,
    searchText: `${g.name} ${g.barcode || ''} ${g.goodsGroupName || ''}`,
  })),
)
const filledLinesCount = computed(
  () => form.items.filter((i) => i.goodsId > 0 && Number(i.count) > 0).length,
)

function nowLocal() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}

function toIso(local: string) {
  return new Date(local).toISOString().slice(0, 19)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && modalOpen.value) closeModal()
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [list, wh, ws, goods] = await Promise.all([
      fetchFinishedGoodsReceipts(),
      fetchWarehouses(),
      fetchWorkshops(),
      fetchGoods(),
    ])
    rows.value = list.data || []
    warehouses.value = wh.data || []
    workshops.value = ws.data || []
    finishedProducts.value = (goods.data || []).filter(
      (g) => (g.type || '').toUpperCase() === 'FINISHED_PRODUCT',
    )
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    warehouseId: warehouses.value[0]?.id || 0,
    workshopId: 0,
    documentDate: nowLocal(),
    comment: '',
    items: [{ key: ++lineSeq, goodsId: 0, count: 1, unitCost: null }],
  })
}

function openCreate() {
  if (writeBlocked.value) return
  editingId.value = null
  formError.value = null
  resetForm()
  modalOpen.value = true
  document.body.style.overflow = 'hidden'
}

function openEdit(row: FinishedGoodsReceipt) {
  if (writeBlocked.value) return
  editingId.value = row.id
  formError.value = null
  const dt = row.documentDate ? new Date(row.documentDate) : new Date()
  dt.setMinutes(dt.getMinutes() - dt.getTimezoneOffset())
  Object.assign(form, {
    warehouseId: row.warehouseId,
    workshopId: row.workshopId || 0,
    documentDate: dt.toISOString().slice(0, 16),
    comment: row.comment || '',
    items: (row.items || []).map((i) => ({
      key: ++lineSeq,
      goodsId: i.goodsId,
      count: Number(i.count),
      unitCost: i.unitCost != null ? Number(i.unitCost) : null,
    })),
  })
  if (!form.items.length) form.items.push({ key: ++lineSeq, goodsId: 0, count: 1, unitCost: null })
  modalOpen.value = true
  document.body.style.overflow = 'hidden'
}

function closeModal() {
  if (saving.value) return
  modalOpen.value = false
  document.body.style.overflow = ''
}

function addLine() {
  form.items.push({ key: ++lineSeq, goodsId: 0, count: 1, unitCost: null })
}

function removeLine(idx: number) {
  if (form.items.length <= 1) return
  form.items.splice(idx, 1)
}

async function onSubmit() {
  formError.value = null
  if (!form.warehouseId) {
    formError.value = t('finishedGoods.needWarehouse')
    return
  }
  const items = form.items.filter((i) => i.goodsId && Number(i.count) > 0)
  if (!items.length) {
    formError.value = t('finishedGoods.needItems')
    return
  }
  saving.value = true
  try {
    const payload = {
      warehouseId: form.warehouseId,
      workshopId: form.workshopId || null,
      comment: form.comment || undefined,
      documentDate: toIso(form.documentDate),
      items: items.map((i) => ({
        goodsId: i.goodsId,
        count: Number(i.count),
        unitCost: i.unitCost != null && Number(i.unitCost) >= 0 ? Number(i.unitCost) : null,
      })),
    }
    if (editingId.value) await updateFinishedGoodsReceipt(editingId.value, payload)
    else await createFinishedGoodsReceipt(payload)
    closeModal()
    await load()
  } catch (e) {
    formError.value = formatApiError(e, t('common.saveError'))
  } finally {
    saving.value = false
  }
}

async function onPost(row: FinishedGoodsReceipt) {
  const ok = await Swal.fire({
    title: t('finishedGoods.post'),
    text: t('finishedGoods.postConfirm', { id: row.id }),
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: t('finishedGoods.post'),
    cancelButtonText: t('common.cancel'),
  })
  if (!ok.isConfirmed) return
  postingId.value = row.id
  try {
    await postFinishedGoodsReceipt(row.id)
    await load()
    await Swal.fire({ title: t('finishedGoods.posted'), icon: 'success', timer: 1200, showConfirmButton: false })
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    postingId.value = null
  }
}

async function onDelete(row: FinishedGoodsReceipt) {
  if (!confirm(t('common.deleteConfirmNamed', { name: `#${row.id}` }))) return
  try {
    await deleteFinishedGoodsReceipt(row.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKeydown)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.page-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.page-title { font-size: 1.125rem; font-weight: 650; color: #1f2937; }
.page-sub { margin-top: 0.15rem; font-size: 0.8125rem; color: #6b7280; }
.dark .page-title { color: rgba(255,255,255,0.92); }

.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.dark .card { border-color: #1f2937; background: rgb(255 255 255 / 0.03); }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.85rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.row { border-bottom: 1px solid #f3f4f6; transition: background 0.12s ease; }
.row:hover { background: #f9fafb; }
.dark .row { border-color: #1f2937; }
.dark .row:hover { background: rgb(255 255 255 / 0.03); }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }

.actions { display: inline-flex; flex-wrap: wrap; justify-content: flex-end; gap: 0.35rem; }
.action {
  border-radius: 0.45rem;
  border: 1px solid transparent;
  padding: 0.28rem 0.55rem;
  font-size: 0.75rem;
  font-weight: 550;
  color: #465fff;
  background: #eef2ff;
}
.action.strong { background: #ecfdf3; color: #039855; }
.action.danger { background: #fef3f2; color: #d92d20; }
.action:disabled { opacity: 0.5; cursor: not-allowed; }

.badge { display: inline-flex; border-radius: 9999px; padding: 0.15rem 0.55rem; font-size: 0.7rem; font-weight: 600; }
.badge.ok { background: #ecfdf3; color: #039855; }
.badge.draft { background: #f2f4f7; color: #475467; }

.err {
  border-radius: 0.75rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  color: #dc2626;
}

.overlay {
  position: fixed;
  inset: 0;
  z-index: 99999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: rgb(15 23 42 / 0.48);
  backdrop-filter: blur(4px);
  animation: fadeIn 0.15s ease;
}
.sheet {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 48rem;
  max-height: min(90vh, 860px);
  overflow: hidden;
  border-radius: 1.15rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  box-shadow: 0 24px 80px rgb(15 23 42 / 0.22);
  animation: rise 0.18s ease;
}
.dark .sheet { border-color: #344054; background: #0f172a; }
.sheet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.15rem 1.25rem 1rem;
  border-bottom: 1px solid #f2f4f7;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
}
.dark .sheet-head {
  border-color: #1f2937;
  background: linear-gradient(180deg, rgb(255 255 255 / 0.04), transparent);
}
.sheet-kicker {
  font-size: 0.7rem;
  font-weight: 600;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #98a2b3;
}
.sheet-title { margin-top: 0.15rem; font-size: 1.15rem; font-weight: 700; color: #101828; }
.dark .sheet-title { color: rgba(255,255,255,0.95); }
.sheet-sub { margin-top: 0.25rem; font-size: 0.8125rem; color: #667085; max-width: 36rem; }
.sheet-body {
  flex: 1;
  overflow: auto;
  padding: 1.1rem 1.25rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.sheet-foot {
  display: flex;
  justify-content: flex-end;
  gap: 0.6rem;
  padding: 0.9rem 1.25rem;
  border-top: 1px solid #f2f4f7;
  background: #fafbfc;
}
.dark .sheet-foot { border-color: #1f2937; background: rgb(255 255 255 / 0.02); }

.block {
  border: 1px solid #eef0f3;
  border-radius: 0.95rem;
  padding: 0.95rem;
  background: #fcfcfd;
}
.dark .block { border-color: #1f2937; background: rgb(255 255 255 / 0.02); }
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.85rem;
}
.block-title { margin-bottom: 0.75rem; font-size: 0.8125rem; font-weight: 650; color: #344054; }
.dark .block-title { color: #d0d5dd; }
.block-meta { margin-top: 0.15rem; font-size: 0.75rem; color: #98a2b3; }

.grid-3 { display: grid; grid-template-columns: 1fr; gap: 0.75rem; }
@media (min-width: 640px) {
  .grid-3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
.field-wrap { display: block; min-width: 0; }
.lbl {
  display: block;
  margin-bottom: 0.35rem;
  font-size: 0.75rem;
  font-weight: 550;
  color: #667085;
}
.lbl em { color: #f04438; font-style: normal; }
.field {
  height: 2.55rem;
  width: 100%;
  border-radius: 0.65rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  padding: 0 0.8rem;
  font-size: 0.875rem;
  color: #101828;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.field:focus {
  outline: none;
  border-color: #8098f9;
  box-shadow: 0 0 0 3px rgb(70 95 255 / 0.15);
}
.dark .field {
  border-color: #344054;
  background: #111827;
  color: rgba(255,255,255,0.92);
}

.lines { display: flex; flex-direction: column; gap: 0.55rem; }
.lines-head {
  display: none;
  grid-template-columns: minmax(0, 1fr) 6.5rem 8rem 2.5rem;
  gap: 0.55rem;
  padding: 0 0.15rem;
  font-size: 0.7rem;
  font-weight: 600;
  color: #98a2b3;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}
@media (min-width: 640px) {
  .lines-head { display: grid; }
}
.line {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 6.5rem 8rem 2.5rem;
  gap: 0.55rem;
  align-items: center;
  padding: 0.55rem;
  border-radius: 0.8rem;
  border: 1px solid #eef0f3;
  background: #fff;
}
.dark .line { border-color: #344054; background: #111827; }
.qty { text-align: right; font-variant-numeric: tabular-nums; }
.lines-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.65rem;
  padding: 1.4rem 1rem;
  border: 1px dashed #d0d5dd;
  border-radius: 0.85rem;
  color: #98a2b3;
  font-size: 0.8125rem;
  text-align: center;
}

.btn {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border-radius: 0.65rem;
  padding: 0 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  transition: background 0.15s ease, opacity 0.15s ease, border-color 0.15s ease;
}
.btn:disabled { opacity: 0.55; cursor: not-allowed; }
.btn.primary { background: #465fff; color: #fff; }
.btn.primary:hover:not(:disabled) { background: #3641f5; }
.btn.ghost {
  border: 1px solid #d0d5dd;
  background: #fff;
  color: #344054;
}
.btn.ghost:hover:not(:disabled) { background: #f9fafb; }
.dark .btn.ghost {
  border-color: #344054;
  background: transparent;
  color: #d0d5dd;
}

.icon-btn {
  display: inline-flex;
  height: 2.35rem;
  width: 2.35rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.65rem;
  color: #667085;
  transition: background 0.15s ease, color 0.15s ease;
}
.icon-btn:hover { background: #f2f4f7; color: #101828; }
.icon-btn.danger { color: #f04438; }
.icon-btn.danger:hover { background: #fef3f2; }
.icon-btn:disabled { opacity: 0.35; cursor: not-allowed; }

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes rise {
  from { opacity: 0; transform: translateY(10px) scale(0.985); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>
