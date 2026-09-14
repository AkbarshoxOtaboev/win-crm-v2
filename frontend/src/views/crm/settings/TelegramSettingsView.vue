<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Telegram bot" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>
    <div class="space-y-4 rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6">
      <p class="text-sm text-gray-500">Bot: {{ bot?.botUsername || 'ulangan emas' }}</p>
      <form class="grid gap-3 md:grid-cols-2" @submit.prevent="onBotSave">
        <input v-model="botForm.botUsername" required class="field" placeholder="@WinCrmBot" />
        <input v-model="botForm.token" required class="field" placeholder="Token" />
        <button type="submit" class="btn">Ro‘yxatdan o‘tkazish</button>
        <button type="button" class="ghost" @click="onReconnect">Qayta ulash</button>
      </form>
      <form class="grid gap-3 border-t border-gray-100 pt-4 md:grid-cols-2 dark:border-gray-800" @submit.prevent="onSend">
        <select v-model.number="tgClientId" class="field">
          <option :value="0" disabled>Mijoz</option>
          <option v-for="c in clients" :key="c.id" :value="c.id">{{ c.fullName }}</option>
        </select>
        <input v-model="tgMessage" class="field" placeholder="Xabar" />
        <button type="submit" class="btn">Telegram yuborish</button>
      </form>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchBotSettings, reconnectBot, registerBot, sendTelegramToClient, type BotSettings } from '@/api/telegram'
import { fetchClients, type Client } from '@/api/clients'
import { ApiError, formatApiError } from '@/api/http'

const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const bot = ref<BotSettings | null>(null)
const botForm = reactive({ botUsername: '', token: '' })
const clients = ref<Client[]>([])
const tgClientId = ref(0)
const tgMessage = ref('')

async function load() {
  try {
    const [b, c] = await Promise.all([fetchBotSettings(), fetchClients()])
    bot.value = b.data
    clients.value = c.data || []
    botForm.botUsername = bot.value?.botUsername || ''
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      clients.value = (await fetchClients()).data || []
      return
    }
    error.value = formatApiError(e)
  }
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

async function onSend() {
  try {
    await sendTelegramToClient(tgClientId.value, tgMessage.value)
    ok.value = 'Xabar yuborildi'
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
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
</style>
