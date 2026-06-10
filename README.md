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
- UI: `http://localhost:4020/`
- Dashboard: `http://localhost:4020/dashboard`
- Matrix: `http://localhost:4020/dashboard/matrix`

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

HiveWatch's example deployment publishes HTTP on port `4020` and uses the
`latest` GHCR image tags declared by the project playbooks. The selected
HiveForge profile is an action parameter; HiveForge passes it to the playbooks
as `HIVEFORGE_PROFILE`.

The rendered Compose file uses Docker-managed default networking. Compose
profiles get a Compose project network, and Swarm profiles get a stack network.
HiveForge runs the project playbooks in render-only mode, then HiveForge owns
Docker execution, network creation, and removal.

The deploy action runs `deploy/hiveforge/deploy.yml`, renders
`deploy/hiveforge/templates/docker-compose.yml.j2` into the file declared by
`HIVEFORGE_RENDERED_COMPOSE_FILE`, validates the rendered Compose file, and then
HiveForge starts the rendered stack.

Swarm profiles publish the HiveWatch HTTP port through Swarm ingress and connect
the service to PostgreSQL through `tasks.postgres`. The profile is intentionally
not pinned to a concrete Swarm node.

The remove action fails explicitly until HiveForge provides owner-side Docker
remove execution. Project Ansible does not have Docker access in the HiveForge
`0.5` contract.

The purge action fails explicitly until HiveForge provides owner-side Docker
purge execution. Project Ansible does not have Docker access in the HiveForge
`0.5` contract.

The update action renders the Compose template into
`HIVEFORGE_RENDERED_COMPOSE_FILE`; HiveForge reconciles the stack without
deleting volumes.

The upgrade action renders the Compose template into
`HIVEFORGE_RENDERED_COMPOSE_FILE` after explicit upgrade approval; HiveForge
reconciles the stack.
