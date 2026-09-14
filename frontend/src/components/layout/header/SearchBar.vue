<template>
  <div class="hidden xl:block" ref="root">
    <div class="relative">
      <label for="search-input" class="sr-only">{{ t('header.searchClients') }}</label>
      <span class="pointer-events-none absolute start-4 top-1/2 -translate-y-1/2">
        <svg
          class="fill-gray-500 dark:fill-gray-400"
          width="20"
          height="20"
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <path
            fill-rule="evenodd"
            clip-rule="evenodd"
            d="M3.04175 9.37363C3.04175 5.87693 5.87711 3.04199 9.37508 3.04199C12.8731 3.04199 15.7084 5.87693 15.7084 9.37363C15.7084 12.8703 12.8731 15.7053 9.37508 15.7053C5.87711 15.7053 3.04175 12.8703 3.04175 9.37363ZM9.37508 1.54199C5.04902 1.54199 1.54175 5.04817 1.54175 9.37363C1.54175 13.6991 5.04902 17.2053 9.37508 17.2053C11.2674 17.2053 13.003 16.5344 14.357 15.4176L17.177 18.238C17.4699 18.5309 17.9448 18.5309 18.2377 18.238C18.5306 17.9451 18.5306 17.4703 18.2377 17.1774L15.418 14.3573C16.5365 13.0033 17.2084 11.2669 17.2084 9.37363C17.2084 5.04817 13.7011 1.54199 9.37508 1.54199Z"
          />
        </svg>
      </span>
      <input
        id="search-input"
        ref="inputEl"
        v-model="query"
        type="search"
        autocomplete="off"
        :placeholder="t('header.searchClients')"
        class="dark:bg-dark-900 h-11 w-full rounded-lg border border-gray-200 bg-transparent py-2.5 ps-12 pe-14 text-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 dark:border-gray-800 dark:bg-white/3 dark:text-white/90 dark:placeholder:text-white/30 dark:focus:border-brand-800 xl:w-[430px]"
        @focus="open = true"
        @keydown.down.prevent="move(1)"
        @keydown.up.prevent="move(-1)"
        @keydown.enter.prevent="selectActive"
        @keydown.esc="close"
      />
      <button
        type="button"
        tabindex="-1"
        class="absolute end-2.5 top-1/2 inline-flex -translate-y-1/2 items-center gap-0.5 rounded-lg border border-gray-200 bg-gray-50 px-[7px] py-[4.5px] text-xs -tracking-[0.2px] text-gray-500 dark:border-gray-800 dark:bg-white/3 dark:text-gray-400"
        aria-label="Ctrl+K"
        @click="focusInput"
      >
        <span aria-hidden="true">Ctrl</span>
        <span aria-hidden="true">K</span>
      </button>

      <div
        v-if="open && query.trim()"
        class="absolute start-0 top-12 z-50 w-full overflow-hidden rounded-xl border border-gray-200 bg-white shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark"
      >
        <p class="border-b border-gray-100 px-3 py-2 text-xs text-gray-500 dark:border-gray-800">
          {{ t('header.searchHint') }}
        </p>
        <ul v-if="filtered.length" class="max-h-72 overflow-y-auto py-1">
          <li v-for="(c, i) in filtered" :key="c.id">
            <button
              type="button"
              class="flex w-full flex-col gap-0.5 px-3 py-2 text-left text-sm transition"
              :class="
                i === activeIndex
                  ? 'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400'
                  : 'text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-white/5'
              "
              @mousedown.prevent="goTo(c.id)"
              @mouseenter="activeIndex = i"
            >
              <span class="font-medium">{{ c.fullName }}</span>
              <span class="text-xs text-gray-500">{{ c.phone }}{{ c.additionalPhone ? ` · ${c.additionalPhone}` : '' }}</span>
            </button>
          </li>
        </ul>
        <p v-else class="px-3 py-6 text-center text-sm text-gray-500">{{ t('header.noClients') }}</p>
      </div>
    </div>
  </div>

  <!-- Mobile / Ctrl+K overlay when header search is hidden -->
  <div
    v-if="overlayOpen"
    class="fixed inset-0 z-99999 flex items-start justify-center bg-black/40 p-4 pt-[12vh] xl:hidden"
    @click.self="closeOverlay"
  >
    <div class="w-full max-w-lg overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-theme-lg dark:border-gray-800 dark:bg-gray-900">
      <div class="relative border-b border-gray-100 p-3 dark:border-gray-800">
        <input
          ref="overlayInput"
          v-model="query"
          type="search"
          class="h-11 w-full rounded-lg border border-gray-200 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white/90"
          :placeholder="t('header.searchClients')"
          @keydown.down.prevent="move(1)"
          @keydown.up.prevent="move(-1)"
          @keydown.enter.prevent="selectActive"
          @keydown.esc="closeOverlay"
        />
      </div>
      <ul v-if="filtered.length" class="max-h-80 overflow-y-auto py-1">
        <li v-for="(c, i) in filtered" :key="c.id">
          <button
            type="button"
            class="flex w-full flex-col gap-0.5 px-4 py-2.5 text-left text-sm"
            :class="i === activeIndex ? 'bg-brand-50 text-brand-600' : 'text-gray-700'"
            @click="goTo(c.id)"
          >
            <span class="font-medium">{{ c.fullName }}</span>
            <span class="text-xs text-gray-500">{{ c.phone }}</span>
          </button>
        </li>
      </ul>
      <p v-else-if="query.trim()" class="px-4 py-8 text-center text-sm text-gray-500">
        {{ t('header.noClients') }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchClients, type Client } from '@/api/clients'
import { matchesClientQuery } from '@/utils/phone'

const { t } = useI18n()
const router = useRouter()

const query = ref('')
const open = ref(false)
const overlayOpen = ref(false)
const clients = ref<Client[]>([])
const activeIndex = ref(0)
const inputEl = ref<HTMLInputElement | null>(null)
const overlayInput = ref<HTMLInputElement | null>(null)
const root = ref<HTMLElement | null>(null)
let loaded = false

const filtered = computed(() => {
  const q = query.value.trim()
  if (!q) return []
  return clients.value
    .filter((c) =>
      matchesClientQuery(q, {
        fullName: c.fullName,
        phone: c.phone,
        additionalPhone: c.additionalPhone,
        address: c.address,
        inn: c.inn,
      }),
    )
    .slice(0, 12)
})

watch(filtered, () => {
  activeIndex.value = 0
})

async function ensureClients() {
  if (loaded) return
  try {
    clients.value = (await fetchClients()).data || []
    loaded = true
  } catch {
    clients.value = []
  }
}

function focusInput() {
  void ensureClients()
  if (window.innerWidth < 1280) {
    overlayOpen.value = true
    nextTick(() => overlayInput.value?.focus())
    return
  }
  open.value = true
  inputEl.value?.focus()
}

function close() {
  open.value = false
}

function closeOverlay() {
  overlayOpen.value = false
  query.value = ''
}

function move(delta: number) {
  const len = filtered.value.length
  if (!len) return
  activeIndex.value = (activeIndex.value + delta + len) % len
}

function selectActive() {
  const c = filtered.value[activeIndex.value]
  if (c) goTo(c.id)
}

function goTo(id: number) {
  query.value = ''
  open.value = false
  overlayOpen.value = false
  router.push(`/clients/${id}`)
}

function onKeydown(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    focusInput()
  }
}

function onDocClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) close()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  document.addEventListener('click', onDocClick)
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', onDocClick)
})
</script>
