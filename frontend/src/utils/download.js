export function parseFilenameFromContentDisposition(value, fallbackName) {
  const fallback = fallbackName || 'download'
  if (!value || typeof value !== 'string') return fallback

  const starMatch = value.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (starMatch && starMatch[1]) {
    try {
      return decodeURIComponent(starMatch[1])
    } catch (e) {
      return starMatch[1]
    }
  }

  const normalMatch = value.match(/filename\s*=\s*"?([^"]+)"?/i)
  if (normalMatch && normalMatch[1]) {
    return normalMatch[1]
  }

  return fallback
}

export function downloadBlobResponse(resp, fallbackName) {
  const blob = resp && resp.data instanceof Blob ? resp.data : new Blob([resp && resp.data])
  const headers = (resp && resp.headers) || {}
  const disposition = headers['content-disposition'] || headers['Content-Disposition']
  const filename = parseFilenameFromContentDisposition(disposition, fallbackName)

  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  window.URL.revokeObjectURL(url)
}

