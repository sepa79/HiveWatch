import { NavLink } from 'react-router-dom'
import { Icon } from './Icon'
import { useAuth } from '../lib/authContext'

function NavItem({
  to,
  title,
  icon,
  expanded,
}: {
  to: string
  title: string
  icon: Parameters<typeof Icon>[0]['name']
  expanded: boolean
}) {
  return (
    <NavLink to={to} className={expanded ? 'navItem' : 'navIcon'} title={title} aria-label={title}>
      <Icon name={icon} />
      {expanded ? <span className="navLabel">{title}</span> : null}
    </NavLink>
  )
}

export function SideNav({ expanded, onToggle }: { expanded: boolean; onToggle: () => void }) {
  const { state } = useAuth()
  const roles = state.kind === 'ready' ? state.me.roles : []
  const isAdmin = roles.includes('ADMIN')
  const isAuthed = state.kind === 'ready'

  return (
    <div className="navIconStack">
      <div className={expanded ? 'navHeader navHeaderExpanded' : 'navHeader'}>
        <button
          type="button"
          className={expanded ? 'navCollapseBtn navCollapseBtnExpanded' : 'navCollapseBtn'}
          onClick={onToggle}
          title={expanded ? 'Collapse' : 'Expand'}
          aria-label={expanded ? 'Collapse navigation' : 'Expand navigation'}
        >
          {expanded ? '«' : '»'}
        </button>
        {expanded ? <div className="navHeaderTitle">Navigation</div> : null}
      </div>

      {isAuthed ? <NavItem to="/dashboard" title="Dashboard" icon="home" expanded={expanded} /> : null}
      {isAuthed ? <NavItem to="/environments" title="Environments" icon="settings" expanded={expanded} /> : null}
      {isAuthed && isAdmin ? <NavItem to="/admin/users" title="Admin" icon="user" expanded={expanded} /> : null}
      <NavItem to="/help" title="Help" icon="other" expanded={expanded} />
      {!isAuthed ? <NavItem to="/login" title="Login" icon="user" expanded={expanded} /> : null}
    </div>
  )
}
