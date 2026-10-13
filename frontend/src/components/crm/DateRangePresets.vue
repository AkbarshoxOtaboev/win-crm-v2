<template>
  <div class="presets" role="group">
    <button
      v-for="p in PRESETS"
      :key="p"
      type="button"
      class="preset"
      :class="{ active: active === p }"
      :aria-pressed="active === p"
      @click="apply(p)"
    >
      {{ t(`common.periods.${p}`) }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const PRESETS = ['today', 'week', 'month', 'year'] as const
type Preset = (typeof PRESETS)[number]

const start = defineModel<string>('start', { required: true })
const end = defineModel<string>('end', { required: true })
const { t } = useI18n()

const active = computed<Preset | null>(() =>
  end.value === iso(new Date()) ? PRESETS.find((p) => presetStart(p) === start.value) || null : null,
)

function iso(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function presetStart(p: Preset) {
  const d = new Date()
  if (p === 'week') d.setDate(d.getDate() - ((d.getDay() + 6) % 7))
  if (p === 'month') d.setDate(1)
  if (p === 'year') d.setMonth(0, 1)
  return iso(d)
}

function apply(p: Preset) {
  start.value = presetStart(p)
  end.value = iso(new Date())
}
</script>

<style scoped>
.presets { display: inline-flex; height: 2.5rem; align-items: center; gap: 0.25rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0.25rem; }
.preset { height: 100%; border-radius: 0.375rem; padding: 0 0.75rem; font-size: 0.8125rem; font-weight: 500; color: #6b7280; white-space: nowrap; }
.preset:hover:not(.active) { background: #f3f4f6; }
.preset.active { background: #465fff; color: #fff; }
:global(html.dark .presets) { border-color: #374151; }
:global(html.dark .preset:not(.active)) { color: #9ca3af; }
:global(html.dark .preset:hover:not(.active)) { background: #1f2937; }
</style>
