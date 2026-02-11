import { NavLink, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import { Breadcrumbs } from './Breadcrumbs'
import { ConnectivityIndicator } from './ConnectivityIndicator'
import { UserMenu } from './UserMenu'
import { LogoMark } from './LogoMark'

function TopBarSearch() {
  const inputRef = useRef<HTMLInputElement | null>(null)
  const [query, setQuery] = useState('')
  const [miss, setMiss] = useState(false)

  const runFind = (needle: string, backward: boolean) => {
    const finder = (window as Window & { find?: (...args: any[]) => boolean }).find
    if (!finder) return false
    return finder(needle, false, backward, true, false, false, false)
  }

  const findNext = (backward = false) => {
    const needle = query.trim()
    if (!needle) return
    const found = runFind(needle, backward)
    setMiss(!found)
  }

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'f') {
        event.preventDefault()
        inputRef.current?.focus()
        inputRef.current?.select()
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [])

  return (
    <div className={`topBarSearch ${miss ? 'topBarSearchMiss' : ''}`}>
      <input
        ref={inputRef}
        className="topBarSearchInput"
        placeholder="Search page..."
        value={query}
        onChange={(event) => {
          setQuery(event.target.value)
          setMiss(false)
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            findNext(event.shiftKey)
          }
        }}
      />
      <div className="topBarSearchActions">
        <button
          type="button"
          className="iconButton iconButtonBare"
          title="Find previous"
          aria-label="Find previous"
          onClick={() => findNext(true)}
        >
          ↑
        </button>
        <button
          type="button"
          className="iconButton iconButtonBare"
          title="Find next"
          aria-label="Find next"
          onClick={() => findNext(false)}
        >
          ↓
        </button>
        <button
          type="button"
          className="iconButton iconButtonBare"
          title="Clear search"
          aria-label="Clear search"
          onClick={() => {
            setQuery('')
            setMiss(false)
            inputRef.current?.blur()
          }}
        >
          ×
        </button>
      </div>
    </div>
  )
}

export function TopBar() {
  const navigate = useNavigate()

  return (
    <div className="topBarInner">
      <div className="topBarLeft">
        <NavLink to="/" className="logoLink logoLinkBare" aria-label="HiveWatch home" title="Home">
          <span className="brandMark" aria-hidden="true">
            <LogoMark size={28} />
          </span>
          <span className="brandWordmark" aria-hidden="true">
            <span className="brandWordHive">Hive</span>
            <span className="brandWordPocket">Watch</span>
          </span>
        </NavLink>
        <Breadcrumbs />
      </div>
      <div className="topBarCenter">
        <TopBarSearch />
      </div>
      <div className="topBarRight">
        <ConnectivityIndicator />
        <button
          type="button"
          className="iconButton iconButtonBare"
          title="Help"
          onClick={() => navigate('/help')}
          aria-label="Help"
        >
          <span className="odysseyIcon" aria-hidden="true" />
        </button>
        <UserMenu />
      </div>
    </div>
  )
}
