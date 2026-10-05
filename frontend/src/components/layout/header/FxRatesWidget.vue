<template>
  <div v-if="usd" class="relative" ref="root">
    <button
      type="button"
      class="inline-flex h-10 items-center gap-2.5 rounded-lg border border-gray-200 bg-white px-2.5 text-xs transition hover:bg-gray-50 dark:border-gray-800 dark:bg-gray-900 dark:hover:bg-white/[0.03]"
      :title="t('header.fxTitle')"
      @click="open = !open"
    >
      <span class="flex h-6 w-6 items-center justify-center rounded-full bg-brand-50 text-brand-500 dark:bg-brand-500/15 dark:text-brand-400">
        <DollarSign class="h-3.5 w-3.5" />
      </span>
      <template v-if="hasCompanyRate">
        <span class="flex flex-col items-start leading-tight">
          <span class="flex items-center gap-1 text-[10px] font-medium uppercase text-gray-500 dark:text-gray-400">
            <ArrowDownLeft class="h-3 w-3 text-success-500" />{{ t('header.fxBuy') }}
          </span>
          <span class="font-semibold text-gray-800 dark:text-white/90">{{ rateText(usd.buyRate) }}</span>
        </span>
        <span class="h-6 w-px bg-gray-200 dark:bg-gray-700" />
        <span class="flex flex-col items-start leading-tight">
          <span class="flex items-center gap-1 text-[10px] font-medium uppercase text-gray-500 dark:text-gray-400">
            <ArrowUpRight class="h-3 w-3 text-error-500" />{{ t('header.fxSell') }}
          </span>
          <span class="font-semibold text-gray-800 dark:text-white/90">{{ rateText(usd.sellRate) }}</span>
        </span>
      </template>
      <span v-else class="flex flex-col items-start leading-tight">
        <span class="text-[10px] font-medium uppercase text-gray-500 dark:text-gray-400">{{ t('header.fxCbu') }}</span>
        <span class="font-semibold text-gray-800 dark:text-white/90">{{ rateText(usd.cbuRate) }}</span>
      </span>
    </button>

    <div
      v-if="open"
      class="absolute end-0 z-50 mt-2 w-72 rounded-xl border border-gray-200 bg-white p-4 shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark"
    >
      <div class="mb-3 flex items-center justify-between">
        <p class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('header.fxTitle') }} · USD</p>
        <button
          type="button"
          class="rounded-md p-1 text-gray-500 hover:bg-gray-100 dark:text-gray-400 dark:hover:bg-white/5"
          :title="t('common.refresh')"
          @click="load"
        >
          <RefreshCw class="h-3.5 w-3.5" :class="{ 'animate-spin': loading }" />
        </button>
      </div>

      <div v-if="hasCompanyRate" class="grid grid-cols-2 gap-2">
        <div class="rounded-lg bg-success-50 p-2.5 dark:bg-success-500/10">
          <p class="flex items-center gap-1 text-theme-xs text-success-700 dark:text-success-400">
            <ArrowDownLeft class="h-3.5 w-3.5" />{{ t('header.fxBuy') }}
          </p>
          <p class="mt-0.5 text-base font-bold text-gray-800 dark:text-white/90">{{ rateText(usd.buyRate) }}</p>
        </div>
        <div class="rounded-lg bg-error-50 p-2.5 dark:bg-error-500/10">
          <p class="flex items-center gap-1 text-theme-xs text-error-700 dark:text-error-400">
            <ArrowUpRight class="h-3.5 w-3.5" />{{ t('header.fxSell') }}
          </p>
          <p class="mt-0.5 text-base font-bold text-gray-800 dark:text-white/90">{{ rateText(usd.sellRate) }}</p>
        </div>
      </div>
      <p v-else class="rounded-lg bg-warning-50 p-2.5 text-theme-xs text-warning-700 dark:bg-warning-500/10 dark:text-warning-400">
        {{ t('header.fxNotSet') }}
      </p>

      <div class="mt-3 flex items-center justify-between border-t border-gray-100 pt-3 text-theme-xs dark:border-gray-800">
        <span class="flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
          <Landmark class="h-3.5 w-3.5" />{{ t('header.fxCbu') }}
        </span>
        <span class="flex items-center gap-1.5">
          <span class="font-semibold text-gray-800 dark:text-white/90">{{ rateText(usd.cbuRate) }}</span>
          <span
            v-if="usd.cbuChange != null"
            class="inline-flex items-center gap-0.5 font-medium"
            :class="changeTone"
          >
            <component :is="changeIcon" class="h-3 w-3" />{{ changeText }}
          </span>
        </span>
      </div>
      <p v-if="usd.updatedAt" class="mt-1 text-[11px] text-gray-400">
        {{ formatDmyTime(usd.updatedAt) }}<template v-if="usd.updatedUsername"> · {{ usd.updatedUsername }}</template>
      </p>

      <router-link
        v-if="canManage"
        to="/currency"
        class="mt-3 flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 px-3 py-2 text-theme-xs font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-800 dark:text-gray-300 dark:hover:bg-white/5"
        @click="open = false"
      >
        <Settings2 class="h-3.5 w-3.5" />{{ t('header.fxManage') }}
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  ArrowDownLeft,
  ArrowUpRight,
  DollarSign,
  Landmark,
  Minus,
  RefreshCw,
  Settings2,
  TrendingDown,
  TrendingUp,
} from 'lucide-vue-next'
import { useCompanyFxRates } from '@/composables/useCompanyFxRates'
import { useAuthStore } from '@/stores/auth'
import { formatDmyTime } from '@/utils/format'

const RELOAD_MS = 5 * 60 * 1000

const { t } = useI18n()
const auth = useAuthStore()
const fx = useCompanyFxRates()
const { usd, loading } = fx
const open = ref(false)
const root = ref<HTMLElement | null>(null)
let timer: ReturnType<typeof setInterval> | undefined

const rateFormat = new Intl.NumberFormat('uz-UZ', { maximumFractionDigits: 2 })

const canManage = computed(
  () => !auth.isCashierOnly && !auth.isProductionManagerOnly && !auth.isTransportManagerOnly,
)
const hasCompanyRate = computed(() => !!usd.value && usd.value.buyRate != null && usd.value.sellRate != null)

const changeTone = computed(() => {
  const n = Number(usd.value?.cbuChange || 0)
  return n > 0 ? 'text-success-600 dark:text-success-400' : n < 0 ? 'text-error-600 dark:text-error-400' : 'text-gray-500'
})
const changeIcon = computed(() => {
  const n = Number(usd.value?.cbuChange || 0)
  return n > 0 ? TrendingUp : n < 0 ? TrendingDown : Minus
})
const changeText = computed(() => {
  const n = Number(usd.value?.cbuChange || 0)
  return `${n > 0 ? '+' : n < 0 ? '−' : ''}${rateFormat.format(Math.abs(n))}`
})

function rateText(v?: number | null) {
  return v == null ? '—' : rateFormat.format(Number(v))
}

/* navbar vidjeti xatoni ko'rsatmaydi */
function load() {
  if (auth.isAuthenticated) fx.load().catch(() => {})
}

function onClickOutside(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) open.value = false
}

onMounted(() => {
  if (auth.isAuthenticated) fx.loadIfStale().catch(() => {})
  timer = setInterval(load, RELOAD_MS)
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('click', onClickOutside)
})
</script>
