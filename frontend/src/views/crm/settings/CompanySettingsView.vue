<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Tashkilot rekvizitlari" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>
    <div class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6">
      <form class="grid gap-3 md:grid-cols-2" @submit.prevent="onSave">
        <input v-model="company.companyName" required class="field" placeholder="Kompaniya *" />
        <input v-model="company.inn" required class="field" placeholder="INN *" />
        <input v-model="company.oked" class="field" placeholder="OKED" />
        <input v-model="company.mfo" class="field" placeholder="MFO" />
        <input v-model="company.accountNumber" class="field" placeholder="Hisob" />
        <input v-model="company.bankName" class="field" placeholder="Bank" />
        <input v-model="company.director" class="field" placeholder="Direktor" />
        <input v-model="company.phone" class="field" placeholder="Telefon" />
        <input v-model="company.email" class="field" placeholder="Email" />
        <input v-model="company.address" class="field md:col-span-2" placeholder="Manzil" />
        <textarea v-model="company.description" class="field md:col-span-2 !h-auto py-2" rows="2" placeholder="Izoh" />
        <button type="submit" class="btn">Saqlash</button>
      </form>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  createCompanyDetail,
  fetchCompanyDetails,
  fetchCurrentCompany,
  updateCompanyDetail,
} from '@/api/company'
import { ApiError, formatApiError } from '@/api/http'

const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const companyId = ref<number | null>(null)
const company = reactive({
  companyName: '',
  inn: '',
  oked: '',
  mfo: '',
  accountNumber: '',
  bankName: '',
  director: '',
  phone: '',
  email: '',
  address: '',
  description: '',
})

function apply(c: Record<string, unknown>) {
  companyId.value = (c.id as number) || null
  Object.assign(company, {
    companyName: c.companyName || '',
    inn: c.inn || '',
    oked: c.oked || '',
    mfo: c.mfo || '',
    accountNumber: c.accountNumber || '',
    bankName: c.bankName || '',
    director: c.director || '',
    phone: c.phone || '',
    email: c.email || '',
    address: c.address || '',
    description: c.description || '',
  })
}

async function load() {
  try {
    const res = await fetchCurrentCompany()
    if (res.data) apply(res.data as Record<string, unknown>)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      try {
        const list = await fetchCompanyDetails()
        if (list.data?.[0]) apply(list.data[0] as Record<string, unknown>)
      } catch {
        /* empty */
      }
      return
    }
    error.value = formatApiError(e)
  }
}

async function onSave() {
  try {
    const payload = { ...company }
    if (companyId.value) await updateCompanyDetail(companyId.value, payload)
    else {
      const res = await createCompanyDetail(payload)
      companyId.value = res.data?.id || null
    }
    ok.value = 'Kompaniya saqlandi'
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
