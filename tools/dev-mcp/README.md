# HiveWatch Dev MCP

Development-only MCP server for local HiveWatch work.

This is not the production MCP convenience layer described in `docs/PROVISIONING_PLAN_AND_PROBES.md`.
It exists so developers and coding agents can inspect the local app, DB, dummy stack, and logs while implementing features.

## Scope

Available tools:

- `hivewatch_api_get`
  - Calls a local HiveWatch GET endpoint with dev-header auth.
- `hivewatch_api_post`
  - Calls a local HiveWatch POST endpoint with dev-header auth.
- `hivewatch_db_read`
  - Runs a read-only `SELECT` or `WITH` query against the local `hivewatch-postgres` container.
  - Returns CSV.
- `hivewatch_docker_ps`
  - Lists local containers.
- `hivewatch_container_logs`
  - Reads recent logs from a named container.
- `hivewatch_dummy_stack`
  - Runs bounded `docker compose` actions for `dummy-stack/docker-compose.dummy-stack.yml`.
- `hivewatch_stack`
  - Runs bounded `docker compose` actions for the main app stack.

## Boundaries

- Dev-only.
- Local-only by convention.
- No production credentials.
- No production DB access.
- DB tool rejects non-read-only SQL and uses Postgres read-only settings.
- Production MCP must use HiveWatch APIs only.
- Any useful behavior from this dev server must graduate into a normal HiveWatch API before production MCP depends on it.

## Run

From the repository root:

```bash
python3 tools/dev-mcp/hivewatch_dev_mcp.py
```

The server speaks MCP over stdio. Configure your MCP client to launch the script.

Example MCP client config shape:

```json
{
  "mcpServers": {
    "hivewatch-dev": {
      "command": "python3",
      "args": ["/home/sepa/HiveWatch/tools/dev-mcp/hivewatch_dev_mcp.py"]
    }
  }
}
```

## Expected Local Services

Main app stack:

```bash
./build-hive-watch.sh --restart
```

Dummy stack:

```bash
./tools/run-dummy-stack.sh up
```

Default API base URL:

```text
http://localhost:18180
```

Default dev user header:

```text
X-HW-Username: local-admin
```

## Example Tool Calls

List admin environments through the API:

```json
{
  "path": "/api/v1/admin/environments"
}
```

Inspect environments through DB:

```json
{
  "sql": "select id, name from hw_environments order by name"
}
```

Read service logs:

```json
{
  "container": "hive-watch-service",
  "tail": 120
}
```

Show dummy stack status:

```json
{
  "action": "ps"
}
```
