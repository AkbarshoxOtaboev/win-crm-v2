<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Umumiy sozlamalar" />
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

const links = [
  { path: '/settings/filials', title: 'Filiallar', desc: 'Filiallar va direktorlar', icon: Building2 },
  { path: '/settings/users', title: 'Foydalanuvchilar', desc: 'Login, rollar va status', icon: UserCircle },
  { path: '/settings/sessions', title: 'Faol sessiyalar', desc: 'Kirishlar va qurilmalar', icon: Plug },
  { path: '/settings/roles', title: 'Rollar', desc: 'Ruxsatlar boshqaruvi', icon: Shield },
  { path: '/settings/company', title: 'Tashkilot rekvizitlari', desc: 'INN, bank, manzil', icon: Files },
  { path: '/settings/telegram', title: 'Telegram bot', desc: 'Bot token va ulanish', icon: Plug },
  { path: '/settings/eskiz', title: 'Eskiz.uz SMS', desc: 'SMS sozlamalari', icon: Send },
  { path: '/settings/units', title: 'O‘lchov birliklari', desc: 'dona, m², kg…', icon: List },
  { path: '/settings/warehouses', title: 'Omborlar', desc: 'Omborlar ro‘yxati', icon: Box },
  { path: '/settings/workshops', title: 'Sexlar', desc: 'Ishlab chiqarish sexlari', icon: Factory },
  { path: '/settings/payment-types', title: 'To‘lov turlari', desc: 'Naqd, karta…', icon: BarChart3 },
  { path: '/settings/expense-categories', title: 'Xarajat kategoriyalari', desc: 'Xarajat turlari', icon: Folder },
  { path: '/settings/audit', title: 'Audit loglar', desc: 'Tizim amallar tarixi', icon: ShieldCheck },
  { path: '/profile', title: 'Profil', desc: 'Shaxsiy ma’lumotlar', icon: Settings },
]
</script>
