import { useLocation, Link } from 'react-router-dom'

type Crumb = { label: string; to?: string }

function normalizeSegment(segment: string): string {
  try {
    return decodeURIComponent(segment)
  } catch {
    return segment
  }
}

function buildCrumbs(pathname: string): Crumb[] {
  const parts = pathname.split('/').filter(Boolean).map(normalizeSegment)
  if (parts.length === 0) return [{ label: 'Dashboard', to: '/' }]

  const section = parts[0]
  if (section === 'environments')
    return [{ label: 'Environments', to: '/environments' }, ...parts.slice(1).map((p) => ({ label: p }))]
  if (section === 'admin') return [{ label: 'Admin', to: '/admin/users' }, ...parts.slice(1).map((p) => ({ label: p }))]
  if (section === 'diagnostics') return [{ label: 'Diagnostics', to: '/diagnostics' }]
  if (section === 'help') return [{ label: 'Help', to: '/help' }]
  if (section === 'login') return [{ label: 'Login', to: '/login' }]

  return parts.map((p) => ({ label: p }))
}

export function Breadcrumbs() {
  const location = useLocation()
  const crumbs = buildCrumbs(location.pathname)

  return (
    <nav className="breadcrumbs" aria-label="Breadcrumb">
      {crumbs.map((c, idx) => {
        const last = idx === crumbs.length - 1
        const node = c.to && !last ? (
          <Link className="breadcrumbLink" to={c.to}>
            {c.label}
          </Link>
        ) : (
          <span className={last ? 'breadcrumbCurrent' : 'breadcrumbText'}>{c.label}</span>
        )
        return (
          <span key={`${idx}:${c.label}`} className="breadcrumbItem">
            {node}
            {!last ? <span className="breadcrumbSep">/</span> : null}
          </span>
        )
      })}
    </nav>
  )
}
