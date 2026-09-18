<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Filiallar" />
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <div>
          <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Filiallar</h3>
          <p class="mt-1 text-sm text-gray-500">Har bir filial o‘z ombor, savdo, mijoz va to‘lovlariga ega</p>
        </div>
        <button v-if="auth.superAdmin" type="button" class="btn" @click="openCreate">+ Yangi filial</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Nomi</th>
              <th class="th">Manzil</th>
              <th class="th">Telefon</th>
              <th class="th">Direktor</th>
              <th v-if="auth.superAdmin" class="th text-right whitespace-nowrap">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ item.id }}</td>
              <td class="td font-medium text-gray-800 dark:text-white/90">{{ item.name }}</td>
              <td class="td">{{ item.address || '—' }}</td>
              <td class="td whitespace-nowrap">{{ item.phone ? formatUzPhone(item.phone) : '—' }}</td>
              <td class="td">
                <div v-if="item.directorFullName">
                  <div>{{ item.directorFullName }}</div>
                  <div class="text-xs text-gray-400">{{ item.directorUsername }}</div>
                </div>
                <span v-else class="text-gray-400">Tayinlanmagan</span>
              </td>
              <td v-if="auth.superAdmin" class="td text-right whitespace-nowrap">
                <RowActions @edit="openEdit(item)" @delete="onDelete(item)" />
              </td>
            </tr>
            <tr v-if="items.length === 0">
              <td :colspan="auth.superAdmin ? 6 : 5" class="empty">Filial yo‘q</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modal" class="overlay" @click.self="closeModal">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="filial-modal-title">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div>
            <h3 id="filial-modal-title" class="text-lg font-semibold text-gray-800 dark:text-white/90">
              {{ editingId ? 'Filialni tahrirlash' : 'Yangi filial' }}
            </h3>
            <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
              Filial nomi, manzil va direktorini kiriting
            </p>
          </div>
          <button
            type="button"
            class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-gray-400 hover:bg-gray-100 hover:text-gray-700 dark:hover:bg-white/5 dark:hover:text-white/80"
            aria-label="Yopish"
            @click="closeModal"
          >
            <X class="h-5 w-5" />
          </button>
        </div>

        <div v-if="formError" class="err mb-4">{{ formError }}</div>

        <form class="space-y-4" @submit.prevent="onSave">
          <div>
            <label for="filial-name" class="lbl">Filial nomi <span class="req">*</span></label>
            <div class="relative">
              <Building2 class="field-icon" />
              <input
                id="filial-name"
                v-model="form.name"
                required
                placeholder="Masalan: Toshkent filial"
                class="field"
              />
            </div>
          </div>

          <div>
            <label for="filial-address" class="lbl">Manzil</label>
            <div class="relative">
              <MapPin class="field-icon" />
              <input
                id="filial-address"
                v-model="form.address"
                placeholder="Ko‘cha, tuman, shahar"
                class="field"
              />
            </div>
          </div>

          <div>
            <label for="filial-phone" class="lbl">Telefon</label>
            <div class="relative">
              <Phone class="field-icon" />
              <input
                id="filial-phone"
                :value="form.phone"
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
            <label for="filial-director" class="lbl">Direktor</label>
            <div class="relative">
              <User class="field-icon" />
              <select id="filial-director" v-model.number="form.directorId" class="field field-select">
                <option :value="0">Direktor tanlanmagan</option>
                <option v-for="u in users" :key="u.id" :value="u.id">
                  {{ u.fullName }} ({{ u.username }})
                </option>
              </select>
              <ChevronDown class="pointer-events-none absolute top-1/2 right-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-1">
            <button type="button" class="ghost" @click="closeModal">Bekor</button>
            <button type="submit" class="btn" :disabled="saving">
              {{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Building2, ChevronDown, MapPin, Phone, User, X } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  createFilial,
  deleteFilial,
  fetchFilials,
  updateFilial,
  type FilialItem,
} from '@/api/filials'
import { fetchUsers, type UserItem } from '@/api/users'
import { formatApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { formatUzPhone, isCompleteUzPhone, phoneDigits } from '@/utils/phone'

const auth = useAuthStore()
const items = ref<FilialItem[]>([])
const users = ref<UserItem[]>([])
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const form = reactive({ name: '', address: '', phone: '', directorId: 0 })

async function load() {
  error.value = null
  try {
    const [filialsRes, usersRes] = await Promise.all([
      fetchFilials(),
      auth.superAdmin ? fetchUsers() : Promise.resolve({ data: [] as UserItem[] }),
    ])
    items.value = filialsRes.data || []
    users.value = (usersRes.data || []).filter((u) => !u.role?.some((r) => r.name === 'SUPER_ADMIN'))
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function closeModal() {
  modal.value = false
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', address: '', phone: '+998-', directorId: 0 })
  formError.value = null
  modal.value = true
}

function openEdit(item: FilialItem) {
  editingId.value = item.id
  Object.assign(form, {
    name: item.name,
    address: item.address || '',
    phone: item.phone ? formatUzPhone(item.phone) : '+998-',
    directorId: item.directorId || 0,
  })
  formError.value = null
  modal.value = true
}

function onPhoneInput(e: Event) {
  const el = e.target as HTMLInputElement
  const formatted = formatUzPhone(el.value)
  form.phone = formatted
  el.value = formatted
}

async function onSave() {
  formError.value = null
  const digits = phoneDigits(form.phone)
  if (digits.length > 3 && !isCompleteUzPhone(form.phone)) {
    formError.value = 'Telefon +998-(12)-345-67-89 formatida to‘liq bo‘lishi kerak'
    return
  }
  saving.value = true
  const phoneValue = isCompleteUzPhone(form.phone) ? formatUzPhone(form.phone) : ''
  const payload = {
    name: form.name.trim(),
    address: form.address.trim(),
    phone: phoneValue,
    directorId: form.directorId || null,
  }
  try {
    if (editingId.value) await updateFilial(editingId.value, payload)
    else await createFilial(payload)
    closeModal()
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(item: FilialItem) {
  if (!confirm(`“${item.name}” o‘chirilsinmi?`)) return
  try {
    await deleteFilial(item.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

onMounted(load)
</script>

<style scoped>
.th { padding: 0.75rem 1.25rem; text-align: left; font-size: 0.75rem; color: #6b7280; }
.td { padding: 0.75rem 1.25rem; font-size: 0.875rem; color: #4b5563; }
.empty { padding: 2rem; text-align: center; font-size: 0.875rem; color: #6b7280; }
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
.field-select {
  appearance: none;
  padding-right: 2.5rem;
}
.btn { height: 2.75rem; border-radius: 0.5rem; background: #465fff; padding: 0 1.25rem; color: #fff; font-weight: 500; }
.btn:disabled { opacity: 0.6; }
.ghost { height: 2.75rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1.25rem; color: #374151; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; font-size: 0.875rem; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal {
  width: 100%;
  max-width: 28rem;
  border-radius: 1rem;
  background: #fff;
  padding: 1.5rem;
  box-shadow: 0 20px 40px rgb(0 0 0 / 12%);
}
:global(.dark) .lbl { color: #9ca3af; }
:global(.dark) .field {
  border-color: #344054;
  color: rgba(255,255,255,.9);
}
:global(.dark) .ghost { border-color: #344054; color: #d1d5db; }
:global(.dark) .modal { background: #101828; }
</style>
