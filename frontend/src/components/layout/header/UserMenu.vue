<template>
  <div class="relative" ref="dropdownRef">
    <button
      class="flex h-10 items-center gap-2 rounded-lg border border-gray-200 bg-white ps-px pe-2 text-gray-700 transition hover:bg-gray-50 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-300 dark:hover:bg-white/[0.03] cursor-pointer"
      @click.prevent="toggleDropdown"
      type="button"
      :aria-expanded="dropdownOpen"
      aria-haspopup="menu"
    >
      <UserAvatar :preference="avatarPref" size="sm" :title="displayName" />
      <span class="block max-w-[120px] truncate font-medium text-theme-sm xl:hidden 2xl:block">{{ displayName }}</span>
      <ChevronDownIcon
        class="size-4 transition-transform duration-200 text-gray-500 dark:text-gray-400"
        :class="{ 'rotate-180': dropdownOpen }"
      />
    </button>

    <div
      v-if="dropdownOpen"
      role="menu"
      class="absolute end-0 z-50 mt-2 flex w-[280px] flex-col rounded-2xl border border-gray-200 bg-white p-3 shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark animate-fadeIn"
    >
      <div class="flex items-center gap-3 px-1 pb-1">
        <UserAvatar :preference="avatarPref" size="sm" />
        <div class="min-w-0">
          <span class="block truncate font-medium text-gray-700 text-theme-sm dark:text-gray-300">
            {{ displayName }}
          </span>
          <span class="mt-0.5 block truncate text-theme-xs text-gray-500 dark:text-gray-400">
            {{ auth.username || 'WinCRM' }}
          </span>
        </div>
      </div>

      <ul class="flex flex-col gap-1 border-b border-gray-200 pt-4 pb-3 dark:border-gray-800">
        <li>
          <router-link
            to="/profile"
            class="group flex items-center gap-3 rounded-lg px-3 py-2 text-theme-sm font-medium text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-white/5 dark:hover:text-gray-300"
            @click="closeDropdown"
          >
            <UserCircleIcon
              class="fill-gray-500 group-hover:fill-gray-700 dark:fill-gray-400 dark:group-hover:fill-gray-300"
            />
            {{ t('userMenu.profile') }}
          </router-link>
        </li>
        <li>
          <router-link
            to="/profile#password"
            class="group flex items-center gap-3 rounded-lg px-3 py-2 text-theme-sm font-medium text-gray-700 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-white/5 dark:hover:text-gray-300"
            @click="closeDropdown"
          >
            <LockIcon
              class="h-5 w-5 text-gray-500 group-hover:text-gray-700 dark:text-gray-400 dark:group-hover:text-gray-300"
            />
            {{ t('userMenu.changePassword') }}
          </router-link>
        </li>
      </ul>

      <div class="flex flex-col gap-3 border-b border-gray-200 py-3 dark:border-gray-800">
        <div class="px-1">
          <div class="mb-2 flex items-center gap-2 text-theme-xs font-medium uppercase tracking-wide text-gray-500 dark:text-gray-400">
            <Languages class="h-3.5 w-3.5" />
            {{ t('userMenu.language') }}
          </div>
          <div class="grid grid-cols-3 gap-1 rounded-xl bg-gray-100 p-1 dark:bg-white/5" role="listbox" :aria-label="t('userMenu.language')">
            <button
              v-for="lang in languages"
              :key="lang.id"
              type="button"
              role="option"
              :aria-selected="locale === lang.id"
              class="flex items-center justify-center gap-1.5 rounded-lg px-2 py-1.5 text-theme-xs font-medium transition"
              :class="
                locale === lang.id
                  ? 'bg-white text-gray-800 shadow-theme-xs dark:bg-gray-800 dark:text-white'
                  : 'text-gray-600 hover:text-gray-800 dark:text-gray-400 dark:hover:text-gray-200'
              "
              @click="setLang(lang.id)"
            >
              <img :src="'/images/icons/' + lang.flag" :alt="lang.id" class="h-3.5 w-3.5 rounded-full object-cover" />
              <span class="uppercase">{{ lang.id }}</span>
            </button>
          </div>
        </div>

        <div class="flex items-center justify-between gap-3 px-1">
          <div class="flex min-w-0 items-center gap-2">
            <Moon class="h-4 w-4 shrink-0 text-gray-500 dark:text-gray-400" />
            <div class="min-w-0">
              <p class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('userMenu.darkMode') }}</p>
              <p class="text-theme-xs text-gray-500 dark:text-gray-400">
                {{ isDarkMode ? t('userMenu.dark') : t('userMenu.light') }}
              </p>
            </div>
          </div>
          <button
            type="button"
            role="switch"
            :aria-checked="isDarkMode"
            :aria-label="t('userMenu.darkMode')"
            class="relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full transition-colors"
            :class="isDarkMode ? 'bg-brand-500' : 'bg-gray-200 dark:bg-gray-700'"
            @click="toggleTheme"
          >
            <span
              class="inline-block h-5 w-5 rounded-full bg-white shadow-theme-xs transition-transform"
              :class="isDarkMode ? 'translate-x-5' : 'translate-x-0.5'"
            />
          </button>
        </div>
      </div>

      <button
        type="button"
        class="group mt-3 flex w-full items-center justify-center gap-3 rounded-lg border border-gray-200 px-3 py-2 text-theme-sm font-medium text-gray-700 hover:bg-gray-100 dark:border-gray-800 dark:text-gray-400 dark:hover:bg-white/5 dark:hover:text-gray-300"
        @click="signOut"
      >
        {{ t('userMenu.signOut') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { UserCircleIcon, ChevronDownIcon } from '@/icons'
import { Languages, Lock as LockIcon, Moon } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { computed, onMounted, onUnmounted, ref, watch, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { getAvatarPreference, type AvatarPreference } from '@/utils/avatar'
import { storeLocale, type AppLocale } from '@/i18n'
import { useTheme } from '@/components/layout/ThemeProvider.vue'

const { t, locale } = useI18n()
const theme = useTheme() as {
  isDarkMode: ComputedRef<boolean>
  toggleTheme: () => void
}
const isDarkMode = theme.isDarkMode
const { toggleTheme } = theme
const router = useRouter()
const auth = useAuthStore()
const displayName = computed(() => auth.username || t('common.user'))
const dropdownOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const avatarPref = ref<AvatarPreference>(getAvatarPreference(auth.username))

const languages: { id: AppLocale; flag: string }[] = [
  { id: 'uz', flag: 'flag-uz.svg' },
  { id: 'ru', flag: 'flag-ru.svg' },
  { id: 'en', flag: 'flag-us.svg' },
]

function setLang(id: AppLocale) {
  locale.value = id
  storeLocale(id)
}

function reloadAvatar() {
  avatarPref.value = getAvatarPreference(auth.username)
}

function onAvatarEvent(e: Event) {
  const detail = (e as CustomEvent<{ username?: string }>).detail
  if (!detail?.username || detail.username === auth.username) reloadAvatar()
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value
}

function closeDropdown() {
  dropdownOpen.value = false
}

async function signOut() {
  closeDropdown()
  await auth.logout()
  router.push('/signin')
}

function handleClickOutside(event: MouseEvent) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
    closeDropdown()
  }
}

watch(
  () => auth.username,
  () => reloadAvatar(),
)

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('wincrm-avatar-changed', onAvatarEvent)
  reloadAvatar()
})
onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('wincrm-avatar-changed', onAvatarEvent)
})
</script>
