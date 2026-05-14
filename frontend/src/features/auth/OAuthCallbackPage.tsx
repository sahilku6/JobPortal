import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAppDispatch } from '../../shared/hooks/redux'
import { setAuthFromOAuth } from '../../store/slices/authSlice'
import { PageSpinner } from '../../shared/components/ui'
import toast from 'react-hot-toast'

export default function OAuthCallbackPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  useEffect(() => {
    const params       = new URLSearchParams(window.location.search)
    const accessToken  = params.get('accessToken')
    const refreshToken = params.get('refreshToken')
    const error        = params.get('error')

    // Remove tokens from URL immediately to prevent leakage
    window.history.replaceState({}, document.title, '/oauth/callback')

    if (error || !accessToken) {
      toast.error('Social login failed. Please try again.', { id: 'oauth-error' })
      navigate('/login', { replace: true })
      return
    }

    dispatch(setAuthFromOAuth({ accessToken, refreshToken: refreshToken ?? '' }))
      .unwrap()
      .then((result) => {
        const { user } = result
        toast.success(`Welcome, ${user.fullName || user.username}!`, { id: 'oauth-success' })
        const role = (user.role ?? '').toUpperCase()
        if (role.includes('RECRUITER'))  navigate('/dashboard/recruiter', { replace: true })
        else if (role.includes('ADMIN')) navigate('/dashboard/admin',     { replace: true })
        else                             navigate('/',                     { replace: true })
      })
      .catch(() => {
        toast.error('Failed to complete login. Please try again.', { id: 'oauth-fail' })
        navigate('/login', { replace: true })
      })
  }, [dispatch, navigate])

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-950">
      <div className="text-center space-y-4">
        <PageSpinner />
        <p className="text-sm text-slate-400">Completing sign in…</p>
      </div>
    </div>
  )
}
