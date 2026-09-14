import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface BotSettings {
  id?: number
  botUsername?: string
  connected?: boolean
  status?: string
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

export function sendTelegramToClient(clientId: number, message: string) {
  return apiRequest<RestApiResponse<null>>('/api/telegram/messages/send-to-client', {
    method: 'POST',
    body: { clientId, message },
  })
}
