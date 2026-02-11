import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchDashboardEnvironments, scanEnvironmentTomcats, type DashboardEnvironment } from '../lib/hivewatchApi'
import { Link } from 'react-router-dom'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; environments: DashboardEnvironment[] }
  | { kind: 'error'; message: string }

export function DashboardPage() {
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [scanningEnvId, setScanningEnvId] = useState<string | null>(null)

  const refresh = useCallback((signal?: AbortSignal) => {
    return fetchDashboardEnvironments(signal)
      .then((environments) => setState({ kind: 'ready', environments }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Dashboard · ${state.environments.length} environments`
    return 'Dashboard'
  }, [state])

  const formatTs = (iso: string | null) => {
    if (!iso) return '—'
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso
    return date.toLocaleString()
  }

  const pill = (env: DashboardEnvironment) => {
    switch (env.tomcatStatus) {
      case 'OK':
        return (
          <div className="pill" data-kind="ok">
            OK
          </div>
        )
      case 'BLOCK':
        return (
          <div className="pill" data-kind="alert">
            BLOCK
          </div>
        )
      default:
        return (
          <div className="pill" data-kind="missing">
            UNKNOWN
          </div>
        )
    }
  }

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">Visible environments for the current user. First slice: Tomcat targets → scan → webapps list.</div>

      <div className="card" style={{ marginTop: 12 }}>
        {state.kind === 'loading' ? <div className="muted">Loading…</div> : null}
        {state.kind === 'error' ? <div className="muted">Error: {state.message}</div> : null}
        {state.kind === 'ready' && state.environments.length === 0 ? (
          <div className="muted">No environments yet.</div>
        ) : null}

        {state.kind === 'ready' && state.environments.length > 0 ? (
          <div className="list">
            {state.environments.map((env) => (
              <details key={env.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ fontWeight: 700, minWidth: 0 }}>
                    <Link to={`/environments/${encodeURIComponent(env.id)}`} style={{ textDecoration: 'none' }}>
                      {env.name}
                    </Link>
                  </div>
                  {pill(env)}

                  <button
                    type="button"
                    className="button"
                    style={{ marginLeft: 'auto' }}
                    disabled={scanningEnvId === env.id}
                    onClick={(event) => {
                      event.preventDefault()
                      event.stopPropagation()
                      const controller = new AbortController()
                      setScanningEnvId(env.id)
                      scanEnvironmentTomcats(env.id, controller.signal)
                        .then(() => refresh())
                        .finally(() => setScanningEnvId((current) => (current === env.id ? null : current)))
                    }}
                  >
                    {scanningEnvId === env.id ? 'Scanning…' : 'Scan Tomcats'}
                  </button>
                </summary>
                <div className="kv" style={{ marginTop: 10 }}>
                  <div className="k">tomcatTargets</div>
                  <div className="v">{env.tomcatTargets}</div>
                  <div className="k">webappsTotal</div>
                  <div className="v">{env.tomcatWebappsTotal}</div>
                  <div className="k">lastScan</div>
                  <div className="v">{formatTs(env.tomcatLastScanAt)}</div>
                  <div className="k">ok / error</div>
                  <div className="v">
                    {env.tomcatOk} / {env.tomcatError}
                  </div>
                </div>

                {env.tomcatTargets === 0 ? (
                  <div className="muted" style={{ marginTop: 10 }}>
                    No Tomcat targets yet. Add them in <Link to={`/environments/${encodeURIComponent(env.id)}`}>Environment config</Link>.
                  </div>
                ) : null}
              </details>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  )
}
