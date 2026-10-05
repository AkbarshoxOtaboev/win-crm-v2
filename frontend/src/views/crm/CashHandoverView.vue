<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.cashHandover')" />

    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="notice" class="ok mb-4">{{ notice }}</div>

    <div v-if="canCreate" class="card mb-4">
      <div class="flex flex-wrap items-end justify-between gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('cashHandover.submitTitle') }}</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ t('cashHandover.submitHint') }}</p>
        </div>
        <label class="lbl-block w-44">
          {{ t('cashHandover.day') }}
          <input v-model="day" type="date" class="field" :max="todayLocal" @change="loadSummary" />
        </label>
      </div>

      <div v-if="summaryLoading" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="summary.length === 0" class="empty">{{ t('cashHandover.noCashboxes') }}</div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('cashHandover.cashbox') }}</th>
              <th class="th text-end">{{ t('cashHandover.incoming') }}</th>
              <th class="th text-end">{{ t('cashHandover.outgoing') }}</th>
              <th class="th text-end">{{ t('cashHandover.handed') }}</th>
              <th class="th text-end">{{ t('cashHandover.remaining') }}</th>
              <th class="th w-48">{{ t('cashHandover.amountToHand') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="s in summary"
              :key="rowKey(s)"
              class="border-b border-gray-100 dark:border-gray-800"
              :class="{ 'opacity-60': isIdle(s) }"
            >
              <td class="td font-medium">
                {{ s.paymentTypeName }}
                <span class="cur">{{ s.currency }}</span>
              </td>
              <td class="td text-end text-success-600 dark:text-success-400">{{ amt(s.incoming, s.currency) }}</td>
              <td class="td text-end text-error-600 dark:text-error-400">{{ amt(s.outgoing, s.currency) }}</td>
              <td class="td text-end">
                {{ amt(Number(s.pending) + Number(s.accepted), s.currency) }}
                <div v-if="Number(s.pending) > 0" class="text-xs text-warning-600 dark:text-warning-400">
                  {{ t('cashHandover.pendingOf', { sum: amt(s.pending, s.currency) }) }}
                </div>
              </td>
              <td class="td text-end font-semibold text-gray-800 dark:text-white/90">{{ amt(s.remaining, s.currency) }}</td>
              <td class="td">
                <input v-model="amounts[rowKey(s)]" type="number" min="0" step="any" class="field" placeholder="0" />
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="flex flex-wrap items-end gap-3 px-5 py-4">
        <label class="lbl-block min-w-[16rem] flex-1">
          {{ t('common.comment') }}
          <input v-model="comment" type="text" maxlength="255" class="field" :placeholder="t('cashHandover.commentPlaceholder')" />
        </label>
        <div class="text-sm text-gray-600 dark:text-gray-400">
          {{ t('cashHandover.toHandTotal') }}: <b class="text-gray-800 dark:text-white/90">{{ totalsText(draftTotals) }}</b>
        </div>
        <button type="button" class="btn-primary" :disabled="submitting || draftItems.length === 0" @click="submit">
          {{ submitting ? t('common.saving') : t('cashHandover.submit') }}
        </button>
      </div>
    </div>

    <div class="card mb-4 p-4">
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <label class="lbl-block">
          {{ t('common.from') }}
          <input v-model="fromDate" type="date" class="field" @change="loadList" />
        </label>
        <label class="lbl-block">
          {{ t('common.to') }}
          <input v-model="toDate" type="date" class="field" @change="loadList" />
        </label>
        <label class="lbl-block">
          {{ t('common.status') }}
          <select v-model="statusFilter" class="field" @change="loadList">
            <option value="">{{ t('common.all') }}</option>
            <option v-for="st in STATUSES" :key="st" :value="st">{{ t(`cashHandover.status.${st}`) }}</option>
          </select>
        </label>
        <label v-if="canReview" class="lbl-block">
          {{ t('cashHandover.employee') }}
          <select v-model.number="userFilter" class="field" @change="loadList">
            <option :value="0">{{ t('cashHandover.allEmployees') }}</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
        </label>
      </div>
    </div>

    <div class="mb-4 grid grid-cols-1 gap-4 md:grid-cols-3">
      <article class="stat">
        <p class="stat-label">{{ t('cashHandover.pendingCount') }}</p>
        <h4 class="stat-value pending">{{ pendingRows.length }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('cashHandover.pendingSum') }}</p>
        <h4 class="stat-value">{{ totalsText(sumBy(pendingRows)) }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('cashHandover.acceptedSum') }}</p>
        <h4 class="stat-value accepted">{{ totalsText(sumBy(handovers.filter((h) => h.handoverStatus === 'ACCEPTED'))) }}</h4>
      </article>
    </div>

    <div class="card">
      <div v-if="listLoading" class="empty">{{ t('common.loading') }}</div>
      <div v-else-if="handovers.length === 0" class="empty">{{ t('cashHandover.empty') }}</div>
      <div v-else class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('common.date') }}</th>
              <th class="th">{{ t('cashHandover.employee') }}</th>
              <th class="th">{{ t('cashHandover.cashbox') }}</th>
              <th class="th text-end">{{ t('cashHandover.expected') }}</th>
              <th class="th text-end">{{ t('cashHandover.handedAmount') }}</th>
              <th class="th text-end">{{ t('cashHandover.difference') }}</th>
              <th class="th">{{ t('common.status') }}</th>
              <th class="th">{{ t('cashHandover.reviewer') }}</th>
              <th class="th text-end">{{ t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="h in handovers" :key="h.id" class="border-b border-gray-100 align-top dark:border-gray-800">
              <td class="td whitespace-nowrap">
                {{ formatDmy(h.handoverDate) }}
                <div class="text-xs text-gray-400">#{{ h.id }}</div>
              </td>
              <td class="td">
                {{ h.cashierName || '—' }}
                <div v-if="h.comment" class="text-xs text-gray-400">{{ h.comment }}</div>
              </td>
              <td class="td whitespace-nowrap">
                {{ h.paymentTypeName }}
                <span class="cur">{{ h.currency }}</span>
              </td>
              <td class="td whitespace-nowrap text-end">{{ amt(h.expectedAmount, h.currency) }}</td>
              <td class="td whitespace-nowrap text-end font-semibold text-gray-800 dark:text-white/90">{{ amt(h.amount, h.currency) }}</td>
              <td class="td whitespace-nowrap text-end" :class="diffClass(h.difference)">
                {{ diffText(h.difference, h.currency) }}
              </td>
              <td class="td whitespace-nowrap">
                <span class="badge" :class="`badge-${h.handoverStatus.toLowerCase()}`">
                  {{ t(`cashHandover.status.${h.handoverStatus}`) }}
                </span>
              </td>
              <td class="td">
                <template v-if="h.reviewerName">
                  {{ h.reviewerName }}
                  <div class="text-xs text-gray-400">{{ formatDmyTime(h.reviewedAt) }}</div>
                  <div v-if="h.reviewComment" class="text-xs text-gray-500">{{ h.reviewComment }}</div>
                </template>
                <span v-else>—</span>
              </td>
              <td class="td whitespace-nowrap text-end">
                <template v-if="h.handoverStatus === 'PENDING'">
                  <template v-if="canReview && (!h.mine || auth.superAdmin)">
                    <button type="button" class="btn-sm btn-accept" :disabled="busyId === h.id" @click="accept(h)">
                      {{ t('cashHandover.accept') }}
                    </button>
                    <button type="button" class="btn-sm btn-reject ms-2" :disabled="busyId === h.id" @click="reject(h)">
                      {{ t('cashHandover.reject') }}
                    </button>
                  </template>
                  <button v-if="h.mine" type="button" class="btn-sm btn-ghost ms-2" :disabled="busyId === h.id" @click="cancel(h)">
                    {{ t('common.cancel') }}
                  </button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  acceptCashHandover,
  cancelCashHandover,
  createCashHandover,
  fetchCashHandovers,
  fetchCashHandoverSummary,
  rejectCashHandover,
  type CashHandover,
  type CashHandoverStatus,
  type CashHandoverSummary,
} from '@/api/cashHandovers'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { formatApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { BASE_CURRENCY, CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'
import { formatDmyTime, money } from '@/utils/format'

const STATUSES: CashHandoverStatus[] = ['PENDING', 'ACCEPTED', 'REJECTED']

const { t } = useI18n()
const auth = useAuthStore()
const canCreate = computed(() => auth.can('CASH_HANDOVER_CREATE'))
const canReview = computed(() => auth.can('CASH_HANDOVER_EDIT'))

function localDate(d = new Date()) {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const todayLocal = localDate()
const day = ref(todayLocal)
const summary = ref<CashHandoverSummary[]>([])
const amounts = ref<Record<string, string | number>>({})
const comment = ref('')
const summaryLoading = ref(false)
const submitting = ref(false)

const weekAgo = new Date()
weekAgo.setDate(weekAgo.getDate() - 7)
const fromDate = ref(localDate(weekAgo))
const toDate = ref(todayLocal)
const statusFilter = ref<CashHandoverStatus | ''>('')
const userFilter = ref(0)
const users = ref<UserItem[]>([])
const handovers = ref<CashHandover[]>([])
const listLoading = ref(false)
const busyId = ref<number | null>(null)

const error = ref<string | null>(null)
const notice = ref<string | null>(null)

function rowKey(s: CashHandoverSummary) {
  return `${s.paymentTypeId}:${s.currency}`
}

function isIdle(s: CashHandoverSummary) {
  return [s.incoming, s.outgoing, s.pending, s.accepted].every((v) => Number(v || 0) === 0)
}

function amt(v: number | null | undefined, currency?: CurrencyCode) {
  return currency && currency !== BASE_CURRENCY ? moneyIn(v, currency) : money(v)
}

function formatDmy(v?: string | null) {
  return v ? formatDmyTime(v).slice(0, 10) : '—'
}

function diffText(v: number, currency: CurrencyCode) {
  const n = Number(v || 0)
  if (n === 0) return '0'
  return `${n > 0 ? '+' : '−'}${amt(Math.abs(n), currency)}`
}

function diffClass(v: number) {
  const n = Number(v || 0)
  if (n < 0) return 'text-error-600 dark:text-error-400 font-semibold'
  if (n > 0) return 'text-warning-600 dark:text-warning-400'
  return 'text-gray-400'
}

/** Valyuta bo'yicha alohida jami: so'm va dollar qo'shilmaydi. */
function totalsOf(rows: { currency: CurrencyCode; amount: number }[]) {
  const totals = new Map<CurrencyCode, number>()
  for (const r of rows) totals.set(r.currency, (totals.get(r.currency) || 0) + Number(r.amount || 0))
  return CURRENCIES.filter((c) => totals.has(c)).map((c) => ({ currency: c, amount: totals.get(c) || 0 }))
}

function sumBy(rows: CashHandover[]) {
  return totalsOf(rows.map((h) => ({ currency: h.currency, amount: h.amount })))
}

function totalsText(totals: { currency: CurrencyCode; amount: number }[]) {
  if (!totals.length) return money(0)
  return totals.map((x) => amt(x.amount, x.currency)).join(' · ')
}

const draftItems = computed(() =>
  summary.value
    .map((s) => ({ s, amount: Number(amounts.value[rowKey(s)] || 0) }))
    .filter((x) => x.amount > 0),
)

const draftTotals = computed(() => totalsOf(draftItems.value.map((x) => ({ currency: x.s.currency, amount: x.amount }))))

const pendingRows = computed(() => handovers.value.filter((h) => h.handoverStatus === 'PENDING'))

async function loadSummary() {
  if (!canCreate.value) return
  summaryLoading.value = true
  try {
    const res = await fetchCashHandoverSummary({ date: day.value || undefined })
    summary.value = res.data || []
    const next: Record<string, number | string> = {}
    for (const s of summary.value) {
      const remaining = Number(s.remaining || 0)
      next[rowKey(s)] = remaining > 0 ? remaining : ''
    }
    amounts.value = next
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    summaryLoading.value = false
  }
}

async function loadList() {
  listLoading.value = true
  try {
    const res = await fetchCashHandovers({
      fromDate: fromDate.value || undefined,
      toDate: toDate.value || undefined,
      status: statusFilter.value || undefined,
      userId: canReview.value && userFilter.value ? userFilter.value : undefined,
    })
    handovers.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    listLoading.value = false
  }
}

async function submit() {
  if (!draftItems.value.length) return
  if (!confirm(t('cashHandover.submitConfirm', { sum: totalsText(draftTotals.value) }))) return
  submitting.value = true
  error.value = null
  notice.value = null
  try {
    await createCashHandover({
      handoverDate: day.value || undefined,
      comment: comment.value.trim() || undefined,
      items: draftItems.value.map((x) => ({ paymentTypeId: x.s.paymentTypeId, amount: x.amount })),
    })
    comment.value = ''
    notice.value = t('cashHandover.submitted')
    await Promise.all([loadSummary(), loadList()])
  } catch (e) {
    error.value = formatApiError(e, t('common.saveError'))
  } finally {
    submitting.value = false
  }
}

async function runAction(h: CashHandover, action: () => Promise<unknown>) {
  busyId.value = h.id
  error.value = null
  notice.value = null
  try {
    await action()
    await Promise.all([loadList(), h.mine ? loadSummary() : Promise.resolve()])
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    busyId.value = null
  }
}

function accept(h: CashHandover) {
  if (!confirm(t('cashHandover.acceptConfirm', { name: h.cashierName || '', sum: amt(h.amount, h.currency) }))) return
  return runAction(h, () => acceptCashHandover(h.id))
}

function reject(h: CashHandover) {
  const reason = prompt(t('cashHandover.rejectPrompt'))
  if (reason === null) return
  return runAction(h, () => rejectCashHandover(h.id, reason.trim() || undefined))
}

function cancel(h: CashHandover) {
  if (!confirm(t('cashHandover.cancelConfirm'))) return
  return runAction(h, () => cancelCashHandover(h.id))
}

onMounted(async () => {
  if (canReview.value) {
    try {
      users.value = (await fetchUserOptions()).data || []
    } catch {
      users.value = []
    }
  }
  await Promise.all([loadSummary(), loadList()])
})
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.lbl-block { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.8125rem; font-weight: 500; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.ok { border-radius: 0.5rem; border: 1px solid #a6f4c5; background: #ecfdf3; padding: 0.75rem 1rem; font-size: 0.875rem; color: #027a48; }
.th { padding: 0.75rem 1.25rem; text-align: start; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.th.text-end { text-align: end; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.stat { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.stat-label { font-size: 0.875rem; color: #4b5563; }
.stat-value { margin-top: 0.25rem; font-size: 1.25rem; font-weight: 700; color: #1f2937; }
.stat-value.pending { color: #dc6803; }
.stat-value.accepted { color: #039855; }
.cur { margin-inline-start: 0.25rem; border-radius: 0.25rem; background: #f2f4f7; padding: 0.05rem 0.375rem; font-size: 0.6875rem; color: #667085; }
.badge { display: inline-flex; border-radius: 9999px; padding: 0.125rem 0.625rem; font-size: 0.75rem; font-weight: 500; }
.badge-pending { background: #fffaeb; color: #b54708; }
.badge-accepted { background: #ecfdf3; color: #027a48; }
.badge-rejected { background: #fef3f2; color: #b42318; }
.btn-primary { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; font-size: 0.875rem; font-weight: 500; color: #fff; }
.btn-primary:hover:not(:disabled) { background: #3641f5; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-sm { height: 2rem; border-radius: 0.5rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 500; }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-accept { background: #12b76a; color: #fff; }
.btn-accept:hover:not(:disabled) { background: #039855; }
.btn-reject { border: 1px solid #fda29b; color: #d92d20; }
.btn-reject:hover:not(:disabled) { background: #fef3f2; }
.btn-ghost { border: 1px solid #d0d5dd; color: #344054; }
.btn-ghost:hover:not(:disabled) { background: #f9fafb; }
.dark .card, .dark .stat { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .field option { background: #101828; }
.dark .lbl-block { color: #9ca3af; }
.dark .td { color: #9ca3af; }
.dark .stat-label { color: #9ca3af; }
.dark .stat-value { color: rgba(255, 255, 255, 0.92); }
.dark .stat-value.pending { color: #fdb022; }
.dark .stat-value.accepted { color: #32d583; }
.dark .cur { background: rgb(255 255 255 / 5%); color: #98a2b3; }
.dark .err { border-color: rgb(240 68 56 / 35%); background: rgb(240 68 56 / 10%); color: #fda29b; }
.dark .ok { border-color: rgb(18 183 106 / 35%); background: rgb(18 183 106 / 10%); color: #6ce9a6; }
.dark .badge-pending { background: rgb(247 144 9 / 15%); color: #fdb022; }
.dark .badge-accepted { background: rgb(18 183 106 / 15%); color: #32d583; }
.dark .badge-rejected { background: rgb(240 68 56 / 15%); color: #f97066; }
.dark .btn-reject { border-color: rgb(240 68 56 / 45%); color: #f97066; }
.dark .btn-reject:hover:not(:disabled) { background: rgb(240 68 56 / 10%); }
.dark .btn-ghost { border-color: #344054; color: #d0d5dd; }
.dark .btn-ghost:hover:not(:disabled) { background: rgb(255 255 255 / 5%); }
</style>
