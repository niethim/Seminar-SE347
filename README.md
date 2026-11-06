# Flash-Voucher Claim & Clearing Demo

A Jakarta EE 10 demonstration of concurrent voucher stock claiming under heavy contention, asynchronous voucher fulfillment via JMS & MDB, and chunk-oriented batch reconciliation.

## Architecture

- **Frontend (`fe/`)**: Vanilla HTML5, CSS3, ES6 JavaScript (`fetch` API). Zero external dependencies or build tools.
- **Backend (`be/`)**: Pure Jakarta EE 10 (JAX-RS 3.1, CDI 4.0, EJB 4.0, JPA 3.1, JTA 2.0, Jakarta Messaging 3.1, Jakarta Batch 2.1, Jakarta Security 3.0, Interceptors 2.1) deployed on WildFly.
- **Infrastructure**: PostgreSQL 16 and ActiveMQ Artemis managed via `compose.yml` (supports both Podman and Docker).

## Quick Start

### 1. Start Infrastructure
```bash
make infra-up
```
Starts PostgreSQL (port 5432) and ActiveMQ Artemis (port 61616 & 8161) with persistent volumes and health checks.

### 2. Build Backend
```bash
make backend-build
```

### 3. Run Backend (WildFly)
```bash
make backend-run
```
Provisions and starts WildFly on `http://localhost:8080/be/api`.
- Default Admin credentials for batch reconciliation: `admin` / `admin123!`

### 4. Run Frontend
```bash
make frontend
```
Serves the UI at `http://localhost:3000`.

### 5. Tear Down & Clean
```bash
make clean
```

## Key Endpoints

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/be/api/vouchers/{code}` | Get voucher stock status | Public |
| `POST` | `/be/api/vouchers/{code}/claim` | Claim voucher under pessimistic lock | Public |
| `GET` | `/be/api/vouchers/{code}/claims/{claimCode}` | Get claim status (CLAIMED -> ISSUED) | Public |
| `POST` | `/be/api/vouchers/{code}/init?stock=50` | Reset / reseed voucher stock | Public |
| `POST` | `/be/api/batch/reconciliation` | Trigger Jakarta Batch chunk reconciliation | Public |
| `GET` | `/be/api/batch/reconciliation/{id}` | Check batch execution status | Public |
| `GET` | `/be/api/batch/records` | Query persisted batch clearing records | Public |
| `GET` | `/be/api/audit-logs` | Query distributed audit trail | Public |

## Enterprise Features Verified
- **Concurrency & Locking**: JPA `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`) prevents overselling under heavy parallel load.
- **Atomicity**: Container-managed JTA transaction ensures `simulateFraud=true` rolls back stock decrement and aborts JMS send.
- **Idempotency**: PostgreSQL unique constraint on `(voucher_id, user_id)` and `claim_code` rejects duplicate claims with `409 Conflict`.
- **Asynchronous Issuance**: Message-Driven Bean (`VoucherIssuanceMDB`) consumes from ActiveMQ Artemis queue and updates claim status to `ISSUED`.
- **Batch Chunk Processing**: Jakarta Batch chunk job (`reconciliationJob.xml`) aggregates issued vouchers in chunks and writes `batch_clearing_records`.
- **Audit Interceptor**: CDI `@AroundInvoke` interceptor logs trace ID, caller principal, action, and execution duration in ms.
