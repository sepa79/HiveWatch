import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { RequireAuth } from './components/RequireAuth'
import { AdminUsersPage } from './pages/AdminUsersPage'
import { DashboardMatrixPage, DashboardOverviewPage } from './pages/DashboardPage'
import { DiagnosticsPage } from './pages/DiagnosticsPage'
import { DockerClusterPage } from './pages/DockerClusterPage'
import { DockerServicePage } from './pages/DockerServicePage'
import { EnvironmentDetailPage } from './pages/EnvironmentDetailPage'
import { EnvironmentsPage } from './pages/EnvironmentsPage'
import { HelpPage } from './pages/HelpPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { AuthProvider } from './lib/authContext'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<AppShell />}>
          <Route path="/" element={<HomePage />} />
          <Route
            path="/dashboard"
            element={
              <RequireAuth>
                <DashboardOverviewPage />
              </RequireAuth>
            }
          />
          <Route
            path="/dashboard/matrix"
            element={
              <RequireAuth>
                <DashboardMatrixPage />
              </RequireAuth>
            }
          />
          <Route
            path="/dashboard/matrix/:environmentId"
            element={
              <RequireAuth>
                <DashboardMatrixPage />
              </RequireAuth>
            }
          />
          <Route
            path="/dashboard/docker/:environmentId/:serverId"
            element={
              <RequireAuth>
                <DockerClusterPage />
              </RequireAuth>
            }
          />
          <Route
            path="/dashboard/docker/:environmentId/:serverId/services/:targetId"
            element={
              <RequireAuth>
                <DockerServicePage />
              </RequireAuth>
            }
          />

          <Route
            path="/environments"
            element={
              <RequireAuth>
                <EnvironmentsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/environments/:environmentId"
            element={
              <RequireAuth>
                <EnvironmentDetailPage />
              </RequireAuth>
            }
          />

          <Route
            path="/admin/users"
            element={
              <RequireAuth>
                <AdminUsersPage />
              </RequireAuth>
            }
          />
          <Route path="/diagnostics" element={<DiagnosticsPage />} />
          <Route path="/help" element={<HelpPage />} />
          <Route path="/login" element={<LoginPage />} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
