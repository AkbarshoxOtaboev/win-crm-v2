<template>
  <AdminLayout>
    <PageBreadcrumb :pageTitle="t('password.title')" />
    <div v-if="error" class="err mb-4">{{ error }}</div>
    <div v-if="ok" class="ok mb-4">{{ ok }}</div>
    <div class="max-w-md rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6">
      <form class="space-y-3" @submit.prevent="onSave">
        <div>
          <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">{{ t('password.newPassword') }}</label>
          <input v-model="password" type="password" required minlength="4" class="field" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">{{ t('password.confirmPassword') }}</label>
          <input v-model="confirm" type="password" required minlength="4" class="field" />
        </div>
        <div class="flex justify-end gap-2 pt-2">
          <router-link to="/profile" class="ghost inline-flex items-center">{{ t('common.cancel') }}</router-link>
          <button type="submit" class="btn" :disabled="saving">{{ saving ? '...' : t('password.save') }}</button>
        </div>
      </form>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import { fetchUsers, updateUser, type UserItem } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'

const { t } = useI18n()
const auth = useAuthStore()
const me = ref<UserItem | null>(null)
const password = ref('')
const confirm = ref('')
const error = ref<string | null>(null)
const ok = ref<string | null>(null)
const saving = ref(false)

onMounted(async () => {
  try {
    const list = await fetchUsers()
    me.value = (list.data || []).find((u) => u.username === auth.username) || null
  } catch (e) {
    error.value = formatApiError(e)
  }
})

async function onSave() {
  error.value = null
  ok.value = null
  if (password.value.length < 4) {
    error.value = t('password.tooShort')
    return
  }
  if (password.value !== confirm.value) {
    error.value = t('password.mismatch')
    return
  }
  if (!me.value) {
    error.value = formatApiError(new Error('User not found'))
    return
  }
  saving.value = true
  try {
    const fd = new FormData()
    fd.append('username', me.value.username)
    fd.append('fullName', me.value.fullName || me.value.username)
    fd.append('phone', me.value.phone || '000')
    fd.append('password', password.value)
    const roleId = me.value.role?.[0]?.id
    if (roleId) fd.append('roleIds', String(roleId))
    await updateUser(me.value.id, fd)
    ok.value = t('password.success')
    password.value = ''
    confirm.value = ''
  } catch (e) {
    error.value = formatApiError(e)
  } finally {
    saving.value = false
  }
}
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
}
.ghost {
  height: 2.5rem;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  padding: 0 1rem;
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
