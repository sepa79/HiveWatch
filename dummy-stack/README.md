# Dummy Stack (M8)

Local stack for deterministic Health Checker development:
- three environment sets (`NFT-01`, `NFT-02`, `Release-01`),
- `NFT-01` has two servers (`Touchpoint`, `Services`) and each server runs three Tomcats: `payments`, `services`, `auth`,
- `NFT-02` has one server (`All-in-one`) and runs three Tomcats: `payments`, `services`, `auth`,
- `Release-01` has one server (`All-in-one`) and runs three Tomcats: `payments`, `services`, `auth`,
- `NFT-01` additionally simulates a `Docker Swarm` node with 3 microservices exposed as actuator-style endpoints,
- all Tomcats expose `/manager/html` with fixed credentials.

## 1) Start/Stop local infrastructure

From repository root:

```bash
./tools/run-dummy-stack.sh up
./tools/run-dummy-stack.sh ps
./tools/run-dummy-stack.sh down
```

Tomcat manager credentials:
- username: `hc-manager`
- password: `hc-manager-pass`

## 2) Exposed endpoints

Tomcat managers (`/manager/html`):
- `NFT-01 / Touchpoint`: `payments` `http://localhost:19111`, `services` `http://localhost:19112`, `auth` `http://localhost:19113`
- `NFT-01 / Services`: `payments` `http://localhost:19114`, `services` `http://localhost:19115`, `auth` `http://localhost:19116`
- `NFT-02 / All-in-one`: `payments` `http://localhost:19211`, `services` `http://localhost:19212`, `auth` `http://localhost:19213`
- `Release-01 / All-in-one`: `payments` `http://localhost:19311`, `services` `http://localhost:19312`, `auth` `http://localhost:19313`

From inside Docker (e.g. `hive-watch-service` container), the same Tomcats are reachable via the shared `hivewatch-dev` network:
- `NFT-01 / Touchpoint`: `http://hc-dummy-nft-01-touchpoint-tomcats:8081/manager/html`, `:8082`, `:8083`
- `NFT-01 / Services`: `http://hc-dummy-nft-01-services-tomcats:8081/manager/html`, `:8082`, `:8083`
- `NFT-02 / All-in-one`: `http://hc-dummy-nft-02-all-in-one-tomcats:8081/manager/html`, `:8082`, `:8083`
- `Release-01 / All-in-one`: `http://hc-dummy-release-01-all-in-one-tomcats:8081/manager/html`, `:8082`, `:8083`

Example webapps:
- `http://localhost:19111/PaymentApp1/`
- `http://localhost:19111/PaymentApp2/`
- `http://localhost:19115/ServicesApp2/`
- `http://localhost:19113/SSOConsole/`

Simulated microservices (actuator-style):
- `NFT-01 / Docker Swarm`: base `http://localhost:19121` (`payments`, `services`, `auth`)
- `NFT-02 / Docker Swarm`: base `http://localhost:19221` (`payments`, `services`, `auth`)
- `Release-01 / Docker Swarm`: base `http://localhost:19321` (`payments`, `services`, `auth`)

Examples:
- `http://localhost:19121/payments/actuator/health`
- `http://localhost:19121/services/actuator/info`
- `http://localhost:19121/auth/actuator/metrics/system.cpu.usage`
- `http://localhost:19121/auth/actuator/metrics/jvm.memory.used`

From inside Docker (shared `hivewatch-dev` network), actuator bases are:
- `NFT-01`: `http://hc-dummy-nft-01-docker-swarm-microservices:8080/{profile}/actuator/...`
- `NFT-02`: `http://hc-dummy-nft-02-docker-swarm-microservices:8080/{profile}/actuator/...`
- `Release-01`: `http://hc-dummy-release-01-docker-swarm-microservices:8080/{profile}/actuator/...`

## 3) Next: wiring into Health Checker

Ten repo na razie zawiera tylko sam stack (Tomcat + mock microservices).

Jeśli chcesz, w następnym kroku skopiuję z `~/TomcatScanner` skrypty typu:
- bootstrap configu dummy-stack do Health Checkera,
- “scenario runner” do Pull API,
- refresh manager fixtures.
