import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import MainLayout from './layouts/MainLayout'
import { ProtectedRoute, PublicOnlyRoute } from './core/guards/RouteGuards'
import { PageSpinner } from './shared/components/ui'

// Auth
const LoginPage           = lazy(() => import('./features/auth/LoginPage'))
const RegisterPage        = lazy(() => import('./features/auth/RegisterPage'))
const ForgotPasswordPage  = lazy(() => import('./features/auth/ForgotPasswordPage'))
const OAuthCallbackPage   = lazy(() => import('./features/auth/OAuthCallbackPage'))

// Jobs
const HomePage            = lazy(() => import('./features/jobs/HomePage'))
const JobsPage            = lazy(() => import('./features/jobs/JobsPage'))
const JobDetailPage       = lazy(() => import('./features/jobs/JobDetailPage'))
const PostJobPage         = lazy(() => import('./features/jobs/PostJobPage'))
const RecruiterJobsPage   = lazy(() => import('./features/jobs/RecruiterJobsPage'))

// Applications
const ApplicationsPage          = lazy(() => import('./features/applications/ApplicationsPage'))
const RecruiterApplicationsPage = lazy(() => import('./features/applications/RecruiterApplicationsPage'))

// Dashboards
const AdminDashboardPage     = lazy(() => import('./features/dashboard/AdminDashboardPage'))
const RecruiterDashboardPage = lazy(() => import('./features/dashboard/RecruiterDashboardPage'))
const JobSeekerDashboardPage = lazy(() => import('./features/dashboard/JobSeekerDashboardPage'))

// Info / Other
const AboutUsPage      = lazy(() => import('./features/info/AboutUsPage'))
const ContactUsPage    = lazy(() => import('./features/info/ContactUsPage'))
const PrivacyPage      = lazy(() => import('./features/info/PrivacyPolicyPage'))
const TermsPage        = lazy(() => import('./features/info/TermsOfServicePage'))
const AdminPage        = lazy(() => import('./features/admin/AdminPage'))
const ProfilePage      = lazy(() => import('./features/profile/ProfilePage'))
const NotFoundPage     = lazy(() => import('./features/NotFoundPage'))

function Wrap({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<PageSpinner />}>{children}</Suspense>
}

export default function App() {
  return (
    <Wrap>
      <Routes>
        {/* Public-only (redirects to home if already logged in) */}
        <Route path="/login"           element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
        <Route path="/register"        element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />
        <Route path="/forgot-password" element={<PublicOnlyRoute><ForgotPasswordPage /></PublicOnlyRoute>} />

        {/* OAuth callback — no auth guard, tokens arrive here */}
        <Route path="/oauth/callback" element={<OAuthCallbackPage />} />

        {/* Main app layout */}
        <Route element={<MainLayout />}>
          {/* Public pages */}
          <Route path="/"         element={<HomePage />} />
          <Route path="/jobs"     element={<JobsPage />} />
          <Route path="/jobs/:id" element={<JobDetailPage />} />
          <Route path="/about"    element={<AboutUsPage />} />
          <Route path="/contact"  element={<ContactUsPage />} />
          <Route path="/privacy"  element={<PrivacyPage />} />
          <Route path="/terms"    element={<TermsPage />} />

          {/* Job Seeker */}
          <Route path="/applications" element={
            <ProtectedRoute roles={['JOB_SEEKER']}>
              <ApplicationsPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/jobseeker" element={
            <ProtectedRoute roles={['JOB_SEEKER']}>
              <JobSeekerDashboardPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/job-seeker" element={<Navigate to="/dashboard/jobseeker" replace />} />

          {/* Recruiter */}
          <Route path="/recruiter/jobs" element={
            <ProtectedRoute roles={['RECRUITER']}>
              <RecruiterJobsPage />
            </ProtectedRoute>
          } />
          <Route path="/recruiter/post-job" element={
            <ProtectedRoute roles={['RECRUITER']}>
              <PostJobPage />
            </ProtectedRoute>
          } />
          <Route path="/recruiter/jobs/:id/edit" element={
            <ProtectedRoute roles={['RECRUITER']}>
              <PostJobPage />
            </ProtectedRoute>
          } />
          <Route path="/recruiter/applications" element={
            <ProtectedRoute roles={['RECRUITER']}>
              <RecruiterApplicationsPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/recruiter" element={
            <ProtectedRoute roles={['RECRUITER']}>
              <RecruiterDashboardPage />
            </ProtectedRoute>
          } />

          {/* Admin */}
          <Route path="/admin" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminPage />
            </ProtectedRoute>
          } />
          <Route path="/dashboard/admin" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          } />

          {/* Shared protected */}
          <Route path="/profile" element={
            <ProtectedRoute roles={['JOB_SEEKER', 'RECRUITER', 'ADMIN']}>
              <ProfilePage />
            </ProtectedRoute>
          } />

          {/* Fallback redirects */}
          <Route path="/dashboard" element={<Navigate to="/" replace />} />
          <Route path="*"          element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Wrap>
  )
}
