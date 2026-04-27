#!/usr/bin/env python3
"""
API-only MCP convenience server for HiveWatch.

This server intentionally uses public HiveWatch APIs only. It does not read the
database, does not inspect local containers, and does not own provisioning
contracts. HiveWatch remains the source of truth for validation, apply,
audit, revisioning, and domain rules.
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any


DEFAULT_API_BASE_URL = "http://localhost:18180"


TOOLS: list[dict[str, Any]] = [
    {
        "name": "hivewatch_list_environments",
        "description": "List HiveWatch environments through the normal API.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
            },
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_list_expected_set_templates",
        "description": "List expected-set templates for one explicit kind.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "kind": {"type": "string", "enum": ["TOMCAT_WEBAPP_PATH", "DOCKER_SERVICE_PROFILE"]},
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
            },
            "required": ["kind"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_list_environment_target_roles",
        "description": "List configured target roles for one HiveWatch environment through the normal API.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "environmentId": {"type": "string", "description": "HiveWatch environment UUID."},
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
            },
            "required": ["environmentId"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_probe_target",
        "description": "Run one explicit HiveWatch target probe candidate.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "probe": {
                    "type": "object",
                    "description": "TargetProbeRequestDto.",
                    "additionalProperties": True,
                },
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 60, "default": 20},
            },
            "required": ["probe"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_validate_environment_plan",
        "description": "Validate an EnvironmentProvisioningPlanDto through HiveWatch.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "plan": {
                    "type": "object",
                    "description": "EnvironmentProvisioningPlanDto owned by HiveWatch.",
                    "additionalProperties": True,
                },
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 60, "default": 20},
            },
            "required": ["plan"],
            "additionalProperties": False,
        },
    },
    {
        "name": "hivewatch_apply_environment_plan",
        "description": "Apply an EnvironmentProvisioningPlanDto through HiveWatch after explicit approval.",
        "inputSchema": {
            "type": "object",
            "properties": {
                "plan": {
                    "type": "object",
                    "description": "EnvironmentProvisioningPlanDto owned by HiveWatch.",
                    "additionalProperties": True,
                },
                "scanAfterApply": {"type": "boolean", "default": False},
                "confirmApply": {"type": "boolean", "description": "Must be true after explicit human approval."},
                "approval": {"type": "string", "description": "Short approval note or ticket/reference."},
                "baseUrl": {"type": "string", "default": DEFAULT_API_BASE_URL},
                "timeoutSeconds": {"type": "integer", "minimum": 1, "maximum": 60, "default": 20},
            },
            "required": ["plan", "confirmApply", "approval"],
            "additionalProperties": False,
        },
    },
]


class ToolError(Exception):
    pass


def main() -> None:
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            message = json.loads(line)
            response = handle_message(message)
        except Exception as exc:
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
                "serverInfo": {"name": "hivewatch-mcp", "version": "0.1.0"},
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
            return tool_response(request_id, call_tool(tool_name, arguments))
        except ToolError as exc:
            return tool_response(request_id, str(exc), is_error=True)

    if request_id is None:
        return None
    return error_response(request_id, -32601, f"Unsupported method: {method}")


def call_tool(tool_name: str, args: dict[str, Any]) -> str:
    if tool_name == "hivewatch_list_environments":
        return api_request("GET", "/api/v1/environments", None, args)
    if tool_name == "hivewatch_list_expected_set_templates":
        kind = require_string(args, "kind")
        path = "/api/v1/expected-set-templates?kind=" + urllib.parse.quote(kind)
        return api_request("GET", path, None, args)
    if tool_name == "hivewatch_list_environment_target_roles":
        environment_id = require_string(args, "environmentId")
        path = "/api/v1/environments/" + urllib.parse.quote(environment_id) + "/target-roles"
        return api_request("GET", path, None, args)
    if tool_name == "hivewatch_probe_target":
        return api_request("POST", "/api/v1/admin/target-probes", require_object(args, "probe"), args)
    if tool_name == "hivewatch_validate_environment_plan":
        return api_request("POST", "/api/v1/admin/environment-provisioning/plans/validate", require_object(args, "plan"), args)
    if tool_name == "hivewatch_apply_environment_plan":
        if args.get("confirmApply") is not True:
            raise ToolError("confirmApply must be true after explicit human approval")
        approval = require_string(args, "approval").strip()
        if not approval:
            raise ToolError("approval is required")
        body = {
            "plan": require_object(args, "plan"),
            "scanAfterApply": bool(args.get("scanAfterApply", False)),
        }
        return api_request("POST", "/api/v1/admin/environment-provisioning/plans/apply", body, args)
    raise ToolError(f"Unknown tool: {tool_name}")


def api_request(method: str, path: str, body: dict[str, Any] | None, args: dict[str, Any]) -> str:
    base_url = str(args.get("baseUrl") or os.environ.get("HIVEWATCH_API_BASE_URL") or DEFAULT_API_BASE_URL).rstrip("/")
    timeout = bounded_int(args.get("timeoutSeconds", 10), "timeoutSeconds", 1, 60)
    url = base_url + path

    data: bytes | None = None
    headers = auth_headers()
    headers["Accept"] = "application/json"
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body_text = response.read().decode("utf-8", errors="replace")
            return format_http_response(response.status, body_text)
    except urllib.error.HTTPError as exc:
        body_text = exc.read().decode("utf-8", errors="replace")
        return format_http_response(exc.code, body_text)
    except urllib.error.URLError as exc:
        raise ToolError(f"HTTP request failed: {exc.reason}")


def auth_headers() -> dict[str, str]:
    token = os.environ.get("HIVEWATCH_BEARER_TOKEN")
    if token:
        return {"Authorization": f"Bearer {token}"}

    dev_username = os.environ.get("HIVEWATCH_DEV_USERNAME")
    if dev_username:
        return {"X-HW-Username": dev_username}

    return {}


def format_http_response(status: int, body_text: str) -> str:
    parsed: Any
    try:
        parsed = json.loads(body_text) if body_text else None
    except json.JSONDecodeError:
        parsed = body_text
    return json.dumps({"status": status, "body": parsed}, indent=2, sort_keys=True)


def require_object(args: dict[str, Any], key: str) -> dict[str, Any]:
    value = args.get(key)
    if not isinstance(value, dict):
        raise ToolError(f"{key} must be an object")
    return value


def require_string(args: dict[str, Any], key: str) -> str:
    value = args.get(key)
    if not isinstance(value, str):
        raise ToolError(f"{key} must be a string")
    return value


def bounded_int(value: Any, name: str, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError) as exc:
        raise ToolError(f"{name} must be an integer") from exc
    if parsed < minimum or parsed > maximum:
        raise ToolError(f"{name} must be between {minimum} and {maximum}")
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


if __name__ == "__main__":
    main()
