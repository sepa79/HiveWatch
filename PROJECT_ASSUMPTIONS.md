# Project Assumptions - Health Checker (TomcatScanner)

## 1. Goal
Build a standalone Health Checker that monitors technical environments and exposes machine-readable health for automation.

Primary goals:
- Human monitoring dashboard (NFT, Regression, other teams, Environment Manager).
- Reliable pull-based health decisions for automation (PocketHive, Jenkins, other runners).

## 2. Scope

### 2.1 In scope (v1)
- Standalone app running in Docker.
- Multi-environment support from day one.
- Web UI + API for scan configuration and diagnostics.
- User/team management and role-based access.
- Favorites + team/all visibility filters.
- First adapters:
  - Tomcat Manager HTML (`/manager` or `/manager/html`)
  - HTTP Actuator endpoint checks
- Weighted decision engine with explicit thresholds/rules.
- Config revisioning + rollback + audit.
- Retention default 4 weeks (configurable).
- Local dummy stack for integration testing.

### 2.2 Out of scope (v1)
- Automatic fallback between adapters/protocols.
- Hard coupling to PocketHive internal model.
- Full external observability ecosystem in first release (Dynatrace/Nagios later).

## 3. Core Principles
- NFF (No Fraking Fallbacks): no cascading defaults, no silent compatibility shims, no automatic adapter switching.
- Explicit configuration over implicit behavior.
- SSOT contracts for API/config/normalized observation model.
- KISS and fail-fast on missing required data.

## 4. Canonical Domain Model (SSOT)

Main hierarchy:
- `Environment` (top-level domain object)
- `GroupSection` (category, e.g. `Servers`, `Docker`, `AWS`)
- `GroupEntry` (logical node, e.g. `Sites`, `Batch`, `Touchpoint`)
- `Subgroup` (service slot, e.g. `Payments`, `WebServices`, `Security`)
- `Endpoint` (typed scan target: `TOMCAT_MANAGER_HTML`, `ACTUATOR_HTTP`, future types)
- `Observation` (scan result)
- `Decision` (aggregated verdict)

Operational meaning:
- Scanner runs against endpoints.
- UI groups by environment -> group section -> group entry -> subgroup.
- Decisions aggregate from endpoint level up to environment level.
- Change history is stored as `ChangeEvent` / `Incident`.

## 5. Visibility and Access Model
- Environment can be assigned to zero or more teams.
- Users belong to zero or more teams.
- Environment visibility is resolved server-side from team membership + role.
- No ownership model in v1 (environments are shared resources).
- Favorites are per-user preferences only.

## 6. Human vs Machine Outcome Model
- Human dashboard status: `HEALTHY`, `DEGRADED`, `UNHEALTHY`, `UNKNOWN`.
- Machine decision for automation: `CONTINUE`, `WARN_ONLY`, `ABORT`.
- Mapping from rule outcomes to statuses is explicit and configurable.
- Internal rule severities can stay `OK/WARN/BLOCK`.

## 7. Adapters and Extensibility

### 7.1 Current adapters
- Tomcat Manager HTML parser.
- Actuator HTTP checks (health/status style endpoints).

### 7.2 Planned adapters
- Tomcat Manager text API (future).
- Docker service checks.
- AWS service checks.
- Metrics providers (Dynatrace, Nagios, etc.).

### 7.3 Adapter contract
- Each adapter emits common normalized `Observation`.
- Adapter type is explicit in endpoint config.
- No runtime adapter/protocol fallback.

## 8. Configuration and UI Assumptions
- Configuration editable via UI and API.
- Configuration is versioned and immutable per revision.
- Activation and rollback are explicit operations.
- Every mutation is audited (`who`, `when`, `what`).
- UI baseline flow:
  - normal login (no manual token-entry screen),
  - environment list,
  - environment config editor (groups/subgroups/endpoints),
  - environment health dashboard and drill-down.

## 9. Security and Auth
- Target model: OIDC + JWT.
- Browser flow: Authorization Code + PKCE.
- Machine-to-machine flow: Client Credentials.
- Roles baseline:
  - `guest` (public read-only subset),
  - `viewer`,
  - `operator`,
  - `env-manager`,
  - `admin`,
  - `service-account`.
- Authorization is always enforced server-side.

## 10. Data and Retention
- Separate logical stores for:
  - current state,
  - change events/incidents,
  - snapshots (time-sampled),
  - policy/rules,
  - config revisions and audit.
- Default retention: 28 days, configurable.
- Snapshot retention configurable independently.
- Persist change-only history where possible.

## 11. Scheduling and Execution
- Scan interval is configurable per endpoint (or inherited explicitly from configured scope).
- Timeouts/retries/concurrency are explicit config fields.
- Execution result includes explicit error classification.

## 12. Testing and Dummy Stack
- Keep deterministic local dummy stack with:
  - multiple environments,
  - multiple group entries,
  - multiple Tomcats per logical server,
  - actuator endpoints across service groups.
- Use scenarios for decision behavior validation (`all-green`, `service-down`, `bad-version`, `cpu-high`, intermittent timeout).

## 13. PocketHive Integration Boundary
- Integration style: Pull API.
- PH consumes high-level health/decision and compact failure context.
- PH does not need full topology details by default.
- PH may poll frequently during test execution.
- PH can store periodic snapshots in PH storage for test reports.

Boundary rule:
- In HC core model, top-level object is `Environment`.
- At integration boundary, PH `SUT` maps to HC `Environment` (alias/mapping layer).
- No hard dependency on PH internal schema.

## 14. Initial API Surface (draft)

Environment-facing API:
- `GET /api/v1/environments?scope=favorites|team|all`
- `GET /api/v1/environments/{environmentId}/status`
- `GET /api/v1/environments/{environmentId}/state-hash`
- `GET /api/v1/environments/{environmentId}/snapshots?from=<ts>&to=<ts>&step=<duration>`
- `GET /api/v1/environments/{environmentId}/incidents/latest`

Future:
- `POST /api/v1/suts/{sutId}/decision/evaluate` (custom criteria from orchestrator/tests)
- `GET /api/v1/environments/{environmentId}/health?level=full`

