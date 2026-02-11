#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

SKIP_TESTS=false
CLEAN_STACK=false
BUILD_ONLY=false
NO_DOCKER=false
RESTART=false

usage() {
  cat <<USAGE
Usage: $(basename "$0") [options]

Options:
  --quick        Skip tests during Maven build (-DskipTests).
  --clean        Stop and remove compose stack before build.
  --build-only   Build project and image, do not run compose up.
  --no-docker    Run Maven build only (skip Docker build and compose).
  --restart      Restart compose services after successful build.
  --help         Show this help.

Behavior:
  No flags: run Maven tests, package artifact, build image, and start local compose stack.
USAGE
}

require_tools() {
  if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven is required but missing from PATH." >&2
    exit 1
  fi
  if $NO_DOCKER; then
    return
  fi
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is required but missing from PATH." >&2
    exit 1
  fi
  if ! docker compose version >/dev/null 2>&1; then
    echo "Docker Compose V2 is required (docker compose)." >&2
    exit 1
  fi
}

ensure_network() {
  if $NO_DOCKER; then
    return
  fi
  if ! docker network inspect hivewatch-dev >/dev/null 2>&1; then
    docker network create hivewatch-dev >/dev/null
  fi
}

while (($# > 0)); do
  case "$1" in
    --quick)
      SKIP_TESTS=true
      shift
      ;;
    --clean)
      CLEAN_STACK=true
      shift
      ;;
    --build-only)
      BUILD_ONLY=true
      shift
      ;;
    --no-docker)
      NO_DOCKER=true
      shift
      ;;
    --restart)
      RESTART=true
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

require_tools
ensure_network

if $NO_DOCKER && $CLEAN_STACK; then
  echo "--clean cannot be used with --no-docker." >&2
  exit 1
fi

if $NO_DOCKER && $BUILD_ONLY; then
  echo "--build-only cannot be used with --no-docker." >&2
  exit 1
fi

if $CLEAN_STACK; then
  echo "Stopping existing compose stack..."
  docker compose down --remove-orphans || true
fi

MAVEN_ARGS=(clean package)
if $SKIP_TESTS; then
  MAVEN_ARGS+=(-DskipTests)
fi

echo "Running Maven build: mvn ${MAVEN_ARGS[*]}"
mvn "${MAVEN_ARGS[@]}"

if $NO_DOCKER; then
  echo "Build completed (Docker steps skipped)."
  exit 0
fi

echo "Building runtime image: hive-watch-service:latest"
docker build -f hive-watch-service/Dockerfile.runtime -t hive-watch-service:latest .

if $BUILD_ONLY; then
  echo "Build completed (compose up skipped due to --build-only)."
  exit 0
fi

if $RESTART; then
  echo "Recreating compose services..."
  docker compose up -d --force-recreate
else
  echo "Starting compose stack..."
  docker compose up -d
fi

HOST_PORT="${HW_HTTP_PORT:-18180}"
echo "UI: http://localhost:${HOST_PORT}/"
echo "Health: http://localhost:${HOST_PORT}/actuator/health"
