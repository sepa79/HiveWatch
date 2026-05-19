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

HiveWatch declares four explicit HiveForge profiles:

- `normal` - HiveWatch service and PostgreSQL only.
- `test` - `normal` plus the dummy Tomcat and actuator stack.
- `swarm-marax-normal` - HiveWatch service and PostgreSQL on the WSL Swarm smoke node.
- `swarm-proxmox-mgr1-normal` - HiveWatch service and PostgreSQL on the Proxmox Swarm manager smoke node.

All profiles require explicit `HIVEFORGE_PROFILE`, `HW_HTTP_PORT`,
`HIVEWATCH_SERVICE_IMAGE_TAG`, and `HIVEWATCH_DUMMY_IMAGE_TAG`. Swarm profiles
also require `HIVEWATCH_SWARM_NODE`, and the value must match the node declared
by the selected profile.

The rendered Compose file declares `hivewatch-dev` as an external network.
The project playbooks therefore create or validate that network explicitly:
Compose profiles require a bridge network, and Swarm profiles require an overlay
network. HiveForge does not manage Docker networks; it only runs these project
playbooks.

Swarm profiles publish the HiveWatch HTTP port in host mode and connect the
service to PostgreSQL through `tasks.postgres`. Both choices are explicit
project deployment behavior for the tested Swarm environments.

Deploy:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service deploy
```

The deploy action runs `deploy/hiveforge/deploy.yml`, verifies Docker access, renders `deploy/hiveforge/templates/docker-compose.yml.j2` into `.hiveforge/docker-compose.yml`, validates the `hivewatch-dev` Docker network, and starts the rendered stack. Compose profiles use the explicit Docker Compose project `hivewatch-poc`; Swarm profiles use the explicit Docker stack `hivewatch-poc`.

Swarm deploy example:

```bash
HW_HTTP_PORT=18183 HIVEFORGE_PROFILE=swarm-proxmox-mgr1-normal HIVEWATCH_SWARM_NODE=docker-swarm-mgr-1 HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service deploy
```

Remove:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service remove
```

The remove action renders the Compose template, then stops/removes the `hivewatch-poc` Compose project or Swarm stack without deleting Docker volumes.

Purge:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 HIVEFORGE_PURGE_APPROVED=true hiveforge run-action hivewatch main service purge
```

The purge action renders the Compose template and removes the `hivewatch-poc` Compose project or Swarm stack including Docker volumes. Swarm purge waits for stack removal before deleting the stack-scoped PostgreSQL volume.

Update:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 hiveforge run-action hivewatch main service update
```

The update action renders the Compose template, pulls the currently declared images, and reconciles the Compose project or Swarm stack without deleting volumes.

Upgrade:

```bash
HW_HTTP_PORT=18180 HIVEFORGE_PROFILE=normal HIVEWATCH_SERVICE_IMAGE_TAG=0.1.0 HIVEWATCH_DUMMY_IMAGE_TAG=0.1.0 HIVEFORGE_UPGRADE_APPROVED=true hiveforge run-action hivewatch main service upgrade
```

The upgrade action renders the Compose template, requires explicit upgrade approval, pulls the declared images, and reconciles the Compose project or Swarm stack.
