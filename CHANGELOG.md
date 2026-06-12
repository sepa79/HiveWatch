# Changelog

## 0.1.5 - 2026-06-12

- Updated HiveForge deploy, update, and upgrade playbooks for the 0.5.2 action
  root contract: project Ansible writes rendered Compose to
  `/hf/stacks/compose.yml` instead of reading `HIVEFORGE_RENDERED_COMPOSE_FILE`.
- Documented HiveForge-owned remove and purge execution for the current 0.5
  contract.

## 0.1.4 - 2026-06-10

- Updated the HiveForge deployment contract to `0.5`.
- Changed HiveForge deploy, update, and upgrade playbooks to render only the
  Compose file declared by `HIVEFORGE_RENDERED_COMPOSE_FILE`; HiveForge now owns
  Docker execution.
- Changed HiveForge remove and purge playbooks to fail explicitly until
  HiveForge provides owner-side Docker remove and purge operations.

## 0.1.3 - 2026-06-05

- Changed the default published HiveWatch HTTP port from `18180` to `4020`
  across local Compose, HiveForge defaults, README guidance, and MCP tooling.

## 0.1.2 - 2026-05-26

- Added Docker default-network aliases for HiveWatch dummy services so seeded
  test targets resolve without requiring a project-owned external network.

## 0.1.1 - 2026-05-26

- Replaced environment-specific HiveForge Swarm profiles with portable
  `docker-single`, `docker-single-test`, and `docker-swarm` profiles.
- Removed Swarm node pinning from the HiveForge POC deployment template so the
  Swarm profile can run on a user-provided Swarm instead of named smoke nodes.
- Updated HiveForge README guidance to link to HiveForge as the source of truth
  for MCP/REST operator flow instead of duplicating old CLI examples.
- Moved HiveWatch example port and image tags into deployment YAML defaults so
  the example no longer requires operator-provided `HW_HTTP_PORT`,
  `HIVEWATCH_SERVICE_IMAGE_TAG`, or `HIVEWATCH_DUMMY_IMAGE_TAG`.
- Removed `HIVEFORGE_PROFILE` from component environment requirements because
  the profile is a HiveForge action parameter, not operator-provided project
  environment.
- Removed the external `hivewatch-dev` network and project-owned network
  creation; Docker Compose and Docker Swarm now create the default deployment
  network.

## 0.1.0 - 2026-05-19

- Added HiveForge POC deployment manifests for the HiveWatch service component.
- Added explicit `normal` and `test` HiveForge profiles for local Docker deployment.
- Added explicit Swarm smoke profiles for the WSL and Proxmox Swarm environments.
- Added explicit HiveForge lifecycle playbooks for deploy, remove, purge, update, and upgrade.
- Added project-owned external network validation/creation for the rendered Compose file.
- Added Swarm stack deployment, removal, purge, update, and upgrade paths.
- Added explicit Swarm node validation, placement constraints, host-mode HTTP publishing, and `tasks.postgres` database routing for Swarm profiles.
- Added versioned image tag inputs for HiveWatch and dummy-stack images; playbooks reject missing tags.
- Documented the HiveForge POC workflow in the README.
