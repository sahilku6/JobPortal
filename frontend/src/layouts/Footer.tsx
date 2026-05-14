import { Link } from 'react-router-dom'
import { Github, Twitter, Linkedin } from 'lucide-react'
import { useAppSelector } from '../shared/hooks/redux'
import { Logo } from '../shared/components/brand/Logo'

const SOCIALS = [
  { Icon: Github,   href: 'https://github.com',   label: 'GitHub'   },
  { Icon: Twitter,  href: 'https://twitter.com',  label: 'Twitter'  },
  { Icon: Linkedin, href: 'https://linkedin.com', label: 'LinkedIn' },
]

export default function Footer() {
  const { user } = useAppSelector((state) => state.auth)
  const role = user?.role?.toUpperCase() ?? ''
  const isJobSeeker = role === 'JOB_SEEKER' || role === 'ROLE_JOB_SEEKER'
  const isRecruiter = role === 'RECRUITER'   || role === 'ROLE_RECRUITER'

  return (
    <footer className="relative overflow-hidden bg-slate-950 text-slate-400 mt-auto border-t border-white/10">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(124,58,237,0.16),transparent_28%),radial-gradient(circle_at_top_right,rgba(6,182,212,0.12),transparent_26%)]" />
      <div className="page-container py-12 relative">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8">

          {/* Brand */}
          <div className="col-span-1 md:col-span-2">
            <Link to="/" className="flex items-center gap-2.5 mb-3">
              <img src={Logo} alt="CareerBridge" className="h-12 w-auto" />
            </Link>
            <p className="text-sm text-slate-500 max-w-xs leading-relaxed">
              Connecting talent with opportunity. Build a faster, clearer hiring experience for modern teams.
            </p>
            <div className="flex items-center gap-3 mt-4">
              {SOCIALS.map(({ Icon, href, label }) => (
                <a
                  key={label}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  title={label}
                  aria-label={label}
                  className="p-2 rounded-lg bg-white/5 hover:bg-violet-500/20 text-slate-300 hover:text-white border border-white/10 transition-colors"
                >
                  <Icon className="w-4 h-4" />
                </a>
              ))}
            </div>
          </div>

          {/* Role-based links */}
          {isJobSeeker ? (
            <div>
              <h4 className="text-sm font-semibold text-slate-300 mb-3">My Account</h4>
              <ul className="space-y-2 text-sm">
                <li><Link to="/jobs"         className="hover:text-white transition-colors">Browse Jobs</Link></li>
                <li><Link to="/applications" className="hover:text-white transition-colors">Applications</Link></li>
                <li><Link to="/profile"      className="hover:text-white transition-colors">My Profile</Link></li>
                <li><Link to="/dashboard/jobseeker" className="hover:text-white transition-colors">Dashboard</Link></li>
              </ul>
            </div>
          ) : isRecruiter ? (
            <div>
              <h4 className="text-sm font-semibold text-slate-300 mb-3">Recruitment</h4>
              <ul className="space-y-2 text-sm">
                <li><Link to="/recruiter/post-job"     className="hover:text-white transition-colors">Create Job Posting</Link></li>
                <li><Link to="/recruiter/jobs"         className="hover:text-white transition-colors">Job Postings</Link></li>
                <li><Link to="/recruiter/applications" className="hover:text-white transition-colors">Applications</Link></li>
                <li><Link to="/dashboard/recruiter"    className="hover:text-white transition-colors">Dashboard</Link></li>
              </ul>
            </div>
          ) : (
            <div>
                <h4 className="text-sm font-semibold text-slate-300 mb-3">Quick Links</h4>
              <ul className="space-y-2 text-sm">
                <li><Link to="/jobs"     className="hover:text-white transition-colors">Browse Jobs</Link></li>
                <li><Link to="/register" className="hover:text-white transition-colors">Create Account</Link></li>
                <li><Link to="/about"    className="hover:text-white transition-colors">About Us</Link></li>
              </ul>
            </div>
          )}

          {/* Company */}
          <div>
            <h4 className="text-sm font-semibold text-slate-300 mb-3">Company</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/about"   className="hover:text-white transition-colors">About Us</Link></li>
              <li><Link to="/contact" className="hover:text-white transition-colors">Contact Us</Link></li>
              <li><Link to="/privacy" className="hover:text-white transition-colors">Privacy Policy</Link></li>
              <li><Link to="/terms"   className="hover:text-white transition-colors">Terms of Service</Link></li>
            </ul>
          </div>
        </div>

        <div className="border-t border-slate-800 pt-6 flex flex-col sm:flex-row items-center justify-between gap-3 text-sm">
          <p>© {new Date().getFullYear()} CareerBridge. All rights reserved.</p>
          <div className="flex items-center gap-4">
            <Link to="/privacy" className="hover:text-white transition-colors">Privacy Policy</Link>
            <Link to="/terms"   className="hover:text-white transition-colors">Terms of Service</Link>
          </div>
        </div>
      </div>
    </footer>
  )
}
