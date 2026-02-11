export type EnvironmentSummary = {
  id: string
  name: string
}

export type TomcatEnvironmentStatus = 'UNKNOWN' | 'OK' | 'BLOCK'

export type DashboardEnvironment = {
  id: string
  name: string
  tomcatTargets: number
  tomcatOk: number
  tomcatError: number
  tomcatWebappsTotal: number
  tomcatLastScanAt: string | null
  tomcatStatus: TomcatEnvironmentStatus
  actuatorTargets: number
  actuatorUp: number
  actuatorDown: number
  actuatorError: number
  actuatorLastScanAt: string | null
  actuatorStatus: TomcatEnvironmentStatus
}

export type TomcatScanOutcomeKind = 'SUCCESS' | 'ERROR'
export type TomcatScanErrorKind = 'AUTH' | 'CONNECTIVITY' | 'TIMEOUT' | 'HTTP' | 'PARSE' | 'UNKNOWN'
export type TomcatRole = 'PAYMENTS' | 'SERVICES' | 'AUTH'

export type TomcatTargetState = {
  scannedAt: string
  outcomeKind: TomcatScanOutcomeKind
  errorKind: TomcatScanErrorKind | null
  errorMessage: string | null
  webapps: string[]
}

export type TomcatTarget = {
  id: string
  serverId: string
  serverName: string
  role: TomcatRole
  baseUrl: string
  port: number
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

export type UserSummary = {
  id: string
  username: string
  displayName: string
  roles: string[]
  active: boolean
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
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
    body: body ? JSON.stringify(body) : undefined,
    signal,
  })
  return readJsonOrThrow<T>(response)
}

export async function fetchEnvironments(signal?: AbortSignal): Promise<EnvironmentSummary[]> {
  const response = await fetch('/api/v1/environments', { signal })
  return readJsonOrThrow<EnvironmentSummary[]>(response)
}

export async function fetchDashboardEnvironments(signal?: AbortSignal): Promise<DashboardEnvironment[]> {
  const response = await fetch('/api/v1/dashboard/environments', { signal })
  return readJsonOrThrow<DashboardEnvironment[]>(response)
}

export async function fetchTomcatTargets(environmentId: string, signal?: AbortSignal): Promise<TomcatTarget[]> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets`, { signal })
  return readJsonOrThrow<TomcatTarget[]>(response)
}

export async function fetchServers(environmentId: string, signal?: AbortSignal): Promise<Server[]> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/servers`, { signal })
  return readJsonOrThrow<Server[]>(response)
}

export async function createServer(
  environmentId: string,
  request: ServerCreateRequest,
  signal?: AbortSignal,
): Promise<Server> {
  return postJsonOrThrow<Server>(`/api/v1/environments/${encodeURIComponent(environmentId)}/servers`, request, signal)
}

export async function createTomcatTarget(
  environmentId: string,
  request: TomcatTargetCreateRequest,
  signal?: AbortSignal,
): Promise<TomcatTarget> {
  return postJsonOrThrow<TomcatTarget>(`/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets`, request, signal)
}

export async function scanEnvironmentTomcats(environmentId: string, signal?: AbortSignal): Promise<TomcatTarget[]> {
  return postJsonOrThrow<TomcatTarget[]>(`/api/v1/environments/${encodeURIComponent(environmentId)}/tomcat-targets/scan`, undefined, signal)
}

export async function fetchActuatorTargets(environmentId: string, signal?: AbortSignal): Promise<ActuatorTarget[]> {
  const response = await fetch(`/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets`, { signal })
  return readJsonOrThrow<ActuatorTarget[]>(response)
}

export async function createActuatorTarget(
  environmentId: string,
  request: ActuatorTargetCreateRequest,
  signal?: AbortSignal,
): Promise<ActuatorTarget> {
  return postJsonOrThrow<ActuatorTarget>(`/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets`, request, signal)
}

export async function scanEnvironmentActuators(environmentId: string, signal?: AbortSignal): Promise<ActuatorTarget[]> {
  return postJsonOrThrow<ActuatorTarget[]>(`/api/v1/environments/${encodeURIComponent(environmentId)}/actuator-targets/scan`, undefined, signal)
}

export async function fetchUsers(signal?: AbortSignal): Promise<UserSummary[]> {
  const response = await fetch('/api/v1/admin/users', { signal })
  return readJsonOrThrow<UserSummary[]>(response)
}

export async function fetchBackendHealth(signal?: AbortSignal): Promise<{ status: string }> {
  const response = await fetch('/actuator/health', { signal })
  return readJsonOrThrow<{ status: string }>(response)
}
