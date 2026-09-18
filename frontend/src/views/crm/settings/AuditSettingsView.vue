<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Audit loglar" />

    <div class="card">
      <div class="head">
        <div>
          <h3 class="title">Audit loglar</h3>
          <p class="sub">Tizim amallarini filtrlash va sahifalash</p>
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
        <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
          <label class="lbl">
            Foydalanuvchi
            <select v-model="filters.username" class="field">
              <option value="">Barchasi</option>
              <option v-for="u in users" :key="u.id" :value="u.username">
                {{ u.fullName || u.username }}
              </option>
            </select>
          </label>
          <label class="lbl">
            Sana (dan)
            <input v-model="filters.fromDate" type="date" class="field" />
          </label>
          <label class="lbl">
            Sana (gacha)
            <input v-model="filters.toDate" type="date" class="field" />
          </label>
          <label class="lbl">
            Sahifada
            <select v-model.number="pageSizePreset" class="field" @change="onPresetChange">
              <option v-for="n in PAGE_SIZE_OPTIONS" :key="n" :value="n">{{ n }} ta</option>
              <option :value="-1">Boshqa...</option>
            </select>
          </label>
          <label class="lbl">
            Son (qo‘lda)
            <input
              v-model.number="customPageSize"
              type="number"
              min="1"
              max="500"
              class="field"
              placeholder="Masalan: 75"
              @change="onCustomSizeChange"
            />
          </label>
        </div>
        <div class="mt-3 flex flex-wrap items-center gap-2">
          <button type="button" class="btn" :disabled="loading" @click="applyFilters">Filtrlash</button>
          <button type="button" class="ghost" @click="clearFilters">Filterni tozalash</button>
          <span class="text-sm text-gray-500">Jami: {{ totalElements }} ta</span>
        </div>
      </div>

      <div v-if="error" class="err mx-5 mb-4">{{ error }}</div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">User</th>
              <th class="th">Method</th>
              <th class="th">Action</th>
              <th class="th">Entity</th>
              <th class="th">URL</th>
              <th class="th">IP</th>
              <th class="th">Sana</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="empty">Yuklanmoqda...</td>
            </tr>
            <tr v-else-if="audits.length === 0">
              <td colspan="8" class="empty">Log yo‘q</td>
            </tr>
            <template v-else>
              <tr
                v-for="a in audits"
                :key="a.id"
                class="border-b border-gray-100 dark:border-gray-800"
              >
                <td class="td">{{ a.id }}</td>
                <td class="td">{{ a.username || '—' }}</td>
                <td class="td">
                  <span class="method-badge" :class="methodClass(a.httpMethod)">
                    {{ a.httpMethod || '—' }}
                  </span>
                </td>
                <td class="td">{{ a.action || '—' }}</td>
                <td class="td">{{ a.entity || '—' }}</td>
                <td class="td url-cell" :title="a.requestUrl">{{ a.requestUrl || '—' }}</td>
                <td class="td">{{ a.ipAddress || '—' }}</td>
                <td class="td">{{ formatDate(a.createdAt) }}</td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 0" class="pager">
        <button type="button" class="ghost" :disabled="page <= 0 || loading" @click="goPage(page - 1)">
          Oldingi
        </button>
        <span class="pager-info">
          Sahifa {{ page + 1 }} / {{ totalPages }}
          <span class="text-gray-400">({{ pageSize }} ta)</span>
        </span>
        <button
          type="button"
          class="ghost"
          :disabled="page >= totalPages - 1 || loading"
          @click="goPage(page + 1)"
        >
          Keyingi
        </button>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ListFilter, RefreshCw } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchAuditLogs, type AuditLog } from '@/api/audit'
import { fetchUsers, type UserItem } from '@/api/users'
import { formatApiError } from '@/api/http'
import { formatDate } from '@/utils/format'

const PAGE_SIZE_OPTIONS = [50, 100, 150, 200, 250] as const

const error = ref<string | null>(null)
const loading = ref(false)
const showFilters = ref(true)
const audits = ref<AuditLog[]>([])
const users = ref<UserItem[]>([])

const page = ref(0)
const pageSize = ref(50)
const pageSizePreset = ref<number>(50)
const customPageSize = ref<number | null>(null)
const totalElements = ref(0)
const totalPages = ref(0)

const filters = reactive({
  username: '',
  fromDate: '',
  toDate: '',
})

function methodClass(method?: string) {
  switch ((method || '').toUpperCase()) {
    case 'GET':
      return 'method-get'
    case 'POST':
      return 'method-post'
    case 'PUT':
    case 'PATCH':
      return 'method-put'
    case 'DELETE':
      return 'method-delete'
    default:
      return 'method-other'
  }
}

function toFromDateIso(date: string) {
  return date ? `${date}T00:00:00` : undefined
}

function toToDateIso(date: string) {
  return date ? `${date}T23:59:59` : undefined
}

function onPresetChange() {
  if (pageSizePreset.value === -1) {
    if (customPageSize.value && customPageSize.value > 0) {
      pageSize.value = Math.min(500, Math.floor(customPageSize.value))
    }
    return
  }
  pageSize.value = pageSizePreset.value
  customPageSize.value = pageSizePreset.value
}

function onCustomSizeChange() {
  const n = Number(customPageSize.value)
  if (!Number.isFinite(n) || n < 1) return
  pageSize.value = Math.min(500, Math.floor(n))
  pageSizePreset.value = PAGE_SIZE_OPTIONS.includes(pageSize.value as (typeof PAGE_SIZE_OPTIONS)[number])
    ? pageSize.value
    : -1
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await fetchAuditLogs({
      page: page.value,
      size: pageSize.value,
      username: filters.username || undefined,
      fromDate: toFromDateIso(filters.fromDate),
      toDate: toToDateIso(filters.toDate),
    })
    const data = res.data
    audits.value = data?.content || []
    totalElements.value = data?.page?.totalElements ?? 0
    totalPages.value = data?.page?.totalPages ?? 0
    if (data?.page) {
      page.value = data.page.number
      pageSize.value = data.page.size
    }
  } catch (e) {
    error.value = formatApiError(e)
    audits.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 0
  if (pageSizePreset.value === -1) {
    onCustomSizeChange()
  } else {
    pageSize.value = pageSizePreset.value
    customPageSize.value = pageSizePreset.value
  }
  load()
}

function clearFilters() {
  filters.username = ''
  filters.fromDate = ''
  filters.toDate = ''
  page.value = 0
  pageSize.value = 50
  pageSizePreset.value = 50
  customPageSize.value = 50
  load()
}

function goPage(p: number) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  load()
}

onMounted(async () => {
  customPageSize.value = 50
  try {
    users.value = (await fetchUsers()).data || []
  } catch {
    users.value = []
  }
  await load()
})
</script>

<style scoped>
.card {
  border-radius: 1rem;
  border: 1px solid #e5e7eb;
  background: #fff;
}
.dark .card {
  border-color: #1f2937;
  background: rgb(255 255 255 / 0.03);
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  border-bottom: 1px solid #f3f4f6;
  padding: 1rem 1.25rem;
}
.dark .head {
  border-color: #1f2937;
}
.title {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}
.dark .title {
  color: rgb(255 255 255 / 0.9);
}
.sub {
  margin-top: 0.25rem;
  font-size: 0.875rem;
  color: #6b7280;
}
.filters {
  border-bottom: 1px solid #f3f4f6;
  padding: 1rem 1.25rem;
}
.dark .filters {
  border-color: #1f2937;
}
.lbl {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.75rem;
  color: #6b7280;
}
.field {
  height: 2.5rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  color: #374151;
}
.dark .field {
  border-color: #374151;
  background: #111827;
  color: #e5e7eb;
}
.icon-btn {
  display: inline-flex;
  height: 2.25rem;
  width: 2.25rem;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  color: #4b5563;
}
.dark .icon-btn {
  border-color: #374151;
  color: #d1d5db;
}
.btn {
  height: 2.25rem;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: #fff;
}
.btn:disabled {
  opacity: 0.6;
}
.ghost {
  height: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 0.875rem;
  font-size: 0.875rem;
  color: #374151;
  background: transparent;
}
.ghost:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.dark .ghost {
  border-color: #4b5563;
  color: #e5e7eb;
}
.th {
  padding: 0.75rem 1.25rem;
  text-align: left;
  font-size: 0.75rem;
  color: #6b7280;
}
.td {
  padding: 0.75rem 1.25rem;
  font-size: 0.875rem;
  color: #4b5563;
}
.dark .td {
  color: #d1d5db;
}
.url-cell {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty {
  padding: 2rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  border-top: 1px solid #f3f4f6;
  padding: 0.875rem 1.25rem;
}
.dark .pager {
  border-color: #1f2937;
}
.pager-info {
  font-size: 0.875rem;
  color: #4b5563;
}
.method-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 3.5rem;
  border-radius: 9999px;
  padding: 0.15rem 0.55rem;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.method-get {
  background: #eff6ff;
  color: #2563eb;
}
.method-post {
  background: #ecfdf5;
  color: #059669;
}
.method-put {
  background: #fffbeb;
  color: #d97706;
}
.method-delete {
  background: #fef2f2;
  color: #dc2626;
}
.method-other {
  background: #f3f4f6;
  color: #4b5563;
}
.dark .method-get {
  background: rgb(37 99 235 / 0.15);
  color: #60a5fa;
}
.dark .method-post {
  background: rgb(5 150 105 / 0.15);
  color: #34d399;
}
.dark .method-put {
  background: rgb(217 119 6 / 0.15);
  color: #fbbf24;
}
.dark .method-delete {
  background: rgb(220 38 38 / 0.15);
  color: #f87171;
}
.dark .method-other {
  background: rgb(255 255 255 / 0.08);
  color: #d1d5db;
}
</style>
