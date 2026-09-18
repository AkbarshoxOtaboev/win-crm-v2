<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Sotuv hisoboti" />

    <div class="mb-4 flex flex-wrap items-center gap-2">
      <input v-model="startDate" type="date" class="field" @change="load" />
      <input v-model="endDate" type="date" class="field" @change="load" />
    </div>

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="mb-4 grid grid-cols-1 gap-4 md:grid-cols-4">
      <article class="stat">
        <p class="stat-label">Buyurtmalar</p>
        <h4 class="stat-value">{{ orders.length }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">Jami sotuv</p>
        <h4 class="stat-value">{{ money(totals.sales) }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">To‘langan</p>
        <h4 class="stat-value text-emerald-600">{{ money(totals.paid) }}</h4>
      </article>
      <article class="stat">
        <p class="stat-label">Qarz</p>
        <h4 class="stat-value text-red-600">{{ money(totals.debt) }}</h4>
      </article>
    </div>

    <div class="card mb-4">
      <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Buyurtmalar</h3>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Sana</th>
              <th class="th">Mijoz</th>
              <th class="th">Sotuvchi</th>
              <th class="th">Jami</th>
              <th class="th">To‘langan</th>
              <th class="th">Qarz</th>
              <th class="th">Holat</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading"><td colspan="8" class="empty">Yuklanmoqda...</td></tr>
            <tr v-else-if="orders.length === 0"><td colspan="8" class="empty">Hisobot uchun savdo yo‘q</td></tr>
            <tr v-for="o in orders" :key="o.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">
                <router-link :to="`/sales/${o.id}`" class="text-brand-500 hover:underline">#{{ o.id }}</router-link>
              </td>
              <td class="td">{{ formatDate(o.orderDate) }}</td>
              <td class="td">{{ o.clientFullName || '—' }}</td>
              <td class="td">{{ o.userFullName || '—' }}</td>
              <td class="td">{{ money(o.totalSum) }}</td>
              <td class="td">{{ money(o.paidSum) }}</td>
              <td class="td">{{ money(o.debtSum) }}</td>
              <td class="td">{{ o.orderStatus || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <div class="card">
        <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Sotuvchi bo‘yicha</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">Sotuvchi</th>
                <th class="th">Soni</th>
                <th class="th">Jami</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="bySeller.length === 0"><td colspan="3" class="empty">Yo‘q</td></tr>
              <tr v-for="row in bySeller" :key="row.name" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td">{{ row.name }}</td>
                <td class="td">{{ row.count }}</td>
                <td class="td">{{ money(row.sum) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div class="card">
        <div class="border-b border-gray-100 px-5 py-4 dark:border-gray-800">
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Mijoz bo‘yicha</h3>
        </div>
        <div class="overflow-x-auto">
          <table class="min-w-full">
            <thead>
              <tr class="border-b border-gray-100 dark:border-gray-800">
                <th class="th">Mijoz</th>
                <th class="th">Soni</th>
                <th class="th">Jami</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="byClient.length === 0"><td colspan="3" class="empty">Yo‘q</td></tr>
              <tr v-for="row in byClient" :key="row.name" class="border-b border-gray-100 dark:border-gray-800">
                <td class="td">{{ row.name }}</td>
                <td class="td">{{ row.count }}</td>
                <td class="td">{{ money(row.sum) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchSaleOrdersByDateRange, type SaleOrder } from '@/api/sales'
import { formatApiError } from '@/api/http'
import { formatDate, money, today } from '@/utils/format'

interface GroupRow {
  name: string
  count: number
  sum: number
}

const startDate = ref(monthStart())
const endDate = ref(today())
const loading = ref(false)
const error = ref<string | null>(null)
const orders = ref<SaleOrder[]>([])

const totals = computed(() => ({
  sales: orders.value.reduce((s, o) => s + Number(o.totalSum || 0), 0),
  paid: orders.value.reduce((s, o) => s + Number(o.paidSum || 0), 0),
  debt: orders.value.reduce((s, o) => s + Number(o.debtSum || 0), 0),
}))

const bySeller = computed(() => groupBy(orders.value, (o) => o.userFullName || '—'))
const byClient = computed(() => groupBy(orders.value, (o) => o.clientFullName || '—'))

function monthStart() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

function groupBy(list: SaleOrder[], keyFn: (o: SaleOrder) => string): GroupRow[] {
  const map = new Map<string, GroupRow>()
  for (const o of list) {
    const name = keyFn(o)
    const cur = map.get(name) || { name, count: 0, sum: 0 }
    cur.count += 1
    cur.sum += Number(o.totalSum || 0)
    map.set(name, cur)
  }
  return [...map.values()].sort((a, b) => b.sum - a.sum)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchSaleOrdersByDateRange(`${startDate.value}T00:00:00`, `${endDate.value}T23:59:59`)
    orders.value = [...(res.data || [])].sort((a, b) => String(b.orderDate || '').localeCompare(String(a.orderDate || '')))
  } catch (e) {
    error.value = formatApiError(e, 'Hisobotni yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.field { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; background: transparent; padding: 0 0.75rem; font-size: 0.875rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem 1rem; font-size: 0.875rem; color: #dc2626; }
.card { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; }
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; font-weight: 500; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1.25rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.stat { border-radius: 1rem; border: 1px solid #e5e7eb; background: #fff; padding: 1.25rem; }
.stat-label { font-size: 0.875rem; color: #4b5563; }
.stat-value { margin-top: 0.25rem; font-size: 1.25rem; font-weight: 700; color: #1f2937; }
</style>
