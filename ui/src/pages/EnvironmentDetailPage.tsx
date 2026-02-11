import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  createTomcatTarget,
  fetchTomcatTargets,
  scanEnvironmentTomcats,
  type TomcatTarget,
  type TomcatTargetCreateRequest,
} from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; targets: TomcatTarget[] }
  | { kind: 'error'; message: string }

export function EnvironmentDetailPage() {
  const params = useParams()
  const environmentId = params.environmentId ?? ''
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [saving, setSaving] = useState(false)
  const [scanning, setScanning] = useState(false)

  const [form, setForm] = useState<TomcatTargetCreateRequest>({
    name: '',
    baseUrl: 'http://hc-dummy-nft-01-touchpoint-tomcats',
    port: 8081,
    username: 'hc-manager',
    password: 'hc-manager-pass',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  })

  const refresh = (signal?: AbortSignal) =>
    fetchTomcatTargets(environmentId, signal)
      .then((targets) => setState({ kind: 'ready', targets }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [environmentId])

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Environment · ${state.targets.length} Tomcat targets`
    return 'Environment'
  }, [state])

  const pill = (t: TomcatTarget) => {
    if (!t.state) return <div className="pill" data-kind="missing">UNKNOWN</div>
    if (t.state.outcomeKind === 'SUCCESS') return <div className="pill" data-kind="ok">OK</div>
    return <div className="pill" data-kind="alert">BLOCK</div>
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
        <div className="h2">Tomcat targets (Manager HTML)</div>
        <div className="muted" style={{ marginTop: 6 }}>
          Scanner hits <code>/manager/html</code> using HTTP Basic auth. Every target must be explicit (baseUrl + port + creds + timeouts). If
          HiveWatch runs in Docker, <code>localhost</code> means the container, so use the dummy-stack container hostnames (see dummy-stack README).
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
            {state.targets.length === 0 ? <div className="muted">No Tomcat targets yet.</div> : null}
            {state.targets.map((t) => (
              <details key={t.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ fontWeight: 700 }}>{t.name}</div>
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
          </div>
        ) : null}

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
                .then(() =>
                  setForm((f) => ({
                    ...f,
                    name: '',
                  })),
                )
                .finally(() => setSaving(false))
            }}
          >
            <label className="field">
              <div className="fieldLabel">Name</div>
              <input
                className="fieldInput"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder="Touchpoint · payments"
                required
              />
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
