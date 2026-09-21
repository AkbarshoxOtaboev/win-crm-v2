import i18n, { getStoredLocale } from '@/i18n'

export class ApiError extends Error {
  status: number
  body: unknown

  constructor(message: string, status: number, body?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

interface RequestOptions {
  method?: HttpMethod
  body?: unknown
  auth?: boolean
  headers?: Record<string, string>
}

const ACCESS_KEY = 'wincrm_access_token'
const REFRESH_KEY = 'wincrm_refresh_token'
const SESSION_KEY = 'wincrm_session_id'
const SUPER_ADMIN_KEY = 'wincrm_is_super_admin'
const SELECTED_FILIAL_KEY = 'wincrm_selected_filial_id'
const ASSIGNED_FILIAL_KEY = 'wincrm_assigned_filial_id'
const ASSIGNED_FILIAL_NAME_KEY = 'wincrm_assigned_filial_name'
const ROLES_KEY = 'wincrm_roles'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

export function getSessionId(): string | null {
  return localStorage.getItem(SESSION_KEY)
}

export function setTokens(accessToken: string, refreshToken: string, sessionId?: number | null) {
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
  if (sessionId != null) {
    localStorage.setItem(SESSION_KEY, String(sessionId))
  }
}

export function clearTokens() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(SESSION_KEY)
  localStorage.removeItem('wincrm_username')
  localStorage.removeItem(SUPER_ADMIN_KEY)
  localStorage.removeItem(SELECTED_FILIAL_KEY)
  localStorage.removeItem(ASSIGNED_FILIAL_KEY)
  localStorage.removeItem(ASSIGNED_FILIAL_NAME_KEY)
  localStorage.removeItem(ROLES_KEY)
}

export function isStoredSuperAdmin(): boolean {
  return localStorage.getItem(SUPER_ADMIN_KEY) === 'true'
}

export function getSelectedFilialId(): string | null {
  return localStorage.getItem(SELECTED_FILIAL_KEY)
}

export function setSelectedFilialId(id: number | string | null) {
  if (id == null || id === '' || id === 'all') {
    localStorage.removeItem(SELECTED_FILIAL_KEY)
    return
  }
  localStorage.setItem(SELECTED_FILIAL_KEY, String(id))
}

export function persistAuthProfile(profile: {
  superAdmin?: boolean
  roles?: string[]
  filialId?: number | null
  filialName?: string | null
}) {
  localStorage.setItem(SUPER_ADMIN_KEY, profile.superAdmin ? 'true' : 'false')
  localStorage.setItem(ROLES_KEY, JSON.stringify(profile.roles || []))
  if (profile.filialId != null) {
    localStorage.setItem(ASSIGNED_FILIAL_KEY, String(profile.filialId))
  } else {
    localStorage.removeItem(ASSIGNED_FILIAL_KEY)
  }
  if (profile.filialName) {
    localStorage.setItem(ASSIGNED_FILIAL_NAME_KEY, profile.filialName)
  } else {
    localStorage.removeItem(ASSIGNED_FILIAL_NAME_KEY)
  }
}

export function formatApiError(e: unknown, fallback?: string): string {
  const t = i18n.global.t
  const defaultFallback = fallback ?? String(t('errors.generic'))
  if (e instanceof ApiError) {
    if (e.status === 403) {
      return String(t('errors.forbidden'))
    }
    if (e.status === 401) {
      return String(t('errors.unauthorized'))
    }
    const body = e.body as
      | { message?: string; error?: string; phone?: string; name?: string }
      | Record<string, string>
      | null
    if (body && typeof body === 'object') {
      if ('message' in body && body.message) {
        return localizeKnownApiMessage(String(body.message), t)
      }
      if ('error' in body && body.error) {
        return localizeKnownApiMessage(String(body.error), t)
      }
      const fieldMsgs = Object.entries(body)
        .filter(([k, v]) => k !== 'timestamp' && k !== 'status' && k !== 'path' && typeof v === 'string')
        .map(([, v]) => v)
      if (fieldMsgs.length) return fieldMsgs.join('; ')
    }
    return localizeKnownApiMessage(e.message || defaultFallback, t)
  }
  if (e instanceof Error) return localizeKnownApiMessage(e.message, t)
  return defaultFallback
}

const SALE_STATUS_TRANSITION_RE = /Cannot change order status from (\w+) to (\w+)/i

function localizeKnownApiMessage(message: string, t: typeof i18n.global.t): string {
  const match = message.match(SALE_STATUS_TRANSITION_RE)
  if (match) {
    const fromKey = `saleStatus.${match[1]}`
    const toKey = `saleStatus.${match[2]}`
    const from = t(fromKey) !== fromKey ? String(t(fromKey)) : match[1]
    const to = t(toKey) !== toKey ? String(t(toKey)) : match[2]
    return String(t('errors.saleStatusTransition', { from, to }))
  }
  return message
}

function redirectToSignin() {
  if (typeof window === 'undefined') return
  const path = window.location.pathname
  if (path === '/signin') return
  const redirect = encodeURIComponent(`${path}${window.location.search}`)
  window.location.assign(`/signin?redirect=${redirect}`)
}

let refreshPromise: Promise<boolean> | null = null

async function tryRefreshToken(): Promise<boolean> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) return false

  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!res.ok) {
      clearTokens()
      return false
    }
    const data = (await res.json()) as {
      accessToken: string
      refreshToken: string
      sessionId?: number
      roles?: string[]
      superAdmin?: boolean
      filialId?: number | null
      filialName?: string | null
    }
    setTokens(data.accessToken, data.refreshToken, data.sessionId)
    persistAuthProfile({
      superAdmin: data.superAdmin,
      roles: data.roles,
      filialId: data.filialId,
      filialName: data.filialName,
    })
    return true
  } catch {
    clearTokens()
    return false
  }
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = true, headers = {} } = options
  const isFormData = typeof FormData !== 'undefined' && body instanceof FormData

  const doFetch = async (): Promise<Response> => {
    const reqHeaders: Record<string, string> = {
      Accept: 'application/json',
      'Accept-Language': getStoredLocale(),
      ...headers,
    }
    if (body !== undefined && !isFormData) {
      reqHeaders['Content-Type'] = 'application/json'
    }
    if (auth) {
      const token = getAccessToken()
      if (token) {
        reqHeaders.Authorization = `Bearer ${token}`
      }
      if (isStoredSuperAdmin()) {
        const filialId = getSelectedFilialId()
        if (filialId) {
          reqHeaders['X-Filial-Id'] = filialId
        }
      }
    }

    return fetch(path, {
      method,
      headers: reqHeaders,
      body:
        body === undefined
          ? undefined
          : isFormData
            ? (body as FormData)
            : JSON.stringify(body),
    })
  }

  let response = await doFetch()

  if (response.status === 401 && auth) {
    if (!refreshPromise) {
      refreshPromise = tryRefreshToken().finally(() => {
        refreshPromise = null
      })
    }
    const refreshed = await refreshPromise
    if (refreshed) {
      response = await doFetch()
    } else {
      redirectToSignin()
      throw new ApiError('Sessiya tugadi. Qayta kiring.', 401)
    }
  }

  if (!response.ok) {
    let payload: unknown = null
    try {
      payload = await response.json()
    } catch {
      /* ignore */
    }
    const message =
      (payload as { message?: string } | null)?.message ||
      `Request failed (${response.status})`
    throw new ApiError(message, response.status, payload)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }
  try {
    return JSON.parse(text) as T
  } catch {
    return text as T
  }
}
