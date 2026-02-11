import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useToolsBar } from '../components/ToolsBarContext'
import { fetchActuatorTarget, type ActuatorTarget } from '../lib/hivewatchApi'

type LoadState = { kind: 'loading' } | { kind: 'ready'; target: ActuatorTarget } | { kind: 'error'; message: string }

function formatTs(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString()
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

function DockerServiceToolsBar({
  backTo,
  label,
  onRefresh,
}: {
  backTo: string
  label: string
  onRefresh: () => void
}) {
  const navigate = useNavigate()
  return (
    <div className="toolsRow">
      <button type="button" className="button" onClick={() => navigate(backTo)}>
        ← Services
      </button>
      <div className="muted" style={{ marginLeft: 10, fontWeight: 800 }}>
        {label}
      </div>
      <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={onRefresh}>
        Refresh
      </button>
    </div>
  )
}

export function DockerServicePage() {
  const params = useParams()
  const environmentId = (params.environmentId ?? '').trim()
  const serverId = (params.serverId ?? '').trim()
  const targetId = (params.targetId ?? '').trim()

  const [state, setState] = useState<LoadState>({ kind: 'loading' })

  const refresh = useCallback(
    (signal?: AbortSignal) => {
      if (!environmentId || !targetId) {
        setState({ kind: 'error', message: 'Missing environmentId/targetId' })
        return Promise.resolve()
      }
      return fetchActuatorTarget(environmentId, targetId, signal)
        .then((target) => setState({ kind: 'ready', target }))
        .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    },
    [environmentId, targetId],
  )

  const refreshNow = useCallback(() => {
    refresh()
  }, [refresh])

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const backTo = useMemo(() => `/dashboard/docker/${encodeURIComponent(environmentId)}/${encodeURIComponent(serverId)}`, [environmentId, serverId])

  const tools = useMemo(() => {
    const label = state.kind === 'ready' ? state.target.profile : 'Service'
    return <DockerServiceToolsBar backTo={backTo} label={label} onRefresh={refreshNow} />
  }, [backTo, refreshNow, state])
  useToolsBar(tools)

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Docker · ${state.target.profile}`
    return 'Docker'
  }, [state])

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">
        <Link to={backTo}>← Back to services</Link>
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
        <div className="card" style={{ marginTop: 12 }}>
          <div className="kv">
            <div className="k">Environment</div>
            <div className="v">{environmentId}</div>
            <div className="k">Server</div>
            <div className="v">{state.target.serverName}</div>
            <div className="k">Profile</div>
            <div className="v">{state.target.profile}</div>
            <div className="k">App</div>
            <div className="v">{state.target.state?.appName ?? '—'}</div>
            <div className="k">Endpoint</div>
            <div className="v">
              {state.target.baseUrl}:{state.target.port}
            </div>
            <div className="k">Status</div>
            <div className="v">{statusPill(targetStatus(state.target))}</div>
            <div className="k">Last scan</div>
            <div className="v">{formatTs(state.target.state?.scannedAt ?? null)}</div>
            <div className="k">Health</div>
            <div className="v">{state.target.state?.healthStatus ?? '—'}</div>
            <div className="k">Version</div>
            <div className="v">{state.target.state?.buildVersion ?? '—'}</div>
            <div className="k">Error</div>
            <div className="v">
              {state.target.state && state.target.state.outcomeKind !== 'SUCCESS'
                ? `${state.target.state.errorKind ?? 'UNKNOWN'}: ${state.target.state.errorMessage ?? 'Request failed'}`
                : '—'}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

