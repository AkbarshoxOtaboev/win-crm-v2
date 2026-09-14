<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Eskiz.uz SMS" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>
    <div class="space-y-3 rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6">
      <p class="text-sm text-gray-500">
        Eskiz: {{ eskiz?.email || (eskiz?.configured ? 'sozlangan' : 'sozlanmagan') }}
      </p>
      <form class="grid gap-3 md:grid-cols-2" @submit.prevent="onSave">
        <input v-model="form.email" type="email" required class="field" placeholder="Email" />
        <input v-model="form.password" type="password" required class="field" placeholder="Parol" />
        <button type="submit" class="btn">Saqlash</button>
      </form>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchEskizSettings, saveEskizSettings, type EskizSettings } from '@/api/eskiz'
import { formatApiError } from '@/api/http'

const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const eskiz = ref<EskizSettings | null>(null)
const form = reactive({ email: '', password: '' })

async function load() {
  try {
    eskiz.value = (await fetchEskizSettings()).data
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onSave() {
  try {
    eskiz.value = (await saveEskizSettings(form.email, form.password)).data
    ok.value = 'Eskiz saqlandi'
    error.value = null
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
