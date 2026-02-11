import { Link } from 'react-router-dom'

export function HomePage() {
  const fullLogoSrc = `${import.meta.env.BASE_URL}logo.svg`
  return (
    <div className="page homePage">
      <div className="homeHero">
        <img className="homeLogoFull" src={fullLogoSrc} alt="HiveWatch" />
        <div className="muted" style={{ textAlign: 'center' }}>
          Environment monitor (WIP). UI shell based on PocketHive UI v2.
        </div>
      </div>

      <div className="tileGrid" style={{ marginTop: 14 }}>
        <Link className="tile" to="/dashboard">
          <div className="tileTitle">Dashboard</div>
          <div className="tileDesc">Environments visible for the current user.</div>
        </Link>
        <Link className="tile" to="/environments">
          <div className="tileTitle">Environments</div>
          <div className="tileDesc">Configure environments, targets, and adapters.</div>
        </Link>
        <Link className="tile" to="/admin/users">
          <div className="tileTitle">Users</div>
          <div className="tileDesc">User visibility and role configuration.</div>
        </Link>
        <Link className="tile" to="/help">
          <div className="tileTitle">Help</div>
          <div className="tileDesc">Docs and project notes.</div>
        </Link>
      </div>
    </div>
  )
}

