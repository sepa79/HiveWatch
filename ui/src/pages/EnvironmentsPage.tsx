import { Fragment, useCallback, useEffect, useState } from 'react'
import {
  applyEnvironmentProvisioningPlan,
  cloneAdminEnvironmentConfig,
  createAdminEnvironment,
  deleteAdminEnvironment,
  type EnvironmentProvisioningPlan,
  fetchAdminEnvironments,
  fetchEnvironments,
  type EnvironmentSummary,
  type ProvisioningPlanValidationResult,
  type TomcatRole,
  validateEnvironmentProvisioningPlan,
} from '../lib/hivewatchApi'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../lib/authContext'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; environments: EnvironmentSummary[] }
  | { kind: 'error'; message: string }

export function EnvironmentsPage() {
  const { state: auth } = useAuth()
  const navigate = useNavigate()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [createName, setCreateName] = useState('')
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)
  const [cloningFromId, setCloningFromId] = useState<string | null>(null)
  const [cloneNewName, setCloneNewName] = useState<string>('')
  const [cloning, setCloning] = useState(false)
  const [cloneError, setCloneError] = useState<string | null>(null)

  const isAdmin = auth.kind === 'ready' && auth.me.roles.includes('ADMIN')

  const refresh = useCallback(
    (signal?: AbortSignal) => {
      setState({ kind: 'loading' })
      const loader = isAdmin ? fetchAdminEnvironments : fetchEnvironments
      return loader(signal)
        .then((environments) => setState({ kind: 'ready', environments }))
        .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    },
    [isAdmin],
  )

  useEffect(() => {
    const controller = new AbortController()
    refresh(controller.signal)
    return () => controller.abort()
  }, [refresh])

  const onCreate = useCallback(() => {
    const name = createName.trim()
    if (!name) {
      setCreateError('Name is required')
      return
    }
    setCreateError(null)
    setCreating(true)
    const controller = new AbortController()
    createAdminEnvironment({ name }, controller.signal)
      .then(() => {
        setCreateName('')
        return refresh(controller.signal)
      })
      .catch((e) => setCreateError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setCreating(false))
  }, [createName, refresh])

  const onDelete = useCallback(
    (env: EnvironmentSummary) => {
      if (!window.confirm(`Delete environment '${env.name}'?\n\nThis will also delete its servers, targets, expected sets, and user visibility mappings.`))
        return
      setDeletingId(env.id)
      const controller = new AbortController()
      deleteAdminEnvironment(env.id, controller.signal)
        .then(() => refresh(controller.signal))
        .catch((e) => window.alert(e instanceof Error ? e.message : 'Request failed'))
        .finally(() => setDeletingId(null))
    },
    [refresh],
  )

  return (
    <div className="page">
      <h1 className="h1">Environments</h1>
      <div className="muted">Pick an environment to edit. Admins can also create and delete environments.</div>

      <div className="card" style={{ marginTop: 12, padding: 12 }}>
        <div className="h2" style={{ margin: 0 }}>
          Environments
        </div>
        {isAdmin ? (
          <>
            <div style={{ display: 'flex', gap: 8, marginTop: 10, alignItems: 'center', maxWidth: 720 }}>
              <label className="field" style={{ flex: 1 }}>
                <div className="fieldLabel">New environment</div>
                <input
                  className="fieldInput"
                  placeholder="Name…"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                  aria-label="New environment name"
                />
              </label>
              <button type="button" className="button" onClick={onCreate} disabled={creating}>
                {creating ? 'Creating…' : 'Create'}
              </button>
            </div>
            {createError ? (
              <div className="muted" style={{ marginTop: 8 }}>
                Error: {createError}
              </div>
            ) : null}
          </>
        ) : null}

        {state.kind === 'loading' ? (
          <div className="muted" style={{ marginTop: 10 }}>
            Loading…
          </div>
        ) : null}
        {state.kind === 'error' ? (
          <div className="muted" style={{ marginTop: 10 }}>
            Error: {state.message}
          </div>
        ) : null}

        {state.kind === 'ready' ? (
          <div className="tableWrap" style={{ marginTop: 12 }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Environment</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {state.environments.length === 0 ? (
                  <tr>
                    <td colSpan={2} className="muted">
                      No environments.
                    </td>
                  </tr>
                ) : null}
                {state.environments.map((env) => (
                  <Fragment key={env.id}>
                    <tr>
                      <td style={{ fontWeight: 900 }}>
                        <Link to={`/environments/${encodeURIComponent(env.id)}`}>{env.name}</Link>
                        {isAdmin ? (
                          <div className="muted" style={{ fontWeight: 500, marginTop: 4 }}>
                            <code>{env.id}</code>
                          </div>
                        ) : null}
                      </td>
                      <td style={{ whiteSpace: 'nowrap' }}>
                        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                          <Link className="button" to={`/environments/${encodeURIComponent(env.id)}`}>
                            Edit
                          </Link>
                        {isAdmin ? (
                          <button
                              type="button"
                              className="button"
                              onClick={() => {
                                if (state.kind !== 'ready') return
                                setCloneError(null)
                                if (cloningFromId === env.id) {
                                  setCloningFromId(null)
                                  return
                                }
                                setCloningFromId(env.id)
                                setCloneNewName(`${env.name} Copy`)
                              }}
                              disabled={cloning}
                            >
                              {cloningFromId === env.id ? 'Close clone' : 'Clone'}
                            </button>
                          ) : null}
                          {isAdmin ? (
                            <button type="button" className="button" onClick={() => onDelete(env)} disabled={deletingId === env.id || cloning}>
                              {deletingId === env.id ? 'Deleting…' : 'Delete'}
                            </button>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                    {isAdmin && state.kind === 'ready' && cloningFromId === env.id ? (
                      <tr>
                        <td colSpan={2}>
                          <div className="card" style={{ padding: 12 }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <div style={{ fontWeight: 900 }}>Clone “{env.name}” into a new environment</div>
                              <div className="muted" style={{ marginLeft: 'auto' }}>
                                Creates a new env and copies servers/targets/expected sets.
                              </div>
                            </div>

                            {cloneError ? (
                              <div className="muted" style={{ marginTop: 8 }}>
                                Error: {cloneError}
                              </div>
                            ) : null}

                            <div style={{ display: 'flex', gap: 10, alignItems: 'end', marginTop: 10, maxWidth: 920 }}>
                              <label className="field" style={{ flex: 1 }}>
                                <div className="fieldLabel">New environment name</div>
                                <input
                                  className="fieldInput"
                                  value={cloneNewName}
                                  onChange={(e) => setCloneNewName(e.target.value)}
                                  disabled={cloning}
                                  aria-label="New environment name"
                                />
                              </label>
                              <button
                                type="button"
                                className="button"
                                disabled={cloning || !cloneNewName.trim()}
                                onClick={() => {
                                  const name = cloneNewName.trim()
                                  if (!name) {
                                    setCloneError('Name is required.')
                                    return
                                  }
                                  if (!window.confirm(`Create '${name}' by cloning '${env.name}'?\n\nThis will create servers and targets.`)) return
                                  setCloning(true)
                                  setCloneError(null)
                                  const controller = new AbortController()
                                  createAdminEnvironment({ name }, controller.signal)
                                    .then((created) =>
                                      cloneAdminEnvironmentConfig(created.id, { sourceEnvironmentId: env.id }, controller.signal).then((r) => ({
                                        created,
                                        r,
                                      })),
                                    )
                                    .then(({ created, r }) => {
                                      window.alert(
                                        `Created '${created.name}'. Cloned: ${r.servers} servers, ${r.tomcatTargets} tomcat targets, ${r.actuatorTargets} microservices, ${r.tomcatExpectedSpecs} tomcat expected specs, ${r.dockerExpectedSpecs} docker expected specs.`,
                                      )
                                      setCloningFromId(null)
                                      return refresh(controller.signal).then(() => created)
                                    })
                                    .then((created) => navigate(`/environments/${encodeURIComponent(created.id)}/overview`))
                                    .catch((e) => setCloneError(e instanceof Error ? e.message : 'Request failed'))
                                    .finally(() => setCloning(false))
                                }}
                              >
                                {cloning ? 'Cloning…' : 'Clone'}
                              </button>
                              <button
                                type="button"
                                className="button"
                                disabled={cloning}
                                onClick={() => {
                                  setCloningFromId(null)
                                  setCloneError(null)
                                }}
                              >
                                Cancel
                              </button>
                            </div>
                          </div>
                        </td>
                      </tr>
                    ) : null}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
      </div>

      {isAdmin ? (
        <ProvisioningWizard
          onApplied={(environmentId) => refresh().then(() => navigate(`/environments/${encodeURIComponent(environmentId)}/overview`))}
        />
      ) : null}
    </div>
  )
}

type ProvisioningWizardDraft = {
  environmentName: string
  serverName: string
  role: TomcatRole
  tomcatBaseUrl: string
  tomcatPort: number
  tomcatUsername: string
  tomcatPassword: string
  actuatorBaseUrl: string
  actuatorPort: number
  actuatorProfile: string
  expectedWebapps: string
}

const roles: TomcatRole[] = ['PAYMENTS', 'SERVICES', 'AUTH']

const defaultProvisioningDraft: ProvisioningWizardDraft = {
  environmentName: '',
  serverName: '',
  role: 'PAYMENTS',
  tomcatBaseUrl: '',
  tomcatPort: 8081,
  tomcatUsername: '',
  tomcatPassword: '',
  actuatorBaseUrl: '',
  actuatorPort: 8080,
  actuatorProfile: '',
  expectedWebapps: '',
}

function ProvisioningWizard({ onApplied }: { onApplied: (environmentId: string) => Promise<void> }) {
  const [draft, setDraft] = useState<ProvisioningWizardDraft>(defaultProvisioningDraft)
  const [validation, setValidation] = useState<ProvisioningPlanValidationResult | null>(null)
  const [validating, setValidating] = useState(false)
  const [applying, setApplying] = useState(false)
  const [requestError, setRequestError] = useState<string | null>(null)
  const [appliedMessage, setAppliedMessage] = useState<string | null>(null)

  const updateDraft = <K extends keyof ProvisioningWizardDraft>(key: K, value: ProvisioningWizardDraft[K]) => {
    setDraft((current) => ({ ...current, [key]: value }))
    setValidation(null)
    setAppliedMessage(null)
  }

  const buildPlan = useCallback((): EnvironmentProvisioningPlan => {
    const expectedItems = splitExpectedItems(draft.expectedWebapps)
    const serverName = draft.serverName.trim()
    const clientRef = toClientRef(serverName || 'server')

    return {
      source: 'UI',
      correlationId: null,
      reason: `Add ${draft.environmentName.trim()}`,
      environment: {
        mode: 'CREATE',
        environmentId: null,
        name: draft.environmentName.trim(),
      },
      servers: [
        {
          clientRef,
          mode: 'CREATE',
          serverId: null,
          name: serverName,
          tomcatTargets: [
            {
              role: draft.role,
              adapterType: 'TOMCAT_MANAGER_HTML',
              baseUrl: draft.tomcatBaseUrl.trim(),
              port: draft.tomcatPort,
              username: draft.tomcatUsername.trim(),
              password: draft.tomcatPassword,
              connectTimeoutMs: 1500,
              requestTimeoutMs: 5000,
            },
          ],
          actuatorTargets: [
            {
              role: draft.role,
              adapterType: 'ACTUATOR_HTTP',
              baseUrl: draft.actuatorBaseUrl.trim(),
              port: draft.actuatorPort,
              profile: draft.actuatorProfile.trim(),
              connectTimeoutMs: 1500,
              requestTimeoutMs: 5000,
            },
          ],
          tomcatExpectedWebapps:
            expectedItems.length > 0
              ? {
                  changeMode: 'REPLACE',
                  specs: [
                    {
                      role: draft.role,
                      mode: 'EXPLICIT',
                      templateId: null,
                      items: expectedItems,
                    },
                  ],
                }
              : {
                  changeMode: 'NO_CHANGE',
                  specs: [],
                },
          dockerExpectedServices: {
            changeMode: 'NO_CHANGE',
            specs: [],
          },
        },
      ],
    }
  }, [draft])

  const onValidate = useCallback(() => {
    setValidating(true)
    setRequestError(null)
    setAppliedMessage(null)
    const controller = new AbortController()
    validateEnvironmentProvisioningPlan(buildPlan(), controller.signal)
      .then(setValidation)
      .catch((e) => setRequestError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setValidating(false))
  }, [buildPlan])

  const onApply = useCallback(() => {
    if (!validation?.valid) {
      setRequestError('Validate the plan before apply.')
      return
    }
    if (!window.confirm(`Apply provisioning plan for '${draft.environmentName.trim()}'?`)) return

    setApplying(true)
    setRequestError(null)
    const controller = new AbortController()
    applyEnvironmentProvisioningPlan({ plan: buildPlan(), scanAfterApply: false }, controller.signal)
      .then((result) => {
        setAppliedMessage(
          `Created ${result.summary.environmentsCreated} environment, ${result.summary.serversCreated} server, ${result.summary.tomcatTargetsCreated} Tomcat target, and ${result.summary.actuatorTargetsCreated} microservice target.`,
        )
        setDraft(defaultProvisioningDraft)
        setValidation(null)
        return onApplied(result.environmentId)
      })
      .catch((e) => setRequestError(e instanceof Error ? e.message : 'Request failed'))
      .finally(() => setApplying(false))
  }, [buildPlan, draft.environmentName, onApplied, validation?.valid])

  return (
    <div className="card" style={{ marginTop: 12, padding: 12 }}>
      <div className="h2" style={{ margin: 0 }}>
        Provision environment
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10, marginTop: 12 }}>
        <TextField label="Environment" value={draft.environmentName} onChange={(value) => updateDraft('environmentName', value)} />
        <TextField label="Server" value={draft.serverName} onChange={(value) => updateDraft('serverName', value)} />
        <label className="field">
          <div className="fieldLabel">Role</div>
          <select className="fieldInput" value={draft.role} onChange={(e) => updateDraft('role', e.target.value as TomcatRole)}>
            {roles.map((role) => (
              <option key={role} value={role}>
                {role}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10, marginTop: 12 }}>
        <TextField label="Tomcat base URL" value={draft.tomcatBaseUrl} onChange={(value) => updateDraft('tomcatBaseUrl', value)} />
        <NumberField label="Tomcat port" value={draft.tomcatPort} onChange={(value) => updateDraft('tomcatPort', value)} />
        <TextField label="Tomcat username" value={draft.tomcatUsername} onChange={(value) => updateDraft('tomcatUsername', value)} />
        <TextField label="Tomcat password" value={draft.tomcatPassword} onChange={(value) => updateDraft('tomcatPassword', value)} type="password" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10, marginTop: 12 }}>
        <TextField label="Microservice base URL" value={draft.actuatorBaseUrl} onChange={(value) => updateDraft('actuatorBaseUrl', value)} />
        <NumberField label="Microservice port" value={draft.actuatorPort} onChange={(value) => updateDraft('actuatorPort', value)} />
        <TextField label="Profile" value={draft.actuatorProfile} onChange={(value) => updateDraft('actuatorProfile', value)} />
      </div>

      <label className="field" style={{ marginTop: 12 }}>
        <div className="fieldLabel">Expected Tomcat webapps</div>
        <textarea
          className="fieldInput"
          rows={3}
          value={draft.expectedWebapps}
          onChange={(e) => updateDraft('expectedWebapps', e.target.value)}
          placeholder="/payments, /payments-admin"
        />
      </label>

      {requestError ? (
        <div className="muted" style={{ marginTop: 8 }}>
          Error: {requestError}
        </div>
      ) : null}
      {appliedMessage ? (
        <div className="muted" style={{ marginTop: 8 }}>
          {appliedMessage}
        </div>
      ) : null}

      <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
        <button type="button" className="button" onClick={onValidate} disabled={validating || applying}>
          {validating ? 'Validating...' : 'Validate'}
        </button>
        <button type="button" className="button" onClick={onApply} disabled={applying || !validation?.valid}>
          {applying ? 'Applying...' : 'Apply'}
        </button>
      </div>

      {validation ? <ProvisioningValidationView validation={validation} /> : null}
    </div>
  )
}

function ProvisioningValidationView({ validation }: { validation: ProvisioningPlanValidationResult }) {
  return (
    <div style={{ marginTop: 12 }}>
      <div className="muted">{validation.valid ? 'Plan is valid.' : 'Plan has blocking errors.'}</div>
      {validation.errors.length > 0 ? (
        <IssueList title="Errors" issues={validation.errors.map((issue) => `${issue.path}: ${issue.message}`)} />
      ) : null}
      {validation.warnings.length > 0 ? (
        <IssueList title="Warnings" issues={validation.warnings.map((issue) => `${issue.path}: ${issue.message}`)} />
      ) : null}
      {validation.diff.length > 0 ? (
        <IssueList title="Diff" issues={validation.diff.map((item) => `${item.action} ${item.objectType}: ${item.label}`)} />
      ) : null}
    </div>
  )
}

function IssueList({ title, issues }: { title: string; issues: string[] }) {
  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ fontWeight: 900 }}>{title}</div>
      <ul style={{ marginTop: 6 }}>
        {issues.map((issue) => (
          <li key={issue}>{issue}</li>
        ))}
      </ul>
    </div>
  )
}

function TextField({
  label,
  value,
  onChange,
  type = 'text',
}: {
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
}) {
  return (
    <label className="field">
      <div className="fieldLabel">{label}</div>
      <input className="fieldInput" type={type} value={value} onChange={(e) => onChange(e.target.value)} />
    </label>
  )
}

function NumberField({ label, value, onChange }: { label: string; value: number; onChange: (value: number) => void }) {
  return (
    <label className="field">
      <div className="fieldLabel">{label}</div>
      <input className="fieldInput" type="number" value={value} onChange={(e) => onChange(Number(e.target.value))} />
    </label>
  )
}

function splitExpectedItems(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function toClientRef(raw: string): string {
  return raw.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'server'
}
