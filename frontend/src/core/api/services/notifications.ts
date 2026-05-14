import api from '../axios'
import type { PageResponse } from './common'

export interface NotificationApiResponse {
  id: number
  uuid?: string
  userId: number
  applicationId?: number
  to?: string
  subject: string
  body: string
  type?: string
  status?: string
  isRead?: boolean
  errorMessage?: string
  readAt?: string
  sentAt?: string
  createdAt: string
}

export interface FeedbackReview {
  id: number
  userId: number
  userName: string
  userRole: string
  reviewText: string
  rating: number
  triggerType: string
  createdAt: string
}

export const notificationsApi = {
  getMyNotifications: (page = 0, size = 20) =>
    api.get<PageResponse<NotificationApiResponse>>('/notifications/my', { params: { page, size } }),

  subscribeToMyNotifications: (token: string): EventSource => {
    const base = ((import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
    return new EventSource(`${base}/api/v1/notifications/my/stream?token=${encodeURIComponent(token)}`)
  },

  markAsRead: (id: string | number) =>
    api.patch(`/notifications/my/${id}/read`),

  markAllAsRead: () =>
    api.patch('/notifications/my/read-all'),

  clearAll: () =>
    api.delete('/notifications/my'),

  deleteOne: (id: string | number) =>
    api.delete(`/notifications/my/${id}`),

  // Phase 3: Feedback
  submitFeedback: (data: {
    reviewText: string; rating: number; triggerType: string; userName?: string; userRole?: string
  }) => api.post('/notifications/feedback', data),

  getPublicFeedback: (limit = 10) =>
    api.get<FeedbackReview[]>('/notifications/feedback/public', { params: { limit } }),
}
