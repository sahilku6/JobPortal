import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { authApi, UserResponse } from '../../core/api/services'
import { clearTokens, setTokens } from '../../core/api/axios'

interface AuthState {
  user:            UserResponse | null
  accessToken:     string | null
  refreshToken:    string | null
  isAuthenticated: boolean
  loading:         boolean
  error:           string | null
}

const TOKEN_KEY   = 'cb_access_token'
const REFRESH_KEY = 'cb_refresh_token'
const USER_KEY    = 'cb_user'

function loadFromStorage(): Partial<AuthState> {
  try {
    const accessToken  = localStorage.getItem(TOKEN_KEY)
    const refreshToken = localStorage.getItem(REFRESH_KEY)
    const userRaw      = localStorage.getItem(USER_KEY)
    const user         = userRaw ? (JSON.parse(userRaw) as UserResponse) : null
    return { accessToken, refreshToken, user, isAuthenticated: !!accessToken && !!user }
  } catch { return {} }
}

const initialState: AuthState = {
  user: null, accessToken: null, refreshToken: null,
  isAuthenticated: false, loading: false, error: null,
  ...loadFromStorage(),
}

export const loginThunk = createAsyncThunk(
  'auth/login',
  async ({ email, password }: { email: string; password: string }, { rejectWithValue }) => {
    try {
      const res = await authApi.login(email, password)
      return res.data
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      return rejectWithValue(msg ?? 'Wrong username or password')
    }
  },
)

export const registerThunk = createAsyncThunk(
  'auth/register',
  async (data: Parameters<typeof authApi.register>[0], { rejectWithValue }) => {
    try {
      const res = await authApi.register(data)
      return res.data
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      return rejectWithValue(msg ?? 'Registration failed')
    }
  },
)

export const logoutThunk = createAsyncThunk('auth/logout', async () => {
  clearTokens()
})

// OAuth callback: store tokens then fetch /me
export const setAuthFromOAuth = createAsyncThunk(
  'auth/setAuthFromOAuth',
  async ({ accessToken, refreshToken }: { accessToken: string; refreshToken: string }, { rejectWithValue }) => {
    try {
      setTokens(accessToken, refreshToken)
      const res = await authApi.getMe()
      localStorage.setItem(USER_KEY, JSON.stringify(res.data))
      return { accessToken, refreshToken, user: res.data }
    } catch {
      clearTokens()
      return rejectWithValue('Failed to load user profile')
    }
  },
)

export const getMeThunk = createAsyncThunk('auth/getMe', async (_, { rejectWithValue }) => {
  try {
    const res = await authApi.getMe()
    return res.data
  } catch { return rejectWithValue('Session expired') }
})

export const updateProfileThunk = createAsyncThunk(
  'auth/updateProfile',
  async (data: Parameters<typeof authApi.updateProfile>[0], { rejectWithValue }) => {
    try {
      const res = await authApi.updateProfile(data)
      return res.data
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      return rejectWithValue(msg ?? 'Profile update failed')
    }
  },
)

function persistAuth(state: AuthState, payload: { accessToken: string; refreshToken: string; user: UserResponse }) {
  state.loading        = false
  state.isAuthenticated = true
  state.user           = payload.user
  state.accessToken    = payload.accessToken
  state.refreshToken   = payload.refreshToken
  setTokens(payload.accessToken, payload.refreshToken)
  localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
}

function clearAuth(state: AuthState) {
  state.user           = null
  state.accessToken    = null
  state.refreshToken   = null
  state.isAuthenticated = false
  state.error          = null
  clearTokens()
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearAuthError: (state) => { state.error = null },
    setUser: (state, action: PayloadAction<UserResponse>) => {
      state.user = action.payload
      localStorage.setItem(USER_KEY, JSON.stringify(action.payload))
    },
    // Called by the axios interceptor's forceLogout (via custom event if needed)
    sessionExpired: (state) => { clearAuth(state) },
  },
  extraReducers: (builder) => {
    builder
      // register
      .addCase(registerThunk.pending,   (state) => { state.loading = true; state.error = null })
      .addCase(registerThunk.fulfilled, (state, action) => { persistAuth(state, action.payload) })
      .addCase(registerThunk.rejected,  (state, action) => { state.loading = false; state.error = action.payload as string })
      // login
      .addCase(loginThunk.pending,   (state) => { state.loading = true; state.error = null })
      .addCase(loginThunk.fulfilled, (state, action) => { persistAuth(state, action.payload) })
      .addCase(loginThunk.rejected,  (state, action) => { state.loading = false; state.error = action.payload as string })
      // logout
      .addCase(logoutThunk.fulfilled, (state) => { clearAuth(state) })
      // OAuth
      .addCase(setAuthFromOAuth.pending,   (state) => { state.loading = true })
      .addCase(setAuthFromOAuth.fulfilled, (state, action) => { persistAuth(state, action.payload) })
      .addCase(setAuthFromOAuth.rejected,  (state, action) => { state.loading = false; state.error = action.payload as string })
      // updateProfile
      .addCase(updateProfileThunk.fulfilled, (state, action) => {
        state.user = action.payload
        localStorage.setItem(USER_KEY, JSON.stringify(action.payload))
      })
      // getMe
      .addCase(getMeThunk.fulfilled, (state, action: PayloadAction<UserResponse>) => {
        state.user = action.payload
        localStorage.setItem(USER_KEY, JSON.stringify(action.payload))
      })
      .addCase(getMeThunk.rejected, (state) => { clearAuth(state) })
  },
})

export const { clearAuthError, setUser, sessionExpired } = authSlice.actions
export default authSlice.reducer

// Alias used by Navbar
export const logout = logoutThunk
