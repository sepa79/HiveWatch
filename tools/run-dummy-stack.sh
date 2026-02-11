#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${REPO_ROOT}/dummy-stack/docker-compose.dummy-stack.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required." >&2
  exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose V2 is required (docker compose)." >&2
  exit 1
fi

ensure_network() {
  if ! docker network inspect hivewatch-dev >/dev/null 2>&1; then
    docker network create hivewatch-dev >/dev/null
  fi
}

usage() {
  cat <<USAGE
Usage: $(basename "$0") <up|down|restart|ps|logs>

Commands:
  up       Build and start dummy stack in detached mode.
  down     Stop and remove dummy stack.
  restart  Restart dummy stack containers.
  ps       Show dummy stack containers.
  logs     Stream logs for all dummy stack containers.
USAGE
}

if [[ $# -ne 1 ]]; then
  usage >&2
  exit 1
fi

command_name="$1"
case "${command_name}" in
  up)
    ensure_network
    docker compose -f "${COMPOSE_FILE}" up -d --build --remove-orphans
    ;;
  down)
    docker compose -f "${COMPOSE_FILE}" down --remove-orphans
    ;;
  restart)
    docker compose -f "${COMPOSE_FILE}" restart
    ;;
  ps)
    docker compose -f "${COMPOSE_FILE}" ps
    ;;
  logs)
    docker compose -f "${COMPOSE_FILE}" logs -f
    ;;
  *)
    usage >&2
    exit 1
    ;;
esac
