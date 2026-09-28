<template>
  <span class="badge" :class="`badge-${(status || 'none').toLowerCase()}`">
    <span class="dot" />
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ status?: string | null }>()
const { t, te } = useI18n()

const label = computed(() => {
  if (!props.status) return '—'
  const key = `deliveryStatus.${props.status}`
  return te(key) ? t(key) : props.status
})
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  border-radius: 9999px;
  padding: 0.125rem 0.625rem;
  font-size: 0.75rem;
  font-weight: 500;
  line-height: 1.25rem;
  white-space: nowrap;
  background: #f3f4f6;
  color: #4b5563;
}
.dot { height: 0.375rem; width: 0.375rem; border-radius: 9999px; background: currentColor; }
.badge-pending { background: #fffbeb; color: #b45309; }
.badge-accepted { background: #eff6ff; color: #2563eb; }
.badge-in_transit { background: #ecfeff; color: #0e7490; }
.badge-arrived { background: #f5f3ff; color: #7c3aed; }
.badge-confirmed { background: #ecfdf5; color: #059669; }
.badge-cancelled { background: #fef2f2; color: #dc2626; }
.dark .badge { background: rgb(255 255 255 / 6%); color: #d1d5db; }
.dark .badge-pending { background: rgb(245 158 11 / 15%); color: #fcd34d; }
.dark .badge-accepted { background: rgb(59 130 246 / 15%); color: #93c5fd; }
.dark .badge-in_transit { background: rgb(6 182 212 / 15%); color: #67e8f9; }
.dark .badge-arrived { background: rgb(139 92 246 / 18%); color: #c4b5fd; }
.dark .badge-confirmed { background: rgb(16 185 129 / 15%); color: #6ee7b7; }
.dark .badge-cancelled { background: rgb(239 68 68 / 15%); color: #fca5a5; }
</style>
