import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { DashboardPage } from './pages/DashboardPage'
import { DiagnosticsPage } from './pages/DiagnosticsPage'
import { EnvironmentDetailPage } from './pages/EnvironmentDetailPage'
import { EnvironmentsPage } from './pages/EnvironmentsPage'
import { HelpPage } from './pages/HelpPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/dashboard" element={<DashboardPage />} />

        <Route path="/environments" element={<EnvironmentsPage />} />
        <Route path="/environments/:environmentId" element={<EnvironmentDetailPage />} />

        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/diagnostics" element={<DiagnosticsPage />} />
        <Route path="/help" element={<HelpPage />} />
        <Route path="/login" element={<LoginPage />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  )
}
