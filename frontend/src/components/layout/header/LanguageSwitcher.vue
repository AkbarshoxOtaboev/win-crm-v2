<template>
  <div class="relative" ref="root">
    <button
      type="button"
      class="inline-flex h-10 items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 text-sm font-medium text-gray-700 transition hover:bg-gray-50 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-200 dark:hover:bg-white/[0.03]"
      :title="t('header.language')"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click="open = !open"
    >
      <img :src="'/images/icons/' + current.flag" :alt="current.id" class="h-4 w-4 rounded-full object-cover" />
      <span class="uppercase">{{ current.id }}</span>
      <ChevronDown class="h-4 w-4 text-gray-500 transition-transform duration-200 dark:text-gray-400" :class="{ 'rotate-180': open }" />
    </button>

    <ul
      v-if="open"
      role="listbox"
      class="absolute end-0 z-50 mt-2 w-44 rounded-xl border border-gray-200 bg-white p-1.5 shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark"
    >
      <li v-for="lang in languages" :key="lang.id">
        <button
          type="button"
          role="option"
          :aria-selected="locale === lang.id"
          class="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-start text-theme-sm transition"
          :class="
            locale === lang.id
              ? 'bg-brand-50 font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400'
              : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5'
          "
          @click="setLang(lang.id)"
        >
          <img :src="'/images/icons/' + lang.flag" :alt="lang.id" class="h-5 w-5 rounded-full object-cover" />
          <span class="flex-1">{{ lang.name }}</span>
          <Check v-if="locale === lang.id" class="h-4 w-4" />
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, ChevronDown } from 'lucide-vue-next'
import { storeLocale, type AppLocale } from '@/i18n'

const { locale, t } = useI18n()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

const languages: { id: AppLocale; name: string; flag: string }[] = [
  { id: 'uz', name: 'O‘zbekcha', flag: 'flag-uz.svg' },
  { id: 'ru', name: 'Русский', flag: 'flag-ru.svg' },
  { id: 'en', name: 'English', flag: 'flag-us.svg' },
]

const current = computed(() => languages.find((l) => l.id === locale.value) ?? languages[0])

function setLang(id: AppLocale) {
  locale.value = id
  storeLocale(id)
  open.value = false
}

function onClickOutside(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) open.value = false
}

onMounted(() => document.addEventListener('click', onClickOutside))
onUnmounted(() => document.removeEventListener('click', onClickOutside))
</script>
