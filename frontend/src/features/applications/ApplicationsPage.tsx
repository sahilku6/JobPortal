import { getMyApplicationsThunk, withdrawThunk } from '../../store/slices/applicationsSlice'
import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../../shared/hooks/redux'
import { Briefcase, Clock, CheckCircle2, XCircle, Award, CheckCircle } from 'lucide-react'
import { PageSpinner, EmptyState, StatsCard, Pagination } from '../../shared/components/ui'
import { Link } from 'react-router-dom'
import type { ApplicationResponse } from '../../core/api/services'
import FeedbackPromptModal from '../../shared/components/feedback/FeedbackPromptModal'
import { JobGroupSection } from './JobGroupSection'


// Group applications by job
function groupApplicationsByJob(applications: ApplicationResponse[]): Map<number, { jobId: number; jobTitle: string; companyName: string; applicants: ApplicationResponse[] }> {
  const grouped = new Map<number, { jobId: number; jobTitle: string; companyName: string; applicants: ApplicationResponse[] }>()

  for (const app of applications) {
    if (!grouped.has(app.jobId)) {
      grouped.set(app.jobId, {
        jobId: app.jobId,
        jobTitle: app.jobTitle || 'Position',
        companyName: app.companyName || 'Company',
        applicants: [],
      })
    }
    grouped.get(app.jobId)!.applicants.push(app)
  }

  return grouped
}

export default function ApplicationsPage() {
  const dispatch = useAppDispatch()
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
    const { myApplications, stats, loading, myTotalPages } = useAppSelector((s) => s.applications)
  const [page, setPage] = useState(() => Number(searchParams.get('page')) || 0)
  const successState = location.state as { submitted?: boolean; feedbackPrompt?: boolean; jobTitle?: string; companyName?: string } | null
  const querySubmitted = searchParams.get('applied') === '1'
  const sessionSubmitted = sessionStorage.getItem('jobApplySuccess') === '1'
  const successJobTitle = successState?.jobTitle || searchParams.get('jobTitle') || undefined
  const successCompanyName = successState?.companyName || searchParams.get('companyName') || undefined
  const [feedbackOpen, setFeedbackOpen] = useState(false)
  const [showSubmittedBanner, setShowSubmittedBanner] = useState(Boolean(successState?.submitted || querySubmitted || sessionSubmitted))
  
  // Show feedback prompt only on first job application

  useEffect(() => {
    dispatch(getMyApplicationsThunk({ page, size: 10 }))
    dispatch(getMyApplicationsThunk({ page: 0, size: 10 }))
  }, [dispatch, page])

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams)
    if (page > 0) nextParams.set('page', String(page))
    else nextParams.delete('page')
    setSearchParams(nextParams, { replace: true })
  }, [page, searchParams, setSearchParams])

  const hasSubmitSignal = Boolean(successState?.submitted || querySubmitted || sessionSubmitted)

  useEffect(() => {
    if (!hasSubmitSignal) return

    setShowSubmittedBanner(true)
    setFeedbackOpen(false)
    if (sessionSubmitted) {
      sessionStorage.removeItem('jobApplySuccess')
    }

    const nextParams = new URLSearchParams(searchParams)
    let paramsChanged = false
    if (nextParams.has('applied')) {
      nextParams.delete('applied')
      paramsChanged = true
    }
    if (nextParams.has('jobTitle')) {
      nextParams.delete('jobTitle')
      paramsChanged = true
    }
    if (nextParams.has('companyName')) {
      nextParams.delete('companyName')
      paramsChanged = true
    }

    if (paramsChanged) {
      setSearchParams(nextParams, { replace: true })
    } else if (successState?.submitted) {
      navigate({ pathname: location.pathname, search: location.search }, { replace: true })
    }
  }, [
    location.pathname,
    location.search,
    navigate,
    querySubmitted,
    searchParams,
    sessionSubmitted,
    setSearchParams,
    successState?.feedbackPrompt,
    successState?.submitted,
  ])

  useEffect(() => {
    if (!showSubmittedBanner) return

    window.scrollTo({ top: 0, behavior: 'smooth' })
    const timeoutId = window.setTimeout(() => setShowSubmittedBanner(false), 5000)
    return () => window.clearTimeout(timeoutId)
  }, [showSubmittedBanner])

  const dismissSubmittedBanner = () => setShowSubmittedBanner(false)
  const handleWithdraw = (id: number) => dispatch(withdrawThunk(id))

  if (loading && myApplications.length === 0) return <PageSpinner />

  return (
    <div className="py-8">
      <div className="page-container">
        <div className="mb-8">
          <h2 className="section-title mb-1">Applications</h2>
              <p className="text-body-sm text-slate-500">Track and manage your job applications</p>
        </div>

        {showSubmittedBanner && successState?.submitted && (
          <div className="mb-6 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-4 text-emerald-900 shadow-sm dark:border-emerald-900/40 dark:bg-emerald-900/20 dark:text-emerald-100">
            <div className="flex items-start gap-3">
              <div className="mt-0.5 rounded-full bg-emerald-100 p-2 text-emerald-600 dark:bg-emerald-900/40 dark:text-emerald-300">
                <CheckCircle className="w-5 h-5" />
              </div>
              <div className="flex-1">
                <p className="font-semibold text-body-sm">Application submitted successfully</p>
                    <p className="text-body-sm text-emerald-800/90 dark:text-emerald-100/90">
                  {successJobTitle
                    ? `Your application for ${successJobTitle}${successCompanyName ? ` at ${successCompanyName}` : ''} is now in My Applications.`
                    : 'Your application is now in My Applications.'}
                </p>
              </div>
              <button onClick={dismissSubmittedBanner} className="rounded-lg px-2 py-1 text-body-sm font-medium text-emerald-700 hover:bg-emerald-100 dark:text-emerald-200 dark:hover:bg-emerald-900/40">
                Dismiss
              </button>
            </div>
          </div>
        )}

        <FeedbackPromptModal
            open={feedbackOpen}
            onClose={() => setFeedbackOpen(false)}
            triggerType="FIRST_JOB_APPLY"
          />

        {/* Stats */}
        {stats && (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4 mb-8">
            <StatsCard label="Total Applied" value={0 || 0} icon={<Briefcase className="w-5 h-5" />} color="blue" />
            <StatsCard label="Under Review" value={0} icon={<Clock className="w-5 h-5" />} color="orange" />
            <StatsCard label="Shortlisted" value={0 || 0} icon={<CheckCircle2 className="w-5 h-5" />} color="purple" />
            <StatsCard label="Offers" value={0 || 0} icon={<Award className="w-5 h-5" />} color="green" />
            <StatsCard label="Rejected" value={0 || 0} icon={<XCircle className="w-5 h-5" />} color="red" />
          </div>
        )}

        {myApplications.length === 0 ? (
          <EmptyState title="No applications yet" description="Start applying to jobs to track your progress here."
            icon={<Briefcase className="w-12 h-12" />}
            action={<Link to="/jobs" className="btn-primary">Browse Jobs</Link>}
          />
        ) : (
          <>
            {Array.from(groupApplicationsByJob(myApplications).values()).map((group) => (
              <JobGroupSection
                key={group.jobId}
                jobId={group.jobId}
                jobTitle={group.jobTitle}
                companyName={group.companyName}
                applicants={group.applicants}
                onWithdraw={handleWithdraw}
                showExport={false}
              />
            ))}
            <Pagination page={page} totalPages={myTotalPages} onPageChange={(p: number) => { setPage(p) }} />
          </>
        )}
      </div>
    </div>
  )
}
