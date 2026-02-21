export function normalizeScore(raw) {
  if (raw == null) return null
  const n = Number(raw)
  if (Number.isNaN(n)) return null
  return n
}

export function matchLevelFromScore(raw) {
  const score = normalizeScore(raw)
  if (score == null) return 'LOW'
  if (score >= 0.8) return 'HIGH'
  if (score >= 0.6) return 'MEDIUM'
  return 'LOW'
}

export function matchLabelFromLevel(level) {
  const map = { HIGH: '高匹配', MEDIUM: '中匹配', LOW: '低匹配' }
  return map[level] || '低匹配'
}

export function matchTagTypeFromLevel(level) {
  const map = { HIGH: 'success', MEDIUM: 'warning', LOW: 'danger' }
  return map[level] || 'danger'
}

export function matchTagFromScore(raw) {
  const level = matchLevelFromScore(raw)
  return {
    level,
    label: matchLabelFromLevel(level),
    type: matchTagTypeFromLevel(level)
  }
}

export function matchPercentText(raw) {
  const score = normalizeScore(raw)
  if (score == null) return ''
  const pct = Math.round(score * 100)
  return `${pct}%`
}
