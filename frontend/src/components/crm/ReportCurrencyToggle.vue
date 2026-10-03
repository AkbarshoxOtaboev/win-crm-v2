<template>
  <div class="cur-toggle" :title="t('reportCurrency.hint')">
    <button
      v-for="c in CURRENCIES"
      :key="c"
      type="button"
      class="cur-option"
      :class="{ active: modelValue === c }"
      @click="select(c)"
    >
      {{ c === 'USD' ? '$' : t('common.currency') }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { CURRENCIES, type CurrencyCode } from '@/utils/currency'

const props = defineProps<{ modelValue: CurrencyCode }>()
const emit = defineEmits<{ 'update:modelValue': [value: CurrencyCode] }>()
const { t } = useI18n()

function select(c: CurrencyCode) {
  if (c !== props.modelValue) emit('update:modelValue', c)
}
</script>

<style scoped>
.cur-toggle {
  display: inline-flex;
  height: 2.5rem;
  align-items: center;
  gap: 0.25rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0.25rem;
}
.cur-option {
  height: 100%;
  min-width: 2.5rem;
  border-radius: 0.375rem;
  padding: 0 0.75rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #6b7280;
}
.cur-option.active {
  background: #465fff;
  color: #fff;
}
:global(html.dark .cur-toggle) {
  border-color: #374151;
}
:global(html.dark .cur-option:not(.active)) {
  color: #9ca3af;
}
</style>
