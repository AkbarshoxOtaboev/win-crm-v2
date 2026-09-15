import { apiRequest } from './http'
import type { RestApiResponse } from './types'

export interface Goods {
  id: number
  name: string
  goodsGroupId?: number
  goodsGroupName?: string
  unitTypeId?: number
  unitTypeName?: string
  type?: string
  typeLabel?: string
  priceCost?: number
  priceSelling?: number
  barcode?: string
  photo?: string
  width?: number
  height?: number
  status?: string
}

export interface GoodsGroup {
  id: number
  name: string
  status?: string
}

export interface UnitType {
  id: number
  name: string
  status?: string
}

export function fetchGoods() {
  return apiRequest<RestApiResponse<Goods[]>>('/api/goods')
}

export function deleteGoods(id: number) {
  return apiRequest<RestApiResponse<null>>(`/api/goods/delete/${id}`, { method: 'DELETE' })
}

export function createGoods(form: FormData) {
  return apiRequest<RestApiResponse<Goods>>('/api/goods/create', {
    method: 'POST',
    body: form,
  })
}

export function updateGoods(id: number, form: FormData) {
  return apiRequest<RestApiResponse<Goods>>(`/api/goods/update/${id}`, {
    method: 'PUT',
    body: form,
  })
}

export function fetchGoodsGroups() {
  return apiRequest<GoodsGroup[]>('/api/goods-groups')
}

export function createGoodsGroup(name: string) {
  return apiRequest<GoodsGroup>('/api/goods-groups', {
    method: 'POST',
    body: { name },
  })
}

export function updateGoodsGroup(id: number, name: string) {
  return apiRequest<GoodsGroup>(`/api/goods-groups/${id}`, {
    method: 'PUT',
    body: { name },
  })
}

export function deleteGoodsGroup(id: number) {
  return apiRequest<void>(`/api/goods-groups/${id}`, { method: 'DELETE' })
}

export function changeGoodsGroupStatus(id: number) {
  return apiRequest<GoodsGroup>(`/api/goods-groups/${id}/change-status`, {
    method: 'PUT',
  })
}

export function fetchUnitTypes() {
  return apiRequest<UnitType[]>('/api/unit-types')
}

export function createUnitType(name: string) {
  return apiRequest<UnitType>('/api/unit-types', {
    method: 'POST',
    body: { name },
  })
}

export function updateUnitType(id: number, name: string) {
  return apiRequest<UnitType>(`/api/unit-types/${id}`, {
    method: 'PUT',
    body: { name },
  })
}

export function deleteUnitType(id: number) {
  return apiRequest<void>(`/api/unit-types/${id}`, { method: 'DELETE' })
}

export function changeUnitTypeStatus(id: number) {
  return apiRequest<UnitType>(`/api/unit-types/${id}/change-status`, {
    method: 'PUT',
  })
}
