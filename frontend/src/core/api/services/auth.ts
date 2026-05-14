import api from '../axios'

export interface UserResponse {
  id: number
  uuid?: string
  username: string
  email: string
  fullName: string
  phoneNumber?: string
  profileImageUrl?: string
  bio?: string
  location?: string
  companyName?: string
  role: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserResponse
}

export const authApi = {
  register: (data: {
    username: string; email: string; password: string; otp?: string
    fullName: string; phoneNumber: string; role: string; companyName?: string
  }) => api.post<AuthResponse>('/auth/register', data),

  requestEmailOtp: (email: string) =>
    api.post('/auth/request-email-otp', { email }),

  verifyEmailOtp: (email: string, otp: string) =>
    api.post('/auth/verify-email-otp', { email, otp }),

  login: (email: string, password: string) =>
    api.post<AuthResponse>('/auth/login', { email, password }),

  refresh: (refreshToken: string) =>
    api.post<AuthResponse>('/auth/refresh', { refreshToken }),

  getMe: () => api.get<UserResponse>('/users/me'),

  updateProfile: (data: { fullName?: string; phoneNumber?: string; bio?: string; location?: string; companyName?: string }) =>
    api.put<UserResponse>('/users/me', data),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/users/me/change-password', { currentPassword, newPassword }),

  uploadProfileImage: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.post<UserResponse>('/users/me/profile-image', form, {
      headers: { 'Content-Type': undefined },
    })
  },

  // Phase 4: Forgot password
  forgotPasswordRequest: (email: string) =>
    api.post<{ message: string }>('/auth/forgot-password/request', { email }),

  forgotPasswordVerify: (email: string, otp: string) =>
    api.post<{ message: string }>('/auth/forgot-password/verify', { email, otp }),

  forgotPasswordReset: (email: string, otp: string, newPassword: string) =>
    api.post<{ message: string }>('/auth/forgot-password/reset', { email, otp, newPassword }),

  // Phase 3: Feedback prompt
  shouldPromptFeedback: (triggerType: 'FIRST_JOB_POST' | 'FIRST_JOB_APPLY') =>
    api.get<{ shouldPrompt: boolean; triggerType: string }>(
      '/users/should-prompt-feedback', { params: { triggerType } }
    ),

  // Admin
  getAllUsers: (params?: { keyword?: string; role?: string; isActive?: boolean; page?: number; size?: number }) =>
    api.get('/users', { params }),

  getUsersByRole: (role: string, page = 0, size = 10) =>
    api.get('/users/role/' + role, { params: { page, size } }),

  toggleUserStatus: (id: string | number) =>
    api.patch(`/users/${id}/toggle-status`),

  deleteUser: (id: string | number) =>
    api.delete(`/users/${id}`),

  // OTP (used by RegisterPage)
  sendOtp: (email: string) =>
    api.post('/auth/request-email-otp', { email }),

  verifyOtp: (email: string, otp: string) =>
    api.post('/auth/verify-email-otp', { email, otp }),
}
