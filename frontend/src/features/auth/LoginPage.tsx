import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAppDispatch, useAppSelector } from '../../shared/hooks/redux'
import { loginThunk } from '../../store/slices/authSlice'
import { Button, Input, Spinner } from '../../shared/components/ui'
import { Eye, EyeOff, Github } from 'lucide-react'
import { cn } from '../../shared/utils/helpers'
import toast from 'react-hot-toast'
import { Logo } from '../../shared/components/brand/Logo'

const API_BASE = ((import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')

const schema = z.object({
  email:    z.string().trim().min(1, 'Email is required').email('Please enter a valid email address'),
  password: z.string().min(1, 'Password is required').max(128, 'Password is too long'),
})
type FormData = z.infer<typeof schema>

function GoogleIcon() {
  return (
    <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none">
      <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
      <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
      <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
      <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
    </svg>
  )
}

export default function LoginPage() {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const location = useLocation()
  const { loading } = useAppSelector((s) => s.auth)
  const [showPass,    setShowPass]    = useState(false)
  const [loginError,  setLoginError]  = useState<string | null>(null)
  const [successMsg,  setSuccessMsg]  = useState<string | null>(
    (location.state as { registrationSuccessMessage?: string })?.registrationSuccessMessage ?? null,
  )

  useEffect(() => {
    if (!successMsg) return undefined
    const t = window.setTimeout(() => setSuccessMsg(null), 5000)
    return () => window.clearTimeout(t)
  }, [successMsg])

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: FormData) => {
    setLoginError(null)
    const res = await dispatch(loginThunk(data))
    if (loginThunk.fulfilled.match(res)) {
      navigate('/', { replace: true })
      return
    }
    if (loginThunk.rejected.match(res)) {
      const msg = (res.payload as string) ?? 'Wrong username or password'
      if (msg === 'User not found') {
        setLoginError('User not found. Redirecting to register page...')
        window.setTimeout(() => navigate(`/register?email=${encodeURIComponent(data.email)}`, { replace: true }), 1200)
        return
      }
      setLoginError(null)
      toast.error(msg, { id: 'login-error' })
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left decorative panel */}
      <div className="hidden lg:flex flex-col w-1/2 bg-gradient-to-br from-slate-950 via-violet-950 to-cyan-950 p-12 relative overflow-hidden">
        <div className="absolute inset-0 opacity-40" />
        <div className="absolute top-16 left-10 w-40 h-40 rounded-full bg-cyan-500/15 blur-3xl" />
        <div className="absolute bottom-10 right-16 w-56 h-56 rounded-full bg-fuchsia-500/15 blur-3xl" />
        <div className="relative">
          <Link to="/" className="flex items-center gap-2.5 mb-3">
            <img src={Logo} alt="CareerBridge" className="h-10 w-auto" />
          </Link>
        </div>
        <div className="relative flex-1 flex items-center">
          <div className="max-w-lg">
            <blockquote className="font-display text-3xl font-medium text-white leading-snug mb-6">
              "Your path to career success starts here."
            </blockquote>
            <div className="grid grid-cols-3 gap-4">
              {[['10K+', 'Active Jobs'], ['50K+', 'Candidates'], ['5K+', 'Companies']].map(([n, l], idx) => (
                <div key={l} className={cn('rounded-2xl p-4 border border-white/10 backdrop-blur',
                  idx === 0 && 'bg-white/8', idx === 1 && 'bg-cyan-500/10', idx === 2 && 'bg-fuchsia-500/10')}>
                  <p className="text-2xl font-bold text-brand-300">{n}</p>
                  <p className="text-sm text-slate-400">{l}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-6 lg:p-12 bg-gradient-to-br from-slate-50 via-white to-violet-50 dark:from-slate-950 dark:via-slate-950 dark:to-slate-900">
        <div className="w-full max-w-md">
          <div className="mb-8">
            <h2 className="font-display text-2xl font-bold text-slate-900 dark:text-slate-100 mb-2">Welcome back</h2>
            <p className="text-slate-500">Access your candidate or hiring workspace.</p>
          </div>

          {/* OAuth buttons */}
          <div className="flex gap-3 mb-5">
            <a
              href={`${API_BASE}/oauth2/authorization/google`}
              className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-white/5 dark:hover:bg-white/10 border border-slate-200 dark:border-white/10 text-sm text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white transition-colors"
            >
              <GoogleIcon />Continue with Google
            </a>
            <a
              href={`${API_BASE}/oauth2/authorization/github`}
              className="flex-1 flex items-center justify-center gap-2 px-3 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-white/5 dark:hover:bg-white/10 border border-slate-200 dark:border-white/10 text-sm text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white transition-colors"
            >
              <Github className="w-4 h-4" />Continue with GitHub
            </a>
          </div>

          <div className="flex items-center gap-3 mb-5">
            <div className="flex-1 h-px bg-slate-200 dark:bg-white/10" />
            <span className="text-xs text-slate-400">or sign in with email</span>
            <div className="flex-1 h-px bg-slate-200 dark:bg-white/10" />
          </div>

          {/* Alerts */}
          {successMsg && (
            <div className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 dark:bg-emerald-500/10 dark:border-emerald-500/20 dark:text-emerald-400" role="status">
              {successMsg}
            </div>
          )}
          {loginError && (
            <div className="mb-4 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 dark:bg-red-500/10 dark:border-red-500/20 dark:text-red-400" role="alert">
              {loginError}
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            <Input
              label="Email address"
              type="email"
              placeholder="you@example.com"
              error={errors.email?.message}
              hint="Use the email address associated with your account"
              {...register('email')}
            />

            {/* Password with forgot-password link */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300">
                  Password
                </label>
                {/* ── PHASE 4: Forgot password link ── */}
                <Link
                  to="/forgot-password"
                  className="text-xs text-brand-600 dark:text-brand-400 hover:underline font-medium"
                >
                  Reset password
                </Link>
              </div>
              <Input
                type={showPass ? 'text' : 'password'}
                placeholder="Password"
                error={errors.password?.message}
                hint="Password is case-sensitive and must match your account"
                rightIcon={
                  <button
                    type="button"
                    onClick={() => setShowPass((v) => !v)}
                    className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                    aria-label={showPass ? 'Hide password' : 'Show password'}
                  >
                    {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                }
                {...register('password')}
              />
            </div>

            <Button type="submit" disabled={loading} className="w-full shadow-lg shadow-violet-500/15" size="lg">
              {loading ? <><Spinner size="sm" /> Signing in...</> : 'Sign in securely'}
            </Button>
          </form>

          <p className="mt-6 text-center text-sm text-slate-500">
            Don't have an account?{' '}
            <Link to="/register" className="text-brand-600 dark:text-brand-400 font-medium hover:underline">
              Create your account
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
