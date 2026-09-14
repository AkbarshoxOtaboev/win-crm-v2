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
              <th class="th text-right whitespace-nowrap">Amallar</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id" class="border-b border-gray-100 dark:border-gray-800">
              <td class="td">{{ u.id }}</td>
              <td class="td">{{ u.username }}</td>
              <td class="td">{{ u.fullName }}</td>
              <td class="td">{{ u.phone || '—' }}</td>
              <td class="td">
                <button type="button" class="text-brand-500" @click="onToggle(u)">{{ u.status }}</button>
              </td>
              <td class="td text-right whitespace-nowrap">
                <RowActions @edit="openEdit(u)" @delete="onDelete(u)" />
              </td>
            </tr>
            <tr v-if="users.length === 0"><td colspan="6" class="empty">Foydalanuvchi yo‘q</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="modal" class="overlay" @click.self="modal = false">
      <div class="modal">
        <div v-if="formError" class="err mb-3">{{ formError }}</div>
        <form class="space-y-3" @submit.prevent="onSave">
          <input v-model="form.username" required class="field" placeholder="Username" />
          <input v-model="form.fullName" required class="field" placeholder="F.I.Sh" />
          <input v-model="form.phone" required class="field" placeholder="Telefon" />
          <input v-model="form.password" :required="!editingId" type="password" class="field" placeholder="Parol" />
          <select v-model.number="form.roleId" required class="field">
            <option :value="0" disabled>Rol</option>
            <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</option>
          </select>
          <div class="flex justify-end gap-2">
            <button type="button" class="ghost" @click="modal = false">Bekor</button>
            <button type="submit" class="btn">Saqlash</button>
          </div>
        </form>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
import { formatApiError } from '@/api/http'

const users = ref<UserItem[]>([])
const roles = ref<RoleItem[]>([])
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const modal = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ username: '', fullName: '', phone: '', password: '', roleId: 0 })

async function load() {
  error.value = null
  try {
    const [u, r] = await Promise.all([fetchUsers(), fetchRoles()])
    users.value = u.data || []
    roles.value = r.data || []
  } catch (e) {
    error.value = formatApiError(e)
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { username: '', fullName: '', phone: '', password: '', roleId: roles.value[0]?.id || 0 })
  formError.value = null
  modal.value = true
}

function openEdit(u: UserItem) {
  editingId.value = u.id
  Object.assign(form, {
    username: u.username,
    fullName: u.fullName || '',
    phone: u.phone || '',
    password: '',
    roleId: u.role?.[0]?.id || 0,
  })
  formError.value = null
  modal.value = true
}

function fd() {
  const data = new FormData()
  data.append('username', form.username)
  data.append('fullName', form.fullName)
  data.append('phone', form.phone)
  if (form.password) data.append('password', form.password)
  data.append('roleIds', String(form.roleId))
  return data
}

async function onSave() {
  formError.value = null
  try {
    if (editingId.value) await updateUser(editingId.value, fd())
    else await createUser(fd())
    modal.value = false
    await load()
  } catch (e) {
    formError.value = formatApiError(e)
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
.field { height: 2.5rem; width: 100%; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 0.75rem; }
.btn { height: 2.5rem; border-radius: 0.5rem; background: #465fff; padding: 0 1rem; color: #fff; }
.ghost { height: 2.5rem; border-radius: 0.5rem; border: 1px solid #d1d5db; padding: 0 1rem; }
.err { border-radius: 0.5rem; border: 1px solid #fecaca; background: #fef2f2; padding: 0.75rem; color: #dc2626; }
.overlay { position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.4); padding: 1rem; }
.modal { width: 100%; max-width: 28rem; border-radius: 1rem; background: #fff; padding: 1.25rem; }
</style>
