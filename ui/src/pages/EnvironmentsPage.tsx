import { useCallback, useEffect, useState } from 'react'
import { createAdminEnvironment, fetchEnvironments, type EnvironmentSummary } from '../lib/hivewatchApi'
import { Link } from 'react-router-dom'
import { useAuth } from '../lib/authContext'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; environments: EnvironmentSummary[] }
  | { kind: 'error'; message: string }

export function EnvironmentsPage() {
  const { state: auth } = useAuth()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [createName, setCreateName] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)

  const refresh = useCallback((signal?: AbortSignal) => {
    setState({ kind: 'loading' })
    return fetchEnvironments(signal)
      .then((environments) => setState({ kind: 'ready', environments }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const isAdmin = auth.kind === 'ready' && auth.me.roles.includes('ADMIN')

  const onCreate = useCallback(() => {
    const name = createName.trim()
    if (!name) {
      setCreateError('Name is required')
      return
    }
    setCreateError(null)
    setCreating(true)
    const controller = new AbortController()
    createAdminEnvironment({ name }, controller.signal)
      .then(() => {
        setCreateName('')
        return refresh(controller.signal)
      })
      .catch((e) => setCreateError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setCreating(false))
  }, [createName, refresh])

  return (
    <div className="page">
      <h1 className="h1">Environment config</h1>
      <div className="muted">Topology editor will live here (Environment → Groups → Subgroups → Endpoints).</div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="h2">Environments</div>
        {isAdmin ? (
          <div style={{ display: 'flex', gap: 8, marginTop: 10, alignItems: 'center' }}>
            <input
              className="input"
              style={{ maxWidth: 420 }}
              placeholder="New environment name…"
              value={createName}
              onChange={(e) => setCreateName(e.target.value)}
              aria-label="New environment name"
            />
            <button type="button" className="button" onClick={onCreate} disabled={creating}>
              {creating ? 'Creating…' : 'Create'}
            </button>
            {createError ? <div className="muted">Error: {createError}</div> : null}
          </div>
        ) : null}
        {state.kind === 'loading' ? <div className="muted">Loading…</div> : null}
        {state.kind === 'error' ? <div className="muted">Error: {state.message}</div> : null}
        {state.kind === 'ready' && state.environments.length === 0 ? <div className="muted">No environments.</div> : null}
        {state.kind === 'ready' && state.environments.length > 0 ? (
          <ul className="list">
            {state.environments.map((env) => (
              <li key={env.id}>
                <Link to={`/environments/${encodeURIComponent(env.id)}`}>{env.name}</Link>
              </li>
            ))}
          </ul>
        ) : null}
      </div>
    </div>
  )
}
