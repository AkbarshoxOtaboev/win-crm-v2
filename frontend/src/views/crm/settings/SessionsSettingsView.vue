<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Faol sessiyalar" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <p class="text-sm text-gray-600 dark:text-gray-400">
          Faol: {{ summary?.active ?? '—' }} / {{ summary?.total ?? '—' }}
        </p>
        <button type="button" class="ghost" @click="onRevokeOthers">Boshqalarni yopish</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">User</th>
              <th class="th">Status</th>
              <th class="th">IP</th>
              <th class="th text-right">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in sessions" :key="s.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ s.id }}</td>
              <td class="td">{{ s.username }}</td>
              <td class="td">{{ s.status }}</td>
              <td class="td">{{ s.ipAddress || '—' }}</td>
              <td class="td text-right">
                <button type="button" class="text-error-500" @click="onRevoke(s.id)">Yopish</button>
              </td>
            </tr>
            <tr v-if="sessions.length === 0">
              <td colspan="5" class="empty">Sessiya yo‘q</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchSessions,
  fetchSessionSummary,
  heartbeat,
  revokeOtherSessions,
  revokeSession,
  type SessionItem,
  type SessionSummary,
} from '@/api/sessions'
import { formatApiError } from '@/api/http'

const error = ref<string | null>(null)
const sessions = ref<SessionItem[]>([])
const summary = ref<SessionSummary | null>(null)

async function load() {
  try {
    await heartbeat()
    const [s, sum] = await Promise.all([fetchSessions(), fetchSessionSummary()])
    sessions.value = s.data || []
    summary.value = sum.data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onRevoke(id: number) {
  try {
    await revokeSession(id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onRevokeOthers() {
  try {
    await revokeOtherSessions()
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
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
.empty {
  padding: 2rem;
  text-align: center;
  font-size: 0.875rem;
  color: #6b7280;
}
.ghost {
  height: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1rem;
  font-size: 0.875rem;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
}
</style>
