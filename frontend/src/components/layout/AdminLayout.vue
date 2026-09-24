<template>
  <div class="min-h-screen xl:flex">
    <app-sidebar />
    <Backdrop />
    <div
      class="flex-1 transition-all duration-300 ease-in-out"
      :class="[isExpanded || isHovered ? 'xl:ms-[290px]' : 'xl:ms-[90px]']"
    >
      <app-header />
      <div class="mx-auto max-w-(--breakpoint-2xl) p-4 pb-20 md:p-6 md:pb-6">
        <div
          v-if="writeBlocked"
          class="mb-4 rounded-xl border border-warning-200 bg-warning-50 px-4 py-3 text-sm text-warning-700 dark:border-warning-500/30 dark:bg-warning-500/10 dark:text-warning-400"
        >
          {{ writeBlockedMessage }}
        </div>
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import AppSidebar from './AppSidebar.vue'
import AppHeader from './AppHeader.vue'
import { useSidebar } from '@/composables/useSidebar'
import { useFilialScope } from '@/composables/useFilialScope'
import Backdrop from './Backdrop.vue'
const { isExpanded, isHovered } = useSidebar()
const { writeBlocked, writeBlockedMessage } = useFilialScope()
</script>
