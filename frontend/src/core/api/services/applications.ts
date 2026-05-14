import api from '../axios'
import type { PageResponse } from './common'

export interface ApplicationResponse {
  id: number
  uuid?: string
  userId: number
  jobId: number
  recruiterId: number
  status: string
  resumeUrl: string
  coverLetter?: string
  applicantName?: string
  applicantEmail?: string
  applicantPhone?: string
  jobTitle?: string
  companyName?: string
  statusNote?: string
  appliedAt: string
  updatedAt: string
}

export const applicationsApi = {
  apply: (data: { jobId: string | number; resumeUrl: string; resumePublicId?: string; coverLetter?: string }) =>
    api.post<ApplicationResponse>('/applications', data),

  // Phase 1: resume is now mandatory multipart
  applyWithResume: (data: { jobId: string | number; resume: File; coverLetter?: string }) => {
    const form = new FormData()
    form.append('jobId', String(data.jobId))
    form.append('resume', data.resume)
    if (data.coverLetter) form.append('coverLetter', data.coverLetter)
    return api.post<ApplicationResponse>('/applications/with-resume', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  getMy: (page = 0, size = 10) =>
    api.get<PageResponse<ApplicationResponse>>('/applications/my', { params: { page, size } }),

  getMyStats: () => api.get('/applications/my/stats'),

  getRecruiterInbox: (page = 0, size = 10) =>
    api.get<PageResponse<ApplicationResponse>>('/applications/recruiter/inbox', { params: { page, size } }),

  getJobApplicants: (jobId: string | number, page = 0, size = 10) =>
    api.get<PageResponse<ApplicationResponse>>(`/applications/job/${jobId}`, { params: { page, size } }),

  getById: (id: string | number) =>
    api.get<ApplicationResponse>(`/applications/${id}`),

  updateStatus: (id: string | number, data: { status: string; statusNote?: string }) =>
    api.patch<ApplicationResponse>(`/applications/${id}/status`, data),

  withdraw: (id: string | number) =>
    api.patch(`/applications/${id}/withdraw`),

  getAllApplications: (params?: { keyword?: string; status?: string; page?: number; size?: number }) =>
    api.get('/applications/admin/all', { params }),

  getMyApplications: (page = 0, size = 10) =>
    api.get('/applications/my', { params: { page, size } }),
}
