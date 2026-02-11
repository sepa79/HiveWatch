import { useEffect, useState } from 'react'
import { fetchEnvironments, type EnvironmentSummary } from '../lib/hivewatchApi'
import { Link } from 'react-router-dom'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; environments: EnvironmentSummary[] }
  | { kind: 'error'; message: string }

export function EnvironmentsPage() {
  const [state, setState] = useState<LoadState>({ kind: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    fetchEnvironments(controller.signal)
      .then((environments) => setState({ kind: 'ready', environments }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    return () => controller.abort()
  }, [])

  return (
    <div className="page">
      <h1 className="h1">Environment config</h1>
      <div className="muted">Topology editor will live here (Environment → Groups → Subgroups → Endpoints).</div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="h2">Environments</div>
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

