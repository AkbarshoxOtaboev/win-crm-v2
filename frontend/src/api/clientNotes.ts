import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface ClientNote {
  id: number
  clientId?: number
  saleOrderId?: number
  type?: string
  content?: string
  interactionDate?: string
  reminderDate?: string
  reminderStatus?: string
  promisedAmount?: number
  createdUsername?: string
}

export interface ClientNotePayload {
  clientId: number
  type: string
  content: string
  saleOrderId?: number | null
  interactionDate?: string
  reminderDate?: string
  promisedAmount?: number | null
}

export function fetchClientNotes(clientId: number) {
  return apiRequest<RestApiResponse<ClientNote[]>>(`/api/client-notes/client/${clientId}`)
}

export function createClientNote(payload: ClientNotePayload) {
  return apiRequest<RestApiResponse<ClientNote>>('/api/client-notes/create', {
    method: 'POST',
    body: payload,
  })
}

export function updateClientNote(id: number, payload: ClientNotePayload) {
  return apiRequest<RestApiResponse<ClientNote>>(`/api/client-notes/update/${id}`, {
    method: 'PUT',
    body: payload,
  })
}

export function updateReminderStatus(id: number, reminderStatus: string) {
  return apiRequest<RestApiResponse<ClientNote>>(
    `/api/client-notes/${id}/reminder-status?reminderStatus=${encodeURIComponent(reminderStatus)}`,
    { method: 'PATCH' },
  )
}

export function deleteClientNote(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/client-notes/delete/${id}`, {
    method: 'DELETE',
  })
}
