<template>
  <div class="relative min-w-0">
    <template v-if="auth.superAdmin">
      <Building2 class="pointer-events-none absolute start-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500 dark:text-gray-400" />
      <select
        :value="auth.selectedFilialId ?? 'all'"
        class="h-10 max-w-[200px] cursor-pointer appearance-none truncate rounded-lg border border-gray-200 bg-white ps-9 pe-8 text-sm font-medium text-gray-700 transition hover:bg-gray-50 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-200 dark:hover:bg-white/[0.03]"
        :title="t('shared.allFilials')"
        @change="onChange"
      >
        <option value="all">{{ t('shared.allFilials') }}</option>
        <option v-for="item in filials" :key="item.id" :value="item.id">{{ item.name }}</option>
      </select>
      <ChevronDown class="pointer-events-none absolute end-2.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-500 dark:text-gray-400" />
    </template>
    <div
      v-else-if="auth.assignedFilialName"
      class="flex h-10 max-w-[200px] items-center gap-2 rounded-lg border border-gray-200 bg-gray-50 px-3 text-sm font-medium text-gray-700 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-200"
      :title="auth.assignedFilialName"
    >
      <Building2 class="h-4 w-4 shrink-0 text-gray-500 dark:text-gray-400" />
      <span class="truncate">{{ auth.assignedFilialName }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Building2, ChevronDown } from 'lucide-vue-next'
import { fetchFilials, type FilialItem } from '@/api/filials'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
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
