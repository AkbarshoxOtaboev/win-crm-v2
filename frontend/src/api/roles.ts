import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface PermissionItem {
  id: number
  name: string
  description?: string
}

export interface RoleItem {
  id: number
  name: string
  status?: string
  permissions?: PermissionItem[]
}

export function fetchRoles() {
  return apiRequest<RestApiResponse<RoleItem[]>>('/api/roles')
}

export function fetchRole(id: number) {
  return apiRequest<RestApiResponse<RoleItem>>(`/api/roles/${id}`)
}

export function createRole(name: string) {
  return apiRequest<RestApiResponse<RoleItem>>('/api/roles/create', {
    method: 'POST',
    body: { name },
  })
}

export function updateRole(id: number, name: string) {
  return apiRequest<RestApiResponse<RoleItem>>(`/api/roles/update/${id}`, {
    method: 'PUT',
    body: { name },
  })
}

export function deleteRole(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/roles/delete/${id}`, { method: 'DELETE' })
}

export function fetchRolePermissions(roleId: number) {
  return apiRequest<RestApiResponse<PermissionItem[]>>(`/api/roles/${roleId}/permissions`)
}

export function fetchAllPermissions() {
  return apiRequest<RestApiResponse<PermissionItem[]>>('/api/roles/all/permissions')
}

export function assignPermission(roleId: number, permissionId: number) {
  return apiRequest<RestApiResponse<null>>(`/api/roles/${roleId}/permissions/${permissionId}`, {
    method: 'POST',
  })
}

export function removePermission(roleId: number, permissionId: number) {
  return apiRequest<RestApiResponse<null>>(`/api/roles/${roleId}/permissions/${permissionId}`, {
    method: 'DELETE',
  })
}
