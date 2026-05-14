import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowLeft, CheckCircle2 } from 'lucide-react'
import { Button, Input, Spinner } from '../../shared/components/ui'
import { authApi } from '../../core/api/services/auth'
import { cn } from '../../shared/utils/helpers'
import { Logo } from '../../shared/components/brand/Logo'

const emailSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').email('Enter a valid email address'),
})
const otpSchema = z.object({
  otp: z.string().regex(/^\d{6}$/, 'Enter the 6-digit code from your email'),
})

const forgotPasswordStrengthSchema = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .refine((value) => /[A-Z]/.test(value), { message: 'Password must include at least one uppercase letter' })
  .refine((value) => /[a-z]/.test(value), { message: 'Password must include at least one lowercase letter' })
  .refine((value) => /[0-9]/.test(value), { message: 'Password must include at least one number' })

const passwordSchema = z.object({
  newPassword:     forgotPasswordStrengthSchema,
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((d) => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})

type EmailForm = z.infer<typeof emailSchema>
type OtpForm   = z.infer<typeof otpSchema>
type PwForm    = z.infer<typeof passwordSchema>

const STEPS = [
  { n: 1, label: 'Email' },
  { n: 2, label: 'Verify' },
  { n: 3, label: 'Password' },
]

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [step,    setStep]    = useState<1 | 2 | 3 | 4>(1)
  const [email,   setEmail]   = useState('')
  const [otp,     setOtp]     = useState('')
  const [loading, setLoading] = useState(false)
  const [error,   setError]   = useState<string | null>(null)

  const ef = useForm<EmailForm>({ resolver: zodResolver(emailSchema) })
  const of = useForm<OtpForm>  ({ resolver: zodResolver(otpSchema) })
  const pf = useForm<PwForm>   ({ resolver: zodResolver(passwordSchema) })

  const onEmailSubmit = async ({ email: e }: EmailForm) => {
    setLoading(true); setError(null)
    try {
      await authApi.forgotPasswordRequest(e)
      setEmail(e)
      setStep(2)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Something went wrong. Please try again.')
    } finally { setLoading(false) }
  }

  const onOtpSubmit = async ({ otp: o }: OtpForm) => {
    setLoading(true); setError(null)
    try {
      await authApi.forgotPasswordVerify(email, o)
      setOtp(o)
      setStep(3)
    } catch { setError('Invalid or expired OTP. Please check your email and try again.') }
    finally { setLoading(false) }
  }

  const onPwSubmit = async ({ newPassword }: PwForm) => {
    setLoading(true); setError(null)
    try {
      await authApi.forgotPasswordReset(email, otp, newPassword)
      setStep(4)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      setError(msg ?? 'Failed to reset password. Please start over.')
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-50 via-white to-violet-50 dark:from-slate-950 dark:via-slate-950 dark:to-slate-900 px-4">
      <div className="w-full max-w-md">

        {/* Logo */}
        <Link to="/" className="flex items-center justify-center gap-2.5 mb-8">
          <img src={Logo} alt="CareerBridge" className="h-10 w-auto" />
        </Link>

        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-white/10 shadow-xl p-8">

          {/* Header + stepper (shown for steps 1–3) */}
          {step < 4 && (
            <>
              <h2 className="text-xl font-display font-bold text-slate-900 dark:text-white mb-1">
                Forgot Password
              </h2>
              <p className="text-sm text-slate-500 mb-6">
                {step === 1 && "Enter your email and we'll send you a reset code."}
                {step === 2 && `We sent a 6-digit code to ${email}.`}
                {step === 3 && 'Almost done. Choose a new password.'}
              </p>

              {/* Progress stepper */}
              <div className="flex items-center mb-8">
                {STEPS.map((s, i) => (
                  <div key={s.n} className="flex items-center flex-1">
                    <div className="flex items-center gap-2">
                      <div className={cn(
                        'w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 transition-colors',
                        step > s.n  ? 'bg-emerald-500 text-white'
                          : step === s.n ? 'bg-brand-500 text-white'
                          : 'bg-slate-100 dark:bg-white/10 text-slate-400 dark:text-slate-500',
                      )}>
                        {step > s.n ? <CheckCircle2 className="w-4 h-4" /> : s.n}
                      </div>
                      <span className={cn(
                        'text-xs font-medium hidden sm:block',
                        step === s.n ? 'text-slate-900 dark:text-white' : 'text-slate-400',
                      )}>
                        {s.label}
                      </span>
                    </div>
                    {i < STEPS.length - 1 && (
                      <div className={cn(
                        'flex-1 h-px mx-2',
                        step > s.n ? 'bg-emerald-400' : 'bg-slate-200 dark:bg-white/10',
                      )} />
                    )}
                  </div>
                ))}
              </div>
            </>
          )}

          {/* Error alert */}
          {error && (
            <div className="mb-4 rounded-xl border border-red-200 bg-red-50 dark:bg-red-500/10 dark:border-red-500/20 px-4 py-3 text-sm text-red-700 dark:text-red-400">
              {error}
            </div>
          )}

          {/* ── Step 1: Email ── */}
          {step === 1 && (
            <form onSubmit={ef.handleSubmit(onEmailSubmit)} className="space-y-4">
              <Input
                label="Email address"
                type="email"
                placeholder="you@example.com"
                error={ef.formState.errors.email?.message}
                hint="Enter the email address linked to your account"
                {...ef.register('email')}
              />
              <Button type="submit" disabled={loading} className="w-full" size="lg">
                {loading ? <><Spinner size="sm" /> Sending...</> : 'Send Reset Code'}
              </Button>
            </form>
          )}

          {/* ── Step 2: OTP ── */}
          {step === 2 && (
            <form onSubmit={of.handleSubmit(onOtpSubmit)} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1.5">
                  Verification Code
                </label>
                <input
                  type="text"
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="000000"
                  autoComplete="one-time-code"
                  className={cn(
                    'w-full rounded-xl border bg-white dark:bg-slate-800 px-4 py-3 text-center text-2xl tracking-[0.5em] font-mono transition-colors outline-none',
                    'border-slate-300 dark:border-white/10 focus:border-brand-500 dark:focus:border-brand-400',
                    of.formState.errors.otp && 'border-red-400 dark:border-red-500',
                  )}
                  {...of.register('otp')}
                />
                {of.formState.errors.otp && (
                  <p className="mt-1 text-xs text-red-500">{of.formState.errors.otp.message}</p>
                )}
                <p className="mt-1 text-xs text-slate-400">Check your inbox (and spam folder)</p>
              </div>
              <Button type="submit" disabled={loading} className="w-full" size="lg">
                {loading ? <><Spinner size="sm" /> Verifying...</> : 'Verify Code'}
              </Button>
              <button
                type="button"
                onClick={() => { setStep(1); setError(null); of.reset() }}
                className="w-full text-center text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors"
              >
                Use a different email
              </button>
            </form>
          )}

          {/* ── Step 3: New password ── */}
          {step === 3 && (
            <form onSubmit={pf.handleSubmit(onPwSubmit)} className="space-y-4">
              <Input
                label="New Password"
                type="password"
                placeholder="Minimum 8 characters"
                error={pf.formState.errors.newPassword?.message}
                hint="Use at least 8 characters with uppercase, lowercase, and a number"
                {...pf.register('newPassword')}
              />
              <Input
                label="Confirm Password"
                type="password"
                placeholder="Repeat your new password"
                error={pf.formState.errors.confirmPassword?.message}
                {...pf.register('confirmPassword')}
              />
              <Button type="submit" disabled={loading} className="w-full" size="lg">
                {loading ? <><Spinner size="sm" /> Resetting...</> : 'Reset Password'}
              </Button>
            </form>
          )}

          {/* ── Step 4: Success ── */}
          {step === 4 && (
            <div className="text-center space-y-4 py-4">
              <div className="w-16 h-16 rounded-full bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center mx-auto">
                <CheckCircle2 className="w-8 h-8 text-emerald-500" />
              </div>
              <h2 className="text-xl font-display font-bold text-slate-900 dark:text-white">
                Password Reset!
              </h2>
              <p className="text-sm text-slate-500">
                Your password has been updated. Please log in with your new credentials.
              </p>
              <Button onClick={() => navigate('/login')} className="w-full" size="lg">
                Go to Login
              </Button>
            </div>
          )}

          {/* Back to login */}
          {step < 4 && (
            <div className="mt-6 text-center">
              <Link
                to="/login"
                className="inline-flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-300 transition-colors"
              >
                <ArrowLeft className="w-3 h-3" /> Back to login
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
