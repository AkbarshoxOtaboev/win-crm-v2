import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth'
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  getSessionId,
  setTokens,
} from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const refreshToken = ref<string | null>(getRefreshToken())
  const sessionId = ref<string | null>(getSessionId())
  const username = ref<string | null>(localStorage.getItem('wincrm_username'))
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => Boolean(accessToken.value))

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
      return data
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Login failed'
      error.value = message
      throw e
    } finally {
      loading.value = false
    }
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
    }
  }

  return {
    accessToken,
    refreshToken,
    sessionId,
    username,
    loading,
    error,
    isAuthenticated,
    login,
    logout,
  }
})
