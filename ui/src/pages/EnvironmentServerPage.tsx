import { type FormEvent, type ReactNode, useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import {
  createActuatorTarget,
  createTomcatTarget,
  deleteActuatorTarget,
  deleteTomcatTarget,
  deleteServer,
  fetchExpectedSetTemplates,
  fetchActuatorTargets,
  fetchEnvironmentTargetRoles,
  fetchServerDockerExpectedServicesSpec,
  fetchServerTomcatExpectedWebappsSpecs,
  fetchServers,
  fetchTomcatTargets,
  replaceServerDockerExpectedServicesSpec,
  replaceServerTomcatExpectedWebappsSpecs,
  updateActuatorTarget,
  updateServer,
  updateTomcatTarget,
  type ActuatorTarget,
  type ActuatorTargetCreateRequest,
  type DockerExpectedServicesSpec,
  type EnvironmentTargetRole,
  type ExpectedSetMode,
  type ExpectedSetTemplate,
  type Server,
  type TomcatRole,
  type TomcatExpectedWebappsSpec,
  type TomcatTarget,
  type TomcatTargetCreateRequest,
} from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | {
      kind: 'ready'
      server: Server
      tomcats: TomcatTarget[]
      microservices: ActuatorTarget[]
      tomcatExpected: TomcatExpectedWebappsSpec[]
      dockerExpected: DockerExpectedServicesSpec
      tomcatTemplates: ExpectedSetTemplate[]
      dockerTemplates: ExpectedSetTemplate[]
      targetRoles: EnvironmentTargetRole[]
    }
  | { kind: 'error'; message: string }

type ServerTab = 'tomcats' | 'microservices'
type TomcatTargetModal = { kind: 'add' } | { kind: 'edit'; targetId: string }
type ActuatorTargetModal = { kind: 'add' } | { kind: 'edit'; targetId: string }

function formatTs(iso: string | null): string {
  if (!iso) return '—'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString()
}

function roleLabel(role: TomcatRole, targetRoles: EnvironmentTargetRole[] = []) {
  return targetRoles.find((candidate) => candidate.code === role)?.label ?? role
}

function parseLines(raw: string): string[] {
  const seen = new Set<string>()
  const result: string[] = []
  raw
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .forEach((line) => {
      if (seen.has(line)) return
      seen.add(line)
      result.push(line)
    })
  return result
}

function defaultTomcatForm(serverId: string): TomcatTargetCreateRequest {
  return {
    serverId,
    role: '',
    baseUrl: 'http://hc-dummy-nft-01-touchpoint-tomcats',
    port: 8081,
    username: 'hc-manager',
    password: 'hc-manager-pass',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  }
}

function defaultActuatorForm(serverId: string): ActuatorTargetCreateRequest {
  return {
    serverId,
    role: '',
    baseUrl: 'http://hc-dummy-nft-01-docker-swarm-microservices',
    port: 8080,
    profile: 'payments',
    connectTimeoutMs: 1500,
    requestTimeoutMs: 5000,
  }
}

function emptyDockerExpected(serverId: string): DockerExpectedServicesSpec {
  return {
    serverId,
    mode: 'UNCONFIGURED',
    templateId: null,
    items: [],
  }
}

function defaultTomcatExpected(serverId: string, role: TomcatRole): TomcatExpectedWebappsSpec {
  return {
    serverId,
    role,
    mode: 'UNCONFIGURED',
    templateId: null,
    items: [],
  }
}

function upsertTomcatExpected(
  current: TomcatExpectedWebappsSpec[],
  nextSpec: TomcatExpectedWebappsSpec,
  previousRole: TomcatRole,
): TomcatExpectedWebappsSpec[] {
  const next = current.filter((spec) => spec.role !== previousRole && spec.role !== nextSpec.role)
  if (nextSpec.mode !== 'UNCONFIGURED') {
    next.push(nextSpec)
  }
  return next.sort((a, b) => a.role.localeCompare(b.role))
}

function expectedTemplateName(templates: ExpectedSetTemplate[], templateId: string | null): string {
  if (!templateId) return '—'
  return templates.find((template) => template.id === templateId)?.name ?? templateId
}

function tomcatExpectedSummary(spec: TomcatExpectedWebappsSpec | null, templates: ExpectedSetTemplate[]): string {
  if (!spec || spec.mode === 'UNCONFIGURED') return 'Disabled'
  if (spec.mode === 'TEMPLATE') return `Template: ${expectedTemplateName(templates, spec.templateId)}`
  return spec.items.length === 0 ? 'No webapps' : spec.items.join(', ')
}

function dockerExpectedSummary(expected: DockerExpectedServicesSpec, profile: string, templates: ExpectedSetTemplate[]): string {
  if (expected.mode === 'UNCONFIGURED') return 'No'
  if (!expected.items.includes(profile)) return 'No'
  if (expected.mode === 'TEMPLATE') return `Yes (${expectedTemplateName(templates, expected.templateId)})`
  return 'Yes'
}

export function EnvironmentServerPage() {
  const params = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  const environmentId = (params.environmentId ?? '').trim()
  const serverId = (params.serverId ?? '').trim()

  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [activeTab, setActiveTab] = useState<ServerTab>('tomcats')

  const [editingServer, setEditingServer] = useState(false)
  const [serverNameDraft, setServerNameDraft] = useState('')
  const [savingServerName, setSavingServerName] = useState(false)

  const [tomcatModal, setTomcatModal] = useState<TomcatTargetModal | null>(null)
  const [tomcatForm, setTomcatForm] = useState<TomcatTargetCreateRequest>(() => defaultTomcatForm(serverId))
  const [savingTomcat, setSavingTomcat] = useState(false)

  const [actuatorModal, setActuatorModal] = useState<ActuatorTargetModal | null>(null)
  const [actuatorForm, setActuatorForm] = useState<ActuatorTargetCreateRequest>(() => defaultActuatorForm(serverId))
  const [actuatorExpected, setActuatorExpected] = useState(true)
  const [savingActuator, setSavingActuator] = useState(false)

  const [expectedTomcatDraft, setExpectedTomcatDraft] = useState<TomcatExpectedWebappsSpec[]>([])
  const [expectedTomcatForm, setExpectedTomcatForm] = useState<TomcatExpectedWebappsSpec>({
    serverId,
    role: '',
    mode: 'UNCONFIGURED',
    templateId: null,
    items: [],
  })
  const [expectedTomcatItemsText, setExpectedTomcatItemsText] = useState('')
  const [expectedTomcatError, setExpectedTomcatError] = useState<string | null>(null)

  const [expectedDockerDraft, setExpectedDockerDraft] = useState<DockerExpectedServicesSpec>(() => emptyDockerExpected(serverId))
  const [expectedDockerError, setExpectedDockerError] = useState<string | null>(null)

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
        fetchServerTomcatExpectedWebappsSpecs(environmentId, serverId, signal),
        fetchServerDockerExpectedServicesSpec(environmentId, serverId, signal),
        fetchExpectedSetTemplates('TOMCAT_WEBAPP_PATH', signal),
        fetchExpectedSetTemplates('DOCKER_SERVICE_PROFILE', signal),
        fetchEnvironmentTargetRoles(environmentId, signal),
      ])
        .then(([servers, tomcats, microservices, tomcatExpected, dockerExpected, tomcatTemplates, dockerTemplates, targetRoles]) => {
          const srv = servers.find((s) => s.id === serverId)
          if (!srv) {
            setState({ kind: 'error', message: 'Server not found' })
            return
          }
          const serverTomcats = tomcats.filter((t) => t.serverId === serverId)
          const serverMicroservices = microservices.filter((t) => t.serverId === serverId)

          setState({
            kind: 'ready',
            server: srv,
            tomcats: serverTomcats,
            microservices: serverMicroservices,
            tomcatExpected,
            dockerExpected,
            tomcatTemplates,
            dockerTemplates,
            targetRoles,
          })
          setServerNameDraft(srv.name)
          setExpectedTomcatDraft(tomcatExpected.filter((spec) => spec.mode !== 'UNCONFIGURED'))
          setExpectedDockerDraft(dockerExpected)
          setExpectedTomcatError(null)
          setExpectedDockerError(null)
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

  useEffect(() => {
    if (location.hash === '#microservices') setActiveTab('microservices')
    if (location.hash === '#tomcats' || location.hash === '#expected-sets') setActiveTab('tomcats')
  }, [location.hash])

  const title = useMemo(() => {
    if (state.kind === 'ready') return `Server · ${state.server.name}`
    return 'Server'
  }, [state])

  const openAddTomcat = () => {
    const usedRoles = state.kind === 'ready' ? new Set(state.tomcats.map((target) => target.role)) : new Set<TomcatRole>()
    const configuredRoles = state.kind === 'ready' ? state.targetRoles.filter((role) => role.active) : []
    const firstRole = configuredRoles.find((role) => !usedRoles.has(role.code))?.code
    if (!firstRole) {
      window.alert('Configure an active target role first.')
      return
    }
    const expected = expectedTomcatDraft.find((spec) => spec.role === firstRole) ?? defaultTomcatExpected(serverId, firstRole)
    setTomcatForm({ ...defaultTomcatForm(serverId), role: firstRole })
    setExpectedTomcatForm(expected)
    setExpectedTomcatItemsText(expected.items.join('\n'))
    setTomcatModal({ kind: 'add' })
  }

  const openEditTomcat = (role: TomcatRole, target: TomcatTarget | null) => {
    const expected = expectedTomcatDraft.find((spec) => spec.role === role) ?? defaultTomcatExpected(serverId, role)
    setExpectedTomcatForm(expected)
    setExpectedTomcatItemsText(expected.items.join('\n'))
    if (!target) {
      setTomcatForm({ ...defaultTomcatForm(serverId), role })
      setTomcatModal({ kind: 'add' })
      return
    }
    setTomcatForm({
      serverId: target.serverId,
      role: target.role,
      baseUrl: target.baseUrl,
      port: target.port,
      username: target.username,
      password: '',
      connectTimeoutMs: target.connectTimeoutMs,
      requestTimeoutMs: target.requestTimeoutMs,
    })
    setTomcatModal({ kind: 'edit', targetId: target.id })
  }

  const openAddActuator = (profile?: string) => {
    const firstRole = state.kind === 'ready' ? state.targetRoles.find((role) => role.active)?.code : null
    if (!firstRole) {
      window.alert('Configure an active target role first.')
      return
    }
    setActuatorForm({ ...defaultActuatorForm(serverId), role: firstRole, profile: profile ?? defaultActuatorForm(serverId).profile })
    setActuatorExpected(true)
    setActuatorModal({ kind: 'add' })
  }

  const openEditActuator = (target: ActuatorTarget) => {
    setActuatorForm({
      serverId: target.serverId,
      role: target.role,
      baseUrl: target.baseUrl,
      port: target.port,
      profile: target.profile,
      connectTimeoutMs: target.connectTimeoutMs,
      requestTimeoutMs: target.requestTimeoutMs,
    })
    setActuatorExpected(expectedDockerDraft.mode !== 'UNCONFIGURED' && expectedDockerDraft.items.includes(target.profile))
    setActuatorModal({ kind: 'edit', targetId: target.id })
  }

  const saveTomcatTarget = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!tomcatModal) return
    if (tomcatModal.kind === 'edit' && !tomcatForm.password.trim()) {
      window.alert('Password is required.')
      return
    }

    const items = expectedTomcatForm.mode === 'EXPLICIT' ? parseLines(expectedTomcatItemsText) : expectedTomcatForm.items
    if (expectedTomcatForm.mode === 'EXPLICIT' && items.length === 0) {
      setExpectedTomcatError('Items are required for explicit mode.')
      return
    }
    if (expectedTomcatForm.mode === 'TEMPLATE' && !expectedTomcatForm.templateId) {
      setExpectedTomcatError('Template is required.')
      return
    }

    const previousRole =
      tomcatModal.kind === 'edit' && state.kind === 'ready'
        ? state.tomcats.find((target) => target.id === tomcatModal.targetId)?.role ?? tomcatForm.role
        : tomcatForm.role
    const nextExpected = upsertTomcatExpected(
      expectedTomcatDraft,
      {
        ...expectedTomcatForm,
        serverId,
        role: tomcatForm.role,
        templateId: expectedTomcatForm.mode === 'TEMPLATE' ? expectedTomcatForm.templateId : null,
        items: expectedTomcatForm.mode === 'UNCONFIGURED' ? [] : items,
      },
      previousRole,
    )

    const controller = new AbortController()
    setSavingTomcat(true)
    setExpectedTomcatError(null)
    const request = { ...tomcatForm, serverId }
    const action =
      tomcatModal.kind === 'add'
        ? createTomcatTarget(environmentId, request, controller.signal)
        : updateTomcatTarget(environmentId, tomcatModal.targetId, request, controller.signal)

    action
      .then(() => replaceServerTomcatExpectedWebappsSpecs(environmentId, serverId, { specs: nextExpected }, controller.signal))
      .then((updated) => setExpectedTomcatDraft(updated.filter((spec) => spec.mode !== 'UNCONFIGURED')))
      .then(() => refresh(controller.signal))
      .then(() => setTomcatModal(null))
      .catch((e) => {
        const message = e instanceof Error ? e.message : 'Request failed'
        setExpectedTomcatError(message)
        window.alert(message)
      })
      .finally(() => setSavingTomcat(false))
  }

  const saveActuatorTarget = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!actuatorModal) return

    const controller = new AbortController()
    setSavingActuator(true)
    const request = { ...actuatorForm, serverId }
    const previousProfile =
      actuatorModal.kind === 'edit' && state.kind === 'ready'
        ? state.microservices.find((target) => target.id === actuatorModal.targetId)?.profile ?? actuatorForm.profile
        : actuatorForm.profile
    const expectedItems = expectedDockerDraft.items
      .filter((profile) => profile !== previousProfile && profile !== request.profile)
      .concat(actuatorExpected ? [request.profile] : [])
      .sort()
    const nextExpected: DockerExpectedServicesSpec = {
      serverId,
      mode: expectedItems.length === 0 ? 'UNCONFIGURED' : 'EXPLICIT',
      templateId: null,
      items: expectedItems,
    }
    const action =
      actuatorModal.kind === 'add'
        ? createActuatorTarget(environmentId, request, controller.signal)
        : updateActuatorTarget(environmentId, actuatorModal.targetId, request, controller.signal)

    action
      .then(() => replaceServerDockerExpectedServicesSpec(environmentId, serverId, { specs: nextExpected.mode === 'UNCONFIGURED' ? [] : [nextExpected] }, controller.signal))
      .then((updated) => setExpectedDockerDraft(updated))
      .then(() => refresh(controller.signal))
      .then(() => setActuatorModal(null))
      .catch((e) => {
        const message = e instanceof Error ? e.message : 'Request failed'
        setExpectedDockerError(message)
        window.alert(message)
      })
      .finally(() => setSavingActuator(false))
  }

  return (
    <div className="page">
      <h1 className="h1">{title}</h1>
      <div className="muted">
        <Link to={`/environments/${encodeURIComponent(environmentId)}/overview`}>← Back to environment</Link>
      </div>

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
        <>
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
                  if (!window.confirm(`Delete server '${state.server.name}'? This will also delete its targets and expected sets.`)) return
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
                    const controller = new AbortController()
                    setSavingServerName(true)
                    updateServer(environmentId, state.server.id, { name: next }, controller.signal)
                      .then(() => refresh(controller.signal))
                      .then(() => setEditingServer(false))
                      .catch((e) => window.alert(e instanceof Error ? e.message : 'Request failed'))
                      .finally(() => setSavingServerName(false))
                  }}
                >
                  {savingServerName ? 'Saving…' : 'Save'}
                </button>
                <button
                  type="button"
                  className="button"
                  disabled={savingServerName}
                  onClick={() => {
                    setEditingServer(false)
                    setServerNameDraft(state.server.name)
                  }}
                >
                  Cancel
                </button>
              </div>
            ) : null}
          </div>

          <div className="tabRow" style={{ marginTop: 12 }} role="tablist" aria-label="Server configuration">
            <button
              type="button"
              className={`tabButton ${activeTab === 'tomcats' ? 'tabButtonActive' : ''}`}
              role="tab"
              aria-selected={activeTab === 'tomcats'}
              onClick={() => setActiveTab('tomcats')}
            >
              Tomcats
            </button>
            <button
              type="button"
              className={`tabButton ${activeTab === 'microservices' ? 'tabButtonActive' : ''}`}
              role="tab"
              aria-selected={activeTab === 'microservices'}
              onClick={() => setActiveTab('microservices')}
            >
              Microservices
            </button>
          </div>

          {activeTab === 'tomcats' ? (
            <TomcatsTab
              tomcats={state.tomcats}
              expected={expectedTomcatDraft}
              templates={state.tomcatTemplates}
              targetRoles={state.targetRoles}
              expectedError={expectedTomcatError}
              onAddTarget={openAddTomcat}
              onEditTarget={openEditTomcat}
              onDeleteRow={(role, target) => {
                if (!window.confirm(`Delete Tomcat row '${roleLabel(role, state.targetRoles)}'? This removes the target and expected webapps for this role.`)) return
                const controller = new AbortController()
                const nextExpected = expectedTomcatDraft.filter((spec) => spec.role !== role)
                const deleteTarget = target ? deleteTomcatTarget(environmentId, target.id, controller.signal) : Promise.resolve()
                deleteTarget
                  .then(() => replaceServerTomcatExpectedWebappsSpecs(environmentId, serverId, { specs: nextExpected }, controller.signal))
                  .then((updated) => setExpectedTomcatDraft(updated.filter((spec) => spec.mode !== 'UNCONFIGURED')))
                  .then(() => refresh(controller.signal))
                  .catch((e) => {
                    const message = e instanceof Error ? e.message : 'Request failed'
                    setExpectedTomcatError(message)
                    window.alert(message)
                  })
              }}
            />
          ) : (
            <MicroservicesTab
              microservices={state.microservices}
              expected={expectedDockerDraft}
              templates={state.dockerTemplates}
              targetRoles={state.targetRoles}
              expectedError={expectedDockerError}
              onAddTarget={openAddActuator}
              onEditRow={(profile, target) => {
                if (target) {
                  openEditActuator(target)
                } else {
                  openAddActuator(profile)
                }
              }}
              onDeleteRow={(profile, target) => {
                if (!window.confirm(`Delete microservice row '${profile}'? This removes the target and expected profile.`)) return
                const controller = new AbortController()
                const nextItems = expectedDockerDraft.items.filter((item) => item !== profile)
                const nextExpected: DockerExpectedServicesSpec = {
                  serverId,
                  mode: nextItems.length === 0 ? 'UNCONFIGURED' : 'EXPLICIT',
                  templateId: null,
                  items: nextItems,
                }
                const deleteTarget = target ? deleteActuatorTarget(environmentId, target.id, controller.signal) : Promise.resolve()
                deleteTarget
                  .then(() =>
                    replaceServerDockerExpectedServicesSpec(
                      environmentId,
                      serverId,
                      { specs: nextExpected.mode === 'UNCONFIGURED' ? [] : [nextExpected] },
                      controller.signal,
                    ),
                  )
                  .then((updated) => setExpectedDockerDraft(updated))
                  .then(() => refresh(controller.signal))
                  .catch((e) => {
                    const message = e instanceof Error ? e.message : 'Request failed'
                    setExpectedDockerError(message)
                    window.alert(message)
                  })
              }}
            />
          )}
        </>
      ) : null}

      {tomcatModal ? (
        <Modal title={tomcatModal.kind === 'add' ? 'Add Tomcat target' : 'Edit Tomcat target'} onClose={() => setTomcatModal(null)}>
          <TomcatTargetForm
            form={tomcatForm}
            setForm={setTomcatForm}
            expectedForm={expectedTomcatForm}
            setExpectedForm={setExpectedTomcatForm}
            expectedItemsText={expectedTomcatItemsText}
            setExpectedItemsText={setExpectedTomcatItemsText}
            templates={state.kind === 'ready' ? state.tomcatTemplates : []}
            targetRoles={state.kind === 'ready' ? state.targetRoles : []}
            saving={savingTomcat}
            submitLabel={tomcatModal.kind === 'add' ? 'Add target' : 'Save changes'}
            onSubmit={saveTomcatTarget}
            onCancel={() => setTomcatModal(null)}
            isEdit={tomcatModal.kind === 'edit'}
          />
          {expectedTomcatError ? <div className="muted" style={{ marginTop: 10 }}>Error: {expectedTomcatError}</div> : null}
        </Modal>
      ) : null}

      {actuatorModal ? (
        <Modal title={actuatorModal.kind === 'add' ? 'Add microservice' : 'Edit microservice'} onClose={() => setActuatorModal(null)}>
          <ActuatorTargetForm
            form={actuatorForm}
            setForm={setActuatorForm}
            targetRoles={state.kind === 'ready' ? state.targetRoles : []}
            expected={actuatorExpected}
            setExpected={setActuatorExpected}
            saving={savingActuator}
            submitLabel={actuatorModal.kind === 'add' ? 'Add microservice' : 'Save changes'}
            onSubmit={saveActuatorTarget}
            onCancel={() => setActuatorModal(null)}
          />
          {expectedDockerError ? <div className="muted" style={{ marginTop: 10 }}>Error: {expectedDockerError}</div> : null}
        </Modal>
      ) : null}
    </div>
  )
}

function TomcatsTab({
  tomcats,
  expected,
  templates,
  targetRoles,
  expectedError,
  onAddTarget,
  onEditTarget,
  onDeleteRow,
}: {
  tomcats: TomcatTarget[]
  expected: TomcatExpectedWebappsSpec[]
  templates: ExpectedSetTemplate[]
  targetRoles: EnvironmentTargetRole[]
  expectedError: string | null
  onAddTarget: () => void
  onEditTarget: (role: TomcatRole, target: TomcatTarget | null) => void
  onDeleteRow: (role: TomcatRole, target: TomcatTarget | null) => void
}) {
  const targetByRole = new Map(tomcats.map((target) => [target.role, target]))
  const expectedByRole = new Map(expected.map((spec) => [spec.role, spec]))
  const rowRoles = Array.from(new Set([...tomcats.map((target) => target.role), ...expected.map((spec) => spec.role)]))
    .sort((a, b) => roleLabel(a, targetRoles).localeCompare(roleLabel(b, targetRoles)))
  const canAddTarget = targetRoles.some((role) => role.active && !targetByRole.has(role.code))

  return (
    <div role="tabpanel" className="serverTabPanel">
      <section className="panelSection">
        <div className="sectionHeader">
          <div>
            <div className="h2" style={{ margin: 0 }}>Tomcats</div>
            <div className="muted" style={{ marginTop: 4 }}>Manager endpoint and expected webapps per role.</div>
          </div>
          <button type="button" className="button" onClick={onAddTarget} disabled={!canAddTarget}>Add</button>
        </div>
        {expectedError ? <div className="muted" style={{ marginTop: 8 }}>Error: {expectedError}</div> : null}
        <div className="tableWrap" style={{ marginTop: 10 }}>
          <table className="table">
            <thead>
              <tr>
                <th>Role</th>
                <th>Endpoint</th>
                <th>Expected webapps</th>
                <th>Last scan</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {rowRoles.length === 0 ? (
                <tr><td colSpan={5} className="muted">No Tomcats configured.</td></tr>
              ) : null}
              {rowRoles.map((role) => {
                const target = targetByRole.get(role) ?? null
                const spec = expectedByRole.get(role) ?? null
                return (
                  <tr key={role}>
                    <td style={{ fontWeight: 900 }}>{roleLabel(role, targetRoles)}</td>
                    <td className="muted">{target ? `${target.baseUrl}:${target.port}` : 'No target'}</td>
                    <td className="muted">{tomcatExpectedSummary(spec, templates)}</td>
                    <td className="muted">{formatTs(target?.state?.scannedAt ?? null)}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button type="button" className="button" onClick={() => onEditTarget(role, target)}>Edit</button>
                      <button type="button" className="button" onClick={() => onDeleteRow(role, target)}>Del</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}

function MicroservicesTab({
  microservices,
  expected,
  templates,
  targetRoles,
  expectedError,
  onAddTarget,
  onEditRow,
  onDeleteRow,
}: {
  microservices: ActuatorTarget[]
  expected: DockerExpectedServicesSpec
  templates: ExpectedSetTemplate[]
  targetRoles: EnvironmentTargetRole[]
  expectedError: string | null
  onAddTarget: () => void
  onEditRow: (profile: string, target: ActuatorTarget | null) => void
  onDeleteRow: (profile: string, target: ActuatorTarget | null) => void
}) {
  const targetByProfile = new Map(microservices.map((target) => [target.profile, target]))
  const profiles = Array.from(new Set([...microservices.map((target) => target.profile), ...expected.items])).sort()
  const canAddTarget = targetRoles.some((role) => role.active)

  return (
    <div role="tabpanel" className="serverTabPanel">
      <section className="panelSection">
        <div className="sectionHeader">
          <div>
            <div className="h2" style={{ margin: 0 }}>Microservices</div>
            <div className="muted" style={{ marginTop: 4 }}>Spring Boot Actuator endpoints grouped by profile.</div>
          </div>
          <button type="button" className="button" onClick={onAddTarget} disabled={!canAddTarget}>Add</button>
        </div>
        {expectedError ? <div className="muted" style={{ marginTop: 8 }}>Error: {expectedError}</div> : null}
        <div className="tableWrap" style={{ marginTop: 10 }}>
          <table className="table">
            <thead>
              <tr>
                <th>Profile</th>
                <th>Endpoint</th>
                <th>Expected</th>
                <th>Last scan</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {profiles.length === 0 ? (
                <tr><td colSpan={5} className="muted">No microservices configured.</td></tr>
              ) : null}
              {profiles.map((profile) => {
                const target = targetByProfile.get(profile) ?? null
                return (
                  <tr key={profile}>
                    <td style={{ fontWeight: 900 }}>{profile}</td>
                    <td className="muted">{target ? `${target.baseUrl}:${target.port}` : 'No target'}</td>
                    <td className="muted">{dockerExpectedSummary(expected, profile, templates)}</td>
                    <td className="muted">{formatTs(target?.state?.scannedAt ?? null)}</td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      <button type="button" className="button" onClick={() => onEditRow(profile, target)}>Edit</button>
                      <button type="button" className="button" onClick={() => onDeleteRow(profile, target)}>Del</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}

function TomcatTargetForm({
  form,
  setForm,
  expectedForm,
  setExpectedForm,
  expectedItemsText,
  setExpectedItemsText,
  templates,
  targetRoles,
  saving,
  submitLabel,
  onSubmit,
  onCancel,
  isEdit,
}: {
  form: TomcatTargetCreateRequest
  setForm: (form: TomcatTargetCreateRequest) => void
  expectedForm: TomcatExpectedWebappsSpec
  setExpectedForm: (form: TomcatExpectedWebappsSpec) => void
  expectedItemsText: string
  setExpectedItemsText: (value: string) => void
  templates: ExpectedSetTemplate[]
  targetRoles: EnvironmentTargetRole[]
  saving: boolean
  submitLabel: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
  isEdit: boolean
}) {
  return (
    <form className="modalFormGrid" onSubmit={onSubmit}>
      <RoleField
        value={form.role}
        targetRoles={targetRoles}
        onChange={(role) => {
          setForm({ ...form, role })
          setExpectedForm({ ...expectedForm, role })
        }}
      />
      <div />
      <TextInput label="Base URL" value={form.baseUrl} onChange={(baseUrl) => setForm({ ...form, baseUrl })} required />
      <NumberInput label="Port" value={form.port} onChange={(port) => setForm({ ...form, port })} min={1} max={65535} required />
      <TextInput label="Username" value={form.username} onChange={(username) => setForm({ ...form, username })} required />
      <TextInput label={isEdit ? 'Password (required to save)' : 'Password'} type="password" value={form.password} onChange={(password) => setForm({ ...form, password })} required />
      <NumberInput label="Connect timeout (ms)" value={form.connectTimeoutMs} onChange={(connectTimeoutMs) => setForm({ ...form, connectTimeoutMs })} min={1} required />
      <NumberInput label="Request timeout (ms)" value={form.requestTimeoutMs} onChange={(requestTimeoutMs) => setForm({ ...form, requestTimeoutMs })} min={1} required />
      <label className="field">
        <div className="fieldLabel">Expected webapps</div>
        <select
          className="fieldInput"
          value={expectedForm.mode}
          onChange={(e) => setExpectedForm({ ...expectedForm, mode: e.target.value as ExpectedSetMode, templateId: null, items: [] })}
        >
          <option value="UNCONFIGURED">Disabled</option>
          <option value="EXPLICIT">Explicit</option>
          <option value="TEMPLATE">Template</option>
        </select>
      </label>
      {expectedForm.mode === 'TEMPLATE' ? (
        <label className="field">
          <div className="fieldLabel">Template</div>
          <select
            className="fieldInput"
            value={expectedForm.templateId ?? ''}
            onChange={(e) => {
              const templateId = e.target.value || null
              const template = templates.find((candidate) => candidate.id === templateId)
              setExpectedForm({ ...expectedForm, templateId, items: template?.items ?? [] })
            }}
            required
          >
            <option value="">Select template…</option>
            {templates.map((template) => (
              <option key={template.id} value={template.id}>{template.name}</option>
            ))}
          </select>
        </label>
      ) : (
        <div />
      )}
      {expectedForm.mode === 'EXPLICIT' ? (
        <label className="field" style={{ gridColumn: '1 / span 2' }}>
          <div className="fieldLabel">Expected paths</div>
          <textarea className="fieldInput" rows={5} value={expectedItemsText} onChange={(e) => setExpectedItemsText(e.target.value)} required />
        </label>
      ) : null}
      <ModalActions saving={saving} submitLabel={submitLabel} onCancel={onCancel} />
    </form>
  )
}

function ActuatorTargetForm({
  form,
  setForm,
  targetRoles,
  expected,
  setExpected,
  saving,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  form: ActuatorTargetCreateRequest
  setForm: (form: ActuatorTargetCreateRequest) => void
  targetRoles: EnvironmentTargetRole[]
  expected: boolean
  setExpected: (expected: boolean) => void
  saving: boolean
  submitLabel: string
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}) {
  return (
    <form className="modalFormGrid" onSubmit={onSubmit}>
      <RoleField value={form.role} targetRoles={targetRoles} onChange={(role) => setForm({ ...form, role })} />
      <div />
      <TextInput label="Base URL" value={form.baseUrl} onChange={(baseUrl) => setForm({ ...form, baseUrl })} required />
      <NumberInput label="Port" value={form.port} onChange={(port) => setForm({ ...form, port })} min={1} max={65535} required />
      <TextInput label="Profile" value={form.profile} onChange={(profile) => setForm({ ...form, profile })} required />
      <div />
      <NumberInput label="Connect timeout (ms)" value={form.connectTimeoutMs} onChange={(connectTimeoutMs) => setForm({ ...form, connectTimeoutMs })} min={1} required />
      <NumberInput label="Request timeout (ms)" value={form.requestTimeoutMs} onChange={(requestTimeoutMs) => setForm({ ...form, requestTimeoutMs })} min={1} required />
      <label className="field" style={{ gridColumn: '1 / span 2' }}>
        <div className="fieldLabel">Expected profile</div>
        <label style={{ display: 'flex', gap: 8, alignItems: 'center', fontWeight: 800 }}>
          <input type="checkbox" checked={expected} onChange={(e) => setExpected(e.target.checked)} />
          Include this profile in expected services
        </label>
      </label>
      <ModalActions saving={saving} submitLabel={submitLabel} onCancel={onCancel} />
    </form>
  )
}

function RoleField({
  value,
  targetRoles,
  onChange,
}: {
  value: TomcatRole
  targetRoles: EnvironmentTargetRole[]
  onChange: (role: TomcatRole) => void
}) {
  const options = targetRoles.filter((role) => role.active || role.code === value)
  return (
    <label className="field">
      <div className="fieldLabel">Role</div>
      <select className="fieldInput" value={value} onChange={(e) => onChange(e.target.value as TomcatRole)} required>
        <option value="" disabled>
          Select role…
        </option>
        {options.map((role) => (
          <option key={role.code} value={role.code}>{role.label}</option>
        ))}
      </select>
    </label>
  )
}

function TextInput({
  label,
  value,
  onChange,
  type = 'text',
  required,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
  required?: boolean
}) {
  return (
    <label className="field">
      <div className="fieldLabel">{label}</div>
      <input className="fieldInput" type={type} value={value} onChange={(e) => onChange(e.target.value)} required={required} />
    </label>
  )
}

function NumberInput({
  label,
  value,
  onChange,
  min,
  max,
  required,
}: {
  label: string
  value: number
  onChange: (value: number) => void
  min?: number
  max?: number
  required?: boolean
}) {
  return (
    <label className="field">
      <div className="fieldLabel">{label}</div>
      <input className="fieldInput" type="number" value={value} min={min} max={max} onChange={(e) => onChange(Number(e.target.value))} required={required} />
    </label>
  )
}

function Modal({
  title,
  children,
  onClose,
}: {
  title: string
  children: ReactNode
  onClose: () => void
}) {
  return (
    <div className="modalBackdrop" role="presentation" onMouseDown={(e) => {
      if (e.target === e.currentTarget) onClose()
    }}>
      <div className="modalPanel" role="dialog" aria-modal="true" aria-label={title}>
        <div className="sectionHeader">
          <div className="h2" style={{ margin: 0 }}>{title}</div>
          <button type="button" className="button" onClick={onClose}>Close</button>
        </div>
        <div style={{ marginTop: 12 }}>{children}</div>
      </div>
    </div>
  )
}

function ModalActions({
  saving = false,
  submitLabel,
  onCancel,
}: {
  saving?: boolean
  submitLabel: string
  onCancel: () => void
}) {
  return (
    <div style={{ gridColumn: '1 / span 2', display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
      <button type="button" className="button" onClick={onCancel} disabled={saving}>Cancel</button>
      <button type="submit" className="button" disabled={saving}>{saving ? 'Saving…' : submitLabel}</button>
    </div>
  )
}
