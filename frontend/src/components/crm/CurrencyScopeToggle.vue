<template>
  <div class="scope-toggle" role="radiogroup" :aria-label="t('reportCurrency.hint')">
    <button
      v-for="o in options"
      :key="o.id"
      type="button"
      role="radio"
      class="scope-option"
      :class="{ active: modelValue === o.id }"
      :aria-checked="modelValue === o.id"
      :title="o.hint"
      @click="select(o.id)"
    >
      <component :is="o.icon" class="h-3.5 w-3.5" />
      {{ o.label }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Coins, DollarSign, Layers } from 'lucide-vue-next'
import type { CurrencyCode } from '@/utils/currency'

/** ALL - hammasi so'mga o'girib jamlanadi; UZS / USD - faqat shu valyutadagi hujjatlar. */
export type CurrencyScope = 'ALL' | CurrencyCode

const props = defineProps<{ modelValue: CurrencyScope }>()
const emit = defineEmits<{ 'update:modelValue': [value: CurrencyScope] }>()
const { t } = useI18n()

const options = computed(() => [
  { id: 'ALL' as const, label: t('reportCurrency.all'), hint: t('reportCurrency.allHint'), icon: Layers },
  { id: 'UZS' as const, label: t('common.currency'), hint: t('reportCurrency.onlyUzsHint'), icon: Coins },
  { id: 'USD' as const, label: '$', hint: t('reportCurrency.onlyUsdHint'), icon: DollarSign },
])

function select(scope: CurrencyScope) {
  if (scope !== props.modelValue) emit('update:modelValue', scope)
}
</script>

<style scoped>
.scope-toggle {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.25rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0.25rem;
}
.scope-option {
  display: inline-flex;
  height: 100%;
  min-width: 2.5rem;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  border-radius: 0.375rem;
  padding: 0 0.75rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #6b7280;
  transition: background-color 0.15s, color 0.15s;
}
.scope-option:not(.active):hover {
  background: #f3f4f6;
  color: #1f2937;
}
.scope-option.active {
  background: #465fff;
  color: #fff;
}
:global(html.dark .scope-toggle) {
  border-color: #374151;
}
:global(html.dark .scope-option:not(.active)) {
  color: #9ca3af;
}
:global(html.dark .scope-option:not(.active):hover) {
  background: rgba(255, 255, 255, 0.05);
  color: #e5e7eb;
}
</style>
