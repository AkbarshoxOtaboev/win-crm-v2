<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="title" />
    <div
      class="min-h-screen rounded-2xl border border-gray-200 bg-white px-5 py-7 dark:border-gray-800 dark:bg-white/[0.03] xl:px-10 xl:py-12"
    >
      <div class="mx-auto w-full max-w-[720px] text-center">
        <h3 class="mb-4 font-semibold text-gray-800 text-theme-xl dark:text-white/90 sm:text-2xl">
          {{ title }}
        </h3>
        <p class="text-sm text-gray-500 dark:text-gray-400 sm:text-base">
          {{ description }}
        </p>
        <p
          v-if="apiHint"
          class="mt-4 text-xs text-gray-400 dark:text-gray-500 font-mono"
        >
          API: {{ apiHint }}
        </p>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'

const route = useRoute()

const title = computed(() => (route.meta.title as string) || 'Modul')
const description = computed(
  () =>
    (route.meta.description as string) ||
    'Bu modul keyingi qadamda API ga ulanadi. Hozircha sahifa skeleton.',
)
const apiHint = computed(() => (route.meta.api as string) || '')
</script>
