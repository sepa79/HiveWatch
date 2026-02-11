import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useToolsBar } from '../components/ToolsBarContext'
import { fetchActuatorTargets, fetchServers, type ActuatorTarget } from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; serverName: string; targets: ActuatorTarget[] }
  | { kind: 'error'; message: string }

function errorIcon(title: string) {
  return (
    <span title={title} style={{ fontWeight: 900 }} aria-label="error">
      !
    </span>
  )
}

function statusPill(status: 'OK' | 'BLOCK' | 'UNKNOWN') {
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

function targetStatus(t: ActuatorTarget): 'OK' | 'BLOCK' | 'UNKNOWN' {
  const st = t.state
  if (!st) return 'UNKNOWN'
  if (st.outcomeKind !== 'SUCCESS') return 'BLOCK'
  if ((st.healthStatus ?? '').toUpperCase() !== 'UP') return 'BLOCK'
  return 'OK'
}

function formatTs(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString()
}

function DockerClusterToolsBar({
  environmentId,
  serverName,
  search,
  onSearchChange,
  onRefresh,
}: {
  environmentId: string
  serverName: string
  search: string
  onSearchChange: (next: string) => void
  onRefresh: () => void
}) {
  const navigate = useNavigate()
  return (
    <div className="toolsRow">
      <button type="button" className="button" onClick={() => navigate(`/dashboard/matrix/${encodeURIComponent(environmentId)}`)}>
        ← Matrix
      </button>
      <div className="muted" style={{ marginLeft: 10, fontWeight: 800 }}>
        {serverName}
      </div>
      <input
        className="input"
        style={{ marginLeft: 12, maxWidth: 360 }}
        placeholder="Search services…"
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
        aria-label="Search services"
      />
      <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={onRefresh}>
        Refresh
      </button>
    </div>
  )
}

export function DockerClusterPage() {
  const params = useParams()
  const environmentId = (params.environmentId ?? '').trim()
  const serverId = (params.serverId ?? '').trim()

  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [search, setSearch] = useState('')

  const refresh = useCallback(
    (signal?: AbortSignal) => {
      if (!environmentId || !serverId) {
        setState({ kind: 'error', message: 'Missing environmentId/serverId' })
        return Promise.resolve()
      }
      return Promise.all([fetchServers(environmentId, signal), fetchActuatorTargets(environmentId, signal)])
        .then(([servers, targets]) => {
          const serverName = servers.find((s) => s.id === serverId)?.name ?? targets.find((t) => t.serverId === serverId)?.serverName ?? 'Unknown'
          const filtered = targets.filter((t) => t.serverId === serverId)
          setState({ kind: 'ready', serverName, targets: filtered })
        })
        .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    },
    [environmentId, serverId],
  )

  const refreshNow = useCallback(() => {
    refresh()
  }, [refresh])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const onSearchChange = useCallback((next: string) => setSearch(next), [])

  const tools = useMemo(() => {
    const serverName = state.kind === 'ready' ? state.serverName : 'Docker'
    return (
      <DockerClusterToolsBar
        environmentId={environmentId}
        serverName={serverName}
        search={search}
        onSearchChange={onSearchChange}
        onRefresh={refreshNow}
      />
    )
  }, [environmentId, onSearchChange, refreshNow, search, state])
  useToolsBar(tools)

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Docker · ${state.serverName} · ${state.targets.length} services`
    return 'Docker'
  }, [state])

  const rows = useMemo(() => {
    if (state.kind !== 'ready') return []
    const q = search.trim().toLowerCase()
    const list = q
      ? state.targets.filter((t) => (t.profile + ' ' + (t.state?.appName ?? '')).toLowerCase().includes(q))
      : state.targets
    return [...list].sort((a, b) => a.profile.localeCompare(b.profile))
  }, [search, state])

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">
        <Link to={`/dashboard/matrix/${encodeURIComponent(environmentId)}`}>← Back to matrix</Link>
      </div>
      <div className="muted" style={{ marginTop: 6 }}>
        Scanner runs automatically on a background schedule. No manual scanning from UI.
      </div>

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
                <th>Service</th>
                <th>Version</th>
                <th>Health</th>
                <th>Last scan</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={5} className="muted">
                    No services.
                  </td>
                </tr>
              ) : null}
              {rows.map((t) => {
                const st = t.state
                const stKind = targetStatus(t)
                const detailLink = `/dashboard/docker/${encodeURIComponent(environmentId)}/${encodeURIComponent(serverId)}/services/${encodeURIComponent(t.id)}`
                const healthText =
                  st && st.outcomeKind === 'SUCCESS' ? (st.healthStatus ?? 'UNKNOWN') : st ? 'ERROR' : '—'
                const version =
                  st && st.outcomeKind === 'SUCCESS' && (st.healthStatus ?? '').toUpperCase() === 'UP'
                    ? st.buildVersion
                      ? st.buildVersion
                      : null
                    : null
                const versionNode =
                  st && st.outcomeKind !== 'SUCCESS'
                    ? errorIcon(`${t.profile} error: ${st.errorKind ?? 'UNKNOWN'}: ${st.errorMessage ?? 'Request failed'}`)
                    : st && (st.healthStatus ?? '').toUpperCase() !== 'UP'
                      ? errorIcon(`${t.profile} is ${st.healthStatus ?? 'UNKNOWN'}`)
                      : version
                        ? version
                        : <span className="muted">—</span>

                return (
                  <tr key={t.id}>
                    <td style={{ fontWeight: 800 }}>
                      <Link to={detailLink} style={{ textDecoration: 'none' }}>
                        {t.profile}
                      </Link>
                      <div className="muted" style={{ fontWeight: 500 }}>
                        {st?.appName ?? '—'}
                      </div>
                    </td>
                    <td>{versionNode}</td>
                    <td>{healthText}</td>
                    <td className="muted">{formatTs(st?.scannedAt ?? null)}</td>
                    <td>{statusPill(stKind)}</td>
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

