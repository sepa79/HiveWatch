import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createActuatorTarget,
  createExpectedSetTemplate,
  createServer,
  createTomcatTarget,
  fetchDockerExpectedServicesSpecs,
  fetchExpectedSetTemplates,
  deleteActuatorTarget,
  deleteServer,
  deleteTomcatTarget,
  fetchActuatorTargets,
  fetchEnvironmentStatus,
  fetchServers,
  fetchTomcatExpectedWebappsSpecs,
  fetchTomcatTargets,
  replaceDockerExpectedServicesSpecs,
  replaceTomcatExpectedWebappsSpecs,
  renameAdminEnvironment,
  updateActuatorTarget,
  updateServer,
  updateTomcatTarget,
  type ActuatorTarget,
  type ActuatorTargetCreateRequest,
  type DockerExpectedServicesSpec,
  type EnvironmentStatus,
  type ExpectedSetMode,
  type ExpectedSetTemplateCreateRequest,
  type ExpectedSetTemplate,
  type Server,
  type ServerCreateRequest,
  type TomcatExpectedWebappsSpec,
  type TomcatTarget,
  type TomcatTargetCreateRequest,
  type TomcatRole,
} from '../lib/hivewatchApi'
import { useAuth } from '../lib/authContext'

type LoadState =
  | { kind: 'loading' }
  | {
      kind: 'ready'
      servers: Server[]
      targets: TomcatTarget[]
      actuatorTargets: ActuatorTarget[]
      status: EnvironmentStatus
      tomcatExpectedSpecs: TomcatExpectedWebappsSpec[]
      dockerExpectedSpecs: DockerExpectedServicesSpec[]
      tomcatTemplates: ExpectedSetTemplate[]
      dockerTemplates: ExpectedSetTemplate[]
    }
  | { kind: 'error'; message: string }

export type EnvironmentDetailView = 'overview' | 'expected-sets' | 'templates' | 'topology'

export function EnvironmentDetailPage({ view }: { view: EnvironmentDetailView }) {
  const params = useParams()
  const navigate = useNavigate()
  const environmentId = params.environmentId ?? ''
  const { state: auth } = useAuth()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [saving, setSaving] = useState(false)
  const [savingActuator, setSavingActuator] = useState(false)
  const [savingServer, setSavingServer] = useState(false)
  const [editingServerId, setEditingServerId] = useState<string | null>(null)
  const [serverEditName, setServerEditName] = useState('')
  const [savingServerEdit, setSavingServerEdit] = useState(false)

  const [tomcatExpectedSpecsDraft, setTomcatExpectedSpecsDraft] = useState<TomcatExpectedWebappsSpec[]>([])
  const [dockerExpectedSpecsDraft, setDockerExpectedSpecsDraft] = useState<DockerExpectedServicesSpec[]>([])
  const [savingExpectedTomcat, setSavingExpectedTomcat] = useState(false)
  const [savingExpectedDocker, setSavingExpectedDocker] = useState(false)
  const [expectedTomcatError, setExpectedTomcatError] = useState<string | null>(null)
  const [expectedDockerError, setExpectedDockerError] = useState<string | null>(null)

  const [creatingTomcatTemplate, setCreatingTomcatTemplate] = useState(false)
  const [creatingDockerTemplate, setCreatingDockerTemplate] = useState(false)
  const [tomcatTemplateForm, setTomcatTemplateForm] = useState({ name: '', items: '' })
  const [dockerTemplateForm, setDockerTemplateForm] = useState({ name: '', items: '' })
  const [templateError, setTemplateError] = useState<string | null>(null)

  const [editingEnvName, setEditingEnvName] = useState(false)
  const [envNameDraft, setEnvNameDraft] = useState('')
  const [savingEnvName, setSavingEnvName] = useState(false)
  const [envNameError, setEnvNameError] = useState<string | null>(null)

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
      fetchTomcatExpectedWebappsSpecs(environmentId, signal),
      fetchDockerExpectedServicesSpecs(environmentId, signal),
      fetchExpectedSetTemplates('TOMCAT_WEBAPP_PATH', signal),
      fetchExpectedSetTemplates('DOCKER_SERVICE_PROFILE', signal),
    ])
      .then(([servers, targets, actuatorTargets, status, tomcatExpectedSpecs, dockerExpectedSpecs, tomcatTemplates, dockerTemplates]) => {
        setState({
          kind: 'ready',
          servers,
          targets,
          actuatorTargets,
          status,
          tomcatExpectedSpecs,
          dockerExpectedSpecs,
          tomcatTemplates,
          dockerTemplates,
        })
        setTomcatExpectedSpecsDraft(tomcatExpectedSpecs)
        setDockerExpectedSpecsDraft(dockerExpectedSpecs)
        setExpectedTomcatError(null)
        setExpectedDockerError(null)
      })
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

  useEffect(() => {
    if (state.kind !== 'ready') return
    if (editingEnvName) return
    setEnvNameDraft(state.status.environmentName)
  }, [editingEnvName, state])

  const title = useMemo(() => {
    const suffix = (() => {
      switch (view) {
        case 'overview':
          return 'Environment'
        case 'expected-sets':
          return 'Expected sets'
        case 'templates':
          return 'Templates'
        case 'topology':
          return 'Topology'
      }
    })()
    if (state.kind === 'ready') {
      return `Environment · ${suffix} · ${state.servers.length} servers · ${state.targets.length} Tomcat targets · ${state.actuatorTargets.length} microservices`
    }
    return `Environment · ${suffix}`
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

  const parseLines = (raw: string): string[] =>
    raw
      .split('\n')
      .map((s) => s.trim())
      .filter((s) => s.length > 0)

  const setTomcatSpec = (key: { serverId: string; role: TomcatRole }, patch: Partial<TomcatExpectedWebappsSpec>) => {
    setTomcatExpectedSpecsDraft((prev) =>
      prev.map((s) => (s.serverId === key.serverId && s.role === key.role ? { ...s, ...patch } : s)),
    )
  }

  const setDockerSpec = (serverId: string, patch: Partial<DockerExpectedServicesSpec>) => {
    setDockerExpectedSpecsDraft((prev) => prev.map((s) => (s.serverId === serverId ? { ...s, ...patch } : s)))
  }

  const saveTomcatExpectedSpecs = () => {
    if (state.kind !== 'ready') return
    if (tomcatExpectedSpecsDraft.some((s) => s.mode === 'UNCONFIGURED')) {
      setExpectedTomcatError('All Tomcat expected-set specs must be configured (EXPLICIT or TEMPLATE).')
      return
    }

    setSavingExpectedTomcat(true)
    setExpectedTomcatError(null)
    const controller = new AbortController()
    replaceTomcatExpectedWebappsSpecs(
      environmentId,
      {
        specs: tomcatExpectedSpecsDraft.map((s) =>
          s.mode === 'TEMPLATE'
            ? { ...s, items: [] }
            : s,
        ),
      },
      controller.signal,
    )
      .then(() => refresh())
      .catch((e) => setExpectedTomcatError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setSavingExpectedTomcat(false))
  }

  const saveDockerExpectedSpecs = () => {
    if (state.kind !== 'ready') return
    const dockerServerIds = new Set(state.actuatorTargets.map((t) => t.serverId))
    const specs = dockerExpectedSpecsDraft.filter((s) => dockerServerIds.has(s.serverId))
    if (specs.some((s) => s.mode === 'UNCONFIGURED')) {
      setExpectedDockerError('All Docker expected-set specs must be configured (EXPLICIT or TEMPLATE).')
      return
    }

    setSavingExpectedDocker(true)
    setExpectedDockerError(null)
    const controller = new AbortController()
    replaceDockerExpectedServicesSpecs(
      environmentId,
      {
        specs: specs.map((s) =>
          s.mode === 'TEMPLATE'
            ? { ...s, items: [] }
            : s,
        ),
      },
      controller.signal,
    )
      .then(() => refresh())
      .catch((e) => setExpectedDockerError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setSavingExpectedDocker(false))
  }

  const isAdmin = auth.kind === 'ready' && auth.me.roles.includes('ADMIN')

  const beginEnvRename = () => {
    if (state.kind !== 'ready') return
    setEnvNameError(null)
    setEnvNameDraft(state.status.environmentName)
    setEditingEnvName(true)
  }

  const cancelEnvRename = () => {
    if (state.kind !== 'ready') return
    setEnvNameError(null)
    setEnvNameDraft(state.status.environmentName)
    setEditingEnvName(false)
  }

  const saveEnvRename = () => {
    if (!isAdmin) {
      setEnvNameError('Admin role is required to rename environments.')
      return
    }
    const next = envNameDraft.trim()
    if (!next) {
      setEnvNameError('Name is required.')
      return
    }
    setSavingEnvName(true)
    setEnvNameError(null)
    const controller = new AbortController()
    renameAdminEnvironment(environmentId, { name: next }, controller.signal)
      .then(() => refresh(controller.signal))
      .then(() => setEditingEnvName(false))
      .catch((e) => setEnvNameError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setSavingEnvName(false))
  }

  const createTemplate = (request: ExpectedSetTemplateCreateRequest, setCreating: (v: boolean) => void, onClear: () => void) => {
    if (!isAdmin) {
      setTemplateError('Admin role is required to create templates.')
      return
    }
    setCreating(true)
    setTemplateError(null)
    const controller = new AbortController()
    createExpectedSetTemplate(request, controller.signal)
      .then(() => {
        onClear()
        return refresh(controller.signal)
      })
      .catch((e) => setTemplateError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setCreating(false))
  }

  const onCreateTomcatTemplate = () => {
    const name = tomcatTemplateForm.name.trim()
    const items = parseLines(tomcatTemplateForm.items)
    if (!name) {
      setTemplateError('Template name is required.')
      return
    }
    if (items.length === 0) {
      setTemplateError('Template items are required (one webapp path per line).')
      return
    }
    createTemplate(
      { kind: 'TOMCAT_WEBAPP_PATH', name, items },
      setCreatingTomcatTemplate,
      () => setTomcatTemplateForm({ name: '', items: '' }),
    )
  }

  const onCreateDockerTemplate = () => {
    const name = dockerTemplateForm.name.trim()
    const items = parseLines(dockerTemplateForm.items)
    if (!name) {
      setTemplateError('Template name is required.')
      return
    }
    if (items.length === 0) {
      setTemplateError('Template items are required (one service profile per line).')
      return
    }
    createTemplate(
      { kind: 'DOCKER_SERVICE_PROFILE', name, items },
      setCreatingDockerTemplate,
      () => setDockerTemplateForm({ name: '', items: '' }),
    )
  }

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">
        <Link to="/environments">← Back to environments</Link>
      </div>

      {state.kind === 'ready' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div className="h2" style={{ margin: 0 }}>
              {editingEnvName ? 'Environment name' : state.status.environmentName}
            </div>
            <div className="muted" style={{ marginLeft: 8 }}>
              <code>{state.status.environmentId}</code>
            </div>
            {isAdmin && !editingEnvName ? (
              <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={beginEnvRename}>
                Rename
              </button>
            ) : null}
          </div>

          {editingEnvName ? (
            <div style={{ display: 'flex', gap: 10, alignItems: 'end', marginTop: 10 }}>
              <label className="field" style={{ flex: 1 }}>
                <div className="fieldLabel">Name</div>
                <input
                  className="fieldInput"
                  value={envNameDraft}
                  onChange={(e) => setEnvNameDraft(e.target.value)}
                  disabled={savingEnvName}
                  required
                />
              </label>
              <button type="button" className="button" onClick={saveEnvRename} disabled={savingEnvName}>
                {savingEnvName ? 'Saving…' : 'Save'}
              </button>
              <button type="button" className="button" onClick={cancelEnvRename} disabled={savingEnvName}>
                Cancel
              </button>
            </div>
          ) : null}

          {envNameError ? (
            <div className="muted" style={{ marginTop: 8 }}>
              Error: {envNameError}
            </div>
          ) : null}
        </div>
      ) : null}

      <details className="card" style={{ marginTop: 12, padding: 12 }}>
        <summary style={{ cursor: 'pointer', fontWeight: 900 }}>Help</summary>
        <div className="muted" style={{ marginTop: 8 }}>
          {view === 'overview' ? (
            <>Edit the environment and manage Servers. Then drill down into a Server to configure Tomcat targets and microservices.</>
          ) : null}
          {view === 'expected-sets' ? (
            <>
              Expected sets define what should exist (for missing checks on the Dashboard). Configure them per Server (open a Server and edit Expected sets there). Use{' '}
              <code>EXPLICIT</code> for a custom list, or <code>TEMPLATE</code> to pick a reusable template.
            </>
          ) : null}
          {view === 'templates' ? (
            <>
              Templates are global reusable sets (Tomcat webapp paths or Docker service profiles). After creating a template here, you can select it under the Expected
              sets tab.
            </>
          ) : null}
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

      {state.kind === 'ready' && view === 'overview' ? (
        <div style={{ marginTop: 12, display: 'grid', gap: 10 }}>
          <div className="card" style={{ padding: 12 }}>
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

          <div className="card" style={{ padding: 12 }}>
            <div className="h2" style={{ margin: 0 }}>
              Servers
            </div>
            <div className="muted" style={{ marginTop: 6 }}>
              Add/rename/delete servers here, then drill down into a server to configure targets and microservices.
            </div>

            <div className="tableWrap" style={{ marginTop: 10 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Server</th>
                    <th>Tomcats</th>
                    <th>Microservices</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {state.servers.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="muted">
                        No servers yet.
                      </td>
                    </tr>
                  ) : null}
                  {state.servers.map((server) => {
                    const tomcats = state.targets.filter((t) => t.serverId === server.id).length
                    const micro = state.actuatorTargets.filter((t) => t.serverId === server.id).length
                    const open = () =>
                      navigate(`/environments/${encodeURIComponent(environmentId)}/servers/${encodeURIComponent(server.id)}`)

                    return (
                      <tr key={server.id}>
                        <td style={{ fontWeight: 800 }}>
                          {editingServerId === server.id ? (
                            <input
                              className="fieldInput"
                              value={serverEditName}
                              onChange={(e) => setServerEditName(e.target.value)}
                              aria-label={`Server name: ${server.name}`}
                            />
                          ) : (
                            <>
                              {server.name}
                              <div className="muted" style={{ fontWeight: 500 }}>
                                <code>{server.id}</code>
                              </div>
                            </>
                          )}
                        </td>
                        <td className="muted">{tomcats}</td>
                        <td className="muted">{micro}</td>
                        <td style={{ whiteSpace: 'nowrap' }}>
                          <button type="button" className="button" onClick={open}>
                            Edit
                          </button>
                          {editingServerId === server.id ? (
                            <>
                              <button
                                type="button"
                                className="button"
                                disabled={savingServerEdit}
                                onClick={() => {
                                  const controller = new AbortController()
                                  setSavingServerEdit(true)
                                  updateServer(environmentId, server.id, { name: serverEditName }, controller.signal)
                                    .then(() => refresh())
                                    .then(() => setEditingServerId(null))
                                    .finally(() => setSavingServerEdit(false))
                                }}
                              >
                                {savingServerEdit ? 'Saving…' : 'Save'}
                              </button>
                              <button type="button" className="button" onClick={() => setEditingServerId(null)} disabled={savingServerEdit}>
                                Cancel
                              </button>
                            </>
                          ) : (
                            <button type="button" className="button" onClick={() => beginServerEdit(server)}>
                              Rename
                            </button>
                          )}
                          <button
                            type="button"
                            className="button"
                            onClick={() => {
                              if (!window.confirm(`Delete server '${server.name}'? This will also delete its targets.`)) return
                              const controller = new AbortController()
                              deleteServer(environmentId, server.id, controller.signal).then(() => refresh())
                            }}
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

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
          </div>
        </div>
      ) : null}

      {state.kind === 'ready' && view === 'expected-sets' ? (
        <div className="card" style={{ marginTop: 12, padding: 12 }}>
          <div className="h2" style={{ margin: 0 }}>
            Expected sets
          </div>
          <div className="muted" style={{ marginTop: 6 }}>
            Expected sets are configured per Server. Use the Servers list below to open a Server and edit its Expected sets.
          </div>
          <div className="muted" style={{ marginTop: 6 }}>
            Templates are managed on the <code>Templates</code> tab.
          </div>

          <div className="tableWrap" style={{ marginTop: 12 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Server</th>
                  <th>Tomcat roles</th>
                  <th>Microservices</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {state.servers.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="muted">
                      No servers yet.
                    </td>
                  </tr>
                ) : null}
                {state.servers
                  .slice()
                  .sort((a, b) => a.name.localeCompare(b.name))
                  .map((srv) => {
                    const roles = new Set(state.targets.filter((t) => t.serverId === srv.id).map((t) => t.role))
                    const micro = state.actuatorTargets.filter((t) => t.serverId === srv.id).length
                    return (
                      <tr key={srv.id}>
                        <td style={{ fontWeight: 900 }}>{srv.name}</td>
                        <td className="muted">{roles.size}</td>
                        <td className="muted">{micro}</td>
                        <td style={{ whiteSpace: 'nowrap' }}>
                          <Link className="button" to={`/environments/${encodeURIComponent(environmentId)}/servers/${encodeURIComponent(srv.id)}#expected-sets`}>
                            Edit
                          </Link>
                        </td>
                      </tr>
                    )
                  })}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}

      {state.kind === 'ready' && view === 'templates' ? (
        <div className="card" style={{ marginTop: 12 }}>
          <div className="card" style={{ padding: 12, marginBottom: 12 }}>
            <div className="h2" style={{ margin: 0 }}>
              Create template
            </div>
            <div className="muted" style={{ marginTop: 6 }}>
              {isAdmin ? (
                <>Admin-only. Templates are global reusable sets.</>
              ) : (
                <>Only admins can create templates. You can still view existing templates below.</>
              )}
            </div>
            {templateError ? (
              <div className="muted" style={{ marginTop: 8 }}>
                Error: {templateError}
              </div>
            ) : null}

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginTop: 12 }}>
              <div className="card" style={{ padding: 12 }}>
                <div style={{ fontWeight: 900 }}>Tomcat webapps template</div>
                <div className="muted" style={{ marginTop: 6 }}>
                  One webapp path per line (must start with <code>/</code>).
                </div>
                <label className="field" style={{ marginTop: 10 }}>
                  <div className="fieldLabel">Name</div>
                  <input
                    className="fieldInput"
                    value={tomcatTemplateForm.name}
                    onChange={(e) => setTomcatTemplateForm((f) => ({ ...f, name: e.target.value }))}
                    placeholder="touchpoint-payments"
                    disabled={!isAdmin || creatingTomcatTemplate || creatingDockerTemplate}
                  />
                </label>
                <label className="field" style={{ marginTop: 10 }}>
                  <div className="fieldLabel">Items</div>
                  <textarea
                    className="fieldInput"
                    style={{ minHeight: 120 }}
                    value={tomcatTemplateForm.items}
                    onChange={(e) => setTomcatTemplateForm((f) => ({ ...f, items: e.target.value }))}
                    placeholder="/PaymentApp1\n/PaymentApp2"
                    disabled={!isAdmin || creatingTomcatTemplate || creatingDockerTemplate}
                  />
                </label>
                <button type="button" className="button" style={{ marginTop: 10 }} onClick={onCreateTomcatTemplate} disabled={!isAdmin || creatingTomcatTemplate || creatingDockerTemplate}>
                  {creatingTomcatTemplate ? 'Creating…' : 'Create'}
                </button>
              </div>

              <div className="card" style={{ padding: 12 }}>
                <div style={{ fontWeight: 900 }}>Docker services template</div>
                <div className="muted" style={{ marginTop: 6 }}>
                  One service profile per line.
                </div>
                <label className="field" style={{ marginTop: 10 }}>
                  <div className="fieldLabel">Name</div>
                  <input
                    className="fieldInput"
                    value={dockerTemplateForm.name}
                    onChange={(e) => setDockerTemplateForm((f) => ({ ...f, name: e.target.value }))}
                    placeholder="docker-basic"
                    disabled={!isAdmin || creatingTomcatTemplate || creatingDockerTemplate}
                  />
                </label>
                <label className="field" style={{ marginTop: 10 }}>
                  <div className="fieldLabel">Items</div>
                  <textarea
                    className="fieldInput"
                    style={{ minHeight: 120 }}
                    value={dockerTemplateForm.items}
                    onChange={(e) => setDockerTemplateForm((f) => ({ ...f, items: e.target.value }))}
                    placeholder="payments\nservices\nauth"
                    disabled={!isAdmin || creatingTomcatTemplate || creatingDockerTemplate}
                  />
                </label>
                <button type="button" className="button" style={{ marginTop: 10 }} onClick={onCreateDockerTemplate} disabled={!isAdmin || creatingTomcatTemplate || creatingDockerTemplate}>
                  {creatingDockerTemplate ? 'Creating…' : 'Create'}
                </button>
              </div>
            </div>
          </div>

          <div className="card" style={{ padding: 12, marginBottom: 12 }}>
            <div className="h2" style={{ margin: 0 }}>
              Tomcat templates
            </div>
            <div className="tableWrap" style={{ marginTop: 10 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Items</th>
                  </tr>
                </thead>
                <tbody>
                  {state.tomcatTemplates.length === 0 ? (
                    <tr>
                      <td colSpan={2} className="muted">
                        No templates.
                      </td>
                    </tr>
                  ) : null}
                  {state.tomcatTemplates
                    .slice()
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map((t) => (
                      <tr key={t.id}>
                        <td style={{ fontWeight: 900 }}>{t.name}</td>
                        <td className="muted">{t.items.join(', ')}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="card" style={{ padding: 12 }}>
            <div className="h2" style={{ margin: 0 }}>
              Docker templates
            </div>
            <div className="tableWrap" style={{ marginTop: 10 }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Items</th>
                  </tr>
                </thead>
                <tbody>
                  {state.dockerTemplates.length === 0 ? (
                    <tr>
                      <td colSpan={2} className="muted">
                        No templates.
                      </td>
                    </tr>
                  ) : null}
                  {state.dockerTemplates
                    .slice()
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map((t) => (
                      <tr key={t.id}>
                        <td style={{ fontWeight: 900 }}>{t.name}</td>
                        <td className="muted">{t.items.join(', ')}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : null}

      {state.kind === 'ready' && view === 'topology' ? (
        <div className="card" style={{ marginTop: 12 }}>
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
      ) : null}

    </div>
  )
}
