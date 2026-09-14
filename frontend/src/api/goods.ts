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

export function fetchUnitTypes() {
  return apiRequest<UnitType[]>('/api/unit-types')
}
