import { useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/authContext'

export function LoginPage() {
  const { state, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const from = (location.state as { from?: string } | null)?.from

  const [username, setUsername] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const suggested = useMemo(() => ['local-admin', 'local-operator', 'local-viewer'], [])

  return (
    <div className="page">
      <h1 className="h1">Login</h1>
      <div className="muted">Dev auth mode: backend requires <code>X-HW-Username</code>. Pick a seeded user or type your own.</div>

      {state.kind === 'ready' ? (
        <div className="card" style={{ marginTop: 12, maxWidth: 520, padding: 12 }}>
          <div className="kv">
            <div className="k">Logged in</div>
            <div className="v">
              {state.me.displayName} <span className="muted">@{state.me.username}</span>
            </div>
          </div>
        </div>
      ) : null}

      <div className="card" style={{ marginTop: 12, maxWidth: 520, padding: 12 }}>
        <div className="h2">Sign in</div>
        <div className="muted" style={{ marginTop: 6 }}>
          Suggested users: {suggested.map((u) => (<span key={u} style={{ marginRight: 8 }}><code>{u}</code></span>))}
        </div>

        {error ? (
          <div className="muted" style={{ marginTop: 10 }}>
            Error: {error}
          </div>
        ) : null}

        <form
          style={{ marginTop: 10, display: 'flex', gap: 10, alignItems: 'end' }}
          onSubmit={(e) => {
            e.preventDefault()
            setSaving(true)
            setError(null)
            login(username)
              .then(() => navigate(from || '/dashboard', { replace: true }))
              .catch((e2) => setError(e2 instanceof Error ? e2.message : 'Login failed'))
              .finally(() => setSaving(false))
          }}
        >
          <label className="field" style={{ flex: 1 }}>
            <div className="fieldLabel">Username</div>
            <input
              className="fieldInput"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="local-operator"
              required
            />
          </label>
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div style={{ marginTop: 10, display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {suggested.map((u) => (
            <button
              key={u}
              type="button"
              className="button"
              onClick={() => {
                setUsername(u)
              }}
            >
              Use {u}
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
