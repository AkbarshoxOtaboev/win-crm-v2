<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.productionBoard')" />
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-col gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('nav.productionBoard') }}</h3>
            <p v-if="workshopId && !loading" class="mt-0.5 text-xs text-gray-500 dark:text-gray-400">
              {{ t('productionBoard.summary', counts) }}
            </p>
          </div>
          <select
            v-model.number="workshopId"
            class="field ws-select"
            :disabled="visibleWorkshops.length === 1 && auth.workshopIds.length > 0"
            @change="loadBoard"
          >
            <option :value="0" disabled>{{ t('productionDashboard.selectWorkshop') }}</option>
            <option v-for="w in visibleWorkshops" :key="w.id" :value="w.id">{{ w.name }}</option>
          </select>
        </div>
        <div class="filter-row">
          <label class="filter-date">
            <span>{{ t('common.from') }}</span>
            <input v-model="fromDate" type="date" class="field" :max="toDate || undefined" @change="loadBoard" />
          </label>
          <label class="filter-date">
            <span>{{ t('common.to') }}</span>
            <input v-model="toDate" type="date" class="field" :min="fromDate || undefined" @change="loadBoard" />
          </label>
          <button
            type="button"
            class="icon-btn"
            :disabled="isCurrentMonth"
            :title="t('productionBoard.resetMonth')"
            :aria-label="t('productionBoard.resetMonth')"
            @click="resetMonth"
          >
            <FilterX class="h-4 w-4" />
          </button>
          <button
            type="button"
            class="icon-btn"
            :disabled="loading"
            :title="t('common.refresh')"
            :aria-label="t('common.refresh')"
            @click="loadBoard"
          >
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
          <span class="hint">{{ t('productionBoard.rangeHint') }}</span>
        </div>
      </div>

      <div v-if="error" class="err mx-5 mt-4">{{ error }}</div>

      <div class="space-y-3 p-5">
        <div v-if="loading && items.length === 0" class="empty">{{ t('common.loading') }}</div>
        <div v-else-if="!workshopId" class="empty">{{ t('productionDashboard.selectWorkshop') }}</div>
        <div v-else-if="items.length === 0" class="empty">{{ t('productionBoard.emptyRange') }}</div>

        <article
          v-for="o in items"
          :key="o.boardAssignmentId ?? o.id"
          :class="['po-card', isDone(o) ? 'is-done' : o.boardAssignmentStatus === 'ACTIVE' ? 'is-active' : 'is-pending']"
        >
          <header class="po-head">
            <div class="min-w-0">
              <div class="flex flex-wrap items-center gap-2">
                <span class="po-no">{{ t('productionBoard.poNumber') }}{{ o.id }}</span>
                <span v-if="o.saleOrderId" class="po-sale">{{ t('productionBoard.sale', { id: o.saleOrderId }) }}</span>
                <span :class="['badge', badgeClass(o)]">
                  <CheckCircle2 v-if="isDone(o)" class="h-3.5 w-3.5" />
                  {{ boardStatusLabel(o) }}
                </span>
              </div>
              <p class="po-client">{{ o.clientFullName || '—' }}</p>
              <span v-if="o.route && o.route.length > 1" class="route-mini">
                <span v-for="s in o.route" :key="s.stepNo" :class="['route-chip', s.state.toLowerCase()]">
                  {{ s.stepNo }}. {{ s.workshopName }}
                </span>
              </span>
            </div>
            <div v-if="canAct(o)" class="inline-flex flex-wrap justify-end gap-1.5">
              <button
                v-if="o.boardAssignmentStatus === 'PENDING'"
                type="button"
                class="btn-sm"
                @click="onStart(o)"
              >
                {{ t('productionBoard.start') }}
              </button>
              <button type="button" class="btn-sm alt" @click="openRedirect(o)">{{ t('productionBoard.redirect') }}</button>
              <button type="button" class="btn-sm ok" @click="onComplete(o)">
                {{ o.nextWorkshopName ? t('productionBoard.finishToNext', { name: o.nextWorkshopName }) : t('productionBoard.complete') }}
              </button>
            </div>
          </header>

          <dl class="po-dates">
            <div><dt>{{ t('productionBoard.orderDate') }}</dt><dd>{{ formatDate(o.orderDate) }}</dd></div>
            <div><dt>{{ t('productionBoard.plannedReady') }}</dt><dd>{{ formatDate(o.plannedReadyDate) }}</dd></div>
            <div><dt>{{ t('production.acceptedAt') }}</dt><dd>{{ formatDate(o.acceptedAt) }}</dd></div>
            <div><dt>{{ t('production.submittedAt') }}</dt><dd>{{ formatDate(o.submittedAt) }}</dd></div>
          </dl>

          <p v-if="o.saleOrderComment || o.note" class="po-comment">
            <span>{{ t('productionBoard.comment') }}:</span>
            {{ [o.saleOrderComment, o.note].filter(Boolean).join(' · ') }}
          </p>

          <div class="po-body">
            <section class="po-items">
              <h4>{{ t('productionBoard.products') }}</h4>
              <p v-if="!o.items?.length" class="muted">{{ t('productionBoard.noItems') }}</p>
              <table v-else class="items-table">
                <thead>
                  <tr>
                    <th>{{ t('productionBoard.product') }}</th>
                    <th>{{ t('productionBoard.size') }}</th>
                    <th class="text-right">{{ t('productionBoard.pieces') }}</th>
                    <th class="text-right">{{ t('productionBoard.quantity') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="it in o.items" :key="it.id">
                    <td class="font-medium text-gray-800 dark:text-white/90">{{ it.goodsName || '—' }}</td>
                    <td class="whitespace-nowrap">{{ sizeLabel(it) }}</td>
                    <td class="text-right">{{ it.pieces != null ? money(it.pieces) : '—' }}</td>
                    <td class="whitespace-nowrap text-right">
                      {{ money(it.count) }}<span v-if="it.width != null && it.height != null" class="unit"> {{ t('productionBoard.kvm') }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </section>
            <section v-if="o.images?.length" class="po-images">
              <h4>{{ t('productionBoard.images') }} ({{ o.images.length }})</h4>
              <div class="thumbs">
                <button
                  v-for="img in o.images"
                  :key="img.id"
                  type="button"
                  class="thumb"
                  :title="img.originalFileName || ''"
                  @click="preview = img"
                >
                  <AuthImage :src="img.url" :alt="img.originalFileName || ''" class="thumb-img" />
                </button>
              </div>
            </section>
          </div>
        </article>
      </div>
    </div>

    <div
      v-if="preview"
      class="fixed inset-0 z-99999 flex items-center justify-center bg-black/70 p-4"
      @click.self="preview = null"
    >
      <div class="relative max-h-full max-w-5xl">
        <button
          type="button"
          class="preview-close"
          :title="t('common.close')"
          :aria-label="t('common.close')"
          @click="preview = null"
        >
          <X class="h-5 w-5" />
        </button>
        <AuthImage
          :src="preview.url"
          :alt="preview.originalFileName || ''"
          class="max-h-[85vh] min-h-40 min-w-60 rounded-lg object-contain"
        />
        <p v-if="preview.originalFileName" class="mt-2 text-center text-sm text-white/80">{{ preview.originalFileName }}</p>
      </div>
    </div>

    <div v-if="redirectOpen" class="fixed inset-0 z-99999 flex items-center justify-center bg-black/40 p-4">
      <div class="w-full max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-gray-900">
        <h3 class="mb-4 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('productionBoard.redirectTitle') }}</h3>
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onRedirect">
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">{{ t('productionBoard.nextWorkshop') }} *</label>
            <select v-model.number="nextWorkshopId" required class="field">
              <option :value="0" disabled>{{ t('common.select') }}</option>
              <option
                v-for="w in redirectWorkshops"
                :key="w.id"
                :value="w.id"
              >
                {{ w.name }}
              </option>
            </select>
          </div>
          <div>
            <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">{{ t('common.note') }}</label>
            <input v-model="note" class="field" />
          </div>
          <div class="flex justify-end gap-2">
            <button type="button" class="h-10 rounded-lg border border-gray-300 px-4 text-sm" @click="redirectOpen = false">{{ t('common.cancel') }}</button>
            <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('productionBoard.redirect') }}</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { CheckCircle2, FilterX, RefreshCw, X } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import AuthImage from '@/components/crm/AuthImage.vue'
import {
  completeProduction,
  fetchProductionBoard,
  redirectProduction,
  startProduction,
  type ProductionOrder,
  type ProductionOrderImage,
  type ProductionOrderItem,
} from '@/api/production'
import { fetchActiveWorkshops, type Workshop } from '@/api/workshops'
import { formatApiError } from '@/api/http'
import { formatDate, money } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const workshops = ref<Workshop[]>([])
const visibleWorkshops = computed(() =>
  auth.workshopIds.length ? workshops.value.filter((w) => auth.workshopIds.includes(w.id)) : workshops.value,
)
const workshopId = ref(0)
const items = ref<ProductionOrder[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const redirectOpen = ref(false)
const selected = ref<ProductionOrder | null>(null)
const nextWorkshopId = ref(0)
const note = ref('')
const formError = ref<string | null>(null)
const saving = ref(false)
const preview = ref<ProductionOrderImage | null>(null)

function localIsoDate(d: Date) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function monthStart() {
  const d = new Date()
  return localIsoDate(new Date(d.getFullYear(), d.getMonth(), 1))
}
function todayLocal() {
  return localIsoDate(new Date())
}

const fromDate = ref(monthStart())
const toDate = ref(todayLocal())
const isCurrentMonth = computed(() => fromDate.value === monthStart() && toDate.value === todayLocal())

const counts = computed(() => ({
  pending: items.value.filter((o) => o.boardAssignmentStatus === 'PENDING').length,
  active: items.value.filter((o) => o.boardAssignmentStatus === 'ACTIVE').length,
  done: items.value.filter(isDone).length,
}))

const redirectWorkshops = computed(() =>
  workshops.value.filter((w) => w.id !== selected.value?.currentWorkshopId),
)

function isDone(o: ProductionOrder) {
  return o.boardAssignmentStatus === 'DONE'
}

function canAct(o: ProductionOrder) {
  return (
    (o.boardAssignmentStatus === 'PENDING' || o.boardAssignmentStatus === 'ACTIVE') &&
    o.boardAssignmentId === o.currentAssignmentId
  )
}

function boardStatusLabel(o: ProductionOrder) {
  if (isDone(o)) return t('productionBoard.completed')
  if (o.boardAssignmentStatus === 'ACTIVE') return t('productionBoard.active')
  return t('productionBoard.pending')
}

function badgeClass(o: ProductionOrder) {
  if (isDone(o)) return 'done'
  return o.boardAssignmentStatus === 'ACTIVE' ? 'active' : 'pending'
}

function sizeLabel(it: ProductionOrderItem) {
  if (it.width == null || it.height == null) return '—'
  return `${money(it.width)} × ${money(it.height)}`
}

async function loadWorkshops() {
  const res = await fetchActiveWorkshops()
  workshops.value = res.data || []
  pickWorkshop()
}

function pickWorkshop() {
  if (!visibleWorkshops.value.some((w) => w.id === workshopId.value)) {
    workshopId.value = visibleWorkshops.value[0]?.id || 0
  }
}

watch(
  () => auth.workshopIds,
  () => {
    const before = workshopId.value
    pickWorkshop()
    if (workshopId.value !== before) void loadBoard()
  },
)

function resetMonth() {
  fromDate.value = monthStart()
  toDate.value = todayLocal()
  void loadBoard()
}

async function loadBoard() {
  if (!workshopId.value) {
    items.value = []
    return
  }
  if (fromDate.value && toDate.value && fromDate.value > toDate.value) {
    error.value = t('productionBoard.invalidRange')
    return
  }
  loading.value = true
  error.value = null
  try {
    const res = await fetchProductionBoard(workshopId.value, fromDate.value || undefined, toDate.value || undefined)
    items.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

async function onStart(o: ProductionOrder) {
  try {
    await startProduction(o.id)
    await loadBoard()
  } catch (e) {
    error.value = formatApiError(e, t('productionBoard.startError'))
  }
}

function openRedirect(o: ProductionOrder) {
  selected.value = o
  nextWorkshopId.value = o.nextWorkshopId || 0
  note.value = ''
  formError.value = null
  redirectOpen.value = true
}

async function onRedirect() {
  if (!selected.value || !nextWorkshopId.value) return
  saving.value = true
  formError.value = null
  try {
    await redirectProduction(selected.value.id, nextWorkshopId.value, note.value.trim() || undefined)
    redirectOpen.value = false
    await loadBoard()
  } catch (e) {
    formError.value = formatApiError(e, t('productionBoard.redirectError'))
  } finally {
    saving.value = false
  }
}

async function onComplete(o: ProductionOrder) {
  const question = o.nextWorkshopName
    ? t('productionBoard.finishToNextConfirm', { id: o.id, name: o.nextWorkshopName })
    : t('productionBoard.completeConfirm', { id: o.id })
  if (!confirm(question)) return
  try {
    await completeProduction(o.id)
    await loadBoard()
  } catch (e) {
    error.value = formatApiError(e, t('productionBoard.completeError'))
  }
}

onMounted(async () => {
  try {
    await loadWorkshops()
    await loadBoard()
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  }
})
</script>

<style scoped>
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.dark .field { border-color: #344054; color: #e5e7eb; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn-sm { display: inline-flex; height: 2rem; align-items: center; border-radius: 0.5rem; background: #465fff; padding: 0 0.75rem; font-size: 0.75rem; font-weight: 500; color: #fff; }
.btn-sm.alt { background: #f79009; }
.btn-sm.ok { background: #12b76a; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; flex: none; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #4b5563; }
.icon-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.dark .icon-btn { border-color: #344054; color: #d1d5db; }

@media (min-width: 640px) { .ws-select { width: 16rem; flex: none; } }
.unit { margin-left: 0.2rem; font-size: 0.72rem; color: #9ca3af; }
.filter-row { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
.filter-date { display: inline-flex; align-items: center; gap: 0.4rem; font-size: 0.8rem; color: #6b7280; }
.filter-date .field { width: 10.5rem; }
.hint { font-size: 0.75rem; color: #9ca3af; }

.po-card { border-radius: 0.875rem; border: 1px solid #e5e7eb; border-left-width: 4px; padding: 1rem 1.1rem; background: #fff; }
.po-card.is-pending { border-left-color: #3b82f6; }
.po-card.is-active { border-left-color: #f97316; }
.po-card.is-done { border-left-color: #12b76a; background: #f6fef9; }
.dark .po-card { border-color: #1f2937; background: rgb(255 255 255 / 0.02); }
.dark .po-card.is-pending { border-left-color: #3b82f6; }
.dark .po-card.is-active { border-left-color: #f97316; }
.dark .po-card.is-done { border-left-color: #12b76a; background: rgb(18 183 106 / 6%); }

.po-head { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: 0.75rem; }
.po-no { font-size: 0.95rem; font-weight: 700; color: #1f2937; }
.dark .po-no { color: rgb(255 255 255 / 0.9); }
.po-sale { font-size: 0.8rem; color: #6b7280; }
.po-client { margin-top: 0.2rem; font-size: 0.9rem; font-weight: 500; color: #374151; }
.dark .po-client { color: #d1d5db; }

.badge { display: inline-flex; align-items: center; gap: 0.25rem; border-radius: 9999px; padding: 0.1rem 0.6rem; font-size: 0.72rem; font-weight: 600; }
.badge.pending { background: #eff4ff; color: #3b5bdb; }
.badge.active { background: #fff6ed; color: #c4320a; }
.badge.done { background: #dcfae6; color: #067647; }
.dark .badge.pending { background: rgb(70 95 255 / 15%); color: #9cb0ff; }
.dark .badge.active { background: rgb(249 115 22 / 15%); color: #fdba74; }
.dark .badge.done { background: rgb(18 183 106 / 15%); color: #6ce9a6; }

.po-dates { display: grid; grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); gap: 0.5rem 1rem; margin-top: 0.75rem; }
.po-dates dt { font-size: 0.7rem; color: #9ca3af; }
.po-dates dd { font-size: 0.82rem; color: #374151; white-space: nowrap; }
.dark .po-dates dd { color: #d1d5db; }
.po-comment { margin-top: 0.6rem; border-radius: 0.5rem; background: #f9fafb; padding: 0.45rem 0.7rem; font-size: 0.82rem; color: #4b5563; }
.po-comment span { font-weight: 600; }
.dark .po-comment { background: rgb(255 255 255 / 0.04); color: #d1d5db; }

.po-body { display: flex; flex-wrap: wrap; gap: 1rem; margin-top: 0.75rem; }
.po-items { flex: 1 1 22rem; min-width: 0; }
.po-images { flex: 0 1 18rem; }
.po-body h4 { margin-bottom: 0.35rem; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.03em; color: #6b7280; }
.muted { font-size: 0.82rem; color: #9ca3af; }
.items-table { width: 100%; border-collapse: collapse; font-size: 0.82rem; }
.items-table th { padding: 0.35rem 0.5rem; text-align: left; font-size: 0.7rem; font-weight: 500; color: #9ca3af; border-bottom: 1px solid #f3f4f6; }
.items-table th.text-right { text-align: right; }
.items-table td { padding: 0.4rem 0.5rem; color: #4b5563; border-bottom: 1px solid #f3f4f6; }
.dark .items-table th, .dark .items-table td { border-color: #1f2937; }
.dark .items-table td { color: #d1d5db; }

.thumbs { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.thumb { height: 4.5rem; width: 4.5rem; overflow: hidden; border-radius: 0.5rem; border: 1px solid #e5e7eb; background: #f9fafb; }
.thumb :deep(.thumb-img) { height: 100%; width: 100%; object-fit: cover; transition: transform 0.15s; }
.thumb:hover :deep(.thumb-img) { transform: scale(1.06); }
.dark .thumb { border-color: #344054; background: #111827; }
.preview-close { position: absolute; top: -0.75rem; right: -0.75rem; display: inline-flex; height: 2.25rem; width: 2.25rem; align-items: center; justify-content: center; border-radius: 9999px; background: #fff; color: #111827; box-shadow: 0 2px 8px rgb(0 0 0 / 30%); }

.route-mini { display: flex; flex-wrap: wrap; gap: 0.25rem; margin-top: 0.35rem; }
.route-chip { border-radius: 9999px; border: 1px solid #e5e7eb; padding: 0.05rem 0.5rem; font-size: 0.7rem; font-weight: 500; color: #6b7280; }
.route-chip.done { border-color: #a6f4c5; background: #ecfdf3; color: #027a48; }
.route-chip.current { border-color: #9cb0ff; background: #eff4ff; color: #465fff; }
.dark .route-chip { border-color: #344054; color: #9ca3af; }
.dark .route-chip.done { border-color: rgb(18 183 106 / 40%); background: rgb(18 183 106 / 10%); color: #6ce9a6; }
.dark .route-chip.current { border-color: rgb(70 95 255 / 50%); background: rgb(70 95 255 / 12%); color: #9cb0ff; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
</style>
