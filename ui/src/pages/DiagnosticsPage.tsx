import { useEffect, useState } from 'react'
import { fetchBackendHealth } from '../lib/hivewatchApi'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'ready'; status: string }
  | { kind: 'error'; message: string }

export function DiagnosticsPage() {
  const [state, setState] = useState<LoadState>({ kind: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    fetchBackendHealth(controller.signal)
      .then((payload) => setState({ kind: 'ready', status: String(payload.status ?? 'UNKNOWN') }))
      .catch((e) => setState({ kind: 'error', message: e instanceof Error ? e.message : 'Request failed' }))
    return () => controller.abort()
  }, [])

  return (
    <div className="page">
      <h1 className="h1">Diagnostics</h1>
      <div className="muted">Backend connectivity + runtime basics.</div>

      <div className="card" style={{ marginTop: 12, maxWidth: 520 }}>
        {state.kind === 'loading' ? <div className="muted">Checking…</div> : null}
        {state.kind === 'error' ? <div className="muted">Error: {state.message}</div> : null}
        {state.kind === 'ready' ? (
          <div className="kv">
            <div className="k">/actuator/health</div>
            <div className="v">{state.status}</div>
          </div>
        ) : null}
      </div>
    </div>
  )
}

