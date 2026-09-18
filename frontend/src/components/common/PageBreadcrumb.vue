<template>
  <div class="mb-6 flex flex-wrap items-center justify-between gap-3">
    <h2 class="text-xl font-semibold text-gray-800 dark:text-white/90">
      {{ pageTitle }}
    </h2>
    <nav>
      <ol class="flex flex-wrap items-center gap-1.5">
        <li v-for="(item, index) in crumbs" :key="`${item.label}-${index}`" class="flex items-center gap-1.5">
          <router-link
            v-if="item.to && index < crumbs.length - 1"
            class="text-sm text-gray-500 dark:text-gray-400 hover:text-brand-500"
            :to="item.to"
          >
            {{ item.label }}
          </router-link>
          <span
            v-else
            class="text-sm"
            :class="
              index === crumbs.length - 1
                ? 'text-gray-800 dark:text-white/90'
                : 'text-gray-500 dark:text-gray-400'
            "
          >
            {{ item.label }}
          </span>
          <svg
            v-if="index < crumbs.length - 1"
            class="stroke-current text-gray-500 dark:text-gray-400"
            width="17"
            height="16"
            viewBox="0 0 17 16"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M6.0765 12.667L10.2432 8.50033L6.0765 4.33366"
              stroke=""
              stroke-width="1.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </li>
      </ol>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export interface BreadcrumbItem {
  label: string
  to?: string
}

const props = defineProps<{
  pageTitle: string
  items?: BreadcrumbItem[]
}>()

const { t } = useI18n()

const crumbs = computed<BreadcrumbItem[]>(() => {
  if (props.items?.length) return props.items
  return [
    { label: t('nav.home'), to: '/' },
    { label: props.pageTitle },
  ]
})
</script>
