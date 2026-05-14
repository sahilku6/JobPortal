import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit'
import { jobsApi, JobResponse, JobSearchParams } from '../../core/api/services'
import type { PageResponse } from '../../core/api/services/common'
import toast from 'react-hot-toast'

interface JobsState {
  jobs: JobResponse[]
  currentJob: JobResponse | null
  myJobs: JobResponse[]
  // Public job search pagination
  totalPages: number
  totalElements: number
  currentPage: number
  // Recruiter jobs pagination
  myJobsTotalPages: number
  myJobsCurrentPage: number
  myJobsTotalElements: number
  categories: string[]
  loading: boolean
  myJobsLoading: boolean
  error: string | null
}

const initialState: JobsState = {
  jobs: [],
  currentJob: null,
  myJobs: [],
  totalPages: 0,
  totalElements: 0,
  currentPage: 0,
  myJobsTotalPages: 0,
  myJobsCurrentPage: 0,
  myJobsTotalElements: 0,
  categories: [],
  loading: false,
  myJobsLoading: false,
  error: null,
}

export const searchJobsThunk = createAsyncThunk(
  'jobs/search',
  async (params: JobSearchParams, { rejectWithValue }) => {
    try {
      const res = await jobsApi.search(params)
      return res.data
    } catch {
      return rejectWithValue('Unable to load jobs. Please try again.')
    }
  },
)

export const getJobByIdThunk = createAsyncThunk(
  'jobs/getById',
  async ({ id, incrementViewCount = true }: { id: string | number; incrementViewCount?: boolean }, { rejectWithValue }) => {
    try {
      const res = await jobsApi.getById(id, incrementViewCount)
      return res.data
    } catch {
      return rejectWithValue('Job not found')
    }
  },
)

export const getMyJobsThunk = createAsyncThunk(
  'jobs/getMyJobs',
  async ({ page = 0, size = 10 }: { page?: number; size?: number }, { rejectWithValue }) => {
    try {
      const res = await jobsApi.getMyJobs(page, size)
      return res.data
    } catch {
      return rejectWithValue('Unable to load your jobs. Please try again.')
    }
  },
)

export const createJobThunk = createAsyncThunk(
  'jobs/create',
  async (data: Parameters<typeof jobsApi.create>[0], { rejectWithValue }) => {
    try {
      const res = await jobsApi.create(data)
      return res.data
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Unable to create the job. Please try again.'
      return rejectWithValue(msg)
    }
  },
)

export const updateJobThunk = createAsyncThunk(
  'jobs/update',
  async ({ id, data }: { id: string | number; data: Parameters<typeof jobsApi.update>[1] }, { rejectWithValue }) => {
    try {
      const res = await jobsApi.update(id, data)
      return res.data
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Unable to update the job. Please try again.'
      return rejectWithValue(msg)
    }
  },
)

export const deleteJobThunk = createAsyncThunk(
  'jobs/delete',
  async (id: string | number, { rejectWithValue }) => {
    try {
      await jobsApi.delete(id)
      return id
    } catch {
      return rejectWithValue('Unable to delete the job.')
    }
  },
)

export const getCategoriesThunk = createAsyncThunk('jobs/getCategories', async () => {
  const res = await jobsApi.getCategories()
  return res.data
})

const jobsSlice = createSlice({
  name: 'jobs',
  initialState,
  reducers: {
    clearCurrentJob: (state) => { state.currentJob = null },
    clearJobError:   (state) => { state.error = null },
  },
  extraReducers: (builder) => {
    builder
      // searchJobs
      .addCase(searchJobsThunk.pending,  (state) => { state.loading = true; state.error = null })
      .addCase(searchJobsThunk.fulfilled,(state, action: PayloadAction<PageResponse<JobResponse>>) => {
        state.loading       = false
        state.jobs          = action.payload.content
        // Phase 2: use currentPage (backend returns currentPage not number)
        state.currentPage   = action.payload.currentPage
        state.totalPages    = action.payload.totalPages
        state.totalElements = action.payload.totalElements
      })
      .addCase(searchJobsThunk.rejected, (state, action) => {
        state.loading = false; state.error = action.payload as string
      })
      // getById
      .addCase(getJobByIdThunk.pending,   (state) => { state.loading = true })
      .addCase(getJobByIdThunk.fulfilled, (state, action: PayloadAction<JobResponse>) => {
        state.loading = false; state.currentJob = action.payload
      })
      .addCase(getJobByIdThunk.rejected,  (state) => { state.loading = false })
      // getMyJobs
      .addCase(getMyJobsThunk.pending,  (state) => { state.myJobsLoading = true })
      .addCase(getMyJobsThunk.fulfilled,(state, action: PayloadAction<PageResponse<JobResponse>>) => {
        state.myJobsLoading      = false
        state.myJobs             = action.payload.content
        state.myJobsCurrentPage  = action.payload.currentPage
        state.myJobsTotalPages   = action.payload.totalPages
        state.myJobsTotalElements= action.payload.totalElements
      })
      .addCase(getMyJobsThunk.rejected,  (state) => { state.myJobsLoading = false })
      // create — toast shown in PostJobPage to avoid duplicates
      .addCase(createJobThunk.fulfilled, (state, action: PayloadAction<JobResponse>) => {
        state.myJobs = [action.payload, ...state.myJobs]
      })
      .addCase(createJobThunk.rejected, (state, action) => {
        state.error = action.payload as string
      })
      // update — toast shown in PostJobPage
      .addCase(updateJobThunk.fulfilled, (state, action: PayloadAction<JobResponse>) => {
        state.myJobs = state.myJobs.map(j => j.id === action.payload.id ? action.payload : j)
        if (state.currentJob?.id === action.payload.id) state.currentJob = action.payload
      })
      .addCase(updateJobThunk.rejected, (state, action) => {
        state.error = action.payload as string
      })
      // delete
      .addCase(deleteJobThunk.fulfilled, (state, action) => {
        state.myJobs = state.myJobs.filter(j => j.id !== action.payload)
        toast.success('Job deleted.', { id: 'job-delete' })
      })
      .addCase(deleteJobThunk.rejected, (_, action) => {
        toast.error(action.payload as string, { id: 'job-delete-err' })
      })
      // categories
      .addCase(getCategoriesThunk.fulfilled,(state, action: PayloadAction<string[]>) => {
        state.categories = action.payload
      })
  },
})

export const { clearCurrentJob, clearJobError } = jobsSlice.actions
export default jobsSlice.reducer
