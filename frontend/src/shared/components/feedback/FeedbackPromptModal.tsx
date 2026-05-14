import { useState } from 'react'
import { Star, X } from 'lucide-react'
import { notificationsApi } from '../../../core/api/services/notifications'
import { Button, Spinner } from '../ui'
import { cn } from '../../utils/helpers'

interface Props {
  open:        boolean
  onClose:     () => void
  triggerType: 'FIRST_JOB_POST' | 'FIRST_JOB_APPLY'
  userName?:   string
  userRole?:   string
}

export default function FeedbackPromptModal({ open, onClose, triggerType, userName, userRole }: Props) {
  const [rating,      setRating]      = useState(0)
  const [hoverRating, setHoverRating] = useState(0)
  const [reviewText,  setReviewText]  = useState('')
  const [loading,     setLoading]     = useState(false)
  const [submitted,   setSubmitted]   = useState(false)
  const [error,       setError]       = useState<string | null>(null)

  if (!open) return null

  const displayRating = hoverRating || rating

  const handleSubmit = async () => {
    if (rating === 0) { setError('Please select a star rating.'); return }
    if (reviewText.trim().length < 10) { setError('Please write at least 10 characters.'); return }
    setLoading(true); setError(null)
    try {
      await notificationsApi.submitFeedback({
        reviewText: reviewText.trim(),
        rating,
        triggerType,
        userName:  userName  ?? 'Anonymous',
        userRole:  userRole  ?? 'User',
      })
      setSubmitted(true)
    } catch {
      setError('Failed to save your feedback. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-2xl p-6 z-10">
        <button
          type="button"
          onClick={onClose}
          aria-label="Close feedback prompt"
          title="Close feedback prompt"
          className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/5 transition-colors"
        >
          <X className="w-4 h-4" />
        </button>

        {submitted ? (
          <div className="text-center py-4 space-y-3">
            <div className="text-4xl">🎉</div>
            <h3 className="text-lg font-display font-bold text-slate-900 dark:text-white">Thank you!</h3>
            <p className="text-sm text-slate-500">Your feedback helps us improve CareerBridge for everyone.</p>
            <Button onClick={onClose} className="w-full mt-2">Continue</Button>
          </div>
        ) : (
          <>
            <div className="mb-5">
              <h3 className="text-lg font-display font-bold text-slate-900 dark:text-white">
                {triggerType === 'FIRST_JOB_POST' ? '🎊 Congrats on your first job post!' : '✅ Application submitted!'}
              </h3>
              <p className="text-sm text-slate-500 mt-1">How's your experience with CareerBridge so far?</p>
            </div>

            {/* Stars */}
            <div className="flex items-center justify-center gap-1 mb-5">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onMouseEnter={() => setHoverRating(star)}
                  onMouseLeave={() => setHoverRating(0)}
                  onClick={() => { setRating(star); setError(null) }}
                  className="p-1 transition-transform hover:scale-110"
                  aria-label={`${star} star${star !== 1 ? 's' : ''}`}
                >
                  <Star className={cn('w-8 h-8 transition-colors',
                    displayRating >= star ? 'fill-amber-400 text-amber-400' : 'text-slate-300 dark:text-slate-600')} />
                </button>
              ))}
            </div>

            {/* Review text */}
            <textarea
              value={reviewText}
              onChange={(e) => { setReviewText(e.target.value); setError(null) }}
              placeholder="Share your experience… (optional but appreciated!)"
              rows={4}
              maxLength={1000}
              className="w-full rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-slate-800 px-3 py-2.5 text-sm text-slate-900 dark:text-slate-100 placeholder-slate-400 resize-none outline-none focus:border-brand-400 transition-colors mb-1"
            />
            <p className="text-xs text-slate-400 text-right mb-4">{reviewText.length}/1000</p>

            {error && <p className="text-xs text-red-500 dark:text-red-400 mb-3">{error}</p>}

            <div className="flex gap-3">
              <Button variant="ghost" onClick={onClose} className="flex-1">Skip</Button>
              <Button onClick={handleSubmit} disabled={loading} className="flex-1 flex items-center justify-center gap-2">
                {loading && <Spinner size="sm" />}
                Submit Feedback
              </Button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
