<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Tashkilot rekvizitlari" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>

    <div class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6">
      <div class="mb-5">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Kompaniya ma’lumotlari</h3>
        <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Hisob-faktura va hujjatlar uchun tashkilot rekvizitlari
        </p>
      </div>

      <form class="grid gap-4 md:grid-cols-2" @submit.prevent="onSave">
        <div>
          <label for="company-name" class="lbl">Kompaniya nomi <span class="req">*</span></label>
          <div class="relative">
            <Building2 class="field-icon" />
            <input
              id="company-name"
              v-model="company.companyName"
              required
              placeholder="Masalan: WinCRM LLC"
              class="field"
            />
          </div>
        </div>

        <div>
          <label for="company-inn" class="lbl">INN <span class="req">*</span></label>
          <div class="relative">
            <Hash class="field-icon" />
            <input id="company-inn" v-model="company.inn" required placeholder="123456789" class="field" />
          </div>
        </div>

        <div>
          <label for="company-oked" class="lbl">OKED</label>
          <div class="relative">
            <FileText class="field-icon" />
            <input id="company-oked" v-model="company.oked" placeholder="OKED kodi" class="field" />
          </div>
        </div>

        <div>
          <label for="company-mfo" class="lbl">MFO</label>
          <div class="relative">
            <Landmark class="field-icon" />
            <input id="company-mfo" v-model="company.mfo" placeholder="00000" class="field" />
          </div>
        </div>

        <div>
          <label for="company-account" class="lbl">Hisob raqami</label>
          <div class="relative">
            <CreditCard class="field-icon" />
            <input
              id="company-account"
              v-model="company.accountNumber"
              placeholder="20208000..."
              class="field"
            />
          </div>
        </div>

        <div>
          <label for="company-bank" class="lbl">Bank</label>
          <div class="relative">
            <Landmark class="field-icon" />
            <input id="company-bank" v-model="company.bankName" placeholder="Bank nomi" class="field" />
          </div>
        </div>

        <div>
          <label for="company-director" class="lbl">Direktor</label>
          <div class="relative">
            <User class="field-icon" />
            <input
              id="company-director"
              v-model="company.director"
              placeholder="F.I.Sh"
              class="field"
            />
          </div>
        </div>

        <div>
          <label for="company-phone" class="lbl">Telefon</label>
          <div class="relative">
            <Phone class="field-icon" />
            <input
              id="company-phone"
              :value="company.phone"
              type="tel"
              inputmode="tel"
              autocomplete="tel"
              placeholder="+998-(12)-345-67-89"
              maxlength="19"
              class="field"
              @input="onPhoneInput"
            />
          </div>
          <p class="mt-1 text-xs text-gray-400">Format: +998-(12)-345-67-89</p>
        </div>

        <div>
          <label for="company-email" class="lbl">Email</label>
          <div class="relative">
            <Mail class="field-icon" />
            <input
              id="company-email"
              v-model="company.email"
              type="email"
              placeholder="info@company.uz"
              class="field"
            />
          </div>
        </div>

        <div class="md:col-span-2">
          <label for="company-address" class="lbl">Manzil</label>
          <div class="relative">
            <MapPin class="field-icon" />
            <input
              id="company-address"
              v-model="company.address"
              placeholder="Ko‘cha, tuman, shahar"
              class="field"
            />
          </div>
        </div>

        <div class="md:col-span-2">
          <label for="company-desc" class="lbl">Izoh</label>
          <div class="relative">
            <AlignLeft class="field-icon field-icon-top" />
            <textarea
              id="company-desc"
              v-model="company.description"
              rows="3"
              placeholder="Qo‘shimcha ma’lumot"
              class="field field-textarea"
            />
          </div>
        </div>

        <div class="md:col-span-2 flex justify-end pt-1">
          <button type="submit" class="btn" :disabled="saving">
            {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
          </button>
        </div>
      </form>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  AlignLeft,
  Building2,
  CreditCard,
  FileText,
  Hash,
  Landmark,
  Mail,
  MapPin,
  Phone,
  User,
} from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import {
  createCompanyDetail,
  fetchCompanyDetails,
  fetchCurrentCompany,
  updateCompanyDetail,
} from '@/api/company'
import { ApiError, formatApiError } from '@/api/http'
import { formatUzPhone, isCompleteUzPhone, phoneDigits } from '@/utils/phone'

const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const saving = ref(false)
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
    phone: c.phone ? formatUzPhone(String(c.phone)) : '+998-',
    email: c.email || '',
    address: c.address || '',
    description: c.description || '',
  })
}

function onPhoneInput(e: Event) {
  const el = e.target as HTMLInputElement
  const formatted = formatUzPhone(el.value)
  company.phone = formatted
  el.value = formatted
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
        else company.phone = '+998-'
      } catch {
        company.phone = '+998-'
      }
      return
    }
    error.value = formatApiError(e)
  }
}

async function onSave() {
  error.value = null
  ok.value = null
  const digits = phoneDigits(company.phone)
  if (digits.length > 3 && !isCompleteUzPhone(company.phone)) {
    error.value = 'Telefon +998-(12)-345-67-89 formatida to‘liq bo‘lishi kerak'
    return
  }
  saving.value = true
  try {
    const payload = {
      ...company,
      companyName: company.companyName.trim(),
      inn: company.inn.trim(),
      phone: isCompleteUzPhone(company.phone) ? formatUzPhone(company.phone) : '',
    }
    if (companyId.value) await updateCompanyDetail(companyId.value, payload)
    else {
      const res = await createCompanyDetail(payload)
      companyId.value = res.data?.id || null
    }
    ok.value = 'Kompaniya saqlandi'
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.lbl { display: block; margin-bottom: 0.375rem; font-size: 0.875rem; font-weight: 500; color: #374151; }
.req { color: #ef4444; }
.field-icon {
  position: absolute;
  top: 50%;
  left: 0.75rem;
  z-index: 10;
  height: 1.1rem;
  width: 1.1rem;
  transform: translateY(-50%);
  color: #98a2b3;
  pointer-events: none;
}
.field-icon-top {
  top: 0.9rem;
  transform: none;
}
.field {
  height: 2.75rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: transparent;
  padding: 0 0.75rem 0 2.5rem;
  font-size: 0.875rem;
  color: #1f2937;
  outline: none;
}
.field:focus {
  border-color: #9cb0ff;
  box-shadow: 0 0 0 4px rgb(70 95 255 / 10%);
}
.field-textarea {
  height: auto;
  min-height: 5rem;
  padding-top: 0.75rem;
  padding-bottom: 0.75rem;
  resize: vertical;
}
.btn {
  height: 2.75rem;
  border-radius: 0.5rem;
  background: #465fff;
  padding: 0 1.25rem;
  color: #fff;
  font-weight: 500;
  width: fit-content;
}
.btn:disabled { opacity: 0.6; }
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
:global(.dark) .lbl { color: #9ca3af; }
:global(.dark) .field {
  border-color: #344054;
  color: rgba(255,255,255,.9);
}
:global(.dark) .field::placeholder { color: rgba(255,255,255,.3); }
:global(.dark) .err {
  border-color: rgb(239 68 68 / 30%);
  background: rgb(239 68 68 / 10%);
  color: #f87171;
}
:global(.dark) .ok {
  border-color: rgb(34 197 94 / 30%);
  background: rgb(34 197 94 / 10%);
  color: #4ade80;
}
</style>
