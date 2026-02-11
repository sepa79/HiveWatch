import { useEffect, useState } from 'react'
import { fetchUsers, type UserSummary } from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; users: UserSummary[] }
  | { kind: 'error'; message: string }

export function AdminUsersPage() {
  const [state, setState] = useState<LoadState>({ kind: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    fetchUsers(controller.signal)
      .then((users) => setState({ kind: 'ready', users }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    return () => controller.abort()
  }, [])

  return (
    <div className="page">
      <h1 className="h1">Users</h1>
      <div className="muted">Admin UI placeholder. Next: roles, teams, visibility rules.</div>

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
    </div>
  )
}

