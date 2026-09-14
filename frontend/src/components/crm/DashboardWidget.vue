<template>
  <div class="h-full rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 dark:border-gray-800 dark:bg-white/[0.03] sm:px-6 sm:pt-6">
    <div class="flex items-start gap-2">
      <button
        type="button"
        class="widget-drag-handle mt-0.5 shrink-0 cursor-grab text-gray-400 hover:text-gray-600 active:cursor-grabbing dark:hover:text-gray-300"
        title="Tartibni o‘zgartirish uchun ushlab torting"
      >
        <GripVertical class="h-5 w-5" />
      </button>
      <div class="min-w-0 flex-1">
        <div class="flex items-center gap-2.5">
          <span
            class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-xl"
            :class="iconWrap"
          >
            <component :is="icon" class="h-4 w-4" :class="iconColor" />
          </span>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ title }}</h3>
        </div>

        <div class="mt-4 flex items-center justify-between px-1">
          <button
            type="button"
            class="inline-flex items-center gap-1 text-xs font-medium text-gray-500 transition hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400"
            @click="toggleSort('name')"
          >
            {{ nameLabel }}
            <ChevronDown
              class="h-3.5 w-3.5 transition"
              :class="sortKey === 'name' ? 'text-brand-500' : 'text-gray-400'"
              :style="sortKey === 'name' && sortDir === 'asc' ? 'transform: rotate(180deg)' : ''"
            />
          </button>
          <button
            type="button"
            class="inline-flex items-center gap-1 text-xs font-medium text-gray-500 transition hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400"
            @click="toggleSort('value')"
          >
            {{ valueLabel }}
            <ChevronDown
              class="h-3.5 w-3.5 transition"
              :class="sortKey === 'value' ? 'text-brand-500' : 'text-gray-400'"
              :style="sortKey === 'value' && sortDir === 'asc' ? 'transform: rotate(180deg)' : ''"
            />
          </button>
        </div>

        <p v-if="sortedRows.length === 0" class="py-12 text-center text-sm text-gray-500">
          Topilmadi
        </p>
        <ul v-else class="mt-3 space-y-2">
          <li
            v-for="(row, i) in sortedRows"
            :key="row.id ?? i"
            class="flex items-center justify-between gap-3 rounded-xl px-2 py-2 text-sm hover:bg-gray-50 dark:hover:bg-white/[0.03]"
          >
            <span class="flex min-w-0 items-center gap-2 text-gray-700 dark:text-gray-300">
              <span
                class="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-semibold"
                :class="badge"
              >
                {{ i + 1 }}
              </span>
              <span class="truncate">{{ row.name }}</span>
            </span>
            <span class="shrink-0 font-semibold" :class="valueColor">{{ format(row.value) }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { ChevronDown, GripVertical } from 'lucide-vue-next'
import { money } from '@/utils/format'

export interface DashboardRow {
  id?: string | number
  name: string
  value: number
}

const props = withDefaults(
  defineProps<{
    title: string
    icon: Component
    nameLabel?: string
    valueLabel?: string
    rows: DashboardRow[]
    formatAs?: 'money' | 'number'
    tone?: 'blue' | 'indigo' | 'violet' | 'green' | 'teal' | 'orange'
  }>(),
  {
    nameLabel: 'Nomi',
    valueLabel: 'Sof tushum',
    formatAs: 'money',
    tone: 'blue',
  },
)

const sortKey = ref<'name' | 'value'>('value')
const sortDir = ref<'asc' | 'desc'>('desc')

const tones = {
  blue: {
    iconWrap: 'bg-blue-light-50 text-blue-light-600 dark:bg-blue-light-500/15 dark:text-blue-light-400',
    iconColor: 'text-blue-light-600 dark:text-blue-light-400',
    badge: 'bg-blue-light-50 text-blue-light-600 dark:bg-blue-light-500/15 dark:text-blue-light-400',
    valueColor: 'text-blue-light-600 dark:text-blue-light-400',
  },
  indigo: {
    iconWrap: 'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400',
    iconColor: 'text-brand-600 dark:text-brand-400',
    badge: 'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400',
    valueColor: 'text-brand-600 dark:text-brand-400',
  },
  violet: {
    iconWrap: 'bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-warning-400',
    iconColor: 'text-warning-600 dark:text-warning-400',
    badge: 'bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-warning-400',
    valueColor: 'text-warning-600 dark:text-warning-400',
  },
  green: {
    iconWrap: 'bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-400',
    iconColor: 'text-success-600 dark:text-success-400',
    badge: 'bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-400',
    valueColor: 'text-success-600 dark:text-success-400',
  },
  teal: {
    iconWrap: 'bg-blue-light-50 text-blue-light-500 dark:bg-blue-light-500/15 dark:text-blue-light-400',
    iconColor: 'text-blue-light-500 dark:text-blue-light-400',
    badge: 'bg-blue-light-50 text-blue-light-500 dark:bg-blue-light-500/15 dark:text-blue-light-400',
    valueColor: 'text-blue-light-500 dark:text-blue-light-400',
  },
  orange: {
    iconWrap: 'bg-orange-50 text-orange-600 dark:bg-orange-500/15 dark:text-orange-400',
    iconColor: 'text-orange-600 dark:text-orange-400',
    badge: 'bg-orange-50 text-orange-600 dark:bg-orange-500/15 dark:text-orange-400',
    valueColor: 'text-orange-600 dark:text-orange-400',
  },
}

const iconWrap = computed(() => tones[props.tone].iconWrap)
const iconColor = computed(() => tones[props.tone].iconColor)
const badge = computed(() => tones[props.tone].badge)
const valueColor = computed(() => tones[props.tone].valueColor)

const sortedRows = computed(() => {
  const copy = [...props.rows]
  copy.sort((a, b) => {
    const cmp =
      sortKey.value === 'name'
        ? a.name.localeCompare(b.name, 'uz')
        : Number(a.value || 0) - Number(b.value || 0)
    return sortDir.value === 'asc' ? cmp : -cmp
  })
  return copy
})

function toggleSort(key: 'name' | 'value') {
  if (sortKey.value === key) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDir.value = key === 'name' ? 'asc' : 'desc'
  }
}

function format(v: number) {
  if (props.formatAs === 'number') return new Intl.NumberFormat('uz-UZ').format(Number(v || 0))
  return money(v)
}
</script>
