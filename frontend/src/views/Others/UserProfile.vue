<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Profil" />
    <div v-if="error" class="mb-4 rounded-lg border border-error-200 bg-error-50 px-4 py-3 text-sm text-error-600">
      {{ error }}
    </div>
    <div class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03]">
      <div v-if="!user" class="text-sm text-gray-500">Yuklanmoqda...</div>
      <div v-else class="space-y-3 text-sm">
        <p><b>Login:</b> {{ user.username }}</p>
        <p><b>F.I.Sh:</b> {{ user.fullName || '—' }}</p>
        <p><b>Telefon:</b> {{ user.phone || '—' }}</p>
        <p><b>Status:</b> {{ user.status || '—' }}</p>
        <p><b>Rollar:</b> {{ (user.role || []).map((r) => r.name).join(', ') || '—' }}</p>
        <div v-if="stats" class="grid grid-cols-3 gap-3 pt-3">
          <div>Savdolar: {{ stats.orderCount ?? '—' }}</div>
          <div>To‘lovlar: {{ stats.paymentSum ?? '—' }}</div>
          <div>Qarz: {{ stats.debtSum ?? '—' }}</div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchUser, fetchUsers, fetchUserStats, type UserItem, type UserStat } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'

const auth = useAuthStore()
const user = ref<UserItem | null>(null)
const stats = ref<UserStat | null>(null)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const list = await fetchUsers()
    const me = (list.data || []).find((u) => u.username === auth.username)
    if (!me) return
    user.value = (await fetchUser(me.id)).data
    try {
      stats.value = (await fetchUserStats(me.id)).data
    } catch {
      /* stats optional */
    }
  } catch (e) {
    error.value = formatApiError(e)
  }
})
</script>
