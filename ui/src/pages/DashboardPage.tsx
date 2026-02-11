import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  fetchDashboardEnvironments,
  fetchTomcatTargets,
  scanEnvironmentTomcats,
  type DashboardEnvironment,
  type TomcatTarget,
} from '../lib/hivewatchApi'
import { Link } from 'react-router-dom'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; environments: DashboardEnvironment[] }
  | { kind: 'error'; message: string }

type EnvTargetsState =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'ready'; targets: TomcatTarget[] }
  | { kind: 'error'; message: string }

export function DashboardPage() {
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [scanningEnvId, setScanningEnvId] = useState<string | null>(null)
  const [envTargets, setEnvTargets] = useState<Record<string, EnvTargetsState>>({})

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

  const getEnvTargetsState = (envId: string): EnvTargetsState => envTargets[envId] ?? { kind: 'idle' }

  const loadEnvTargets = (envId: string) => {
    if (getEnvTargetsState(envId).kind === 'loading' || getEnvTargetsState(envId).kind === 'ready') return
    const controller = new AbortController()
    setEnvTargets((prev) => ({ ...prev, [envId]: { kind: 'loading' } }))
    fetchTomcatTargets(envId, controller.signal)
      .then((targets) => setEnvTargets((prev) => ({ ...prev, [envId]: { kind: 'ready', targets } })))
      .catch((e) =>
        setEnvTargets((prev) => ({
          ...prev,
          [envId]: { kind: 'error', message: e instanceof Error ? e.message : 'Request failed' },
        })),
      )
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
              <details
                key={env.id}
                className="card"
                style={{ marginTop: 10, padding: 12 }}
                onToggle={(e) => {
                  const el = e.currentTarget
                  if (el.open) loadEnvTargets(env.id)
                }}
              >
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

                {env.tomcatTargets > 0 ? (
                  <div style={{ marginTop: 10 }}>
                    {(() => {
                      const tstate = getEnvTargetsState(env.id)
                      if (tstate.kind === 'idle') {
                        return <div className="muted">Expand to load topology…</div>
                      }
                      if (tstate.kind === 'loading') {
                        return <div className="muted">Loading targets…</div>
                      }
                      if (tstate.kind === 'error') {
                        return <div className="muted">Error: {tstate.message}</div>
                      }
                      if (tstate.kind !== 'ready') {
                        return null
                      }
                      return (
                        <div className="kv">
                          {tstate.targets
                            .slice()
                            .sort((a, b) => (a.serverName + a.role).localeCompare(b.serverName + b.role))
                            .map((t) => (
                              <div key={t.id} style={{ display: 'contents' }}>
                                <div className="k">
                                  {t.serverName} · {t.role.toLowerCase()}
                                </div>
                                <div className="v">
                                  {t.state ? (
                                    t.state.outcomeKind === 'SUCCESS' ? (
                                      <span>
                                        OK · {t.state.webapps.length} webapps
                                      </span>
                                    ) : (
                                      <span>
                                        ERROR · {t.state.errorKind}: {t.state.errorMessage}
                                      </span>
                                    )
                                  ) : (
                                    <span className="muted">Not scanned yet</span>
                                  )}
                                </div>
                              </div>
                            ))}
                        </div>
                      )
                    })()}
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
