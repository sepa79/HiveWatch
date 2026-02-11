# HiveWatch — Vertical Slices (tracking)

Goal: close real end-to-end loops (config → scan → normalized output → dashboard) on the `dummy-stack` with real Tomcats.

Dev note (for now): schema is treated as rewriteable; `./build-hive-watch.sh --dev` resets and reseeds DB. We can lock down real migrations closer to `1.0`.

## Slice 1 — 1 Tomcat target → webapps list
- [x] DB: `TomcatTarget` (explicit adapter + explicit settings; no fallbacks)
- [x] Backend API: CRUD for targets scoped to Environment
- [x] Adapter: Tomcat Manager HTML fetch + parse + normalize (timeouts + error classification)
- [x] Manual scan: “Scan now” (per target + per environment)
- [x] UI: Environments → add target, Environment detail → show targets + last result
- [x] UI: Dashboard → show env status + apps count (from latest scan state)
- [x] Tests: parser unit test + backend context/DB integration

## Slice 2 — Grouping: Environment → Server → 3 Tomcats (same base URL, different ports)
- [x] Domain: `Server` grouping + explicit roles (`payments/services/auth`)
- [x] UI: drill-down (Dashboard + Environment topology)
- [x] Normalization: group results by server + role

## Slice 3 — Microservices mock (Actuator HTTP)
- [x] Adapter: explicit actuator endpoints (`/health`, `/info`, selected `/metrics/*`)
- [x] UI: show microservices separately + aggregated status

## Slice 4 — Decision engine (OK/WARN/BLOCK) + snapshot
- [x] Policy engine: weighted rules + unit tests
- [x] Snapshot DTO: compact failure snapshot for PocketHive Pull API
- [x] Dashboard uses Decision (not “reachable”)

## Slice 5 — User visibility + admin config
- [x] Auth: OIDC/JWT skeleton (server-side enforcement)
- [x] Admin UI: users + environment visibility
- [x] Dashboard filtered per user (no placeholder visibility)
