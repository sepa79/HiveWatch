import { useEffect, useMemo, useState } from 'react'
import {
  fetchAdminEnvironments,
  fetchUserEnvironmentVisibility,
  fetchUsers,
  replaceUserEnvironmentVisibility,
  type EnvironmentSummary,
  type UserEnvironmentVisibilityUpdateRequest,
  type UserSummary,
} from '../lib/hivewatchApi'
import { useAuth } from '../lib/authContext'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; users: UserSummary[]; environments: EnvironmentSummary[] }
  | { kind: 'error'; message: string }

export function AdminUsersPage() {
  const { state: auth } = useAuth()
  const [state, setState] = useState<LoadState>({ kind: 'loading' })
  const [selectedUserId, setSelectedUserId] = useState<string>('')
  const [visibility, setVisibility] = useState<UserEnvironmentVisibilityUpdateRequest | null>(null)
  const [saving, setSaving] = useState(false)
  const [visError, setVisError] = useState<string | null>(null)

  useEffect(() => {
    if (auth.kind === 'ready' && !auth.me.roles.includes('ADMIN')) {
      setState({ kind: 'error', message: 'Forbidden (requires ADMIN)' })
      return
    }
    const controller = new AbortController()
    Promise.all([fetchUsers(controller.signal), fetchAdminEnvironments(controller.signal)])
      .then(([users, environments]) => setState({ kind: 'ready', users, environments }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    return () => controller.abort()
  }, [auth])

  const selectedUser = useMemo(() => {
    if (state.kind !== 'ready') return null
    return state.users.find((u) => u.id === selectedUserId) ?? null
  }, [state, selectedUserId])

  useEffect(() => {
    if (state.kind !== 'ready') return
    if (selectedUserId) return
    if (state.users.length === 0) return
    setSelectedUserId(state.users[0].id)
  }, [state, selectedUserId])

  useEffect(() => {
    if (state.kind !== 'ready') return
    if (!selectedUserId) return
    const controller = new AbortController()
    setVisibility(null)
    setVisError(null)
    fetchUserEnvironmentVisibility(selectedUserId, controller.signal)
      .then((v) => setVisibility(v))
      .catch((e) => setVisError(e instanceof Error ? e.message : 'Request failed'))
    return () => controller.abort()
  }, [state, selectedUserId])

  const setEnvVisible = (envId: string, checked: boolean) => {
    setVisibility((v) => {
      if (!v) return v
      const set = new Set(v.environmentIds)
      if (checked) set.add(envId)
      else set.delete(envId)
      return { environmentIds: Array.from(set).sort() }
    })
  }

  return (
    <div className="page">
      <h1 className="h1">Users</h1>
      <div className="muted">Admin config: users + environment visibility (server-side enforced).</div>

      {auth.kind === 'ready' && !auth.me.roles.includes('ADMIN') ? (
        <div className="card" style={{ marginTop: 12, maxWidth: 520, padding: 12 }}>
          <div className="muted">Forbidden: requires ADMIN.</div>
        </div>
      ) : null}

      <div className="card" style={{ marginTop: 12 }}>
        {state.kind === 'loading' ? <div className="muted">Loading…</div> : null}
        {state.kind === 'error' ? <div className="muted">Error: {state.message}</div> : null}
        {state.kind === 'ready' && state.users.length === 0 ? <div className="muted">No users.</div> : null}

        {state.kind === 'ready' && state.users.length > 0 ? (
          <div className="list">
            {state.users.map((user) => (
              <div key={user.id} className="card" style={{ marginTop: 10, padding: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ fontWeight: 700 }}>{user.displayName}</div>
                  <div className="muted">@{user.username}</div>
                  <div style={{ marginLeft: 'auto' }} className={user.active ? 'pill' : 'pill'} data-kind={user.active ? 'ok' : 'alert'}>
                    {user.active ? 'ACTIVE' : 'DISABLED'}
                  </div>
                </div>
                <div className="muted" style={{ marginTop: 8 }}>
                  Roles: {user.roles.join(', ')}
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </div>

      {state.kind === 'ready' && state.users.length > 0 ? (
        <div className="card" style={{ marginTop: 12, padding: 12, maxWidth: 820 }}>
          <div className="h2">Environment visibility</div>
          <div className="muted" style={{ marginTop: 6 }}>
            For non-admin users, Dashboard and Environment APIs are filtered by this list.
          </div>

          <label className="field" style={{ marginTop: 10, maxWidth: 520 }}>
            <div className="fieldLabel">User</div>
            <select className="fieldInput" value={selectedUserId} onChange={(e) => setSelectedUserId(e.target.value)} required>
              {state.users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.displayName} (@{u.username})
                </option>
              ))}
            </select>
          </label>

          {selectedUser ? (
            <div className="muted" style={{ marginTop: 8 }}>
              Selected: <code>{selectedUser.username}</code> · roles: {selectedUser.roles.join(', ')}
            </div>
          ) : null}

          {visError ? (
            <div className="muted" style={{ marginTop: 10 }}>
              Error: {visError}
            </div>
          ) : null}

          {!visibility ? (
            <div className="muted" style={{ marginTop: 10 }}>
              Loading visibility…
            </div>
          ) : (
            <div style={{ marginTop: 10 }}>
              {state.environments.map((env) => (
                <label key={env.id} className="field" style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 6 }}>
                  <input
                    type="checkbox"
                    checked={visibility.environmentIds.includes(env.id)}
                    onChange={(e) => setEnvVisible(env.id, e.target.checked)}
                  />
                  <span>
                    <strong>{env.name}</strong> <span className="muted">({env.id})</span>
                  </span>
                </label>
              ))}

              <div style={{ marginTop: 12, display: 'flex', gap: 10 }}>
                <button
                  type="button"
                  className="button"
                  disabled={saving}
                  onClick={() => {
                    if (!selectedUserId || !visibility) return
                    const controller = new AbortController()
                    setSaving(true)
                    setVisError(null)
                    replaceUserEnvironmentVisibility(selectedUserId, visibility, controller.signal)
                      .then((v) => setVisibility(v))
                      .catch((e) => setVisError(e instanceof Error ? e.message : 'Request failed'))
                      .finally(() => setSaving(false))
                  }}
                >
                  {saving ? 'Saving…' : 'Save visibility'}
                </button>
              </div>
            </div>
          )}
        </div>
      ) : null}
    </div>
  )
}
