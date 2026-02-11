import { Navigate, Outlet, useMatch, useNavigate, useParams } from 'react-router-dom'
import { useToolsBar } from '../components/ToolsBarContext'
import { useMemo } from 'react'

function EnvironmentToolsBar({ environmentId }: { environmentId: string }) {
  const navigate = useNavigate()

  const isOverview = !!useMatch({ path: '/environments/:environmentId/overview', end: true })
  const isExpected = !!useMatch({ path: '/environments/:environmentId/expected-sets', end: true })
  const isTopology = !!useMatch({ path: '/environments/:environmentId/topology', end: true })
  const isTemplates = !!useMatch({ path: '/environments/:environmentId/templates', end: true })

  return (
    <div className="toolsRow">
      <button type="button" className="button" onClick={() => navigate('/environments')}>
        ← Environments
      </button>
      <div className="muted" style={{ marginLeft: 10, fontWeight: 800 }}>
        {environmentId}
      </div>
      <div style={{ display: 'flex', gap: 8, marginLeft: 12 }}>
        <button type="button" className="button" disabled={isOverview} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/overview`)}>
          Overview
        </button>
        <button type="button" className="button" disabled={isExpected} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/expected-sets`)}>
          Expected sets
        </button>
        <button type="button" className="button" disabled={isTopology} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/topology`)}>
          Topology
        </button>
        <button type="button" className="button" disabled={isTemplates} onClick={() => navigate(`/environments/${encodeURIComponent(environmentId)}/templates`)}>
          Templates
        </button>
      </div>
      <button type="button" className="button" style={{ marginLeft: 'auto' }} onClick={() => navigate('/help')}>
        Help
      </button>
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

