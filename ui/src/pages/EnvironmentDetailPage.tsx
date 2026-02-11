import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  createServer,
  createTomcatTarget,
  fetchServers,
  fetchTomcatTargets,
  scanEnvironmentTomcats,
  type Server,
  type ServerCreateRequest,
  type TomcatTarget,
  type TomcatTargetCreateRequest,
  type TomcatRole,
} from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; servers: Server[]; targets: TomcatTarget[] }
  | { kind: 'error'; message: string }

export function EnvironmentDetailPage() {
  const params = useParams()
  const environmentId = params.environmentId ?? ''
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [saving, setSaving] = useState(false)
  const [savingServer, setSavingServer] = useState(false)
  const [scanning, setScanning] = useState(false)

  const [form, setForm] = useState<TomcatTargetCreateRequest>({
    serverId: '',
    role: 'PAYMENTS',
    baseUrl: 'http://hc-dummy-nft-01-touchpoint-tomcats',
    port: 8081,
    username: 'hc-manager',
    password: 'hc-manager-pass',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  })

  const [serverForm, setServerForm] = useState<ServerCreateRequest>({ name: '' })

  const refresh = (signal?: AbortSignal) =>
    Promise.all([fetchServers(environmentId, signal), fetchTomcatTargets(environmentId, signal)])
      .then(([servers, targets]) => setState({ kind: 'ready', servers, targets }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [environmentId])

  useEffect(() => {
    if (state.kind !== 'ready') return
    if (form.serverId) return
    if (state.servers.length === 0) return
    setForm((f) => ({ ...f, serverId: state.servers[0].id }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state])

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Environment · ${state.servers.length} servers · ${state.targets.length} Tomcat targets`
    return 'Environment'
  }, [state])

  const pill = (t: TomcatTarget) => {
    if (!t.state) return <div className="pill" data-kind="missing">UNKNOWN</div>
    if (t.state.outcomeKind === 'SUCCESS') return <div className="pill" data-kind="ok">OK</div>
    return <div className="pill" data-kind="alert">BLOCK</div>
  }

  const roleLabel = (role: TomcatRole) => {
    const labels: Record<TomcatRole, string> = {
      PAYMENTS: 'payments',
      SERVICES: 'services',
      AUTH: 'auth',
    }
    return labels[role]
  }

  const formatTs = (iso: string | null) => {
    if (!iso) return '—'
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso
    return date.toLocaleString()
  }

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">
        <Link to="/environments">← Back to environments</Link>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        <div className="h2">Topology: Environment → Server → Tomcats</div>
        <div className="muted" style={{ marginTop: 6 }}>
          Every Tomcat target is explicitly assigned to a Server and Role (<code>payments</code>/<code>services</code>/<code>auth</code>). Scanner hits{' '}
          <code>/manager/html</code> using HTTP Basic auth.
        </div>
        <div className="muted" style={{ marginTop: 6 }}>
          If HiveWatch runs in Docker, <code>localhost</code> means the container, so use the dummy-stack container hostnames (see dummy-stack README).
        </div>

        <div style={{ marginTop: 10, display: 'flex', gap: 10, alignItems: 'center' }}>
          <button
            type="button"
            className="button"
            disabled={scanning || state.kind !== 'ready' || state.targets.length === 0}
            onClick={() => {
              const controller = new AbortController()
              setScanning(true)
              scanEnvironmentTomcats(environmentId, controller.signal)
                .then(() => refresh())
                .finally(() => setScanning(false))
            }}
          >
            {scanning ? 'Scanning…' : 'Scan all targets'}
          </button>
        </div>

        {state.kind === 'loading' ? <div className="muted" style={{ marginTop: 10 }}>Loading…</div> : null}
        {state.kind === 'error' ? <div className="muted" style={{ marginTop: 10 }}>Error: {state.message}</div> : null}

        {state.kind === 'ready' ? (
          <div style={{ marginTop: 10 }}>
            {state.servers.length === 0 ? <div className="muted">No servers yet.</div> : null}

            {state.servers.map((server) => {
              const targets = state.targets
                .filter((t) => t.serverId === server.id)
                .slice()
                .sort((a, b) => a.role.localeCompare(b.role))

              return (
                <details key={server.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                  <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ fontWeight: 800 }}>{server.name}</div>
                    <div className="muted" style={{ marginLeft: 'auto' }}>
                      {targets.length} targets
                    </div>
                  </summary>

                  {targets.length === 0 ? <div className="muted" style={{ marginTop: 10 }}>No targets.</div> : null}

                  {targets.map((t) => (
                    <details key={t.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                      <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ fontWeight: 700 }}>{roleLabel(t.role)}</div>
                        {pill(t)}
                        <div className="muted" style={{ marginLeft: 'auto' }}>
                          {t.baseUrl}:{t.port}
                        </div>
                      </summary>

                      <div className="kv" style={{ marginTop: 10 }}>
                        <div className="k">targetId</div>
                        <div className="v">{t.id}</div>
                        <div className="k">lastScan</div>
                        <div className="v">{t.state ? formatTs(t.state.scannedAt) : '—'}</div>
                        <div className="k">webapps</div>
                        <div className="v">{t.state ? t.state.webapps.length : 0}</div>
                        {t.state && t.state.outcomeKind === 'ERROR' ? (
                          <>
                            <div className="k">error</div>
                            <div className="v">
                              {t.state.errorKind}: {t.state.errorMessage}
                            </div>
                          </>
                        ) : null}
                      </div>

                      {t.state && t.state.webapps.length > 0 ? (
                        <ul className="list" style={{ marginTop: 10 }}>
                          {t.state.webapps.map((w) => (
                            <li key={w}>
                              <code>{w}</code>
                            </li>
                          ))}
                        </ul>
                      ) : null}
                    </details>
                  ))}
                </details>
              )
            })}
          </div>
        ) : null}

        <div className="card" style={{ marginTop: 12, padding: 12, maxWidth: 820 }}>
          <div className="h2">Add server</div>
          <form
            style={{ marginTop: 10, display: 'flex', gap: 10, alignItems: 'end' }}
            onSubmit={(e) => {
              e.preventDefault()
              const controller = new AbortController()
              setSavingServer(true)
              createServer(environmentId, serverForm, controller.signal)
                .then(() => refresh())
                .then(() => setServerForm({ name: '' }))
                .finally(() => setSavingServer(false))
            }}
          >
            <label className="field" style={{ flex: 1 }}>
              <div className="fieldLabel">Name</div>
              <input
                className="fieldInput"
                value={serverForm.name}
                onChange={(e) => setServerForm({ name: e.target.value })}
                placeholder="Touchpoint"
                required
              />
            </label>
            <button type="submit" className="button" disabled={savingServer}>
              {savingServer ? 'Saving…' : 'Add server'}
            </button>
          </form>
        </div>

        <div className="card" style={{ marginTop: 12, padding: 12, maxWidth: 820 }}>
          <div className="h2">Add Tomcat target</div>
          <div className="muted" style={{ marginTop: 6 }}>
            Dummy-stack defaults are prefilled for convenience (still sent explicitly).
          </div>

          <form
            style={{ marginTop: 10, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}
            onSubmit={(e) => {
              e.preventDefault()
              const controller = new AbortController()
              setSaving(true)
              createTomcatTarget(environmentId, form, controller.signal)
                .then(() => refresh())
                .then(() => setForm((f) => ({ ...f })))
                .finally(() => setSaving(false))
            }}
          >
            <label className="field">
              <div className="fieldLabel">Server</div>
              <select
                className="fieldInput"
                value={form.serverId}
                onChange={(e) => setForm((f) => ({ ...f, serverId: e.target.value }))}
                required
              >
                <option value="" disabled>
                  Select server…
                </option>
                {state.kind === 'ready'
                  ? state.servers.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))
                  : null}
              </select>
            </label>
            <label className="field">
              <div className="fieldLabel">Role</div>
              <select
                className="fieldInput"
                value={form.role}
                onChange={(e) => setForm((f) => ({ ...f, role: e.target.value as TomcatRole }))}
                required
              >
                <option value="PAYMENTS">payments</option>
                <option value="SERVICES">services</option>
                <option value="AUTH">auth</option>
              </select>
            </label>
            <label className="field">
              <div className="fieldLabel">Base URL</div>
              <input
                className="fieldInput"
                value={form.baseUrl}
                onChange={(e) => setForm((f) => ({ ...f, baseUrl: e.target.value }))}
                placeholder="http://localhost"
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Port</div>
              <input
                className="fieldInput"
                type="number"
                value={form.port}
                onChange={(e) => setForm((f) => ({ ...f, port: Number(e.target.value) }))}
                min={1}
                max={65535}
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Username</div>
              <input
                className="fieldInput"
                value={form.username}
                onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Password</div>
              <input
                className="fieldInput"
                type="password"
                value={form.password}
                onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Connect timeout (ms)</div>
              <input
                className="fieldInput"
                type="number"
                value={form.connectTimeoutMs}
                onChange={(e) => setForm((f) => ({ ...f, connectTimeoutMs: Number(e.target.value) }))}
                min={1}
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Request timeout (ms)</div>
              <input
                className="fieldInput"
                type="number"
                value={form.requestTimeoutMs}
                onChange={(e) => setForm((f) => ({ ...f, requestTimeoutMs: Number(e.target.value) }))}
                min={1}
                required
              />
            </label>

            <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
              <button type="submit" className="button" disabled={saving}>
                {saving ? 'Saving…' : 'Add target'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
