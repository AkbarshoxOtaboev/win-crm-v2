<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('nav.generalSettings')" />
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <router-link
        v-for="item in links"
        :key="item.path"
        :to="item.path"
        class="group rounded-2xl border border-gray-200 bg-white p-5 transition hover:border-brand-300 hover:shadow-theme-sm dark:border-gray-800 dark:bg-white/[0.03] dark:hover:border-brand-500/40"
      >
        <span
          class="inline-flex h-11 w-11 items-center justify-center rounded-xl bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400"
        >
          <component :is="item.icon" class="h-5 w-5" />
        </span>
        <h3 class="mt-4 text-base font-semibold text-gray-800 group-hover:text-brand-500 dark:text-white/90">
          {{ item.title }}
        </h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">{{ item.desc }}</p>
      </router-link>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  BarChart3,
  Box,
  Building2,
  Factory,
  Folder,
  List,
  Plug,
  Send,
  Settings,
  Shield,
  ShieldCheck,
  UserCircle,
  Files,
} from 'lucide-vue-next'

const { t } = useI18n()

const linkDefs = [
  { path: '/settings/filials', key: 'filials', icon: Building2 },
  { path: '/settings/users', key: 'users', icon: UserCircle },
  { path: '/settings/sessions', key: 'sessions', icon: Plug },
  { path: '/settings/roles', key: 'roles', icon: Shield },
  { path: '/settings/company', key: 'company', icon: Files },
  { path: '/settings/telegram', key: 'telegram', icon: Plug },
  { path: '/settings/eskiz', key: 'eskiz', icon: Send },
  { path: '/settings/units', key: 'units', icon: List },
  { path: '/settings/warehouses', key: 'warehouses', icon: Box },
  { path: '/settings/workshops', key: 'workshops', icon: Factory },
  { path: '/settings/payment-types', key: 'paymentTypes', icon: BarChart3 },
  { path: '/settings/expense-categories', key: 'expenseCategories', icon: Folder },
  { path: '/settings/audit', key: 'audit', icon: ShieldCheck },
  { path: '/profile', key: 'profile', icon: Settings },
]

const links = computed(() =>
  linkDefs.map((l) => ({
    path: l.path,
    title: t(`nav.${l.key}`),
    desc: t(`settings.general.desc.${l.key}`),
    icon: l.icon,
  })),
)
</script>
