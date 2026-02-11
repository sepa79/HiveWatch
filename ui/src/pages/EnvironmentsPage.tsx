import { Fragment, useCallback, useEffect, useState } from 'react'
import {
  cloneAdminEnvironmentConfig,
  createAdminEnvironment,
  deleteAdminEnvironment,
  fetchAdminEnvironments,
  fetchEnvironments,
  type EnvironmentSummary,
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
    </div>
  )
}
