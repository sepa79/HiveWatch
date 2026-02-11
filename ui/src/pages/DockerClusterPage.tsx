import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useToolsBar } from '../components/ToolsBarContext'
import { fetchDockerServicesPage, fetchServers, type DockerServiceListItem, type DockerServicesPage } from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; serverName: string; page: DockerServicesPage }
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

function targetStatus(t: DockerServiceListItem): 'OK' | 'BLOCK' | 'UNKNOWN' {
  if (!t.outcomeKind) return 'UNKNOWN'
  if (t.outcomeKind !== 'SUCCESS') return 'BLOCK'
  if ((t.healthStatus ?? '').toUpperCase() !== 'UP') return 'BLOCK'
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
  page,
  size,
  total,
  onPrev,
  onNext,
  onRefresh,
}: {
  environmentId: string
  serverName: string
  search: string
  onSearchChange: (next: string) => void
  page: number
  size: number
  total: number
  onPrev: () => void
  onNext: () => void
  onRefresh: () => void
}) {
  const navigate = useNavigate()
  const pageCount = Math.max(1, Math.ceil(total / Math.max(1, size)))
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
      <div className="muted" style={{ marginLeft: 12, whiteSpace: 'nowrap' }}>
        Page {page + 1} / {pageCount}
      </div>
      <button type="button" className="button" onClick={onPrev} disabled={page <= 0}>
        Prev
      </button>
      <button type="button" className="button" onClick={onNext} disabled={(page + 1) * size >= total}>
        Next
      </button>
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
  const [page, setPage] = useState(0)
  const size = 50

  const refresh = useCallback(
    (signal?: AbortSignal) => {
      if (!environmentId || !serverId) {
        setState({ kind: 'error', message: 'Missing environmentId/serverId' })
        return Promise.resolve()
      }
      return Promise.all([fetchServers(environmentId, signal), fetchDockerServicesPage(environmentId, serverId, { q: search.trim() || undefined, page, size }, signal)])
        .then(([servers, pageDto]) => {
          const serverName = servers.find((s) => s.id === serverId)?.name ?? 'Unknown'
          setState({ kind: 'ready', serverName, page: pageDto })
        })
        .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    },
    [environmentId, page, search, serverId],
  )

  const refreshNow = useCallback(() => {
    refresh()
  }, [refresh])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const onSearchChange = useCallback((next: string) => {
    setSearch(next)
    setPage(0)
  }, [])

  const tools = useMemo(() => {
    const serverName = state.kind === 'ready' ? state.serverName : 'Docker'
    const total = state.kind === 'ready' ? state.page.total : 0
    const sizeValue = state.kind === 'ready' ? state.page.size : size
    const pageValue = state.kind === 'ready' ? state.page.page : page
    return (
      <DockerClusterToolsBar
        environmentId={environmentId}
        serverName={serverName}
        search={search}
        onSearchChange={onSearchChange}
        page={pageValue}
        size={sizeValue}
        total={total}
        onPrev={() => setPage((p) => Math.max(0, p - 1))}
        onNext={() => setPage((p) => p + 1)}
        onRefresh={refreshNow}
      />
    )
  }, [environmentId, onSearchChange, page, refreshNow, search, size, state])
  useToolsBar(tools)

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Docker · ${state.serverName} · ${state.page.total} services`
    return 'Docker'
  }, [state])

  const rows = useMemo(() => (state.kind === 'ready' ? state.page.items : []), [state])

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
                const stKind = targetStatus(t)
                const detailLink = `/dashboard/docker/${encodeURIComponent(environmentId)}/${encodeURIComponent(serverId)}/services/${encodeURIComponent(t.targetId)}`
                const healthText = t.outcomeKind === 'SUCCESS' ? (t.healthStatus ?? 'UNKNOWN') : t.outcomeKind ? 'ERROR' : '—'
                const version =
                  t.outcomeKind === 'SUCCESS' && (t.healthStatus ?? '').toUpperCase() === 'UP'
                    ? t.buildVersion
                      ? t.buildVersion
                      : null
                    : null
                const versionNode =
                  t.outcomeKind && t.outcomeKind !== 'SUCCESS'
                    ? errorIcon(`${t.profile} error: ${t.errorKind ?? 'UNKNOWN'}: ${t.errorMessage ?? 'Request failed'}`)
                    : t.outcomeKind && (t.healthStatus ?? '').toUpperCase() !== 'UP'
                      ? errorIcon(`${t.profile} is ${t.healthStatus ?? 'UNKNOWN'}`)
                      : version
                        ? version
                        : <span className="muted">—</span>

                return (
                  <tr key={t.targetId}>
                    <td style={{ fontWeight: 800 }}>
                      <Link to={detailLink} style={{ textDecoration: 'none' }}>
                        {t.profile}
                      </Link>
                      <div className="muted" style={{ fontWeight: 500 }}>
                        {t.appName ?? '—'}
                      </div>
                    </td>
                    <td>{versionNode}</td>
                    <td>{healthText}</td>
                    <td className="muted">{formatTs(t.scannedAt ?? null)}</td>
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
