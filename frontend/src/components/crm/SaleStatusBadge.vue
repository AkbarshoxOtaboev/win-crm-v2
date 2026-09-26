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
  const key = `saleStatus.${props.status}`
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
.badge-new { background: #eff6ff; color: #2563eb; }
.badge-confirmed { background: #fffbeb; color: #b45309; }
.badge-processing { background: #fff7ed; color: #c2410c; }
.badge-ready { background: #f5f3ff; color: #7c3aed; }
.badge-delivered { background: #ecfdf5; color: #059669; }
.badge-completed { background: #d1fae5; color: #047857; }
.badge-cancelled { background: #fef2f2; color: #dc2626; }
.dark .badge { background: rgb(255 255 255 / 6%); color: #d1d5db; }
.dark .badge-new { background: rgb(59 130 246 / 15%); color: #93c5fd; }
.dark .badge-confirmed { background: rgb(245 158 11 / 15%); color: #fcd34d; }
.dark .badge-processing { background: rgb(249 115 22 / 15%); color: #fdba74; }
.dark .badge-ready { background: rgb(139 92 246 / 18%); color: #c4b5fd; }
.dark .badge-delivered { background: rgb(16 185 129 / 15%); color: #6ee7b7; }
.dark .badge-completed { background: rgb(5 150 105 / 25%); color: #34d399; }
.dark .badge-cancelled { background: rgb(239 68 68 / 15%); color: #fca5a5; }
</style>
