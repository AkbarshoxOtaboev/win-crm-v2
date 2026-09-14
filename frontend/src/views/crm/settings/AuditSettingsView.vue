<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Audit loglar" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">User</th>
              <th class="th">Action</th>
              <th class="th">Entity</th>
              <th class="th">Sana</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in audits" :key="a.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ a.id }}</td>
              <td class="td">{{ a.username }}</td>
              <td class="td">{{ a.action }}</td>
              <td class="td">{{ a.entity }} {{ a.entityId || '' }}</td>
              <td class="td">{{ formatDate(a.createdAt) }}</td>
            </tr>
            <tr v-if="audits.length === 0">
              <td colspan="5" class="empty">Log yo‘q</td>
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
import { fetchAuditLogs, type AuditLog } from '@/api/audit'
import { formatApiError } from '@/api/http'
import { formatDate } from '@/utils/format'

const error = ref<string | null>(null)
const audits = ref<AuditLog[]>([])

onMounted(async () => {
  try {
    audits.value = (await fetchAuditLogs()).data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
})
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
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
}
</style>
