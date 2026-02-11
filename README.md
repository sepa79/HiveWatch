# HiveWatch

Standalone, multi-SUT Health Checker with PocketHive Pull API integration (work in progress).

## Quick start (local)

Prereqs: Docker + Docker Compose v2.

1) Start dummy infrastructure (real Tomcats + mock microservices):

```bash
./tools/run-dummy-stack.sh up
```

2) Build and run HiveWatch (backend serves UI):

```bash
./build-hive-watch.sh --restart
```

Open:
- UI: `http://localhost:18180/`
- Dashboard: `http://localhost:18180/dashboard`
- Matrix: `http://localhost:18180/dashboard/matrix`

Auth (dev mode): UI sends `X-HW-Username` header from local storage.
Seeded users:
- `local-admin`
- `local-operator`
- `local-viewer`

## Dev reset / reseed DB

This project treats schema as rewriteable during development.

```bash
./build-hive-watch.sh --dev --restart
```

## Notes

- Scans run automatically on a background scheduler (no “scan now” buttons in UI).
- Dummy-stack endpoints and ports are documented in `dummy-stack/README.md`.

