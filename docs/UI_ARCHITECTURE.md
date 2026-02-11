# HiveWatch UI — Architecture (v0)

This document captures the **UI shell & routing rules** for HiveWatch, aligned with PocketHive UI v2 assumptions (see `PocketHive/docs/ui-v2/UI_V2_FLOW.md`).

## Goals

- **Stable shell** (no unmount flicker) while navigating between pages.
- **URL is the state**: **path-based navigation** (no query params to switch screens/views).
- UI remains **diagnostics + configuration** only; verdict/decision logic stays server-side.
- SSOT for contracts: TypeScript API types map 1:1 to backend DTOs.

## Shell layout

HiveWatch uses the same “IDE-like” shell structure:

- `TopBar` (`ui/src/components/TopBar.tsx`)
  - Logo (Home), breadcrumbs, connectivity, help, user menu.
- `SideNav` (`ui/src/components/SideNav.tsx`)
  - Flat list, collapsible (persisted in session storage).
- `PageToolsBar` (`ui/src/components/PageToolsBar.tsx`)
  - A per-page toolbar region (filters, view toggles, actions).
  - Pages set content via `useToolsBar()` (`ui/src/components/ToolsBarContext.tsx`).
- Content area (`Outlet`) inside `AppShell` (`ui/src/components/AppShell.tsx`)

## Routing rules (hard requirements)

- **Path-based routes** only (no query params to change screens).
- Browser `Back` must work (normal history semantics).
- Screens must be linkable and shareable via their URL.

### Dashboard routes

- High-level overview: `/dashboard`
- Detailed matrix: `/dashboard/matrix`
- Matrix focused on an environment (scroll + highlight): `/dashboard/matrix/:environmentId`

The Dashboard view switch lives in `PageToolsBar` and navigates between these paths.

## Data flow (current)

- Pages call API via `ui/src/lib/hivewatchApi.ts`.
- Dashboard:
  - Overview uses backend aggregation: `GET /api/v1/dashboard/environments`.
  - Matrix currently uses N+1 calls per visible environment:
    - `GET /api/v1/environments`
    - per env: `GET /servers`, `GET /tomcat-targets`, `GET /actuator-targets`
  - Later we will replace this with a single SSOT dashboard payload endpoint.

## Status icons (HAL Eye)

HiveWatch reuses the “HAL Eye” concept as a compact status indicator:

- `ConnectivityIndicator` uses the CSS-based `hal-eye` (`ui/src/components/ConnectivityIndicator.tsx`, `ui/src/styles.css`).
- Dashboard Overview uses the same `hal-eye` rendering for group statuses.
- Click behavior is always **drill-down** (navigate to details), not “run scan”.
