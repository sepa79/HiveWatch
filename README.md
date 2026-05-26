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

Use the current HiveForge operator flow from the HiveForge project docs:

- [HiveForge README](https://github.com/sepa79/HiveForge#how-to-use)
- [First Swarm quickstart](https://github.com/sepa79/HiveForge/blob/main/docs/quickstart/first-swarm.md)

HiveWatch does not duplicate HiveForge MCP or REST usage here. Treat the
HiveForge docs as the source of truth for project registration, environment
policy, requirement validation, and action execution.

The declared HiveForge lifecycle actions are `deploy`, `remove`, `purge`,
`update`, and `upgrade`.

HiveWatch declares three portable HiveForge profiles:

- `docker-single` - HiveWatch service and PostgreSQL on one Docker engine.
- `docker-single-test` - `docker-single` plus the dummy Tomcat and actuator stack.
- `docker-swarm` - HiveWatch service and PostgreSQL on Docker Swarm.

HiveWatch's example deployment publishes HTTP on port `18180` and uses the
`latest` GHCR image tags declared by the project playbooks. The selected
HiveForge profile is an action parameter; HiveForge passes it to the playbooks
as `HIVEFORGE_PROFILE`.

The rendered Compose file uses Docker-managed default networking. Compose
profiles get a Compose project network, and Swarm profiles get a stack network.
HiveForge only runs the project playbooks; Docker owns network creation and
removal.

The deploy action runs `deploy/hiveforge/deploy.yml`, verifies Docker access, renders `deploy/hiveforge/templates/docker-compose.yml.j2` into `.hiveforge/docker-compose.yml`, and starts the rendered stack. Compose profiles use the explicit Docker Compose project `hivewatch-poc`; Swarm profiles use the explicit Docker stack `hivewatch-poc`.

Swarm profiles publish the HiveWatch HTTP port through Swarm ingress and connect
the service to PostgreSQL through `tasks.postgres`. The profile is intentionally
not pinned to a concrete Swarm node.

The remove action renders the Compose template, then stops/removes the `hivewatch-poc` Compose project or Swarm stack without deleting Docker volumes.

The purge action renders the Compose template and removes the `hivewatch-poc` Compose project or Swarm stack including Docker volumes. Swarm purge waits for stack removal before deleting the stack-scoped PostgreSQL volume.

The update action renders the Compose template, pulls the currently declared images, and reconciles the Compose project or Swarm stack without deleting volumes.

The upgrade action renders the Compose template, requires explicit upgrade approval, pulls the declared images, and reconciles the Compose project or Swarm stack.
