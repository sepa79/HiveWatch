import { Navigate, Outlet, useMatch, useNavigate, useParams } from 'react-router-dom'
import { useToolsBar } from '../components/ToolsBarContext'
import { useEffect, useMemo, useState } from 'react'
import { fetchEnvironments, fetchServers } from '../lib/hivewatchApi'

function EnvironmentToolsBar({ environmentId }: { environmentId: string }) {
  const navigate = useNavigate()

  const isOverview = !!useMatch({ path: '/environments/:environmentId/overview', end: true })
  const isExpected = !!useMatch({ path: '/environments/:environmentId/expected-sets', end: true })
  const isTemplates = !!useMatch({ path: '/environments/:environmentId/templates', end: true })
  const serverMatch = useMatch({ path: '/environments/:environmentId/servers/:serverId', end: true })
  const serverId = serverMatch?.params.serverId ?? null

  const [environmentName, setEnvironmentName] = useState<string | null>(null)
  const [serverName, setServerName] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    setEnvironmentName(null)
    fetchEnvironments(controller.signal)
      .then((envs) => envs.find((e) => e.id === environmentId)?.name ?? null)
      .then((name) => setEnvironmentName(name))
      .catch(() => {
        // Ignore: tools bar should not hard-fail navigation.
      })
    return () => controller.abort()
  }, [environmentId])

  useEffect(() => {
    if (!serverId) {
      setServerName(null)
      return
    }
    const controller = new AbortController()
    setServerName(null)
    fetchServers(environmentId, controller.signal)
      .then((servers) => servers.find((s) => s.id === serverId)?.name ?? null)
      .then((name) => setServerName(name))
      .catch(() => {
        // Ignore: tools bar should not hard-fail navigation.
      })
    return () => controller.abort()
  }, [environmentId, serverId])

  return (
    <div className="toolsRow">
      <button type="button" className="button" onClick={() => navigate('/environments')}>
        ← Environments
      </button>
      <div className="muted" style={{ marginLeft: 10, fontWeight: 900 }}>
        {environmentName ?? 'Environment'}
        {serverId ? (
          <>
            <span style={{ opacity: 0.8 }}> · </span>
            <span>Server: {serverName ?? '…'}</span>
          </>
        ) : null}
      </div>
      <div style={{ display: 'flex', gap: 8, marginLeft: 12 }}>
        <button
          type="button"
          className="button"
          disabled={isOverview}
          onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/overview`)}
        >
          Environment
        </button>
        <button type="button" className="button" disabled={isExpected} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/expected-sets`)}>
          Expected sets
        </button>
        <button type="button" className="button" disabled={isTemplates} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/templates`)}>
          Templates
        </button>
      </div>
      {serverId ? (
        <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/overview`)}>
          ← Back to environment
        </button>
      ) : null}
    </div>
  )
}

export function EnvironmentLayout() {
  const params = useParams()
  const environmentId = (params.environmentId ?? '').trim()

  const tools = useMemo(() => {
    if (!environmentId) return null
    return <EnvironmentToolsBar environmentId={environmentId} />
  }, [environmentId])
  useToolsBar(tools)

  if (!environmentId) return <Navigate to="/environments" replace />
  return <Outlet />
}
