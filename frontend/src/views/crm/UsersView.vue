<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Foydalanuvchilar" />
    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4 dark:border-gray-800">
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Foydalanuvchilar</h3>
        <button type="button" class="btn" @click="openCreate">+ Yangi</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full">
          <thead>
            <tr class="border-b border-gray-100 dark:border-gray-800">
              <th class="th">#</th>
              <th class="th">Login</th>
              <th class="th">F.I.Sh</th>
              <th class="th">Telefon</th>
              <th class="th">Status</th>
              <th class="th">Role</th>
              <th class="th">Filial</th>
              <th class="th text-right whitespace-nowrap">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ u.id }}</td>
              <td class="td">{{ u.username }}</td>
              <td class="td">{{ u.fullName }}</td>
              <td class="td whitespace-nowrap">{{ u.phone ? formatUzPhone(u.phone) : '—' }}</td>
              <td class="td">
                <button type="button" class="text-brand-500" @click="onToggle(u)">{{ u.status }}</button>
              </td>
              <td class="td">{{ formatRoles(u.role) }}</td>
              <td class="td">{{ u.filialName || '—' }}</td>
              <td class="td text-right whitespace-nowrap">
                <RowActions @edit="openEdit(u)" @delete="onDelete(u)" />
              </td>
            </tr>
            <tr v-if="users.length === 0"><td colspan="8" class="empty">Foydalanuvchi yo‘q</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modal" class="overlay" @click.self="closeModal">
      <div class="modal" role="dialog" aria-modal="true" :aria-labelledby="modalTitleId">
        <div class="mb-5 flex items-start justify-between gap-3">
          <div>
            <h3 id="modalTitleId" class="text-lg font-semibold text-gray-800 dark:text-white/90">
              {{ editingId ? 'Foydalanuvchini tahrirlash' : 'Yangi foydalanuvchi' }}
            </h3>
            <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
              Login, telefon va rolni to‘ldiring
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
            <label for="user-username" class="lbl">Login <span class="req">*</span></label>
            <div class="relative">
              <AtSign class="field-icon" />
              <input
                id="user-username"
                v-model="form.username"
                required
                autocomplete="username"
                placeholder="admin"
                class="field"
              />
            </div>
          </div>

          <div>
            <label for="user-fullname" class="lbl">F.I.Sh <span class="req">*</span></label>
            <div class="relative">
              <User class="field-icon" />
              <input
                id="user-fullname"
                v-model="form.fullName"
                required
                autocomplete="name"
                placeholder="Ism familiya"
                class="field"
              />
            </div>
          </div>

          <div>
            <label for="user-phone" class="lbl">Telefon <span class="req">*</span></label>
            <div class="relative">
              <Phone class="field-icon" />
              <input
                id="user-phone"
                :value="form.phone"
                required
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
            <label for="user-password" class="lbl">
              Parol <span v-if="!editingId" class="req">*</span>
              <span v-else class="font-normal text-gray-400">(ixtiyoriy)</span>
            </label>
            <div class="relative">
              <Lock class="field-icon" />
              <input
                id="user-password"
                v-model="form.password"
                :required="!editingId"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                :placeholder="editingId ? 'O‘zgartirish uchun yangi parol' : 'Parol'"
                class="field field-password"
              />
              <button
                type="button"
                class="absolute top-1/2 right-3 z-10 -translate-y-1/2 text-gray-400 hover:text-gray-700 dark:hover:text-gray-200"
                :aria-label="showPassword ? 'Parolni yashirish' : 'Parolni ko‘rish'"
                @click="showPassword = !showPassword"
              >
                <EyeOff v-if="showPassword" class="h-5 w-5" />
                <Eye v-else class="h-5 w-5" />
              </button>
            </div>
          </div>

          <div>
            <label for="user-role" class="lbl">Rol <span class="req">*</span></label>
            <div class="relative">
              <Shield class="field-icon" />
              <select id="user-role" v-model.number="form.roleId" required class="field field-select">
                <option :value="0" disabled>Rolni tanlang</option>
                <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</option>
              </select>
              <ChevronDown class="pointer-events-none absolute top-1/2 right-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
            </div>
          </div>

          <div v-if="auth.superAdmin">
            <label for="user-filial" class="lbl">Filial</label>
            <div class="relative">
              <Building2 class="field-icon" />
              <select id="user-filial" v-model.number="form.filialId" class="field field-select">
                <option :value="0">Filial tanlanmagan</option>
                <option v-for="f in filials" :key="f.id" :value="f.id">{{ f.name }}</option>
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
import { AtSign, Building2, ChevronDown, Eye, EyeOff, Lock, Phone, Shield, User, X } from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import RowActions from '@/components/crm/RowActions.vue'
import {
  changeUserStatus,
  createUser,
  deleteUser,
  fetchUsers,
  updateUser,
  type UserItem,
} from '@/api/users'
import { fetchRoles, type RoleItem } from '@/api/roles'
import { fetchFilials, type FilialItem } from '@/api/filials'
import { formatApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { formatUzPhone, isCompleteUzPhone } from '@/utils/phone'

const auth = useAuthStore()
const users = ref<UserItem[]>([])
const roles = ref<RoleItem[]>([])
const filials = ref<FilialItem[]>([])
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const showPassword = ref(false)
const saving = ref(false)
const form = reactive({ username: '', fullName: '', phone: '', password: '', roleId: 0, filialId: 0 })

function formatRoles(role?: RoleItem[]) {
  if (!role?.length) return '—'
  return role.map((r) => r.name).join(', ')
}

async function load() {
  error.value = null
  try {
    const [u, r, f] = await Promise.all([
      fetchUsers(),
      fetchRoles(),
      auth.superAdmin ? fetchFilials() : Promise.resolve({ data: [] as FilialItem[] }),
    ])
    users.value = u.data || []
    roles.value = r.data || []
    filials.value = f.data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function closeModal() {
  modal.value = false
  showPassword.value = false
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    username: '',
    fullName: '',
    phone: '+998-',
    password: '',
    roleId: roles.value[0]?.id || 0,
    filialId: auth.assignedFilialId || 0,
  })
  formError.value = null
  showPassword.value = false
  modal.value = true
}

function openEdit(u: UserItem) {
  editingId.value = u.id
  Object.assign(form, {
    username: u.username,
    fullName: u.fullName || '',
    phone: u.phone ? formatUzPhone(u.phone) : '+998-',
    password: '',
    roleId: u.role?.[0]?.id || 0,
    filialId: u.filialId || 0,
  })
  formError.value = null
  showPassword.value = false
  modal.value = true
}

function onPhoneInput(e: Event) {
  const el = e.target as HTMLInputElement
  const formatted = formatUzPhone(el.value)
  form.phone = formatted
  el.value = formatted
}

function fd() {
  const data = new FormData()
  data.append('username', form.username.trim())
  data.append('fullName', form.fullName.trim())
  data.append('phone', formatUzPhone(form.phone))
  if (form.password) data.append('password', form.password)
  data.append('roleIds', String(form.roleId))
  if (form.filialId) data.append('filialId', String(form.filialId))
  return data
}

async function onSave() {
  formError.value = null
  if (!isCompleteUzPhone(form.phone)) {
    formError.value = 'Telefon +998-(12)-345-67-89 formatida to‘liq bo‘lishi kerak'
    return
  }
  saving.value = true
  try {
    if (editingId.value) await updateUser(editingId.value, fd())
    else await createUser(fd())
    closeModal()
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}

async function onDelete(u: UserItem) {
  if (!confirm(`“${u.username}” o‘chirilsinmi?`)) return
  try {
    await deleteUser(u.id)
    await load()
  } catch (e) {
    error.value = formatApiError(e)
  }
}

async function onToggle(u: UserItem) {
  try {
    await changeUserStatus(u.id)
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
.field-password { padding-right: 2.75rem; }
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
