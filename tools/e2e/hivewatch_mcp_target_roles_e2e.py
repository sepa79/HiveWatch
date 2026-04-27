#!/usr/bin/env python3
"""
End-to-end smoke test for HiveWatch target roles through MCP.

Prerequisites:
- Main HiveWatch stack is running at http://localhost:18180.
- Local dev-header auth is enabled for user local-admin.
- Postgres container is named hivewatch-postgres.

The test mutates one environment's target-role config, then restores the exact
original role list in a finally block.
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
ENVIRONMENT_ID = os.environ.get("HIVEWATCH_E2E_ENVIRONMENT_ID", "11111111-1111-1111-1111-111111111111")
SERVER_ID = os.environ.get("HIVEWATCH_E2E_SERVER_ID", "11111111-1111-1111-1111-111111110001")
PROD_MCP = REPO_ROOT / "tools" / "hivewatch-mcp" / "hivewatch_mcp.py"
DEV_MCP = REPO_ROOT / "tools" / "dev-mcp" / "hivewatch_dev_mcp.py"


class E2EFailure(Exception):
    pass


class McpClient:
    def __init__(self, script: Path, env: dict[str, str] | None = None) -> None:
        self.script = script
        self.proc = subprocess.Popen(
            ["python3", str(script)],
            cwd=str(REPO_ROOT),
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            env={**os.environ, **(env or {})},
        )
        self.next_id = 1
        self._request("initialize", {"protocolVersion": "2024-11-05", "capabilities": {}, "clientInfo": {"name": "hivewatch-e2e", "version": "0"}})
        self._notify("notifications/initialized")

    def close(self) -> None:
        if self.proc.stdin and not self.proc.stdin.closed:
            self.proc.stdin.close()
        try:
            self.proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            self.proc.kill()

    def list_tools(self) -> list[dict[str, Any]]:
        result = self._request("tools/list", {})
        return result["tools"]

    def call(self, name: str, arguments: dict[str, Any] | None = None) -> str:
        result = self._request("tools/call", {"name": name, "arguments": arguments or {}})
        if result.get("isError"):
            text = result["content"][0]["text"]
            raise E2EFailure(f"{self.script.name}:{name} returned tool error: {text}")
        return result["content"][0]["text"]

    def _notify(self, method: str) -> None:
        assert self.proc.stdin is not None
        self.proc.stdin.write(json.dumps({"jsonrpc": "2.0", "method": method}) + "\n")
        self.proc.stdin.flush()

    def _request(self, method: str, params: dict[str, Any]) -> dict[str, Any]:
        assert self.proc.stdin is not None
        assert self.proc.stdout is not None
        request_id = self.next_id
        self.next_id += 1
        self.proc.stdin.write(json.dumps({"jsonrpc": "2.0", "id": request_id, "method": method, "params": params}) + "\n")
        self.proc.stdin.flush()
        line = self.proc.stdout.readline()
        if not line:
            stderr = self.proc.stderr.read() if self.proc.stderr else ""
            raise E2EFailure(f"{self.script.name} closed stdout unexpectedly: {stderr}")
        response = json.loads(line)
        if response.get("id") != request_id:
            raise E2EFailure(f"{self.script.name} returned mismatched response id: {response}")
        if "error" in response:
            raise E2EFailure(f"{self.script.name} returned JSON-RPC error: {response['error']}")
        return response["result"]


def main() -> int:
    prod = McpClient(PROD_MCP, {"HIVEWATCH_DEV_USERNAME": "local-admin"})
    dev = McpClient(DEV_MCP)
    original_roles: list[dict[str, Any]] | None = None

    try:
        assert_tool(prod, "hivewatch_list_environment_target_roles")
        assert_tool(dev, "hivewatch_api_put")

        stack = dev.call("hivewatch_stack", {"action": "ps"})
        require("hive-watch-service" in stack and "hivewatch-postgres" in stack, "main stack is not running")

        dummy = dev.call("hivewatch_dummy_stack", {"action": "ps"})
        require("hc-dummy-nft-01-touchpoint-tomcats" in dummy, "dummy stack is not running")

        original_roles = dev_api_body(dev.call("hivewatch_api_get", {"path": f"/api/v1/environments/{ENVIRONMENT_ID}/target-roles"}))
        require(isinstance(original_roles, list) and original_roles, "target roles API returned no roles")

        db_before = dev.call("hivewatch_db_read", {"sql": f"select code, label, active from hw_environment_target_roles where environment_id = '{ENVIRONMENT_ID}' order by sort_order"})
        for role in original_roles:
            require(role["code"] in db_before, f"DB is missing initial role {role['code']}")

        e2e_code = unique_code({role["code"] for role in original_roles})
        mutated = original_roles + [{"code": e2e_code, "label": "E2E MCP role", "sortOrder": 999, "active": True}]
        replaced = dev_api_body(dev.call(
            "hivewatch_api_put",
            {"path": f"/api/v1/admin/environments/{ENVIRONMENT_ID}/target-roles", "body": {"roles": mutated}},
        ))
        require(any(role["code"] == e2e_code for role in replaced), "PUT target roles did not return the E2E role")

        api_after = dev_api_body(dev.call("hivewatch_api_get", {"path": f"/api/v1/environments/{ENVIRONMENT_ID}/target-roles"}))
        require(any(role["code"] == e2e_code for role in api_after), "GET target roles did not include the E2E role")

        prod_after = prod_body(prod.call("hivewatch_list_environment_target_roles", {"environmentId": ENVIRONMENT_ID}))
        require(any(role["code"] == e2e_code for role in prod_after), "production MCP did not expose the E2E role")

        db_after = dev.call("hivewatch_db_read", {"sql": f"select code, label, active from hw_environment_target_roles where environment_id = '{ENVIRONMENT_ID}' order by sort_order"})
        require(e2e_code in db_after, "DB read did not include the E2E role")

        invalid_create = dev.call(
            "hivewatch_api_post",
            {
                "path": f"/api/v1/environments/{ENVIRONMENT_ID}/tomcat-targets",
                "body": {
                    "serverId": SERVER_ID,
                    "role": "NOT_CONFIGURED_E2E_ROLE",
                    "baseUrl": "http://example.invalid",
                    "port": 8080,
                    "username": "e2e",
                    "password": "e2e",
                    "connectTimeoutMs": 1000,
                    "requestTimeoutMs": 1000,
                },
            },
        )
        require(invalid_create.startswith("HTTP 400"), "unconfigured target role was not rejected")
        require("role is not configured for this environment" in invalid_create, "unconfigured role rejection did not explain the role problem")

        print("PASS hivewatch MCP target-roles E2E")
        return 0
    finally:
        try:
            if original_roles is not None:
                dev.call(
                    "hivewatch_api_put",
                    {"path": f"/api/v1/admin/environments/{ENVIRONMENT_ID}/target-roles", "body": {"roles": original_roles}},
                )
        finally:
            prod.close()
            dev.close()


def assert_tool(client: McpClient, expected: str) -> None:
    names = {tool["name"] for tool in client.list_tools()}
    require(expected in names, f"{client.script.name} does not expose {expected}")


def dev_api_body(text: str) -> Any:
    status, body = split_dev_http(text)
    require(200 <= status < 300, f"expected 2xx API response, got HTTP {status}: {body}")
    return json.loads(body) if body.strip() else None


def prod_body(text: str) -> Any:
    payload = json.loads(text)
    require(payload["status"] == 200, f"expected production MCP HTTP 200, got {payload['status']}: {payload.get('body')}")
    return payload["body"]


def split_dev_http(text: str) -> tuple[int, str]:
    first, _, body = text.partition("\n\n")
    require(first.startswith("HTTP "), f"unexpected dev API response: {text}")
    return int(first.removeprefix("HTTP ").strip()), body


def unique_code(existing: set[str]) -> str:
    base = "E2E_MCP_ROLE"
    if base not in existing:
        return base
    index = 2
    while f"{base}_{index}" in existing:
        index += 1
    return f"{base}_{index}"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise E2EFailure(message)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except E2EFailure as exc:
        print(f"FAIL {exc}", file=sys.stderr)
        raise SystemExit(1)
