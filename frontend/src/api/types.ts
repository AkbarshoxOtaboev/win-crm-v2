export interface RestApiResponse<T> {
  message?: string
  data: T
}

export interface PageInfo {
  size: number
  number: number
  totalElements: number
  totalPages: number
  first?: boolean
  last?: boolean
  hasNext?: boolean
  hasPrevious?: boolean
}

export interface PageResponse<T> {
  content: T[]
  page: PageInfo
}

/** Spring Data Page JSON */
export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
