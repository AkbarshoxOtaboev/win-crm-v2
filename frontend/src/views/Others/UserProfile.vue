<template>
  <AdminLayout>
    <PageBreadcrumb pageTitle="Profil" />

    <div v-if="error" class="err mb-4">{{ error }}</div>

    <div
      class="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] sm:p-6"
    >
      <div v-if="!user" class="text-sm text-gray-500">Yuklanmoqda...</div>

      <div v-else class="flex flex-col gap-6 sm:flex-row sm:items-start">
        <!-- Left: avatar + picker -->
        <div class="flex w-full shrink-0 flex-col items-center sm:w-56">
          <UserAvatar :preference="avatarPref" size="xl" :title="user.fullName || user.username" />
          <p class="mt-3 text-center text-sm font-semibold text-gray-800 dark:text-white/90">
            {{ user.fullName || user.username }}
          </p>
          <p class="text-center text-xs text-gray-500">@{{ user.username }}</p>

          <div class="mt-5 w-full">
            <p class="mb-2 text-center text-xs font-medium text-gray-500">Default ikonka</p>
            <div class="flex flex-wrap justify-center gap-2">
              <button
                v-for="icon in AVATAR_ICONS"
                :key="icon.id"
                type="button"
                class="icon-pick"
                :class="{
                  active: avatarPref.mode === 'icon' && avatarPref.iconId === icon.id,
                }"
                :title="icon.label"
                @click="selectIcon(icon.id)"
              >
                <span
                  class="flex h-10 w-10 items-center justify-center rounded-full"
                  :class="icon.bg"
                >
                  <component
                    :is="iconComponent(icon.id)"
                    class="h-5 w-5"
                    :class="icon.fg"
                    :stroke-width="2"
                  />
                </span>
              </button>
            </div>
          </div>

          <div class="mt-4 flex w-full flex-col gap-2">
            <label class="upload-btn">
              <Upload class="h-4 w-4" />
              Rasm yuklash
              <input
                ref="fileInput"
                type="file"
                accept="image/png,image/jpeg,image/webp,image/gif"
                class="hidden"
                @change="onFileChange"
              />
            </label>
            <button
              v-if="avatarPref.mode === 'upload'"
              type="button"
              class="ghost-btn"
              :disabled="saving"
              @click="clearUpload"
            >
              Rasmni olib tashlash
            </button>
            <p v-if="avatarMsg" class="text-center text-xs" :class="avatarMsgError ? 'text-error-500' : 'text-success-600'">
              {{ avatarMsg }}
            </p>
          </div>
        </div>

        <!-- Right: profile info -->
        <div class="min-w-0 flex-1 space-y-4 border-t border-gray-100 pt-5 sm:border-t-0 sm:border-s sm:ps-6 sm:pt-0 dark:border-gray-800">
          <div>
            <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">Shaxsiy ma’lumotlar</h3>
            <p class="mt-1 text-sm text-gray-500">Hisob ma’lumotlari va statistika</p>
          </div>

          <dl class="grid gap-3 sm:grid-cols-2">
            <div class="info-cell">
              <dt>Login</dt>
              <dd>{{ user.username }}</dd>
            </div>
            <div class="info-cell">
              <dt>F.I.Sh</dt>
              <dd>{{ user.fullName || '—' }}</dd>
            </div>
            <div class="info-cell">
              <dt>Telefon</dt>
              <dd>{{ user.phone || '—' }}</dd>
            </div>
            <div class="info-cell">
              <dt>Status</dt>
              <dd>{{ user.status || '—' }}</dd>
            </div>
            <div class="info-cell sm:col-span-2">
              <dt>Rollar</dt>
              <dd>{{ (user.role || []).map((r) => r.name).join(', ') || '—' }}</dd>
            </div>
          </dl>

          <div v-if="stats" class="grid grid-cols-3 gap-3 pt-1">
            <div class="stat-cell">
              <span class="stat-lbl">Savdolar</span>
              <span class="stat-val">{{ stats.orderCount ?? '—' }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-lbl">To‘lovlar</span>
              <span class="stat-val">{{ stats.paymentSum ?? '—' }}</span>
            </div>
            <div class="stat-cell">
              <span class="stat-lbl">Qarz</span>
              <span class="stat-val">{{ stats.debtSum ?? '—' }}</span>
            </div>
          </div>

          <div id="password" class="password-box">
            <div class="mb-3">
              <h4 class="text-base font-semibold text-gray-800 dark:text-white/90">
                {{ t('password.title') }}
              </h4>
              <p class="mt-0.5 text-sm text-gray-500">Yangi parol kiriting yoki generatsiya qiling</p>
            </div>
            <div v-if="pwdError" class="err mb-3">{{ pwdError }}</div>
            <div v-if="pwdOk" class="ok mb-3">{{ pwdOk }}</div>
            <form class="space-y-3" @submit.prevent="onChangePassword">
              <div>
                <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">
                  {{ t('password.newPassword') }}
                </label>
                <div class="flex gap-2">
                  <input
                    v-model="newPassword"
                    :type="showPassword ? 'text' : 'password'"
                    required
                    minlength="4"
                    class="field flex-1"
                    autocomplete="new-password"
                  />
                  <button type="button" class="ghost-btn !w-auto px-3" @click="showPassword = !showPassword">
                    {{ showPassword ? 'Yashirish' : 'Ko‘rsatish' }}
                  </button>
                  <button type="button" class="gen-btn" @click="generatePassword">
                    <Sparkles class="h-4 w-4" />
                    {{ t('password.generate') }}
                  </button>
                </div>
              </div>
              <div>
                <label class="mb-1 block text-sm text-gray-600 dark:text-gray-400">
                  {{ t('password.confirmPassword') }}
                </label>
                <input
                  v-model="confirmPassword"
                  :type="showPassword ? 'text' : 'password'"
                  required
                  minlength="4"
                  class="field"
                  autocomplete="new-password"
                />
              </div>
              <div class="flex justify-end">
                <button type="submit" class="upload-btn !w-auto px-5" :disabled="pwdSaving">
                  {{ pwdSaving ? '...' : t('password.save') }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import {
  Briefcase,
  Coffee,
  GraduationCap,
  Headphones,
  Rocket,
  Smile,
  Sparkles,
  Upload,
  User,
} from 'lucide-vue-next'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import {
  fetchUser,
  fetchUsers,
  fetchUserStats,
  updateUser,
  type UserItem,
  type UserStat,
} from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import { formatApiError } from '@/api/http'
import {
  AVATAR_ICONS,
  fileToAvatarDataUrl,
  getAvatarPreference,
  saveAvatarPreference,
  type AvatarIconId,
  type AvatarPreference,
} from '@/utils/avatar'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()
const user = ref<UserItem | null>(null)
const stats = ref<UserStat | null>(null)
const error = ref<string | null>(null)
const saving = ref(false)
const avatarMsg = ref('')
const avatarMsgError = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const avatarPref = ref<AvatarPreference>(getAvatarPreference(auth.username))

const newPassword = ref('')
const confirmPassword = ref('')
const showPassword = ref(false)
const pwdSaving = ref(false)
const pwdError = ref<string | null>(null)
const pwdOk = ref<string | null>(null)

const iconComponents: Record<AvatarIconId, Component> = {
  user: User,
  smile: Smile,
  briefcase: Briefcase,
  headphones: Headphones,
  graduation: GraduationCap,
  coffee: Coffee,
  rocket: Rocket,
}

function iconComponent(id: AvatarIconId) {
  return iconComponents[id] || User
}

function generatePassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$'
  const len = 12
  const bytes = new Uint32Array(len)
  crypto.getRandomValues(bytes)
  let out = ''
  for (let i = 0; i < len; i++) out += chars[bytes[i]! % chars.length]
  newPassword.value = out
  confirmPassword.value = out
  showPassword.value = true
  pwdError.value = null
  pwdOk.value = null
}

async function onChangePassword() {
  pwdError.value = null
  pwdOk.value = null
  if (newPassword.value.length < 4) {
    pwdError.value = String(t('password.tooShort'))
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    pwdError.value = String(t('password.mismatch'))
    return
  }
  if (!user.value) {
    pwdError.value = 'Foydalanuvchi topilmadi'
    return
  }
  pwdSaving.value = true
  try {
    const fd = new FormData()
    fd.append('username', user.value.username)
    fd.append('fullName', user.value.fullName || user.value.username)
    fd.append('phone', user.value.phone || '000')
    fd.append('password', newPassword.value)
    for (const r of user.value.role || []) {
      if (r.id != null) fd.append('roleIds', String(r.id))
    }
    if (user.value.filialId) fd.append('filialId', String(user.value.filialId))
    await updateUser(user.value.id, fd)
    pwdOk.value = String(t('password.success'))
    newPassword.value = ''
    confirmPassword.value = ''
    showPassword.value = false
  } catch (e) {
    pwdError.value = formatApiError(e)
  } finally {
    pwdSaving.value = false
  }
}

function reloadAvatar() {
  avatarPref.value = getAvatarPreference(auth.username)
}

function onAvatarEvent(e: Event) {
  const detail = (e as CustomEvent<{ username?: string }>).detail
  if (!detail?.username || detail.username === auth.username) reloadAvatar()
}

function selectIcon(id: AvatarIconId) {
  if (!auth.username) return
  const next: AvatarPreference = {
    mode: 'icon',
    iconId: id,
    dataUrl: null,
    photoLink: avatarPref.value.photoLink,
  }
  saveAvatarPreference(auth.username, next)
  avatarPref.value = next
  avatarMsg.value = 'Ikonka tanlandi'
  avatarMsgError.value = false
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !auth.username || !user.value) return

  if (!file.type.startsWith('image/')) {
    avatarMsg.value = 'Faqat rasm fayli yuklang'
    avatarMsgError.value = true
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    avatarMsg.value = 'Rasm 5 MB dan oshmasin'
    avatarMsgError.value = true
    return
  }

  saving.value = true
  avatarMsg.value = ''
  try {
    const dataUrl = await fileToAvatarDataUrl(file)
    let photoLink = avatarPref.value.photoLink || null

    // Try persist on server when roles allow (USER_EDIT)
    try {
      const fd = new FormData()
      fd.append('username', user.value.username)
      fd.append('fullName', user.value.fullName || user.value.username)
      fd.append('phone', user.value.phone || '')
      for (const r of user.value.role || []) {
        if (r.id != null) fd.append('roleIds', String(r.id))
      }
      if (user.value.filialId) fd.append('filialId', String(user.value.filialId))
      fd.append('photo', file)
      const res = await updateUser(user.value.id, fd)
      photoLink = res.data?.photoLink || photoLink
      user.value = { ...user.value, photoLink: photoLink || undefined }
    } catch {
      /* local-only fallback if no permission */
    }

    const next: AvatarPreference = {
      mode: 'upload',
      iconId: avatarPref.value.iconId,
      dataUrl,
      photoLink,
    }
    saveAvatarPreference(auth.username, next)
    avatarPref.value = next
    avatarMsg.value = 'Rasm saqlandi'
    avatarMsgError.value = false
  } catch (err) {
    avatarMsg.value = formatApiError(err, 'Rasm yuklanmadi')
    avatarMsgError.value = true
  } finally {
    saving.value = false
  }
}

function clearUpload() {
  if (!auth.username) return
  const next: AvatarPreference = {
    mode: 'icon',
    iconId: avatarPref.value.iconId || 'user',
    dataUrl: null,
    photoLink: avatarPref.value.photoLink,
  }
  saveAvatarPreference(auth.username, next)
  avatarPref.value = next
  avatarMsg.value = 'Default ikonkaga qaytildi'
  avatarMsgError.value = false
}

onMounted(async () => {
  window.addEventListener('wincrm-avatar-changed', onAvatarEvent)
  reloadAvatar()
  try {
    const list = await fetchUsers()
    const me = (list.data || []).find((u) => u.username === auth.username)
    if (!me) return
    user.value = (await fetchUser(me.id)).data
    if (user.value?.photoLink && auth.username) {
      const pref = getAvatarPreference(auth.username)
      if (pref.mode === 'upload' && !pref.dataUrl && user.value.photoLink) {
        const synced: AvatarPreference = { ...pref, photoLink: user.value.photoLink }
        saveAvatarPreference(auth.username, synced)
        avatarPref.value = synced
      }
    }
    try {
      stats.value = (await fetchUserStats(me.id)).data
    } catch {
      /* stats optional */
    }
  } catch (e) {
    error.value = formatApiError(e)
  }
  if (route.hash === '#password') {
    await nextTick()
    document.getElementById('password')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
})

onUnmounted(() => {
  window.removeEventListener('wincrm-avatar-changed', onAvatarEvent)
})
</script>

<style scoped>
.err {
  border-radius: 0.5rem;
  border: 1px solid #fecaca;
  background: #fef2f2;
  padding: 0.75rem 1rem;
  color: #dc2626;
  font-size: 0.875rem;
}
.icon-pick {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  border: 2px solid transparent;
  padding: 0.25rem;
  transition: border-color 0.15s, background 0.15s;
}
.icon-pick:hover {
  background: #f3f4f6;
}
.icon-pick.active {
  border-color: #465fff;
  background: #eff4ff;
}
.upload-btn {
  display: inline-flex;
  height: 2.5rem;
  width: 100%;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  border-radius: 0.5rem;
  background: #465fff;
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
}
.upload-btn:hover {
  opacity: 0.92;
}
.ghost-btn {
  display: inline-flex;
  height: 2.5rem;
  width: 100%;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  font-size: 0.875rem;
  color: #374151;
}
.info-cell {
  border-radius: 0.75rem;
  border: 1px solid #f3f4f6;
  background: #f9fafb;
  padding: 0.75rem 1rem;
}
.info-cell dt {
  font-size: 0.75rem;
  color: #6b7280;
  margin-bottom: 0.2rem;
}
.info-cell dd {
  font-size: 0.875rem;
  font-weight: 500;
  color: #1f2937;
  word-break: break-word;
}
.stat-cell {
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  padding: 0.75rem;
  text-align: center;
}
.stat-lbl {
  display: block;
  font-size: 0.7rem;
  color: #6b7280;
}
.stat-val {
  display: block;
  margin-top: 0.25rem;
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1f2937;
}
.password-box {
  margin-top: 0.5rem;
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  padding: 1rem;
}
.field {
  height: 2.5rem;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid #d1d5db;
  background: #fff;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  color: #1f2937;
  outline: none;
}
.field:focus {
  border-color: #9cb0ff;
  box-shadow: 0 0 0 3px rgb(70 95 255 / 12%);
}
.gen-btn {
  display: inline-flex;
  height: 2.5rem;
  flex-shrink: 0;
  align-items: center;
  gap: 0.35rem;
  border-radius: 0.5rem;
  border: 1px solid #c7d2fe;
  background: #eff4ff;
  padding: 0 0.85rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #465fff;
  white-space: nowrap;
}
.ok {
  border-radius: 0.5rem;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  padding: 0.75rem;
  color: #166534;
  font-size: 0.875rem;
}
:global(.dark) .icon-pick:hover {
  background: rgb(255 255 255 / 5%);
}
:global(.dark) .icon-pick.active {
  background: rgb(70 95 255 / 12%);
}
:global(.dark) .ghost-btn {
  border-color: #344054;
  color: #d1d5db;
}
:global(.dark) .info-cell {
  border-color: #344054;
  background: rgb(255 255 255 / 3%);
}
:global(.dark) .info-cell dd,
:global(.dark) .stat-val {
  color: rgba(255, 255, 255, 0.9);
}
:global(.dark) .stat-cell,
:global(.dark) .password-box {
  border-color: #344054;
  background: rgb(255 255 255 / 3%);
}
:global(.dark) .field {
  border-color: #344054;
  background: #101828;
  color: rgba(255, 255, 255, 0.9);
}
:global(.dark) .gen-btn {
  border-color: #465fff;
  background: rgb(70 95 255 / 15%);
}
</style>
