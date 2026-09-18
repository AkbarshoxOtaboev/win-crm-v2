<template>
  <div class="relative min-w-0">
    <select
      v-if="auth.superAdmin"
      :value="auth.selectedFilialId ?? 'all'"
      class="h-10 max-w-[220px] rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-700 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-200"
      @change="onChange"
    >
      <option value="all">Barcha filiallar</option>
      <option v-for="item in filials" :key="item.id" :value="item.id">{{ item.name }}</option>
    </select>
    <div
      v-else-if="auth.assignedFilialName"
      class="flex h-10 items-center rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm text-gray-700 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-200"
    >
      {{ auth.assignedFilialName }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchFilials, type FilialItem } from '@/api/filials'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const filials = ref<FilialItem[]>([])

async function load() {
  if (!auth.isAuthenticated) return
  try {
    const res = await fetchFilials()
    filials.value = res.data || []
  } catch {
    filials.value = []
  }
}

function onChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  auth.selectFilial(value === 'all' ? null : Number(value))
  window.location.reload()
}

onMounted(load)
</script>
