import { apiRequest } from './http'
import type { RestApiResponse, SpringPage } from './types'

export interface BotSettings {
  id?: number
  botUsername?: string
  connected?: boolean
  status?: string
}

export interface TelegramUserItem {
  id: number
  chatId?: number
  telegramUsername?: string | null
  firstName?: string | null
  lastName?: string | null
  phone?: string | null
  role?: string
  verified?: boolean
  clientId?: number | null
  clientFullName?: string | null
  status?: string
  createdAt?: string
}

export function fetchBotSettings() {
  return apiRequest<RestApiResponse<BotSettings>>('/api/telegram/bot-settings')
}

export function registerBot(botUsername: string, token: string) {
  return apiRequest<RestApiResponse<BotSettings>>('/api/telegram/bot-settings/register', {
    method: 'POST',
    body: { botUsername, token },
  })
}

export function reconnectBot() {
  return apiRequest<RestApiResponse<BotSettings>>('/api/telegram/bot-settings/reconnect', {
    method: 'POST',
  })
}

export function fetchTelegramUsers(params: { page?: number; size?: number } = {}) {
  const q = new URLSearchParams()
  q.set('page', String(params.page ?? 0))
  q.set('size', String(params.size ?? 20))
  q.set('sort', 'id,desc')
  return apiRequest<RestApiResponse<SpringPage<TelegramUserItem>>>(
    `/api/telegram/users?${q.toString()}`,
  )
}
