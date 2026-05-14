export default function TermsOfServicePage() {
  return (
    <div className="py-12">
      <div className="page-container max-w-4xl">
        <div className="card relative overflow-hidden p-8 md:p-10 border border-slate-100/80 dark:border-slate-800/80">
          <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-brand-500 via-cyan-500 to-indigo-500" />
          <p className="text-sm uppercase tracking-[0.2em] text-brand-500 font-semibold mb-3">Legal</p>
          <h2 className="font-display text-display-xl md:text-display-2xl font-bold text-slate-900 dark:text-slate-100 mb-2">Terms of Service</h2>
          <p className="text-sm text-slate-400 mb-8">Last updated: January 2025</p>

          <div className="prose prose-sm dark:prose-invert max-w-none space-y-8 text-slate-600 dark:text-slate-400">
            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">1. Acceptance of Terms</h2>
              <p className="leading-relaxed">By creating an account or using CareerBridge, you agree to these Terms of Service. If you do not agree, please do not use the platform. We may update these terms; continued use after changes constitutes acceptance.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">2. Eligibility</h2>
              <p className="leading-relaxed">You must be at least 18 years old to use CareerBridge. By registering, you confirm that the information you provide is accurate and complete. You are responsible for keeping your account credentials secure.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">3. Account Responsibilities</h2>
              <p className="leading-relaxed">You are responsible for all activity under your account. Do not share your credentials. If you believe your account has been compromised, contact us immediately. We reserve the right to suspend or terminate accounts that violate these terms.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">4. Job Postings and Applications</h2>
              <p className="leading-relaxed">Recruiters are responsible for the accuracy of job listings. Job seekers are responsible for the accuracy of their applications and resumes. CareerBridge does not guarantee job placement or hiring outcomes. Posting fraudulent jobs or submitting false applications is prohibited.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">5. Prohibited Conduct</h2>
              <p className="leading-relaxed">You may not use CareerBridge to: post illegal or discriminatory job listings, harass other users, attempt to access other users' accounts, scrape or automate requests, or violate any applicable laws. Violations may result in permanent account suspension.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">6. Intellectual Property</h2>
              <p className="leading-relaxed">The CareerBridge platform, logo, and design are our intellectual property. You retain ownership of the content you submit (resumes, job descriptions) but grant us a license to display and process that content to operate the service.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">7. Limitation of Liability</h2>
              <p className="leading-relaxed">CareerBridge is provided "as is." We are not liable for any indirect, incidental, or consequential damages arising from your use of the platform. Our total liability for any claim is limited to the amount you paid us in the 12 months prior to the claim.</p>
            </section>

            <section>
              <h2 className="text-lg font-semibold text-slate-800 dark:text-slate-200 mb-3">8. Contact</h2>
              <p className="leading-relaxed">Questions about these terms? Email <a href="mailto:legal@careerbridge.com" className="text-brand-500 hover:underline">legal@careerbridge.com</a>.</p>
            </section>
          </div>
        </div>
      </div>
    </div>
  )
}
