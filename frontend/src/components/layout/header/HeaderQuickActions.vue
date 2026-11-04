<template>
  <div v-if="actions.length" class="flex items-center gap-2">
    <template v-for="a in actions" :key="a.key">
      <span
        v-if="filialMissing"
        role="link"
        aria-disabled="true"
        class="inline-flex h-10 cursor-not-allowed items-center gap-2 rounded-lg px-3 text-sm font-medium text-white opacity-50 shadow-theme-xs"
        :class="a.tone"
        :title="`${a.label} — ${writeBlockedMessage}`"
      >
        <component :is="a.icon" class="h-4 w-4" />
        <span class="hidden sm:inline xl:hidden 2xl:inline">{{ a.label }}</span>
      </span>
      <router-link
        v-else
        :to="a.to"
        class="inline-flex h-10 items-center gap-2 rounded-lg px-3 text-sm font-medium text-white shadow-theme-xs transition"
        :class="[a.tone, a.hover]"
        :title="a.label"
      >
        <component :is="a.icon" class="h-4 w-4" />
        <span class="hidden sm:inline xl:hidden 2xl:inline">{{ a.label }}</span>
      </router-link>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { RouteLocationRaw } from 'vue-router'
import { ClipboardPlus, Receipt, ShoppingCart, Wallet } from 'lucide-vue-next'
import { useFilialScope } from '@/composables/useFilialScope'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const { filialMissing, writeBlockedMessage } = useFilialScope()

const restricted = computed(() => auth.isProductionManagerOnly || auth.isTransportManagerOnly)
const showOrder = computed(() => !restricted.value && !auth.isCashierOnly && auth.can('SALE_ORDER_CREATE'))
const showInbound = computed(() => !restricted.value && !auth.isCashierOnly && auth.can('WAREHOUSE_ORDER_CREATE'))
const showPayment = computed(() => !restricted.value && auth.can('PAYMENT_CREATE'))
const showExpense = computed(() => !restricted.value && auth.can('EXPENSE_VIEW'))

const actions = computed(() => {
  const list: { key: string; label: string; to: RouteLocationRaw; icon: typeof Wallet; tone: string; hover: string }[] = []
  if (showInbound.value) {
    list.push({
      key: 'inbound',
      label: t('header.addInbound'),
      to: '/warehouse-orders/create',
      icon: ClipboardPlus,
      tone: 'bg-indigo-500',
      hover: 'hover:bg-indigo-600',
    })
  }
  if (showOrder.value) {
    list.push({
      key: 'order',
      label: t('header.addOrder'),
      to: '/sales/create',
      icon: ShoppingCart,
      tone: 'bg-brand-500',
      hover: 'hover:bg-brand-600',
    })
  }
  if (showPayment.value) {
    list.push({
      key: 'payment',
      label: t('header.makePayment'),
      to: { path: '/payments', query: { create: '1' } },
      icon: Wallet,
      tone: 'bg-success-500',
      hover: 'hover:bg-success-600',
    })
  }
  if (showExpense.value) {
    list.push({
      key: 'expense',
      label: t('header.expenses'),
      to: '/expenses',
      icon: Receipt,
      tone: 'bg-orange-500',
      hover: 'hover:bg-orange-600',
    })
  }
  return list
})
</script>
