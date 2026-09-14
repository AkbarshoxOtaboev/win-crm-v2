<template>
  <div class="relative" ref="root">
    <div class="inline-flex items-center gap-1 rounded-lg border border-gray-200 bg-gray-50 p-1 dark:border-gray-800 dark:bg-white/[0.03]">
      <button
        v-for="lang in languages"
        :key="lang.id"
        type="button"
        class="inline-flex items-center gap-1.5 rounded-md px-2 py-1.5 text-xs font-medium transition"
        :class="
          locale === lang.id
            ? 'bg-white text-gray-800 shadow-theme-xs dark:bg-gray-800 dark:text-white/90'
            : 'text-gray-500 hover:text-gray-800 dark:text-gray-400 dark:hover:text-white/80'
        "
        :title="lang.name"
        @click="setLang(lang.id)"
      >
        <img
          :src="'/images/icons/' + lang.flag"
          :alt="lang.id"
          class="h-4 w-4 rounded-full object-cover"
        />
        <span class="uppercase">{{ lang.id }}</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { storeLocale, type AppLocale } from '@/i18n'

const { locale } = useI18n()

const languages: { id: AppLocale; name: string; flag: string }[] = [
  { id: 'uz', name: 'O‘zbekcha', flag: 'flag-uz.svg' },
  { id: 'ru', name: 'Русский', flag: 'flag-ru.svg' },
  { id: 'en', name: 'English', flag: 'flag-us.svg' },
]

function setLang(id: AppLocale) {
  locale.value = id
  storeLocale(id)
}
</script>
