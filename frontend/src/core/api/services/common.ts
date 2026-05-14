// Phase 2: PageResponse now uses currentPage (not .number)
export interface PageResponse<T> {
  content: T[]
  currentPage: number   // 0-based index
  totalPages: number
  totalElements: number
  pageSize: number
  first: boolean
  last: boolean
}
