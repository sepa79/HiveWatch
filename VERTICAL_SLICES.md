# HiveWatch — Vertical Slices (tracking)

Goal: close real end-to-end loops (config → scan → normalized output → dashboard) on the `dummy-stack` with real Tomcats.

## Slice 1 — 1 Tomcat target → webapps list
- [x] DB: `TomcatTarget` (explicit adapter + explicit settings; no fallbacks)
- [x] Backend API: CRUD for targets scoped to Environment
- [x] Adapter: Tomcat Manager HTML fetch + parse + normalize (timeouts + error classification)
- [x] Manual scan: “Scan now” (per target + per environment)
- [x] UI: Environments → add target, Environment detail → show targets + last result
- [x] UI: Dashboard → show env status + apps count (from latest scan state)
- [x] Tests: parser unit test + backend context/DB integration

## Slice 2 — Grouping: Environment → Server → 3 Tomcats (same base URL, different ports)
- [ ] Domain: `Server` grouping + explicit roles (`payments/services/auth`)
- [ ] UI: drill-down like in assumptions (expand groups/servers)
- [ ] Normalization: group results by server + role

## Slice 3 — Microservices mock (Actuator HTTP)
- [ ] Adapter: explicit actuator endpoints (`/health`, `/info`, selected `/metrics/*`)
- [ ] UI: show microservices separately + aggregated status

## Slice 4 — Decision engine (OK/WARN/BLOCK) + snapshot
- [ ] Policy engine: weighted rules + unit tests
- [ ] Snapshot DTO: compact failure snapshot for PocketHive Pull API
- [ ] Dashboard uses Decision (not “reachable”)

## Slice 5 — User visibility + admin config
- [ ] Auth: OIDC/JWT skeleton (server-side enforcement)
- [ ] Admin UI: users + environment visibility
- [ ] Dashboard filtered per user (no placeholder visibility)
