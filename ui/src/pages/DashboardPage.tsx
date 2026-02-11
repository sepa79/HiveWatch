import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  fetchActuatorTargets,
  fetchDashboardEnvironments,
  fetchEnvironments,
  fetchServers,
  fetchTomcatTargets,
  type ActuatorTarget,
  type DashboardEnvironment,
  type DecisionVerdict,
  type Server,
  type TomcatEnvironmentStatus,
  type TomcatRole,
  type TomcatTarget,
  type TomcatWebapp,
} from '../lib/hivewatchApi'
import { useToolsBar } from '../components/ToolsBarContext'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; environments: { id: string; name: string }[] }
  | { kind: 'error'; message: string }

type EnvData =
  | { kind: 'loading' }
  | { kind: 'ready'; servers: Server[]; tomcatTargets: TomcatTarget[]; actuatorTargets: ActuatorTarget[] }
  | { kind: 'error'; message: string }

type RowStatus = 'OK' | 'BLOCK' | 'UNKNOWN'

const TOMCAT_COLUMN_ORDER: { role: TomcatRole; label: string }[] = [
  { role: 'SERVICES', label: 'Services' },
  { role: 'AUTH', label: 'Auth' },
  { role: 'PAYMENTS', label: 'Payments' },
]

const DEFAULT_DOCKER_SERVICE_ORDER = ['payments', 'services', 'auth']

const BUILT_IN_WEBAPPS = new Set(['/', '/manager', '/host-manager', '/docs', '/examples'])

function titleFromError(targetKind: string, message: string) {
  return `${targetKind} error: ${message}`
}

function errorIcon(title: string) {
  return (
    <span title={title} style={{ fontWeight: 900 }} aria-label="error">
      !
    </span>
  )
}

function statusPill(status: RowStatus) {
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

function relevantWebapps(webapps: TomcatWebapp[]): TomcatWebapp[] {
  return webapps.filter((w) => !BUILT_IN_WEBAPPS.has(w.path))
}

function singleWebappsVersion(webapps: TomcatWebapp[]):
  | { kind: 'ok'; version: string }
  | { kind: 'error'; message: string }
  | { kind: 'unknown' } {
  const relevant = relevantWebapps(webapps)
  if (relevant.length === 0) {
    return { kind: 'unknown' }
  }

  const versions = new Set<string>()
  const missing: string[] = []
  for (const w of relevant) {
    if (!w.version) {
      missing.push(w.name)
      continue
    }
    versions.add(w.version)
  }

  if (missing.length > 0) {
    return { kind: 'error', message: `Missing webapp versions: ${missing.slice(0, 6).join(', ')}` }
  }
  if (versions.size === 1) {
    return { kind: 'ok', version: [...versions][0] }
  }
  return { kind: 'error', message: `Multiple webapp versions: ${[...versions].join(', ')}` }
}

function uniformString(values: Array<string | null | undefined>):
  | { kind: 'ok'; value: string }
  | { kind: 'unknown' }
  | { kind: 'error'; message: string } {
  const present = values.map((v) => (v ?? '').trim()).filter((v) => v !== '')
  if (present.length === 0) return { kind: 'unknown' }
  const uniq = new Set(present)
  if (uniq.size === 1) return { kind: 'ok', value: [...uniq][0] }
  return { kind: 'error', message: `Multiple values: ${[...uniq].join(' · ')}` }
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

function mapGroupStatus(status: TomcatEnvironmentStatus): 'OK' | 'BLOCK' | 'UNKNOWN' {
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

export function DashboardOverviewPage() {
  const navigate = useNavigate()
  type OverviewState =
    | { kind: 'loading' }
    | { kind: 'ready'; environments: DashboardEnvironment[] }
    | { kind: 'error'; message: string }

  const [state, setState] = useState<OverviewState>({ kind: 'loading' })

  const refresh = useCallback((signal?: AbortSignal) => {
    return fetchDashboardEnvironments(signal)
      .then((environments) => setState({ kind: 'ready', environments }))
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
    if (state.kind === 'ready') return `Dashboard · ${state.environments.length} environments`
    return 'Dashboard'
  }, [state])

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">High-level overview. Click any status icon to drill down into details.</div>

      <div className="tableWrap" style={{ marginTop: 12 }}>
        <table className="table">
          <thead>
            <tr>
              <th style={{ minWidth: 240 }}>Environment</th>
              <th>Servers</th>
              <th>Docker</th>
              <th>AWS</th>
              <th>Overall</th>
            </tr>
          </thead>
          <tbody>
            {state.kind === 'loading' ? (
              <tr>
                <td colSpan={5}>
                  <span className="muted">Loading…</span>
                </td>
              </tr>
            ) : null}
            {state.kind === 'error' ? (
              <tr>
                <td colSpan={5}>
                  <span className="muted">Error: {state.message}</span>
                </td>
              </tr>
            ) : null}
            {state.kind === 'ready' && state.environments.length === 0 ? (
              <tr>
                <td colSpan={5}>
                  <span className="muted">No environments visible for the current user.</span>
                </td>
              </tr>
            ) : null}

            {state.kind === 'ready'
              ? state.environments.map((env) => {
                  const serversStatus = mapGroupStatus(env.tomcatStatus)
                  const dockerStatus = mapGroupStatus(env.actuatorStatus)
                  const awsStatus: 'UNKNOWN' = 'UNKNOWN'
                  const overallStatus = decisionAsStatus(env.decisionVerdict)

                  const matrixLink = `/dashboard/matrix/${encodeURIComponent(env.id)}`
                  const serversTitle = `Tomcats: ${env.tomcatOk} OK · ${env.tomcatError} ERR · ${env.tomcatTargets} total`
                  const dockerTitle = `Microservices: ${env.actuatorUp} UP · ${env.actuatorDown} DOWN · ${env.actuatorError} ERR · ${env.actuatorTargets} total`
                  const overallTitle = `Decision: ${env.decisionVerdict} · issues: ${env.decisionBlockIssues} block, ${env.decisionWarnIssues} warn, ${env.decisionUnknownIssues} unknown`

                  return (
                    <tr key={env.id}>
                      <td style={{ fontWeight: 900 }}>
                        <button
                          type="button"
                          className="linkButton"
                          onClick={() => navigate(matrixLink)}
                          style={{ textDecoration: 'none' }}
                        >
                          {env.name}
                        </button>
                      </td>
                      <td>
                        <Link to={matrixLink} className="statusIconLink" aria-label={`${env.name} servers status`}>
                          <span className="hal-eye" data-state={halEyeState(serversStatus)} title={serversTitle} aria-hidden="true" />
                        </Link>
                      </td>
                      <td>
                        <Link to={matrixLink} className="statusIconLink" aria-label={`${env.name} docker status`}>
                          <span className="hal-eye" data-state={halEyeState(dockerStatus)} title={dockerTitle} aria-hidden="true" />
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
                })
              : null}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export function DashboardMatrixPage() {
  const params = useParams()
  const focusEnvId = (params.environmentId ?? '').trim() || null
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [envData, setEnvData] = useState<Record<string, EnvData>>({})

  const refreshEnvironments = useCallback((signal?: AbortSignal) => {
    return fetchEnvironments(signal)
      .then((environments) => setState({ kind: 'ready', environments }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
  }, [])

  const refreshNow = useCallback(() => {
    refreshEnvironments()
  }, [refreshEnvironments])

  const loadEnvironment = useCallback((environmentId: string, signal?: AbortSignal) => {
    setEnvData((prev) => ({ ...prev, [environmentId]: { kind: 'loading' } }))
    return Promise.all([
      fetchServers(environmentId, signal),
      fetchTomcatTargets(environmentId, signal),
      fetchActuatorTargets(environmentId, signal),
    ])
      .then(([servers, tomcatTargets, actuatorTargets]) =>
        setEnvData((prev) => ({ ...prev, [environmentId]: { kind: 'ready', servers, tomcatTargets, actuatorTargets } })),
      )
      .catch((e) =>
        setEnvData((prev) => ({
          ...prev,
          [environmentId]: { kind: 'error', message: e instanceof Error ? e.message : 'Request failed' },
        })),
      )
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    refreshEnvironments(controller.signal)
    return () => controller.abort()
  }, [refreshEnvironments])

  useEffect(() => {
    if (state.kind !== 'ready') return
    const controller = new AbortController()
    Promise.all(state.environments.map((e) => loadEnvironment(e.id, controller.signal)))
    return () => controller.abort()
  }, [state, loadEnvironment])

  useEffect(() => {
    if (!focusEnvId) return
    if (state.kind !== 'ready') return
    const envReady = envData[focusEnvId]?.kind === 'ready'
    if (!envReady) return
    const el = document.getElementById(`env-${focusEnvId}`)
    if (!el) return
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [focusEnvId, envData, state])

  const tools = useMemo(() => <DashboardToolsBar active="matrix" onRefresh={refreshNow} />, [refreshNow])
  useToolsBar(tools)

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Dashboard · ${state.environments.length} environments`
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
          {state.environments.map((env) => {
            const data = envData[env.id] ?? { kind: 'loading' as const }
            const hasTomcats = data.kind === 'ready' && data.tomcatTargets.length > 0
            const hasActuators = data.kind === 'ready' && data.actuatorTargets.length > 0
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

                {data.kind === 'loading' ? <div className="muted" style={{ marginTop: 10 }}>Loading topology…</div> : null}
                {data.kind === 'error' ? <div className="muted" style={{ marginTop: 10 }}>Error: {data.message}</div> : null}

                {data.kind === 'ready' && !hasTomcats && !hasActuators ? (
                  <div className="muted" style={{ marginTop: 10 }}>
                    No targets in this environment.
                  </div>
                ) : null}

                {data.kind === 'ready' && hasTomcats ? (
                  <div style={{ marginTop: 12 }}>
                    <div className="h2">Tomcats</div>
                    {(() => {
                      const targetsByServerId = new Map<string, TomcatTarget[]>()
                      for (const t of data.tomcatTargets) {
                        const list = targetsByServerId.get(t.serverId) ?? []
                        list.push(t)
                        targetsByServerId.set(t.serverId, list)
                      }
                      const rows = [...targetsByServerId.entries()].map(([serverId, targets]) => {
                        const serverName = targets[0]?.serverName ?? data.servers.find((s) => s.id === serverId)?.name ?? 'Unknown'
                        const byRole = new Map<TomcatRole, TomcatTarget>()
                        for (const t of targets) byRole.set(t.role, t)

                        const roleCells = TOMCAT_COLUMN_ORDER.map(({ role }) => {
                          const target = byRole.get(role)
                          if (!target) return { kind: 'error' as const, title: `Missing target: ${role}` }
                          const st = target.state
                          if (!st) return { kind: 'unknown' as const }
                          if (st.outcomeKind !== 'SUCCESS') {
                            return {
                              kind: 'error' as const,
                              title: titleFromError(role, `${st.errorKind ?? 'UNKNOWN'}: ${st.errorMessage ?? 'Request failed'}`),
                            }
                          }
                          const v = singleWebappsVersion(st.webapps)
                          if (v.kind === 'ok') return { kind: 'ok' as const, version: v.version }
                          if (v.kind === 'unknown') return { kind: 'unknown' as const }
                          return { kind: 'error' as const, title: `${role} mismatch: ${v.message}` }
                        })

                        const serverTomcat = uniformString(
                          TOMCAT_COLUMN_ORDER.map(({ role }) => byRole.get(role)?.state?.tomcatVersion ?? null),
                        )
                        const serverJava = uniformString(
                          TOMCAT_COLUMN_ORDER.map(({ role }) => byRole.get(role)?.state?.javaVersion ?? null),
                        )
                        const serverOs = uniformString(TOMCAT_COLUMN_ORDER.map(({ role }) => byRole.get(role)?.state?.os ?? null))

                        const rowStatus: RowStatus = (() => {
                          if (roleCells.some((c) => c.kind === 'error')) return 'BLOCK'
                          if ([serverTomcat, serverJava, serverOs].some((c) => c.kind === 'error')) return 'BLOCK'
                          if (
                            roleCells.some((c) => c.kind === 'unknown') ||
                            [serverTomcat, serverJava, serverOs].some((c) => c.kind === 'unknown')
                          )
                            return 'UNKNOWN'
                          return 'OK'
                        })()

                        return { serverId, serverName, roleCells, serverTomcat, serverJava, serverOs, rowStatus }
                      })

                      rows.sort((a, b) => a.serverName.localeCompare(b.serverName))

                      return (
                        <div className="tableWrap" style={{ marginTop: 10 }}>
                          <table className="table">
                            <thead>
                              <tr>
                                <th>Server</th>
                                {TOMCAT_COLUMN_ORDER.map((c) => (
                                  <th key={c.role}>{c.label}</th>
                                ))}
                                <th>Tomcat</th>
                                <th>Java</th>
                                <th>OS</th>
                                <th>Status</th>
                              </tr>
                            </thead>
                            <tbody>
                              {rows.map((r) => (
                                <tr key={r.serverId}>
                                  <td style={{ fontWeight: 800 }}>{r.serverName}</td>
                                  {r.roleCells.map((c, idx) => (
                                    <td key={idx}>
                                      {c.kind === 'ok' ? c.version : c.kind === 'unknown' ? <span className="muted">—</span> : errorIcon(c.title)}
                                    </td>
                                  ))}
                                  <td>
                                    {r.serverTomcat.kind === 'ok'
                                      ? r.serverTomcat.value
                                      : r.serverTomcat.kind === 'unknown'
                                        ? <span className="muted">—</span>
                                        : errorIcon(r.serverTomcat.message)}
                                  </td>
                                  <td>
                                    {r.serverJava.kind === 'ok'
                                      ? r.serverJava.value
                                      : r.serverJava.kind === 'unknown'
                                        ? <span className="muted">—</span>
                                        : errorIcon(r.serverJava.message)}
                                  </td>
                                  <td>
                                    {r.serverOs.kind === 'ok'
                                      ? r.serverOs.value
                                      : r.serverOs.kind === 'unknown'
                                        ? <span className="muted">—</span>
                                        : errorIcon(r.serverOs.message)}
                                  </td>
                                  <td>{statusPill(r.rowStatus)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )
                    })()}
                  </div>
                ) : null}

                {data.kind === 'ready' && hasActuators ? (
                  <div style={{ marginTop: 16 }}>
                    <div className="h2">Docker Swarm</div>
                    {(() => {
                      const targetsByServerId = new Map<string, ActuatorTarget[]>()
                      for (const t of data.actuatorTargets) {
                        const list = targetsByServerId.get(t.serverId) ?? []
                        list.push(t)
                        targetsByServerId.set(t.serverId, list)
                      }

                      const serviceColumns = (() => {
                        const all = new Set<string>()
                        for (const t of data.actuatorTargets) all.add(t.profile)
                        const cols = [...all]
                        cols.sort((a, b) => {
                          const ai = DEFAULT_DOCKER_SERVICE_ORDER.indexOf(a)
                          const bi = DEFAULT_DOCKER_SERVICE_ORDER.indexOf(b)
                          if (ai !== -1 || bi !== -1) return (ai === -1 ? 999 : ai) - (bi === -1 ? 999 : bi)
                          return a.localeCompare(b)
                        })
                        return cols
                      })()

                      const rows = [...targetsByServerId.entries()].map(([serverId, targets]) => {
                        const serverName = targets[0]?.serverName ?? data.servers.find((s) => s.id === serverId)?.name ?? 'Unknown'
                        const byProfile = new Map<string, ActuatorTarget>()
                        for (const t of targets) byProfile.set(t.profile, t)

                        const cells = serviceColumns.map((profile) => {
                          const t = byProfile.get(profile)
                          if (!t) return { kind: 'error' as const, title: `Missing service: ${profile}` }
                          const st = t.state
                          if (!st) return { kind: 'unknown' as const }
                          if (st.outcomeKind !== 'SUCCESS') {
                            return {
                              kind: 'error' as const,
                              title: titleFromError(profile, `${st.errorKind ?? 'UNKNOWN'}: ${st.errorMessage ?? 'Request failed'}`),
                            }
                          }
                          if ((st.healthStatus ?? '').toUpperCase() !== 'UP') {
                            return { kind: 'error' as const, title: `${profile} is ${st.healthStatus ?? 'UNKNOWN'}` }
                          }
                          if (!st.buildVersion) {
                            return { kind: 'unknown' as const }
                          }
                          return { kind: 'ok' as const, version: st.buildVersion, title: st.appName ?? profile }
                        })

                        const rowStatus: RowStatus = (() => {
                          if (cells.some((c) => c.kind === 'error')) return 'BLOCK'
                          if (cells.some((c) => c.kind === 'unknown')) return 'UNKNOWN'
                          return 'OK'
                        })()

                        return { serverId, serverName, cells, rowStatus }
                      })

                      rows.sort((a, b) => a.serverName.localeCompare(b.serverName))

                      return (
                        <div className="tableWrap" style={{ marginTop: 10 }}>
                          <table className="table">
                            <thead>
                              <tr>
                                <th>Server</th>
                                {serviceColumns.map((c) => (
                                  <th key={c}>{c}</th>
                                ))}
                                <th>Status</th>
                              </tr>
                            </thead>
                            <tbody>
                              {rows.map((r) => (
                                <tr key={r.serverId}>
                                  <td style={{ fontWeight: 800 }}>{r.serverName}</td>
                                  {r.cells.map((c, idx) => (
                                    <td key={idx} title={c.kind === 'ok' ? c.title : undefined}>
                                      {c.kind === 'ok' ? c.version : c.kind === 'unknown' ? <span className="muted">—</span> : errorIcon(c.title)}
                                    </td>
                                  ))}
                                  <td>{statusPill(r.rowStatus)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )
                    })()}
                  </div>
                ) : null}
              </section>
            )
          })}
        </div>
      ) : null}
    </div>
  )
}
