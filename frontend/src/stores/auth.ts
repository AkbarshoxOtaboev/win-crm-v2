import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  getSelectedFilialId,
  getSessionId,
  persistAuthProfile,
  setSelectedFilialId,
  setTokens,
} from '@/api/http'

const SUPER_ADMIN_KEY = 'wincrm_is_super_admin'
const ASSIGNED_FILIAL_KEY = 'wincrm_assigned_filial_id'
const ASSIGNED_FILIAL_NAME_KEY = 'wincrm_assigned_filial_name'
const ROLES_KEY = 'wincrm_roles'

function readRoles(): string[] {
  try {
    const raw = localStorage.getItem(ROLES_KEY)
    return raw ? (JSON.parse(raw) as string[]) : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const refreshToken = ref<string | null>(getRefreshToken())
  const sessionId = ref<string | null>(getSessionId())
  const username = ref<string | null>(localStorage.getItem('wincrm_username'))
  const loading = ref(false)
  const error = ref<string | null>(null)
  const roles = ref<string[]>(readRoles())
  const superAdmin = ref(localStorage.getItem(SUPER_ADMIN_KEY) === 'true')
  const assignedFilialId = ref<number | null>(
    localStorage.getItem(ASSIGNED_FILIAL_KEY) ? Number(localStorage.getItem(ASSIGNED_FILIAL_KEY)) : null,
  )
  const assignedFilialName = ref<string | null>(localStorage.getItem(ASSIGNED_FILIAL_NAME_KEY))
  const selectedFilialId = ref<number | null>(
    getSelectedFilialId() ? Number(getSelectedFilialId()) : null,
  )

  const isAuthenticated = computed(() => Boolean(accessToken.value))

  function applyProfile(data: authApi.AuthResponse) {
    roles.value = data.roles || []
    superAdmin.value = Boolean(data.superAdmin)
    assignedFilialId.value = data.filialId ?? null
    assignedFilialName.value = data.filialName ?? null
    persistAuthProfile({
      superAdmin: data.superAdmin,
      roles: data.roles,
      filialId: data.filialId,
      filialName: data.filialName,
    })
    if (!data.superAdmin && data.filialId) {
      selectedFilialId.value = data.filialId
      setSelectedFilialId(data.filialId)
    }
  }

  async function login(user: string, password: string) {
    loading.value = true
    error.value = null
    try {
      const data = await authApi.login({ username: user, password })
      setTokens(data.accessToken, data.refreshToken, data.sessionId)
      accessToken.value = data.accessToken
      refreshToken.value = data.refreshToken
      sessionId.value = String(data.sessionId)
      username.value = user
      localStorage.setItem('wincrm_username', user)
      applyProfile(data)
      return data
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Login failed'
      error.value = message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function hydrateProfile() {
    if (!accessToken.value) return
    try {
      const data = await authApi.fetchMe()
      applyProfile(data)
    } catch {
      /* ignore — token refresh/http layer handles session */
    }
  }

  function selectFilial(id: number | null) {
    selectedFilialId.value = id
    setSelectedFilialId(id)
  }

  async function logout() {
    try {
      if (accessToken.value) {
        await authApi.logout()
      }
    } catch {
      /* still clear local session */
    } finally {
      clearTokens()
      localStorage.removeItem('wincrm_username')
      accessToken.value = null
      refreshToken.value = null
      sessionId.value = null
      username.value = null
      error.value = null
      roles.value = []
      superAdmin.value = false
      assignedFilialId.value = null
      assignedFilialName.value = null
      selectedFilialId.value = null
    }
  }

  return {
    accessToken,
    refreshToken,
    sessionId,
    username,
    loading,
    error,
    roles,
    superAdmin,
    assignedFilialId,
    assignedFilialName,
    selectedFilialId,
    isAuthenticated,
    login,
    logout,
    hydrateProfile,
    selectFilial,
  }
})
