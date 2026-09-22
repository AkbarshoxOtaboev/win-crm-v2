<template>
  <div ref="root" class="relative w-full min-w-0">
    <button
      type="button"
      class="field-btn"
      :class="{ open: open, disabled }"
      :disabled="disabled"
      @click="toggle"
    >
      <span class="truncate" :class="selectedLabel ? 'text-gray-800' : 'text-gray-400'">
        {{ selectedLabel || placeholder }}
      </span>
      <svg class="h-4 w-4 shrink-0 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
        <path
          fill-rule="evenodd"
          d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z"
          clip-rule="evenodd"
        />
      </svg>
    </button>

    <div v-if="open" class="dropdown">
      <input
        ref="searchInput"
        v-model="query"
        type="search"
        class="search"
        :placeholder="searchPlaceholder"
        @keydown.esc.prevent="close"
        @keydown.enter.prevent="pickFirst"
      />
      <ul class="options">
        <li v-if="filtered.length === 0" class="empty">Topilmadi</li>
        <li
          v-for="opt in filtered"
          :key="opt.value"
          class="option"
          :class="{ active: Number(opt.value) === Number(modelValue) }"
          @mousedown.prevent="select(opt.value)"
        >
          {{ opt.label }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

export interface SearchableOption {
  value: number
  label: string
  /** Extra text for search (e.g. phone, FIO) — not shown in the button */
  searchText?: string
}

const props = withDefaults(
  defineProps<{
    modelValue: number
    options: SearchableOption[]
    placeholder?: string
    searchPlaceholder?: string
    disabled?: boolean
  }>(),
  {
    placeholder: 'Tanlang...',
    searchPlaceholder: 'Qidirish...',
    disabled: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

const open = ref(false)
const query = ref('')
const root = ref<HTMLElement | null>(null)
const searchInput = ref<HTMLInputElement | null>(null)

const selectedLabel = computed(() => {
  const found = props.options.find((o) => Number(o.value) === Number(props.modelValue))
  return found?.label || ''
})

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return props.options
  const qDigits = q.replace(/\D/g, '')
  return props.options.filter((o) => {
    const hay = `${o.label} ${o.searchText || ''}`.toLowerCase()
    if (hay.includes(q)) return true
    if (qDigits.length >= 2) {
      const digits = `${o.label} ${o.searchText || ''}`.replace(/\D/g, '')
      if (digits.includes(qDigits)) return true
    }
    return false
  })
})

function toggle() {
  if (props.disabled) return
  open.value = !open.value
  if (open.value) {
    query.value = ''
    nextTick(() => searchInput.value?.focus())
  }
}

function close() {
  open.value = false
  query.value = ''
}

function select(value: number) {
  emit('update:modelValue', value)
  close()
}

function pickFirst() {
  const first = filtered.value[0]
  if (first) select(first.value)
}

function onDocClick(e: MouseEvent) {
  if (!root.value?.contains(e.target as Node)) close()
}

watch(open, (v) => {
  if (v) document.addEventListener('mousedown', onDocClick)
  else document.removeEventListener('mousedown', onDocClick)
})

onBeforeUnmount(() => document.removeEventListener('mousedown', onDocClick))
onMounted(() => {})
</script>

<style scoped>
.field-btn {
  display: flex;
  height: 2.75rem;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  text-align: left;
}
.field-btn.open {
  border-color: #465fff;
  box-shadow: 0 0 0 2px rgba(70, 95, 255, 0.15);
}
.field-btn.disabled,
.field-btn:disabled {
  background: #f9fafb;
  color: #6b7280;
  cursor: not-allowed;
}
.dropdown {
  position: absolute;
  z-index: 50;
  margin-top: 0.25rem;
  width: 100%;
  overflow: hidden;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  background: #fff;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.08);
}
.search {
  height: 2.5rem;
  width: 100%;
  border: 0;
  border-bottom: 1px solid #f3f4f6;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  outline: none;
}
.options {
  max-height: 14rem;
  overflow-y: auto;
  padding: 0.25rem;
}
.option {
  cursor: pointer;
  border-radius: 0.375rem;
  padding: 0.5rem 0.65rem;
  font-size: 0.875rem;
  color: #374151;
}
.option:hover,
.option.active {
  background: #eff4ff;
  color: #465fff;
}
.empty {
  padding: 0.75rem;
  text-align: center;
  font-size: 0.8125rem;
  color: #9ca3af;
}
</style>
