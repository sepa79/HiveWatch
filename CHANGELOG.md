# Changelog

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
