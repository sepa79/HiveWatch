import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  createActuatorTarget,
  createServer,
  createTomcatTarget,
  deleteActuatorTarget,
  deleteServer,
  deleteTomcatTarget,
  fetchActuatorTargets,
  fetchEnvironmentStatus,
  fetchServers,
  fetchTomcatTargets,
  updateActuatorTarget,
  updateServer,
  updateTomcatTarget,
  type ActuatorTarget,
  type ActuatorTargetCreateRequest,
  type EnvironmentStatus,
  type Server,
  type ServerCreateRequest,
  type TomcatTarget,
  type TomcatTargetCreateRequest,
  type TomcatRole,
} from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; servers: Server[]; targets: TomcatTarget[]; actuatorTargets: ActuatorTarget[]; status: EnvironmentStatus }
  | { kind: 'error'; message: string }

export function EnvironmentDetailPage() {
  const params = useParams()
  const environmentId = params.environmentId ?? ''
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [saving, setSaving] = useState(false)
  const [savingActuator, setSavingActuator] = useState(false)
  const [savingServer, setSavingServer] = useState(false)
  const [editingServerId, setEditingServerId] = useState<string | null>(null)
  const [serverEditName, setServerEditName] = useState('')
  const [savingServerEdit, setSavingServerEdit] = useState(false)

  const [editingTomcatTargetId, setEditingTomcatTargetId] = useState<string | null>(null)
  const [tomcatEditForm, setTomcatEditForm] = useState<TomcatTargetCreateRequest | null>(null)
  const [savingTomcatEdit, setSavingTomcatEdit] = useState(false)

  const [editingActuatorTargetId, setEditingActuatorTargetId] = useState<string | null>(null)
  const [actuatorEditForm, setActuatorEditForm] = useState<ActuatorTargetCreateRequest | null>(null)
  const [savingActuatorEdit, setSavingActuatorEdit] = useState(false)

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

  const [actuatorForm, setActuatorForm] = useState<ActuatorTargetCreateRequest>({
    serverId: '',
    role: 'PAYMENTS',
    baseUrl: 'http://hc-dummy-nft-01-docker-swarm-microservices',
    port: 8080,
    profile: 'payments',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  })

  const [serverForm, setServerForm] = useState<ServerCreateRequest>({ name: '' })

  const refresh = (signal?: AbortSignal) =>
    Promise.all([
      fetchServers(environmentId, signal),
      fetchTomcatTargets(environmentId, signal),
      fetchActuatorTargets(environmentId, signal),
      fetchEnvironmentStatus(environmentId, signal),
    ])
      .then(([servers, targets, actuatorTargets, status]) => setState({ kind: 'ready', servers, targets, actuatorTargets, status }))
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

  useEffect(() => {
    if (state.kind !== 'ready') return
    if (actuatorForm.serverId) return
    if (state.servers.length === 0) return
    const swarm = state.servers.find((s) => s.name.toLowerCase().includes('docker swarm'))
    setActuatorForm((f) => ({ ...f, serverId: (swarm ?? state.servers[0]).id }))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state])

  const title = useMemo(() => {
    if (state.kind === 'ready')
      return `Environment · ${state.servers.length} servers · ${state.targets.length} Tomcat targets · ${state.actuatorTargets.length} microservices`
    return 'Environment'
  }, [state])

  const pill = (t: TomcatTarget) => {
    if (!t.state) return <div className="pill" data-kind="missing">UNKNOWN</div>
    if (t.state.outcomeKind === 'SUCCESS') return <div className="pill" data-kind="ok">OK</div>
    return <div className="pill" data-kind="alert">BLOCK</div>
  }

  const actuatorPill = (t: ActuatorTarget) => {
    if (!t.state) return <div className="pill" data-kind="missing">UNKNOWN</div>
    if (t.state.outcomeKind === 'SUCCESS' && (t.state.healthStatus ?? '').toUpperCase() === 'UP') {
      return <div className="pill" data-kind="ok">UP</div>
    }
    if (t.state.outcomeKind === 'SUCCESS') return <div className="pill" data-kind="alert">{t.state.healthStatus ?? 'DOWN'}</div>
    return <div className="pill" data-kind="alert">ERROR</div>
  }

  const decisionPill = (status: EnvironmentStatus) => {
    switch (status.verdict) {
      case 'OK':
        return <div className="pill" data-kind="ok">OK</div>
      case 'WARN':
        return <div className="pill" data-kind="warn">WARN</div>
      case 'BLOCK':
        return <div className="pill" data-kind="alert">BLOCK</div>
      default:
        return <div className="pill" data-kind="missing">UNKNOWN</div>
    }
  }

  const roleLabel = (role: TomcatRole) => {
    const labels: Record<TomcatRole, string> = {
      PAYMENTS: 'payments',
      SERVICES: 'services',
      AUTH: 'auth',
    }
    return labels[role]
  }

  const formatCpu = (cpuUsage: number | null) => {
    if (cpuUsage == null) return '—'
    const pct = Math.round(cpuUsage * 1000) / 10
    return `${pct}%`
  }

  const formatBytes = (bytes: number | null) => {
    if (bytes == null) return '—'
    if (!Number.isFinite(bytes)) return String(bytes)
    const mb = bytes / (1024 * 1024)
    if (mb < 1024) return `${Math.round(mb)} MB`
    const gb = mb / 1024
    return `${Math.round(gb * 10) / 10} GB`
  }

  const formatTs = (iso: string | null) => {
    if (!iso) return '—'
    const date = new Date(iso)
    if (Number.isNaN(date.getTime())) return iso
    return date.toLocaleString()
  }

  const beginServerEdit = (server: Server) => {
    setEditingServerId(server.id)
    setServerEditName(server.name)
  }

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
        <Link to="/environments">← Back to environments</Link>
      </div>

      <div className="card" style={{ marginTop: 12 }}>
        {state.kind === 'ready' ? (
          <div className="card" style={{ padding: 12, marginBottom: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div style={{ fontWeight: 900 }}>Decision</div>
              {decisionPill(state.status)}
              <div className="muted" style={{ marginLeft: 'auto' }}>
                evaluated: {formatTs(state.status.evaluatedAt)}
              </div>
            </div>
            {state.status.issues.length > 0 ? (
              <div className="kv" style={{ marginTop: 10 }}>
                {state.status.issues.slice(0, 10).map((i) => (
                  <div key={i.targetId + i.label} style={{ display: 'contents' }}>
                    <div className="k">
                      {i.serverName} · {i.label}
                    </div>
                    <div className="v">
                      <span className="muted">{i.severity}</span> · {i.message}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="muted" style={{ marginTop: 10 }}>
                No issues.
              </div>
            )}
          </div>
        ) : null}

        <div className="h2">Topology: Environment → Server → Tomcats</div>
        <div className="muted" style={{ marginTop: 6 }}>
          Every Tomcat target is explicitly assigned to a Server and Role (<code>payments</code>/<code>services</code>/<code>auth</code>). Scanner hits{' '}
          <code>/manager/html</code> using HTTP Basic auth.
        </div>
        <div className="muted" style={{ marginTop: 6 }}>
          If HiveWatch runs in Docker, <code>localhost</code> means the container, so use the dummy-stack container hostnames (see dummy-stack README).
        </div>
        <div className="muted" style={{ marginTop: 6 }}>
          Scans run automatically on a background schedule (no manual scanning from UI).
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
              const actuatorTargets = state.actuatorTargets
                .filter((t) => t.serverId === server.id)
                .slice()
                .sort((a, b) => (a.profile + a.role).localeCompare(b.profile + b.role))

              return (
                <details key={server.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                  <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ fontWeight: 800 }}>{server.name}</div>
                    <div className="muted" style={{ marginLeft: 'auto' }}>
                      {targets.length} Tomcat · {actuatorTargets.length} microservices
                    </div>
                    <button
                      type="button"
                      className="button"
                      onClick={(e) => {
                        e.preventDefault()
                        e.stopPropagation()
                        beginServerEdit(server)
                      }}
                    >
                      Rename
                    </button>
                    <button
                      type="button"
                      className="button"
                      onClick={(e) => {
                        e.preventDefault()
                        e.stopPropagation()
                        if (!window.confirm(`Delete server '${server.name}'? This will also delete its targets.`)) return
                        const controller = new AbortController()
                        deleteServer(environmentId, server.id, controller.signal).then(() => refresh())
                      }}
                    >
                      Delete
                    </button>
                  </summary>

                  {editingServerId === server.id ? (
                    <div className="card" style={{ marginTop: 10, padding: 12, maxWidth: 720 }}>
                      <div className="h2">Rename server</div>
                      <form
                        style={{ marginTop: 10, display: 'flex', gap: 10, alignItems: 'end' }}
                        onSubmit={(e) => {
                          e.preventDefault()
                          const controller = new AbortController()
                          setSavingServerEdit(true)
                          updateServer(environmentId, server.id, { name: serverEditName }, controller.signal)
                            .then(() => refresh())
                            .then(() => setEditingServerId(null))
                            .finally(() => setSavingServerEdit(false))
                        }}
                      >
                        <label className="field" style={{ flex: 1 }}>
                          <div className="fieldLabel">Name</div>
                          <input
                            className="fieldInput"
                            value={serverEditName}
                            onChange={(e) => setServerEditName(e.target.value)}
                            required
                          />
                        </label>
                        <button type="submit" className="button" disabled={savingServerEdit}>
                          {savingServerEdit ? 'Saving…' : 'Save'}
                        </button>
                        <button
                          type="button"
                          className="button"
                          onClick={() => {
                            setEditingServerId(null)
                          }}
                        >
                          Cancel
                        </button>
                      </form>
                    </div>
                  ) : null}

                  {targets.length === 0 ? <div className="muted" style={{ marginTop: 10 }}>No targets.</div> : null}

                  {targets.map((t) => (
                    <details key={t.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                      <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ fontWeight: 700 }}>{roleLabel(t.role)}</div>
                        {pill(t)}
                        <div className="muted" style={{ marginLeft: 'auto' }}>
                          {t.baseUrl}:{t.port}
                        </div>
                        <button
                          type="button"
                          className="button"
                          onClick={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            beginTomcatEdit(t)
                          }}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="button"
                          onClick={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            if (!window.confirm(`Delete Tomcat target '${server.name} · ${roleLabel(t.role)}'?`)) return
                            const controller = new AbortController()
                            deleteTomcatTarget(environmentId, t.id, controller.signal).then(() => refresh())
                          }}
                        >
                          Delete
                        </button>
                      </summary>

                      <div className="kv" style={{ marginTop: 10 }}>
                        <div className="k">targetId</div>
                        <div className="v">{t.id}</div>
                        <div className="k">lastScan</div>
                        <div className="v">{t.state ? formatTs(t.state.scannedAt) : '—'}</div>
                        <div className="k">webapps</div>
                        <div className="v">{t.state ? t.state.webapps.length : 0}</div>
                        <div className="k">username</div>
                        <div className="v">{t.username}</div>
                        <div className="k">timeouts</div>
                        <div className="v">
                          {t.connectTimeoutMs}ms / {t.requestTimeoutMs}ms
                        </div>
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
                            <li key={w.path}>
                              <code>
                                {w.name}
                                {w.version ? `##${w.version}` : ''}
                              </code>
                            </li>
                          ))}
                        </ul>
                      ) : null}

                      {editingTomcatTargetId === t.id && tomcatEditForm ? (
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
                              updateTomcatTarget(environmentId, t.id, tomcatEditForm, controller.signal)
                                .then(() => refresh())
                                .then(() => setEditingTomcatTargetId(null))
                                .finally(() => setSavingTomcatEdit(false))
                            }}
                          >
                            <label className="field">
                              <div className="fieldLabel">Server</div>
                              <select
                                className="fieldInput"
                                value={tomcatEditForm.serverId}
                                onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, serverId: e.target.value } : f))}
                                required
                              >
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
                                value={tomcatEditForm.role}
                                onChange={(e) =>
                                  setTomcatEditForm((f) => (f ? { ...f, role: e.target.value as TomcatRole } : f))
                                }
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
                                value={tomcatEditForm.baseUrl}
                                onChange={(e) => setTomcatEditForm((f) => (f ? { ...f, baseUrl: e.target.value } : f))}
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Port</div>
                              <input
                                className="fieldInput"
                                type="number"
                                value={tomcatEditForm.port}
                                onChange={(e) =>
                                  setTomcatEditForm((f) => (f ? { ...f, port: Number(e.target.value) } : f))
                                }
                                min={1}
                                max={65535}
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Username</div>
                              <input
                                className="fieldInput"
                                value={tomcatEditForm.username}
                                onChange={(e) =>
                                  setTomcatEditForm((f) => (f ? { ...f, username: e.target.value } : f))
                                }
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Password</div>
                              <input
                                className="fieldInput"
                                type="password"
                                value={tomcatEditForm.password}
                                onChange={(e) =>
                                  setTomcatEditForm((f) => (f ? { ...f, password: e.target.value } : f))
                                }
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Connect timeout (ms)</div>
                              <input
                                className="fieldInput"
                                type="number"
                                value={tomcatEditForm.connectTimeoutMs}
                                onChange={(e) =>
                                  setTomcatEditForm((f) => (f ? { ...f, connectTimeoutMs: Number(e.target.value) } : f))
                                }
                                min={1}
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Request timeout (ms)</div>
                              <input
                                className="fieldInput"
                                type="number"
                                value={tomcatEditForm.requestTimeoutMs}
                                onChange={(e) =>
                                  setTomcatEditForm((f) => (f ? { ...f, requestTimeoutMs: Number(e.target.value) } : f))
                                }
                                min={1}
                                required
                              />
                            </label>

                            <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
                              <button type="submit" className="button" disabled={savingTomcatEdit}>
                                {savingTomcatEdit ? 'Saving…' : 'Save changes'}
                              </button>
                              <button
                                type="button"
                                className="button"
                                onClick={() => {
                                  setEditingTomcatTargetId(null)
                                }}
                              >
                                Cancel
                              </button>
                            </div>
                          </form>
                        </div>
                      ) : null}
                    </details>
                  ))}

                  <div style={{ marginTop: 12, fontWeight: 800 }}>Microservices (Actuator)</div>
                  {actuatorTargets.length === 0 ? (
                    <div className="muted" style={{ marginTop: 10 }}>
                      No microservices.
                    </div>
                  ) : null}

                  {actuatorTargets.map((t) => (
                    <details key={t.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                      <summary style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                        <div style={{ fontWeight: 700 }}>{t.profile}</div>
                        {actuatorPill(t)}
                        <div className="muted" style={{ marginLeft: 'auto' }}>
                          {t.baseUrl}:{t.port}
                        </div>
                        <button
                          type="button"
                          className="button"
                          onClick={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            beginActuatorEdit(t)
                          }}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="button"
                          onClick={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            if (!window.confirm(`Delete microservice '${server.name} · ${t.profile}'?`)) return
                            const controller = new AbortController()
                            deleteActuatorTarget(environmentId, t.id, controller.signal).then(() => refresh())
                          }}
                        >
                          Delete
                        </button>
                      </summary>

                      <div className="kv" style={{ marginTop: 10 }}>
                        <div className="k">targetId</div>
                        <div className="v">{t.id}</div>
                        <div className="k">lastScan</div>
                        <div className="v">{t.state ? formatTs(t.state.scannedAt) : '—'}</div>
                        <div className="k">health</div>
                        <div className="v">{t.state?.healthStatus ?? '—'}</div>
                        <div className="k">app</div>
                        <div className="v">{t.state?.appName ?? '—'}</div>
                        <div className="k">version</div>
                        <div className="v">{t.state?.buildVersion ?? '—'}</div>
                        <div className="k">cpu</div>
                        <div className="v">{formatCpu(t.state?.cpuUsage ?? null)}</div>
                        <div className="k">memory</div>
                        <div className="v">{formatBytes(t.state?.memoryUsedBytes ?? null)}</div>
                        <div className="k">timeouts</div>
                        <div className="v">
                          {t.connectTimeoutMs}ms / {t.requestTimeoutMs}ms
                        </div>
                        {t.state && t.state.outcomeKind === 'ERROR' ? (
                          <>
                            <div className="k">error</div>
                            <div className="v">
                              {t.state.errorKind}: {t.state.errorMessage}
                            </div>
                          </>
                        ) : null}
                      </div>

                      {editingActuatorTargetId === t.id && actuatorEditForm ? (
                        <div className="card" style={{ marginTop: 12, padding: 12 }}>
                          <div className="h2">Edit microservice (Actuator target)</div>
                          <form
                            style={{ marginTop: 10, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}
                            onSubmit={(e) => {
                              e.preventDefault()
                              const controller = new AbortController()
                              setSavingActuatorEdit(true)
                              updateActuatorTarget(environmentId, t.id, actuatorEditForm, controller.signal)
                                .then(() => refresh())
                                .then(() => setEditingActuatorTargetId(null))
                                .finally(() => setSavingActuatorEdit(false))
                            }}
                          >
                            <label className="field">
                              <div className="fieldLabel">Server</div>
                              <select
                                className="fieldInput"
                                value={actuatorEditForm.serverId}
                                onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, serverId: e.target.value } : f))}
                                required
                              >
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
                                value={actuatorEditForm.role}
                                onChange={(e) =>
                                  setActuatorEditForm((f) => (f ? { ...f, role: e.target.value as TomcatRole } : f))
                                }
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
                                value={actuatorEditForm.baseUrl}
                                onChange={(e) => setActuatorEditForm((f) => (f ? { ...f, baseUrl: e.target.value } : f))}
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Port</div>
                              <input
                                className="fieldInput"
                                type="number"
                                value={actuatorEditForm.port}
                                onChange={(e) =>
                                  setActuatorEditForm((f) => (f ? { ...f, port: Number(e.target.value) } : f))
                                }
                                min={1}
                                max={65535}
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Profile</div>
                              <input
                                className="fieldInput"
                                value={actuatorEditForm.profile}
                                onChange={(e) =>
                                  setActuatorEditForm((f) => (f ? { ...f, profile: e.target.value } : f))
                                }
                                required
                              />
                            </label>
                            <div />
                            <label className="field">
                              <div className="fieldLabel">Connect timeout (ms)</div>
                              <input
                                className="fieldInput"
                                type="number"
                                value={actuatorEditForm.connectTimeoutMs}
                                onChange={(e) =>
                                  setActuatorEditForm((f) => (f ? { ...f, connectTimeoutMs: Number(e.target.value) } : f))
                                }
                                min={1}
                                required
                              />
                            </label>
                            <label className="field">
                              <div className="fieldLabel">Request timeout (ms)</div>
                              <input
                                className="fieldInput"
                                type="number"
                                value={actuatorEditForm.requestTimeoutMs}
                                onChange={(e) =>
                                  setActuatorEditForm((f) => (f ? { ...f, requestTimeoutMs: Number(e.target.value) } : f))
                                }
                                min={1}
                                required
                              />
                            </label>

                            <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
                              <button type="submit" className="button" disabled={savingActuatorEdit}>
                                {savingActuatorEdit ? 'Saving…' : 'Save changes'}
                              </button>
                              <button
                                type="button"
                                className="button"
                                onClick={() => {
                                  setEditingActuatorTargetId(null)
                                }}
                              >
                                Cancel
                              </button>
                            </div>
                          </form>
                        </div>
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

        <div className="card" style={{ marginTop: 12, padding: 12, maxWidth: 820 }}>
          <div className="h2">Add microservice (Actuator target)</div>
          <div className="muted" style={{ marginTop: 6 }}>
            Scanner calls <code>/{'{'}profile{'}'}/actuator/*</code>. <code>baseUrl</code> must be absolute and must not include port/path.
          </div>

          <form
            style={{ marginTop: 10, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}
            onSubmit={(e) => {
              e.preventDefault()
              const controller = new AbortController()
              setSavingActuator(true)
              createActuatorTarget(environmentId, actuatorForm, controller.signal)
                .then(() => refresh())
                .finally(() => setSavingActuator(false))
            }}
          >
            <label className="field">
              <div className="fieldLabel">Server</div>
              <select
                className="fieldInput"
                value={actuatorForm.serverId}
                onChange={(e) => setActuatorForm((f) => ({ ...f, serverId: e.target.value }))}
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
                value={actuatorForm.role}
                onChange={(e) => setActuatorForm((f) => ({ ...f, role: e.target.value as TomcatRole }))}
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
                value={actuatorForm.baseUrl}
                onChange={(e) => setActuatorForm((f) => ({ ...f, baseUrl: e.target.value }))}
                placeholder="http://hc-dummy-nft-01-docker-swarm-microservices"
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Port</div>
              <input
                className="fieldInput"
                type="number"
                value={actuatorForm.port}
                onChange={(e) => setActuatorForm((f) => ({ ...f, port: Number(e.target.value) }))}
                min={1}
                max={65535}
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Profile</div>
              <input
                className="fieldInput"
                value={actuatorForm.profile}
                onChange={(e) => setActuatorForm((f) => ({ ...f, profile: e.target.value }))}
                placeholder="payments"
                required
              />
            </label>
            <div />
            <label className="field">
              <div className="fieldLabel">Connect timeout (ms)</div>
              <input
                className="fieldInput"
                type="number"
                value={actuatorForm.connectTimeoutMs}
                onChange={(e) => setActuatorForm((f) => ({ ...f, connectTimeoutMs: Number(e.target.value) }))}
                min={1}
                required
              />
            </label>
            <label className="field">
              <div className="fieldLabel">Request timeout (ms)</div>
              <input
                className="fieldInput"
                type="number"
                value={actuatorForm.requestTimeoutMs}
                onChange={(e) => setActuatorForm((f) => ({ ...f, requestTimeoutMs: Number(e.target.value) }))}
                min={1}
                required
              />
            </label>

            <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10 }}>
              <button type="submit" className="button" disabled={savingActuator}>
                {savingActuator ? 'Saving…' : 'Add microservice'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
