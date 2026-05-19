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

## HiveForge POC deployment

HiveForge manages this repo through `hiveforge.yaml`.

The declared HiveForge lifecycle actions are `deploy`, `remove`, `purge`, `update`, and `upgrade`.

HiveWatch declares two explicit HiveForge profiles:

- `normal` - HiveWatch service and PostgreSQL only.
- `test` - `normal` plus the dummy Tomcat and actuator stack.

Deploy:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service deploy
```

The deploy action runs `deploy/hiveforge/deploy.yml`, verifies Docker and Docker Compose access, renders `deploy/hiveforge/templates/docker-compose.yml.j2` into `.hiveforge/docker-compose.yml`, creates the `hivewatch-dev` Docker network when it is missing, and starts the rendered Compose stack under the explicit Docker Compose project `hivewatch-poc`. Container names are owned by Docker Compose project naming.

Remove:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service remove
```

The remove action renders the Compose template, then stops/removes the `hivewatch-poc` Compose stack without deleting Docker volumes.

Purge:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 HIVEFORGE_PURGE_APPROVED=true hiveforge run-action hivewatch main service purge
```

The purge action renders the Compose template and removes the `hivewatch-poc` Compose stack including Docker volumes.

Update:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service update
```

The update action renders the Compose template, pulls the currently declared images, and reconciles the stack without deleting volumes.

Upgrade:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 HIVEFORGE_UPGRADE_APPROVED=true hiveforge run-action hivewatch main service upgrade
```

The upgrade action renders the Compose template, requires explicit upgrade approval, pulls the declared images, and reconciles the stack.
