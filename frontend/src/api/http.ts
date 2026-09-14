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
}

export function formatApiError(e: unknown, fallback = 'Xatolik yuz berdi'): string {
  if (e instanceof ApiError) {
    if (e.status === 403) {
      return 'Ruxsat yo‘q (403). Bu amal uchun yetarli huquqingiz yo‘q.'
    }
    if (e.status === 401) {
      return 'Sessiya tugadi. Qayta kiring.'
    }
    const body = e.body as
      | { message?: string; error?: string; phone?: string; name?: string }
      | Record<string, string>
      | null
    if (body && typeof body === 'object') {
      if ('message' in body && body.message) return String(body.message)
      if ('error' in body && body.error) return String(body.error)
      const fieldMsgs = Object.entries(body)
        .filter(([k, v]) => k !== 'timestamp' && k !== 'status' && k !== 'path' && typeof v === 'string')
        .map(([, v]) => v)
      if (fieldMsgs.length) return fieldMsgs.join('; ')
    }
    return e.message || fallback
  }
  if (e instanceof Error) return e.message
  return fallback
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
    }
    setTokens(data.accessToken, data.refreshToken, data.sessionId)
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
