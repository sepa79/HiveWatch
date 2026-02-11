import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useToolsBar } from '../components/ToolsBarContext'
import { fetchDashboard, type Dashboard, type DashboardCell, type DashboardGroupStatus, type DashboardRowStatus, type DecisionVerdict } from '../lib/hivewatchApi'

function errorIcon(title: string) {
  return (
    <span title={title} style={{ fontWeight: 900 }} aria-label="error">
      !
    </span>
  )
}

function statusPill(status: DashboardRowStatus) {
  switch (status) {
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

function DashboardToolsBar({ active, onRefresh }: { active: 'overview' | 'matrix'; onRefresh: () => void }) {
  const navigate = useNavigate()
  return (
    <div className="toolsRow">
      <div className="toolsToggle" role="tablist" aria-label="Dashboard view">
        <button
          type="button"
          className={`button ${active === 'overview' ? 'toolsToggleActive' : ''}`}
          role="tab"
          aria-selected={active === 'overview'}
          onClick={() => navigate('/dashboard')}
        >
          Overview
        </button>
        <button
          type="button"
          className={`button ${active === 'matrix' ? 'toolsToggleActive' : ''}`}
          role="tab"
          aria-selected={active === 'matrix'}
          onClick={() => navigate('/dashboard/matrix')}
        >
          Matrix
        </button>
      </div>
      <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={onRefresh}>
        Refresh
      </button>
    </div>
  )
}

function groupAsStatus(status: DashboardGroupStatus): 'OK' | 'BLOCK' | 'UNKNOWN' {
  if (status === 'OK') return 'OK'
  if (status === 'BLOCK') return 'BLOCK'
  return 'UNKNOWN'
}

function decisionAsStatus(v: DecisionVerdict): 'OK' | 'WARN' | 'BLOCK' | 'UNKNOWN' {
  if (v === 'OK') return 'OK'
  if (v === 'WARN') return 'WARN'
  if (v === 'BLOCK') return 'BLOCK'
  return 'UNKNOWN'
}

function halEyeState(status: 'OK' | 'WARN' | 'BLOCK' | 'UNKNOWN'): 'ok' | 'warn' | 'alert' | 'missing' {
  if (status === 'OK') return 'ok'
  if (status === 'WARN') return 'warn'
  if (status === 'BLOCK') return 'alert'
  return 'missing'
}

function renderCell(c: DashboardCell) {
  if (c.kind === 'VALUE') return c.text ?? ''
  if (c.kind === 'UNKNOWN') return <span className="muted">—</span>
  return errorIcon(c.title ?? 'Error')
}

export function DashboardOverviewPage() {
  type OverviewState = { kind: 'loading' } | { kind: 'ready'; dashboard: Dashboard } | { kind: 'error'; message: string }
  const [state, setState] = useState<OverviewState>({ kind: 'loading' })

  const refresh = useCallback((signal?: AbortSignal) => {
    return fetchDashboard(signal)
      .then((dashboard) => setState({ kind: 'ready', dashboard }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
  }, [])

  const refreshNow = useCallback(() => {
    refresh()
  }, [refresh])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const tools = useMemo(() => <DashboardToolsBar active="overview" onRefresh={refreshNow} />, [refreshNow])
  useToolsBar(tools)

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Dashboard · ${state.dashboard.environments.length} environments`
    return 'Dashboard'
  }, [state])

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">High-level overview. Detailed matrix available in the view switch above.</div>

      {state.kind === 'loading' ? (
        <div className="card" style={{ marginTop: 12 }}>
          <div className="muted">Loading…</div>
        </div>
      ) : null}
      {state.kind === 'error' ? (
        <div className="card" style={{ marginTop: 12 }}>
          <div className="muted">Error: {state.message}</div>
        </div>
      ) : null}

      {state.kind === 'ready' ? (
        <div className="tableWrap" style={{ marginTop: 12 }}>
          <table className="table">
            <thead>
              <tr>
                <th>Environment</th>
                <th>Servers</th>
                <th>Docker</th>
                <th>AWS</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {state.dashboard.environments.map((env) => {
                const serversStatus = groupAsStatus(env.summary.tomcats.status)
                const dockerStatus = groupAsStatus(env.summary.docker.status)
                const awsStatus = groupAsStatus(env.summary.aws.status)
                const overallStatus = decisionAsStatus(env.summary.verdict)

                const matrixLink = `/dashboard/matrix/${encodeURIComponent(env.id)}`
                const overallTitle = `Decision: ${env.summary.verdict} · BLOCK ${env.summary.blockIssues} · WARN ${env.summary.warnIssues} · UNKNOWN ${env.summary.unknownIssues}`

                return (
                  <tr key={env.id}>
                    <td style={{ fontWeight: 800 }}>
                      <Link to={matrixLink} style={{ textDecoration: 'none' }}>
                        {env.name}
                      </Link>
                    </td>
                    <td>
                      <Link to={matrixLink} className="statusIconLink" aria-label={`${env.name} servers status`}>
                        <span
                          className="hal-eye"
                          data-state={halEyeState(serversStatus)}
                          title={`Tomcats: ${env.summary.tomcats.targets} · last scan: ${env.summary.tomcats.lastScanAt ?? '—'}`}
                          aria-hidden="true"
                        />
                      </Link>
                    </td>
                    <td>
                      <Link to={matrixLink} className="statusIconLink" aria-label={`${env.name} docker status`}>
                        <span
                          className="hal-eye"
                          data-state={halEyeState(dockerStatus)}
                          title={`Microservices: ${env.summary.docker.targets} · last scan: ${env.summary.docker.lastScanAt ?? '—'}`}
                          aria-hidden="true"
                        />
                      </Link>
                    </td>
                    <td>
                      <span className="statusIconLink" aria-label={`${env.name} aws status`}>
                        <span className="hal-eye" data-state={halEyeState(awsStatus)} title="AWS: not configured yet" aria-hidden="true" />
                      </span>
                    </td>
                    <td>
                      <Link to={matrixLink} className="statusIconLink" aria-label={`${env.name} overall status`}>
                        <span className="hal-eye" data-state={halEyeState(overallStatus)} title={overallTitle} aria-hidden="true" />
                      </Link>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  )
}

export function DashboardMatrixPage() {
  type MatrixState = { kind: 'loading' } | { kind: 'ready'; dashboard: Dashboard } | { kind: 'error'; message: string }

  const params = useParams()
  const focusEnvId = (params.environmentId ?? '').trim() || null
  const [state, setState] = useState<MatrixState>({ kind: 'loading' })

  const refresh = useCallback((signal?: AbortSignal) => {
    return fetchDashboard(signal)
      .then((dashboard) => setState({ kind: 'ready', dashboard }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
  }, [])

  const refreshNow = useCallback(() => {
    refresh()
  }, [refresh])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  useEffect(() => {
    if (!focusEnvId) return
    if (state.kind !== 'ready') return
    const el = document.getElementById(`env-${focusEnvId}`)
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [focusEnvId, state])

  const tools = useMemo(() => <DashboardToolsBar active="matrix" onRefresh={refreshNow} />, [refreshNow])
  useToolsBar(tools)

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Dashboard · ${state.dashboard.environments.length} environments`
    return 'Dashboard'
  }, [state])

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">Detailed matrix. Scans run automatically on a background schedule.</div>

      {state.kind === 'loading' ? (
        <div className="card" style={{ marginTop: 12 }}>
          <div className="muted">Loading…</div>
        </div>
      ) : null}
      {state.kind === 'error' ? (
        <div className="card" style={{ marginTop: 12 }}>
          <div className="muted">Error: {state.message}</div>
        </div>
      ) : null}

      {state.kind === 'ready' ? (
        <div style={{ marginTop: 12 }}>
          {state.dashboard.environments.map((env) => {
            const focus = focusEnvId === env.id
            return (
              <section
                id={`env-${env.id}`}
                key={env.id}
                className="card"
                style={{ marginTop: 12, padding: 12, outline: focus ? '2px solid rgba(51, 225, 255, 0.25)' : undefined }}
              >
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
                  <div className="h2" style={{ margin: 0 }}>
                    {env.name}
                  </div>
                  <div className="muted" style={{ marginLeft: 'auto' }}>
                    <Link to={`/environments/${encodeURIComponent(env.id)}`} style={{ textDecoration: 'none' }}>
                      Environment config
                    </Link>
                  </div>
                </div>

                {env.sections.map((section) => {
                  const hasRows = section.rows.length > 0
                  return (
                    <div key={section.kind} style={{ marginTop: 16 }}>
                      <div className="h2">{section.title}</div>
                      {!hasRows ? <div className="muted" style={{ marginTop: 8 }}>No targets in this section.</div> : null}
                      {hasRows ? (
                        <div className="tableWrap" style={{ marginTop: 10 }}>
                          <table className="table">
                            <thead>
                              <tr>
                                <th>Server</th>
                                {section.columns.map((c) => (
                                  <th key={c.key}>{c.label}</th>
                                ))}
                                <th>Status</th>
                              </tr>
                            </thead>
                            <tbody>
                              {section.rows.map((r) => (
                                <tr key={r.id}>
                                  <td style={{ fontWeight: 800 }}>
                                    {r.link ? (
                                      <Link to={r.link} style={{ textDecoration: 'none' }}>
                                        {r.label}
                                      </Link>
                                    ) : (
                                      r.label
                                    )}
                                  </td>
                                  {r.cells.map((c, idx) => (
                                    <td key={idx} title={c.title ?? undefined}>
                                      {renderCell(c)}
                                    </td>
                                  ))}
                                  <td>{statusPill(r.status)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      ) : null}
                    </div>
                  )
                })}
              </section>
            )
          })}
        </div>
      ) : null}
    </div>
  )
}
