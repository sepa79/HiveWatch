import { withDevAuthHeaders } from './auth'

export type EnvironmentSummary = {
  id: string
  name: string
}

export type TomcatEnvironmentStatus = 'UNKNOWN' | 'OK' | 'BLOCK'
export type DecisionVerdict = 'OK' | 'WARN' | 'BLOCK' | 'UNKNOWN'
export type HiveWatchRole = 'ADMIN' | 'OPERATOR' | 'VIEWER'

export type DashboardGroupStatus = 'OK' | 'BLOCK' | 'UNKNOWN'

export type DashboardGroupSummary = {
  status: DashboardGroupStatus
  targets: number
  lastScanAt: string | null
}

export type DashboardEnvironmentSummary = {
  tomcats: DashboardGroupSummary
  docker: DashboardGroupSummary
  aws: DashboardGroupSummary
  verdict: DecisionVerdict
  blockIssues: number
  warnIssues: number
  unknownIssues: number
  evaluatedAt: string
}

export type DashboardCellKind = 'VALUE' | 'ERROR' | 'UNKNOWN'
export type DashboardCell = {
  kind: DashboardCellKind
  text: string | null
  title: string | null
}

export type DashboardColumn = {
  key: string
  label: string
}

export type DashboardRowStatus = 'OK' | 'BLOCK' | 'UNKNOWN'
export type DashboardRow = {
  id: string
  label: string
  link: string | null
  cells: DashboardCell[]
  status: DashboardRowStatus
}

export type DashboardSectionKind = 'TOMCATS' | 'DOCKER' | 'AWS'
export type DashboardSection = {
  kind: DashboardSectionKind
  title: string
  columns: DashboardColumn[]
  rows: DashboardRow[]
}

export type DashboardEnvironmentBlock = {
  id: string
  name: string
  summary: DashboardEnvironmentSummary
  sections: DashboardSection[]
}

export type Dashboard = {
  environments: DashboardEnvironmentBlock[]
}

export type TomcatScanOutcomeKind = 'SUCCESS' | 'ERROR'
export type TomcatScanErrorKind = 'AUTH' | 'CONNECTIVITY' | 'TIMEOUT' | 'HTTP' | 'PARSE' | 'UNKNOWN'
export type TomcatRole = 'PAYMENTS' | 'SERVICES' | 'AUTH'

export type TomcatTargetState = {
  scannedAt: string
  outcomeKind: TomcatScanOutcomeKind
  errorKind: TomcatScanErrorKind | null
  errorMessage: string | null
  tomcatVersion: string | null
  javaVersion: string | null
  os: string | null
  webapps: TomcatWebapp[]
}

export type TomcatWebapp = {
  path: string
  name: string
  version: string | null
}

export type TomcatTarget = {
  id: string
  serverId: string
  serverName: string
  role: TomcatRole
  baseUrl: string
  port: number
  username: string
  connectTimeoutMs: number
  requestTimeoutMs: number
  state: TomcatTargetState | null
}

export type TomcatTargetCreateRequest = {
  serverId: string
  role: TomcatRole
  baseUrl: string
  port: number
  username: string
  password: string
  connectTimeoutMs: number
  requestTimeoutMs: number
}

export type ActuatorTargetState = {
  scannedAt: string
  outcomeKind: TomcatScanOutcomeKind
  errorKind: TomcatScanErrorKind | null
  errorMessage: string | null
  healthStatus: string | null
  appName: string | null
  buildVersion: string | null
  cpuUsage: number | null
  memoryUsedBytes: number | null
}

export type ActuatorTarget = {
  id: string
  serverId: string
  serverName: string
  role: TomcatRole
  baseUrl: string
  port: number
  profile: string
  connectTimeoutMs: number
  requestTimeoutMs: number
  state: ActuatorTargetState | null
}

export type ActuatorTargetCreateRequest = {
  serverId: string
  role: TomcatRole
  baseUrl: string
  port: number
  profile: string
  connectTimeoutMs: number
  requestTimeoutMs: number
}

export type Server = {
  id: string
  environmentId: string
  name: string
}

export type ServerCreateRequest = {
  name: string
}

export type ServerUpdateRequest = {
  name: string
}

export type UserSummary = {
  id: string
  username: string
  displayName: string
  roles: HiveWatchRole[]
  active: boolean
}

export type UserCreateRequest = {
  username: string
  displayName: string
  roles: HiveWatchRole[]
  active: boolean
}

export type UserEnvironmentVisibilityUpdateRequest = {
  environmentIds: string[]
}

export type DecisionIssue = {
  severity: DecisionVerdict
  kind: 'TOMCAT_TARGET' | 'ACTUATOR_TARGET'
  targetId: string
  serverName: string
  role: TomcatRole
  label: string
  message: string
}

export type EnvironmentStatus = {
  environmentId: string
  environmentName: string
  verdict: DecisionVerdict
  evaluatedAt: string
  issues: DecisionIssue[]
}

async function readJsonOrThrow<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  return (await response.json()) as T
}

async function postJsonOrThrow<T>(url: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: withDevAuthHeaders(body ? { 'Content-Type': 'application/json' } : undefined),
    body: body ? JSON.stringify(body) : undefined,
    signal,
  })
  return readJsonOrThrow<T>(response)
}

async function putJsonOrThrow<T>(url: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, {
    method: 'PUT',
    headers: withDevAuthHeaders(body ? { 'Content-Type': 'application/json' } : undefined),
    body: body ? JSON.stringify(body) : undefined,
    signal,
  })
  return readJsonOrThrow<T>(response)
}

export async function fetchEnvironments(signal?: AbortSignal): Promise<EnvironmentSummary[]> {
  const response = await fetch('/api/v1/environments', { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<EnvironmentSummary[]>(response)
}

export async function fetchDashboard(signal?: AbortSignal): Promise<Dashboard> {
  const response = await fetch('/api/v1/dashboard', { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<Dashboard>(response)
}

export async function fetchTomcatTargets(environmentId: string, signal?: AbortSignal): Promise<TomcatTarget[]> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets`, {
    signal,
    headers: withDevAuthHeaders(),
  })
  return readJsonOrThrow<TomcatTarget[]>(response)
}

export async function fetchServers(environmentId: string, signal?: AbortSignal): Promise<Server[]> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/servers`, { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<Server[]>(response)
}

export async function fetchEnvironmentStatus(environmentId: string, signal?: AbortSignal): Promise<EnvironmentStatus> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/status`, { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<EnvironmentStatus>(response)
}

export async function createServer(
  environmentId: string,
  request: ServerCreateRequest,
  signal?: AbortSignal,
): Promise<Server> {
  return postJsonOrThrow<Server>(`/api/v1/environments/${encodeURIComponent(environmentId)}/servers`, request, signal)
}

export async function updateServer(
  environmentId: string,
  serverId: string,
  request: ServerUpdateRequest,
  signal?: AbortSignal,
): Promise<Server> {
  return putJsonOrThrow<Server>(
    `/api/v1/environments/${encodeURIComponent(environmentId)}/servers/${encodeURIComponent(serverId)}`,
    request,
    signal,
  )
}

export async function deleteServer(environmentId: string, serverId: string, signal?: AbortSignal): Promise<void> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/servers/${encodeURIComponent(serverId)}`, {
    method: 'DELETE',
    signal,
    headers: withDevAuthHeaders(),
  })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
}

export async function createTomcatTarget(
  environmentId: string,
  request: TomcatTargetCreateRequest,
  signal?: AbortSignal,
): Promise<TomcatTarget> {
  return postJsonOrThrow<TomcatTarget>(`/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets`, request, signal)
}

export async function updateTomcatTarget(
  environmentId: string,
  targetId: string,
  request: TomcatTargetCreateRequest,
  signal?: AbortSignal,
): Promise<TomcatTarget> {
  return putJsonOrThrow<TomcatTarget>(
    `/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets/${encodeURIComponent(targetId)}`,
    request,
    signal,
  )
}

export async function deleteTomcatTarget(environmentId: string, targetId: string, signal?: AbortSignal): Promise<void> {
  const response = await fetch(
    `/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets/${encodeURIComponent(targetId)}`,
    { method: 'DELETE', signal, headers: withDevAuthHeaders() },
  )
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
}

export async function fetchActuatorTargets(environmentId: string, signal?: AbortSignal): Promise<ActuatorTarget[]> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets`, {
    signal,
    headers: withDevAuthHeaders(),
  })
  return readJsonOrThrow<ActuatorTarget[]>(response)
}

export async function createActuatorTarget(
  environmentId: string,
  request: ActuatorTargetCreateRequest,
  signal?: AbortSignal,
): Promise<ActuatorTarget> {
  return postJsonOrThrow<ActuatorTarget>(`/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets`, request, signal)
}

export async function updateActuatorTarget(
  environmentId: string,
  targetId: string,
  request: ActuatorTargetCreateRequest,
  signal?: AbortSignal,
): Promise<ActuatorTarget> {
  return putJsonOrThrow<ActuatorTarget>(
    `/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets/${encodeURIComponent(targetId)}`,
    request,
    signal,
  )
}

export async function deleteActuatorTarget(environmentId: string, targetId: string, signal?: AbortSignal): Promise<void> {
  const response = await fetch(
    `/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets/${encodeURIComponent(targetId)}`,
    { method: 'DELETE', signal, headers: withDevAuthHeaders() },
  )
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
}

export async function fetchUsers(signal?: AbortSignal): Promise<UserSummary[]> {
  const response = await fetch('/api/v1/admin/users', { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<UserSummary[]>(response)
}

export async function createUser(request: UserCreateRequest, signal?: AbortSignal): Promise<UserSummary> {
  return postJsonOrThrow<UserSummary>('/api/v1/admin/users', request, signal)
}

export async function fetchMe(signal?: AbortSignal): Promise<UserSummary> {
  const response = await fetch('/api/v1/me', { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<UserSummary>(response)
}

export async function fetchAdminEnvironments(signal?: AbortSignal): Promise<EnvironmentSummary[]> {
  const response = await fetch('/api/v1/admin/environments', { signal, headers: withDevAuthHeaders() })
  return readJsonOrThrow<EnvironmentSummary[]>(response)
}

export async function fetchUserEnvironmentVisibility(
  userId: string,
  signal?: AbortSignal,
): Promise<UserEnvironmentVisibilityUpdateRequest> {
  const response = await fetch(`/api/v1/admin/users/${encodeURIComponent(userId)}/environment-visibility`, {
    signal,
    headers: withDevAuthHeaders(),
  })
  return readJsonOrThrow<UserEnvironmentVisibilityUpdateRequest>(response)
}

export async function replaceUserEnvironmentVisibility(
  userId: string,
  request: UserEnvironmentVisibilityUpdateRequest,
  signal?: AbortSignal,
): Promise<UserEnvironmentVisibilityUpdateRequest> {
  return putJsonOrThrow<UserEnvironmentVisibilityUpdateRequest>(
    `/api/v1/admin/users/${encodeURIComponent(userId)}/environment-visibility`,
    request,
    signal,
  )
}

export async function fetchBackendHealth(signal?: AbortSignal): Promise<{ status: string }> {
  const response = await fetch('/actuator/health', { signal })
  return readJsonOrThrow<{ status: string }>(response)
}
