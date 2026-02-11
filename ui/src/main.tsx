import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './styles.css'
import { installTheme } from './lib/theme'
import { detectUiBasename } from './lib/routing/basename'

installTheme()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter basename={detectUiBasename(window.location.pathname) || undefined}>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
