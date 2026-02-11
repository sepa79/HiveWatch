import json
import os
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer


DEFAULT_PROFILE_DATA = {
    "notifications": {
        "app_name": "notifications-actuator",
        "health_status": "UP",
        "cpu_usage": 0.21,
        "memory_used_bytes": 536870912,
    },
    "banking": {
        "app_name": "banking-actuator",
        "health_status": "DOWN",
        "cpu_usage": 0.93,
        "memory_used_bytes": 1400897536,
    },
    "accounts": {
        "app_name": "accounts-actuator",
        "health_status": "UP",
        "cpu_usage": 0.67,
        "memory_used_bytes": 933232640,
    },
}


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def load_profile_data() -> dict:
    raw = os.getenv("ACTUATOR_PROFILES_JSON", "")
    if raw.strip() == "":
        return DEFAULT_PROFILE_DATA

    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError as ex:
        raise RuntimeError("ACTUATOR_PROFILES_JSON must be valid JSON.") from ex

    if not isinstance(parsed, dict) or len(parsed) == 0:
        raise RuntimeError("ACTUATOR_PROFILES_JSON must be a non-empty object.")

    normalized = {}
    for profile_name, payload in parsed.items():
        if not isinstance(profile_name, str) or profile_name.strip() == "":
            raise RuntimeError("Profile name must be a non-empty string.")
        if not isinstance(payload, dict):
            raise RuntimeError(f"Profile '{profile_name}' payload must be an object.")

        app_name = str(payload.get("app_name", "")).strip()
        health_status = str(payload.get("health_status", "")).strip().upper()
        cpu_usage = payload.get("cpu_usage")
        memory_used = payload.get("memory_used_bytes")

        if app_name == "":
            raise RuntimeError(f"Profile '{profile_name}' requires app_name.")
        if health_status not in {"UP", "DOWN", "UNKNOWN"}:
            raise RuntimeError(f"Profile '{profile_name}' has invalid health_status: {health_status}")
        if not isinstance(cpu_usage, (int, float)):
            raise RuntimeError(f"Profile '{profile_name}' cpu_usage must be numeric.")
        if not isinstance(memory_used, int):
            raise RuntimeError(f"Profile '{profile_name}' memory_used_bytes must be integer.")

        normalized[profile_name] = {
            "app_name": app_name,
            "health_status": health_status,
            "cpu_usage": float(cpu_usage),
            "memory_used_bytes": memory_used,
        }

    return normalized


PROFILE_DATA = load_profile_data()
ENVIRONMENT_NAME = os.getenv("ACTUATOR_ENVIRONMENT_NAME", "dummy")


class ActuatorHandler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        path_parts = [part for part in self.path.split("?")[0].split("/") if part]
        if len(path_parts) < 3:
            self.write_not_found()
            return

        profile_name = path_parts[0]
        resource_path = "/" + "/".join(path_parts[1:])

        profile = PROFILE_DATA.get(profile_name)
        if profile is None:
            self.write_not_found()
            return

        routes = {
            "/actuator/health": {
                "status": profile["health_status"],
                "components": {
                    "diskSpace": {"status": "UP"},
                    "ping": {"status": "UP"},
                },
                "timestamp": now_iso(),
                "environment": ENVIRONMENT_NAME,
                "profile": profile_name,
            },
            "/actuator/info": {
                "app": {
                    "name": profile["app_name"],
                    "profile": profile_name,
                    "environment": ENVIRONMENT_NAME,
                    "build": {"version": "dummy-stack-2.0.0"},
                },
                "timestamp": now_iso(),
            },
            "/actuator/metrics/system.cpu.usage": {
                "name": "system.cpu.usage",
                "baseUnit": "ratio",
                "measurements": [{"statistic": "VALUE", "value": profile["cpu_usage"]}],
                "availableTags": [],
                "timestamp": now_iso(),
                "profile": profile_name,
            },
            "/actuator/metrics/jvm.memory.used": {
                "name": "jvm.memory.used",
                "baseUnit": "bytes",
                "measurements": [{"statistic": "VALUE", "value": profile["memory_used_bytes"]}],
                "availableTags": [{"tag": "area", "values": ["heap"]}],
                "timestamp": now_iso(),
                "profile": profile_name,
            },
        }

        payload = routes.get(resource_path)
        if payload is None:
            self.write_not_found()
            return

        body = json.dumps(payload).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def write_not_found(self) -> None:
        body = json.dumps({"error": "NOT_FOUND", "path": self.path, "timestamp": now_iso()}).encode("utf-8")
        self.send_response(404)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format: str, *args) -> None:
        return


if __name__ == "__main__":
    server = HTTPServer(("0.0.0.0", 8080), ActuatorHandler)
    server.serve_forever()
