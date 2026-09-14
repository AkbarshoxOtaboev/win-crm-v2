<template>
  <div
    class="mx-auto mb-10 w-full max-w-60 rounded-2xl bg-gray-50 px-4 py-5 text-center dark:bg-white/[0.03]"
  >
    <h3 class="mb-2 font-semibold text-gray-900 dark:text-white">{{ t('sidebar.widgetTitle') }}</h3>
    <p class="mb-0 text-theme-sm text-gray-500 dark:text-gray-400">
      {{ t('sidebar.widgetText') }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>
