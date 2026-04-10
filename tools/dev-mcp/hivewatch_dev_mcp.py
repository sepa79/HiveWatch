#!/usr/bin/env python3
"""
Development-only MCP server for HiveWatch.

This server is intentionally local/dev tooling. It exposes read-only DB inspection,
local HiveWatch API calls, Docker/testbed status, and bounded log reads so coding
agents can inspect implementation progress without inventing production shortcuts.
"""

from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_API_BASE_URL = "http://localhost:18180"
DEFAULT_DEV_USERNAME = "local-admin"
POSTGRES_CONTAINER = "hivewatch-postgres"
DUMMY_COMPOSE_FILE = REPO_ROOT / "dummy-stack" / "docker-compose.dummy-stack.yml"


FORBIDDEN_SQL = re.compile(
    r"\b(insert|update|delete|merge|alter|drop|create|truncate|grant|revoke|copy|call|do|vacuum|analyze|reindex|cluster|refresh)\b",
    re.IGNORECASE,
)


TOOLS: list[dict[str, Any]] = [
    {
        "name": "hivewatch_api_get",
        "description": "Call a local HiveWatch GET API endpoint using dev-header auth.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "HTTP path, for example /api/v1/admin/environments."},
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "username": {"type": "string", "default": DEFAULT_DEV_USERNAME},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
            },
            "required": ["path"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_api_post",
        "description": "Call a local HiveWatch POST API endpoint using dev-header auth.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "path": {"type": "string", "description": "HTTP path, for example /api/v1/admin/target-probes."},
                "body": {"type": "object", "default": {}},
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "username": {"type": "string", "default": DEFAULT_DEV_USERNAME},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
            },
            "required": ["path"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_db_read",
        "description": "Run a read-only SELECT/WITH query against the local HiveWatch Postgres container and return CSV.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "sql": {"type": "string", "description": "Single SELECT or WITH query. Semicolons and write statements are rejected."},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
            },
            "required": ["sql"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_docker_ps",
        "description": "List local Docker containers relevant to HiveWatch development.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "all": {"type": "boolean", "default": False},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_container_logs",
        "description": "Read recent logs from a local development container.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "container": {"type": "string", "description": "Container name."},
                "tail": {"type": "integer", "minimum": 1, "maximum": 500, "default": 100},
            },
            "required": ["container"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_dummy_stack",
        "description": "Run a bounded dummy-stack action through docker compose.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "action": {"type": "string", "enum": ["ps", "up", "down", "restart", "logs"]},
                "tail": {"type": "integer", "minimum": 1, "maximum": 500, "default": 100},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 120, "default": 30},
            },
            "required": ["action"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_stack",
        "description": "Run a bounded HiveWatch app-stack action through docker compose.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "action": {"type": "string", "enum": ["ps", "up", "down", "restart", "logs"]},
                "tail": {"type": "integer", "minimum": 1, "maximum": 500, "default": 100},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 120, "default": 30},
            },
            "required": ["action"],
            "additionalProperties": False,
        },
    },
]


def main() -> None:
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            message = json.loads(line)
            response = handle_message(message)
        except Exception as exc:  # Keep the stdio transport alive for bad calls.
            response = error_response(None, -32603, f"Internal error: {exc}")

        if response is not None:
            sys.stdout.write(json.dumps(response, separators=(",", ":")) + "\n")
            sys.stdout.flush()


def handle_message(message: dict[str, Any]) -> dict[str, Any] | None:
    method = message.get("method")
    request_id = message.get("id")

    if method == "initialize":
        return {
            "jsonrpc": "2.0",
            "id": request_id,
            "result": {
                "protocolVersion": "2024-11-05",
                "capabilities": {"tools": {}},
                "serverInfo": {"name": "hivewatch-dev-mcp", "version": "0.1.0"},
            },
        }

    if method == "notifications/initialized":
        return None

    if method == "tools/list":
        return {"jsonrpc": "2.0", "id": request_id, "result": {"tools": TOOLS}}

    if method == "tools/call":
        params = message.get("params") or {}
        tool_name = params.get("name")
        arguments = params.get("arguments") or {}
        try:
            result_text = call_tool(tool_name, arguments)
            return tool_response(request_id, result_text)
        except ToolError as exc:
            return tool_response(request_id, str(exc), is_error=True)

    if request_id is None:
        return None
    return error_response(request_id, -32601, f"Unsupported method: {method}")


def call_tool(tool_name: str, args: dict[str, Any]) -> str:
    if tool_name == "hivewatch_api_get":
        return api_request("GET", args)
    if tool_name == "hivewatch_api_post":
        return api_request("POST", args)
    if tool_name == "hivewatch_db_read":
        return db_read(args)
    if tool_name == "hivewatch_docker_ps":
        return docker_ps(args)
    if tool_name == "hivewatch_container_logs":
        return container_logs(args)
    if tool_name == "hivewatch_dummy_stack":
        return compose_action(DUMMY_COMPOSE_FILE, args)
    if tool_name == "hivewatch_stack":
        return compose_action(REPO_ROOT / "docker-compose.yml", args)
    raise ToolError(f"Unknown tool: {tool_name}")


def api_request(method: str, args: dict[str, Any]) -> str:
    path = require_string(args, "path")
    if not path.startswith("/"):
        raise ToolError("path must start with /")

    base_url = str(args.get("baseUrl") or DEFAULT_API_BASE_URL).rstrip("/")
    username = str(args.get("username") or DEFAULT_DEV_USERNAME)
    timeout = bounded_int(args.get("timeoutSeconds", 10), "timeoutSeconds", 1, 30)
    url = base_url + path

    data: bytes | None = None
    headers = {"X-HW-Username": username, "Accept": "application/json"}
    if method == "POST":
        body = args.get("body") or {}
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body_text = response.read().decode("utf-8", errors="replace")
            return format_http_response(response.status, dict(response.headers), body_text)
    except urllib.error.HTTPError as exc:
        body_text = exc.read().decode("utf-8", errors="replace")
        return format_http_response(exc.code, dict(exc.headers), body_text)
    except urllib.error.URLError as exc:
        raise ToolError(f"HTTP request failed: {exc.reason}")


def db_read(args: dict[str, Any]) -> str:
    sql = require_string(args, "sql").strip()
    timeout = bounded_int(args.get("timeoutSeconds", 10), "timeoutSeconds", 1, 30)
    query = sanitize_read_query(sql)

    copy_sql = f"COPY ({query}) TO STDOUT WITH CSV HEADER"
    env = os.environ.copy()
    env["PGOPTIONS"] = "-c statement_timeout=5000 -c default_transaction_read_only=on"
    command = [
        "docker",
        "exec",
        "-e",
        f"PGOPTIONS={env['PGOPTIONS']}",
        POSTGRES_CONTAINER,
        "psql",
        "-U",
        "hive_watch",
        "-d",
        "hive_watch",
        "-X",
        "--set",
        "ON_ERROR_STOP=1",
        "-c",
        copy_sql,
    ]
    return run_command(command, timeout=timeout, cwd=REPO_ROOT)


def docker_ps(args: dict[str, Any]) -> str:
    include_all = bool(args.get("all", False))
    command = ["docker", "ps"]
    if include_all:
        command.append("--all")
    command.extend(["--format", "table {{.Names}}\t{{.Status}}\t{{.Ports}}"])
    return run_command(command, timeout=10, cwd=REPO_ROOT)


def container_logs(args: dict[str, Any]) -> str:
    container = require_container_name(args, "container")
    tail = bounded_int(args.get("tail", 100), "tail", 1, 500)
    command = ["docker", "logs", "--tail", str(tail), container]
    return run_command(command, timeout=15, cwd=REPO_ROOT)


def compose_action(compose_file: Path, args: dict[str, Any]) -> str:
    action = require_string(args, "action")
    if action not in {"ps", "up", "down", "restart", "logs"}:
        raise ToolError("action must be one of ps, up, down, restart, logs")
    if not compose_file.exists():
        raise ToolError(f"compose file not found: {compose_file}")

    tail = bounded_int(args.get("tail", 100), "tail", 1, 500)
    timeout = bounded_int(args.get("timeoutSeconds", 30), "timeoutSeconds", 1, 120)
    command = ["docker", "compose", "-f", str(compose_file)]
    if action == "ps":
        command.append("ps")
    elif action == "up":
        command.extend(["up", "-d", "--build", "--remove-orphans"])
    elif action == "down":
        command.extend(["down", "--remove-orphans"])
    elif action == "restart":
        command.append("restart")
    elif action == "logs":
        command.extend(["logs", "--tail", str(tail)])
    return run_command(command, timeout=timeout, cwd=REPO_ROOT)


def sanitize_read_query(sql: str) -> str:
    if not sql:
        raise ToolError("sql is required")
    query = sql.strip()
    if query.endswith(";"):
        query = query[:-1].strip()
    if ";" in query:
        raise ToolError("sql must contain a single statement")
    if not re.match(r"^(select|with)\b", query, re.IGNORECASE):
        raise ToolError("sql must start with SELECT or WITH")
    if FORBIDDEN_SQL.search(query):
        raise ToolError("sql contains a forbidden non-read-only keyword")
    return query


def format_http_response(status: int, headers: dict[str, Any], body: str) -> str:
    content_type = str(headers.get("Content-Type") or headers.get("content-type") or "")
    if "application/json" in content_type:
        try:
            parsed = json.loads(body) if body else None
            body = json.dumps(parsed, indent=2, sort_keys=True)
        except json.JSONDecodeError:
            pass
    return f"HTTP {status}\n\n{body}"


def run_command(command: list[str], timeout: int, cwd: Path) -> str:
    try:
        completed = subprocess.run(
            command,
            cwd=str(cwd),
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            check=False,
        )
    except FileNotFoundError as exc:
        raise ToolError(f"Command not found: {exc.filename}")
    except subprocess.TimeoutExpired:
        raise ToolError(f"Command timed out after {timeout}s")

    output = completed.stdout
    if completed.stderr:
        output = output + ("\n" if output else "") + completed.stderr
    if completed.returncode != 0:
        raise ToolError(f"Command exited {completed.returncode}\n\n{output.strip()}")
    return output.strip() or "(no output)"


def require_string(args: dict[str, Any], name: str) -> str:
    value = args.get(name)
    if not isinstance(value, str) or not value.strip():
        raise ToolError(f"{name} is required")
    return value.strip()


def require_container_name(args: dict[str, Any], name: str) -> str:
    value = require_string(args, name)
    if not re.match(r"^[a-zA-Z0-9_.-]+$", value):
        raise ToolError(f"{name} contains invalid characters")
    return value


def bounded_int(value: Any, name: str, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        raise ToolError(f"{name} must be an integer")
    if parsed < minimum or parsed > maximum:
        raise ToolError(f"{name} must be {minimum}..{maximum}")
    return parsed


def tool_response(request_id: Any, text: str, is_error: bool = False) -> dict[str, Any]:
    return {
        "jsonrpc": "2.0",
        "id": request_id,
        "result": {
            "content": [{"type": "text", "text": text}],
            "isError": is_error,
        },
    }


def error_response(request_id: Any, code: int, message: str) -> dict[str, Any]:
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


class ToolError(Exception):
    pass


if __name__ == "__main__":
    main()
