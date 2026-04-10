# Provisioning Plans, Probes, and MCP

## 1. Goal

Add an explicit provisioning workflow for creating and extending HiveWatch environments.

The workflow must support:
- UI-driven setup and endpoint testing.
- CLI or script-driven setup.
- MCP-assisted setup for AI agents.
- Development-time MCP tools for fast inspection of DB state, testbed state, and implementation progress.

HiveWatch remains the owner of contracts, validation, persistence, audit, versioning, and domain rules. MCP is a convenience layer only.

## 2. Principles

- NFF applies to provisioning.
  - No hidden adapter fallback.
  - No implicit protocol switching.
  - No automatic target creation from probe results.
  - Every persisted target must declare explicit adapter and explicit settings.
- Probe is not discovery.
  - A probe checks one explicit adapter candidate.
  - A client or MCP agent may run many probes, but that orchestration is outside the backend contract.
- Provisioning plan DTOs belong to HiveWatch.
  - UI, MCP, and future CLI flows must use the same canonical DTOs.
  - Do not create MCP-only config contracts.
- Apply is the only batch provisioning write path.
  - Audit and config revisioning must live behind the apply API.
  - Once audit/versioning exists, MCP should benefit automatically by continuing to call the same apply API.
- Actor identity comes from authentication.
  - MCP may pass source metadata and correlation IDs.
  - MCP must not be allowed to invent the audit actor as request text.

## 3. Target Shape

Current hierarchy remains:

- `Environment`
- `Server`
- typed targets:
  - `TOMCAT_MANAGER_HTML`
  - `ACTUATOR_HTTP`
- expected state/policy specs
- observations and decisions from normal scans

Provisioning does not introduce a parallel domain model. It introduces a plan format for creating or changing existing domain objects through one controlled service boundary.

## 4. API Surface

### 4.1 Target Probe API

Stateless endpoint checks for a concrete adapter candidate.

Draft endpoint:

```http
POST /api/v1/admin/target-probes
```

Draft request:

```json
{
  "adapterType": "TOMCAT_MANAGER_HTML",
  "baseUrl": "http://nft03-tomcats.internal",
  "port": 8081,
  "username": "hc-manager",
  "password": "secret",
  "connectTimeoutMs": 1500,
  "requestTimeoutMs": 5000
}
```

For actuator:

```json
{
  "adapterType": "ACTUATOR_HTTP",
  "baseUrl": "http://nft03-services.internal",
  "port": 8080,
  "profile": "payments",
  "connectTimeoutMs": 1500,
  "requestTimeoutMs": 5000
}
```

Draft response:

```json
{
  "adapterType": "TOMCAT_MANAGER_HTML",
  "outcomeKind": "SUCCESS",
  "errorKind": null,
  "errorMessage": null,
  "observed": {
    "tomcatVersion": "10.1.x",
    "javaVersion": "21",
    "os": "Linux",
    "webapps": [
      {
        "path": "/PaymentApp1",
        "name": "PaymentApp1",
        "version": "2.0.0"
      }
    ]
  },
  "candidate": {
    "baseUrl": "http://nft03-tomcats.internal",
    "port": 8081
  }
}
```

Rules:
- Probe never writes config.
- Probe reuses the same adapter client/parser/normalization logic as normal scans where practical.
- Probe returns the same error classification vocabulary as scans.
- Probe validates the same URL rules as persisted targets.
- Probe must not try alternate adapter types, alternate protocols, alternate paths, or alternate ports unless the caller sends separate explicit requests.
- Credentials and secrets must not be logged.

### 4.2 Provisioning Plan Validation API

Validates a canonical plan before write.

Draft endpoint:

```http
POST /api/v1/admin/environment-provisioning/plans/validate
```

Responsibilities:
- Check required fields.
- Check environment name conflicts.
- Check server name conflicts.
- Check duplicate `server + role` target conflicts.
- Check adapter-specific field validity.
- Check timeout and port ranges.
- Check expected-set consistency.
- Return a diff-style summary of objects that would be created or changed.
- Return blocking errors separately from warnings.

Validation must not write config.

### 4.3 Provisioning Plan Apply API

Applies a validated plan transactionally.

Draft endpoint:

```http
POST /api/v1/admin/environment-provisioning/plans/apply
```

Responsibilities:
- Re-run validation server-side.
- Persist all accepted changes in one transaction where possible.
- Create audit data.
- Create or update config revisions when revisioning exists.
- Return stable IDs for created objects.
- Return an apply summary.
- Optionally trigger scans only when an explicit request flag asks for it.

Apply must not trust a previous validation response. It must validate the submitted plan again.

## 5. Canonical DTO Draft

Initial DTO names:

- `TargetAdapterTypeDto`
  - `TOMCAT_MANAGER_HTML`
  - `ACTUATOR_HTTP`
- `TargetProbeRequestDto`
- `TargetProbeResultDto`
- `EnvironmentProvisioningPlanDto`
- `ProvisioningEnvironmentDto`
- `ProvisioningServerDto`
- `ProvisioningTomcatTargetDto`
- `ProvisioningActuatorTargetDto`
- `ProvisioningExpectedSetChangeModeDto`
- `ProvisioningTomcatExpectedWebappsDto`
- `ProvisioningTomcatExpectedWebappsSpecDto`
- `ProvisioningDockerExpectedServicesDto`
- `ProvisioningDockerExpectedServicesSpecDto`
- `ProvisioningPlanValidationResultDto`
- `ProvisioningPlanIssueDto`
- `ProvisioningPlanDiffDto`
- `ProvisioningPlanApplyRequestDto`
- `ProvisioningPlanApplyResultDto`
- `ProvisioningApplySummaryDto`

Plan shape, simplified:

```json
{
  "source": "UI",
  "correlationId": "optional-client-generated-id",
  "reason": "Add NFT-03 initial monitoring",
  "environment": {
    "mode": "CREATE",
    "environmentId": null,
    "name": "NFT-03"
  },
  "servers": [
    {
      "clientRef": "touchpoint",
      "mode": "CREATE",
      "serverId": null,
      "name": "Touchpoint",
      "tomcatTargets": [
        {
          "role": "PAYMENTS",
          "adapterType": "TOMCAT_MANAGER_HTML",
          "baseUrl": "http://nft03-tomcats.internal",
          "port": 8081,
          "username": "hc-manager",
          "password": "secret",
          "connectTimeoutMs": 1500,
          "requestTimeoutMs": 5000
        }
      ],
      "actuatorTargets": [
        {
          "role": "PAYMENTS",
          "adapterType": "ACTUATOR_HTTP",
          "baseUrl": "http://nft03-services.internal",
          "port": 8080,
          "profile": "payments",
          "connectTimeoutMs": 1500,
          "requestTimeoutMs": 5000
        }
      ],
      "tomcatExpectedWebapps": {
        "changeMode": "REPLACE",
        "specs": [
          {
            "role": "PAYMENTS",
            "mode": "EXPLICIT",
            "templateId": null,
            "items": ["/payments", "/payments-admin"]
          }
        ]
      },
      "dockerExpectedServices": {
        "changeMode": "NO_CHANGE",
        "specs": []
      }
    }
  ]
}
```

Notes:
- `clientRef` exists only to correlate request items with validation/apply results.
- Persisted IDs remain generated by HiveWatch.
- `mode` must be explicit.
- Expected-set blocks are required on each server.
- Expected-set `changeMode` must be explicit:
  - `NO_CHANGE` means apply does not modify that expected-set area and `specs` must be empty.
  - `REPLACE` means apply replaces the server-scoped expected-set area with exactly the provided `specs`; an empty list explicitly clears it.
- Expected-set spec `mode` must be `EXPLICIT` or `TEMPLATE`; `UNCONFIGURED` is represented by `REPLACE` with an empty `specs` list, not by a spec row.
- Tomcat expected webapp specs are scoped by `role` and the role must match a configured Tomcat target on the server after apply.
- Docker expected service specs are server-scoped and may contain at most one spec per server.
- Future secret handling should replace direct secret values with secret references where possible.

## 6. UI Flows

### 6.1 Test Endpoint Button

The target form can call `POST /api/v1/admin/target-probes`.

Expected behavior:
- Show success/failure and observed metadata.
- Do not save config.
- Do not mutate form fields silently from probe output.

### 6.2 Environment Provisioning Wizard

Potential flow:
- User enters environment/server/target fields.
- User can test each target via probe.
- UI builds `EnvironmentProvisioningPlanDto`.
- UI calls validate.
- UI shows errors, warnings, and diff.
- User applies.
- Backend applies plan and records audit/versioning data.

## 7. MCP Flows

### 7.1 Production MCP Convenience Layer

Production MCP should use only explicit HiveWatch APIs.

Allowed behavior:
- Ask the user for missing required fields.
- List existing environments and topology through API.
- Run explicit probes for user-supplied candidates.
- Build a HiveWatch-owned provisioning plan DTO.
- Validate the plan through API.
- Present errors, warnings, and diff.
- Ask for human approval before apply.
- Apply the plan through API.

Not allowed:
- Direct DB writes.
- Private config formats.
- Hidden adapter fallback.
- Broad network scans without explicit candidate scope.
- Silent mutation based on probe results.
- Secret logging.

### 7.2 Development MCP

Start with a dev-MCP that is intentionally more powerful and explicitly non-production.

Purpose:
- Help developers and AI coding agents inspect progress quickly.
- Inspect DB state during feature implementation.
- Inspect dummy-stack/testbed state.
- Compare API output with persisted state.
- Run targeted probes and normal scans during development.
- Surface validation/audit/versioning gaps while the feature is being built.

Allowed dev-only capabilities:
- Read-only DB inspection.
- Local Docker/testbed inspection.
- Calling local HiveWatch APIs with dev credentials.
- Running project scripts such as dummy-stack startup/status commands.
- Reading logs from local containers.
- Optional controlled DB cleanup/reset only through explicit dev commands or existing project reset scripts.

Boundaries:
- Dev-MCP must be disabled by default in production.
- Dev-MCP must be clearly documented as non-production tooling.
- Dev-MCP may read DB state, but production MCP must not depend on DB access.
- Any production behavior proven useful through dev-MCP must graduate into a normal HiveWatch API before MCP relies on it.
- Dev-MCP must not become the canonical owner of provisioning rules.

This gives fast feedback during implementation without weakening the target architecture.

## 8. Security and Audit

Auth decisions:
- Probe and provisioning apply should initially require admin access unless a narrower operator permission is added.
- If MCP uses a service account, audit should record that principal.
- If delegated user auth is available, prefer delegated/OBO identity so audit reflects the human operator.

Audit metadata:
- `actor` from authenticated principal.
- `source`, for example `UI`, `MCP`, `CLI`, or `DEV_MCP`.
- `correlationId`, optional.
- `reason`, optional but recommended.
- plan hash or revision reference.
- summary of created/updated/deleted objects.

When config revisioning is implemented, `apply` should create the revision record internally. MCP should not need any contract change if it already uses the apply API.

## 9. Implementation Slices

### Slice 1: Dev-MCP

- Add a local dev-MCP server or scriptable MCP tools for developers.
- Expose DB read inspection, testbed status, API calls, and logs.
- Keep it explicitly non-production.
- Document setup and safety boundaries.

### Slice 2: Probe API

- Add `TargetAdapterTypeDto`.
- Add probe request/result DTOs.
- Reuse Tomcat Manager HTML client/parser for probe.
- Reuse Actuator client/validation for probe.
- Add controller and service.
- Add tests for success and explicit failure classification.

### Slice 3: Provisioning Plan Validation

- Add canonical plan DTOs.
- Add validation service.
- Validate conflicts against existing environments, servers, and targets.
- Return structured errors/warnings/diff.
- Add unit tests for validation rules.

### Slice 4: Provisioning Plan Apply

- Add apply request/result DTOs.
- Re-run validation inside apply.
- Persist environment, servers, targets, and expected sets in one service boundary.
- Return stable object IDs and summary.
- Keep internals ready for audit/versioning insertion.

### Slice 5: UI Probe Button

- Add target form "Test endpoint" using probe API.
- Show observed metadata and classified errors.
- Keep save/apply separate from probe.

### Slice 6: UI Provisioning Wizard

- Build plans using the canonical DTO.
- Validate before apply.
- Show diff and blocking errors.
- Apply through the provisioning API.

### Slice 7: Production MCP

- Use normal HiveWatch APIs only.
- Build the same canonical plan DTO as UI.
- Use probe as an explicit endpoint check.
- Require human approval before apply.

### Slice 8: Audit and Config Revisioning

- Add audit/config revision tables and services.
- Wire apply through revision creation.
- Preserve the public provisioning API contract.
- Ensure MCP and UI automatically benefit from revisioning by continuing to call apply.

## 10. Test Plan

- Probe tests:
  - Tomcat success.
  - Tomcat auth failure.
  - Tomcat connectivity failure.
  - Tomcat invalid URL rejection.
  - Actuator success.
  - Actuator profile validation failure.
  - Actuator parse failure.
- Validation tests:
  - missing required fields.
  - duplicate environment name.
  - duplicate server name in environment.
  - duplicate target role per server.
  - baseUrl with port/path rejected.
  - invalid timeout rejected.
  - expected set references checked.
- Apply tests:
  - successful create environment with servers and targets.
  - apply rejects stale or invalid plan.
  - partial write rollback on error.
  - apply summary includes created IDs.
- Security tests:
  - admin can probe/validate/apply.
  - non-admin rejected until a narrower role is explicitly added.
- NFF regression tests:
  - no automatic switch from Tomcat HTML to text API.
  - no automatic switch from HTTP to HTTPS.
  - no automatic alternate port probing inside one probe call.

## 11. Open Decisions

- Whether operators, environment managers, or only admins may apply provisioning plans.
- Whether probe API should be admin-only or available to operators with environment visibility.
- Whether initial apply supports updates/deletes or only creates.
- Whether direct `password` remains acceptable short-term or must be replaced by `secretRef` before production.
- Whether apply should trigger scans by explicit flag or always rely on the scheduler.
- Whether plan validation should support dry-run diffs for updates in the first implementation slice.
