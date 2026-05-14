import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios'
import toast from 'react-hot-toast'

const ACCESS_TOKEN_KEY  = 'cb_access_token'
const REFRESH_TOKEN_KEY = 'cb_refresh_token'
const USER_KEY          = 'cb_user'

const viteEnv = (import.meta as ImportMeta & { env?: Record<string, string | undefined> }).env
const BASE_URL = (viteEnv?.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const API_BASE_URL = `${BASE_URL}/api/v1`

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

const PUBLIC_AUTH_PATHS = new Set([
  '/auth/login',
  '/auth/register',
  '/auth/request-email-otp',
  '/auth/verify-email-otp',
  '/auth/forgot-password/request',
  '/auth/forgot-password/verify',
  '/auth/forgot-password/reset',
])

function shouldAttachAuthHeader(url?: string): boolean {
  if (!url) return true
  const path = url.startsWith('http') ? new URL(url).pathname.replace(/^\/api\/v1/, '') : url
  return !PUBLIC_AUTH_PATHS.has(path)
}

function isPublicAuthPath(url?: string): boolean {
  if (!url) return false
  const path = url.startsWith('http') ? new URL(url).pathname.replace(/^\/api\/v1/, '') : url
  return PUBLIC_AUTH_PATHS.has(path)
}

// Request interceptor — attach token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    if (token && shouldAttachAuthHeader(config.url)) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ── Refresh / logout state ───────────────────────────────────────────────────
let isRefreshing        = false
let logoutInProgress    = false
let failedQueue: Array<{ resolve: (v: string) => void; reject: (e: unknown) => void }> = []

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((p) => (token ? p.resolve(token) : p.reject(error)))
  failedQueue = []
}

/**
 * Force-logout: clear all tokens, show a single toast, redirect once.
 * Guards against double-execution so the toast never appears twice.
 */
export function forceLogout(message: string) {
  if (logoutInProgress) return
  logoutInProgress = true

  // Cancel any pending queued requests
  processQueue(new Error('Session ended'), null)

  clearTokens()
  toast.error(message, { id: 'session-expired', duration: 4000 })

  // Short delay so React can finish any in-flight state updates before redirect
  setTimeout(() => {
    logoutInProgress = false
    if (window.location.pathname !== '/login') {
      window.location.replace('/login')
    }
  }, 100)
}

// ── Response interceptor ─────────────────────────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original  = error.config as InternalAxiosRequestConfig & { _retry?: boolean }
    const errData   = error.response?.data as { message?: string; error?: string } | undefined
    const errMessage = (errData?.message ?? errData?.error ?? '').toLowerCase()
    const status    = error.response?.status

    if (status === 401 && isPublicAuthPath(original?.url)) {
      return Promise.reject(error)
    }

    // ── 401 handling ──────────────────────────────────────────────────────────
    if (status === 401 && !original?._retry) {

      // Server explicitly says the session was invalidated (another device logged in)
      // or the token is the standard "session invalidated / log in again" message.
      const isSessionInvalidated =
        errMessage.includes('session invalidated') ||
        errMessage.includes('log in again') ||
        errMessage.includes('jti mismatch')

      if (isSessionInvalidated) {
        forceLogout('You were signed in on another device. Please sign in again.')
        return Promise.reject(error)
      }

      // If we have no refresh token at all, give up immediately
      const refreshToken = getRefreshToken()
      if (!refreshToken) {
        forceLogout('Your session has expired. Please sign in again.')
        return Promise.reject(error)
      }

      // Queue other requests while refresh is in progress
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          if (original) original.headers.Authorization = `Bearer ${token}`
          return api(original!)
        })
      }

      original._retry = true
      isRefreshing    = true

      try {
        const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
        const { accessToken, refreshToken: newRefresh } = data
        setTokens(accessToken, newRefresh)
        if (original) original.headers.Authorization = `Bearer ${accessToken}`
        processQueue(null, accessToken)
        return api(original!)
      } catch (refreshError: unknown) {
        processQueue(refreshError, null)
        const refreshErrMsg = (
          (refreshError as AxiosError<{ message?: string }>)?.response?.data?.message ?? ''
        ).toLowerCase()
        const sessionMsg = refreshErrMsg.includes('session') || refreshErrMsg.includes('jti')
          ? 'You were signed in on another device. Please sign in again.'
          : 'Your session has expired. Please sign in again.'
        forceLogout(sessionMsg)
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // ── Other HTTP error toasts (deduped by id) ───────────────────────────────
    if (status === 403)
      toast.error('You do not have permission to perform this action.', { id: 'err-403', duration: 3000 })
    else if (status === 409)
      toast.error(errData?.message || 'This action conflicts with existing data.', { id: 'err-409', duration: 3000 })
    else if (status && status >= 500)
      toast.error('A server error occurred. Please try again.', { id: 'err-500', duration: 3000 })
    // 404 — intentionally left to callers

    return Promise.reject(error)
  },
)

// ── Token helpers ────────────────────────────────────────────────────────────
let _accessToken: string | null = null

export function getAccessToken(): string | null {
  return _accessToken || localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setTokens(access: string, refresh: string) {
  _accessToken = access
  localStorage.setItem(ACCESS_TOKEN_KEY,  access)
  localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
}

export function clearTokens() {
  _accessToken = null
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  // Legacy keys
  localStorage.removeItem('refresh_token')
  localStorage.removeItem('user')
  sessionStorage.removeItem('access_token')
}

export default api
