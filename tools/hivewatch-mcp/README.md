# HiveWatch MCP

API-only MCP convenience server for AI-assisted HiveWatch setup.

HiveWatch owns the provisioning DTOs, validation, apply behavior, audit, revisioning, and domain rules. This MCP server only calls normal HiveWatch APIs.

## Tools

- `hivewatch_list_environments`
- `hivewatch_list_environment_target_roles`
- `hivewatch_list_expected_set_templates`
- `hivewatch_probe_target`
- `hivewatch_validate_environment_plan`
- `hivewatch_apply_environment_plan`

`hivewatch_apply_environment_plan` requires `confirmApply: true` and a non-empty `approval` string. The backend still re-validates the plan and remains the only write authority.

## Auth

Configure one of:

```bash
export HIVEWATCH_API_BASE_URL="https://hivewatch.example.internal"
export HIVEWATCH_BEARER_TOKEN="..."
```

For local development only:

```bash
export HIVEWATCH_API_BASE_URL="http://localhost:18180"
export HIVEWATCH_DEV_USERNAME="local-admin"
```

## Run

From the repository root:

```bash
python3 tools/hivewatch-mcp/hivewatch_mcp.py
```

Example MCP client config shape:

```json
{
  "mcpServers": {
    "hivewatch": {
      "command": "python3",
      "args": ["/home/sepa/HiveWatch/tools/hivewatch-mcp/hivewatch_mcp.py"],
      "env": {
        "HIVEWATCH_API_BASE_URL": "https://hivewatch.example.internal",
        "HIVEWATCH_BEARER_TOKEN": "..."
      }
    }
  }
}
```

## Boundaries

- No DB access.
- No Docker or testbed access.
- No MCP-owned provisioning schema.
- No hidden adapter discovery.
- No write path except the normal provisioning apply API.
- Audit and config revisioning should be implemented inside HiveWatch apply, so MCP benefits without a contract change.
