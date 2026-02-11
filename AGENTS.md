# AGENTS.md — TomcatScanner / Health Checker

## 1) Scope and intent
This file defines enforceable rules for humans and AI contributors in this repository.
The project target is a standalone, multi-SUT Health Checker with Pull API integration for PocketHive.

Core direction:
- Standalone app first.
- PocketHive integration over Pull API.
- Configuration and policy editing via Web UI and API.
- First adapter: Tomcat Manager HTML.
- Future adapters: Tomcat text API, Docker/AWS services, metrics providers (Dynatrace/Nagios, etc.).

## 2) Non-negotiable rules
- NFF (No Fraking Fallbacks).
  - Do not add cascading defaults, heuristic fallback chains, or silent compatibility shims.
  - Every target must declare explicit adapter and explicit settings.
  - Do not auto-switch from one adapter/protocol to another.
  - Prefer explicit failure and explicit configuration over auto-recovery logic.
  - Rationale: fallback-heavy code quickly becomes hard to reason about and hard to trust.
- No implicit backward compatibility.
  - Breaking changes are acceptable unless compatibility is explicitly required.
- No implicit Optional for core state/config flags.
  - Use explicit required fields and explicit enums/states.
- SSOT for contracts.
  - One canonical schema/DTO per API/event/config contract.
  - Do not keep duplicate validators/parsers for the same contract.
- KISS.
  - Prefer straightforward, maintainable implementations over clever abstractions.

## 3) Domain boundaries (must keep)
Primary domain model:
- `SUT` -> `Environment` -> `Target` -> `Observation` -> `Decision`.
- `Incident`/`ChangeEvent` for historical deltas.

Boundary rules:
- Adapter layer collects raw health signals.
- Policy engine computes verdict (`OK`/`WARN`/`BLOCK`) using weighted rules.
- Integration API exposes summarized status for PocketHive.
- UI handles configuration and diagnostics only, not domain decision logic.

## 4) Java backend standards
- Java 21 LTS.
- Spring Boot 3.5.x baseline.
- PostgreSQL + Flyway for schema/versioned migrations.
- Use records for API DTOs and persisted contract payloads.
- Lombok allowed for internal domain/application code only.
- Enforce timeouts, bounded retries, and clear error classification in adapters.
- Keep package structure by feature/domain, not by technical layer only.
- Use constructor injection only.
- Avoid static mutable state.

Testing:
- Unit tests for policy engine and normalization.
- Integration tests with Testcontainers for DB and adapter-facing dependencies.
- Contract tests for adapter parity (same normalized output shape).

## 5) UI standards
- React + TypeScript (strict mode) + Vite.
- UI must be configuration-driven (dynamic targets/columns/views).
- Keep API contract types centralized and reused across app.
- No hidden business rules in components.
- Forms require validation and explicit error messages.
- Keep role-aware views (`admin`/`operator`/`viewer`) and avoid client-side-only security assumptions.
- Accessibility baseline: keyboard navigation, labels, and readable status indicators.

## 6) Security and auth
- Primary auth model: OIDC + JWT.
- UI: Authorization Code + PKCE.
- Machine-to-machine (PocketHive -> Health Checker): Client Credentials.
- Roles/scopes must be explicit and validated server-side.
- Secrets must not be stored in plaintext in repo or logs.

## 7) Data and retention
- Keep separate stores for:
  - current state,
  - change events/history,
  - expected state/policies,
  - config revisions.
- Retention is configurable; default is 4 weeks.
- Configuration must be versioned and rollback-capable.
- Every config mutation requires audit data (`who`, `when`, `what`).

## 8) API and integration constraints
- Integration style with PocketHive is Pull.
- PocketHive must be able to fetch:
  - high-level health/decision,
  - compact failure snapshot,
  - link back to Health Checker details.
- API responses must be stable, versioned, and backward-compatibility decisions must be explicit.

## 9) Git and change hygiene
- Do not push from AI agents.
- Do not commit unless explicitly requested by a human.
- Keep changes scoped; do not mix unrelated refactors with feature work.
- Update docs/contracts first when changing API, policy semantics, or config schema.

## 10) When in doubt
- Prefer explicitness over magic.
- Prefer single-source contracts over parallel representations.
- Raise ambiguities early and resolve them in docs before coding.
