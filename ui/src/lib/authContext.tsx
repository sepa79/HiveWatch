import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { clearDevUsername, getDevUsername, setDevUsername } from './auth'
import { fetchMe, type UserSummary } from './hivewatchApi'

type AuthState =
  | { kind: 'anonymous' }
  | { kind: 'loading' }
  | { kind: 'ready'; me: UserSummary }
  | { kind: 'error'; message: string }

type AuthContextValue = {
  state: AuthState
  login: (username: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({ kind: 'loading' })

  const load = useCallback((signal?: AbortSignal) => {
    const username = getDevUsername()
    if (!username) {
      setState({ kind: 'anonymous' })
      return Promise.resolve()
    }
    setState({ kind: 'loading' })
    return fetchMe(signal)
      .then((me) => setState({ kind: 'ready', me }))
      .catch((e) => {
        setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' })
      })
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
    return () => controller.abort()
  }, [load])

  const login = useCallback(async (username: string) => {
    setDevUsername(username)
    await load()
  }, [load])

  const logout = useCallback(() => {
    clearDevUsername()
    setState({ kind: 'anonymous' })
  }, [])

  const value = useMemo<AuthContextValue>(() => ({ state, login, logout }), [state, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('AuthProvider is missing')
  }
  return ctx
}

