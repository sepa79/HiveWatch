# HiveWatch E2E

Local end-to-end checks for development.

## MCP Target Roles

```bash
tools/e2e/hivewatch_mcp_target_roles_e2e.py
```

Prerequisites:

- HiveWatch app stack is running at `http://localhost:18180`.
- Dummy stack is running.
- Dev-header auth accepts `local-admin`.
- Postgres container is named `hivewatch-postgres`.

The test talks to both MCP servers over stdio, mutates one environment's target-role config through dev-MCP, verifies the result through API, DB, and production MCP, checks that unconfigured roles are rejected for target creation, and restores the original role list.
