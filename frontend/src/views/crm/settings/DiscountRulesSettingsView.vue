<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.discountRules')" />
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="card mb-6">
      <div class="card-head">
        <div>
          <h3 class="card-title">{{ t('discountRules.rolesTitle') }}</h3>
          <p class="hint">{{ t('discountRules.rolesHint') }}</p>
        </div>
        <button type="button" class="btn shrink-0" :disabled="rulesSaving || rules.length === 0" @click="onSaveRules">
          {{ rulesSaving ? '...' : t('discountRules.saveRules') }}
        </button>
      </div>
      <div v-if="rulesMessage" class="ok mx-5 mt-4">{{ rulesMessage }}</div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('discountRules.role') }}</th>
              <th class="th w-64">{{ t('discountRules.maxPercent') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="2" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-for="r in rules" :key="r.roleId" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ r.roleName }}</td>
              <td class="td">
                <span v-if="r.locked" class="muted" :title="t('discountRules.locked')">{{ t('discountRules.unlimited') }}</span>
                <div v-else class="pct-input">
                  <input
                    v-model="ruleInputs[r.roleId]"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    class="field"
                    :placeholder="t('discountRules.unlimited')"
                  />
                  <span class="pct-suffix">%</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <div>
          <h3 class="card-title">{{ t('discountRules.kpiTitle') }}</h3>
          <p class="hint">{{ t('discountRules.kpiHint') }}</p>
        </div>
        <input v-model="search" class="field search shrink-0" :placeholder="t('discountRules.search')" />
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('discountRules.employee') }}</th>
              <th class="th">{{ t('discountRules.roles') }}</th>
              <th class="th">{{ t('discountRules.filial') }}</th>
              <th class="th w-64">{{ t('discountRules.kpiPercent') }}</th>
              <th class="th w-32"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="5" class="empty">{{ t('common.loading') }}</td></tr>
            <tr v-else-if="filteredRates.length === 0"><td colspan="5" class="empty">{{ t('discountRules.noUsers') }}</td></tr>
            <tr v-for="u in filteredRates" :key="u.userId" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">
                <div class="font-medium text-gray-800 dark:text-white/90">{{ u.fullName || u.username }}</div>
                <div class="muted text-xs">{{ u.username }}</div>
              </td>
              <td class="td">{{ (u.roles || []).join(', ') || '—' }}</td>
              <td class="td">{{ u.filialName || '—' }}</td>
              <td class="td">
                <div class="pct-input">
                  <input
                    v-model="kpiInputs[u.userId]"
                    type="number"
                    min="0"
                    max="100"
                    step="0.01"
                    class="field"
                    placeholder="—"
                    @keydown.enter="onSaveKpi(u)"
                  />
                  <span class="pct-suffix">%</span>
                </div>
              </td>
              <td class="td text-right">
                <span v-if="savedUserId === u.userId && !kpiDirty(u)" class="ok-text">{{ t('discountRules.saved') }}</span>
                <button
                  v-else
                  type="button"
                  class="btn btn-sm"
                  :disabled="!kpiDirty(u) || savingUserId === u.userId"
                  @click="onSaveKpi(u)"
                >
                  {{ savingUserId === u.userId ? '...' : t('common.save') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchDiscountRules, saveDiscountRules, type DiscountRule } from '@/api/discountRules'
import { fetchKpiRates, saveKpiRate, type KpiRate } from '@/api/kpi'
import { formatApiError } from '@/api/http'

const { t } = useI18n()

const loading = ref(false)
const error = ref<string | null>(null)

const rules = ref<DiscountRule[]>([])
const ruleInputs = reactive<Record<number, string | number>>({})
const rulesSaving = ref(false)
const rulesMessage = ref<string | null>(null)

const rates = ref<KpiRate[]>([])
const kpiInputs = reactive<Record<number, string | number>>({})
const search = ref('')
const savingUserId = ref<number | null>(null)
const savedUserId = ref<number | null>(null)

const filteredRates = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return rates.value
  return rates.value.filter((u) =>
    [u.fullName, u.username, u.filialName, ...(u.roles || [])].some((s) => s?.toLowerCase().includes(q)),
  )
})

/** v-model on number inputs yields '' when cleared; treat that as "no value". */
function parsePercent(raw: string | number | undefined): number | null | undefined {
  if (raw === '' || raw == null) return null
  const n = Number(raw)
  if (!Number.isFinite(n) || n < 0 || n > 100) return undefined
  return n
}

function kpiDirty(u: KpiRate) {
  const value = parsePercent(kpiInputs[u.userId])
  return value === undefined || (value || null) !== (u.percent ?? null)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [rulesRes, ratesRes] = await Promise.all([fetchDiscountRules(), fetchKpiRates()])
    rules.value = rulesRes.data || []
    for (const r of rules.value) ruleInputs[r.roleId] = r.maxDiscountPercent ?? ''
    rates.value = ratesRes.data || []
    for (const u of rates.value) kpiInputs[u.userId] = u.percent ?? ''
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

async function onSaveRules() {
  error.value = null
  rulesMessage.value = null
  const payload = []
  for (const r of rules.value) {
    if (r.locked) continue
    const value = parsePercent(ruleInputs[r.roleId])
    if (value === undefined) {
      error.value = `${r.roleName}: ${t('discountRules.percentRange')}`
      return
    }
    payload.push({ roleId: r.roleId, maxDiscountPercent: value })
  }
  rulesSaving.value = true
  try {
    const res = await saveDiscountRules(payload)
    rules.value = res.data || rules.value
    for (const r of rules.value) ruleInputs[r.roleId] = r.maxDiscountPercent ?? ''
    rulesMessage.value = t('discountRules.rulesSaved')
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    rulesSaving.value = false
  }
}

async function onSaveKpi(u: KpiRate) {
  if (!kpiDirty(u)) return
  const value = parsePercent(kpiInputs[u.userId])
  if (value === undefined) {
    error.value = `${u.fullName || u.username}: ${t('discountRules.percentRange')}`
    return
  }
  error.value = null
  savingUserId.value = u.userId
  try {
    const res = await saveKpiRate(u.userId, value || null)
    u.percent = res.data?.percent ?? null
    kpiInputs[u.userId] = u.percent ?? ''
    savedUserId.value = u.userId
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    savingUserId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.card-head { display: flex; flex-wrap: wrap; align-items: flex-start; justify-content: space-between; gap: 1rem; border-bottom: 1px solid #f3f4f6; padding: 1rem 1.25rem; }
.card-title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.hint { margin-top: 0.25rem; max-width: 48rem; font-size: 0.8125rem; color: #6b7280; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.625rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.muted { color: #9ca3af; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.search { width: 16rem; }
.pct-input { position: relative; max-width: 12rem; }
.pct-input .field { padding-inline-end: 2rem; }
.pct-suffix { position: absolute; inset-inline-end: 0.75rem; top: 50%; transform: translateY(-50%); font-size: 0.875rem; color: #9ca3af; pointer-events: none; }
.btn { display: inline-flex; height: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-sm { height: 2.25rem; padding: 0 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.ok { border-radius: 0.5rem; border: 1px solid #bbf7d0; background: #f0fdf4; padding: 0.625rem 1rem; font-size: 0.875rem; color: #15803d; }
.ok-text { font-size: 0.8125rem; color: #16a34a; }

.dark .card { border-color: #1f2937; background: rgba(255, 255, 255, 0.03); }
.dark .card-head { border-color: #1f2937; }
.dark .card-title { color: rgba(255, 255, 255, 0.9); }
.dark .hint, .dark .th, .dark .empty { color: #9ca3af; }
.dark .td { color: #d1d5db; }
.dark .field { border-color: #374151; color: #e5e7eb; }
.dark .err { border-color: #7f1d1d; background: rgba(127, 29, 29, 0.2); color: #fca5a5; }
.dark .ok { border-color: #14532d; background: rgba(20, 83, 45, 0.2); color: #86efac; }
</style>
