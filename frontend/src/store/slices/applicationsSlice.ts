import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { applicationsApi, ApplicationResponse } from '../../core/api/services'
import type { PageResponse } from '../../core/api/services/common'
import toast from 'react-hot-toast'

interface ApplicationsState {
  myApplications: ApplicationResponse[]
  recruiterApplications: ApplicationResponse[]
  recruiterInbox: ApplicationResponse[]          // alias for recruiterApplications
  jobApplicants: ApplicationResponse[]
  currentApplication: ApplicationResponse | null
  myTotalPages: number
  myCurrentPage: number
  myTotalElements: number
  recruiterTotalPages: number
  recruiterCurrentPage: number
  recruiterTotalElements: number
  recruiterInboxTotalPages: number
  recruiterInboxTotalElements: number
  jobApplicantsTotalPages: number
  stats: null
  loading: boolean
  error: string | null
}

const initialState: ApplicationsState = {
  myApplications: [], recruiterApplications: [], recruiterInbox: [],
  jobApplicants: [], currentApplication: null,
  myTotalPages: 0, myCurrentPage: 0, myTotalElements: 0,
  recruiterTotalPages: 0, recruiterCurrentPage: 0, recruiterTotalElements: 0,
  recruiterInboxTotalPages: 0, recruiterInboxTotalElements: 0,
  jobApplicantsTotalPages: 0, stats: null,
  loading: false, error: null,
}

export const getMyApplicationsThunk = createAsyncThunk(
  'applications/getMy',
  async ({ page = 0, size = 10 }: { page?: number; size?: number }, { rejectWithValue }) => {
    try { return (await applicationsApi.getMy(page, size)).data }
    catch { return rejectWithValue('Unable to load applications.') }
  },
)

export const getRecruiterApplicationsThunk = createAsyncThunk(
  'applications/getRecruiter',
  async ({ page = 0, size = 10 }: { page?: number; size?: number }, { rejectWithValue }) => {
    try { return (await applicationsApi.getRecruiterInbox(page, size)).data }
    catch { return rejectWithValue('Unable to load applications.') }
  },
)

export const applyThunk = createAsyncThunk(
  'applications/apply',
  async (data: { jobId: string | number; resumeUrl: string; coverLetter?: string }, { rejectWithValue }) => {
    try { return (await applicationsApi.apply(data)).data }
    catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Failed to submit application.'
      return rejectWithValue(msg)
    }
  },
)

export const applyWithResumeThunk = createAsyncThunk(
  'applications/applyWithResume',
  async (data: { jobId: string | number; resume: File; coverLetter?: string }, { rejectWithValue }) => {
    try { return (await applicationsApi.applyWithResume(data)).data }
    catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Failed to submit application.'
      return rejectWithValue(msg)
    }
  },
)

export const withdrawThunk = createAsyncThunk(
  'applications/withdraw',
  async (id: string | number, { rejectWithValue }) => {
    try { await applicationsApi.withdraw(id); return id }
    catch { return rejectWithValue('Failed to withdraw application.') }
  },
)

export const updateStatusThunk = createAsyncThunk(
  'applications/updateStatus',
  async ({ id, status, statusNote }: { id: string | number; status: string; statusNote?: string }, { rejectWithValue }) => {
    try { return (await applicationsApi.updateStatus(id, { status, statusNote })).data }
    catch { return rejectWithValue('Failed to update status.') }
  },
)

const applicationsSlice = createSlice({
  name: 'applications',
  initialState,
  reducers: { clearApplicationError: (state) => { state.error = null } },
  extraReducers: (builder) => {
    builder
      .addCase(getMyApplicationsThunk.pending,  (state) => { state.loading = true })
      .addCase(getMyApplicationsThunk.fulfilled,(state, action: PayloadAction<PageResponse<ApplicationResponse>>) => {
        state.loading = false
        state.myApplications  = action.payload.content
        state.myCurrentPage   = action.payload.currentPage
        state.myTotalPages    = action.payload.totalPages
        state.myTotalElements = action.payload.totalElements
      })
      .addCase(getMyApplicationsThunk.rejected, (state, action) => {
        state.loading = false; state.error = action.payload as string
      })
      .addCase(getRecruiterApplicationsThunk.pending,  (state) => { state.loading = true })
      .addCase(getRecruiterApplicationsThunk.fulfilled,(state, action: PayloadAction<PageResponse<ApplicationResponse>>) => {
        state.loading = false
        state.recruiterApplications      = action.payload.content
        state.recruiterInbox             = action.payload.content
        state.recruiterCurrentPage       = action.payload.currentPage
        state.recruiterTotalPages        = action.payload.totalPages
        state.recruiterTotalElements     = action.payload.totalElements
        state.recruiterInboxTotalPages   = action.payload.totalPages
        state.recruiterInboxTotalElements= action.payload.totalElements
      })
      .addCase(getRecruiterApplicationsThunk.rejected, (state, action) => {
        state.loading = false; state.error = action.payload as string
      })
      .addCase(applyThunk.pending,    (state) => { state.loading = true })
      .addCase(applyThunk.fulfilled,  (state, action: PayloadAction<ApplicationResponse>) => {
        state.loading = false; state.myApplications = [action.payload, ...state.myApplications]
      })
      .addCase(applyThunk.rejected,   (state, action) => { state.loading = false; state.error = action.payload as string })
      .addCase(applyWithResumeThunk.pending,   (state) => { state.loading = true })
      .addCase(applyWithResumeThunk.fulfilled, (state, action: PayloadAction<ApplicationResponse>) => {
        state.loading = false; state.myApplications = [action.payload, ...state.myApplications]
      })
      .addCase(applyWithResumeThunk.rejected,  (state, action) => { state.loading = false; state.error = action.payload as string })
      .addCase(withdrawThunk.fulfilled, (state, action) => {
        state.myApplications = state.myApplications.map(a =>
          a.id === action.payload ? { ...a, status: 'WITHDRAWN' } : a)
        toast.success('Application withdrawn.', { id: 'withdraw' })
      })
      .addCase(withdrawThunk.rejected, (_, action) => {
        toast.error(action.payload as string, { id: 'withdraw-err' })
      })
      .addCase(updateStatusThunk.fulfilled,(state, action: PayloadAction<ApplicationResponse>) => {
        state.recruiterApplications = state.recruiterApplications.map(a =>
          a.id === action.payload.id ? action.payload : a)
        toast.success('Status updated.', { id: 'status-update' })
      })
      .addCase(updateStatusThunk.rejected,(_, action) => {
        toast.error(action.payload as string, { id: 'status-update-err' })
      })
  },
})

export const { clearApplicationError } = applicationsSlice.actions
export default applicationsSlice.reducer

// Aliases for backward compatibility with existing feature pages
export const getMyStatsThunk           = getMyApplicationsThunk
export const getRecruiterInboxThunk    = getRecruiterApplicationsThunk
export const getJobApplicantsThunk     = getRecruiterApplicationsThunk
