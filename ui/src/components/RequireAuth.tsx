import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/authContext'

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { state } = useAuth()
  const location = useLocation()

  if (state.kind === 'anonymous') {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  if (state.kind === 'loading') {
    return (
      <div className="page">
        <h1 className="h1">Loading…</h1>
        <div className="muted">Checking authentication.</div>
      </div>
    )
  }
  if (state.kind === 'error') {
    return (
      <div className="page">
        <h1 className="h1">Auth error</h1>
        <div className="muted">{state.message}</div>
        <div className="muted" style={{ marginTop: 8 }}>
          Go to <a href="/login">Login</a>.
        </div>
      </div>
    )
  }
  return <>{children}</>
}

