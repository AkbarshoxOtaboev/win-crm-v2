<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Telegram bot" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>

    <div class="mb-4 space-y-4 rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6">
      <p class="text-sm text-gray-500 dark:text-gray-400">
        Bot: {{ bot?.botUsername || 'ulangan emas' }}
      </p>
      <form class="grid gap-3 md:grid-cols-2" @submit.prevent="onBotSave">
        <input v-model="botForm.botUsername" required class="field" placeholder="@WinCrmBot" />
        <input v-model="botForm.token" required class="field" placeholder="Token" />
        <button type="submit" class="btn">Ro‘yxatdan o‘tkazish</button>
        <button type="button" class="ghost" @click="onReconnect">Qayta ulash</button>
      </form>
    </div>

    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">
            Bot orqali ro‘yxatdan o‘tganlar
          </h3>
          <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Faqat CRM mijozlari (Clients) botdan ro‘yxatdan o‘ta oladi
          </p>
        </div>
        <div class="text-sm text-gray-500 dark:text-gray-400">
          Jami: <span class="font-medium text-gray-800 dark:text-white/90">{{ totalElements }}</span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Telegram</th>
              <th class="th">Telefon</th>
              <th class="th">CRM mijoz</th>
              <th class="th">Status</th>
              <th class="th">Ro‘yxatdan o‘tgan</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loadingUsers">
              <td colspan="6" class="empty">Yuklanmoqda...</td>
            </tr>
            <tr v-else-if="items.length === 0">
              <td colspan="6" class="empty">Hali bot orqali ro‘yxatdan o‘tgan mijoz yo‘q</td>
            </tr>
            <tr
              v-for="u in items"
              :key="u.id"
              class="border-b border-gray-100 dark:border-gray-800"
            >
              <td class="td">{{ u.id }}</td>
              <td class="td">
                <div class="font-medium text-gray-800 dark:text-white/90">
                  {{ displayName(u) }}
                </div>
                <div v-if="u.telegramUsername" class="text-xs text-gray-400">
                  @{{ u.telegramUsername.replace(/^@/, '') }}
                </div>
              </td>
              <td class="td whitespace-nowrap">
                {{ u.phone ? formatUzPhone(u.phone) : '—' }}
              </td>
              <td class="td">
                <router-link
                  v-if="u.clientId"
                  :to="`/clients/${u.clientId}`"
                  class="text-brand-500 hover:underline"
                >
                  {{ u.clientFullName || `#${u.clientId}` }}
                </router-link>
                <span v-else class="text-gray-400">Bog‘lanmagan</span>
              </td>
              <td class="td">
                <span
                  class="inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium"
                  :class="u.clientId
                    ? 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400'
                    : 'bg-orange-50 text-orange-600 dark:bg-orange-500/10 dark:text-orange-400'"
                >
                  {{ u.clientId ? 'Bog‘langan' : 'Faqat Telegram' }}
                </span>
              </td>
              <td class="td whitespace-nowrap">{{ formatDate(u.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div
        v-if="totalPages > 1"
        class="flex flex-wrap items-center justify-between gap-3 border-t border-gray-100 px-5 py-4 dark:border-gray-800"
      >
        <p class="text-sm text-gray-500 dark:text-gray-400">
          {{ page + 1 }} / {{ totalPages }} sahifa
        </p>
        <div class="flex items-center gap-2">
          <button
            type="button"
            class="page-btn"
            :disabled="page <= 0 || loadingUsers"
            @click="goTo(page - 1)"
          >
            Oldingi
          </button>
          <button
            v-for="p in visiblePages"
            :key="p"
            type="button"
            class="page-num"
            :class="{ active: p === page }"
            @click="goTo(p)"
          >
            {{ p + 1 }}
          </button>
          <button
            type="button"
            class="page-btn"
            :disabled="page >= totalPages - 1 || loadingUsers"
            @click="goTo(page + 1)"
          >
            Keyingi
          </button>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  fetchBotSettings,
  fetchTelegramUsers,
  reconnectBot,
  registerBot,
  type BotSettings,
  type TelegramUserItem,
} from '@/api/telegram'
import { ApiError, formatApiError } from '@/api/http'
import { formatUzPhone } from '@/utils/phone'

const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const bot = ref<BotSettings | null>(null)
const botForm = reactive({ botUsername: '', token: '' })

const loadingUsers = ref(false)
const items = ref<TelegramUserItem[]>([])
const page = ref(0)
const size = 20
const totalPages = ref(0)
const totalElements = ref(0)

const visiblePages = computed(() => {
  const total = totalPages.value
  if (total <= 1) return []
  const current = page.value
  const start = Math.max(0, current - 2)
  const end = Math.min(total - 1, start + 4)
  const pages: number[] = []
  for (let i = Math.max(0, end - 4); i <= end; i++) pages.push(i)
  return pages
})

function displayName(u: TelegramUserItem) {
  const name = [u.firstName, u.lastName].filter(Boolean).join(' ').trim()
  if (name) return name
  if (u.telegramUsername) return `@${u.telegramUsername.replace(/^@/, '')}`
  return `Chat #${u.chatId || u.id}`
}

function formatDate(value?: string) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return d.toLocaleString('uz-UZ', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function loadBot() {
  try {
    const b = await fetchBotSettings()
    bot.value = b.data
    botForm.botUsername = bot.value?.botUsername || ''
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) return
    error.value = formatApiError(e)
  }
}

async function loadUsers() {
  loadingUsers.value = true
  try {
    const res = await fetchTelegramUsers({ page: page.value, size })
    const data = res.data
    items.value = data?.content || []
    totalPages.value = data?.totalPages || 0
    totalElements.value = data?.totalElements || 0
  } catch (e) {
    error.value = formatApiError(e)
    items.value = []
  } finally {
    loadingUsers.value = false
  }
}

function goTo(next: number) {
  if (next < 0 || next >= totalPages.value || next === page.value) return
  page.value = next
  void loadUsers()
}

async function onBotSave() {
  try {
    bot.value = (await registerBot(botForm.botUsername, botForm.token)).data
    ok.value = 'Bot saqlandi'
    error.value = null
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onReconnect() {
  try {
    bot.value = (await reconnectBot()).data
    ok.value = 'Qayta ulandi'
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(() => {
  void loadBot()
  void loadUsers()
})
</script>

<style scoped>
.field {
  height: 2.5rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 0.75rem;
  background: transparent;
}
.btn {
  height: 2.5rem;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1rem;
  color: #fff;
  width: fit-content;
}
.ghost {
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1rem;
  width: fit-content;
}
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem;
  color: #dc2626;
}
.ok {
  border-radius: 0.5rem;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  padding: 0.75rem;
  color: #166534;
}
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2.5rem 1rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
.page-btn {
  height: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 0.875rem;
  font-size: 0.875rem;
  color: #374151;
  background: transparent;
}
.page-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.page-num {
  height: 2.25rem;
  min-width: 2.25rem;
  border-radius: 0.5rem;
  border: 1px solid transparent;
  padding: 0 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}
.page-num.active {
  background: #465fff;
  color: #fff;
}
:global(.dark) .field {
  border-color: #344054;
  color: rgba(255,255,255,.9);
}
:global(.dark) .ghost {
  border-color: #344054;
  color: #d1d5db;
}
:global(.dark) .page-btn {
  border-color: #344054;
  color: #d1d5db;
}
:global(.dark) .td { color: #9ca3af; }
</style>
