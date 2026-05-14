const CLOUDINARY_HOST = 'res.cloudinary.com'

function safeParse(url: string): URL | null {
  try {
    return new URL(url)
  } catch {
    return null
  }
}

function isCloudinaryUrl(url: string): boolean {
  const parsed = safeParse(url)
  return !!parsed && parsed.hostname.includes(CLOUDINARY_HOST)
}

function isLikelyPdf(url: string): boolean {
  const path = safeParse(url)?.pathname.toLowerCase() || ''
  return path.endsWith('.pdf')
}

export function getResumeViewUrl(url: string): string {
  // Browsers can render PDFs directly.
  if (isLikelyPdf(url)) return url

  // For DOC/DOCX and other office-like files, use Google Docs viewer.
  return `https://docs.google.com/gview?embedded=1&url=${encodeURIComponent(url)}`
}

export function getResumeDownloadUrl(url: string): string {
  if (!isCloudinaryUrl(url)) return url

  // Force download from Cloudinary while preserving file bytes.
  // Example: /raw/upload/... -> /raw/upload/fl_attachment/... 
  if (url.includes('/upload/fl_attachment/')) return url
  return url.replace('/upload/', '/upload/fl_attachment/')
}
