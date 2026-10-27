// Server LocalDateTime fields represent UTC. Display consistently in Asia/Shanghai.
export function formatShanghai(value) {
  if (!value) return ''
  let date
  if (Array.isArray(value) && value.length >= 3) {
    date = new Date(Date.UTC(value[0], value[1]-1, value[2], value[3] || 0, value[4] || 0, value[5] || 0))
  } else if (typeof value === 'string') {
    const normalized = value.replace(' ', 'T')
    date = new Date(/[zZ]$|[+-]\d\d:\d\d$/.test(normalized) ? normalized : normalized + 'Z')
  } else date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const parts = new Intl.DateTimeFormat('en-CA', {timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).formatToParts(date)
  const part = key => parts.find(item => item.type === key).value
  return `${part('year')}-${part('month')}-${part('day')} ${part('hour')}:${part('minute')}`
}
