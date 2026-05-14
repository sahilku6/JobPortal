export default function PrivacyPolicyPage() {
  return (
    <div className="py-12">
      <div className="page-container max-w-4xl">
        <div className="card relative overflow-hidden p-8 md:p-10 border border-slate-100/80 dark:border-slate-800/80">
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-brand-500 via-cyan-500 to-indigo-500" />
          <p className="text-sm uppercase tracking-[0.2em] text-brand-500 font-semibold mb-3">Legal</p>
          <h2 className="font-display text-display-xl md:text-display-2xl font-bold text-slate-900 dark:text-slate-100 mb-2">Privacy Policy</h2>
          <p className="text-sm text-slate-400 mb-8">Last updated: January 2025</p>

          <div className="prose prose-sm dark:prose-invert max-w-none space-y-8 text-slate-600 dark:text-slate-400">
            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">1. Information We Collect</h2>
              <p className="leading-relaxed">We collect information you provide directly when you create an account, post jobs, or submit applications. This includes your name, email address, phone number, resume, and professional details. We also automatically collect usage data such as pages visited, actions taken, and device information to improve the platform.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">2. How We Use Your Information</h2>
              <p className="leading-relaxed">Your data is used to operate CareerBridge: matching job seekers with recruiters, sending notifications about applications, verifying your identity, and improving our services. We do not sell your personal data to third parties.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">3. Data Sharing</h2>
              <p className="leading-relaxed">When you apply for a job, your resume and application details are shared with the recruiter who posted that job. Your profile information is only visible to recruiters when you choose to apply. We may share data with service providers (email, file storage) who are bound by confidentiality agreements.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">4. Data Security</h2>
              <p className="leading-relaxed">We use industry-standard encryption, JWT-based authentication, and secure storage to protect your data. Passwords are hashed and never stored in plain text. Sessions are managed securely with single-device enforcement available.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">5. Your Rights</h2>
              <p className="leading-relaxed">You have the right to access, correct, or delete your personal data at any time. You can update your profile from the Profile page or contact us to request full account deletion. You may also opt out of non-essential email notifications from your account settings.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">6. Cookies</h2>
              <p className="leading-relaxed">CareerBridge uses browser local storage for session management and user preferences (such as dark mode). We do not use third-party advertising cookies.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">7. Contact</h2>
              <p className="leading-relaxed">For privacy-related questions, email us at <a href="mailto:privacy@careerbridge.com" className="text-brand-500 hover:underline">privacy@careerbridge.com</a>.</p>
            </section>
          </div>
        </div>
      </div>
    </div>
  )
}
