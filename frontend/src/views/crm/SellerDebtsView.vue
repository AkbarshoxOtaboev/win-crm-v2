<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.sellerDebts')" />

    <div class="card mb-4 p-4">
      <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <label v-if="canViewAll" class="lbl-block">
          {{ t('sellerDebts.seller') }}
          <select v-model.number="userId" class="field" @change="load">
            <option :value="0">{{ t('sellerDebts.allSellers') }}</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.fullName || u.username }}</option>
          </select>
        </label>
        <label class="lbl-block">
          {{ t('common.from') }}
          <input v-model="startDate" type="date" class="field" @change="load" />
        </label>
        <label class="lbl-block">
          {{ t('sellerDebts.to') }}
          <input v-model="endDate" type="date" class="field" @change="load" />
        </label>
        <label class="lbl-block">
          {{ t('sellerDebts.search') }}
          <input v-model="search" type="search" class="field" :placeholder="t('sellerDebts.searchPlaceholder')" />
        </label>
      </div>
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="mb-4 grid grid-cols-1 gap-4 md:grid-cols-3">
      <article class="stat">
        <p class="stat-label">{{ t('sellerDebts.sellers') }}</p>
        <h4 class="stat-value">{{ filtered.length }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('sellerDebts.clients') }}</p>
        <h4 class="stat-value">{{ clientCount }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">{{ t('sellerDebts.totalDebt') }}</p>
        <h4 class="stat-value debt">{{ totalsText(grandTotals) }}</h4>
      </article>
    </div>

    <div v-if="loading" class="card empty">{{ t('common.loading') }}</div>
    <div v-else-if="filtered.length === 0" class="card empty">{{ t('sellerDebts.empty') }}</div>

    <div v-for="s in filtered" :key="s.userId" class="card mb-4">
      <div class="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ s.userFullName || '—' }}</h3>
          <p class="text-sm text-gray-500 dark:text-gray-400">{{ t('sellerDebts.clientsCount', { n: s.clients.length }) }}</p>
        </div>
        <span class="text-lg font-bold text-error-600 dark:text-error-400">{{ totalsText(debtTotals(s.clients)) }}</span>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">{{ t('common.client') }}</th>
              <th class="th">{{ t('sellerDebts.orders') }}</th>
              <th class="th">{{ t('common.total') }}</th>
              <th class="th">{{ t('sellerDebts.paid') }}</th>
              <th class="th">{{ t('sellerDebts.debt') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in s.clients" :key="`${s.userId}-${c.clientId ?? 0}-${c.currency || 'UZS'}`" class="border-b border-gray-100 align-top dark:border-gray-800">
              <td class="td">
                <router-link v-if="c.clientId" :to="`/clients/${c.clientId}`" class="font-medium text-brand-500 hover:underline">
                  {{ c.clientFullName }}
                </router-link>
                <span v-else class="font-medium">{{ t('sellerDebts.noClient') }}</span>
                <div v-if="c.phone" class="text-xs text-gray-400">{{ formatUzPhone(c.phone) }}</div>
              </td>
              <td class="td">
                <div class="flex flex-wrap gap-1">
                  <router-link
                    v-for="o in c.orders"
                    :key="o.saleOrderId"
                    :to="`/sales/${o.saleOrderId}`"
                    class="order-chip"
                    :title="`${formatDate(o.orderDate)} · ${amt(o.totalSum, o.currency)}`"
                  >
                    #{{ o.saleOrderId }} · {{ amt(o.debtSum, o.currency) }}
                  </router-link>
                </div>
              </td>
              <td class="td whitespace-nowrap">{{ amt(c.totalSum, c.currency) }}</td>
              <td class="td whitespace-nowrap text-success-600 dark:text-success-400">{{ amt(c.paidSum, c.currency) }}</td>
              <td class="td whitespace-nowrap font-semibold text-error-600 dark:text-error-400">{{ amt(c.debt, c.currency) }}</td>
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
import { fetchSellerDebts, type SellerClientDebt, type SellerDebt } from '@/api/sales'
import { BASE_CURRENCY, CURRENCIES, moneyIn, type CurrencyCode } from '@/utils/currency'
import { fetchUserOptions, type UserItem } from '@/api/users'
import { formatApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { formatDate, money } from '@/utils/format'
import { formatUzPhone } from '@/utils/phone'

const { t } = useI18n()
const auth = useAuthStore()
const canViewAll = computed(() => auth.can('DEBT_NOTIFICATION_VIEW'))

const sellers = ref<SellerDebt[]>([])
const users = ref<UserItem[]>([])
const userId = ref(0)
const startDate = ref('')
const endDate = ref('')
const search = ref('')
const loading = ref(false)
const error = ref<string | null>(null)

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return sellers.value
  return sellers.value
    .map((s) => {
      const clients = s.clients.filter((c) =>
        [c.clientFullName, c.phone, ...c.orders.map((o) => `#${o.saleOrderId}`)].some((v) =>
          String(v || '').toLowerCase().includes(q),
        ),
      )
      return { ...s, clients }
    })
    .filter((s) => s.clients.length > 0 || String(s.userFullName || '').toLowerCase().includes(q))
})

const clientCount = computed(
  () => new Set(filtered.value.flatMap((s) => s.clients.map((c) => `${s.userId}-${c.clientId ?? 0}`))).size,
)

function amt(v: number | null | undefined, currency?: CurrencyCode) {
  return currency && currency !== BASE_CURRENCY ? moneyIn(v, currency) : money(v)
}

/** Valyuta bo'yicha alohida jami: so'm va dollar qo'shilmaydi. */
function debtTotals(clients: SellerClientDebt[]) {
  const totals = new Map<CurrencyCode, number>()
  for (const c of clients) {
    const cur = c.currency || BASE_CURRENCY
    totals.set(cur, (totals.get(cur) || 0) + Number(c.debt || 0))
  }
  return CURRENCIES.filter((c) => totals.has(c)).map((c) => ({ currency: c, amount: totals.get(c) || 0 }))
}

function totalsText(totals: { currency: CurrencyCode; amount: number }[]) {
  if (!totals.length) return money(0)
  return totals.map((x) => amt(x.amount, x.currency)).join(' · ')
}

const grandTotals = computed(() => debtTotals(filtered.value.flatMap((s) => s.clients)))

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchSellerDebts({
      userId: userId.value || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
    })
    sellers.value = res.data || []
  } catch (e) {
    error.value = formatApiError(e, t('common.loadError'))
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  if (canViewAll.value) {
    try {
      users.value = (await fetchUserOptions()).data || []
    } catch {
      users.value = []
    }
  }
  await load()
})
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.lbl-block { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.8125rem; font-weight: 500; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.th { padding: 0.75rem 1.25rem; text-align: start; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.stat { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.stat-label { font-size: 0.875rem; color: #4b5563; }
.stat-value { margin-top: 0.25rem; font-size: 1.25rem; font-weight: 700; color: #1f2937; }
.stat-value.debt { color: #dc2626; }
.order-chip { display: inline-flex; border-radius: 0.375rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.1rem 0.45rem; font-size: 0.75rem; color: #b42318; white-space: nowrap; }
.order-chip:hover { border-color: #f97066; }
.dark .card, .dark .stat { border-color: #1f2937; background: rgb(255 255 255 / 3%); }
.dark .field { border-color: #344054; color: rgba(255, 255, 255, 0.9); }
.dark .field option { background: #101828; }
.dark .lbl-block { color: #9ca3af; }
.dark .td { color: #9ca3af; }
.dark .stat-label { color: #9ca3af; }
.dark .stat-value { color: rgba(255, 255, 255, 0.92); }
.dark .stat-value.debt { color: #f87171; }
.dark .order-chip { border-color: rgb(240 68 56 / 35%); background: rgb(240 68 56 / 10%); color: #fda29b; }
</style>
