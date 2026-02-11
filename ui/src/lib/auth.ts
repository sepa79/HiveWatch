const DEV_USERNAME_KEY = 'HW_DEV_USERNAME'

export function getDevUsername(): string | null {
  try {
    const raw = window.localStorage.getItem(DEV_USERNAME_KEY)
    if (raw && raw.trim()) return raw.trim()
  } catch {
    // ignore
  }
  return null
}

export function setDevUsername(username: string) {
  const trimmed = username.trim()
  if (!trimmed) throw new Error('Username is required')
  try {
    window.localStorage.setItem(DEV_USERNAME_KEY, trimmed)
  } catch {
    // ignore
  }
}

export function clearDevUsername() {
  try {
    window.localStorage.removeItem(DEV_USERNAME_KEY)
  } catch {
    // ignore
  }
}

export function withDevAuthHeaders(headers?: HeadersInit): HeadersInit {
  const username = getDevUsername()
  if (!username) return headers ?? {}
  return {
    ...(headers ?? {}),
    'X-HW-Username': username,
  }
}

