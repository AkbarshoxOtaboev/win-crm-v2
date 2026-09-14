<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Yetkazib beruvchilar balansi" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">Yetkazib beruvchilar balansi</h3>
          <p class="sub">Xarid, to‘lov va qarzdorlik bo‘yicha umumiy holat</p>
        </div>
        <div class="flex items-center gap-2">
          <button type="button" class="icon-btn" title="Filter" @click="showFilters = !showFilters">
            <ListFilter class="h-4 w-4" />
          </button>
          <button type="button" class="icon-btn" title="Yangilash" :disabled="loading" @click="load">
            <RefreshCw class="h-4 w-4" :class="{ 'animate-spin': loading }" />
          </button>
        </div>
      </div>

      <div v-if="showFilters" class="filters">
        <div class="flex flex-wrap items-end gap-3">
          <label class="lbl min-w-[16rem] flex-1">
            Qidirish
            <input
              v-model="search"
              type="search"
              class="field"
              placeholder="Nom yoki ID bo‘yicha qidirish..."
            />
          </label>
          <label class="check">
            <input v-model="onlyDebtors" type="checkbox" class="h-4 w-4 rounded border-gray-300" />
            Faqat qarzdorlar
          </label>
        </div>
      </div>

      <div class="stats">
        <div class="stat stat-blue">
          <div class="stat-label">Xarid summasi</div>
          <div class="stat-value">{{ moneySom(totals.purchase) }}</div>
        </div>
        <div class="stat stat-green">
          <div class="stat-label">To‘langan</div>
          <div class="stat-value text-emerald-600">{{ moneySom(totals.paid) }}</div>
        </div>
        <div class="stat stat-red">
          <div class="stat-label">Qarz</div>
          <div class="stat-value text-red-600">{{ moneySom(totals.debt) }}</div>
        </div>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100">
              <th class="th">ID</th>
              <th class="th">Yetkazib beruvchi</th>
              <th class="th">Xarid summasi</th>
              <th class="th">To‘langan</th>
              <th class="th">Qarz</th>
              <th class="th">Oxirgi yangilanish</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="7" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="filtered.length === 0"><td colspan="7" class="empty">Balans yo‘q</td></tr>
            <tr v-for="b in filtered" :key="b.supplierId || b.id" class="border-b border-gray-100">
              <td class="td">{{ b.supplierId || b.id }}</td>
              <td class="td">
                <router-link class="link" :to="{ path: '/suppliers', query: { id: String(b.supplierId || '') } }">
                  {{ b.supplierName || b.supplierId }}
                </router-link>
              </td>
              <td class="td">{{ moneySom(b.totalPurchase) }}</td>
              <td class="td text-emerald-600">{{ moneySom(b.totalPaid) }}</td>
              <td class="td text-red-600">{{ moneySom(debtOf(b)) }}</td>
              <td class="td">{{ formatDateTime(b.lastUpdated || b.updatedAt) }}</td>
              <td class="td text-right">
                <router-link class="link" :to="{ path: '/suppliers', query: { id: String(b.supplierId || '') } }">
                  Ochish
                </router-link>
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
import { ListFilter, RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchSupplierBalances, type SupplierBalance } from '@/api/suppliers'
import { formatApiError } from '@/api/http'
import { money } from '@/utils/format'

const items = ref<SupplierBalance[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const showFilters = ref(true)
const search = ref('')
const onlyDebtors = ref(false)

function debtOf(b: SupplierBalance) {
  if (b.totalDebt != null) return Number(b.totalDebt)
  if (b.balance != null) return Number(b.balance)
  return Math.max(0, Number(b.totalPurchase || 0) - Number(b.totalPaid || 0))
}

function moneySom(v?: number | null) {
  return `${money(Number(v || 0))} so‘m`
}

function formatDateTime(v?: string | null) {
  if (!v) return '—'
  const d = new Date(v)
  if (Number.isNaN(d.getTime())) return v.replace('T', ' ').slice(0, 19)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}, ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return [...items.value]
    .filter((b) => {
      if (onlyDebtors.value && debtOf(b) <= 0) return false
      if (!q) return true
      const id = String(b.supplierId || b.id || '')
      const name = (b.supplierName || '').toLowerCase()
      return id.includes(q) || name.includes(q)
    })
    .sort((a, b) => Number(a.supplierId || a.id || 0) - Number(b.supplierId || b.id || 0))
})

const totals = computed(() => {
  return filtered.value.reduce(
    (acc, b) => {
      acc.purchase += Number(b.totalPurchase || 0)
      acc.paid += Number(b.totalPaid || 0)
      acc.debt += debtOf(b)
      return acc
    },
    { purchase: 0, paid: 0, debt: 0 },
  )
})

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchSupplierBalances(0, 500)
    items.value = res.data?.content || []
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.title { font-size: 1.125rem; font-weight: 600; color: #1f2937; }
.sub { margin-top: 0.25rem; font-size: 0.875rem; color: #6b7280; }
.head { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; padding: 1.25rem 1.25rem 1rem; border-bottom: 1px solid #f3f4f6; }
.filters { padding: 1rem 1.25rem; border-bottom: 1px solid #f3f4f6; }
.lbl { display: flex; flex-direction: column; gap: 0.35rem; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.check { display: inline-flex; align-items: center; gap: 0.5rem; height: 2.5rem; font-size: 0.875rem; color: #374151; white-space: nowrap; }
.stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; padding: 1.25rem; }
.stat { border-radius: 0.75rem; border: 1px solid #e5e7eb; background: #fff; padding: 1rem 1.1rem; border-bottom-width: 3px; }
.stat-blue { border-bottom-color: #465fff; }
.stat-green { border-bottom-color: #10b981; }
.stat-red { border-bottom-color: #ef4444; }
.stat-label { font-size: 0.8125rem; color: #6b7280; margin-bottom: 0.35rem; }
.stat-value { font-size: 1.25rem; font-weight: 700; color: #111827; }
.th { padding: 0.75rem 1rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.td { padding: 0.75rem 1rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; color: #6b7280; }
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; font-size: 0.875rem; background: #fff; }
.icon-btn { display: inline-flex; height: 2.5rem; width: 2.5rem; align-items: center; justify-content: center; border-radius: 0.5rem; border: 1px solid #d1d5db; color: #4b5563; background: #fff; }
.link { color: #465fff; font-weight: 500; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
@media (max-width: 768px) {
  .stats { grid-template-columns: 1fr; }
}
</style>
