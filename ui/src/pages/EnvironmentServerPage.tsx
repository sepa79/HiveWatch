import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createActuatorTarget,
  createTomcatTarget,
  deleteActuatorTarget,
  deleteTomcatTarget,
  deleteServer,
  fetchActuatorTargets,
  fetchServers,
  fetchTomcatTargets,
  updateActuatorTarget,
  updateServer,
  updateTomcatTarget,
  type ActuatorTarget,
  type ActuatorTargetCreateRequest,
  type Server,
  type TomcatRole,
  type TomcatTarget,
  type TomcatTargetCreateRequest,
} from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; server: Server; tomcats: TomcatTarget[]; microservices: ActuatorTarget[] }
  | { kind: 'error'; message: string }

function formatTs(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString()
}

function roleLabel(role: TomcatRole) {
  const labels: Record<TomcatRole, string> = {
    PAYMENTS: 'payments',
    SERVICES: 'services',
    AUTH: 'auth',
  }
  return labels[role]
}

export function EnvironmentServerPage() {
  const params = useParams()
  const navigate = useNavigate()

  const environmentId = (params.environmentId ?? '').trim()
  const serverId = (params.serverId ?? '').trim()

  const [state, setState] = useState<LoadState>({ kind: 'loading' })

  const [editingServer, setEditingServer] = useState(false)
  const [serverNameDraft, setServerNameDraft] = useState('')
  const [savingServerName, setSavingServerName] = useState(false)

  const [editingTomcatTargetId, setEditingTomcatTargetId] = useState<string | null>(null)
  const [tomcatEditForm, setTomcatEditForm] = useState<TomcatTargetCreateRequest | null>(null)
  const [savingTomcatEdit, setSavingTomcatEdit] = useState(false)

  const [editingActuatorTargetId, setEditingActuatorTargetId] = useState<string | null>(null)
  const [actuatorEditForm, setActuatorEditForm] = useState<ActuatorTargetCreateRequest | null>(null)
  const [savingActuatorEdit, setSavingActuatorEdit] = useState(false)

  const [savingTomcatCreate, setSavingTomcatCreate] = useState(false)
  const [savingActuatorCreate, setSavingActuatorCreate] = useState(false)

  const [tomcatForm, setTomcatForm] = useState<TomcatTargetCreateRequest>({
    serverId,
    role: 'PAYMENTS',
    baseUrl: 'http://hc-dummy-nft-01-touchpoint-tomcats',
    port: 8081,
    username: 'hc-manager',
    password: 'hc-manager-pass',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  })

  const [actuatorForm, setActuatorForm] = useState<ActuatorTargetCreateRequest>({
    serverId,
    role: 'PAYMENTS',
    baseUrl: 'http://hc-dummy-nft-01-docker-swarm-microservices',
    port: 8080,
    profile: 'payments',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  })

  useEffect(() => {
    setTomcatForm((f) => ({ ...f, serverId }))
    setActuatorForm((f) => ({ ...f, serverId }))
  }, [serverId])

  const refresh = useCallback(
    (signal?: AbortSignal) => {
      if (!environmentId || !serverId) {
        setState({ kind: 'error', message: 'Missing environmentId/serverId' })
        return Promise.resolve()
      }
      setState({ kind: 'loading' })
      return Promise.all([
        fetchServers(environmentId, signal),
        fetchTomcatTargets(environmentId, signal),
        fetchActuatorTargets(environmentId, signal),
      ])
        .then(([servers, tomcats, microservices]) => {
          const srv = servers.find((s) => s.id === serverId)
          if (!srv) {
            setState({ kind: 'error', message: 'Server not found' })
            return
          }
          setState({
            kind: 'ready',
            server: srv,
            tomcats: tomcats.filter((t) => t.serverId === serverId),
            microservices: microservices.filter((t) => t.serverId === serverId),
          })
          setServerNameDraft(srv.name)
        })
        .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    },
    [environmentId, serverId],
  )

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Server · ${state.server.name} · ${state.tomcats.length} Tomcats · ${state.microservices.length} microservices`
    return 'Server'
  }, [state])

  const beginTomcatEdit = (t: TomcatTarget) => {
    setEditingTomcatTargetId(t.id)
    setTomcatEditForm({
      serverId: t.serverId,
      role: t.role,
      baseUrl: t.baseUrl,
      port: t.port,
      username: t.username,
      password: '',
      connectTimeoutMs: t.connectTimeoutMs,
      requestTimeoutMs: t.requestTimeoutMs,
    })
  }

  const beginActuatorEdit = (t: ActuatorTarget) => {
    setEditingActuatorTargetId(t.id)
    setActuatorEditForm({
      serverId: t.serverId,
      role: t.role,
      baseUrl: t.baseUrl,
      port: t.port,
      profile: t.profile,
      connectTimeoutMs: t.connectTimeoutMs,
      requestTimeoutMs: t.requestTimeoutMs,
    })
  }

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">
        <Link to={`/environments/${encodeURIComponent(environmentId)}/overview`}>← Back to environment</Link>
      </div>

      <details className="card" style={{ marginTop: 12, padding: 12 }}>
        <summary style={{ cursor: 'pointer', fontWeight: 900 }}>Help</summary>
        <div className="muted" style={{ marginTop: 8 }}>
          This screen configures a single Server: rename/delete server, and add/edit/remove Tomcat targets and microservices (Actuator targets).
        </div>
      </details>

      {state.kind === 'loading' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div className="muted">Loading…</div>
        </div>
      ) : null}
      {state.kind === 'error' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div className="muted">Error: {state.message}</div>
        </div>
      ) : null}

      {state.kind === 'ready' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div className="h2" style={{ margin: 0 }}>
              {state.server.name}
            </div>
            <div className="muted">
              <code>{state.server.id}</code>
            </div>
            {!editingServer ? (
              <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={() => setEditingServer(true)}>
                Rename
              </button>
            ) : null}
            <button
              type="button"
              className="button"
              onClick={() => {
                if (!window.confirm(`Delete server '${state.server.name}'? This will also delete its targets.`)) return
                const controller = new AbortController()
                deleteServer(environmentId, state.server.id, controller.signal)
                  .then(() => navigate(`/environments/${encodeURIComponent(environmentId)}/overview`))
                  .catch((e) => window.alert(e instanceof Error ? e.message : 'Request failed'))
              }}
            >
              Delete
            </button>
          </div>

          {editingServer ? (
            <div style={{ display: 'flex', gap: 10, alignItems: 'end', marginTop: 10, maxWidth: 720 }}>
              <label className="field" style={{ flex: 1 }}>
                <div className="fieldLabel">Name</div>
                <input className="fieldInput" value={serverNameDraft} onChange={(e) => setServerNameDraft(e.target.value)} required />
              </label>
              <button
                type="button"
                className="button"
                disabled={savingServerName}
                onClick={() => {
                  const next = serverNameDraft.trim()
                  if (!next) {
                    window.alert('Name is required')
                    return
                  }
                  setSavingServerName(true)
                  const controller = new AbortController()
                  updateServer(environmentId, state.server.id, { name: next }, controller.signal)
                    .then(() => refresh(controller.signal))
                    .then(() => setEditingServer(false))
                    .catch((e) => window.alert(e instanceof Error ? e.message : 'Request failed'))
                    .finally(() => setSavingServerName(false))
                }}
              >
                {savingServerName ? 'Saving…' : 'Save'}
              </button>
              <button type="button" className="button" disabled={savingServerName} onClick={() => { setEditingServer(false); setServerNameDraft(state.server.name) }}>
                Cancel
              </button>
            </div>
          ) : null}
        </div>
      ) : null}

      {state.kind === 'ready' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div className="h2" style={{ margin: 0 }}>
            Tomcat targets
          </div>
          <div className="muted" style={{ marginTop: 6 }}>
            Scanner hits <code>/manager/html</code> using HTTP Basic auth. Scans run automatically on a background schedule (no manual scanning from UI).
          </div>

          <div className="tableWrap" style={{ marginTop: 10 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Role</th>
                  <th>Endpoint</th>
                  <th>User</th>
                  <th>Last scan</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {state.tomcats.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="muted">
                      No Tomcat targets.
                    </td>
                  </tr>
                ) : null}
                {state.tomcats
                  .slice()
                  .sort((a, b) => a.role.localeCompare(b.role))
                  .map((t) => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: 800 }}>{roleLabel(t.role)}</td>
                      <td className="muted">
                        {t.baseUrl}:{t.port}
                      </td>
                      <td className="muted">{t.username}</td>
                      <td className="muted">{formatTs(t.state?.scannedAt ?? null)}</td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <button type="button" className="button" onClick={() => beginTomcatEdit(t)}>
                          Edit
                        </button>
                        <button
                          type="button"
                          className="button"
                          onClick={() => {
                            if (!window.confirm(`Delete Tomcat target '${roleLabel(t.role)}'?`)) return
                            const controller = new AbortController()
                            deleteTomcatTarget(environmentId, t.id, controller.signal).then(() => refresh())
                          }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>

          {editingTomcatTargetId && tomcatEditForm ? (
            <div className="card" style={{ marginTop: 12, padding: 12 }}>
              <div className="h2">Edit Tomcat target</div>
              <div className="muted" style={{ marginTop: 6 }}>
                Password is required on update (stored in DB; not returned by API).
              </div>
              <form
                style={{ marginTop: 10, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}
                onSubmit={(e) => {
                  e.preventDefault()
                  if (!tomcatEditForm.password.trim()) {
                    window.alert('Password is required')
                    return
                  }
                  const controller = new AbortController()
                  setSavingTomcatEdit(true)
                  updateTomcatTarget(environmentId, editingTomcatTargetId, tomcatEditForm, controller.signal)
                    .then(() => refresh())
                    .then(() => setEditingTomcatTargetId(null))
                    .finally(() => setSavingTomcatEdit(false))
                }}
              >
                <label className="field">
                  <div className="fieldLabel">Role</div>
                  <select className="fieldInput" value={tomcatEditForm.role} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, role: e.target.value as TomcatRole } : f))} required>
                    <option value="PAYMENTS">payments</option>
                    <option value="SERVICES">services</option>
                    <option value="AUTH">auth</option>
                  </select>
                </label>
                <div />
                <label className="field">
                  <div className="fieldLabel">Base URL</div>
                  <input className="fieldInput" value={tomcatEditForm.baseUrl} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, baseUrl: e.target.value } : f))} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Port</div>
                  <input className="fieldInput" type="number" value={tomcatEditForm.port} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, port: Number(e.target.value) } : f))} min={1} max={65535} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Username</div>
                  <input className="fieldInput" value={tomcatEditForm.username} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, username: e.target.value } : f))} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Password</div>
                  <input className="fieldInput" type="password" value={tomcatEditForm.password} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, password: e.target.value } : f))} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Connect timeout (ms)</div>
                  <input className="fieldInput" type="number" value={tomcatEditForm.connectTimeoutMs} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, connectTimeoutMs: Number(e.target.value) } : f))} min={1} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Request timeout (ms)</div>
                  <input className="fieldInput" type="number" value={tomcatEditForm.requestTimeoutMs} onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, requestTimeoutMs: Number(e.target.value) } : f))} min={1} required />
                </label>
                <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
                  <button type="submit" className="button" disabled={savingTomcatEdit}>
                    {savingTomcatEdit ? 'Saving…' : 'Save changes'}
                  </button>
                  <button type="button" className="button" onClick={() => setEditingTomcatTargetId(null)}>
                    Cancel
                  </button>
                </div>
              </form>
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
                setSavingTomcatCreate(true)
                createTomcatTarget(environmentId, { ...tomcatForm, serverId }, controller.signal)
                  .then(() => refresh())
                  .finally(() => setSavingTomcatCreate(false))
              }}
            >
              <label className="field">
                <div className="fieldLabel">Role</div>
                <select className="fieldInput" value={tomcatForm.role} onChange={(e) => setTomcatForm((f) => ({ ...f, role: e.target.value as TomcatRole }))} required>
                  <option value="PAYMENTS">payments</option>
                  <option value="SERVICES">services</option>
                  <option value="AUTH">auth</option>
                </select>
              </label>
              <div />
              <label className="field">
                <div className="fieldLabel">Base URL</div>
                <input className="fieldInput" value={tomcatForm.baseUrl} onChange={(e) => setTomcatForm((f) => ({ ...f, baseUrl: e.target.value }))} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Port</div>
                <input className="fieldInput" type="number" value={tomcatForm.port} onChange={(e) => setTomcatForm((f) => ({ ...f, port: Number(e.target.value) }))} min={1} max={65535} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Username</div>
                <input className="fieldInput" value={tomcatForm.username} onChange={(e) => setTomcatForm((f) => ({ ...f, username: e.target.value }))} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Password</div>
                <input className="fieldInput" type="password" value={tomcatForm.password} onChange={(e) => setTomcatForm((f) => ({ ...f, password: e.target.value }))} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Connect timeout (ms)</div>
                <input className="fieldInput" type="number" value={tomcatForm.connectTimeoutMs} onChange={(e) => setTomcatForm((f) => ({ ...f, connectTimeoutMs: Number(e.target.value) }))} min={1} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Request timeout (ms)</div>
                <input className="fieldInput" type="number" value={tomcatForm.requestTimeoutMs} onChange={(e) => setTomcatForm((f) => ({ ...f, requestTimeoutMs: Number(e.target.value) }))} min={1} required />
              </label>
              <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
                <button type="submit" className="button" disabled={savingTomcatCreate}>
                  {savingTomcatCreate ? 'Saving…' : 'Add target'}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {state.kind === 'ready' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div className="h2" style={{ margin: 0 }}>
            Microservices (Actuator targets)
          </div>
          <div className="muted" style={{ marginTop: 6 }}>
            Scanner calls <code>/{'{'}profile{'}'}/actuator/*</code>. <code>baseUrl</code> must be absolute and must not include port/path.
          </div>

          <div className="tableWrap" style={{ marginTop: 10 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Profile</th>
                  <th>Endpoint</th>
                  <th>Last scan</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {state.microservices.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="muted">
                      No microservices.
                    </td>
                  </tr>
                ) : null}
                {state.microservices
                  .slice()
                  .sort((a, b) => a.profile.localeCompare(b.profile))
                  .map((t) => (
                    <tr key={t.id}>
                      <td style={{ fontWeight: 800 }}>{t.profile}</td>
                      <td className="muted">
                        {t.baseUrl}:{t.port}
                      </td>
                      <td className="muted">{formatTs(t.state?.scannedAt ?? null)}</td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <button type="button" className="button" onClick={() => beginActuatorEdit(t)}>
                          Edit
                        </button>
                        <button
                          type="button"
                          className="button"
                          onClick={() => {
                            if (!window.confirm(`Delete microservice '${t.profile}'?`)) return
                            const controller = new AbortController()
                            deleteActuatorTarget(environmentId, t.id, controller.signal).then(() => refresh())
                          }}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>

          {editingActuatorTargetId && actuatorEditForm ? (
            <div className="card" style={{ marginTop: 12, padding: 12 }}>
              <div className="h2">Edit microservice</div>
              <form
                style={{ marginTop: 10, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}
                onSubmit={(e) => {
                  e.preventDefault()
                  const controller = new AbortController()
                  setSavingActuatorEdit(true)
                  updateActuatorTarget(environmentId, editingActuatorTargetId, actuatorEditForm, controller.signal)
                    .then(() => refresh())
                    .then(() => setEditingActuatorTargetId(null))
                    .finally(() => setSavingActuatorEdit(false))
                }}
              >
                <label className="field">
                  <div className="fieldLabel">Role</div>
                  <select className="fieldInput" value={actuatorEditForm.role} onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, role: e.target.value as TomcatRole } : f))} required>
                    <option value="PAYMENTS">payments</option>
                    <option value="SERVICES">services</option>
                    <option value="AUTH">auth</option>
                  </select>
                </label>
                <div />
                <label className="field">
                  <div className="fieldLabel">Base URL</div>
                  <input className="fieldInput" value={actuatorEditForm.baseUrl} onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, baseUrl: e.target.value } : f))} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Port</div>
                  <input className="fieldInput" type="number" value={actuatorEditForm.port} onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, port: Number(e.target.value) } : f))} min={1} max={65535} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Profile</div>
                  <input className="fieldInput" value={actuatorEditForm.profile} onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, profile: e.target.value } : f))} required />
                </label>
                <div />
                <label className="field">
                  <div className="fieldLabel">Connect timeout (ms)</div>
                  <input className="fieldInput" type="number" value={actuatorEditForm.connectTimeoutMs} onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, connectTimeoutMs: Number(e.target.value) } : f))} min={1} required />
                </label>
                <label className="field">
                  <div className="fieldLabel">Request timeout (ms)</div>
                  <input className="fieldInput" type="number" value={actuatorEditForm.requestTimeoutMs} onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, requestTimeoutMs: Number(e.target.value) } : f))} min={1} required />
                </label>
                <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
                  <button type="submit" className="button" disabled={savingActuatorEdit}>
                    {savingActuatorEdit ? 'Saving…' : 'Save changes'}
                  </button>
                  <button type="button" className="button" onClick={() => setEditingActuatorTargetId(null)}>
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          ) : null}

          <div className="card" style={{ marginTop: 12, padding: 12, maxWidth: 820 }}>
            <div className="h2">Add microservice</div>
            <form
              style={{ marginTop: 10, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}
              onSubmit={(e) => {
                e.preventDefault()
                const controller = new AbortController()
                setSavingActuatorCreate(true)
                createActuatorTarget(environmentId, { ...actuatorForm, serverId }, controller.signal)
                  .then(() => refresh())
                  .finally(() => setSavingActuatorCreate(false))
              }}
            >
              <label className="field">
                <div className="fieldLabel">Role</div>
                <select className="fieldInput" value={actuatorForm.role} onChange={(e) => setActuatorForm((f) => ({ ...f, role: e.target.value as TomcatRole }))} required>
                  <option value="PAYMENTS">payments</option>
                  <option value="SERVICES">services</option>
                  <option value="AUTH">auth</option>
                </select>
              </label>
              <div />
              <label className="field">
                <div className="fieldLabel">Base URL</div>
                <input className="fieldInput" value={actuatorForm.baseUrl} onChange={(e) => setActuatorForm((f) => ({ ...f, baseUrl: e.target.value }))} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Port</div>
                <input className="fieldInput" type="number" value={actuatorForm.port} onChange={(e) => setActuatorForm((f) => ({ ...f, port: Number(e.target.value) }))} min={1} max={65535} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Profile</div>
                <input className="fieldInput" value={actuatorForm.profile} onChange={(e) => setActuatorForm((f) => ({ ...f, profile: e.target.value }))} required />
              </label>
              <div />
              <label className="field">
                <div className="fieldLabel">Connect timeout (ms)</div>
                <input className="fieldInput" type="number" value={actuatorForm.connectTimeoutMs} onChange={(e) => setActuatorForm((f) => ({ ...f, connectTimeoutMs: Number(e.target.value) }))} min={1} required />
              </label>
              <label className="field">
                <div className="fieldLabel">Request timeout (ms)</div>
                <input className="fieldInput" type="number" value={actuatorForm.requestTimeoutMs} onChange={(e) => setActuatorForm((f) => ({ ...f, requestTimeoutMs: Number(e.target.value) }))} min={1} required />
              </label>
              <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
                <button type="submit" className="button" disabled={savingActuatorCreate}>
                  {savingActuatorCreate ? 'Saving…' : 'Add microservice'}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </div>
  )
}

