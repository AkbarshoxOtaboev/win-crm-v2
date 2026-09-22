<template>
  <div class="relative" ref="dropdownRef">
    <button
      class="flex items-center text-gray-700 dark:text-gray-400 cursor-pointer"
      @click.prevent="toggleDropdown"
      type="button"
    >
      <UserAvatar
        class="ltr:mr-3 rtl:ml-3"
        :preference="avatarPref"
        size="md"
        :title="displayName"
      />
      <span class="block font-medium text-theme-sm ltr:mr-1 rtl:ml-1">{{ displayName }}</span>
      <ChevronDownIcon
        class="size-5 transition-transform duration-200 text-gray-500 dark:text-gray-400"
        :class="{ 'rotate-180': dropdownOpen }"
      />
    </button>

    <div
      v-if="dropdownOpen"
      class="absolute ltr:right-0 rtl:left-0 z-50 mt-[17px] flex w-[260px] flex-col rounded-2xl border border-gray-200 bg-white p-3 shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark animate-fadeIn"
    >
      <div class="flex items-center gap-3 px-1 pb-1">
        <UserAvatar :preference="avatarPref" size="sm" />
        <div class="min-w-0">
          <span class="block truncate font-medium text-gray-700 text-theme-sm dark:text-gray-400">
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
import { Lock as LockIcon } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { getAvatarPreference, type AvatarPreference } from '@/utils/avatar'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const displayName = computed(() => auth.username || 'User')
const dropdownOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const avatarPref = ref<AvatarPreference>(getAvatarPreference(auth.username))

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
