import { Link } from 'react-router-dom'
import { Briefcase, Users, ShieldCheck } from 'lucide-react'

export default function AboutUsPage() {
  return (
    <div className="py-12">
      <div className="page-container max-w-5xl">
        <div className="card relative overflow-hidden p-8 md:p-10 border border-slate-100/80 dark:border-slate-800/80">
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-brand-500 via-cyan-500 to-indigo-500" />
          <div className="absolute -right-10 -top-10 h-28 w-28 rounded-full bg-brand-500/10 blur-2xl opacity-70" />
          <p className="text-sm uppercase tracking-[0.2em] text-brand-500 font-semibold mb-3">About us</p>
          <h2 className="font-display text-display-xl md:text-display-2xl font-bold text-slate-900 dark:text-slate-100 mb-4">Connecting talent with opportunity</h2>
          <p className="text-slate-500 max-w-3xl leading-relaxed mb-8">
            CareerBridge helps job seekers, recruiters, and administrators manage hiring in one place with a modern, reliable experience.
          </p>

          <div className="grid md:grid-cols-3 gap-4 mb-8">
            <div className="card p-5 border border-slate-100/80 dark:border-slate-800/80">
              <Briefcase className="w-5 h-5 text-brand-500 mb-3" />
              <h3 className="max-w-[11rem] text-lg sm:text-xl font-semibold leading-tight mb-2">Job discovery</h3>
              <p className="text-sm text-slate-500">Find and manage opportunities in a focused workflow.</p>
            </div>
            <div className="card p-5 border border-slate-100/80 dark:border-slate-800/80">
              <Users className="w-5 h-5 text-brand-500 mb-3" />
              <h3 className="max-w-[11rem] text-lg sm:text-xl font-semibold leading-tight mb-2">Simple collaboration</h3>
              <p className="text-sm text-slate-500">Keep hiring teams and candidates aligned.</p>
            </div>
            <div className="card p-5 border border-slate-100/80 dark:border-slate-800/80">
              <ShieldCheck className="w-5 h-5 text-brand-500 mb-3" />
              <h3 className="max-w-[11rem] text-lg sm:text-xl font-semibold leading-tight mb-2">Secure accounts</h3>
              <p className="text-sm text-slate-500">Role-based access with clear account controls.</p>
            </div>
          </div>

          <Link to="/register" className="btn-primary inline-flex">Create Account</Link>
        </div>
      </div>
    </div>
  )
}